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
import eu.kalafatic.evolution.controller.orchestration.attachments.TaskIntent;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentHypothesis;
import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceArtifact;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.trajectory.TrajectoryAnalysisRecord;
import eu.kalafatic.evolution.controller.trajectory.strategy.BranchSelector;
import eu.kalafatic.evolution.controller.trajectory.strategy.EvolutionBranch;
import eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive.DiversityPressureController;
import eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive.EvolutionaryPenaltyModel;
import eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive.RejectionPatternAnalyzer;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.controller.orchestration.capability.ICapability;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityStatus;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityContext;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityHealth;
import eu.kalafatic.evolution.controller.orchestration.capability.contracts.IMutationContract;
import java.util.Collections;

public class DarwinEngine extends BaseAiAgent implements ICapability, IMutationContract {
    private final TaskContext context;
    private final IterationMemoryService memoryService;
    private final SystemStateSignalProvider stateProvider;
    private final RejectionPatternAnalyzer rejectionAnalyzer = new RejectionPatternAnalyzer();
    private final EvolutionaryPenaltyModel penaltyModel = new EvolutionaryPenaltyModel();
    private final DiversityPressureController diversityController = new DiversityPressureController();

    private final PolicyResolver policyResolver = new PolicyResolver();
    private final PromptComposer promptComposer = new PromptComposer();
    private CapabilityStatus status = CapabilityStatus.STOPPED;

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
    public String getCapabilityId() {
        return "capability.mutation";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CapabilityStatus getStatus() {
        return status;
    }

    @Override
    public void initialize(CapabilityContext context) throws CapabilityException {
        status = CapabilityStatus.INITIALIZED;
    }

    @Override
    public void start() throws CapabilityException {
        status = CapabilityStatus.STARTED;
    }

    @Override
    public void stop() throws CapabilityException {
        status = CapabilityStatus.STOPPED;
    }

    @Override
    public List<String> getSupportedContracts() {
        return Collections.singletonList(IMutationContract.ID);
    }

    @Override
    public List<String> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    public CapabilityHealth getHealth() {
        return new CapabilityHealth(1.0, "Healthy", 0);
    }

    @Override
    protected String getAgentInstructions() {
        // This is now handled by generateVariants using PromptComposer
        return "Role: Darwin Engine. Strategy: Iterative cognitive orchestration.";
    }

    @Override
    protected String getFooterInstructions() {
        return getFooterInstructions(Collections.emptyList());
    }

    protected String getFooterInstructions(List<EvolutionBranch> selectedStrategies) {
        double eps = 0.5;
        if (context != null && context.getOrchestrationState() != null) {
            Object epsObj = context.getOrchestrationState().getMetadata().get("eps");
            if (epsObj instanceof Double) eps = (Double) epsObj;
        }

        int variantCount = Math.max(2, selectedStrategies.size());

        StringBuilder countInstruction = new StringBuilder();
        countInstruction.append("Output MUST be a valid JSON array of EXACTLY ").append(variantCount).append(" object").append(variantCount > 1 ? "s" : "").append(".\n");
        countInstruction.append("CRITICAL: Each object must represent a DISTINCT engineering strategy with different technical trade-offs.\n");

        if (!selectedStrategies.isEmpty()) {
            countInstruction.append("Each object in the array MUST correspond to one of the following strategies in the specified order:\n");
            for (int i = 0; i < selectedStrategies.size(); i++) {
                EvolutionBranch strategy = selectedStrategies.get(i);
                countInstruction.append(i + 1).append(". ").append(strategy.getType().name()).append(": ").append(strategy.getInstructions()).append("\n");
            }
            if (selectedStrategies.size() < 2) {
                countInstruction.append("2. CURIOSITY: Explore an alternative architectural path or hidden project dependency that might impact the goal.\n");
                variantCount = Math.max(variantCount, 2);
            }
        } else {
            countInstruction.append("You MUST propose at least 2 DIFFERENT strategies (e.g., one CONSERVATIVE_FUTURE and one INNOVATIVE_FUTURE).\n");
            countInstruction.append("STRICT RULE: Do NOT return the same strategy twice. If you only have one idea, create an 'EXPLORATION' variant to explore the codebase.\n");
        }

        return countInstruction.toString() + "\n" +
               "CRITICAL: Do NOT include any conversation, explanation, or <think> tags. ONLY return the JSON array.\n" +
               "Schema:\n" +
               "[\n" +
               "  {\n" +
               "    \"id\": \"string-id\",\n" +
               "    \"strategy_type\": \"<CONSERVATIVE_FUTURE | INNOVATIVE_FUTURE | STRUCTURAL_FUTURE | STABILIZATION | EXPLORATION>\",\n" +
               "    \"strategy\": \"<high-level intent>\",\n" +
               "    \"survival_argument\": \"<why this trajectory should continue - REQUIRED for survival>\",\n" +
               "    \"tradeoffs\": \"<explicit technical tradeoffs>\",\n" +
               "    \"failure_risks\": \"<potential risks and failure modes>\",\n" +
               "    \"pros_cons\": \"<pros and cons analysis of this specific hypothesis>\",\n" +
               "    \"semantic_justification\": \"<why this future should exist in the architecture>\",\n" +
               "    \"projected_steps\": [\"<future adaptive step 1 (N+1)>\", \"<future adaptive step 2 (N+2)>\"],\n" +
               "    \"expected_outputs\": [\"<file/artifact path 1>\", \"<file/artifact path 2>\"],\n" +
               "    \"score\": 0.0-1.0, // Predicted fitness score\n" +
               "    \"suffix\": \"<short string for branch name>\",\n" +
               "    \"actions\": [\n" +
               "      {\n" +
               "        \"domain\": \"file | test | build | structure\",\n" +
               "        \"operation\": \"<operation name, e.g. WRITE, DELETE, MKDIR, TEST, BUILD, ANALYZE>\",\n" +
               "        \"target\": \"<file/module/test path>\",\n" +
               "        \"description\": \"<detailed instruction for the FIRST step only>\"\n" +
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

        // 1. Check for ATOMIC INTENT first - Simplified path for high-confidence simple tasks
        // We still create variants but we ensure diversity if EPS is high enough
        AtomicIntentAnalysis atomicAnalysis = (AtomicIntentAnalysis) context.getOrchestrationState().getMetadata().get("atomicAnalysis");

        // 2. Read BitState from context
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
            state.append("\n--- EVOLUTIONARY STRATEGY HINT: ").append(currentPhase).append(" ---\n");
            if (EvolutionConstants.PHASE_INTENT_EXPANSION.equals(currentPhase)) {
                state.append("STRATEGY: USER INTENT RECONSTRUCTION\n");
                state.append("Focus on analyzing explicit/implied intent, hidden expectations, and missing constraints. Reformulate the user request into a precise engineering objective.\n");
                state.append("STRICT RULE: Reformulations MUST stay grounded in the user's primary objective. Do NOT propose unrelated boilerplate or structural changes.\n");
            } else if (EvolutionConstants.PHASE_ARCHITECTURE_VARIANTS.equals(currentPhase)) {
                state.append("STRATEGY: ARCHITECTURE DISCOVERY & DESIGN\n");
                state.append("Focus on analyzing repository structure intelligently. Identify orchestrators, controllers, and architecture-defining files. Propose competing architectural designs.\n");
            } else if (EvolutionConstants.PHASE_SELECTION_REFINEMENT.equals(currentPhase)) {
                state.append("STRATEGY: CONTEXT CURATION & SELECTION\n");
                state.append("Focus on constructing a STRICT CONTEXT MANIFEST. Categorize files into CORE, ARCHITECTURE, and DOCS context.\n");
            } else if (EvolutionConstants.PHASE_IMPLEMENTATION_PLAN.equals(currentPhase)) {
                state.append("STRATEGY: PROMPT EVOLUTION & PLANNING\n");
                state.append("Focus on constructing the FINAL EXECUTION PROMPT. Brief an external LLM as a senior architect briefing an elite engineer.\n");
            } else if (EvolutionConstants.PHASE_FINAL_SYNTHESIS.equals(currentPhase)) {
                state.append("STRATEGY: IMPLEMENTATION GUIDANCE & PACKAGING\n");
                state.append("Focus on providing architectural constraints, integration cautions, and validation expectations.\n");
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
            state.append("\n--- STRUCTURED INTENT ANALYSIS ---\n");
            state.append("Interpretation State: ").append(expansion.getState()).append("\n");
            if (expansion.getDominantIntent() != null) {
                state.append("Dominant Intent: ").append(expansion.getDominantIntent()).append("\n");
            }

            if (!expansion.getImplementationStrategies().isEmpty()) {
                state.append("\nPROPOSED IMPLEMENTATION STRATEGIES (SPAWN BRANCHES FOR THESE):\n");
                for (String strategy : expansion.getImplementationStrategies()) {
                    state.append("- ").append(strategy).append("\n");
                }
            }

            state.append("\nHYPOTHESES:\n");
            for (IntentHypothesis h : expansion.getHypotheses()) {
                state.append("- Hypothesis [").append(h.getId()).append("]: ").append(h.getDescription()).append("\n");
                for (IntentHypothesis.DimensionValue dv : h.getDimensionValues()) {
                    state.append("  * ").append(dv.getDimensionId()).append(": ").append(dv.getValue()).append("\n");
                }
            }
            state.append("\nYour variants MUST be derived from the dominant intent and these strategies/hypotheses.\n");
        }

        // Activation Gate: Only ACTIVE branches influence subsequent iterations
        List<IterationRecord> records = memoryService.getRecords();
        List<IterationRecord> activeRecords = memoryService.getActiveLineage();

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

        // SEQUENTIAL MUTATION CHAIN: Retrieve all previous branches in this iteration history
        List<TrajectoryAnalysisRecord> currentIterationBranches = memoryService.getTrajectoryAnalyses().stream()
                .filter(t -> String.valueOf(context.getCurrentIteration()).equals(t.getIterationId()))
                .collect(Collectors.toList());

        if (!currentIterationBranches.isEmpty()) {
            state.append("\n--- EVOLUTION MEMORY (CURRENT ITERATION PROPOSALS) ---\n");
            for (TrajectoryAnalysisRecord t : currentIterationBranches) {
                state.append("- Previously proposed in this iteration: ").append(t.getStrategy()).append("\n");
            }
        }

        // Adaptive Feedback Learning: Extract guidance from rejected patterns
        try {
            context.log("[DARWIN] Running adaptive feedback analysis on " + records.size() + " records.");
            JSONObject adaptiveAnalysis = rejectionAnalyzer.analyze(records, context);
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
        String composedPrompt = promptComposer.compose(policy, modules, state.toString());
        String basePrompt = buildPrompt(composedPrompt, context, null);

        // MODULATION: Inject EPS context into the prompt
        Object epsObj = context.getOrchestrationState().getMetadata().get("eps");
        double eps = (epsObj instanceof Double) ? (Double) epsObj : 0.5;

        // ADJUST PRESSURE: Increase EPS if last reality check was not significant
        Boolean lastSignificant = (Boolean) context.getOrchestrationState().getMetadata().get("lastRealityCheckSignificant");
        if (lastSignificant != null && !lastSignificant) {
            eps = Math.min(1.0, eps + 0.2);
            context.log("[DARWIN] Increasing Evolution Pressure (EPS) due to insignificant last reality check: " + String.format("%.2f", eps));
            context.getOrchestrationState().getMetadata().put("eps", eps);
        }
        basePrompt += "\n[SYSTEM_DIRECTIVE] Evolution Pressure Scalar (EPS): " + String.format("%.2f", eps) + ".\n";

        // ========================================
        // SEMANTIC MUTATION PIPELINE (REFACTOR)
        // ========================================

        DarwinVariantSpawner spawner = new DarwinVariantSpawner(aiService);
        DarwinDiversityAnalyzer diversityAnalyzer = new DarwinDiversityAnalyzer();

        List<DarwinStrategySeed> mutationSeeds = new ArrayList<>();
        int currentIteration = context.getCurrentIteration();
        boolean isHighConfidenceAtomic = atomicAnalysis != null && atomicAnalysis.isAtomic() && atomicAnalysis.getConfidence() >= 0.8 && !atomicAnalysis.isRequiresPlanning();

        if (isHighConfidenceAtomic && currentIteration == 1) {
            context.log("[DARWIN] High-confidence atomic intent detected. Spawning competing simple futures.");
            mutationSeeds.add(new DarwinStrategySeed(DarwinStrategyType.KEEPER_EVOLUTION, "Direct minimal implementation of: " + goal, true));

            // Simple tasks produce 2-4 trajectories
            mutationSeeds.add(DarwinStrategySeed.semanticFuture("Minimalist", "Atomic Utility", "A minimal, zero-dependency atomic utility for: " + goal));
            mutationSeeds.add(DarwinStrategySeed.semanticFuture("Extensible", "Service Abstraction", "A reusable service abstraction with an interface for: " + goal));
            if (eps >= 0.7) {
                mutationSeeds.add(DarwinStrategySeed.semanticFuture("Robust", "Defensive Engineering", "A robust implementation with extensive error handling and logging for: " + goal));
            }
            context.getOrchestrationState().getMetadata().put("is_atomic_round", true);
        } else if (currentIteration == 1) {
            if (expansion != null && (!expansion.getImplementationStrategies().isEmpty() || !expansion.getHypotheses().isEmpty())) {
                context.log("[DARWIN] Iteration 1: Spawning seeds from expanded intent space.");
                for (IntentHypothesis h : expansion.getHypotheses()) {
                    mutationSeeds.add(DarwinStrategySeed.semanticFuture("Hypothesis", h.getDescription(), h.getDescription()));
                }
                for (String strategy : expansion.getImplementationStrategies()) {
                    mutationSeeds.add(DarwinStrategySeed.semanticFuture("Implementation Strategy", "Architectural Assumption", strategy));
                }
            }

            if (mutationSeeds.isEmpty()) {
                mutationSeeds.add(DarwinStrategySeed.keeperEvolution());
                mutationSeeds.add(DarwinStrategySeed.divergenceA());
                if (eps >= 0.6) {
                    mutationSeeds.add(DarwinStrategySeed.divergenceB());
                }
            }
        } else {
            context.log("[DARWIN] Iteration " + currentIteration + ": Mutating surviving trajectory.");
            IterationRecord winner = activeRecords.isEmpty() ? null : activeRecords.get(activeRecords.size() - 1);
            if (winner != null) {
                mutationSeeds.add(DarwinStrategySeed.keeperEvolution()); // Refinement

                // Mutate the winner's trajectory along different dimensions
                mutationSeeds.add(DarwinStrategySeed.semanticFuture(winner.getStrategy(), "Robustness Mutation", "Extend " + winner.getStrategy() + " with production-grade error handling and validation."));
                mutationSeeds.add(DarwinStrategySeed.semanticFuture(winner.getStrategy(), "Performance Mutation", "Optimize " + winner.getStrategy() + " for high-throughput or non-blocking execution."));
                mutationSeeds.add(DarwinStrategySeed.semanticFuture(winner.getStrategy(), "Flexibility Mutation", "Extract interfaces and apply dependency injection to " + winner.getStrategy()));
            } else {
                mutationSeeds.add(DarwinStrategySeed.keeperEvolution());
                mutationSeeds.add(DarwinStrategySeed.divergenceA());
            }
        }

        // Limit seeds to 4 for small model stability, but ensure at least 2
        if (mutationSeeds.size() > 4) mutationSeeds = mutationSeeds.subList(0, 4);
        while (mutationSeeds.size() < 2) mutationSeeds.add(DarwinStrategySeed.divergenceA());

        context.log("[DARWIN] Executing Semantic Mutation Chain with " + mutationSeeds.size() + " seeds.");
        List<JSONObject> mutationVariants = spawner.spawn(goal, mutationSeeds, basePrompt, context);
        List<JSONObject> uniqueVariants = diversityAnalyzer.analyze(mutationVariants, context);

        // 8. Synthetic Recovery (Phase 6)
        DarwinSyntheticVariantFactory syntheticFactory = new DarwinSyntheticVariantFactory();
        if (uniqueVariants.isEmpty()) {
            uniqueVariants.add(syntheticFactory.synthesizeImplementation(goal, atomicAnalysis));
        }
        if (uniqueVariants.size() < 2) {
            uniqueVariants.add(syntheticFactory.synthesizeSemanticAlternative(uniqueVariants.get(0), goal));
        }

        // 9. Fitness Ranking
        DarwinFitnessRanker ranker = new DarwinFitnessRanker();
        Object isAtomicRound = context.getOrchestrationState().getMetadata().get("is_atomic_round");
        ranker.rank(uniqueVariants, isAtomicRound instanceof Boolean && (Boolean)isAtomicRound);

        context.log("[DARWIN_BRANCHES] " + uniqueVariants.toString());

        // 10. Map to Model
        List<BranchVariant> variants = new ArrayList<>();
        for (JSONObject obj : uniqueVariants) {
            BranchVariant v = mapToBranchVariant(obj, goal, currentPhase, trajectory, context);
            variants.add(v);
        }

        // PERSISTENCE: Save successful historical mutation patterns
        persistSuccessfulPatterns(variants, context);

        return variants;
    }

    private BranchVariant mapToBranchVariant(JSONObject obj, String goal, String currentPhase, Trajectory trajectory, TaskContext context) {
        BranchVariant v = new BranchVariant();
        v.setId(obj.optString("id", "v-" + System.currentTimeMillis()));
        v.setBranchId(v.getId());
        v.setLineageId(context.getSessionId());
        v.setActivationState(BranchVariant.ActivationState.ARCHIVED);
        v.setStrategyType(obj.optString("strategy_type", "UNKNOWN"));
        v.setStrategy(obj.optString("strategy", "unknown"));
        v.setSemanticAnchor(v.getStrategy());
        v.setMutationTrace("Generated in phase: " + currentPhase);
        v.setScore(obj.optDouble("score", 0.0));
        String suffix = obj.optString("suffix", "variant");
        v.setBranchName("exp/" + sanitize(goal) + "/" + sanitize(suffix));
        v.setSurvivalArgument(obj.optString("survival_argument", "none"));
        v.setTradeoffs(obj.optString("tradeoffs", "none"));
        v.setFailureRisks(obj.optString("failure_risks", "none"));

        // Initialize Trajectory metadata
        Trajectory t = new Trajectory(v.getId(), v.getStrategy());
        t.setProsConsAnalysis(obj.optString("pros_cons", "none"));
        t.setSemanticJustification(obj.optString("semantic_justification", "none"));
        t.setFitnessScore(v.getScore());

        // Darwinian Lineage Tracking
        if (trajectory != null) {
            t.setParentTrajectoryId(trajectory.getTrajectoryId());
            trajectory.addChildTrajectoryId(t.getTrajectoryId());
            t.getMutationLineage().addAll(trajectory.getMutationLineage());
        }
        t.addMutationToLineage(v.getStrategy());

        v.setTrajectoryId(t.getTrajectoryId());
        if (memoryService != null && memoryService.getTrajectoryMemory() != null) {
            memoryService.getTrajectoryMemory().recordTrajectory(t);
        }

        JSONArray stepsArr = obj.optJSONArray("projected_steps");
        if (stepsArr != null) {
            for (int j = 0; j < stepsArr.length(); j++) {
                v.getProjectedSteps().add(stepsArr.getString(j));
            }
        }

        JSONArray outputsArr = obj.optJSONArray("expected_outputs");
        if (outputsArr != null) {
            for (int j = 0; j < outputsArr.length(); j++) {
                v.getExpectedOutputs().add(outputsArr.getString(j));
            }
        }

        // Parse Actions
        JSONArray actionsArr = obj.optJSONArray("actions");
        if (actionsArr != null) {
            for (int i = 0; i < actionsArr.length(); i++) {
                JSONObject aObj = actionsArr.getJSONObject(i);
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

        return v;
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
