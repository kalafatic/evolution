package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.IterationStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
import eu.kalafatic.evolution.model.orchestration.Task;

import java.util.stream.Collectors;

public class IterationManager {
    private final Iteration iteration;
    private final TaskContext context;
    private final GitManager gitManager;
    private final TaskPlanner planner;
    private final TaskExecutor executor;
    private final Evaluator evaluator;
    private final DarwinEngine darwinEngine;
    private final IterationMemoryService memoryService;

    public IterationManager(Iteration iteration, TaskContext context) {
        this(iteration, context, new TaskPlanner(), new TaskExecutor(context), new IterationMemoryService(context.getProjectRoot()));
    }

    public IterationManager(Iteration iteration, TaskContext context, TaskPlanner planner, TaskExecutor executor, IterationMemoryService memoryService) {
        this.iteration = iteration;
        this.context = context;
        this.gitManager = new GitManager(context.getProjectRoot(), context);
        this.planner = planner;
        this.executor = executor;
        this.evaluator = new Evaluator(context.getProjectRoot(), context);
        this.memoryService = memoryService;
        this.darwinEngine = new DarwinEngine(context, memoryService);
    }

    public EvaluationResult run() throws Exception {
        context.log("[ITERATION] Starting iteration: " + iteration.getId());
        iteration.setStatus(IterationStatus.RUNNING);
        iteration.setPhase("OBSERVE");

        String goal = context.getOrchestrator().getSelfDevSession() != null ?
                     context.getOrchestrator().getSelfDevSession().getInitialRequest() : "Autonomous Improvement";

        try {
            // Darwinian Branch Strategy
            iteration.setPhase("ANALYZE");

            // Use last error from memory if available
            String lastError = null;
            List<IterationRecord> pastRecords = memoryService.getRecords();
            if (!pastRecords.isEmpty()) {
                IterationRecord last = pastRecords.get(pastRecords.size() - 1);
                if ("FAIL".equals(last.getResult())) {
                    lastError = last.getErrorMessage();
                }
            }

            List<BranchVariant> variants = darwinEngine.generateVariants(goal, lastError);

            iteration.setPhase("PLAN");
            BranchVariant bestVariant = darwinEngine.evaluateVariants(variants, planner, iteration);

            if (bestVariant == null || bestVariant.getScore() <= 0) {
                context.log("[ITERATION] No successful variant found. Skipping.");
                iteration.setStatus(IterationStatus.FAILED);
                EvaluationResult failResult = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                failResult.setSuccess(false);
                failResult.setDecision(SelfDevDecision.ROLLBACK);
                return failResult;
            }

            context.log("[ITERATION] Best variant selected: " + bestVariant.getBranchName() + " with score " + bestVariant.getScore());

            // Merge best variant
            iteration.setPhase("EXECUTE");
            String baseBranch = gitManager.getCurrentBranch();
            gitManager.merge(bestVariant.getBranchName());

            // Final evaluation on main branch
            iteration.setPhase("TEST");
            EvaluationResult result = evaluator.evaluate();

            iteration.setEvaluationResult(result);

            IterationRecord record = new IterationRecord();
            record.setIteration(context.getOrchestrator().getSelfDevSession().getIterations().size());
            record.setGoal(goal);
            record.setBranch(bestVariant.getBranchName());
            record.setResult(result.isSuccess() ? "SUCCESS" : "FAIL");
            record.setScore(bestVariant.getScore());
            record.setTimestamp(System.currentTimeMillis());
            record.setAttempt(1); // Default to 1 for now

            // Populate changed files from tasks
            List<String> changedFiles = iteration.getTasks().stream()
                .filter(t -> "file".equalsIgnoreCase(t.getType()))
                .map(Task::getResultSummary)
                .filter(path -> path != null && !path.isEmpty())
                .distinct()
                .collect(Collectors.toList());
            record.setChangedFiles(changedFiles);

            if (!result.isSuccess()) {
                record.setErrorMessage(result.getErrors().toString());
            }
            memoryService.saveRecord(record);

            iteration.setPhase("EVALUATE");
            if (result.isSuccess() && result.getDecision() == SelfDevDecision.CONTINUE) {
                iteration.setPhase("COMMIT");
                gitManager.commit("Self-Development Iteration " + iteration.getId() + " (Darwin best variant: " + bestVariant.getStrategy() + ")");

                iteration.setPhase("PR");
                context.log("[ITERATION] Creating Pull Request (Simulated)");

                iteration.setPhase("FEEDBACK");
                try {
                    context.requestApproval("Darwin evolved branch " + bestVariant.getBranchName() + " merged. Please review.").get();
                } catch (Exception e) {}

                iteration.setPhase("REFINE");
            }

            // Cleanup experiment branches
            for (BranchVariant v : variants) {
                try {
                    gitManager.deleteBranch(v.getBranchName());
                } catch (Exception e) {}
            }

            iteration.setPhase("LEARN");
            if (result.getDecision() == SelfDevDecision.CONTINUE) {
                iteration.setStatus(IterationStatus.DONE);
            } else {
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
