package eu.kalafatic.evolution.controller.agents;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.ConversationState;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionMemoryGraph;
import eu.kalafatic.evolution.controller.parsers.structured.StructuredResponsePipeline;

/**
 * LLM-powered intent analyzer for EVO prompts.
 * 
 * <p><b>Core Principle:</b> Zero hardcoded strings. Everything is semantic.</p>
 */
public class PromptIntentAnalyzer extends BaseAiAgent {

    private static final String SYSTEM_INSTRUCTION = 
        "You are a Prompt Intent Analyzer for an evolutionary AI coding platform called EVO.\n\n" +
        "Your task is to classify user prompts into EXACTLY ONE of these categories based on their SEMANTIC INTENT, not just keywords.\n\n" +
        "1. CHAT - Pure conversation, meta-discussion about the project, or general knowledge inquiry.\n" +
        "   - Semantic Markers: Greetings, status checks, non-technical explanations, philosophy, or questions that DO NOT imply modifying the current codebase.\n" +
        "2. TASK - Any request that implies changing, creating, analyzing, or optimizing code or architecture.\n" +
        "   - Semantic Markers: Requests for implementation, refactoring, bug fixes, adding features, or deep technical analysis of specific project components.\n" +
        "3. CONTROL - Explicit system commands or workflow decisions.\n" +
        "   - Semantic Markers: Approval/rejection of variants, selection of specific paths, process steering, or confirmation of automated actions.\n\n" +
        "CRITICAL RULES:\n" +
        "- Avoid reliance on specific keywords (e.g., 'code', 'class'); instead, look for the underlying action requested.\n" +
        "- If the user expresses an intent to perform technical work, even without naming specific artifacts, classify as TASK.\n" +
        "- If uncertain between CHAT and TASK, bias toward TASK to ensure the evolutionary engine is engaged.\n" +
        "- Provide confidence score (0.0-1.0) and a deep semantic reasoning for your choice.";

    private static final String OUTPUT_SCHEMA = 
        "{\n" +
        "  \"category\": \"CHAT|TASK|CONTROL\",\n" +
        "  \"confidence\": 0.95,\n" +
        "  \"reasoning\": \"Brief explanation of classification\",\n" +
        "  \"subIntent\": \"Optional: for TASK, specify: CODE_CREATION|REFACTORING|DEBUGGING|ANALYSIS|OPTIMIZATION\",\n" +
        "  \"targetArtifact\": \"Optional: what is being worked on\"\n" +
        "}";

    private final StructuredResponsePipeline pipeline = new StructuredResponsePipeline();
    private final PromptIntentCache cache;

    public PromptIntentAnalyzer(SessionContainer container, File projectRoot) {
        super("PromptIntentAnalyzer", "PromptIntentAnalyzer", container);
        this.cache = new PromptIntentCache(container, projectRoot);
    }

    @Override
    protected String getAgentInstructions() {
        return SYSTEM_INSTRUCTION + "\n\nOUTPUT SCHEMA:\n" + OUTPUT_SCHEMA;
    }

    @Override
    protected String getFooterInstructions() {
        return "MANDATORY: Wrap your JSON response in <BEGIN_JSON> and <END_JSON> tags.\n" +
               "MANDATORY: category MUST be one of CHAT, TASK, or CONTROL.\n" +
               "MANDATORY: confidence MUST be between 0.0 and 1.0.\n" +
               "MANDATORY: Provide reasoning for your classification.";
    }

    /**
     * Analyzes the intent of a user prompt using LLM.
     */
    public IntentResult analyze(String prompt, TaskContext context) throws Exception {
        if (prompt == null || prompt.trim().isEmpty()) {
            return new IntentResult(IntentCategory.CHAT, 1.0, "Empty input", null, null);
        }

        // 1. Check cache
        IntentResult cached = cache.get(prompt);
        if (cached != null) {
            context.log("[INTENT] Cache hit for: " + prompt.substring(0, Math.min(prompt.length(), 30)) + "...");
            return cached;
        }

        context.log("[INTENT] LLM analyzing: " + prompt);

        // 2. Build prompt with context awareness
        String contextHint = buildContextHint(context);
        String userPrompt = String.format(
            "Analyze this user prompt:\n\n\"%s\"\n\n%s",
            prompt, contextHint
        );

        // 3. Call LLM
        String systemPrompt = getAgentInstructions() + "\n\n" + getFooterInstructions();
        String response = aiService.sendRequest(
            context.getOrchestrator(),
            systemPrompt + "\n\n" + userPrompt,
            context
        );

        // 4. Parse response
        JSONObject json = pipeline.process(response, new java.util.HashMap<>(), context);

        // 5. Extract results
        String categoryStr = json.optString("category", "TASK").toUpperCase();
        IntentCategory category;
        try {
            category = IntentCategory.valueOf(categoryStr);
        } catch (IllegalArgumentException e) {
            category = IntentCategory.TASK;
        }

        double confidence = json.optDouble("confidence", 0.5);
        String reasoning = json.optString("reasoning", "LLM classification");
        String subIntent = json.optString("subIntent", null);
        String targetArtifact = json.optString("targetArtifact", null);

        // 6. Validate confidence
        if (confidence < 0.3) {
            context.log("[INTENT] Low confidence (" + confidence + "). Defaulting to TASK.");
            confidence = 0.3;
            reasoning = "Low confidence, conservative fallback to TASK: " + reasoning;
            category = IntentCategory.TASK;
        }

        IntentResult result = new IntentResult(category, confidence, reasoning, subIntent, targetArtifact);

        // 8. Cache result
        cache.put(prompt, result);

        context.log("[INTENT] Result: " + category + " (confidence: " + confidence + ") - " + reasoning);
        return result;
    }

    /**
     * Builds a context hint for the LLM.
     */
    /**
     * Builds a context hint for the LLM.
     */
    private String buildContextHint(TaskContext context) {
        StringBuilder sb = new StringBuilder();
        
        // Current system state
        String state = context.getStateHolder().getState().toString();
        sb.append("Current system state: ").append(state).append("\n");

        // Phase
        String phase = context.getOrchestrationState().getCurrentPhase();
        if (phase != null && !phase.isEmpty()) {
            sb.append("Current evolution phase: ").append(phase).append("\n");
        }

        // Iteration count
        int iteration = context.getOrchestrationState().getIterationCount();
        sb.append("Iteration count: ").append(iteration).append("\n");

        // Platform mode
        if (context.getPlatformMode() != null) {
            sb.append("Platform mode: ").append(context.getPlatformMode().getType()).append("\n");
        }

        // Self-Dev mode
        boolean isSelfDev = context.getBehaviorProfile().hasTrait(
            eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait.WORKFLOW_SELF_DEV);
        sb.append("Self-Dev mode: ").append(isSelfDev).append("\n");

        // Mediated mode
        boolean isMediated = context.getOrchestrator() != null && 
            eu.kalafatic.evolution.model.orchestration.AiMode.MEDIATED.equals(context.getOrchestrator().getAiMode());
        sb.append("Mediated mode: ").append(isMediated).append("\n");

        // Conversation history (last messages)
        try {
            ConversationState convState = ConversationState.load(context.getSharedMemory(), context.getSessionId());
            if (convState != null) {
                // ✅ CORRECT: Use getLastMessages()
                java.util.List<String> messages = convState.getLastMessages();
                
                if (messages != null && !messages.isEmpty()) {
                    int size = messages.size();
                    int start = Math.max(0, size - 3);
                    sb.append("\nRecent conversation:\n");
                    for (int i = start; i < size; i++) {
                        String msg = messages.get(i);
                        if (msg != null) {
                            String truncated = msg.length() > 100 ? msg.substring(0, 100) + "..." : msg;
                            sb.append("- ").append(truncated).append("\n");
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore if conversation state unavailable
        }

        return sb.toString();
    }


    // ============================================================
    // INNER CLASSES
    // ============================================================

    public enum IntentCategory {
        CHAT, TASK, CONTROL
    }

    public static final class IntentResult {
        private final IntentCategory category;
        private final double confidence;
        private final String reasoning;
        private final String subIntent;
        private final String targetArtifact;

        public IntentResult(IntentCategory category, double confidence, String reasoning, 
                           String subIntent, String targetArtifact) {
            this.category = category;
            this.confidence = Math.max(0.0, Math.min(1.0, confidence));
            this.reasoning = reasoning;
            this.subIntent = subIntent;
            this.targetArtifact = targetArtifact;
        }

        public IntentCategory getCategory() { return category; }
        public double getConfidence() { return confidence; }
        public String getReasoning() { return reasoning; }
        public String getSubIntent() { return subIntent; }
        public String getTargetArtifact() { return targetArtifact; }

        public boolean isChat() { return category == IntentCategory.CHAT; }
        public boolean isTask() { return category == IntentCategory.TASK; }
        public boolean isControl() { return category == IntentCategory.CONTROL; }

        @Override
        public String toString() {
            return String.format("IntentResult{category=%s, confidence=%.2f, reasoning='%s', subIntent=%s, target=%s}",
                category, confidence, reasoning, subIntent, targetArtifact);
        }
    }

    // ============================================================
    // CACHE
    // ============================================================

    private static final class PromptIntentCache {
     // In PromptIntentCache:
        private final SessionContainer container;
        private final File projectRoot;
        private final Map<String, IntentResult> localCache = new ConcurrentHashMap<>(); // Local fallback
        private static final int MAX_CACHE_SIZE = 100;
        
        PromptIntentCache(SessionContainer container, File projectRoot) {
            this.container = container;
            this.projectRoot = projectRoot;
        }

        IntentResult get(String prompt) {
            String key = prompt.toLowerCase().trim();
            
            // Try to get from EvolutionMemoryGraph metadata
            try {
                EvolutionMemoryGraph graph = container.getEvolutionMemoryGraph();
                if (graph != null) {
                    Map<String, Object> metadata = graph.getMetadata();
                    if (metadata != null) {
                        Object cache = metadata.get("intentCache");
                        if (cache instanceof Map) {
                            Map<String, IntentResult> map = (Map<String, IntentResult>) cache;
                            return map.get(key);
                        }
                    }
                }
            } catch (Exception e) {
                // Fall back to local cache
            }
            
            return localCache.get(key);
        }

        void put(String prompt, IntentResult result) {
            String key = prompt.toLowerCase().trim();
            
            // Store in local cache
            if (localCache.size() >= MAX_CACHE_SIZE) {
                String firstKey = localCache.keySet().iterator().next();
                localCache.remove(firstKey);
            }
            localCache.put(key, result);
            
            // Also try to store in EvolutionMemoryGraph
            try {
                EvolutionMemoryGraph graph = container.getEvolutionMemoryGraph();
                if (graph != null) {
                    Map<String, Object> metadata = graph.getMetadata();
                    if (metadata == null) {
                        // Some graphs may not expose a mutable map — if so, skip
                        return;
                    }
                    Map<String, IntentResult> cache = (Map<String, IntentResult>) metadata.get("intentCache");
                    if (cache == null) {
                        cache = new ConcurrentHashMap<>();
                        metadata.put("intentCache", cache);
                    }
                    if (cache.size() >= MAX_CACHE_SIZE) {
                        String firstKey = cache.keySet().iterator().next();
                        cache.remove(firstKey);
                    }
                    cache.put(key, result);
                }
            } catch (Exception e) {
                // Skip persistence — local cache is enough
            }
        }
    }
}