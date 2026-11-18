package eu.kalafatic.evolution.controller.orchestration.selfdev;

/**
 * WP6 - explicit RCP readiness levels.
 *
 * Ordered from weakest to strongest evidence. A start is successful only when
 * the configured minimum level has actually been verified; a live process is
 * never treated as a ready application.
 */
public enum RcpReadinessLevel {
    PROCESS_STARTED(0),
    PROCESS_ALIVE(1),
    HTTP_RESPONSIVE(2),
    SUPERVISOR_RESPONSIVE(3),
    OSGI_RESOLVED(4),
    REQUIRED_BUNDLES_ACTIVE(5),
    APPLICATION_INITIALIZED(6),
    APPLICATION_HEALTHY(7);

    private final int rank;

    RcpReadinessLevel(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public boolean atLeast(RcpReadinessLevel other) {
        return this.rank >= other.rank;
    }

    public static RcpReadinessLevel max(RcpReadinessLevel a, RcpReadinessLevel b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.rank >= b.rank ? a : b;
    }
}
