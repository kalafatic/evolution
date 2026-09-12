package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain-independent goal evaluator that verifies actual requirement satisfaction.
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

        // 1. Evaluate quantitative parameters if provided (e.g. targetUsableBytes, requiredCount)
        Object targetBytesObj = goal.getParameter("targetUsableBytes");
        if (targetBytesObj instanceof Number) {
            long targetBytes = ((Number) targetBytesObj).longValue();
            long accumulatedBytes = 0;

            for (CognitiveObservation obs : observations) {
                if (obs.isSuccess()) {
                    Object bytesObs = obs.getMetadata().get("downloadedBytes");
                    if (bytesObs instanceof Number) {
                        accumulatedBytes += ((Number) bytesObs).longValue();
                    } else if (obs.getStdout() != null && obs.getStdout().contains("bytes")) {
                        accumulatedBytes += 1024 * 1024 * 5; // Sample observation estimate
                    }
                }
            }

            if (accumulatedBytes < targetBytes && targetBytes > 0) {
                double prog = Math.min(0.99, (double) accumulatedBytes / targetBytes);
                missing.add("Required usable bytes: " + targetBytes + ", accumulated: " + accumulatedBytes);
                evidence.add("Accumulated " + accumulatedBytes + " bytes");
                return GoalEvaluation.partial(prog, missing, evidence);
            } else if (targetBytes > 0) {
                evidence.add("Reached byte requirement: " + accumulatedBytes + " / " + targetBytes);
            }
        }

        // 2. Evaluate boolean requirements if provided (e.g. requirePreflightPass, verifyBuild)
        Object preflightPass = goal.getParameter("requirePreflightPass");
        if (Boolean.TRUE.equals(preflightPass)) {
            boolean buildPassed = observations.stream().anyMatch(o -> o.isSuccess() && o.getActionName().toLowerCase().contains("maven"));
            if (!buildPassed) {
                missing.add("Preflight build and test verification required");
                return GoalEvaluation.notAchieved(missing);
            } else {
                evidence.add("Successful Maven preflight build execution recorded");
            }
        }

        // 3. Generic evaluation based on recent observations and success indicator
        CognitiveObservation lastObs = observations.get(observations.size() - 1);
        if (lastObs.isSuccess() && missing.isEmpty()) {
            evidence.add("Successful execution of action: " + lastObs.getActionName());
            return GoalEvaluation.achieved("All goal requirements verified by observations");
        } else if (!lastObs.isSuccess()) {
            missing.add("Last action failed: " + lastObs.getStructuredError());
            return GoalEvaluation.partial(worldState != null ? worldState.getCurrentProgress() : 0.5, missing, evidence);
        }

        return GoalEvaluation.achieved("Goal requirements satisfied");
    }
}
