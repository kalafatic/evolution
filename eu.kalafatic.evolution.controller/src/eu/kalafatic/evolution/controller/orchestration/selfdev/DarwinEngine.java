package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.parsers.JsonUtils;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorProfile;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorResolver;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.orchestration.behavior.DarwinIterativeInstructionModule;
import eu.kalafatic.evolution.controller.orchestration.behavior.InstructionModule;
import eu.kalafatic.evolution.controller.orchestration.behavior.MediatedInstructionModule;
import eu.kalafatic.evolution.controller.orchestration.behavior.SelfDevInstructionModule;
import eu.kalafatic.evolution.controller.orchestration.behavior.StepModeInstructionModule;
import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive.DiversityPressureController;
import eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive.EvolutionaryPenaltyModel;
import eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive.RejectionPatternAnalyzer;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Task;

public class DarwinEngine extends BaseAiAgent {
    private final TaskContext context;
    private final IterationMemoryService memoryService;
    private final SystemStateSignalProvider stateProvider;
    private final RejectionPatternAnalyzer rejectionAnalyzer = new RejectionPatternAnalyzer();
    private final EvolutionaryPenaltyModel penaltyModel = new EvolutionaryPenaltyModel();
    private final DiversityPressureController diversityController = new DiversityPressureController();

    public DarwinEngine(TaskContext context, IterationMemoryService memoryService, SystemStateSignalProvider stateProvider) {
        super("DarwinEngine", "DarwinEngine");
        this.context = context;
        this.memoryService = memoryService;
        this.stateProvider = stateProvider;
    }

    @Override
    public void setAiService(eu.kalafatic.evolution.controller.orchestration.AiService aiService) {
        super.setAiService(aiService);
        rejectionAnalyzer.setAiService(aiService);
    }

    @Override
    protected String getAgentInstructions() {
        BehaviorResolver resolver = new BehaviorResolver();
        BehaviorProfile profile = resolver.resolve(context);

        StringBuilder sb = new StringBuilder();
        sb.append("Role: Darwin Engine. Strategy: Iterative cognitive orchestration.\n\n");

        List<InstructionModule> modules = new ArrayList<>();
        if (profile.hasTrait(BehaviorTrait.SUPERVISION_MEDIATED)) {
            modules.add(new MediatedInstructionModule());
        }
        if (profile.hasTrait(BehaviorTrait.WORKFLOW_SELF_DEV)) {
            modules.add(new SelfDevInstructionModule());
        }
        if (profile.hasTrait(BehaviorTrait.REASONING_DARWIN_ITERATIVE)) {
            modules.add(new DarwinIterativeInstructionModule());
        }
        if (profile.hasTrait(BehaviorTrait.INTERACTION_STEP_MODE)) {
            modules.add(new StepModeInstructionModule());
        }

        if (modules.isEmpty()) {
            sb.append("Your task is to propose the best STRATEGY to fulfill the user's goal by reasoning over STATE and FEEDBACK.\n\n");
        } else {
            for (InstructionModule module : modules) {
                sb.append(module.getInstructions()).append("\n\n");
            }
        }

        sb.append("PRIMARY OBJECTIVE:\n")
          .append("→ Propose 2-3 distinct candidate state transitions (strategies) to achieve the goal.\n")
          .append("→ CRITICAL: Fulfillment of the current goal is the HIGHEST priority.\n")
          .append("→ If the goal is ANALYTICAL (e.g., 'analyze project'), use ANALYZE operations in 'structure' or 'test' domains.\n\n");

        sb.append("STATE MODEL:\n")
          .append("→ Use the provided StateSnapshot (build, tests, coverage) to inform decisions.\n")
          .append("→ Analyze relationships between elements (files, modules, tests, failures, dependencies).\n")
          .append("→ Identify weak points (failures, instability, complexity).\n")
          .append("→ Propose actions that improve overall system health and achieve the target goal.\n\n")
          .append("FAILURE FINGERPRINTING & ANTI-LOOP:\n")
          .append("→ Avoid repeating actions that lead to the same failure fingerprints.\n")
          .append("→ If a failure is REPEATING (count >= 2), you MUST change your strategy.\n\n")
          .append("HYPOTHESIS-DRIVEN VARIANTS:\n")
          .append("→ Every variant MUST include a hypothesis: a causal explanation of why the proposed changes will lead to the expected effects.\n")
          .append("→ Expected effects must be measurable outcomes (e.g., 'build success', 'test X passes', 'new class prints text').\n\n")
          .append("TRAJECTORY AWARENESS:\n")
          .append("→ Consider the build/test trends. Prefer variants that improve ANY dimension.\n\n")
          .append("PRIORITY LOGIC:\n")
          .append("→ IF build == FAIL → focus on build fixes.\n")
          .append("→ ELSE IF tests failing → focus on test fixes.\n")
          .append("→ ELSE → fulfillment of the current goal.\n\n")
          .append("ITERATION STRATEGY (DARWINIAN):\n")
          .append("→ Generate 2–3 DIFFERENT candidate state transitions.\n")
          .append("→ Each candidate must represent a distinct strategy.\n")
          .append("→ Avoid cosmetic changes, repeated failed approaches, or low-impact modifications.");

        return sb.toString();
    }

    @Override
    protected String getFooterInstructions() {
        return "Output MUST be a valid JSON array of 2-3 objects. Each object is a structured PROPOSAL for a state transition.\n" +
               "CRITICAL: Do NOT include any conversation, explanation, or <think> tags. ONLY return the JSON array.\n" +
               "Schema:\n" +
               "[\n" +
               "  {\n" +
               "    \"id\": \"string-id\",\n" +
               "    \"strategy\": \"<high-level intent>\",\n" +
               "    \"score\": 0.0-1.0, // Predicted probability of success/correctness\n" +
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

        String history = memoryService.getHistoryAnalysis();
        context.log("[DARWIN] History Analysis: " + history);
        state.append("\n--- LEARNING FROM HISTORY ---\n");
        state.append(history).append("\n");

        // Adaptive Feedback Learning
        try {
            context.log("[DARWIN] Running adaptive feedback analysis on " + memoryService.getRecords().size() + " records.");
            JSONObject adaptiveAnalysis = rejectionAnalyzer.analyze(memoryService.getRecords(), context);
            if (adaptiveAnalysis != null) {
                penaltyModel.updateFromAnalysis(adaptiveAnalysis);
                state.append("\n--- ADAPTIVE EVOLUTIONARY GUIDANCE ---\n");

                JSONArray avoid = adaptiveAnalysis.optJSONArray("avoidGuidelines");
                if (avoid != null && avoid.length() > 0) {
                    state.append("AVOID PATTERNS:\n");
                    for (int i = 0; i < avoid.length(); i++) state.append("- ").append(avoid.getString(i)).append("\n");
                }

                JSONArray prefer = adaptiveAnalysis.optJSONArray("preferGuidelines");
                if (prefer != null && prefer.length() > 0) {
                    state.append("PREFER APPROACHES:\n");
                    for (int i = 0; i < prefer.length(); i++) state.append("- ").append(prefer.getString(i)).append("\n");
                }

                JSONArray diversity = adaptiveAnalysis.optJSONArray("diversityDirectives");
                if (diversity != null && diversity.length() > 0) {
                    state.append("DIVERSITY OBJECTIVES:\n");
                    for (int i = 0; i < diversity.length(); i++) state.append("- ").append(diversity.getString(i)).append("\n");
                    diversityController.increasePressure();
                }

                context.log("[DARWIN] Adaptive guidance injected. Pressure Level: " + diversityController.getPressureLevel());
            }
        } catch (Exception e) {
            context.log("[DARWIN] Adaptive analysis failed: " + e.getMessage());
        }

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
        context.log("Evo-DarwinEngine-Thinking: " + fullPrompt);
        String response = aiService.sendRequest(context.getOrchestrator(), fullPrompt, context);
        context.log("Evo-DarwinEngine-Response: " + response);

        if (response == null || response.trim().isEmpty()) {
            context.log("[DARWIN] ERROR: Received empty AI response for variants");
            throw new Exception("Empty AI response for variants");
        }

        JSONArray array = JsonUtils.extractJsonArrayFlexible(response);
        if (array == null) {
            context.log("[DARWIN] ERROR: Failed to extract variants from AI response. Check format.");
            throw new Exception("Invalid AI response for variants: " + response);
        }

        List<BranchVariant> variants = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) {
                context.log("[DARWIN] WARNING: Variant at index " + i + " is not a JSON object. Skipping.");
                continue;
            }
            BranchVariant v = new BranchVariant();
            v.setId(obj.optString("id", "v" + i));
            v.setStrategy(obj.optString("strategy", "unknown"));
            v.setScore(obj.optDouble("score", 0.0));
            String suffix = obj.optString("suffix", "variant-" + i);
            v.setBranchName("exp/" + sanitize(goal) + "/" + sanitize(suffix));

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
        if (s == null || s.isEmpty()) return "unnamed";
        String sanitized = s.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-");
        if (sanitized.startsWith("-")) sanitized = sanitized.substring(1);
        if (sanitized.endsWith("-")) sanitized = sanitized.substring(0, sanitized.length() - 1);
        if (sanitized.isEmpty()) return "unnamed";
        return sanitized.substring(0, Math.min(sanitized.length(), 30));
    }
}
