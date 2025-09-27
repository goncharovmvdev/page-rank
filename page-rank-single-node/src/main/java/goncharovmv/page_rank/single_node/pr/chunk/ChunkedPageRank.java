package goncharovmv.page_rank.single_node.pr.chunk;

import goncharovmv.page_rank.single_node.graph.GraphMetadata;
import goncharovmv.page_rank.single_node.graph.chunk.ChunkedGraph;
import goncharovmv.page_rank.single_node.pr.PageRank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class ChunkedPageRank implements PageRank<ChunkedGraph> {
    private static final double DAMPING = 0.85;
    private static final double TOLERANCE = 1e-6;
    private static final int MAX_ITERS = 100;

    @Override
    public Map<Integer, Double> compute(ChunkedGraph graph) {
        GraphMetadata metadata = graph.metadata();
        Set<Integer> vertices = metadata.vertices();
        Map<Integer, Integer> outDegrees = metadata.outgoingDegrees();

        Map<Integer, Double> rank = new HashMap<>();
        Map<Integer, Double> newRank = new HashMap<>();
        double initialRank = 1.0 / vertices.size();
        for (int v : vertices) {
            rank.put(v, initialRank);
            newRank.put(v, 0.0);
        }

        for (int iter = 0; iter < MAX_ITERS; iter++) {
            double teleport = (1 - DAMPING) / vertices.size();
            for (int v : vertices) {
                newRank.put(v, teleport);
            }

            graph.reset();
            Map<Integer, List<Integer>> chunk;
            while ((chunk = graph.nextAdjListChunk(1_000_000)) != null) {
                for (Map.Entry<Integer, List<Integer>> entry : chunk.entrySet()) {
                    int u = entry.getKey();
                    List<Integer> neighbors = entry.getValue();
                    int degree = outDegrees.getOrDefault(u, 0);
                    if (degree == 0) {
                        continue;
                    }
                    double contrib = DAMPING * rank.get(u) / degree;
                    for (int v : neighbors) {
                        newRank.put(v, newRank.getOrDefault(v, 0.0) + contrib);
                    }
                }
            }

            // Compute convergence
            double diff = 0.0;
            for (int v : vertices) {
                diff += Math.abs(newRank.get(v) - rank.get(v));
            }
            if (diff < TOLERANCE) {
                break;
            }

            // Swap
            Map<Integer, Double> temp = rank;
            rank = newRank;
            newRank = temp;
        }

        return rank;
    }
}
