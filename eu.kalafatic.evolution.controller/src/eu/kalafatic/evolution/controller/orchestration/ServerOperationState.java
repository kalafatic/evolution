package eu.kalafatic.evolution.controller.orchestration;

import java.util.EnumSet;
import java.util.Set;

/**
 * Explicit lifecycle states for asynchronous server operations:
 * CREATED, QUEUED, RUNNING, WAITING, CANCELLING, CANCELLED, COMPLETED, FAILED, TIMED_OUT.
 */
public enum ServerOperationState {
    CREATED,
    QUEUED,
    RUNNING,
    WAITING,
    CANCELLING,
    CANCELLED,
    COMPLETED,
    FAILED,
    TIMED_OUT;

    /**
     * Determines whether transitioning from this state to the target state is valid.
     * Prevents invalid transitions, especially from terminal states.
     */
    public boolean canTransitionTo(ServerOperationState target) {
        if (target == null) {
            return false;
        }
        if (this == target) {
            return true;
        }

        return switch (this) {
            case CREATED -> target == QUEUED || target == RUNNING || target == CANCELLING || target == CANCELLED;
            case QUEUED -> target == RUNNING || target == CANCELLING || target == CANCELLED || target == TIMED_OUT;
            case RUNNING -> target == WAITING || target == CANCELLING || target == CANCELLED || target == COMPLETED || target == FAILED || target == TIMED_OUT;
            case WAITING -> target == RUNNING || target == CANCELLING || target == CANCELLED || target == COMPLETED || target == FAILED || target == TIMED_OUT;
            case CANCELLING -> target == CANCELLED || target == COMPLETED || target == FAILED || target == TIMED_OUT;
            // Terminal states cannot transition to any other state
            case CANCELLED, COMPLETED, FAILED, TIMED_OUT -> false;
        };
    }

    /**
     * Checks if this state represents a terminal completion/failure state.
     */
    public boolean isTerminal() {
        return this == CANCELLED || this == COMPLETED || this == FAILED || this == TIMED_OUT;
    }

    /**
     * Checks if this state represents active running work.
     */
    public boolean isActive() {
        return this == QUEUED || this == RUNNING || this == WAITING || this == CANCELLING;
    }
}
