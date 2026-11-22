package eu.kalafatic.evolution.forge.model.llm;

/**
 * Rotary Positional Embedding (RoPE) for the EVO native model.
 *
 * <p>Applies the standard Llama-style rotary transform to query and key
 * tensors inside multi-head attention. The rotation table is precomputed
 * for {@code maxSeqLen} absolute positions using the frequency schedule
 * {@code theta_i = base^(-2i/headDim)} with {@code base = 10000}, which is
 * exactly the schedule llama.cpp uses for {@code general.architecture=llama}
 * with {@code llama.rope.freq_base=10000} and
 * {@code llama.rope.dimension_count=headDim}. This makes the GGUF export of
 * a rope-trained model mathematically faithful when loaded by Ollama.</p>
 *
 * <p>Rotation layout: for every absolute position p, every head h and every
 * pair (2i, 2i+1) within the head, the pair is rotated by the angle
 * p * theta_i:</p>
 *
 * <pre>
 *   x'[2i]   = x[2i] * cos - x[2i+1] * sin
 *   x'[2i+1] = x[2i] * sin + x[2i+1] * cos
 * </pre>
 *
 * <p>The inverse (backward) transform is the transpose rotation:
 *
 * <pre>
 *   d[2i]   = d'[2i] * cos + d'[2i+1] * sin
 *   d[2i+1] = -d'[2i] * sin + d'[2i+1] * cos
 * </pre>
 */
public class RoPEPositionalEncoding {

    private final int maxSeqLen;
    private final int headDim;
    private final float base;
    private final float[] cosTable;
    private final float[] sinTable;

    public RoPEPositionalEncoding(int maxSeqLen, int headDim) {
        this(maxSeqLen, headDim, 10000.0f);
    }

    public RoPEPositionalEncoding(int maxSeqLen, int headDim, float base) {
        if (maxSeqLen <= 0) {
            throw new IllegalArgumentException("maxSeqLen must be positive: " + maxSeqLen);
        }
        if (headDim <= 0 || (headDim % 2) != 0) {
            throw new IllegalArgumentException("headDim must be a positive even number: " + headDim);
        }
        if (base <= 0.0f) {
            throw new IllegalArgumentException("base must be positive: " + base);
        }
        this.maxSeqLen = maxSeqLen;
        this.headDim = headDim;
        this.base = base;

        int pairs = headDim / 2;
        this.cosTable = new float[maxSeqLen * pairs];
        this.sinTable = new float[maxSeqLen * pairs];

        for (int pos = 0; pos < maxSeqLen; pos++) {
            for (int i = 0; i < pairs; i++) {
                double theta = pos / Math.pow(base, (2.0 * i) / headDim);
                cosTable[pos * pairs + i] = (float) Math.cos(theta);
                sinTable[pos * pairs + i] = (float) Math.sin(theta);
            }
        }
    }

    public int getMaxSeqLen() { return maxSeqLen; }
    public int getHeadDim() { return headDim; }
    public float getBase() { return base; }

    /**
     * Rotates a [seqLen, dModel] Q or K tensor in place.
     *
     * @param data      flat row-major tensor data (length seqLen * dModel)
     * @param dModel    model dimension
     * @param numHeads  number of attention heads (headDim == dModel / numHeads)
     * @param seqLen    number of rows to process (may be less than the tensor rows)
     * @param startPos  absolute position of the first row
     */
    public void rotateInPlace(float[] data, int dModel, int numHeads, int seqLen, int startPos) {
        int perHead = dModel / numHeads;
        if (perHead != headDim) {
            throw new IllegalArgumentException("Attention head dimension (" + perHead
                    + ") does not match RoPE headDim (" + headDim + ")");
        }
        if (startPos < 0 || (long) startPos + seqLen > maxSeqLen) {
            throw new IllegalArgumentException("RoPE position range [" + startPos + ", " + (startPos + seqLen)
                    + ") exceeds table length " + maxSeqLen);
        }
        int pairs = headDim / 2;

        for (int r = 0; r < seqLen; r++) {
            int absolutePos = startPos + r;
            int rowBase = r * dModel;
            int tableBase = absolutePos * pairs;

            for (int h = 0; h < numHeads; h++) {
                int headBase = rowBase + h * headDim;
                for (int i = 0; i < pairs; i++) {
                    int idx = headBase + 2 * i;
                    float cos = cosTable[tableBase + i];
                    float sin = sinTable[tableBase + i];
                    float x0 = data[idx];
                    float x1 = data[idx + 1];
                    data[idx] = x0 * cos - x1 * sin;
                    data[idx + 1] = x0 * sin + x1 * cos;
                }
            }
        }
    }

    /**
     * Applies the inverse rotation (transpose) to a gradient tensor in place.
     * Signature and layout match {@link #rotateInPlace}.
     */
    public void rotateBackwardInPlace(float[] gradData, int dModel, int numHeads, int seqLen, int startPos) {
        int perHead = dModel / numHeads;
        if (perHead != headDim) {
            throw new IllegalArgumentException("Attention head dimension (" + perHead
                    + ") does not match RoPE headDim (" + headDim + ")");
        }
        if (startPos < 0 || (long) startPos + seqLen > maxSeqLen) {
            throw new IllegalArgumentException("RoPE position range [" + startPos + ", " + (startPos + seqLen)
                    + ") exceeds table length " + maxSeqLen);
        }
        int pairs = headDim / 2;

        for (int r = 0; r < seqLen; r++) {
            int absolutePos = startPos + r;
            int rowBase = r * dModel;
            int tableBase = absolutePos * pairs;

            for (int h = 0; h < numHeads; h++) {
                int headBase = rowBase + h * headDim;
                for (int i = 0; i < pairs; i++) {
                    int idx = headBase + 2 * i;
                    float cos = cosTable[tableBase + i];
                    float sin = sinTable[tableBase + i];
                    float d0 = gradData[idx];
                    float d1 = gradData[idx + 1];
                    gradData[idx] = d0 * cos + d1 * sin;
                    gradData[idx + 1] = -d0 * sin + d1 * cos;
                }
            }
        }
    }
}
