package eu.kalafatic.evolution.forge.data.api.source;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Context object providing configuration, logging, cancellation, and metrics during dataset preparation.
 */
public class DatasetPreparationContext {

    private long targetUsableBytes = 524_288_000L; // Default 500 MB
    private String language = "en";
    private BooleanSupplier cancellationSupplier = () -> false;
    private Consumer<String> logger = System.out::println;
    private DatasetSourceConfig sourceConfig;
    private DatasetSourceStats stats = new DatasetSourceStats();

    public DatasetPreparationContext() {}

    public DatasetPreparationContext(long targetUsableBytes, Consumer<String> logger) {
        if (targetUsableBytes >= 0) {
            this.targetUsableBytes = targetUsableBytes;
        }
        if (logger != null) {
            this.logger = logger;
        }
    }

    public boolean isCancelled() {
        return cancellationSupplier != null && cancellationSupplier.getAsBoolean();
    }

    public void log(String message) {
        if (logger != null && message != null) {
            logger.accept(message);
        }
    }

    public long getTargetUsableBytes() {
        return targetUsableBytes;
    }

    public void setTargetUsableBytes(long targetUsableBytes) {
        this.targetUsableBytes = targetUsableBytes;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public BooleanSupplier getCancellationSupplier() {
        return cancellationSupplier;
    }

    public void setCancellationSupplier(BooleanSupplier cancellationSupplier) {
        this.cancellationSupplier = cancellationSupplier;
    }

    public Consumer<String> getLogger() {
        return logger;
    }

    public void setLogger(Consumer<String> logger) {
        this.logger = logger;
    }

    public DatasetSourceConfig getSourceConfig() {
        return sourceConfig;
    }

    public void setSourceConfig(DatasetSourceConfig sourceConfig) {
        this.sourceConfig = sourceConfig;
    }

    public DatasetSourceStats getStats() {
        return stats;
    }

    public void setStats(DatasetSourceStats stats) {
        if (stats != null) {
            this.stats = stats;
        }
    }
}
