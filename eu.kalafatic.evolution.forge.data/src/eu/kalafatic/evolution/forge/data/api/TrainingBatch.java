package eu.kalafatic.evolution.forge.data.api;

public class TrainingBatch {
    public final int[][] inputIds;
    public final int[][] labels;
    public final float[][] lossMask;
    public final float[][] attentionMask;
    public final int batchSize;

    public TrainingBatch(int[][] inputIds, int[][] labels, float[][] lossMask, float[][] attentionMask) {
        this.inputIds = inputIds;
        this.labels = labels;
        this.lossMask = lossMask;
        this.attentionMask = attentionMask;
        this.batchSize = inputIds != null ? inputIds.length : 0;
    }
}
