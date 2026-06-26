package eu.kalafatic.evolution.controller.orchestration;

/**
 * A security token required to transition the system state.
 * Only components with access to create these (internal to the orchestration package)
 * can issue transitions.
 */
public final class TransitionToken {

    /**
     * Public constructor to allow service-based state management.
     */
    public TransitionToken() {
    }
}
