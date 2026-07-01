package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;
import eu.kalafatic.evolution.controller.orchestration.goal.SemanticEnvelope;
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
               "GOAL: Generate ONE additional sibling for the current generation.\n" +
               "DIMENSION MANDATE:\n" +
               "Your task is to generate a sibling that competes EXCLUSIVELY on the active mutation dimension.\n" +
               "ALL other architectural decisions must remain identical to the parent or common lineage.\n\n" +
               "RULES:\n" +
               "1. Same parent.\n" +
               "2. Same semantic goal.\n" +
               "3. Same abstraction level.\n" +
               "4. Different mutation than every existing sibling.\n" +
               "5. Do NOT regenerate any existing mutation.\n" +
               "6. USE HISTORY: Review grouped history by dimension. If a dimension was previously explored, ensure this new sibling provides a NEW valid branch within that dimension that hasn't been tried, or builds upon previous successes.\n\n" +
               "TASK:\n" +
               "Generate another technical philosophy for the ACTIVE DIMENSION that has NOT been touched.\n" +
               "MANDATORY: 'strategy' MUST be a specific technical architectural name. NEVER use generic placeholders like 'ROOT', 'create', 'bootstrap', 'ANALYZE', or 'EXECUTE'.\n" +
               "EXAMPLES OF GOOD STRATEGY NAMES:\n" +
               "- Asynchronous Buffer Service\n" +
               "- Functional Transformation Pipeline\n" +
               "- Reactive Event Dispatcher\n" +
               "- Stateless Utility Pattern\n" +
               "- Interface-Driven Strategy\n\n" +
               "STRICT EVOLUTION CONSTRAINTS:\n" +
               "- NO ARCHITECTURAL INFLATION: If the task is simple, DO NOT introduce unnecessary complexity.\n" +
               "- GROUNDING: All blueprints MUST be descendants of the discovered Target Reality and hotspots.";
    }

    public TrajectoryBlueprint discoverNextSibling(SiblingGenerationContext ctx, TaskContext context) throws Exception {
        String goal = ctx.getGoal().getPrimaryAction();
        context.log("[TERRITORY] Sequentially discovering sibling #" + (ctx.getSiblingIndex() + 1) + " for: " + goal);

        AbstractionLevel lockedLevel = context.getOrchestrationState().getLockedAbstractionLevel();
        eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType capability = context.getExecutionProfile().getCapability();

        Object genomeObj = context.getOrchestrationState().getMetadata().get("semanticGenome");
        SemanticGenome genome = eu.kalafatic.evolution.controller.parsers.JsonUtils.restoreFromMetadata(genomeObj, SemanticGenome.class, "semanticGenome", context);

        eu.kalafatic.evolution.controller.orchestration.behavior.DarwinPromptBuilder builder =
            new eu.kalafatic.evolution.controller.orchestration.behavior.DarwinPromptBuilder(context);

        builder.addSystem("You are a deterministic evolutionary territory mapper. Your task is to solve a constraint-satisfaction problem to find the next valid sibling.")
               .addGoal(goal)
               .addMutationDimension(ctx.getDimension())
               .addSemanticEnvelope()
               .addReality()
               .addGenomeMemory(genome);

        // Add lineage from tree if available
        if (ctx.getTree() != null && ctx.getCurrentParentId() != null) {
            String lineage = ctx.getTree().reconstructLineagePrompt(ctx.getCurrentParentId());
            if (lineage != null && !lineage.isEmpty()) {
                builder.addLineage(lineage);
            }
        }

        // Add Parent info
        if (ctx.getParent() != null) {
            builder.addConstraints("PARENT:\n" + ctx.getParent().getStrategy() + " (" + ctx.getParent().getPhilosophy() + ")");
        }

        // Add Existing Siblings as exclusion constraints
        if (!ctx.getOlderSiblings().isEmpty()) {
            StringBuilder siblingsStr = new StringBuilder("EXISTING SIBLINGS (DO NOT REPEAT):\n");
            for (TrajectoryBlueprint sibling : ctx.getOlderSiblings()) {
                siblingsStr.append("- ").append(sibling.getStrategy()).append(" (Mutation: ").append(sibling.getPhilosophy()).append(")\n");
            }
            builder.addConstraints(siblingsStr.toString());
        }

        String taskDirective = "TASK: Generate ONE additional sibling.\n" +
                               "Rule: mutation MUST NOT equal any previous sibling.\n" +
                               "Complete the sibling population for dimension: " + ctx.getDimension().getId() + " (" + ctx.getDimension().getSemanticDomain() + ")\n" +
                               "MANDATE: The sibling MUST vary ONLY on this dimension. Do NOT introduce variations in other regions of the architecture.";
        builder.addConstraints(taskDirective);

        String schema = "{\n" +
          "  \"id\": \"unique-blueprint-id\",\n" +
          "  \"strategy\": \"(Concise technical title, e.g., 'Asynchronous Logger Service')\",\n" +
          "  \"philosophy\": \"(Architectural core concept, e.g., 'Event-driven decoupling for performance')\",\n" +
          "  \"mutation_philosophy\": \"(Engineering style, e.g., 'Functional reactive')\",\n" +
          "  \"direction\": \"(Detailed implementation path)\",\n" +
          "  \"characteristics\": [\"Trait 1\", \"Trait 2\"],\n" +
          "  \"tradeoffs\": \"what is sacrificed\",\n" +
          "  \"survival_argument\": \"why this path is viable\",\n" +
          "  \"strategy_type\": \"(Choose EXACTLY ONE: PROBABLE_SURVIVOR, PHILOSOPHY_MAPPING, MAXIMAL_DIVERGENCE, SPECULATIVE_ARCHITECTURE)\"\n" +
          "}";
        builder.addJsonSchema(schema);

        String directive = getAgentInstructions();
        if (lockedLevel != null) {
            directive += "\n[LOCKED_ABSTRACTION_LEVEL] You MUST operate strictly at the " + lockedLevel + " level.\n";
        }
        builder.addExecutionDirective(directive);

        String response = aiService.sendRequest(context.getOrchestrator(), builder.build(), context);
        JSONObject obj = JsonUtils.extractJsonObject(response);

        if (obj != null) {
            String id = "bp-iter" + context.getOrchestrationState().getIterationCount() + "-s" + ctx.getSiblingIndex() + "-" + System.currentTimeMillis();
            TrajectoryBlueprint bp = new TrajectoryBlueprint(id, goal, obj.optString("strategy"));
            bp.setPhilosophy(obj.optString("philosophy"));
            bp.setMutationPhilosophy(obj.optString("mutation_philosophy"));
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
    public TrajectoryBlueprint discoverNext(String goal, TaskContext context, List<TrajectoryBlueprint> existing, String mutationContext, EvolutionDimension activeDimension) throws Exception {
        return null; // Implementation replaced by discoverNextSibling
    }

}
