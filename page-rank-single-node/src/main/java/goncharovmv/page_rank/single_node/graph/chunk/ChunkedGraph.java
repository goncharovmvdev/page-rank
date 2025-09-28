package goncharovmv.page_rank.single_node.graph.chunk;

import goncharovmv.page_rank.single_node.graph.Graph;
import goncharovmv.page_rank.single_node.graph.GraphMetadata;

import java.util.List;
import java.util.Map;

public interface ChunkedGraph extends Graph {

    GraphMetadata metadata();

    Map<Integer, List<Integer>> nextAdjListChunk(int chunkSize);
}
