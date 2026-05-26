package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;

/**
 * Manages transitions between evolution phases.
 */
public class EvolutionPhaseMachine {

    public EvolutionPhase getInitialPhase() {
        return EvolutionPhase.INTENT_EXPANSION;
    }

    public EvolutionPhase next(EvolutionPhase current) {
        return next(current, false, 0);
    }

    public EvolutionPhase next(EvolutionPhase current, boolean converged, int generation) {
        // MANDATORY EVOLUTIONARY FLOW (First 4 Generations)
        // Iteration 0 (Gen 0) -> ARCHITECTURE_VARIANTS (Explore philosophies)
        // Iteration 1 (Gen 1) -> SELECTION_REFINEMENT (Mutate philosophy)
        // Iteration 2 (Gen 2) -> IMPLEMENTATION_PLAN (Refine execution strategy)
        // Iteration 3 (Gen 3) -> FINAL_SYNTHESIS (Execution and stabilization)

        if (generation == 0 && current == EvolutionPhase.INTENT_EXPANSION) return EvolutionPhase.ARCHITECTURE_VARIANTS;
        if (generation == 1 && current == EvolutionPhase.ARCHITECTURE_VARIANTS) return EvolutionPhase.SELECTION_REFINEMENT;
        if (generation == 2 && current == EvolutionPhase.SELECTION_REFINEMENT) return EvolutionPhase.IMPLEMENTATION_PLAN;
        if (generation == 3 && current == EvolutionPhase.IMPLEMENTATION_PLAN) return EvolutionPhase.FINAL_SYNTHESIS;

        // Enforce minimum evolutionary depth (at least until generation 4)
        if (generation < 4 && !isTerminal(current)) {
            // Force progression if we are not at the expected phase for the current generation
            if (generation == 1 && current == EvolutionPhase.INTENT_EXPANSION) return EvolutionPhase.ARCHITECTURE_VARIANTS;
            if (generation == 2 && (current == EvolutionPhase.INTENT_EXPANSION || current == EvolutionPhase.ARCHITECTURE_VARIANTS)) return EvolutionPhase.SELECTION_REFINEMENT;

            // If already in the target phase for the generation, stay there to satisfy multi-gen requirement if not converged
            return current;
        }

        if (!converged && (current == EvolutionPhase.ARCHITECTURE_VARIANTS || current == EvolutionPhase.SELECTION_REFINEMENT)) {
            // Stay in the same phase to allow multi-generation trajectory evolution
            return current;
        }

        switch (current) {
            case INTENT_EXPANSION:
                return EvolutionPhase.ARCHITECTURE_VARIANTS;
            case ARCHITECTURE_VARIANTS:
                return EvolutionPhase.SELECTION_REFINEMENT;
            case SELECTION_REFINEMENT:
                return EvolutionPhase.IMPLEMENTATION_PLAN;
            case IMPLEMENTATION_PLAN:
                return EvolutionPhase.FINAL_SYNTHESIS;
            case FINAL_SYNTHESIS:
                return EvolutionPhase.TERMINAL_SUCCESS;
            default:
                return current;
        }
    }

    public boolean isTerminal(EvolutionPhase phase) {
        return phase == EvolutionPhase.TERMINAL_SUCCESS || phase == EvolutionPhase.TERMINAL_FAILURE;
    }

    /**
     * Compatibility bridge for existing string-based constants.
     */
    public static String toLegacyString(EvolutionPhase phase) {
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
