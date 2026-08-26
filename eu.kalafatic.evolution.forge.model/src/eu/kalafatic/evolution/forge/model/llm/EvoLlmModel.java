package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EvoLlmModel {
    private final Embedding embedding;
    private final PositionalEncoding posEncoding;
    private final List<TransformerBlock> blocks;
    private final RMSNorm outputNorm;
    private final Tensor lmHead;

    private final EvoLlmArchitecture architecture;
    
    private Tensor lastX;

    public EvoLlmModel(EvoLlmArchitecture architecture) {
        this.architecture = Objects.requireNonNull(architecture, "architecture cannot be null");

        this.embedding = new Embedding(architecture.getVocabSize(), architecture.getDModel());
        this.posEncoding = new PositionalEncoding(architecture.getMaxSeqLen(), architecture.getDModel());
        this.blocks = new ArrayList<>();
        for (int i = 0; i < architecture.getNumBlocks(); i++) {
            blocks.add(new TransformerBlock(architecture.getDModel(), architecture.getNumHeads(), architecture.getDff()));
        }
        this.outputNorm = new RMSNorm(architecture.getDModel());
        this.lmHead = new SimpleTensor(architecture.getDModel(), architecture.getVocabSize());
        
        float[] hData = lmHead.getData();
        java.util.Random r = new java.util.Random();
        float std = (float) (1.0 / Math.sqrt(architecture.getDModel()));
        for (int i = 0; i < hData.length; i++) {
            hData[i] = (float) (r.nextGaussian() * std);
        }
    }

    public EvoLlmModel(int vocabSize, int dModel, int numHeads, int numBlocks, int dff, int maxSeqLen) {
        this(new EvoLlmArchitecture(vocabSize, dModel, numHeads, numBlocks, dff, maxSeqLen));
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
            params.add(block.getFfn().getW3());            // ffn_up (W3)
            params.add(block.getFfn().getW2());            // ffn_down (W2)
        }
        
        // 3. Output Norm
        params.add(outputNorm.getWeight());
        
        // 4. LM Head
        params.add(lmHead);
        
        return params;
    }
    
    public EvoLlmArchitecture getArchitecture() { return architecture; }
    public int getVocabSize() { return architecture.getVocabSize(); }
    public int getDModel() { return architecture.getDModel(); }
    public int getNumHeads() { return architecture.getNumHeads(); }
    public int getNumBlocks() { return architecture.getNumBlocks(); }
    public int getDff() { return architecture.getDff(); }
    public int getMaxSeqLen() { return architecture.getMaxSeqLen(); }
    public Tensor getLmHead() { return lmHead; }
    public RMSNorm getOutputNorm() { return outputNorm; }
    public List<TransformerBlock> getBlocks() { return blocks; }

	
}
