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
    private final int numHeads;
    private final int numBlocks;
    private final int dff;
    private final int maxSeqLen;

    private Tensor lastX; // Cached input to lmHead for backward pass

    public EvoLlmModel(int vocabSize, int dModel, int numHeads, int numBlocks, int dff, int maxSeqLen) {
        this.vocabSize = vocabSize;
        this.dModel = dModel;
        this.numHeads = numHeads;
        this.numBlocks = numBlocks;
        this.dff = dff;
        this.maxSeqLen = maxSeqLen;

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
        
        this.lastX = x;
        return x.matmul(lmHead);
    }

    public void backward(Tensor dLogits) {
        if (lastX == null) return;

        // 1. lmHead backprop
        // logits = x * lmHead, so dx = dLogits * lmHead^T, dlmHead = x^T * dLogits
        Tensor dx = dLogits.matmul(lmHead.transpose());
        Tensor dLmHead = lastX.transpose().matmul(dLogits);

        float[] hGrad = lmHead.getGrad();
        float[] dHeadData = dLmHead.getData();
        for (int i = 0; i < hGrad.length; i++) {
            hGrad[i] += dHeadData[i];
        }

        // 2. TransformerBlocks in reverse order
        for (int i = blocks.size() - 1; i >= 0; i--) {
            dx = blocks.get(i).backward(dx);
        }

        // 3. PositionalEncoding backprop
        dx = posEncoding.backward(dx);

        // 4. Embedding backprop
        embedding.backward(dx);
    }

    public List<Tensor> parameters() {
        List<Tensor> params = new ArrayList<>();
        params.add(embedding.getWeights());
        for (TransformerBlock block : blocks) {
            params.add(block.getAttention().getWQ());
            params.add(block.getAttention().getWK());
            params.add(block.getAttention().getWV());
            params.add(block.getAttention().getWO());
            params.add(block.getFfn().getW1());
            params.add(block.getFfn().getW2());
        }
        params.add(lmHead);
        return params;
    }
    
    public int getVocabSize() { return vocabSize; }
    public int getDModel() { return dModel; }
    public int getNumHeads() { return numHeads; }
    public int getNumBlocks() { return numBlocks; }
    public int getDff() { return dff; }
    public int getMaxSeqLen() { return maxSeqLen; }
    public Tensor getLmHead() { return lmHead; }
}
