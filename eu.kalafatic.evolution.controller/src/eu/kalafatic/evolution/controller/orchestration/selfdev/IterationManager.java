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

        try {
            // 1. Create Git Branch
            gitManager.createBranch(iteration.getBranchName());

            // 2. Plan Tasks
            TaskPlanner.PlanningResult planningResult = planner.generateTasks(context);
            List<Task> tasks = planningResult.tasks;
            iteration.setRationale(planningResult.rationale);
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
            EvaluationResult result = evaluator.evaluate();
            iteration.setEvaluationResult(result);

            // 5. Decision
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
