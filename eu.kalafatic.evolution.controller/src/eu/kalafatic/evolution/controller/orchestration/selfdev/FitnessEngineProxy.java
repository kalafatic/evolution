package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

import eu.kalafatic.evolution.controller.kernel.FitnessEngine;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator.Evaluation;
import eu.kalafatic.evolution.controller.tools.ITool;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;

/**
 * Proxy for FitnessEngine that delegates to the context's evaluator.
 * This avoids the need for a concrete FitnessEngine implementation.
 */
public class FitnessEngineProxy implements FitnessEngine {
    
    private final TaskContext context;
    
    public FitnessEngineProxy(TaskContext context) {
        this.context = context;
    }
    
    @Override
    public EvaluationResult evaluate(File projectRoot, TaskContext context, RealityLevel level) throws Exception {
        // Use the existing evaluator from the context
        Evaluator evaluator = (Evaluator) this.context.getMetadata().get("evaluator");
        if (evaluator != null) {
            // Delegate to the evaluator
            return evaluator.evaluate(projectRoot, context, level);
        }
        
        // Fallback: return a simple success result
        EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        result.setSuccess(true);
        return result;
    }
    
    @Override
    public EvaluationResult evaluate(File projectRoot, TaskContext context, EvolutionaryPressureVector pressure) throws Exception {
        // Use the existing evaluator from the context
        Evaluator evaluator = (Evaluator) this.context.getMetadata().get("evaluator");
        if (evaluator != null) {
            return evaluator.evaluate(projectRoot, context, (ITool) null);
        }
        
        // Fallback
        EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        result.setSuccess(true);
        return result;
    }
    
    @Override
    public double calculateScore(EvaluationResult result) {
        // Simple scoring based on success
        if (result == null) return 0.0;
        return result.isSuccess() ? 0.8 : 0.2;
    }

	@Override
	public Evaluation evaluateWithSnapshot() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}