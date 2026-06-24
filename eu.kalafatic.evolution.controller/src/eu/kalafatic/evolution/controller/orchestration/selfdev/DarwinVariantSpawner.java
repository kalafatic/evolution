package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.EvolutionProgressPublisher;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.behavior.DarwinPromptBuilder;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.goal.SemanticEnvelope;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Spawner for Darwin evolutionary branch variants.
 * Executes isolated generation requests for each strategy seed.
 */
public class DarwinVariantSpawner {
    private final AiService aiService;
    private final DarwinVariantValidator validator;

    public DarwinVariantSpawner(AiService aiService) {
        this.aiService = aiService;
        this.validator = new DarwinVariantValidator();
    }

    /**
     * Materializes a single variant based on a blueprint.
     * Pure materialization: no retries, no repair, no planning.
     */
    public JSONObject spawnSingleBlueprint(GoalModel goal, TrajectoryBlueprint bp, String basePrompt, String lineageContext, List<String> rejectedSiblings, String mutationContext, boolean isMediated, TaskContext context, EvolutionDimension activeDimension, SemanticGenome genome) {
        Orchestrator orchestrator = context.getOrchestrator();

        context.log("[SPAWNER] Materializing trajectory from blueprint: " + bp.getId());

        String bpPrompt = buildBlueprintPrompt(bp, basePrompt, lineageContext, rejectedSiblings, mutationContext, isMediated, context, activeDimension, genome);

        try {
            String response = aiService.sendRequest(orchestrator, bpPrompt, context);
            return validator.validate(response, bp.getStrategyType(), context);
        } catch (Exception e) {
            context.log("[SPAWNER] Error during blueprint materialization for " + bp.getId() + ": " + e.getMessage());
            return null;
        }
    }

    public JSONObject autoRepair(TrajectoryBlueprint bp, TaskContext context) {
        // Core Principle: Evolution should create new organisms, not repair dead ones.
        // Synthetic healing causes semantic drift.
        context.log("[SPAWNER] Materialization failed for " + bp.getId() + ". Skipping synthetic auto-repair to prevent semantic drift.");
        return null;
    }

    private String buildBlueprintPrompt(TrajectoryBlueprint bp, String basePrompt, String lineageContext, List<String> rejectedSiblings, String mutationContext, boolean isMediated, TaskContext context, EvolutionDimension activeDimension, SemanticGenome genome) {
        eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType capability = context.getExecutionProfile().getCapability();

        DarwinPromptBuilder builder = new DarwinPromptBuilder(context);

        String role;
        if (capability == eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType.CHAT) {
            role = "You are a conversational evolutionary mutation engine.\n" +
                   "Your goal is to perform a BOUNDED LOCAL MUTATION on the conversational response.\n" +
                   "MANDATE: You are ONLY allowed to mutate the specific dimension (tone, depth, etc.) identified below.\n" +
                   "Each response MUST contain exactly ONE branch only.";
        } else {
            role = "You are a single-path evolutionary mutation engine.\n" +
                   "Your goal is to perform a BOUNDED LOCAL MUTATION on the provided parent implementation.\n" +
                   "MANDATE: You MUST preserve the parent implementation. You are ONLY allowed to mutate the specific dimension identified below.\n" +
                   "Do NOT redesign the complete architecture. Do NOT start from scratch. Focus only on the active mutation dimension.\n" +
                   "Each response MUST contain exactly ONE branch only.";
        }

        builder.addSystem(role)
               .addGoal(bp.getGoal())
               .addSemanticEnvelope()
               .addLineage(lineageContext)
               .addGenomeMemory(genome)
               .addReality()
               .addMutationDimension(activeDimension);

        StringBuilder constraintSb = new StringBuilder();
        constraintSb.append("DIVERGENCE REQUIREMENT (CRITICAL):\n")
          .append("Your solution MUST intentionally diverge from prior branches. Pivot sharply on philosophy, execution model, and control flow.\n\n")
          .append("STABILIZATION CONSTRAINTS:\n")
          .append("- NO ARCHITECTURAL INFLATION: If task is trivial, provide a MINIMAL implementation.\n")
          .append("- MINIMUM VIABLE SOLUTION BIAS: Prefer the simplest code that satisfies the goal.\n")
          .append("- GROUNDING: Use ONLY the provided Target Reality Model and Hotspots.\n\n")
          .append("BLUEPRINT CONSTRAINTS:\n")
          .append("- ID: ").append(bp.getId()).append("\n")
          .append("- Philosophy: ").append(bp.getPhilosophy()).append("\n")
          .append("- Mutation Philosophy (ENGINEERING STYLE): ").append(bp.getMutationPhilosophy()).append("\n")
          .append("- Architectural Direction: ").append(bp.getArchitecturalDirection()).append("\n");

        builder.addConstraints(constraintSb.toString());

        StringBuilder schemaSb = new StringBuilder();
        schemaSb.append("{\n")
          .append("  \"id\": \"").append(bp.getId()).append("\",\n")
          .append("  \"strategy\": \"(Specific title, e.g. 'Standard Console Printer'. DO NOT USE 'ROOT' or 'create')\",\n")
          .append("  \"strategy_type\": \"").append(bp.getStrategyType().name()).append("\",\n")
          .append("  \"mutation_philosophy\": \"").append(bp.getMutationPhilosophy()).append("\",\n")
          .append("  \"semantic_anchor\": \"").append(bp.getPhilosophy()).append("\",\n")
          .append("  \"survival_argument\": \"why this branch is better\",\n")
          .append("  \"tradeoffs\": \"what is sacrificed\",\n")
          .append("  \"failure_risks\": \"how it might fail\",\n")
          .append("  \"expected_outputs\": [\"stdout message\"],\n")
          .append("  \"reasoning_focus\": \"priority\",\n")
          .append("  \"projected_steps\": [\"Implement class\", \"Add main method\"],\n")
          .append("  \"mutation_journal\": [\"Applied philosophy\"],\n")
          .append("  \"engineering_dimensions\": {\n")
          .append("    \"philosophy\": \"").append(bp.getPhilosophy()).append("\",\n")
          .append("    \"execution_model\": \"atomic\",\n")
          .append("    \"abstraction_depth\": \"low\",\n")
          .append("    \"modularity_approach\": \"monolithic\",\n")
          .append("    \"testing_strategy\": \"unit\",\n")
          .append("    \"extensibility\": \"low\",\n")
          .append("    \"dependency_assumptions\": \"none\",\n")
          .append("    \"runtime_behavior\": \"deterministic\",\n")
          .append("    \"risk_acceptance\": \"conservative\"\n")
          .append("  },\n");

        if (isMediated) {
            schemaSb.append("  \"mediation_candidate\": {\n")
              .append("    \"prompt\": \"(Genome A: The optimized prompt for the external LLM)\",\n")
              .append("    \"selected_files\": [\"(Genome B: Select 8-16 key files from the candidate list)\"],\n")
              .append("    \"architecture_summary\": \"(Genome B: Concise architecture mapping)\",\n")
              .append("    \"subsystems\": [\n")
              .append("      { \"id\": \"s1\", \"name\": \"Subsystem Name\", \"purpose\": \"Subsystem Purpose\", \"description\": \"...\", \"boundaries\": [\"...\"], \"critical_files\": [\"...\"], \"responsibilities\": [\"...\"] }\n")
              .append("    ],\n")
              .append("    \"architectural_facts\": [\n")
              .append("      { \"id\": \"f1\", \"subject\": \"...\", \"predicate\": \"...\", \"description\": \"...\", \"confidence\": 1.0, \"evidence\": [\"...\"] }\n")
              .append("    ],\n")
              .append("    \"dependencies\": \"(Genome B: Key module relationships and third-party deps)\",\n")
              .append("    \"execution_instructions\": \"(Genome B: Specific instructions for the external LLM)\",\n")
              .append("    \"evaluation\": \"(Self-evaluation of this candidate's quality)\"\n")
              .append("  },\n");
        }

        schemaSb.append("  \"actions\": [\n")
          .append("    { \"domain\": \"file\", \"operation\": \"WRITE\", \"target\": \"src/main/java/com/example/App.java\", \"description\": \"Create class\", \"implementation\": \"public class App { ... }\" }\n")
          .append("  ]\n")
          .append("}\n");

        builder.addJsonSchema(schemaSb.toString());
        builder.addConstraints("MANDATORY: Wrap your JSON response in <BEGIN_DARWIN_JSON> and <END_DARWIN_JSON> tags.\n" +
                               "MANDATORY: 'strategy' MUST be a specific architectural name. NEVER use 'ROOT', 'create', or 'bootstrap'.\n" +
                               "MANDATORY: The 'actions' array MUST NOT be empty. It must contain at least one WRITE action with FULL source code.");

        String directive = "Your goal is controlled divergence under constraints. Move the system into a meaningfully different evolutionary region.\n\n";
        if (capability == eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType.CHAT) {
            directive += "Mandatory: For simple chat, use the 'TALK' operation in 'actions' to provide your response in the 'implementation' field.";
        } else {
            directive += "Mandatory: For every 'WRITE' action, you MUST provide the FULL, FUNCTIONAL source code in the 'implementation' field.";
        }
        builder.addExecutionDirective(directive);

        return builder.build() + "\n\nCONTEXT:\n" + basePrompt;
    }

    /**
     * Spawns variants for the given strategies.
     */
    public List<JSONObject> spawn(GoalModel goal, List<DarwinStrategySeed> seeds, String basePrompt, String lineageContext, List<String> rejectedSiblings, boolean isMediated, TaskContext context) {
        List<JSONObject> variants = new ArrayList<>();
        Orchestrator orchestrator = context.getOrchestrator();

        // Sequential Evolution: Collect full JSON of already generated variants in this round
        List<JSONObject> currentRoundVariants = new ArrayList<>();

        for (DarwinStrategySeed seed : seeds) {
            context.log("[SPAWNER] Generating " + seed.getType() + " trajectory...");
            String branchId = "v-" + seed.getType().name().toLowerCase();
            EvolutionProgressPublisher.updateBranchStatus(context, branchId, seed.getType().name(), "active", null);
            EvolutionProgressPublisher.updateActiveModel(context, orchestrator != null ? (orchestrator.getOllama() != null ? orchestrator.getOllama().getModel() : "local") : "local", "Generating Branch " + (variants.size() + 1) + " of " + seeds.size());

            String seedPrompt = buildSeedPrompt(seed, basePrompt, lineageContext, rejectedSiblings, currentRoundVariants, isMediated, context);
            JSONObject validated = null;

            for (int retry = 0; retry < 2; retry++) {
                try {
                    String response = aiService.sendRequest(orchestrator, seedPrompt, context);
                    validated = validator.validate(response, seed.getType(), context);
                    if (validated != null) {
                        // IMPLEMENTATION PLANNING
                        ImplementationPlanner implementationPlanner = new ImplementationPlanner();
                        validated = implementationPlanner.plan(validated, context);

                        // Ensure ID is injected if missing from LLM response
                        if (!validated.has("id")) {
                            validated.put("id", "v-" + seed.getType().name().toLowerCase());
                        }
                        break;
                    }
                    context.log("[SPAWNER] Fatal Validation error for " + seed.getType() + ". Retry " + (retry + 1) + "/2...");
                } catch (Exception e) {
                    context.log("[SPAWNER] Error during generation for " + seed.getType() + ": " + e.getMessage());
                }
            }

            if (validated != null) {
                variants.add(validated);
                currentRoundVariants.add(validated);
                context.log("[SPAWNER] Successfully generated " + seed.getType() + " trajectory.");
                double score = validated.optDouble("score", 0.0);
                EvolutionProgressPublisher.updateBranchStatus(context, validated.optString("id"), seed.getType().name(), "complete", Double.isNaN(score) ? null : score);
            } else if (seed.isMandatory()) {
                context.log("[SPAWNER] FAILED to generate mandatory " + seed.getType() + " trajectory after retries.");
            }
        }

        return variants;
    }

    private String buildSeedPrompt(DarwinStrategySeed seed, String basePrompt, String lineageContext, List<String> rejectedSiblings, List<JSONObject> currentRoundVariants, boolean isMediated, TaskContext context) {
        DarwinPromptBuilder builder = new DarwinPromptBuilder(context);

        String role = "You are a single-path evolutionary mutation engine.\n" +
                      "Your goal is to generate another implementation PHILOSOPHY that satisfies the user's request while diverging from ancestors.\n\n" +
                      "🧠 CORE RULE: Each response MUST contain exactly ONE branch only. structurally distinct, NOT a variation, NOT a “better version”.";

        builder.addSystem(role)
               .addLineage(lineageContext)
               .addReality()
               .addSemanticEnvelope();

        Object genomeObj = context.getOrchestrationState().getMetadata().get("semanticGenome");
        SemanticGenome genome = eu.kalafatic.evolution.controller.parsers.JsonUtils.restoreFromMetadata(genomeObj, SemanticGenome.class, "semanticGenome", context);
        builder.addGenomeMemory(genome);

        StringBuilder constraintSb = new StringBuilder();
        if (rejectedSiblings != null && !rejectedSiblings.isEmpty()) {
            constraintSb.append("FORBIDDEN STRATEGIES (PREVIOUSLY REJECTED OR OCCUPIED):\n");
            for (String rejected : rejectedSiblings) constraintSb.append("- ").append(rejected).append("\n");
        }

        if (!currentRoundVariants.isEmpty()) {
            constraintSb.append("\n🚫 FORBIDDEN ATTRACTOR SET (SIBLINGS IN THIS ROUND):\n");
            for (JSONObject v : currentRoundVariants) {
                constraintSb.append("- STRATEGY: ").append(v.optString("strategy")).append("\n")
                  .append("  PHILOSOPHY: ").append(v.optString("semantic_anchor")).append("\n");
            }
        }

        constraintSb.append("\n🔥 DIVERGENCE REQUIREMENT (CRITICAL):\n")
          .append("Choose a DIFFERENT architectural philosophy, execution model, and state approach.\n\n")
          .append("TARGET TRAJECTORY GOAL: ").append(seed.getType()).append("\n");

        if (seed.getInterpretation() != null) {
            constraintSb.append("Interpretation: ").append(seed.getInterpretation()).append("\n")
              .append("Architectural Assumption: ").append(seed.getAssumption()).append("\n");
        }

        builder.addConstraints(constraintSb.toString());

        StringBuilder schemaSb = new StringBuilder();
        schemaSb.append("{\n")
          .append("  \"id\": \"v-").append(seed.getType().name().toLowerCase()).append("\",\n")
          .append("  \"strategy\": \"(Concrete technical strategy name)\",\n")
          .append("  \"strategy_type\": \"").append(seed.getType()).append("\",\n")
          .append("  \"mutation_philosophy\": \"(Engineering philosophy)\",\n")
          .append("  \"semantic_anchor\": \"(Core idea or philosophy)\",\n")
          .append("  \"survival_argument\": \"why this branch is better in this context\",\n")
          .append("  \"tradeoffs\": \"what is sacrificed\",\n")
          .append("  \"failure_risks\": \"how it might fail\",\n")
          .append("  \"expected_outputs\": [\"result 1\"],\n")
          .append("  \"reasoning_focus\": \"what this branch prioritizes\",\n")
          .append("  \"projected_steps\": [\"step 1\", \"step 2\"],\n")
          .append("  \"engineering_dimensions\": {\n")
          .append("    \"philosophy\": \"(Philosophy for this branch)\",\n")
          .append("    \"execution_model\": \"atomic/service/reactive/etc\",\n")
          .append("    \"abstraction_depth\": \"low/medium/high\",\n")
          .append("    \"modularity_approach\": \"monolithic/modular/etc\",\n")
          .append("    \"testing_strategy\": \"unit/integration/etc\",\n")
          .append("    \"extensibility\": \"low/medium/high\",\n")
          .append("    \"dependency_assumptions\": \"none/internal/external\",\n")
          .append("    \"runtime_behavior\": \"deterministic/async/etc\",\n")
          .append("    \"risk_acceptance\": \"conservative/experimental/etc\"\n")
          .append("  },\n");

        if (isMediated) {
            schemaSb.append("  \"mediation_candidate\": {\n")
              .append("    \"prompt\": \"(Genome A: The optimized prompt for the external LLM)\",\n")
              .append("    \"selected_files\": [\"(Genome B: Select 8-16 key files from the candidate list)\"],\n")
              .append("    \"architecture_summary\": \"(Genome B: Concise architecture mapping)\",\n")
              .append("    \"subsystems\": [\n")
              .append("      { \"id\": \"s1\", \"name\": \"Subsystem Name\", \"purpose\": \"Subsystem Purpose\", \"description\": \"...\", \"boundaries\": [\"...\"], \"critical_files\": [\"...\"], \"responsibilities\": [\"...\"] }\n")
              .append("    ],\n")
              .append("    \"architectural_facts\": [\n")
              .append("      { \"id\": \"f1\", \"subject\": \"...\", \"predicate\": \"...\", \"description\": \"...\", \"confidence\": 1.0, \"evidence\": [\"...\"] }\n")
              .append("    ],\n")
              .append("    \"dependencies\": \"(Genome B: Key module relationships and third-party deps)\",\n")
              .append("    \"execution_instructions\": \"(Genome B: Specific instructions for the external LLM)\",\n")
              .append("    \"evaluation\": \"(Self-evaluation of this candidate's quality)\"\n")
              .append("  },\n");
        }

        schemaSb.append("  \"actions\": [{ \"domain\": \"file\", \"operation\": \"WRITE\", \"target\": \"path/to/artifact\", \"description\": \"Action description\", \"implementation\": \"(Full source code for the file)\" }]\n")
          .append("}\n");

        builder.addJsonSchema(schemaSb.toString());
        builder.addExecutionDirective("Each branch must move the system into a meaningfully different evolutionary region.");

        return builder.build() + "\n\nCONTEXT:\n" + basePrompt;
    }
}
