package eu.kalafatic.evolution.controller.orchestration.goal;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the semantic boundaries and mutation space for an evolutionary task.
 * Anchors evolution to prevent semantic drift.
 */
public class SemanticEnvelope {
    private String coreIntent;
    private List<String> mandatoryConcepts = new ArrayList<>();
    private List<String> allowedMutationDimensions = new ArrayList<>();
    private List<String> discouragedRegions = new ArrayList<>();
    private List<String> forbiddenRegions = new ArrayList<>();
    private int maxAbstractionDepth = 3; // 1-5 scale
    private double semanticDistanceThreshold = 0.3;

    public String getCoreIntent() {
        return coreIntent;
    }

    public void setCoreIntent(String coreIntent) {
        this.coreIntent = coreIntent;
    }

    public List<String> getMandatoryConcepts() {
        return mandatoryConcepts;
    }

    public void setMandatoryConcepts(List<String> mandatoryConcepts) {
        this.mandatoryConcepts = mandatoryConcepts;
    }

    public List<String> getAllowedMutationDimensions() {
        return allowedMutationDimensions;
    }

    public void setAllowedMutationDimensions(List<String> allowedMutationDimensions) {
        this.allowedMutationDimensions = allowedMutationDimensions;
    }

    public List<String> getDiscouragedRegions() {
        return discouragedRegions;
    }

    public void setDiscouragedRegions(List<String> discouragedRegions) {
        this.discouragedRegions = discouragedRegions;
    }

    public List<String> getForbiddenRegions() {
        return forbiddenRegions;
    }

    public void setForbiddenRegions(List<String> forbiddenRegions) {
        this.forbiddenRegions = forbiddenRegions;
    }

    public int getMaxAbstractionDepth() {
        return maxAbstractionDepth;
    }

    public void setMaxAbstractionDepth(int maxAbstractionDepth) {
        this.maxAbstractionDepth = maxAbstractionDepth;
    }

    public double getSemanticDistanceThreshold() {
        return semanticDistanceThreshold;
    }

    public void setSemanticDistanceThreshold(double semanticDistanceThreshold) {
        this.semanticDistanceThreshold = semanticDistanceThreshold;
    }

    @Override
    public String toString() {
        return "SemanticEnvelope[" +
                "intent='" + coreIntent + '\'' +
                ", mandatory=" + mandatoryConcepts +
                ", dimensions=" + allowedMutationDimensions +
                ", forbidden=" + forbiddenRegions +
                ", maxDepth=" + maxAbstractionDepth +
                ']';
    }
}
