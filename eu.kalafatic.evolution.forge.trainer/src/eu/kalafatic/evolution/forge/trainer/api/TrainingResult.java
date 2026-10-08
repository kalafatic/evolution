package eu.kalafatic.evolution.forge.trainer.api;

import java.util.HashMap;
import java.util.Map;

public class TrainingResult {

    private TrainingTerminationStatus terminationStatus = TrainingTerminationStatus.TRAINING_COMPLETED;
    private TerminationReason terminationReason = TerminationReason.PLANNED_COMPLETION;
    private int plannedEpochs;
    private int completedEpochs;
    private int plannedSteps;
    private int completedSteps;
    private long trainingDurationMs;
    private double lastLoss;
    private double valLoss;
    private boolean modelValid = true;
    private String failureDetails = "";
    private final Map<String, Object> metadata = new HashMap<>();

    public TrainingResult() {}

    public TrainingResult(TrainingTerminationStatus terminationStatus, TerminationReason terminationReason) {
        this.terminationStatus = terminationStatus;
        this.terminationReason = terminationReason;
    }

    public TrainingTerminationStatus getTerminationStatus() { return terminationStatus; }
    public void setTerminationStatus(TrainingTerminationStatus terminationStatus) { this.terminationStatus = terminationStatus; }

    public TerminationReason getTerminationReason() { return terminationReason; }
    public void setTerminationReason(TerminationReason terminationReason) { this.terminationReason = terminationReason; }

    public int getPlannedEpochs() { return plannedEpochs; }
    public void setPlannedEpochs(int plannedEpochs) { this.plannedEpochs = plannedEpochs; }

    public int getCompletedEpochs() { return completedEpochs; }
    public void setCompletedEpochs(int completedEpochs) { this.completedEpochs = completedEpochs; }

    public int getPlannedSteps() { return plannedSteps; }
    public void setPlannedSteps(int plannedSteps) { this.plannedSteps = plannedSteps; }

    public int getCompletedSteps() { return completedSteps; }
    public void setCompletedSteps(int completedSteps) { this.completedSteps = completedSteps; }

    public long getTrainingDurationMs() { return trainingDurationMs; }
    public void setTrainingDurationMs(long trainingDurationMs) { this.trainingDurationMs = trainingDurationMs; }

    public double getLastLoss() { return lastLoss; }
    public void setLastLoss(double lastLoss) { this.lastLoss = lastLoss; }

    public double getValLoss() { return valLoss; }
    public void setValLoss(double valLoss) { this.valLoss = valLoss; }

    public boolean isModelValid() { return modelValid; }
    public void setModelValid(boolean modelValid) { this.modelValid = modelValid; }

    public String getFailureDetails() { return failureDetails; }
    public void setFailureDetails(String failureDetails) { this.failureDetails = failureDetails; }

    public Map<String, Object> getMetadata() { return metadata; }

    public boolean isCompletedNormally() {
        return terminationStatus == TrainingTerminationStatus.TRAINING_COMPLETED;
    }

    public boolean isStoppedEarly() {
        return terminationStatus == TrainingTerminationStatus.TRAINING_STOPPED
            || terminationStatus == TrainingTerminationStatus.TRAINING_CANCELLED;
    }

    public boolean isFailed() {
        return terminationStatus == TrainingTerminationStatus.TRAINING_FAILED
            || terminationStatus == TrainingTerminationStatus.TRAINING_ABORTED;
    }

    @Override
    public String toString() {
        return String.format("TrainingResult{status=%s, reason=%s, epochs=%d/%d, steps=%d/%d, loss=%.4f, valid=%b}",
                terminationStatus, terminationReason, completedEpochs, plannedEpochs, completedSteps, plannedSteps, lastLoss, modelValid);
    }
}
