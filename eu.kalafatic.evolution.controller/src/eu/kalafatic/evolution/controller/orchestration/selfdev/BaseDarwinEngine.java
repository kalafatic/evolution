package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.List;

import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.engines.SelectionEngine;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;

/**
 * Abstract base implementation of IBaseDarwinEngine.
 */
public abstract class BaseDarwinEngine implements IBaseDarwinEngine {
    
    protected final TaskContext context;
    protected final IterationMemoryService memoryService;
    protected final SelectionEngine selectionEngine;
    protected Evaluator evaluator;
    protected SessionContainer sessionContainer;
    
    public BaseDarwinEngine(TaskContext context, IterationMemoryService memoryService) {
    	
        this.context = context;
        this.memoryService = memoryService;
        this.selectionEngine = new SelectionEngine();
        
        // Try to get SessionContainer from context
        if (context.getOrchestrator() != null) {
            try {
                // Try to get via reflection or direct getter
                java.lang.reflect.Method method = context.getOrchestrator().getClass()
                    .getMethod("getSessionContainer");
                Object container = method.invoke(context.getOrchestrator());
                if (container instanceof SessionContainer) {
                    this.sessionContainer = (SessionContainer) container;
                }
            } catch (Exception e) {
                // SessionContainer not available via reflection
            }
        }
    }
    
    public BaseDarwinEngine(TaskContext context, IterationMemoryService memoryService, 
                            SessionContainer sessionContainer) {
    	    	
        this.context = context;
        this.memoryService = memoryService;
        this.sessionContainer = sessionContainer;
        this.selectionEngine = new SelectionEngine();
    }
    
    public SessionContainer getSessionContainer() {
        return this.sessionContainer;
    }
    
    public void setSessionContainer(SessionContainer sessionContainer) {
        this.sessionContainer = sessionContainer;
    }
    
    @Override
    public void setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }
    
    @Override
    public Evaluator getEvaluator() {
        return this.evaluator;
    }
    
    @Override
    public abstract EvaluationResult runIteration(GoalModel goal, IterationManager manager) throws Exception;
    
    @Override
    public abstract List<BranchVariant> generateVariants(GoalModel goal, IterationManager manager) throws Exception;
    
    @Override
    public abstract List<BranchVariant> validateVariants(List<BranchVariant> variants, IterationManager manager);
    
    @Override
    public abstract EvaluationResult executeWinner(BranchVariant winner, IterationManager manager) throws Exception;
    
    @Override
    public abstract String getMode();
    
    @Override
    public TaskContext getContext() {
        return context;
    }
    
    @Override
    public Trajectory getActiveTrajectory() {
        if (context.getKernelContext() != null && 
            context.getKernelContext().getMemoryService() != null) {
            
            IterationMemoryService memory = context.getKernelContext().getMemoryService();
            List<IterationRecord> records = memory.getRecords();
            
            if (records != null && !records.isEmpty()) {
                for (int i = records.size() - 1; i >= 0; i--) {
                    IterationRecord record = records.get(i);
                    if ("ACTIVE".equals(record.getActivationState()) && 
                        record.getBranchId() != null) {
                        return memory.getTrajectoryMemory().getTrajectory(record.getBranchId());
                    }
                }
            }
        }
        
        Object trajectoryObj = context.getOrchestrationState().getMetadata().get("activeTrajectory");
        if (trajectoryObj instanceof Trajectory) {
            return (Trajectory) trajectoryObj;
        }
        
        return null;
    }
    
    @Override
    public void setActiveTrajectory(Trajectory trajectory) {
        context.getOrchestrationState().getMetadata().put("activeTrajectory", trajectory);
    }
    
    @Override
    public EvaluationResult evaluateFitness(File projectRoot, TaskContext context, RealityLevel level) throws Exception {
        if (this.evaluator != null) {
            return this.evaluator.evaluate(projectRoot, context, level);
        }
        
        // Fallback: return a simple success result
        EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        result.setSuccess(true);
        result.setDecision(SelfDevDecision.CONTINUE);
        return result;
    }
    
    protected BranchVariant selectBestVariant(List<BranchVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        return variants.stream()
            .max((v1, v2) -> Double.compare(v1.getScore(), v2.getScore()))
            .orElse(variants.get(0));
    }
    
    protected EvaluationResult failedResult(String reason) {
        EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        result.setSuccess(false);
        result.setDecision(SelfDevDecision.ROLLBACK);
        result.getErrors().add(reason);
        return result;
    }
    
    protected EvaluationResult successResult() {
        EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        result.setSuccess(true);
        result.setDecision(SelfDevDecision.CONTINUE);
        return result;
    }
}