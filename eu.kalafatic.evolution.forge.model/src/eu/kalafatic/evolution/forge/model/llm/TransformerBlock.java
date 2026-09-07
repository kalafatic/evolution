package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import eu.kalafatic.evolution.forge.model.inference.KVCache;

public class TransformerBlock {
    private final MultiHeadAttention attention;
    private final FeedForward ffn;
    private final RMSNorm attnNorm;
    private final RMSNorm ffnNorm;
    
    private Tensor attnOutput;  // Cached for backward
    private Tensor ffnOutput;   // Cached for backward
    
    public TransformerBlock(int dModel, int numHeads, int dff) {
        this.attention = new MultiHeadAttention(dModel, numHeads);
        this.ffn = new FeedForward(dModel, dff);
        this.attnNorm = new RMSNorm(dModel);
        this.ffnNorm = new RMSNorm(dModel);
    }
    
    public Tensor forward(Tensor x) {
        // Pre-norm architecture (standard LLaMA)
        // 1. Attention with residual
        Tensor norm1 = attnNorm.forward(x);
        Tensor attnOut = attention.forward(norm1);
        this.attnOutput = attnOut;
        
        // Add & Norm
        Tensor afterAttn = x.add(attnOut);
        
        // 2. FFN with residual
        Tensor norm2 = ffnNorm.forward(afterAttn);
        Tensor ffnOut = ffn.forward(norm2);
        this.ffnOutput = ffnOut;
        
        // Add & Norm
        return afterAttn.add(ffnOut);
    }

    public Tensor forwardWithCache(Tensor x, KVCache.LayerKVCache cache) {
        Tensor norm1 = attnNorm.forward(x);
        Tensor attnOut = attention.forwardWithCache(norm1, cache);
        this.attnOutput = attnOut;

        Tensor afterAttn = x.add(attnOut);

        Tensor norm2 = ffnNorm.forward(afterAttn);
        Tensor ffnOut = ffn.forward(norm2);
        this.ffnOutput = ffnOut;

        return afterAttn.add(ffnOut);
    }
    
    public Tensor backward(Tensor dOutput) {
        // 1. FFN backward: f = ffn(ffnNorm(afterAttn))
        Tensor dX2 = ffn.backward(dOutput);
        Tensor dAfterAttn_ffn = ffnNorm.backward(dX2);
        
        // Total gradient w.r.t. afterAttn (direct residual path + FFN path)
        Tensor dAfterAttn = dOutput.add(dAfterAttn_ffn);
        
        // 2. Attention backward: a = attention(attnNorm(x))
        Tensor dX1 = attention.backward(dAfterAttn);
        Tensor dX_attn = attnNorm.backward(dX1);
        
        // Total gradient w.r.t. x (direct residual path + Attention path)
        return dAfterAttn.add(dX_attn);
    }
    
    public MultiHeadAttention getAttention() { return attention; }
    public FeedForward getFfn() { return ffn; }
    public RMSNorm getAttnNorm() { return attnNorm; }
    public RMSNorm getFfnNorm() { return ffnNorm; }
}