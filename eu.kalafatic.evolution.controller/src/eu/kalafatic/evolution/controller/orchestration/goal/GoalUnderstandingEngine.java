package eu.kalafatic.evolution.controller.orchestration.goal;

import org.json.JSONObject;
import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.util.ModeRecognizer;
import eu.kalafatic.evolution.controller.parsers.structured.StructuredResponsePipeline;

/**
 * Engine for parsing user prompts into a formal GoalModel.
 * Everything goes through LLM, EXCEPT pure chat which is fast‑path detected.
 */
public class GoalUnderstandingEngine extends BaseAiAgent {
    private final StructuredResponsePipeline pipeline = new StructuredResponsePipeline();

    public GoalUnderstandingEngine(SessionContainer container) {
        super("GoalUnderstandingEngine", "GoalUnderstandingEngine", container);
    }

    @Override
    protected String getAgentInstructions() {
        return "You are a Goal Understanding Engine. Your role is to parse user prompts into a formal GoalModel.\n" +
               "Identify the type of request, the domain, the intent, the requested artifact, the primary action, complexity, and expected outputs.\n" +
               "Be precise. Do not over-engineer simple requests.\n" +
               "STRICT RULE: Greetings (e.g., 'hello', 'hi') and non-technical conversational prompts must be assigned to the 'GENERAL' domain with a 'primaryAction' like 'respond conversationally'. Do NOT default them to 'JAVA' or other technical domains.";
    }

    @Override
    protected String getFooterInstructions() {
        return "OUTPUT SCHEMA:\n" +
               "{\n" +
               "  \"goalType\": \"CODE_GENERATION|REFACTORING|ANALYSIS|DOCUMENTATION|CHAT\",\n" +
               "  \"domain\": \"JAVA|SPRING|SQL|HTML|XML|AI|ARCHITECTURE|SELF_DEV|GENERAL\",\n" +
               "  \"intent\": \"CREATE|MODIFY|DELETE|EXPLORE|FIX|RESPOND\",\n" +
               "  \"requestedArtifact\": \"string\",\n" +
               "  \"primaryAction\": \"string\",\n" +
               "  \"complexity\": \"SIMPLE|MEDIUM|HIGH\",\n" +
               "  \"requiredOutputs\": \"string\",\n" +
               "  \"confidence\": float,\n" +
               "  \"ambiguity\": float\n" +
               "}";
    }

    public GoalModel understand(SessionContainer sessionContainer, String prompt, TaskContext context) throws Exception {
        context.log("[GOAL UNDERSTANDING] Analyzing user prompt: " + prompt);

        // ============================================================
        // FAST‑PATH: Detect pure chat prompts
        // Everything still goes through LLM EXCEPT these trivial greetings
        // ============================================================
        String trimmed = prompt == null ? "" : prompt.trim().toLowerCase();
       
        if (new ModeRecognizer(sessionContainer).isChatMode(context)) {
            context.log("[GOAL UNDERSTANDING] Fast‑path: CHAT detected. Returning chat‑friendly GoalModel.");

            GoalModel chatModel = new GoalModel();
            chatModel.setGoalType("CHAT");
            chatModel.setDomain("GENERAL");
            chatModel.setIntent("RESPOND");
            chatModel.setRequestedArtifact("conversation");
            chatModel.setPrimaryAction(generateChatAction(trimmed));
            chatModel.setComplexity("SIMPLE");
            chatModel.setRequiredOutputs("conversational response");
            chatModel.setConfidence(1.0);
            chatModel.setAmbiguity(0.0);

            context.log("[GOAL UNDERSTANDING] Derived Goal Model (CHAT fast‑path): " + chatModel.toString());
            return chatModel;
        }

        // ============================================================
        // NORMAL FLOW: LLM‑based goal extraction for tasks
        // ============================================================
        String systemPrompt = getAgentInstructions() + "\n\n" + getFooterInstructions();
        String userPrompt = "Parse the following user request into a GoalModel:\n\n" + prompt;

        String response = aiService.sendRequest(context.getOrchestrator(), systemPrompt + "\n\n" + userPrompt, context);

        JSONObject json = pipeline.process(response, new java.util.HashMap<>(), context);

        GoalModel model = new GoalModel();
        model.setGoalType(json.optString("goalType", "CODE_GENERATION"));
        model.setDomain(json.optString("domain", "GENERAL"));
        model.setIntent(json.optString("intent", "CREATE"));
        model.setRequestedArtifact(json.optString("requestedArtifact", "unknown"));
        model.setPrimaryAction(json.optString("primaryAction", "unknown"));
        model.setComplexity(json.optString("complexity", "MEDIUM"));
        model.setRequiredOutputs(json.optString("requiredOutputs", "unknown"));
        model.setConfidence(json.optDouble("confidence", 0.5));
        model.setAmbiguity(json.optDouble("ambiguity", 0.5));

        context.log("[GOAL UNDERSTANDING] Derived Goal Model: " + model.toString());
        return model;
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    private String generateChatAction(String trimmed) {
        if (trimmed.matches("^(hi|hello|hey|good morning|good afternoon|good evening|howdy|greetings|yo|sup)$")) {
            return "respond to greeting";
        }
        if (trimmed.matches("^(how are you|how are you doing|how's it going|what's up|whats up)$")) {
            return "respond to status inquiry";
        }
        if (trimmed.matches("^(thanks|thank you|thx|ty|great|awesome|nice|sure|fine)$")) {
            return "respond to acknowledgment";
        }
        if (trimmed.matches("^(goodbye|bye|see you|see ya)$")) {
            return "respond to farewell";
        }
        return "respond conversationally";
    }
}