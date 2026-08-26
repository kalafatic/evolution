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
        float stdIn = (float) (1.0 / Math.sqrt(dModel));
        float stdOut = (float) (1.0 / Math.sqrt(dff));

        for (Tensor t : new Tensor[]{W1, W3}) {
            float[] data = t.getData();
            for (int i = 0; i < data.length; i++) {
                data[i] = (float) (r.nextGaussian() * stdIn);
            }
        }
        float[] w2Data = W2.getData();
        for (int i = 0; i < w2Data.length; i++) {
            w2Data[i] = (float) (r.nextGaussian() * stdOut);
        }
    }
    
    private Tensor lastX;
    private Tensor lastGateRaw;
    private Tensor lastUp;
    private Tensor lastH;

    public Tensor forward(Tensor x) {
        this.lastX = x;
        this.lastGateRaw = x.matmul(W1);
        this.lastUp = x.matmul(W3);
        
        float[] gateRawData = lastGateRaw.getData();
        float[] upData = lastUp.getData();

        Tensor h = new SimpleTensor(lastGateRaw.getShape());
        float[] hData = h.getData();
        
        for (int i = 0; i < gateRawData.length; i++) {
            float z = gateRawData[i];
            float sigmoid = 1.0f / (1.0f + (float) Math.exp(-z));
            float swish = z * sigmoid;
            hData[i] = swish * upData[i];
        }
        this.lastH = h;
        
        return h.matmul(W2);
    }
    
    public Tensor backward(Tensor dOutput) {
        if (lastX == null) return dOutput;

        // 1. W2 gradient: dW2 = lastH^T * dOutput
        Tensor dW2 = lastH.transpose().matmul(dOutput);
        float[] w2Grad = W2.getGrad();
        float[] dW2Data = dW2.getData();
        for (int i = 0; i < w2Grad.length; i++) {
            w2Grad[i] += dW2Data[i];
        }

        // 2. Gradient w.r.t h: dh = dOutput * W2^T
        Tensor dh = dOutput.matmul(W2.transpose());
        float[] dhData = dh.getData();
        float[] gateRawData = lastGateRaw.getData();
        float[] upData = lastUp.getData();

        Tensor dGateRaw = new SimpleTensor(lastGateRaw.getShape());
        float[] dGateRawData = dGateRaw.getData();

        Tensor dUp = new SimpleTensor(lastUp.getShape());
        float[] dUpData = dUp.getData();

        for (int i = 0; i < dhData.length; i++) {
            float z = gateRawData[i];
            float sig = 1.0f / (1.0f + (float) Math.exp(-z));
            float swish = z * sig;
            float dSwish = dhData[i] * upData[i];

            dUpData[i] = dhData[i] * swish;

            // d/dz swish(z) = sig * (1 + z * (1 - sig))
            float dZ = dSwish * (sig * (1.0f + z * (1.0f - sig)));
            dGateRawData[i] = dZ;
        }

        // 3. W3 gradient: dW3 = lastX^T * dUp
        Tensor dW3 = lastX.transpose().matmul(dUp);
        float[] w3Grad = W3.getGrad();
        float[] dW3Data = dW3.getData();
        for (int i = 0; i < w3Grad.length; i++) {
            w3Grad[i] += dW3Data[i];
        }

        // 4. W1 gradient: dW1 = lastX^T * dGateRaw
        Tensor dW1 = lastX.transpose().matmul(dGateRaw);
        float[] w1Grad = W1.getGrad();
        float[] dW1Data = dW1.getData();
        for (int i = 0; i < w1Grad.length; i++) {
            w1Grad[i] += dW1Data[i];
        }

        // 5. Input gradient: dX = dGateRaw * W1^T + dUp * W3^T
        Tensor dX1 = dGateRaw.matmul(W1.transpose());
        Tensor dX3 = dUp.matmul(W3.transpose());
        return dX1.add(dX3);
    }
    
    public Tensor getW1() { return W1; }
    public Tensor getW2() { return W2; }
    public Tensor getW3() { return W3; }
}