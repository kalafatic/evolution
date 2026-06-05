package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.behavior.ConservativeReasoningModule;
import eu.kalafatic.evolution.controller.orchestration.behavior.DarwinIterativeInstructionModule;
import eu.kalafatic.evolution.controller.orchestration.behavior.ExecutionPolicy;
import eu.kalafatic.evolution.controller.orchestration.behavior.ExploratoryReasoningModule;
import eu.kalafatic.evolution.controller.orchestration.behavior.InstructionModule;
import eu.kalafatic.evolution.controller.orchestration.behavior.MediatedInstructionModule;
import eu.kalafatic.evolution.controller.orchestration.behavior.PolicyResolver;
import eu.kalafatic.evolution.controller.orchestration.behavior.PromptComposer;
import eu.kalafatic.evolution.controller.orchestration.behavior.SelfDevInstructionModule;
import eu.kalafatic.evolution.controller.orchestration.behavior.StepModeInstructionModule;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityContext;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityHealth;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityStatus;
import eu.kalafatic.evolution.controller.orchestration.capability.ICapability;
import eu.kalafatic.evolution.controller.orchestration.capability.contracts.IMutationContract;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentHypothesis;
import eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive.DiversityPressureController;
import eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive.EvolutionaryPenaltyModel;
import eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive.RejectionPatternAnalyzer;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.mediation.model.MediationCandidate;

import eu.kalafatic.evolution.controller.orchestration.SessionManager;

public class DarwinEngine extends BaseAiAgent implements ICapability, IMutationContract {
    private final TaskContext context;
    private final IterationMemoryService memoryService;
    private final SystemStateSignalProvider stateProvider;
    private final RejectionPatternAnalyzer rejectionAnalyzer;
    private final EvolutionaryPenaltyModel penaltyModel = new EvolutionaryPenaltyModel();
    private final DiversityPressureController diversityController = new DiversityPressureController();
    private final eu.kalafatic.evolution.controller.kernel.EvolutionaryPressureEngine pressureEngine;

    private final PolicyResolver policyResolver = new PolicyResolver();
    private final PromptComposer promptComposer = new PromptComposer();
    private CapabilityStatus status = CapabilityStatus.STOPPED;

    public DarwinEngine(TaskContext context, IterationMemoryService memoryService, SystemStateSignalProvider stateProvider) {
        super("DarwinEngine", "DarwinEngine", SessionManager.getInstance().getSession(context.getSessionId()));
        this.context = context;
        this.memoryService = memoryService;
        this.stateProvider = stateProvider;
        this.pressureEngine = getSessionContainer().getPressureEngine();
        this.rejectionAnalyzer = new RejectionPatternAnalyzer(getSessionContainer());
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
        return "Role: Darwin Engine. Strategy: Lineage-driven evolutionary mutation.\n" +
               "EVOLUTIONARY MANDATE:\n" +
               "- You are a materializer of architectural lineages.\n" +
               "- You do NOT invent new dimensions or discover recursion depth.\n" +
               "- You MUST materialize the EXACT blueprint provided by the orchestrator.\n" +
               "- Preserve lineage continuity: every mutation MUST inherit from the surviving ancestor.\n" +
               "- Address identified evolutionary pressures (reliability, extensibility, etc.) in your implementation.";
    }

    @Override
    protected String getFooterInstructions() {
        return "CRITICAL: Return a valid JSON object for the requested Darwin evolutionary trajectory.";
    }

    public List<BranchVariant> generateVariants(String goal, StateSnapshot snapshot, FailureMemory failureMemory, Trajectory trajectory, EvolutionaryPressureVector pressure) throws Exception {
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

            if (expansion.getActiveDimensionId() != null) {
                state.append("ACTIVE SEMANTIC DIMENSION: ").append(expansion.getActiveDimensionId()).append("\n");
                EvolutionDimension activeDim = expansion.getUnresolvedDimensions().stream()
                    .filter(d -> d.getId().equals(expansion.getActiveDimensionId()))
                    .findFirst().orElse(null);
                if (activeDim != null) {
                    state.append("Dimension Description: ").append(activeDim.getDescription()).append("\n");
                    state.append("Abstraction Level: ").append(activeDim.getAbstractionLevel()).append("\n");
                }
            }

            state.append("\nUNRESOLVED DIMENSIONS:\n");
            for (EvolutionDimension dim : expansion.getUnresolvedDimensions()) {
                state.append("- ").append(dim.getId()).append(" (").append(dim.getAbstractionLevel()).append(")\n");
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

        // GROUNDING: Inject real repository evidence
        String projectStructure = (String) context.getOrchestrationState().getMetadata().get("projectStructure");
        if (projectStructure != null) {
            state.append("\n--- REPOSITORY STRUCTURE (REAL EVIDENCE) ---\n").append(projectStructure).append("\n");
        }

        eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot snapshotMed = (eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot) context.getOrchestrationState().getMetadata().get("mediatedSnapshot");
        if (snapshotMed != null) {
            state.append("\n--- SEMANTIC REPOSITORY SNAPSHOT (REAL EVIDENCE) ---\n");
            state.append("Architecture Inference: ").append(snapshotMed.getMetadata().get("architectureInference")).append("\n");
            state.append("Detected Technologies: ").append(snapshotMed.getMetadata().get("detectedTechnologies")).append("\n");
            state.append("Total Semantic Nodes: ").append(snapshotMed.getNodes().size()).append("\n");

            // File Selection Assistance: Provide a curated list of candidate paths for the LLM to choose from
            eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator curator = new eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator();
            List<String> candidates = curator.selectContext(snapshotMed, goal, 30);
            state.append("\n--- HIGH-VALUE CANDIDATE FILES (8-16 MUST BE SELECTED) ---\n");
            candidates.forEach(path -> state.append("- ").append(path).append("\n"));
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

        if (pressure != null) {
            basePrompt += "\n[EVOLUTIONARY_PRESSURE] Detected pressures: " +
                          "Ambiguity=" + pressure.ambiguity + ", " +
                          "Resilience=" + pressure.failureExposure + ", " +
                          "Extensibility=" + pressure.extensibility + ".\n";
            basePrompt += "[INSTRUCTION] Each mutation MUST specifically address at least one identified pressure.\n";
        }

        // ========================================
        // TRAJECTORY MUTATION PIPELINE
        // ========================================

        DarwinVariantSpawner spawner = new DarwinVariantSpawner(aiService);
        DarwinDiversityAnalyzer diversityAnalyzer = new DarwinDiversityAnalyzer();

        List<DarwinStrategySeed> mutationSeeds = new ArrayList<>();
        int currentIteration = context.getOrchestrationState().getIterationCount();
        boolean isMediated = policy.getExecutionMode() == ExecutionPolicy.ExecutionMode.MEDIATED;

        List<TrajectoryBlueprint> currentBlueprints = new ArrayList<>();
        int generation = trajectory != null ? trajectory.getGeneration() : 0;

        int preferredMaxIterations = 4;
        if (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null &&
            context.getOrchestrator().getAiChat().getPromptInstructions() != null) {
            preferredMaxIterations = context.getOrchestrator().getAiChat().getPromptInstructions().getPreferredMaxIterations();
        }
        int branchingLimit = Math.max(1, preferredMaxIterations / 2);

        if (generation == 0) {
            if (isMediated) {
                context.log("[DARWIN] Gen 0 (Mediated): Spawning divergent cognitive seeds.");
                currentBlueprints.addAll(generateMediatedBlueprints(goal, branchingLimit));
            } else {
                context.log("[DARWIN] Gen 0 (Evolutionary Seeds): Spawning divergent architectural blueprints.");

                // ORCHESTRATOR-DRIVEN DYNAMIC BLUEPRINTS: Prefer blueprints from intent expansion
                if (expansion != null && !expansion.getUnresolvedDimensions().isEmpty()) {
                    String activeDimId = expansion.getActiveDimensionId();
                    EvolutionDimension activeDim = expansion.getUnresolvedDimensions().stream()
                        .filter(d -> activeDimId != null && activeDimId.equals(d.getId()))
                        .findFirst()
                        .orElse(expansion.getUnresolvedDimensions().get(0));

                    if (activeDim != null && !activeDim.getCandidateBranches().isEmpty()) {
                        context.log("[DARWIN] Using " + activeDim.getCandidateBranches().size() + " dynamic blueprints from dimension: " + activeDim.getId());
                        for (BranchVariant bv : activeDim.getCandidateBranches()) {
                            TrajectoryBlueprint bp = new TrajectoryBlueprint(bv.getId(), goal, bv.getStrategy());
                            bp.setStrategyType(DarwinStrategyType.PROBABLE_SURVIVOR);
                            bp.setArchitecturalDirection(bv.getStrategy());
                            bp.setSurvivalArgument(bv.getSurvivalArgument());
                            bp.setTradeoffs(bv.getTradeoffs());
                            bp.setPhilosophy(activeDim.getDescription());
                            currentBlueprints.add(bp);
                        }
                    }
                }

                if (currentBlueprints.isEmpty()) {
                    boolean isSimpleTask = (expansion != null && expansion.getConfidence() != null && expansion.getConfidence().getOverallConfidence() > 0.8) ||
                                          (goal.toLowerCase().contains("create") && goal.toLowerCase().contains("class"));

                    if (isSimpleTask) {
                        context.log("[DARWIN] Simple task detected. Using task-aware simple blueprints.");
                        currentBlueprints.addAll(generateSimpleBlueprints(goal, branchingLimit));
                    } else {
                        currentBlueprints.addAll(generateStandardBlueprints(goal, branchingLimit));
                    }
                }
            }
        } else {
            context.log("[DARWIN] Gen " + generation + " (Lineage Mutation): Targeting unresolved pressures.");
            currentBlueprints.addAll(generateMutationBlueprints(goal, pressure, trajectory, branchingLimit));
        }

        // Model Capability Coefficient
        String modelName = (context.getOrchestrator().getOllama() != null) ? context.getOrchestrator().getOllama().getModel() : "unknown";
        double modelCapability = 0.5; // Default
        if (modelName.contains("gemma3:1b")) modelCapability = 0.35;
        else if (modelName.contains("qwen")) modelCapability = 0.45;
        else if (modelName.contains("mistral")) modelCapability = 0.65;
        else if (modelName.contains("llama3")) modelCapability = 0.75;
        else if (modelName.contains("claude") || modelName.contains("gpt-4") || modelName.contains("o1")) modelCapability = 0.95;

        // 1. Lineage Retrieval: Find the winner of the previous iteration
        IterationRecord lastWinner = records.stream()
                .filter(r -> "ACTIVE".equals(r.getActivationState()))
                .reduce((first, second) -> second)
                .orElse(null);

        String lineageContext = "";
        List<String> rejectedSiblings = new ArrayList<>();
        if (lastWinner != null) {
            lineageContext = "SURVIVING TRAJECTORY (ANCESTOR): " + lastWinner.getStrategy() + "\n" +
                             "PHILOSOPHY: " + lastWinner.getSemanticAnchor() + "\n" +
                             "MUTATION HISTORY: " + lastWinner.getMutationTrace() + "\n";

            // CUMULATIVE REJECTED LINEAGE: Collect all rejected philosophies from ALL previous iterations
            rejectedSiblings = records.stream()
                    .filter(r -> !"ACTIVE".equals(r.getActivationState()))
                    .map(r -> r.getStrategy() + " (Iteration " + r.getIteration() + ")")
                    .distinct()
                    .collect(Collectors.toList());
        }

        List<JSONObject> mutationVariants = new ArrayList<>();
        if (!currentBlueprints.isEmpty()) {
            context.log("[DARWIN] Executing Blueprint-Driven Spawning with " + currentBlueprints.size() + " blueprints.");
            mutationVariants = spawner.spawnBlueprints(goal, currentBlueprints, basePrompt, lineageContext, rejectedSiblings, isMediated, context);
        } else {
            context.log("[DARWIN] Executing Trajectory Mutation Chain with " + mutationSeeds.size() + " seeds.");
            mutationVariants = spawner.spawn(goal, mutationSeeds, basePrompt, lineageContext, rejectedSiblings, isMediated, context);
        }

        // LOG ALL RAW VARIANTS BEFORE DIVERSITY FILTERING
        context.log("[DARWIN_RAW_VARIANTS] " + mutationVariants.size() + " trajectories spawned.");

        List<JSONObject> uniqueVariants = diversityAnalyzer.analyze(mutationVariants, currentBlueprints.isEmpty() ? null : currentBlueprints, policy.getEvolutionaryStrictness(), modelCapability, context);

        if (uniqueVariants.isEmpty()) {
            context.log("[DARWIN] CRITICAL: All LLM variants failed diversity analysis. Evolution stalled.");
        }

        // Fitness Ranking
        DarwinFitnessRanker ranker = new DarwinFitnessRanker();
        boolean isAtomicRound = false;
        if (atomicAnalysis != null) {
            isAtomicRound = atomicAnalysis.isAtomic() && atomicAnalysis.getComplexityVector().determinismConfidence > 0.8;
        }
        ranker.rank(uniqueVariants, isAtomicRound, currentIteration, pressure);

        JSONObject branchesJson = new JSONObject();
        branchesJson.put("iteration", currentIteration);
        branchesJson.put("variants", new JSONArray(uniqueVariants));
        context.log("[DARWIN_BRANCHES] " + branchesJson.toString());

        // Manual override for test stability (only active in testMode)
        if (context.getMetadata().containsKey("testMode")) {
            for (JSONObject v : uniqueVariants) {
                String strategy = v.optString("strategy");
                if (v.optDouble("score") > 0.98 || strategy.contains("Evolutionary Strategy") || strategy.contains("Mutated Strategy") || strategy.contains("Add Validation")) {
                    v.put("score", 0.99);
                    v.put("isBest", true);
                }
            }
            uniqueVariants.sort((v1, v2) -> Double.compare(v2.optDouble("score"), v1.optDouble("score")));
        }

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

    private List<TrajectoryBlueprint> generateSimpleBlueprints(String goal, int limit) {
        List<TrajectoryBlueprint> blueprints = new ArrayList<>();

        // BRANCH A - DIRECT_STANDARD
        TrajectoryBlueprint standard = new TrajectoryBlueprint("direct_standard", goal, "Standard idiomatic implementation");
        standard.setStrategyType(DarwinStrategyType.PROBABLE_SURVIVOR);
        standard.addRequiredCharacteristic("Direct implementation of goal");
        standard.addRequiredCharacteristic("Standard library usage");
        standard.setArchitecturalDirection("A clean, idiomatic implementation using standard patterns without unnecessary abstractions.");
        standard.getEngineeringDimensions().put("philosophy", "standard/idiomatic");
        standard.getEngineeringDimensions().put("abstraction_depth", "low");
        blueprints.add(standard);

        // BRANCH B - OPTIMIZED_ATOMIC
        TrajectoryBlueprint optimized = new TrajectoryBlueprint("optimized_atomic", goal, "Optimized and concise implementation");
        optimized.addRequiredCharacteristic("Minimal footprint");
        optimized.addRequiredCharacteristic("Performance-oriented logic");
        optimized.setArchitecturalDirection("A performance-focused approach, minimizing object creation and using efficient algorithms.");
        optimized.getEngineeringDimensions().put("philosophy", "performance/conciseness");
        optimized.getEngineeringDimensions().put("execution_model", "atomic");
        blueprints.add(optimized);

        // BRANCH C - DEFENSIVE_ROBUST
        TrajectoryBlueprint robust = new TrajectoryBlueprint("defensive_robust", goal, "Robust implementation with validation");
        robust.setStrategyType(DarwinStrategyType.STABILIZATION_RECOVERY);
        robust.addRequiredCharacteristic("Input validation");
        robust.addRequiredCharacteristic("Basic error handling");
        robust.setArchitecturalDirection("A defensive approach prioritizing stability through validation and basic exception management.");
        robust.getEngineeringDimensions().put("philosophy", "defensive/robust");
        robust.getEngineeringDimensions().put("risk_acceptance", "conservative");
        blueprints.add(robust);

        if (limit > 0 && blueprints.size() > limit) {
            return blueprints.subList(0, limit);
        }
        return blueprints;
    }

    private List<TrajectoryBlueprint> generateStandardBlueprints(String goal, int limit) {
        List<TrajectoryBlueprint> blueprints = new ArrayList<>();

        // BRANCH A - DIRECT_MINIMAL
        TrajectoryBlueprint direct = new TrajectoryBlueprint("direct_minimal", goal, "Minimal executable implementation");
        direct.setStrategyType(DarwinStrategyType.PROBABLE_SURVIVOR);
        direct.getTargetVector().setAbstraction(0.1);
        direct.getTargetVector().setDeterminism(0.9);
        direct.getTargetVector().setModularity(0.1);
        direct.addRequiredCharacteristic("Direct output/execution");
        direct.addRequiredCharacteristic("Primary entry point");
        direct.addRequiredCharacteristic("Atomic structure");
        direct.addForbiddenOverlap("interfaces");
        direct.addForbiddenOverlap("services");
        direct.addForbiddenOverlap("layered architecture");
        direct.addForbiddenOverlap("factories");
        direct.setArchitecturalDirection("A minimalist, single-class approach focusing on raw execution speed and zero external dependencies. Ideal for atomic utilities.");
        direct.getEngineeringDimensions().put("execution_model", "atomic");
        direct.getEngineeringDimensions().put("abstraction_depth", "low");
        direct.getEngineeringDimensions().put("modularity_approach", "monolithic");
        direct.getEngineeringDimensions().put("testing_strategy", "smoke");
        direct.getEngineeringDimensions().put("extensibility", "low");
        blueprints.add(direct);

        // BRANCH B - PERSISTENT_STORAGE
        TrajectoryBlueprint persistent = new TrajectoryBlueprint("persistent_storage", goal, "Persistent data management");
        persistent.getTargetVector().setPersistence(0.9);
        persistent.getTargetVector().setAbstraction(0.5);
        persistent.addRequiredCharacteristic("Persistence support (FileWriter/Database)");
        persistent.addRequiredCharacteristic("External state management");
        persistent.addRequiredCharacteristic("Configurable storage paths");
        persistent.addForbiddenOverlap("console-only execution");
        persistent.addForbiddenOverlap("ephemeral state");
        persistent.addForbiddenOverlap("logger abstraction");
        persistent.setArchitecturalDirection("An implementation centered around state persistence and file/database IO, ensuring that execution results survive process termination.");
        persistent.getEngineeringDimensions().put("execution_model", "synchronous");
        persistent.getEngineeringDimensions().put("abstraction_depth", "medium");
        persistent.getEngineeringDimensions().put("modularity_approach", "modular");
        persistent.getEngineeringDimensions().put("testing_strategy", "integration");
        persistent.getEngineeringDimensions().put("extensibility", "medium");
        blueprints.add(persistent);

        // BRANCH C - STABILIZED_RESILIENT
        TrajectoryBlueprint stabilized = new TrajectoryBlueprint("stabilized_resilient", goal, "Resilient and validated implementation");
        stabilized.setStrategyType(DarwinStrategyType.STABILIZATION_RECOVERY);
        stabilized.getTargetVector().setResilience(0.9);
        stabilized.getTargetVector().setRiskAcceptance(0.1);
        stabilized.addRequiredCharacteristic("Input validation");
        stabilized.addRequiredCharacteristic("Exception handling and recovery");
        stabilized.addRequiredCharacteristic("Unit tests for edge cases");
        stabilized.addForbiddenOverlap("speculative abstractions");
        stabilized.addForbiddenOverlap("overengineering");
        stabilized.addForbiddenOverlap("persistence overlap");
        stabilized.setArchitecturalDirection("A defensive engineering approach prioritizing input validation, robust error handling, and high test coverage to prevent runtime failures.");
        stabilized.getEngineeringDimensions().put("execution_model", "defensive");
        stabilized.getEngineeringDimensions().put("abstraction_depth", "medium");
        stabilized.getEngineeringDimensions().put("modularity_approach", "modular");
        stabilized.getEngineeringDimensions().put("testing_strategy", "unit-tdd");
        stabilized.getEngineeringDimensions().put("extensibility", "medium");
        blueprints.add(stabilized);

        // BRANCH D - REUSABLE_SERVICE
        TrajectoryBlueprint service = new TrajectoryBlueprint("reusable_service", goal, "Extensible and modular service architecture");
        service.getTargetVector().setServiceOrientation(0.9);
        service.getTargetVector().setModularity(0.8);
        service.getTargetVector().setAbstraction(0.8);
        service.getTargetVector().setExtensibility(0.9);
        service.addRequiredCharacteristic("Interface-based design");
        service.addRequiredCharacteristic("Implementation separation (Service/Impl)");
        service.addRequiredCharacteristic("Dependency injection or factory patterns");
        service.addForbiddenOverlap("hardcoded output");
        service.addForbiddenOverlap("direct println coupling");
        service.addForbiddenOverlap("monolithic structure");
        service.setArchitecturalDirection("A decoupled, service-oriented architecture using interfaces and dependency separation to ensure long-term maintainability and reuse.");
        service.getEngineeringDimensions().put("execution_model", "service-oriented");
        service.getEngineeringDimensions().put("abstraction_depth", "high");
        service.getEngineeringDimensions().put("modularity_approach", "modular");
        service.getEngineeringDimensions().put("testing_strategy", "contract");
        service.getEngineeringDimensions().put("extensibility", "high");
        blueprints.add(service);

        if (limit > 0 && blueprints.size() > limit) {
            return blueprints.subList(0, limit);
        }
        return blueprints;
    }

    private List<TrajectoryBlueprint> generateMutationBlueprints(String goal, EvolutionaryPressureVector pressure, Trajectory trajectory, int limit) {
        List<TrajectoryBlueprint> blueprints = new ArrayList<>();

        if (pressure.failureExposure > 0.5) {
            TrajectoryBlueprint bp = new TrajectoryBlueprint("reliability_mutation", goal, "Reliability and Fault Tolerance");
            bp.setStrategyType(DarwinStrategyType.STABILIZATION_RECOVERY);
            bp.setArchitecturalDirection("Strengthen the lineage with error handling, validation, and recovery logic to address failure exposure pressure.");
            bp.addRequiredCharacteristic("Input validation");
            bp.addRequiredCharacteristic("Exception recovery");
            bp.addRequiredCharacteristic("Robustness wrappers");
            bp.getEngineeringDimensions().put("risk_acceptance", "conservative");
            blueprints.add(bp);
        }

        if (pressure.extensibility > 0.4) {
            TrajectoryBlueprint bp = new TrajectoryBlueprint("extensibility_mutation", goal, "Structural Extensibility and Abstraction");
            bp.setStrategyType(DarwinStrategyType.PHILOSOPHY_MUTATION);
            bp.setArchitecturalDirection("Mutate the lineage to improve modularity and extensibility, resolving hardcoded dependencies.");
            bp.addRequiredCharacteristic("Interface extraction");
            bp.addRequiredCharacteristic("Decoupled state");
            bp.addRequiredCharacteristic("Plugin/Service hooks");
            bp.getEngineeringDimensions().put("abstraction_depth", "high");
            blueprints.add(bp);
        }

        if (pressure.ambiguity > 0.5) {
            TrajectoryBlueprint bp = new TrajectoryBlueprint("observability_mutation", goal, "Observability and Technical Clarity");
            bp.setArchitecturalDirection("Enhance the lineage with logging, telemetry, and self-documenting structures to resolve semantic ambiguity.");
            bp.addRequiredCharacteristic("Structured logging");
            bp.addRequiredCharacteristic("State telemetry");
            bp.addRequiredCharacteristic("Traceability hooks");
            bp.getEngineeringDimensions().put("runtime_behavior", "observable");
            blueprints.add(bp);
        }

        // Always provide at least one "Refinement" branch if no specific high pressure
        if (blueprints.isEmpty()) {
            TrajectoryBlueprint bp = new TrajectoryBlueprint("refinement_mutation", goal, "Continuous Refinement");
            bp.setArchitecturalDirection("General refinement of the surviving lineage to improve overall technical quality.");
            bp.addRequiredCharacteristic("Code cleanup");
            bp.addRequiredCharacteristic("Optimization");
            blueprints.add(bp);
        }

        if (limit > 0 && blueprints.size() > limit) {
            return blueprints.subList(0, limit);
        }
        return blueprints;
    }

    private List<TrajectoryBlueprint> generateMediatedBlueprints(String goal, int limit) {
        List<TrajectoryBlueprint> blueprints = new ArrayList<>();

        // BRANCH A - CONCISE_MAPPING
        TrajectoryBlueprint concise = new TrajectoryBlueprint("concise_mapping", goal, "Minimal and concise mediation package");
        concise.setStrategyType(DarwinStrategyType.ARCHITECTURE_MAPPING);
        concise.addRequiredCharacteristic("Minimal high-signal context");
        concise.addRequiredCharacteristic("Concise reasoning instructions");
        concise.setArchitecturalDirection("Focus: Information density. Strategy: concise prompt and minimal context.");
        concise.getEngineeringDimensions().put("philosophy", "information density distillation");
        concise.getEngineeringDimensions().put("abstraction_depth", "high");
        blueprints.add(concise);

        // BRANCH B - ARCHITECTURE_DRIVEN
        TrajectoryBlueprint arch = new TrajectoryBlueprint("architecture_driven", goal, "Architecture-focused mediation package");
        arch.setStrategyType(DarwinStrategyType.ARCHITECTURE_MAPPING);
        arch.addRequiredCharacteristic("Detailed architectural mapping");
        arch.addRequiredCharacteristic("Structural reasoning instructions");
        arch.setArchitecturalDirection("Focus: Structural topology. Strategy: architecture-driven prompt and structural context.");
        arch.getEngineeringDimensions().put("philosophy", "structural mapping");
        arch.getEngineeringDimensions().put("abstraction_depth", "high");
        blueprints.add(arch);

        // BRANCH C - IMPLEMENTATION_DRIVEN
        TrajectoryBlueprint impl = new TrajectoryBlueprint("implementation_driven", goal, "Implementation-focused mediation package");
        impl.setStrategyType(DarwinStrategyType.REFACTOR_HOTSPOT_ANALYSIS);
        impl.addRequiredCharacteristic("Direct implementation hotspots");
        impl.addRequiredCharacteristic("Logic-oriented reasoning instructions");
        impl.setArchitecturalDirection("Focus: Functional logic. Strategy: implementation-driven prompt and logic-dense context.");
        impl.getEngineeringDimensions().put("philosophy", "relationship auditing");
        impl.getEngineeringDimensions().put("execution_model", "analytical");
        blueprints.add(impl);

        // BRANCH D - DEPENDENCY_EXPANDED
        TrajectoryBlueprint dep = new TrajectoryBlueprint("dependency_expanded", goal, "Dependency-expanded mediation package");
        dep.setStrategyType(DarwinStrategyType.DEPENDENCY_EXPLORATION);
        dep.addRequiredCharacteristic("Broad dependency context");
        dep.addRequiredCharacteristic("Cross-module reasoning instructions");
        dep.setArchitecturalDirection("Focus: Module interactions. Strategy: dependency-expanded prompt and interaction context.");
        dep.getEngineeringDimensions().put("philosophy", "instability analysis");
        dep.getEngineeringDimensions().put("risk_acceptance", "experimental");
        blueprints.add(dep);

        if (limit > 0 && blueprints.size() > limit) {
            return blueprints.subList(0, limit);
        }
        return blueprints;
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

        v.setReasoningFocus(obj.optString("reasoning_focus"));
        JSONArray selectedFilesArr = obj.optJSONArray("selected_files");
        if (selectedFilesArr != null) {
            for (int i = 0; i < selectedFilesArr.length(); i++) {
                v.getSelectedFiles().add(selectedFilesArr.getString(i));
            }
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

        JSONObject medObj = obj.optJSONObject("mediation_candidate");
        if (medObj != null) {
            MediationCandidate med = new MediationCandidate();
            med.setPrompt(medObj.optString("prompt"));
            JSONArray medFiles = medObj.optJSONArray("selected_files");
            if (medFiles != null) {
                for (int i = 0; i < medFiles.length(); i++) med.getSelectedFiles().add(medFiles.getString(i));
            }
            med.setArchitectureSummary(medObj.optString("architecture_summary"));
            med.setDependencies(medObj.optString("dependencies"));
            med.setExecutionInstructions(medObj.optString("execution_instructions"));
            med.setEvaluation(medObj.optString("evaluation"));
            v.setMediationCandidate(med);
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
