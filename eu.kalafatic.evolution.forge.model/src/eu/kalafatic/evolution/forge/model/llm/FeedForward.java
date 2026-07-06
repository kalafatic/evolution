package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import java.util.Random;

public class FeedForward {
    private final Tensor w1, w2;

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

    public Tensor forward(Tensor input) {
        // Linear 1 -> ReLU -> Linear 2
        Tensor out = input.matmul(w1);
        relu(out);
        return out.matmul(w2);
    }

    private void relu(Tensor t) {
        float[] d = t.getData();
        for (int i = 0; i < d.length; i++) {
            if (d[i] < 0) d[i] = 0;
        }
    }
}
