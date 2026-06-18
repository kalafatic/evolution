package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.EvolutionProgressPublisher;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
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
     * Spawns a single variant based on a blueprint and sequential mutation context.
     */
    public JSONObject spawnSingleBlueprint(String goal, TrajectoryBlueprint bp, String basePrompt, String lineageContext, List<String> rejectedSiblings, String mutationContext, boolean isMediated, TaskContext context) {
        Orchestrator orchestrator = context.getOrchestrator();

        context.log("[SPAWNER] Materializing trajectory from blueprint: " + bp.getId());
        EvolutionProgressPublisher.updateBranchStatus(context, bp.getId(), bp.getPhilosophy(), "active", null);
        EvolutionProgressPublisher.updateActiveModel(context, orchestrator != null ? (orchestrator.getOllama() != null ? orchestrator.getOllama().getModel() : "local") : "local", "Materializing Branch " + bp.getId());

        String bpPrompt = buildBlueprintPrompt(bp, basePrompt, lineageContext, rejectedSiblings, mutationContext, isMediated, context);
        JSONObject validated = null;

        // Materialization Retries: The branch topology (blueprint) is preserved; only the implementation details are retried.
        for (int retry = 0; retry < 3; retry++) {
            try {
                String response = aiService.sendRequest(orchestrator, bpPrompt, context);
                validated = validator.validate(response, bp.getStrategyType(), context); // Blueprints are technical mutations
                if (validated != null) {
                    // ORCHESTRATOR SCHEMA COMPLETION: Inject metadata into semantic fragment
                    validated = completeTrajectorySchema(validated, bp, context);
                    break;
                }

                context.log("[SPAWNER] Materialization failed for blueprint " + bp.getId() + ". Retry " + (retry + 1) + "/3...");
            } catch (Exception e) {
                context.log("[SPAWNER] Error during blueprint materialization for " + bp.getId() + ": " + e.getMessage());
            }
        }

        if (validated != null) {
            context.log("[SPAWNER] Successfully materialized blueprint: " + bp.getId());
            double score = validated.optDouble("score", 0.0);
            EvolutionProgressPublisher.updateBranchStatus(context, bp.getId(), bp.getPhilosophy(), "complete", Double.isNaN(score) ? null : score);
            return validated;
        } else {
            context.log("[SPAWNER] Materialization retries failed for " + bp.getId() + ". Attempting deterministic auto-repair.");
            JSONObject repaired = autoRepair(bp, context);
            if (repaired != null) {
                context.log("[SPAWNER] Successfully auto-repaired blueprint: " + bp.getId());
                return repaired;
            } else {
                context.log("[SPAWNER] CRITICAL: Failed to materialize or repair mandatory blueprint: " + bp.getId());
                return null;
            }
        }
    }

    private JSONObject completeTrajectorySchema(JSONObject fragment, TrajectoryBlueprint bp, TaskContext context) {
        // Ensure core fields exist and are consistent with blueprint
        fragment.put("id", bp.getId());
        fragment.put("strategy_type", bp.getStrategyType().name());
        fragment.put("semantic_justification", bp.getPhilosophy());
        fragment.put("semantic_anchor", bp.getPhilosophy());

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

            repair.put("reasoning_focus", "Deterministic architectural recovery for " + bp.getId());

            org.json.JSONArray selectedFiles = new org.json.JSONArray();
            repair.put("selected_files", selectedFiles);

            repair.put("survival_argument", "Mandatory architectural diversity branch ensured by orchestrator.");
            repair.put("tradeoffs", "Deterministic fallback; lacks LLM-refined implementation nuance.");
            repair.put("failure_risks", "Lower specificity than materialized variants.");
            repair.put("semantic_justification", bp.getPhilosophy());

            org.json.JSONArray steps = new org.json.JSONArray();
            for (String s : bp.getRequiredCharacteristics()) steps.put("Realize blueprint characteristic: " + s);
            repair.put("projected_steps", steps);

            repair.put("expected_outputs", new org.json.JSONArray());
            repair.put("score", 0.45); // Auto-repaired branches start with lower fitness

            org.json.JSONArray actions = new org.json.JSONArray();

            // GENERIC TASK RECOVERY: Use context-driven bootstrap synthesis instead of hardcoded templates
            AtomicIntentAnalysis atomic = (AtomicIntentAnalysis) context.getOrchestrationState().getMetadata().get("atomicAnalysis");
            if (atomic != null && atomic.isAtomic() && atomic.getTargetArtifact() != null && !atomic.getTargetArtifact().isEmpty()) {
                JSONObject writeAction = new JSONObject();
                String target = atomic.getTargetArtifact();

                writeAction.put("domain", "semantic");
                writeAction.put("operation", "BOOTSTRAP");
                writeAction.put("target", target);

                try {
                    String bootstrapPrompt = "Generate a minimal valid bootstrap implementation for " + target + " given the goal: " + bp.getGoal() + ". Return ONLY the content.";
                    String content = aiService.sendRequest(context.getOrchestrator(), bootstrapPrompt, context);
                    writeAction.put("description", content);
                } catch (Exception e) {
                    writeAction.put("description", "// Dynamic bootstrap failed for " + target);
                }

                actions.put(writeAction);
                repair.put("strategy", "Dynamic auto-repair: bootstrapping " + target);
            } else {
                JSONObject action = new JSONObject();
                action.put("domain", "kernel");
                action.put("operation", "ANALYZE");
                action.put("target", "workspace");
                action.put("description", "Bootstrap " + bp.getId() + " architectural strategy.");
                actions.put(action);
            }
            repair.put("actions", actions);

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

    private String buildBlueprintPrompt(TrajectoryBlueprint bp, String basePrompt, String lineageContext, List<String> rejectedSiblings, String mutationContext, boolean isMediated, TaskContext context) {
        eu.kalafatic.evolution.controller.orchestration.behavior.PromptComposer composer = new eu.kalafatic.evolution.controller.orchestration.behavior.PromptComposer();
        StringBuilder sb = new StringBuilder();

        sb.append(composer.composeSystem(null)).append("\n\n");
        sb.append("You are a single-path evolutionary mutation engine.\n")
          .append("Each response MUST contain exactly ONE branch only.\n\n");

        sb.append(composer.composeGoal(bp.getGoal())).append("\n\n");

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
        constraintSb.append("DIVERGENCE REQUIREMENT (CRITICAL):\n")
          .append("Your solution MUST intentionally diverge from prior branches. Pivot sharply on philosophy, execution model, and control flow.\n\n")
          .append("STABILIZATION CONSTRAINTS:\n")
          .append("- NO ARCHITECTURAL INFLATION: If task is trivial, provide a MINIMAL implementation.\n")
          .append("- MINIMUM VIABLE SOLUTION BIAS: Prefer the simplest code that satisfies the goal.\n")
          .append("- GROUNDING: Use ONLY the provided Target Reality Model and Hotspots.\n\n")
          .append("BLUEPRINT CONSTRAINTS:\n")
          .append("- ID: ").append(bp.getId()).append("\n")
          .append("- Philosophy: ").append(bp.getPhilosophy()).append("\n")
          .append("- Architectural Direction: ").append(bp.getArchitecturalDirection()).append("\n");
        sb.append(composer.composeConstraints(constraintSb.toString())).append("\n\n");

        StringBuilder schemaSb = new StringBuilder();
        schemaSb.append("{\n")
          .append("  \"id\": \"").append(bp.getId()).append("\",\n")
          .append("  \"strategy\": \"(Concrete technical strategy name)\",\n")
          .append("  \"strategy_type\": \"").append(bp.getStrategyType().name()).append("\",\n")
          .append("  \"semantic_anchor\": \"").append(bp.getPhilosophy()).append("\",\n")
          .append("  \"survival_argument\": \"why this branch is better in this context\",\n")
          .append("  \"tradeoffs\": \"what is sacrificed\",\n")
          .append("  \"failure_risks\": \"how it might fail\",\n")
          .append("  \"expected_outputs\": [\"result 1\"],\n")
          .append("  \"reasoning_focus\": \"what this branch prioritizes\",\n")
          .append("  \"projected_steps\": [\"step 1\", \"step 2\"],\n")
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
            schemaSb.append("  \"mediation_candidate\": {\n")
              .append("    \"prompt\": \"(The optimized prompt for the external LLM)\",\n")
              .append("    \"selected_files\": [\"(Select 8-16 key files from the candidate list)\"],\n")
              .append("    \"architecture_summary\": \"(Concise architecture mapping)\",\n")
              .append("    \"subsystems\": [\n")
              .append("      { \"id\": \"s1\", \"name\": \"Subsystem Name\", \"purpose\": \"Subsystem Purpose\", \"description\": \"...\", \"boundaries\": [\"...\"], \"critical_files\": [\"...\"], \"responsibilities\": [\"...\"] }\n")
              .append("    ],\n")
              .append("    \"architectural_facts\": [\n")
              .append("      { \"id\": \"f1\", \"subject\": \"...\", \"predicate\": \"...\", \"description\": \"...\", \"confidence\": 1.0, \"evidence\": [\"...\"] }\n")
              .append("    ],\n")
              .append("    \"dependencies\": \"(Key module relationships and third-party deps)\",\n")
              .append("    \"execution_instructions\": \"(Specific instructions for the external LLM)\",\n")
              .append("    \"evaluation\": \"(Self-evaluation of this candidate's quality)\"\n")
              .append("  },\n");
        }

        schemaSb.append("  \"actions\": [{ \"domain\": \"file\", \"operation\": \"WRITE\", \"target\": \"path/to/artifact\", \"description\": \"Action description\" }]\n")
          .append("}\n");
        sb.append(composer.composeJsonSchema(schemaSb.toString())).append("\n\n");

        sb.append("🧭 EVOLUTION DIRECTIVE\n\n")
          .append("Your goal is controlled divergence under constraints. Move the system into a meaningfully different evolutionary region.\n\n")
          .append("CONTEXT:\n")
          .append(basePrompt);

        return sb.toString();
    }

    /**
     * Spawns variants for the given strategies.
     */
    public List<JSONObject> spawn(String goal, List<DarwinStrategySeed> seeds, String basePrompt, String lineageContext, List<String> rejectedSiblings, boolean isMediated, TaskContext context) {
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
                        // Ensure ID is injected if missing from LLM response
                        if (!validated.has("id")) {
                            validated.put("id", "v-" + seed.getType().name().toLowerCase());
                        }
                        break;
                    }
                    context.log("[SPAWNER] Validation failed for " + seed.getType() + ". Retry " + (retry + 1) + "/2...");
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
          .append("You are a single-path evolutionary mutation engine.\n\n")
          .append("You do NOT generate multiple solutions.\n")
          .append("You do NOT compare alternatives.\n")
          .append("You do NOT output sets or lists of strategies.\n\n")
          .append("You perform one controlled mutation of a prior solution state.\n\n")
          .append("🧠 CORE RULE\n\n")
          .append("Each response MUST contain exactly ONE branch only.\n\n")
          .append("This branch must be:\n\n")
          .append("structurally distinct from all previously seen branches\n")
          .append("NOT a variation of earlier strategies\n")
          .append("NOT a recombination of prior ideas\n")
          .append("NOT a “better version” of the same approach\n\n")
          .append("You are evolving a lineage, not generating options.\n\n")
          .append("📌 INPUT CONTEXT\n\n")
          .append("GOAL (target task): (Implicit in workspace context)\n\n");

        if (context != null && context.getOrchestrationState() != null) {
            var metadata = context.getOrchestrationState().getMetadata();
            sb.append("CURRENT BEST UNDERSTANDING:\n")
              .append("- ").append(metadata.getOrDefault("current_understanding", "None")).append("\n\n");
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
            sb.append("  \"mediation_candidate\": {\n")
              .append("    \"prompt\": \"(The optimized prompt for the external LLM)\",\n")
              .append("    \"selected_files\": [\"(Select 8-16 key files from the candidate list)\"],\n")
              .append("    \"architecture_summary\": \"(Concise architecture mapping)\",\n")
              .append("    \"subsystems\": [\n")
              .append("      { \"id\": \"s1\", \"name\": \"Subsystem Name\", \"purpose\": \"Subsystem Purpose\", \"description\": \"...\", \"boundaries\": [\"...\"], \"critical_files\": [\"...\"], \"responsibilities\": [\"...\"] }\n")
              .append("    ],\n")
              .append("    \"architectural_facts\": [\n")
              .append("      { \"id\": \"f1\", \"subject\": \"...\", \"predicate\": \"...\", \"description\": \"...\", \"confidence\": 1.0, \"evidence\": [\"...\"] }\n")
              .append("    ],\n")
              .append("    \"dependencies\": \"(Key module relationships and third-party deps)\",\n")
              .append("    \"execution_instructions\": \"(Specific instructions for the external LLM)\",\n")
              .append("    \"evaluation\": \"(Self-evaluation of this candidate's quality)\"\n")
              .append("  },\n");
        }

        sb.append("  \"actions\": [{ \"domain\": \"file\", \"operation\": \"WRITE\", \"target\": \"path/to/artifact\", \"description\": \"Action description\" }]\n")
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
