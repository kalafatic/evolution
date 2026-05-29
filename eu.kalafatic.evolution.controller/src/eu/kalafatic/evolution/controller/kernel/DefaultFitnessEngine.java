package eu.kalafatic.evolution.controller.kernel;

import java.io.File;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.capability.contracts.IEvaluationContract;

public class DefaultFitnessEngine implements FitnessEngine {
    private final Evaluator evaluator;
    private final SessionContainer sessionContainer;

    public DefaultFitnessEngine(Evaluator evaluator, SessionContainer container) {
        this.evaluator = evaluator;
        this.sessionContainer = container;
    }

    @Override
    public EvaluationResult evaluate(File projectRoot, TaskContext context) throws Exception {
        IEvaluationContract contract = sessionContainer.getCapabilityRegistry().getContractImplementation(IEvaluationContract.ID, IEvaluationContract.class);
        return contract.evaluate(projectRoot, context, evaluator != null ? evaluator.getMavenTool() : null);
    }

    @Override
    public Evaluator.Evaluation evaluateWithSnapshot() throws Exception {
        return evaluator.evaluateWithSnapshot();
    }
}
