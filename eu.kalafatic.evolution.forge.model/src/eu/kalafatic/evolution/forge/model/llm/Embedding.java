package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import java.util.Random;

public class Embedding {
    private final Tensor weights;
    private final int vocabSize;
    private final int embeddingDim;
    private final int unkTokenId;
    private int[] lastTokenIds;

    public Embedding(int vocabSize, int embeddingDim) {
        this(vocabSize, embeddingDim, 0);
    }

    public Embedding(int vocabSize, int embeddingDim, int unkTokenId) {
        this.vocabSize = vocabSize;
        this.embeddingDim = embeddingDim;
        // The EVO tokenizer convention assigns id 0 to <unk>; never fall back to id 1
        // (which is BOS) unless the caller explicitly configures it.
        this.unkTokenId = unkTokenId;
        this.weights = new SimpleTensor(vocabSize, embeddingDim);
        initialize();
    }

    private void initialize() {
        float[] data = weights.getData();
        Random rand = new Random(42);
        float std = (float) (1.0 / Math.sqrt(embeddingDim));
        for (int i = 0; i < data.length; i++) {
            data[i] = (float) (rand.nextGaussian() * std);
        }
    }

    public Tensor getWeights() {
        return weights;
    }

    public Tensor forward(int[] tokenIds) {
        this.lastTokenIds = tokenIds.clone();
        int seqLen = tokenIds.length;
        Tensor result = new SimpleTensor(seqLen, embeddingDim);
        float[] resData = result.getData();
        float[] wData = weights.getData();
        
        for (int i = 0; i < seqLen; i++) {
            int tokenId = tokenIds[i];
            if (tokenId < 0 || tokenId >= vocabSize) tokenId = unkTokenId;
            System.arraycopy(wData, tokenId * embeddingDim, resData, i * embeddingDim, embeddingDim);
        }
        return result;
    }

    public void backward(Tensor dOutput) {
        if (lastTokenIds == null) return;
        float[] dOutData = dOutput.getData();
        float[] wGrad = weights.getGrad();
        int seqLen = lastTokenIds.length;
        for (int i = 0; i < seqLen; i++) {
            int tokenId = lastTokenIds[i];
            if (tokenId < 0 || tokenId >= vocabSize) tokenId = unkTokenId;
            for (int j = 0; j < embeddingDim; j++) {
                wGrad[tokenId * embeddingDim + j] += dOutData[i * embeddingDim + j];
            }
        }
    }
}
