package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;

/**
 * The single authoritative transition engine for the evolution lifecycle.
 * Sole authority for phase transitions and valid transition graph.
 * Pure transition authority: NO heuristic scheduling logic.
 */
public class EvolutionPhaseMachine {

    public EvolutionPhase getInitialPhase() {
        return EvolutionPhase.INTENT_EXPANSION;
    }

    public EvolutionPhase next(EvolutionPhase current) {
        EvolutionPhase nextPhase;
        switch (current) {
            case INTENT_EXPANSION:
                nextPhase = EvolutionPhase.ARCHITECTURE_VARIANTS;
                break;
            case ARCHITECTURE_VARIANTS:
                nextPhase = EvolutionPhase.SELECTION_REFINEMENT;
                break;
            case SELECTION_REFINEMENT:
                nextPhase = EvolutionPhase.IMPLEMENTATION_PLAN;
                break;
            case IMPLEMENTATION_PLAN:
                nextPhase = EvolutionPhase.FINAL_SYNTHESIS;
                break;
            case FINAL_SYNTHESIS:
                nextPhase = EvolutionPhase.DESIGN_SATISFIED;
                break;
            case DESIGN_SATISFIED:
                nextPhase = EvolutionPhase.TERMINAL_SUCCESS;
                break;
            default:
                nextPhase = current;
                break;
        }
        validateTransition(current, nextPhase);
        return nextPhase;
    }

    public SelfDevDecision determineDecision(EvolutionPhase phase) {
        return isTerminal(phase) ? SelfDevDecision.STOP : SelfDevDecision.CONTINUE;
    }

    public void validateTransition(EvolutionPhase current, EvolutionPhase next) {
        if (current == next) return;
        if (isTerminal(current)) {
             throw new IllegalStateException("[PHASE_MACHINE] Cannot transition from terminal state: " + current);
        }

        // Allow jump to TERMINAL_FAILURE at any time.
        if (next == EvolutionPhase.TERMINAL_FAILURE) return;

        if (next.ordinal() < current.ordinal()) {
             throw new IllegalStateException("[PHASE_MACHINE] Illegal backward transition: " + current + " -> " + next);
        }
    }

    public boolean isTerminal(EvolutionPhase phase) {
        return phase == EvolutionPhase.TERMINAL_SUCCESS || phase == EvolutionPhase.TERMINAL_FAILURE;
    }

    /**
     * Compatibility bridge for existing string-based constants.
     */
    public static String toLegacyString(EvolutionPhase phase) {
        if (phase == null) return null;
        switch (phase) {
            case INTENT_EXPANSION: return EvolutionConstants.PHASE_INTENT_EXPANSION;
            case ARCHITECTURE_VARIANTS: return EvolutionConstants.PHASE_ARCHITECTURE_VARIANTS;
            case SELECTION_REFINEMENT: return EvolutionConstants.PHASE_SELECTION_REFINEMENT;
            case IMPLEMENTATION_PLAN: return EvolutionConstants.PHASE_IMPLEMENTATION_PLAN;
            case FINAL_SYNTHESIS: return EvolutionConstants.PHASE_FINAL_SYNTHESIS;
            default: return phase.name();
        }
    }
}
