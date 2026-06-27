package eu.kalafatic.evolution.controller.orchestration;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.kernel.EvolutionProfile;
import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;

/**
 * The single authoritative transition engine for the evolution lifecycle.
 * Sole authority for phase transitions and valid transition graph.
 * Pure transition authority: NO heuristic scheduling logic.
 */
public class EvolutionPhaseMachine {

    public eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase getInitialPhase() {
        return EvolutionPhase.INTENT_EXPANSION;
    }

    public eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase next(eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase current) {
        return next(current, null);
    }

    public eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase next(eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase current, EvolutionProfile profile) {
        if (profile != null && profile.getCapability() == CapabilityType.CHAT) {
            if (current == EvolutionPhase.INTENT_EXPANSION) {
                return EvolutionPhase.FINAL_SYNTHESIS;
            } else if (current == EvolutionPhase.FINAL_SYNTHESIS) {
                return EvolutionPhase.TERMINAL_SUCCESS;
            }
        }

        eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase nextPhase;
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

    public SelfDevDecision determineDecision(eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase phase) {
        return isTerminal(phase) ? SelfDevDecision.STOP : SelfDevDecision.CONTINUE;
    }

    public void validateTransition(eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase current, eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase next) {
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

    public boolean isTerminal(eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase phase) {
        return phase == EvolutionPhase.TERMINAL_SUCCESS || phase == EvolutionPhase.TERMINAL_FAILURE;
    }

    /**
     * Compatibility bridge for existing string-based constants.
     */
    public static String toLegacyString(eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase phase) {
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
