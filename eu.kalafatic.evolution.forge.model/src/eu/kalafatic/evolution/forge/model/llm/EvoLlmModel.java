package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import java.util.ArrayList;
import java.util.List;

public class EvoLlmModel {
    private final Embedding embedding;
    private final PositionalEncoding posEncoding;
    private final List<TransformerBlock> blocks;
    private final Tensor lmHead; // Linear mapping to vocab size

    private final int vocabSize;
    private final int dModel;

    public EvoLlmModel(int vocabSize, int dModel, int numHeads, int numBlocks, int dff, int maxSeqLen) {
        this.vocabSize = vocabSize;
        this.dModel = dModel;
        this.embedding = new Embedding(vocabSize, dModel);
        this.posEncoding = new PositionalEncoding(maxSeqLen, dModel);
        this.blocks = new ArrayList<>();
        for (int i = 0; i < numBlocks; i++) {
            blocks.add(new TransformerBlock(dModel, numHeads, dff));
        }
        this.lmHead = new SimpleTensor(dModel, vocabSize);
        // Initialize head weights
        float[] hData = lmHead.getData();
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < hData.length; i++) hData[i] = (r.nextFloat() * 2 - 1) * 0.1f;
    }

    public Tensor forward(int[] tokenIds) {
        Tensor x = embedding.forward(tokenIds);
        x = posEncoding.forward(x);

        for (TransformerBlock block : blocks) {
            x = block.forward(x);
        }

        return x.matmul(lmHead);
    }

    public int getVocabSize() { return vocabSize; }
    public int getDModel() { return dModel; }
    public Tensor getLmHead() { return lmHead; }
}
