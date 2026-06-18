package eu.kalafatic.evolution.controller.orchestration;

/**
 * Formal states of the Darwin evolutionary lifecycle.
 */
public enum EvolutionPhase {
    INTENT_EXPANSION,
    ARCHITECTURE_VARIANTS,
    SELECTION_REFINEMENT,
    IMPLEMENTATION_PLAN,
    FINAL_SYNTHESIS,
    DESIGN_SATISFIED,
    TERMINAL_SUCCESS,
    TERMINAL_FAILURE;

    public static EvolutionPhase fromString(String phase) {
        if (phase == null) return INTENT_EXPANSION;
        try {
            return EvolutionPhase.valueOf(phase);
        } catch (IllegalArgumentException e) {
            // Backward compatibility adapter
            switch (phase) {
                case "INTENT_EXPANSION": return INTENT_EXPANSION;
                case "ARCHITECTURE_VARIANTS": return ARCHITECTURE_VARIANTS;
                case "SELECTION_REFINEMENT": return SELECTION_REFINEMENT;
                case "IMPLEMENTATION_PLAN": return IMPLEMENTATION_PLAN;
                case "FINAL_SYNTHESIS": return FINAL_SYNTHESIS;
                default: return INTENT_EXPANSION;
            }
        }
    }
}
