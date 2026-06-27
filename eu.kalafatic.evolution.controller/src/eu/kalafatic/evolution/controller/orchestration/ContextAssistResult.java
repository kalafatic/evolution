package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.List;

/**
 * Result object for the Context Assist layer.
 */
public class ContextAssistResult {
    private PlatformType mode;
    private ConfidenceLevel confidence;
    private String clarifiedGoal;
    private List<String> missingInfo = new ArrayList<>();
    private List<String> suggestedSteps = new ArrayList<>();

    public ContextAssistResult() {
        this.mode = PlatformType.SIMPLE_CHAT;
        this.confidence = ConfidenceLevel.LOW;
    }

    public PlatformType getMode() {
        return mode;
    }

    public void setMode(PlatformType mode) {
        this.mode = mode;
    }

    public ConfidenceLevel getConfidence() {
        return confidence;
    }

    public void setConfidence(ConfidenceLevel confidence) {
        this.confidence = confidence;
    }

    public String getClarifiedGoal() {
        return clarifiedGoal;
    }

    public void setClarifiedGoal(String clarifiedGoal) {
        this.clarifiedGoal = clarifiedGoal;
    }

    public List<String> getMissingInfo() {
        return missingInfo;
    }

    public void setMissingInfo(List<String> missingInfo) {
        this.missingInfo = missingInfo;
    }

    public List<String> getSuggestedSteps() {
        return suggestedSteps;
    }

    public void setSuggestedSteps(List<String> suggestedSteps) {
        this.suggestedSteps = suggestedSteps;
    }
}
