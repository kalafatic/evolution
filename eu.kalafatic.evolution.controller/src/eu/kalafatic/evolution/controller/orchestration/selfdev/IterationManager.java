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

    public IterationManager(Iteration iteration, TaskContext context, TaskPlanner planner, TaskExecutor executor) {
        this(iteration, context, planner, executor, new IterationMemoryService(context.getProjectRoot()));
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
        if (context.getOrchestrator().isDarwinMode()) {
            return runDarwin();
        } else {
            return runIterative();
        }
    }

    private EvaluationResult runIterative() throws Exception {
        context.log("[ITERATION] Starting iterative iteration: " + iteration.getId());
        iteration.setStatus(IterationStatus.RUNNING);
        iteration.setPhase("PLAN");

        String goal = context.getOrchestrator().getSelfDevSession() != null ?
                     context.getOrchestrator().getSelfDevSession().getInitialRequest() : "Autonomous Improvement";

        try {
            List<Task> tasks = planner.generateTasks(context, goal);
            iteration.setPhase("EXECUTE");
            boolean success = executor.executeTasks(tasks);

            iteration.setPhase("TEST");
            EvaluationResult result = evaluator.evaluate();
            iteration.setEvaluationResult(result);

            iteration.setPhase("EVALUATE");
            if (result.isSuccess() && result.getDecision() == SelfDevDecision.CONTINUE) {
                iteration.setPhase("COMMIT");
                gitManager.commit("Self-Development Iteration " + iteration.getId());
                iteration.setStatus(IterationStatus.DONE);
            } else {
                gitManager.rollback();
                iteration.setStatus(IterationStatus.FAILED);
            }
            return result;
        } catch (Exception e) {
            context.log("[ITERATION] Error in iterative iteration: " + e.getMessage());
            gitManager.rollback();
            iteration.setStatus(IterationStatus.FAILED);
            throw e;
        }
    }

    private EvaluationResult runDarwin() throws Exception {
        context.log("[ITERATION] Starting Darwin iteration: " + iteration.getId());
        iteration.setStatus(IterationStatus.RUNNING);
        iteration.setPhase("OBSERVE");

        String goal = context.getOrchestrator().getSelfDevSession() != null ?
                     context.getOrchestrator().getSelfDevSession().getInitialRequest() : "Autonomous Improvement";

        String originalBranch = gitManager.getCurrentBranch();
        String snapshotBranch = "snapshot/" + iteration.getId() + "-" + System.currentTimeMillis();

        try {
            // Before iteration: create a base snapshot branch
            gitManager.createBranch(snapshotBranch);

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
            // Ensure we start variants from the snapshot
            gitManager.forceCheckout(snapshotBranch);
            BranchVariant bestVariant = darwinEngine.evaluateVariants(variants, planner, iteration);

            // Save records for ALL variants to memory
            for (BranchVariant v : variants) {
                IterationRecord rec = new IterationRecord();
                int iterCount = 0;
                if (context.getOrchestrator().getSelfDevSession() != null) {
                    iterCount = context.getOrchestrator().getSelfDevSession().getIterations().size();
                }
                rec.setIteration(iterCount);
                rec.setGoal(goal);
                rec.setStrategy(v.getStrategy());
                rec.setBranch(v.getBranchName());
                rec.setResult(v.isSuccess() ? "SUCCESS" : "FAIL");
                rec.setScore(v.getScore());
                rec.setErrorMessage(v.getErrorMessage());
                rec.setChangedFiles(v.getChangedFiles());
                rec.setTimestamp(System.currentTimeMillis());
                memoryService.saveRecord(rec);
            }

            if (bestVariant == null || bestVariant.getScore() <= 0) {
                context.log("[ITERATION] No successful variant found. Skipping.");
                iteration.setStatus(IterationStatus.FAILED);
                // Cleanup
                gitManager.forceCheckout(originalBranch);
                gitManager.deleteBranch(snapshotBranch);
                for (BranchVariant v : variants) {
                    try { gitManager.deleteBranch(v.getBranchName()); } catch (Exception e) {}
                }
                EvaluationResult failResult = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                failResult.setSuccess(false);
                failResult.setDecision(SelfDevDecision.ROLLBACK);
                return failResult;
            }

            context.log("[ITERATION] Best variant selected: " + bestVariant.getBranchName() + " with score " + bestVariant.getScore());

            // Merge best variant into original branch
            iteration.setPhase("EXECUTE");
            gitManager.forceCheckout(originalBranch);
            gitManager.merge(bestVariant.getBranchName());

            // Final evaluation on main branch
            iteration.setPhase("TEST");
            EvaluationResult result = evaluator.evaluate();

            iteration.setEvaluationResult(result);

            iteration.setPhase("EVALUATE");
            if (result.isSuccess() && result.getDecision() == SelfDevDecision.CONTINUE) {
                iteration.setPhase("COMMIT");
                gitManager.commit("Self-Development Iteration " + iteration.getId() + " (Darwin best variant: " + bestVariant.getStrategy() + ")");

                iteration.setPhase("PR");
                context.log("[ITERATION] Creating Pull Request (Simulated)");

                iteration.setPhase("FEEDBACK");
                try {
                    if (!context.isAutoApprove()) {
                        context.requestApproval("Darwin evolved branch " + bestVariant.getBranchName() + " merged. Please review.").get();
                    }
                } catch (Exception e) {}

                iteration.setPhase("REFINE");
            }

            // Cleanup experiment branches and snapshot
            gitManager.forceCheckout(originalBranch);
            gitManager.deleteBranch(snapshotBranch);
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
            context.log("[ITERATION] Error in iteration: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            context.log(sw.toString());
            gitManager.forceCheckout(originalBranch);
            try { gitManager.deleteBranch(snapshotBranch); } catch (Exception ex) {}
            gitManager.rollback();
            iteration.setStatus(IterationStatus.FAILED);
            throw e;
        }
    }
}
