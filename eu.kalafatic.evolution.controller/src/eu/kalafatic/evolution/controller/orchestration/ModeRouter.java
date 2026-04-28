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
        return route(prompt, orchestrator, null);
    }

    /**
     * Attempts to determine the mode using fast, rule-based logic without an LLM.
     * @return PlatformMode if determined, null otherwise.
     */
    public PlatformMode routeFast(String prompt, Orchestrator orchestrator) {
        if (prompt == null) prompt = "";
        String lowerPrompt = prompt.toLowerCase();

        // 1. Explicit mode keywords
        if (lowerPrompt.contains("mode: chat")) return createSimpleChatMode();
        if (lowerPrompt.contains("mode: assisted")) return createAssistedCodingMode();
        if (lowerPrompt.contains("mode: darwin")) return createDarwinMode();
        if (lowerPrompt.contains("mode: self-dev")) return createSelfDevMode();

        // 2. Map from existing model flags
        if (orchestrator != null) {
            if (orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null) {
                if (orchestrator.getAiChat().getPromptInstructions().isSelfIterativeMode()) {
                    return createSelfDevMode();
                }
            }
            if (orchestrator.isDarwinMode()) {
                return createDarwinMode();
            }
            if (orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null) {
                if (orchestrator.getAiChat().getPromptInstructions().isIterativeMode()) {
                    return createAssistedCodingMode();
                }
            }
        }

        // 3. Obvious coding keywords detection
        if (lowerPrompt.matches(".*\\b(create|fix|add|run|test|generate|write|refactor|modify|delete|check|implement|build)\\b.*")) {
            return createAssistedCodingMode();
        }

        // 4. Greetings and simple chat detection
        if (lowerPrompt.matches("^\\s*(hi|hello|hey|greetings|good morning|good afternoon|good evening)\\s*[!.]*\\s*$")) {
            return createSimpleChatMode();
        }

        return null; // Not fast-routable
    }

    /**
     * Detects or assigns PlatformMode based on user input, orchestrator state, and optional context assist result.
     */
    public PlatformMode route(String prompt, Orchestrator orchestrator, ContextAssistResult assistResult) {
        if (prompt == null) prompt = "";
        String lowerPrompt = prompt.toLowerCase();

        // 1. Try fast routing first
        PlatformMode fastMode = routeFast(prompt, orchestrator);
        if (fastMode != null) return fastMode;

        // 1b. High confidence context assist result
        if (assistResult != null && assistResult.getConfidence() == ConfidenceLevel.HIGH) {
            switch (assistResult.getMode()) {
                case SIMPLE_CHAT: return createSimpleChatMode();
                case ASSISTED_CODING: return createAssistedCodingMode();
                case DARWIN_MODE: return createDarwinMode();
                case SELF_DEV_MODE: return createSelfDevMode();
            }
        }

        // 2. Default to SIMPLE_CHAT if unclear
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
