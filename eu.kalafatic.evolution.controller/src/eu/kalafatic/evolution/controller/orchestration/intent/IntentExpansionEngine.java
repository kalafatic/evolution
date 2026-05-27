package eu.kalafatic.evolution.controller.orchestration.intent;

import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;
import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceArtifact;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionAxis;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TrajectoryBlueprint;
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
        return "You are an Intent Expansion Engine. Your goal is to analyze user requests and CONSTRUCT THE EVOLUTIONARY SEARCH SPACE.\n" +
               "Identify implementation polymorphism - multiple valid ways to implement the intent.\n" +
               "\n" +
               "PHASE 1 - INTENT DECOMPOSITION:\n" +
               "Derive engineering dimensions before branch spawning. Analyze the following 9 dimensions:\n" +
               "1. Engineering Philosophy\n" +
               "2. Execution Model\n" +
               "3. Abstraction Depth\n" +
               "4. Modularity Approach\n" +
               "5. Testing Strategy\n" +
               "6. Extensibility\n" +
               "7. Dependency Assumptions\n" +
               "8. Runtime Behavior\n" +
               "9. Risk Acceptance\n" +
               "\n" +
               "PHASE 2 - AXIS DETECTION:\n" +
               "Identify 'Evolutionary Axes' where critical architectural choices must be made.\n" +
               "For each axis, you MUST define deterministic 'Candidate Blueprints'.\n" +
               "\n" +
               "BLUEPRINT RULES:\n" +
               "- Each blueprint MUST be architecturally distinct.\n" +
               "- Blueprints MUST contain 'requiredCharacteristics' (technical requirements).\n" +
               "- Blueprints MUST contain 'forbiddenOverlaps' (technical constraints to ensure divergence).\n" +
               "\n" +
               "STRICT RULES:\n" +
               "1. Do NOT generate code or tasks.\n" +
               "2. Focus on structural and behavioral assumptions.\n" +
               "3. Output MUST be ONLY valid JSON.";
    }

    @Override
    protected String getFooterInstructions() {
        return "OUTPUT SCHEMA:\n" +
               "{\n" +
               "  \"state\": \"CLEAR\", // [CLEAR, EVOLVABLE, NEEDS_CLARIFICATION, BLOCKED, CONTRADICTORY]\n" +
               "  \"dominantIntent\": \"string\",\n" +
               "  \"dominantConfidence\": float,\n" +
               "  \"engineeringDimensions\": {\n" +
               "    \"philosophy\": \"string\",\n" +
               "    \"execution_model\": \"string\",\n" +
               "    \"abstraction_depth\": \"string\",\n" +
               "    \"modularity_approach\": \"string\",\n" +
               "    \"testing_strategy\": \"string\",\n" +
               "    \"extensibility\": \"string\",\n" +
               "    \"dependency_assumptions\": \"string\",\n" +
               "    \"runtime_behavior\": \"string\",\n" +
               "    \"risk_acceptance\": \"string\"\n" +
               "  },\n" +
               "  \"evolutionaryAxes\": [\n" +
               "    {\n" +
               "      \"name\": \"string (e.g., Output Philosophy)\",\n" +
               "      \"description\": \"string\",\n" +
               "      \"candidateBlueprints\": [\n" +
               "        {\n" +
               "          \"id\": \"string (e.g., direct_console, file_persistent)\",\n" +
               "          \"goal\": \"string\",\n" +
               "          \"philosophy\": \"string\",\n" +
               "          \"requiredCharacteristics\": [\"string\"],\n" +
               "          \"forbiddenOverlaps\": [\"string\"],\n" +
               "          \"architecturalDirection\": \"string\"\n" +
               "        }\n" +
               "      ]\n" +
               "    }\n" +
               "  ],\n" +
               "  \"confidence\": {\n" +
               "    \"overallConfidence\": float,\n" +
               "    \"rationale\": \"string\"\n" +
               "  }\n" +
               "}";
    }

    public IntentExpansionResult expand(String prompt, TaskContext context) throws Exception {
        context.consoleLog("[INTENT EXPANSION] Analyzing intent space for: " + prompt);

        // FAST BYPASS: Detect direct variant selection or force solution commands
        String lower = prompt.toLowerCase().trim();
        if (lower.startsWith("select ") || lower.startsWith("approve variant ") || lower.equalsIgnoreCase("force solution")) {
            context.consoleLog("[INTENT EXPANSION] Detected bypass command: " + lower + ". Bypassing LLM expansion.");
            IntentExpansionResult result = new IntentExpansionResult();
            result.setOriginalPrompt(prompt);
            result.setState(InterpretationState.CLEAR);
            result.setDominantIntent(prompt);
            result.setDominantConfidence(1.0);

            IntentConfidence c = new IntentConfidence();
            c.setOverallConfidence(1.0);
            c.setRationale("Bypass command triggered: " + lower);
            result.setConfidence(c);
            return result;
        }

        // PERSISTENCE: Check Semantic Workspace for prior resolutions
        List<WorkspaceArtifact> priorConclusions = context.getSemanticWorkspace().findArtifactsByType("clarification-conclusion");
        String priorContext = "";
        if (!priorConclusions.isEmpty()) {
            StringBuilder sb = new StringBuilder("\nPRIOR CLARIFICATIONS:\n");
            for (WorkspaceArtifact a : priorConclusions) {
                sb.append("- ").append(a.getContent()).append("\n");
            }
            priorContext = sb.toString();
            context.consoleLog("[INTENT EXPANSION] Found " + priorConclusions.size() + " prior clarifications in workspace.");
        }

        String systemPrompt = getAgentInstructions() + "\n\n" + getFooterInstructions();
        String userPrompt = "Analyze the following user request and expand the intent space:\n\n" + prompt + priorContext;

        String response = aiService.sendRequest(context.getOrchestrator(), systemPrompt + "\n\n" + userPrompt, context);
        JSONObject json = JsonUtils.extractJsonObject(response);

        if (json == null) {
            throw new Exception("Failed to parse intent expansion JSON: " + response);
        }

        context.log("[DARWIN_BRANCHES] " + json.toString());

        IntentExpansionResult result = new IntentExpansionResult();
        result.setOriginalPrompt(prompt);

        // Parse Intent Resolution fields
        String stateStr = json.optString("state", "CLEAR");
        result.setState(parseState(stateStr, context));

        result.setDominantIntent(json.optString("dominantIntent"));
        result.setDominantConfidence(json.optDouble("dominantConfidence", 0.5));

        // Parse Engineering Dimensions
        JSONObject dimensions = json.optJSONObject("engineeringDimensions");
        if (dimensions != null) {
            java.util.Map<String, Object> dimMap = new java.util.HashMap<>();
            java.util.Iterator<String> keys = dimensions.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                dimMap.put(key, dimensions.get(key));
            }
            context.getOrchestrationState().getMetadata().put("engineeringDimensions", dimMap);
            context.consoleLog("[INTENT EXPANSION] Derived 9 engineering dimensions.");
        }

        context.consoleLog("[INTENT EXPANSION] Interpretation State: " + result.getState());
        context.consoleLog("[INTENT EXPANSION] Dominant Intent: " + result.getDominantIntent());

        // Parse Evolutionary Axes
        JSONArray axes = json.optJSONArray("evolutionaryAxes");
        if (axes != null) {
            for (int i = 0; i < axes.length(); i++) {
                JSONObject axisObj = axes.getJSONObject(i);
                EvolutionAxis axis = new EvolutionAxis(axisObj.optString("name"), axisObj.optString("description"));

                JSONArray blueprints = axisObj.optJSONArray("candidateBlueprints");
                if (blueprints != null) {
                    for (int j = 0; j < blueprints.length(); j++) {
                        JSONObject bpObj = blueprints.getJSONObject(j);
                        TrajectoryBlueprint bp = new TrajectoryBlueprint(bpObj.optString("id"), bpObj.optString("goal"), bpObj.optString("philosophy"));

                        bp.getRequiredCharacteristics().addAll(JsonUtils.toStringList(bpObj.optJSONArray("requiredCharacteristics")));
                        bp.getForbiddenOverlaps().addAll(JsonUtils.toStringList(bpObj.optJSONArray("forbiddenOverlaps")));
                        bp.setArchitecturalDirection(bpObj.optString("architecturalDirection"));
                        axis.addBlueprint(bp);
                    }
                }
                result.getEvolutionaryAxes().add(axis);
            }
        }

        // Parse Confidence
        JSONObject confObj = json.optJSONObject("confidence");
        IntentConfidence c = new IntentConfidence();
        if (confObj != null) {
            c.setOverallConfidence(confObj.optDouble("overallConfidence", 0.5));
            c.setRationale(confObj.optString("rationale", "Inferred from content"));
        } else {
            c.setOverallConfidence(0.5);
            c.setRationale("No confidence data provided by AI");
        }
        result.setConfidence(c);

        // DIAGNOSTICS: Emit causal node for intent expansion
        context.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
            "intent-expansion-" + System.currentTimeMillis(),
            "INTENT_EXPANSION",
            "IntentExpansionEngine",
            List.of(prompt),
            result.getEvolutionaryAxes().stream().map(a -> a.getName()).collect(java.util.stream.Collectors.toList()),
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
                context.consoleLog("[INTENT EXPANSION] Detected enum list echo in state: " + stateStr + ". Defaulting to CLEAR.");
            }
            // If it contains BLOCKED but also others, it might be BLOCKED.
            // But if it's the full list, it's a hallucination.
            if (cleanState.contains("BLOCKED") && !cleanState.contains("CLEAR")) {
                return InterpretationState.BLOCKED;
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
                context.consoleLog("[INTENT EXPANSION] WARNING: Noisy state string '" + stateStr + "' resolved to " + resolved);
            }
            return resolved;
        }
    }

}
