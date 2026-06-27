package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.intent.EvolutionAssessment;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;

/**
 * Core interface for all Darwin engines.
 */
public interface IBaseDarwinEngine {
    
    OrchestratorResponse orchestrateEvolution(TaskRequest taskRequest, IterationManager iterationManager) throws Exception;

    OrchestratorResponse evolve(String request, IterationManager iterationManager, EvolutionAssessment initialAssessment) throws Exception;

    EvaluationResult runIteration(GoalModel goal, IterationManager manager) throws Exception;
    
    List<BranchVariant> generateVariants(GoalModel goal, IterationManager manager) throws Exception;
    
    List<BranchVariant> validateVariants(List<BranchVariant> variants, IterationManager manager);
    
    EvaluationResult executeWinner(BranchVariant winner, IterationManager manager) throws Exception;
    
    String getMode();
    
    TaskContext getContext();
    
    Trajectory getActiveTrajectory();
    
    void setActiveTrajectory(Trajectory trajectory);
    
    /**
     * Evaluates fitness using the engine's evaluator.
     */
    EvaluationResult evaluateFitness(File projectRoot, TaskContext context, RealityLevel level) throws Exception;
    
    /**
     * Sets the evaluator for this engine.
     */
    void setEvaluator(Evaluator evaluator);
    
    /**
     * Gets the evaluator for this engine.
     */
    Evaluator getEvaluator();

    /**
     * Sets the AI service for this engine.
     */
    void setAiService(AiService aiService);
}