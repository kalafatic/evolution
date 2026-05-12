package eu.kalafatic.evolution.controller.orchestration.capability;

public class CapabilityException extends Exception {
    public CapabilityException(String message) {
        super(message);
    }
    public CapabilityException(String message, Throwable cause) {
        super(message, cause);
    }
}
