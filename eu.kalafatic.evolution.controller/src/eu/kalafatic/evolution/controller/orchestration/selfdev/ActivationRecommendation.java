package eu.kalafatic.evolution.controller.orchestration.selfdev;

/**
 * Pure recommendation model for Darwin branch activation.
 * Contains analysis and scoring without authority to mutate state.
 */
public class ActivationRecommendation {
    private final String branchId;
    private final double confidenceScore;
    private final int rankingPosition;
    private final double semanticAlignmentScore;
    private final String rationale;
    private final double recommendedActivationThreshold;

    public ActivationRecommendation(String branchId, double confidenceScore, int rankingPosition,
                                   double semanticAlignmentScore, String rationale, double recommendedActivationThreshold) {
        this.branchId = branchId;
        this.confidenceScore = confidenceScore;
        this.rankingPosition = rankingPosition;
        this.semanticAlignmentScore = semanticAlignmentScore;
        this.rationale = rationale;
        this.recommendedActivationThreshold = recommendedActivationThreshold;
    }

    public String getBranchId() { return branchId; }
    public double getConfidenceScore() { return confidenceScore; }
    public int getRankingPosition() { return rankingPosition; }
    public double getSemanticAlignmentScore() { return semanticAlignmentScore; }
    public String getRationale() { return rationale; }
    public double getRecommendedActivationThreshold() { return recommendedActivationThreshold; }

    @Override
    public String toString() {
        return String.format("Recommendation[Branch: %s, Rank: %d, Confidence: %.2f, Align: %.2f, Threshold: %.2f, Rationale: %s]",
                branchId, rankingPosition, confidenceScore, semanticAlignmentScore, recommendedActivationThreshold, rationale);
    }
}
