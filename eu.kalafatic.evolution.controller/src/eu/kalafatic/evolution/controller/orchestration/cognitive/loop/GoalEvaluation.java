package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

import java.util.Collections;
import java.util.List;

/**
 * Result of evaluating goal satisfaction against current world state and observations.
 */
public class GoalEvaluation {

    public enum Status {
        ACHIEVED,
        PARTIAL,
        NOT_ACHIEVED,
        UNKNOWN
    }

    private final Status status;
    private final double progress; // 0.0 to 1.0
    private final List<String> missingRequirements;
    private final List<String> evidence;
    private final double confidence;

    public GoalEvaluation(Status status, double progress, List<String> missingRequirements, List<String> evidence, double confidence) {
        this.status = status;
        this.progress = Math.max(0.0, Math.min(1.0, progress));
        this.missingRequirements = missingRequirements != null ? List.copyOf(missingRequirements) : Collections.emptyList();
        this.evidence = evidence != null ? List.copyOf(evidence) : Collections.emptyList();
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    public static GoalEvaluation achieved(String proof) {
        return new GoalEvaluation(Status.ACHIEVED, 1.0, Collections.emptyList(), List.of(proof), 1.0);
    }

    public static GoalEvaluation partial(double progress, List<String> missing, List<String> evidence) {
        return new GoalEvaluation(Status.PARTIAL, progress, missing, evidence, 0.8);
    }

    public static GoalEvaluation notAchieved(List<String> missing) {
        return new GoalEvaluation(Status.NOT_ACHIEVED, 0.0, missing, Collections.emptyList(), 0.9);
    }

    public boolean isAchieved() {
        return status == Status.ACHIEVED;
    }

    public Status getStatus() {
        return status;
    }

    public double getProgress() {
        return progress;
    }

    public List<String> getMissingRequirements() {
        return missingRequirements;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public double getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return "GoalEvaluation{" +
                "status=" + status +
                ", progress=" + String.format("%.1f%%", progress * 100) +
                ", missing=" + missingRequirements +
                ", evidence=" + evidence +
                ", confidence=" + confidence +
                '}';
    }
}
