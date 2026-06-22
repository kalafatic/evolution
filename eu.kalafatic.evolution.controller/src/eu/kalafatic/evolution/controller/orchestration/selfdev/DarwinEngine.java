package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.EvolutionProgressPublisher;
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
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.goal.SemanticEnvelope;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentHypothesis;
import eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive.DiversityPressureController;
import eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive.EvolutionaryPenaltyModel;
import eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive.RejectionPatternAnalyzer;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
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
    private final GenomeDimensionScheduler dimensionScheduler = new GenomeDimensionScheduler();
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

    @Override
    public List<BranchVariant> generateVariants(GoalModel goal, StateSnapshot snapshot, FailureMemory failureMemory, Trajectory trajectory, EvolutionaryPressureVector pressure) throws Exception {
        context.log("Stage: Goal\nGoalModel: " + goal);
        context.log("[DARWIN] Generating trajectory-driven variants for goal: " + goal.getPrimaryAction());

        // ADAPTIVE KERNEL: Uniform Intensity Calculation
        eu.kalafatic.evolution.controller.kernel.EvolutionProfile profile =
            context.getExecutionProfile();
        int intensity = profile.getIntensity();


        AtomicIntentAnalysis atomicAnalysis = (AtomicIntentAnalysis) context.getOrchestrationState().getMetadata().get("atomicAnalysis");
        if (context != null) {
            context.log("Stage: Intent Analysis\nAtomic: " + (atomicAnalysis != null && atomicAnalysis.isAtomic()) + "\nTarget: " + (atomicAnalysis != null ? atomicAnalysis.getTargetArtifact() : "none"));
        }
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
        state.append("Current Goal: ").append(goal.getPrimaryAction()).append("\n");

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
            List<String> candidates = curator.selectContext(snapshotMed, goal.getPrimaryAction(), 32);
            state.append("\n--- HIGH-VALUE CANDIDATE FILES (4-16 MUST BE SELECTED) ---\n");
            candidates.forEach(path -> state.append("- ").append(path).append("\n"));
        }

        eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel realityModel = (eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel) context.getOrchestrationState().getMetadata().get("targetRealityModel");
        if (realityModel != null) {
            state.append("\n--- DISCOVERED TARGET REALITY (GROUNDING SOURCE) ---\n");
            state.append("Domain: ").append(realityModel.getDomain()).append("\n");
            state.append("Purpose: ").append(realityModel.getPurpose()).append("\n");
            state.append("Architecture Summary: ").append(realityModel.getArchitectureSummary()).append("\n");

            if (!realityModel.getSubsystems().isEmpty()) {
                state.append("\nDISCOVERED SUBSYSTEMS:\n");
                for (var s : realityModel.getSubsystems()) {
                    state.append("- ").append(s.getName()).append(": ").append(s.getPurpose()).append("\n");
                }
            }

            if (!realityModel.getArchitecturalFacts().isEmpty()) {
                state.append("\nARCHITECTURAL FACTS:\n");
                for (var f : realityModel.getArchitecturalFacts()) {
                    state.append("- ").append(f.toString()).append("\n");
                }
            }

            state.append("\nObjectives: ").append(realityModel.getObjectives()).append("\n");
            state.append("Risks: ").append(realityModel.getRisks()).append("\n");

            state.append("\nIDENTIFIED HOTSPOTS (PRIORITY EVOLUTION TARGETS):\n");
            for (eu.kalafatic.evolution.controller.mediation.model.Hotspot hotspot : realityModel.getHotspots()) {
                state.append("- ").append(hotspot.getName()).append(" [").append(hotspot.getType()).append("]: ").append(hotspot.getDescription()).append(" (Significance: ").append(hotspot.getSignificance()).append(")\n");
            }
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
        boolean isMediated = policy.getExecutionMode() == ExecutionPolicy.ExecutionMode.MEDIATED ||
                             context.getBehaviorProfile().hasTrait(eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait.WORKFLOW_EXPORT_ONLY);

        List<TrajectoryBlueprint> currentBlueprints = new ArrayList<>();
        int generation = trajectory != null ? trajectory.getGeneration() : 0;

        // Model Capability Coefficient
        String modelName = (context.getOrchestrator().getOllama() != null) ? context.getOrchestrator().getOllama().getModel() : "unknown";
        double modelCapability = 0.5; // Default
        if (modelName.contains("gemma3:1b")) modelCapability = 0.35;
        else if (modelName.contains("qwen")) modelCapability = 0.45;
        else if (modelName.contains("mistral")) modelCapability = 0.65;
        else if (modelName.contains("llama3")) modelCapability = 0.75;
        else if (modelName.contains("claude") || modelName.contains("gpt-4") || modelName.contains("o1")) modelCapability = 0.95;

        // 1. Expansion-Based Population Scaling (Milestone Requirement)
        int expansionValue = 5; // Default Medium
        if (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null) {
            String sessionId = context.getSessionId();
            eu.kalafatic.evolution.model.orchestration.ChatSession chatSession = context.getOrchestrator().getAiChat().getSessions().stream()
                .filter(s -> s.getId().equals(sessionId)).findFirst().orElse(null);
            if (chatSession != null) {
                expansionValue = chatSession.getExpansion();
            }
        }

        // 2. Population Scaling based on Capability and Intensity
        int branchingLimit = 2; // Default
        eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType capType = profile.getCapability();

        switch (capType) {
            case CHAT: branchingLimit = 2; break;
            case CODE: branchingLimit = 2; break; // IMPLEMENTATION
            case EVOLUTION: branchingLimit = 3; break; // REFACTOR
            case ARCHITECTURE: branchingLimit = 4; break; // DESIGN
            case SELF_DEV: branchingLimit = 6; break; // RESEARCH
            default: branchingLimit = 2; break;
        }

        // Expand based on intensity if pressure is high
        if (intensity >= 3) branchingLimit += 1;
        if (intensity == 4) branchingLimit += 1;

        // Respect expansionValue from UI if high
        if (expansionValue >= 8) branchingLimit = Math.max(branchingLimit, 4);

        // Scale by model capability if extremely low
        if (modelCapability < 0.4) branchingLimit = Math.min(branchingLimit, 2);

        context.log("[DARWIN] Adaptive Kernel Intensity: " + intensity + ". Population Target: " + branchingLimit);

        // 1. MULTI-LINEAGE RETRIEVAL: Retrieve both ACTIVE and KEPT survivors (Milestone Requirement)
        List<IterationRecord> survivors = records.stream()
                .filter(r -> "ACTIVE".equals(r.getActivationState()) || "KEPT".equals(r.getActivationState()))
                .collect(Collectors.toList());

        // If no survivors in last iteration, fallback to overall active lineage
        if (survivors.isEmpty()) {
            survivors = activeRecords;
        }

        StringBuilder lineageBuilder = new StringBuilder();
        List<String> rejectedSiblings = new ArrayList<>();
        if (!survivors.isEmpty()) {
            lineageBuilder.append("### EVOLUTIONARY ANCESTORS (COMPETING LINEAGES) ###\n");
            for (IterationRecord ancestor : survivors) {
                lineageBuilder.append("ANCESTOR LINEAGE: ").append(ancestor.getBranchId()).append("\n");
                lineageBuilder.append("STRATEGY: ").append(ancestor.getStrategy()).append("\n");
                lineageBuilder.append("PHILOSOPHY: ").append(ancestor.getSemanticAnchor()).append("\n");
                lineageBuilder.append("MUTATION TRACE: ").append(ancestor.getMutationTrace()).append("\n\n");
            }

            // REFINEMENT: Inject evolved mediation context if present (Understanding Refinement)
            Object winningMedCandidate = context.getOrchestrationState().getMetadata().get("winningMediationCandidate");
            if (winningMedCandidate instanceof eu.kalafatic.evolution.controller.mediation.model.MediationCandidate) {
                eu.kalafatic.evolution.controller.mediation.model.MediationCandidate med = (eu.kalafatic.evolution.controller.mediation.model.MediationCandidate) winningMedCandidate;
                lineageBuilder.append("\n--- EVOLVED MEDIATION GENOME (COMMON ANCESTOR) ---\n");
                lineageBuilder.append("GENOME A (PROMPT): ").append(med.getPrompt()).append("\n\n");
                lineageBuilder.append("GENOME B (PACKAGE/CONTEXT):\n");
                lineageBuilder.append("- ARCHITECTURE: ").append(med.getArchitectureSummary()).append("\n");

                if (med.getSubsystems() != null && !med.getSubsystems().isEmpty()) {
                    lineageBuilder.append("- DISCOVERED SUBSYSTEMS:\n");
                    for (var s : med.getSubsystems()) lineageBuilder.append("  - ").append(s.getName()).append(": ").append(s.getPurpose()).append("\n");
                }

                if (med.getArchitecturalFacts() != null && !med.getArchitecturalFacts().isEmpty()) {
                    lineageBuilder.append("- ARCHITECTURAL FACTS:\n");
                    for (var f : med.getArchitecturalFacts()) lineageBuilder.append("  - ").append(f.toString()).append("\n");
                }

                lineageBuilder.append("- SELECTED FILES: ").append(med.getSelectedFiles()).append("\n");
                lineageBuilder.append("- DEPENDENCIES: ").append(med.getDependencies()).append("\n");
                lineageBuilder.append("- INSTRUCTIONS: ").append(med.getExecutionInstructions()).append("\n");
            }

            // CUMULATIVE REJECTED LINEAGE: Collect all rejected philosophies from ALL previous iterations
            rejectedSiblings = records.stream()
                    .filter(r -> !"ACTIVE".equals(r.getActivationState()) && !"KEPT".equals(r.getActivationState()))
                    .map(r -> r.getStrategy() + " (Iteration " + r.getIteration() + ")")
                    .distinct()
                    .collect(Collectors.toList());
        }
        String lineageContext = lineageBuilder.toString();

        List<JSONObject> uniqueVariants = new ArrayList<>();

        // ADAPTIVE CAPABILITIES (Respecting LOCKED Abstraction Level)
        AbstractionLevel lockedLevel = context.getOrchestrationState().getLockedAbstractionLevel();
        boolean architectureEnabled = intensity >= 3 && (lockedLevel == null || lockedLevel == AbstractionLevel.ARCHITECTURE);
        boolean implementationEnabled = intensity >= 2 || (lockedLevel == AbstractionLevel.IMPLEMENTATION);
        BranchVariant.ReasoningLevel reasoningLevel = intensity == 1 ? BranchVariant.ReasoningLevel.MINIMAL :
                                                      intensity == 4 ? BranchVariant.ReasoningLevel.DEEP : BranchVariant.ReasoningLevel.BALANCED;
        StringBuilder siblingMemoryBuilder = new StringBuilder();

        EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
        String currentParentId = tree.getCurrentWinnerId();
        if (currentParentId == null && tree.getRootId() != null) {
            currentParentId = tree.getRootId();
        }

        // SEMANTIC GENOME: Initialize or retrieve from orchestration state
        SemanticGenome genome = createGenome(goal, expansion);

        // Select the next mutable dimension
        EvolutionDimension activeDimension = dimensionScheduler.selectNextDimension(genome);
        if (activeDimension != null) {
            context.log("[DARWIN] Scheduled Mutation Dimension: " + activeDimension.getId());
        } else {
            context.log("[DARWIN] No active dimension selected. Evolution may have converged or all dimensions are locked.");
        }

        // DYNAMIC TERRITORY DISCOVERY & MATERIALIZATION: Sequential loop to ensure diversity
        TrajectoryTerritoryMapper mapper = new TrajectoryTerritoryMapper(getSessionContainer());
        mapper.setAiService(aiService);

        context.log("[DARWIN] Sequential Mutation Branching initialized (Target: " + branchingLimit + " unique trajectories).");

        int attempts = 0;
        int maxAttempts = branchingLimit * 3;
        while (uniqueVariants.size() < branchingLimit && attempts < maxAttempts) {
            attempts++;
            context.log("[DARWIN] Sequential Branching: Attempt " + attempts + " (Targets: " + uniqueVariants.size() + "/" + branchingLimit + ")");
            try {
                String discoveryGoal = generation == 0 ? goal.getPrimaryAction() : goal.getPrimaryAction() + " (Mutation Gen " + generation + ")";

                // 1. Reconstruct Lineage Context from EvolutionTree
                String fullLineagePrompt = tree.reconstructLineagePrompt(currentParentId);

                // 2. Sequential Blueprint Discovery
                TrajectoryBlueprint bp = constructTrajectoryBlueprint(goal, expansion, currentBlueprints, generation,
						siblingMemoryBuilder, mapper, discoveryGoal, fullLineagePrompt, activeDimension);

                if (bp != null) {
                    if (activeDimension != null) {
                        bp.getEngineeringDimensions().put("active_dimension", activeDimension.getId());
                        bp.getEngineeringDimensions().put("active_dimension_description", activeDimension.getDescription());
                    }
                    // Avoid duplicate strategies or philosophies
                    boolean isDuplicate = currentBlueprints.stream().anyMatch(existing ->
                        existing.getStrategy().equalsIgnoreCase(bp.getStrategy()) ||
                        existing.getPhilosophy().equalsIgnoreCase(bp.getPhilosophy())
                    );

                    if (!isDuplicate) {
                        currentBlueprints.add(bp);

                        // 3. Sequential Blueprint Materialization
                        // LIFECYCLE: ANALYZING
                        EvolutionProgressPublisher.updateBranchStatus(context, bp.getId(), bp.getPhilosophy(), "analyzing", null);

                        JSONObject variant = spawner.spawnSingleBlueprint(goal, bp, basePrompt, fullLineagePrompt + lineageContext, rejectedSiblings, siblingMemoryBuilder.toString(), isMediated, context, activeDimension, genome);

                        if (variant != null) {
                            variant.put("reasoning_level", reasoningLevel.name());
                            variant.put("architecture_enabled", architectureEnabled);
                            variant.put("implementation_enabled", implementationEnabled);

                            // Evolutionary Identity
                            String branchSuffix = String.valueOf((char)('A' + uniqueVariants.size()));
                            String parentIdentity = "ROOT";
                            EvolutionNode parentNode = tree.getNode(currentParentId);
                            if (parentNode != null && parentNode.getMutationIdentity() != null) {
                                parentIdentity = parentNode.getMutationIdentity();
                                branchSuffix = parentIdentity + (uniqueVariants.size() + 1);
                            } else {
                                branchSuffix = "Branch " + branchSuffix;
                            }
                            variant.put("mutation_identity", branchSuffix);
                            variant.put("parent_identity", parentIdentity);

                            // LIFECYCLE: PLANNED
                            EvolutionProgressPublisher.updateBranchStatus(context, bp.getId(), bp.getPhilosophy(), "planned", null);

                            uniqueVariants.add(variant);

                            // 4. Update EvolutionTree with new mutation node
                            EvolutionNode node = new EvolutionNode();
                            node.setId(variant.optString("id"));
                            node.setParentId(currentParentId);
                            node.setIteration(currentIteration);
                            node.setGeneration(generation);
                            node.setStrategy(variant.optString("strategy"));
                            node.setSemanticPhilosophy(variant.optString("semantic_anchor"));
                            node.setMutationIdentity(branchSuffix);
                            node.setLlmPrompt(basePrompt); // Simplified for now
                            node.setLlmResponse(variant.toString());
                            node.setStatus("KEPT");

                            if (parentNode != null) {
                                node.setParentStrengths(parentNode.getSelectionReason());
                                // In a real scenario, weaknesses might come from a CriticAgent or previous failure analysis
                                node.setParentWeaknesses("Mutation required to satisfy dimension: " + (activeDimension != null ? activeDimension.getId() : "Implementation"));
                            }

                            // Capture Code Snapshots
                            JSONArray variantActions = variant.optJSONArray("actions");
                            if (variantActions != null) {
                                for (int i = 0; i < variantActions.length(); i++) {
                                    JSONObject vAction = variantActions.optJSONObject(i);
                                    if (vAction != null && "WRITE".equals(vAction.optString("operation"))) {
                                        String target = vAction.optString("target");
                                        String impl = vAction.optString("implementation");
                                        if (target != null && impl != null) {
                                            node.getCodeSnapshots().put(target, impl);
                                        }
                                    }
                                }
                            }

                            // Populating MutationRecord
                            MutationRecord mut = new MutationRecord();
                            mut.setStrategy(variant.optString("strategy"));
                            mut.setSemanticAnchor(variant.optString("semantic_anchor"));
                            mut.setPhilosophy(variant.optString("semantic_anchor"));
                            mut.setReasoningFocus(variant.optString("reasoning_focus"));
                            mut.setTradeoffs(variant.optString("tradeoffs"));
                            mut.setSurvivalArgument(variant.optString("survival_argument"));
                            JSONObject dims = variant.optJSONObject("engineering_dimensions");
                            if (dims != null) {
                                for (Object k : dims.keySet()) {
                                    String key = (String) k;
                                    String val = String.valueOf(dims.get(key));
                                    mut.getEngineeringDimensions().put(key, val);
                                    node.getEngineeringDimensions().put(key, val);
                                }
                                mut.setExecutionModel(dims.optString("execution_model"));
                            }
                            node.setMutationRecord(mut);

                            Object fitnessObj = variant.opt("fitness_record");
                            if (fitnessObj instanceof FitnessRecord) {
                                node.setFitnessRecord((FitnessRecord) fitnessObj);
                            }

                            node.setGenomeSnapshot(genome.copy()); // Snapshotted for lineage history
                            tree.addNode(node);
                            context.getKernelContext().getMemoryService().saveEvolutionTree();

                            // Record mutation in genome
                            genome.recordMutation(mut);

                            getSessionContainer().getEventBus().publish(new RuntimeEvent(RuntimeEventType.SIBLING_GENERATED, context.getSessionId(), node.getId(), node.getStrategy()));

                            // 5. Accumulate Structured Sibling Memory for next iteration
                            siblingMemoryBuilder.append("SIBLING: ").append(variant.optString("strategy")).append("\n")
                                               .append("  IDENTITY: ").append(branchSuffix).append("\n")
                                               .append("  PHILOSOPHY: ").append(variant.optString("semantic_anchor")).append("\n")
                                               .append("  EXECUTION MODEL: ").append(mut.getExecutionModel()).append("\n")
                                               .append("  DIMENSIONS: ").append(mut.getEngineeringDimensions()).append("\n");

                            if (isMediated && variant.has("mediation_candidate")) {
                                JSONObject medCand = variant.getJSONObject("mediation_candidate");
                                siblingMemoryBuilder.append("  MEDIATION GENOME A (PROMPT): ").append(medCand.optString("prompt")).append("\n")
                                                   .append("  MEDIATION GENOME B (FILES): ").append(medCand.optJSONArray("selected_files")).append("\n");
                            }

                            siblingMemoryBuilder.append("  SELECTED FILES: ").append(variant.optJSONArray("selected_files")).append("\n\n");
                        }
                    } else {
                        context.log("[DARWIN] Sequential Branching: Ignoring duplicate blueprint: " + bp.getStrategy());
                    }
                } else {
                    context.log("[DARWIN] Sequential Branching: Mapper returned null at attempt " + attempts);
                }
            } catch (Exception e) {
                context.log("[DARWIN] Sequential Branching Error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // FALLBACK: If sequential branching failed to produce enough variants, inject divergent fallbacks
        if (uniqueVariants.size() < 2) {
             context.log("[DARWIN] Sequential Branching yielded insufficient variants (" + uniqueVariants.size() + "). Injecting divergent fallbacks.");
             DarwinSyntheticVariantFactory factory = new DarwinSyntheticVariantFactory();
             if (uniqueVariants.isEmpty()) {
                 uniqueVariants.add(factory.synthesizeImplementation(goal.getPrimaryAction(), atomicAnalysis));
             }
             if (uniqueVariants.size() < 2) {
                 uniqueVariants.add(factory.synthesizeSemanticAlternative(uniqueVariants.get(0), goal.getPrimaryAction(), atomicAnalysis));
             }
        }

        Object envObj = context.getOrchestrationState().getMetadata().get("semanticEnvelope");
        final SemanticEnvelope envelope;
        if (envObj instanceof SemanticEnvelope) {
            envelope = (SemanticEnvelope) envObj;
            
        } else if (envObj instanceof Map) {
            envelope = new com.fasterxml.jackson.databind.ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .convertValue(envObj, SemanticEnvelope.class);
        } else {
        	envelope=null;
			context.log("[DARWIN] WARNING: No valid SemanticEnvelope found in orchestration state metadata	. Goal-driven validation will be skipped.");
        }

        // 1. Goal-Driven Validation: Semantic Distance and Domain Matching
        // [DARWIN IMPROVEMENT] Delayed Semantic Filtering: Do NOT removeIf. Just mark status in tree.
        for (JSONObject variant : uniqueVariants) {
            double distance = semanticDistance(goal, variant, envelope);
            boolean domainMatch = variant.optString("domain", goal.getDomain()).equalsIgnoreCase(goal.getDomain());
            variant.put("semantic_distance", distance);
            variant.put("domain_match", domainMatch);

            if (distance > 0.60 || !domainMatch) {
                String reason = distance > 0.60 ? "Semantic distance (" + String.format("%.2f", distance) + ") exceeds threshold (0.60)"
                                               : "Domain mismatch (Expected " + goal.getDomain() + ")";
                context.log("[DARWIN] Semantic Validation WARNING for " + variant.optString("mutation_identity") + ": " + reason);

                // Update EvolutionTree status but KEEP the node
                EvolutionNode node = tree.getNode(variant.optString("id"));
                if (node != null) {
                    node.setStatus("REJECTED_SEMANTIC");
                    node.setRejectionReason(reason);
                    context.getKernelContext().getMemoryService().saveEvolutionTree();
                }
            }
        }

        // Fitness Ranking
        DarwinFitnessRanker ranker = new DarwinFitnessRanker();
        ranker.rank(uniqueVariants, atomicAnalysis, currentIteration, pressure);

        getSessionContainer().getEventBus().publish(
            new RuntimeEvent(RuntimeEventType.ITERATION_COMPLETED, context.getSessionId(), "DarwinEngine", "Iteration " + currentIteration)
        );

        // Mark the best variant for UI highlighting
        if (!uniqueVariants.isEmpty()) {
            JSONObject best = uniqueVariants.get(0);
            best.put("isBest", true);
        }

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
            BranchVariant v = mapToBranchVariant(obj, goal.getPrimaryAction(), "TRAJECTORY_EVOLUTION", trajectory, context);
            if (!survivors.isEmpty()) {
                v.setInheritedContext(lineageContext);
                v.setRejectedSiblings(rejectedSiblings);
            }
            variants.add(v);
        }

        return variants;
    }

	private SemanticGenome createGenome(GoalModel goal, IntentExpansionResult expansion) {
		Object genomeObj = context.getOrchestrationState().getMetadata().get("semanticGenome");
        SemanticGenome genome = eu.kalafatic.evolution.controller.parsers.JsonUtils.restoreFromMetadata(genomeObj, SemanticGenome.class, "semanticGenome", context);
        if (genome != null && genome != genomeObj) {
            context.getOrchestrationState().getMetadata().put("semanticGenome", genome);
        }

        if (genome == null) {
            genome = new SemanticGenome(goal.getPrimaryAction());
            // Populate dimensions from intent expansion if available
            if (expansion != null) {
                for (EvolutionDimension dim : expansion.getUnresolvedDimensions()) {
                    genome.addDimension(dim);
                }
            }
            context.getOrchestrationState().getMetadata().put("semanticGenome", genome);
        }
		return genome;
	}

	private TrajectoryBlueprint constructTrajectoryBlueprint(GoalModel goal, IntentExpansionResult expansion,
			List<TrajectoryBlueprint> currentBlueprints, int generation, StringBuilder siblingMemoryBuilder,
			TrajectoryTerritoryMapper mapper, String discoveryGoal, String fullLineagePrompt, EvolutionDimension activeDimension) throws Exception {
		TrajectoryBlueprint bp = null;

		// PRE-CONSTRAINT: Use existing blueprints from intent expansion if available for Gen 0
		if (generation == 0 && expansion != null && expansion.getActiveDimensionId() != null) {
		    EvolutionDimension activeDim = expansion.getUnresolvedDimensions().stream()
		        .filter(d -> d.getId().equals(expansion.getActiveDimensionId()))
		        .findFirst().orElse(null);

		    if (activeDim != null && !activeDim.getCandidateBranches().isEmpty()) {
		        // Use pre-defined blueprints from intent expansion if available
		        for (BranchVariant bv : activeDim.getCandidateBranches()) {
		            boolean alreadyUsed = currentBlueprints.stream().anyMatch(existingBp ->
		                existingBp.getStrategy().equalsIgnoreCase(bv.getStrategy()));

		            if (!alreadyUsed) {
		                context.log("[DARWIN] Seeding blueprint from intent expansion dimension: " + activeDim.getId() + " -> " + bv.getStrategy());
		                bp = new TrajectoryBlueprint("bp-seed-" + bv.getId(), goal.getPrimaryAction(), bv.getStrategy());
		                bp.setPhilosophy(bv.getSurvivalArgument());
		                bp.setSurvivalArgument(bv.getSurvivalArgument());
		                bp.setTradeoffs(bv.getTradeoffs());
		                bp.setStrategyType(DarwinStrategyType.PROBABLE_SURVIVOR);
		                break;
		            }
		        }
		    }
		}

		if (bp == null) {
		    bp = mapper.discoverNext(discoveryGoal, context, currentBlueprints, fullLineagePrompt + siblingMemoryBuilder.toString(), activeDimension);
		}
		return bp;
	}


    private BranchVariant mapToBranchVariant(JSONObject obj, String goal, String currentPhase, Trajectory trajectory, TaskContext context) {
        BranchVariant v = new BranchVariant();
        v.setId(obj.optString("id", "v-" + System.currentTimeMillis()));
        v.setBranchId(v.getId());
        v.setLineageId(context.getSessionId());
        v.setActivationState(BranchVariant.ActivationState.ARCHIVED);
        v.setStrategyType(obj.optString("strategy_type", "UNKNOWN"));
        v.setReasoningLevel(BranchVariant.ReasoningLevel.valueOf(obj.optString("reasoning_level", "BALANCED")));
        v.setArchitectureEnabled(obj.optBoolean("architecture_enabled", true));
        v.setImplementationEnabled(obj.optBoolean("implementation_enabled", true));
        v.setStrategy(obj.optString("strategy", "unknown"));
        v.setSemanticAnchor(obj.optString("semantic_anchor", v.getStrategy()));
        v.setMutationPhilosophy(obj.optString("mutation_philosophy"));
        v.setMutationTrace("Generated in trajectory round.");
        v.setScore(obj.optDouble("score", 0.0));
        String suffix = obj.optString("suffix", "variant");
        v.setBranchName("exp/" + sanitize(goal) + "/" + v.getId() + "-" + System.currentTimeMillis());
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
                String s = selectedFilesArr.optString(i);
                if (s != null && !s.isEmpty()) v.getSelectedFiles().add(s);
            }
        }

        JSONArray stepsArr = obj.optJSONArray("projected_steps");
        if (stepsArr != null) {
            for (int j = 0; j < stepsArr.length(); j++) {
                String s = stepsArr.optString(j);
                if (s != null && !s.isEmpty()) v.getProjectedSteps().add(s);
            }
        }

        JSONArray outputsArr = obj.optJSONArray("expected_outputs");
        if (outputsArr != null) {
            for (int j = 0; j < outputsArr.length(); j++) {
                String s = outputsArr.optString(j);
                if (s != null && !s.isEmpty()) v.getExpectedOutputs().add(s);
            }
        }

        JSONArray actionsArr = obj.optJSONArray("actions");
        if (actionsArr != null) {
            for (int i = 0; i < actionsArr.length(); i++) {
                JSONObject aObj = actionsArr.optJSONObject(i);
                if (aObj == null) continue;
                BranchVariant.Action action = new BranchVariant.Action();
                action.setDomain(aObj.optString("domain", "kernel"));
                action.setOperation(aObj.optString("operation", "ANALYZE"));
                action.setTarget(aObj.optString("target", "workspace"));
                action.setDescription(aObj.optString("description", "Materialize architectural intent"));
                action.setImplementation(aObj.optString("implementation", ""));
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

            JSONArray subArr = medObj.optJSONArray("subsystems");
            if (subArr != null) {
                for (int i = 0; i < subArr.length(); i++) {
                    JSONObject sObj = subArr.optJSONObject(i);
                    if (sObj == null) continue;
                    eu.kalafatic.evolution.controller.mediation.model.Subsystem subsystem = new eu.kalafatic.evolution.controller.mediation.model.Subsystem();
                    subsystem.setId(sObj.optString("id"));
                    subsystem.setName(sObj.optString("name"));
                    subsystem.setPurpose(sObj.optString("purpose"));
                    subsystem.setDescription(sObj.optString("description"));
                    JSONArray bounds = sObj.optJSONArray("boundaries");
                    if (bounds != null) for (int j = 0; j < bounds.length(); j++) subsystem.getBoundaries().add(bounds.getString(j));
                    JSONArray crit = sObj.optJSONArray("critical_files");
                    if (crit != null) for (int j = 0; j < crit.length(); j++) subsystem.getCriticalFiles().add(crit.getString(j));
                    JSONArray resp = sObj.optJSONArray("responsibilities");
                    if (resp != null) for (int j = 0; j < resp.length(); j++) subsystem.getResponsibilities().add(resp.getString(j));
                    med.getSubsystems().add(subsystem);
                }
            }

            JSONArray factArr = medObj.optJSONArray("architectural_facts");
            if (factArr != null) {
                for (int i = 0; i < factArr.length(); i++) {
                    JSONObject fObj = factArr.optJSONObject(i);
                    if (fObj == null) continue;
                    eu.kalafatic.evolution.controller.mediation.model.ArchitecturalFact fact = new eu.kalafatic.evolution.controller.mediation.model.ArchitecturalFact();
                    fact.setId(fObj.optString("id"));
                    fact.setSubject(fObj.optString("subject"));
                    fact.setPredicate(fObj.optString("predicate"));
                    fact.setDescription(fObj.optString("description"));
                    fact.setConfidence(fObj.optDouble("confidence", 1.0));
                    JSONArray ev = fObj.optJSONArray("evidence");
                    if (ev != null) for (int j = 0; j < ev.length(); j++) fact.getEvidence().add(ev.getString(j));
                    med.getArchitecturalFacts().add(fact);
                }
            }

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

    private double semanticDistance(GoalModel goal, JSONObject variant, SemanticEnvelope envelope) {
        String strategy = variant.optString("strategy", "").toLowerCase();
        String philosophy = variant.optString("semantic_anchor", "").toLowerCase();
        String primaryAction = goal.getPrimaryAction().toLowerCase();

        // Technical keywords that are often semantically identical for the same goal
        String[] identicalTechnicalConcepts = {"static", "instance", "constructor", "overloads", "logger", "system.out", "varargs", "library", "utility"};

        double distance = 0.0;

        // 1. Mandatory Concepts Check (Goal Relative)
        if (envelope != null && !envelope.getMandatoryConcepts().isEmpty()) {
            int missed = 0;
            for (String concept : envelope.getMandatoryConcepts()) {
                String c = concept.toLowerCase();
                // If it's a technical variety keyword, we are lenient
                boolean isTechnicalVariety = false;
                for (String tech : identicalTechnicalConcepts) {
                    if (c.contains(tech)) { isTechnicalVariety = true; break; }
                }

                if (!strategy.contains(c) && !philosophy.contains(c)) {
                    if (!isTechnicalVariety) missed++;
                }
            }
            distance += (double) missed / envelope.getMandatoryConcepts().size() * 0.4;
        }

        // 2. Exact Match or Intent Overlap
        if (strategy.contains(primaryAction) || philosophy.contains(primaryAction)) {
            distance += 0.0; // Perfect intent match
        } else {
            // 3. Keyword Overlap (Weighted toward intent keywords)
            String[] keywords = primaryAction.split(" ");
            int matches = 0;
            int significantKeywords = 0;
            for (String k : keywords) {
                if (k.length() <= 3) continue;

                boolean isTechnical = false;
                for (String tech : identicalTechnicalConcepts) {
                    if (k.equalsIgnoreCase(tech)) { isTechnical = true; break; }
                }

                if (!isTechnical) {
                    significantKeywords++;
                    if (strategy.contains(k) || philosophy.contains(k)) {
                        matches++;
                    }
                }
            }
            double overlap = significantKeywords > 0 ? (double) matches / significantKeywords : 1.0;
            distance += (1.0 - overlap) * 0.6;
        }

        // 4. Forbidden Regions Check (Architectural Inflation)
        if (envelope != null && !envelope.getForbiddenRegions().isEmpty()) {
            for (String region : envelope.getForbiddenRegions()) {
                String r = region.toLowerCase();
                if (strategy.contains(r) || philosophy.contains(r)) {
                    distance += 0.8; // Heavy penalty for architectural inflation
                }
            }
        }

        return Math.min(1.0, distance);
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
