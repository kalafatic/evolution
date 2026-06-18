package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.AgentFactory;
import eu.kalafatic.evolution.controller.agents.AnalyticAgent;
import eu.kalafatic.evolution.controller.agents.CriticAgent;
import eu.kalafatic.evolution.controller.agents.FinalResponseAgent;
import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.agents.PlannerAgent;
import eu.kalafatic.evolution.controller.agents.RealityDiscoveryAgent;
import eu.kalafatic.evolution.controller.agents.StructureAgent;
import eu.kalafatic.evolution.controller.kernel.AuthorityEngine;
import eu.kalafatic.evolution.controller.kernel.BranchManager;
import eu.kalafatic.evolution.controller.kernel.DefaultAuthorityEngine;
import eu.kalafatic.evolution.controller.kernel.DefaultBranchManager;
import eu.kalafatic.evolution.controller.kernel.DefaultFitnessEngine;
import eu.kalafatic.evolution.controller.kernel.DefaultGitEvolutionAdapter;
import eu.kalafatic.evolution.controller.kernel.DefaultMutationEngine;
import eu.kalafatic.evolution.controller.kernel.DefaultPhaseEngine;
import eu.kalafatic.evolution.controller.kernel.DefaultRealityEngine;
import eu.kalafatic.evolution.controller.kernel.DefaultTrajectoryEngine;
import eu.kalafatic.evolution.controller.kernel.EvolutionaryPressureEngine;
import eu.kalafatic.evolution.controller.kernel.FitnessEngine;
import eu.kalafatic.evolution.controller.kernel.GitEvolutionAdapter;
import eu.kalafatic.evolution.controller.kernel.MutationEngine;
import eu.kalafatic.evolution.controller.kernel.PhaseEngine;
import eu.kalafatic.evolution.controller.kernel.RealityEngine;
import eu.kalafatic.evolution.controller.kernel.TrajectoryEngine;
import eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator;
import eu.kalafatic.evolution.controller.mediation.analysis.PromptSynthesizer;
import eu.kalafatic.evolution.controller.mediation.analysis.SemanticExtractor;
import eu.kalafatic.evolution.controller.mediation.model.ArchitecturalFact;
import eu.kalafatic.evolution.controller.mediation.model.Subsystem;
import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetScanner;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorProfile;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityRegistry;
import eu.kalafatic.evolution.controller.orchestration.capability.contracts.ISchedulingContract;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CognitiveTrace;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.ReplayEngine;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;
import eu.kalafatic.evolution.controller.orchestration.intent.ClarificationManager;
import eu.kalafatic.evolution.controller.orchestration.intent.ClarificationPlanner;
import eu.kalafatic.evolution.controller.orchestration.intent.DefaultDimensionInferenceEngine;
import eu.kalafatic.evolution.controller.orchestration.intent.DimensionInferenceEngine;
import eu.kalafatic.evolution.controller.orchestration.intent.EvolutionAssessment;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionEngine;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskExecutor;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceArtifact;
import eu.kalafatic.evolution.controller.supervision.EvolutionDecision;
import eu.kalafatic.evolution.controller.trajectory.EvolutionaryTrajectoryEngine;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.trajectory.TrajectoryAnalysisRecord;
import eu.kalafatic.evolution.controller.workflow.MediatedExportManager;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.controller.workflow.StepModeController;
import eu.kalafatic.evolution.controller.workflow.WorkflowStatus;
import eu.kalafatic.evolution.controller.workflow.WorkflowStep;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.IterationStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.PromptInstructions;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.SelfDevStatus;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;

/**
 * The Kernel Control Plane. Sole authority for state transitions and strategic orchestration.
 * Unified and refactored for architectural coherence.
 */
@EvolutionComponent(
    domain = "orchestration",
    role = "state-authority",
    purpose = "Single source of truth for kernel state transitions",
    stability = Stability.STABLE,
    evolutionaryImpact = EvolutionaryImpact.CRITICAL
)
public class IterationManager {

    private final TaskContext context;
    private SessionContainer sessionContainer;
    private final AiService aiService;
    private final GitManager gitManager;
    private final TaskPlanner taskPlanner;
    private final TaskExecutor taskExecutor;
    private final Evaluator evaluator;
    private final DarwinEngine darwinEngine;
    private final IterationMemoryService memoryService;

    // Kernel Components
    private final PhaseEngine phaseEngine;
    private final BranchManager branchManager;
    private final MutationEngine mutationEngine;
    private final FitnessEngine fitnessEngine;
    private final RealityEngine realityEngine;
    private final AuthorityEngine authorityEngine;
    private final TrajectoryEngine trajectoryEngine;
    private final EvolutionaryPressureEngine pressureEngine;
    private final GitEvolutionAdapter gitAdapter;
    private final ClarificationManager clarificationManager = new ClarificationManager();
    private final IntentExpansionEngine intentExpansionEngine;
    private final DimensionInferenceEngine dimensionInferenceEngine;
    private final ClarificationPlanner clarificationPlanner = new ClarificationPlanner();

    private final EvolutionaryTrajectoryEngine evolutionaryTrajectoryEngine = new EvolutionaryTrajectoryEngine();

    private final AnalyticAgent analyticAgent;
    private final RealityDiscoveryAgent realityDiscoveryAgent;
    private final StructureAgent structureAgent;
    private final PlannerAgent strategicPlanner;
    private final CriticAgent criticAgent;
    private final FinalResponseAgent finalResponseAgent;
    private final eu.kalafatic.evolution.controller.agents.ValidatorAgent validator;
    private final eu.kalafatic.evolution.controller.agents.RepairAgent repairAgent;
    private final List<IAgent> availableAgents = new ArrayList<>();

    private Iteration currentIterationModel;

    public TaskContext getContext() { return context; }
    public AiService getAiService() { return aiService; }
    public GitManager getGitManager() { return gitManager; }
    public TaskPlanner getTaskPlanner() { return taskPlanner; }
    public TaskExecutor getTaskExecutor() { return taskExecutor; }
    public Evaluator getEvaluator() { return evaluator; }
    public DarwinEngine getDarwinEngine() { return darwinEngine; }
    public PhaseEngine getPhaseEngine() { return phaseEngine; }
    public BranchManager getBranchManager() { return branchManager; }
    public MutationEngine getMutationEngine() { return mutationEngine; }
    public FitnessEngine getFitnessEngine() { return fitnessEngine; }
    public RealityEngine getRealityEngine() { return realityEngine; }
    public AuthorityEngine getAuthorityEngine() { return authorityEngine; }
    public TrajectoryEngine getTrajectoryEngine() { return trajectoryEngine; }
    public EvolutionaryPressureEngine getPressureEngine() { return pressureEngine; }
    public GitEvolutionAdapter getGitAdapter() { return gitAdapter; }
    public IntentExpansionEngine getIntentExpansionEngine() { return intentExpansionEngine; }
    public ClarificationPlanner getClarificationPlanner() { return clarificationPlanner; }
    public IterationMemoryService getMemoryService() { return memoryService; }
    public AnalyticAgent getAnalyticAgent() { return analyticAgent; }
    public FinalResponseAgent getFinalResponseAgent() { return finalResponseAgent; }
    public SessionContainer getSessionContainer() { return sessionContainer; }
    public eu.kalafatic.evolution.model.orchestration.Iteration getCurrentIterationModel() { return currentIterationModel; }


    public IterationManager(
            TaskContext context,
            eu.kalafatic.evolution.controller.orchestration.SessionContainer sessionContainer,
            AiService aiService,
            GitManager gitManager,
            TaskPlanner taskPlanner,
            TaskExecutor taskExecutor,
            Evaluator evaluator,
            DarwinEngine darwinEngine,
            IterationMemoryService memoryService) {
        this.context = context;
        this.sessionContainer = sessionContainer;
        this.aiService = aiService;
        this.gitManager = gitManager;
        this.taskPlanner = taskPlanner;
        this.taskExecutor = taskExecutor;
        this.evaluator = evaluator;
        this.darwinEngine = darwinEngine;
        this.memoryService = memoryService;

        if (sessionContainer != null) {
            Map<String, IAgent> registry = (sessionContainer instanceof SessionContext) ? ((SessionContext)sessionContainer).getAgentRegistry() : new java.util.HashMap<>();
            if (registry.isEmpty()) {
                List<IAgent> isolated = AgentFactory.createIsolatedAgents(sessionContainer);
                isolated.forEach(a -> registry.put(a.getType(), a));
            }
            availableAgents.addAll(registry.values());
        } else {
            throw new IllegalArgumentException("IterationManager: sessionContainer cannot be null. Session isolation is mandatory.");
        }

        analyticAgent = (AnalyticAgent) getInternalAgent(EvolutionConstants.AGENT_ANALYTIC);
        realityDiscoveryAgent = new RealityDiscoveryAgent(sessionContainer);
        realityDiscoveryAgent.setAiService(aiService);
        structureAgent = (StructureAgent) getInternalAgent(EvolutionConstants.AGENT_STRUCTURE);
        strategicPlanner = (PlannerAgent) getInternalAgent(EvolutionConstants.AGENT_PLANNER);
        criticAgent = (CriticAgent) getInternalAgent(EvolutionConstants.AGENT_CRITIC);
        finalResponseAgent = (FinalResponseAgent) getInternalAgent(EvolutionConstants.AGENT_FINAL_RESPONSE);
        validator = (eu.kalafatic.evolution.controller.agents.ValidatorAgent) getInternalAgent(EvolutionConstants.AGENT_VALIDATOR);
        repairAgent = (eu.kalafatic.evolution.controller.agents.RepairAgent) getInternalAgent(EvolutionConstants.AGENT_REPAIR);

        // RESTART CONTINUITY
        boolean resumed = false;
        if (context.getOrchestrator() != null && context.getOrchestrator().getSelfDevSession() != null) {
            SelfDevSession session = context.getOrchestrator().getSelfDevSession();
            if (session.getStatus() == SelfDevStatus.RUNNING && !session.getIterations().isEmpty()) {
                Iteration lastIter = session.getIterations().get(session.getIterations().size() - 1);
                this.currentIterationModel = lastIter;
                context.log("[KERNEL] Resuming from existing session: " + session.getId() + ", Iteration: " + lastIter.getId());

                if (lastIter.getPhase() != null) {
                    context.getOrchestrationState().setCurrentPhase(lastIter.getPhase());
                }
                resumed = true;
            }
        }

        if (!resumed && memoryService != null) {
            Checkpoint checkpoint = memoryService.loadCheckpoint(context.getSessionId());
            if (checkpoint != null) {
                context.log("[KERNEL] Found memory checkpoint for session: " + context.getSessionId());
                restoreStateFromCheckpoint(checkpoint);
                resumed = true;
            }
        }

        // Initialize Kernel Components
        this.phaseEngine = new DefaultPhaseEngine();
        this.branchManager = new DefaultBranchManager(gitManager);
        this.mutationEngine = new DefaultMutationEngine(darwinEngine);
        this.fitnessEngine = new DefaultFitnessEngine(evaluator, sessionContainer);
        this.realityEngine = new DefaultRealityEngine(context.getProjectRoot(), context);
        this.authorityEngine = new DefaultAuthorityEngine(context.getKernelContext().getAuthority());
        this.trajectoryEngine = new DefaultTrajectoryEngine(memoryService);
        this.pressureEngine = sessionContainer.getPressureEngine();
        this.gitAdapter = new DefaultGitEvolutionAdapter(gitManager);

        context.getKernelContext().setGitManager(gitManager);

        // Register Capabilities
        try {
            if (sessionContainer == null) {
                throw new IllegalStateException("IterationManager: sessionContainer is null. Cannot register capabilities.");
            }
            CapabilityRegistry reg = sessionContainer.getCapabilityRegistry();
            reg.register(evaluator);
            reg.register(darwinEngine);
            reg.register(context.getSemanticWorkspace());
            reg.register(context.getOrchestrationState().getCognitiveTrace());
            ISchedulingContract existingScheduler = reg.getContractImplementation(ISchedulingContract.ID, ISchedulingContract.class);
            if (existingScheduler == null) {
                reg.register(new eu.kalafatic.evolution.controller.execution.KernelScheduler());
            }
            if (memoryService != null) {
                reg.register(new eu.kalafatic.evolution.controller.supervision.ActivationResolver(memoryService.getTrajectoryMemory()));
            }
        } catch (CapabilityException e) {
            context.log("[KERNEL] Capability registration error: " + e.getMessage());
        }

        this.intentExpansionEngine = new IntentExpansionEngine(sessionContainer);
        this.intentExpansionEngine.setAiService(aiService);
        this.dimensionInferenceEngine = new DefaultDimensionInferenceEngine(intentExpansionEngine);

        // Inject AiService into agents
        availableAgents.forEach(a -> {
            if (a instanceof eu.kalafatic.evolution.controller.agents.BaseAiAgent) {
                ((eu.kalafatic.evolution.controller.agents.BaseAiAgent)a).setAiService(aiService);
            }
        });
        darwinEngine.setAiService(aiService);
        taskExecutor.getOrchestrator().setAiService(aiService);
    }

    public OrchestratorResponse handle(TaskRequest taskRequest) throws Exception {
        eu.kalafatic.evolution.controller.kernel.SessionBoundaryGuard.enterSession(context.getSessionId());
        try {
            return handleInternal(taskRequest);
        } finally {
            eu.kalafatic.evolution.controller.kernel.SessionBoundaryGuard.exitSession();
        }
    }

    private OrchestratorResponse handleInternal(TaskRequest taskRequest) throws Exception {
        context.setStartTime(Instant.now());
        String request = taskRequest.getPrompt();
        OrchestrationState state = context.getOrchestrationState();

        Map<String, Object> contextMap = taskRequest.getContext();
        if (contextMap != null) {
            state.getMetadata().putAll(contextMap);
        }

        if (context.getStateHolder().getState() == SystemState.EXECUTING && !context.getOrchestrator().getTasks().isEmpty()) {
            context.log("[KERNEL] Pre-populated tasks detected in EXECUTING state. Bypassing orchestration for direct execution.");
            boolean success = executeTasksWithRetries(context.getOrchestrator().getTasks());
            OrchestratorResponse bypassResponse = new OrchestratorResponse();
            bypassResponse.setResultType(ResultType.CHAT);
            bypassResponse.setSummary(success ? "Execution completed successfully." : "Execution failed.");
            transition(success ? SystemState.DONE : SystemState.FAILED, context);
            return bypassResponse;
        }

        transition(SystemState.INIT, context);

        String prompt = request.trim();
        boolean isControl = prompt.equalsIgnoreCase("yes") || prompt.equalsIgnoreCase("no") ||
                           prompt.toLowerCase().startsWith("select ") ||
                           prompt.toLowerCase().startsWith("approve variant ") ||
                           prompt.toLowerCase().startsWith("reject variant ") ||
                           prompt.toLowerCase().startsWith("keep variant ") ||
                           prompt.equalsIgnoreCase("force solution") ||
                           prompt.equalsIgnoreCase("approved") ||
                           prompt.equalsIgnoreCase("rejected") ||
                           prompt.equalsIgnoreCase("proceed");

        String checkpointGoal = (String) state.getMetadata().get("checkpoint_goal");
        if (isControl) {
            state.getMetadata().put("pendingControlCommand", prompt);
        }

        if (!isControl) {
            if (checkpointGoal != null && !checkpointGoal.equalsIgnoreCase(request)) {
                context.log("[KERNEL] New request detected. Invalidating stale evolution phase: " + state.getCurrentPhase());
                state.setCurrentPhase(null);
                state.setIterationCount(0);

                // Also reset trajectory lineage
                context.getKernelContext().getMemoryService().getRecords().clear();
            }
            state.setRawInput(request);
            state.getMetadata().put("checkpoint_goal", request);
        } else if (state.getRawInput() == null || state.getRawInput().isEmpty()) {
            state.setRawInput(request);
            state.getMetadata().put("checkpoint_goal", request);
        }

        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(ResultType.CHAT);

        BehaviorProfile profile = context.getBehaviorProfile();
        ModeRouter router = new ModeRouter();

        try {
            context.getOrchestrator().getTasks().clear();
            context.setCurrentTaskName("Initialization");
            context.log("[KERNEL] Strategic Initialization: " + request);

            ConversationState convState = ConversationState.load(context.getSharedMemory(), context.getSessionId());
            convState.addMessage("User: " + request);

            // Restore cognitive state from history if present
            if (convState.getCognitiveState() != null) {
                context.log("[KERNEL] Restoring cognitive state from conversation history.");
                sessionContainer.getCognitiveState().setCurrentCapability(convState.getCognitiveState().getCurrentCapability());
                sessionContainer.getCognitiveState().setCurrentIntent(convState.getCognitiveState().getCurrentIntent());
                sessionContainer.getCognitiveState().setCurrentDirection(convState.getCognitiveState().getCurrentDirection());
                sessionContainer.getCognitiveState().setConfidence(convState.getCognitiveState().getConfidence());
                sessionContainer.getCognitiveState().setCognitiveDepth(convState.getCognitiveState().getCognitiveDepth());
                sessionContainer.getCognitiveState().setVelocity(convState.getCognitiveState().getVelocity());
                sessionContainer.getCognitiveState().setAcceleration(convState.getCognitiveState().getAcceleration());
                sessionContainer.getCognitiveState().setDominantTrend(convState.getCognitiveState().getDominantTrend());
                sessionContainer.getCognitiveState().setTrendStability(convState.getCognitiveState().getTrendStability());
                sessionContainer.getCognitiveState().setTrajectory(new ArrayList<>(convState.getCognitiveState().getTrajectory()));
                sessionContainer.getCognitiveState().getCapabilityScores().putAll(convState.getCognitiveState().getCapabilityScores());
                sessionContainer.getCognitiveState().getCapabilityHistory().addAll(convState.getCognitiveState().getCapabilityHistory());
            }

            // 1. DISCOVERY phase
            if (!profile.hasTrait(BehaviorTrait.REASONING_ATOMIC)) {
                if (gitManager.isGitRepository()) {
                    transition(SystemState.ANALYZING, context);
                    context.log("[KERNEL] Discovery: Inspecting repository structure.");
                    String projectStructure = structureAgent.process("Provide a concise summary of the project structure and technology stack.", context, null);
                    state.getMetadata().put("projectStructure", projectStructure);

                    WorkspaceArtifact archArtifact = new WorkspaceArtifact("arch-summary-" + System.currentTimeMillis(), "architecture-summary");
                    archArtifact.setContent(projectStructure);
                    archArtifact.getSemanticTags().add("architecture");
                    archArtifact.getSemanticTags().add("structure");
                    context.getSemanticWorkspace().addArtifact(archArtifact);

                    // Formal Reality Discovery
                    context.log("[KERNEL] Discovery: Building semantic repository snapshot.");
                    TargetScanner scanner = new TargetScanner();
                    TargetSnapshot.TargetType type = context.getProjectRoot().getAbsolutePath().contains("evolution") ? TargetSnapshot.TargetType.SELF : TargetSnapshot.TargetType.PROJECT;
                    TargetSnapshot snapshot = scanner.scanToSnapshot(context.getProjectRoot(), type);

                    // TWO-STAGE SELECTION: Heuristic pick 32 candidates for deep analysis
                    ContextCurator curator = new ContextCurator();
                    List<String> candidates = curator.selectContext(snapshot, request, 32);

                    context.log("[KERNEL] Discovery: Selective deep analysis of " + candidates.size() + " high-signal candidates.");
                    SemanticExtractor extractor = new SemanticExtractor();
                    extractor.extractToSnapshot(snapshot, candidates);

                    state.getMetadata().put("mediatedSnapshot", snapshot);

                    // Construct formal TargetRealityModel
                    context.log("[KERNEL] Discovery: Formalizing Target Reality Model.");
                    eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel realityModel = realityDiscoveryAgent.discover(request, context, context.getProjectRoot().getAbsolutePath());
                    state.getMetadata().put("targetRealityModel", realityModel);

                    if (profile.hasTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY)) {
                        context.log("[KERNEL] Mediated Mode: Triggering MetadataAgent repository cognition.");
                        eu.kalafatic.evolution.controller.agents.MetadataAgent metadataAgent = new eu.kalafatic.evolution.controller.agents.MetadataAgent();
                        metadataAgent.generate(context.getProjectRoot());
                    }

                    context.getOrchestrationState().addDiagnostic("[OrchestrationTrace] Discovery complete. Target Reality Model initialized.");
                }
            }

            // 2. ANALYZING stage
            transition(SystemState.ANALYZING, context);
            if (gitManager.isGitRepository() || context.getMetadata().containsKey("testMode")) {
                gitManager.ensureInitialCommit();

                PromptInstructions instructions = (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null) ?
                        context.getOrchestrator().getAiChat().getPromptInstructions() : null;

                if (instructions != null && instructions.isGitAutomation()) {
                    String requestedBranch = (String) state.getMetadata().get("branch");
                    String branchName = (requestedBranch != null && !requestedBranch.isEmpty()) ?
                                         requestedBranch : "evo-" + context.getSessionId().substring(0, Math.min(context.getSessionId().length(), 8));

                    context.log("[KERNEL] Git Automation enabled. Creating/Switching to branch: " + branchName);
                    try {
                        if (!gitManager.getCurrentBranch().equals(branchName)) {
                            gitManager.createBranch(branchName);
                        }
                    } catch (Exception e) {
                        context.log("[KERNEL] Git Warning: Could not manage branch " + branchName + ": " + e.getMessage());
                    }
                }
            }

            if (context.getPlatformMode() == null) {
                PlatformMode mode = router.route(request, context.getOrchestrator());
                context.setPlatformMode(mode);
                context.log("Platform Mode: " + mode.getType());

            if (sessionContainer == null) {
                throw new IllegalStateException("IterationManager: sessionContainer is null. Cannot publish mode change event.");
            }
            RuntimeEventBus bus = sessionContainer.getEventBus();
                bus.publish(
                    new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                        eu.kalafatic.evolution.controller.workflow.RuntimeEventType.MODE_CHANGED,
                        context.getSessionId(), "Kernel", mode.getType().toString()));
            }

            if (profile.hasTrait(BehaviorTrait.COGNITIVE_SIMPLE_CHAT)) {
                PlatformMode fastMode = router.routeFast(request, context.getOrchestrator());
                if (fastMode != null && fastMode.getType() == PlatformType.SIMPLE_CHAT) {
                    context.log("[KERNEL] Fast-track greeting detected via cognitive trait. Bypassing evolutionary kernel.");
                    IOrchestrationFlow flow = (IOrchestrationFlow) getInternalAgent(EvolutionConstants.AGENT_GENERAL);
                    String resultStr = ((eu.kalafatic.evolution.controller.agents.GeneralAgent)flow).process(request, context, null);
                    response.setResultType(ResultType.CHAT);
                    response.setSummary(resultStr);
                    response.setContent(resultStr);
                    transition(SystemState.DONE, context);
                    FinalResponseAssembler assembler = new FinalResponseAssembler();
                    response.setFinalResponse(assembler.assemble(context, resultStr, true, context.getStartTime()));
                    return response;
                }
            }

            context.log("[KERNEL] Inspecting goal for unresolved semantic uncertainty.");
            EvolutionAssessment initialAssessment = dimensionInferenceEngine.analyze(request, context);
            if (initialAssessment.hasUnresolvedDimensions()) {
                context.log("[KERNEL] Unresolved dimensions detected: " +
                    initialAssessment.getUnresolvedDimensions().stream().map(d -> d.getId()).collect(Collectors.joining(", ")));
            } else {
                context.log("[KERNEL] No significant semantic uncertainty detected. Evolution will proceed with discovery grounding.");
            }

            context.log("[KERNEL] Routing to unified iterative evolutionary kernel.");
            OrchestratorResponse result = evolve(request, context, initialAssessment);

            boolean isError = result != null && result.getResultType() == ResultType.ERROR;

            if (isError) {
                transition(SystemState.FAILED, context);
            } else {
                // Final response handled inside evolve if terminal phase reached
                if (!context.getStateHolder().getState().equals(SystemState.DONE)) {
                    transition(SystemState.DONE, context);
                }
            }

            if (!isError) {
                PromptInstructions instructions = (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null) ?
                        context.getOrchestrator().getAiChat().getPromptInstructions() : null;

                if (instructions != null && instructions.isGitAutomation() && gitManager.isGitRepository()) {
                    context.log("[KERNEL] Git Automation: Committing changes.");
                    try {
                        gitManager.commit("Evolution Task: " + request.substring(0, Math.min(request.length(), 50)), context);
                    } catch (Exception e) {
                        context.log("[KERNEL] Git Warning: Could not commit changes: " + e.getMessage());
                    }
                }
            }

            FinalResponseAssembler assembler = new FinalResponseAssembler();
            FinalResponse finalResponse = assembler.assemble(context, result.getSummary(), !isError, context.getStartTime());
            result.setFinalResponse(finalResponse);

            return result;

        } catch (Exception e) {
            context.log("[KERNEL] [CRITICAL] Orchestration failed: " + e.getMessage());
            if (System.getProperty("evolution.test.debug") != null || context.getMetadata().containsKey("testMode")) {
                e.printStackTrace();
            }
            state.addDiagnostic("Critical error: " + e.getMessage());
            transition(SystemState.FAILED, context);

            FinalResponseAssembler assembler = new FinalResponseAssembler();
            FinalResponse finalResponse = assembler.assemble(context, "Error: " + e.getMessage(), false, context.getStartTime());
            OrchestratorResponse errorResponse = new OrchestratorResponse();
            errorResponse.setResultType(ResultType.ERROR);
            errorResponse.setFinalResponse(finalResponse);

            if (context.getMetadata().containsKey("testMode")) {
                throw e;
            }

            return errorResponse;
        }
    }

    public void checkStep(String entityId, String type, String description) throws Exception {
        if (context.getOrchestrator().getAiChat() != null &&
            context.getOrchestrator().getAiChat().getPromptInstructions() != null &&
            context.getOrchestrator().getAiChat().getPromptInstructions().isStepMode()) {

            WorkflowStep step = new WorkflowStep("step-" + System.currentTimeMillis(), entityId, type);
            step.setDescription(description);
            if (sessionContainer == null) {
                throw new IllegalStateException("IterationManager: sessionContainer is null. Cannot wait for step.");
            }
            StepModeController smc = (sessionContainer instanceof SessionContext) ? ((SessionContext)sessionContainer).getStepModeController() : null;
            if (smc == null) {
                throw new IllegalStateException("IterationManager: StepModeController is null.");
            }
            WorkflowStatus result = smc.waitForStep(context.getSessionId(), step, context);
            if (result == WorkflowStatus.FAILED) {
                throw new Exception("Step failed or rejected by user: " + description);
            }
        }
    }

    @Deprecated
    public static void forceTransition(SystemState to, TaskContext ctx) {
        ctx.getStateHolder().applyTransition(new TransitionToken(), to);
    }

    public void transition(SystemState to, TaskContext ctx) {
        if (sessionContainer == null) {
            throw new IllegalStateException("IterationManager: sessionContainer is null. Cannot transition state.");
        }
        sessionContainer.getStatusManager().updateStatus(ctx.getSessionId(), 0.0, to.toString());

        SystemState current = ctx.getStateHolder().getState();
        if (current == to) return;

        if (current == SystemState.DONE || current == SystemState.FAILED) {
            if (to != SystemState.INIT && to != SystemState.RECOVERING) {
                ctx.log("[KERNEL] Illegal state transition attempt: " + current + " -> " + to + ". Terminal states can only transition to INIT or RECOVERING.");
                return;
            }
        }

        TransitionToken token = new TransitionToken();
        SystemState from = ctx.getStateHolder().getState();
        ctx.getStateHolder().applyTransition(token, to);

        if (to == SystemState.INIT || to == SystemState.RECOVERING) {
            if (gitManager != null) {
                gitManager.cleanupLocks();
            }
        }

        if (currentIterationModel != null) {
            switch (to) {
                case DONE: currentIterationModel.setStatus(IterationStatus.DONE); break;
                case FAILED: currentIterationModel.setStatus(IterationStatus.FAILED); break;
                default: currentIterationModel.setStatus(IterationStatus.RUNNING); break;
            }
            String existingJustification = currentIterationModel.getJustification() != null ? currentIterationModel.getJustification() : "";
            if (!existingJustification.contains(to.toString())) {
                currentIterationModel.setJustification(existingJustification + "\n[STATE_TRANSITION] Reached phase: " + to);
            }
        }

        RuntimeEventBus bus = sessionContainer.getEventBus();
        bus.publish(
            new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                eu.kalafatic.evolution.controller.workflow.RuntimeEventType.SUPERVISOR_STATUS_CHANGED,
                ctx.getSessionId(), "Kernel", to.toString())
                .withMetadata("execId", ctx.getDeterministicExecutionId())
                .withMetadata("fromState", from != null ? from.toString() : "NONE")
                .withMetadata("toState", to.toString()));

        String logMsg = String.format("[KERNEL] [%s] [%d] [%d] Transition: %s -> %s", ctx.getDeterministicExecutionId(), System.currentTimeMillis(), Thread.currentThread().getId(), (current != null ? current : "NONE"), to);
        ctx.log(logMsg);
        ctx.getOrchestrationState().addDiagnostic("[OrchestrationTrace] " + logMsg);

        ctx.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
            "state-transition-" + System.currentTimeMillis(),
            "STATE_TRANSITION",
            "IterationManager",
            List.of(current != null ? current.toString() : "NONE"),
            List.of(to.toString()),
            1.0,
            "Transition to " + to
        ));
    }

    public OrchestratorResponse evolve(String request, TaskContext context) throws Exception {
        return evolve(request, context, null);
    }

    public OrchestratorResponse evolve(String request, TaskContext context, EvolutionAssessment initialAssessment) throws Exception {
        context.log("[KERNEL] Starting Recursive Evolutionary Cognition Loop.");

        sessionContainer.getEventBus().publish(new RuntimeEvent(RuntimeEventType.FLOW_STARTED, context.getSessionId(), "Kernel", request));

        OrchestrationState state = context.getOrchestrationState();
        state.getCognitiveTrace().addNode(new CausalNode(
            "evolution-start-" + System.currentTimeMillis(),
            "EVOLUTION_INIT",
            "IterationManager",
            List.of(),
            List.of("DarwinFlow"),
            1.0,
            "Recursive evolutionary cognition kernel active."
        ));

        EvaluationResult result = null;
        int safetyCounter = 0;
        DarwinFlow darwinFlow = new DarwinFlow(aiService, this);

        // expansion-based iteration limit
        int expansionValue = 5;
        if (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null) {
            String sessionId = context.getSessionId();
            eu.kalafatic.evolution.model.orchestration.ChatSession chatSession = context.getOrchestrator().getAiChat().getSessions().stream()
                .filter(s -> s.getId().equals(sessionId)).findFirst().orElse(null);
            if (chatSession != null) {
                expansionValue = chatSession.getExpansion();
            }
        }

        int maxIterationsLimit = 20; // Default Medium
        if (expansionValue <= 3) maxIterationsLimit = 10; // Conservative
        else if (expansionValue >= 8) maxIterationsLimit = 50; // Research/High

        context.log("[KERNEL] Dynamic Expansion Control: Target Max Iterations = " + maxIterationsLimit);

        // 1. Recursive Evolutionary Loop
        context.log("[KERNEL] Phase: Recursive Evolutionary Trajectory System.");
        while (safetyCounter < maxIterationsLimit && !context.isPaused()) {
            context.log("[KERNEL] [LOOP] Starting Iteration " + (safetyCounter + 1) + " (Phase: " + state.getCurrentPhase() + ")");

            // RECURSIVE ARCHITECTURAL DISCOVERY: Refine model in each iteration
            if (safetyCounter > 0) {
                refineTargetReality(request, context);
            }

            try {
                result = runDarwinIteration(context, darwinFlow);
            } catch (Exception e) {
                context.log("[KERNEL] [CRITICAL] Darwin iteration failed with exception: " + e.getMessage());
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                context.log(sw.toString());
                throw e;
            }
            safetyCounter++;

            // Evaluate Stability and Evolutionary Pressure
            Trajectory activeTrajectory = getActiveTrajectory(context);
            if (activeTrajectory != null && !isIntentExpansionPhase(context)) {
                boolean stabilized = evolutionaryTrajectoryEngine.evolve(activeTrajectory, context);
                if (stabilized) {
                    context.log("[KERNEL] [LOOP] Evolutionary equilibrium detected for trajectory " + activeTrajectory.getTrajectoryId() + ". Converging.");
                }
            }

            saveFullCheckpoint();

            if (result.getDecision() != SelfDevDecision.CONTINUE) {
                sessionContainer.getEventBus().publish(new RuntimeEvent(RuntimeEventType.FLOW_COMPLETED, context.getSessionId(), "Kernel", result.getDecision().toString()));
                break;
            }

            // If we reached a terminal phase during the iteration, break the loop
            if (state.getCurrentPhase().contains("TERMINAL")) {
                break;
            }
        }

        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(ResultType.CHAT);

        // FAILURE PROPAGATION: If the loop exited without reaching a terminal phase, it might be a failure
        if (result != null && !result.isSuccess()) {
            response.setResultType(ResultType.ERROR);
            context.log("[KERNEL] Evolution loop terminated due to iteration failure.");
        }

        String summary;
        if ((state.getCurrentPhase().contains("TERMINAL") || state.getCurrentPhase().contains("SYNTHESIS")) && response.getResultType() != ResultType.ERROR) {
            if (context.getBehaviorProfile().hasTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY)) {
                summary = performMediatedExportConvergence(request, context);
            } else if (context.getMetadata().containsKey("testMode")) {
                summary = "Evolution completed (Test Mode).";
            } else {
                summary = getFinalResponseAgent().generateFinalResponse(request, context.getOrchestrator().getTasks(), context);
            }
            transition(SystemState.DONE, context);
        } else {
            summary = "Evolution completed at phase: " + state.getCurrentPhase();
        }

        response.setSummary(summary);
        return response;
    }

    public EvaluationResult runIteration(Iteration iteration) {
        this.currentIterationModel = iteration;

        try {
            return runDarwinIteration(context, new DarwinFlow(aiService, this));
        } catch (Exception e) {
            context.log("[KERNEL] Critical error in iteration: " + e.getMessage());
            if (System.getProperty("evolution.test.debug") != null) {
                e.printStackTrace();
            }
            EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
            result.setSuccess(false);
            result.setDecision(SelfDevDecision.ROLLBACK);
            return result;
        }
    }

    public EvaluationResult failedResult() {
        EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        res.setSuccess(false);
        res.setDecision(SelfDevDecision.ROLLBACK);
        return res;
    }

    public void recordRejection(String goal, String message) {
        IterationRecord record = createBaseRecord(goal);
        record.setStrategy("Darwin Variant Selection");
        record.setResult("FAIL");
        record.setStatus("REJECTED");
        record.setErrorMessage(message);
        memoryService.saveRecord(record);
    }

    public void recordIterationResult(String goal, String strategy, String branchId, boolean success) {
        IterationRecord record = createBaseRecord(goal);
        record.setStrategy(strategy);
        record.setBranchId(branchId);
        record.setResult(success ? "SUCCESS" : "FAILURE");
        record.setStatus("COMPLETED");
        memoryService.saveRecord(record);
    }

    public void recordTrajectoryAnalysis(String iterId, String branchId, String strategy, double score) {
        TrajectoryAnalysisRecord tar = new TrajectoryAnalysisRecord();
        tar.setIterationId(iterId);
        tar.setBranchId(branchId);
        tar.setStrategy(strategy);
        tar.setFitnessScore(score);
        memoryService.saveTrajectoryAnalysis(tar);
        memoryService.flush();
    }

    public EvaluationResult runDarwinIteration(TaskContext context, DarwinFlow darwinFlow) throws Exception {
        SystemState currentState = context.getStateHolder().getState();
        if (currentState == SystemState.DONE || currentState == SystemState.FAILED) {
            transition(SystemState.INIT, context);
        }

        BehaviorProfile profile = context.getBehaviorProfile();
        OrchestrationState state = context.getOrchestrationState();
        String goal = state.getRawInput();
        if (goal == null || goal.isEmpty()) {
            goal = context.getOrchestrator().getSelfDevSession() != null ? context.getOrchestrator().getSelfDevSession().getInitialRequest() : "Autonomous Improvement";
        }

        EvolutionPhaseMachine phaseMachine = new EvolutionPhaseMachine();
        EvolutionPhase phase = state.getCurrentPhase() != null ? EvolutionPhase.fromString(state.getCurrentPhase()) : phaseMachine.getInitialPhase();

        state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(phase));
        if (currentIterationModel != null) {
            currentIterationModel.setPhase(state.getCurrentPhase());
        }

        context.log("[KERNEL] Darwin Evolution Phase: " + state.getCurrentPhase());

        Trajectory activeTrajectory = getActiveTrajectory(context);
        int generation = activeTrajectory != null ? activeTrajectory.getGeneration() : 0;
        String lineage = activeTrajectory != null ? activeTrajectory.getTrajectoryId() : "alpha";
        EvolutionProgressPublisher.startIteration(context, state.getIterationCount(), generation, lineage);

        if (phase == EvolutionPhase.INTENT_EXPANSION) {
            transition(SystemState.ANALYZING, context);
            IntentExpansionResult expansion = getIntentExpansionEngine().expand(goal, context);
            state.getMetadata().put("intentExpansion", expansion);

            context.consoleLog("[KERNEL] Intent Interpretation: " + expansion.getState());

            if (!handleIntentReview(context, expansion, goal)) {
                return failedResult();
            }

            ClarificationPlanner planner = getClarificationPlanner();
            ClarificationPlanner.Strategy strategy = planner.determineStrategy(expansion, context);
            context.consoleLog("[KERNEL] Clarification Strategy: " + strategy);

            boolean isStepMode = context.getOrchestrator().getAiChat() != null &&
                               context.getOrchestrator().getAiChat().getPromptInstructions() != null &&
                               context.getOrchestrator().getAiChat().getPromptInstructions().isStepMode();

            if (strategy == ClarificationPlanner.Strategy.CLARIFY_USER && !isStepMode) {
                if (context.isAutoApprove()) {
                    context.log("[KERNEL] AUTO Mode: Downgrading CLARIFY_USER to AUTO_INFER.");
                    strategy = ClarificationPlanner.Strategy.AUTO_INFER;
                } else {
                    context.log("[KERNEL] MANUAL Mode: Downgrading CLARIFY_USER to BRANCH_PARALLEL for evolutionary steering.");
                    strategy = ClarificationPlanner.Strategy.BRANCH_PARALLEL;
                }
            }

            if (strategy == ClarificationPlanner.Strategy.CLARIFY_USER) {
                if (!handleClarification(context, planner, expansion, goal)) {
                    return failedResult();
                }
                EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                res.setSuccess(true);
                res.setDecision(SelfDevDecision.CONTINUE);
                return res;
            }

            // Progression from INTENT_EXPANSION is now unified.
            // Dimension discovery and pressure injection are handled in the following architectural phases.
            EvolutionPhase nextPhase = evolutionaryTrajectoryEngine.determineNextPhase(phase, null, context);
            state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(nextPhase));

            context.log("[KERNEL] Intent grounding complete. Progressing to " + nextPhase);
            state.setIterationCount(state.getIterationCount() + 1);

            EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
            res.setSuccess(true);
            res.setDecision(SelfDevDecision.CONTINUE);
            return res;
        }

        Object intentExpansionObj = state.getMetadata().get("intentExpansion");
        IntentExpansionResult intentExpansionFinal = null;
        if (intentExpansionObj instanceof IntentExpansionResult) {
            intentExpansionFinal = (IntentExpansionResult) intentExpansionObj;
        } else if (intentExpansionObj instanceof Map) {
            intentExpansionFinal = new com.fasterxml.jackson.databind.ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .convertValue(intentExpansionObj, IntentExpansionResult.class);
            state.getMetadata().put("intentExpansion", intentExpansionFinal);
        }

        final IntentExpansionResult intentExpansion = intentExpansionFinal;

        checkStep(state.getCurrentPhase(), "BRANCH_GENERATION", "Spawning competing trajectories for: " + goal);
        List<BranchVariant> variants = darwinFlow.generateProposals(context, goal);

        if (variants.isEmpty()) {
            context.log("[KERNEL] CRITICAL: No trajectories survived diversity analysis. Evolution blocked.");
            return failedResult();
        }

        String manualId = null;

        if (state.getMetadata().containsKey("pendingControlCommand")) {
            String pendingCommand = (String) state.getMetadata().remove("pendingControlCommand");
            if (pendingCommand.toLowerCase().startsWith("select ") || pendingCommand.toLowerCase().startsWith("approve variant ")) {
                manualId = pendingCommand.toLowerCase().startsWith("select ") ? pendingCommand.substring(7).trim() : pendingCommand.substring(16).trim();
                context.log("[KERNEL] Auto-resolving variant selection from initial command: " + manualId);
            } else if (pendingCommand.equalsIgnoreCase("approved") || pendingCommand.equalsIgnoreCase("proceed") || pendingCommand.equalsIgnoreCase("yes") || pendingCommand.equalsIgnoreCase("force solution")) {
                manualId = variants.stream()
                        .max((v1, v2) -> Double.compare(v1.getScore(), v2.getScore()))
                        .map(v -> v.getId())
                        .orElse(null);
                context.log("[KERNEL] Auto-approving best variant from initial command: " + manualId);
            }
        }

        if (variants.size() < 4 && state.getIterationCount() < 2) {
            context.log("[KERNEL] EVOLUTIONARY MANDATE: Attempting to maximize vector diversity within capability envelope.");
        }

        if (manualId == null && !context.isAutoApprove()) {
            manualId = handleVariantSelection(context, variants, goal);
            if ("REGENERATE".equals(manualId)) {
                return runDarwinIteration(context, darwinFlow);
            }
            if (manualId == null || "STOP".equals(manualId) || "FAILED".equals(manualId)) {
                EvaluationResult res = failedResult();
                res.setDecision(SelfDevDecision.STOP);
                return res;
            }
        }

        String iterId = currentIterationModel != null ? currentIterationModel.getId() : "default";
        eu.kalafatic.evolution.controller.supervision.EvolutionDecision decision = decide(iterId, variants, context, manualId);

        // Propagate pressure to decision for consistent evaluation      
        if (activeTrajectory != null) {
            decision.setPressure(pressureEngine.analyze(activeTrajectory, context));
        }

        if ("force solution".equalsIgnoreCase(manualId)) {
            context.log("[KERNEL] Committing selected trajectory via Force Solution.");
        }

        EvaluationResult result = darwinFlow.executeWinner(context, decision, variants, goal);

        if (result.isSuccess()) {
            EvolutionPhase currentPhaseEnum = EvolutionPhase.fromString(state.getCurrentPhase());

            EvolutionPhase nextPhase = evolutionaryTrajectoryEngine.determineNextPhase(currentPhaseEnum, getActiveTrajectory(context), context);
            state.setIterationCount(state.getIterationCount() + 1);

            if (nextPhase == currentPhaseEnum) {
                context.log("[KERNEL] Evolution continuing in current phase: " + nextPhase + " (Generation: " + state.getIterationCount() + ")");
            } else {
                context.log("[KERNEL] Evolution transitioning to phase: " + nextPhase + " (Generation: " + state.getIterationCount() + ")");
            }

            state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(nextPhase));
            result.setDecision(phaseMachine.determineDecision(nextPhase));

            if (!handlePhaseConfirmation(context, state)) {
                result.setDecision(SelfDevDecision.STOP);
            }

            EvolutionProgressPublisher.completeIteration(context);
            transition(SystemState.DONE, context);
        } else {
            EvolutionProgressPublisher.completeIteration(context);
            transition(SystemState.FAILED, context);
        }

        return result;
    }

    private boolean hasStateChangeIntent(TaskContext context) {
        OrchestrationState state = context.getOrchestrationState();
        return state.getTaskIntents() != null && (
                state.getTaskIntents().contains(eu.kalafatic.evolution.controller.orchestration.attachments.TaskIntent.IMPLEMENTATION) ||
                state.getTaskIntents().contains(eu.kalafatic.evolution.controller.orchestration.attachments.TaskIntent.REFACTORING) ||
                state.getTaskIntents().contains(eu.kalafatic.evolution.controller.orchestration.attachments.TaskIntent.DEBUGGING) ||
                state.getTaskIntents().contains(eu.kalafatic.evolution.controller.orchestration.attachments.TaskIntent.TESTING) ||
                state.getTaskIntents().contains(eu.kalafatic.evolution.controller.orchestration.attachments.TaskIntent.OPTIMIZATION)
        );
    }

    private boolean handleIntentReview(TaskContext context, IntentExpansionResult expansion, String goal) throws Exception {
        boolean isStepMode = context.getOrchestrator().getAiChat() != null &&
                           context.getOrchestrator().getAiChat().getPromptInstructions() != null &&
                           context.getOrchestrator().getAiChat().getPromptInstructions().isStepMode();

        if (!context.isAutoApprove() && isStepMode) {
            while (true) {
                context.log("[COGNITION] Intent Interpretation: Pausing for semantic review.");
                transition(SystemState.CLARIFYING, context);
                String userResponse = context.requestInput("Intent interpretation complete. State: " + expansion.getState() + ". Review and select a hypothesis to proceed, or reject to refine.").get();

                if ("Force Solution".equalsIgnoreCase(userResponse)) {
                    context.log("[KERNEL] Force Solution requested. Enabling auto-approval and final convergence.");
                    context.getOrchestrationState().getMetadata().put("forceSolution", true);
                    context.setAutoApprove(true);
                    return true;
                } else if ("No".equalsIgnoreCase(userResponse) || "Reject".equalsIgnoreCase(userResponse) || "Rejected".equalsIgnoreCase(userResponse)) {
                    recordRejection(goal, "User rejected intent interpretation.");
                    transition(SystemState.FAILED, context);
                    return false;
                }

                String selectedHypothesisId = null;
                if (userResponse.startsWith("Select ")) {
                    selectedHypothesisId = userResponse.substring(7).trim();
                } else if (userResponse.startsWith("Approve variant ")) {
                    selectedHypothesisId = userResponse.substring(16).trim();
                }

                if (selectedHypothesisId != null) {
                    context.log("[KERNEL] User selected hypothesis: " + selectedHypothesisId);
                    String finalId = selectedHypothesisId;
                    boolean found = expansion.getHypotheses().stream()
                        .filter(h -> h.getId().equals(finalId))
                        .findFirst()
                        .map(h -> {
                            expansion.setDominantIntent(h.getDescription());
                            expansion.setDominantConfidence(1.0);
                            return true;
                        }).orElse(false);
                    if (found) return true;
                    context.log("[KERNEL] Warning: Selected hypothesis ID not found: " + finalId);
                } else if (userResponse.equalsIgnoreCase("Yes") || userResponse.equalsIgnoreCase("Approved") || userResponse.equalsIgnoreCase("Proceed")) {
                    return true;
                }
            }
        }
        return true;
    }

    private boolean handleClarification(TaskContext context, ClarificationPlanner planner, IntentExpansionResult expansion, String goal) throws Exception {
        String clarificationRequest = planner.formatClarificationRequest(expansion);
        context.log(clarificationRequest);
        String userResponse = context.requestInput(clarificationRequest).get();
        String trimmed = (userResponse != null) ? userResponse.trim() : "";

        if ("Rejected".equalsIgnoreCase(trimmed)) {
            recordRejection(goal, "User rejected clarification request.");
            transition(SystemState.FAILED, context);
            return false;
        }

        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("Approved") || trimmed.equalsIgnoreCase("Proceed") || trimmed.equalsIgnoreCase("Yes") || trimmed.equalsIgnoreCase("OK")) {
            context.log("[KERNEL] User approved intent expansion.");
            return true;
        } else {
            String newGoal = goal + " (Clarification: " + userResponse + ")";
            context.getOrchestrationState().setRawInput(newGoal);
            if (context.getOrchestrator().getSelfDevSession() != null) {
                 context.getOrchestrator().getSelfDevSession().setInitialRequest(newGoal);
            }
            return true;
        }
    }

    private boolean handlePhaseConfirmation(TaskContext context, OrchestrationState state) {
        boolean isStepModeConfirmation = context.getOrchestrator().getAiChat() != null &&
                                       context.getOrchestrator().getAiChat().getPromptInstructions() != null &&
                                       context.getOrchestrator().getAiChat().getPromptInstructions().isStepMode();

        String nextPhase = state.getCurrentPhase();

        if (!context.isAutoApprove() && isStepModeConfirmation) {
            context.log("[COGNITION] Evolutionary Phase: Generation completed. Proceeding to: " + nextPhase);
            try {
                String userResponse = context.requestInput("Phase completed successfully. Proceed to " + nextPhase + "? (Yes/No)").get();
                if ("Force Solution".equalsIgnoreCase(userResponse)) {
                    context.log("[KERNEL] Force Solution requested. Enabling auto-approval and final convergence.");
                    context.getOrchestrationState().getMetadata().put("forceSolution", true);
                    context.setAutoApprove(true);
                    return true;
                } else if ("No".equalsIgnoreCase(userResponse) || "Reject".equalsIgnoreCase(userResponse)) {
                    context.log("[KERNEL] User stopped evolution.");
                    return false;
                }
            } catch (Exception e) {
                context.log("[KERNEL] Error during phase confirmation: " + e.getMessage());
            }
        }
        return true;
    }

    public String handleVariantSelection(TaskContext context, List<BranchVariant> variants, String goal) throws Exception {
        while (true) {
            transition(SystemState.AWAITING_BRANCH_SELECTION, context);
            context.log("[COGNITION] Trajectory Competition: Pausing for semantic selection (Manual Mode).");

            StringBuilder sb = new StringBuilder("Darwin evolved " + variants.size() + " trajectories for your review:\n");
            for (BranchVariant v : variants) {
                String status = (v.getActivationState() == BranchVariant.ActivationState.KEPT) ? " [KEPT]" : "";
                sb.append(String.format("- [%s] %s (Predicted Score: %.2f)%s\n", v.getId(), v.getStrategy(), v.getScore(), status));
            }
            sb.append("\nMANUAL MODE: ALL branches preserved. No auto-collapse.\n");
            sb.append("Select a trajectory to execute (e.g. 'Select v0'), Keep to save, or Reject to stop.");

            String input = context.requestInput(sb.toString()).get();
            String trimmed = (input != null) ? input.trim() : "";

            if (trimmed.isEmpty() || "Approved".equalsIgnoreCase(trimmed) || "Yes".equalsIgnoreCase(trimmed) || "Proceed".equalsIgnoreCase(trimmed) || "OK".equalsIgnoreCase(trimmed)) {
                context.log("[KERNEL] User approved best trajectory via fast-approval.");
                return variants.stream()
                        .max((v1, v2) -> Double.compare(v1.getScore(), v2.getScore()))
                        .map(v -> v.getId())
                        .orElse(null);
            }

            if ("Force Solution".equalsIgnoreCase(trimmed)) {
                context.log("[KERNEL] Force Solution requested. Picking best variant and enabling final convergence.");
                context.getOrchestrationState().getMetadata().put("forceSolution", true);
                context.setAutoApprove(true);

                // Return best variant to proceed with execution immediately
                return variants.stream()
                        .max((v1, v2) -> Double.compare(v1.getScore(), v2.getScore()))
                        .map(v -> v.getId())
                        .orElse(null);
            }

            if (trimmed.startsWith("Select ") || trimmed.startsWith("Approve variant ")) {
                String manualId = trimmed.startsWith("Select ") ? trimmed.substring(7).trim() : trimmed.substring(16).trim();
                boolean found = variants.stream().anyMatch(v -> v.getId().equals(manualId));
                if (found) {
                    context.log("[KERNEL] User selected trajectory: " + manualId);
                    emitDarwinBranches(context, variants, manualId);
                    return manualId;
                } else {
                    context.log("[KERNEL] Warning: Selected trajectory ID not found: " + manualId);
                }
            } else if (trimmed.startsWith("Keep variant ")) {
                String keepId = trimmed.substring(13).trim();
                variants.stream().filter(v -> v.getId().equals(keepId)).findFirst().ifPresent(v -> {
                    v.setActivationState(BranchVariant.ActivationState.KEPT);
                    context.log("[KERNEL] Trajectory " + keepId + " marked as KEPT for final evaluation.");
                    emitDarwinBranches(context, variants, null);
                });
            } else if (trimmed.startsWith("Reject variant ")) {
                String rejectedId = trimmed.substring(15).trim();
                variants.stream().filter(v -> v.getId().equals(rejectedId)).findFirst().ifPresent(v -> {
                    v.setActivationState(BranchVariant.ActivationState.REJECTED);
                    context.log("[KERNEL] Trajectory " + rejectedId + " rejected by user.");
                    sessionContainer.getEvolutionMemoryGraph().recordRejection("MANUAL_SELECTION", rejectedId, "User rejected explicitly.");
                    emitDarwinBranches(context, variants, null);
                });
            } else if ("Rejected".equalsIgnoreCase(trimmed) || "Reject".equalsIgnoreCase(trimmed) || "No".equalsIgnoreCase(trimmed)) {
                recordRejection(goal, "Darwin trajectories rejected by user.");
                sessionContainer.getEvolutionMemoryGraph().recordEntropy(1.0);
                return "FAILED";
            } else if (trimmed.startsWith("Propose:") || trimmed.startsWith("{")) {
                context.log("[KERNEL] User injected a new trajectory. Integrating as a first-class candidate.");
                BranchVariant userVariant = createUserVariant(trimmed, goal, context);
                variants.add(userVariant);
                context.log("[KERNEL] User trajectory " + userVariant.getId() + " added to the evolutionary pool.");
            } else {
                context.log("[KERNEL] User provided guidance: " + trimmed + ". Refining intent and regenerating trajectories.");
                String newGoal = goal + " (Guidance: " + trimmed + ")";
                context.getOrchestrationState().setRawInput(newGoal);
                if (context.getOrchestrator().getSelfDevSession() != null) {
                     context.getOrchestrator().getSelfDevSession().setInitialRequest(newGoal);
                }
                return "REGENERATE";
            }
        }
    }

    private BranchVariant createUserVariant(String input, String goal, TaskContext context) {
        BranchVariant v = new BranchVariant();
        v.setId("v-user-" + System.currentTimeMillis());
        v.setBranchId(v.getId());
        v.setLineageId(context.getSessionId());
        v.setActivationState(BranchVariant.ActivationState.ARCHIVED);
        v.setStrategyType("USER_PROPOSAL");

        String strategyText = input.startsWith("Propose:") ? input.substring(8).trim() : input;

        if (strategyText.trim().startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(strategyText);
                v.setStrategy(obj.optString("strategy", "User-defined strategy"));
                v.setSurvivalArgument(obj.optString("survival_argument", "User injection"));
                v.setTradeoffs(obj.optString("tradeoffs", "Explicit user directive"));
            } catch (Exception e) {
                v.setStrategy(strategyText);
            }
        } else {
            v.setStrategy(strategyText);
            v.setSurvivalArgument("Direct user proposal");
            v.setTradeoffs("User-defined trajectory");
        }

        v.setScore(0.95);
        v.setBranchName("exp/user/" + sanitizeForBranch(v.getStrategy()));

        Trajectory t = new Trajectory(v.getId(), v.getStrategy());
        t.setFitnessScore(v.getScore());
        v.setTrajectoryId(t.getTrajectoryId());

        if (context.getKernelContext().getMemoryService().getTrajectoryMemory() != null) {
            context.getKernelContext().getMemoryService().getTrajectoryMemory().recordTrajectory(t);
        }

        return v;
    }

    private boolean isIntentExpansionPhase(TaskContext context) {
        String phaseStr = context.getOrchestrationState().getCurrentPhase();
        return EvolutionPhase.INTENT_EXPANSION.name().equals(phaseStr) || "INTENT_EXPANSION".equals(phaseStr);
    }

    private void emitDarwinBranches(TaskContext context, List<BranchVariant> variants, String approvedId) {
        StringBuilder outcomeBuilder = new StringBuilder("[DARWIN_BRANCHES] ");
        if (approvedId != null) {
            outcomeBuilder.append("[APPROVED:").append(approvedId).append("] ");
        }

        for (BranchVariant v : variants) {
            if (v.getId().equals(approvedId)) continue;
            String status = (v.getActivationState() == BranchVariant.ActivationState.KEPT) ? "KEPT" :
                            (v.getActivationState() == BranchVariant.ActivationState.REJECTED) ? "REJECTED" : "PENDING";

            if (!"PENDING".equals(status)) {
                outcomeBuilder.append("[").append(status).append(":").append(v.getId()).append("] ");
            }
        }
        outcomeBuilder.append("[DECISION:MANUAL]");
        context.log(outcomeBuilder.toString());
    }

    private String sanitizeForBranch(String s) {
        if (s == null || s.isEmpty()) return "unnamed";
        String sanitized = s.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-");
        if (sanitized.startsWith("-")) sanitized = sanitized.substring(1);
        if (sanitized.endsWith("-")) sanitized = sanitized.substring(0, sanitized.length() - 1);
        if (sanitized.isEmpty()) return "unnamed";
        return sanitized.substring(0, Math.min(sanitized.length(), 30));
    }

    private IterationRecord createBaseRecord(String goal) {
        IterationRecord record = new IterationRecord();
        int iterNum = 0;
        try {
            if (currentIterationModel != null) {
                iterNum = Integer.parseInt(currentIterationModel.getId().replace("iteration-", ""));
            } else if (context.getOrchestrationState() != null) {
                iterNum = context.getOrchestrationState().getIterationCount();
            }
        } catch (Exception e) {}
        record.setIteration(iterNum);
        record.setGoal(goal);
        record.setTimestamp(System.currentTimeMillis());
        return record;
    }

    public IOrchestrationFlow resolveFlow(ModeRouter router, AtomicIntentAnalysis atomicAnalysis) {
        eu.kalafatic.evolution.controller.kernel.RuntimeInvariant.checkSession(context.getSessionId(), "IterationManager.resolveFlow");
        return new eu.kalafatic.evolution.controller.orchestration.DarwinFlow(aiService, this);
    }

    public void replayIteration(CognitiveTrace trace) {
        ReplayEngine engine = new ReplayEngine();
        engine.replay(trace, this, context);
    }

    public IterationManager createVariantManager(TaskContext variantContext, AiService aiService) {
        return KernelFactory.create(variantContext, sessionContainer, aiService);
    }

    private Trajectory getActiveTrajectory(TaskContext context) {
        IterationRecord lastWinner = context.getKernelContext().getMemoryService().getRecords().stream()
                .filter(r -> "ACTIVE".equals(r.getActivationState()))
                .reduce((first, second) -> second)
                .orElse(null);

        if (lastWinner != null && lastWinner.getBranchId() != null) {
             return context.getSemanticWorkspace().getTrajectoryMemory().getTrajectory(lastWinner.getBranchId());
        }
        return null;
    }

    public void advanceEvolutionPhase(OrchestrationState state) {
        EvolutionPhase current = EvolutionPhase.fromString(state.getCurrentPhase());
        EvolutionPhase next = evolutionaryTrajectoryEngine.determineNextPhase(current, getActiveTrajectory(context), context);
        state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(next));
    }

    private void saveFullCheckpoint() {
        if (memoryService == null) return;

        OrchestrationState state = context.getOrchestrationState();
        Checkpoint cp = new Checkpoint();
        cp.setSessionId(context.getSessionId());
        cp.setCurrentPhase(state.getCurrentPhase());
        cp.setRawInput(state.getRawInput());
        cp.setIterationCount(state.getIterationCount());
        cp.setMetadata(state.getMetadata());
        cp.setChangedFiles(context.getFileChangeTracker().getChangedFiles());
        cp.setActiveLineage(memoryService.getActiveLineage());
        cp.setCurrentIterationId(currentIterationModel != null ? currentIterationModel.getId() : state.getCurrentIterationId());
        cp.setArtifacts(context.getSemanticWorkspace().getAllArtifacts());
        cp.setCognitiveTraceNodes(state.getCognitiveTrace().getNodes());
        cp.setRejectedBranches(memoryService.getEvolutionGraph().getRejectedBranches());
        cp.setRationales(memoryService.getEvolutionGraph().getRationales());
        cp.setEntropyHistory(memoryService.getEvolutionGraph().getEntropyHistory());
        cp.setDimensions(memoryService.getEvolutionGraph().getDimensions());
        cp.setConvergenceReasoning(memoryService.getEvolutionGraph().getConvergenceReasoning());
        cp.setGlobalPressureHistory(memoryService.getEvolutionGraph().getGlobalPressureHistory());

        cp.setFailureFingerprints(memoryService.getFailureMemory().getFingerprints());
        cp.setStrategyFailures(memoryService.getFailureMemory().getStrategyFailures());
        cp.setMutationEffectiveness(memoryService.getFailureMemory().getMutationEffectiveness());

        cp.setAllRecords(memoryService.getRecords());
        cp.setArchitectureHotspots(memoryService.getArchitectureHotspots());

        cp.setTrajectories(context.getSemanticWorkspace().getTrajectoryMemory().getTrajectories());

        Object lastSnapshot = state.getMetadata().get("lastSnapshot");
        if (lastSnapshot instanceof StateSnapshot) {
            cp.setLastSnapshot((StateSnapshot) lastSnapshot);
        }

        memoryService.saveCheckpoint(cp);
    }

    private void restoreStateFromCheckpoint(Checkpoint cp) {
        OrchestrationState state = context.getOrchestrationState();
        state.setCurrentPhase(cp.getCurrentPhase());
        state.setRawInput(cp.getRawInput());
        state.setIterationCount(cp.getIterationCount());

        if (cp.getMetadata() != null) {
            state.getMetadata().putAll(cp.getMetadata());
        }

        if (cp.getChangedFiles() != null) {
            context.getFileChangeTracker().restore(cp.getChangedFiles());
        }

        if (cp.getCurrentIterationId() != null) {
            state.setCurrentIterationId(cp.getCurrentIterationId());
        }

        if (cp.getArtifacts() != null) {
            cp.getArtifacts().forEach(a -> context.getSemanticWorkspace().addArtifact(a));
        }

        if (cp.getCognitiveTraceNodes() != null) {
            cp.getCognitiveTraceNodes().forEach(node -> state.getCognitiveTrace().addNode(node));
        }

        if (memoryService.getEvolutionGraph() != null) {
            memoryService.getEvolutionGraph().restore(
                    cp.getDimensions(),
                    cp.getRejectedBranches(),
                    cp.getRationales(),
                    cp.getEntropyHistory(),
                    cp.getConvergenceReasoning(),
                    cp.getGlobalPressureHistory()
            );
        }

        if (memoryService.getFailureMemory() != null) {
            memoryService.getFailureMemory().restore(
                    cp.getFailureFingerprints(),
                    cp.getStrategyFailures(),
                    cp.getMutationEffectiveness()
            );
        }

        if (cp.getAllRecords() != null) {
            cp.getAllRecords().forEach(r -> {
                boolean exists = memoryService.getRecords().stream()
                        .anyMatch(existing -> r.getBranchId() != null && r.getBranchId().equals(existing.getBranchId()));
                if (!exists) {
                    memoryService.getRecords().add(r);
                }
            });
        }

        if (cp.getArchitectureHotspots() != null) {
            memoryService.getArchitectureHotspots().putAll(cp.getArchitectureHotspots());
        }

        if (cp.getTrajectories() != null) {
            cp.getTrajectories().forEach((id, t) -> {
                Trajectory traj = t;
                if (t instanceof Map) {
                    traj = new com.fasterxml.jackson.databind.ObjectMapper().convertValue(t, Trajectory.class);
                }
                context.getSemanticWorkspace().getTrajectoryMemory().recordTrajectory(traj);
            });
        }

        if (cp.getLastSnapshot() != null) {
            state.getMetadata().put("lastSnapshot", cp.getLastSnapshot());
        }

        if (cp.getActiveLineage() != null) {
            for (IterationRecord r : cp.getActiveLineage()) {
                boolean exists = memoryService.getRecords().stream()
                        .anyMatch(existing -> r.getBranchId() != null && r.getBranchId().equals(existing.getBranchId()));
                if (!exists) {
                    memoryService.getRecords().add(r);
                }
            }
        }

        context.log("[KERNEL] Runtime continuity restored. Resuming at phase: " + cp.getCurrentPhase());

        if (sessionContainer == null) {
            throw new IllegalStateException("IterationManager: sessionContainer is null. Cannot publish session resumed event.");
        }
        RuntimeEventBus bus = sessionContainer.getEventBus();
        bus.publish(new RuntimeEvent(RuntimeEventType.SESSION_RESUMED, context.getSessionId(), "Kernel", cp));
    }


    private void refineTargetReality(String goal, TaskContext context) throws Exception {
        context.log("[KERNEL] Recursive Discovery: Refining Target Reality Model based on new iteration evidence.");
        TargetSnapshot snapshot = (TargetSnapshot) context.getOrchestrationState().getMetadata().get("mediatedSnapshot");
        TargetRealityModel existingModel = (TargetRealityModel) context.getOrchestrationState().getMetadata().get("targetRealityModel");

        if (snapshot == null || existingModel == null) return;

        // Recursive Reconstruction Loop: iterate discovery until completeness threshold or convergence
        double lastCompleteness = existingModel.getRealityCompleteness();
        int pass = 1;
        while (pass <= 3 && existingModel.getRealityCompleteness() < 0.85) {
            context.log("[KERNEL] Discovery Loop Pass " + pass + " (Completeness: " + existingModel.getRealityCompleteness() + ")");

            // Targeted Discovery driven by Knowledge Gaps and coverage scores
            existingModel = realityDiscoveryAgent.discover(goal, context, snapshot.getRootPath(), existingModel);
            context.getOrchestrationState().getMetadata().put("targetRealityModel", existingModel);

            if (Math.abs(existingModel.getRealityCompleteness() - lastCompleteness) < 0.05) {
                context.log("[KERNEL] Discovery converged.");
                break;
            }
            lastCompleteness = existingModel.getRealityCompleteness();
            pass++;
        }
    }

    public void mergeArchitecturalDiscovery(BranchVariant winner, TaskContext context) {
        if (winner.getMediationCandidate() == null) return;

        TargetRealityModel model = (TargetRealityModel) context.getOrchestrationState().getMetadata().get("targetRealityModel");
        if (model == null) return;

        context.log("[KERNEL] Merging architectural discovery from winning variant into Target Reality Model.");
        int iteration = context.getOrchestrationState().getIterationCount();

        for (ArchitecturalFact fact : winner.getMediationCandidate().getArchitecturalFacts()) {
            fact.setDiscoveryIteration(iteration);
            model.addArchitecturalFact(fact);
        }

        for (Subsystem sub : winner.getMediationCandidate().getSubsystems()) {
            sub.setDiscoveryIteration(iteration);
            model.addSubsystem(sub);
        }

        // Merge discovered genes
        for (eu.kalafatic.evolution.controller.mediation.model.ArchitecturalGene gene : winner.getMediationCandidate().getGenes()) {
            gene.setDiscoveryIteration(iteration);
            model.addGene(gene);
        }

        // Merge Knowledge Gaps
        for (eu.kalafatic.evolution.controller.mediation.model.KnowledgeGap gap : winner.getMediationCandidate().getKnowledgeGaps()) {
            model.addKnowledgeGap(gap);
        }
    }

    private String performMediatedExportConvergence(String request, TaskContext context) {
        try {
            context.log("[KERNEL] Mediated Mode: Converging understanding into export package.");
            checkStep(context.getSessionId(), "MEDIATION_EXPORT", "Preparing final repository-grounded cognition export.");

            Object snapshotObj = context.getOrchestrationState().getMetadata().get("mediatedSnapshot");
            TargetSnapshot snapshot = null;
            if (snapshotObj instanceof TargetSnapshot) {
                snapshot = (TargetSnapshot) snapshotObj;
            } else if (snapshotObj instanceof Map) {
                // RESILIENCE: Restore snapshot from Map if it came from a JSON checkpoint
                try {
                    snapshot = new com.fasterxml.jackson.databind.ObjectMapper()
                        .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .convertValue(snapshotObj, TargetSnapshot.class);
                } catch (Exception e) {
                    context.log("[KERNEL] Warning: Failed to restore mediatedSnapshot from Map: " + e.getMessage());
                }
            }

            if (snapshot == null) {
                context.log("[KERNEL] Mediated Mode: Building fresh semantic repository snapshot.");
                TargetScanner scanner = new TargetScanner();
                TargetSnapshot.TargetType type = context.getProjectRoot().getAbsolutePath().contains("evolution") ? TargetSnapshot.TargetType.SELF : TargetSnapshot.TargetType.PROJECT;
                snapshot = scanner.scanToSnapshot(context.getProjectRoot(), type);

                // Heuristic pick 32 candidates for analysis
                ContextCurator curator = new ContextCurator();
                List<String> candidates = curator.selectContext(snapshot, request, 32);

                context.log("[KERNEL] Mediated Mode: Selective deep analysis of " + candidates.size() + " high-signal candidates.");
                SemanticExtractor extractor = new SemanticExtractor();
                extractor.extractToSnapshot(snapshot, candidates);
                context.getOrchestrationState().getMetadata().put("mediatedSnapshot", snapshot);
            }

            Object winningCandidateObj = context.getOrchestrationState().getMetadata().get("winningMediationCandidate");
            eu.kalafatic.evolution.controller.mediation.model.MediationCandidate winningCandidate = null;
            if (winningCandidateObj instanceof eu.kalafatic.evolution.controller.mediation.model.MediationCandidate) {
                winningCandidate = (eu.kalafatic.evolution.controller.mediation.model.MediationCandidate) winningCandidateObj;
            } else if (winningCandidateObj != null) {
                // RESILIENCE: Handle candidate restored from JSON checkpoint (Map or JSONObject)
                try {
                    winningCandidate = new com.fasterxml.jackson.databind.ObjectMapper()
                        .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE)
                        .convertValue(winningCandidateObj, eu.kalafatic.evolution.controller.mediation.model.MediationCandidate.class);
                } catch (Exception e) {
                    context.log("[KERNEL] Warning: Failed to restore winningMediationCandidate using Jackson: " + e.getMessage());
                    // Fallback to manual Map extraction if Jackson fails
                    if (winningCandidateObj instanceof Map) {
                        Map<String, Object> map = (Map<String, Object>) winningCandidateObj;
                        winningCandidate = new eu.kalafatic.evolution.controller.mediation.model.MediationCandidate();
                        winningCandidate.setPrompt((String) map.get("prompt"));
                        winningCandidate.setArchitectureSummary((String) (map.containsKey("architecture_summary") ? map.get("architecture_summary") : map.get("architectureSummary")));
                        winningCandidate.setDependencies((String) map.get("dependencies"));
                        winningCandidate.setExecutionInstructions((String) (map.containsKey("execution_instructions") ? map.get("execution_instructions") : map.get("executionInstructions")));
                        winningCandidate.setEvaluation((String) map.get("evaluation"));
                        Object files = map.get("selectedFiles");
                        if (files == null) files = map.get("selected_files");
                        if (files instanceof List) {
                            winningCandidate.setSelectedFiles((List<String>) files);
                        }
                    }
                }
            }

            List<String> selectedPaths;
            String optimizedPrompt;
            String architectureSummary = null;
            String dependencies = null;
            String executionInstructions = null;

            if (winningCandidate != null) {
                context.log("[KERNEL] Mediated Mode: Using evolved mediation candidate.");
                selectedPaths = new ArrayList<>();
                if (winningCandidate.getSelectedFiles() != null) {
                    for (String p : winningCandidate.getSelectedFiles()) {
                         if (p != null && !p.isEmpty()) selectedPaths.add(p);
                    }
                }

                // Ensure context completeness: If LLM failed to select enough files, fall back to curation
                if (selectedPaths.size() < 4 && snapshot != null) {
                    context.log("[KERNEL] Mediated Mode: Evolved candidate contains insufficient context (" + selectedPaths.size() + " files). Supplementing with curated files.");
                    ContextCurator curator = new ContextCurator();
                    List<String> curated = curator.selectContext(snapshot, request, 16);
                    for (String path : curated) {
                        if (path != null && !selectedPaths.contains(path)) selectedPaths.add(path);
                    }
                }

                // Final safety limit
                if (selectedPaths.size() > 16) {
                    selectedPaths = selectedPaths.subList(0, 16);
                }

                optimizedPrompt = winningCandidate.getPrompt();
                architectureSummary = winningCandidate.getArchitectureSummary();
                dependencies = winningCandidate.getDependencies();
                executionInstructions = winningCandidate.getExecutionInstructions();
            } else {
                context.log("[KERNEL] Mediated Mode: No evolved mediation candidate found. Falling back to static curation and synthesis.");
                Object currentSelected = context.getOrchestrationState().getMetadata().get("current_selected_files");
                if (currentSelected instanceof List) {
                    selectedPaths = (List<String>) currentSelected;
                } else {
                    selectedPaths = new ArrayList<>();
                }

                if ((selectedPaths == null || selectedPaths.isEmpty()) && snapshot != null) {
                    ContextCurator curator = new ContextCurator();
                    selectedPaths = curator.selectContext(snapshot, request, 16);
                }
                Object understanding = context.getOrchestrationState().getMetadata().get("current_understanding");
                Object focus = context.getOrchestrationState().getMetadata().get("current_reasoning_focus");
                String evolvedUnderstanding = understanding != null ? understanding.toString() : "";
                String evolvedReasoningFocus = focus != null ? focus.toString() : "";
                PromptSynthesizer synthesizer = new PromptSynthesizer();
                optimizedPrompt = synthesizer.synthesizeOptimized(request, snapshot, selectedPaths, evolvedUnderstanding + "\n\nREASONING FOCUS: " + evolvedReasoningFocus);
            }

            if (!context.isAutoApprove()) {
                context.log("[KERNEL] Mediated Mode: Pausing for final export package review.");
                boolean approved = context.requestApproval("Final review: Ready to generate export package with " + selectedPaths.size() + " files?").get();
                if (!approved) return "Export cancelled by user.";
            }

            String sessionId = context.getSessionId();
            String outputPath = null;
            if (context.getOrchestrator().getAiChat() != null) {
                ChatSession session = context.getOrchestrator().getAiChat().getSessions().stream()
                        .filter(s -> s != null && s.getId() != null && s.getId().equals(sessionId))
                        .findFirst().orElse(null);
                outputPath = session != null ? session.getOutputPath() : null;
            }

            MediatedExportManager exportManager = new MediatedExportManager();

            Object realityModelObj = context.getOrchestrationState().getMetadata().get("targetRealityModel");

            // DRIVE UNIFIED PROJECTIONS
            TargetRealityModel model = (TargetRealityModel) realityModelObj;
            context.log("[KERNEL] Mediated Mode: Generating Unified Reality Projections.");

            // POPULATE NEW FIELDS FROM METADATA/HISTORY
            if (model != null) {
                model.getMetadata().put("sessionId", sessionId);
                Object impacts = context.getOrchestrationState().getMetadata().get("impactPaths");
                if (impacts instanceof List) model.getImpactPaths().addAll((List<String>) impacts);

                Object lessons = context.getOrchestrationState().getMetadata().get("lessons");
                if (lessons instanceof List) model.getLessons().addAll((List<String>) lessons);

                Object patterns = context.getOrchestrationState().getMetadata().get("patterns");
                if (patterns instanceof List) model.getPatterns().addAll((List<String>) patterns);

                Object refs = context.getOrchestrationState().getMetadata().get("referenceImplementations");
                if (refs instanceof List) model.getReferenceImplementations().addAll((List<String>) refs);

                // AUTHORITY VS FRONTIER POPULATION
                model.getArchitecturalAuthorityFiles().addAll(model.getSelectedFiles());

                Object frontier = context.getOrchestrationState().getMetadata().get("implementationFrontier");
                if (frontier instanceof List) {
                    model.getImplementationFrontierFiles().addAll((List<String>) frontier);
                } else {
                    // Fallback: frontier = authority
                    model.getImplementationFrontierFiles().addAll(model.getSelectedFiles());
                }

                // RECONSTRUCTION FIELDS
                Object flows = context.getOrchestrationState().getMetadata().get("executionFlows");
                if (flows instanceof List) model.getExecutionFlows().addAll((List<String>) flows);

                Object decisions = context.getOrchestrationState().getMetadata().get("decisionFlows");
                if (decisions instanceof List) model.getDecisionFlows().addAll((List<String>) decisions);

                Object influence = context.getOrchestrationState().getMetadata().get("influenceGraph");
                if (influence instanceof Map) model.getInfluenceGraph().putAll((Map<String, Double>) influence);
            }

            File exportPackage = exportManager.createUnifiedExport(model, MediatedExportManager.ExportProfile.FULL, request, context.getProjectRoot(), outputPath, context.getSessionId());

            // Record as a change so it appears in Changes view
            context.getFileChangeTracker().recordChange(exportPackage.getName(), FileChangeTracker.ChangeType.NEW);

            StringBuilder summaryBuilder = new StringBuilder();
            summaryBuilder.append("### Mediated Darwin Evolution Complete\n\n");

            String packageUri = exportPackage.toURI().toString();
            summaryBuilder.append("**Unified Export Package (ZIP):** [").append(exportPackage.getName()).append("](").append(packageUri).append(")\n");
            summaryBuilder.append("**Selected Files:** ").append(selectedPaths.size()).append(" (Limit: 16)\n\n");
            summaryBuilder.append("**Target Type:** ").append(snapshot.getTargetType()).append("\n");
            summaryBuilder.append("**Inferred Architecture:** ").append(snapshot.getMetadata().get("architectureInference")).append("\n\n");

            summaryBuilder.append("#### Evolutionary Lineage Analysis\n");
            summaryBuilder.append(memoryService.getHistoryAnalysis()).append("\n\n");

            summaryBuilder.append("**Optimized Prompt Sample:**\n\n");
            if (optimizedPrompt != null && optimizedPrompt.length() > 500) {
                summaryBuilder.append(optimizedPrompt.substring(0, 500)).append("...\n");
            } else {
                summaryBuilder.append(optimizedPrompt);
            }

            return summaryBuilder.toString();
        } catch (Exception e) {
            context.log("[KERNEL] Mediated Export Failed: " + e.getMessage());
            return "Mediated Export Failed: " + e.getMessage();
        }
    }


    public boolean executeTasksWithRetries(List<Task> tasks) throws Exception {
        return executeTasksWithRetries(tasks, null);
    }

    public boolean executeTasksWithRetries(List<Task> tasks, Runnable onStepComplete) throws Exception {
        OrchestrationState state = context.getOrchestrationState();
        for (Task task : tasks) {
            if (task.getStatus() == eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE) continue;
            boolean success = false;
            for (int retry = 1; retry <= EvolutionConstants.MAX_TASK_RETRIES; retry++) {
                state.addDiagnostic("[OrchestrationTrace] Executing task: " + task.getName() + " (Attempt " + retry + ")");
                context.checkPause();
                transition(SystemState.EXECUTING, context);
                task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.RUNNING);

                if (sessionContainer == null) {
                    throw new IllegalStateException("IterationManager: sessionContainer is null. Cannot publish task started event.");
                }
                RuntimeEventBus bus = sessionContainer.getEventBus();
                bus.publish(
                    new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                        eu.kalafatic.evolution.controller.workflow.RuntimeEventType.TASK_STARTED,
                        context.getSessionId(), "Kernel", task.getId()));

                checkStep(task.getId(), "TASK_EXECUTION", "Executing task: " + task.getName());

                String result = taskExecutor.getOrchestrator().executeTask(task, context);
                transition(SystemState.VERIFYING, context);
                task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.VERIFYING);
                ChangeUnit change = new ChangeUnit();
                change.setPatch(task.getResponse());

                checkStep(task.getId(), "PATCH_GENERATION", "Patch generated for task: " + task.getName());

                JSONObject evaluation = validator.evaluate(change, task.getName(), context);
                if (evaluation.optBoolean("success", false)) {
                    task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE);
                    state.addDiagnostic("[OrchestrationTrace] Task " + task.getName() + " succeeded.");

                    bus.publish(
                        new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                            eu.kalafatic.evolution.controller.workflow.RuntimeEventType.TASK_COMPLETED,
                            context.getSessionId(), "Kernel", task.getId()));

                    success = true;
                    if (onStepComplete != null) {
                        onStepComplete.run();
                    }
                    break;
                } else if (retry < EvolutionConstants.MAX_TASK_RETRIES) {
                    state.addDiagnostic("[OrchestrationTrace] Task " + task.getName() + " failed attempt " + retry + ". Diagnosing...");
                    transition(SystemState.ANALYZING, context);
                    analyticAgent.diagnose(result, evaluation.optString("feedback"), context);
                    transition(SystemState.MUTATING, context);
                } else {
                    state.addDiagnostic("[OrchestrationTrace] Task " + task.getName() + " failed after max retries.");
                    task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.FAILED);
                }
            }
            if (!success) return false;
        }
        return true;
    }

    public void updateVariantLifecycle(List<BranchVariant> variants, String targetId, BranchVariant.ActivationState newState, TaskContext context) {
        authorityEngine.updateLifecycle(variants, targetId, newState, context);
    }

    public EvolutionDecision decide(String iterationId, List<BranchVariant> variants, TaskContext context, String manualSelectionId) {
        EvolutionProgressPublisher.updateStage(context, EvolutionStage.SELECT_WINNER);
        EvolutionDecision decision = authorityEngine.decide(iterationId, variants, context, manualSelectionId);
        applyDecision(decision, variants, context);

        // UI SYNC: Emit centralized [DARWIN_BRANCHES] message for variant status updates
        sessionContainer.getEventBus().publish(new RuntimeEvent(RuntimeEventType.DECISION_UPDATED, context.getSessionId(), manualSelectionId, iterationId, "Kernel", decision.getSelectedVariantId(), System.currentTimeMillis()));

        StringBuilder outcomeBuilder = new StringBuilder("[DARWIN_BRANCHES] ");
        String winnerId = decision.getSelectedVariantId();
        outcomeBuilder.append("[APPROVED:").append(winnerId).append("] ");

        for (BranchVariant v : variants) {
            if (v.getId().equals(winnerId)) continue;

            // Survival Rule: If not winner and not manually kept, it is explicitly REJECTED
            String status = (v.getActivationState() == BranchVariant.ActivationState.KEPT) ? "KEPT" : "REJECTED";

            // Force state update for consistent UI stamping
            if (!"KEPT".equals(status)) {
                v.setActivationState(BranchVariant.ActivationState.REJECTED);
            }

            outcomeBuilder.append("[").append(status).append(":").append(v.getId()).append("] ");
        }

        // Ensure decision type and variant metadata are logged for visual stamping
        String decisionType = (manualSelectionId != null) ? "MANUAL" : "AUTO";
        outcomeBuilder.append("[DECISION:").append(decisionType).append("]");

        // STAMPING MANDATE: Always emit branch statuses for the UI to render stamps correctly
        context.log(outcomeBuilder.toString());

        return decision;
    }

    private void applyDecision(EvolutionDecision decision, List<BranchVariant> variants, TaskContext context) {
        String winnerId = decision.getSelectedVariantId();

        for (BranchVariant variant : variants) {
            if (variant.getId().equals(winnerId)) {
                updateVariantLifecycle(variants, variant.getId(), BranchVariant.ActivationState.ACTIVE, context);
                variant.setRank("winner");
            } else {
                BranchVariant.ActivationState newState = (variant.getActivationState() == BranchVariant.ActivationState.KEPT) ? BranchVariant.ActivationState.KEPT : BranchVariant.ActivationState.REJECTED;
                updateVariantLifecycle(variants, variant.getId(), newState, context);
                variant.setRank(decision.getRejectedVariantIds().contains(variant.getId()) ? "runner-up" : "noise");
            }
        }
    }

    private IAgent getInternalAgent(String type) {
        if (sessionContainer != null) {
            Map<String, IAgent> registry = sessionContainer.getAgentRegistry();
            return registry.get(type);
        }
        return null;
    }

    public void updateVariantFromInput(List<BranchVariant> variants, String input) {
        try {
            String[] lines = input.split("\n");
            if (lines.length == 0) return;
            String firstLine = lines[0].trim();
            if (!firstLine.startsWith("EDIT PROPOSAL ")) return;
            int firstColon = firstLine.indexOf(":");
            if (firstColon == -1) return;
            String variantId = firstLine.substring("EDIT PROPOSAL ".length(), firstColon).trim();
            String strategy = firstLine.substring(firstColon + 1).trim();
            BranchVariant target = variants.stream().filter(v -> v.getId().equals(variantId)).findFirst().orElse(null);
            if (target != null) {
                target.setStrategy(strategy);
                context.log("[KERNEL] Updated variant " + variantId + " from manual edit.");
            }
        } catch (Exception e) {}
    }
}
