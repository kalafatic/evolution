package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;

public class FeedForward {
    private final Tensor W1; // Gate (dModel, dff)
    private final Tensor W2; // Down (dff, dModel)
    private final Tensor W3; // Up (dModel, dff) - for SwiGLU
    
    public FeedForward(int dModel, int dff) {
        this.W1 = new SimpleTensor(dModel, dff);
        this.W2 = new SimpleTensor(dff, dModel);
        this.W3 = new SimpleTensor(dModel, dff);
        
        // Initialize
        java.util.Random r = new java.util.Random();
        for (Tensor t : new Tensor[]{W1, W2, W3}) {
            float[] data = t.getData();
            for (int i = 0; i < data.length; i++) {
                data[i] = (r.nextFloat() * 2 - 1) * 0.02f;
            }
        }
    }
    
    public Tensor forward(Tensor x) {
        // Standard LLaMA FFN: SwiGLU
        // gate = x * W1, up = x * W3, down = (gate * sigmoid(gate)) * up * W2
        // Simplified to: x * W1 * W2 (for testing)
        Tensor hidden = x.matmul(W1);
        return hidden.matmul(W2);
    }
    
    public Tensor backward(Tensor dOutput) {
        return dOutput; // Simplified
    }
    
    public Tensor getW1() { return W1; }
    public Tensor getW2() { return W2; }
    public Tensor getW3() { return W3; }
}