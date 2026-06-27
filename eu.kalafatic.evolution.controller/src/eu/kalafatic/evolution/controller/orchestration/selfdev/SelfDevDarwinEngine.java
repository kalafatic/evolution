package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
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
public class SelfDevDarwinEngine extends AbstractBaseDarwinEngine {

    private final SelfDevSupervisor supervisor;
    private String currentBranch;
    
    public SelfDevDarwinEngine(TaskContext context, IterationMemoryService memoryService, 
                               SelfDevSupervisor supervisor, AiService aiService) {
        super(context, memoryService);
        this.supervisor = supervisor;
        this.currentBranch = supervisor != null ? supervisor.getMainBranch() : "master";
        this.aiService = aiService;
    }

    @Override
    public OrchestratorResponse orchestrateEvolution(TaskRequest taskRequest, IterationManager iterationManager) throws Exception {
        return evolve(taskRequest.getPrompt(), iterationManager, null);
    }

    @Override
    public OrchestratorResponse evolve(String request, IterationManager manager, eu.kalafatic.evolution.controller.orchestration.intent.EvolutionAssessment initialAssessment) throws Exception {
        // Get or create goal model
        GoalModel goal = (GoalModel) context.getOrchestrationState().getMetadata().get("goalModel");
        if (goal == null) {
            goal = manager.getGoalUnderstandingEngine().understand(request, context);
            context.getOrchestrationState().getMetadata().put("goalModel", goal);
        }

        // Run the iteration
        EvaluationResult result = runIteration(goal, manager);

        OrchestratorResponse response = new OrchestratorResponse();
        response.setSummary(result.getSummary() != null ? result.getSummary() : (result.isSuccess() ? "Self-Dev evolution successful" : "Self-Dev evolution failed"));
        return response;
    }
    
    @Override
    public EvaluationResult runIteration(GoalModel goal, IterationManager manager) throws Exception {
        context.log("[SELF_DEV_DARWIN] Running self-dev iteration for: " + goal.getPrimaryAction());
        
        if (supervisor == null) {
            return failedResult("Supervisor not configured for Self-Dev mode");
        }

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
        
        // 2. Generate code variants using the Mutation Engine
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
        
        if (!result.isSuccess()) {
            supervisor.rollback(branchName, "Execution failed");
            return result;
        }

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
                result.setSummary("Merged successfully: " + mergeResult.commitId);
            } else {
                context.log("[SELF_DEV_DARWIN] Merge failed: " + mergeResult.message);
                supervisor.rollback(branchName, "Merge failed: " + mergeResult.message);
                result.setSuccess(false);
                result.setDecision(SelfDevDecision.ROLLBACK);
                result.setSummary("Merge failed: " + mergeResult.message);
            }
        } else {
            supervisor.rollback(branchName, "Fitness threshold not met: " + fitness.score);
            context.log("[SELF_DEV_DARWIN] Rolled back. Fitness: " + fitness.score);
            result.setSuccess(false);
            result.setDecision(SelfDevDecision.ROLLBACK);
            result.getErrors().add("Fitness threshold not met: " + fitness.score);
            result.setSummary("Fitness threshold not met: " + fitness.score);
        }
        
        return result;
    }
    
    @Override
    public List<BranchVariant> generateVariants(GoalModel goal, IterationManager manager) throws Exception {
        // Delegate variant generation to the Mutation Engine capability
        IBaseDarwinEngine engine = (manager != null) ? manager.getDarwinEngine() : null;
        if (engine != null && engine != this) {
            return engine.generateVariants(goal, manager);
        }
        // Fallback to standard mutation logic if we are the primary engine or manager is null
        StandardDarwinEngine standard = new StandardDarwinEngine(context, memoryService, aiService);
        return standard.generateVariants(goal, manager);
    }
    
    @Override
    public List<BranchVariant> validateVariants(List<BranchVariant> variants, IterationManager manager) {
        List<BranchVariant> valid = new ArrayList<>();
        for (BranchVariant v : variants) {
            if (v.getActions() != null && !v.getActions().isEmpty()) {
                valid.add(v);
            }
        }
        // In Self-Dev, we are more lenient if no actions yet (they might be planned during executeWinner)
        if (valid.isEmpty() && variants != null) {
            return variants;
        }
        return valid;
    }
    
    @Override
    public EvaluationResult executeWinner(BranchVariant winner, IterationManager manager) throws Exception {
        context.log("[SELF_DEV_DARWIN] Executing winner variant on branch: " + currentBranch);
        
        // Convert BranchVariant.Actions to EMF Tasks
        List<Task> tasks = convertActionsToTasks(winner.getActions());
        
        if (tasks.isEmpty() && manager != null) {
            tasks = manager.getTaskPlanner().generateTasksFromVariant(context, winner);
        }

        // Apply the code changes
        boolean success = (manager != null) ? manager.executeTasksWithRetries(tasks) : false;
        
        if (success && manager != null) {
            manager.getGitManager().commit("Self-Dev: " + winner.getStrategy(), context);
        }
        
        EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        result.setSuccess(success);
        result.setDecision(success ? SelfDevDecision.CONTINUE : SelfDevDecision.ROLLBACK);
        result.setSummary(success ? "Winner executed successfully" : "Winner execution failed");
        
        return result;
    }
    
    @Override
    public String getMode() {
        return "SELF_DEV";
    }
}
