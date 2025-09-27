package goncharovmv.page_rank.single_node.pr;

import goncharovmv.page_rank.single_node.graph.Graph;

import java.util.Map;

public interface PageRank<G extends Graph> {

    Map<Integer, Double> compute(G graph);
}
