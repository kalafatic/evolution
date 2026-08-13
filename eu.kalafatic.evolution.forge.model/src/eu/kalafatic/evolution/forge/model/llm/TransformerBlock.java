package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;

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
    
    public Tensor backward(Tensor dOutput) {
        // 1. FFN backward
        Tensor dAfterAttn = dOutput;
        
        // Gradient through residual connection
        Tensor dFFN = dAfterAttn; // Since output = afterAttn + ffnOut
        Tensor dNorm2 = ffnNorm.backward(dFFN);
        Tensor dFFNInput = ffn.backward(dNorm2);
        
        // Gradient through second residual
        Tensor dAttnOut = dAfterAttn; // Since afterAttn = x + attnOut
        Tensor dNorm1 = attnNorm.backward(dAttnOut);
        Tensor dAttnInput = attention.backward(dNorm1);
        
        // Combine gradients through both residual paths
        return dFFNInput.add(dAttnInput);
    }
    
    public MultiHeadAttention getAttention() { return attention; }
    public FeedForward getFfn() { return ffn; }
    public RMSNorm getAttnNorm() { return attnNorm; }
    public RMSNorm getFfnNorm() { return ffnNorm; }
}