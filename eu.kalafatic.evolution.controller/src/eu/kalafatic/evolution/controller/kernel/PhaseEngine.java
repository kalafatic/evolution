package eu.kalafatic.evolution.controller.kernel;

import eu.kalafatic.evolution.controller.orchestration.EvolutionPhase;

/**
 * Interface for managing evolutionary phases.
 */
public interface PhaseEngine {
    EvolutionPhase getInitialPhase();
    EvolutionPhase next(EvolutionPhase current);
    boolean isTerminal(EvolutionPhase phase);
    String toLegacyString(EvolutionPhase phase);
}
