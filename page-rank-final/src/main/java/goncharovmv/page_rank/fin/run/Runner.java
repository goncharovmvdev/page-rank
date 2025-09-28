package goncharovmv.page_rank.fin.run;

import goncharovmv.page_rank.fin.config.Config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class Runner {

    private static final int INT_BYTES = 4;
    private static final int DOUBLE_BYTES = 8;

    private final Config cfg;
    private final Path outdegFile;
    private final Path ranksFileA;
    private final Path ranksFileB;
    private final Path chunksDir;

    public Runner(Config cfg) {
        this.cfg = cfg;
        this.outdegFile = cfg.outDir().resolve("outdeg.bin");
        this.ranksFileA = cfg.outDir().resolve("ranksA.bin");
        this.ranksFileB = cfg.outDir().resolve("ranksB.bin");
        this.chunksDir = cfg.outDir().resolve("chunks");
    }

    public void run() throws Exception {
        System.out.println(cfg);
        buildOutdegFile();

        initRanksFile(ranksFileA);

        Path curRanks = ranksFileA;
        Path nextRanks = ranksFileB;

        for (int iter = 1; iter <= cfg.maxIter(); iter++) {
            System.out.println("Iteration " + iter + " ...");

            double danglingMass = computeDanglingMass(curRanks);
            System.out.printf("  Dangling mass: %.6f%n", danglingMass);

            Files.createDirectories(chunksDir);
            List<Path> chunkFiles = createContributionChunks(curRanks);

            aggregateChunksToNewRanks(chunkFiles, nextRanks, danglingMass);

            double l1 = computeL1Diff(curRanks, nextRanks);
            System.out.printf("  L1 diff: %.10e%n", l1);

            cleanupChunks(chunkFiles);

            if (l1 < cfg.tol()) {
                System.out.println("Converged at iteration " + iter + ", L1=" + l1);

                if (curRanks != nextRanks) {
                    Files.move(
                            nextRanks,
                            curRanks.resolveSibling("ranks_final.bin"),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                    Files.move(
                            curRanks,
                            nextRanks.resolveSibling("ranks_prev.bin"),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }
                break;
            }

            Path tmp = curRanks;
            curRanks = nextRanks;
            nextRanks = tmp;
        }

        System.out.println("PageRank finished. Final rank file likely at: " + curRanks.toAbsolutePath());

        dumpRanksToText(
                Paths.get("prout/ranks_final.bin"),
                Paths.get("prout/ranks_final.txt")
        );
    }

    private void buildOutdegFile() throws Exception {
        System.out.println("Building outdegree file: " + outdegFile);

        int numNodes = cfg.numNodes();
        int[] outdeg = new int[numNodes];

        try (BufferedReader br = Files.newBufferedReader(cfg.edgesFile())) {
            String line;
            long lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if ((lineNo & 0x1FFFFF) == 0) {
                    System.out.printf("  processed %d edges for outdeg%n", lineNo);
                }

                String s = line.trim();
                if (s.isEmpty()) {
                    continue;
                }

                String[] parts = s.split("\\s+");
                if (parts.length < 2) {
                    continue;
                }

                int u = Integer.parseInt(parts[0]);
                if (u >= 0 && u < numNodes) {
                    outdeg[u]++;
                }
            }
            System.out.printf("  finished outdeg, processed %d edge lines%n", lineNo);
        }

        try (
                DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(outdegFile)))
        ) {
            for (int i = 0; i < numNodes; i++) {
                dos.writeInt(outdeg[i]);
            }
        }
    }

    private void initRanksFile(Path ranksFile) throws IOException {
        long numNodes = cfg.numNodes();
        System.out.println("Initializing rank file: " + ranksFile);

        try (RandomAccessFile raf = new RandomAccessFile(ranksFile.toFile(), "rw")) {
            raf.setLength(numNodes * DOUBLE_BYTES);
            double init = 1.0 / numNodes;
            for (long i = 0; i < numNodes; i++) {
                raf.writeDouble(init);
            }
        }
    }

    private double computeDanglingMass(Path ranksFile) throws Exception {
        long n = cfg.numNodes();
        double dangling = 0.0;
        try (
                RandomAccessFile rafOutdeg = new RandomAccessFile(outdegFile.toFile(), "r");
                RandomAccessFile rafRank = new RandomAccessFile(ranksFile.toFile(), "r")
        ) {
            for (int i = 0; i < n; i++) {
                rafOutdeg.seek((long) i * INT_BYTES);
                int deg = rafOutdeg.readInt();
                rafRank.seek((long) i * DOUBLE_BYTES);
                double r = rafRank.readDouble();
                if (deg == 0) {
                    dangling += r;
                }
            }
        }
        return dangling;
    }

    private List<Path> createContributionChunks(Path curRanks) throws Exception {
        System.out.println("Creating contribution chunks ...");
        int chunkLimit = cfg.memChunkLimitRecords();
        List<Path> chunkFiles = Collections.synchronizedList(new ArrayList<>());

        ExecutorService sorterPool = Executors.newFixedThreadPool(
                Math.max(1, Runtime.getRuntime().availableProcessors() / 2)
        );
        List<Future<?>> tasks = new ArrayList<>();

        int[] keys = new int[chunkLimit];
        double[] vals = new double[chunkLimit];
        int idx = 0;

        try (BufferedReader br = Files.newBufferedReader(cfg.edgesFile());
             DataInputStream disOutdeg = new DataInputStream(new BufferedInputStream(Files.newInputStream(outdegFile)));
             DataInputStream disRank = new DataInputStream(new BufferedInputStream(Files.newInputStream(curRanks)))
        ) {

            int numNodes = cfg.numNodes();
            int[] outdegArr = new int[numNodes];
            double[] rankArr = new double[numNodes];

            for (int i = 0; i < numNodes; i++) {
                outdegArr[i] = disOutdeg.readInt();
            }

            for (int i = 0; i < numNodes; i++) {
                rankArr[i] = disRank.readDouble();
            }

            String line;
            long seq = 0;
            while ((line = br.readLine()) != null) {
                seq++;
                if ((seq & 0x1FFFFF) == 0) {
                    System.out.printf("  scanned %d edges%n", seq);
                }

                String s = line.trim();
                if (s.isEmpty() || s.charAt(0) == '#') {
                    continue;
                }

                String[] p = s.split("\\s+");
                if (p.length < 2) {
                    continue;
                }

                int u = Integer.parseInt(p[0]);
                int v = Integer.parseInt(p[1]);

                int deg = outdegArr[u];
                if (deg == 0) {
                    continue;
                }

                double contrib = rankArr[u] / deg;

                keys[idx] = v;
                vals[idx] = contrib;
                idx++;

                if (idx >= chunkLimit) {
                    int use = idx;
                    int[] keysCopy = Arrays.copyOf(keys, use);
                    double[] valsCopy = Arrays.copyOf(vals, use);
                    idx = 0;
                    Future<?> f = sorterPool.submit(() -> {
                        try {
                            Path chunk = writeSortedChunk(keysCopy, valsCopy);
                            chunkFiles.add(chunk);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                    tasks.add(f);
                }
            }
            if (idx > 0) {
                int[] keysCopy = Arrays.copyOf(keys, idx);
                double[] valsCopy = Arrays.copyOf(vals, idx);
                Future<?> f = sorterPool.submit(() -> {
                    try {
                        Path chunk = writeSortedChunk(keysCopy, valsCopy);
                        chunkFiles.add(chunk);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
                tasks.add(f);
            }
        }

        sorterPool.shutdown();
        for (Future<?> t : tasks) {
            t.get();
        }

        chunkFiles.sort(Comparator.comparing(Path::toString));
        System.out.println("  created " + chunkFiles.size() + " chunk files");
        return chunkFiles;
    }

    private Path writeSortedChunk(
            int[] keys,
            double[] vals
    ) throws IOException {
        int n = keys.length;
        Integer[] idxs = new Integer[n];
        for (int i = 0; i < n; i++) {
            idxs[i] = i;
        }
        Arrays.sort(idxs, Comparator.comparingInt(i -> keys[i]));

        Path tmp = Files.createTempFile(chunksDir, "chunk_", ".bin");

        try (
                FileOutputStream fos = new FileOutputStream(tmp.toFile());
                BufferedOutputStream bos = new BufferedOutputStream(fos, 1 << 20)
        ) {
            DataOutputStream dos = new DataOutputStream(bos);
            for (int id : idxs) {
                dos.writeInt(keys[id]);
                dos.writeDouble(vals[id]);
            }
            dos.flush();
        }
        return tmp;
    }

    private void aggregateChunksToNewRanks(
            List<Path> chunkFiles,
            Path outRanksFile,
            double danglingMass
    ) throws Exception {
        System.out.println("Aggregating chunks to new ranks: " + outRanksFile);
        long n = cfg.numNodes();
        double damping = cfg.damping();
        double base = (1.0 - damping) / n;
        double danglingContribution = damping * danglingMass / n;

        try (RandomAccessFile raf = new RandomAccessFile(outRanksFile.toFile(), "rw")) {
            raf.setLength(n * DOUBLE_BYTES);
            double init = base + danglingContribution;
            for (long i = 0; i < n; i++) {
                raf.writeDouble(init);
            }
        }

        if (chunkFiles.isEmpty()) {
            System.out.println("  no contributions (no edges or all dangling). Wrote uniform ranks.");
            return;
        }

        int k = chunkFiles.size();
        DataInputStream[] ins = new DataInputStream[k];
        for (int i = 0; i < k; i++) {
            ins[i] = new DataInputStream(new BufferedInputStream(new FileInputStream(chunkFiles.get(i).toFile()), 1 << 20));
        }


        PriorityQueue<Entry> pq = new PriorityQueue<>(Comparator.comparingInt(e -> e.key));

        for (int i = 0; i < k; i++) {
            try {
                int key = ins[i].readInt();
                double val = ins[i].readDouble();
                pq.add(new Entry(key, val, i));
            } catch (EOFException eof) {
                ins[i].close();
                ins[i] = null;
            }
        }

        try (RandomAccessFile rafOut = new RandomAccessFile(outRanksFile.toFile(), "rw")) {
            while (!pq.isEmpty()) {
                Entry e = pq.poll();
                int key = e.key;
                double sum = e.value;
                int fi = e.fileIdx;
                while (!pq.isEmpty() && pq.peek().key == key) {
                    Entry e2 = pq.poll();
                    sum += e2.value;
                    int fi2 = e2.fileIdx;
                    try {
                        int k2 = ins[fi2].readInt();
                        double v2 = ins[fi2].readDouble();
                        pq.add(new Entry(k2, v2, fi2));
                    } catch (EOFException eof) {
                        ins[fi2].close();
                        ins[fi2] = null;
                    }
                }
                try {
                    int nk = ins[fi].readInt();
                    double nv = ins[fi].readDouble();
                    pq.add(new Entry(nk, nv, fi));
                } catch (EOFException eof) {
                    if (ins[fi] != null) {
                        ins[fi].close();
                        ins[fi] = null;
                    }
                }
                double contrib = damping * sum;
                long pos = (long) key * DOUBLE_BYTES;
                rafOut.seek(pos);
                double cur = rafOut.readDouble();
                rafOut.seek(pos);
                rafOut.writeDouble(cur + contrib);
            }
        } finally {
            for (DataInputStream di : ins)
                if (di != null) try {
                    di.close();
                } catch (IOException ignored) {
                    //throw new UncheckedIOException(e);
                }
        }
        System.out.println("  aggregated contributions to ranks file.");
    }

    private double computeL1Diff(Path a, Path b) throws IOException {
        long n = cfg.numNodes();
        double sum = 0.0;
        try (
                RandomAccessFile rafA = new RandomAccessFile(a.toFile(), "r");
                RandomAccessFile rafB = new RandomAccessFile(b.toFile(), "r")
        ) {
            for (int i = 0; i < n; i++) {
                rafA.seek((long) i * DOUBLE_BYTES);
                rafB.seek((long) i * DOUBLE_BYTES);
                double va = rafA.readDouble();
                double vb = rafB.readDouble();
                sum += Math.abs(va - vb);
            }
        }
        return sum;
    }

    private void cleanupChunks(List<Path> chunkFiles) {
        for (Path p : chunkFiles) {
            try {
                Files.deleteIfExists(p);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        try {
            Files.deleteIfExists(chunksDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void dumpRanksToText(
            Path ranksFile,
            Path textFile
    ) throws IOException {
        try (
                DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(ranksFile)));
                BufferedWriter bw = Files.newBufferedWriter(textFile)
        ) {

            for (int i = 0; i < cfg.numNodes(); i++) {
                double rank = dis.readDouble();
                bw.write(i + "\t" + rank);
                bw.newLine();
            }
        }
    }

    private static class Entry {
        private final int key;
        private final double value;
        private final int fileIdx;

        Entry(int k, double v, int fi) {
            key = k;
            value = v;
            fileIdx = fi;
        }
    }
}
