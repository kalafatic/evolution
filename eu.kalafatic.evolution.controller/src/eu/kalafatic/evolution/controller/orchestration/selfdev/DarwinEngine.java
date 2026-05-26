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
        return "Role: Darwin Engine. Strategy: Trajectory-driven engineering evolution.";
    }

    @Override
    protected String getFooterInstructions() {
        return "CRITICAL: Return a valid JSON object for the requested Darwin evolutionary trajectory.";
    }

    public List<BranchVariant> generateVariants(String goal, StateSnapshot snapshot, FailureMemory failureMemory, Trajectory trajectory) throws Exception {
        context.log("[DARWIN] Generating trajectory-driven variants for goal: " + goal);

        AtomicIntentAnalysis atomicAnalysis = (AtomicIntentAnalysis) context.getOrchestrationState().getMetadata().get("atomicAnalysis");
        long bitState = context.getOrchestrationState().getBitState();
        ExecutionPolicy policy = policyResolver.resolve(bitState);

        List<InstructionModule> modules = new ArrayList<>();
        if (policy.getExecutionMode() == ExecutionPolicy.ExecutionMode.MEDIATED) modules.add(new MediatedInstructionModule());
        if (policy.getWorkflowModel() == ExecutionPolicy.WorkflowModel.SELF_DEV) modules.add(new SelfDevInstructionModule());
        if (policy.getReasoningStrategy() == ExecutionPolicy.ReasoningStrategy.DARWIN) modules.add(new DarwinIterativeInstructionModule());
        if (policy.getReasoningStrategy() == ExecutionPolicy.ReasoningStrategy.CONSERVATIVE) modules.add(new ConservativeReasoningModule());
        if (policy.getReasoningStrategy() == ExecutionPolicy.ReasoningStrategy.EXPLORATORY) modules.add(new ExploratoryReasoningModule());
        if (policy.getInteractionMode() == ExecutionPolicy.InteractionMode.STEP) modules.add(new StepModeInstructionModule());

        StringBuilder state = new StringBuilder();
        state.append("Current Goal: ").append(goal).append("\n");

        if (snapshot != null) {
            state.append("\n--- CURRENT STATE SNAPSHOT ---\n");
            state.append("Build Status: ").append(snapshot.build.status).append("\n");
            state.append("Build Errors: ").append(snapshot.build.errorCount).append(" (").append(snapshot.build.errorTypes).append(")\n");
            state.append("Tests: ").append(snapshot.tests.passed).append("/").append(snapshot.tests.total).append(" passed\n");
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

        IntentExpansionResult expansion = (IntentExpansionResult) context.getMetadata().get("intentExpansion");
        if (expansion != null) {
            state.append("\n--- STRUCTURED INTENT ANALYSIS ---\n");
            state.append("Dominant Intent: ").append(expansion.getDominantIntent()).append("\n");

            if (!expansion.getImplementationStrategies().isEmpty()) {
                state.append("\nPROPOSED IMPLEMENTATION STRATEGIES:\n");
                for (String s : expansion.getImplementationStrategies()) state.append("- ").append(s).append("\n");
            }

            state.append("\nHYPOTHESES:\n");
            for (IntentHypothesis h : expansion.getHypotheses()) {
                state.append("- Hypothesis [").append(h.getId()).append("]: ").append(h.getDescription()).append("\n");
            }
        }

        if (atomicAnalysis != null) {
            state.append("\n--- ATOMIC EXECUTION CONTEXT ---\n");
            state.append("EXPECTED TARGET ARTIFACT: ").append(atomicAnalysis.getTargetArtifact()).append("\n");
            state.append("EXPECTED ARTIFACT TYPE: ").append(atomicAnalysis.getArtifactType()).append("\n");
        }

        List<IterationRecord> records = memoryService.getRecords();
        List<IterationRecord> activeRecords = memoryService.getActiveLineage();

        String history = activeRecords.isEmpty() ? "No active lineage history." :
                      activeRecords.stream()
                        .map(r -> "- Iteration " + r.getIteration() + ": " + r.getStrategy())
                        .collect(Collectors.joining("\n"));

        state.append("\n--- ACTIVE LINEAGE HISTORY ---\n").append(history).append("\n");

        String composedPrompt = promptComposer.compose(policy, modules, state.toString());
        String basePrompt = buildPrompt(composedPrompt, context, null);

        Object epsObj = context.getOrchestrationState().getMetadata().get("eps");
        double eps = (epsObj instanceof Double) ? (Double) epsObj : 0.5;
        basePrompt += "\n[SYSTEM_DIRECTIVE] Evolution Pressure Scalar (EPS): " + String.format("%.2f", eps) + ".\n";

        // ========================================
        // TRAJECTORY MUTATION PIPELINE
        // ========================================

        DarwinVariantSpawner spawner = new DarwinVariantSpawner(aiService);
        DarwinDiversityAnalyzer diversityAnalyzer = new DarwinDiversityAnalyzer();

        List<DarwinStrategySeed> mutationSeeds = new ArrayList<>();
        int currentIteration = context.getOrchestrationState().getIterationCount();
        boolean isHighConfidenceAtomic = atomicAnalysis != null && atomicAnalysis.isAtomic() && atomicAnalysis.getConfidence() >= 0.8;
        boolean isMediated = policy.getExecutionMode() == ExecutionPolicy.ExecutionMode.MEDIATED;

        if (isMediated) {
            context.log("[DARWIN] Mediated Mode: Spawning cognitive interpretation trajectories.");
            mutationSeeds.add(DarwinStrategySeed.architectureMapping());
            mutationSeeds.add(DarwinStrategySeed.dependencyExploration());
            mutationSeeds.add(DarwinStrategySeed.refactorHotspotAnalysis());
            mutationSeeds.add(DarwinStrategySeed.contextReduction());
        } else if (currentIteration == 0) {
            context.log("[DARWIN] Iteration 0: Spawning 4-branch trajectory model.");
            mutationSeeds.add(DarwinStrategySeed.probableSurvivor());
            mutationSeeds.add(DarwinStrategySeed.philosophyMutation());
            mutationSeeds.add(DarwinStrategySeed.maximalDivergence());
            mutationSeeds.add(DarwinStrategySeed.stabilizationRecovery());
        } else {
            context.log("[DARWIN] Iteration " + currentIteration + ": Mutating surviving trajectory.");
            mutationSeeds.add(DarwinStrategySeed.probableSurvivor()); // Refinement of the winner
            mutationSeeds.add(DarwinStrategySeed.philosophyMutation()); // Direct mutation of the winner's philosophy
            mutationSeeds.add(DarwinStrategySeed.maximalDivergence()); // Explore conceptual distance
            mutationSeeds.add(DarwinStrategySeed.stabilizationRecovery()); // Stability fallback
        }

        // 1. Lineage Retrieval: Find the winner of the previous iteration
        IterationRecord lastWinner = records.stream()
                .filter(r -> "ACTIVE".equals(r.getActivationState()))
                .reduce((first, second) -> second)
                .orElse(null);

        String lineageContext = "";
        List<String> rejectedSiblings = new ArrayList<>();
        if (lastWinner != null) {
            lineageContext = "SURVIVING TRAJECTORY: " + lastWinner.getStrategy() + "\n" +
                             "PHILOSOPHY: " + lastWinner.getSemanticAnchor() + "\n" +
                             "MUTATION HISTORY: " + lastWinner.getMutationTrace() + "\n";

            // Collect siblings that were rejected in the same iteration as the winner
            rejectedSiblings = records.stream()
                    .filter(r -> r.getIteration() == lastWinner.getIteration() && !"ACTIVE".equals(r.getActivationState()))
                    .map(IterationRecord::getStrategy)
                    .collect(Collectors.toList());
        }

        context.log("[DARWIN] Executing Trajectory Mutation Chain with " + mutationSeeds.size() + " seeds.");
        List<JSONObject> mutationVariants = spawner.spawn(goal, mutationSeeds, basePrompt, lineageContext, rejectedSiblings, context);
        List<JSONObject> uniqueVariants = diversityAnalyzer.analyze(mutationVariants, context);

        // Synthetic Recovery
        DarwinSyntheticVariantFactory syntheticFactory = new DarwinSyntheticVariantFactory();
        if (uniqueVariants.isEmpty()) {
            uniqueVariants.add(syntheticFactory.synthesizeImplementation(goal, atomicAnalysis));
        }
        if (uniqueVariants.size() < 2) {
            uniqueVariants.add(syntheticFactory.synthesizeSemanticAlternative(uniqueVariants.get(0), goal, atomicAnalysis));
        }

        // Fitness Ranking
        DarwinFitnessRanker ranker = new DarwinFitnessRanker();
        Object isAtomicRound = context.getOrchestrationState().getMetadata().get("is_atomic_round");
        ranker.rank(uniqueVariants, isAtomicRound instanceof Boolean && (Boolean)isAtomicRound, currentIteration);

        context.log("[DARWIN_BRANCHES] " + uniqueVariants.toString());

        List<BranchVariant> variants = new ArrayList<>();
        for (JSONObject obj : uniqueVariants) {
            BranchVariant v = mapToBranchVariant(obj, goal, "TRAJECTORY_EVOLUTION", trajectory, context);
            if (lastWinner != null) {
                v.setInheritedContext(lineageContext);
                v.setRejectedSiblings(rejectedSiblings);
            }
            variants.add(v);
        }

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
        v.setMutationTrace("Generated in trajectory round.");
        v.setScore(obj.optDouble("score", 0.0));
        String suffix = obj.optString("suffix", "variant");
        v.setBranchName("exp/" + sanitize(goal) + "/" + sanitize(suffix));
        v.setSurvivalArgument(obj.optString("survival_argument", "none"));
        v.setTradeoffs(obj.optString("tradeoffs", "none"));
        v.setFailureRisks(obj.optString("failure_risks", "none"));

        Trajectory t = new Trajectory(v.getId(), v.getStrategy());
        t.setProsConsAnalysis(obj.optString("pros_cons", "none"));
        t.setSemanticJustification(obj.optString("semantic_justification", "none"));
        t.setFitnessScore(v.getScore());

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
            for (int j = 0; j < stepsArr.length(); j++) v.getProjectedSteps().add(stepsArr.getString(j));
        }

        JSONArray outputsArr = obj.optJSONArray("expected_outputs");
        if (outputsArr != null) {
            for (int j = 0; j < outputsArr.length(); j++) v.getExpectedOutputs().add(outputsArr.getString(j));
        }

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

        JSONObject hypObj = obj.optJSONObject("hypothesis");
        if (hypObj != null) {
            BranchVariant.Hypothesis hyp = new BranchVariant.Hypothesis();
            hyp.setDescription(hypObj.optString("description"));
            JSONArray effectsArr = hypObj.optJSONArray("expected_effects");
            if (effectsArr != null) {
                for (int j = 0; j < effectsArr.length(); j++) hyp.getExpectedEffects().add(effectsArr.getString(j));
            }
            v.setHypothesis(hyp);
        }

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

    private String sanitize(String s) {
        if (s == null || s.isEmpty()) return "unnamed";
        String sanitized = s.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-");
        if (sanitized.startsWith("-")) sanitized = sanitized.substring(1);
        if (sanitized.endsWith("-")) sanitized = sanitized.substring(0, sanitized.length() - 1);
        if (sanitized.isEmpty()) return "unnamed";
        return sanitized.substring(0, Math.min(sanitized.length(), 30));
    }
}
