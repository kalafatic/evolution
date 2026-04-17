package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Task;

public class DarwinEngine extends BaseAiAgent {
    private final TaskContext context;
    private final GitManager gitManager;
    private TaskExecutor executor;
    private Evaluator evaluator;
    private final IterationMemoryService memoryService;

    public DarwinEngine(TaskContext context, IterationMemoryService memoryService) {
        super("DarwinEngine", "DarwinEngine");
        this.context = context;
        this.gitManager = new GitManager(context.getProjectRoot(), context);
        this.executor = new TaskExecutor(context);
        this.evaluator = new Evaluator(context.getProjectRoot(), context);
        this.memoryService = memoryService;
    }

    public void setExecutor(TaskExecutor executor) {
        this.executor = executor;
    }

    public void setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an advanced Darwinian Evolution Engine for software development.\n" +
               "Your goal is to suggest 2-3 different solution strategies (variants) based on the current goal and past performance history.\n" +
               "You must learn from past successes and avoid repeating past failures (Guided Evolution).\n" +
               "Each strategy must be clearly distinct in approach.";
    }

    public List<BranchVariant> generateVariants(String goal, String lastError) throws Exception {
        context.log("[DARWIN] Generating variants for goal: " + goal);

        String historyAnalysis = memoryService.getHistoryAnalysis();
        context.log("[DARWIN] " + historyAnalysis);

        StringBuilder prompt = new StringBuilder();
        prompt.append("Current Goal: ").append(goal).append("\n\n");
        prompt.append(historyAnalysis).append("\n\n");

        if (lastError != null) {
            prompt.append("CRITICAL: Last attempt failed with error: ").append(lastError).append("\n");
            if (lastError.toLowerCase().contains("build failed") || lastError.toLowerCase().contains("compilation error")) {
                prompt.append("ADVICE: The build is broken. Focus on a direct, minimal fix as the primary strategy.\n");
            }
        }

        prompt.append("Based on the history and the current goal, generate 2-3 DIFFERENT strategies to achieve the goal.\n");
        prompt.append("For each strategy, provide:\n");
        prompt.append("- strategy: Concrete description of the approach.\n");
        prompt.append("- suffix: Short string for branch name (e.g., 'refactor-stream').\n");
        prompt.append("- expectedImpact: HIGH (significant improvement), MEDIUM, or LOW (minimal/safe).\n");
        prompt.append("- riskLevel: HIGH (likely to break things), MEDIUM, or LOW (unlikely to cause regressions).\n");
        prompt.append("- complexity: HIGH (large changes), MEDIUM, or LOW (small/simple changes).\n");
        prompt.append("- reasoning: Why this strategy was chosen based on historical signal.\n");

        prompt.append("\nOutput MUST be a valid JSON object with the following structure:\n");
        prompt.append("{\n");
        prompt.append("  \"analysis\": {\n");
        prompt.append("    \"what_worked\": \"...\",\n");
        prompt.append("    \"what_failed\": \"...\",\n");
        prompt.append("    \"patterns_to_avoid\": [\"...\"],\n");
        prompt.append("    \"strategy_for_next_iteration\": \"...\"\n");
        prompt.append("  },\n");
        prompt.append("  \"variants\": [\n");
        prompt.append("    {\"strategy\": \"...\", \"suffix\": \"...\", \"expectedImpact\": \"...\", \"riskLevel\": \"...\", \"complexity\": \"...\", \"reasoning\": \"...\"}\n");
        prompt.append("  ]\n");
        prompt.append("}\n");

        String response = aiService.sendRequest(context.getOrchestrator(), buildPrompt(prompt.toString(), context, null), context);

        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start == -1 || end == -1) throw new Exception("Invalid AI response for variants");

        JSONObject root = new JSONObject(response.substring(start, end + 1));
        JSONArray array = root.getJSONArray("variants");

        // Log analysis
        if (root.has("analysis")) {
            JSONObject analysis = root.getJSONObject("analysis");
            context.log("[DARWIN-ANALYSIS] Worked: " + analysis.optString("what_worked"));
            context.log("[DARWIN-ANALYSIS] Failed: " + analysis.optString("what_failed"));
        }

        List<BranchVariant> variants = new ArrayList<>();

        // Skip branching if build is critically broken (Design Rule: Step 9)
        boolean skipBranching = lastError != null && (lastError.toLowerCase().contains("build failed") || lastError.toLowerCase().contains("compilation error"));
        int maxVariants = skipBranching ? 1 : 3;

        for (int i = 0; i < Math.min(array.length(), maxVariants); i++) {
            JSONObject obj = array.getJSONObject(i);
            BranchVariant v = new BranchVariant();
            String suffix = obj.getString("suffix");
            v.setBranchName("exp/" + sanitize(goal) + "/" + suffix);
            v.setStrategy(obj.getString("strategy"));
            v.setExpectedImpact(obj.optString("expectedImpact", "MEDIUM"));
            v.setRiskLevel(obj.optString("riskLevel", "MEDIUM"));
            v.setComplexity(obj.optString("complexity", "MEDIUM"));
            v.setReasoning(obj.optString("reasoning", ""));

            // Programmatic Anti-Loop Protection (Step 6 of Design)
            if (isRepeatedFailure(v.getStrategy())) {
                context.log("[DARWIN] Penalizing variant matching repeated failure: " + v.getStrategy());
                v.setPredictedScore(-10.0); // Large penalty
            }

            variants.add(v);
        }
        return variants;
    }

    private boolean isRepeatedFailure(String strategy) {
        List<IterationRecord> records = memoryService.getRecords();
        int failureCount = 0;
        // Check last 5 records for same strategy failing
        for (int i = Math.max(0, records.size() - 5); i < records.size(); i++) {
            IterationRecord r = records.get(i);
            if ("FAIL".equals(r.getResult()) && strategy.equalsIgnoreCase(r.getStrategy())) {
                failureCount++;
            }
        }
        return failureCount >= 3;
    }

    private String sanitize(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").substring(0, Math.min(s.length(), 20));
    }

    public BranchVariant evaluateVariants(List<BranchVariant> variants, TaskPlanner planner, Iteration iteration) throws Exception {
        if (variants == null || variants.isEmpty()) return null;

        context.log("[BRANCHES]");
        for (int i = 0; i < variants.size(); i++) {
            BranchVariant v = variants.get(i);
            char id = (char) ('A' + i);
            double score = calculateScore(v);
            v.setPredictedScore(score);
            context.log(String.format("%c: %s (impact=%s, risk=%s, complexity=%s, score=%.1f)",
                id, v.getStrategy(), v.getExpectedImpact(), v.getRiskLevel(), v.getComplexity(), score));
        }

        BranchVariant selected = selectBestVariant(variants);
        context.log("\n[SELECTED]\nBranch " + selected.getBranchName());

        String baseBranch = gitManager.getCurrentBranch();
        context.log("[DARWIN] Executing selected variant: " + selected.getStrategy());

        try {
            gitManager.createBranch(selected.getBranchName());

            List<Task> tasks = planner.generateTasks(context, selected.getStrategy());
            if (tasks.isEmpty()) {
                context.log("[DARWIN] No tasks generated for selected variant.");
                selected.setScore(0.0);
            } else {
                boolean success = executor.executeTasks(tasks);

                // Capture changed files
                List<String> changed = tasks.stream()
                    .filter(t -> "file".equalsIgnoreCase(t.getType()))
                    .map(Task::getResultSummary)
                    .filter(path -> path != null && !path.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
                selected.setChangedFiles(changed);

                if (success) {
                    gitManager.commit("Darwin Variant Strategy: " + selected.getStrategy());
                }

                EvaluationResult result = evaluator.evaluate();
                selected.setSuccess(result.isSuccess());
                if (!result.isSuccess()) {
                    selected.setErrorMessage(result.getErrors().toString());
                }

                // Actual score from evaluation
                double score = result.isSuccess() ? 1.0 : 0.0;
                selected.setScore(score);
            }
        } catch (Exception e) {
            context.log("[DARWIN] Error executing variant " + selected.getBranchName() + ": " + e.getMessage());
            selected.setScore(0.0);
        } finally {
            gitManager.forceCheckout(baseBranch);
        }

        return selected;
    }

    private double calculateScore(BranchVariant v) {
        int impact = mapValue(v.getExpectedImpact());
        int risk = mapValue(v.getRiskLevel());
        int complexity = mapValue(v.getComplexity());

        // formula: (3 * impact) - (2 * risk) - (1 * complexity)
        return (3.0 * impact) - (2.0 * risk) - (1.0 * complexity);
    }

    private int mapValue(String val) {
        if ("HIGH".equalsIgnoreCase(val)) return 3;
        if ("MEDIUM".equalsIgnoreCase(val)) return 2;
        if ("LOW".equalsIgnoreCase(val)) return 1;
        return 2; // Default to MEDIUM
    }

    private BranchVariant selectBestVariant(List<BranchVariant> variants) {
        return variants.stream()
            .sorted((v1, v2) -> {
                int cmp = Double.compare(v2.getPredictedScore(), v1.getPredictedScore());
                if (cmp != 0) return cmp;

                // Tie-breaker 1: Lower complexity
                int c1 = mapValue(v1.getComplexity());
                int c2 = mapValue(v2.getComplexity());
                if (c1 != c2) return Integer.compare(c1, c2);

                // Tie-breaker 2: Lower risk
                int r1 = mapValue(v1.getRiskLevel());
                int r2 = mapValue(v2.getRiskLevel());
                return Integer.compare(r1, r2);
            })
            .findFirst()
            .orElse(variants.get(0));
    }
}
