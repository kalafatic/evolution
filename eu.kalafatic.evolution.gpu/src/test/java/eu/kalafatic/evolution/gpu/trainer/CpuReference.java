package eu.kalafatic.evolution.gpu.trainer;

/**
 * Independent CPU reference implementation of the transformer forward pass, used to
 * verify the numerical correctness of the GPU kernels. This is a plain-Java mirror of
 * the exact mathematics implemented in {@link GpuTrainingEngine}'s CUDA kernels.
 */
public final class CpuReference {

    private CpuReference() {}

    public static float[] forward(GpuTrainingEngine.Config cfg, float[] w, int[] tokens) {
        int V = cfg.vocabSize, D = cfg.dModel, H = cfg.numHeads, L = cfg.numBlocks, F = cfg.dff;
        int HD = D / H;
        int emb = V * D, qkv = D * D, w1 = D * F, w2 = F * D, norm = D;
        int offOut = emb + L * (norm + 4 * qkv + norm + 2 * w1 + w2);
        int offHead = offOut + norm;
        int T = tokens.length;

        float[] x = new float[T * D];
        for (int t = 0; t < T; t++)
            System.arraycopy(w, tokens[t] * D, x, t * D, D);

        for (int b = 0; b < L; b++) {
            int oAttnNorm = emb + b * (norm + 4 * qkv + norm + 2 * w1 + w2);
            int oWQ = oAttnNorm + norm, oWK = oWQ + qkv, oWV = oWK + qkv, oWO = oWV + qkv;
            int oFfnNorm = oWO + qkv, oW1 = oFfnNorm + norm, oW3 = oW1 + w1, oW2 = oW3 + w1;

            float[] n1 = rmsnorm(x, w, oAttnNorm, T, D);
            float[] Q = matmul(n1, w, oWQ, T, D, D);
            float[] K = matmul(n1, w, oWK, T, D, D);
            float[] Vv = matmul(n1, w, oWV, T, D, D);
            rope(Q, T, D, H);
            rope(K, T, D, H);
            float[] attn = attention(Q, K, Vv, T, D, H);
            float[] attnProj = matmul(attn, w, oWO, T, D, D);
            x = add(x, attnProj);

            float[] n2 = rmsnorm(x, w, oFfnNorm, T, D);
            float[] gate = matmul(n2, w, oW1, T, F, D);
            float[] up = matmul(n2, w, oW3, T, F, D);
            float[] h = swiglu(gate, up);
            float[] ffnOut = matmul(h, w, oW2, T, D, F);
            x = add(x, ffnOut);
        }

        float[] fin = rmsnorm(x, w, offOut, T, D);
        return matmul(fin, w, offHead, T, V, D);
    }

    private static float[] rmsnorm(float[] x, float[] w, int wOff, int T, int D) {
        float[] y = new float[T * D];
        for (int r = 0; r < T; r++) {
            float sumSq = 0;
            for (int i = 0; i < D; i++) { float v = x[r * D + i]; sumSq += v * v; }
            float rms = (float) Math.sqrt(sumSq / D + 1e-5f);
            for (int i = 0; i < D; i++) y[r * D + i] = (x[r * D + i] / rms) * w[wOff + i];
        }
        return y;
    }

    private static float[] matmul(float[] A, float[] B, int bOff, int M, int N, int K) {
        float[] C = new float[M * N];
        for (int r = 0; r < M; r++)
            for (int c = 0; c < N; c++) {
                float s = 0;
                for (int k = 0; k < K; k++) s += A[r * K + k] * B[bOff + k * N + c];
                C[r * N + c] = s;
            }
        return C;
    }

    private static void rope(float[] data, int T, int D, int H) {
        int HD = D / H, pairs = HD / 2;
        for (int r = 0; r < T; r++)
            for (int h = 0; h < H; h++) {
                int base = r * D + h * HD;
                for (int i = 0; i < pairs; i++) {
                    double theta = r / Math.pow(10000.0, (2.0 * i) / HD);
                    float c = (float) Math.cos(theta), s = (float) Math.sin(theta);
                    int p = base + 2 * i;
                    float x0 = data[p], x1 = data[p + 1];
                    data[p] = x0 * c - x1 * s;
                    data[p + 1] = x0 * s + x1 * c;
                }
            }
    }

    private static float[] attention(float[] Q, float[] K, float[] Vv, int T, int D, int H) {
        int HD = D / H;
        float scale = 1.0f / (float) Math.sqrt(HD);
        float[] out = new float[T * D];
        for (int h = 0; h < H; h++) {
            int ho = h * HD;
            for (int r = 0; r < T; r++) {
                float maxv = -1e30f;
                for (int c = 0; c <= r; c++) {
                    float dot = 0;
                    for (int d = 0; d < HD; d++) dot += Q[r * D + ho + d] * K[c * D + ho + d];
                    float s = dot * scale;
                    if (s > maxv) maxv = s;
                }
                float sum = 0;
                for (int c = 0; c <= r; c++) {
                    float dot = 0;
                    for (int d = 0; d < HD; d++) dot += Q[r * D + ho + d] * K[c * D + ho + d];
                    sum += Math.exp(dot * scale - maxv);
                }
                for (int d = 0; d < HD; d++) {
                    float acc = 0;
                    for (int c = 0; c <= r; c++) {
                        float dot = 0;
                        for (int dd = 0; dd < HD; dd++) dot += Q[r * D + ho + dd] * K[c * D + ho + dd];
                        acc += Math.exp(dot * scale - maxv) * Vv[c * D + ho + d];
                    }
                    out[r * D + ho + d] = acc / sum;
                }
            }
        }
        return out;
    }

    private static float[] swiglu(float[] gate, float[] up) {
        float[] h = new float[gate.length];
        for (int i = 0; i < gate.length; i++) {
            float z = gate[i];
            float sig = 1.0f / (1.0f + (float) Math.exp(-z));
            h[i] = (z * sig) * up[i];
        }
        return h;
    }

    private static float[] add(float[] a, float[] b) {
        float[] c = new float[a.length];
        for (int i = 0; i < a.length; i++) c[i] = a[i] + b[i];
        return c;
    }
}
