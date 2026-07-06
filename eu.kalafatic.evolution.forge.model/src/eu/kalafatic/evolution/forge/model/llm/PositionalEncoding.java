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
        Tensor posPart = new SimpleTensor(seqLen, dModel);
        System.arraycopy(encoding.getData(), 0, posPart.getData(), 0, seqLen * dModel);
        return input.add(posPart);
    }
}
