package goncharovmv.page_rank.single_node.pr.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class ConcurrentPageRankSettings {

    public static final ConcurrentPageRankSettings DEFAULT = new Builder()
            .build();

    private final double dampingFactor;
    private final int maxIterations;
    private final double convergenceThreshold;
    private final ExecutorService executorService;

    private ConcurrentPageRankSettings(
            double dampingFactor,
            int maxIterations,
            double convergenceThreshold,
            ExecutorService executorService
    ) {
        this.dampingFactor = dampingFactor;
        this.maxIterations = maxIterations;
        this.convergenceThreshold = convergenceThreshold;
        this.executorService = executorService;
    }

    public static Builder builder() {
        return new Builder();
    }

    public double getDampingFactor() {
        return dampingFactor;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public double getConvergenceThreshold() {
        return convergenceThreshold;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public static final class Builder {

        private static final double DEFAULT_DAMPING_FACTOR = 0.85;
        private static final int DEFAULT_MAX_ITERATIONS = 100;
        private static final double DEFAULT_CONVERGENCE_THRESHOLD = 1e-6;
        private static final Supplier<? extends ExecutorService> DEFAULT_EXECUTOR_SERVICE_SUPPLIER = () -> Executors.newWorkStealingPool(
                Runtime.getRuntime().availableProcessors()
        );

        private double dampingFactor = DEFAULT_DAMPING_FACTOR;
        private int maxIterations = DEFAULT_MAX_ITERATIONS;
        private double convergenceThreshold = DEFAULT_CONVERGENCE_THRESHOLD;
        private ExecutorService executorService = DEFAULT_EXECUTOR_SERVICE_SUPPLIER.get();

        public Builder dumpingFactor(double dampingFactor) {
            this.dampingFactor = dampingFactor;
            return this;
        }

        public Builder maxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        public Builder convergenceThreshold(double convergenceThreshold) {
            this.convergenceThreshold = convergenceThreshold;
            return this;
        }

        public Builder executorService(Supplier<? extends ExecutorService> executorServiceSupplier) {
            this.executorService = executorServiceSupplier.get();
            return this;
        }

        public ConcurrentPageRankSettings build() {
            return new ConcurrentPageRankSettings(
                    this.dampingFactor,
                    this.maxIterations,
                    this.convergenceThreshold,
                    this.executorService
            );
        }
    }
}
