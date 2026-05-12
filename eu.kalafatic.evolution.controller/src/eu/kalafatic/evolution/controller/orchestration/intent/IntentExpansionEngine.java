package eu.kalafatic.evolution.controller.orchestration.intent;

import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceArtifact;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Engine for expanding user intent and exploring ambiguity before Darwin execution.
 */
public class IntentExpansionEngine extends BaseAiAgent {

    public IntentExpansionEngine() {
        super("IntentExpansionEngine", "IntentExpansionEngine");
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an Intent Expansion Engine. Your goal is to analyze user requests for ambiguity and explore the 'intent space' before implementation begins.\n" +
               "Identify unresolved dimensions of intent (e.g., target environment, style, specific behavior) and generate coherent hypotheses for what the user might want.\n" +
               "STRICT RULES:\n" +
               "1. Do NOT generate code.\n" +
               "2. Do NOT execute tasks.\n" +
               "3. Focus on structural and behavioral assumptions.\n" +
               "4. Output MUST be ONLY valid JSON.";
    }

    @Override
    protected String getFooterInstructions() {
        return "OUTPUT SCHEMA:\n" +
               "{\n" +
               "  \"dimensions\": [\n" +
               "    {\n" +
               "      \"dimensionId\": \"string\",\n" +
               "      \"name\": \"string\",\n" +
               "      \"confidence\": float,\n" +
               "      \"inferredValue\": \"string\",\n" +
               "      \"candidateValues\": [\"string\"],\n" +
               "      \"ambiguityScore\": float,\n" +
               "      \"requiresUserInput\": boolean,\n" +
               "      \"rationale\": \"string\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"hypotheses\": [\n" +
               "    {\n" +
               "      \"id\": \"string\",\n" +
               "      \"description\": \"string\",\n" +
               "      \"confidence\": float,\n" +
               "      \"dimensionValues\": [\n" +
               "        { \"dimensionId\": \"string\", \"value\": \"string\" }\n" +
               "      ]\n" +
               "    }\n" +
               "  ],\n" +
               "  \"confidence\": {\n" +
               "    \"overallConfidence\": float,\n" +
               "    \"structuralConfidence\": float,\n" +
               "    \"semanticConfidence\": float,\n" +
               "    \"rationale\": \"string\"\n" +
               "  }\n" +
               "}";
    }

    public IntentExpansionResult expand(String prompt, TaskContext context) throws Exception {
        context.log("[INTENT EXPANSION] Analyzing intent space for: " + prompt);

        String systemPrompt = getAgentInstructions() + "\n\n" + getFooterInstructions();
        String userPrompt = "Analyze the following user request and expand the intent space:\n\n" + prompt;

        String response = aiService.sendRequest(context.getOrchestrator(), systemPrompt + "\n\n" + userPrompt, context);
        JSONObject json = JsonUtils.extractJsonObject(response);

        if (json == null) {
            throw new Exception("Failed to parse intent expansion JSON: " + response);
        }

        IntentExpansionResult result = new IntentExpansionResult();
        result.setOriginalPrompt(prompt);

        // Parse Dimensions
        JSONArray dims = json.optJSONArray("dimensions");
        if (dims != null) {
            for (int i = 0; i < dims.length(); i++) {
                JSONObject dObj = dims.getJSONObject(i);
                IntentDimension d = new IntentDimension();
                d.setDimensionId(dObj.optString("dimensionId"));
                d.setName(dObj.optString("name"));
                d.setConfidence(dObj.optDouble("confidence", 0.5));
                d.setInferredValue(dObj.optString("inferredValue"));
                d.setCandidateValues(JsonUtils.toStringList(dObj.optJSONArray("candidateValues")));
                d.setAmbiguityScore(dObj.optDouble("ambiguityScore", 0.0));
                d.setRequiresUserInput(dObj.optBoolean("requiresUserInput", false));
                d.setRationale(dObj.optString("rationale"));
                result.getDimensions().add(d);
            }
        }

        // Parse Hypotheses
        JSONArray hyps = json.optJSONArray("hypotheses");
        if (hyps != null) {
            for (int i = 0; i < hyps.length(); i++) {
                JSONObject hObj = hyps.getJSONObject(i);
                IntentHypothesis h = new IntentHypothesis();
                h.setId(hObj.optString("id"));
                h.setDescription(hObj.optString("description"));
                h.setConfidence(hObj.optDouble("confidence", 0.5));

                JSONArray vals = hObj.optJSONArray("dimensionValues");
                if (vals != null) {
                    for (int j = 0; j < vals.length(); j++) {
                        JSONObject vObj = vals.getJSONObject(j);
                        IntentHypothesis.DimensionValue dv = new IntentHypothesis.DimensionValue();
                        dv.setDimensionId(vObj.optString("dimensionId"));
                        dv.setValue(vObj.optString("value"));
                        h.getDimensionValues().add(dv);
                    }
                }
                result.getHypotheses().add(h);
            }
        }

        // Parse Confidence
        JSONObject confObj = json.optJSONObject("confidence");
        if (confObj != null) {
            IntentConfidence c = new IntentConfidence();
            c.setOverallConfidence(confObj.optDouble("overallConfidence", 0.0));
            c.setStructuralConfidence(confObj.optDouble("structuralConfidence", 0.0));
            c.setSemanticConfidence(confObj.optDouble("semanticConfidence", 0.0));
            c.setRationale(confObj.optString("rationale"));
            result.setConfidence(c);
        }

        // PERSISTENCE: Save clarification conclusions to Semantic Workspace
        persistClarifications(result, context);

        return result;
    }

    private void persistClarifications(IntentExpansionResult result, TaskContext context) {
        for (IntentDimension dim : result.getDimensions()) {
            if (dim.getConfidence() > 0.7 && dim.getInferredValue() != null && !dim.getInferredValue().isEmpty()) {
                String artifactId = "clarification-" + dim.getDimensionId() + "-" + System.currentTimeMillis();
                WorkspaceArtifact artifact = new WorkspaceArtifact(artifactId, "clarification-conclusion");
                artifact.setContent("Intent clarified: " + dim.getName() + " is resolved to: " + dim.getInferredValue());
                artifact.setConfidence(dim.getConfidence());
                artifact.getSemanticTags().add(dim.getName());
                artifact.getSemanticTags().add("intent");
                artifact.setSourceIteration("it-" + context.getCurrentIteration());

                context.getSemanticWorkspace().addArtifact(artifact);
                context.log("[WORKSPACE] Persisted intent clarification: " + dim.getName());
            }
        }
    }
}
