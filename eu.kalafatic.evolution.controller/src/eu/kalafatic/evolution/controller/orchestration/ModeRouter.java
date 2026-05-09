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
        String lowerPrompt = prompt.toLowerCase().trim();

        // --- FAST PRECHECK: Greetings and simple chat detection ---
        if (lowerPrompt.matches("^(hi|hello|hey|greetings|good morning|good afternoon|good evening)\\s*[!.]*$")) {
            return createSimpleChatMode();
        }

        if (lowerPrompt.equals("tell me a joke")) return createSimpleChatMode();

        // 0. Analytical keywords detection - force Iterative (Assisted) mode early
        // to ensure "analyze project" works correctly even in MEDIATED mode.
        if (lowerPrompt.matches(".*\\b(analyze|investigate|report|summarize|discovery|audit)\\b.*")) {
            return createAssistedCodingMode();
        }

        // 1. Explicit mode keywords
        if (lowerPrompt.contains("mode: chat")) return createSimpleChatMode();
        if (lowerPrompt.contains("mode: assisted")) return createAssistedCodingMode();
        if (lowerPrompt.contains("mode: darwin")) return createDarwinMode();
        if (lowerPrompt.contains("mode: self-dev")) return createSelfDevMode();
        if (lowerPrompt.contains("mode: export") || lowerPrompt.contains("prepare export") || lowerPrompt.contains("manual self-dev package") || lowerPrompt.contains("export for chatgpt")) {
            return createHybridManualExportMode();
        }

        // 2. Map from existing model flags
        if (orchestrator != null) {
            // MEDIATED mode is a top-level override for manual workflow
            if (orchestrator.getAiMode() == eu.kalafatic.evolution.model.orchestration.AiMode.MEDIATED) {
                return createHybridManualExportMode();
            }

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
                case HYBRID_MANUAL_EXPORT: return createHybridManualExportMode();
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
        mode.getAllowedPaths().add("eu.kalafatic.evolution.controller/src");
        mode.getAllowedPaths().add("eu.kalafatic.evolution.view/src");
        mode.getAllowedPaths().add("eu.kalafatic.evolution.model/src");
        return mode;
    }

    private PlatformMode createHybridManualExportMode() {
        return new PlatformMode(PlatformType.HYBRID_MANUAL_EXPORT, AutonomyLevel.LOW, 1, false);
    }
}
