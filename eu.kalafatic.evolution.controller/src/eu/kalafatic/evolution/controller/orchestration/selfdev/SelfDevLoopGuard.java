package eu.kalafatic.evolution.controller.orchestration.selfdev;

/**
 * WP8 - bounded self-dev loop guard.
 *
 * Enforces explicit termination conditions for the autonomous loop:
 * maximum iterations, maximum wall-clock duration, repeated identical
 * failures, no-change iterations, stalls and explicit cancellation.
 * Every termination carries an explicit reason. A non-positive maxIterations
 * resolves to the DEFAULT bound - never an unbounded loop (D-56).
 */
public class SelfDevLoopGuard {

    public static final int DEFAULT_MAX_ITERATIONS = 5;
    public static final long DEFAULT_MAX_WALL_CLOCK_MS = 30 * 60 * 1000L;
    public static final int DEFAULT_REPEATED_FAILURE_LIMIT = 3;
    public static final int DEFAULT_NO_CHANGE_LIMIT = 2;
    public static final long DEFAULT_STALL_TIMEOUT_MS = 10 * 60 * 1000L;

    public enum TerminationReason {
        NONE(null),
        MAX_ITERATIONS_REACHED("Maximum iterations reached"),
        WALL_CLOCK_EXCEEDED("Maximum wall-clock duration exceeded"),
        REPEATED_IDENTICAL_FAILURES("Repeated identical failures"),
        NO_CHANGE_ITERATIONS("No-change iterations detected"),
        STALLED("Stall detected: no progress within the stall timeout"),
        CANCELLED("Cancellation requested"),
        TERMINAL_STAGE("Terminal stage reached (ACCEPTED/REJECTED)");

        private final String description;

        TerminationReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final int maxIterations;
    private final long maxWallClockMs;
    private final int repeatedFailureLimit;
    private final int noChangeLimit;
    private final long stallTimeoutMs;
    private final long startTimeMs;

    private volatile boolean cancelled;

    private int completedIterations;
    private String lastFailureFingerprint;
    private int consecutiveIdenticalFailures;
    private int consecutiveNoChangeIterations;
    private long lastProgressAt;

    public SelfDevLoopGuard(int maxIterations, long maxWallClockMs, int repeatedFailureLimit,
                            int noChangeLimit, long stallTimeoutMs) {
        this.maxIterations = maxIterations > 0 ? maxIterations : DEFAULT_MAX_ITERATIONS;
        this.maxWallClockMs = maxWallClockMs > 0 ? maxWallClockMs : DEFAULT_MAX_WALL_CLOCK_MS;
        this.repeatedFailureLimit = repeatedFailureLimit > 0 ? repeatedFailureLimit : DEFAULT_REPEATED_FAILURE_LIMIT;
        this.noChangeLimit = noChangeLimit > 0 ? noChangeLimit : DEFAULT_NO_CHANGE_LIMIT;
        this.stallTimeoutMs = stallTimeoutMs > 0 ? stallTimeoutMs : DEFAULT_STALL_TIMEOUT_MS;
        this.startTimeMs = System.currentTimeMillis();
        this.lastProgressAt = startTimeMs;
    }

    public static SelfDevLoopGuard defaults() {
        return new SelfDevLoopGuard(-1, -1, -1, -1, -1);
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public synchronized void recordIterationOutcome(boolean success, String failureFingerprint, boolean changed) {
        completedIterations++;
        lastProgressAt = System.currentTimeMillis();

        if (success) {
            consecutiveIdenticalFailures = 0;
            lastFailureFingerprint = null;
        }
        if (!changed) {
            consecutiveNoChangeIterations++;
        } else {
            consecutiveNoChangeIterations = 0;
        }
        if (!success && failureFingerprint != null) {
            if (failureFingerprint.equals(lastFailureFingerprint)) {
                consecutiveIdenticalFailures++;
            } else {
                consecutiveIdenticalFailures = 1;
                lastFailureFingerprint = failureFingerprint;
            }
        }
    }

    public synchronized TerminationReason evaluate(boolean terminalStageReached) {
        if (cancelled) {
            return TerminationReason.CANCELLED;
        }
        if (terminalStageReached) {
            return TerminationReason.TERMINAL_STAGE;
        }
        if (completedIterations >= maxIterations) {
            return TerminationReason.MAX_ITERATIONS_REACHED;
        }
        if (System.currentTimeMillis() - startTimeMs > maxWallClockMs) {
            return TerminationReason.WALL_CLOCK_EXCEEDED;
        }
        if (consecutiveIdenticalFailures >= repeatedFailureLimit) {
            return TerminationReason.REPEATED_IDENTICAL_FAILURES;
        }
        if (consecutiveNoChangeIterations >= noChangeLimit) {
            return TerminationReason.NO_CHANGE_ITERATIONS;
        }
        if (System.currentTimeMillis() - lastProgressAt > stallTimeoutMs) {
            return TerminationReason.STALLED;
        }
        return TerminationReason.NONE;
    }

    public int getCompletedIterations() {
        return completedIterations;
    }

    public int getMaxIterations() {
        return maxIterations;
    }
}
