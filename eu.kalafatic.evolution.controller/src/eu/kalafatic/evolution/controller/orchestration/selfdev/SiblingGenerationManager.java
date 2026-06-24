package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.EvolutionProgressPublisher;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Manages the sequential generation of siblings in an evolutionary generation.
 * Each sibling is generated based on its older siblings as exclusion constraints.
 */
public class SiblingGenerationManager {

    private final TrajectoryTerritoryMapper mapper;
    private final DarwinVariantSpawner spawner;
    private final AiService aiService;
    private final eu.kalafatic.evolution.controller.orchestration.SessionContainer container;

    public SiblingGenerationManager(eu.kalafatic.evolution.controller.orchestration.SessionContainer container, AiService aiService) {
        this.container = container;
        this.mapper = new TrajectoryTerritoryMapper(container);
        this.mapper.setAiService(aiService);
        this.spawner = new DarwinVariantSpawner(aiService);
        this.aiService = aiService;
    }

    public List<JSONObject> generateSiblings(
            GoalModel goal,
            EvolutionDimension activeDimension,
            int targetPopulation,
            String basePrompt,
            String lineageContext,
            List<String> rejectedSiblings,
            TaskContext context,
            SemanticGenome genome,
            EvolutionTree tree,
            String currentParentId,
            int generation,
            BranchVariant.ReasoningLevel reasoningLevel,
            boolean architectureEnabled,
            boolean implementationEnabled,
            IntentExpansionResult expansion,
            Orchestrator orchestrator) throws Exception {

        List<TrajectoryBlueprint> currentBlueprints = new ArrayList<>();
        List<JSONObject> uniqueVariants = new ArrayList<>();
        StringBuilder siblingMemoryBuilder = new StringBuilder();

        boolean isMediated = context.getBehaviorProfile().hasTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY);

        int attempts = 0;
        int maxAttempts = targetPopulation * 5;

        context.log("[SIBLING_MANAGER] Sequential Sibling Generation active. Target: " + targetPopulation);

        while (uniqueVariants.size() < targetPopulation && attempts < maxAttempts) {
            attempts++;
            int i = uniqueVariants.size();
            context.log("[SIBLING_MANAGER] Territory Exploration: Attempt " + attempts + " (Found: " + i + ")");

            try {
                String discoveryGoal = generation == 0 ? goal.getPrimaryAction() : goal.getPrimaryAction() + " (Mutation Gen " + generation + ")";
                String fullLineagePrompt = tree.reconstructLineagePrompt(currentParentId);

                // 1. Sequential Blueprint Discovery
                TrajectoryBlueprint bp = constructTrajectoryBlueprint(goal, expansion, currentBlueprints, generation,
                        siblingMemoryBuilder.toString(), mapper, discoveryGoal, fullLineagePrompt, activeDimension, context,
                        currentParentId, targetPopulation);

                if (bp != null) {
                    // STABILIZATION: Ensure unique BP ID across iterations/attempts to prevent tree & git conflicts
                    if (bp.getId() == null || bp.getId().equals("unique-blueprint-id") || bp.getId().contains("seed")) {
                        bp.setId("bp-iter" + context.getOrchestrationState().getIterationCount() + "-v" + i + "-" + System.currentTimeMillis());
                    }

                    if (activeDimension != null) {
                        bp.getEngineeringDimensions().put("active_dimension", activeDimension.getId());
                        bp.getEngineeringDimensions().put("active_dimension_description", activeDimension.getDescription());
                    }

                    // 2. Rigorous Technical Similarity Check (Search Memory)
                    if (isTechnicallyDuplicate(bp, currentBlueprints)) {
                        context.log("[SIBLING_MANAGER] Territory Rejected: Blueprint matches existing design species: " + bp.getStrategy());
                        continue;
                    }

                    currentBlueprints.add(bp);

                    // 3. Sequential Blueprint Materialization
                    EvolutionProgressPublisher.updateBranchStatus(context, bp.getId(), bp.getPhilosophy(), "analyzing", null);
                    EvolutionProgressPublisher.updateActiveModel(context, orchestrator != null ? (orchestrator.getOllama() != null ? orchestrator.getOllama().getModel() : "local") : "local", "Materializing Branch " + bp.getId());

                    JSONObject variant = null;
                    for (int retry = 0; retry < EvolutionConstants.MAX_MATERIALIZATION_RETRIES; retry++) {
                        context.log("[SIBLING_MANAGER] Materialization Attempt " + (retry + 1) + " for " + bp.getId());
                        variant = spawner.spawnSingleBlueprint(goal, bp, basePrompt, fullLineagePrompt + lineageContext, rejectedSiblings, siblingMemoryBuilder.toString(), isMediated, context, activeDimension, genome);
                        if (variant != null) break;
                    }

                    if (variant == null) {
                        context.log("[SIBLING_MANAGER] All materialization retries failed for " + bp.getId() + ". Triggering repair orchestration.");
                        variant = spawner.autoRepair(bp, context);
                    }

                    if (variant != null) {
                        // IMPLEMENTATION PLANNING
                        ImplementationPlanner planner = new ImplementationPlanner();
                        variant = planner.plan(variant, context);
                        variant = completeTrajectorySchema(variant, bp, context);

                        // 4. Semantic Vector Divergence Validation
                        if (isTechnicallyIdentical(variant, uniqueVariants)) {
                            context.log("[SIBLING_MANAGER] Territory Rejected: Materialized variant 90% identical to existing sibling.");
                            continue;
                        }

                        variant.put("reasoning_level", reasoningLevel.name());
                        variant.put("architecture_enabled", architectureEnabled);
                        variant.put("implementation_enabled", implementationEnabled);

                        // Evolutionary Identity
                        String branchSuffix = String.valueOf((char)('A' + uniqueVariants.size()));
                        String parentIdentity = "ROOT";
                        EvolutionNode parentNode = tree.getNode(currentParentId);
                        if (parentNode != null && parentNode.getMutationIdentity() != null) {
                            parentIdentity = parentNode.getMutationIdentity();
                            branchSuffix = parentIdentity + (uniqueVariants.size() + 1);
                        } else {
                            branchSuffix = "Branch " + branchSuffix;
                        }
                        variant.put("mutation_identity", branchSuffix);
                        variant.put("parent_identity", parentIdentity);

                        EvolutionProgressPublisher.updateBranchStatus(context, bp.getId(), bp.getPhilosophy(), "planned", null);
                        uniqueVariants.add(variant);

                        // 5. Update EvolutionTree with new mutation node
                        registerMutationInTree(variant, bp, tree, currentParentId, activeDimension, branchSuffix, basePrompt, generation, context, genome);

                        // 6. Accumulate Structured Sibling Memory for NEXT sequential discovery
                        MutationRecord mut = tree.getNode(variant.optString("id")).getMutationRecord();
                        siblingMemoryBuilder.append("EXPLORED TERRITORY: ").append(variant.optString("strategy")).append("\n")
                                           .append("  PHILOSOPHY: ").append(variant.optString("semantic_anchor")).append("\n")
                                           .append("  TECHNICAL_QUADRANT: ").append(mut.getEngineeringDimensions().get("execution_model")).append("\n")
                                           .append("  DIMENSIONS: ").append(mut.getEngineeringDimensions()).append("\n\n");
                    }
                }
            } catch (Exception e) {
                context.log("[SIBLING_MANAGER] Territory Exploration Error: " + e.getMessage());
            }
        }
        return uniqueVariants;
    }

    private TrajectoryBlueprint constructTrajectoryBlueprint(GoalModel goal, IntentExpansionResult expansion,
            List<TrajectoryBlueprint> currentBlueprints, int generation, String siblingMemoryBuilder,
            TrajectoryTerritoryMapper mapper, String discoveryGoal, String fullLineagePrompt, EvolutionDimension activeDimension, TaskContext context,
            String currentParentId, int targetPopulation) throws Exception {
        TrajectoryBlueprint bp = null;

        if (generation == 0 && expansion != null && expansion.getActiveDimensionId() != null) {
            EvolutionDimension activeDim = expansion.getUnresolvedDimensions().stream()
                .filter(d -> d.getId().equals(expansion.getActiveDimensionId()))
                .findFirst().orElse(null);

            if (activeDim != null && !activeDim.getCandidateBranches().isEmpty()) {
                for (BranchVariant bv : activeDim.getCandidateBranches()) {
                    boolean alreadyUsed = currentBlueprints.stream().anyMatch(existingBp ->
                        existingBp.getStrategy().equalsIgnoreCase(bv.getStrategy()));

                    if (!alreadyUsed) {
                        context.log("[SIBLING_MANAGER] Seeding blueprint from intent expansion dimension: " + activeDim.getId() + " -> " + bv.getStrategy());
                        bp = new TrajectoryBlueprint("bp-seed-" + bv.getId(), goal.getPrimaryAction(), bv.getStrategy());
                        bp.setPhilosophy(bv.getSurvivalArgument());
                        bp.setSurvivalArgument(bv.getSurvivalArgument());
                        bp.setTradeoffs(bv.getTradeoffs());
                        bp.setStrategyType(DarwinStrategyType.PROBABLE_SURVIVOR);
                        break;
                    }
                }
            }
        }

        if (bp == null) {
            EvolutionNode parentNode = container.getMemoryService(context.getProjectRoot()).getEvolutionTree().getNode(currentParentId);
            TrajectoryBlueprint parentBp = null;
            if (parentNode != null) {
                parentBp = new TrajectoryBlueprint(parentNode.getId(), goal.getPrimaryAction(), parentNode.getStrategy());
                parentBp.setPhilosophy(parentNode.getSemanticPhilosophy());
            }

            SiblingGenerationContext siblingCtx = new SiblingGenerationContext(goal, parentBp, activeDimension, targetPopulation);
            siblingCtx.setSiblingIndex(currentBlueprints.size());
            siblingCtx.setOlderSiblings(new ArrayList<>(currentBlueprints));
            bp = mapper.discoverNextSibling(siblingCtx, context);
        }
        return bp;
    }

    private void registerMutationInTree(JSONObject variant, TrajectoryBlueprint bp, EvolutionTree tree, String currentParentId, EvolutionDimension activeDimension, String branchSuffix, String basePrompt, int generation, TaskContext context, SemanticGenome genome) {
        EvolutionNode node = new EvolutionNode();
        node.setId(variant.optString("id"));
        node.setParentId(currentParentId);
        node.setIteration(context.getOrchestrationState().getIterationCount());
        node.setGeneration(generation);
        node.setStrategy(variant.optString("strategy"));
        node.setSemanticPhilosophy(variant.optString("semantic_anchor"));
        node.setActiveDimension(activeDimension != null ? activeDimension.getId() : "IMPLEMENTATION");
        node.setMutationIdentity(branchSuffix);
        node.setLlmPrompt(basePrompt);
        node.setLlmResponse(variant.toString());
        node.setStatus("KEPT");

        JSONArray journalArr = variant.optJSONArray("mutation_journal");
        if (journalArr != null) {
            for (int j = 0; j < journalArr.length(); j++) {
                node.getMutationJournal().add(journalArr.optString(j));
            }
        }

        EvolutionNode parentNode = tree.getNode(currentParentId);
        if (parentNode != null) {
            node.setParentStrengths(parentNode.getSelectionReason());
            node.setParentWeaknesses("Mutation required to satisfy dimension: " + (activeDimension != null ? activeDimension.getId() : "Implementation"));
        }

        JSONArray variantActions = variant.optJSONArray("actions");
        if (variantActions != null) {
            for (int j = 0; j < variantActions.length(); j++) {
                JSONObject vAction = variantActions.optJSONObject(j);
                if (vAction != null && ("WRITE".equals(vAction.optString("operation")) || "CREATE".equals(vAction.optString("operation")))) {
                    String target = vAction.optString("target");
                    String impl = vAction.optString("implementation");
                    if (target != null && impl != null) {
                        node.getCodeSnapshots().put(target, impl);
                    }
                }
            }
        }

        MutationRecord mut = new MutationRecord();
        mut.setStrategy(variant.optString("strategy"));
        mut.setSemanticAnchor(variant.optString("semantic_anchor"));
        mut.setPhilosophy(variant.optString("semantic_anchor"));
        mut.setReasoningFocus(variant.optString("reasoning_focus"));
        mut.setTradeoffs(variant.optString("tradeoffs"));
        mut.setSurvivalArgument(variant.optString("survival_argument"));
        JSONObject dims = variant.optJSONObject("engineering_dimensions");
        if (dims != null) {
            for (Object k : dims.keySet()) {
                String key = (String) k;
                String val = String.valueOf(dims.get(key));
                mut.getEngineeringDimensions().put(key, val);
                node.getEngineeringDimensions().put(key, val);
            }
            mut.setExecutionModel(dims.optString("execution_model"));
        }
        node.setMutationRecord(mut);

        Object fitnessObj = variant.opt("fitness_record");
        if (fitnessObj instanceof FitnessRecord) {
            node.setFitnessRecord((FitnessRecord) fitnessObj);
        }

        node.setGenomeSnapshot(genome.copy());
        tree.addNode(node);
        context.getKernelContext().getMemoryService().saveEvolutionTree();
        genome.recordMutation(mut);

        container.getEventBus().publish(new RuntimeEvent(RuntimeEventType.SIBLING_GENERATED, context.getSessionId(), node.getId(), node.getStrategy()));
    }

    private boolean isTechnicallyDuplicate(TrajectoryBlueprint bp, List<TrajectoryBlueprint> existing) {
        for (TrajectoryBlueprint other : existing) {
            if (bp.getStrategy().equalsIgnoreCase(other.getStrategy())) return true;
            if (bp.getPhilosophy() != null && other.getPhilosophy() != null &&
                bp.getPhilosophy().equalsIgnoreCase(other.getPhilosophy())) return true;
        }
        return false;
    }

    private boolean isTechnicallyIdentical(JSONObject variant, List<JSONObject> existing) {
        for (JSONObject other : existing) {
            if (variant.optString("semantic_anchor").equalsIgnoreCase(other.optString("semantic_anchor"))) return true;
            JSONObject dims1 = variant.optJSONObject("engineering_dimensions");
            JSONObject dims2 = other.optJSONObject("engineering_dimensions");
            if (dims1 != null && dims2 != null) {
                int matches = 0;
                int total = 0;
                for (String key : (java.util.Set<String>)(java.util.Set<?>)dims1.keySet()) {
                    if (dims2.has(key)) {
                        total++;
                        if (String.valueOf(dims1.get(key)).equalsIgnoreCase(String.valueOf(dims2.get(key)))) {
                            matches++;
                        }
                    }
                }
                if (total > 0 && (double) matches / total >= 0.9) return true;
            }
        }
        return false;
    }

    private JSONObject completeTrajectorySchema(JSONObject fragment, TrajectoryBlueprint bp, TaskContext context) {
        fragment.put("id", bp.getId());
        fragment.put("strategy_type", bp.getStrategyType().name());
        if (!fragment.has("strategy") || fragment.optString("strategy").isEmpty()) {
            fragment.put("strategy", "Architectural strategy for " + bp.getPhilosophy());
        }
        fragment.put("semantic_justification", bp.getPhilosophy());
        fragment.put("semantic_anchor", bp.getPhilosophy());
        if (!fragment.has("survival_argument") || fragment.optString("survival_argument").isEmpty()) {
            fragment.put("survival_argument", "Proposed as a divergent architectural candidate for " + bp.getPhilosophy());
        }
        if (!fragment.has("tradeoffs") || fragment.optString("tradeoffs").isEmpty()) {
            fragment.put("tradeoffs", "Standard trade-offs for " + bp.getStrategyType() + " architecture.");
        }
        if (!fragment.has("failure_risks") || fragment.optString("failure_risks").isEmpty()) {
            fragment.put("failure_risks", "Managed risks within " + bp.getStrategyType() + " evolutionary boundaries.");
        }
        JSONObject dimensions = fragment.optJSONObject("engineering_dimensions");
        if (dimensions == null) {
            dimensions = new JSONObject();
            fragment.put("engineering_dimensions", dimensions);
        }
        for (java.util.Map.Entry<String, String> entry : bp.getEngineeringDimensions().entrySet()) {
            String dimKey = entry.getKey();
            if (!dimensions.has(dimKey)) dimensions.put(dimKey, entry.getValue());
        }
        if (!dimensions.has("philosophy")) dimensions.put("philosophy", bp.getPhilosophy());
        return fragment;
    }
}
