package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionNode;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionTree;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.controller.supervision.ActivationResolver;
import eu.kalafatic.evolution.controller.supervision.AuthorityController;
import eu.kalafatic.evolution.controller.supervision.DecisionSnapshot;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;

/**
 * Evolutionary Darwin loop orchestration flow.
 */
@EvolutionComponent(
    domain = "orchestration",
    role = "exploration-orchestrator",
    purpose = "Coordinates multi-branch evolution proposals and evaluation",
    stability = Stability.STABLE,
    evolutionaryImpact = EvolutionaryImpact.HIGH
)
public class DarwinFlow implements IOrchestrationFlow {
    private static final ExecutorService variantExecutor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
    private final AiService aiService;
    private final IterationManager manager;
    private final SessionContainer sessionContainer;

    public DarwinFlow(AiService aiService, IterationManager manager) {
        this.aiService = aiService;
        this.manager = manager;
        this.sessionContainer = SessionManager.getInstance().getOrCreateSession(manager.getContext().getSessionId());
        if (this.sessionContainer == null) {
            throw new IllegalStateException("DarwinFlow: sessionContainer is null for sessionId: " + manager.getContext().getSessionId());
        }
    }

    @Override
    public OrchestratorResponse execute(String request, TaskContext context) throws Exception {
        return manager.getDarwinEngine().orchestrateEvolution(new TaskRequest(request, context.getProjectRoot()), manager);
    }

//    @Deprecated
//    public List<BranchVariant> generateProposals(TaskContext context, GoalModel goal) throws Exception {
//	return manager.getDarwinEngine().generateProposals(context, goal, manager);
//    }
//
//    @Deprecated
//    public EvaluationResult executeWinner(TaskContext context, eu.kalafatic.evolution.controller.supervision.EvolutionDecision decision, List<BranchVariant> variants, GoalModel goal) throws Exception {
//	return manager.getDarwinEngine().executeWinner(context, decision, variants, goal, manager);
//    }

    public VariantExecutionContext evaluateVariantParallel(BranchVariant variant, eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner planner, TaskContext context, String baseCommit, eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector pressure) {
        // Delegate to authoritative engine implementation in ADarwinEngine
        eu.kalafatic.evolution.controller.orchestration.selfdev.IDarwinEngine engine = manager.getDarwinEngine();
        if (engine instanceof eu.kalafatic.evolution.controller.orchestration.selfdev.ADarwinEngine) {
            return ((eu.kalafatic.evolution.controller.orchestration.selfdev.ADarwinEngine) engine)
                    .evaluateVariantParallel(variant, planner, context, baseCommit, pressure, manager);
        }
        VariantExecutionContext fallbackContext = new VariantExecutionContext(variant.getId());
        variant.setSuccess(false);
        variant.setScore(0.0);
        variant.setErrorMessage("Authoritative ADarwinEngine unavailable for variant evaluation.");
        manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.REJECTED, context);
        return fallbackContext;
    }

    private BranchVariant createUserVariant(String input, GoalModel goal, TaskContext context) {
        BranchVariant v = new BranchVariant();
        v.setId("v-user-" + System.currentTimeMillis());
        v.setBranchId(v.getId());
        v.setLineageId(context.getSessionId());
        v.setActivationState(BranchVariant.ActivationState.ARCHIVED);
        v.setStrategyType("USER_TRAJECTORY");

        String strategyText = input.startsWith("Propose:") ? input.substring(8).trim() : input;

        if (strategyText.trim().startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(strategyText);
                v.setStrategy(obj.optString("strategy", "User-defined strategy"));
                v.setSurvivalArgument(obj.optString("survival_argument", "User injection"));
                v.setTradeoffs(obj.optString("tradeoffs", "Explicit user directive"));
            } catch (Exception e) {
                v.setStrategy(strategyText);
            }
        } else {
            v.setStrategy(strategyText);
            v.setSurvivalArgument("Direct user proposal");
            v.setTradeoffs("User-defined trajectory");
        }

        v.setScore(0.95);
        v.setBranchName("exp/user/" + sanitize(v.getStrategy()));

        Trajectory t = new Trajectory(v.getId(), v.getStrategy());
        t.setFitnessScore(v.getScore());
        v.setTrajectoryId(t.getTrajectoryId());

        if (context.getKernelContext().getMemoryService().getTrajectoryMemory() != null) {
            context.getKernelContext().getMemoryService().getTrajectoryMemory().recordTrajectory(t);
        }

        return v;
    }

    private void mergeHybridInsights(List<BranchVariant> variants, BranchVariant winner, TaskContext context) {
        JSONArray analyticalInsights = new JSONArray();
        JSONArray stabilizationInsights = new JSONArray();

        for (BranchVariant v : variants) {
            if (v.getId().equals(winner.getId())) continue;

            JSONObject insight = new JSONObject();
            insight.put("strategy", v.getStrategy());
            insight.put("risks", v.getFailureRisks());
            insight.put("tradeoffs", v.getTradeoffs());

            if ("ANALYTICAL".equals(v.getStrategyType())) {
                analyticalInsights.put(insight);
            } else if ("STABILIZATION".equals(v.getStrategyType())) {
                stabilizationInsights.put(insight);
            }
        }

        if (analyticalInsights.length() > 0) {
            context.getOrchestrationState().getMetadata().put("hybrid_analytical_insights", analyticalInsights);
            context.log("[DARWIN] Merged " + analyticalInsights.length() + " analytical insights into context.");
        }
        if (stabilizationInsights.length() > 0) {
            context.getOrchestrationState().getMetadata().put("hybrid_stabilization_insights", stabilizationInsights);
            context.log("[DARWIN] Merged " + stabilizationInsights.length() + " stabilization insights into context.");
        }
    }

    public boolean checkConvergence(List<BranchVariant> variants, TaskContext context) {
        if (variants == null || variants.isEmpty()) return false;

        eu.kalafatic.evolution.controller.trajectory.StabilityAnalyzer analyzer = new eu.kalafatic.evolution.controller.trajectory.StabilityAnalyzer();

        IterationRecord lastWinner = context.getKernelContext().getMemoryService().getRecords().stream()
                .filter(r -> "ACTIVE".equals(r.getActivationState()))
                .reduce((first, second) -> second)
                .orElse(null);

        if (lastWinner != null && lastWinner.getBranchId() != null) {
             Trajectory trajectory = context.getSemanticWorkspace().getTrajectoryMemory().getTrajectory(lastWinner.getBranchId());
             if (trajectory != null) {
                 return analyzer.isConverged(trajectory, context);
             }
        }

        return false;
    }

    private String sanitize(String s) {
        if (s == null || s.isEmpty()) return "unnamed";
        String sanitized = s.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-");
        if (sanitized.startsWith("-")) sanitized = sanitized.substring(1);
        if (sanitized.endsWith("-")) sanitized = sanitized.substring(0, sanitized.length() - 1);
        if (sanitized.isEmpty()) return "unnamed";
        return sanitized.substring(0, Math.min(sanitized.length(), 30));
    }

    private void deleteDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) deleteDirectory(file);
        }
        directory.delete();
    }
}
