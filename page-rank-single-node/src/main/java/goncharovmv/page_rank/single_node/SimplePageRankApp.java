package goncharovmv.page_rank.single_node;

import goncharovmv.page_rank.single_node.graph.chunk.TxtFileChunkedGraph;
import goncharovmv.page_rank.single_node.graph.simple.AdjListSimpleGraphImpl;
import goncharovmv.page_rank.single_node.pr.chunk.ChunkedPageRank;
import goncharovmv.page_rank.single_node.pr.concurrent.ConcurrentPageRankCreator;
import goncharovmv.page_rank.single_node.pr.concurrent.ConcurrentPageRankSettings;

import java.util.Map;
import java.util.TreeMap;

public final class SimplePageRankApp {
    public static void main(String[] args) {
        AdjListSimpleGraphImpl adjListSimpleGraphImpl = AdjListSimpleGraphImpl
                .builder()
                .addEdge(1, 2)
                .addEdge(2, 3)
                .addEdge(3, 1)
                .addEdge(4, 1)
                .build();

        Map<Integer, Double> pageRanks = new TreeMap<>(ConcurrentPageRankCreator
                .fromConfig(ConcurrentPageRankSettings.DEFAULT)
                .compute(adjListSimpleGraphImpl));
        System.out.println("Concurrent " + pageRanks);

        Map<Integer, Double> chunked = new ChunkedPageRank()
                .compute(new TxtFileChunkedGraph("graph.txt"));

        if(! pageRanks.equals(chunked)) {
            throw new AssertionError();
        }
    }
}