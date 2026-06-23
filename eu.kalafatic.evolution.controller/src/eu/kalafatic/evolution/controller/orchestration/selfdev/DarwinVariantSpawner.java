package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.EvolutionProgressPublisher;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
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
    private final ImplementationPlanner implementationPlanner;

    public DarwinVariantSpawner(AiService aiService) {
        this.aiService = aiService;
        this.validator = new DarwinVariantValidator();
        this.implementationPlanner = new ImplementationPlanner();
    }

    /**
     * Spawns a single variant based on a blueprint and sequential mutation context.
     */
    public JSONObject spawnSingleBlueprint(GoalModel goal, TrajectoryBlueprint bp, String basePrompt, String lineageContext, List<String> rejectedSiblings, String mutationContext, boolean isMediated, TaskContext context, EvolutionDimension activeDimension, SemanticGenome genome) {
        Orchestrator orchestrator = context.getOrchestrator();

        context.log("[SPAWNER] Materializing trajectory from blueprint: " + bp.getId());
        EvolutionProgressPublisher.updateBranchStatus(context, bp.getId(), bp.getPhilosophy(), "active", null);
        EvolutionProgressPublisher.updateActiveModel(context, orchestrator != null ? (orchestrator.getOllama() != null ? orchestrator.getOllama().getModel() : "local") : "local", "Materializing Branch " + bp.getId());

        String bpPrompt = buildBlueprintPrompt(bp, basePrompt, lineageContext, rejectedSiblings, mutationContext, isMediated, context, activeDimension, genome);
        context.log("Stage: PromptComposer\nPrompt length: " + bpPrompt.length() + "\nPrompt hash: " + bpPrompt.hashCode());
        JSONObject validated = null;

        // Materialization Retries: Triggered only for fatal structural errors.
        for (int retry = 0; retry < 3; retry++) {
            context.log("Stage: Retry\nRetry count: " + retry);
            try {
                String response = aiService.sendRequest(orchestrator, bpPrompt, context);
                validated = validator.validate(response, bp.getStrategyType(), context);

                if (validated != null) {
                    // IMPLEMENTATION PLANNING: Convert architectural reasoning into actions
                    validated = implementationPlanner.plan(validated, context);
                    validated = completeTrajectorySchema(validated, bp, context);
                    break;
                }

                context.log("[SPAWNER] Fatal Materialization error for blueprint " + bp.getId() + ". Retry " + (retry + 1) + "/3...");

                // ADJUST PROMPT: If validation failed, add a strict hint for the next retry
                if (retry == 0) {
                    bpPrompt += "\n\nCRITICAL: Your previous response was fatally invalid. Ensure you return a single JSON object with at least 'strategy' and 'semantic_anchor'.";
                }
            } catch (Exception e) {
                context.log("[SPAWNER] Error during blueprint materialization for " + bp.getId() + ": " + e.getMessage());
            }
        }

        if (validated != null) {
            context.log("Stage: Variant accepted");
            context.log("[SPAWNER] Successfully materialized blueprint: " + bp.getId());
            double score = validated.optDouble("score", 0.0);
            EvolutionProgressPublisher.updateBranchStatus(context, bp.getId(), bp.getPhilosophy(), "complete", Double.isNaN(score) ? null : score);
            return validated;
        } else {
            context.log("Stage: Fallback\nReason: Materialization retries failed");
            context.log("[SPAWNER] Materialization retries failed for " + bp.getId() + ". Attempting deterministic auto-repair.");
            JSONObject repaired = autoRepair(bp, context);
            if (repaired != null) {
                context.log("Stage: Repair\nRepair attempted: true\nResult: Success");
                context.log("[SPAWNER] Successfully auto-repaired blueprint: " + bp.getId());
                return repaired;
            } else {
                context.log("Stage: Repair\nRepair attempted: true\nResult: Failed");
                context.log("[SPAWNER] CRITICAL: Failed to materialize or repair mandatory blueprint: " + bp.getId());
                return null;
            }
        }
    }

    private JSONObject completeTrajectorySchema(JSONObject fragment, TrajectoryBlueprint bp, TaskContext context) {
        // Ensure core fields exist and are consistent with blueprint
        fragment.put("id", bp.getId());
        fragment.put("strategy_type", bp.getStrategyType().name());

        if (!fragment.has("strategy") || fragment.optString("strategy").isEmpty()) {
            fragment.put("strategy", "Architectural strategy for " + bp.getPhilosophy());
        }

        fragment.put("semantic_justification", bp.getPhilosophy());
        fragment.put("semantic_anchor", bp.getPhilosophy());

        // Standard Darwin defaults for missing metadata
        if (!fragment.has("survival_argument") || fragment.optString("survival_argument").isEmpty()) {
            fragment.put("survival_argument", "Proposed as a divergent architectural candidate for " + bp.getPhilosophy());
        }
        if (!fragment.has("tradeoffs") || fragment.optString("tradeoffs").isEmpty()) {
            fragment.put("tradeoffs", "Standard trade-offs for " + bp.getStrategyType() + " architecture.");
        }
        if (!fragment.has("failure_risks") || fragment.optString("failure_risks").isEmpty()) {
            fragment.put("failure_risks", "Managed risks within " + bp.getStrategyType() + " evolutionary boundaries.");
        }

        // Inject dimensions from blueprint if missing in LLM response
        JSONObject dimensions = fragment.optJSONObject("engineering_dimensions");
        if (dimensions == null) {
            dimensions = new JSONObject();
            fragment.put("engineering_dimensions", dimensions);
        }

        for (java.util.Map.Entry<String, String> entry : bp.getEngineeringDimensions().entrySet()) {
            String dimKey = entry.getKey();
            if (!dimensions.has(dimKey)) {
                dimensions.put(dimKey, entry.getValue());
            }
        }

        if (!dimensions.has("philosophy")) {
            dimensions.put("philosophy", bp.getPhilosophy());
        }

        return fragment;
    }

    private JSONObject autoRepair(TrajectoryBlueprint bp, TaskContext context) {
        try {
            JSONObject repair = new JSONObject();
            repair.put("id", bp.getId());
            repair.put("strategy_type", bp.getStrategyType().name());

            // Synthesize valid architectural strategy text
            String synthesizedStrategy = "Architectural realization of " + bp.getId() + " philosophy: " + bp.getArchitecturalDirection();
            repair.put("strategy", synthesizedStrategy);
            repair.put("semantic_anchor", bp.getPhilosophy());
            repair.put("semantic_justification", bp.getPhilosophy());

            // Standard Darwin defaults for repair
            repair.put("survival_argument", "Deterministic architectural recovery for mandatory lineage.");
            repair.put("tradeoffs", "Synthesized fallback path.");
            repair.put("failure_risks", "Potential for reduced architectural specificity.");

            // Invoke planner to generate executable actions for the blueprint
            repair = implementationPlanner.plan(repair, context);

            // GUARANTEE EXECUTABLE ACTIONS: If planner failed, add fallback ANALYZE action
            org.json.JSONArray actions = repair.optJSONArray("actions");
            if (actions == null || actions.length() == 0) {
                if (actions == null) actions = new org.json.JSONArray();
                JSONObject fallback = new JSONObject();
                fallback.put("domain", "kernel");
                fallback.put("operation", "ANALYZE");
                fallback.put("target", "workspace");
                fallback.put("description", "Materialize " + bp.getId() + " architectural intent (Fallback)");
                actions.put(fallback);
                repair.put("actions", actions);
            }

            repair.put("reasoning_focus", "Deterministic architectural recovery for " + bp.getId());

            org.json.JSONArray selectedFiles = new org.json.JSONArray();
            repair.put("selected_files", selectedFiles);

            repair.put("survival_argument", "Mandatory architectural diversity branch ensured by orchestrator.");
            repair.put("expected_outputs", new org.json.JSONArray());
            repair.put("score", 0.45); // Auto-repaired branches start with lower fitness

            JSONObject dimensions = new JSONObject();
            for (java.util.Map.Entry<String, String> entry : bp.getEngineeringDimensions().entrySet()) {
                dimensions.put(entry.getKey(), entry.getValue());
            }
            if (!dimensions.has("philosophy")) {
                dimensions.put("philosophy", bp.getPhilosophy());
            }
            repair.put("engineering_dimensions", dimensions);

            // AUTO-REPAIR MEDIATION: Ensure mediated mode still gets a valid context package
            if (context.getBehaviorProfile().hasTrait(eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait.SUPERVISION_MEDIATED)) {
                eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot snapshot = (eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot) context.getOrchestrationState().getMetadata().get("mediatedSnapshot");
                if (snapshot != null) {
                    eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator curator = new eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator();
                    List<String> files = curator.selectContext(snapshot, bp.getGoal(), 12); // Aim for sweet spot 12

                    JSONObject mediationCandidate = new JSONObject();
                    org.json.JSONArray medFiles = new org.json.JSONArray();
                    for (String f : files) {
                        medFiles.put(f);
                        selectedFiles.put(f);
                    }

                    mediationCandidate.put("selected_files", medFiles);
                    mediationCandidate.put("prompt", "Analyze and propose improvements based on the provided high-density repository context. Focus on the core architectural hotspots.");
                    mediationCandidate.put("architecture_summary", "Auto-recovered high-signal architecture mapping.");
                    mediationCandidate.put("dependencies", "Auto-recovered critical dependency mapping.");
                    mediationCandidate.put("execution_instructions", "Perform deep reasoning on the provided distilled files. Do not exceed the scope of the provided context.");
                    mediationCandidate.put("evaluation", "Auto-repaired distilled fallback candidate (High Signal).");
                    repair.put("mediation_candidate", mediationCandidate);
                }
            }

            return repair;
        } catch (Exception e) {
            context.log("[SPAWNER] Auto-repair failed: " + e.getMessage());
            return null;
        }
    }

    private String buildBlueprintPrompt(TrajectoryBlueprint bp, String basePrompt, String lineageContext, List<String> rejectedSiblings, String mutationContext, boolean isMediated, TaskContext context, EvolutionDimension activeDimension, SemanticGenome genome) {
        eu.kalafatic.evolution.controller.orchestration.behavior.PromptComposer composer = new eu.kalafatic.evolution.controller.orchestration.behavior.PromptComposer();
        StringBuilder sb = new StringBuilder();
        eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType capability = context.getExecutionProfile().getCapability();

        sb.append(composer.composeSystem(null)).append("\n\n");
        if (capability == eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType.CHAT) {
            sb.append("You are a conversational evolutionary mutation engine.\n")
              .append("Your goal is to perform a BOUNDED LOCAL MUTATION on the conversational response.\n")
              .append("MANDATE: You are ONLY allowed to mutate the specific dimension (tone, depth, etc.) identified below.\n")
              .append("Each response MUST contain exactly ONE branch only.\n\n");
        } else {
            sb.append("You are a single-path evolutionary mutation engine.\n")
              .append("Your goal is to perform a BOUNDED LOCAL MUTATION on the provided parent implementation.\n")
              .append("MANDATE: You MUST preserve the parent implementation. You are ONLY allowed to mutate the specific dimension identified below.\n")
              .append("Do NOT redesign the complete architecture. Do NOT start from scratch. Focus only on the active mutation dimension.\n")
              .append("Each response MUST contain exactly ONE branch only.\n\n");
        }

        sb.append(composer.composeGoal(bp.getGoal())).append("\n\n");

        GoalModel goalModel = (GoalModel) context.getOrchestrationState().getMetadata().get("goalModel");
        if (goalModel != null) {
            sb.append("--- SEMANTIC ANCHOR (STRICT BOUNDARY) ---\n")
              .append("Goal Type: ").append(goalModel.getGoalType()).append("\n")
              .append("Domain: ").append(goalModel.getDomain()).append("\n")
              .append("Intent: ").append(goalModel.getIntent()).append("\n")
              .append("Requested Artifact: ").append(goalModel.getRequestedArtifact()).append("\n")
              .append("Primary Action: ").append(goalModel.getPrimaryAction()).append("\n")
              .append("Complexity: ").append(goalModel.getComplexity()).append("\n")
              .append("Required Outputs: ").append(goalModel.getRequiredOutputs()).append("\n\n");
        }

        Object envObj = context.getOrchestrationState().getMetadata().get("semanticEnvelope");
        SemanticEnvelope envelope = null;
        if (envObj instanceof SemanticEnvelope) {
            envelope = (SemanticEnvelope) envObj;
        } else if (envObj instanceof Map) {
            envelope = new com.fasterxml.jackson.databind.ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .convertValue(envObj, SemanticEnvelope.class);
        }
        if (envelope != null) {
            sb.append("--- SEMANTIC ENVELOPE (EVOLUTIONARY MANDATE) ---\n")
              .append("MANDATORY CONCEPTS: ").append(envelope.getMandatoryConcepts()).append("\n")
              .append("ALLOWED MUTATION DIMENSIONS: ").append(envelope.getAllowedMutationDimensions()).append("\n")
              .append("FORBIDDEN SEMANTIC REGIONS (VIOLATION = FAILURE): ").append(envelope.getForbiddenRegions()).append("\n")
              .append("MAX ABSTRACTION DEPTH: ").append(envelope.getMaxAbstractionDepth()).append("\n\n");
        }
        sb.append("MANDATE: You MUST stay within these semantic boundaries. Do NOT invent new problems, do NOT introduce frameworks unless requested, and do NOT increase architectural complexity beyond the minimum required.\n\n");

        if (context != null && context.getOrchestrationState() != null) {
            var metadata = context.getOrchestrationState().getMetadata();
            sb.append("CURRENT BEST UNDERSTANDING:\n")
              .append("- ").append(metadata.getOrDefault("current_understanding", "None")).append("\n\n");
        }

        sb.append(composer.composeLineage(lineageContext)).append("\n\n");

        StringBuilder siblingSb = new StringBuilder();
        if (rejectedSiblings != null && !rejectedSiblings.isEmpty()) {
            siblingSb.append("FORBIDDEN STRATEGIES (PREVIOUSLY REJECTED OR OCCUPIED):\n");
            for (String rejected : rejectedSiblings) siblingSb.append("- ").append(rejected).append("\n");
        }

        if (genome == null) {
            Object genomeObj = context.getOrchestrationState().getMetadata().get("semanticGenome");
            genome = eu.kalafatic.evolution.controller.parsers.JsonUtils.restoreFromMetadata(genomeObj, SemanticGenome.class, "semanticGenome", context);
        }

        if (genome != null) {
            if (!genome.getRejectedMutations().isEmpty()) {
                siblingSb.append("FORBIDDEN MUTATIONS (REJECTED BY SEMANTIC VALIDATOR):\n");
                for (MutationRecord rejected : genome.getRejectedMutations()) {
                    siblingSb.append("- ").append(rejected.getStrategy()).append(" (Reason: ").append(rejected.getTradeoffs()).append(")\n");
                }
            }
            if (!genome.getDiscoveredMutations().isEmpty()) {
                siblingSb.append("EXPLORED MUTATIONS (ALREADY ATTEMPTED):\n");
                for (MutationRecord explored : genome.getDiscoveredMutations()) {
                    siblingSb.append("- ").append(explored.getStrategy()).append("\n");
                }
            }
        }

        if (mutationContext != null && !mutationContext.isEmpty()) {
            siblingSb.append(mutationContext);
        }
        if (bp.getForbiddenOverlaps() != null && !bp.getForbiddenOverlaps().isEmpty()) {
            for (String overlap : bp.getForbiddenOverlaps()) siblingSb.append("- ").append(overlap).append("\n");
        }
        sb.append(composer.composeSiblingMemory(siblingSb.toString())).append("\n\n");

        // GROUNDING: Repository Awareness
        String projectStructure = (String) context.getOrchestrationState().getMetadata().get("projectStructure");
        if (projectStructure != null) {
            sb.append(composer.composeContext(projectStructure)).append("\n\n");
        }

        StringBuilder constraintSb = new StringBuilder();
        String activeDimensionId = bp.getEngineeringDimensions().get("active_dimension");
        String activeDimensionDesc = bp.getEngineeringDimensions().get("active_dimension_description");

        if (activeDimensionId != null) {
            constraintSb.append("ACTIVE MUTATION DIMENSION: ").append(activeDimensionId).append("\n")
              .append("DIMENSION DESCRIPTION: ").append(activeDimensionDesc).append("\n")
              .append("INSTRUCTION: Mutate ONLY this dimension. Keep all other winning decisions from the parent unchanged.\n\n");
        }

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
        sb.append(composer.composeConstraints(constraintSb.toString())).append("\n\n");

        sb.append("--- MUTATION JOURNAL DIRECTIVE ---\n")
          .append("You MUST provide a step-by-step 'mutation_journal' of your technical decisions.\n")
          .append("Format: [Step Name]: [Reasoning]\n")
          .append("Example: \"Added overload: To support flexible input\", \"Rejected logging: Unnecessary complexity\"\n\n");

        StringBuilder schemaSb = new StringBuilder();
        schemaSb.append("{\n")
          .append("  \"id\": \"").append(bp.getId()).append("\",\n")
          .append("  \"strategy\": \"(Concrete technical strategy name)\",\n")
          .append("  \"strategy_type\": \"").append(bp.getStrategyType().name()).append("\",\n")
          .append("  \"mutation_philosophy\": \"").append(bp.getMutationPhilosophy()).append("\",\n")
          .append("  \"semantic_anchor\": \"").append(bp.getPhilosophy()).append("\",\n")
          .append("  \"survival_argument\": \"why this branch is better in this context\",\n")
          .append("  \"tradeoffs\": \"what is sacrificed\",\n")
          .append("  \"failure_risks\": \"how it might fail\",\n")
          .append("  \"expected_outputs\": [\"result 1\"],\n")
          .append("  \"reasoning_focus\": \"what this branch prioritizes\",\n")
          .append("  \"projected_steps\": [\"step 1\", \"step 2\"],\n")
          .append("  \"mutation_journal\": [\"(Step 1)\", \"(Step 2)\"],\n")
          .append("  \"engineering_dimensions\": {\n")
          .append("    \"philosophy\": \"").append(bp.getPhilosophy()).append("\",\n")
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
            sb.append("🔴 MEDIATED MODE: DUAL-GENOME EVOLUTION DIRECTIVE\n\n")
              .append("Your primary organism is the MEDIATION EXPORT PACKAGE.\n")
              .append("You are evolving two independent genomes:\n")
              .append("Genome A: The Prompt (instructional strategy for the external LLM).\n")
              .append("Genome B: The Package (selected files, architectural summaries, and metadata).\n\n")
              .append("ITERATIVE MUTATION RULES:\n")
              .append("- Mutate the prompt wording, task framing, and implementation guidance.\n")
              .append("- Mutate the file selection: add, remove, or replace files and summaries.\n")
              .append("- Optimize for: Maximum Understanding ÷ Minimum Context.\n")
              .append("- Use the provided candidate files list as your primary search space.\n\n");

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
        sb.append(composer.composeJsonSchema(schemaSb.toString())).append("\n\n");

        sb.append("🧭 EVOLUTION DIRECTIVE\n\n")
          .append("Your goal is controlled divergence under constraints. Move the system into a meaningfully different evolutionary region.\n\n");

        if (capability == eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType.CHAT) {
            sb.append("Mandatory: For simple chat, use the 'TALK' operation in 'actions' to provide your response in the 'implementation' field.\n\n");
        } else {
            sb.append("Mandatory: For every 'WRITE' action, you MUST provide the FULL, FUNCTIONAL source code in the 'implementation' field.\n\n");
        }

        sb.append("CONTEXT:\n")
          .append(basePrompt);

        return sb.toString();
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
        StringBuilder sb = new StringBuilder();
        sb.append("🔴 SYSTEM / ROLE\n\n")
          .append("You are a single-path evolutionary mutation engine.\n")
          .append("Your goal is to generate another implementation PHILOSOPHY that satisfies the user's request while diverging from ancestors.\n\n")
          .append("You do NOT generate multiple solutions.\n")
          .append("You do NOT compare alternatives.\n")
          .append("You do NOT output sets or lists of strategies.\n\n")
          .append("You perform one controlled mutation of a prior solution state.\n\n")
          .append("🧠 CORE RULE\n\n")
          .append("Each response MUST contain exactly ONE branch only.\n\n")
          .append("This branch must be:\n\n")
          .append("structurally distinct from all previously seen branches\n")
          .append("NOT a variation of earlier strategies\n")
          .append("NOT a “better version” of the same approach\n\n")
          .append("You are evolving a lineage, not generating options.\n\n")
          .append("📌 INPUT CONTEXT\n\n")
          .append("GOAL (target task): (Implicit in workspace context)\n\n");

        if (context != null && context.getOrchestrationState() != null) {
            var metadata = context.getOrchestrationState().getMetadata();
            sb.append("CURRENT BEST UNDERSTANDING:\n")
              .append("- ").append(metadata.getOrDefault("current_understanding", "None")).append("\n\n");

            Object envObjSeed = metadata.get("semanticEnvelope");
            SemanticEnvelope envelope = null;
            if (envObjSeed instanceof SemanticEnvelope) {
                envelope = (SemanticEnvelope) envObjSeed;
            } else if (envObjSeed instanceof Map) {
                envelope = new com.fasterxml.jackson.databind.ObjectMapper()
                    .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .convertValue(envObjSeed, SemanticEnvelope.class);
            }
            if (envelope != null) {
                sb.append("SEMANTIC ENVELOPE (EVOLUTIONARY MANDATE):\n")
                  .append("- MANDATORY CONCEPTS: ").append(envelope.getMandatoryConcepts()).append("\n")
                  .append("- ALLOWED MUTATION DIMENSIONS: ").append(envelope.getAllowedMutationDimensions()).append("\n")
                  .append("- FORBIDDEN SEMANTIC REGIONS (VIOLATION = FAILURE): ").append(envelope.getForbiddenRegions()).append("\n")
                  .append("- MAX ABSTRACTION DEPTH: ").append(envelope.getMaxAbstractionDepth()).append("\n\n");
            }
        }

        if (lineageContext != null && !lineageContext.isEmpty()) {
            sb.append("🧬 CUMULATIVE EVOLUTIONARY ANCESTRY\n\n")
              .append("You are mutating a specific lineage. This is your biological memory:\n")
              .append(lineageContext).append("\n")
              .append("DIRECTIVE: Your mutation MUST satisfy the current goal while remaining CONTINUOUS with this lineage.\n\n");
        }

        sb.append("FORBIDDEN STRATEGIES (PREVIOUSLY REJECTED OR OCCUPIED):\n");
        if (rejectedSiblings != null && !rejectedSiblings.isEmpty()) {
            for (String rejected : rejectedSiblings) {
                sb.append("- ").append(rejected).append("\n");
            }
        }
        if (!currentRoundVariants.isEmpty()) {
            sb.append("🚫 CUMULATIVE REJECTION MEMORY (HARD CONSTRAINT)\n\n")
              .append("You are NOT generating from a clean state.\n")
              .append("You are generating from a constrained evolutionary space where previous outputs are already occupied regions.\n")
              .append("You MUST treat all previous branches as forbidden attractors.\n\n")
              .append("FORBIDDEN ATTRACTOR SET:\n");

            for (JSONObject v : currentRoundVariants) {
                sb.append("- STRATEGY: ").append(v.optString("strategy")).append("\n")
                  .append("  PHILOSOPHY: ").append(v.optString("semantic_justification")).append("\n")
                  .append("  MODEL: ").append(v.optJSONObject("engineering_dimensions")).append("\n\n");
            }

            sb.append("RULES:\n\n")
              .append("1. You MUST NOT generate a strategy that is semantically close to ANY entry in the FORBIDDEN ATTRACTOR SET.\n")
              .append("2. Similarity includes:\n")
              .append("   - same execution model (even if renamed)\n")
              .append("   - same decomposition style\n")
              .append("   - same control flow paradigm\n")
              .append("   - same state model\n")
              .append("   - same architecture family\n\n")
              .append("3. You MUST actively select a NON-OVERLAPPING REGION:\n")
              .append("   - different computation paradigm\n")
              .append("   - different system topology\n")
              .append("   - different reasoning structure\n\n")
              .append("4. If you cannot find a non-overlapping region:\n")
              .append("   STOP and pivot to maximal divergence (not refinement).\n\n")
              .append("5. You are penalized for “soft mutations”:\n")
              .append("   - rewording is invalid\n")
              .append("   - reordering is invalid\n")
              .append("   - parameter tuning is invalid\n\n")
              .append("VALID OUTPUT MUST BE A NEW ARCHITECTURAL CLASS, NOT A VARIANT.\n\n");
            for (JSONObject v : currentRoundVariants) {
                sb.append("- ").append(v.optString("strategy")).append(" (Philosophy: ").append(v.optString("semantic_justification")).append(")\n");
            }
        }

        // GROUNDING: Repository Awareness
        String projectStructure = (String) context.getOrchestrationState().getMetadata().get("projectStructure");
        if (projectStructure != null) {
            sb.append("📂 REPOSITORY REALITY (GROUNDING SOURCE)\n\n")
              .append("Your mutation MUST be grounded in this physical structure:\n")
              .append(projectStructure).append("\n\n");
        }

        sb.append("\n🚫 FORBIDDEN BEHAVIOR\n\n")
          .append("You MUST NOT:\n\n")
          .append("generate multiple candidate solutions\n")
          .append("provide alternatives or comparisons\n")
          .append("reuse prior architectural patterns\n")
          .append("slightly modify previous branch\n")
          .append("output generic “improved version”\n")
          .append("converge toward average solution\n")
          .append("blend multiple previous branches\n\n")
          .append("If you do any of the above, your output is invalid.\n\n")
          .append("🔥 DIVERGENCE REQUIREMENT (CRITICAL)\n\n")
          .append("Your solution MUST intentionally diverge from prior branches.\n\n")
          .append("To achieve this:\n\n")
          .append("choose a DIFFERENT architectural philosophy\n")
          .append("change the core execution model\n")
          .append("change control flow style\n")
          .append("change system decomposition strategy\n")
          .append("change state management approach\n\n")
          .append("If previous branch was similar in structure, you MUST pivot sharply.\n\n")
          .append("🧬 ANTI-REPETITION MEMORY\n\n")
          .append("You MUST explicitly avoid repeating the strategies and assumptions listed in the FORBIDDEN STRATEGIES section above.\n\n")
          .append("Mandatory: For every 'WRITE' action, you MUST provide the FULL, FUNCTIONAL source code in the 'implementation' field.\n\n")
          .append("STABILIZATION CONSTRAINTS:\n")
          .append("- NO ARCHITECTURAL INFLATION: If task is trivial, provide a MINIMAL implementation.\n")
          .append("- MINIMUM VIABLE SOLUTION BIAS: Prefer the simplest code that satisfies the goal.\n")
          .append("- GROUNDING: Use ONLY the provided Target Reality Model and Hotspots.\n\n")
          .append("TARGET TRAJECTORY GOAL: ").append(seed.getType()).append("\n");
        if (seed.getInterpretation() != null) {
            sb.append("Interpretation: ").append(seed.getInterpretation()).append("\n")
              .append("Architectural Assumption: ").append(seed.getAssumption()).append("\n")
              .append("Goal Detail: ").append(seed.getFutureGoal()).append("\n");
        }
        sb.append("\n🎯 OUTPUT FORMAT (STRICT)\n\n")
          .append("Return ONLY one JSON object within <BEGIN_DARWIN_JSON> and <END_DARWIN_JSON> tags:\n\n")
          .append("{\n")
          .append("  \"id\": \"v-").append(seed.getType().name().toLowerCase()).append("\",\n")
          .append("  \"strategy\": \"(Concrete technical strategy name)\",\n")
          .append("  \"strategy_type\": \"").append(seed.getType()).append("\",\n")
          .append("  \"mutation_philosophy\": \"(Engineering philosophy: minimalism | extensibility | performance | robustness | idiomatic | etc.)\",\n")
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
            sb.append("🔴 MEDIATED MODE: DUAL-GENOME EVOLUTION DIRECTIVE\n\n")
              .append("Your primary organism is the MEDIATION EXPORT PACKAGE.\n")
              .append("You are evolving two independent genomes:\n")
              .append("Genome A: The Prompt (instructional strategy for the external LLM).\n")
              .append("Genome B: The Package (selected files, architectural summaries, and metadata).\n\n")
              .append("ITERATIVE MUTATION RULES:\n")
              .append("- Mutate the prompt wording, task framing, and implementation guidance.\n")
              .append("- Mutate the file selection: add, remove, or replace files and summaries.\n")
              .append("- Optimize for: Maximum Understanding ÷ Minimum Context.\n")
              .append("- Use the provided candidate files list as your primary search space.\n\n");

            sb.append("  \"mediation_candidate\": {\n")
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

        sb.append("  \"actions\": [{ \"domain\": \"file\", \"operation\": \"WRITE\", \"target\": \"path/to/artifact\", \"description\": \"Action description\", \"implementation\": \"(Full source code for the file)\" }]\n")
          .append("}\n\n")
          .append("⚠️ CRITICAL OUTPUT CONSTRAINT\n")
          .append("ONLY ONE JSON OBJECT\n")
          .append("NO arrays of solutions\n")
          .append("NO multiple strategies\n")
          .append("NO explanation outside JSON\n")
          .append("NO markdown\n")
          .append("NO commentary\n\n")
          .append("🧭 EVOLUTION DIRECTIVE\n\n")
          .append("Your goal is not correctness.\n\n")
          .append("Your goal is controlled divergence under constraints.\n\n")
          .append("Each branch must move the system into a meaningfully different evolutionary region.\n\n")
          .append("🧪 FINAL CHECK (MENTAL SELF-VALIDATION)\n\n")
          .append("Before outputting, verify:\n\n")
          .append("Did I generate only ONE branch?\n")
          .append("Is it structurally different from previous ones?\n")
          .append("Did I avoid combining previous ideas?\n")
          .append("Did I change the underlying philosophy, not just details?\n\n")
          .append("CONTEXT:\n")
          .append(basePrompt);

        return sb.toString();
    }
}
