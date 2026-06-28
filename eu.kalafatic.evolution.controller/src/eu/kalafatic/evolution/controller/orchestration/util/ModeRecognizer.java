package eu.kalafatic.evolution.controller.orchestration.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.model.orchestration.AiMode;

/**
 * Central Mode Recognition Utility.
 * Non-static to allow dependency injection of SessionContainer.
 */
public class ModeRecognizer {
    
    private final SessionContainer sessionContainer;
    private final Map<String, PromptIntentAnalyzer> analyzerCache = new ConcurrentHashMap<>();
    
    public ModeRecognizer(SessionContainer sessionContainer) {
        this.sessionContainer = sessionContainer;
    }
    
    /**
     * Determines the current mode.
     */
    public String determineMode(TaskContext context) {
        if (isSelfDevMode(context)) {
            return "SELF_DEV";
        }
        if (isMediatedMode(context)) {
            return "MEDIATED";
        }
        if (isChatMode(context)) {
            return "CHAT";
        }
        return "STANDARD";
    }
    
    public static boolean isSelfDevMode(TaskContext context) {
        return (context.getOrchestrator() != null) 
                && (context.getOrchestrator().getAiChat() != null) 
                && (context.getOrchestrator().getAiChat().getPromptInstructions() != null)
                && (context.getOrchestrator().getAiChat().getPromptInstructions().isSelfIterativeMode());
    }
    
    public static boolean isMediatedMode(TaskContext context) {
        return (context.getOrchestrator() != null) 
                && (AiMode.MEDIATED.equals(context.getOrchestrator().getAiMode()));
    }
    
    public boolean isChatMode(TaskContext context) {
        try {
            String rawInput = context.getOrchestrationState().getRawInput();
            if (rawInput == null || rawInput.trim().isEmpty()) {
                return false;
            }
            
            PromptIntentAnalyzer analyzer = getOrCreateAnalyzer(context);
            if (analyzer == null) {
                return false;
            }
            
            PromptIntentAnalyzer.IntentResult result = analyzer.analyze(rawInput, context);
            return result.isChat();
        } catch (Exception e) {
            context.log("[ModeRecognizer] LLM analysis failed: " + e.getMessage());
            return isConversationalInput(context.getOrchestrationState().getRawInput());
        }
    }
    
    private PromptIntentAnalyzer getOrCreateAnalyzer(TaskContext context) {
        String sessionId = context.getSessionId();
        PromptIntentAnalyzer analyzer = analyzerCache.get(sessionId);
        if (analyzer != null) {
            return analyzer;
        }
        
        try {
            java.io.File projectRoot = context.getProjectRoot();
            analyzer = new PromptIntentAnalyzer(sessionContainer, projectRoot);
            analyzer.setAiService(context.getAiService());
            analyzerCache.put(sessionId, analyzer);
            return analyzer;
        } catch (Exception e) {
            context.log("[ModeRecognizer] Failed to create analyzer: " + e.getMessage());
            return null;
        }
    }
    
    private boolean isConversationalInput(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        String lower = input.toLowerCase().trim();
        
        if (lower.matches("^(hi|hello|hey|greetings|howdy|sup|yo|what's up|how are you).*")) {
            return true;
        }
        if (lower.matches("^(what|why|when|where|who|how|can you|will you|do you|is it|are you).*")) {
            if (!lower.matches(".*(code|class|method|function|implement|generate|create|write|build|print).*")) {
                return true;
            }
        }
        if (lower.length() < 30 && 
            !lower.matches(".*(create|generate|write|implement|build|class|method|function|code|print).*")) {
            return true;
        }
        return false;
    }
}