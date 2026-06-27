package eu.kalafatic.evolution.controller.orchestration.engines;

import java.io.File;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;

public class FitnessEngine {

    public double calculateScore(EvaluationResult result) {
        return result.isSuccess() ? 0.8 + (result.getTestPassRate() * 0.2) : result.getTestPassRate() * 0.5;
    }

    public EvaluationResult evaluateReality(File tempDir, TaskContext variantContext, RealityLevel level, IterationManager manager) {
        try {
            eu.kalafatic.evolution.controller.orchestration.selfdev.RealityLevel selfDevLevel = eu.kalafatic.evolution.controller.orchestration.selfdev.RealityLevel.valueOf(level.name());
            return manager.getFitnessEngine().evaluate(tempDir, variantContext, selfDevLevel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
