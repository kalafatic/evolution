package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import java.util.Random;

public class MultiHeadAttention {
    private final int dModel;
    private final int numHeads;
    private final int dHead;

    private Tensor wQ, wK, wV, wO;

    public MultiHeadAttention(int dModel, int numHeads) {
        this.dModel = dModel;
        this.numHeads = numHeads;
        this.dHead = dModel / numHeads;

        this.wQ = new SimpleTensor(dModel, dModel);
        this.wK = new SimpleTensor(dModel, dModel);
        this.wV = new SimpleTensor(dModel, dModel);
        this.wO = new SimpleTensor(dModel, dModel);
        initialize();
    }

    private void initialize() {
        Random rand = new Random();
        float scale = (float) Math.sqrt(1.0 / dModel);
        initWeights(wQ, rand, scale);
        initWeights(wK, rand, scale);
        initWeights(wV, rand, scale);
        initWeights(wO, rand, scale);
    }

    private void initWeights(Tensor t, Random r, float s) {
        float[] d = t.getData();
        for (int i = 0; i < d.length; i++) d[i] = (r.nextFloat() * 2 - 1) * s;
    }

    public Tensor forward(Tensor input) {
        // Simplified Single-Head Attention Logic for educational purposes
        // Multi-head would split the tensors and run in parallel
        Tensor q = input.matmul(wQ);
        Tensor k = input.matmul(wK);
        Tensor v = input.matmul(wV);

        int seqLen = (int) input.getShape()[0];

        // Attention Score = Softmax(QK^T / sqrt(dHead))
        Tensor scores = q.matmul(k.transpose()).div(new SimpleTensor(new long[]{seqLen, seqLen}, fill((float) Math.sqrt(dHead), seqLen * seqLen)));

        // Causal mask (optional but good for LLM)
        applyCausalMask(scores);

        Tensor attnWeights = scores.softmax();
        Tensor output = attnWeights.matmul(v);

        return output.matmul(wO);
    }

    private void applyCausalMask(Tensor scores) {
        int seqLen = (int) scores.getShape()[0];
        float[] data = scores.getData();
        for (int i = 0; i < seqLen; i++) {
            for (int j = i + 1; j < seqLen; j++) {
                data[i * seqLen + j] = -1e9f;
            }
        }
    }

    private float[] fill(float val, int size) {
        float[] d = new float[size];
        for (int i = 0; i < size; i++) d[i] = val;
        return d;
    }
}
