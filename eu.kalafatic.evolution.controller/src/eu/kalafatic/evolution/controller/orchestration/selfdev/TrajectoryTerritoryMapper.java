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
        return "You are a Trajectory Territory Mapper. Your goal is to DISCOVER COMPETING EVOLUTIONARY BLUEPRINTS.\n" +
               "Given a goal and system context, identify divergent architectural directions.\n" +
               "Avoid hardcoded rules. Infer the best divergence axes (e.g., Performance vs. Resilience, Monolithic vs. Service).\n" +
               "Generate 4-6 unique blueprints that explore different technical futures.";
    }

    public List<TrajectoryBlueprint> map(String goal, TaskContext context, int limit) throws Exception {
        context.log("[TERRITORY] Dynamically mapping evolutionary trajectories for: " + goal);

        StringBuilder sb = new StringBuilder();
        sb.append("GOAL: ").append(goal).append("\n\n");

        String projectStructure = (String) context.getOrchestrationState().getMetadata().get("projectStructure");
        if (projectStructure != null) sb.append("STRUCTURE: ").append(projectStructure).append("\n");

        IntentExpansionResult expansion = (IntentExpansionResult) context.getMetadata().get("intentExpansion");
        if (expansion != null) {
            sb.append("INTENT: ").append(expansion.getDominantIntent()).append("\n");
        }

        String prompt = sb.toString() + "\n\n" +
               "Output exactly ONE JSON array of blueprint objects. Each object MUST have:\n" +
               "- id: unique string\n" +
               "- strategy: concise title\n" +
               "- philosophy: architectural core\n" +
               "- direction: detailed technical path\n" +
               "- characteristics: array of required traits\n" +
               "- tradeoffs: what is sacrificed\n" +
               "- strategy_type: one of [PROBABLE_SURVIVOR, PHILOSOPHY_MUTATION, MAXIMAL_DIVERGENCE, STABILIZATION_RECOVERY, ARCHITECTURE_MAPPING, REFACTOR_HOTSPOT_ANALYSIS]";

        String response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
        JSONArray array = JsonUtils.extractJsonArrayFlexible(response);

        List<TrajectoryBlueprint> blueprints = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < Math.min(array.length(), limit); i++) {
                JSONObject obj = array.getJSONObject(i);
                TrajectoryBlueprint bp = new TrajectoryBlueprint(obj.optString("id"), goal, obj.optString("strategy"));
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
                blueprints.add(bp);
            }
        }

        return blueprints;
    }
}
