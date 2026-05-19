package eu.kalafatic.evolution.controller.orchestration;

import java.util.Collections;
import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;

/**
 * Lightweight Single-Variant Darwin evolution flow.
 * Unified execution model for simple tasks that benefit from evolutionary semantics.
 */
@EvolutionComponent(
    domain = "orchestration",
    role = "lightweight-orchestrator",
    purpose = "Executes single-variant evolution for simple tasks",
    stability = Stability.STABLE,
    evolutionaryImpact = EvolutionaryImpact.MEDIUM
)
public class SingleVariantDarwinFlow implements IOrchestrationFlow {
    private final AiService aiService;
    private final IterationManager manager;

    public SingleVariantDarwinFlow(AiService aiService, IterationManager manager) {
        this.aiService = aiService;
        this.manager = manager;
    }

    @Override
    public OrchestratorResponse execute(String request, TaskContext context) throws Exception {
        context.log("[KERNEL] Executing Single-Variant Darwin Flow.");
        OrchestrationState state = context.getOrchestrationState();

        manager.transition(SystemState.INIT, context);
        manager.transition(SystemState.ANALYZING, context);

        if (manager.getGitAdapter().isGitRepository()) {
            manager.getGitAdapter().ensureInitialCommit();
        }

        String originalBranch = manager.getGitAdapter().getCurrentBranch();
        String baseCommit = manager.getGitAdapter().getHeadCommit();

        Evaluator.Evaluation initialEval = manager.getFitnessEngine().evaluateWithSnapshot();
        StateSnapshot snapshot = initialEval.snapshot;

        manager.transition(SystemState.MUTATING, context);
        // Simplified mutation: generate 1 variant
        List<BranchVariant> variants;
        AtomicIntentAnalysis atomicAnalysis = (AtomicIntentAnalysis) state.getMetadata().get("atomicAnalysis");
        if (atomicAnalysis != null && atomicAnalysis.isAtomic()) {
            context.log("[KERNEL] Simple atomic task detected. Using synthetic variant.");
            BranchVariant v = new BranchVariant();
            v.setId("atomic-v1");
            v.setStrategy("Atomic Execution: " + request);
            v.setActivationState(BranchVariant.ActivationState.ACTIVE);

            BranchVariant.Action action = new BranchVariant.Action();
            action.setDomain(atomicAnalysis.getArtifactType() != null ? atomicAnalysis.getArtifactType() : "file");
            action.setOperation("WRITE");
            action.setTarget(atomicAnalysis.getTargetArtifact());
            action.setDescription(request);
            v.getActions().add(action);

            variants = Collections.singletonList(v);
        } else {
            variants = manager.getMutationEngine().generateVariants(request, snapshot, context.getKernelContext().getMemoryService().getFailureMemory(), null);
        }

        if (variants.isEmpty()) {
            throw new Exception("Mutation engine failed to generate variants.");
        }
        BranchVariant variant = variants.get(0);
        variant.setActivationState(BranchVariant.ActivationState.ACTIVE);

        manager.transition(SystemState.PLAN_LOCKED, context);
        manager.getGitAdapter().forceCheckout(originalBranch);

        manager.transition(SystemState.EXECUTING, context);

        List<Task> tasks = manager.getTaskPlanner().generateTasksFromVariant(context, variant);
        state.getExecutionPlan().addAll(tasks);
        context.getOrchestrator().getTasks().addAll(tasks);

        boolean success = manager.executeTasksWithRetries(tasks);

        if (success) {
            manager.getGitAdapter().commit("Single-Variant Execution: " + variant.getStrategy(), context);
            manager.transition(SystemState.VERIFYING, context);
            EvaluationResult result = manager.getFitnessEngine().evaluate(context.getProjectRoot(), context);

            if (result.isSuccess()) {
                manager.transition(SystemState.DONE, context);
                String summary = manager.getFinalResponseAgent().generateFinalResponse(request, tasks, context);
                OrchestratorResponse response = new OrchestratorResponse();
                response.setResultType(ResultType.CHAT);
                response.setSummary(summary);
                return response;
            } else {
                manager.getGitAdapter().rollback();
                manager.transition(SystemState.FAILED, context);
                throw new Exception("Verification failed after successful execution.");
            }
        } else {
            manager.getGitAdapter().rollback();
            manager.transition(SystemState.FAILED, context);
            throw new Exception("Task execution failed.");
        }
    }
}
