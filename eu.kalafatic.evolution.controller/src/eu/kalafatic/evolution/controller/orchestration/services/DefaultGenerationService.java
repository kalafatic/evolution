package eu.kalafatic.evolution.controller.orchestration.services;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.io.File;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.EvolutionStage;
import eu.kalafatic.evolution.controller.orchestration.EvolutionProgressPublisher;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.goal.SemanticEnvelope;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionNode;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionTree;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.engines.DimensionEngine;
import eu.kalafatic.evolution.controller.orchestration.engines.LineageEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SiblingGenerationManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SemanticGenome;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionDimension;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinVariantSpawner;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinFitnessRanker;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinSyntheticVariantFactory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.AbstractionLevel;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult;
import eu.kalafatic.evolution.controller.orchestration.behavior.ExecutionPolicy;
import eu.kalafatic.evolution.controller.orchestration.behavior.PolicyResolver;
import eu.kalafatic.evolution.controller.orchestration.behavior.PromptComposer;
import eu.kalafatic.evolution.controller.orchestration.behavior.DarwinPromptBuilder;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.VariantExecutionContext;
import eu.kalafatic.evolution.controller.orchestration.KernelFactory;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;

public class DefaultGenerationService implements GenerationService {
    private final DimensionEngine dimensionEngine = new DimensionEngine();
    private final LineageEngine lineageEngine = new LineageEngine();
    private WinnerService winnerService;
    private AiService aiService;

    @Override
    public void setWinnerService(WinnerService winnerService) { this.winnerService = winnerService; }
    @Override
    public void setAiService(AiService aiService) { this.aiService = aiService; }
    @Override
    public DimensionEngine getDimensionEngine() { return dimensionEngine; }
    @Override
    public LineageEngine getLineageEngine() { return lineageEngine; }

    @Override
    public List<BranchVariant> generateProposals(TaskContext context, GoalModel goal, IterationManager manager) throws Exception {
        Iteration currentIterationModelImpl = manager.getCurrentIterationModel();
        String iterId = currentIterationModelImpl != null ? currentIterationModelImpl.getId() : "default";

        eu.kalafatic.evolution.controller.kernel.EvolutionProfile profile = context.getExecutionProfile();
        String originalBranch = null;
        String baseCommit = null;
        if (profile.requiresRepository() && manager.getGitManager().isGitRepository()) {
            originalBranch = manager.getGitManager().getCurrentBranch();
            baseCommit = manager.getGitManager().getHeadCommit();
        }

        EvolutionProgressPublisher.updateStage(context, EvolutionStage.ANALYZE_PARENT);
        Evaluator.Evaluation initialEval = manager.getEvaluator().evaluateWithSnapshot();
        StateSnapshot snapshot = initialEval.snapshot;

        Trajectory trajectory = null;
        IterationRecord lastWinner = context.getKernelContext().getMemoryService().getRecords().stream()
                .filter(r -> "ACTIVE".equals(r.getActivationState()))
                .reduce((first, second) -> second)
                .orElse(null);

        if (lastWinner != null && lastWinner.getBranchId() != null) {
             trajectory = context.getSemanticWorkspace().getTrajectoryMemory().getTrajectory(lastWinner.getBranchId());
        }

        if (trajectory == null) {
            trajectory = new Trajectory("traj-" + iterId, goal.getPrimaryAction());
            context.getSemanticWorkspace().getTrajectoryMemory().recordTrajectory(trajectory);
            EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
            if (tree.getRootId() == null) {
                EvolutionNode root = new EvolutionNode();
                root.setId("root-" + iterId);
                root.setStrategy("Evolutionary Root: " + goal.getPrimaryAction());
                root.setSemanticPhilosophy("Initial evolutionary root");
                root.setIteration(0);
                root.setStatus("ROOT");
                tree.addNode(root);
                context.getKernelContext().getMemoryService().saveEvolutionTree();
            }
        }

        FailureMemory failureMemory = context.getKernelContext().getMemoryService().getFailureMemory();
        manager.getSessionContainer().getEventBus().publish(new RuntimeEvent(RuntimeEventType.MUTATING, context.getSessionId(), "DarwinEngine", goal.getPrimaryAction()));

        EvolutionaryPressureVector pressure = null;
        if (trajectory != null) {
            pressure = manager.getSessionContainer().getPressureEngine().analyze(trajectory, context);
            trajectory.recordPressure(pressure);
        }

        EvolutionProgressPublisher.updateStage(context, EvolutionStage.GENERATE_BRANCH);
        List<BranchVariant> rawVariants = generateVariants(goal, snapshot, failureMemory, trajectory, pressure, context, manager);

        manager.getSessionContainer().getEventBus().publish(new RuntimeEvent(RuntimeEventType.BRANCH_CREATED, context.getSessionId(), "DarwinEngine", rawVariants.size()));

        if (rawVariants.isEmpty()) return Collections.emptyList();

        List<BranchVariant> evaluationCandidates = rawVariants.stream()
                .filter(v -> {
                    EvolutionNode node = context.getKernelContext().getMemoryService().getEvolutionTree().getNode(v.getId());
                    return node != null && !"REJECTED_SEMANTIC".equals(node.getStatus());
                })
                .collect(Collectors.toList());

        if (profile.useImplementation()) {
            String baseCommitFinal = baseCommit;
            EvolutionaryPressureVector pressureFinal = pressure;
            evaluationCandidates.parallelStream().forEach(variant -> {
                try {
                    evaluateVariantParallel(variant, manager.getTaskPlanner(), context, baseCommitFinal, pressureFinal, manager);
                } catch (Exception e) {}
            });
        } else {
            for (BranchVariant variant : evaluationCandidates) {
                variant.setSuccess(true);
                variant.setScore(0.95);
            }
        }

        for (BranchVariant v : rawVariants) {
            EvolutionNode node = context.getKernelContext().getMemoryService().getEvolutionTree().getNode(v.getId());
            if (node != null && "REJECTED_SEMANTIC".equals(node.getStatus())) {
                v.setSuccess(false);
                v.setScore(Math.min(v.getScore(), 0.1));
            }
        }

        eu.kalafatic.evolution.controller.orchestration.capability.contracts.ISchedulingContract scheduler = manager.getSessionContainer().getCapabilityRegistry().getContractImplementation(eu.kalafatic.evolution.controller.orchestration.capability.contracts.ISchedulingContract.ID, eu.kalafatic.evolution.controller.orchestration.capability.contracts.ISchedulingContract.class);
        eu.kalafatic.evolution.controller.execution.ScheduledExecutionPlan executionPlan;
        if (scheduler != null) {
            executionPlan = (eu.kalafatic.evolution.controller.execution.ScheduledExecutionPlan) scheduler.schedule(rawVariants, context);
        } else {
            executionPlan = new eu.kalafatic.evolution.controller.execution.ScheduledExecutionPlan(rawVariants, "Manual fallback", eu.kalafatic.evolution.controller.execution.ExecutionBudget.defaultProfile());
        }
        List<BranchVariant> variants = executionPlan.getScheduledVariants();
        context.getOrchestrationState().getMetadata().put("executionPlan", executionPlan);

        for (BranchVariant v : variants) {
            eu.kalafatic.evolution.controller.trajectory.TrajectoryAnalysisRecord tar = new eu.kalafatic.evolution.controller.trajectory.TrajectoryAnalysisRecord();
            tar.setIterationId(iterId);
            tar.setBranchId(v.getId());
            tar.setStrategy(v.getStrategy());
            tar.setFitnessScore(v.getScore());
            context.getKernelContext().getMemoryService().saveTrajectoryAnalysis(tar);
        }
        context.getKernelContext().getMemoryService().flush();
        return variants;
    }

    @Override
    public List<BranchVariant> generateVariants(GoalModel goal, StateSnapshot snapshot, FailureMemory failureMemory, Trajectory trajectory, EvolutionaryPressureVector pressure, TaskContext context, IterationManager manager) throws Exception {
        eu.kalafatic.evolution.controller.kernel.EvolutionProfile profile = context.getExecutionProfile();
        int intensity = profile.getIntensity();

        long bitState = context.getOrchestrationState().getBitState();
        PolicyResolver policyResolver = new PolicyResolver();
        ExecutionPolicy policy = policyResolver.resolve(bitState);

        StringBuilder state = new StringBuilder();
        state.append("Current Goal: ").append(goal.getPrimaryAction()).append("\n");
        if (snapshot != null) {
            state.append("\n--- CURRENT STATE SNAPSHOT ---\n");
            state.append("Build Status: ").append(snapshot.build.status).append("\n");
            state.append("Build Errors: ").append(snapshot.build.errorCount).append(" (").append(snapshot.build.errorTypes).append(")\n");
            state.append("Tests: ").append(snapshot.tests.passed).append("/").append(snapshot.tests.total).append(" passed\n");
        }

        Object expansionObj = context.getOrchestrationState().getMetadata().get("intentExpansion");
        IntentExpansionResult expansion = eu.kalafatic.evolution.controller.parsers.JsonUtils.restoreFromMetadata(expansionObj, IntentExpansionResult.class, "intentExpansion", context);

        String projectStructure = (String) context.getOrchestrationState().getMetadata().get("projectStructure");
        if (projectStructure != null) state.append("\n--- REPOSITORY STRUCTURE ---\n").append(projectStructure).append("\n");

        eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot snapshotMed = (eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot) context.getOrchestrationState().getMetadata().get("mediatedSnapshot");
        if (snapshotMed != null) {
            state.append("\n--- SEMANTIC REPOSITORY SNAPSHOT ---\n");
            eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator curator = new eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator();
            List<String> candidates = curator.selectContext(snapshotMed, goal.getPrimaryAction(), 32);
            state.append("\n--- HIGH-VALUE CANDIDATE FILES ---\n");
            candidates.forEach(path -> state.append("- ").append(path).append("\n"));
        }

        List<IterationRecord> records = manager.getMemoryService().getRecords();
        List<IterationRecord> activeRecords = manager.getMemoryService().getActiveLineage();
        String history = activeRecords.isEmpty() ? "No active lineage history." :
                      activeRecords.stream()
                        .map(r -> "- Iteration " + r.getIteration() + ": " + r.getStrategy())
                        .collect(Collectors.joining("\n"));
        state.append("\n--- ACTIVE LINEAGE HISTORY ---\n").append(history).append("\n");

        DarwinPromptBuilder baseBuilder = new DarwinPromptBuilder(context);
        PromptComposer promptComposer = new PromptComposer();
        baseBuilder.addSystem(promptComposer.composeSystem(policy)).addGoal(goal.getPrimaryAction()).addReality().addSemanticEnvelope();
        String basePrompt = baseBuilder.build();

        int expansionValue = 5;
        if (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null) {
            String sessionId = context.getSessionId();
            eu.kalafatic.evolution.model.orchestration.ChatSession chatSession = context.getOrchestrator().getAiChat().getSessions().stream()
                .filter(s -> s.getId().equals(sessionId)).findFirst().orElse(null);
            if (chatSession != null) expansionValue = chatSession.getExpansion();
        }

        int branchingLimit = 4;
        if (expansionValue <= 3) branchingLimit = 2;
        else if (expansionValue >= 8) branchingLimit = 6;

        EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
        String currentParentId = tree.getCurrentWinnerId();
        currentParentId = manager.getLineageEngine().handleBranchRevival(tree, currentParentId, context);
        if (currentParentId == null && tree.getRootId() != null) currentParentId = tree.getRootId();

        SemanticGenome genome = dimensionEngine.createGenome(goal, expansion, context);
        EvolutionDimension activeDimension = dimensionEngine.selectNextDimension(genome, context);

        StringBuilder lineageBuilder = new StringBuilder();
        List<String> rejectedSiblings = new ArrayList<>();
        List<IterationRecord> survivors = records.stream()
                .filter(r -> "ACTIVE".equals(r.getActivationState()) || "KEPT".equals(r.getActivationState()))
                .collect(Collectors.toList());
        if (survivors.isEmpty()) survivors = activeRecords;

        if (!survivors.isEmpty()) {
            lineageBuilder.append("### EVOLUTIONARY ANCESTORS ###\n");
            for (IterationRecord ancestor : survivors) {
                lineageBuilder.append("ANCESTOR LINEAGE: ").append(ancestor.getBranchId()).append("\n");
                lineageBuilder.append("STRATEGY: ").append(ancestor.getStrategy()).append("\n\n");
            }
            rejectedSiblings = records.stream()
                    .filter(r -> !"ACTIVE".equals(r.getActivationState()) && !"KEPT".equals(r.getActivationState()))
                    .map(r -> r.getStrategy() + " (Iteration " + r.getIteration() + ")")
                    .distinct()
                    .collect(Collectors.toList());
        }
        String lineageContext = lineageBuilder.toString();

        AbstractionLevel lockedLevel = context.getOrchestrationState().getLockedAbstractionLevel();
        boolean architectureEnabled = intensity >= 3 && (lockedLevel == null || lockedLevel == AbstractionLevel.ARCHITECTURE);
        boolean implementationEnabled = intensity >= 2 || (lockedLevel == AbstractionLevel.IMPLEMENTATION);
        BranchVariant.ReasoningLevel reasoningLevel = intensity == 1 ? BranchVariant.ReasoningLevel.MINIMAL :
                                                      intensity == 4 ? BranchVariant.ReasoningLevel.DEEP : BranchVariant.ReasoningLevel.BALANCED;

        SiblingGenerationManager siblingManager = new SiblingGenerationManager(manager.getSessionContainer(), aiService);
        List<JSONObject> uniqueVariants = siblingManager.generateSiblings(
                goal, activeDimension, branchingLimit, basePrompt, lineageContext, rejectedSiblings,
                context, genome, tree, currentParentId, trajectory != null ? trajectory.getGeneration() : 0,
                reasoningLevel, architectureEnabled, implementationEnabled, expansion, context.getOrchestrator());

        if (uniqueVariants.size() < 2) {
             DarwinSyntheticVariantFactory factory = new DarwinSyntheticVariantFactory();
             if (uniqueVariants.isEmpty()) uniqueVariants.add(factory.synthesizeImplementation(goal.getPrimaryAction(), null));
             if (uniqueVariants.size() < 2) uniqueVariants.add(factory.synthesizeSemanticAlternative(uniqueVariants.get(0), goal.getPrimaryAction(), null));
        }

        Object envObj = context.getOrchestrationState().getMetadata().get("semanticEnvelope");
        SemanticEnvelope envelope = eu.kalafatic.evolution.controller.parsers.JsonUtils.restoreFromMetadata(envObj, SemanticEnvelope.class, "semanticEnvelope", context);

        for (JSONObject variant : uniqueVariants) {
            double distance = semanticDistance(goal, variant, envelope);
            if (distance > 0.60) {
                EvolutionNode node = tree.getNode(variant.optString("id"));
                if (node != null) {
                    node.setStatus("REJECTED_SEMANTIC");
                    node.setRejectionReason("Semantic distance exceeds threshold");
                }
            }
        }

        DarwinFitnessRanker ranker = new DarwinFitnessRanker();
        ranker.rank(uniqueVariants, null, context.getOrchestrationState().getIterationCount(), pressure);

        List<BranchVariant> variants = new ArrayList<>();
        for (JSONObject obj : uniqueVariants) {
            BranchVariant v = mapToBranchVariant(obj, goal.getPrimaryAction(), trajectory, context);
            if (!survivors.isEmpty()) {
                v.setInheritedContext(lineageContext);
                v.setRejectedSiblings(rejectedSiblings);
            }
            variants.add(v);
        }
        return variants;
    }

    private VariantExecutionContext evaluateVariantParallel(BranchVariant variant, TaskPlanner planner, TaskContext context, String baseCommit, EvolutionaryPressureVector pressure, IterationManager manager) {
        File tempDir = null;
        VariantExecutionContext variantExecContext = new VariantExecutionContext(variant.getId());
        boolean isMediated = context.getBehaviorProfile().hasTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY);

        try {
            if (context.getMetadata().containsKey("testMode") || isMediated) {
                tempDir = context.getProjectRoot();
            } else {
                tempDir = java.nio.file.Files.createTempDirectory("evo-variant-" + variant.getId()).toFile();
                manager.getBranchManager().createWorktree(variant.getBranchName(), tempDir.getAbsolutePath());
            }
            TaskContext variantContext = new TaskContext(context.getOrchestrator(), tempDir);
            variantContext.setSessionId(context.getSessionId());
            variantContext.setKernelContext(context.getKernelContext());
            variantContext.getMetadata().put("variantId", variant.getId());
            variantContext.getMetadata().put("variantExecContext", variantExecContext);
            variantContext.setPlatformMode(context.getPlatformMode());
            variantContext.setAutoApprove(true);
            variantContext.setAiService(aiService);

            List<eu.kalafatic.evolution.model.orchestration.Task> tasks = planner.generateTasksFromVariant(variantContext, variant);
            IterationManager variantManager = KernelFactory.create(variantContext, manager.getSessionContainer(), aiService);

            boolean success = true;
            manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.EXECUTING, context);

            if (context.getMetadata().containsKey("testMode") || isMediated) {
                variant.setSuccess(true);
                variant.setScore(0.95);
                manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.VERIFIED, context);
                manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.SCORING, context);
                return variantExecContext;
            }

            for (eu.kalafatic.evolution.model.orchestration.Task task : tasks) {
                boolean taskSuccess = variantManager.executeTasksWithRetries(List.of(task));
                variantExecContext.getTasks().add(task);
                if (!taskSuccess) { success = false; break; }
            }

            if (success) {
                variantManager.getGitManager().commit("Darwin Variant Execution: " + variant.getStrategy(), variantContext);
                manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.VERIFIED, context);
            } else {
                manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.REJECTED, context);
            }
            variant.setSuccess(success);

            EvaluationResult result;
            if (context.getExecutionProfile().shouldPerformRealityCheck()) {
                result = manager.getFitnessEngine().evaluate(tempDir, variantContext, pressure);
            } else {
                result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                result.setSuccess(true);
            }

            variant.setSuccess(result.isSuccess());
            if (result.isSuccess()) {
                manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.SCORING, context);
            }
            return variantExecContext;
        } catch (Exception e) {
            variant.setSuccess(false); variant.setScore(0.0);
            manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.REJECTED, context);
            return variantExecContext;
        } finally {
            if (tempDir != null && !context.getMetadata().containsKey("testMode") && !isMediated) {
                try { manager.getGitManager().removeWorktree(tempDir.getAbsolutePath()); } catch (Exception e) {}
            }
        }
    }

    private double semanticDistance(GoalModel goal, JSONObject variant, SemanticEnvelope envelope) {
        String strategy = variant.optString("strategy", "").toLowerCase();
        String primaryAction = goal.getPrimaryAction().toLowerCase();
        double distance = 0.0;
        if (strategy.contains(primaryAction)) return 0.0;
        return 0.5;
    }

    private BranchVariant mapToBranchVariant(JSONObject obj, String goal, Trajectory trajectory, TaskContext context) {
        BranchVariant v = new BranchVariant();
        v.setId(obj.optString("id", "v-" + System.currentTimeMillis()));
        v.setBranchId(v.getId());
        v.setStrategy(obj.optString("strategy", "unknown"));
        v.setScore(obj.optDouble("score", 0.0));
        v.setBranchName("exp/" + v.getId());
        v.setSurvivalArgument(obj.optString("survival_argument", "none"));
        v.setTradeoffs(obj.optString("tradeoffs", "none"));
        v.setActivationState(BranchVariant.ActivationState.ARCHIVED);
        return v;
    }
}
