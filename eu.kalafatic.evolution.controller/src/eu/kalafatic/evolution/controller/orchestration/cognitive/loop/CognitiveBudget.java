package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

/**
 * Multi-dimensional execution budget constraints for the Control Loop.
 */
public class CognitiveBudget {

    private final int maxIterations;
    private final long maxWallClockTimeMs;
    private final int maxDarwinInvocations;
    private final int maxConsecutiveNoProgress;
    private final int maxRetries;

    public CognitiveBudget() {
        this(15, 600000L, 3, 3, 2); // Default: 15 iterations, 10 min, 3 Darwin runs, 3 no-progress steps, 2 retries
    }

    public CognitiveBudget(int maxIterations, long maxWallClockTimeMs, int maxDarwinInvocations, int maxConsecutiveNoProgress) {
        this(maxIterations, maxWallClockTimeMs, maxDarwinInvocations, maxConsecutiveNoProgress, 2);
    }

    public CognitiveBudget(int maxIterations, long maxWallClockTimeMs, int maxDarwinInvocations, int maxConsecutiveNoProgress, int maxRetries) {
        this.maxIterations = maxIterations;
        this.maxWallClockTimeMs = maxWallClockTimeMs;
        this.maxDarwinInvocations = maxDarwinInvocations;
        this.maxConsecutiveNoProgress = maxConsecutiveNoProgress;
        this.maxRetries = maxRetries;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public long getMaxWallClockTimeMs() {
        return maxWallClockTimeMs;
    }

    public int getMaxDarwinInvocations() {
        return maxDarwinInvocations;
    }

    public int getMaxConsecutiveNoProgress() {
        return maxConsecutiveNoProgress;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public boolean isExhausted(int currentIteration, long elapsedTimeMs, int currentDarwinInvocations, int consecutiveNoProgress) {
        if (currentIteration >= maxIterations) return true;
        if (elapsedTimeMs >= maxWallClockTimeMs) return true;
        if (currentDarwinInvocations >= maxDarwinInvocations) return true;
        if (consecutiveNoProgress >= maxConsecutiveNoProgress) return true;
        return false;
    }

    @Override
    public String toString() {
        return "CognitiveBudget{" +
                "maxIterations=" + maxIterations +
                ", maxWallClockTimeMs=" + maxWallClockTimeMs +
                ", maxDarwinInvocations=" + maxDarwinInvocations +
                ", maxConsecutiveNoProgress=" + maxConsecutiveNoProgress +
                ", maxRetries=" + maxRetries +
                '}';
    }
}
