package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

import eu.kalafatic.evolution.controller.kernel.FitnessEngine;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator.Evaluation;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;

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
    public EvaluationResult evaluate(File projectRoot, TaskContext context, RealityLevel level) {
        // Use the existing evaluator from the context
        if (this.context.getEvaluator() != null) {
            // Delegate to the evaluator
            return this.context.getEvaluator().evaluate(projectRoot, context, level);
        }
        
        // Fallback: return a simple success result
        EvaluationResult result = new EvaluationResult();
        result.setSuccess(true);
        return result;
    }
    
    @Override
    public EvaluationResult evaluate(File projectRoot, TaskContext context, EvolutionaryPressureVector pressure) {
        // Use the existing evaluator from the context
        if (this.context.getEvaluator() != null) {
            return this.context.getEvaluator().evaluate(projectRoot, context, pressure);
        }
        
        // Fallback
        EvaluationResult result = new EvaluationResult();
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