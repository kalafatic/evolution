package eu.kalafatic.evolution.controller.orchestration;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * A security token required to transition the system state.
 * Only components with access to create these (internal to the orchestration package)
 * can issue transitions.
 */
public final class TransitionToken {

    /**
     * Package-private constructor to enforce control plane authority.
     */
    TransitionToken() {
    }
}
