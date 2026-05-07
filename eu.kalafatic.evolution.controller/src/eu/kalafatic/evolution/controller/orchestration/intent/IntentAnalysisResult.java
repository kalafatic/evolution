package eu.kalafatic.evolution.controller.orchestration.intent;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured result of the intent analysis phase.
 */
public class IntentAnalysisResult {
    private String goal;
    private String language;
    private String framework;
    private String targetPlatform;
    private String expectedOutput;
    private List<String> constraints = new ArrayList<>();
    private List<MissingRequirement> missingInformation = new ArrayList<>();
    private List<Ambiguity> ambiguities = new ArrayList<>();
    private double confidenceScore;

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getTargetPlatform() {
        return targetPlatform;
    }

    public void setTargetPlatform(String targetPlatform) {
        this.targetPlatform = targetPlatform;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public List<String> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<String> constraints) {
        this.constraints = constraints;
    }

    public List<MissingRequirement> getMissingInformation() {
        return missingInformation;
    }

    public void setMissingInformation(List<MissingRequirement> missingInformation) {
        this.missingInformation = missingInformation;
    }

    public List<Ambiguity> getAmbiguities() {
        return ambiguities;
    }

    public void setAmbiguities(List<Ambiguity> ambiguities) {
        this.ambiguities = ambiguities;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public boolean isAmbiguous() {
        return !ambiguities.isEmpty() || !missingInformation.isEmpty();
    }
}
