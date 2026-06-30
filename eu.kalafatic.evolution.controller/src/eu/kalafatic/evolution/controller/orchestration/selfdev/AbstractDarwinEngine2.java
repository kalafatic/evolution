//package eu.kalafatic.evolution.controller.orchestration.selfdev;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//
//import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
//import eu.kalafatic.evolution.controller.kernel.EvolutionProfile;
//import eu.kalafatic.evolution.controller.orchestration.IterationManager;
//import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
//import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
//import eu.kalafatic.evolution.controller.orchestration.TaskContext;
//import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
//import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;
//import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
//import eu.kalafatic.evolution.controller.orchestration.mediation.MediationEngine;
//import eu.kalafatic.evolution.controller.orchestration.selfdev.enums.RealityLevel;
//import eu.kalafatic.evolution.controller.orchestration.util.ModeRecognizer;
//import eu.kalafatic.evolution.controller.trajectory.Trajectory;
//import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
//import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
//import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
//
///**
// * Abstract base implementation of IDarwinEngine.
// * Provides common functionality for all Darwin engines.
// * 
// * Specific engine implementations should extend this class.
// */
//public abstract class AbstractDarwinEngine implements IDarwinEngine {
//
//    // ============================================================
//    // FIELDS
//    // ============================================================
//
//    protected final TaskContext context;
//    protected final IterationMemoryService memoryService;
//    protected final SessionContainer sessionContainer;
//    protected final eu.kalafatic.evolution.controller.orchestration.AiService aiService;
//    protected final MediationEngine mediationEngine;
//    protected final SelectionEngine selectionEngine;
//    protected final LineageEngine lineageEngine;
//    protected final FitnessEngine fitnessEngine;
//    protected final ExecutionEngine executionEngine;
//    protected final DimensionEngine dimensionEngine;
//    protected final eu.kalafatic.evolution.controller.kernel.EvolutionaryPressureEngine pressureEngine;
//
//    protected Evaluator evaluator;
//    protected EvolutionProfile executionProfile;
//    protected Trajectory activeTrajectory;
//
//    // ============================================================
//    // CONSTRUCTOR
//    // ============================================================
//
//    public AbstractDarwinEngine(TaskContext context, IterationMemoryService memoryService) {
//        this.context = context;
//        this.memoryService = memoryService;
//        this.sessionContainer = context.getSessionContainer();
//        this.aiService = context.getAiService();
//        this.mediationEngine = new MediationEngine();
//        this.selectionEngine = new SelectionEngine();
//        this.lineageEngine = new LineageEngine();
//        this.fitnessEngine = new FitnessEngine();
//        this.executionEngine = new ExecutionEngine();
//        this.dimensionEngine = new DimensionEngine();
//        this.pressureEngine = sessionContainer.getPressureEngine();
//
//        // Initialize with default profile
//        this.executionProfile = EvolutionProfile.create(getCapabilityType(), 2);
//    }
//
//    // ============================================================
//    // ABSTRACT METHODS (Implemented by Subclasses)
//    // ============================================================
//
//    @Override
//    public abstract EvaluationResult runIteration(GoalModel goal, IterationManager manager) throws Exception;
//
//    @Override
//    public abstract List<BranchVariant> generateVariants(GoalModel goal, IterationManager manager) throws Exception;
//
//    @Override
//    public abstract List<BranchVariant> validateVariants(List<BranchVariant> variants, IterationManager manager);
//
//    @Override
//    public abstract EvaluationResult executeWinner(BranchVariant winner, IterationManager manager) throws Exception;
//
//    @Override
//    public abstract eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType getCapabilityType();
//
//    @Override
//    public abstract String getMode();
//
//    // ============================================================
//    // COMMON IMPLEMENTATIONS
//    // ============================================================
//
//    @Override
//    public OrchestratorResponse orchestrateEvolution(TaskRequest taskRequest, IterationManager iterationManager)
//            throws Exception {
//        // Default implementation - subclasses can override
//        context.log("[DARWIN] Orchestrating evolution for mode: " + getMode());
//
//        // Get or create goal model
//        GoalModel goal = GoalModel.extract(context.getOrchestrationState().getMetadata(),
//                iterationManager, taskRequest.getPrompt(), context);
//
//        // Run iteration
//        EvaluationResult result = runIteration(goal, iterationManager);
//
//        // Build response
//        OrchestratorResponse response = new OrchestratorResponse();
//        response.setResultType(eu.kalafatic.evolution.controller.orchestration.ResultType.CHAT);
//
//        if (result.isSuccess()) {
//            response.setSummary(getMode() + " evolution completed successfully");
//            iterationManager.transition(SystemState.DONE, context);
//        } else {
//            response.setSummary(getMode() + " evolution failed: " + String.join("; ", result.getErrors()));
//            iterationManager.transition(SystemState.FAILED, context);
//        }
//
//        return response;
//    }
//
//    @Override
//    public TaskContext getContext() {
//        return context;
//    }
//
//    @Override
//    public Trajectory getActiveTrajectory() {
//        if (activeTrajectory != null) {
//            return activeTrajectory;
//        }
//
//        // Try to load from memory
//        if (context.getKernelContext() != null &&
//                context.getKernelContext().getMemoryService() != null) {
//            IterationMemoryService memory = context.getKernelContext().getMemoryService();
//            List<IterationRecord> records = memory.getRecords();
//
//            if (records != null && !records.isEmpty()) {
//                for (int i = records.size() - 1; i >= 0; i--) {
//                    IterationRecord record = records.get(i);
//                    if ("ACTIVE".equals(record.getActivationState()) &&
//                            record.getBranchId() != null) {
//                        activeTrajectory = memory.getTrajectoryMemory()
//                                .getTrajectory(record.getBranchId());
//                        if (activeTrajectory != null) {
//                            return activeTrajectory;
//                        }
//                    }
//                }
//            }
//        }
//
//        return null;
//    }
//
//    @Override
//    public void setActiveTrajectory(Trajectory trajectory) {
//        this.activeTrajectory = trajectory;
//        context.getOrchestrationState().getMetadata().put("activeTrajectory", trajectory);
//    }
//
//    @Override
//    public EvaluationResult evaluateFitness(File projectRoot, TaskContext context, RealityLevel level)
//            throws Exception {
//        if (this.evaluator != null) {
//            return this.evaluator.evaluate(projectRoot, context, level);
//        }
//
//        EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
//        result.setSuccess(true);
//        result.setDecision(SelfDevDecision.CONTINUE);
//        return result;
//    }
//
//    @Override
//    public void setEvaluator(Evaluator evaluator) {
//        this.evaluator = evaluator;
//    }
//
//    @Override
//    public Evaluator getEvaluator() {
//        return this.evaluator;
//    }
//
//    // ============================================================
//    // PROTECTED HELPER METHODS
//    // ============================================================
//
//    /**
//     * Gets the execution profile for this engine.
//     */
//    protected EvolutionProfile getExecutionProfile() {
//        if (executionProfile == null) {
//            executionProfile = EvolutionProfile.create(getCapabilityType(), 2);
//        }
//        return executionProfile;
//    }
//
//    /**
//     * Sets the execution profile for this engine.
//     */
//    protected void setExecutionProfile(EvolutionProfile profile) {
//        this.executionProfile = profile;
//        context.getOrchestrationState().setExecutionProfile(profile);
//    }
//
//    /**
//     * Gets the SessionContainer.
//     */
//    protected SessionContainer getSessionContainer() {
//        return sessionContainer;
//    }
//
//    /**
//     * Gets the AiService.
//     */
//    protected eu.kalafatic.evolution.controller.orchestration.AiService getAiService() {
//        return aiService;
//    }
//
//    /**
//     * Gets the MediationEngine.
//     */
//    protected MediationEngine getMediationEngine() {
//        return mediationEngine;
//    }
//
//    /**
//     * Gets the FitnessEngine.
//     */
//    protected FitnessEngine getFitnessEngine() {
//        return fitnessEngine;
//    }
//
//    /**
//     * Selects the best variant from a list.
//     */
//    protected BranchVariant selectBestVariant(List<BranchVariant> variants) {
//        if (variants == null || variants.isEmpty()) {
//            return null;
//        }
//        return variants.stream()
//                .max((v1, v2) -> Double.compare(v1.getScore(), v2.getScore()))
//                .orElse(variants.get(0));
//    }
//
//    /**
//     * Creates a failed result.
//     */
//    protected EvaluationResult failedResult(String reason) {
//        EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
//        result.setSuccess(false);
//        result.setDecision(SelfDevDecision.ROLLBACK);
//        result.getErrors().add(reason);
//        return result;
//    }
//
//    /**
//     * Creates a success result.
//     */
//    protected EvaluationResult successResult() {
//        EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
//        result.setSuccess(true);
//        result.setDecision(SelfDevDecision.CONTINUE);
//        return result;
//    }
//
//    /**
//     * Checks if the current request is a chat request.
//     */
//    protected boolean isChatRequest() {
//        return ModeRecognizer.isChatMode(context);
//    }
//
//    /**
//     * Checks if the current request is mediated.
//     */
//    protected boolean isMediated() {
//        return ModeRecognizer.isMediatedMode(context);
//    }
//
//    /**
//     * Checks if the current request is self-dev.
//     */
//    protected boolean isSelfDev() {
//        return ModeRecognizer.isSelfDevMode(context);
//    }
//
//    /**
//     * Gets the goal model from context.
//     */
//    protected GoalModel getGoalModel(IterationManager manager, String request) throws Exception {
//        return GoalModel.extract(context.getOrchestrationState().getMetadata(), manager, request, context);
//    }
//}