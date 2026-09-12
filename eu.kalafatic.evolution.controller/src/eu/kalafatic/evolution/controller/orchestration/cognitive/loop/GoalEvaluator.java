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
        Object currentMetricObj = goal.getParameter("currentMetricKey");

        if (targetMetricObj instanceof Number) {
            double target = ((Number) targetMetricObj).doubleValue();
            double accumulated = 0.0;

            String metricKey = currentMetricObj != null ? currentMetricObj.toString() : "quantity";
            for (CognitiveObservation obs : observations) {
                if (obs.isSuccess()) {
                    Object val = obs.getMetadata().get(metricKey);
                    if (val instanceof Number) {
                        accumulated += ((Number) val).doubleValue();
                    }
                }
            }

            if (accumulated < target && target > 0) {
                double prog = Math.min(0.99, accumulated / target);
                missing.add("Required " + metricKey + ": " + target + ", accumulated: " + accumulated);
                evidence.add("Accumulated " + accumulated + " for " + metricKey);
                return GoalEvaluation.partial(prog, missing, evidence);
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
        if (lastObs.isSuccess() && missing.isEmpty()) {
            evidence.add("Successful execution of capability action: " + lastObs.getActionName());
            return GoalEvaluation.achieved("All declared goal requirements verified by observations");
        } else if (!lastObs.isSuccess()) {
            missing.add("Last action failed: " + lastObs.getStructuredError());
            double currentProg = worldState != null ? worldState.getCurrentProgress() : 0.0;
            return GoalEvaluation.partial(currentProg, missing, evidence);
        }

        return GoalEvaluation.achieved("Goal requirements satisfied");
    }
}
