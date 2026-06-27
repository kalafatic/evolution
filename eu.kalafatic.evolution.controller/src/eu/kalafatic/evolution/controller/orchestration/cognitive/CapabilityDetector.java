package eu.kalafatic.evolution.controller.orchestration.cognitive;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Interface for independent capability detectors.
 */
public interface CapabilityDetector {

    /**
     * Independently evaluates the prompt and returns a signal.
     */
    CapabilitySignal detect(String prompt);

}
