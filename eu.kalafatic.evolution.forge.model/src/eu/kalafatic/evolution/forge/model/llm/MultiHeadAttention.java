package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import java.util.Random;

public class MultiHeadAttention {
    private final int dModel;
    private final int numHeads;
    private final int dHead;
    
    private Tensor wQ, wK, wV, wO;
    private Tensor lastInput;
    private Tensor q, k, v;
    private Tensor attnWeights;

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

    public Tensor getWQ() { return wQ; }
    public Tensor getWK() { return wK; }
    public Tensor getWV() { return wV; }
    public Tensor getWO() { return wO; }

    public Tensor forward(Tensor input) {
        this.lastInput = input;
        this.q = input.matmul(wQ);
        this.k = input.matmul(wK);
        this.v = input.matmul(wV);

        int seqLen = (int) input.getShape()[0];
        
        // Attention Score = Softmax(QK^T / sqrt(dHead))
        Tensor scores = q.matmul(k.transpose()).div(new SimpleTensor(new long[]{seqLen, seqLen}, fill((float) Math.sqrt(dHead), seqLen * seqLen)));
        
        // Causal mask
        applyCausalMask(scores);
        
        this.attnWeights = scores.softmax();
        Tensor context = attnWeights.matmul(v);
        
        return context.matmul(wO);
    }

    public Tensor backward(Tensor dOutput) {
        if (lastInput == null) return dOutput;

        int seqLen = (int) lastInput.getShape()[0];

        // 1. Projection Backprop
        // output = context * wO, so dContext = dOutput * wO^T
        Tensor dContext = dOutput.matmul(wO.transpose());
        // dwO = context^T * dOutput
        Tensor context = attnWeights.matmul(v);
        Tensor dwO = context.transpose().matmul(dOutput);
        accumulateGrad(wO, dwO);

        // 2. attnWeights and v Backprop
        // context = attnWeights * v, so dAttnWeights = dContext * v^T, dv = attnWeights^T * dContext
        Tensor dAttnWeights = dContext.matmul(v.transpose());
        Tensor dv = attnWeights.transpose().matmul(dContext);

        // 3. Softmax Backprop
        // For each row i of scores: dScore_ij = softmax_ij * (dSoftmax_ij - sum_k(dSoftmax_ik * softmax_ik))
        Tensor dScores = new SimpleTensor(seqLen, seqLen);
        float[] dScoresData = dScores.getData();
        float[] attnData = attnWeights.getData();
        float[] dAttnData = dAttnWeights.getData();

        for (int i = 0; i < seqLen; i++) {
            float sumTerm = 0.0f;
            for (int k = 0; k < seqLen; k++) {
                int idx = i * seqLen + k;
                sumTerm += dAttnData[idx] * attnData[idx];
            }
            for (int j = 0; j < seqLen; j++) {
                int idx = i * seqLen + j;
                dScoresData[idx] = attnData[idx] * (dAttnData[idx] - sumTerm);
            }
        }

        // Apply scaling factor (1 / sqrt(dHead))
        float scale = 1.0f / (float) Math.sqrt(dHead);
        for (int i = 0; i < dScoresData.length; i++) {
            dScoresData[i] *= scale;
        }

        // Apply mask backprop: masked positions have score = -1e9, gradient is 0
        applyCausalMaskGrad(dScores);

        // 4. Q and K Backprop
        // scores = q * k^T, so dq = dScores * k, dk = dScores^T * q
        Tensor dq = dScores.matmul(k);
        Tensor dk = dScores.transpose().matmul(q);

        // 5. Weight matrices gradients dwQ, dwK, dwV
        Tensor dwQ = lastInput.transpose().matmul(dq);
        Tensor dwK = lastInput.transpose().matmul(dk);
        Tensor dwV = lastInput.transpose().matmul(dv);

        accumulateGrad(wQ, dwQ);
        accumulateGrad(wK, dwK);
        accumulateGrad(wV, dwV);

        // 6. dInput
        // dInput = dq * wQ^T + dk * wK^T + dv * wV^T
        Tensor dInput = dq.matmul(wQ.transpose())
                .add(dk.matmul(wK.transpose()))
                .add(dv.matmul(wV.transpose()));

        return dInput;
    }

    private void accumulateGrad(Tensor weight, Tensor grad) {
        float[] wGrad = weight.getGrad();
        float[] gData = grad.getData();
        for (int i = 0; i < wGrad.length; i++) {
            wGrad[i] += gData[i];
        }
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

    private void applyCausalMaskGrad(Tensor dScores) {
        int seqLen = (int) dScores.getShape()[0];
        float[] data = dScores.getData();
        for (int i = 0; i < seqLen; i++) {
            for (int j = i + 1; j < seqLen; j++) {
                data[i * seqLen + j] = 0.0f;
            }
        }
    }

    private float[] fill(float val, int size) {
        float[] d = new float[size];
        for (int i = 0; i < size; i++) d[i] = val;
        return d;
    }
}
