package eu.kalafatic.evolution.controller.kernel;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Exception thrown when a runtime invariant is violated.
 */
public class InvariantViolationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvariantViolationException(String message) {
        super(message);
    }
}
