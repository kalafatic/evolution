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
        return "You are a Trajectory Territory Mapper (STABILIZATION LAYER).\n\n" +
               "GOAL: Discover ONE UNIQUE EVOLUTIONARY BLUEPRINT.\n" +
               "CORE DIRECTIVE: Every candidate represents a distinct ARCHITECTURAL THEORY. Diversity must be STRUCTURAL (execution model, dependency model, control flow), not cosmetic (naming).\n\n" +
               "STRICT EVOLUTION CONSTRAINTS:\n" +
               "- NO ARCHITECTURAL INFLATION: For trivial tasks, discover MINIMAL implementation theories.\n" +
               "- AXIS DIVERGENCE: Intentionally pivot on [Sync vs Async], [Direct vs Abstracted], [Linear vs Modular].\n" +
               "- GROUNDING: All blueprints MUST be descendants of the discovered Target Reality and hotspots.\n\n" +
               "MANDATORY: You MUST generate a blueprint that is CONCEPTUALLY DISTINCT from any provided existing blueprints. Focus on an unexplored technical quadrant of the target reality.\n" +
               "TECHNICAL SPECIFICITY: Blueprints MUST contain specific technical mechanisms and patterns. Avoid generic descriptions.";
    }

    public List<TrajectoryBlueprint> map(String goal, TaskContext context, int limit) throws Exception {
        return mapSequential(goal, context, limit, new ArrayList<>());
    }

    public TrajectoryBlueprint discoverNext(String goal, TaskContext context, List<TrajectoryBlueprint> existing, String mutationContext) throws Exception {
        context.log("[TERRITORY] Sequentially discovering next unique evolutionary trajectory for: " + goal);

        StringBuilder sb = new StringBuilder();
        sb.append("🔴 SYSTEM / ROLE\n\n")
          .append("You are a single-path evolutionary territory mapper.\n\n")
          .append("You do NOT generate multiple solutions.\n")
          .append("You perform one controlled discovery of a unique evolutionary blueprint.\n\n")
          .append("🧠 CORE RULE\n\n")
          .append("Each response MUST contain exactly ONE blueprint only.\n")
          .append("This blueprint must be structurally distinct from all previously seen ones.\n\n")
          .append("📌 INPUT CONTEXT\n\n")
          .append("GOAL (target task): ").append(goal).append("\n\n");

        String projectStructure = (String) context.getOrchestrationState().getMetadata().get("projectStructure");
        if (projectStructure != null) {
            sb.append("WORKSPACE STRUCTURE:\n").append(projectStructure).append("\n\n");
        }

        eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel realityModel = (eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel) context.getOrchestrationState().getMetadata().get("targetRealityModel");
        if (realityModel != null) {
            sb.append("TARGET REALITY GROUNDING:\n")
              .append("Domain: ").append(realityModel.getDomain()).append("\n")
              .append("Hotspots: ").append(realityModel.getHotspots().stream().map(h -> h.getName()).collect(java.util.stream.Collectors.joining(", "))).append("\n\n");
        }

        sb.append("PREVIOUS BLUEPRINTS (FORBIDDEN):\n");
        if (!existing.isEmpty()) {
            for (TrajectoryBlueprint bp : existing) {
                sb.append("- ").append(bp.getStrategy()).append(" (Philosophy: ").append(bp.getPhilosophy()).append(")\n");
            }
        } else {
            sb.append("- None\n");
        }

        if (mutationContext != null && !mutationContext.isEmpty()) {
            sb.append("\nSEQUENTIAL MUTATION CONSTRAINTS (FORBIDDEN):\n")
              .append(mutationContext).append("\n");
        }

        sb.append("\n🔥 DIVERGENCE REQUIREMENT\n\n")
          .append("Your blueprint MUST intentionally diverge from prior ones in philosophy and execution model.\n\n")
          .append("🎯 OUTPUT FORMAT (STRICT)\n\n")
          .append("Return ONLY one JSON object:\n\n")
          .append("{\n")
          .append("  \"id\": \"unique-blueprint-id\",\n")
          .append("  \"strategy\": \"(Concise title for this path)\",\n")
          .append("  \"philosophy\": \"(Architectural core concept)\",\n")
          .append("  \"direction\": \"(Detailed technical implementation path: SPECIFIC patterns or components)\",\n")
          .append("  \"characteristics\": [\"Required Trait 1\", \"Required Trait 2\"],\n")
          .append("  \"tradeoffs\": \"what is sacrificed\",\n")
          .append("  \"survival_argument\": \"why this path is technically viable\",\n")
          .append("  \"strategy_type\": \"PROBABLE_SURVIVOR | PHILOSOPHY_MUTATION | MAXIMAL_DIVERGENCE | STABILIZATION_RECOVERY | ARCHITECTURE_MAPPING | REFACTOR_HOTSPOT_ANALYSIS\"\n")
          .append("}\n\n")
          .append("⚠️ CRITICAL OUTPUT CONSTRAINT\n")
          .append("ONLY ONE JSON OBJECT\n")
          .append("NO explanation outside JSON\n")
          .append("NO markdown\n\n")
          .append("CONTEXT:\n")
          .append(getAgentInstructions());

        String response = aiService.sendRequest(context.getOrchestrator(), sb.toString(), context);
        JSONObject obj = JsonUtils.extractJsonObject(response);

        if (obj != null) {
            String id = obj.optString("id");
            if (id == null || id.isEmpty()) {
                id = "bp-" + System.currentTimeMillis() + "-" + (existing.size() + 1);
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

        return null;
    }

    @Deprecated
    public List<TrajectoryBlueprint> mapSequential(String goal, TaskContext context, int limit, List<TrajectoryBlueprint> existing) throws Exception {
        // This is now handled by DarwinEngine's sequential loop
        return existing;
    }
}
