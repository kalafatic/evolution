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

        // STRICT SEQUENTIAL PROGRESSION FOR FIRST 4 GENERATIONS
        // This ensures mandatory evolutionary depth and prevents premature convergence.
        if (generation < 4 && !isTerminal(current)) {
            switch (current) {
                case INTENT_EXPANSION: return EvolutionPhase.ARCHITECTURE_VARIANTS;
                case ARCHITECTURE_VARIANTS: return EvolutionPhase.SELECTION_REFINEMENT;
                case SELECTION_REFINEMENT: return EvolutionPhase.IMPLEMENTATION_PLAN;
                case IMPLEMENTATION_PLAN: return EvolutionPhase.FINAL_SYNTHESIS;
                default: break;
            }
        }

        // After generation 4, we allow convergence or manual shortcuts if requested
        if (!converged && (current == EvolutionPhase.ARCHITECTURE_VARIANTS || current == EvolutionPhase.SELECTION_REFINEMENT)) {
            // Stay in the same phase to allow multi-generation trajectory evolution if not converged
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
