package eu.kalafatic.evolution.controller.orchestration.intent;

import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;
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
               "Identify unresolved dimensions of intent and generate coherent hypotheses for what the user might want.\n" +
               "\n" +
               "CRITICAL DISTINCTION:\n" +
               "1. SEMANTIC AMBIGUITY: Missing critical information or contradictory constraints that prevent safe execution (e.g., unknown platform, unknown auth model).\n" +
               "2. IMPLEMENTATION POLYMORPHISM: Multiple valid ways to implement a clear intent (e.g., choice of library, sync vs async, logger vs println).\n" +
               "\n" +
               "Implementation polymorphism MUST be captured as 'implementationStrategies', NOT as ambiguities requiring user clarification.\n" +
               "\n" +
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
               "  \"state\": \"CLEAR\", // CRITICAL: Choose exactly ONE from [CLEAR, EVOLVABLE, NEEDS_CLARIFICATION, BLOCKED, CONTRADICTORY]. Do NOT echo the list.\n" +
               "  \"dominantIntent\": \"string (clear engineering objective)\",\n" +
               "  \"dominantConfidence\": float (0.0-1.0),\n" +
               "  \"ambiguityScore\": float (0.0-1.0, only for semantic ambiguity),\n" +
               "  \"executionRiskScore\": float (0.0-1.0),\n" +
               "  \"evolutionOpportunityScore\": float (0.0-1.0),\n" +
               "  \"implementationStrategies\": [\"string (valid alternative approach)\"],\n" +
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

        // PERSISTENCE: Check Semantic Workspace for prior resolutions
        List<WorkspaceArtifact> priorConclusions = context.getSemanticWorkspace().findArtifactsByType("clarification-conclusion");
        String priorContext = "";
        if (!priorConclusions.isEmpty()) {
            StringBuilder sb = new StringBuilder("\nPRIOR CLARIFICATIONS:\n");
            for (WorkspaceArtifact a : priorConclusions) {
                sb.append("- ").append(a.getContent()).append("\n");
            }
            priorContext = sb.toString();
            context.log("[INTENT EXPANSION] Found " + priorConclusions.size() + " prior clarifications in workspace.");
        }

        String systemPrompt = getAgentInstructions() + "\n\n" + getFooterInstructions();
        String userPrompt = "Analyze the following user request and expand the intent space:\n\n" + prompt + priorContext;

        String response = aiService.sendRequest(context.getOrchestrator(), systemPrompt + "\n\n" + userPrompt, context);
        JSONObject json = JsonUtils.extractJsonObject(response);

        if (json == null) {
            throw new Exception("Failed to parse intent expansion JSON: " + response);
        }

        IntentExpansionResult result = new IntentExpansionResult();
        result.setOriginalPrompt(prompt);

        // Parse Intent Resolution fields
        String stateStr = json.optString("state", "CLEAR");
        result.setState(parseState(stateStr, context));

        result.setDominantIntent(json.optString("dominantIntent"));
        result.setDominantConfidence(json.optDouble("dominantConfidence", 0.5));
        result.setAmbiguityScore(json.optDouble("ambiguityScore", 0.0));
        result.setExecutionRiskScore(json.optDouble("executionRiskScore", 0.0));
        result.setEvolutionOpportunityScore(json.optDouble("evolutionOpportunityScore", 0.0));
        result.setImplementationStrategies(JsonUtils.toStringList(json.optJSONArray("implementationStrategies")));

        context.log("[INTENT EXPANSION] Interpretation State: " + result.getState());
        context.log("[INTENT EXPANSION] Dominant Intent: " + result.getDominantIntent());

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
        IntentConfidence c = new IntentConfidence();
        if (confObj != null) {
            c.setOverallConfidence(confObj.optDouble("overallConfidence", 0.5));
            c.setStructuralConfidence(confObj.optDouble("structuralConfidence", 0.5));
            c.setSemanticConfidence(confObj.optDouble("semanticConfidence", 0.5));
            c.setRationale(confObj.optString("rationale", "Inferred from content"));
        } else {
            c.setOverallConfidence(0.5);
            c.setRationale("No confidence data provided by AI");
        }
        result.setConfidence(c);

        // PERSISTENCE: Save clarification conclusions to Semantic Workspace
        persistClarifications(result, context);

        // DIAGNOSTICS: Emit causal node for intent expansion
        context.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
            "intent-expansion-" + System.currentTimeMillis(),
            "INTENT_EXPANSION",
            "IntentExpansionEngine",
            List.of(prompt),
            result.getHypotheses().stream().map(h -> h.getId()).collect(java.util.stream.Collectors.toList()),
            result.getConfidence().getOverallConfidence(),
            result.getConfidence().getRationale()
        ));

        return result;
    }

    public static InterpretationState parseState(String stateStr, TaskContext context) {
        if (stateStr == null) return InterpretationState.CLEAR;
        String cleanState = stateStr.trim().toUpperCase();

        // HARDENING: If the model echoes the whole enum list, it's probably actually CLEAR or EVOLVABLE
        if (cleanState.contains("|") || (cleanState.contains("CLEAR") && cleanState.contains("CONTRADICTORY"))) {
            if (context != null) {
                context.log("[INTENT EXPANSION] Detected enum list echo in state: " + stateStr + ". Defaulting to CLEAR.");
            }
            return InterpretationState.CLEAR;
        }

        try {
            return InterpretationState.valueOf(cleanState);
        } catch (IllegalArgumentException e) {
            // Robust fallback for noisy LLM output (e.g., "State: CLEAR")
            InterpretationState resolved = InterpretationState.CLEAR;
            for (InterpretationState s : InterpretationState.values()) {
                if (cleanState.contains(s.name())) {
                    resolved = s;
                    break;
                }
            }
            if (context != null) {
                context.log("[INTENT EXPANSION] WARNING: Noisy state string '" + stateStr + "' resolved to " + resolved);
            }
            return resolved;
        }
    }

    private void persistClarifications(IntentExpansionResult result, TaskContext context) {
        List<WorkspaceArtifact> existing = context.getSemanticWorkspace().findArtifactsByType("clarification-conclusion");

        for (IntentDimension dim : result.getDimensions()) {
            if (dim.getConfidence() > 0.7 && dim.getInferredValue() != null && !dim.getInferredValue().isEmpty()) {
                String newContent = "Intent clarified: " + dim.getName() + " is resolved to: " + dim.getInferredValue();

                boolean alreadyExists = existing.stream()
                        .anyMatch(a -> a.getContent() != null && a.getContent().equalsIgnoreCase(newContent));

                if (alreadyExists) {
                    continue;
                }

                String artifactId = "clarification-" + dim.getDimensionId() + "-" + System.currentTimeMillis();
                WorkspaceArtifact artifact = new WorkspaceArtifact(artifactId, "clarification-conclusion");
                artifact.setContent(newContent);
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
