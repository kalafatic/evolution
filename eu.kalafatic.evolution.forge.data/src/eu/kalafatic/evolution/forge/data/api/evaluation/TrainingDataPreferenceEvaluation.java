package eu.kalafatic.evolution.forge.data.api.evaluation;

import java.util.Map;

/**
 * Result evaluation detailing preference requirement satisfaction statuses.
 */
public class TrainingDataPreferenceEvaluation {

    public enum SatisfactionStatus {
        SATISFIED,
        PARTIALLY_SATISFIED,
        NOT_SATISFIED,
        UNKNOWN
    }

    private final boolean allHardRequirementsSatisfied;
    private final SatisfactionStatus overallStatus;
    private final Map<String, SatisfactionStatus> requirementStatuses;

    public TrainingDataPreferenceEvaluation(
            boolean allHardRequirementsSatisfied,
            SatisfactionStatus overallStatus,
            Map<String, SatisfactionStatus> requirementStatuses) {
        this.allHardRequirementsSatisfied = allHardRequirementsSatisfied;
        this.overallStatus = overallStatus;
        this.requirementStatuses = requirementStatuses != null ? Map.copyOf(requirementStatuses) : Map.of();
    }

    public boolean isAllHardRequirementsSatisfied() { return allHardRequirementsSatisfied; }
    public SatisfactionStatus getOverallStatus() { return overallStatus; }
    public Map<String, SatisfactionStatus> getRequirementStatuses() { return requirementStatuses; }
}
