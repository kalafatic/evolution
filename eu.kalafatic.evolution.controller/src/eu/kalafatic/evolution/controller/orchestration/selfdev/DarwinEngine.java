package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Task;

public class DarwinEngine extends BaseAiAgent {
    private final TaskContext context;
    private final GitManager gitManager;
    private final TaskExecutor executor;
    private final Evaluator evaluator;
    private final IterationMemoryService memoryService;

    public DarwinEngine(TaskContext context, IterationMemoryService memoryService) {
        super("DarwinEngine", "DarwinEngine");
        this.context = context;
        this.gitManager = new GitManager(context.getProjectRoot(), context);
        this.executor = new TaskExecutor(context);
        this.evaluator = new Evaluator(context.getProjectRoot(), context);
        this.memoryService = memoryService;
    }

    @Override
    protected String getAgentInstructions() {
        return "You are a Darwinian Evolution Engine for software development.\n" +
               "Your goal is to suggest 2-3 different strategies to solve a problem based on past experience and the current goal.";
    }

    public List<BranchVariant> generateVariants(String goal, String lastError) throws Exception {
        context.log("[DARWIN] Generating variants for goal: " + goal);

        StringBuilder prompt = new StringBuilder();
        prompt.append("Current Goal: ").append(goal).append("\n");
        if (lastError != null) {
            prompt.append("Last Error: ").append(lastError).append("\n");
            List<IterationRecord> failedVariants = memoryService.findByError(lastError);
            if (!failedVariants.isEmpty()) {
                prompt.append("Failed attempts for this error:\n");
                for (IterationRecord r : failedVariants) {
                    prompt.append("- Attempted branch: ").append(r.getBranch()).append(" but failed.\n");
                }
            }
        }

        List<IterationRecord> successful = memoryService.findSuccessfulPatterns(goal);
        if (!successful.isEmpty()) {
            prompt.append("Successful patterns from memory:\n");
            for (IterationRecord r : successful) {
                prompt.append("- ").append(r.getGoal()).append(" (Strategy used in successful branch: ").append(r.getBranch()).append(")\n");
            }
        }

        prompt.append("\nGenerate 2-3 different strategies to achieve the goal.\n");
        prompt.append("Output MUST be a valid JSON array of objects with 'strategy' (description) and 'suffix' (short string for branch name).\n");
        prompt.append("Example: [{\"strategy\": \"Use pattern X\", \"suffix\": \"pattern-x\"}]\n");

        String response = aiService.sendRequest(context.getOrchestrator(), buildPrompt(prompt.toString(), context, null), context);

        int start = response.indexOf("[");
        int end = response.lastIndexOf("]");
        if (start == -1 || end == -1) throw new Exception("Invalid AI response for variants");

        JSONArray array = new JSONArray(response.substring(start, end + 1));
        List<BranchVariant> variants = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            BranchVariant v = new BranchVariant();
            String suffix = obj.getString("suffix");
            v.setBranchName("exp/" + sanitize(goal) + "/" + suffix);
            v.setStrategy(obj.getString("strategy"));
            variants.add(v);
        }
        return variants;
    }

    private String sanitize(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").substring(0, Math.min(s.length(), 20));
    }

    public BranchVariant evaluateVariants(List<BranchVariant> variants, TaskPlanner planner, Iteration iteration) throws Exception {
        String baseBranch = gitManager.getCurrentBranch();
        BranchVariant bestVariant = null;
        double bestScore = -1.0;

        for (BranchVariant variant : variants) {
            context.log("[DARWIN] Evaluating variant: " + variant.getStrategy() + " on branch " + variant.getBranchName());
            try {
                gitManager.createBranch(variant.getBranchName());

                List<Task> tasks = planner.generateTasks(context, variant.getStrategy());
                if (tasks.isEmpty()) {
                    context.log("[DARWIN] No tasks for variant " + variant.getBranchName());
                    variant.setScore(0.0);
                } else {
                    boolean success = executor.executeTasks(tasks);

                    // CRITICAL: Commit changes to the variant branch so they are not lost
                    if (success) {
                        gitManager.commit("Darwin Variant Strategy: " + variant.getStrategy());
                    }

                    EvaluationResult result = evaluator.evaluate();

                    // Simple scoring: testsPassed ? 1.0 : 0.0
                    double score = result.isSuccess() ? 1.0 : 0.0;
                    variant.setScore(score);

                    if (score > bestScore) {
                        bestScore = score;
                        bestVariant = variant;
                    }
                }
            } catch (Exception e) {
                context.log("[DARWIN] Error evaluating variant " + variant.getBranchName() + ": " + e.getMessage());
                variant.setScore(0.0);
            } finally {
                gitManager.forceCheckout(baseBranch);
            }
        }

        return bestVariant;
    }
}
