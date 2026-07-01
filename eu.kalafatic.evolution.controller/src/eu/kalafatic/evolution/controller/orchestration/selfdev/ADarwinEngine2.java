//package eu.kalafatic.evolution.controller.orchestration.selfdev;
//
//import java.io.File;
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//
//import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
//import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
//import eu.kalafatic.evolution.controller.kernel.EvolutionProfile;
//import eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator;
//import eu.kalafatic.evolution.controller.mediation.analysis.SemanticExtractor;
//import eu.kalafatic.evolution.controller.mediation.model.Hotspot;
//import eu.kalafatic.evolution.controller.mediation.model.MediationResult;
//import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
//import eu.kalafatic.evolution.controller.mediation.scanner.TargetScanner;
//import eu.kalafatic.evolution.controller.orchestration.ConversationState;
//import eu.kalafatic.evolution.controller.orchestration.EvolutionPhase;
//import eu.kalafatic.evolution.controller.orchestration.EvolutionPhaseMachine;
//import eu.kalafatic.evolution.controller.orchestration.EvolutionProgressPublisher;
//import eu.kalafatic.evolution.controller.orchestration.EvolutionStage;
//import eu.kalafatic.evolution.controller.orchestration.FileChangeTracker;
//import eu.kalafatic.evolution.controller.orchestration.FinalResponse;
//import eu.kalafatic.evolution.controller.orchestration.FinalResponseAssembler;
//import eu.kalafatic.evolution.controller.orchestration.IterationManager;
//import eu.kalafatic.evolution.controller.orchestration.KernelFactory;
//import eu.kalafatic.evolution.controller.orchestration.ModeRouter;
//import eu.kalafatic.evolution.controller.orchestration.OrchestrationState;
//import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
//import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
//import eu.kalafatic.evolution.controller.orchestration.ResultType;
//import eu.kalafatic.evolution.controller.orchestration.SessionManager;
//import eu.kalafatic.evolution.controller.orchestration.SystemState;
//import eu.kalafatic.evolution.controller.orchestration.TaskContext;
//import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
//import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
//import eu.kalafatic.evolution.controller.orchestration.behavior.PolicyResolver;
//import eu.kalafatic.evolution.controller.orchestration.behavior.PromptComposer;
//import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityContext;
//import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException;
//import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityHealth;
//import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityStatus;
//import eu.kalafatic.evolution.controller.orchestration.capability.ICapability;
//import eu.kalafatic.evolution.controller.orchestration.capability.contracts.IMutationContract;
//import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;
//import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;
//import eu.kalafatic.evolution.controller.orchestration.engines.DimensionEngine;
//import eu.kalafatic.evolution.controller.orchestration.engines.ExecutionEngine;
//import eu.kalafatic.evolution.controller.orchestration.engines.FitnessEngine;
//import eu.kalafatic.evolution.controller.orchestration.engines.LineageEngine;
//import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
//import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
//import eu.kalafatic.evolution.controller.orchestration.goal.SemanticEnvelope;
//import eu.kalafatic.evolution.controller.orchestration.mediation.MediationEngine;
//import eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive.DiversityPressureController;
//import eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive.EvolutionaryPenaltyModel;
//import eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive.RejectionPatternAnalyzer;
//import eu.kalafatic.evolution.controller.orchestration.util.ModeRecognizer;
//import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceArtifact;
//import eu.kalafatic.evolution.controller.trajectory.Trajectory;
//import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
//import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
//import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
//import eu.kalafatic.evolution.model.orchestration.Iteration;
//import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
//import eu.kalafatic.evolution.model.orchestration.PromptInstructions;
//import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
//
///**
// * Abstract base class for Darwin engines.
// * Extracted from DarwinEngine to support polymorphic behavior.
// * 
// * Subclasses should override mode-specific behavior while reusing
// * the common evolutionary infrastructure.
// */
//public abstract class ADarwinEngine2 extends BaseAiAgent implements ICapability, IMutationContract {
//
//    // ============================================================
//    // PROTECTED FIELDS (copied from DarwinEngine)
//    // ============================================================
//    
//    protected final TaskContext context;
//    protected final IterationMemoryService memoryService;
//    protected final SystemStateSignalProvider stateProvider;
//    protected final RejectionPatternAnalyzer rejectionAnalyzer;
//    protected final EvolutionaryPenaltyModel penaltyModel = new EvolutionaryPenaltyModel();
//    protected final DiversityPressureController diversityController = new DiversityPressureController();
//    protected final eu.kalafatic.evolution.controller.kernel.EvolutionaryPressureEngine pressureEngine;
//    
//    protected MediationEngine mediationEngine;
//    protected final PolicyResolver policyResolver = new PolicyResolver();
//    protected final PromptComposer promptComposer = new PromptComposer();
//    protected final DimensionEngine dimensionEngine = new DimensionEngine();
//    protected final LineageEngine lineageEngine = new LineageEngine();
//    protected final FitnessEngine fitnessEngine = new FitnessEngine();
//    protected final ExecutionEngine executionEngine = new ExecutionEngine();
//    protected final eu.kalafatic.evolution.controller.orchestration.engines.SelectionEngine selectionEngine = 
//        new eu.kalafatic.evolution.controller.orchestration.engines.SelectionEngine();
//    
//    protected CapabilityStatus status = CapabilityStatus.STOPPED;
//    protected final PromptIntentAnalyzer intentAnalyzer;
//    protected ModeRecognizer modeRecognizer;
//
//    // ============================================================
//    // CONSTRUCTOR
//    // ============================================================
//    
//    public ADarwinEngine2(TaskContext context, IterationMemoryService memoryService,
//                         SystemStateSignalProvider stateProvider) {
//        super("DarwinEngine", "DarwinEngine", SessionManager.getInstance().getSession(context.getSessionId()));
//        this.context = context;
//        this.memoryService = memoryService;
//        this.stateProvider = stateProvider;
//        this.pressureEngine = getSessionContainer().getPressureEngine();
//        this.rejectionAnalyzer = new RejectionPatternAnalyzer(getSessionContainer());
//        this.mediationEngine = new MediationEngine();
//        this.intentAnalyzer = new PromptIntentAnalyzer(getSessionContainer(), context.getProjectRoot());
//        this.intentAnalyzer.setAiService(aiService);
//        this.modeRecognizer = new ModeRecognizer(getSessionContainer());
//    }
//
//    // ============================================================
//    // ABSTRACT METHODS - To be implemented by subclasses
//    // ============================================================
//    
//    /**
//     * Gets the mode identifier for this engine.
//     */
//    public abstract String getMode();
//    
//    /**
//     * Gets the capability type for this engine.
//     */
//    public abstract CapabilityType getCapabilityType();
//    
//    /**
//     * Handles intent-based routing. Subclasses override for specific behavior.
//     * Returns null to continue with normal flow, or a response to return early.
//     */
//    protected abstract OrchestratorResponse handleRouting(PromptIntentAnalyzer.IntentResult intent, 
//                                                           String request, 
//                                                           TaskRequest taskRequest,
//                                                           IterationManager iterationManager) throws Exception;
//
//    // ============================================================
//    // COMMON METHODS - Copied from DarwinEngine
//    // ============================================================
//    
//    /**
//     * Main orchestration method - copied from DarwinEngine.
//     */
//    public OrchestratorResponse orchestrateEvolution(TaskRequest taskRequest, IterationManager iterationManager)
//            throws Exception {
//        context.setStartTime(Instant.now());
//        String request = taskRequest.getPrompt();
//        OrchestrationState state = context.getOrchestrationState();
//        
//        // ============================================================
//        // 1. LLM-POWERED INTENT ANALYSIS
//        // ============================================================
//        PromptIntentAnalyzer.IntentResult intent = intentAnalyzer.analyze(request, context);
//        context.log("[DARWIN] Intent Analysis: " + intent.toString());
//        
//        // ============================================================
//        // 2. ROUTE BASED ON INTENT - Delegate to subclass
//        // ============================================================
//        OrchestratorResponse routedResponse = handleRouting(intent, request, taskRequest, iterationManager);
//        if (routedResponse != null) {
//            return routedResponse;
//        }
//
//        Map<String, Object> contextMap = taskRequest.getContext();
//        if (contextMap != null) {
//            state.getMetadata().putAll(contextMap);
//        }
//
//        if (context.getStateHolder().getState() == SystemState.EXECUTING
//                && !context.getOrchestrator().getTasks().isEmpty()) {
//            context.log(
//                    "[DARWIN] Pre-populated tasks detected in EXECUTING state. Bypassing orchestration for direct execution.");
//            boolean success = iterationManager.executeTasksWithRetries(context.getOrchestrator().getTasks());
//            OrchestratorResponse bypassResponse = new OrchestratorResponse();
//            bypassResponse.setResultType(ResultType.CHAT);
//            bypassResponse.setSummary(success ? "Execution completed successfully." : "Execution failed.");
//            iterationManager.transition(success ? SystemState.DONE : SystemState.FAILED, context);
//            return bypassResponse;
//        }
//
//        String prompt = request.trim();
//        boolean isControl = prompt.equalsIgnoreCase("yes") || prompt.equalsIgnoreCase("no")
//                || prompt.toLowerCase().startsWith("select ") || prompt.toLowerCase().startsWith("approve variant ")
//                || prompt.toLowerCase().startsWith("reject variant ")
//                || prompt.toLowerCase().startsWith("keep variant ") || prompt.equalsIgnoreCase("force solution")
//                || prompt.equalsIgnoreCase("approved") || prompt.equalsIgnoreCase("rejected")
//                || prompt.equalsIgnoreCase("proceed") || prompt.equalsIgnoreCase("ok")
//                || prompt.equalsIgnoreCase("okay")
//                || prompt.matches("^(yes|y|ok|okay|approve|proceed|go ahead|yep|sure)$");
//
//        String checkpointGoal = (String) state.getMetadata().get("checkpoint_goal");
//        if (isControl) {
//            state.getMetadata().put("pendingControlCommand", prompt);
//        }
//
//        if (!isControl) {
//            iterationManager.transition(SystemState.INIT, context);
//            boolean isNewGoal = (checkpointGoal != null && !checkpointGoal.equalsIgnoreCase(request));
//            boolean isStaleTerminal = state.getCurrentPhase() != null && (state.getCurrentPhase().contains("TERMINAL")
//                    || state.getCurrentPhase().contains("SUCCESS") || state.getCurrentPhase().contains("SATISFIED"));
//
//            if (isNewGoal || isStaleTerminal) {
//                context.log("[DARWIN] Resetting kernel for new request. Current phase: " + state.getCurrentPhase());
//                state.setCurrentPhase(null);
//                state.setIterationCount(0);
//                context.getKernelContext().getMemoryService().getRecords().clear();
//                context.getOrchestrator().getTasks().clear();
//                state.getMetadata().remove("intentExpansion");
//                state.getMetadata().remove("engineeringDimensions");
//                state.getMetadata().remove("goalModel");
//                state.getMetadata().remove("semanticEnvelope");
//                state.getMetadata().remove("lastDecisionSnapshot");
//                state.getMetadata().remove("isChatRequest");
//                state.setLockedAbstractionLevel(null);
//            }
//            state.setRawInput(request);
//            state.getMetadata().put("checkpoint_goal", request);
//        } else if (state.getRawInput() == null || state.getRawInput().isEmpty()) {
//            state.setRawInput(request);
//            state.getMetadata().put("checkpoint_goal", request);
//        }
//
//        OrchestratorResponse response = new OrchestratorResponse();
//        response.setResultType(ResultType.CHAT);
//
//        eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorProfile profile = context.getBehaviorProfile();
//        ModeRouter router = new ModeRouter();
//
//        try {
//            context.getOrchestrator().getTasks().clear();
//            context.setCurrentTaskName("Initialization");
//            context.log("[DARWIN] Strategic Initialization: " + request);
//
//            ConversationState convState = ConversationState.load(context.getSharedMemory(), context.getSessionId());
//            convState.addMessage("User: " + request);
//
//            // Restore cognitive state from history if present
//            if (convState.getCognitiveState() != null) {
//                context.log("[DARWIN] Restoring cognitive state from conversation history.");
//                getSessionContainer().getCognitiveState()
//                        .setCurrentCapability(convState.getCognitiveState().getCurrentCapability());
//                getSessionContainer().getCognitiveState()
//                        .setCurrentIntent(convState.getCognitiveState().getCurrentIntent());
//                getSessionContainer().getCognitiveState()
//                        .setCurrentDirection(convState.getCognitiveState().getCurrentDirection());
//                getSessionContainer().getCognitiveState().setConfidence(convState.getCognitiveState().getConfidence());
//                getSessionContainer().getCognitiveState()
//                        .setCognitiveDepth(convState.getCognitiveState().getCognitiveDepth());
//                getSessionContainer().getCognitiveState().setVelocity(convState.getCognitiveState().getVelocity());
//                getSessionContainer().getCognitiveState()
//                        .setAcceleration(convState.getCognitiveState().getAcceleration());
//                getSessionContainer().getCognitiveState()
//                        .setDominantTrend(convState.getCognitiveState().getDominantTrend());
//                getSessionContainer().getCognitiveState()
//                        .setTrendStability(convState.getCognitiveState().getTrendStability());
//                getSessionContainer().getCognitiveState()
//                        .setTrajectory(new ArrayList<>(convState.getCognitiveState().getTrajectory()));
//                getSessionContainer().getCognitiveState().getCapabilityScores()
//                        .putAll(convState.getCognitiveState().getCapabilityScores());
//                getSessionContainer().getCognitiveState().getCapabilityHistory()
//                        .addAll(convState.getCognitiveState().getCapabilityHistory());
//            }
//
//            // 1. DISCOVERY phase
//            if (!profile.hasTrait(BehaviorTrait.REASONING_ATOMIC)) {
//                if (iterationManager.getGitManager().isGitRepository()) {
//                    iterationManager.transition(SystemState.ANALYZING, context);
//                    context.log("[DARWIN] Discovery: Inspecting repository structure.");
//                    String projectStructure = iterationManager.getStructureAgent().process(
//                            "Provide a concise summary of the project structure and technology stack.", context, null);
//                    state.getMetadata().put("projectStructure", projectStructure);
//
//                    WorkspaceArtifact archArtifact = new WorkspaceArtifact("arch-summary-" + System.currentTimeMillis(),
//                            "architecture-summary");
//                    archArtifact.setContent(projectStructure);
//                    archArtifact.getSemanticTags().add("architecture");
//                    archArtifact.getSemanticTags().add("structure");
//                    context.getSemanticWorkspace().addArtifact(archArtifact);
//
//                    context.log("[DARWIN] Discovery: Building semantic repository snapshot.");
//                    TargetScanner scanner = new TargetScanner();
//                    TargetSnapshot.TargetType type = context.getProjectRoot().getAbsolutePath().contains("evolution")
//                            ? TargetSnapshot.TargetType.SELF
//                            : TargetSnapshot.TargetType.PROJECT;
//                    TargetSnapshot snapshot = scanner.scanToSnapshot(context.getProjectRoot(), type);
//
//                    ContextCurator curator = new ContextCurator();
//                    List<String> candidates = curator.selectContext(snapshot, request, 32);
//
//                    context.log("[DARWIN] Discovery: Selective deep analysis of " + candidates.size()
//                            + " high-signal candidates.");
//                    SemanticExtractor extractor = new SemanticExtractor();
//                    extractor.extractToSnapshot(snapshot, candidates);
//
//                    state.getMetadata().put("mediatedSnapshot", snapshot);
//
//                    context.log("[DARWIN] Discovery: Formalizing Target Reality Model.");
//                    eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel realityModel = iterationManager
//                            .getRealityDiscoveryAgent()
//                            .discover(request, context, context.getProjectRoot().getAbsolutePath());
//                    state.getMetadata().put("targetRealityModel", realityModel);
//
//                    if (profile.hasTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY)) {
//                        context.log("[DARWIN] Mediated Mode: Triggering MetadataAgent repository cognition.");
//                        eu.kalafatic.evolution.controller.agents.MetadataAgent metadataAgent = new eu.kalafatic.evolution.controller.agents.MetadataAgent();
//                        metadataAgent.generate(context.getProjectRoot());
//                    }
//
//                    context.getOrchestrationState()
//                            .addDiagnostic("[DarwinTrace] Discovery complete. Target Reality Model initialized.");
//                }
//            }
//
//            // 2. ANALYZING stage
//            iterationManager.transition(SystemState.ANALYZING, context);
//            if (iterationManager.getGitManager().isGitRepository() || context.getMetadata().containsKey("testMode")) {
//                iterationManager.getGitManager().ensureInitialCommit();
//
//                PromptInstructions instructions = (context.getOrchestrator() != null
//                        && context.getOrchestrator().getAiChat() != null)
//                                ? context.getOrchestrator().getAiChat().getPromptInstructions()
//                                : null;
//
//                if (instructions != null && instructions.isGitAutomation()) {
//                    String requestedBranch = (String) state.getMetadata().get("branch");
//                    String branchName = (requestedBranch != null && !requestedBranch.isEmpty()) ? requestedBranch
//                            : "evo-" + context.getSessionId().substring(0,
//                                    Math.min(context.getSessionId().length(), 8));
//
//                    context.log("[DARWIN] Git Automation enabled. Creating/Switching to branch: " + branchName);
//                    try {
//                        if (!iterationManager.getGitManager().getCurrentBranch().equals(branchName)) {
//                            iterationManager.getGitManager().createBranch(branchName);
//                        }
//                    } catch (Exception e) {
//                        context.log(
//                                "[DARWIN] Git Warning: Could not manage branch " + branchName + ": " + e.getMessage());
//                    }
//                }
//            }
//
//            if (context.getPlatformMode() == null) {
//                PlatformMode mode = router.route(request, context.getOrchestrator());
//                context.setPlatformMode(mode);
//                context.log("Platform Mode: " + mode.getType());
//
//                getSessionContainer().getEventBus()
//                        .publish(new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
//                                eu.kalafatic.evolution.controller.workflow.RuntimeEventType.MODE_CHANGED,
//                                context.getSessionId(), "DarwinEngine", mode.getType().toString()));
//            }
//
//            // GROUNDING: Establish Goal Model and Locked Abstraction Level
//            GoalModel goalModel = GoalModel.extract(state.getMetadata(), iterationManager, request, context);
//            
//            if (state.getLockedAbstractionLevel() == null) {
//                AbstractionLevel lockedLevel = AbstractionLevel.DESIGN;
//                String complexity = goalModel.getComplexity() != null ? goalModel.getComplexity().toUpperCase() : "MEDIUM";
//                String type = goalModel.getGoalType() != null ? goalModel.getGoalType().toUpperCase() : "GENERAL";
//
//                if ("ANALYSIS".equalsIgnoreCase(type) || "ANALYSIS".equalsIgnoreCase(goalModel.getIntent())) {
//                    lockedLevel = AbstractionLevel.ARCHITECTURE;
//                } else if ("SIMPLE".equals(complexity)) {
//                    lockedLevel = AbstractionLevel.IMPLEMENTATION;
//                } else if ("HIGH".equals(complexity)) {
//                    lockedLevel = AbstractionLevel.ARCHITECTURE;
//                }
//
//                state.setLockedAbstractionLevel(lockedLevel);
//                context.log("[DARWIN] Abstraction level LOCKED to: " + lockedLevel + " based on complexity: " + complexity + ", type: " + type);
//            }
//
//            // ADAPTIVE KERNEL: Ensure execution profile is initialized
//            if (context.getExecutionProfile() == null) {
//                eu.kalafatic.evolution.controller.kernel.EvolutionProfile profile_init = eu.kalafatic.evolution.controller.kernel.EvolutionIntensityCalculator
//                        .calculate(context, iterationManager.getActiveTrajectory(context), null);
//                context.getOrchestrationState().setExecutionProfile(profile_init);
//            }
//
//            // ADAPTIVE KERNEL: Intensity-based analysis gating
//            int intensity = context.getExecutionProfile() != null ? context.getExecutionProfile().getIntensity() : 2;
//            eu.kalafatic.evolution.controller.orchestration.intent.EvolutionAssessment initialAssessment = null;
//
//            if (intensity > 1) {
//                context.log("[DARWIN] Inspecting goal for unresolved semantic uncertainty.");
//                initialAssessment = iterationManager.getDimensionInferenceEngine().analyze(request, context);
//                dimensionEngine.detectUnresolvedDimensions(initialAssessment, context);
//            } else {
//                context.log("[DARWIN] Low Intensity detected. Bypassing deep semantic analysis.");
//            }
//
//            context.log("[DARWIN] Starting Unified Iterative Evolutionary Loop.");
//            OrchestratorResponse result = evolve(request, iterationManager, initialAssessment);
//
//            boolean isError = result != null && result.getResultType() == ResultType.ERROR;
//
//            if (isError) {
//                iterationManager.transition(SystemState.FAILED, context);
//            } else {
//                if (!context.getStateHolder().getState().equals(SystemState.DONE)) {
//                    iterationManager.transition(SystemState.DONE, context);
//                }
//            }
//
//            if (!isError) {
//                PromptInstructions instructions = (context.getOrchestrator() != null
//                        && context.getOrchestrator().getAiChat() != null)
//                                ? context.getOrchestrator().getAiChat().getPromptInstructions()
//                                : null;
//
//                if (instructions != null && instructions.isGitAutomation()
//                        && iterationManager.getGitManager().isGitRepository()) {
//                    context.log("[DARWIN] Git Automation: Committing changes.");
//                    try {
//                        iterationManager.getGitManager().commit(
//                                "Evolution Task: " + request.substring(0, Math.min(request.length(), 50)), context);
//                    } catch (Exception e) {
//                        context.log("[DARWIN] Git Warning: Could not commit changes: " + e.getMessage());
//                    }
//                }
//            }
//
//            FinalResponseAssembler assembler = new FinalResponseAssembler();
//            FinalResponse finalResponse = assembler.assemble(context, result.getSummary(), !isError,
//                    context.getStartTime());
//            result.setFinalResponse(finalResponse);
//
//            return result;
//
//        } catch (Exception e) {
//            context.log("[DARWIN] [CRITICAL] Orchestration failed: " + e.getMessage());
//            if (System.getProperty("evolution.test.debug") != null || context.getMetadata().containsKey("testMode")) {
//                e.printStackTrace();
//            }
//            state.addDiagnostic("Critical error: " + e.getMessage());
//            iterationManager.transition(SystemState.FAILED, context);
//
//            FinalResponseAssembler assembler = new FinalResponseAssembler();
//            FinalResponse finalResponse = assembler.assemble(context, "Error: " + e.getMessage(), false,
//                    context.getStartTime());
//            OrchestratorResponse errorResponse = new OrchestratorResponse();
//            errorResponse.setResultType(ResultType.ERROR);
//            errorResponse.setFinalResponse(finalResponse);
//
//            if (context.getMetadata().containsKey("testMode")) {
//                throw e;
//            }
//
//            return errorResponse;
//        }
//    }
//
//    /**
//     * Evolution loop - copied from DarwinEngine.
//     */
//    public OrchestratorResponse evolve(String request, IterationManager iterationManager,
//            eu.kalafatic.evolution.controller.orchestration.intent.EvolutionAssessment initialAssessment)
//            throws Exception {
//        if (context.getExecutionProfile() == null) {
//            eu.kalafatic.evolution.controller.kernel.EvolutionProfile profile_init = eu.kalafatic.evolution.controller.kernel.EvolutionIntensityCalculator
//                    .calculate(context, iterationManager.getActiveTrajectory(context), null);
//            context.getOrchestrationState().setExecutionProfile(profile_init);
//        }
//
//        context.log("[DARWIN] Starting Recursive Evolutionary Cognition Loop.");
//
//        getSessionContainer().getEventBus().publish(
//                new RuntimeEvent(RuntimeEventType.FLOW_STARTED, context.getSessionId(), "DarwinEngine", request));
//
//        OrchestrationState state = context.getOrchestrationState();
//
//        if (initialAssessment != null && initialAssessment.hasUnresolvedDimensions()) {
//            context.log("[DARWIN] Grounding evolution with initial assessment.");
//        }
//
//        state.getCognitiveTrace()
//                .addNode(new CausalNode("evolution-start-" + System.currentTimeMillis(), "EVOLUTION_INIT",
//                        "DarwinEngine", List.of(), List.of("DarwinFlow"), 1.0,
//                        "Recursive evolutionary cognition kernel active."));
//
//        EvaluationResult result = null;
//        int safetyCounter = 0;
//
//        int expansionValue = 5;
//        if (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null) {
//            String sessionId = context.getSessionId();
//            eu.kalafatic.evolution.model.orchestration.ChatSession chatSession = context.getOrchestrator().getAiChat()
//                    .getSessions().stream().filter(s -> s.getId().equals(sessionId)).findFirst().orElse(null);
//            if (chatSession != null) {
//                expansionValue = chatSession.getExpansion();
//            }
//        }
//
//        int intensity_val = context.getExecutionProfile() != null ? context.getExecutionProfile().getIntensity() : 2;
//
//        int minIterations = 1;
//        PromptInstructions instructions = (context.getOrchestrator() != null
//                && context.getOrchestrator().getAiChat() != null)
//                        ? context.getOrchestrator().getAiChat().getPromptInstructions()
//                        : null;
//        if (instructions != null) {
//            minIterations = instructions.getPreferredMaxIterations();
//        }
//
//        if (minIterations < 2 && intensity_val >= 1 && (context.getExecutionProfile() == null || context
//                .getExecutionProfile()
//                .getCapability() != eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType.CHAT)) {
//            minIterations = 2;
//        }
//
//        if (context.getExecutionProfile() != null && context.getExecutionProfile()
//                .getCapability() == eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType.CHAT) {
//            if (minIterations > 1)
//                minIterations = 1;
//        }
//
//        if (minIterations < 1)
//            minIterations = 1;
//
//        int maxIterationsLimit = 20;
//        if (intensity_val == 1)
//            maxIterationsLimit = 10;
//        else if (intensity_val == 2)
//            maxIterationsLimit = 15;
//        else if (expansionValue <= 3)
//            maxIterationsLimit = 25;
//        else if (expansionValue >= 8)
//            maxIterationsLimit = 100;
//
//        maxIterationsLimit = Math.max(maxIterationsLimit, minIterations);
//
//        context.log("[DARWIN] Dynamic Expansion Control: Min Iterations = " + minIterations
//                + ", Target Max Iterations = " + maxIterationsLimit);
//
//        context.log("[DARWIN] Phase: Recursive Evolutionary Trajectory System.");
//        while (safetyCounter < maxIterationsLimit && !context.isPaused()) {
//            state.setIterationCount(safetyCounter);
//            context.log("[DARWIN] [LOOP] Starting Iteration " + (safetyCounter + 1) + " (Phase: "
//                    + state.getCurrentPhase() + ")");
//
//            if (safetyCounter > 0 && intensity_val >= 3) {
//                iterationManager.refineTargetReality(request, context);
//            }
//
//            try {
//                result = runDarwinIteration(context, iterationManager);
//            } catch (Exception e) {
//                context.log("[DARWIN] [CRITICAL] Darwin iteration failed with exception: " + e.getMessage());
//                java.io.StringWriter sw = new java.io.StringWriter();
//                e.printStackTrace(new java.io.PrintWriter(sw));
//                context.log(sw.toString());
//                throw e;
//            }
//            safetyCounter++;
//
//            Trajectory activeTrajectory = iterationManager.getActiveTrajectory(context);
//            if (activeTrajectory != null && !iterationManager.isIntentExpansionPhase(context)) {
//                boolean stabilized = iterationManager.getEvolutionaryTrajectoryEngine().evolve(activeTrajectory,
//                        context);
//                if (stabilized) {
//                    context.log("[DARWIN] [LOOP] Evolutionary equilibrium detected for trajectory "
//                            + activeTrajectory.getTrajectoryId() + ". Converging.");
//                }
//            }
//
//            iterationManager.saveFullCheckpoint();
//
//            if (result.getDecision() != SelfDevDecision.CONTINUE) {
//                if (safetyCounter < minIterations) {
//                    context.log("[DARWIN] Evolution reached decision (" + result.getDecision()
//                            + "), but Min Iterations (" + minIterations + ") not met. Continuing evolution.");
//                    state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(EvolutionPhase.SELECTION_REFINEMENT));
//                    if (context.getStateHolder().getState() == SystemState.DONE) {
//                        iterationManager.transition(SystemState.INIT, context);
//                    }
//                } else {
//                    getSessionContainer().getEventBus().publish(new RuntimeEvent(RuntimeEventType.FLOW_COMPLETED,
//                            context.getSessionId(), "DarwinEngine", result.getDecision().toString()));
//                    break;
//                }
//            }
//
//            String currentPhaseStr = state.getCurrentPhase();
//            if (currentPhaseStr != null
//                    && (currentPhaseStr.contains("TERMINAL") || currentPhaseStr.contains("SATISFIED"))) {
//                if (safetyCounter < minIterations) {
//                    context.log("[DARWIN] Terminal phase (" + state.getCurrentPhase()
//                            + ") reached, but Min Iterations (" + minIterations + ") not met. Continuing evolution.");
//                    state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(EvolutionPhase.SELECTION_REFINEMENT));
//                    if (context.getStateHolder().getState() == SystemState.DONE) {
//                        iterationManager.transition(SystemState.INIT, context);
//                    }
//                } else {
//                    break;
//                }
//            }
//        }
//
//        OrchestratorResponse response = new OrchestratorResponse();
//        response.setResultType(ResultType.CHAT);
//
//        if (result != null && !result.isSuccess()) {
//            response.setResultType(ResultType.ERROR);
//            context.log("[DARWIN] Evolution loop terminated due to iteration failure.");
//        }
//
//        String summary;
//        if ((state.getCurrentPhase().contains("TERMINAL") 
//                || state.getCurrentPhase().contains("SYNTHESIS")
//                || state.getCurrentPhase().contains("DESIGN_SATISFIED")) 
//                && response.getResultType() != ResultType.ERROR) {
//            if (context.getBehaviorProfile().hasTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY)) {
//                summary = iterationManager.performMediatedExportConvergence(request, context);
//            } else if (context.getMetadata().containsKey("testMode")) {
//                summary = "Evolution completed (Test Mode).";
//            } else {
//                int intensity_res = context.getExecutionProfile().getIntensity();
//
//                if (intensity_res == 1) {
//                    IterationRecord winner = context.getKernelContext().getMemoryService().getRecords().stream()
//                            .filter(r -> "ACTIVE".equals(r.getActivationState())).reduce((first, second) -> second)
//                            .orElse(null);
//                    summary = (winner != null && winner.getMutationTrace() != null) ? winner.getMutationTrace()
//                            : "Evolution complete.";
//                } else {
//                    summary = iterationManager.getFinalResponseAgent().generateFinalResponse(request,
//                            context.getOrchestrator().getTasks(), context);
//                }
//            }
//            iterationManager.transition(SystemState.DONE, context);
//        } else {
//            summary = "Evolution completed at phase: " + state.getCurrentPhase();
//        }
//
//        response.setSummary(summary);
//        return response;
//    }
//
//    /**
//     * The heart of the Darwin evolutionary loop - copied from DarwinEngine.
//     */
//    public EvaluationResult runDarwinIteration(TaskContext context, IterationManager manager) throws Exception {
//        // ============================================================
//        // 1. STATE MANAGEMENT & INITIALIZATION
//        // ============================================================
//
//        SystemState currentState = context.getStateHolder().getState();
//        if (currentState == SystemState.DONE || currentState == SystemState.FAILED) {
//            manager.transition(SystemState.INIT, context);
//        }
//
//        eu.kalafatic.evolution.controller.kernel.EvolutionProfile executionProfile = eu.kalafatic.evolution.controller.kernel.EvolutionIntensityCalculator
//                .calculate(context, manager.getActiveTrajectory(context), null);
//        context.log("[DARWIN] Profile derived for Iteration "
//                + (context.getOrchestrationState().getIterationCount() + 1) + ": Capability="
//                + executionProfile.getCapability() + ", Intensity=" + executionProfile.getIntensity());
//        context.getOrchestrationState().setExecutionProfile(executionProfile);
//
//        OrchestrationState state = context.getOrchestrationState();
//        String goal = state.getRawInput();
//        if (goal == null || goal.isEmpty()) {
//            goal = context.getOrchestrator().getSelfDevSession() != null
//                    ? context.getOrchestrator().getSelfDevSession().getInitialRequest()
//                    : "Autonomous Improvement";
//        }
//        
//        // ============================================================
//        // FIX: If this is a TASK, ensure CHAT flag is cleared
//        // ============================================================
//        if (!modeRecognizer.isChatMode(context)) {
//            if (state.getMetadata().containsKey("isChatRequest")) {
//                context.log("[DARWIN] TASK detected. Clearing stale CHAT flag.");
//                state.getMetadata().remove("isChatRequest");
//            }
//            if (context.getExecutionProfile() != null
//                    && context.getExecutionProfile().getCapability() == CapabilityType.CHAT) {
//                context.log("[DARWIN] TASK detected. Resetting profile from CHAT to CODE.");
//                EvolutionProfile taskProfile = EvolutionProfile.create(CapabilityType.CODE, 2);
//                context.getOrchestrationState().setExecutionProfile(taskProfile);
//            }
//        }
//
//        EvolutionPhaseMachine phaseMachine = new EvolutionPhaseMachine();
//        EvolutionPhase phase = state.getCurrentPhase() != null ? EvolutionPhase.fromString(state.getCurrentPhase())
//                : phaseMachine.getInitialPhase();
//
//        state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(phase));
//        if (manager.getCurrentIterationModel() != null) {
//            manager.getCurrentIterationModel().setPhase(state.getCurrentPhase());
//        }
//
//        context.log("[DARWIN] Evolution Phase: " + state.getCurrentPhase());
//
//        // ============================================================
//        // 2. GOAL MODEL & SEMANTIC ENVELOPE
//        // ============================================================
//        GoalModel goalModel = GoalModel.extract(state.getMetadata(), manager, goal, context);
//        
//        if (state.getLockedAbstractionLevel() == null) {
//            AbstractionLevel lockedLevel = AbstractionLevel.DESIGN;
//            String complexity = goalModel.getComplexity() != null ? goalModel.getComplexity().toUpperCase() : "MEDIUM";
//
//            if ("SIMPLE".equals(complexity)) {
//                lockedLevel = AbstractionLevel.IMPLEMENTATION;
//            } else if ("HIGH".equals(complexity)) {
//                lockedLevel = AbstractionLevel.ARCHITECTURE;
//            }
//
//            state.setLockedAbstractionLevel(lockedLevel);
//            context.log("[DARWIN] Abstraction level LOCKED to: " + lockedLevel + " based on complexity: " + complexity);
//        }
//
//        Object envelopeObj = state.getMetadata().get("semanticEnvelope");
//        SemanticEnvelope envelope = null;
//        if (envelopeObj instanceof SemanticEnvelope) {
//            envelope = (SemanticEnvelope) envelopeObj;
//        } else if (envelopeObj instanceof Map) {
//            envelope = new com.fasterxml.jackson.databind.ObjectMapper()
//                    .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
//                    .convertValue(envelopeObj, SemanticEnvelope.class);
//            state.getMetadata().put("semanticEnvelope", envelope);
//        }
//
//        if (envelope == null) {
//            envelope = manager.getSemanticEnvelopeEngine().derive(goalModel, context);
//            state.getMetadata().put("semanticEnvelope", envelope);
//        }
//
//        Trajectory activeTrajectory = manager.getActiveTrajectory(context);
//        int generation = activeTrajectory != null ? activeTrajectory.getGeneration() : 0;
//        String lineage = activeTrajectory != null ? activeTrajectory.getTrajectoryId() : "alpha";
//
//        EvolutionProgressPublisher.startIteration(context, state.getIterationCount() + 1, generation, lineage);
//        EvolutionProgressPublisher.updateStage(context, EvolutionStage.ANALYSIS);
//
//        // ============================================================
//        // 3. TERMINAL PHASE CHECK
//        // ============================================================
//
//        if (phase == EvolutionPhase.TERMINAL_SUCCESS || phase == EvolutionPhase.TERMINAL_FAILURE
//                || phase == EvolutionPhase.DESIGN_SATISFIED) {
//            EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
//            res.setSuccess(phase != EvolutionPhase.TERMINAL_FAILURE);
//            res.setDecision(SelfDevDecision.STOP);
//            return res;
//        }
//
//        // ============================================================
//        // 4. INTENT EXPANSION PHASE
//        // ============================================================
//
//        if (phase == EvolutionPhase.INTENT_EXPANSION) {
//            return handleIntentExpansionPhase(context, manager, phaseMachine, goal, phase);
//        }
//
//        // ============================================================
//        // 5. MEDIATION PHASE
//        // ============================================================
//
//        boolean isMediated = ModeRecognizer.isMediatedMode(context);
//        boolean isSelfDev = ModeRecognizer.isSelfDevMode(context);
//
//        if (isMediated) {
//            context.log("[DARWIN] Mediated Mode: Running mediation cycle...");
//            MediationResult mediation = getMediationEngine().mediate(context, goal, null);
//            state.getMetadata().put("mediationResult", mediation);
//            state.getMetadata().put("mediating", true);
//            state.getMetadata().put("mediationHotspots", mediation.getHotspots());
//            state.getMetadata().put("mediationCandidates", mediation.getCandidates());
//            mergeMediationInsights(mediation, context, manager);
//            context.log("[DARWIN] Mediation complete. Hotspots: " + mediation.getHotspots().size());
//        }
//
//        if (isSelfDev) {
//            context.log("[DARWIN] Self-Dev Mode: Running quick mediation...");
//            MediationResult quickMediation = getMediationEngine().quickMediate(context, goal, null);
//            state.getMetadata().put("quickMediationResult", quickMediation);
//            boolean isSelfIterative = context.getOrchestrator().getAiChat() != null
//                    && context.getOrchestrator().getAiChat().getPromptInstructions() != null
//                    && context.getOrchestrator().getAiChat().getPromptInstructions().isSelfIterativeMode();
//            if (isSelfIterative) {
//                context.log("[DARWIN] Self-Dev iterative mode: Continuous improvement cycle active");
//            }
//        }
//
//        // ============================================================
//        // 6. HIERARCHICAL NODE SELECTION
//        // ============================================================
//
//        EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
//        String nodeToExpandId = lineageEngine.getParentId(tree);
//
//        context.log("[DARWIN] Hierarchical Expansion: Targeting node "
//                + (nodeToExpandId != null ? nodeToExpandId : "ROOT") + " for semantic discovery.");
//
//        manager.checkStep(state.getCurrentPhase(), "BRANCH_GENERATION", "Spawning competing trajectories for: " + goal);
//
//        // ============================================================
//        // 7. BRANCH GENERATION
//        // ============================================================
//        
//        List<BranchVariant> variants = generateProposals(context, goalModel, manager);
//
//        if (variants.isEmpty()) {
//            context.log("[DARWIN] CRITICAL: No trajectories survived diversity analysis. Evolution blocked.");
//            return manager.failedResult();
//        }
//
//        // ============================================================
//        // 8. VARIANT SELECTION
//        // ============================================================
//
//        String manualId = resolveVariantSelection(variants, context, manager);
//
//        if (manualId == null && !context.isAutoApprove()) {
//            if (executionProfile.requireUserSelection()) {
//                manualId = manager.handleVariantSelection(context, variants, goal);
//                if ("REGENERATE".equals(manualId)) {
//                    return runDarwinIteration(context, manager);
//                }
//                if (manualId == null || "STOP".equals(manualId) || "FAILED".equals(manualId)) {
//                    EvaluationResult res = manager.failedResult();
//                    res.setDecision(SelfDevDecision.STOP);
//                    return res;
//                }
//            } else {
//                context.log("[DARWIN] Adaptive Kernel: Auto-selecting best trajectory.");
//                manualId = selectionEngine.selectWinnerAuto(variants);
//            }
//        }
//
//        // ============================================================
//        // 9. DECISION & EXECUTION
//        // ============================================================
//
//        String iterId = manager.getCurrentIterationModel() != null ? manager.getCurrentIterationModel().getId()
//                : "default";
//        eu.kalafatic.evolution.controller.supervision.EvolutionDecision decision = manager.decide(iterId, variants,
//                context, manualId);
//
//        if (activeTrajectory != null) {
//            decision.setPressure(getSessionContainer().getPressureEngine().analyze(activeTrajectory, context));
//        }
//
//        if ("force solution".equalsIgnoreCase(manualId)) {
//            context.log("[DARWIN] Committing selected trajectory via Force Solution.");
//        }
//
//        manager.transition(SystemState.EXECUTING, context);
//        EvaluationResult result = executeWinner(context, decision, variants, goalModel, manager);
//        manager.transition(SystemState.VERIFYING, context);
//
//        // ============================================================
//        // 10. RE-MEDIATION AFTER EXECUTION
//        // ============================================================
//
//        BranchVariant selectedVariant = null;
//        if (decision.getSelectedVariantId() != null) {
//            selectedVariant = variants.stream().filter(v -> v.getId().equals(decision.getSelectedVariantId()))
//                    .findFirst().orElse(null);
//        }
//
//        if (isMediated && selectedVariant != null && result.isSuccess()) {
//            context.log("[DARWIN] Mediated Mode: Running re-mediation after execution...");
//            MediationResult reMediation = getMediationEngine().mediate(context, goal, selectedVariant);
//            state.getMetadata().put("reMediationResult", reMediation);
//
//            if (selectedVariant.getMediationCandidate() != null) {
//                manager.mergeArchitecturalDiscovery(selectedVariant, context);
//            }
//
//            if (reMediation.hasChanges()) {
//                context.log("[DARWIN] Re-mediation detected changes: " + reMediation.getDelta().getSummary());
//                mergeMediationInsights(reMediation, context, manager);
//            }
//        }
//
//        // ============================================================
//        // 11. RESULT HANDLING & PHASE TRANSITION
//        // ============================================================
//
//        if (result.isSuccess()) {
//            EvolutionPhase currentPhaseEnum = EvolutionPhase.fromString(state.getCurrentPhase());
//            EvolutionPhase nextPhase = manager.getEvolutionaryTrajectoryEngine().determineNextPhase(currentPhaseEnum,
//                    manager.getActiveTrajectory(context), context);
//
//            state.setIterationCount(state.getIterationCount() + 1);
//
//            if (nextPhase == currentPhaseEnum) {
//                context.log("[DARWIN] Evolution continuing in current phase: " + nextPhase + " (Generation: "
//                        + state.getIterationCount() + ")");
//            } else {
//                context.log("[DARWIN] Evolution transitioning to phase: " + nextPhase + " (Generation: "
//                        + state.getIterationCount() + ")");
//            }
//
//            state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(nextPhase));
//
//            if (nextPhase == EvolutionPhase.DESIGN_SATISFIED) {
//                manager.handleSatisfactionReview(context, manager.getActiveTrajectory(context));
//                nextPhase = EvolutionPhase.fromString(state.getCurrentPhase());
//            }
//
//            result.setDecision(phaseMachine.determineDecision(nextPhase));
//
//            if (!manager.handlePhaseConfirmation(context, state)) {
//                result.setDecision(SelfDevDecision.STOP);
//            }
//
//            EvolutionProgressPublisher.completeIteration(context);
//            manager.transition(SystemState.DONE, context);
//        } else {
//            handleIterationFailure(context, manager, result);
//            EvolutionProgressPublisher.completeIteration(context);
//            manager.transition(SystemState.FAILED, context);
//        }
//
//        return result;
//    }
//
//    // ============================================================
//    // HELPER METHODS - Copied from DarwinEngine
//    // ============================================================
//
//    protected String generateAlternativeChatResponse(String request, TaskContext context) {
//        return "I'm ready to evolve! Tell me what code you want me to work on, and I'll generate competing implementations for you to choose from.";
//    }
//
//    protected EvaluationResult handleIntentExpansionPhase(TaskContext context, IterationManager manager,
//            EvolutionPhaseMachine phaseMachine, String goal, EvolutionPhase phase) throws Exception {
//
//        OrchestrationState state = context.getOrchestrationState();
//
//        EvolutionProgressPublisher.updateStage(context, EvolutionStage.ANALYSIS);
//        manager.transition(SystemState.ANALYZING, context);
//
//        eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult expansion = manager
//                .getIntentExpansionEngine().expand(goal, context);
//        state.getMetadata().put("intentExpansion", expansion);
//
//        context.consoleLog("[DARWIN] Intent Interpretation: " + expansion.getState());
//
//        if (!manager.handleIntentReview(context, expansion, goal)) {
//            return manager.failedResult();
//        }
//
//        eu.kalafatic.evolution.controller.orchestration.intent.ClarificationPlanner planner = manager
//                .getClarificationPlanner();
//        eu.kalafatic.evolution.controller.orchestration.intent.ClarificationPlanner.Strategy strategy = planner
//                .determineStrategy(expansion, context);
//        context.consoleLog("[DARWIN] Clarification Strategy: " + strategy);
//
//        boolean isStepMode = context.getOrchestrator().getAiChat() != null
//                && context.getOrchestrator().getAiChat().getPromptInstructions() != null
//                && context.getOrchestrator().getAiChat().getPromptInstructions().isStepMode();
//
//        if (strategy == eu.kalafatic.evolution.controller.orchestration.intent.ClarificationPlanner.Strategy.CLARIFY_USER
//                && !isStepMode) {
//            if (context.isAutoApprove()) {
//                context.log("[DARWIN] AUTO Mode: Downgrading CLARIFY_USER to AUTO_INFER.");
//                strategy = eu.kalafatic.evolution.controller.orchestration.intent.ClarificationPlanner.Strategy.AUTO_INFER;
//            } else {
//                context.log("[DARWIN] MANUAL Mode: Downgrading CLARIFY_USER to BRANCH_PARALLEL.");
//                strategy = eu.kalafatic.evolution.controller.orchestration.intent.ClarificationPlanner.Strategy.BRANCH_PARALLEL;
//            }
//        }
//
//        if (strategy == eu.kalafatic.evolution.controller.orchestration.intent.ClarificationPlanner.Strategy.CLARIFY_USER) {
//            if (!manager.handleClarification(context, planner, expansion, goal)) {
//                return manager.failedResult();
//            }
//            EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
//            res.setSuccess(true);
//            res.setDecision(SelfDevDecision.CONTINUE);
//            return res;
//        }
//
//        EvolutionPhase nextPhase = manager.getEvolutionaryTrajectoryEngine().determineNextPhase(phase, null, context);
//        state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(nextPhase));
//
//        context.log("[DARWIN] Intent grounding complete. Progressing to " + nextPhase);
//        state.setIterationCount(state.getIterationCount() + 1);
//
//        EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
//        res.setSuccess(true);
//        res.setDecision(SelfDevDecision.CONTINUE);
//        return res;
//    }
//
//    protected String resolveVariantSelection(List<BranchVariant> variants, TaskContext context,
//            IterationManager manager) {
//
//        OrchestrationState state = context.getOrchestrationState();
//        String manualId = null;
//
//        if (state.getMetadata().containsKey("pendingControlCommand")) {
//            String pendingCommand = (String) state.getMetadata().remove("pendingControlCommand");
//            if (pendingCommand.toLowerCase().startsWith("select ")
//                    || pendingCommand.toLowerCase().startsWith("approve variant ")) {
//                manualId = pendingCommand.toLowerCase().startsWith("select ") ? pendingCommand.substring(7).trim()
//                        : pendingCommand.substring(16).trim();
//                context.log("[DARWIN] Auto-resolving variant selection from initial command: " + manualId);
//            } else if (pendingCommand.equalsIgnoreCase("approved") || pendingCommand.equalsIgnoreCase("proceed")
//                    || pendingCommand.equalsIgnoreCase("yes") || pendingCommand.equalsIgnoreCase("force solution")) {
//                manualId = variants.stream().max((v1, v2) -> Double.compare(v1.getScore(), v2.getScore()))
//                        .map(v -> v.getId()).orElse(null);
//                context.log("[DARWIN] Auto-approving best variant from initial command: " + manualId);
//            }
//        }
//
//        return manualId;
//    }
//
//    protected void handleIterationFailure(TaskContext context, IterationManager manager, EvaluationResult result)
//            throws Exception {
//
//        context.log("[DARWIN] Iteration failed. Attempting recovery...");
//
//        int maxRetries = 3;
//        int retryCount = (int) context.getOrchestrationState().getMetadata().getOrDefault("retryCount", 0);
//
//        if (retryCount < maxRetries) {
//            retryCount++;
//            context.getOrchestrationState().getMetadata().put("retryCount", retryCount);
//            context.log("[DARWIN] Retrying iteration (attempt " + retryCount + "/" + maxRetries + ")");
//            context.getOrchestrationState()
//                    .setCurrentPhase(EvolutionPhaseMachine.toLegacyString(EvolutionPhase.SELECTION_REFINEMENT));
//            manager.transition(SystemState.INIT, context);
//            result.setDecision(SelfDevDecision.CONTINUE);
//        } else {
//            context.log("[DARWIN] Max retries exceeded. Rolling back.");
//            boolean isSelfDev = context.getBehaviorProfile().hasTrait(BehaviorTrait.WORKFLOW_SELF_DEV)
//                    || context.getOrchestrator().getAiChat() != null
//                            && context.getOrchestrator().getAiChat().getPromptInstructions() != null
//                            && context.getOrchestrator().getAiChat().getPromptInstructions().isSelfIterativeMode();
//
//            if (isSelfDev && manager.getGitManager() != null) {
//                try {
//                    manager.getGitManager().rollback(context);
//                    context.log("[DARWIN] Self-Dev rollback successful.");
//                } catch (Exception e) {
//                    context.log("[DARWIN] Self-Dev rollback failed: " + e.getMessage());
//                }
//            }
//            result.setDecision(SelfDevDecision.ROLLBACK);
//        }
//    }
//
//    protected MediationEngine getMediationEngine() {
//        if (mediationEngine == null) {
//            mediationEngine = new MediationEngine();
//        }
//        return mediationEngine;
//    }
//
//    protected void mergeMediationInsights(MediationResult mediation, TaskContext context, IterationManager manager) {
//
//        context.log("[DARWIN] Merging mediation insights...");
//
//        EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
//
//        if (mediation.getHotspots() != null) {
//            for (Hotspot hotspot : mediation.getHotspots()) {
//                String filePath = hotspot.getFile();
//                if (filePath != null && !filePath.isEmpty()) {
//                    String nodeId = "hotspot-" + Math.abs(filePath.hashCode());
//                    EvolutionNode node = tree.getNode(nodeId);
//                    if (node == null) {
//                        node = new EvolutionNode();
//                        node.setId(nodeId);
//                        node.setStrategy("Hotspot: " + hotspot.getName());
//                        node.setStatus("MEDIATED");
//                        tree.addNode(node);
//                    }
//                    Map<String, String> dims = node.getEngineeringDimensions();
//                    if (dims == null) {
//                        dims = new java.util.HashMap<>();
//                    }
//                    dims.put("hotspot_score", String.valueOf(hotspot.getSignificance()));
//                    dims.put("hotspot_type", hotspot.getType() != null ? hotspot.getType() : "UNKNOWN");
//                    if (hotspot.getName() != null) {
//                        dims.put("hotspot_name", hotspot.getName());
//                    }
//                    if (hotspot.getDescription() != null) {
//                        dims.put("hotspot_description", hotspot.getDescription());
//                    }
//                }
//            }
//        }
//
//        if (mediation.getWinner() != null) {
//            context.getOrchestrationState().getMetadata().put("currentMediationWinner", mediation.getWinner());
//        }
//
//        context.getKernelContext().getMemoryService().saveEvolutionTree();
//    }
//
//    public List<BranchVariant> generateProposals(TaskContext context, GoalModel goal, IterationManager manager)
//            throws Exception {
//        context.log("[DARWIN] Entering generateProposals for goal: " + goal.getPrimaryAction());
//        Iteration currentIterationModelImpl = manager.getCurrentIterationModel();
//        String iterId = currentIterationModelImpl != null ? currentIterationModelImpl.getId() : "default";
//
//        eu.kalafatic.evolution.controller.kernel.EvolutionProfile profile = context.getExecutionProfile();
//        String originalBranch = null;
//        String baseCommit = null;
//        if (profile.requiresRepository() && manager.getGitManager().isGitRepository()) {
//            originalBranch = manager.getGitManager().getCurrentBranch();
//            baseCommit = manager.getGitManager().getHeadCommit();
//        }
//
//        context.log("[DARWIN] Discovering semantic trajectories to resolve goal: " + goal);
//        EvolutionProgressPublisher.updateStage(context, EvolutionStage.ANALYZE_PARENT);
//
//        Evaluator.Evaluation initialEval = manager.getEvaluator().evaluateWithSnapshot();
//        StateSnapshot snapshot = initialEval.snapshot;
//
//        Trajectory trajectory = null;
//        IterationRecord lastWinner = context.getKernelContext().getMemoryService().getRecords().stream()
//                .filter(r -> "ACTIVE".equals(r.getActivationState())).reduce((first, second) -> second).orElse(null);
//
//        if (lastWinner != null && lastWinner.getBranchId() != null) {
//            trajectory = context.getSemanticWorkspace().getTrajectoryMemory().getTrajectory(lastWinner.getBranchId());
//            if (trajectory != null) {
//                context.log("[DARWIN] Continuing lineage from survivor: " + trajectory.getTrajectoryId());
//            }
//        }
//
//        if (trajectory == null) {
//            trajectory = new Trajectory("traj-" + iterId, goal.getPrimaryAction());
//            context.getSemanticWorkspace().getTrajectoryMemory().recordTrajectory(trajectory);
//            context.log("[DARWIN] Starting new evolutionary lineage trajectory: " + trajectory.getTrajectoryId());
//
//            EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
//            if (tree.getRootId() == null) {
//                EvolutionNode root = new EvolutionNode();
//                root.setId("root-" + iterId);
//                root.setStrategy("Evolutionary Root: " + goal.getPrimaryAction());
//                root.setSemanticPhilosophy("Initial evolutionary root");
//                root.setIteration(0);
//                root.setStatus("ROOT");
//                tree.addNode(root);
//                context.getKernelContext().getMemoryService().saveEvolutionTree();
//            }
//        }
//
//        FailureMemory failureMemory = context.getKernelContext().getMemoryService().getFailureMemory();
//
//        getSessionContainer().getEventBus().publish(new RuntimeEvent(RuntimeEventType.MUTATING, context.getSessionId(),
//                "DarwinEngine", goal.getPrimaryAction()));
//
//        EvolutionaryPressureVector pressure = null;
//        if (trajectory != null) {
//            pressure = getSessionContainer().getPressureEngine().analyze(trajectory, context);
//            trajectory.recordPressure(pressure);
//        }
//
//        if (trajectory != null && trajectory.getGeneration() == 0) {
//            context.log("[DARWIN] Orchestrator: Mapping evolutionary territory...");
//            var graph = context.getKernelContext().getMemoryService().getEvolutionGraph();
//            graph.recordTerritory("ARCHITECTURE", "Implementation Dimensions");
//            graph.recordTerritory("ARCHITECTURE", "Divergent Blueprints");
//            graph.recordTerritory("STABILITY", "Reliability Pressure");
//            graph.recordTerritory("EXTENSIBILITY", "Service Orientation");
//        }
//
//        EvolutionProgressPublisher.updateStage(context, EvolutionStage.GENERATE_BRANCH);
//        List<BranchVariant> rawVariants = generateVariants(goal, snapshot, failureMemory, trajectory, pressure);
//
//        getSessionContainer().getEventBus().publish(new RuntimeEvent(RuntimeEventType.BRANCH_CREATED,
//                context.getSessionId(), "DarwinEngine", rawVariants.size()));
//
//        if (rawVariants.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        List<BranchVariant> evaluationCandidates = rawVariants.stream().filter(v -> {
//            EvolutionNode node = context.getKernelContext().getMemoryService().getEvolutionTree().getNode(v.getId());
//            return node != null && !"REJECTED_SEMANTIC".equals(node.getStatus());
//        }).collect(Collectors.toList());
//
//        if (profile.useImplementation()) {
//            context.log("[DARWIN] Parallel Evaluation: Triggering implementation validation for "
//                    + evaluationCandidates.size() + " variants.");
//            EvolutionProgressPublisher.updateStage(context, EvolutionStage.VALIDATE_BRANCH);
//
//            String baseCommitFinal = baseCommit;
//            eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector pressureFinal = pressure;
//
//            evaluationCandidates.parallelStream().forEach(variant -> {
//                try {
//                    evaluateVariantParallel(variant, manager.getTaskPlanner(), context, baseCommitFinal, pressureFinal,
//                            manager);
//                } catch (Exception e) {
//                    context.log("[DARWIN] Parallel Evaluation Failed for " + variant.getId() + ": " + e.getMessage());
//                }
//            });
//        } else {
//            context.log("[DARWIN] Adaptive Kernel: Implementation validation disabled for current profile.");
//            for (BranchVariant variant : evaluationCandidates) {
//                variant.setSuccess(true);
//                variant.setScore(0.95);
//            }
//        }
//
//        EvolutionProgressPublisher.updateStage(context, EvolutionStage.SCORE_BRANCH);
//        for (BranchVariant v : rawVariants) {
//            EvolutionNode node = context.getKernelContext().getMemoryService().getEvolutionTree().getNode(v.getId());
//            if (node != null && "REJECTED_SEMANTIC".equals(node.getStatus())) {
//                v.setSuccess(false);
//                v.setScore(Math.min(v.getScore(), 0.1));
//            }
//        }
//
//        context.getOrchestrationState().getCognitiveTrace()
//                .addNode(new CausalNode("darwin-mutation-" + System.currentTimeMillis(), "MUTATION", "DarwinEngine",
//                        List.of(goal.getPrimaryAction()),
//                        rawVariants.stream().map(v -> v.getId()).collect(Collectors.toList()), 1.0,
//                        "Generated and evaluated " + rawVariants.size() + " variants."));
//
//        eu.kalafatic.evolution.controller.orchestration.capability.contracts.ISchedulingContract scheduler = getSessionContainer()
//                .getCapabilityRegistry().getContractImplementation(
//                        eu.kalafatic.evolution.controller.orchestration.capability.contracts.ISchedulingContract.ID,
//                        eu.kalafatic.evolution.controller.orchestration.capability.contracts.ISchedulingContract.class);
//
//        eu.kalafatic.evolution.controller.execution.ScheduledExecutionPlan executionPlan;
//        if (scheduler != null) {
//            executionPlan = (eu.kalafatic.evolution.controller.execution.ScheduledExecutionPlan) scheduler
//                    .schedule(rawVariants, context);
//        } else {
//            context.log("[DARWIN] Scheduler unavailable. Entering manual continuation mode.");
//            executionPlan = new eu.kalafatic.evolution.controller.execution.ScheduledExecutionPlan(rawVariants,
//                    "Manual fallback (No scheduler)",
//                    eu.kalafatic.evolution.controller.execution.ExecutionBudget.defaultProfile());
//        }
//        List<BranchVariant> variants = executionPlan.getScheduledVariants();
//        context.getOrchestrationState().getMetadata().put("executionPlan", executionPlan);
//
//        for (BranchVariant v : variants) {
//            eu.kalafatic.evolution.controller.trajectory.TrajectoryAnalysisRecord tar = new eu.kalafatic.evolution.controller.trajectory.TrajectoryAnalysisRecord();
//            tar.setIterationId(iterId);
//            tar.setBranchId(v.getId());
//            tar.setStrategy(v.getStrategy());
//            tar.setFitnessScore(v.getScore());
//            context.getKernelContext().getMemoryService().saveTrajectoryAnalysis(tar);
//        }
//        context.getKernelContext().getMemoryService().flush();
//
//        return variants;
//    }
//
//    public EvaluationResult executeWinner(TaskContext context,
//            eu.kalafatic.evolution.controller.supervision.EvolutionDecision decision, List<BranchVariant> variants,
//            GoalModel goal, IterationManager manager) throws Exception {
//        eu.kalafatic.evolution.controller.kernel.EvolutionProfile profile = context.getExecutionProfile();
//        context.log("[DARWIN] Entering executeWinner for variant: " + decision.getSelectedVariantId());
//        eu.kalafatic.evolution.controller.orchestration.VariantExecutionContext winningContext = null;
//        String originalBranch = null;
//        String baseCommit = null;
//        if (profile.requiresRepository() && manager.getGitManager().isGitRepository()) {
//            originalBranch = manager.getGitManager().getCurrentBranch();
//            baseCommit = manager.getGitManager().getHeadCommit();
//        }
//        Iteration currentIterationModelImpl = manager.getCurrentIterationModel();
//        String iterId = currentIterationModelImpl != null ? currentIterationModelImpl.getId() : "default";
//        String snapshotBranch = "snapshot/" + iterId + "-" + System.currentTimeMillis();
//
//        String finalWinnerId = decision.getSelectedVariantId();
//        BranchVariant selectedVariant = null;
//        if (finalWinnerId != null) {
//            selectedVariant = variants.stream().filter(v -> v.getId().equals(finalWinnerId)).findFirst().orElse(null);
//        }
//        
//        // ============================================================
//        // FIX: Handle CHAT variants directly without Git
//        // ============================================================
//        if (selectedVariant != null && "CHAT_RESPONSE".equals(selectedVariant.getStrategyType())) {
//            context.log("[DARWIN] CHAT: Executing conversational response directly (no Git).");
//            
//            String response = selectedVariant.getActions().stream()
//                .filter(a -> "TALK".equals(a.getOperation()))
//                .map(a -> a.getImplementation())
//                .findFirst()
//                .orElse("Hello! How can I help you today?");
//            
//            context.getOrchestrationState().getMetadata().put("chatResponse", response);
//            
//            EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
//            result.setSuccess(true);
//            result.setDecision(SelfDevDecision.STOP);
//            
//            IterationRecord record = new IterationRecord();
//            record.setIteration(context.getOrchestrationState().getIterationCount());
//            record.setGoal(goal.getPrimaryAction());
//            record.setStrategy(selectedVariant.getStrategy());
//            record.setBranchId(selectedVariant.getId());
//            record.setResult("SUCCESS");
//            record.setActivationState("ACTIVE");
//            record.setMutationTrace(response);
//            record.setTimestamp(System.currentTimeMillis());
//            context.getKernelContext().getMemoryService().saveRecord(record);
//            context.getKernelContext().getMemoryService().saveEvolutionTree();
//            context.getKernelContext().getMemoryService().flush();
//            
//            return result;
//        }
//
//        double winnerScore = decision.getAggregatedScores().getOrDefault(finalWinnerId, 0.0);
//        getSessionContainer().getEventBus()
//                .publish(new RuntimeEvent(RuntimeEventType.VARIANT_EVALUATED, context.getSessionId(), finalWinnerId,
//                        iterId, "DarwinEngine", winnerScore, System.currentTimeMillis()));
//
//        if (selectedVariant == null || (selectedVariant.getActivationState() != BranchVariant.ActivationState.ACTIVE
//                && selectedVariant.getScore() < 0.3)) {
//            context.log("[DARWIN] Darwin Evolution: No viable winner selected or winner score too low.");
//            return manager.failedResult();
//        }
//
//        EvolutionProgressPublisher.updateStage(context, EvolutionStage.SAVE_LINEAGE);
//
//        if (currentIterationModelImpl != null) {
//            currentIterationModelImpl.setSurvivalArgument(selectedVariant.getSurvivalArgument());
//            currentIterationModelImpl.setTradeoffs(selectedVariant.getTradeoffs());
//            currentIterationModelImpl.setFailureRisks(selectedVariant.getFailureRisks());
//            currentIterationModelImpl.setJustification(selectedVariant.getStrategy());
//        }
//
//        boolean isExportOnly = context.getBehaviorProfile().hasTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY);
//
//        boolean isTestMode = context.getMetadata().containsKey("testMode");
//        try {
//            if (profile.requiresRepository() && !isExportOnly && !isTestMode) {
//                manager.getGitManager().createBranchFrom(originalBranch, snapshotBranch);
//                manager.getGitManager().forceCheckout(snapshotBranch);
//            }
//
//            context.log("[DARWIN] Executing winner variant: " + selectedVariant.getId() + " ("
//                    + selectedVariant.getStrategy() + ")");
//            if (profile.requiresRepository() && !isExportOnly && !isTestMode) {
//                manager.getGitManager().createBranchFrom(originalBranch, selectedVariant.getBranchName());
//            }
//
//            context.log("[DARWIN] Materializing selected semantic territory: " + selectedVariant.getStrategy());
//
//            winningContext = evaluateVariantParallel(selectedVariant, manager.getTaskPlanner(), context, baseCommit,
//                    decision.getPressure(), manager);
//
//            if (isExportOnly && selectedVariant.getMediationCandidate() != null) {
//                manager.mergeArchitecturalDiscovery(selectedVariant, context);
//            }
//
//            eu.kalafatic.evolution.controller.trajectory.ResultSynthesizer synthesizer = new eu.kalafatic.evolution.controller.trajectory.ResultSynthesizer();
//            synthesizer.synthesize(List.of(selectedVariant), context);
//
//            mergeHybridInsights(variants, selectedVariant, context);
//
//            if (!selectedVariant.isSuccess()) {
//                context.log(
//                        "[DARWIN] Winner variant execution failed during materialization: " + selectedVariant.getId());
//                if (!isExportOnly && !isTestMode) {
//                    manager.getGitManager().forceCheckout(originalBranch);
//                    manager.getGitManager().rollback(context);
//                }
//                return manager.failedResult();
//            }
//
//            if (profile.requiresRepository() && !isExportOnly && !isTestMode) {
//                manager.getGitManager().forceCheckout(originalBranch);
//                manager.getGitManager().merge(selectedVariant.getBranchName());
//            } else if (isExportOnly) {
//                context.log("[DARWIN] Applying cognitive winner: " + selectedVariant.getStrategy());
//                context.getOrchestrationState().getMetadata().put("current_understanding",
//                        selectedVariant.getStrategy());
//                context.getOrchestrationState().getMetadata().put("current_strategy",
//                        selectedVariant.getStrategyType());
//                context.getOrchestrationState().getMetadata().put("current_reasoning_focus",
//                        selectedVariant.getReasoningFocus());
//                context.getOrchestrationState().getMetadata().put("current_selected_files",
//                        selectedVariant.getSelectedFiles());
//                context.getOrchestrationState().getMetadata().put("current_actions", selectedVariant.getActions());
//                if (selectedVariant.getMediationCandidate() != null) {
//                    context.getOrchestrationState().getMetadata().put("winningMediationCandidate",
//                            selectedVariant.getMediationCandidate());
//                }
//            }
//
//            if (winningContext != null) {
//                for (eu.kalafatic.evolution.model.orchestration.Task t : winningContext.getTasks()) {
//                    if (!context.getOrchestrator().getTasks().contains(t)) {
//                        context.getOrchestrator().getTasks().add(t);
//                    }
//                }
//            }
//
//            executionEngine.applyWinner(selectedVariant, context);
//
//            if (isExportOnly) {
//                EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
//                res.setSuccess(true);
//                res.setDecision(eu.kalafatic.evolution.model.orchestration.SelfDevDecision.CONTINUE);
//                return res;
//            }
//
//            if (profile.shouldPerformRealityCheck()) {
//                eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceDeltaAnalyzer.DeltaAnalysis reality = executionEngine
//                        .analyzeWorkspace(baseCommit, context);
//                context.log("[DARWIN] Reality Check: Winner variant applied. Analysis: " + reality.toString());
//
//                final BranchVariant finalSelectedVariant = selectedVariant;
//                reality.getChangedFileMap().forEach((path, type) -> {
//                    context.getFileChangeTracker().recordChange(path, type);
//                    if (finalSelectedVariant != null) {
//                        EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
//                        EvolutionNode node = tree.getNode(finalSelectedVariant.getId());
//                        if (node != null) {
//                            if (type == FileChangeTracker.ChangeType.NEW)
//                                node.getCreatedFiles().add(path);
//                            else if (type == FileChangeTracker.ChangeType.REMOVED)
//                                node.getDeletedFiles().add(path);
//                            else
//                                node.getModifiedFiles().add(path);
//                        }
//                    }
//                });
//
//                boolean isSignificant = reality.isSignificant();
//                if (!isSignificant) {
//                    context.log("[DARWIN] Reality Check WARNING: Winner variant resulted in NO physical changes.");
//                }
//                context.getOrchestrationState().getMetadata().put("lastRealityCheckSignificant", isSignificant);
//            }
//
//            EvaluationResult result = manager.getFitnessEngine().evaluate(context.getProjectRoot(), context,
//                    eu.kalafatic.evolution.controller.orchestration.selfdev.RealityLevel.HEAVY);
//
//            if (result.isSuccess() || selectedVariant != null) {
//                String completedPhase = context.getOrchestrationState().getCurrentPhase();
//
//                for (BranchVariant v : variants) {
//                    if (v.getActivationState() == BranchVariant.ActivationState.ACTIVE
//                            || v.getActivationState() == BranchVariant.ActivationState.KEPT) {
//                        IterationRecord record = new IterationRecord();
//                        record.setIteration(context.getOrchestrationState().getIterationCount());
//                        record.setGoal(goal.getPrimaryAction());
//                        record.setStrategy(v.getStrategy());
//                        record.setStrategyType(v.getStrategyType() != null ? v.getStrategyType().toString() : null);
//                        record.setSemanticAnchor(v.getSemanticAnchor());
//                        record.setMutationTrace(v.getMutationTrace());
//                        record.setInheritedContext(v.getInheritedContext());
//                        record.setRejectedSiblings(v.getRejectedSiblings());
//                        record.setBranchId(v.getId());
//
//                        if (v.getId().equals(selectedVariant.getId())) {
//                            record.setResult(result.isSuccess() ? "SUCCESS" : "SUCCESS_WITH_BUILD_ERROR");
//                            record.setActivationState("ACTIVE");
//
//                            List<String> rejected = variants.stream().filter(other -> !other.getId().equals(v.getId()))
//                                    .map(other -> other.getStrategy()).collect(Collectors.toList());
//                            record.setRejectedSiblings(rejected);
//
//                            List<String> reasons = variants.stream().filter(other -> !other.getId().equals(v.getId()))
//                                    .map(other -> {
//                                        EvolutionNode n = context.getKernelContext().getMemoryService()
//                                                .getEvolutionTree().getNode(other.getId());
//                                        return n != null ? n.getRejectionReason() : "Lower fitness score";
//                                    }).collect(Collectors.toList());
//                            record.setRejectionReasons(reasons);
//                        } else {
//                            record.setResult("KEPT_FOR_DIVERSITY");
//                            record.setActivationState("KEPT");
//                        }
//
//                        record.setTimestamp(System.currentTimeMillis());
//                        context.getKernelContext().getMemoryService().saveRecord(record);
//
//                        EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
//                        EvolutionNode node = tree.getNode(v.getId());
//                        if (node != null) {
//                            node.setStatus(v.getActivationState().name());
//                            if (v.getActivationState() == BranchVariant.ActivationState.ACTIVE) {
//                                tree.setCurrentWinnerId(v.getId());
//                                node.setWinner(true);
//                                getSessionContainer().getEventBus()
//                                        .publish(new RuntimeEvent(RuntimeEventType.WINNER_SELECTED,
//                                                context.getSessionId(), v.getId(), v.getStrategy()));
//
//                                Object genomeObj = context.getOrchestrationState().getMetadata().get("semanticGenome");
//                                SemanticGenome genome = eu.kalafatic.evolution.controller.parsers.JsonUtils
//                                        .restoreFromMetadata(genomeObj, SemanticGenome.class, "semanticGenome",
//                                                context);
//                                if (genome != null && genome != genomeObj) {
//                                    context.getOrchestrationState().getMetadata().put("semanticGenome", genome);
//                                }
//
//                                if (genome != null) {
//                                    String activeDimId = node.getActiveDimension();
//                                    if (activeDimId == null) {
//                                        activeDimId = node.getEngineeringDimensions().get("active_dimension");
//                                    }
//
//                                    record.setActiveDimension(activeDimId);
//                                    record.setLockedDimensions(new java.util.ArrayList<>(genome.getLockedDimensions()));
//
//                                    if (activeDimId != null && !activeDimId.isEmpty()) {
//                                        genome.lockDimension(activeDimId);
//                                        context.log("[DARWIN] Dimension LOCKED: " + activeDimId);
//                                    }
//                                }
//                            }
//
//                            getSessionContainer().getEventBus().publish(new RuntimeEvent(
//                                    RuntimeEventType.FITNESS_UPDATED, context.getSessionId(), v.getId(), v.getScore()));
//                        }
//                    }
//                }
//                context.getKernelContext().getMemoryService().saveEvolutionTree();
//                getSessionContainer().getEventBus().publish(
//                        new RuntimeEvent(RuntimeEventType.TREE_UPDATED, context.getSessionId(), "DarwinEngine", null));
//
//                if (profile.requiresRepository() && !isExportOnly && !isTestMode
//                        && manager.getGitManager().isGitRepository()) {
//                    boolean hasPhysicalChanges = context.getOrchestrationState().getMetadata()
//                            .get("lastRealityCheckSignificant") != null
//                            && (Boolean) context.getOrchestrationState().getMetadata()
//                                    .get("lastRealityCheckSignificant");
//
//                    if (hasPhysicalChanges) {
//                        manager.checkStep(selectedVariant.getId(), "GIT_COMMIT",
//                                "Committing evolutionary changes for phase: " + completedPhase);
//                        manager.getGitManager().commit("Darwin Evolution Phase " + completedPhase, context);
//                    } else {
//                        context.log("[DARWIN] Skipping Git commit: No physical changes detected in repository.");
//                    }
//                }
//
//                result.setSuccess(true);
//                return result;
//            } else {
//                if (!isExportOnly && !isTestMode && manager.getGitManager().isGitRepository()) {
//                    manager.getGitManager().rollback();
//                }
//                return result;
//            }
//        } catch (Exception e) {
//            context.log("[DARWIN] DarwinEngine.executeWinner failed: " + e.getMessage());
//            if (profile.requiresRepository() && !isExportOnly && !isTestMode
//                    && manager.getGitManager().isGitRepository()) {
//                try {
//                    manager.getGitManager().forceCheckout(originalBranch);
//                    manager.getGitManager().rollback(context);
//                } catch (Exception ex) {
//                    context.log("[DARWIN] Failed to rollback after error: " + ex.getMessage());
//                }
//            }
//            throw e;
//        }
//    }
//
//    // ============================================================
//    // PROTECTED METHODS - May be overridden by subclasses
//    // ============================================================
//
//    /**
//     * Generates variants. Subclasses override for mode-specific behavior.
//     * This is the IMutationContract implementation.
//     */
//    @Override
//    public abstract List<BranchVariant> generateVariants(GoalModel goal, StateSnapshot snapshot, 
//                                                         FailureMemory failureMemory, 
//                                                         Trajectory trajectory, 
//                                                         EvolutionaryPressureVector pressure) throws Exception;
//
//    // ============================================================
//    // PRIVATE HELPER METHODS - Copied from DarwinEngine
//    // ============================================================
//
//    private eu.kalafatic.evolution.controller.orchestration.VariantExecutionContext evaluateVariantParallel(
//            BranchVariant variant, TaskPlanner planner, TaskContext context, String baseCommit,
//            EvolutionaryPressureVector pressure, IterationManager manager) {
//        File tempDir = null;
//        eu.kalafatic.evolution.controller.supervision.AuthorityController authority = context.getKernelContext()
//                .getAuthority();
//        eu.kalafatic.evolution.controller.orchestration.VariantExecutionContext variantExecContext = new eu.kalafatic.evolution.controller.orchestration.VariantExecutionContext(
//                variant.getId());
//
//        boolean isMediated = ModeRecognizer.isMediatedMode(context);
//        boolean isChatMode = modeRecognizer.isChatMode(context);
//        try {
//            if (context.getMetadata().containsKey("testMode") || isMediated || isChatMode) {
//                tempDir = context.getProjectRoot();
//            } else {
//                tempDir = java.nio.file.Files.createTempDirectory("evo-variant-" + variant.getId()).toFile();
//                try {
//                    manager.getGitManager().removeWorktree(tempDir.getAbsolutePath());
//                    manager.getGitManager().pruneWorktrees();
//                } catch (Exception e) {
//                }
//                manager.getBranchManager().createWorktree(variant.getBranchName(), tempDir.getAbsolutePath());
//            }
//            TaskContext variantContext = new TaskContext(context.getOrchestrator(), tempDir);
//            variantContext.setSessionId(context.getSessionId());
//            variantContext.setKernelContext(context.getKernelContext());
//            variantContext.getMetadata().put("variantId", variant.getId());
//            variantContext.getMetadata().put("variantExecContext", variantExecContext);
//            variantContext.setPlatformMode(context.getPlatformMode());
//            variantContext.setAutoApprove(true);
//            variantContext.setAiService(aiService);
//
//            context.getLogListeners().forEach(variantContext::addLogListener);
//            context.getApprovalListeners().forEach(variantContext::addApprovalListener);
//            context.getInputListeners().forEach(variantContext::addInputListener);
//
//            List<eu.kalafatic.evolution.model.orchestration.Task> tasks = planner
//                    .generateTasksFromVariant(variantContext, variant);
//            context.log("[DARWIN] Generated " + tasks.size() + " tasks for variant: " + variant.getId());
//            IterationManager variantManager = KernelFactory.create(variantContext, getSessionContainer(), aiService);
//            
//            if (context.getMetadata().containsKey("testMode") || isMediated || isChatMode) {
//                variant.setSuccess(true);
//                variant.setScore(0.95);
//
//                if (!isMediated && !isChatMode) {
//                    for (eu.kalafatic.evolution.model.orchestration.Task task : tasks) {
//                        try {
//                            variantManager.getTaskExecutor().getOrchestrator().executeTask(task, variantContext);
//                        } catch (Exception e) {
//                            context.log("[DARWIN] [TEST_MODE] Execution failed but continuing: " + e.getMessage());
//                        }
//                    }
//                } else {
//                    context.log("[DARWIN] " + (isMediated ? "Mediated Mode" : "CHAT") + ": Skipping task execution.");
//                }
//
//                manager.updateVariantLifecycle(List.of(variant), variant.getId(),
//                        BranchVariant.ActivationState.VERIFIED, context);
//                manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.SCORING,
//                        context);
//
//                variant.setMutationTrace(isMediated ? "Cognitive evolution in mediated mode" : "Chat response");
//                return variantExecContext;
//            }
//
//            boolean success = true;
//            manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.EXECUTING,
//                    context);
//
//            if (context.getMetadata().containsKey("testMode") || isMediated) {
//                variant.setSuccess(true);
//                variant.setScore(0.95);
//
//                if (!isMediated) {
//                    for (eu.kalafatic.evolution.model.orchestration.Task task : tasks) {
//                        try {
//                            variantManager.getTaskExecutor().getOrchestrator().executeTask(task, variantContext);
//                        } catch (Exception e) {
//                            context.log("[DARWIN] [TEST_MODE] Execution failed but continuing: " + e.getMessage());
//                        }
//                    }
//                } else {
//                    context.log("[DARWIN] Mediated Mode: Skipping task execution to prevent source modification.");
//                }
//
//                manager.updateVariantLifecycle(List.of(variant), variant.getId(),
//                        BranchVariant.ActivationState.VERIFIED, context);
//                manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.SCORING,
//                        context);
//
//                variant.setMutationTrace(isMediated ? "Cognitive evolution in mediated mode" : "Mocked in test mode");
//                return variantExecContext;
//            }
//
//            for (eu.kalafatic.evolution.model.orchestration.Task task : tasks) {
//                EvolutionProgressPublisher.updateBranchStatus(context, variant.getId(), variant.getStrategy(),
//                        "verifying", null);
//                boolean taskSuccess = variantManager.executeTasksWithRetries(List.of(task));
//                if (variantExecContext != null) {
//                    variantExecContext.getTasks().add(task);
//                }
//                if (!taskSuccess) {
//                    success = false;
//                    break;
//                }
//
//                manager.checkStep(task.getId(), "GIT_STAGING", "Staging changes for task: " + task.getName());
//
//                try {
//                    eu.kalafatic.evolution.controller.tools.GitTool gitTool = new eu.kalafatic.evolution.controller.tools.GitTool();
//                    String diffCommand = (baseCommit != null) ? "diff " + baseCommit + " HEAD" : "diff HEAD";
//                    
//                    String diff = gitTool.execute("diff HEAD", tempDir, variantContext);
//
//                    RuntimeEvent event = new RuntimeEvent(
//                            eu.kalafatic.evolution.controller.workflow.RuntimeEventType.TOOL_EXECUTION_SUCCEEDED,
//                            "DarwinEngine", "GitTool", diff);
//                    variantExecContext.recordEvent(event);
//
//                    eu.kalafatic.evolution.controller.supervision.ActivationResolver resolver = new eu.kalafatic.evolution.controller.supervision.ActivationResolver(
//                            context.getSemanticWorkspace().getTrajectoryMemory());
//                    eu.kalafatic.evolution.controller.supervision.DecisionSnapshot intermediateDecision = resolver
//                            .resolve(variantContext.getOrchestrationState().getCurrentIterationId(), List.of(variant),
//                                    getSessionContainer().getSignalBus().getSignalsForVariant(variant.getId()),
//                                    variantContext);
//
//                    Trajectory t = context.getSemanticWorkspace().getTrajectoryMemory()
//                            .getTrajectory(variant.getTrajectoryId());
//                    if (t != null) {
//                        double currentFitness = intermediateDecision.getAggregatedScores().getOrDefault(variant.getId(),
//                                0.5);
//                        t.setFitnessScore(currentFitness);
//                        t.getFitnessHistory().add(currentFitness);
//                        t.setStabilityScore(intermediateDecision.getAvgLongTermStability());
//                    }
//                } catch (Exception e) {
//                    context.log("[DARWIN] Error during dynamic re-evaluation for variant " + variant.getId() + ": "
//                            + e.getMessage());
//                }
//            }
//
//            if (success) {
//                variantManager.getGitManager().commit("Darwin Variant Execution: " + variant.getStrategy(),
//                        variantContext);
//                manager.updateVariantLifecycle(List.of(variant), variant.getId(),
//                        BranchVariant.ActivationState.VERIFIED, context);
//            } else {
//                manager.updateVariantLifecycle(List.of(variant), variant.getId(),
//                        BranchVariant.ActivationState.REJECTED, context);
//            }
//            variant.setSuccess(success);
//
//            EvaluationResult lightCheck = fitnessEngine.evaluateReality(tempDir, variantContext, RealityLevel.LIGHT,
//                    manager);
//            if (!lightCheck.isSuccess()) {
//                context.log("[DARWIN] Pragma A: LIGHT Reality Gate FAILED for " + variant.getId() + ": "
//                        + String.join("; ", lightCheck.getErrors()));
//                variant.setSuccess(false);
//                variant.setScore(0.1);
//                return variantExecContext;
//            }
//
//            EvaluationResult mediumCheck = fitnessEngine.evaluateReality(tempDir, variantContext, RealityLevel.MEDIUM,
//                    manager);
//            if (!mediumCheck.isSuccess()) {
//                context.log("[DARWIN] Pragma A: MEDIUM Reality Gate FAILED for " + variant.getId() + ": "
//                        + String.join("; ", mediumCheck.getErrors()));
//                variant.setSuccess(false);
//                variant.setScore(0.1);
//                return variantExecContext;
//            }
//
//            EvaluationResult result;
//            if (context.getExecutionProfile().shouldPerformRealityCheck()) {
//                result = manager.getFitnessEngine().evaluate(tempDir, variantContext, pressure);
//            } else {
//                result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
//                result.setSuccess(true);
//            }
//
//            variant.setSuccess(result.isSuccess());
//            if (result.isSuccess()) {
//                manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.SCORING,
//                        context);
//                EvolutionProgressPublisher.updateBranchStatus(context, variant.getId(), variant.getStrategy(),
//                        "scoring", null);
//
//                EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
//                EvolutionNode node = tree.getNode(variant.getId());
//                if (node != null) {
//                    for (BranchVariant.Action action : variant.getActions()) {
//                        if (("WRITE".equals(action.getOperation()) || "CREATE".equals(action.getOperation()))
//                                && action.getTarget() != null) {
//                            File file = new File(tempDir, action.getTarget());
//                            if (file.exists() && file.isFile()) {
//                                try {
//                                    String content = java.nio.file.Files.readString(file.toPath());
//                                    node.getCodeSnapshots().put(action.getTarget(), content);
//                                    action.setImplementation(content);
//                                } catch (Exception e) {
//                                    context.log("[DARWIN] Failed to read implemented file: " + action.getTarget());
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            eu.kalafatic.evolution.controller.tools.GitTool deltaTool = new eu.kalafatic.evolution.controller.tools.GitTool();
//            try {
//                String diffCommand = (baseCommit != null && !baseCommit.equals("null")) 
//                    ? "diff " + baseCommit + " HEAD" 
//                    : "diff HEAD";
//                variant.setMutationTrace(deltaTool.execute(diffCommand, tempDir, variantContext));
//            } catch (Exception e) {
//                context.log("[DARWIN] Failed to capture mutation trace: " + e.getMessage());
//            }
//            variant.setScore(fitnessEngine.calculateScore(result));
//
//            return variantExecContext;
//        } catch (Exception e) {
//            context.log("[DARWIN] Parallel evaluation failed for variant " + variant.getId() + ": " + e.getMessage());
//            variant.setSuccess(false);
//            variant.setScore(0.0);
//            variant.setErrorMessage(e.getMessage());
//            manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.REJECTED,
//                    context);
//            return variantExecContext;
//        } finally {
//            if (tempDir != null && !context.getMetadata().containsKey("testMode") && !isMediated) {
//                try {
//                    manager.getGitManager().removeWorktree(tempDir.getAbsolutePath());
//                } catch (Exception e) {
//                    context.log("[DARWIN] Worktree removal failed: " + e.getMessage());
//                }
//                try {
//                    deleteDirectory(tempDir);
//                } catch (Exception e) {
//                    context.log("[DARWIN] Temporary directory deletion failed: " + e.getMessage());
//                }
//            }
//        }
//    }
//
//    private void mergeHybridInsights(List<BranchVariant> variants, BranchVariant winner, TaskContext context) {
//        JSONArray analyticalInsights = new JSONArray();
//        JSONArray stabilizationInsights = new JSONArray();
//
//        for (BranchVariant v : variants) {
//            if (v.getId().equals(winner.getId()))
//                continue;
//
//            JSONObject insight = new JSONObject();
//            insight.put("strategy", v.getStrategy());
//            insight.put("risks", v.getFailureRisks());
//            insight.put("tradeoffs", v.getTradeoffs());
//
//            if ("ANALYTICAL".equals(v.getStrategyType())) {
//                analyticalInsights.put(insight);
//            } else if ("STABILIZATION".equals(v.getStrategyType())) {
//                stabilizationInsights.put(insight);
//            }
//        }
//
//        if (analyticalInsights.length() > 0) {
//            context.getOrchestrationState().getMetadata().put("hybrid_analytical_insights", analyticalInsights);
//            context.log("[DARWIN] Merged " + analyticalInsights.length() + " analytical insights into context.");
//        }
//        if (stabilizationInsights.length() > 0) {
//            context.getOrchestrationState().getMetadata().put("hybrid_stabilization_insights", stabilizationInsights);
//            context.log("[DARWIN] Merged " + stabilizationInsights.length() + " stabilization insights into context.");
//        }
//    }
//
//    private void deleteDirectory(File directory) {
//        File[] allContents = directory.listFiles();
//        if (allContents != null) {
//            for (File file : allContents)
//                deleteDirectory(file);
//        }
//        directory.delete();
//    }
//    
// // ============================================================
// // COMMON HELPER METHODS - Add to ADarwinEngine
// // ============================================================
//
// /**
//  * Generates a direct chat response.
//  * Available to all subclasses, especially ChatDarwinEngine.
//  */
// protected String generateChatResponse(String request) {
//     try {
//         String systemInstruction = 
//             "You are a friendly, helpful AI assistant. " +
//             "RESPOND CONVERSATIONALLY. DO NOT generate code. " +
//             "Just respond naturally as a helpful assistant.";
//         
//         String prompt = String.format(
//             "%s\n\nUser said: \"%s\"\n\nRespond naturally. Be friendly and helpful.",
//             systemInstruction, request
//         );
//         
//         return aiService.sendRequest(
//             context.getOrchestrator(),
//             prompt,
//             context
//         );
//     } catch (Exception e) {
//         context.log("[DARWIN] Chat response generation failed: " + e.getMessage());
//         return "Hello! How can I help you today?";
//     }
// }
//
// /**
//  * Gets the expansion value from the chat session.
//  * Available to all subclasses.
//  */
// protected int getExpansionValue() {
//     int expansionValue = 5;
//     if (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null) {
//         String sessionId = context.getSessionId();
//         eu.kalafatic.evolution.model.orchestration.ChatSession chatSession = 
//             context.getOrchestrator().getAiChat().getSessions().stream()
//                 .filter(s -> s.getId().equals(sessionId))
//                 .findFirst().orElse(null);
//         if (chatSession != null) {
//             expansionValue = chatSession.getExpansion();
//         }
//     }
//     return expansionValue;
// }
// 
////============================================================
////ABSTRACT METHODS - To be implemented by subclasses
////============================================================
//
///**
//* Handles mode-specific initialization before the evolution loop starts.
//*/
//protected abstract void initializeMode() throws Exception;
//
///**
//* Handles mode-specific cleanup after evolution completes.
//*/
//protected abstract void cleanupMode() throws Exception;
//
//
//
//    // ============================================================
//    // ICapability Implementation - Copied from DarwinEngine
//    // ============================================================
//
//    @Override
//    public String getCapabilityId() {
//        return "capability.mutation";
//    }
//
//    @Override
//    public String getVersion() {
//        return "1.0.0";
//    }
//
//    @Override
//    public CapabilityStatus getStatus() {
//        return status;
//    }
//
//    @Override
//    public void initialize(CapabilityContext context) throws CapabilityException {
//        status = CapabilityStatus.INITIALIZED;
//    }
//
//    @Override
//    public void start() throws CapabilityException {
//        status = CapabilityStatus.STARTED;
//    }
//
//    @Override
//    public void stop() throws CapabilityException {
//        status = CapabilityStatus.STOPPED;
//    }
//
//    @Override
//    public List<String> getSupportedContracts() {
//        return Collections.singletonList(IMutationContract.ID);
//    }
//
//    @Override
//    public List<String> getDependencies() {
//        return Collections.emptyList();
//    }
//
//    @Override
//    public CapabilityHealth getHealth() {
//        return new CapabilityHealth(1.0, "Healthy", 0);
//    }
//
//    // ============================================================
//    // BaseAiAgent Overrides - Copied from DarwinEngine
//    // ============================================================
//
//    @Override
//    protected String getAgentInstructions() {
//        return "Role: Darwin Engine. Strategy: Lineage-driven evolutionary mutation.\n" +
//               "EVOLUTIONARY MANDATE:\n" +
//               "- You are a materializer of architectural lineages.\n" +
//               "- You do NOT invent new dimensions or discover recursion depth.\n" +
//               "- You MUST materialize the EXACT blueprint provided by the orchestrator.\n" +
//               "- Preserve lineage continuity: every mutation MUST inherit from the surviving ancestor.\n" +
//               "- Address identified evolutionary pressures (reliability, extensibility, etc.) in your implementation.";
//    }
//
//    @Override
//    protected String getFooterInstructions() {
//        return "CRITICAL: Return a valid JSON object for the requested Darwin evolutionary trajectory.";
//    }
//
//    @Override
//    public void setAiService(eu.kalafatic.evolution.controller.orchestration.AiService aiService) {
//        super.setAiService(aiService);
//        rejectionAnalyzer.setAiService(aiService);
//    }
//}
