package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import eu.kalafatic.evolution.forge.model.inference.KVCache;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Production Canonical EVO Native Model Core.
 * Represents the single source of truth for model structure, parameters, training state, and lifecycle.
 * Positional information is applied either as additive sinusoidal absolute encoding (legacy artifacts,
 * {@code positionalEncoding=sinusoidal_abs}) or as rotary positional embedding inside attention
 * (current training standard, {@code positionalEncoding=rope} — required for faithful GGUF/llama.cpp export).
 */
public class EvoLlmModel {

    /**
     * Immutable execution context holding activations for a single forward pass.
     */
    public static class ForwardContext {
        public final Tensor inputTokens;
        public final Tensor embedded;
        public final List<Tensor> blockInputs = new ArrayList<>();
        public final Tensor finalNormed;
        public final Tensor logits;

        public ForwardContext(Tensor inputTokens, Tensor embedded, Tensor finalNormed, Tensor logits) {
            this.inputTokens = inputTokens;
            this.embedded = embedded;
            this.finalNormed = finalNormed;
            this.logits = logits;
        }
    }

    private final Embedding embedding;
    private final PositionalEncoding positionalEncoding;
    private final RoPEPositionalEncoding rope;
    private final List<TransformerBlock> blocks;
    private final RMSNorm outputNorm;
    private final Tensor lmHead;
    private final EvoLlmArchitecture architecture;
    private final boolean tieEmbeddings;

    private final TrainingState trainingState;
    private final ModelMetadata metadata;
    private final Map<Integer, String> idToToken;
    private Tensor lastFinalNormed;

    public EvoLlmModel(EvoLlmArchitecture architecture, boolean tieEmbeddings) {
        this.architecture = Objects.requireNonNull(architecture, "architecture cannot be null");
        this.tieEmbeddings = tieEmbeddings;

        this.embedding = new Embedding(architecture.getVocabSize(), architecture.getDModel());

        if (architecture.usesRoPE()) {
            this.positionalEncoding = null;
            this.rope = new RoPEPositionalEncoding(
                    Math.max(128, architecture.getMaxSeqLen()),
                    architecture.getDModel() / architecture.getNumHeads()
            );
        } else {
            this.positionalEncoding = new PositionalEncoding(Math.max(128, architecture.getMaxSeqLen()), architecture.getDModel());
            this.rope = null;
        }

        this.blocks = new ArrayList<>();

        for (int i = 0; i < architecture.getNumBlocks(); i++) {
            blocks.add(new TransformerBlock(
                    architecture.getDModel(),
                    architecture.getNumHeads(),
                    architecture.getDff(),
                    rope
            ));
        }

        this.outputNorm = new RMSNorm(architecture.getDModel());

        if (tieEmbeddings) {
            this.lmHead = this.embedding.getWeights();
        } else {
            this.lmHead = new SimpleTensor(architecture.getDModel(), architecture.getVocabSize());
            initXavier(this.lmHead, architecture.getDModel(), architecture.getVocabSize());
        }

        this.trainingState = new TrainingState();
        this.metadata = new ModelMetadata("evo_model", "1.0.0", "evo_llm");
        this.idToToken = new LinkedHashMap<>();
    }

    public EvoLlmModel(EvoLlmArchitecture architecture) {
        this(architecture, false);
    }

    public EvoLlmModel(int vocabSize, int dModel, int numHeads, int numBlocks, int dff, int maxSeqLen) {
        this(new EvoLlmArchitecture(vocabSize, dModel, numHeads, numBlocks, dff, maxSeqLen), false);
    }

    public EvoLlmModel(int vocabSize, int dModel, int numHeads, int numBlocks, int dff, int maxSeqLen, String positionalEncoding) {
        this(new EvoLlmArchitecture(vocabSize, dModel, numHeads, numBlocks, dff, maxSeqLen, positionalEncoding), false);
    }

    private static void initXavier(Tensor tensor, int fanIn, int fanOut) {
        float[] data = tensor.getData();
        if (data == null) return;
        Random rand = new Random(42);
        float std = (float) Math.sqrt(2.0 / (fanIn + fanOut));
        for (int i = 0; i < data.length; i++) {
            data[i] = (float) (rand.nextGaussian() * std);
        }
    }

    /**
     * Single sequence forward pass for inference / generation: token IDs [seqLen] -> Logits tensor [seqLen, vocabSize].
     */
    public Tensor forward(int[] inputIds) {
        if (inputIds == null || inputIds.length == 0) {
            throw new IllegalArgumentException("Input token IDs cannot be null or empty");
        }
        Tensor x = embedding.forward(inputIds);
        if (positionalEncoding != null) {
            x = positionalEncoding.forward(x);
        }

        for (TransformerBlock block : blocks) {
            x = block.forward(x);
        }
        this.lastFinalNormed = outputNorm.forward(x);
        return lastFinalNormed.matmul(lmHead);
    }

    /**
     * Single sequence forward pass optimized for auto-regressive next-token generation:
     * Computes embeddings, block representations, and final norm, but multiplies lmHead
     * ONLY for the last token position (seqLen - 1). Returns [1, vocabSize] logits tensor.
     */
    public Tensor forwardLast(int[] inputIds) {
        if (inputIds == null || inputIds.length == 0) {
            throw new IllegalArgumentException("Input token IDs cannot be null or empty");
        }
        Tensor x = embedding.forward(inputIds);
        if (positionalEncoding != null) {
            x = positionalEncoding.forward(x);
        }

        for (TransformerBlock block : blocks) {
            x = block.forward(x);
        }
        this.lastFinalNormed = outputNorm.forward(x);
        int seqLen = inputIds.length;
        int dModel = architecture.getDModel();

        float[] lastRowData = new float[dModel];
        System.arraycopy(lastFinalNormed.getData(), (seqLen - 1) * dModel, lastRowData, 0, dModel);
        Tensor lastRowTensor = new SimpleTensor(new long[]{1, dModel}, lastRowData);

        return lastRowTensor.matmul(lmHead);
    }

    /**
     * KV-Cached forward pass for high-performance auto-regressive generation.
     * Applies the positional encoding at the correct absolute position offset:
     * prefill starts at 0, each subsequent single-token step uses the cached
     * sequence length as the position offset.
     */
    public Tensor forwardWithCache(int[] inputIds, KVCache kvCache) {
        if (inputIds == null || inputIds.length == 0) {
            throw new IllegalArgumentException("Input token IDs cannot be null or empty");
        }
        int positionOffset = (kvCache != null) ? kvCache.getCachedSequenceLength() : 0;

        Tensor x = embedding.forward(inputIds);
        if (positionalEncoding != null) {
            x = positionalEncoding.forwardAtOffset(x, positionOffset);
        }

        for (int i = 0; i < blocks.size(); i++) {
            KVCache.LayerKVCache layerCache = (kvCache != null) ? kvCache.getLayerCache(i) : null;
            x = blocks.get(i).forwardWithCache(x, layerCache);
        }
        this.lastFinalNormed = outputNorm.forward(x);
        int seqLen = inputIds.length;
        if (seqLen == 1) {
            return lastFinalNormed.matmul(lmHead);
        } else {
            int dModel = architecture.getDModel();
            float[] lastRowData = new float[dModel];
            System.arraycopy(lastFinalNormed.getData(), (seqLen - 1) * dModel, lastRowData, 0, dModel);
            Tensor lastRowTensor = new SimpleTensor(new long[]{1, dModel}, lastRowData);
            return lastRowTensor.matmul(lmHead);
        }
    }

    /**
     * Overload for forward pass accepting attention mask (single array or matrix).
     * Mask semantics: 1.0 = attend, 0.0 = masked (e.g. padding position).
     */
    public Tensor forward(int[] inputIds, float[] attentionMask) {
        if (inputIds == null || inputIds.length == 0) {
            throw new IllegalArgumentException("Input token IDs cannot be null or empty");
        }
        if (attentionMask != null && attentionMask.length != inputIds.length) {
            throw new IllegalArgumentException("Attention mask length (" + attentionMask.length
                    + ") must match input length (" + inputIds.length + ")");
        }
        Tensor x = embedding.forward(inputIds);
        if (positionalEncoding != null) {
            x = positionalEncoding.forward(x);
        }

        for (TransformerBlock block : blocks) {
            x = block.forward(x, attentionMask);
        }
        this.lastFinalNormed = outputNorm.forward(x);
        return lastFinalNormed.matmul(lmHead);
    }

    /**
     * Batched forward pass for training with loss mask support: [B, T] -> Logits [B, T, V].
     * Processes every batch row (no fake single-row batching) and stacks the per-row
     * logits into a [B, T, V] row-major tensor.
     */
    public ForwardContext forward(int[][] batchInput, float[][] attentionMask) {
        if (batchInput == null || batchInput.length == 0) {
            throw new IllegalArgumentException("Batch input cannot be null or empty");
        }
        if (attentionMask != null && attentionMask.length != batchInput.length) {
            throw new IllegalArgumentException("Attention mask batch size (" + attentionMask.length
                    + ") must match input batch size (" + batchInput.length + ")");
        }
        int b = batchInput.length;
        int t = batchInput[0].length;
        int v = getVocabSize();

        Tensor logits = new SimpleTensor(new long[]{b, t, v});
        float[] logitsData = logits.getData();

        for (int i = 0; i < b; i++) {
            if (batchInput[i] == null || batchInput[i].length != t) {
                throw new IllegalArgumentException("All batch rows must have equal length " + t + ", row " + i + " differs");
            }
            float[] rowMask = (attentionMask != null) ? attentionMask[i] : null;
            Tensor rowLogits = forward(batchInput[i], rowMask);
            System.arraycopy(rowLogits.getData(), 0, logitsData, i * t * v, t * v);
        }

        this.lastFinalNormed = null;
        return new ForwardContext(null, logits, null, logits);
    }

    /**
     * Single sequence backward pass for training loss optimization.
     */
    public void backward(Tensor dLogits) {
        if (dLogits == null) return;

        if (lastFinalNormed != null) {
            Tensor dLmHead = lastFinalNormed.transpose().matmul(dLogits);
            float[] lmGrad = lmHead.getGrad();
            float[] dLmHeadData = dLmHead.getData();
            if (lmGrad != null && dLmHeadData != null) {
                for (int i = 0; i < lmGrad.length; i++) {
                    lmGrad[i] += dLmHeadData[i];
                }
            }
        }

        Tensor dFinalNormed = dLogits.matmul(lmHead.transpose());
        Tensor dx = outputNorm.backward(dFinalNormed);

        for (int i = blocks.size() - 1; i >= 0; i--) {
            dx = blocks.get(i).backward(dx);
        }

        if (positionalEncoding != null) {
            dx = positionalEncoding.backward(dx);
        }
        embedding.backward(dx);
    }

    /**
     * Returns ordered list of all model parameter tensors.
     */
    public List<Tensor> parameters() {
        List<Tensor> params = new ArrayList<>();

        params.add(embedding.getWeights());

        for (TransformerBlock block : blocks) {
            params.add(block.getAttnNorm().getWeight());
            params.add(block.getAttention().getWQ());
            params.add(block.getAttention().getWK());
            params.add(block.getAttention().getWV());
            params.add(block.getAttention().getWO());

            params.add(block.getFfnNorm().getWeight());
            params.add(block.getFfn().getW1());
            params.add(block.getFfn().getW3());
            params.add(block.getFfn().getW2());
        }

        params.add(outputNorm.getWeight());

        if (!tieEmbeddings) {
            params.add(lmHead);
        }

        return params;
    }

    /**
     * Returns first-class canonical parameter registry with stable tensor names.
     */
    public ModelParameters getModelParameters() {
        DefaultModelParameters registry = new DefaultModelParameters();
        registry.register("token_embd.weight", embedding.getWeights());

        for (int i = 0; i < blocks.size(); i++) {
            TransformerBlock block = blocks.get(i);
            registry.register("blk." + i + ".attn_norm.weight", block.getAttnNorm().getWeight());
            registry.register("blk." + i + ".attn_q.weight", block.getAttention().getWQ());
            registry.register("blk." + i + ".attn_k.weight", block.getAttention().getWK());
            registry.register("blk." + i + ".attn_v.weight", block.getAttention().getWV());
            registry.register("blk." + i + ".attn_output.weight", block.getAttention().getWO());

            registry.register("blk." + i + ".ffn_norm.weight", block.getFfnNorm().getWeight());
            registry.register("blk." + i + ".ffn_gate.weight", block.getFfn().getW1());
            registry.register("blk." + i + ".ffn_up.weight", block.getFfn().getW3());
            registry.register("blk." + i + ".ffn_down.weight", block.getFfn().getW2());
        }

        registry.register("output_norm.weight", outputNorm.getWeight());
        if (!tieEmbeddings) {
            registry.register("output.weight", lmHead);
        }

        return registry;
    }

    /**
     * Creates an immutable ModelSnapshot representing the export/serialization boundary.
     */
    public ModelSnapshot createSnapshot() {
        return new DefaultModelSnapshot(
                "EVO_NATIVE",
                1,
                architecture,
                getModelParameters(),
                idToToken,
                trainingState,
                metadata
        );
    }

    /**
     * Persists the native model directly to directory.
     */
    public void save(Path dir) throws IOException {
        EvoModelSerializer.save(this, dir, trainingState.getLastLoss(), trainingState.getEpoch());
    }

    /**
     * Loads a native model from directory.
     */
    public static EvoLlmModel load(Path dir) throws IOException {
        return EvoModelSerializer.load(dir);
    }

    // ============ Getters & Architecture Dimension Accessors ============
    public EvoLlmArchitecture getArchitecture() { return architecture; }
    public boolean isTieEmbeddings() { return tieEmbeddings; }

    public int getVocabSize() { return architecture.getVocabSize(); }
    public int getDModel() { return architecture.getDModel(); }
    public int getNumHeads() { return architecture.getNumHeads(); }
    public int getNumBlocks() { return architecture.getNumBlocks(); }
    public int getDff() { return architecture.getDff(); }
    public int getMaxSeqLen() { return architecture.getMaxSeqLen(); }

    public List<TransformerBlock> getBlocks() { return blocks; }
    public Tensor getLmHead() { return lmHead; }

    public TrainingState getTrainingState() { return trainingState; }
    public ModelMetadata getMetadata() { return metadata; }
    public Map<Integer, String> getIdToToken() { return idToToken; }
    public PositionalEncoding getPositionalEncoding() { return positionalEncoding; }
    public RoPEPositionalEncoding getRoPEPositionalEncoding() { return rope; }
}
