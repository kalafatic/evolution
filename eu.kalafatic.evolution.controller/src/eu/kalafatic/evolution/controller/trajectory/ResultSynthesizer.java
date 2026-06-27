package eu.kalafatic.evolution.controller.trajectory;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;

/**
 * Synthesis layer for merging insights and results from multiple evolutionary branches.
 */
public class ResultSynthesizer {

    public static class SynthesisResult {
        public String mergedInsight;
        public List<String> identifiedRisks = new ArrayList<>();
        public Map<String, Double> strategyConfidence = new HashMap<>();
        public String recommendedNextStep;
    }

    public SynthesisResult synthesize(List<BranchVariant> variants, TaskContext context) {
        if (context == null) {
            throw new IllegalStateException("ResultSynthesizer: context is null. Cannot synthesize results.");
        }
        SynthesisResult result = new SynthesisResult();
        List<EvaluationSignal> signals = context.getKernelContext().getSignalBus().getAllSignals();

        context.log("[SYNTHESIS] Merging results from " + variants.size() + " branches.");

        // 1. Compare branch outputs and extract highest-signal insights
        StringBuilder insights = new StringBuilder();
        int activeCount = 0;
        double maxScore = 0;
        BranchVariant bestVariant = null;

        for (BranchVariant v : variants) {
            double score = v.getScore();
            result.strategyConfidence.put(v.getStrategyType(), score);

            if (v.getActivationState() == BranchVariant.ActivationState.VERIFIED ||
                v.getActivationState() == BranchVariant.ActivationState.SCORING ||
                v.getActivationState() == BranchVariant.ActivationState.ACTIVE ||
                v.getActivationState() == BranchVariant.ActivationState.EXECUTING ||
                v.getActivationState() == BranchVariant.ActivationState.APPROVED) {
                activeCount++;
            }

            if (score > maxScore) {
                maxScore = score;
                bestVariant = v;
            }

            if (score > 0.6) {
                insights.append("- ").append(v.getStrategyType()).append(": ").append(v.getStrategy())
                        .append(" (Fitness: ").append(String.format("%.2f", score)).append(")\n");
            }

            if (v.getExpectedEffect() != null && v.getExpectedEffect().getRisk() > 0.7) {
                result.identifiedRisks.add("High execution risk in " + v.getStrategyType() + " trajectory.");
            }
        }

        StringBuilder merged = new StringBuilder();
        merged.append("Analyzed ").append(variants.size()).append(" evolutionary branches. ");
        if (bestVariant != null) {
            merged.append("Dominant strategy: ").append(bestVariant.getStrategyType()).append(" (").append(String.format("%.0f%%", maxScore * 100)).append(" confidence).\n");
        }
        merged.append("\nDetailed Insights:\n").append(insights);

        result.mergedInsight = merged.toString();

        // 2. Trajectory Update
        updateTrajectory(variants, context);

        // 3. Resolve recommended next step based on synthesized insights
        if (result.strategyConfidence.getOrDefault("STABILIZATION", 0.0) > 0.8) {
            result.recommendedNextStep = "Prioritize system stabilization and idiomatic cleanup.";
        } else if (result.strategyConfidence.getOrDefault("ANALYTICAL", 0.0) > 0.8 && activeCount < 2) {
            result.recommendedNextStep = "Deepen architectural analysis before further mutation.";
        } else if (result.strategyConfidence.getOrDefault("CURIOSITY", 0.0) > 0.5) {
            result.recommendedNextStep = "Explore non-obvious project dependencies or optimizations.";
        } else {
            result.recommendedNextStep = "Proceed with implementation refinement and verification.";
        }

        context.log("[SYNTHESIS] Synthesis complete. " + activeCount + " valid trajectories identified.");
        context.getOrchestrationState().getMetadata().put("synthesisResult", result);

        return result;
    }

    private void updateTrajectory(List<BranchVariant> variants, TaskContext context) {
        Object trajObj = context.getOrchestrationState().getMetadata().get("trajectory");
        Trajectory trajectory = null;
        if (trajObj instanceof Trajectory) {
            trajectory = (Trajectory) trajObj;
        } else if (trajObj instanceof Map) {
            trajectory = new com.fasterxml.jackson.databind.ObjectMapper()
                .convertValue(trajObj, Trajectory.class);
            context.getOrchestrationState().getMetadata().put("trajectory", trajectory);
        }

        if (trajectory == null) {
            trajectory = new Trajectory();
            context.getOrchestrationState().getMetadata().put("trajectory", trajectory);
        }

        double avgScore = variants.stream().mapToDouble(BranchVariant::getScore).average().orElse(0.0);
        trajectory.testTrend = avgScore > 0.5 ? "IMPROVING" : "STABLE";

        context.log("[SYNTHESIS] Trajectory updated. Trend: " + trajectory.testTrend);
    }
}
