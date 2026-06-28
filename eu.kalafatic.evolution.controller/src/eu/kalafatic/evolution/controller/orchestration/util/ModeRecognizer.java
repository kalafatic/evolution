package eu.kalafatic.evolution.controller.orchestration.util;

import java.util.Map;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.SelfDevStatus;

/**
 * Central Mode Recognition Utility.
 * 
 * Mode priority (highest to lowest):
 * 1. SELF_DEV   - Explicit user setting (checkbox)
 * 2. MEDIATED   - Explicit user setting (combo)
 * 3. CHAT       - Recognized conversational intent
 * 4. STANDARD   - Everything else (default)
 * 
 * STANDARD is NOT recognized. It is the DEFAULT.
 * CHAT is recognized. Everything else becomes STANDARD.
 */
public class ModeRecognizer {
    
    /**
     * Determines the current mode.
     * 
     * @param context The task context
     * @return SELF_DEV, MEDIATED, CHAT, or STANDARD
     */
    public static String determineMode(TaskContext context) {
        // 1. SELF_DEV - Explicit user setting (checkbox)
        if (isSelfDevMode(context)) {
            return "SELF_DEV";
        }
        
        // 2. MEDIATED - Explicit user setting (combo)
        if (isMediatedMode(context)) {
            return "MEDIATED";
        }
        
        // 3. CHAT - Recognized conversational intent
        if (isChatMode(context)) {
            return "CHAT";
        }
        
        // 4. STANDARD - Everything else (default)
        return "STANDARD";
    }
    
    /**
     * Checks if Self-Dev mode is enabled via checkbox.
     */
    public static boolean isSelfDevMode(TaskContext context) {
    	return (context.getOrchestrator() != null) 
    			&&  (context.getOrchestrator().getAiChat() != null) 
    					&& (context.getOrchestrator().getAiChat().getPromptInstructions() != null)
    					&& (context.getOrchestrator().getAiChat().getPromptInstructions().isSelfIterativeMode()) ;
    }
    
    /**
     * Checks if Mediated mode is selected in combo.
     */
    public static boolean isMediatedMode(TaskContext context) {
    	return (context.getOrchestrator() != null) 
    			&& (AiMode.MEDIATED.equals(context.getOrchestrator().getAiMode())) ;
    }
    
    /**
     * Checks if the current request is conversational.
     * This is the ONLY mode that is "recognized" - everything else is STANDARD.
     */
    public static boolean isChatMode(TaskContext context) {
        // 1. Check explicit chat platform mode
        if (context.getPlatformMode() != null && 
            "CHAT".equals(context.getPlatformMode().getType().name())) {
            return true;
        }
        
        // 2. Check REASONING_ATOMIC trait (conversational reasoning)
        if (context.getBehaviorProfile().hasTrait(BehaviorTrait.REASONING_ATOMIC)) {
            return true;
        }
        
        // 3. Check GoalModel for chat intent
        GoalModel goal = getGoalModel(context);
        if (goal != null) {
            // Explicit chat goal type
            if ("CHAT".equals(goal.getGoalType()) || 
                "CONVERSATION".equals(goal.getIntent())) {
                return true;
            }
            
            // If there's no clear code intent → could be chat
            String goalType = goal.getGoalType();
            String intent = goal.getIntent();
            String primaryAction = goal.getPrimaryAction();
            
            // Check for explicit code indicators
            boolean isCodeGoal = isCodeGenerationGoal(goalType, intent, primaryAction);
            
            // If it's NOT a code goal, it might be chat
            if (!isCodeGoal) {
                return true;
            }
        }
        
        // 4. Check the raw input for conversational patterns
        String rawInput = context.getOrchestrationState().getRawInput();
        if (rawInput != null && !rawInput.isEmpty()) {
            if (isConversationalInput(rawInput)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if the goal is explicitly code generation.
     */
    public static boolean isCodeGenerationGoal(String goalType, String intent, String primaryAction) {
        // Check goal type
        if (goalType != null) {
            if ("CODE_GENERATION".equals(goalType) ||
                "IMPLEMENTATION".equals(goalType) ||
                "REFACTORING".equals(goalType) ||
                "ANALYSIS".equals(goalType) ||
                "OPTIMIZATION".equals(goalType) ||
                "DEBUGGING".equals(goalType) ||
                "TESTING".equals(goalType)) {
                return true;
            }
        }
        
        // Check intent
        if (intent != null) {
            if ("CREATE".equals(intent) ||
                "MODIFY".equals(intent) ||
                "DELETE".equals(intent) ||
                "FIX".equals(intent) ||
                "OPTIMIZE".equals(intent)) {
                return true;
            }
        }
        
        // Check primary action for code keywords
        if (primaryAction != null) {
            String action = primaryAction.toLowerCase();
            if (action.matches(".*(create|generate|write|implement|build|class|method|function|print|calculate|process|validate|parse|convert).*")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if the input is conversational.
     * This is a narrow check - only clear conversational patterns.
     */
    private static boolean isConversationalInput(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        String lower = input.toLowerCase().trim();
        
        // Greetings
        if (lower.matches("^(hi|hello|hey|greetings|howdy|sup|yo|what's up|how are you).*")) {
            return true;
        }
        
        // Questions (clear natural language)
        if (lower.matches("^(what|why|when|where|who|how|can you|will you|do you|is it|are you).*")) {
            // But not if it contains code keywords
            if (!lower.matches(".*(code|class|method|function|implement|generate|create|write|build).*")) {
                return true;
            }
        }
        
        // Short conversational messages
        if (lower.length() < 30 && 
            !lower.matches(".*(create|generate|write|implement|build|class|method|function|code).*")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets the GoalModel from context.
     */
    private static GoalModel getGoalModel(TaskContext context) {
        Object goalObj = context.getOrchestrationState().getMetadata().get("goalModel");
        if (goalObj instanceof GoalModel) {
            return (GoalModel) goalObj;
        } else if (goalObj instanceof Map) {
            return convertMapToGoalModel((Map<String, Object>) goalObj);
        }
        return null;
    }
    
    /**
     * Converts Map to GoalModel.
     */
    @SuppressWarnings("unchecked")
    private static GoalModel convertMapToGoalModel(Map<String, Object> map) {
        GoalModel goal = new GoalModel();
        if (map.containsKey("goalType")) {
            goal.setGoalType(String.valueOf(map.get("goalType")));
        }
        if (map.containsKey("domain")) {
            goal.setDomain(String.valueOf(map.get("domain")));
        }
        if (map.containsKey("intent")) {
            goal.setIntent(String.valueOf(map.get("intent")));
        }
        if (map.containsKey("requestedArtifact")) {
            goal.setRequestedArtifact(String.valueOf(map.get("requestedArtifact")));
        }
        if (map.containsKey("primaryAction")) {
            goal.setPrimaryAction(String.valueOf(map.get("primaryAction")));
        }
        if (map.containsKey("complexity")) {
            goal.setComplexity(String.valueOf(map.get("complexity")));
        }
        if (map.containsKey("requiredOutputs")) {
            goal.setRequiredOutputs(String.valueOf(map.get("requiredOutputs")));
        }
        if (map.containsKey("confidence")) {
            Object val = map.get("confidence");
            if (val instanceof Number) {
                goal.setConfidence(((Number) val).doubleValue());
            }
        }
        if (map.containsKey("ambiguity")) {
            Object val = map.get("ambiguity");
            if (val instanceof Number) {
                goal.setAmbiguity(((Number) val).doubleValue());
            }
        }
        return goal;
    }
}
