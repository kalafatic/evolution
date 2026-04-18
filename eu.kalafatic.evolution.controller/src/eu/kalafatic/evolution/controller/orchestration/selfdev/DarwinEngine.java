package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.model.orchestration.PlatformMode;
import eu.kalafatic.evolution.model.orchestration.PlatformType;
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
    private final SystemStateSignalProvider stateProvider;

    public DarwinEngine(TaskContext context, IterationMemoryService memoryService, SystemStateSignalProvider stateProvider) {
        super("DarwinEngine", "DarwinEngine");
        this.context = context;
        this.gitManager = new GitManager(context.getProjectRoot(), context);
        this.executor = new TaskExecutor(context);
        this.evaluator = new Evaluator(context.getProjectRoot(), context);
        this.memoryService = memoryService;
        this.stateProvider = stateProvider;
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
               "→ Use the provided StateSnapshot (build, tests, coverage) to inform decisions.\n" +
               "→ Analyze relationships between elements (files, modules, tests, failures, dependencies).\n" +
               "→ Identify weak points (failures, instability, complexity).\n" +
               "→ Propose actions that improve overall system health.\n\n" +
               "FAILURE FINGERPRINTING & ANTI-LOOP:\n" +
               "→ Avoid repeating actions that lead to the same failure fingerprints.\n" +
               "→ If a failure is REPEATING (count >= 2), you MUST change your strategy.\n\n" +
               "HYPOTHESIS-DRIVEN VARIANTS:\n" +
               "→ Every variant MUST include a hypothesis: a causal explanation of why the proposed changes will lead to the expected effects.\n" +
               "→ Expected effects must be measurable outcomes (e.g., 'build success', 'test X passes').\n\n" +
               "TRAJECTORY AWARENESS:\n" +
               "→ Consider the build/test trends. Prefer variants that improve ANY dimension.\n\n" +
               "PRIORITY LOGIC:\n" +
               "→ IF build == FAIL → focus on build fixes.\n" +
               "→ ELSE IF tests failing → focus on test fixes.\n" +
               "→ ELSE → refinement.\n\n" +
               "ITERATION STRATEGY (DARWINIAN):\n" +
               "→ Generate 2–3 DIFFERENT candidate state transitions.\n" +
               "→ Each candidate must represent a distinct strategy and target a meaningful system improvement.\n" +
               "→ Avoid cosmetic changes, repeated failed approaches, or low-impact modifications.";
    }

    @Override
    protected String getFooterInstructions() {
        return "Output MUST be a valid JSON array of 2-3 objects. Each object is a structured PROPOSAL for a state transition.\n" +
               "Schema:\n" +
               "[\n" +
               "  {\n" +
               "    \"id\": \"string-id\",\n" +
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
               "    \"hypothesis\": {\n" +
               "      \"description\": \"<causal explanation of why this will work>\",\n" +
               "      \"expected_effects\": [\"<measurable outcome 1>\", \"<measurable outcome 2>\"]\n" +
               "    },\n" +
               "    \"expected_effect\": {\n" +
               "      \"short_term\": \"...\",\n" +
               "      \"long_term\": \"...\",\n" +
               "      \"risk\": 0.0-1.0,\n" +
               "      \"reversibility\": 0.0-1.0\n" +
               "    }\n" +
               "  }\n" +
               "]";
    }

    public List<BranchVariant> generateVariants(String goal, StateSnapshot snapshot, FailureMemory failureMemory, Trajectory trajectory) throws Exception {
        context.log("[DARWIN] Generating variants for goal: " + goal);

        StringBuilder state = new StringBuilder();
        state.append("Current Goal: ").append(goal).append("\n");

        if (snapshot != null) {
            state.append("\n--- CURRENT STATE SNAPSHOT ---\n");
            state.append("Build Status: ").append(snapshot.build.status).append("\n");
            state.append("Build Errors: ").append(snapshot.build.errorCount).append(" (").append(snapshot.build.errorTypes).append(")\n");
            state.append("Tests: ").append(snapshot.tests.passed).append("/").append(snapshot.tests.total).append(" passed\n");
            if (!snapshot.tests.failingTests.isEmpty()) {
                state.append("Failing Tests: ").append(snapshot.tests.failingTests).append("\n");
            }
        }

        if (trajectory != null) {
            state.append("\n--- TRAJECTORY ---\n");
            state.append("Build Trend: ").append(trajectory.buildTrend).append("\n");
            state.append("Test Trend: ").append(trajectory.testTrend).append("\n");
            state.append("Failure Change: ").append(trajectory.failureChange).append("\n");
        }

        if (failureMemory != null && !failureMemory.getFingerprints().isEmpty()) {
            state.append("\n--- FAILURE MEMORY (ANTI-LOOP) ---\n");
            failureMemory.getFingerprints().forEach((fp, count) -> {
                if (count >= 2) state.append("REPEATING FAILURE: ");
                state.append(fp).append(" (").append(count).append(" occurrences)\n");
            });
        }

        if (stateProvider != null) {
            state.append(stateProvider.getSystemStateSignal());
        }

        state.append("\n--- LEARNING FROM HISTORY ---\n");
        String history = memoryService.getHistoryAnalysis();
        context.log("[DARWIN] History Analysis: " + history);
        state.append(history).append("\n");

        // Adjust instructions based on PlatformMode
        PlatformMode mode = context.getPlatformMode();
        String modeInfo = "";
        if (mode != null) {
            if (mode.getType() == PlatformType.ASSISTED_CODING) {
                modeInfo = "\nPLATFORM MODE: ASSISTED_CODING. Generate only 1 or 2 very safe, conservative variants.\n";
            } else if (mode.getType() == PlatformType.DARWIN_MODE) {
                modeInfo = "\nPLATFORM MODE: DARWIN_MODE. Generate 2-3 competing variants.\n";
            } else if (mode.getType() == PlatformType.SELF_DEV_MODE) {
                modeInfo = "\nPLATFORM MODE: SELF_DEV_MODE. System self-improvement mode. Focus on structural health.\n";
            }
        }

        String fullPrompt = buildPrompt(state.toString() + modeInfo, context, null);
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
            v.setId(obj.optString("id", "v" + i));
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

            // Parse Hypothesis
            JSONObject hypObj = obj.optJSONObject("hypothesis");
            if (hypObj != null) {
                BranchVariant.Hypothesis hyp = new BranchVariant.Hypothesis();
                hyp.setDescription(hypObj.optString("description"));
                JSONArray effectsArr = hypObj.optJSONArray("expected_effects");
                if (effectsArr != null) {
                    for (int j = 0; j < effectsArr.length(); j++) {
                        hyp.getExpectedEffects().add(effectsArr.getString(j));
                    }
                }
                v.setHypothesis(hyp);
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
