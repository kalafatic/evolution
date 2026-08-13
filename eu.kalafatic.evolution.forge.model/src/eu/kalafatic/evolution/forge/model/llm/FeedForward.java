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
        
        java.util.Random r = new java.util.Random();
        for (Tensor t : new Tensor[]{W1, W2, W3}) {
            float[] data = t.getData();
            for (int i = 0; i < data.length; i++) {
                data[i] = (r.nextFloat() * 2 - 1) * 0.02f;
            }
        }
    }
    
    public Tensor forward(Tensor x) {
        // Proper SwiGLU: gate = x * W1, up = x * W3, output = swish(gate) * up * W2
        Tensor gate = x.matmul(W1);
        Tensor up = x.matmul(W3);
        
        // Apply Swish activation: swish(x) = x * sigmoid(x)
        float[] gateData = gate.getData();
        for (int i = 0; i < gateData.length; i++) {
            float sigmoid = 1.0f / (1.0f + (float) Math.exp(-gateData[i]));
            gateData[i] = gateData[i] * sigmoid;
        }
        
        // Element-wise multiply gate with up
        float[] upData = up.getData();
        for (int i = 0; i < gateData.length; i++) {
            gateData[i] = gateData[i] * upData[i];
        }
        
        // Project down with W2
        return gate.matmul(W2);
    }
    
    public Tensor backward(Tensor dOutput) {
        // Simplified - full backward pass would be more complex
        return dOutput;
    }
    
    public Tensor getW1() { return W1; }
    public Tensor getW2() { return W2; }
    public Tensor getW3() { return W3; }
}