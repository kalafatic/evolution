package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;

public class TransformerBlock {
    private final MultiHeadAttention attention;
    private final FeedForward ffn;

    public TransformerBlock(int dModel, int numHeads, int dff) {
        this.attention = new MultiHeadAttention(dModel, numHeads);
        this.ffn = new FeedForward(dModel, dff);
    }

    public Tensor forward(Tensor input) {
        // Attention + Residual + LayerNorm
        Tensor attnOut = attention.forward(input);
        Tensor res1 = input.add(attnOut).layerNorm();
        
        // FFN + Residual + LayerNorm
        Tensor ffnOut = ffn.forward(res1);
        return res1.add(ffnOut).layerNorm();
    }
}
