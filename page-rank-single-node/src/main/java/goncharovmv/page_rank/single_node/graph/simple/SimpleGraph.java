package goncharovmv.page_rank.single_node.graph.simple;

import goncharovmv.page_rank.single_node.graph.Graph;
import goncharovmv.page_rank.single_node.graph.GraphMetadata;

import java.util.List;

public interface SimpleGraph extends Graph {

    GraphMetadata metadata();

    List<Integer> outgoingVertices(int vertex);
}
