package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import eu.kalafatic.evolution.forge.model.inference.KVCache;

import java.util.Objects;
import java.util.Random;

/**
 * Mathematically verified Canonical Multi-Head Attention implementation.
 * Projects input x [seqLen, dModel] to Q, K, V [seqLen, dModel],
 * splits into numHeads subspaces of dimension headDim = dModel / numHeads,
 * computes scaled dot-product attention per head with causal masking,
 * concatenates head outputs [seqLen, dModel], and projects with WO.
 *
 * Provides analytical backward propagation w.r.t WO, WV, WK, WQ and input X.
 */
public class MultiHeadAttention {
    private final Tensor WQ, WK, WV, WO;
    private final int dModel;
    private final int numHeads;
    private final int headDim;
    private final float scale;

    // Cached activations for backward pass
    private Tensor lastX;
    private Tensor lastQ, lastK, lastV;
    private Tensor[] lastSoftmaxPerHead; // [numHeads], each [seqLen, seqLen]
    private Tensor[] lastAttnPerHead;    // [numHeads], each [seqLen, headDim]
    private Tensor lastConcatAttn;       // [seqLen, dModel]

    public MultiHeadAttention(int dModel, int numHeads) {
        if (dModel <= 0) {
            throw new IllegalArgumentException("dModel must be positive: " + dModel);
        }
        if (numHeads <= 0) {
            throw new IllegalArgumentException("numHeads must be positive: " + numHeads);
        }
        if (dModel % numHeads != 0) {
            throw new IllegalArgumentException(String.format(
                    "dModel (%d) must be divisible by numHeads (%d)", dModel, numHeads));
        }

        this.dModel = dModel;
        this.numHeads = numHeads;
        this.headDim = dModel / numHeads;
        this.scale = 1.0f / (float) Math.sqrt(headDim);

        // Weight matrices: [dModel, dModel]
        this.WQ = new SimpleTensor(dModel, dModel);
        this.WK = new SimpleTensor(dModel, dModel);
        this.WV = new SimpleTensor(dModel, dModel);
        this.WO = new SimpleTensor(dModel, dModel);

        // Xavier/He Normal Initialization
        Random r = new Random(42);
        float stdIn = (float) (1.0 / Math.sqrt(dModel));
        for (Tensor t : new Tensor[]{WQ, WK, WV, WO}) {
            float[] data = t.getData();
            for (int i = 0; i < data.length; i++) {
                data[i] = (float) (r.nextGaussian() * stdIn);
            }
        }
    }

    public Tensor forward(Tensor x) {
        Objects.requireNonNull(x, "Input tensor x cannot be null");
        this.lastX = x;
        int seqLen = (int) x.getShape()[0];

        // 1. Linear projections: [seqLen, dModel]
        this.lastQ = x.matmul(WQ);
        this.lastK = x.matmul(WK);
        this.lastV = x.matmul(WV);

        float[] qData = lastQ.getData();
        float[] kData = lastK.getData();
        float[] vData = lastV.getData();

        this.lastSoftmaxPerHead = new Tensor[numHeads];
        this.lastAttnPerHead = new Tensor[numHeads];
        this.lastConcatAttn = new SimpleTensor(seqLen, dModel);
        float[] concatData = lastConcatAttn.getData();

        // 2. Multi-head scaled dot-product attention per head
        for (int h = 0; h < numHeads; h++) {
            int headOffset = h * headDim;

            // Compute scores: Q_h * K_h^T -> [seqLen, seqLen]
            Tensor softProbs = new SimpleTensor(seqLen, seqLen);
            float[] softData = softProbs.getData();

            for (int r = 0; r < seqLen; r++) {
                int qRowOffset = r * dModel + headOffset;
                int scoreRowOffset = r * seqLen;
                float maxVal = Float.NEGATIVE_INFINITY;

                for (int c = 0; c <= r; c++) {
                    int kRowOffset = c * dModel + headOffset;
                    float dot = 0.0f;
                    for (int d = 0; d < headDim; d++) {
                        dot += qData[qRowOffset + d] * kData[kRowOffset + d];
                    }
                    float scaledScore = dot * scale;
                    softData[scoreRowOffset + c] = scaledScore;
                    if (scaledScore > maxVal) {
                        maxVal = scaledScore;
                    }
                }
                for (int c = r + 1; c < seqLen; c++) {
                    softData[scoreRowOffset + c] = -1e9f;
                }

                float sumExp = 0.0f;
                for (int c = 0; c <= r; c++) {
                    float expVal = (float) Math.exp(softData[scoreRowOffset + c] - maxVal);
                    softData[scoreRowOffset + c] = expVal;
                    sumExp += expVal;
                }
                for (int c = r + 1; c < seqLen; c++) {
                    softData[scoreRowOffset + c] = 0.0f;
                }

                if (sumExp > 0.0f) {
                    for (int c = 0; c <= r; c++) {
                        softData[scoreRowOffset + c] /= sumExp;
                    }
                }
            }
            this.lastSoftmaxPerHead[h] = softProbs;

            // Compute head attention: Softmax_h * V_h -> [seqLen, headDim]
            Tensor headAttn = new SimpleTensor(seqLen, headDim);
            float[] headAttnData = headAttn.getData();

            for (int r = 0; r < seqLen; r++) {
                int scoreRowOffset = r * seqLen;
                int outRowOffset = r * headDim;
                for (int c = 0; c <= r; c++) {
                    float p = softData[scoreRowOffset + c];
                    if (p == 0.0f) continue;
                    int vRowOffset = c * dModel + headOffset;
                    for (int d = 0; d < headDim; d++) {
                        headAttnData[outRowOffset + d] += p * vData[vRowOffset + d];
                    }
                }
            }
            this.lastAttnPerHead[h] = headAttn;

            // Copy into concatenated buffer [seqLen, dModel]
            for (int r = 0; r < seqLen; r++) {
                System.arraycopy(headAttnData, r * headDim, concatData, r * dModel + headOffset, headDim);
            }
        }

        // 3. Final linear projection: [seqLen, dModel] * WO [dModel, dModel]
        return lastConcatAttn.matmul(WO);
    }

    public Tensor forwardWithCache(Tensor x, KVCache.LayerKVCache cache) {
        if (cache == null) {
            return forward(x);
        }

        Tensor q = x.matmul(WQ);
        Tensor k = x.matmul(WK);
        Tensor v = x.matmul(WV);

        Tensor K, V;
        if (cache.getKeyCache() == null) {
            cache.setKeyCache(k);
            cache.setValueCache(v);
            K = k;
            V = v;
        } else {
            K = SimpleTensor.concatRowWise(cache.getKeyCache(), k);
            V = SimpleTensor.concatRowWise(cache.getValueCache(), v);
            cache.setKeyCache(K);
            cache.setValueCache(V);
        }

        int qSeqLen = (int) q.getShape()[0];
        int kSeqLen = (int) K.getShape()[0];
        int startPos = kSeqLen - qSeqLen;

        float[] qData = q.getData();
        float[] kData = K.getData();
        float[] vData = V.getData();

        Tensor concatAttn = new SimpleTensor(qSeqLen, dModel);
        float[] concatData = concatAttn.getData();

        for (int h = 0; h < numHeads; h++) {
            int headOffset = h * headDim;

            Tensor softProbs = new SimpleTensor(qSeqLen, kSeqLen);
            float[] softData = softProbs.getData();

            for (int r = 0; r < qSeqLen; r++) {
                int absolutePos = startPos + r;
                int qRowOffset = r * dModel + headOffset;
                int scoreRowOffset = r * kSeqLen;
                float maxVal = Float.NEGATIVE_INFINITY;

                for (int c = 0; c < kSeqLen; c++) {
                    if (c > absolutePos) {
                        softData[scoreRowOffset + c] = -1e9f;
                    } else {
                        int kRowOffset = c * dModel + headOffset;
                        float dot = 0.0f;
                        for (int d = 0; d < headDim; d++) {
                            dot += qData[qRowOffset + d] * kData[kRowOffset + d];
                        }
                        float scaled = dot * scale;
                        softData[scoreRowOffset + c] = scaled;
                        if (scaled > maxVal) {
                            maxVal = scaled;
                        }
                    }
                }

                float sumExp = 0.0f;
                for (int c = 0; c <= absolutePos && c < kSeqLen; c++) {
                    float expVal = (float) Math.exp(softData[scoreRowOffset + c] - maxVal);
                    softData[scoreRowOffset + c] = expVal;
                    sumExp += expVal;
                }
                for (int c = absolutePos + 1; c < kSeqLen; c++) {
                    softData[scoreRowOffset + c] = 0.0f;
                }

                if (sumExp > 0.0f) {
                    for (int c = 0; c <= absolutePos && c < kSeqLen; c++) {
                        softData[scoreRowOffset + c] /= sumExp;
                    }
                }
            }

            for (int r = 0; r < qSeqLen; r++) {
                int scoreRowOffset = r * kSeqLen;
                int targetOffset = r * dModel + headOffset;
                for (int c = 0; c < kSeqLen; c++) {
                    float p = softData[scoreRowOffset + c];
                    if (p == 0.0f) continue;
                    int vRowOffset = c * dModel + headOffset;
                    for (int d = 0; d < headDim; d++) {
                        concatData[targetOffset + d] += p * vData[vRowOffset + d];
                    }
                }
            }
        }

        return concatAttn.matmul(WO);
    }

    public Tensor backward(Tensor dOutput) {
        if (lastX == null || dOutput == null) return dOutput;

        int seqLen = (int) lastX.getShape()[0];

        // 1. dWO = lastConcatAttn^T * dOutput: [dModel, seqLen] * [seqLen, dModel] -> [dModel, dModel]
        Tensor dWO = lastConcatAttn.transpose().matmul(dOutput);
        float[] woGrad = WO.getGrad();
        float[] dWOData = dWO.getData();
        for (int i = 0; i < woGrad.length; i++) {
            woGrad[i] += dWOData[i];
        }

        // 2. dConcatAttn = dOutput * WO^T: [seqLen, dModel] * [dModel, dModel] -> [seqLen, dModel]
        Tensor dConcatAttn = dOutput.matmul(WO.transpose());
        float[] dConcatData = dConcatAttn.getData();

        // Gradient accumulation buffers for Q, K, V
        Tensor dQ = new SimpleTensor(seqLen, dModel);
        Tensor dK = new SimpleTensor(seqLen, dModel);
        Tensor dV = new SimpleTensor(seqLen, dModel);

        float[] dQData = dQ.getData();
        float[] dKData = dK.getData();
        float[] dVData = dV.getData();

        float[] qData = lastQ.getData();
        float[] kData = lastK.getData();
        float[] vData = lastV.getData();

        // 3. Backward propagation per head
        for (int h = 0; h < numHeads; h++) {
            int headOffset = h * headDim;
            float[] softData = lastSoftmaxPerHead[h].getData();

            // Extract dHeadAttn [seqLen, headDim] from dConcatAttn
            float[] dHeadAttn = new float[seqLen * headDim];
            for (int r = 0; r < seqLen; r++) {
                System.arraycopy(dConcatData, r * dModel + headOffset, dHeadAttn, r * headDim, headDim);
            }

            // dV_h = Softmax_h^T * dHeadAttn
            // For position c: dV_h[c, :] = sum_r Softmax_h[r, c] * dHeadAttn[r, :]
            for (int r = 0; r < seqLen; r++) {
                int scoreRowOffset = r * seqLen;
                int dHeadRowOffset = r * headDim;
                for (int c = 0; c <= r; c++) {
                    float p = softData[scoreRowOffset + c];
                    if (p == 0.0f) continue;
                    int vRowOffset = c * dModel + headOffset;
                    for (int d = 0; d < headDim; d++) {
                        dVData[vRowOffset + d] += p * dHeadAttn[dHeadRowOffset + d];
                    }
                }
            }

            // dSoftmax_h = dHeadAttn * V_h^T: [seqLen, headDim] * [headDim, seqLen] -> [seqLen, seqLen]
            float[] dSoftData = new float[seqLen * seqLen];
            for (int r = 0; r < seqLen; r++) {
                int dHeadRowOffset = r * headDim;
                int scoreRowOffset = r * seqLen;
                for (int c = 0; c <= r; c++) {
                    int vRowOffset = c * dModel + headOffset;
                    float dot = 0.0f;
                    for (int d = 0; d < headDim; d++) {
                        dot += dHeadAttn[dHeadRowOffset + d] * vData[vRowOffset + d];
                    }
                    dSoftData[scoreRowOffset + c] = dot;
                }
            }

            // dScores_h from dSoftmax_h via Softmax derivative
            // dScores_h[r, c] = Softmax_h[r, c] * (dSoftmax_h[r, c] - sum_k(dSoftmax_h[r, k] * Softmax_h[r, k])) * scale
            float[] dScoresData = new float[seqLen * seqLen];
            for (int r = 0; r < seqLen; r++) {
                int scoreRowOffset = r * seqLen;
                float dot = 0.0f;
                for (int c = 0; c <= r; c++) {
                    dot += dSoftData[scoreRowOffset + c] * softData[scoreRowOffset + c];
                }
                for (int c = 0; c <= r; c++) {
                    float s = softData[scoreRowOffset + c];
                    float ds = dSoftData[scoreRowOffset + c];
                    dScoresData[scoreRowOffset + c] = s * (ds - dot) * scale;
                }
            }

            // dQ_h = dScores_h * K_h: [seqLen, seqLen] * [seqLen, headDim] -> [seqLen, headDim]
            // dK_h = dScores_h^T * Q_h: [seqLen, seqLen] * [seqLen, headDim] -> [seqLen, headDim]
            for (int r = 0; r < seqLen; r++) {
                int scoreRowOffset = r * seqLen;
                int qRowOffset = r * dModel + headOffset;
                for (int c = 0; c <= r; c++) {
                    float dScore = dScoresData[scoreRowOffset + c];
                    if (dScore == 0.0f) continue;
                    int kRowOffset = c * dModel + headOffset;

                    for (int d = 0; d < headDim; d++) {
                        dQData[qRowOffset + d] += dScore * kData[kRowOffset + d];
                        dKData[kRowOffset + d] += dScore * qData[qRowOffset + d];
                    }
                }
            }
        }

        // 4. Parameter gradients for WQ, WK, WV: lastX^T * dQ, lastX^T * dK, lastX^T * dV
        Tensor dWQ = lastX.transpose().matmul(dQ);
        float[] wqGrad = WQ.getGrad();
        float[] dWQData = dWQ.getData();
        for (int i = 0; i < wqGrad.length; i++) {
            wqGrad[i] += dWQData[i];
        }

        Tensor dWK = lastX.transpose().matmul(dK);
        float[] wkGrad = WK.getGrad();
        float[] dWKData = dWK.getData();
        for (int i = 0; i < wkGrad.length; i++) {
            wkGrad[i] += dWKData[i];
        }

        Tensor dWV = lastX.transpose().matmul(dV);
        float[] wvGrad = WV.getGrad();
        float[] dWVData = dWV.getData();
        for (int i = 0; i < wvGrad.length; i++) {
            wvGrad[i] += dWVData[i];
        }

        // 5. Input gradient w.r.t X: dX = dQ * WQ^T + dK * WK^T + dV * WV^T
        Tensor dXQ = dQ.matmul(WQ.transpose());
        Tensor dXK = dK.matmul(WK.transpose());
        Tensor dXV = dV.matmul(WV.transpose());

        return dXQ.add(dXK).add(dXV);
    }

    public Tensor getWQ() { return WQ; }
    public Tensor getWK() { return WK; }
    public Tensor getWV() { return WV; }
    public Tensor getWO() { return WO; }

    public int getDModel() { return dModel; }
    public int getNumHeads() { return numHeads; }
    public int getHeadDim() { return headDim; }
}
