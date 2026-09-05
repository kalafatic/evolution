package eu.kalafatic.evolution.forge.model.llm;

import java.io.Serializable;

/**
 * Encapsulates persistent training lifecycle state separately from model weight parameters.
 */
public class TrainingState implements Serializable {

    private static final long serialVersionUID = 1L;

    private int epoch;
    private long step;
    private float lastLoss;
    private float learningRate;
    private long totalTrainedTokens;
    private long timestamp;

    public TrainingState() {
        this.timestamp = System.currentTimeMillis();
    }

    public TrainingState(int epoch, long step, float lastLoss, float learningRate, long totalTrainedTokens) {
        this.epoch = epoch;
        this.step = step;
        this.lastLoss = lastLoss;
        this.learningRate = learningRate;
        this.totalTrainedTokens = totalTrainedTokens;
        this.timestamp = System.currentTimeMillis();
    }

    public int getEpoch() { return epoch; }
    public void setEpoch(int epoch) { this.epoch = epoch; }

    public long getStep() { return step; }
    public void setStep(long step) { this.step = step; }

    public float getLastLoss() { return lastLoss; }
    public void setLastLoss(float lastLoss) { this.lastLoss = lastLoss; }

    public float getLearningRate() { return learningRate; }
    public void setLearningRate(float learningRate) { this.learningRate = learningRate; }

    public long getTotalTrainedTokens() { return totalTrainedTokens; }
    public void setTotalTrainedTokens(long totalTrainedTokens) { this.totalTrainedTokens = totalTrainedTokens; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
