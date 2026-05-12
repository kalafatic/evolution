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
}
