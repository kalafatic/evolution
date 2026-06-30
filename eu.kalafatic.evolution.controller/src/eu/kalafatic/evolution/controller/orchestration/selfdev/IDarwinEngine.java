package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.List;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityContext;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException;
import eu.kalafatic.evolution.controller.orchestration.capability.ICapability;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.intent.EvolutionAssessment;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;

/**
 * Core interface for all Darwin engines. Defines the contract for evolutionary
 * iteration, variant generation, validation, execution, and orchestration.
 */
public interface IDarwinEngine extends ICapability {

	// ============================================================
	// CORE EVOLUTION METHODS
	// ============================================================

	/**
	 * Main entry point for running an evolutionary iteration.
	 */
	EvaluationResult runIteration(GoalModel goal, IterationManager manager) throws Exception;

	/**
	 * Generates variants specific to this engine's mode. Matches IMutationContract
	 * signature.
	 */
	List<BranchVariant> generateVariants(GoalModel goal, StateSnapshot snapshot, FailureMemory failureMemory,
			Trajectory trajectory, EvolutionaryPressureVector pressure) throws Exception;

	/**
	 * Validates variants for this engine's mode.
	 */
	List<BranchVariant> validateVariants(List<BranchVariant> variants, IterationManager manager);

	/**
	 * Executes the winner variant.
	 */
	EvaluationResult executeWinner(BranchVariant winner, IterationManager manager) throws Exception;

	/**
	 * Orchestrates the full evolution flow.
	 */
	OrchestratorResponse orchestrateEvolution(TaskRequest taskRequest, IterationManager iterationManager)
			throws Exception;

	// ============================================================
	// MODE & CONTEXT
	// ============================================================

	/**
	 * Gets the mode identifier for this engine.
	 */
	String getMode();

	/**
	 * Gets the capability type for this engine.
	 */
	eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType getCapabilityType();

	/**
	 * Gets the current task context.
	 */
	TaskContext getContext();

	// ============================================================
	// TRAJECTORY
	// ============================================================

	/**
	 * Gets the active trajectory for this engine.
	 */
	Trajectory getActiveTrajectory();

	/**
	 * Sets the active trajectory for this engine.
	 */
	void setActiveTrajectory(Trajectory trajectory);

	// ============================================================
	// FITNESS & EVALUATION
	// ============================================================

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

	List<BranchVariant> generateVariants(GoalModel goal, IterationManager manager) throws Exception;

	OrchestratorResponse evolve(String request, IterationManager iterationManager,
			EvolutionAssessment initialAssessment) throws Exception;

	EvaluationResult runDarwinIteration(TaskContext context, IterationManager iterationManager) throws Exception;

	void setAiService(AiService aiService);

	void initialize(CapabilityContext capabilityContext) throws CapabilityException;
	
	PlatformType getPlatformType();
	
	void setIntentAnalyzer(PromptIntentAnalyzer intentAnalyzer);
	PromptIntentAnalyzer getIntentAnalyzer();
	
	AiService  getAiservice();
}