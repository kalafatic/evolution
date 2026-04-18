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
        return "You are an AI operating inside a general-purpose iterative development system.\n" +
               "Your task is to IMPROVE decision quality and system intelligence by reasoning over STATE and FEEDBACK — not by generating isolated code.\n\n" +
               "PRIMARY OBJECTIVE:\n" +
               "→ Do not think in terms of 'code generation'.\n" +
               "→ Think in terms of: STATE TRANSITIONS, SYSTEM IMPROVEMENT, LONG-TERM EFFECTS.\n" +
               "→ Each iteration must improve the system state, not just produce code.\n\n" +
               "STATE MODEL:\n" +
               "→ Analyze relationships between elements (files, modules, tests, failures, dependencies).\n" +
               "→ Identify weak points (failures, instability, complexity).\n" +
               "→ Propose actions that improve overall system health.\n\n" +
               "ITERATION STRATEGY (DARWINIAN):\n" +
               "→ Generate 2–3 DIFFERENT candidate state transitions.\n" +
               "→ Each candidate must represent a distinct strategy and target a meaningful system improvement (e.g., fix failing tests, reduce complexity, refactor risky code).\n" +
               "→ Avoid cosmetic changes, repeated failed approaches, or low-impact modifications.\n\n" +
               "LEARNING FROM HISTORY:\n" +
               "→ Identify what improved the system and what caused regressions.\n" +
               "→ Prefer strategies with proven success; increase exploration if no recent improvement occurred.\n\n" +
               "EVALUATION THINKING:\n" +
               "→ For each candidate, estimate short_term_impact, long_term_impact, risk (0.0-1.0), and reversibility (0.0-1.0).";
    }

    @Override
    protected String getFooterInstructions() {
        return "Output MUST be a valid JSON array of 2-3 objects. Each object is a structured PROPOSAL for a state transition.\n" +
               "Schema:\n" +
               "[\n" +
               "  {\n" +
               "    \"strategy\": \"<high-level intent>\",\n" +
               "    \"suffix\": \"<short string for branch name>\",\n" +
               "    \"actions\": [\n" +
               "      {\n" +
               "        \"domain\": \"file | test | build | structure\",\n" +
               "        \"operation\": \"<operation name, e.g. WRITE, DELETE, MKDIR, TEST, BUILD, ANALYZE>\",\n" +
               "        \"target\": \"<file/module/test path>\",\n" +
               "        \"description\": \"<detailed instruction of what will be done in this specific step>\"\n" +
               "      }\n" +
               "    ],\n" +
               "    \"expected_effect\": {\n" +
               "      \"short_term\": \"...\",\n" +
               "      \"long_term\": \"...\",\n" +
               "      \"risk\": 0.0-1.0,\n" +
               "      \"reversibility\": 0.0-1.0\n" +
               "    }\n" +
               "  }\n" +
               "]";
    }

    public List<BranchVariant> generateVariants(String goal, String lastError) throws Exception {
        context.log("[DARWIN] Generating variants for goal: " + goal);

        StringBuilder state = new StringBuilder();
        state.append("Current Goal: ").append(goal).append("\n");
        state.append("\n--- SYSTEM STATE ---\n");
        state.append("Modules:\n");
        state.append("- eu.kalafatic.evolution.model (EMF model)\n");
        state.append("- eu.kalafatic.evolution.controller (Core logic, agents, tools)\n");
        state.append("- eu.kalafatic.evolution.view (Eclipse RCP UI)\n");
        state.append("- eu.kalafatic.utils (Utility bundle)\n");
        state.append("Build System: Maven (Tycho)\n");
        state.append("Target Platform: Java 21, Eclipse 2025-12\n");

        state.append("\n--- LEARNING FROM HISTORY & TRAJECTORY ---\n");
        String history = memoryService.getHistoryAnalysis();
        context.log("[DARWIN] History Analysis: " + history);
        state.append(history).append("\n");

        if (lastError != null) {
            state.append("\nURGENT: Last attempt failed with error: ").append(lastError).append("\n");
        }

        String fullPrompt = buildPrompt(state.toString(), context, null);
        context.log("[DARWIN] Built prompt, sending request...");
        String response = aiService.sendRequest(context.getOrchestrator(), fullPrompt, context);
        context.log("[DARWIN] Raw AI response: " + response);

        if (response == null || response.trim().isEmpty()) {
            context.log("[DARWIN] ERROR: Received empty AI response for variants");
            throw new Exception("Empty AI response for variants");
        }

        int start = response.indexOf("[");
        int end = response.lastIndexOf("]");
        if (start == -1 || end == -1) throw new Exception("Invalid AI response for variants");

        JSONArray array = new JSONArray(response.substring(start, end + 1));
        List<BranchVariant> variants = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            BranchVariant v = new BranchVariant();
            v.setStrategy(obj.getString("strategy"));
            String suffix = obj.getString("suffix");
            v.setBranchName("exp/" + sanitize(goal) + "/" + suffix);

            // Parse Actions
            JSONArray actionsArr = obj.optJSONArray("actions");
            if (actionsArr != null) {
                for (int j = 0; j < actionsArr.length(); j++) {
                    JSONObject aObj = actionsArr.getJSONObject(j);
                    BranchVariant.Action action = new BranchVariant.Action();
                    action.setDomain(aObj.optString("domain"));
                    action.setOperation(aObj.optString("operation"));
                    action.setTarget(aObj.optString("target"));
                    action.setDescription(aObj.optString("description"));
                    v.getActions().add(action);
                }
            }

            // Parse Expected Effect
            JSONObject effectObj = obj.optJSONObject("expected_effect");
            if (effectObj != null) {
                BranchVariant.ExpectedEffect effect = new BranchVariant.ExpectedEffect();
                effect.setShortTerm(effectObj.optString("short_term"));
                effect.setLongTerm(effectObj.optString("long_term"));
                effect.setRisk(effectObj.optDouble("risk", 0.5));
                effect.setReversibility(effectObj.optDouble("reversibility", 1.0));
                v.setExpectedEffect(effect);
            }

            variants.add(v);
        }
        return variants;
    }

    private String sanitize(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").substring(0, Math.min(s.length(), 20));
    }
}
