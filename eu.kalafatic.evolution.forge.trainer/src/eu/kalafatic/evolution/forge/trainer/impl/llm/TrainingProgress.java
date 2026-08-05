package eu.kalafatic.evolution.forge.trainer.impl.llm;

import java.time.Instant;

public final class TrainingProgress {

    private final String jobId;
    private final TrainingStatus status;
    private final String phase;
    private final int currentEpoch;
    private final int totalEpochs;
    private final long currentStep;
    private final long totalSteps;
    private final long currentBatch;
    private final long batchesPerEpoch;
    private final double progressPercent;
    private final double trainingLoss;
    private final double validationLoss;
    private final double learningRate;
    private final double stepsPerSecond;
    private final long elapsedMillis;
    private final long estimatedRemainingMillis;
    private final Instant timestamp;
    private final String message;

    public TrainingProgress(
            String jobId,
            TrainingStatus status,
            String phase,
            int currentEpoch,
            int totalEpochs,
            long currentStep,
            long totalSteps,
            long currentBatch,
            long batchesPerEpoch,
            double progressPercent,
            double trainingLoss,
            double validationLoss,
            double learningRate,
            double stepsPerSecond,
            long elapsedMillis,
            long estimatedRemainingMillis,
            Instant timestamp,
            String message) {
        this.jobId = jobId;
        this.status = status;
        this.phase = phase;
        this.currentEpoch = currentEpoch;
        this.totalEpochs = totalEpochs;
        this.currentStep = currentStep;
        this.totalSteps = totalSteps;
        this.currentBatch = currentBatch;
        this.batchesPerEpoch = batchesPerEpoch;
        this.progressPercent = progressPercent;
        this.trainingLoss = trainingLoss;
        this.validationLoss = validationLoss;
        this.learningRate = learningRate;
        this.stepsPerSecond = stepsPerSecond;
        this.elapsedMillis = elapsedMillis;
        this.estimatedRemainingMillis = estimatedRemainingMillis;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.message = message;
    }

    public String getJobId() {
        return jobId;
    }

    public TrainingStatus getStatus() {
        return status;
    }

    public String getPhase() {
        return phase;
    }

    public int getCurrentEpoch() {
        return currentEpoch;
    }

    public int getTotalEpochs() {
        return totalEpochs;
    }

    public long getCurrentStep() {
        return currentStep;
    }

    public long getTotalSteps() {
        return totalSteps;
    }

    public long getCurrentBatch() {
        return currentBatch;
    }

    public long getBatchesPerEpoch() {
        return batchesPerEpoch;
    }

    public double getProgressPercent() {
        return progressPercent;
    }

    public double getTrainingLoss() {
        return trainingLoss;
    }

    public double getValidationLoss() {
        return validationLoss;
    }

    public double getLearningRate() {
        return learningRate;
    }

    public double getStepsPerSecond() {
        return stepsPerSecond;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public long getEstimatedRemainingMillis() {
        return estimatedRemainingMillis;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String jobId;
        private TrainingStatus status;
        private String phase;
        private int currentEpoch;
        private int totalEpochs;
        private long currentStep;
        private long totalSteps;
        private long currentBatch;
        private long batchesPerEpoch;
        private double progressPercent;
        private double trainingLoss;
        private double validationLoss;
        private double learningRate;
        private double stepsPerSecond;
        private long elapsedMillis;
        private long estimatedRemainingMillis;
        private Instant timestamp;
        private String message;

        public Builder() {}

        public Builder from(TrainingProgress other) {
            this.jobId = other.jobId;
            this.status = other.status;
            this.phase = other.phase;
            this.currentEpoch = other.currentEpoch;
            this.totalEpochs = other.totalEpochs;
            this.currentStep = other.currentStep;
            this.totalSteps = other.totalSteps;
            this.currentBatch = other.currentBatch;
            this.batchesPerEpoch = other.batchesPerEpoch;
            this.progressPercent = other.progressPercent;
            this.trainingLoss = other.trainingLoss;
            this.validationLoss = other.validationLoss;
            this.learningRate = other.learningRate;
            this.stepsPerSecond = other.stepsPerSecond;
            this.elapsedMillis = other.elapsedMillis;
            this.estimatedRemainingMillis = other.estimatedRemainingMillis;
            this.timestamp = other.timestamp;
            this.message = other.message;
            return this;
        }

        public Builder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        public Builder status(TrainingStatus status) {
            this.status = status;
            return this;
        }

        public Builder phase(String phase) {
            this.phase = phase;
            return this;
        }

        public Builder currentEpoch(int currentEpoch) {
            this.currentEpoch = currentEpoch;
            return this;
        }

        public Builder totalEpochs(int totalEpochs) {
            this.totalEpochs = totalEpochs;
            return this;
        }

        public Builder currentStep(long currentStep) {
            this.currentStep = currentStep;
            return this;
        }

        public Builder totalSteps(long totalSteps) {
            this.totalSteps = totalSteps;
            return this;
        }

        public Builder currentBatch(long currentBatch) {
            this.currentBatch = currentBatch;
            return this;
        }

        public Builder batchesPerEpoch(long batchesPerEpoch) {
            this.batchesPerEpoch = batchesPerEpoch;
            return this;
        }

        public Builder progressPercent(double progressPercent) {
            this.progressPercent = progressPercent;
            return this;
        }

        public Builder trainingLoss(double trainingLoss) {
            this.trainingLoss = trainingLoss;
            return this;
        }

        public Builder validationLoss(double validationLoss) {
            this.validationLoss = validationLoss;
            return this;
        }

        public Builder learningRate(double learningRate) {
            this.learningRate = learningRate;
            return this;
        }

        public Builder stepsPerSecond(double stepsPerSecond) {
            this.stepsPerSecond = stepsPerSecond;
            return this;
        }

        public Builder elapsedMillis(long elapsedMillis) {
            this.elapsedMillis = elapsedMillis;
            return this;
        }

        public Builder estimatedRemainingMillis(long estimatedRemainingMillis) {
            this.estimatedRemainingMillis = estimatedRemainingMillis;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public TrainingProgress build() {
            return new TrainingProgress(
                    jobId,
                    status,
                    phase,
                    currentEpoch,
                    totalEpochs,
                    currentStep,
                    totalSteps,
                    currentBatch,
                    batchesPerEpoch,
                    progressPercent,
                    trainingLoss,
                    validationLoss,
                    learningRate,
                    stepsPerSecond,
                    elapsedMillis,
                    estimatedRemainingMillis,
                    timestamp,
                    message
            );
        }
    }
}
