package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;

public class PositionalEncoding {
    private final int maxSeqLen;
    private final int dModel;
    private final Tensor encoding;

    public PositionalEncoding(int maxSeqLen, int dModel) {
        this.maxSeqLen = maxSeqLen;
        this.dModel = dModel;
        this.encoding = new SimpleTensor(maxSeqLen, dModel);
        calculateEncodings();
    }

    private void calculateEncodings() {
        float[] data = encoding.getData();
        for (int pos = 0; pos < maxSeqLen; pos++) {
            for (int i = 0; i < dModel; i += 2) {
                double divTerm = Math.pow(10000.0, (double) i / dModel);
                data[pos * dModel + i] = (float) Math.sin(pos / divTerm);
                if (i + 1 < dModel) {
                    data[pos * dModel + i + 1] = (float) Math.cos(pos / divTerm);
                }
            }
        }
    }

    public Tensor forward(Tensor input) {
        int seqLen = (int) input.getShape()[0];
        validateSeqLen(seqLen, 0);
        Tensor posPart = new SimpleTensor(seqLen, dModel);
        System.arraycopy(encoding.getData(), 0, posPart.getData(), 0, seqLen * dModel);
        return input.add(posPart);
    }

    /**
     * Forward pass for autoregressive generation: applies positional encodings
     * for positions [startPos, startPos + seqLen) instead of always starting at 0.
     */
    public Tensor forwardAtOffset(Tensor input, int startPos) {
        if (startPos < 0) {
            throw new IllegalArgumentException("Position offset cannot be negative: " + startPos);
        }
        int seqLen = (int) input.getShape()[0];
        validateSeqLen(seqLen, startPos);
        Tensor posPart = new SimpleTensor(seqLen, dModel);
        System.arraycopy(encoding.getData(), startPos * dModel, posPart.getData(), 0, seqLen * dModel);
        return input.add(posPart);
    }

    private void validateSeqLen(int seqLen, int offset) {
        if ((long) seqLen + offset > maxSeqLen) {
            throw new IllegalArgumentException("Sequence length (" + seqLen + ") at offset (" + offset
                    + ") exceeds positional encoding table length (" + maxSeqLen + ")");
        }
    }

    public Tensor backward(Tensor dOutput) {
        return dOutput;
    }
}
