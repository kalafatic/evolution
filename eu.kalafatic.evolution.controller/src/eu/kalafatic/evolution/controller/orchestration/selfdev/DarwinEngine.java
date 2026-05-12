package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.parsers.JsonUtils;
import eu.kalafatic.evolution.controller.orchestration.behavior.BitState;
import eu.kalafatic.evolution.controller.orchestration.behavior.PolicyResolver;
import eu.kalafatic.evolution.controller.orchestration.behavior.ExecutionPolicy;
import eu.kalafatic.evolution.controller.orchestration.behavior.PromptComposer;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorProfile;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorResolver;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.orchestration.behavior.ConservativeReasoningModule;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.orchestration.behavior.DarwinIterativeInstructionModule;
import eu.kalafatic.evolution.controller.orchestration.behavior.ExploratoryReasoningModule;
import eu.kalafatic.evolution.controller.orchestration.behavior.InstructionModule;
import eu.kalafatic.evolution.controller.orchestration.behavior.MediatedInstructionModule;
import eu.kalafatic.evolution.controller.orchestration.behavior.SelfDevInstructionModule;
import eu.kalafatic.evolution.controller.orchestration.behavior.StepModeInstructionModule;
import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentHypothesis;
import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceArtifact;
import eu.kalafatic.evolution.controller.orchestration.evolution.Trajectory;
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

    private final PolicyResolver policyResolver = new PolicyResolver();
    private final PromptComposer promptComposer = new PromptComposer();

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
        // This is now handled by generateVariants using PromptComposer
        return "Role: Darwin Engine. Strategy: Iterative cognitive orchestration.";
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

        // 1. Read BitState from context
        long bitState = context.getOrchestrationState().getBitState();

        // 2. Pass BitState → PolicyResolver
        ExecutionPolicy policy = policyResolver.resolve(bitState);

        // 3. Use ExecutionPolicy to select InstructionModules
        List<InstructionModule> modules = new ArrayList<>();
        if (policy.getExecutionMode() == ExecutionPolicy.ExecutionMode.MEDIATED) modules.add(new MediatedInstructionModule());
        if (policy.getWorkflowModel() == ExecutionPolicy.WorkflowModel.SELF_DEV) modules.add(new SelfDevInstructionModule());
        if (policy.getReasoningStrategy() == ExecutionPolicy.ReasoningStrategy.DARWIN) modules.add(new DarwinIterativeInstructionModule());
        if (policy.getReasoningStrategy() == ExecutionPolicy.ReasoningStrategy.CONSERVATIVE) modules.add(new ConservativeReasoningModule());
        if (policy.getReasoningStrategy() == ExecutionPolicy.ReasoningStrategy.EXPLORATORY) modules.add(new ExploratoryReasoningModule());
        if (policy.getInteractionMode() == ExecutionPolicy.InteractionMode.STEP) modules.add(new StepModeInstructionModule());

        StringBuilder state = new StringBuilder();
        state.append("Current Goal: ").append(goal).append("\n");
        state.append("Execution Mode: ").append(policy.getExecutionMode()).append("\n");
        state.append("Workflow Model: ").append(policy.getWorkflowModel()).append("\n");
        state.append("Supervision Level: ").append(policy.getSupervisionLevel()).append("\n");
        state.append("Reasoning Strategy: ").append(policy.getReasoningStrategy()).append("\n");
        state.append("Repository Isolation: ").append(policy.getRepositoryMode()).append("\n");

        String currentPhase = context.getOrchestrationState().getCurrentPhase();
        if (currentPhase != null) {
            state.append("\n--- CURRENT EVOLUTION PHASE: ").append(currentPhase).append(" ---\n");
            if (EvolutionConstants.PHASE_INTENT_EXPANSION.equals(currentPhase)) {
                state.append("PHASE: USER INTENT RECONSTRUCTION\n");
                state.append("GOAL: Analyze explicit/implied intent, hidden expectations, and missing constraints. Reformulate the user request into a precise engineering objective.\n");
                state.append("STRICT RULE: Reformulations MUST stay grounded in the user's primary objective. Do NOT propose unrelated boilerplate or structural changes.\n");
                state.append("INSTRUCTIONS: In this phase, the 'strategy' field should contain your reformulated objective. The 'actions' array MUST be empty.\n");
                state.append("OUTPUT: 2-3 distinct interpretations of the core engineering goal.\n");
            } else if (EvolutionConstants.PHASE_ARCHITECTURE_VARIANTS.equals(currentPhase)) {
                state.append("PHASE: ARCHITECTURE DISCOVERY & DESIGN\n");
                state.append("GOAL: Analyze repository structure intelligently. Identify orchestrators, controllers, and architecture-defining files.\n");
                state.append("OUTPUT: 2-3 competing architectural designs. Prioritize modularity and structure over implementation.\n");
            } else if (EvolutionConstants.PHASE_SELECTION_REFINEMENT.equals(currentPhase)) {
                state.append("PHASE: CONTEXT CURATION & SELECTION\n");
                state.append("GOAL: Construct a STRICT CONTEXT MANIFEST. Categorize files into CORE, ARCHITECTURE, and DOCS context.\n");
                state.append("OUTPUT: A refined selection of high-signal files. Explicitly exclude noise (binaries, generated code).\n");
            } else if (EvolutionConstants.PHASE_IMPLEMENTATION_PLAN.equals(currentPhase)) {
                state.append("PHASE: PROMPT EVOLUTION & PLANNING\n");
                state.append("GOAL: Construct the FINAL EXECUTION PROMPT. Brief an external LLM as a senior architect briefing an elite engineer.\n");
                state.append("OUTPUT: Detailed step-by-step implementation plan and optimized execution prompt.\n");
            } else if (EvolutionConstants.PHASE_FINAL_SYNTHESIS.equals(currentPhase)) {
                state.append("PHASE: IMPLEMENTATION GUIDANCE & PACKAGING\n");
                state.append("GOAL: Provide architectural constraints, integration cautions, and validation expectations.\n");
                state.append("OUTPUT: Final implementation-ready task package and ZIP content summary. DO NOT generate code unless explicitly requested.\n");
            }
        }

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

        // PERSISTENCE: Inject successful historical mutation patterns from Workspace
        List<WorkspaceArtifact> patterns = context.getSemanticWorkspace().findArtifactsByType("mutation-pattern");
        if (!patterns.isEmpty()) {
            state.append("\n--- SUCCESSFUL HISTORICAL MUTATION PATTERNS ---\n");
            for (WorkspaceArtifact p : patterns) {
                if (p.getConfidence() > 0.8) {
                    state.append("- ").append(p.getContent()).append("\n");
                }
            }
        }

        IntentExpansionResult expansion = (IntentExpansionResult) context.getMetadata().get("intentExpansion");
        if (expansion != null) {
            state.append("\n--- STRUCTURED INTENT HYPOTHESES ---\n");
            state.append("Original Intent was expanded into the following coherent hypotheses:\n");
            for (IntentHypothesis h : expansion.getHypotheses()) {
                state.append("- Hypothesis [").append(h.getId()).append("]: ").append(h.getDescription()).append("\n");
                for (IntentHypothesis.DimensionValue dv : h.getDimensionValues()) {
                    state.append("  * ").append(dv.getDimensionId()).append(": ").append(dv.getValue()).append("\n");
                }
            }
            state.append("\nYour variants MUST be derived from these structured hypotheses.\n");
        }

        // Activation Gate: Only ACTIVE branches influence subsequent iterations
        ActivationGate gate = new ActivationGate();
        List<IterationRecord> activeRecords = memoryService.getRecords().stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .collect(Collectors.toList());

        String history;
        if (activeRecords.isEmpty()) {
            history = "No active previous iteration history available. Fallback to general history analysis.\n" + memoryService.getHistoryAnalysis();
        } else {
            history = "ACTIVE LINEAGE HISTORY (Explicitly selected trajectories):\n" +
                      activeRecords.stream()
                        .map(r -> "- Iteration " + r.getIteration() + ": " + r.getStrategy() + " [Result: " + r.getResult() + "]")
                        .collect(Collectors.joining("\n"));
        }

        context.log("[DARWIN] History Analysis (Filtered by Activation Gate): " + history);
        state.append("\n--- LEARNING FROM HISTORY (ACTIVATED LINEAGE ONLY) ---\n");
        state.append(history).append("\n");

        // Adaptive Feedback Learning: Extract guidance from rejected patterns
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
            context.log("[DARWIN] WARNING: Adaptive feedback analysis failed (non-critical). Continuing evolution loop. Error: " + e.getMessage());
        }

        // 4. Build final prompt via PromptComposer
        String fullPrompt = buildPrompt(promptComposer.compose(policy, modules, state.toString()), context, null);

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
            v.setBranchId(v.getId());
            v.setLineageId(context.getSessionId());
            v.setActivationState(BranchVariant.ActivationState.INACTIVE);
            v.setStrategy(obj.optString("strategy", "unknown"));
            v.setSemanticAnchor(v.getStrategy());
            v.setMutationTrace("Generated in phase: " + currentPhase);
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

        // PERSISTENCE: Save successful historical mutation patterns
        persistSuccessfulPatterns(variants, context);

        return variants;
    }

    private void persistSuccessfulPatterns(List<BranchVariant> variants, TaskContext context) {
        for (BranchVariant variant : variants) {
            if (variant.getScore() > 0.8) {
                String artifactId = "mutation-pattern-" + variant.getId() + "-" + System.currentTimeMillis();
                WorkspaceArtifact artifact = new WorkspaceArtifact(artifactId, "mutation-pattern");
                artifact.setContent("Successful mutation pattern identified: " + variant.getStrategy());
                artifact.setConfidence(variant.getScore());
                artifact.getSemanticTags().add("mutation");
                artifact.getSemanticTags().add(variant.getStrategy());
                artifact.setSourceIteration("it-" + context.getCurrentIteration());
                artifact.setLineageId(variant.getLineageId());

                context.getSemanticWorkspace().addArtifact(artifact);
                context.log("[WORKSPACE] Persisted mutation pattern: " + variant.getStrategy());
            }
        }
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
