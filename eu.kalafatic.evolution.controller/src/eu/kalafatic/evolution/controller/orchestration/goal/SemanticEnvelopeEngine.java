package eu.kalafatic.evolution.controller.orchestration.goal;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import org.json.JSONObject;
import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.parsers.structured.StructuredResponsePipeline;
import java.util.ArrayList;
import org.json.JSONArray;

/**
 * Engine for deriving a SemanticEnvelope from a GoalModel.
 * Defines the boundaries and permitted mutation space for evolution.
 */
public class SemanticEnvelopeEngine extends BaseAiAgent {
    private final StructuredResponsePipeline pipeline = new StructuredResponsePipeline();

    public SemanticEnvelopeEngine(SessionContainer container) {
        super("SemanticEnvelopeEngine", "SemanticEnvelopeEngine", container);
    }

    @Override
    protected String getAgentInstructions() {
        return "You are a Semantic Envelope Engine. Your role is to define strict boundaries for evolutionary trajectory generation.\n" +
               "Given a GoalModel, you must extract mandatory concepts, derive allowed mutation dimensions, define abstraction limits, and compute forbidden semantic regions.\n" +
               "GOAL: Prevent 'architectural inflation' and 'semantic drift'.\n" +
               "STRICT BOUNDARIES FOR SIMPLE TASKS:\n" +
               "- If the goal is a simple Java task (e.g., 'print text', 'calculate sum'), you MUST identify frameworks (Spring), messaging (Kafka), and distributed patterns (Microservices) as FORBIDDEN regions.\n" +
               "- Allowed mutation dimensions for simple tasks: Class structure, API design (static vs instance), Naming, Documentation style.\n" +
               "- Mandatory concepts must include the core domain objects mentioned in the goal.";
    }

    @Override
    protected String getFooterInstructions() {
        return "OUTPUT SCHEMA:\n" +
               "{\n" +
               "  \"coreIntent\": \"string\",\n" +
               "  \"mandatoryConcepts\": [\"concept1\", \"concept2\"],\n" +
               "  \"allowedMutationDimensions\": [\"dimension1\", \"dimension2\"],\n" +
               "  \"discouragedRegions\": [\"region1\", \"region2\"],\n" +
               "  \"forbiddenRegions\": [\"region1\", \"region2\"],\n" +
               "  \"maxAbstractionDepth\": 1-5,\n" +
               "  \"semanticDistanceThreshold\": 0.0-1.0\n" +
               "}";
    }

    public SemanticEnvelope derive(GoalModel goal, TaskContext context) throws Exception {
        context.log("[SEMANTIC ENVELOPE] Deriving envelope for goal: " + goal.getPrimaryAction());

        String systemPrompt = getAgentInstructions() + "\n\n" + getFooterInstructions();
        String userPrompt = "Derive a SemanticEnvelope for the following GoalModel:\n\n" + goal.toString();

        String response = aiService.sendRequest(context.getOrchestrator(), systemPrompt + "\n\n" + userPrompt, context);

        JSONObject json = pipeline.process(response, new java.util.HashMap<>(), context);

        SemanticEnvelope envelope = new SemanticEnvelope();
        envelope.setCoreIntent(json.optString("coreIntent", goal.getPrimaryAction()));

        JSONArray mandatory = json.optJSONArray("mandatoryConcepts");
        if (mandatory != null) {
            for (int i = 0; i < mandatory.length(); i++) envelope.getMandatoryConcepts().add(mandatory.getString(i));
        }

        JSONArray dimensions = json.optJSONArray("allowedMutationDimensions");
        if (dimensions != null) {
            for (int i = 0; i < dimensions.length(); i++) envelope.getAllowedMutationDimensions().add(dimensions.getString(i));
        }

        JSONArray discouraged = json.optJSONArray("discouragedRegions");
        if (discouraged != null) {
            for (int i = 0; i < discouraged.length(); i++) envelope.getDiscouragedRegions().add(discouraged.getString(i));
        }

        JSONArray forbidden = json.optJSONArray("forbiddenRegions");
        if (forbidden != null) {
            for (int i = 0; i < forbidden.length(); i++) envelope.getForbiddenRegions().add(forbidden.getString(i));
        }

        envelope.setMaxAbstractionDepth(json.optInt("maxAbstractionDepth", 3));
        envelope.setSemanticDistanceThreshold(json.optDouble("semanticDistanceThreshold", 0.3));

        context.log("[SEMANTIC ENVELOPE] Derived Envelope: " + envelope.toString());
        return envelope;
    }
}
