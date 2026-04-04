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
        this(iteration, context, new TaskPlanner(), new TaskExecutor(context));
    }

    public IterationManager(Iteration iteration, TaskContext context, TaskPlanner planner, TaskExecutor executor) {
        this.iteration = iteration;
        this.context = context;
        this.gitManager = new GitManager(context.getProjectRoot(), context);
        this.planner = planner;
        this.executor = executor;
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

            // 4. Evaluate
            iteration.setPhase("TEST");
            EvaluationResult result = evaluator.evaluate();

            if (!executionSuccess || !result.isSuccess() || result.getDecision() == SelfDevDecision.ROLLBACK) {
                context.log("[ITERATION] Iteration failed. Rolling back.");
                gitManager.rollback();
                iteration.setStatus(IterationStatus.FAILED);
                if (result.getDecision() == SelfDevDecision.CONTINUE) {
                    result.setDecision(SelfDevDecision.ROLLBACK);
                }
                return result;
            }
            iteration.setEvaluationResult(result);

            iteration.setPhase("EVALUATE");
            if (result.isSuccess() && result.getDecision() == SelfDevDecision.CONTINUE) {
                // Goal: commit -> PR -> feedback -> refine
                iteration.setPhase("COMMIT");
                gitManager.commit("Self-Development Iteration " + iteration.getId() + ": " + iteration.getTasks().get(0).getName());

                iteration.setPhase("PR");
                context.log("[ITERATION] Creating Pull Request (Simulated)");
                // In a real RCP environment, this would call a GitHub/GitLab API or specialized tool.

                iteration.setPhase("FEEDBACK");
                context.log("[ITERATION] Requesting user feedback on PR...");
                try {
                    context.requestApproval("Pull Request for iteration " + iteration.getId() + " created. Please review and provide feedback.").get();
                } catch (Exception e) {
                    context.log("[ITERATION] Feedback step skipped: " + e.getMessage());
                }

                iteration.setPhase("REFINE");
                context.log("[ITERATION] Refining based on feedback (if any)...");
            }

            // 5. Decision
            iteration.setPhase("LEARN");

            if (result.getDecision() == SelfDevDecision.CONTINUE) {
                context.log("[ITERATION] Evaluation successful.");
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
