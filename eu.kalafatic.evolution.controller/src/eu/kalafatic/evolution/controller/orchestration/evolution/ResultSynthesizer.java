package eu.kalafatic.evolution.controller.orchestration.evolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.evolution.EvaluationSignal;

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
        SynthesisResult result = new SynthesisResult();
        List<EvaluationSignal> signals = SignalBus.getInstance().getAllSignals();

        context.log("[SYNTHESIS] Merging results from " + variants.size() + " branches.");

        // 1. Compare branch outputs and extract highest-signal insights
        StringBuilder insights = new StringBuilder();
        for (BranchVariant v : variants) {
            double score = v.getScore();
            result.strategyConfidence.put(v.getStrategyType(), score);

            if (score > 0.6) {
                insights.append("- Branch [").append(v.getStrategyType()).append("]: ")
                        .append(v.getStrategy()).append(" (Score: ").append(String.format("%.2f", score)).append(")\n");
            }

            if (v.getExpectedEffect() != null && v.getExpectedEffect().getRisk() > 0.7) {
                result.identifiedRisks.add("High risk in " + v.getStrategyType() + ": " + v.getStrategy());
            }
        }
        result.mergedInsight = insights.toString();

        // 2. Trajectory Update
        updateTrajectory(variants, context);

        // 3. Resolve recommended next step based on synthesized insights
        if (result.strategyConfidence.getOrDefault("STABILIZATION", 0.0) > 0.8) {
            result.recommendedNextStep = "Prioritize system stabilization and cleanup.";
        } else if (result.strategyConfidence.getOrDefault("CURIOSITY", 0.0) > 0.5) {
            result.recommendedNextStep = "Explore post-solution enhancements (tests, docs).";
        } else {
            result.recommendedNextStep = "Continue with implementation refinement.";
        }

        context.log("[SYNTHESIS] Synthesis complete. Recommended next step: " + result.recommendedNextStep);
        return result;
    }

    private void updateTrajectory(List<BranchVariant> variants, TaskContext context) {
        Trajectory trajectory = (Trajectory) context.getOrchestrationState().getMetadata().get("trajectory");
        if (trajectory == null) {
            trajectory = new Trajectory();
            context.getOrchestrationState().getMetadata().put("trajectory", trajectory);
        }

        double avgScore = variants.stream().mapToDouble(BranchVariant::getScore).average().orElse(0.0);
        trajectory.testTrend = avgScore > 0.5 ? "IMPROVING" : "STABLE";

        context.log("[SYNTHESIS] Trajectory updated. Trend: " + trajectory.testTrend);
    }
}
