package goncharovmv.page_rank.single_node.graph.chunk;

import goncharovmv.page_rank.single_node.graph.GraphMetadata;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public final class TxtFileChunkedGraph implements ChunkedGraph {

    private final GraphMetadata metadata;
    private final String filePath;
    private BufferedReader reader;

    public TxtFileChunkedGraph(String filePath) throws IOException {
        this.filePath = filePath;
        this.metadata = computeMetadata(filePath);
        reset();
    }

    @Override
    public GraphMetadata metadata() {
        return metadata;
    }

    @Override
    public Map<Integer, List<Integer>> nextAdjListChunk(int chunkSize) {
        Map<Integer, List<Integer>> chunk = new HashMap<>();
        try {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < chunkSize) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 2) {
                    continue;
                }
                int u = Integer.parseInt(parts[0]);
                int v = Integer.parseInt(parts[1]);
                chunk.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
                count++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return chunk;
    }

    @Override
    public void reset() {
        if (reader != null) {
            try {
                reader.close();
                reader = new BufferedReader(new FileReader(filePath));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private GraphMetadata computeMetadata(String file) throws IOException {
        Map<Integer, Integer> outDegrees = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 2) {
                    continue;
                }
                int u = Integer.parseInt(parts[0]);
                outDegrees.put(u, outDegrees.getOrDefault(u, 0) + 1);

                //Кладем и v в outDegrees
                int v = Integer.parseInt(parts[1]);
                outDegrees.putIfAbsent(v, 0);
            }
        }
        return () -> outDegrees;
    }
}
