package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.EvolutionProgressPublisher;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
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
     * Spawns variants based on blueprints.
     */
    public List<JSONObject> spawnBlueprints(String goal, List<TrajectoryBlueprint> blueprints, String basePrompt, String lineageContext, List<String> rejectedSiblings, boolean isMediated, TaskContext context) {
        List<JSONObject> variants = new ArrayList<>();
        Orchestrator orchestrator = context.getOrchestrator();

        List<JSONObject> currentRoundVariants = new ArrayList<>();

        for (TrajectoryBlueprint bp : blueprints) {
            context.log("[SPAWNER] Materializing trajectory from blueprint: " + bp.getId());
            EvolutionProgressPublisher.updateBranchStatus(context, bp.getId(), bp.getPhilosophy(), "active", null);
            EvolutionProgressPublisher.updateActiveModel(context, orchestrator != null ? (orchestrator.getOllama() != null ? orchestrator.getOllama().getModel() : "local") : "local", "Generating Branch " + (variants.size() + 1) + " of " + blueprints.size());

            String bpPrompt = buildBlueprintPrompt(bp, basePrompt, lineageContext, rejectedSiblings, currentRoundVariants, isMediated, context);
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
                variants.add(validated);
                currentRoundVariants.add(validated);
                context.log("[SPAWNER] Successfully materialized blueprint: " + bp.getId());
                double score = validated.optDouble("score", 0.0);
                EvolutionProgressPublisher.updateBranchStatus(context, bp.getId(), bp.getPhilosophy(), "complete", Double.isNaN(score) ? null : score);
            } else {
                context.log("[SPAWNER] Materialization retries failed for " + bp.getId() + ". Attempting deterministic auto-repair.");
                JSONObject repaired = autoRepair(bp, context);
                if (repaired != null) {
                    variants.add(repaired);
                    currentRoundVariants.add(repaired);
                    context.log("[SPAWNER] Successfully auto-repaired blueprint: " + bp.getId());
                } else {
                    context.log("[SPAWNER] CRITICAL: Failed to materialize or repair mandatory blueprint: " + bp.getId());
                }
            }
        }
        return variants;
    }

    private JSONObject completeTrajectorySchema(JSONObject fragment, TrajectoryBlueprint bp, TaskContext context) {
        // Ensure core fields exist and are consistent with blueprint
        fragment.put("id", bp.getId());
        fragment.put("strategy_type", bp.getStrategyType().name());
        fragment.put("semantic_justification", bp.getPhilosophy());

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
            JSONObject action = new JSONObject();
            action.put("domain", "kernel");
            action.put("operation", "ANALYZE");
            action.put("target", "workspace");
            action.put("description", "Bootstrap " + bp.getId() + " architectural strategy.");
            actions.put(action);
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

    private String buildBlueprintPrompt(TrajectoryBlueprint bp, String basePrompt, String lineageContext, List<String> rejectedSiblings, List<JSONObject> currentRoundVariants, boolean isMediated, TaskContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("SYSTEM:\n")
          .append("You are an engineering trajectory materializer. You must MATERIALIZE a SPECIFIC BLUEPRINT.\n\n")
          .append("CRITICAL: You are NOT deciding the divergence. The divergence is PREDEFINED by the ORCHESTRATOR.\n")
          .append("The ORCHESTRATOR owns the evolutionary planning, while you ONLY materialize a constrained local trajectory.\n")
          .append("Your task is to CONSTRAIN your output to the technical characteristics required by the blueprint.\n\n")
              .append("MANDATORY: Evolve the selected trajectory to address identified architectural pressures.\n")
              .append("Do NOT restart brainstorming. Mutate the ancestor to improve stability, resilience, or maintainability.\n\n")
          .append("BLUEPRINT TO MATERIALIZE:\n")
          .append("ID: ").append(bp.getId()).append("\n")
          .append("Goal: ").append(bp.getGoal()).append("\n")
          .append("Philosophy: ").append(bp.getPhilosophy()).append("\n")
          .append("Target Engineering Dimensions: ").append(bp.getEngineeringDimensions()).append("\n")
          .append("Required Characteristics: ").append(bp.getRequiredCharacteristics()).append("\n")
          .append("Forbidden Overlaps: ").append(bp.getForbiddenOverlaps()).append("\n")
          .append("Architectural Direction: ").append(bp.getArchitecturalDirection()).append("\n\n")
          .append("STRICT MATERIALIZATION RULES:\n")
          .append("- Adhere STRICTLY to the philosophy and required characteristics.\n")
          .append("- DO NOT propose anything mentioned in 'Forbidden Overlaps'.\n")
          .append("- If you fail to stay within the blueprint constraints, the trajectory will be REJECTED.\n")
          .append("- Provide CONCRETE technical actions and steps. Do NOT use placeholders like '<path.java>' or 'precise engineering strategy'.\n")
          .append("- Every field MUST contain real, specific technical detail.\n")
          .append("- Output your response within <BEGIN_DARWIN_JSON> and <END_DARWIN_JSON> tags.\n\n");

        if (isMediated) {
            sb.append("MEDIATED MODE COGNITION RULES (CRITICAL):\n")
              .append("- Focus on ARCHITECTURAL UNDERSTANDING, not code.\n")
              .append("- USE ONLY REAL repository evidence from the provided candidate list.\n")
              .append("- Follow the 4-Phase Darwin Process: Scan, Seed, Branching, and Consolidation.\n")
              .append("- Focus on INFORMATION VALUE and DENSITY: smallest set of files explaining architecture, flow, and mutation points.\n")
              .append("- You MUST select 4-16 high-signal files for the FINAL mediation package.\n")
              .append("- CONSOLIDATION RULES: KEEP if in 2+ lineages, extreme centrality, or critical flow; REMOVE if redundant or leaf-only.\n")
              .append("- Ranked selected files by importance (1 = highest) in the reasoning focus.\n")
              .append("- Strictly avoid context bloat (> 16 files is PENALIZED).\n")
              .append("- Your goal is to produce a high-quality mediation candidate for external LLM processing.\n\n");

            if (context != null && context.getOrchestrationState() != null) {
                var metadata = context.getOrchestrationState().getMetadata();
                if (metadata.get("current_understanding") != null) {
                    sb.append("→ CURRENT EVOLVED UNDERSTANDING: ").append(metadata.get("current_understanding")).append("\n");
                }
                if (metadata.get("current_reasoning_focus") != null) {
                    sb.append("→ CURRENT REASONING FOCUS: ").append(metadata.get("current_reasoning_focus")).append("\n");
                }
                if (metadata.get("current_selected_files") != null) {
                    sb.append("→ CURRENT SELECTED FILES: ").append(metadata.get("current_selected_files")).append("\n");
                }
                sb.append("\n");
            }
        }

        if (lineageContext != null && !lineageContext.isEmpty()) {
            sb.append("LINEAGE CONTINUITY (PERSISTENT EVOLUTION):\n")
              .append("You are evolving a surviving lineage. Inherit the successes and avoid the failures of your ancestors.\n")
              .append(lineageContext).append("\n");

            if (rejectedSiblings != null && !rejectedSiblings.isEmpty()) {
                sb.append("REJECTED SIBLING AWARENESS (FORBIDDEN PHILOSOPHIES):\n")
                  .append("The following trajectories were REJECTED in previous generations. You ARE STRICTLY PROHIBITED from re-proposing or pivoting back to these engineering philosophies or their semantic variations:\n");
                for (String rejected : rejectedSiblings) {
                    sb.append("- ").append(rejected).append("\n");
                }
                sb.append("\n");
            }
        }

        if (!currentRoundVariants.isEmpty()) {
            sb.append("FORBIDDEN PHILOSOPHIES (SIBLING MUTATION PRESSURE):\n")
              .append("The following engineering philosophies have already been claimed in this generation. You MUST intentionally mutate AGAINST them to ensure maximum divergence.\n\n");
            for (JSONObject v : currentRoundVariants) {
                sb.append("--- OCCUPIED: ").append(v.optString("id")).append(" ---\n")
                  .append("Strategy: ").append(v.optString("strategy")).append("\n")
                  .append("Philosophy: ").append(v.optString("semantic_justification")).append("\n")
                  .append("Engineering Dimensions: ").append(v.optJSONObject("engineering_dimensions")).append("\n\n");
            }
        }

        sb.append("CONTEXT:\n")
          .append(basePrompt).append("\n\n")
          .append("REQUIRED SCHEMA (CRITICAL: PROVIDE SPECIFIC TECHNICAL VALUES, NO PLACEHOLDERS):\n")
          .append("{\n")
          .append("  \"id\": \"").append(bp.getId()).append("\",\n")
          .append("  \"strategy_type\": \"").append(bp.getStrategyType().name()).append("\",\n")
          .append("  \"strategy\": \"(Concrete description of implementation)\",\n")
          .append("  \"reasoning_focus\": \"(Specific architectural focus)\",\n")
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
            sb.append("  \"mediation_candidate\": {\n")
              .append("    \"prompt\": \"(The optimized prompt for the external LLM)\",\n")
              .append("    \"selected_files\": [\"(Select 8-16 key files from the candidate list)\"],\n")
              .append("    \"architecture_summary\": \"(Concise architecture mapping)\",\n")
              .append("    \"dependencies\": \"(Key module relationships and third-party deps)\",\n")
              .append("    \"execution_instructions\": \"(Specific instructions for the external LLM)\",\n")
              .append("    \"evaluation\": \"(Self-evaluation of this candidate's quality)\"\n")
              .append("  },\n");
        }

        sb.append("  \"selected_files\": [\"(Select 4-16 key files from the candidate list)\"],\n")
          .append("  \"survival_argument\": \"(Technical justification)\",\n")
          .append("  \"tradeoffs\": \"(Technical tradeoffs)\",\n")
          .append("  \"failure_risks\": \"(Potential failure modes)\",\n")
          .append("  \"semantic_justification\": \"").append(bp.getPhilosophy()).append("\",\n")
          .append("  \"projected_steps\": [\"step 1\", \"step 2\"],\n")
          .append("  \"expected_outputs\": [\"App.java\"],\n")
          .append("  \"score\": 0.8,\n")
          .append("  \"actions\": [{ \"domain\": \"file\", \"operation\": \"WRITE\", \"target\": \"src/main/java/App.java\", \"description\": \"Action description\" }]\n")
          .append("}");

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
        sb.append("SYSTEM:\n")
          .append("You are an adaptive engineering evolution engine generating ONE Darwin evolutionary branch trajectory.\n\n")
          .append("CRITICAL OBJECTIVE:\n")
          .append("Realize a specific COMPETING ENGINEERING FUTURE. Do NOT think in terms of roles (e.g., 'implementation', 'analytical').\n")
          .append("Think in terms of COMPETING ARCHITECTURAL PHILOSOPHIES and execution trajectories.\n\n")
          .append("DIVERGENCE RULES (MANDATORY):\n")
          .append("- YOU ARE STRICTLY PROHIBITED FROM GENERATING SEMANTIC REWRITES.\n")
          .append("- Every trajectory MUST realize a technical philosophy that DIVERGES from siblings and parents.\n")
          .append("- If sibling trajectories are provided, you MUST intentionally mutate AGAINST them.\n")
          .append("- You MUST force ARCHITECTURAL DIVERGENCE across 9 dimensions:\n")
          .append("  1. Engineering Philosophy\n")
          .append("  2. Execution Model\n")
          .append("  3. Abstraction Depth\n")
          .append("  4. Modularity Approach\n")
          .append("  5. Testing Strategy\n")
          .append("  6. Extensibility\n")
          .append("  7. Dependency Assumptions\n")
          .append("  8. Runtime Behavior\n")
          .append("  9. Risk Acceptance\n")
          .append("- Each trajectory MUST realize a distinct engineering future with different technical assumptions.\n")
          .append("- Identify the 'DIVERGENCE AXIS' and PIVOT to an unexplored technical quadrant.\n")
          .append("- Maximize TECHNICAL CONTRAST from all other proposed or rejected trajectories.\n")
          .append("- Output EXACTLY ONE JSON object.\n")
          .append("- Do NOT generate an array.\n")
          .append("- strategy_type is FIXED to: ").append(seed.getType()).append("\n")
          .append("- The variant MUST be conceptually distinct from previous trajectories.\n")
          .append("- Mutate the PHILOSOPHY and architectural dimensions, not just the wording.\n")
          .append("- Focus on concrete technical assumptions and operational strategies.\n")
          .append("- Output your response within <BEGIN_DARWIN_JSON> and <END_DARWIN_JSON> tags.\n")
          .append("- Do NOT include conversation or markdown blocks.\n\n");

        if (isMediated) {
            sb.append("MEDIATED MODE COGNITION RULES (CRITICAL):\n")
              .append("- You are performing ITERATIVE REPOSITORY COGNITION.\n")
              .append("- Your goal is to EVOLVE ARCHITECTURAL UNDERSTANDING, not code.\n")
              .append("- DO NOT hallucinate synthetic runtime context, sensors, or memory systems.\n")
              .append("- USE ONLY REAL repository evidence from the provided candidate list (files, structure, technologies).\n")
              .append("- Follow the 4-Phase Darwin Process: Scan, Seed, Branching, and Consolidation.\n")
              .append("- Focus on INFORMATION VALUE and DENSITY: smallest set of files explaining architecture, flow, and mutation points.\n")
              .append("- You MUST select 4-16 high-signal files for the FINAL mediation package.\n")
              .append("- CONSOLIDATION RULES: KEEP if in 2+ lineages, extreme centrality, or critical flow; REMOVE if redundant or leaf-only.\n")
              .append("- Ranked selected files by importance (1 = highest) in the reasoning focus.\n")
              .append("- Strictly avoid context bloat (> 16 files is PENALIZED).\n")
              .append("- Focus on producing high-quality mediation candidates (prompt, context, instructions).\n")
              .append("- Strictly prohibit invented APIs or fictitious infrastructure.\n\n");

            if (context != null && context.getOrchestrationState() != null) {
                var metadata = context.getOrchestrationState().getMetadata();
                if (metadata.get("current_understanding") != null) {
                    sb.append("→ CURRENT EVOLVED UNDERSTANDING: ").append(metadata.get("current_understanding")).append("\n");
                }
                if (metadata.get("current_reasoning_focus") != null) {
                    sb.append("→ CURRENT REASONING FOCUS: ").append(metadata.get("current_reasoning_focus")).append("\n");
                }
                if (metadata.get("current_selected_files") != null) {
                    sb.append("→ CURRENT SELECTED FILES: ").append(metadata.get("current_selected_files")).append("\n");
                }
                sb.append("\n");
            }
        }

        if (lineageContext != null && !lineageContext.isEmpty()) {
            sb.append("LINEAGE CONTINUITY (PERSISTENT EVOLUTION):\n")
              .append("You are evolving a surviving lineage. Inherit the successes and avoid the failures of your ancestors.\n")
              .append(lineageContext).append("\n");

            sb.append("FORBIDDEN ARCHITECTURAL ASSUMPTIONS (LINEAGE PRESSURE):\n")
              .append("- Do NOT reuse the same abstraction level as the ancestor if you are a mutation.\n")
              .append("- Do NOT propose the same implementation style.\n\n");

            if (rejectedSiblings != null && !rejectedSiblings.isEmpty()) {
                sb.append("REJECTED SIBLING AWARENESS (FORBIDDEN PHILOSOPHIES):\n")
                  .append("The following trajectories were REJECTED in previous generations. You ARE STRICTLY PROHIBITED from re-proposing or pivoting back to these engineering philosophies or their semantic variations:\n");
                for (String rejected : rejectedSiblings) {
                    sb.append("- ").append(rejected).append("\n");
                }
                sb.append("\n");
            }
        }

        if (seed.getType() == DarwinStrategyType.PROBABLE_SURVIVOR) {
            sb.append("TRAJECTORY GOAL: PROBABLE SURVIVOR\n")
              .append("Propose the most direct and reliable engineering path to solve the goal.\n\n");
        }

        if (seed.getType() == DarwinStrategyType.PHILOSOPHY_MUTATION) {
            sb.append("TRAJECTORY GOAL: PHILOSOPHY MUTATION\n")
              .append("Identify the core engineering philosophy of the surviving trajectory and RADICALLY MUTATE it.\n")
              .append("Example: If survivor is 'Service-Oriented', mutate to 'Atomic Utility' or 'Event-Driven'.\n\n");
        }

        if (seed.getType() == DarwinStrategyType.MAXIMAL_DIVERGENCE) {
            sb.append("TRAJECTORY GOAL: MAXIMAL_DIVERGENCE\n")
              .append("Maximize conceptual distance from ALL previous trajectories. Explore unconventional engineering tradeoffs.\n\n");
        }

        if (!currentRoundVariants.isEmpty()) {
            sb.append("FORBIDDEN PHILOSOPHIES (SIBLING MUTATION PRESSURE):\n")
              .append("The following engineering philosophies have already been claimed in this generation. You ARE STRICTLY PROHIBITED from proposing a semantic variation of these. You MUST intentionally mutate AGAINST them.\n\n");
            for (JSONObject v : currentRoundVariants) {
                sb.append("--- OCCUPIED: ").append(v.optString("strategy_type")).append(" ---\n")
                  .append("Strategy: ").append(v.optString("strategy")).append("\n")
                  .append("Philosophy: ").append(v.optString("semantic_justification")).append("\n")
                  .append("Tradeoffs: ").append(v.optString("tradeoffs")).append("\n")
                  .append("Operational Behavior: ").append(v.optJSONArray("projected_steps")).append("\n\n");
            }
            sb.append("INSTRUCTION FOR THIS BRANCH:\n")
              .append("- Identify the 'DIVERGENCE AXIS' (e.g., Abstraction Depth, State Management, Runtime Strategy, Dependency Complexity).\n")
              .append("- PIVOT to an unexplored technical quadrant.\n")
              .append("- Maximize TECHNICAL CONTRAST from the occupied space above.\n\n");
        }

        if (seed.getInterpretation() != null) {
            sb.append("TARGET ENGINEERING FUTURE:\n")
              .append("Interpretation: ").append(seed.getInterpretation()).append("\n")
              .append("Architectural Assumption: ").append(seed.getAssumption()).append("\n")
              .append("Goal Detail: ").append(seed.getFutureGoal()).append("\n\n");
        }

        sb.append("TRAJECTORY CONTEXT:\n")
          .append(seed.getInstructions()).append("\n\n")
          .append("USER GOAL AND WORKSPACE CONTEXT:\n")
          .append(basePrompt).append("\n\n")
          .append("CRITICAL: If an EXPECTED TARGET ARTIFACT is provided in the context, you MUST use it in your actions. Do NOT use placeholders like 'actual path' or 'WRITE or DELETE'.\n\n");

        sb.append("REQUIRED SCHEMA (CRITICAL: PROVIDE SPECIFIC TECHNICAL VALUES):\n")
               .append("<BEGIN_DARWIN_JSON>\n")
               .append("{\n")
               .append("  \"id\": \"v-").append(seed.getType().name().toLowerCase()).append("\",\n")
               .append("  \"strategy_type\": \"").append(seed.getType()).append("\",\n")
               .append("  \"strategy\": \"precise engineering strategy for this trajectory\",\n")
               .append("  \"reasoning_focus\": \"specific architectural focus for this mediated trajectory\",\n")
               .append("  \"selected_files\": [\"src/main/java/Main.java\"],\n")
               .append("  \"engineering_dimensions\": {\n")
               .append("    \"philosophy\": \"specific philosophy for this branch\",\n")
               .append("    \"execution_model\": \"atomic/service/reactive/etc\",\n")
               .append("    \"abstraction_depth\": \"low/medium/high\",\n")
               .append("    \"modularity_approach\": \"monolithic/modular/functional/etc\",\n")
               .append("    \"testing_strategy\": \"unit/integration/tdd/etc\",\n")
               .append("    \"extensibility\": \"low/medium/high\",\n")
               .append("    \"dependency_assumptions\": \"none/internal/external\",\n")
               .append("    \"runtime_behavior\": \"deterministic/async/etc\",\n")
               .append("    \"risk_acceptance\": \"conservative/experimental/etc\"\n")
               .append("  },\n");

        if (isMediated) {
            sb.append("  \"mediation_candidate\": {\n")
              .append("    \"prompt\": \"(The optimized prompt for the external LLM)\",\n")
              .append("    \"selected_files\": [\"(Select 4-16 HIGH-SIGNAL files from the candidate list)\"],\n")
              .append("    \"architecture_summary\": \"(Concise architecture mapping)\",\n")
              .append("    \"dependencies\": \"(Key module relationships and third-party deps)\",\n")
              .append("    \"execution_instructions\": \"(Specific instructions for the external LLM)\",\n")
              .append("    \"evaluation\": \"(Self-evaluation of this candidate's quality)\"\n")
              .append("  },\n");
        }

        sb.append("  \"survival_argument\": \"why this specific future should survive technically\",\n")
          .append("  \"tradeoffs\": \"specific technical tradeoffs compared to other trajectories\",\n")
          .append("  \"failure_risks\": \"potential failure modes for this trajectory\",\n")
          .append("  \"pros_cons\": \"detailed pros/cons analysis\",\n")
          .append("  \"semantic_justification\": \"engineering philosophy justification\",\n")
          .append("  \"projected_steps\": [\"next logical step 1\", \"next logical step 2\"],\n")
          .append("  \"expected_outputs\": [\"expected file/artifact 1\"],\n")
          .append("  \"score\": 0.5,\n")
          .append("  \"suffix\": \"").append(seed.getType().name().toLowerCase()).append("\",\n")
          .append("  \"actions\": [\n")
          .append("    {\n")
          .append("      \"domain\": \"file\",\n")
          .append("      \"operation\": \"WRITE\",\n")
          .append("      \"target\": \"src/main/java/Main.java\",\n")
          .append("      \"description\": \"specific technical instruction for this action\"\n")
          .append("    }\n")
          .append("  ],\n")
          .append("  \"hypothesis\": {\n")
          .append("    \"description\": \"testable hypothesis for why this trajectory works\",\n")
          .append("    \"expected_effects\": [\"measurable effect 1\"]\n")
          .append("  },\n")
          .append("  \"expected_effect\": {\n")
          .append("    \"short_term\": \"expected result after execution\",\n")
          .append("    \"long_term\": \"long-term architectural impact\",\n")
          .append("    \"risk\": 0.5,\n")
          .append("    \"reversibility\": 1.0\n")
          .append("  }\n")
          .append("}\n")
          .append("<END_DARWIN_JSON>");

        return sb.toString();
    }
}
