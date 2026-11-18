package eu.kalafatic.evolution.controller.teip.dto;

/**
 * Signals an unsupported TEIP major version in a request envelope (TEIP 1.1 §2).
 * Mapped to the canonical VERSION_MISMATCH error by the API controller.
 */
public class TeipVersionMismatchException extends IllegalArgumentException {

    public TeipVersionMismatchException(String message) {
        super(message);
    }
}
