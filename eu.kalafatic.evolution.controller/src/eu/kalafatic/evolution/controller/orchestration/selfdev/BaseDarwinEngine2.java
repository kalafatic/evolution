//package eu.kalafatic.evolution.controller.orchestration.selfdev;
//
//import java.util.List;
//
//import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
//import eu.kalafatic.evolution.controller.kernel.FitnessEngine;
//import eu.kalafatic.evolution.controller.orchestration.IterationManager;
//import eu.kalafatic.evolution.controller.orchestration.SessionManager;
//import eu.kalafatic.evolution.controller.orchestration.TaskContext;
//import eu.kalafatic.evolution.controller.orchestration.engines.SelectionEngine;
//import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
//import eu.kalafatic.evolution.controller.trajectory.Trajectory;
//import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
//
///**
// * Base Darwin Engine with common evolutionary functionality.
// * All mode-specific Darwin engines extend this.
// */
//public abstract class BaseDarwinEngine2 extends BaseAiAgent implements IBaseDarwinEngine {
//    
//    protected final TaskContext context;
//    protected final IterationMemoryService memoryService;
//    protected final FitnessEngine fitnessEngine;
//    protected final SelectionEngine selectionEngine;
//    
//    public BaseDarwinEngine2(TaskContext context, IterationMemoryService memoryService) {
//   	 super("BaseDarwinEngine", "BaseDarwinEngine", SessionManager.getInstance().getSession(context.getSessionId()));
//   	
//        this.context = context;
//        this.memoryService = memoryService;
//        
//        // Get FitnessEngine from context's kernel or create via factory
//        this.fitnessEngine = createFitnessEngine(context);
//        this.selectionEngine = new SelectionEngine();
//    }
//    
//    /**
//     * Creates or gets the FitnessEngine from the context.
//     */
//    private FitnessEngine createFitnessEngine(TaskContext context) {
//        // Try to get from kernel context
//        if (context.getKernelContext() != null) {
//            // Check if there's an existing fitness engine
//            Object engine = context.getKernelContext().getFitnessEngine();
//            if (engine instanceof FitnessEngine) {
//                return (FitnessEngine) engine;
//            }
//        }
//        
//        // Try to get from session container
//        if (context.getSessionContainer() != null) {
//            try {
//                // Some fitness engines might be registered as capabilities
//                return context.getSessionContainer().getFitnessEngine();
//            } catch (Exception e) {
//                // Fall through
//            }
//        }
//        
//        // Use the existing fitness engine from IterationManager
//        // This is the safest approach since IterationManager already has one
//        return new FitnessEngineProxy(context);
//    }
//    
//    /**
//     * Main entry point for this Darwin instance.
//     * All implementations must provide this.
//     */
//    public abstract EvaluationResult runIteration(GoalModel goal, IterationManager manager) throws Exception;
//    
//    /**
//     * Generates variants specific to this mode.
//     */
//    protected abstract List<BranchVariant> generateVariants(GoalModel goal, IterationManager manager) throws Exception;
//    
//    /**
//     * Validates variants specific to this mode.
//     */
//    protected abstract List<BranchVariant> validateVariants(List<BranchVariant> variants, IterationManager manager);
//    
//    /**
//     * Executes the winner variant.
//     */
//    protected abstract EvaluationResult executeWinner(BranchVariant winner, IterationManager manager) throws Exception;
//    
//    /**
//     * Mode identifier.
//     */
//    public abstract String getMode();
//    
//    /**
//     * Gets the active trajectory from the context.
//     */
//    protected Trajectory getActiveTrajectory() {
//        // Try to get from IterationManager via context
//        if (context.getKernelContext() != null && 
//            context.getKernelContext().getMemoryService() != null) {
//            
//            // Get the last active record
//            IterationMemoryService memory = context.getKernelContext().getMemoryService();
//            List<IterationRecord> records = memory.getRecords();
//            
//            if (records != null && !records.isEmpty()) {
//                // Find the most recent ACTIVE record
//                for (int i = records.size() - 1; i >= 0; i--) {
//                    IterationRecord record = records.get(i);
//                    if ("ACTIVE".equals(record.getActivationState()) && 
//                        record.getBranchId() != null) {
//                        return memory.getTrajectoryMemory().getTrajectory(record.getBranchId());
//                    }
//                }
//            }
//        }
//        
//        // Try to get from metadata
//        Object trajectoryObj = context.getOrchestrationState().getMetadata().get("activeTrajectory");
//        if (trajectoryObj instanceof Trajectory) {
//            return (Trajectory) trajectoryObj;
//        }
//        
//        return null;
//    }
//    
//    /**
//     * Saves the active trajectory to the context.
//     */
//    protected void setActiveTrajectory(Trajectory trajectory) {
//        context.getOrchestrationState().getMetadata().put("activeTrajectory", trajectory);
//    }
//}