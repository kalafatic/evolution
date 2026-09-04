package eu.kalafatic.evolution.forge.data.api;

public class TrainingSample {
    public final int[] inputIds;
    public final int[] labels;
    public final boolean[] lossMask;
    public final float[] attentionMask;

    public TrainingSample(int[] inputIds, int[] labels, boolean[] lossMask, float[] attentionMask) {
        this.inputIds = inputIds;
        this.labels = labels;
        this.lossMask = lossMask;
        this.attentionMask = attentionMask;
    }

    public float[] getAttentionMaskAsFloat() {
        if (attentionMask != null) return attentionMask;
        if (inputIds == null) return new float[0];
        float[] mask = new float[inputIds.length];
        for (int i = 0; i < mask.length; i++) mask[i] = 1.0f;
        return mask;
    }
}
