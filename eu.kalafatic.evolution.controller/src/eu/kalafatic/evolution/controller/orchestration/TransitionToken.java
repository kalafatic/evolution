package eu.kalafatic.evolution.controller.orchestration;

/**
 * A security token required to transition the system state.
 * Only components with access to create these (internal to the orchestration package)
 * can issue transitions.
 */
public final class TransitionToken {

    private String id;

    /**
     * Public constructor to support token-based transitions.
     */
    public TransitionToken() {
    }

    /**
     * Public constructor with identifier to support token-based transitions with context.
     */
    public TransitionToken(String id) {
        this.id = id;
    }

    /**
     * Gets the token identifier.
     */
    public String getId() {
        return id;
    }
}
