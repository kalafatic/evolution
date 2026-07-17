package eu.kalafatic.evolution.forge.trainer.impl;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import java.util.ArrayList;
import java.util.List;

public class EvoAdamW {
    private final float lr;
    private final float beta1;
    private final float beta2;
    private final float eps;
    private final float weightDecay;

    private final List<float[]> mList = new ArrayList<>();
    private final List<float[]> vList = new ArrayList<>();
    private int t = 0;

    public EvoAdamW(float lr, float beta1, float beta2, float eps, float weightDecay) {
        this.lr = lr;
        this.beta1 = beta1;
        this.beta2 = beta2;
        this.eps = eps;
        this.weightDecay = weightDecay;
    }

    public float getLr() {
        return lr;
    }

    public void step(List<Tensor> parameters) {
        t++;
        // Initialize moments if first step
        if (mList.isEmpty()) {
            for (Tensor p : parameters) {
                mList.add(new float[(int) p.getSize()]);
                vList.add(new float[(int) p.getSize()]);
            }
        }

        for (int i = 0; i < parameters.size(); i++) {
            Tensor p = parameters.get(i);
            float[] data = p.getData();
            float[] grad = p.getGrad();
            float[] m = mList.get(i);
            float[] v = vList.get(i);

            for (int j = 0; j < data.length; j++) {
                float g = grad[j];

                // 1. Weight decay
                if (weightDecay > 0.0f) {
                    data[j] -= lr * weightDecay * data[j];
                }

                // 2. Update biased first moment estimate
                m[j] = beta1 * m[j] + (1.0f - beta1) * g;

                // 3. Update biased second raw moment estimate
                v[j] = beta2 * v[j] + (1.0f - beta2) * g * g;

                // 4. Compute bias-corrected first moment estimate
                float mHat = m[j] / (1.0f - (float) Math.pow(beta1, t));

                // 5. Compute bias-corrected second raw moment estimate
                float vHat = v[j] / (1.0f - (float) Math.pow(beta2, t));

                // 6. Update parameters
                data[j] -= lr * mHat / ((float) Math.sqrt(vHat) + eps);
            }
        }
    }
}
