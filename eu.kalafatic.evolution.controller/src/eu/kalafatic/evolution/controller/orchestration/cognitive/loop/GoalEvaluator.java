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

        // 3. Explicit file target conditions
        Object targetFileObj = goal.getParameter("targetFile");
        if (targetFileObj instanceof String) {
            String targetPath = (String) targetFileObj;
            java.io.File file = new java.io.File(targetPath);
            if (!file.exists()) {
                missing.add("Target file does not exist: " + targetPath);
            } else {
                evidence.add("Target file exists: " + targetPath);
                Object containsObj = goal.getParameter("targetFileContains");
                if (containsObj instanceof String) {
                    String pattern = (String) containsObj;
                    try {
                        String content = java.nio.file.Files.readString(file.toPath());
                        if (!content.contains(pattern)) {
                            missing.add("Target file " + targetPath + " does not contain required text: '" + pattern + "'");
                        } else {
                            evidence.add("Target file contains required text: '" + pattern + "'");
                        }
                    } catch (Exception e) {
                        missing.add("Failed reading target file " + targetPath + ": " + e.getMessage());
                    }
                }
            }
        }

        // 4. Explicit output / content match conditions
        Object expectedOutputObj = goal.getParameter("expectedOutputContains");
        if (expectedOutputObj instanceof String) {
            String exp = (String) expectedOutputObj;
            boolean found = observations.stream().anyMatch(o ->
                (o.getStdout() != null && o.getStdout().contains(exp)) ||
                (o.getMetadata() != null && o.getMetadata().values().stream().anyMatch(v -> v != null && v.toString().contains(exp)))
            );
            if (!found) {
                missing.add("Expected output text not found in observations: '" + exp + "'");
            } else {
                evidence.add("Found expected output text: '" + exp + "'");
            }
        }

        // 5. Minimum items / results count condition
        Object minResultsObj = goal.getParameter("minimumResults");
        if (minResultsObj instanceof Number) {
            int minResults = ((Number) minResultsObj).intValue();
            int accumulatedResults = 0;
            for (CognitiveObservation obs : observations) {
                if (obs.isSuccess()) {
                    Object countVal = obs.getMetadata().get("resultCount");
                    if (countVal == null) countVal = obs.getMetadata().get("itemsFound");
                    if (countVal instanceof Number) {
                        accumulatedResults += ((Number) countVal).intValue();
                    }
                }
            }
            if (accumulatedResults < minResults) {
                missing.add("Minimum results not met: required " + minResults + ", found " + accumulatedResults);
            } else {
                evidence.add("Minimum results threshold satisfied: " + accumulatedResults + " >= " + minResults);
            }
        }

        // 6. Explicit Custom Evaluator Predicate
        Object predicateObj = goal.getParameter("goalPredicate");
        if (predicateObj instanceof java.util.function.Predicate) {
            @SuppressWarnings("unchecked")
            java.util.function.Predicate<List<CognitiveObservation>> pred = (java.util.function.Predicate<List<CognitiveObservation>>) predicateObj;
            if (!pred.test(observations)) {
                missing.add("Custom goal predicate evaluation failed");
            } else {
                evidence.add("Custom goal predicate passed");
            }
        }

        // 7. Last action outcome and completion assessment
        CognitiveObservation lastObs = observations.get(observations.size() - 1);
        if (!missing.isEmpty()) {
            double currentProg = worldState != null ? worldState.getCurrentProgress() : 0.0;
            return GoalEvaluation.partial(currentProg, missing, evidence);
        }

        if (!lastObs.isSuccess() && targetMetricObj == null && targetFileObj == null && expectedOutputObj == null) {
            missing.add("Last action failed: " + lastObs.getStructuredError());
            double currentProg = worldState != null ? worldState.getCurrentProgress() : 0.0;
            return GoalEvaluation.partial(currentProg, missing, evidence);
        }

        // If goal had no explicit condition parameters at all, require at least one successful non-empty action
        boolean anySuccessfulAction = observations.stream().anyMatch(CognitiveObservation::isSuccess);
        if (!anySuccessfulAction) {
            missing.add("No successful observation recorded to verify goal");
            return GoalEvaluation.notAchieved(missing);
        }

        if (worldState != null && worldState.getCurrentProgress() < 1.0) {
            worldState.setCurrentProgress(1.0);
        }
        evidence.add("All declared goal criteria verified successfully");
        return GoalEvaluation.achieved(String.join("; ", evidence));
    }
}
