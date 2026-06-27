package eu.kalafatic.evolution.controller.kernel;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;


/**
 * Interface for managing evolutionary phases.
 */
public interface PhaseEngine {
    EvolutionPhase getInitialPhase();
    EvolutionPhase next(EvolutionPhase current);
    boolean isTerminal(EvolutionPhase phase);
    String toLegacyString(EvolutionPhase phase);
}
