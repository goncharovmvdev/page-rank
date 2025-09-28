package goncharovmv.page_rank.fin.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public final class EdgesTxtCreator {

    private static final Random rand = new Random();

    private EdgesTxtCreator() {
        throw new AssertionError();
    }

    public static void createEdgesTxt(
            Path path,
            int numNodes,
            int numEdges
    ) {
        if (path.toFile().exists()) {
            return;
        }

        try {

            Path createdFile = Files.createFile(path);

            try (BufferedWriter bw = Files.newBufferedWriter(createdFile)) {
                for (int i = 0; i < numEdges; i++) {
                    int u = rand.nextInt(numNodes);
                    int v = rand.nextInt(numNodes);

                    // нет связей ноды с собой
                    if (u == v) {
                        v = (v + 1) % numNodes;
                    }

                    bw.write(u + " " + v);
                    bw.newLine();
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
