package eu.kalafatic.evolution.controller.orchestration.goal;

import org.json.JSONObject;
import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.parsers.structured.StructuredResponsePipeline;

/**
 * Engine for parsing user prompts into a formal GoalModel.
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
               "Be precise. Do not over-engineer simple requests.";
    }

    @Override
    protected String getFooterInstructions() {
        return "OUTPUT SCHEMA:\n" +
               "{\n" +
               "  \"goalType\": \"CODE_GENERATION|REFACTORING|ANALYSIS|DOCUMENTATION\",\n" +
               "  \"domain\": \"JAVA|SPRING|SQL|HTML|XML|AI|ARCHITECTURE|SELF_DEV|GENERAL\",\n" +
               "  \"intent\": \"CREATE|MODIFY|DELETE|EXPLORE|FIX\",\n" +
               "  \"requestedArtifact\": \"string\",\n" +
               "  \"primaryAction\": \"string\",\n" +
               "  \"complexity\": \"SIMPLE|MEDIUM|HIGH\",\n" +
               "  \"requiredOutputs\": \"string\",\n" +
               "  \"confidence\": float,\n" +
               "  \"ambiguity\": float\n" +
               "}";
    }

    public GoalModel understand(String prompt, TaskContext context) throws Exception {
        context.log("[GOAL UNDERSTANDING] Analyzing user prompt: " + prompt);
        
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
}
