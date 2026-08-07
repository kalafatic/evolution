package eu.kalafatic.evolution.controller.orchestration.selfdev;

/**
 * Custom unchecked exception class to cleanly signal execution pausing for approval.
 */
public class DarwinWaitException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DarwinWaitException() {
        super("Darwin execution paused waiting for user approval.");
    }

    public DarwinWaitException(String message) {
        super(message);
    }
}
