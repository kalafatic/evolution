package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;

public class TransformerBlock {
    private final MultiHeadAttention attention;
    private final FeedForward ffn;

    public TransformerBlock(int dModel, int numHeads, int dff) {
        this.attention = new MultiHeadAttention(dModel, numHeads);
        this.ffn = new FeedForward(dModel, dff);
    }

    public MultiHeadAttention getAttention() { return attention; }
    public FeedForward getFfn() { return ffn; }

    public Tensor forward(Tensor input) {
        // Attention + Residual + LayerNorm
        Tensor attnOut = attention.forward(input);
        Tensor res1 = input.add(attnOut).layerNorm();
        
        // FFN + Residual + LayerNorm
        Tensor ffnOut = ffn.forward(res1);
        return res1.add(ffnOut).layerNorm();
    }

    public Tensor backward(Tensor dOutput) {
        // dFfnInput = ffn.backward(dOutput)
        Tensor dRes1 = ffn.backward(dOutput);

        // Sum gradients from direct residual and FFN backward path
        Tensor dInputRes = dOutput.add(dRes1);

        // dAttnInput = attention.backward(dInputRes)
        Tensor dAttnOut = attention.backward(dInputRes);

        // Sum gradients from first residual and attention backward path
        Tensor dInput = dInputRes.add(dAttnOut);

        return dInput;
    }
}
