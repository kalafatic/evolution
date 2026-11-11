package eu.kalafatic.evolution.controller.orchestration;

/**
 * Authoritative runtime states for EVO network servers and web applications.
 */
public enum ServerState {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING,
    FAILED;

    public boolean isRunning() {
        return this == RUNNING;
    }

    public boolean canStart() {
        return this == STOPPED || this == FAILED;
    }

    public boolean canStop() {
        return this == RUNNING || this == STARTING;
    }

    public boolean canRestart() {
        return this == RUNNING || this == STOPPED;
    }
}
