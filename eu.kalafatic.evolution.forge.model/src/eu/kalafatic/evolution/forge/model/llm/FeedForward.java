package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import java.util.Random;

public class FeedForward {
    private final Tensor w1, w2;
    private Tensor lastInput;
    private Tensor h; // Pre-ReLU linear output
    private Tensor hRelu; // Post-ReLU activation

    public FeedForward(int dModel, int dff) {
        this.w1 = new SimpleTensor(dModel, dff);
        this.w2 = new SimpleTensor(dff, dModel);
        initialize();
    }

    private void initialize() {
        Random rand = new Random();
        initWeights(w1, rand, 0.1f);
        initWeights(w2, rand, 0.1f);
    }

    private void initWeights(Tensor t, Random r, float s) {
        float[] d = t.getData();
        for (int i = 0; i < d.length; i++) d[i] = (r.nextFloat() * 2 - 1) * s;
    }

    public Tensor getW1() { return w1; }
    public Tensor getW2() { return w2; }

    public Tensor forward(Tensor input) {
        this.lastInput = input;
        this.h = input.matmul(w1);

        // Deep copy h for activation layer
        long[] hShape = h.getShape();
        float[] hDataCopy = h.getData().clone();
        this.hRelu = new SimpleTensor(hShape, hDataCopy);
        relu(hRelu);

        return hRelu.matmul(w2);
    }

    public Tensor backward(Tensor dOutput) {
        if (lastInput == null) return dOutput;

        // dw2 = hRelu^T * dOutput
        Tensor dw2 = hRelu.transpose().matmul(dOutput);
        accumulateGrad(w2, dw2);

        // dhRelu = dOutput * w2^T
        Tensor dhRelu = dOutput.matmul(w2.transpose());

        // dh = dhRelu if h > 0 else 0 (ReLU backward)
        long[] hShape = h.getShape();
        float[] hData = h.getData();
        float[] dhReluData = dhRelu.getData();
        float[] dhData = new float[hData.length];
        for (int i = 0; i < hData.length; i++) {
            dhData[i] = hData[i] > 0.0f ? dhReluData[i] : 0.0f;
        }
        Tensor dh = new SimpleTensor(hShape, dhData);

        // dw1 = input^T * dh
        Tensor dw1 = lastInput.transpose().matmul(dh);
        accumulateGrad(w1, dw1);

        // dInput = dh * w1^T
        Tensor dInput = dh.matmul(w1.transpose());

        return dInput;
    }

    private void accumulateGrad(Tensor weight, Tensor grad) {
        float[] wGrad = weight.getGrad();
        float[] gData = grad.getData();
        for (int i = 0; i < wGrad.length; i++) {
            wGrad[i] += gData[i];
        }
    }

    private void relu(Tensor t) {
        float[] d = t.getData();
        for (int i = 0; i < d.length; i++) {
            if (d[i] < 0) d[i] = 0;
        }
    }
}
