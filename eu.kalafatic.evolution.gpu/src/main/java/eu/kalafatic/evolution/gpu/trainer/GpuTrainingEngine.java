package eu.kalafatic.evolution.gpu.trainer;

import eu.kalafatic.evolution.gpu.cuda.CudaDevice;
import eu.kalafatic.evolution.gpu.cuda.CudaDevice.Module;

import java.util.ArrayList;
import java.util.List;

/**
 * Genuine GPU-accelerated transformer trainer.
 *
 * <p>Executes the complete training math — forward pass, cross-entropy loss,
 * backpropagation and AdamW optimisation — as CUDA kernels running on the NVIDIA
 * GPU. The mathematical operations mirror the EVO native {@code EvoLlmModel}
 * (pre-norm Transformer with RMSNorm, RoPE, causal multi-head attention, SwiGLU
 * feed-forward and an un-tied output projection), so trained weights remain fully
 * compatible with the EVO native model format and GGUF export.</p>
 *
 * <p>This is NOT device detection and NOT a simulated loop: every parameter tensor,
 * intermediate activation and gradient lives in device memory, and all arithmetic
 * (matmul, softmax, RMSNorm, RoPE, SwiGLU, cross-entropy, AdamW) is performed by
 * {@code __global__} kernels launched through {@link CudaDevice}. The only host-side
 * work per step is a single loss read-back.</p>
 */
public final class GpuTrainingEngine {

    private static final String CUDA_SOURCE = """
            extern "C" __global__ void gemm(const float* A, const float* B, float* C,
                                            int M, int N, int K, int ta, int tb) {
                int idx = blockIdx.x * blockDim.x + threadIdx.x;
                if (idx >= M * N) return;
                int row = idx / N, col = idx % N;
                float acc = 0.0f;
                for (int k = 0; k < K; k++) {
                    float a = ta ? A[k * M + row] : A[row * K + k];
                    float b = tb ? B[col * K + k] : B[k * N + col];
                    acc += a * b;
                }
                C[idx] = acc;
            }

            extern "C" __global__ void addv(const float* a, const float* b, float* c, int n) {
                int i = blockIdx.x * blockDim.x + threadIdx.x;
                if (i < n) c[i] = a[i] + b[i];
            }

            extern "C" __global__ void add_into(float* out, const float* add, int n) {
                int i = blockIdx.x * blockDim.x + threadIdx.x;
                if (i < n) out[i] += add[i];
            }

            extern "C" __global__ void zero(float* a, int n) {
                int i = blockIdx.x * blockDim.x + threadIdx.x;
                if (i < n) a[i] = 0.0f;
            }

            extern "C" __global__ void embed_fwd(const float* W, const int* tokens, float* x, int T, int D) {
                int i = blockIdx.x * blockDim.x + threadIdx.x;
                if (i >= T * D) return;
                int t = i / D, d = i % D;
                x[i] = W[tokens[t] * D + d];
            }

            extern "C" __global__ void embed_bwd(const int* tokens, const float* dx, float* dW, int T, int D) {
                int i = blockIdx.x * blockDim.x + threadIdx.x;
                if (i >= T * D) return;
                int t = i / D, d = i % D;
                atomicAdd(&dW[tokens[t] * D + d], dx[i]);
            }

            extern "C" __global__ void posenc_fwd(float* x, const float* pe, int n) {
                int i = blockIdx.x * blockDim.x + threadIdx.x;
                if (i < n) x[i] += pe[i];
            }

            extern "C" __global__ void rmsnorm_fwd(const float* x, const float* w, float* y,
                                                   int rows, int dim, float eps) {
                int r = blockIdx.x * blockDim.x + threadIdx.x;
                if (r >= rows) return;
                int off = r * dim;
                float sumSq = 0.0f;
                for (int i = 0; i < dim; i++) { float v = x[off + i]; sumSq += v * v; }
                float rms = sqrtf(sumSq / dim + eps);
                for (int i = 0; i < dim; i++) y[off + i] = (x[off + i] / rms) * w[i];
            }

            extern "C" __global__ void rmsnorm_bwd(const float* x, const float* w, const float* dy,
                                                   float* dx, float* dw, int rows, int dim, float eps) {
                int r = blockIdx.x * blockDim.x + threadIdx.x;
                if (r >= rows) return;
                int off = r * dim;
                float sumSq = 0.0f;
                for (int i = 0; i < dim; i++) { float v = x[off + i]; sumSq += v * v; }
                float rms = sqrtf(sumSq / dim + eps);
                float dot = 0.0f;
                for (int i = 0; i < dim; i++) {
                    float normX = x[off + i] / rms;
                    float dNorm = dy[off + i] * w[i];
                    dot += dNorm * normX;
                    atomicAdd(&dw[i], dy[off + i] * normX);
                }
                for (int i = 0; i < dim; i++) {
                    float normX = x[off + i] / rms;
                    float dNorm = dy[off + i] * w[i];
                    dx[off + i] = (dNorm - normX * dot / dim) / rms;
                }
            }

            extern "C" __global__ void rope_fwd(float* data, const float* cosT, const float* sinT,
                                                int dModel, int numHeads, int seqLen, int startPos) {
                int idx = blockIdx.x * blockDim.x + threadIdx.x;
                if (idx >= seqLen * numHeads) return;
                int r = idx / numHeads, h = idx % numHeads;
                int headDim = dModel / numHeads, pairs = headDim / 2;
                int rowBase = r * dModel + h * headDim;
                int tb = (startPos + r) * pairs;
                for (int i = 0; i < pairs; i++) {
                    float c = cosT[tb + i], s = sinT[tb + i];
                    int p = rowBase + 2 * i;
                    float x0 = data[p], x1 = data[p + 1];
                    data[p] = x0 * c - x1 * s;
                    data[p + 1] = x0 * s + x1 * c;
                }
            }

            extern "C" __global__ void rope_bwd(float* data, const float* cosT, const float* sinT,
                                                int dModel, int numHeads, int seqLen, int startPos) {
                int idx = blockIdx.x * blockDim.x + threadIdx.x;
                if (idx >= seqLen * numHeads) return;
                int r = idx / numHeads, h = idx % numHeads;
                int headDim = dModel / numHeads, pairs = headDim / 2;
                int rowBase = r * dModel + h * headDim;
                int tb = (startPos + r) * pairs;
                for (int i = 0; i < pairs; i++) {
                    float c = cosT[tb + i], s = sinT[tb + i];
                    int p = rowBase + 2 * i;
                    float d0 = data[p], d1 = data[p + 1];
                    data[p] = d0 * c + d1 * s;
                    data[p + 1] = -d0 * s + d1 * c;
                }
            }

            extern "C" __global__ void attn_fwd(const float* Q, const float* K, const float* V, float* out,
                                                int seqLen, int dModel, int numHeads, float scale) {
                int idx = blockIdx.x * blockDim.x + threadIdx.x;
                if (idx >= seqLen * numHeads) return;
                int r = idx / numHeads, h = idx % numHeads;
                int headDim = dModel / numHeads, ho = h * headDim;
                float maxv = -1e30f;
                for (int c = 0; c <= r; c++) {
                    float dot = 0.0f;
                    for (int d = 0; d < headDim; d++) dot += Q[r * dModel + ho + d] * K[c * dModel + ho + d];
                    float s = dot * scale;
                    if (s > maxv) maxv = s;
                }
                float sum = 0.0f;
                for (int c = 0; c <= r; c++) {
                    float dot = 0.0f;
                    for (int d = 0; d < headDim; d++) dot += Q[r * dModel + ho + d] * K[c * dModel + ho + d];
                    sum += expf(dot * scale - maxv);
                }
                for (int d = 0; d < headDim; d++) {
                    float acc = 0.0f;
                    for (int c = 0; c <= r; c++) {
                        float dot = 0.0f;
                        for (int dd = 0; dd < headDim; dd++) dot += Q[r * dModel + ho + dd] * K[c * dModel + ho + dd];
                        acc += expf(dot * scale - maxv) * V[c * dModel + ho + d];
                    }
                    out[r * dModel + ho + d] = acc / sum;
                }
            }

            extern "C" __global__ void attn_bwd_dv(const float* Q, const float* K, const float* dConcat,
                                                   float* dV, int seqLen, int dModel, int numHeads, float scale) {
                int idx = blockIdx.x * blockDim.x + threadIdx.x;
                int headDim = dModel / numHeads;
                int total = numHeads * seqLen * headDim;
                if (idx >= total) return;
                int hd = idx % headDim;
                int tmp = idx / headDim;
                int c = tmp % seqLen, h = tmp / seqLen;
                int ho = h * headDim;
                float acc = 0.0f;
                for (int r = c; r < seqLen; r++) {
                    float maxv = -1e30f, sum = 0.0f;
                    for (int cc = 0; cc <= r; cc++) {
                        float dot = 0.0f;
                        for (int d = 0; d < headDim; d++) dot += Q[r * dModel + ho + d] * K[cc * dModel + ho + d];
                        float s = dot * scale; if (s > maxv) maxv = s;
                    }
                    for (int cc = 0; cc <= r; cc++) {
                        float dot = 0.0f;
                        for (int d = 0; d < headDim; d++) dot += Q[r * dModel + ho + d] * K[cc * dModel + ho + d];
                        sum += expf(dot * scale - maxv);
                    }
                    float dot = 0.0f;
                    for (int d = 0; d < headDim; d++) dot += Q[r * dModel + ho + d] * K[c * dModel + ho + d];
                    float p = expf(dot * scale - maxv) / sum;
                    acc += p * dConcat[r * dModel + ho + hd];
                }
                dV[c * dModel + ho + hd] = acc;
            }

            extern "C" __global__ void attn_bwd_dqdk(const float* Q, const float* K, const float* V,
                                                     const float* dConcat, float* dQ, float* dK,
                                                     int seqLen, int dModel, int numHeads, float scale) {
                int idx = blockIdx.x * blockDim.x + threadIdx.x;
                if (idx >= seqLen * numHeads) return;
                int r = idx / numHeads, h = idx % numHeads;
                int headDim = dModel / numHeads, ho = h * headDim;
                float maxv = -1e30f;
                for (int c = 0; c <= r; c++) {
                    float dot = 0.0f;
                    for (int d = 0; d < headDim; d++) dot += Q[r * dModel + ho + d] * K[c * dModel + ho + d];
                    float s = dot * scale; if (s > maxv) maxv = s;
                }
                float sum = 0.0f;
                for (int c = 0; c <= r; c++) {
                    float dot = 0.0f;
                    for (int d = 0; d < headDim; d++) dot += Q[r * dModel + ho + d] * K[c * dModel + ho + d];
                    sum += expf(dot * scale - maxv);
                }
                float dotSum = 0.0f;
                for (int c = 0; c <= r; c++) {
                    float ds = 0.0f;
                    for (int d = 0; d < headDim; d++) ds += dConcat[r * dModel + ho + d] * V[c * dModel + ho + d];
                    float dot = 0.0f;
                    for (int d = 0; d < headDim; d++) dot += Q[r * dModel + ho + d] * K[c * dModel + ho + d];
                    float p = expf(dot * scale - maxv) / sum;
                    dotSum += ds * p;
                }
                for (int c = 0; c <= r; c++) {
                    float ds = 0.0f;
                    for (int d = 0; d < headDim; d++) ds += dConcat[r * dModel + ho + d] * V[c * dModel + ho + d];
                    float dot = 0.0f;
                    for (int d = 0; d < headDim; d++) dot += Q[r * dModel + ho + d] * K[c * dModel + ho + d];
                    float p = expf(dot * scale - maxv) / sum;
                    float dScore = p * (ds - dotSum) * scale;
                    for (int d = 0; d < headDim; d++) {
                        dQ[r * dModel + ho + d] += dScore * K[c * dModel + ho + d];
                        atomicAdd(&dK[c * dModel + ho + d], dScore * Q[r * dModel + ho + d]);
                    }
                }
            }

            extern "C" __global__ void swiglu_fwd(const float* gate, const float* up, float* h, int n) {
                int i = blockIdx.x * blockDim.x + threadIdx.x;
                if (i >= n) return;
                float z = gate[i];
                float sig = 1.0f / (1.0f + expf(-z));
                h[i] = (z * sig) * up[i];
            }

            extern "C" __global__ void swiglu_bwd(const float* gate, const float* up, const float* dh,
                                                  float* dGate, float* dUp, int n) {
                int i = blockIdx.x * blockDim.x + threadIdx.x;
                if (i >= n) return;
                float z = gate[i];
                float sig = 1.0f / (1.0f + expf(-z));
                float swish = z * sig;
                dUp[i] = dh[i] * swish;
                dGate[i] = dh[i] * up[i] * (sig * (1.0f + z * (1.0f - sig)));
            }

            extern "C" __global__ void softmax_ce(const float* logits, const int* labels, const float* mask,
                                                  float* dLogits, float* loss_out, int seqLen, int vocab, float normFactor) {
                int t = blockIdx.x * blockDim.x + threadIdx.x;
                if (t >= seqLen) return;
                if (mask[t] == 0.0f) return;
                int off = t * vocab;
                float maxLogit = -1e30f;
                for (int v = 0; v < vocab; v++) { float l = logits[off + v]; if (l > maxLogit) maxLogit = l; }
                float sumExp = 0.0f;
                for (int v = 0; v < vocab; v++) sumExp += expf(logits[off + v] - maxLogit);
                float logSumExp = maxLogit + logf(sumExp);
                int target = labels[t];
                atomicAdd(loss_out, (logSumExp - logits[off + target]));
                for (int v = 0; v < vocab; v++) {
                    float prob = expf(logits[off + v] - logSumExp);
                    float targetDist = (v == target) ? 1.0f : 0.0f;
                    dLogits[off + v] = (prob - targetDist) * mask[t] / normFactor;
                }
            }

            extern "C" __global__ void grad_norm_sq(const float* grad, float* out, int n) {
                int i = blockIdx.x * blockDim.x + threadIdx.x;
                if (i >= n) return;
                float g = grad[i];
                atomicAdd(out, g * g);
            }

            extern "C" __global__ void adamw(float* data, const float* grad, float* m, float* v,
                                             const int* decayMask, float lr, float beta1, float beta2, float eps, float wd,
                                             float clipScale, float bias1, float bias2, int n) {
                int i = blockIdx.x * blockDim.x + threadIdx.x;
                if (i >= n) return;
                float g = grad[i] * clipScale;
                float mVal = beta1 * m[i] + (1.0f - beta1) * g;
                float vVal = beta2 * v[i] + (1.0f - beta2) * g * g;
                m[i] = mVal; v[i] = vVal;
                float mHat = mVal / bias1;
                float vHat = vVal / bias2;
                float update = mHat / (sqrtf(vHat) + eps);
                if (decayMask[i]) data[i] -= lr * (update + wd * data[i]);
                else data[i] -= lr * update;
            }
            """;

    // =====================================================================
    // Configuration
    // =====================================================================
    public static final class Config {
        public int vocabSize = 256;
        public int dModel = 64;
        public int numHeads = 4;
        public int numBlocks = 2;
        public int dff = 128;
        public int maxSeqLen = 64;
        public float learningRate = 1e-3f;
        public float weightDecay = 0.01f;
        public float beta1 = 0.9f;
        public float beta2 = 0.95f;
        public float eps = 1e-8f;
        public float maxGradNorm = 1.0f;
        public int epochs = 3;
        public long seed = 42L;

        /**
         * Positional-encoding mode. {@code true} uses rotary embeddings (RoPE) applied
         * to Q/K inside attention; {@code false} uses the additive sinusoidal absolute
         * encoding that the canonical {@code EvoLlmModel} default constructor applies
         * to the embeddings (matching {@code PositionalEncoding}). The canonical Forge
         * training path constructs the model with the 6-argument constructor, i.e.
         * sinusoidal; the adapter must therefore set this to {@code false} to match.
         */
        public boolean useRoPE = true;
    }

    public static final class Sample {
        public final int[] tokens;
        public Sample(int[] tokens) { this.tokens = tokens; }
    }

    public static final class Result {
        public boolean gpuUsed;
        public String deviceName;
        public int computeCapabilityMajor;
        public int computeCapabilityMinor;
        public long totalMemBytes;
        public long freeMemBytesBefore;
        public long freeMemBytesAfter;
        public List<Double> lossHistory = new ArrayList<>();
        public float finalLoss;
        public long durationMs;
        public long totalTokens;
        public double tokensPerSecond;
        public float[] flatWeights;
        public String failureReason;
    }

    private final int V, D, H, L, F, headDim;
    private final int embSize, qkvSize, w1Size, w2Size, normSize, headSize;
    private final int totalParams;

    // Device scratch buffers allocated by a() and freed in bulk via freeScratch().
    // Every forward/backward step allocates ~40 temporary device buffers; without
    // freeing them the device VRAM grows without bound and the run eventually fails.
    private final List<Long> scratchPointers = new ArrayList<>();

    public GpuTrainingEngine(Config cfg) {
        this.V = cfg.vocabSize;
        this.D = cfg.dModel;
        this.H = cfg.numHeads;
        this.L = cfg.numBlocks;
        this.F = cfg.dff;
        this.headDim = D / H;
        this.embSize = V * D;
        this.qkvSize = D * D;
        this.w1Size = D * F;
        this.w2Size = F * D;
        this.normSize = D;
        this.headSize = D * V;
        this.totalParams = embSize + L * (normSize + 4 * qkvSize + normSize + 2 * w1Size + w2Size) + normSize + headSize;
    }

    public int getTotalParams() { return totalParams; }

    /**
     * Opens a fine-grained training session over this engine's kernels. The session
     * holds a CUDA context, device weight/gradient/moment buffers and the positional
     * encoding tables; it exposes {@link Session#accumulate} (forward+backward) and
     * {@link Session#optimizerStep} (AdamW) so a caller can replicate the canonical
     * {@code EvoLlmTrainer} micro-batch / gradient-accumulation semantics exactly.
     */
    public Session openSession(Config cfg) {
        return new Session(cfg);
    }

    public float[] initWeights(Config cfg) {
        float[] w = new float[totalParams];
        java.util.Random r = new java.util.Random(cfg.seed);
        int off = 0;
        gauss(w, off, embSize, r, 1.0f / (float) Math.sqrt(D)); off += embSize;
        for (int b = 0; b < L; b++) {
            ones(w, off, normSize); off += normSize;
            for (int q = 0; q < 4; q++) { gauss(w, off, qkvSize, r, 1.0f / (float) Math.sqrt(D)); off += qkvSize; }
            ones(w, off, normSize); off += normSize;
            gauss(w, off, w1Size, r, 1.0f / (float) Math.sqrt(D)); off += w1Size;
            gauss(w, off, w1Size, r, 1.0f / (float) Math.sqrt(D)); off += w1Size;
            gauss(w, off, w2Size, r, 1.0f / (float) Math.sqrt(F)); off += w2Size;
        }
        ones(w, off, normSize); off += normSize;
        gauss(w, off, headSize, r, (float) Math.sqrt(2.0 / (D + V))); off += headSize;
        return w;
    }

    private static void gauss(float[] w, int off, int n, java.util.Random r, float std) {
        for (int i = 0; i < n; i++) w[off + i] = (float) (r.nextGaussian() * std);
    }
    private static void ones(float[] w, int off, int n) { java.util.Arrays.fill(w, off, off + n, 1.0f); }

    // =====================================================================
    // Training
    // =====================================================================
    public Result train(Config cfg, List<Sample> samples) {
        Result result = new Result();
        CudaDevice dev = CudaDevice.tryCreate();
        if (dev == null) {
            result.gpuUsed = false;
            result.failureReason = "No CUDA device available";
            return result;
        }
        result.gpuUsed = true;
        result.deviceName = dev.getDeviceName();
        result.computeCapabilityMajor = dev.getComputeCapabilityMajor();
        result.computeCapabilityMinor = dev.getComputeCapabilityMinor();
        result.totalMemBytes = dev.getTotalMemBytes();
        result.freeMemBytesBefore = dev.memGetInfo()[0];

        long start = System.currentTimeMillis();
        try {
            Module mod = dev.compile("evo_transformer", CUDA_SOURCE);
            float[] flat = initWeights(cfg);

            int T = 0;
            for (Sample s : samples) T = Math.max(T, s.tokens.length);
            T = Math.min(T, cfg.maxSeqLen);

            long dW = dev.alloc(totalParams * 4L);
            long dGrad = dev.alloc(totalParams * 4L);
            long dM = dev.alloc(totalParams * 4L);
            long dV = dev.alloc(totalParams * 4L);
            dev.memcpyHtoD(dW, flat);
            zero(dev, mod, dGrad, totalParams);
            zero(dev, mod, dM, totalParams);
            zero(dev, mod, dV, totalParams);

            int pairs = headDim / 2;
            float[] cosT = new float[cfg.maxSeqLen * pairs];
            float[] sinT = new float[cfg.maxSeqLen * pairs];
            for (int p = 0; p < cfg.maxSeqLen; p++)
                for (int i = 0; i < pairs; i++) {
                    double theta = p / Math.pow(10000.0, (2.0 * i) / headDim);
                    cosT[p * pairs + i] = (float) Math.cos(theta);
                    sinT[p * pairs + i] = (float) Math.sin(theta);
                }
            long dCos = dev.alloc(cosT.length * 4L);
            long dSin = dev.alloc(sinT.length * 4L);
            dev.memcpyHtoD(dCos, cosT);
            dev.memcpyHtoD(dSin, sinT);

            long totalTokens = 0;
            int step = 0;
            for (int epoch = 0; epoch < cfg.epochs; epoch++) {
                for (Sample sample : samples) {
                    int t = Math.min(sample.tokens.length, T);
                    int[] tokens = sample.tokens;
                    int[] labels = new int[t];
                    float[] mask = new float[t];
                    for (int i = 0; i < t; i++) {
                        labels[i] = (i + 1 < t) ? tokens[i + 1] : tokens[i];
                        mask[i] = (i + 1 < t) ? 1.0f : 0.0f;
                    }
                    int validTokens = Math.max(1, t - 1);
                    totalTokens += validTokens;

                    float loss = step(dev, mod, dW, dGrad, tokens, labels, mask, t, dCos, dSin, validTokens);
                    step++;
                    adamw(dev, mod, dW, dGrad, dM, dV, cfg, step);
                    result.lossHistory.add((double) loss);
                }
            }

            dev.synchronize();
            dev.memcpyDtoH(flat, dW);
            result.flatWeights = flat;
            result.finalLoss = result.lossHistory.isEmpty() ? 0f : (float) (double) result.lossHistory.get(result.lossHistory.size() - 1);
            result.totalTokens = totalTokens;
            result.durationMs = System.currentTimeMillis() - start;
            result.tokensPerSecond = result.durationMs > 0 ? (totalTokens * 1000.0 / result.durationMs) : 0.0;
            result.freeMemBytesAfter = dev.memGetInfo()[0];

            dev.free(dW); dev.free(dGrad); dev.free(dM); dev.free(dV); dev.free(dCos); dev.free(dSin);
        } catch (Throwable t) {
            result.gpuUsed = false;
            result.failureReason = t.toString();
        }
        return result;
    }

    /** One forward + backward pass, accumulating gradients into dGrad. Returns mean per-token loss. */
    private float step(CudaDevice dev, Module mod, long dW, long dGrad, int[] tokens, int[] labels, float[] mask,
                       int T, long dCos, long dSin, int validTokens) {
        try {
        final float scale = 1.0f / (float) Math.sqrt(headDim);
        long dTokens = a(dev, T);
        long dLabels = a(dev, T);
        long dMask = a(dev, T);
        dev.memcpyHtoD(dTokens, tokens);
        dev.memcpyHtoD(dLabels, labels);
        dev.memcpyHtoD(dMask, mask);

        // Per-block forward activations (distinct buffers, no aliasing).
        long[] fX = new long[L + 1];          // fX[0] = embedding, fX[b+1] = block b output
        long[] fN1 = new long[L], fQ = new long[L], fK = new long[L], fV = new long[L], fAttnOut = new long[L];
        long[] fAttnProj = new long[L], fAfterAttn = new long[L], fN2 = new long[L];
        long[] fGate = new long[L], fUp = new long[L], fH = new long[L], fFfnOut = new long[L];
        for (int b = 0; b < L; b++) {
            fN1[b] = a(dev, T * D); fQ[b] = a(dev, T * D); fK[b] = a(dev, T * D); fV[b] = a(dev, T * D);
            fAttnOut[b] = a(dev, T * D); fAttnProj[b] = a(dev, T * D); fAfterAttn[b] = a(dev, T * D);
            fN2[b] = a(dev, T * D);
            fGate[b] = a(dev, T * F); fUp[b] = a(dev, T * F); fH[b] = a(dev, T * F); fFfnOut[b] = a(dev, T * D);
            fX[b + 1] = a(dev, T * D);
        }
        fX[0] = a(dev, T * D);
        long dFinal = a(dev, T * D), dLogits = a(dev, T * V), dDLogits = a(dev, T * V);
        long dLoss = a(dev, 1);

        // ---- Forward ----
        mod.function("embed_fwd").launch(T * D, List.of(dW, dTokens, fX[0], T, D));
        for (int b = 0; b < L; b++) {
            rmsFwd(dev, mod, fX[b], dW + offAttnNorm(b) * 4L, fN1[b], T, D);
            gemm(dev, mod, fN1[b], dW + offWQ(b) * 4L, fQ[b], T, D, D, 0, 0);
            gemm(dev, mod, fN1[b], dW + offWK(b) * 4L, fK[b], T, D, D, 0, 0);
            gemm(dev, mod, fN1[b], dW + offWV(b) * 4L, fV[b], T, D, D, 0, 0);
            mod.function("rope_fwd").launch(T * H, List.of(fQ[b], dCos, dSin, D, H, T, 0));
            mod.function("rope_fwd").launch(T * H, List.of(fK[b], dCos, dSin, D, H, T, 0));
            mod.function("attn_fwd").launch(T * H, List.of(fQ[b], fK[b], fV[b], fAttnOut[b], T, D, H, scale));
            gemm(dev, mod, fAttnOut[b], dW + offWO(b) * 4L, fAttnProj[b], T, D, D, 0, 0);
            mod.function("addv").launch(T * D, List.of(fX[b], fAttnProj[b], fAfterAttn[b], T * D));
            rmsFwd(dev, mod, fAfterAttn[b], dW + offFfnNorm(b) * 4L, fN2[b], T, D);
            gemm(dev, mod, fN2[b], dW + offW1(b) * 4L, fGate[b], T, F, D, 0, 0);
            gemm(dev, mod, fN2[b], dW + offW3(b) * 4L, fUp[b], T, F, D, 0, 0);
            mod.function("swiglu_fwd").launch(T * F, List.of(fGate[b], fUp[b], fH[b], T * F));
            gemm(dev, mod, fH[b], dW + offW2(b) * 4L, fFfnOut[b], T, D, F, 0, 0);
            mod.function("addv").launch(T * D, List.of(fAfterAttn[b], fFfnOut[b], fX[b + 1], T * D));
        }
        rmsFwd(dev, mod, fX[L], dW + offOutNorm() * 4L, dFinal, T, D);
        gemm(dev, mod, dFinal, dW + offLmHead() * 4L, dLogits, T, V, D, 0, 0);

        // ---- Loss + dLogits ----
        float[] zeroOne = {0.0f};
        dev.memcpyHtoD(dLoss, zeroOne);
        mod.function("softmax_ce").launch(T, List.of(dLogits, dLabels, dMask, dDLogits, dLoss, T, V, (float) validTokens));

        // ---- Backward ----
        gemm(dev, mod, dFinal, dDLogits, dGrad + offLmHead() * 4L, D, V, T, 1, 0);  // dLmHead
        long dDFinal = a(dev, T * D);
        gemm(dev, mod, dDLogits, dW + offLmHead() * 4L, dDFinal, T, D, V, 0, 1);   // dFinal
        long dDX = a(dev, T * D);
        rmsBwd(dev, mod, dFinal, dW + offOutNorm() * 4L, dGrad + offOutNorm() * 4L, dDFinal, dDX, T, D);

        long dOut = dDX;
        for (int b = L - 1; b >= 0; b--) {
            // FFN backward
            long dDh = a(dev, T * F);
            gemm(dev, mod, dOut, dW + offW2(b) * 4L, dDh, T, F, D, 0, 1);            // dh = dOut @ W2^T
            gemm(dev, mod, fH[b], dOut, dGrad + offW2(b) * 4L, F, D, T, 1, 0);       // dW2 = H^T @ dOut
            long dDGate = a(dev, T * F), dDUp = a(dev, T * F);
            mod.function("swiglu_bwd").launch(T * F, List.of(fGate[b], fUp[b], dDh, dDGate, dDUp, T * F));
            long dN2b = a(dev, T * D);
            rmsFwd(dev, mod, fAfterAttn[b], dW + offFfnNorm(b) * 4L, dN2b, T, D);
            gemm(dev, mod, dN2b, dDGate, dGrad + offW1(b) * 4L, D, F, T, 1, 0);     // dW1
            gemm(dev, mod, dN2b, dDUp, dGrad + offW3(b) * 4L, D, F, T, 1, 0);       // dW3
            long dN2grad = a(dev, T * D);
            gemm(dev, mod, dDGate, dW + offW1(b) * 4L, dN2grad, T, D, F, 0, 1);
            long dN2grad2 = a(dev, T * D);
            gemm(dev, mod, dDUp, dW + offW3(b) * 4L, dN2grad2, T, D, F, 0, 1);
            mod.function("addv").launch(T * D, List.of(dN2grad, dN2grad2, dN2grad, T * D));
            long dAfterAttnB = a(dev, T * D);
            rmsBwd(dev, mod, fAfterAttn[b], dW + offFfnNorm(b) * 4L, dGrad + offFfnNorm(b) * 4L, dN2grad, dAfterAttnB, T, D);
            mod.function("add_into").launch(T * D, List.of(dAfterAttnB, dOut, T * D));

            // Attention backward
            gemm(dev, mod, fAttnOut[b], dAfterAttnB, dGrad + offWO(b) * 4L, D, D, T, 1, 0);  // dWO
            long dConcat = a(dev, T * D);
            gemm(dev, mod, dAfterAttnB, dW + offWO(b) * 4L, dConcat, T, D, D, 0, 1);          // dConcat
            long dDQ = a(dev, T * D), dDK = a(dev, T * D), dDV = a(dev, T * D);
            zero(dev, mod, dDQ, T * D); zero(dev, mod, dDK, T * D); zero(dev, mod, dDV, T * D);
            mod.function("attn_bwd_dv").launch(H * T * headDim, List.of(fQ[b], fK[b], dConcat, dDV, T, D, H, scale));
            mod.function("attn_bwd_dqdk").launch(T * H, List.of(fQ[b], fK[b], fV[b], dConcat, dDQ, dDK, T, D, H, scale));
            mod.function("rope_bwd").launch(T * H, List.of(dDQ, dCos, dSin, D, H, T, 0));
            mod.function("rope_bwd").launch(T * H, List.of(dDK, dCos, dSin, D, H, T, 0));
            long dN1b = a(dev, T * D);
            rmsFwd(dev, mod, fX[b], dW + offAttnNorm(b) * 4L, dN1b, T, D);
            gemm(dev, mod, dN1b, dDQ, dGrad + offWQ(b) * 4L, D, D, T, 1, 0);   // dWQ
            gemm(dev, mod, dN1b, dDK, dGrad + offWK(b) * 4L, D, D, T, 1, 0);   // dWK
            gemm(dev, mod, dN1b, dDV, dGrad + offWV(b) * 4L, D, D, T, 1, 0);   // dWV
            long dN1grad = a(dev, T * D);
            gemm(dev, mod, dDQ, dW + offWQ(b) * 4L, dN1grad, T, D, D, 0, 1);
            long tmp = a(dev, T * D);
            gemm(dev, mod, dDK, dW + offWK(b) * 4L, tmp, T, D, D, 0, 1);
            mod.function("add_into").launch(T * D, List.of(dN1grad, tmp, T * D));
            gemm(dev, mod, dDV, dW + offWV(b) * 4L, tmp, T, D, D, 0, 1);
            mod.function("add_into").launch(T * D, List.of(dN1grad, tmp, T * D));
            long dXAttn = a(dev, T * D);
            rmsBwd(dev, mod, fX[b], dW + offAttnNorm(b) * 4L, dGrad + offAttnNorm(b) * 4L, dN1grad, dXAttn, T, D);
            long dBlockIn = a(dev, T * D);
            mod.function("addv").launch(T * D, List.of(dAfterAttnB, dXAttn, dBlockIn, T * D));
            dOut = dBlockIn;
        }
        mod.function("embed_bwd").launch(T * D, List.of(dTokens, dOut, dGrad, T, D));

        dev.synchronize();
        float[] lossBuf = new float[1];
        dev.memcpyDtoH(lossBuf, dLoss);
        float loss = lossBuf[0] / validTokens;
        return loss;
        } finally {
            freeScratch(dev);
        }
    }

    /**
     * Forward-only pass: returns logits [T, V] for the given token sequence using the
     * supplied (already-initialised) flat weights. Used by tests to verify the GPU
     * forward kernels against an independent CPU reference.
     */
    public float[] forwardLogits(Config cfg, float[] flatWeights, int[] tokens) {
        CudaDevice dev = CudaDevice.tryCreate();
        if (dev == null) throw new IllegalStateException("No CUDA device");
        final float scale = 1.0f / (float) Math.sqrt(headDim);
        int T = tokens.length;
        try {
            Module mod = dev.compile("evo_transformer", CUDA_SOURCE);
            long dW = dev.alloc(flatWeights.length * 4L);
            dev.memcpyHtoD(dW, flatWeights);
            int pairs = headDim / 2;
            float[] cosT = new float[cfg.maxSeqLen * pairs], sinT = new float[cfg.maxSeqLen * pairs];
            for (int p = 0; p < cfg.maxSeqLen; p++) for (int i = 0; i < pairs; i++) {
                double theta = p / Math.pow(10000.0, (2.0 * i) / headDim);
                cosT[p * pairs + i] = (float) Math.cos(theta); sinT[p * pairs + i] = (float) Math.sin(theta);
            }
            long dCos = dev.alloc(cosT.length * 4L), dSin = dev.alloc(sinT.length * 4L);
            dev.memcpyHtoD(dCos, cosT); dev.memcpyHtoD(dSin, sinT);

            long dTokens = a(dev, T);
            dev.memcpyHtoD(dTokens, tokens);
            long[] fX = new long[L + 1];
            long dFinal = a(dev, T * D), dLogits = a(dev, T * V);
            for (int b = 0; b <= L; b++) fX[b] = a(dev, T * D);
            long dN1 = a(dev, T * D), dQ = a(dev, T * D), dK = a(dev, T * D), dVb = a(dev, T * D);
            long dAttnOut = a(dev, T * D), dAttnProj = a(dev, T * D), dAfterAttn = a(dev, T * D);
            long dN2 = a(dev, T * D), dGate = a(dev, T * F), dUp = a(dev, T * F), dH = a(dev, T * F), dFfnOut = a(dev, T * D);

            mod.function("embed_fwd").launch(T * D, List.of(dW, dTokens, fX[0], T, D));
            for (int b = 0; b < L; b++) {
                rmsFwd(dev, mod, fX[b], dW + offAttnNorm(b) * 4L, dN1, T, D);
                gemm(dev, mod, dN1, dW + offWQ(b) * 4L, dQ, T, D, D, 0, 0);
                gemm(dev, mod, dN1, dW + offWK(b) * 4L, dK, T, D, D, 0, 0);
                gemm(dev, mod, dN1, dW + offWV(b) * 4L, dVb, T, D, D, 0, 0);
                mod.function("rope_fwd").launch(T * H, List.of(dQ, dCos, dSin, D, H, T, 0));
                mod.function("rope_fwd").launch(T * H, List.of(dK, dCos, dSin, D, H, T, 0));
                mod.function("attn_fwd").launch(T * H, List.of(dQ, dK, dVb, dAttnOut, T, D, H, scale));
                gemm(dev, mod, dAttnOut, dW + offWO(b) * 4L, dAttnProj, T, D, D, 0, 0);
                mod.function("addv").launch(T * D, List.of(fX[b], dAttnProj, dAfterAttn, T * D));
                rmsFwd(dev, mod, dAfterAttn, dW + offFfnNorm(b) * 4L, dN2, T, D);
                gemm(dev, mod, dN2, dW + offW1(b) * 4L, dGate, T, F, D, 0, 0);
                gemm(dev, mod, dN2, dW + offW3(b) * 4L, dUp, T, F, D, 0, 0);
                mod.function("swiglu_fwd").launch(T * F, List.of(dGate, dUp, dH, T * F));
                gemm(dev, mod, dH, dW + offW2(b) * 4L, dFfnOut, T, D, F, 0, 0);
                mod.function("addv").launch(T * D, List.of(dAfterAttn, dFfnOut, fX[b + 1], T * D));
            }
            rmsFwd(dev, mod, fX[L], dW + offOutNorm() * 4L, dFinal, T, D);
            gemm(dev, mod, dFinal, dW + offLmHead() * 4L, dLogits, T, V, D, 0, 0);
            dev.synchronize();
            float[] logits = new float[T * V];
            dev.memcpyDtoH(logits, dLogits);
            freeScratch(dev);
            return logits;
        } finally {
            // persistent weights/cos/sin are freed when the device arena closes (short-lived test usage)
        }
    }

    private void adamw(CudaDevice dev, Module mod, long dW, long dGrad, long dM, long dV, Config cfg, int step) {
        long dNorm = a(dev, 1);
        float[] z = {0.0f};
        dev.memcpyHtoD(dNorm, z);
        mod.function("grad_norm_sq").launch(totalParams, List.of(dGrad, dNorm, totalParams));
        dev.synchronize();
        float[] nb = new float[1];
        dev.memcpyDtoH(nb, dNorm);
        float globalNorm = (float) Math.sqrt(nb[0]);
        float clipScale = (cfg.maxGradNorm > 0 && globalNorm > cfg.maxGradNorm) ? (cfg.maxGradNorm / globalNorm) : 1.0f;
        float bias1 = 1.0f - (float) Math.pow(cfg.beta1, step);
        float bias2 = 1.0f - (float) Math.pow(cfg.beta2, step);
        // Legacy path: apply weight decay uniformly (all-ones mask) to preserve the
        // historical standalone-trainer behaviour.
        long dDecayMask = dev.alloc(totalParams * 4L);
        int[] decayMask = new int[totalParams];
        java.util.Arrays.fill(decayMask, 1);
        dev.memcpyHtoD(dDecayMask, decayMask);
        mod.function("adamw").launch(totalParams, List.of(dW, dGrad, dM, dV, dDecayMask,
                cfg.learningRate, cfg.beta1, cfg.beta2, cfg.eps, cfg.weightDecay, clipScale, bias1, bias2, totalParams));
        dev.free(dDecayMask);
        zero(dev, mod, dGrad, totalParams);
        freeScratch(dev);
    }

    // ---- helpers ----
    private long a(CudaDevice dev, int elems) {
        long p = dev.alloc(elems * 4L);
        scratchPointers.add(p);
        return p;
    }

    private void freeScratch(CudaDevice dev) {
        for (long p : scratchPointers) dev.free(p);
        scratchPointers.clear();
    }
    private void zero(CudaDevice dev, Module mod, long buf, int n) { mod.function("zero").launch(n, List.of(buf, n)); }
    private void gemm(CudaDevice dev, Module mod, long A, long B, long C, int M, int N, int K, int ta, int tb) {
        mod.function("gemm").launch(M * N, List.of(A, B, C, M, N, K, ta, tb));
    }
    private void rmsFwd(CudaDevice dev, Module mod, long x, long w, long y, int rows, int dim) {
        mod.function("rmsnorm_fwd").launch(rows, List.of(x, w, y, rows, dim, 1e-5f));
    }
    private void rmsBwd(CudaDevice dev, Module mod, long x, long w, long dw, long dy, long dx, int rows, int dim) {
        mod.function("rmsnorm_bwd").launch(rows, List.of(x, w, dy, dx, dw, rows, dim, 1e-5f));
    }

    // Weight offsets (in floats).
    private int offAttnNorm(int b) { return embSize + b * (normSize + 4 * qkvSize + normSize + 2 * w1Size + w2Size); }
    private int offWQ(int b) { return offAttnNorm(b) + normSize; }
    private int offWK(int b) { return offWQ(b) + qkvSize; }
    private int offWV(int b) { return offWK(b) + qkvSize; }
    private int offWO(int b) { return offWV(b) + qkvSize; }
    private int offFfnNorm(int b) { return offWO(b) + qkvSize; }
    private int offW1(int b) { return offFfnNorm(b) + normSize; }
    private int offW3(int b) { return offW1(b) + w1Size; }
    private int offW2(int b) { return offW3(b) + w1Size; }
    private int offOutNorm() { return embSize + L * (normSize + 4 * qkvSize + normSize + 2 * w1Size + w2Size); }
    private int offLmHead() { return offOutNorm() + normSize; }

    // =====================================================================
    // Sinusoidal positional encoding (matches PositionalEncoding.calculateEncodings)
    // =====================================================================
    static float[] buildPositionalEncoding(int maxSeqLen, int dModel) {
        float[] pe = new float[maxSeqLen * dModel];
        for (int pos = 0; pos < maxSeqLen; pos++) {
            for (int i = 0; i < dModel; i += 2) {
                double divTerm = Math.pow(10000.0, (double) i / dModel);
                pe[pos * dModel + i] = (float) Math.sin(pos / divTerm);
                if (i + 1 < dModel) pe[pos * dModel + i + 1] = (float) Math.cos(pos / divTerm);
            }
        }
        return pe;
    }

    /**
     * Per-parameter weight-decay mask matching {@code EvoLlmTrainer.ModelParameterGroups}:
     * rank-1 parameters (the RMSNorm weights) are excluded from weight decay; all other
     * parameters (embedding, Q/K/V/O, gate/up/down, output head) are decayed.
     */
    int[] buildDecayMask() {
        int[] mask = new int[totalParams];
        int off = 0;
        java.util.Arrays.fill(mask, off, off + embSize, 1); off += embSize;
        for (int b = 0; b < L; b++) {
            off += normSize;                                   // attn_norm (no decay)
            java.util.Arrays.fill(mask, off, off + 4 * qkvSize, 1); off += 4 * qkvSize; // WQ,WK,WV,WO
            off += normSize;                                   // ffn_norm (no decay)
            java.util.Arrays.fill(mask, off, off + 2 * w1Size + w2Size, 1); off += 2 * w1Size + w2Size; // W1,W3,W2
        }
        off += normSize;                                       // output_norm (no decay)
        java.util.Arrays.fill(mask, off, off + headSize, 1); off += headSize; // lm_head
        return mask;
    }

    // =====================================================================
    // Session — fine-grained GPU training backend.
    // Exposes forward+backward (accumulate) and AdamW (optimizerStep) at the
    // single-sequence granularity so an adapter can replicate EvoLlmTrainer's
    // micro-batch / gradient-accumulation / MEAN_PER_TOKEN semantics exactly.
    // Handles both RoPE and additive sinusoidal positional encoding.
    // =====================================================================
    public final class Session implements AutoCloseable {
        private final CudaDevice dev;
        private final Module mod;
        private final Config cfg;
        private final long dW, dGrad, dM, dV;
        private final long dCos, dSin, dPosEnc, dDecayMask;
        private final boolean useRoPE;
        private int stepCount = 0;
        private boolean closed = false;
        private float currentLr;

        public Session(Config cfg) {
            this.cfg = cfg;
            this.useRoPE = cfg.useRoPE;
            this.currentLr = cfg.learningRate;
            this.dev = CudaDevice.tryCreate();
            if (dev == null) throw new IllegalStateException("No CUDA device available");
            this.mod = dev.compile("evo_transformer", CUDA_SOURCE);
            this.dW = dev.alloc(totalParams * 4L);
            this.dGrad = dev.alloc(totalParams * 4L);
            this.dM = dev.alloc(totalParams * 4L);
            this.dV = dev.alloc(totalParams * 4L);
            zero(dev, mod, dGrad, totalParams);
            zero(dev, mod, dM, totalParams);
            zero(dev, mod, dV, totalParams);

            int[] decayMask = buildDecayMask();
            this.dDecayMask = dev.alloc(decayMask.length * 4L);
            dev.memcpyHtoD(dDecayMask, decayMask);

            if (useRoPE) {
                int pairs = headDim / 2;
                float[] cosT = new float[cfg.maxSeqLen * pairs];
                float[] sinT = new float[cfg.maxSeqLen * pairs];
                for (int p = 0; p < cfg.maxSeqLen; p++)
                    for (int i = 0; i < pairs; i++) {
                        double theta = p / Math.pow(10000.0, (2.0 * i) / headDim);
                        cosT[p * pairs + i] = (float) Math.cos(theta);
                        sinT[p * pairs + i] = (float) Math.sin(theta);
                    }
                this.dCos = dev.alloc(cosT.length * 4L);
                this.dSin = dev.alloc(sinT.length * 4L);
                dev.memcpyHtoD(dCos, cosT);
                dev.memcpyHtoD(dSin, sinT);
                this.dPosEnc = 0;
            } else {
                this.dCos = 0;
                this.dSin = 0;
                float[] pe = buildPositionalEncoding(cfg.maxSeqLen, D);
                this.dPosEnc = dev.alloc(pe.length * 4L);
                dev.memcpyHtoD(dPosEnc, pe);
            }
        }

        public String deviceName() { return dev.getDeviceName(); }
        public long totalMemBytes() { return dev.getTotalMemBytes(); }
        public long freeMemBytes() { return dev.memGetInfo()[0]; }
        public int getStepCount() { return stepCount; }
        public boolean usesRoPE() { return useRoPE; }

        /** Override the learning rate used by subsequent {@link #optimizerStep()} calls. */
        public void setLearningRate(float lr) { this.currentLr = lr; }

        /** Copy host flat weights (in EVO layout) into device memory. */
        public void loadWeights(float[] flat) {
            if (flat.length != totalParams)
                throw new IllegalArgumentException("Expected " + totalParams + " floats, got " + flat.length);
            dev.memcpyHtoD(dW, flat);
        }

        /** Copy device weights back to a host flat buffer (in EVO layout). */
        public void downloadWeights(float[] out) {
            if (out.length != totalParams)
                throw new IllegalArgumentException("Expected " + totalParams + " floats, got " + out.length);
            dev.synchronize();
            dev.memcpyDtoH(out, dW);
        }

        /** Forward + loss + backward for one sequence; accumulates grads into dGrad. Returns raw loss sum. */
        public float accumulate(int[] tokens, int[] labels, float[] mask, int t, float normFactor) {
            List<Long> sc = new ArrayList<>();
            java.util.function.IntFunction<Long> s = n -> { long p = dev.alloc(n * 4L); sc.add(p); return p; };

            try {
            final float scale = 1.0f / (float) Math.sqrt(headDim);
            long dTokens = s.apply(t), dLabels = s.apply(t), dMask = s.apply(t);
            dev.memcpyHtoD(dTokens, tokens);
            dev.memcpyHtoD(dLabels, labels);
            dev.memcpyHtoD(dMask, mask);

            long[] fX = new long[L + 1];
            long[] fN1 = new long[L], fQ = new long[L], fK = new long[L], fV = new long[L], fAttnOut = new long[L];
            long[] fAttnProj = new long[L], fAfterAttn = new long[L], fN2 = new long[L];
            long[] fGate = new long[L], fUp = new long[L], fH = new long[L], fFfnOut = new long[L];
            for (int b = 0; b < L; b++) {
                fN1[b] = s.apply(t * D); fQ[b] = s.apply(t * D); fK[b] = s.apply(t * D); fV[b] = s.apply(t * D);
                fAttnOut[b] = s.apply(t * D); fAttnProj[b] = s.apply(t * D); fAfterAttn[b] = s.apply(t * D);
                fN2[b] = s.apply(t * D);
                fGate[b] = s.apply(t * F); fUp[b] = s.apply(t * F); fH[b] = s.apply(t * F); fFfnOut[b] = s.apply(t * D);
                fX[b + 1] = s.apply(t * D);
            }
            fX[0] = s.apply(t * D);
            long dFinal = s.apply(t * D), dLogits = s.apply(t * V), dDLogits = s.apply(t * V);
            long dLoss = s.apply(1);

            // ---- Forward ----
            mod.function("embed_fwd").launch(t * D, List.of(dW, dTokens, fX[0], t, D));
            if (!useRoPE) mod.function("posenc_fwd").launch(t * D, List.of(fX[0], dPosEnc, t * D));
            for (int b = 0; b < L; b++) {
                rmsFwd(dev, mod, fX[b], dW + offAttnNorm(b) * 4L, fN1[b], t, D);
                gemm(dev, mod, fN1[b], dW + offWQ(b) * 4L, fQ[b], t, D, D, 0, 0);
                gemm(dev, mod, fN1[b], dW + offWK(b) * 4L, fK[b], t, D, D, 0, 0);
                gemm(dev, mod, fN1[b], dW + offWV(b) * 4L, fV[b], t, D, D, 0, 0);
                if (useRoPE) {
                    mod.function("rope_fwd").launch(t * H, List.of(fQ[b], dCos, dSin, D, H, t, 0));
                    mod.function("rope_fwd").launch(t * H, List.of(fK[b], dCos, dSin, D, H, t, 0));
                }
                mod.function("attn_fwd").launch(t * H, List.of(fQ[b], fK[b], fV[b], fAttnOut[b], t, D, H, scale));
                gemm(dev, mod, fAttnOut[b], dW + offWO(b) * 4L, fAttnProj[b], t, D, D, 0, 0);
                mod.function("addv").launch(t * D, List.of(fX[b], fAttnProj[b], fAfterAttn[b], t * D));
                rmsFwd(dev, mod, fAfterAttn[b], dW + offFfnNorm(b) * 4L, fN2[b], t, D);
                gemm(dev, mod, fN2[b], dW + offW1(b) * 4L, fGate[b], t, F, D, 0, 0);
                gemm(dev, mod, fN2[b], dW + offW3(b) * 4L, fUp[b], t, F, D, 0, 0);
                mod.function("swiglu_fwd").launch(t * F, List.of(fGate[b], fUp[b], fH[b], t * F));
                gemm(dev, mod, fH[b], dW + offW2(b) * 4L, fFfnOut[b], t, D, F, 0, 0);
                mod.function("addv").launch(t * D, List.of(fAfterAttn[b], fFfnOut[b], fX[b + 1], t * D));
            }
            rmsFwd(dev, mod, fX[L], dW + offOutNorm() * 4L, dFinal, t, D);
            gemm(dev, mod, dFinal, dW + offLmHead() * 4L, dLogits, t, V, D, 0, 0);

            // ---- Loss + dLogits ----
            float[] zeroOne = {0.0f};
            dev.memcpyHtoD(dLoss, zeroOne);
            mod.function("softmax_ce").launch(t, List.of(dLogits, dLabels, dMask, dDLogits, dLoss, t, V, normFactor));

            // ---- Backward ----
            gemm(dev, mod, dFinal, dDLogits, dGrad + offLmHead() * 4L, D, V, t, 1, 0);   // dLmHead
            long dDFinal = s.apply(t * D);
            gemm(dev, mod, dDLogits, dW + offLmHead() * 4L, dDFinal, t, D, V, 0, 1);      // dFinal
            long dDX = s.apply(t * D);
            rmsBwd(dev, mod, dFinal, dW + offOutNorm() * 4L, dGrad + offOutNorm() * 4L, dDFinal, dDX, t, D);

            long dOut = dDX;
            for (int b = L - 1; b >= 0; b--) {
                long dDh = s.apply(t * F);
                gemm(dev, mod, dOut, dW + offW2(b) * 4L, dDh, t, F, D, 0, 1);             // dh = dOut @ W2^T
                gemm(dev, mod, fH[b], dOut, dGrad + offW2(b) * 4L, F, D, t, 1, 0);        // dW2
                long dDGate = s.apply(t * F), dDUp = s.apply(t * F);
                mod.function("swiglu_bwd").launch(t * F, List.of(fGate[b], fUp[b], dDh, dDGate, dDUp, t * F));
                long dN2b = s.apply(t * D);
                rmsFwd(dev, mod, fAfterAttn[b], dW + offFfnNorm(b) * 4L, dN2b, t, D);
                gemm(dev, mod, dN2b, dDGate, dGrad + offW1(b) * 4L, D, F, t, 1, 0);        // dW1
                gemm(dev, mod, dN2b, dDUp, dGrad + offW3(b) * 4L, D, F, t, 1, 0);         // dW3
                long dN2grad = s.apply(t * D);
                gemm(dev, mod, dDGate, dW + offW1(b) * 4L, dN2grad, t, D, F, 0, 1);
                long dN2grad2 = s.apply(t * D);
                gemm(dev, mod, dDUp, dW + offW3(b) * 4L, dN2grad2, t, D, F, 0, 1);
                mod.function("addv").launch(t * D, List.of(dN2grad, dN2grad2, dN2grad, t * D));
                long dAfterAttnB = s.apply(t * D);
                rmsBwd(dev, mod, fAfterAttn[b], dW + offFfnNorm(b) * 4L, dGrad + offFfnNorm(b) * 4L, dN2grad, dAfterAttnB, t, D);
                mod.function("add_into").launch(t * D, List.of(dAfterAttnB, dOut, t * D));

                gemm(dev, mod, fAttnOut[b], dAfterAttnB, dGrad + offWO(b) * 4L, D, D, t, 1, 0);  // dWO
                long dConcat = s.apply(t * D);
                gemm(dev, mod, dAfterAttnB, dW + offWO(b) * 4L, dConcat, t, D, D, 0, 1);          // dConcat
                long dDQ = s.apply(t * D), dDK = s.apply(t * D), dDV = s.apply(t * D);
                zero(dev, mod, dDQ, t * D); zero(dev, mod, dDK, t * D); zero(dev, mod, dDV, t * D);
                mod.function("attn_bwd_dv").launch(H * t * headDim, List.of(fQ[b], fK[b], dConcat, dDV, t, D, H, scale));
                mod.function("attn_bwd_dqdk").launch(t * H, List.of(fQ[b], fK[b], fV[b], dConcat, dDQ, dDK, t, D, H, scale));
                if (useRoPE) {
                    mod.function("rope_bwd").launch(t * H, List.of(dDQ, dCos, dSin, D, H, t, 0));
                    mod.function("rope_bwd").launch(t * H, List.of(dDK, dCos, dSin, D, H, t, 0));
                }
                long dN1b = s.apply(t * D);
                rmsFwd(dev, mod, fX[b], dW + offAttnNorm(b) * 4L, dN1b, t, D);
                gemm(dev, mod, dN1b, dDQ, dGrad + offWQ(b) * 4L, D, D, t, 1, 0);   // dWQ
                gemm(dev, mod, dN1b, dDK, dGrad + offWK(b) * 4L, D, D, t, 1, 0);   // dWK
                gemm(dev, mod, dN1b, dDV, dGrad + offWV(b) * 4L, D, D, t, 1, 0);   // dWV
                long dN1grad = s.apply(t * D);
                gemm(dev, mod, dDQ, dW + offWQ(b) * 4L, dN1grad, t, D, D, 0, 1);
                long tmp = s.apply(t * D);
                gemm(dev, mod, dDK, dW + offWK(b) * 4L, tmp, t, D, D, 0, 1);
                mod.function("add_into").launch(t * D, List.of(dN1grad, tmp, t * D));
                gemm(dev, mod, dDV, dW + offWV(b) * 4L, tmp, t, D, D, 0, 1);
                mod.function("add_into").launch(t * D, List.of(dN1grad, tmp, t * D));
                long dXAttn = s.apply(t * D);
                rmsBwd(dev, mod, fX[b], dW + offAttnNorm(b) * 4L, dGrad + offAttnNorm(b) * 4L, dN1grad, dXAttn, t, D);
                long dBlockIn = s.apply(t * D);
                mod.function("addv").launch(t * D, List.of(dAfterAttnB, dXAttn, dBlockIn, t * D));
                dOut = dBlockIn;
            }
            mod.function("embed_bwd").launch(t * D, List.of(dTokens, dOut, dGrad, t, D));

            dev.synchronize();
            float[] lossBuf = new float[1];
            dev.memcpyDtoH(lossBuf, dLoss);
            return lossBuf[0];
            } finally {
                // Exception-safe cleanup: every scratch buffer allocated for this step must
                // be released even when forward/backward fails part-way (e.g. cuMemAlloc
                // OOM) so a failed step never leaks device memory.
                for (long p : sc) dev.free(p);
            }
        }

        /** AdamW step: grad-norm clip + update + zero grads (respecting the decay mask). */
        public void optimizerStep() {
            stepCount++;
            long dNorm = dev.alloc(4L);
            float[] z = {0.0f};
            dev.memcpyHtoD(dNorm, z);
            mod.function("grad_norm_sq").launch(totalParams, List.of(dGrad, dNorm, totalParams));
            dev.synchronize();
            float[] nb = new float[1];
            dev.memcpyDtoH(nb, dNorm);
            float globalNorm = (float) Math.sqrt(nb[0]);
            float clipScale = (cfg.maxGradNorm > 0 && globalNorm > cfg.maxGradNorm) ? (cfg.maxGradNorm / globalNorm) : 1.0f;
            float bias1 = 1.0f - (float) Math.pow(cfg.beta1, stepCount);
            float bias2 = 1.0f - (float) Math.pow(cfg.beta2, stepCount);
            mod.function("adamw").launch(totalParams, List.of(dW, dGrad, dM, dV, dDecayMask,
                    currentLr, cfg.beta1, cfg.beta2, cfg.eps, cfg.weightDecay, clipScale, bias1, bias2, totalParams));
            zero(dev, mod, dGrad, totalParams);
            dev.free(dNorm);
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            dev.free(dW);
            dev.free(dGrad);
            dev.free(dM);
            dev.free(dV);
            dev.free(dDecayMask);
            if (dCos != 0) dev.free(dCos);
            if (dSin != 0) dev.free(dSin);
            if (dPosEnc != 0) dev.free(dPosEnc);
        }
    }
}
