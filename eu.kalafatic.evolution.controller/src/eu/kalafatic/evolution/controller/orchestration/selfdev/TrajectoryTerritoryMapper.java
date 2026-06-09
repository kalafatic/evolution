package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult;

/**
 * Dynamically maps the evolutionary territory to discover divergent blueprints.
 * Replaces hardcoded strategy selection with context-driven inference.
 */
public class TrajectoryTerritoryMapper extends BaseAiAgent {

    public TrajectoryTerritoryMapper(eu.kalafatic.evolution.controller.orchestration.SessionContainer container) {
        super("TerritoryMapper", "TerritoryMapper", container);
    }

    @Override
    protected String getAgentInstructions() {
        return "You are a Trajectory Territory Mapper. Your goal is to DISCOVER ONE UNIQUE EVOLUTIONARY BLUEPRINT.\n" +
               "Given a goal and system context, identify a divergent architectural direction.\n" +
               "Avoid hardcoded rules. Infer the best divergence axes (e.g., Performance vs. Resilience, Monolithic vs. Service).\n" +
               "MANDATORY: You MUST generate a blueprint that is CONCEPTUALLY DISTINCT from any provided existing blueprints. Focus on an unexplored technical quadrant.";
    }

    public List<TrajectoryBlueprint> map(String goal, TaskContext context, int limit) throws Exception {
        return mapSequential(goal, context, limit, new ArrayList<>());
    }

    public List<TrajectoryBlueprint> mapBulk(String goal, TaskContext context, int limit) throws Exception {
        context.log("[TERRITORY] Discovering " + limit + " divergent evolutionary trajectories for: " + goal);

        StringBuilder sb = new StringBuilder();
        sb.append("GOAL: ").append(goal).append("\n\n");

        String projectStructure = (String) context.getOrchestrationState().getMetadata().get("projectStructure");
        if (projectStructure != null) sb.append("STRUCTURE: ").append(projectStructure).append("\n");

        IntentExpansionResult expansion = (IntentExpansionResult) context.getMetadata().get("intentExpansion");
        if (expansion != null) {
            sb.append("INTENT: ").append(expansion.getDominantIntent()).append("\n");
        }

        String prompt = sb.toString() + "\n\n" +
               "Identify " + limit + " CONCEPTUALLY DISTINCT and COMPETING architectural directions.\n" +
               "Output exactly ONE JSON object containing a 'blueprints' array. Each blueprint MUST have:\n" +
               "- id: unique string\n" +
               "- strategy: concise title\n" +
               "- philosophy: architectural core\n" +
               "- direction: detailed technical path\n" +
               "- characteristics: array of required traits\n" +
               "- tradeoffs: what is sacrificed\n" +
               "- strategy_type: one of [PROBABLE_SURVIVOR, PHILOSOPHY_MUTATION, MAXIMAL_DIVERGENCE, STABILIZATION_RECOVERY, ARCHITECTURE_MAPPING, REFACTOR_HOTSPOT_ANALYSIS]";

        String response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
        JSONObject root = JsonUtils.extractJsonObject(response);
        List<TrajectoryBlueprint> blueprints = new ArrayList<>();

        if (root != null && root.has("blueprints")) {
            JSONArray arr = root.getJSONArray("blueprints");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                TrajectoryBlueprint bp = convertToBlueprint(obj, goal, blueprints.size());
                if (bp != null) blueprints.add(bp);
            }
        }

        return blueprints;
    }

    private TrajectoryBlueprint convertToBlueprint(JSONObject obj, String goal, int index) {
        if (obj == null) return null;
        String id = obj.optString("id");
        if (id == null || id.isEmpty()) {
            id = "bp-" + System.currentTimeMillis() + "-" + (index + 1);
        }
        TrajectoryBlueprint bp = new TrajectoryBlueprint(id, goal, obj.optString("strategy"));
        bp.setPhilosophy(obj.optString("philosophy"));
        bp.setArchitecturalDirection(obj.optString("direction"));
        bp.setSurvivalArgument(obj.optString("survival_argument", obj.optString("philosophy")));
        bp.setTradeoffs(obj.optString("tradeoffs"));

        String typeStr = obj.optString("strategy_type", "PROBABLE_SURVIVOR");
        try {
            bp.setStrategyType(DarwinStrategyType.valueOf(typeStr.toUpperCase()));
        } catch (Exception e) {
            bp.setStrategyType(DarwinStrategyType.PROBABLE_SURVIVOR);
        }

        JSONArray chars = obj.optJSONArray("characteristics");
        if (chars != null) {
            for (int j = 0; j < chars.length(); j++) bp.addRequiredCharacteristic(chars.getString(j));
        }
        return bp;
    }

    public TrajectoryBlueprint discoverNext(String goal, TaskContext context, List<TrajectoryBlueprint> existing) throws Exception {
        context.log("[TERRITORY] Sequentially discovering next unique evolutionary trajectory for: " + goal);

        StringBuilder sb = new StringBuilder();
        sb.append("GOAL: ").append(goal).append("\n\n");

        String projectStructure = (String) context.getOrchestrationState().getMetadata().get("projectStructure");
        if (projectStructure != null) sb.append("STRUCTURE: ").append(projectStructure).append("\n");

        IntentExpansionResult expansion = (IntentExpansionResult) context.getMetadata().get("intentExpansion");
        if (expansion != null) {
            sb.append("INTENT: ").append(expansion.getDominantIntent()).append("\n");
        }

        if (!existing.isEmpty()) {
            sb.append("\nEXISTING BLUEPRINTS (DO NOT REPEAT OR OVERLAP):\n");
            for (TrajectoryBlueprint bp : existing) {
                sb.append("- ").append(bp.getStrategyType()).append(": ").append(bp.getPhilosophy()).append("\n");
            }
        }

        String prompt = sb.toString() + "\n\n" +
               "Output exactly ONE JSON object for a unique blueprint. The object MUST have:\n" +
               "- id: unique string\n" +
               "- strategy: concise title\n" +
               "- philosophy: architectural core\n" +
               "- direction: detailed technical path\n" +
               "- characteristics: array of required traits\n" +
               "- tradeoffs: what is sacrificed\n" +
               "- strategy_type: one of [PROBABLE_SURVIVOR, PHILOSOPHY_MUTATION, MAXIMAL_DIVERGENCE, STABILIZATION_RECOVERY, ARCHITECTURE_MAPPING, REFACTOR_HOTSPOT_ANALYSIS]";

        String response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
        JSONObject obj = JsonUtils.extractJsonObject(response);
        return convertToBlueprint(obj, goal, existing.size());
    }

    @Deprecated
    public List<TrajectoryBlueprint> mapSequential(String goal, TaskContext context, int limit, List<TrajectoryBlueprint> existing) throws Exception {
        // This is now handled by DarwinEngine's sequential loop
        return existing;
    }
}
