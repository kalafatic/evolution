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
            // 1. FAST-PATH: Check metadata flag if already analyzed
            Object isChat = context.getOrchestrationState().getMetadata().get("isChatRequest");
            if (isChat instanceof Boolean) {
                return (Boolean) isChat;
            }

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
            context.log("[ModeRecognizer] [CRITICAL] LLM analysis failed for chat mode detection: " + e.getMessage());
            // DEFAULT FALLBACK: If LLM fails, we MUST be conservative and assume TASK
            // unless the input is exceptionally short and non-technical.
            String rawInput = context.getOrchestrationState().getRawInput();
            return (rawInput != null && rawInput.trim().length() < 15 && !rawInput.toLowerCase().contains("code"));
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
}