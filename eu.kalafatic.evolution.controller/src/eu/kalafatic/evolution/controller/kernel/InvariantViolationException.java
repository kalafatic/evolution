package eu.kalafatic.evolution.controller.kernel;

/**
 * Exception thrown when a runtime invariant is violated.
 */
public class InvariantViolationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvariantViolationException(String message) {
        super(message);
    }
}
