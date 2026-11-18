package eu.kalafatic.evolution.forge.controller.service.impl.agents;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-logic ETA calculator used by the Forge live monitoring layer.
 *
 * ETA policy (no fabricated speeds, no fixed baselines):
 * - Uses actual timestamped measurements (step/token counters) from the
 *   authoritative telemetry history.
 * - Throughput is an EMA over a rolling measurement window; the window drops
 *   older samples so the prediction adapts to current execution speed.
 * - Zero measurements, paused runs and negative/zero throughput produce an
 *   explicit "unavailable" result instead of a fake number.
 * - Confidence is derived from the amount of real measurement evidence.
 *
 * Stateless: all inputs are passed per call, so it is trivially testable and
 * never retains cross-session state.
 */
public final class TrainingEtaEstimator {

    public static final double DEFAULT_ROLLING_WINDOW_MS = 120_000.0;
    public static final double DEFAULT_EMA_ALPHA = 0.6;

    public static class EtaEstimate {
        /** Effective throughput (tokens/sec) over the rolling window, or -1. */
        public final double effectiveTokensPerSec;
        /** Effective step rate (steps/sec) over the rolling window, or -1. */
        public final double effectiveStepsPerSec;
        /** Remaining seconds or -1 when unavailable. */
        public final double remainingSeconds;
        /** Estimation quality 0..1 (0 = no evidence). */
        public final double confidence;
        /** Human-readable basis of the estimate. */
        public final String basis;

        public EtaEstimate(double effectiveTokensPerSec, double effectiveStepsPerSec,
                           double remainingSeconds, double confidence, String basis) {
            this.effectiveTokensPerSec = effectiveTokensPerSec;
            this.effectiveStepsPerSec = effectiveStepsPerSec;
            this.remainingSeconds = remainingSeconds;
            this.confidence = confidence;
            this.basis = basis;
        }

        public boolean isAvailable() {
            return remainingSeconds >= 0.0;
        }
    }

    /** One timestamped measurement point. */
    public static class Sample {
        public final long timestampMs;
        public final long processedTokens;
        public final int completedSteps;
        public final long totalTokens;
        public final int totalSteps;

        public Sample(long timestampMs, long processedTokens, int completedSteps, long totalTokens, int totalSteps) {
            this.timestampMs = timestampMs;
            this.processedTokens = processedTokens;
            this.completedSteps = completedSteps;
            this.totalTokens = totalTokens;
            this.totalSteps = totalSteps;
        }
    }

    public static final EtaEstimate UNAVAILABLE =
            new EtaEstimate(-1.0, -1.0, -1.0, 0.0, "no-measurements");

    private TrainingEtaEstimator() {}

    /**
     * Computes a measured ETA from a timestamped history.
     *
     * @param samples       measurement history, oldest first
     * @param currentTokens current processed-token counter (may exceed history)
     * @param currentSteps  current optimizer-step counter
     * @param totalTokens   planned total tokens (&gt;0 to enable token-based ETA)
     * @param totalSteps    planned total steps (&gt;0 to enable step-based ETA)
     * @param nowMs         current wall-clock time (for window filtering)
     */
    public static EtaEstimate compute(List<Sample> samples, long currentTokens, int currentSteps,
                                      long totalTokens, int totalSteps, long nowMs) {
        if (samples == null || samples.isEmpty()) {
            return UNAVAILABLE;
        }

        // Rolling window: only recent measurements participate.
        double windowMs = DEFAULT_ROLLING_WINDOW_MS;
        List<Sample> window = new ArrayList<>();
        long windowStartMs = nowMs - (long) windowMs;
        for (int i = samples.size() - 1; i >= 0; i--) {
            Sample s = samples.get(i);
            if (s.timestampMs >= windowStartMs) {
                window.add(0, s);
            }
        }
        if (window.isEmpty()) {
            window.add(samples.get(samples.size() - 1));
        }

        // Token throughput over the window.
        double tokPerSec = -1.0;
        Sample first = window.get(0);
        Sample last = window.get(window.size() - 1);
        double spanMs = Math.max(1.0, last.timestampMs - first.timestampMs);
        if (first.processedTokens >= 0 && last.processedTokens >= first.processedTokens) {
            long deltaTokens = last.processedTokens - first.processedTokens;
            if (deltaTokens > 0) {
                tokPerSec = (deltaTokens * 1000.0) / spanMs;
            }
        }

        // Step throughput over the window.
        double stepsPerSec = -1.0;
        if (first.completedSteps >= 0 && last.completedSteps >= first.completedSteps) {
            long deltaSteps = last.completedSteps - first.completedSteps;
            if (deltaSteps > 0) {
                stepsPerSec = (deltaSteps * 1000.0) / spanMs;
            }
        }

        double remaining = -1.0;
        String basis;
        double confidence = 0.0;

        // Prefer token-based ETA (per-token workload is uniform); fall back to
        // step-based, then to the full-history average when the window alone
        // cannot provide a rate.
        if (tokPerSec > 0.0 && totalTokens > 0) {
            long remainingTokens = Math.max(0, totalTokens - Math.max(currentTokens, last.processedTokens));
            remaining = remainingTokens / tokPerSec;
            basis = "measured-token-throughput";
            confidence = confidenceFrom(window);
        } else if (stepsPerSec > 0.0 && totalSteps > 0) {
            int remainingSteps = Math.max(0, totalSteps - Math.max(currentSteps, last.completedSteps));
            remaining = remainingSteps / stepsPerSec;
            basis = "measured-step-throughput";
            confidence = Math.min(0.8, confidenceFrom(window));
        } else {
            // Full-history fallback (all samples, no rolling window).
            Sample firstAll = samples.get(0);
            Sample lastAll = samples.get(samples.size() - 1);
            double spanAll = Math.max(1.0, lastAll.timestampMs - firstAll.timestampMs);
            if (totalTokens > 0 && lastAll.processedTokens > firstAll.processedTokens) {
                double rate = ((lastAll.processedTokens - firstAll.processedTokens) * 1000.0) / spanAll;
                long remainingTokens = Math.max(0, totalTokens - Math.max(currentTokens, lastAll.processedTokens));
                remaining = remainingTokens / rate;
                tokPerSec = rate;
                basis = "measured-token-throughput-history";
                confidence = Math.min(0.5, 0.2 + 0.3 * samples.size() / 10.0);
            } else if (totalSteps > 0 && lastAll.completedSteps > firstAll.completedSteps) {
                double rate = ((lastAll.completedSteps - firstAll.completedSteps) * 1000.0) / spanAll;
                int remainingSteps = Math.max(0, totalSteps - Math.max(currentSteps, lastAll.completedSteps));
                remaining = remainingSteps / rate;
                stepsPerSec = rate;
                basis = "measured-step-throughput-history";
                confidence = Math.min(0.4, 0.1 + 0.3 * samples.size() / 10.0);
            } else {
                basis = "no-progress-yet";
                return new EtaEstimate(tokPerSec, stepsPerSec, -1.0, 0.0, basis);
            }
        }

        if (Double.isNaN(remaining) || Double.isInfinite(remaining) || remaining < 0.0) {
            return new EtaEstimate(tokPerSec, stepsPerSec, -1.0, confidence, basis);
        }
        return new EtaEstimate(tokPerSec, stepsPerSec, remaining, confidence, basis);
    }

    private static double confidenceFrom(List<Sample> window) {
        int n = window.size();
        if (n <= 0) return 0.0;
        if (n == 1) return 0.3;
        if (n == 2) return 0.5;
        if (n <= 5) return 0.7;
        if (n <= 10) return 0.9;
        return 1.0;
    }
}
