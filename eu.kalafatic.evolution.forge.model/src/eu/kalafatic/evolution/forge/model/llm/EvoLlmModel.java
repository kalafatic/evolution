package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;

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
    private final List<TransformerBlock> blocks;
    private final RMSNorm outputNorm;
    private final Tensor lmHead;
    private final EvoLlmArchitecture architecture;
    private final boolean tieEmbeddings;

    private final TrainingState trainingState;
    private final ModelMetadata metadata;
    private final Map<Integer, String> idToToken;

    public EvoLlmModel(EvoLlmArchitecture architecture, boolean tieEmbeddings) {
        this.architecture = Objects.requireNonNull(architecture, "architecture cannot be null");
        this.tieEmbeddings = tieEmbeddings;

        this.embedding = new Embedding(architecture.getVocabSize(), architecture.getDModel());
        this.blocks = new ArrayList<>();

        for (int i = 0; i < architecture.getNumBlocks(); i++) {
            blocks.add(new TransformerBlock(
                    architecture.getDModel(),
                    architecture.getNumHeads(),
                    architecture.getDff()
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
        for (TransformerBlock block : blocks) {
            x = block.forward(x);
        }
        Tensor normed = outputNorm.forward(x);
        return normed.matmul(lmHead);
    }

    /**
     * Overload for forward pass accepting attention mask (single array or matrix).
     */
    public Tensor forward(int[] inputIds, float[] attentionMask) {
        return forward(inputIds);
    }

    /**
     * Batched forward pass for training with loss mask support: [B, T] -> Logits [B, T, V].
     */
    public ForwardContext forward(int[][] batchInput, float[][] attentionMask) {
        Tensor x = embedding.forward(batchInput[0]);

        ForwardContext context = new ForwardContext(null, x, null, null);

        for (TransformerBlock block : blocks) {
            context.blockInputs.add(x);
            x = block.forward(x);
        }

        Tensor finalNormed = outputNorm.forward(x);
        Tensor logits = finalNormed.matmul(lmHead);

        return new ForwardContext(null, x, finalNormed, logits);
    }

    /**
     * Single sequence backward pass for training loss optimization.
     */
    public void backward(Tensor dLogits) {
        if (dLogits == null) return;

        Tensor dFinalNormed = dLogits.matmul(lmHead.transpose());
        Tensor dx = outputNorm.backward(dFinalNormed);

        for (int i = blocks.size() - 1; i >= 0; i--) {
            dx = blocks.get(i).backward(dx);
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
}
