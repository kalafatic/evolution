package eu.kalafatic.evolution.forge.model.inference;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import java.util.Arrays;

public class InferenceResult {
    public enum TerminationReason {
        EOS_REACHED,
        MAX_TOKENS_REACHED,
        STOP_SEQUENCE,
        ERROR
    }

    private final int[] generatedTokenIds;
    private final String generatedText;
    private final Tensor lastLogits;
    private final int generatedTokenCount;
    private final long executionTimeMs;
    private final TerminationReason terminationReason;

    public InferenceResult(int[] generatedTokenIds, String generatedText, Tensor lastLogits,
                           int generatedTokenCount, long executionTimeMs, TerminationReason terminationReason) {
        this.generatedTokenIds = generatedTokenIds != null ? generatedTokenIds.clone() : new int[0];
        this.generatedText = generatedText != null ? generatedText : "";
        this.lastLogits = lastLogits;
        this.generatedTokenCount = generatedTokenCount;
        this.executionTimeMs = executionTimeMs;
        this.terminationReason = terminationReason != null ? terminationReason : TerminationReason.MAX_TOKENS_REACHED;
    }

    public int[] getGeneratedTokenIds() {
        return generatedTokenIds.clone();
    }

    public String getGeneratedText() {
        return generatedText;
    }

    public Tensor getLastLogits() {
        return lastLogits;
    }

    public int getGeneratedTokenCount() {
        return generatedTokenCount;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public TerminationReason getTerminationReason() {
        return terminationReason;
    }

    @Override
    public String toString() {
        return String.format("InferenceResult[tokens=%d, text='%s', reason=%s, timeMs=%d]",
                generatedTokenCount, generatedText, terminationReason, executionTimeMs);
    }
}
