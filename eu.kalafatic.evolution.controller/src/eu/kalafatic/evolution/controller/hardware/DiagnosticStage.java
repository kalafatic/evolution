package eu.kalafatic.evolution.controller.hardware;

import java.time.Instant;

/**
 * One stage of a {@link CudaDiagnosticReport}. Stages are recorded in
 * execution order with explicit timestamps, durations and details so the UI
 * never has to parse log text to determine what happened.
 */
public final class DiagnosticStage {

    public final String id;
    public final String name;
    public volatile StageStatus status = StageStatus.SKIPPED;
    public volatile long durationMs;
    public volatile String detail = "";
    public volatile Instant startedAt;
    public volatile Instant finishedAt;

    public DiagnosticStage(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public boolean passed() {
        return status == StageStatus.PASS;
    }

    public boolean executed() {
        return status == StageStatus.PASS || status == StageStatus.FAILED;
    }

    @Override
    public String toString() {
        return String.format("%-34s %-12s %6d ms  %s", name, status, durationMs,
                detail == null || detail.isEmpty() ? "" : detail);
    }
}
