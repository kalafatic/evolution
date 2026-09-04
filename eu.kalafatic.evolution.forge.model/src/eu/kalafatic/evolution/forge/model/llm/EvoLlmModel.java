package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Production Causal Transformer Model (Llama-style Architecture).
 * Supports Batched Input [B, T], RoPE, RMSNorm, SwiGLU, and Attention Masking.
 */
public class EvoLlmModel {

    /**
     * Immutable execution context holding activations for a single forward pass.
     * Guarantees thread-safety during gradient accumulation pipelines.
     */
    public static class ForwardContext {
        public final Tensor inputTokens;     // [B, T]
        public final Tensor embedded;        // [B, T, D]
        public final List<Tensor> blockInputs = new ArrayList<>();
        public final Tensor finalNormed;     // [B, T, D]
        public final Tensor logits;          // [B, T, V]

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

    public EvoLlmModel(EvoLlmArchitecture architecture, boolean tieEmbeddings) {
        this.architecture = Objects.requireNonNull(architecture, "architecture cannot be null");
        this.tieEmbeddings = tieEmbeddings;

        this.embedding = new Embedding(architecture.getVocabSize(), architecture.getDModel());
        this.blocks = new ArrayList<>();

        for (int i = 0; i < architecture.getNumBlocks(); i++) {
            blocks.add(new TransformerBlock(
                    architecture.getDModel(),
                    architecture.getNumHeads(),
                    architecture.getNumKVHeads(), // Supports Grouped-Query Attention (GQA)
                    architecture.getDff(),
                    architecture.getMaxSeqLen()
            ));
        }

        this.outputNorm = new RMSNorm(architecture.getDModel());

        if (tieEmbeddings) {
            this.lmHead = this.embedding.getWeights(); // Shared parameter tensor
        } else {
            this.lmHead = new SimpleTensor(architecture.getDModel(), architecture.getVocabSize());
            this.lmHead.initXavier();
        }
    }

    public EvoLlmModel(EvoLlmArchitecture architecture) {
        this(architecture, false);
    }

    /**
     * Batched Forward Pass: Maps token matrix [B, T] -> Logits tensor [B, T, V].
     *
     * @param batchInput    Matrix of token IDs with shape [B, T]
     * @param attentionMask Padding/Attention Mask with shape [B, T] (1.0 = valid, 0.0 = pad)
     * @return Execution context containing output logits and layer activations.
     */
    public ForwardContext forward(int[][] batchInput, float[][] attentionMask) {
        Tensor inputTensor = SimpleTensor.fromIntArray(batchInput); // [B, T]
        Tensor x = embedding.forward(inputTensor);                 // [B, T, D]

        ForwardContext context = new ForwardContext(inputTensor, x, null, null);

        // Transformer blocks execute RoPE internally on Q & K
        for (TransformerBlock block : blocks) {
            context.blockInputs.add(x);
            x = block.forward(x, attentionMask); // [B, T, D]
        }

        Tensor finalNormed = outputNorm.forward(x);
        Tensor logits = finalNormed.matmul(lmHead); // [B, T, D] x [D, V] -> [B, T, V]

        return new ForwardContext(inputTensor, x, finalNormed, logits);
    }

    /**
     * Backward Pass: Propagates loss gradients dLogits [B, T, V] through network.
     * Enforces ADDITIVE GRADIENT ACCUMULATION (grad += dGrad) on all parameter buffers.
     *
     * @param context Activation context returned by forward()
     * @param dLogits Loss gradient tensor with shape [B, T, V]
     */
    public void backward(ForwardContext context, Tensor dLogits) {
        if (context == null || dLogits == null) return;

        // 1. LM Head Gradients
        Tensor dFinalNormed = dLogits.matmul(lmHead.transpose());            // [B, T, D]
        Tensor dLmHead = context.finalNormed.transpose().matmul(dLogits);   // [D, V]
        lmHead.accumulateGrad(dLmHead);

        // 2. Output RMSNorm Backward
        Tensor dx = outputNorm.backward(context.finalNormed, dFinalNormed);

        // 3. Backpropagate through Transformer Blocks in reverse order
        for (int i = blocks.size() - 1; i >= 0; i--) {
            Tensor blockInput = context.blockInputs.get(i);
            dx = blocks.get(i).backward(blockInput, dx);
        }

        // 4. Embedding Backward
        embedding.backward(context.inputTokens, dx);
    }

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
            params.add(block.getFfn().getW1()); // Gate
            params.add(block.getFfn().getW3()); // Up
            params.add(block.getFfn().getW2()); // Down
        }

        params.add(outputNorm.getWeight());

        if (!tieEmbeddings) {
            params.add(lmHead);
        }

        return params;
    }

    public EvoLlmArchitecture getArchitecture() { return architecture; }
    public boolean isTieEmbeddings() { return tieEmbeddings; }
}
