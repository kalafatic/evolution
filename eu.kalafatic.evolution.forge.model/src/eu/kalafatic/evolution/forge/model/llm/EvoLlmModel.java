package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import java.util.ArrayList;
import java.util.List;

public class EvoLlmModel {
    private final Embedding embedding;
    private final PositionalEncoding posEncoding;
    private final List<TransformerBlock> blocks;
    private final RMSNorm outputNorm;
    private final Tensor lmHead;

    private final int vocabSize;
    private final int dModel;
    private final int numHeads;
    private final int numBlocks;
    private final int dff;
    private final int maxSeqLen;
    
    private Tensor lastX;

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
        this.outputNorm = new RMSNorm(dModel);
        this.lmHead = new SimpleTensor(dModel, vocabSize);
        
        float[] hData = lmHead.getData();
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < hData.length; i++) {
            hData[i] = (r.nextFloat() * 2 - 1) * 0.1f;
        }
    }

    public Tensor forward(int[] tokenIds) {
        Tensor x = embedding.forward(tokenIds);
        x = posEncoding.forward(x);
        
        for (TransformerBlock block : blocks) {
            x = block.forward(x);
        }
        
        this.lastX = outputNorm.forward(x);
        return this.lastX.matmul(lmHead);
    }

    public void backward(Tensor dLogits) {
        if (lastX == null) return;

        Tensor dx = dLogits.matmul(lmHead.transpose());
        Tensor dLmHead = lastX.transpose().matmul(dLogits);
        
        float[] hGrad = lmHead.getGrad();
        float[] dHeadData = dLmHead.getData();
        for (int i = 0; i < hGrad.length; i++) {
            hGrad[i] += dHeadData[i];
        }

        dx = outputNorm.backward(dx);

        for (int i = blocks.size() - 1; i >= 0; i--) {
            dx = blocks.get(i).backward(dx);
        }

        dx = posEncoding.backward(dx);
        embedding.backward(dx);
    }

    public List<Tensor> parameters() {
        List<Tensor> params = new ArrayList<>();
        
        // 1. Embedding
        params.add(embedding.getWeights());
        
        // 2. For each block: attn_norm, WQ, WK, WV, WO, ffn_norm, W1 (gate), W3 (up), W2 (down)
        for (TransformerBlock block : blocks) {
            params.add(block.getAttnNorm().getWeight());   // attn_norm
            params.add(block.getAttention().getWQ());      // WQ
            params.add(block.getAttention().getWK());      // WK
            params.add(block.getAttention().getWV());      // WV
            params.add(block.getAttention().getWO());      // WO
            params.add(block.getFfnNorm().getWeight());    // ffn_norm
            params.add(block.getFfn().getW1());            // ffn_gate (W1)
            params.add(block.getFfn().getW3());            // ✅ ffn_up (W3) - ADD THIS
            params.add(block.getFfn().getW2());            // ffn_down (W2)
        }
        
        // 3. Output Norm
        params.add(outputNorm.getWeight());
        
        // 4. LM Head
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
    public RMSNorm getOutputNorm() { return outputNorm; }
    public List<TransformerBlock> getBlocks() { return blocks; }
}