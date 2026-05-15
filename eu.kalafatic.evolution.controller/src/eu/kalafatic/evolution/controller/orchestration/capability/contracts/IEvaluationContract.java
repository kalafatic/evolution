package eu.kalafatic.evolution.controller.orchestration.capability.contracts;

import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;
import java.io.File;
import java.util.List;

/**
 * Contract for evaluators.
 */
public interface IEvaluationContract {
    String ID = "contract.evaluation";

    EvaluationResult evaluate(File projectRoot, TaskContext context, eu.kalafatic.evolution.controller.tools.ITool mavenTool) throws Exception;

    // For signal emission and backward compatibility
    List<EvaluationSignal> evaluate(String variantId);
}
