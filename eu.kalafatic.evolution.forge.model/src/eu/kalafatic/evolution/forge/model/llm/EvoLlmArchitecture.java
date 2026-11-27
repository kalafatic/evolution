package eu.kalafatic.evolution.forge.model.llm;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable architecture configuration for a native EVO LLM model.
 * Single source of truth for architectural dimensions.
 */
public final class EvoLlmArchitecture implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Positional encoding kinds understood by {@link EvoLlmModel}. */
    public static final String POSITIONAL_ROPE = "rope";
    public static final String POSITIONAL_SINUSOIDAL_ABS = "sinusoidal_abs";

    private final int vocabSize;
    private final int dModel;
    private final int numHeads;
    private final int numBlocks;
    private final int dff;
    private final int maxSeqLen;
    private final String positionalEncoding;

    public EvoLlmArchitecture(int vocabSize, int dModel, int numHeads, int numBlocks, int dff, int maxSeqLen) {
        // RoPE is the current training standard and the only positional encoding that can
        // be represented faithfully by the exported GGUF (general.architecture=llama).
        // Defaulting here to RoPE keeps the native model, the CUDA training backend and
        // the GGUF/Ollama export numerically consistent.
        this(vocabSize, dModel, numHeads, numBlocks, dff, maxSeqLen, POSITIONAL_ROPE);
    }

    public EvoLlmArchitecture(int vocabSize, int dModel, int numHeads, int numBlocks, int dff, int maxSeqLen, String positionalEncoding) {
        if (vocabSize <= 0) {
            throw new IllegalArgumentException("vocabSize must be positive, got: " + vocabSize);
        }
        if (dModel <= 0) {
            throw new IllegalArgumentException("dModel must be positive, got: " + dModel);
        }
        if (numHeads <= 0) {
            throw new IllegalArgumentException("numHeads must be positive, got: " + numHeads);
        }
        if (numBlocks <= 0) {
            throw new IllegalArgumentException("numBlocks must be positive, got: " + numBlocks);
        }
        if (dff <= 0) {
            throw new IllegalArgumentException("dff must be positive, got: " + dff);
        }
        if (maxSeqLen <= 0) {
            throw new IllegalArgumentException("maxSeqLen must be positive, got: " + maxSeqLen);
        }
        if (dModel % numHeads != 0) {
            throw new IllegalArgumentException("dModel (" + dModel + ") must be divisible by numHeads (" + numHeads + ")");
        }
        if (dff <= dModel) {
            throw new IllegalArgumentException("dff (" + dff + ") must be strictly greater than dModel (" + dModel + ")");
        }

        this.vocabSize = vocabSize;
        this.dModel = dModel;
        this.numHeads = numHeads;
        this.numBlocks = numBlocks;
        this.dff = dff;
        this.maxSeqLen = maxSeqLen;
        this.positionalEncoding = (positionalEncoding == null || positionalEncoding.isBlank())
                ? POSITIONAL_SINUSOIDAL_ABS
                : positionalEncoding;
    }

    public static EvoLlmArchitecture of(int vocabSize, int dModel, int numHeads, int numBlocks, int dff, int maxSeqLen) {
        return new EvoLlmArchitecture(vocabSize, dModel, numHeads, numBlocks, dff, maxSeqLen);
    }

    public int getVocabSize() { return vocabSize; }
    public int getDModel() { return dModel; }
    public int getNumHeads() { return numHeads; }
    public int getNumBlocks() { return numBlocks; }
    public int getDff() { return dff; }
    public int getMaxSeqLen() { return maxSeqLen; }

    public String getPositionalEncoding() { return positionalEncoding; }

    public boolean usesRoPE() {
        return POSITIONAL_ROPE.equalsIgnoreCase(positionalEncoding);
    }

    public long getParameterCount() {
        long params = (long) vocabSize * dModel;
        params += (long) numBlocks * (
            4L * dModel * dModel +
            3L * dModel * dff +
            2L * dModel
        );
        params += dModel;
        params += (long) dModel * vocabSize;
        return params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvoLlmArchitecture that = (EvoLlmArchitecture) o;
        return vocabSize == that.vocabSize &&
                dModel == that.dModel &&
                numHeads == that.numHeads &&
                numBlocks == that.numBlocks &&
                dff == that.dff &&
                maxSeqLen == that.maxSeqLen &&
                Objects.equals(positionalEncoding, that.positionalEncoding);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vocabSize, dModel, numHeads, numBlocks, dff, maxSeqLen, positionalEncoding);
    }

    @Override
    public String toString() {
        return String.format("EvoLlmArchitecture[vocabSize=%d, dModel=%d, numHeads=%d, numBlocks=%d, dff=%d, maxSeqLen=%d, positionalEncoding=%s]",
                vocabSize, dModel, numHeads, numBlocks, dff, maxSeqLen, positionalEncoding);
    }
}
