package goncharovmv.page_rank.fin;

import goncharovmv.page_rank.fin.config.Config;
import goncharovmv.page_rank.fin.run.Runner;
import goncharovmv.page_rank.fin.util.EdgesTxtCreator;

import java.nio.file.Files;
import java.nio.file.Path;

public class FinalPageRankApp {

    public static void main(String[] args) throws Exception {
        int numEdges = 100_000;
        Config config = Config.DEFAULT;

        EdgesTxtCreator.createEdgesTxt(
                Path.of("edges.txt"),
                config.numNodes(),
                numEdges
        );
        Files.createDirectories(config.outDir());

        new Runner(config).run();
    }
}