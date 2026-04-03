package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.IterationStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
import eu.kalafatic.evolution.model.orchestration.Task;

public class IterationManager {
    private final Iteration iteration;
    private final TaskContext context;
    private final GitManager gitManager;
    private final TaskPlanner planner;
    private final TaskExecutor executor;
    private final Evaluator evaluator;

    public IterationManager(Iteration iteration, TaskContext context) {
        this.iteration = iteration;
        this.context = context;
        this.gitManager = new GitManager(context.getProjectRoot(), context);
        this.planner = new TaskPlanner();
        this.executor = new TaskExecutor(context);
        this.evaluator = new Evaluator(context.getProjectRoot(), context);
    }

    public EvaluationResult run() throws Exception {
        context.log("[ITERATION] Starting iteration: " + iteration.getId());
        iteration.setStatus(IterationStatus.RUNNING);
        iteration.setPhase("OBSERVE");

        try {
            // 1. Create Git Branch
            iteration.setPhase("ANALYZE");
            gitManager.createBranch(iteration.getBranchName());

            // 2. Plan Tasks
            iteration.setPhase("PLAN");
            List<Task> tasks = planner.generateTasks(context);
            if (tasks.isEmpty()) {
                context.log("[ITERATION] No tasks generated. Skipping.");
                iteration.setStatus(IterationStatus.DONE);
                EvaluationResult skipResult = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                skipResult.setSuccess(true);
                skipResult.setDecision(SelfDevDecision.CONTINUE);
                return skipResult;
            }
            iteration.getTasks().addAll(tasks);

            // 3. Execute Tasks
            iteration.setPhase("VALIDATE");
            // In a real scenario, VALIDATE might involve checking the plan.
            // For now, we move directly to EXECUTE.

            iteration.setPhase("EXECUTE");
            boolean executionSuccess = executor.executeTasks(tasks);
            if (!executionSuccess) {
                context.log("[ITERATION] Execution failed. Rolling back.");
                gitManager.rollback();
                iteration.setStatus(IterationStatus.FAILED);
                EvaluationResult failResult = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                failResult.setSuccess(false);
                failResult.setDecision(SelfDevDecision.ROLLBACK);
                return failResult;
            }

            // 4. Evaluate
            iteration.setPhase("TEST");
            // TEST and EVALUATE are handled by the evaluator

            iteration.setPhase("EVALUATE");
            EvaluationResult result = evaluator.evaluate();
            iteration.setEvaluationResult(result);

            // 5. Decision
            iteration.setPhase("LEARN");

            // Pause for user feedback if interactive mode is supported via TaskContext
            try {
                context.log("[ITERATION] Iteration " + iteration.getId() + " completed. Requesting user feedback/rating.");
                context.requestApproval("Iteration " + iteration.getId() + " completed. Please rate and provide feedback in the 'Approval' or 'Ai Chat' page.").get();
            } catch (Exception e) {
                context.log("[ITERATION] User feedback skipped or timed out: " + e.getMessage());
            }

            if (result.getDecision() == SelfDevDecision.CONTINUE) {
                context.log("[ITERATION] Evaluation successful. Committing.");
                gitManager.commit("Self-Development Iteration " + iteration.getId() + " success.");
                iteration.setStatus(IterationStatus.DONE);
            } else {
                context.log("[ITERATION] Evaluation failed or rollback required. Decision: " + result.getDecision());
                gitManager.rollback();
                iteration.setStatus(IterationStatus.FAILED);
            }

            return result;

        } catch (Exception e) {
            context.log("[ITERATION] Error in iteration: " + e.getMessage());
            gitManager.rollback();
            iteration.setStatus(IterationStatus.FAILED);
            throw e;
        }
    }
}
