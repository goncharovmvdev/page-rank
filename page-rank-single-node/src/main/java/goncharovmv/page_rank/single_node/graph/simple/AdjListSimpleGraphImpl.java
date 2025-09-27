package goncharovmv.page_rank.single_node.graph.simple;

import goncharovmv.page_rank.single_node.graph.GraphMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public final class AdjListSimpleGraphImpl implements SimpleGraph {

    private final Map<Integer, List<Integer>> adjList;//read-only
    private final GraphMetadata metadata;

    private AdjListSimpleGraphImpl(Map<Integer, List<Integer>> adjList) {
        this.adjList = adjList.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue())
                ));
        this.metadata = new AdjListGraphMetadata(this.adjList);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public GraphMetadata metadata() {
        return this.metadata;
    }

    @Override
    public List<Integer> outgoingVertices(int vertex) {
        if (!adjList.containsKey(vertex)) {
            throw new IllegalArgumentException("No vertex in graph " + adjList);
        }
        return adjList.getOrDefault(vertex, List.of());
    }

    public static final class Builder {

        private final ConcurrentMap<Integer, List<Integer>> adjList = new ConcurrentHashMap<>();

        public Builder addEdge(int src, int dst) {
            adjList.computeIfAbsent(src, key -> new ArrayList<>()).add(dst);
            adjList.computeIfAbsent(dst, key -> new ArrayList<>());//Даже если ранее не было dst в списке, то надо его добавить (он существует в графе)
            return this;
        }

        public AdjListSimpleGraphImpl build() {
            return new AdjListSimpleGraphImpl(adjList);
        }
    }

    private static class AdjListGraphMetadata implements GraphMetadata {

        private final Map<Integer, Integer> outgoingDegrees;

        private AdjListGraphMetadata(Map<Integer, List<Integer>> adjList) {
            this.outgoingDegrees = adjList.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().size()
                    ));
        }

        @Override
        public Map<Integer, Integer> outgoingDegrees() {
            return outgoingDegrees;
        }
    }
}
