package goncharovmv.page_rank.fin.config;

import java.nio.file.Path;
import java.util.Objects;

public final class Config {

    public static final Config DEFAULT = new Builder()
            .build();

    private final Path edgesFile;
    private final int numNodes;
    private final Path outDir;
    private final int memMB;
    private final int maxIter;
    private final double tol;
    private final double damping;
    private final int memChunkLimitRecords;

    public Config(
            Path edgesFile,
            int numNodes,
            Path outDir,
            int memMB,
            int maxIter,
            double tol,
            double damping,
            int memChunkLimitRecords
    ) {
        this.edgesFile = edgesFile;
        this.numNodes = numNodes;
        this.outDir = outDir;
        this.memMB = memMB;
        this.maxIter = maxIter;
        this.tol = tol;
        this.damping = damping;
        this.memChunkLimitRecords = memChunkLimitRecords;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Config config = (Config) o;
        return numNodes == config.numNodes
                && memMB == config.memMB
                && maxIter == config.maxIter
                && Double.compare(tol, config.tol) == 0
                && Double.compare(damping, config.damping) == 0
                && memChunkLimitRecords == config.memChunkLimitRecords
                && Objects.equals(edgesFile, config.edgesFile)
                && Objects.equals(outDir, config.outDir);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                edgesFile,
                numNodes,
                outDir,
                memMB,
                maxIter,
                tol,
                damping,
                memChunkLimitRecords);
    }


    @Override
    public String toString() {
        return "Builder{" +
                "edgesFile=" + edgesFile +
                ", numNodes=" + numNodes +
                ", outDir=" + outDir +
                ", memMB=" + memMB +
                ", maxIter=" + maxIter +
                ", tol=" + tol +
                ", damping=" + damping +
                ", memChunkLimitRecords=" + memChunkLimitRecords +
                '}';
    }

    public Path edgesFile() {
        return edgesFile;
    }

    public int numNodes() {
        return numNodes;
    }

    public Path outDir() {
        return outDir;
    }

    public int memMB() {
        return memMB;
    }

    public int maxIter() {
        return maxIter;
    }

    public double tol() {
        return tol;
    }

    public double damping() {
        return damping;
    }

    public int memChunkLimitRecords() {
        return memChunkLimitRecords;
    }

    public static final class Builder {

        private Path edgesFile = Path.of("edges.txt");
        private int numNodes = 10_000;
        private Path outDir = Path.of("prout");
        private int memMB = 128;
        private int maxIter = 50;
        private double tol = 1e-6;
        private double damping = 0.85;
        // About 24:
        // roughly estimate number of records per chunk: allocate half memory for contribution buffer
        // each record ~ 12 bytes (int + double) plus overhead -> conservatively 24 bytes
        private int memChunkLimitRecords = Math.max(1, (memMB * 1024 * 1024 / 2) / 24);

        public Builder edgesFile(Path edgesFile) {
            this.edgesFile = edgesFile;
            return this;
        }

        public Builder numNodes(int numNodes) {
            this.numNodes = numNodes;
            return this;
        }

        public Builder outDir(Path outDir) {
            this.outDir = outDir;
            return this;
        }

        public Builder memMB(int memMB) {
            this.memMB = memMB;
            return this;
        }

        public Builder maxIter(int maxIter) {
            this.maxIter = maxIter;
            return this;
        }

        public Builder tol(double tol) {
            this.tol = tol;
            return this;
        }

        public Builder damping(double damping) {
            this.damping = damping;
            return this;
        }

        public Builder memChunkLimitRecords(int memChunkLimitRecords) {
            this.memChunkLimitRecords = memChunkLimitRecords;
            return this;
        }

        public Config build() {
            return new Config(
                    edgesFile,
                    numNodes,
                    outDir,
                    memMB,
                    maxIter,
                    tol,
                    damping,
                    memChunkLimitRecords
            );
        }
    }
}
