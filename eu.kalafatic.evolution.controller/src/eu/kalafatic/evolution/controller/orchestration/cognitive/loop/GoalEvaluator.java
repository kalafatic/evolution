package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic, requirement-driven goal evaluator verifying actual evidence and parameter constraints.
 * Operates purely on abstract requirements without hardcoded domain recipes.
 */
public class GoalEvaluator {

    public GoalEvaluation evaluate(CognitiveGoal goal, WorldState worldState, List<CognitiveObservation> observations) {
        if (goal == null) {
            return GoalEvaluation.notAchieved(List.of("Missing goal definition"));
        }

        if (observations == null || observations.isEmpty()) {
            return GoalEvaluation.notAchieved(List.of("No actions or observations recorded yet"));
        }

        List<String> missing = new ArrayList<>();
        List<String> evidence = new ArrayList<>();

        // 1. Generic quantitative target metric evaluation
        Object targetMetricObj = goal.getParameter("targetMetric");
        if (targetMetricObj == null) {
            targetMetricObj = goal.getParameter("targetUsableBytes");
        }
        Object currentMetricKeyObj = goal.getParameter("currentMetricKey");
        String metricKey = currentMetricKeyObj != null ? currentMetricKeyObj.toString() : "quantity";

        if (targetMetricObj instanceof Number) {
            double target = ((Number) targetMetricObj).doubleValue();
            double accumulated = 0.0;

            for (CognitiveObservation obs : observations) {
                long usable = obs.getUsableBytes();
                if (usable > 0) {
                    accumulated += usable;
                } else if (obs.isSuccess()) {
                    Object val = obs.getMetadata().get(metricKey);
                    if (val instanceof Number) {
                        accumulated += ((Number) val).doubleValue();
                    }
                }
            }

            double progress = target > 0 ? Math.min(1.0, accumulated / target) : 1.0;
            if (worldState != null) {
                worldState.setCurrentProgress(progress);
            }

            if (accumulated < target && target > 0) {
                missing.add("Required " + metricKey + ": " + target + ", accumulated: " + accumulated + " (" + String.format("%.1f%%", progress * 100) + ")");
                evidence.add("Accumulated " + accumulated + " / " + target + " for " + metricKey);
                return GoalEvaluation.partial(progress, missing, evidence);
            } else if (target > 0) {
                evidence.add("Reached requirement for " + metricKey + ": " + accumulated + " / " + target);
            }
        }

        // 2. Generic required verification step evaluation
        Object requiredCapabilityObj = goal.getParameter("requiredCapabilityVerification");
        if (requiredCapabilityObj instanceof String) {
            String requiredCap = (String) requiredCapabilityObj;
            boolean verified = observations.stream().anyMatch(o -> o.isSuccess() && requiredCap.equalsIgnoreCase(o.getActionName()));
            if (!verified) {
                missing.add("Required verification capability execution missing: " + requiredCap);
                return GoalEvaluation.notAchieved(missing);
            } else {
                evidence.add("Verified required capability execution: " + requiredCap);
            }
        }

        // 3. Last action outcome assessment
        CognitiveObservation lastObs = observations.get(observations.size() - 1);
        if (missing.isEmpty()) {
            evidence.add("Goal target metric requirements satisfied");
            return GoalEvaluation.achieved("All declared goal requirements verified by observations");
        } else if (!lastObs.isSuccess()) {
            missing.add("Last action failed: " + lastObs.getStructuredError());
            double currentProg = worldState != null ? worldState.getCurrentProgress() : 0.0;
            return GoalEvaluation.partial(currentProg, missing, evidence);
        }

        return GoalEvaluation.achieved("Goal requirements satisfied");
    }
}
