package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;

/**
 * SELF-DEV Darwin Engine - Handles self-development with external supervisor.
 * Controls builds, tests, git operations, and auto-merge.
 */
public class SelfDevDarwinEngine extends BaseDarwinEngine {
    
    private final SelfDevSupervisor supervisor;
    private String currentBranch;
    
    public SelfDevDarwinEngine(TaskContext context, IterationMemoryService memoryService, 
                               SelfDevSupervisor supervisor, AiService aiService) {
        super(context, memoryService);
        this.supervisor = supervisor;
        this.currentBranch = supervisor.getMainBranch();
    }
    
    @Override
    public EvaluationResult runIteration(GoalModel goal, IterationManager manager) throws Exception {
        context.log("[SELF_DEV_DARWIN] Running self-dev iteration for: " + goal.getPrimaryAction());
        
        // 1. Create branch
        String branchName = "exp/selfdev/" + context.getSessionId().substring(0, 8) + 
                           "-" + System.currentTimeMillis();
        BranchResult branchResult = supervisor.createBranch(branchName, supervisor.getMainBranch());
        
        if (!branchResult.success) {
            context.log("[SELF_DEV_DARWIN] Failed to create branch: " + branchResult.message);
            return failedResult("Branch creation failed: " + branchResult.message);
        }
        this.currentBranch = branchName;
        context.log("[SELF_DEV_DARWIN] Branch created: " + branchName);
        context.getOrchestrationState().getMetadata().put("selfDevBranch", branchName);
        
        // 2. Generate code variants using the original DarwinEngine
        List<BranchVariant> variants = generateVariants(goal, manager);
        
        if (variants.isEmpty()) {
            context.log("[SELF_DEV_DARWIN] No variants generated.");
            supervisor.rollback(branchName, "No variants generated");
            return failedResult("No variants generated");
        }
        
        // 3. Validate variants
        variants = validateVariants(variants, manager);
        
        if (variants.isEmpty()) {
            context.log("[SELF_DEV_DARWIN] No variants passed validation.");
            supervisor.rollback(branchName, "No valid variants");
            return failedResult("No valid variants");
        }
        
        // 4. Select winner
        BranchVariant winner = selectBestVariant(variants);
        context.log("[SELF_DEV_DARWIN] Selected winner: " + winner.getId());
        
        // 5. Execute winner
        EvaluationResult result = executeWinner(winner, manager);
        
        // 6. Build and test (Supervisor)
        context.log("[SELF_DEV_DARWIN] Running build and tests...");
        BuildResult buildResult = supervisor.buildAndTest(branchName, context);
        
        if (!buildResult.success) {
            context.log("[SELF_DEV_DARWIN] Build failed: " + String.join("; ", buildResult.errors));
            supervisor.rollback(branchName, "Build failed");
            return failedResult("Build failed: " + String.join("; ", buildResult.errors));
        }
        
        // 7. Run tests
        TestResult testResult = supervisor.runTests(branchName, context);
        context.log("[SELF_DEV_DARWIN] Tests: " + testResult.passed + "/" + testResult.total + 
                   " passed (" + String.format("%.2f%%", testResult.passRate * 100) + ")");
        
        // 8. Evaluate fitness
        FitnessResult fitness = supervisor.evaluateFitness(buildResult, testResult);
        context.log("[SELF_DEV_DARWIN] Fitness: " + String.format("%.3f", fitness.score) + 
                   " (threshold: " + fitness.thresholdMet + ")");
        
        // 9. Decide
        if (fitness.thresholdMet) {
            MergeResult mergeResult = supervisor.mergeIfApproved(branchName, fitness);
            if (mergeResult.merged) {
                context.log("[SELF_DEV_DARWIN] Merged successfully: " + mergeResult.commitId);
                result.setSuccess(true);
                result.setDecision(SelfDevDecision.CONTINUE);
            } else {
                context.log("[SELF_DEV_DARWIN] Merge failed: " + mergeResult.message);
                supervisor.rollback(branchName, "Merge failed: " + mergeResult.message);
                result.setSuccess(false);
                result.setDecision(SelfDevDecision.ROLLBACK);
            }
        } else {
            supervisor.rollback(branchName, "Fitness threshold not met: " + fitness.score);
            context.log("[SELF_DEV_DARWIN] Rolled back. Fitness: " + fitness.score);
            result.setSuccess(false);
            result.setDecision(SelfDevDecision.ROLLBACK);
            result.getErrors().add("Fitness threshold not met: " + fitness.score);
        }
        
        return result;
    }
    
    @Override
    public List<BranchVariant> generateVariants(GoalModel goal, IterationManager manager) throws Exception {
        // Delegate to the original DarwinEngine's generateProposals
        return manager.getDarwinEngine().generateProposals(context, goal, manager);
    }
    
    @Override
    public List<BranchVariant> validateVariants(List<BranchVariant> variants, IterationManager manager) {
        List<BranchVariant> valid = new ArrayList<>();
        for (BranchVariant v : variants) {
            if (v.getActions() != null && !v.getActions().isEmpty()) {
                valid.add(v);
            }
        }
        return valid;
    }
    
    @Override
    public EvaluationResult executeWinner(BranchVariant winner, IterationManager manager) throws Exception {
        context.log("[SELF_DEV_DARWIN] Executing winner variant on branch: " + currentBranch);
        
        // Convert BranchVariant.Actions to EMF Tasks
        List<Task> tasks = convertActionsToTasks(winner.getActions());
        
        // Apply the code changes
        boolean success = manager.executeTasksWithRetries(tasks);
        
        if (success) {
            manager.getGitManager().commit("Self-Dev: " + winner.getStrategy(), context);
        }
        
        EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        result.setSuccess(success);
        result.setDecision(success ? SelfDevDecision.CONTINUE : SelfDevDecision.ROLLBACK);
        
        return result;
    }
    
    /**
     * Converts BranchVariant.Actions to EMF Task objects.
     */
    private List<Task> convertActionsToTasks(List<BranchVariant.Action> actions) {
        List<Task> tasks = new ArrayList<>();
        
        if (actions == null || actions.isEmpty()) {
            return tasks;
        }
        
        for (BranchVariant.Action action : actions) {
            Task task = OrchestrationFactory.eINSTANCE.createTask();
            task.setId("task-" + System.currentTimeMillis() + "-" + tasks.size());
            task.setName(action.getDescription() != null ? action.getDescription() : action.getOperation());
            task.setType(action.getOperation());
            task.setResponse(action.getImplementation());
            task.setDescription(action.getDescription());
            task.setStatus(TaskStatus.READY);
            task.setPriority(1);
            task.setApprovalRequired(false);
            
            // Store the target path
            if (action.getTarget() != null && !action.getTarget().isEmpty()) {
                task.getAttachments().add(action.getTarget());
            }
            
            tasks.add(task);
        }
        
        return tasks;
    }
    
    @Override
    public String getMode() {
        return "SELF_DEV";
    }
}