package eu.kalafatic.evolution.controller.orchestration.intent;

import java.util.ArrayList;
import java.util.List;

/**
 * Model representing the result of an atomic intent analysis.
 * Determines whether a request is a singular deterministic task or requires strategic planning.
 */
public class AtomicIntentAnalysis {
    private boolean atomic;
    private double confidence;
    private boolean deterministic;
    /** @deprecated Use EvolutionaryComplexityVector for finer-grained control */
    @Deprecated
    private boolean requiresPlanning;
    private boolean multiStep;

    private EvolutionaryComplexityVector complexityVector = new EvolutionaryComplexityVector();

    private String targetArtifact;
    private String artifactType;

    private List<String> extractedTargets = new ArrayList<>();
    private List<String> signals = new ArrayList<>();
    private String reason;

    public boolean isAtomic() {
        return atomic;
    }

    public void setAtomic(boolean atomic) {
        this.atomic = atomic;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public boolean isDeterministic() {
        return deterministic;
    }

    public void setDeterministic(boolean deterministic) {
        this.deterministic = deterministic;
    }

    /** @deprecated Use EvolutionaryComplexityVector */
    @Deprecated
    public boolean isRequiresPlanning() {
        return requiresPlanning;
    }

    /** @deprecated Use EvolutionaryComplexityVector */
    @Deprecated
    public void setRequiresPlanning(boolean requiresPlanning) {
        this.requiresPlanning = requiresPlanning;
    }

    public EvolutionaryComplexityVector getComplexityVector() {
        return complexityVector;
    }

    public void setComplexityVector(EvolutionaryComplexityVector complexityVector) {
        this.complexityVector = complexityVector;
    }

    public boolean isMultiStep() {
        return multiStep;
    }

    public void setMultiStep(boolean multiStep) {
        this.multiStep = multiStep;
    }

    public String getTargetArtifact() {
        return targetArtifact;
    }

    public void setTargetArtifact(String targetArtifact) {
        this.targetArtifact = targetArtifact;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
    }

    public List<String> getExtractedTargets() {
        return extractedTargets;
    }

    public List<String> getSignals() {
        return signals;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "AtomicIntentAnalysis{" +
                "atomic=" + atomic +
                ", confidence=" + confidence +
                ", deterministic=" + deterministic +
                ", requiresPlanning=" + requiresPlanning +
                ", multiStep=" + multiStep +
                ", targetArtifact='" + targetArtifact + '\'' +
                ", artifactType='" + artifactType + '\'' +
                ", signals=" + signals +
                '}';
    }
}
