package goncharovmv.page_rank.single_node.pr.concurrent;

import goncharovmv.page_rank.single_node.conditions.Conditions;
import goncharovmv.page_rank.single_node.graph.simple.SimpleGraph;
import goncharovmv.page_rank.single_node.pr.PageRank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public final class ConcurrentPageRank implements PageRank<SimpleGraph> {

    private final double dampingFactor;
    private final int maxIterations;
    private final double convergenceThreshold;
    private final ExecutorService executorService;

    public ConcurrentPageRank(
            double dampingFactor,
            int maxIterations,
            double convergenceThreshold,
            ExecutorService executorService
    ) {
        this.dampingFactor = Conditions.checkBetween(dampingFactor,0, 1);
        this.maxIterations = Conditions.checkPositive(maxIterations);
        this.convergenceThreshold = Conditions.checkPositive(convergenceThreshold);
        this.executorService = executorService;
    }

    public static ConcurrentMap<Integer, Double> getInitialRanks(SimpleGraph adjListSimpleGraphImpl) {
        Set<Integer> vertices = adjListSimpleGraphImpl.metadata().vertices();

        ConcurrentMap<Integer, Double> ranks = new ConcurrentHashMap<>();
        for (int v : vertices) {
            ranks.put(v, 1.0 / vertices.size());
        }
        return ranks;
    }

    @Override
    public Map<Integer, Double> compute(SimpleGraph graph) {
        Set<Integer> vertices = Objects.requireNonNull(graph).metadata().vertices();
        if (vertices.isEmpty()) {
            return Map.of();
        }

        Map<Integer, Double> currentRanks = getInitialRanks(graph);

        for (int __ = 0; __ < this.maxIterations; __++) {
            List<Future<NodeRank>> futures = new ArrayList<>();

            for (int v : vertices) {
                futures.add(this.executorService.submit(
                        new VertexRankReComputationTask(v, graph, currentRanks, this.dampingFactor)));
            }

            Map<Integer, Double> newRanks = new ConcurrentHashMap<>();

            for (Future<NodeRank> f : futures) {
                try {
                    NodeRank result = f.get(); //каждая таска бежит параллельно, get() блокирует текущую таску до получения результата
                    newRanks.put(result.node, result.rank);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }

            double diff = 0.0;
            for (int v : vertices) {
                diff += Math.abs(newRanks.get(v) - currentRanks.get(v));
            }
            if (diff < this.convergenceThreshold) {
                return Map.copyOf(newRanks);
            }

            currentRanks.putAll(newRanks);
        }

        return Map.copyOf(currentRanks);
    }

    private static final class VertexRankReComputationTask implements Callable<NodeRank> {

        private final int vertex;
        private final SimpleGraph graph;
        private final Map<Integer, Double> currentRanks;
        private final double dampingFactor;

        public VertexRankReComputationTask(
                int vertex,
                SimpleGraph graph,
                Map<Integer, Double> currentRanks,
                double dampingFactor
        ) {
            this.vertex = vertex;
            this.graph = graph;
            this.currentRanks = currentRanks;
            this.dampingFactor = dampingFactor;
        }

        @Override
        public NodeRank call() {
            double rankSum = 0.0;
            for (int u : this.graph.metadata().vertices()) {
                if (this.graph.outgoingVertices(u).contains(this.vertex)) {
                    int outDegree = this.graph.metadata().outgoingDegrees().get(u);
                    if (outDegree > 0) {
                        rankSum += this.currentRanks.get(u) / outDegree;
                    }
                }
            }
            double newRank = ((1 - this.dampingFactor) / this.graph.metadata().vertices().size())
                    + (this.dampingFactor * rankSum);
            return new NodeRank(this.vertex, newRank);
        }
    }

    private static final class NodeRank {

        private final int node;
        private final double rank;

        private NodeRank(int node, double rank) {
            this.node = node;
            this.rank = rank;
        }
    }
}