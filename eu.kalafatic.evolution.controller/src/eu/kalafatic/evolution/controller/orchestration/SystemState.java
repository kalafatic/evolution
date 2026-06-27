package eu.kalafatic.evolution.controller.orchestration;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Hardened system states for the Evolutionary OS Kernel.
 */
public enum SystemState {
    INIT,
    ANALYZING,
    PLAN_LOCKED,
    EXECUTING,
    VERIFYING,
    CLARIFYING,
    MUTATING,
    EXPORTING,
    AWAITING_BRANCH_SELECTION,
    DONE,
    FAILED,
    RECOVERING
}
