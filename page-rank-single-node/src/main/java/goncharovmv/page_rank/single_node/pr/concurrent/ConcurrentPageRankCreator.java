package goncharovmv.page_rank.single_node.pr.concurrent;

import goncharovmv.page_rank.single_node.graph.simple.SimpleGraph;
import goncharovmv.page_rank.single_node.pr.PageRank;

public final class ConcurrentPageRankCreator {

    private ConcurrentPageRankCreator() {
        throw new AssertionError();
    }

    public static PageRank<SimpleGraph> fromConfig(
            final ConcurrentPageRankSettings concurrentPageRankSettings
    ) {
        return new ConcurrentPageRank(
                concurrentPageRankSettings.getDampingFactor(),
                concurrentPageRankSettings.getMaxIterations(),
                concurrentPageRankSettings.getConvergenceThreshold(),
                concurrentPageRankSettings.getExecutorService()
        );
    }
}
