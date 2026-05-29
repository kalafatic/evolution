package eu.kalafatic.evolution.controller.orchestration;

import java.time.Duration;
import java.time.Instant;

/**
 * Capture execution timing metrics.
 */
public class ExecutionMetrics {
    private final Instant startedAt;
    private final Instant finishedAt;
    private final Duration totalDuration;

    private int repairAttempts = 0;
    private int repairSuccesses = 0;
    private int validationFailures = 0;

    public ExecutionMetrics(Instant startedAt, Instant finishedAt) {
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.totalDuration = Duration.between(startedAt, finishedAt);
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Duration getTotalDuration() {
        return totalDuration;
    }

    public String formatDuration() {
        long seconds = totalDuration.getSeconds();
        long millis = totalDuration.toMillisPart();
        return seconds + "." + (millis / 100) + "s";
    }

    public int getRepairAttempts() { return repairAttempts; }
    public void incrementRepairAttempts() { this.repairAttempts++; }

    public int getRepairSuccesses() { return repairSuccesses; }
    public void incrementRepairSuccesses() { this.repairSuccesses++; }

    public int getValidationFailures() { return validationFailures; }
    public void incrementValidationFailures() { this.validationFailures++; }
}
