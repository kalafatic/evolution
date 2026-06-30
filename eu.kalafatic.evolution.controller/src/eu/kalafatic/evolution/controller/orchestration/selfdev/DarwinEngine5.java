//package eu.kalafatic.evolution.controller.orchestration.selfdev;
//
//import java.util.List;
//
//import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
//import eu.kalafatic.evolution.controller.orchestration.IterationManager;
//import eu.kalafatic.evolution.controller.orchestration.OrchestrationState;
//import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
//import eu.kalafatic.evolution.controller.orchestration.SessionManager;
//import eu.kalafatic.evolution.controller.orchestration.TaskContext;
//import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
//import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
//import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityContext;
//import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException;
//import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityHealth;
//import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityStatus;
//import eu.kalafatic.evolution.controller.orchestration.capability.ICapability;
//import eu.kalafatic.evolution.controller.orchestration.capability.contracts.IMutationContract;
//import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
//import eu.kalafatic.evolution.controller.trajectory.Trajectory;
//import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
//
///**
// * DarwinEngine - Facade/Delegate for the polymorphic Darwin engine system.
// * 
// * This class maintains backward compatibility with existing code while
// * delegating to the appropriate engine implementation (Chat, Task, Mediated, SelfDev).
// * 
// * The engine selection is based on:
// * 1. Intent analysis (chat vs task)
// * 2. Behavior profile traits (WORKFLOW_EXPORT_ONLY, WORKFLOW_SELF_DEV, etc.)
// * 3. Context metadata flags (isChatRequest, etc.)
// */
//public class DarwinEngine5 extends BaseAiAgent implements ICapability, IMutationContract {
//
//    private final ADarwinEngine delegate;
//    private final TaskContext context;
//    
//    public DarwinEngine5(TaskContext context, IterationMemoryService memoryService,
//                        SystemStateSignalProvider stateProvider) {
//        super("DarwinEngine", "DarwinEngine", SessionManager.getInstance().getSession(context.getSessionId()));
//        this.context = context;
//        
//        // Determine mode from context
//        String mode = determineMode(context);
//        this.delegate = DarwinEngineFactory.createEngine(mode, context, memoryService, stateProvider);
//        this.delegate.setAiService(aiService);
//        context.log("[DARWIN] Delegating to " + delegate.getClass().getSimpleName());
//    }
//    
//    /**
//     * Determines the appropriate engine mode based on context.
//     */
//    private String determineMode(TaskContext context) {
//        OrchestrationState state = context.getOrchestrationState();
//        
//        // Check if chat flag is set
//        if (state.getMetadata().containsKey("isChatRequest") &&
//            (boolean) state.getMetadata().get("isChatRequest")) {
//            return "CHAT";
//        }
//        
//        // Check behavior profile for mediated mode
//        if (context.getBehaviorProfile().hasTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY)) {
//            return "MEDIATED";
//        }
//        
//        // Check behavior profile for self-dev mode
//        if (context.getBehaviorProfile().hasTrait(BehaviorTrait.WORKFLOW_SELF_DEV) ||
//            context.getBehaviorProfile().hasTrait(BehaviorTrait.SELF_DEVELOPMENT)) {
//            return "SELFDEV";
//        }
//        
//        // Check if self-iterative mode is enabled
//        if (context.getOrchestrator().getAiChat() != null &&
//            context.getOrchestrator().getAiChat().getPromptInstructions() != null &&
//            context.getOrchestrator().getAiChat().getPromptInstructions().isSelfIterativeMode()) {
//            return "SELFDEV";
//        }
//        
//        // Default to TASK
//        return "TASK";
//    }
//
//    // ============================================================
//    // DELEGATE METHODS - IDarwinEngine
//    // ============================================================
//    
//    /**
//     * Main orchestration method - delegates to the appropriate engine.
//     */
//    public OrchestratorResponse orchestrateEvolution(TaskRequest taskRequest, 
//                                                       IterationManager iterationManager) throws Exception {
//        return delegate.orchestrateEvolution(taskRequest, iterationManager);
//    }
//    
//    /**
//     * Evolution loop - delegates to the appropriate engine.
//     * Uses the 2-parameter evolve method from ADarwinEngine.
//     */
//    public OrchestratorResponse evolve(String request, IterationManager iterationManager,
//                                        eu.kalafatic.evolution.controller.orchestration.intent.EvolutionAssessment assessment)
//                                        throws Exception {
//        // ADarwinEngine.evolve takes (String, IterationManager, EvolutionAssessment)
//        return delegate.evolve(request, iterationManager, assessment);
//    }
//    
//    /**
//     * Darwin iteration - delegates to the appropriate engine.
//     * Uses runDarwinIteration from ADarwinEngine.
//     */
//    public EvaluationResult runDarwinIteration(TaskContext context, IterationManager manager) throws Exception {
//        // ADarwinEngine.runDarwinIteration takes (TaskContext, IterationManager)
//        return delegate.runDarwinIteration(context, manager);
//    }
//    
//    /**
//     * Generates proposals - delegates to the appropriate engine.
//     */
//    public List<BranchVariant> generateProposals(TaskContext context, GoalModel goal, 
//                                                   IterationManager manager) throws Exception {
//        return delegate.generateProposals(context, goal, manager);
//    }
//    
//    /**
//     * Executes winner - delegates to the appropriate engine.
//     */
//    public EvaluationResult executeWinner(TaskContext context,
//                                           eu.kalafatic.evolution.controller.supervision.EvolutionDecision decision,
//                                           List<BranchVariant> variants,
//                                           GoalModel goal,
//                                           IterationManager manager) throws Exception {
//        return delegate.executeWinner(context, decision, variants, goal, manager);
//    }
//
//    // ============================================================
//    // IMutationContract Implementation - Delegates to appropriate engine
//    // ============================================================
//    
//    @Override
//    public List<BranchVariant> generateVariants(GoalModel goal, StateSnapshot snapshot, 
//                                                 FailureMemory failureMemory, 
//                                                 Trajectory trajectory, 
//                                                 EvolutionaryPressureVector pressure) throws Exception {
//        return delegate.generateVariants(goal, snapshot, failureMemory, trajectory, pressure);
//    }
//
//    // ============================================================
//    // ICapability Implementation - Delegates to appropriate engine
//    // ============================================================
//    
//    @Override
//    public String getCapabilityId() {
//        return delegate.getCapabilityId();
//    }
//    
//    @Override
//    public String getVersion() {
//        return delegate.getVersion();
//    }
//    
//    @Override
//    public CapabilityStatus getStatus() {
//        return delegate.getStatus();
//    }
//    
//    @Override
//    public void initialize(CapabilityContext context) throws CapabilityException {
//        delegate.initialize(context);
//    }
//    
//    @Override
//    public void start() throws CapabilityException {
//        delegate.start();
//    }
//    
//    @Override
//    public void stop() throws CapabilityException {
//        delegate.stop();
//    }
//    
//    @Override
//    public List<String> getSupportedContracts() {
//        return delegate.getSupportedContracts();
//    }
//    
//    @Override
//    public List<String> getDependencies() {
//        return delegate.getDependencies();
//    }
//    
//    @Override
//    public CapabilityHealth getHealth() {
//        return delegate.getHealth();
//    }
//    
//    // ============================================================
//    // BaseAiAgent Overrides - Delegates to appropriate engine
//    // ============================================================
//    
//    @Override
//    protected String getAgentInstructions() {
//        return delegate.getAgentInstructions();
//    }
//    
//    @Override
//    protected String getFooterInstructions() {
//        return delegate.getFooterInstructions();
//    }
//    
//    @Override
//    public void setAiService(eu.kalafatic.evolution.controller.orchestration.AiService aiService) {
//        super.setAiService(aiService);
//        delegate.setAiService(aiService);
//    }
//}