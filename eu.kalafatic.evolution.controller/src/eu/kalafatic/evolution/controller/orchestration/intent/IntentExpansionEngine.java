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
        return "You are an Intent Expansion Engine. Your goal is to analyze user requests for ambiguity and explore the 'intent space' before implementation begins.\n" +
               "Identify unresolved dimensions of intent and generate coherent hypotheses for what the user might want.\n" +
               "\n" +
               "CRITICAL: CONSTRUCT THE EVOLUTIONARY SEARCH SPACE.\n" +
               "Analyze implementation dimensions, ambiguity points, architectural axes, and extensibility potential.\n" +
               "\n" +
               "PHASE 1 - INTENT DECOMPOSITION:\n" +
               "Identify 'Evolutionary Axes' - dimensions where implementation choices exist.\n" +
               "For each axis, identify potential candidate strategies/blueprints.\n" +
               "\n" +
               "CRITICAL DISTINCTION:\n" +
               "1. SEMANTIC AMBIGUITY: Missing critical information or contradictory constraints that prevent safe execution.\n" +
               "2. IMPLEMENTATION POLYMORPHISM (EVOLUTIONARY AXES): Multiple valid ways to implement a clear intent.\n" +
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
               "  \"state\": \"CLEAR\", // [CLEAR, EVOLVABLE, NEEDS_CLARIFICATION, BLOCKED, CONTRADICTORY]\n" +
               "  \"dominantIntent\": \"string\",\n" +
               "  \"dominantConfidence\": float,\n" +
               "  \"evolutionaryAxes\": [\n" +
               "    {\n" +
               "      \"name\": \"string (e.g., Output Strategy)\",\n" +
               "      \"description\": \"string\",\n" +
               "      \"candidateBlueprints\": [\n" +
               "        {\n" +
               "          \"id\": \"string\",\n" +
               "          \"goal\": \"string\",\n" +
               "          \"philosophy\": \"string\",\n" +
               "          \"requiredCharacteristics\": [\"string\"],\n" +
               "          \"forbiddenOverlaps\": [\"string\"]\n" +
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
        context.log("[INTENT EXPANSION] Analyzing intent space for: " + prompt);

        // FAST BYPASS: Detect direct variant selection or force solution commands
        String lower = prompt.toLowerCase().trim();
        if (lower.startsWith("select ") || lower.startsWith("approve variant ") || lower.equalsIgnoreCase("force solution")) {
            context.log("[INTENT EXPANSION] Detected bypass command: " + lower + ". Bypassing LLM expansion.");
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
            context.log("[INTENT EXPANSION] Found " + priorConclusions.size() + " prior clarifications in workspace.");
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

        context.log("[INTENT EXPANSION] Interpretation State: " + result.getState());
        context.log("[INTENT EXPANSION] Dominant Intent: " + result.getDominantIntent());

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
                context.log("[INTENT EXPANSION] Detected enum list echo in state: " + stateStr + ". Defaulting to CLEAR.");
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
                context.log("[INTENT EXPANSION] WARNING: Noisy state string '" + stateStr + "' resolved to " + resolved);
            }
            return resolved;
        }
    }

}
