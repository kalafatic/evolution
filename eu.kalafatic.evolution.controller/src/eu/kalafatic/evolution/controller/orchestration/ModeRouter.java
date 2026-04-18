package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Routes execution based on detected or assigned PlatformMode.
 */
public class ModeRouter {

    /**
     * Detects or assigns PlatformMode based on user input and orchestrator state.
     */
    public PlatformMode route(String prompt, Orchestrator orchestrator) {
        if (prompt == null) prompt = "";
        String lowerPrompt = prompt.toLowerCase();

        // 1. Explicit user selection (overrides everything)
        if (lowerPrompt.contains("mode: chat")) {
            return createSimpleChatMode();
        } else if (lowerPrompt.contains("mode: assisted")) {
            return createAssistedCodingMode();
        } else if (lowerPrompt.contains("mode: darwin")) {
            return createDarwinMode();
        } else if (lowerPrompt.contains("mode: self-dev")) {
            return createSelfDevMode();
        }

        // 2. Map from existing model flags
        if (orchestrator != null) {
            if (orchestrator.isDarwinMode()) {
                return createDarwinMode();
            }
            if (orchestrator.isSelfIterativeMode()) {
                return createSelfDevMode();
            }
            if (orchestrator.isIterativeMode()) {
                return createAssistedCodingMode();
            }
        }

        // 3. Default to SIMPLE_CHAT if unclear
        return createSimpleChatMode();
    }

    private PlatformMode createSimpleChatMode() {
        return new PlatformMode(PlatformType.SIMPLE_CHAT, AutonomyLevel.LOW, 1, false);
    }

    private PlatformMode createAssistedCodingMode() {
        return new PlatformMode(PlatformType.ASSISTED_CODING, AutonomyLevel.LOW, 2, false);
    }

    private PlatformMode createDarwinMode() {
        return new PlatformMode(PlatformType.DARWIN_MODE, AutonomyLevel.MEDIUM, 3, false);
    }

    private PlatformMode createSelfDevMode() {
        PlatformMode mode = new PlatformMode(PlatformType.SELF_DEV_MODE, AutonomyLevel.HIGH, 5, true);
        // Restrict self-modification to a defined set of modules/directories for safety.
        // For Evo, we allow most things but can exclude core infrastructure if needed.
        mode.getAllowedPaths().add("eu.kalafatic.evolution.controller/src");
        mode.getAllowedPaths().add("eu.kalafatic.evolution.view/src");
        mode.getAllowedPaths().add("eu.kalafatic.evolution.model/src");
        return mode;
    }
}
