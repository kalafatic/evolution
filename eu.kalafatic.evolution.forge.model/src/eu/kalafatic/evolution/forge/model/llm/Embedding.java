package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import java.util.Random;

public class Embedding {
    private final Tensor weights;
    private final int vocabSize;
    private final int embeddingDim;
    private int[] lastTokenIds;

    public Embedding(int vocabSize, int embeddingDim) {
        this.vocabSize = vocabSize;
        this.embeddingDim = embeddingDim;
        this.weights = new SimpleTensor(vocabSize, embeddingDim);
        initialize();
    }

    private void initialize() {
        float[] data = weights.getData();
        Random rand = new Random(42);
        for (int i = 0; i < data.length; i++) {
            data[i] = (rand.nextFloat() * 2 - 1) * 0.1f;
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
            if (tokenId < 0 || tokenId >= vocabSize) tokenId = 1; // UNK
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
            if (tokenId < 0 || tokenId >= vocabSize) tokenId = 1;
            for (int j = 0; j < embeddingDim; j++) {
                wGrad[tokenId * embeddingDim + j] += dOutData[i * embeddingDim + j];
            }
        }
    }
}
