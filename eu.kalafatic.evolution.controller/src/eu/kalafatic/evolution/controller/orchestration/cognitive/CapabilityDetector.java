package eu.kalafatic.evolution.controller.orchestration.cognitive;

/**
 * Interface for independent capability detectors.
 */
public interface CapabilityDetector {

    /**
     * Independently evaluates the prompt and returns a signal.
     */
    CapabilitySignal detect(String prompt);

}
