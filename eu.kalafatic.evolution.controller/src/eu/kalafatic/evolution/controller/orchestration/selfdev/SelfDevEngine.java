//package eu.kalafatic.evolution.controller.orchestration.selfdev;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import eu.kalafatic.evolution.controller.orchestration.IterationManager;
//import eu.kalafatic.evolution.controller.orchestration.TaskContext;
//import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;
//import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
//import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
//import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
//
///**
// * SELF-DEV Darwin Engine - Self-development with external supervisor.
// * 
// * Characteristics:
// * - External supervisor for build/test
// * - Branch creation and management
// * - Auto-merge on success
// * - Rollback on failure
// * - Fitness-based decision making
// */
//public class SelfDevEngine extends AbstractDarwinEngine {
//
//    private final ISelfDevSupervisor supervisor;
//    private final SiblingGenerationManager siblingManager;
//    private String currentBranch;
//
//    public SelfDevEngine(TaskContext context, IterationMemoryService memoryService,
//                         ISelfDevSupervisor supervisor) {
//        super(context, memoryService);
//        this.supervisor = supervisor;
//        this.siblingManager = new SiblingGenerationManager(sessionContainer, aiService);
//        this.currentBranch = supervisor.getMainBranch();
//
//        setExecutionProfile(EvolutionProfile.create(CapabilityType.SELF_DEV, 3));
//    }
//
//    @Override
//    public EvaluationResult runIteration(GoalModel goal, IterationManager manager) throws Exception {
//        context.log("[SELF_DEV_ENGINE] Running self-dev iteration for: " + goal.getPrimaryAction());
//
//        // 1. Create branch
//        String branchName = createBranch();
//        if (branchName == null) {
//            return failedResult("Branch creation failed");
//        }
//
//        // 2. Generate code variants
//        List<BranchVariant> variants = generateVariants(goal, manager);
//
//        if (variants.isEmpty()) {
//            supervisor.rollback(branchName, "No variants generated");
//            return failedResult("No variants generated");
//        }
//
//        // 3. Validate variants
//        variants = validateVariants(variants, manager);
//
//        if (variants.isEmpty()) {
//            supervisor.rollback(branchName, "No valid variants");
//            return failedResult("No valid variants");
//        }
//
//        // 4. Select and execute winner
//        BranchVariant winner = selectBestVariant(variants);
//        context.log("[SELF_DEV_ENGINE] Selected winner: " + winner.getId());
//
//        EvaluationResult result = executeWinner(winner, manager);
//
//        if (!result.isSuccess()) {
//            supervisor.rollback(branchName, "Execution failed");
//            return failedResult("Execution failed");
//        }
//
//        // 5. Build and test via supervisor
//        context.log("[SELF_DEV_ENGINE] Running build and tests...");
//        BuildResult buildResult = supervisor.buildAndTest(branchName, context);
//
//        if (!buildResult.success) {
//            supervisor.rollback(branchName, "Build failed");
//            return failedResult("Build failed: " + String.join("; ", buildResult.errors));
//        }
//
//        // 6. Run tests
//        TestResult testResult = supervisor.runTests(branchName, context);
//        context.log("[SELF_DEV_ENGINE] Tests: " + testResult.passed + "/" + testResult.total +
//                " passed (" + String.format("%.2f%%", testResult.passRate * 100) + ")");
//
//        // 7. Evaluate fitness
//        FitnessResult fitness = supervisor.evaluateFitness(buildResult, testResult);
//        context.log("[SELF_DEV_ENGINE] Fitness: " + String.format("%.3f", fitness.score) +
//                " (threshold: " + fitness.thresholdMet + ")");
//
//        // 8. Decide
//        if (fitness.thresholdMet) {
//            MergeResult mergeResult = supervisor.mergeIfApproved(branchName, fitness);
//            if (mergeResult.merged) {
//                context.log("[SELF_DEV_ENGINE] Merged successfully: " + mergeResult.commitId);
//                result.setSuccess(true);
//                result.setDecision(SelfDevDecision.CONTINUE);
//            } else {
//                supervisor.rollback(branchName, "Merge failed: " + mergeResult.message);
//                result.setSuccess(false);
//                result.setDecision(SelfDevDecision.ROLLBACK);
//            }
//        } else {
//            supervisor.rollback(branchName, "Fitness threshold not met: " + fitness.score);
//            result.setSuccess(false);
//            result.setDecision(SelfDevDecision.ROLLBACK);
//            result.getErrors().add("Fitness threshold not met: " + fitness.score);
//        }
//
//        return result;
//    }
//
//    @Override
//    public List<BranchVariant> generateVariants(GoalModel goal, IterationManager manager) throws Exception {
//        // Use the existing code generation logic
//        return generateProposals(goal, manager);
//    }
//
//    @Override
//    public List<BranchVariant> validateVariants(List<BranchVariant> variants, IterationManager manager) {
//        List<BranchVariant> valid = new ArrayList<>();
//        for (BranchVariant v : variants) {
//            if (v.getActions() != null && !v.getActions().isEmpty()) {
//                boolean hasWriteAction = v.getActions().stream()
//                        .anyMatch(a -> "WRITE".equals(a.getOperation()) || "CREATE".equals(a.getOperation()));
//                if (hasWriteAction) {
//                    valid.add(v);
//                }
//            }
//        }
//        return valid;
//    }
//
//    @Override
//    public EvaluationResult executeWinner(BranchVariant winner, IterationManager manager) throws Exception {
//        context.log("[SELF_DEV_ENGINE] Executing winner variant on branch: " + currentBranch);
//
//        // Apply the code changes
//        List<eu.kalafatic.evolution.model.orchestration.Task> tasks = convertActionsToTasks(winner.getActions());
//        boolean success = manager.executeTasksWithRetries(tasks);
//
//        if (success) {
//            manager.getGitManager().commit("Self-Dev: " + winner.getStrategy(), context);
//        }
//
//        EvaluationResult result = new EvaluationResult();
//        result.setSuccess(success);
//        result.setDecision(success ? SelfDevDecision.CONTINUE : SelfDevDecision.ROLLBACK);
//
//        return result;
//    }
//
//    @Override
//    public CapabilityType getCapabilityType() {
//        return CapabilityType.SELF_DEV;
//    }
//
//    @Override
//    public String getMode() {
//        return "SELF_DEV";
//    }
//
//    // ============================================================
//    // PRIVATE HELPERS
//    // ============================================================
//
//    private String createBranch() {
//        String branchName = "exp/selfdev/" + context.getSessionId().substring(0, 8) +
//                "-" + System.currentTimeMillis();
//
//        BranchResult result = supervisor.createBranch(branchName, supervisor.getMainBranch());
//        if (result.success) {
//            this.currentBranch = branchName;
//            context.getOrchestrationState().getMetadata().put("selfDevBranch", branchName);
//            return branchName;
//        }
//
//        context.log("[SELF_DEV_ENGINE] Failed to create branch: " + result.message);
//        return null;
//    }
//
//    private List<BranchVariant> generateProposals(GoalModel goal, IterationManager manager) throws Exception {
//        // Delegate to the original generation logic
//        return new ArrayList<>(); // Placeholder - actual implementation here
//    }
//
//    private List<eu.kalafatic.evolution.model.orchestration.Task> convertActionsToTasks(
//            List<BranchVariant.Action> actions) {
//        List<eu.kalafatic.evolution.model.orchestration.Task> tasks = new ArrayList<>();
//
//        if (actions == null || actions.isEmpty()) {
//            return tasks;
//        }
//
//        for (BranchVariant.Action action : actions) {
//            eu.kalafatic.evolution.model.orchestration.Task task =
//                    eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createTask();
//            task.setId("task-" + System.currentTimeMillis() + "-" + tasks.size());
//            task.setName(action.getDescription() != null ? action.getDescription() : action.getOperation());
//            task.setType(action.getOperation());
//            task.setResponse(action.getImplementation());
//            task.setDescription(action.getDescription());
//            task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.READY);
//            task.setPriority(1);
//            task.setApprovalRequired(false);
//
//            if (action.getTarget() != null && !action.getTarget().isEmpty()) {
//                task.getAttachments().add(action.getTarget());
//            }
//
//            tasks.add(task);
//        }
//
//        return tasks;
//    }
//}