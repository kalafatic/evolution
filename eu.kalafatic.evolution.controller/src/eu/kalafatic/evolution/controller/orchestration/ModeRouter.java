package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.orchestration.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Routes execution based on detected or assigned PlatformMode.
 * Maps PlatformMode to concrete IOrchestrationFlow implementations.
 */
public class ModeRouter {

    /**
     * Resolves the appropriate orchestration flow based on the platform mode.
     */
    public IOrchestrationFlow resolveFlow(PlatformMode mode, AiService aiService, IterationManager manager) {
        if (mode == null) return (IOrchestrationFlow) eu.kalafatic.evolution.controller.agents.AgentFactory.getAgent(eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants.AGENT_GENERAL); // Fallback to basic chat

        switch (mode.getType()) {
            case DARWIN_MODE:
                return new DarwinFlow(aiService, manager);
            case SELF_DEV_MODE:
                return new DarwinFlow(aiService, manager);
            case HYBRID_MANUAL_EXPORT:
                return new MediatedAnalysisFlow(aiService, manager);
            case ASSISTED_CODING:
                return new DarwinFlow(aiService, manager);
            case SIMPLE_CHAT:
            default:
                return (IOrchestrationFlow) eu.kalafatic.evolution.controller.agents.AgentFactory.getAgent(eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants.AGENT_GENERAL);
        }
    }

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


        // 1. Explicit mode keywords
        if (lowerPrompt.contains("mode: chat")) return createSimpleChatMode();
        if (lowerPrompt.contains("mode: assisted")) return createAssistedCodingMode();
        if (lowerPrompt.contains("mode: darwin")) return createDarwinMode();
        if (lowerPrompt.contains("mode: self-dev")) return createSelfDevMode();
        if (lowerPrompt.contains("mode: mediated") || lowerPrompt.contains("analyze target")) return createHybridManualExportMode();
        if (lowerPrompt.contains("mode: export") || lowerPrompt.contains("prepare export") || lowerPrompt.contains("manual self-dev package") || lowerPrompt.contains("export for chatgpt")) {
            return createHybridManualExportMode();
        }

        // 2. Obvious coding keywords detection - prioritized to bypass heavy mediated flows for simple tasks
        // EXCEPT in MEDIATED mode where we still prefer export for general coding tasks unless analytical
        Pattern codingPattern = Pattern.compile("\\b(create|fix|add|run|test|generate|write|refactor|modify|delete|check|implement|build|improve|update|change|approve|select)\\b");
        if (codingPattern.matcher(lowerPrompt).find()) {
            if (orchestrator != null && orchestrator.getAiMode() == eu.kalafatic.evolution.model.orchestration.AiMode.MEDIATED) {
                 // Fall through to model-based routing for MEDIATED mode
            } else {
                return createAssistedCodingMode();
            }
        }

        // Self-dev intent keywords
        if (lowerPrompt.contains("iterationmanager") || lowerPrompt.contains("darwinflow") || lowerPrompt.contains("kernel") || lowerPrompt.contains("self-dev")) {
            return createSelfDevMode();
        }

        // 3. Map from existing model flags
        if (orchestrator != null) {
            // MEDIATED + SELF_DEV Support: If mediated, we only default to Export if no Iterative/Darwin flags are set.
            boolean isMediated = orchestrator.getAiMode() == eu.kalafatic.evolution.model.orchestration.AiMode.MEDIATED;

            if (orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null) {
                if (orchestrator.getAiChat().getPromptInstructions().isSelfIterativeMode()) {
                    return createSelfDevMode();
                }
            }
            if (orchestrator.isDarwinMode()) {
                // If mediated, we still prefer Export for general tasks unless specifically analytical
                if (isMediated && !isAnalytical(lowerPrompt)) {
                    return createHybridManualExportMode();
                }
                return createDarwinMode();
            }
            if (orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null) {
                if (orchestrator.getAiChat().getPromptInstructions().isIterativeMode()) {
                    return createAssistedCodingMode();
                }
            }

            // Fallback for MEDIATED if no iterative mode is active
            if (isMediated) {
                if (isAnalytical(lowerPrompt)) {
                    return createAssistedCodingMode();
                }
                return createHybridManualExportMode();
            }
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

    private boolean isAnalytical(String prompt) {
        if (prompt == null) return false;
        String lower = prompt.toLowerCase();
        return lower.contains("analyze") || lower.contains("investigate") || lower.contains("report") || lower.contains("summarize");
    }
}
