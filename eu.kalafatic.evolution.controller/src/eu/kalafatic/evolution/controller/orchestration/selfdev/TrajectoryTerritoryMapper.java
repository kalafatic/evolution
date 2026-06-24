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
               "GOAL: Discover the NEXT UNEXPLORED DESIGN TERRITORY.\n" +
               "CORE DIRECTIVE: Do NOT improve existing territories. Do NOT rename existing ones. Return only genuinely NEW semantic territory.\n" +
               "Every candidate represents a distinct ARCHITECTURAL SPECIES. Sibling variants MUST differ significantly in their technical essence, not just wording.\n\n" +
               "SEARCH MEMORY MANDATE:\n" +
               "- You will be provided with currently explored territories.\n" +
               "- Your task is to identify a technical philosophy that has NOT been touched.\n\n" +
               "DIVERSITY EXAMPLES (Species Level):\n" +
               "- Minimalist OO: Direct, simple object-oriented approach.\n" +
               "- Functional Utility: Stateless, pure functions, often static.\n" +
               "- Reactive/Event-Driven: Decoupled via callbacks or streams.\n" +
               "- Dependency Injection: Focus on loose coupling and testability.\n" +
               "- Console/CLI Abstraction: Focus on terminal interaction and formatting.\n" +
               "- Strategy/Command Pattern: Focus on behavior encapsulation.\n" +
               "- Decorator/Pipeline: Focus on incremental processing.\n\n" +
               "STRICT EVOLUTION CONSTRAINTS:\n" +
               "- NO ARCHITECTURAL INFLATION: If the task is simple, DO NOT introduce unnecessary complexity. Discover MINIMAL implementation theories for distinct philosophies.\n" +
               "- AXIS DIVERGENCE: Intentionally pivot on [State], [Lifecycle], [Dependency Model], [API Style], [Execution Model], [Abstraction].\n" +
               "- GROUNDING: All blueprints MUST be descendants of the discovered Target Reality and hotspots.\n\n" +
               "MANDATORY: Return a blueprint that is 90% technical divergence from anything previously explored.";
    }

    public TrajectoryBlueprint discoverNext(String goal, TaskContext context, List<TrajectoryBlueprint> existing, String mutationContext, EvolutionDimension activeDimension) throws Exception {
        context.log("[TERRITORY] Sequentially discovering next unique evolutionary trajectory for: " + goal);

        AbstractionLevel lockedLevel = context.getOrchestrationState().getLockedAbstractionLevel();
        eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType capability = context.getExecutionProfile().getCapability();

        Object genomeObj = context.getOrchestrationState().getMetadata().get("semanticGenome");
        SemanticGenome genome = eu.kalafatic.evolution.controller.parsers.JsonUtils.restoreFromMetadata(genomeObj, SemanticGenome.class, "semanticGenome", context);

        eu.kalafatic.evolution.controller.orchestration.behavior.DarwinPromptBuilder builder =
            new eu.kalafatic.evolution.controller.orchestration.behavior.DarwinPromptBuilder(context);

        builder.addSystem("You are a single-path evolutionary territory mapper. You perform one controlled discovery of a unique evolutionary blueprint.")
               .addGoal(goal)
               .addMutationDimension(activeDimension)
               .addSemanticEnvelope()
               .addReality()
               .addLineage(mutationContext)
               .addGenomeMemory(genome);

        String diversityConstraint;
        if (capability == eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType.CHAT) {
            diversityConstraint = "Discover the NEXT unexplored CONVERSATIONAL territory. Focus on different tones, depths of response, or types of assistance (e.g., 'Concise & Professional', 'Friendly & Elaborate', 'Technical & Precise').";
        } else {
            diversityConstraint = "Discover the NEXT unexplored design territory. Do NOT improve existing ones. Do NOT rename existing ones. Return only genuinely new semantic territory.";
        }
        builder.addConstraints(diversityConstraint);

        String schema = "{\n" +
          "  \"id\": \"unique-blueprint-id\",\n" +
          "  \"strategy\": \"(Concise technical title, e.g., 'Asynchronous Logger Service'. NEVER use 'ROOT', 'create', or 'bootstrap')\",\n" +
          "  \"philosophy\": \"(Architectural core concept, e.g., 'Event-driven decoupling for performance')\",\n" +
          "  \"mutation_philosophy\": \"(Engineering style, e.g., 'Functional reactive')\",\n" +
          "  \"direction\": \"(Detailed implementation path)\",\n" +
          "  \"characteristics\": [\"Trait 1\", \"Trait 2\"],\n" +
          "  \"tradeoffs\": \"what is sacrificed\",\n" +
          "  \"survival_argument\": \"why this path is viable\",\n" +
          "  \"strategy_type\": \"(Choose EXACTLY ONE: PROBABLE_SURVIVOR, PHILOSOPHY_MAPPING, MAXIMAL_DIVERGENCE, SPECULATIVE_ARCHITECTURE)\"\n" +
          "}";
        builder.addJsonSchema(schema);
        builder.addConstraints("MANDATORY: Wrap your JSON response in <BEGIN_DARWIN_JSON> and <END_DARWIN_JSON> tags.\n" +
                               "MANDATORY: 'strategy' MUST be a specific architectural name. NEVER use generic placeholders like 'ROOT', 'create', 'bootstrap', 'ANALYZE', or 'EXECUTE'. Choose a name that reflects the technical species.");

        String directive = getAgentInstructions();
        if (lockedLevel != null) {
            directive += "\n[LOCKED_ABSTRACTION_LEVEL] You MUST operate strictly at the " + lockedLevel + " level.\n";
            if (lockedLevel == AbstractionLevel.IMPLEMENTATION) {
                directive += "- DO NOT propose new architectures or complex patterns.\n" +
                             "- Focus on concrete implementation variations of the SAME core design.";
            } else if (lockedLevel == AbstractionLevel.DESIGN) {
                directive += "- Focus on internal component design and API signatures.";
            }
        }
        builder.addExecutionDirective(directive);

        String response = aiService.sendRequest(context.getOrchestrator(), builder.build(), context);
        JSONObject obj = JsonUtils.extractJsonObject(response);

        if (obj != null) {
            String id = obj.optString("id");
            if (id == null || id.isEmpty()) {
                id = "bp-" + System.currentTimeMillis() + "-" + (existing.size() + 1);
            }
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

}
