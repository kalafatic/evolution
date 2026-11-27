package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.MultiHeadAttention;
import eu.kalafatic.evolution.forge.model.llm.RoPEPositionalEncoding;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * Regression tests for RoPE positional encoding and its integration into
 * EvoLlmModel / MultiHeadAttention. Guards the fix for the GGUF/Ollama
 * inference corruption (the model was trained with additive sinusoidal PE
 * while the exported GGUF declared llama/RoPE).
 */
public class RoPEPositionalEncodingTest {

    @Test
    public void testRoPEPairFormulaMatchesLlamaConvention() {
        int maxSeq = 32;
        int headDim = 4;
        RoPEPositionalEncoding rope = new RoPEPositionalEncoding(maxSeq, headDim, 10000f);

        // Single position (1) with a single head: first pair rotation must match
        // x0' = x0*cos(theta) - x1*sin(theta), x1' = x0*sin(theta) + x1*cos(theta).
        int dModel = headDim; // one head
        float[] data = new float[]{1.0f, 2.0f, 3.0f, 4.0f};
        double theta = 1.0 / Math.pow(10000.0, 0.0 / headDim);
        float c = (float) Math.cos(theta);
        float s = (float) Math.sin(theta);

        rope.rotateInPlace(data, dModel, 1, 1, 1);

        Assert.assertEquals("x0' mismatch", 1.0f * c - 2.0f * s, data[0], 1e-5f);
        Assert.assertEquals("x1' mismatch", 1.0f * s + 2.0f * c, data[1], 1e-5f);
    }

    @Test
    public void testRoPEInverseRotationRoundTrip() {
        int maxSeq = 64;
        int headDim = 8;
        int dModel = 32;
        int heads = 4;
        int seqLen = 6;
        RoPEPositionalEncoding rope = new RoPEPositionalEncoding(maxSeq, headDim, 10000f);

        Random r = new Random(7);
        float[] original = new float[seqLen * dModel];
        for (int i = 0; i < original.length; i++) original[i] = r.nextFloat() * 2f - 1f;

        float[] rotated = original.clone();
        rope.rotateInPlace(rotated, dModel, heads, seqLen, 3);
        rope.rotateBackwardInPlace(rotated, dModel, heads, seqLen, 3);

        for (int i = 0; i < original.length; i++) {
            Assert.assertEquals("Roundtrip mismatch at " + i, original[i], rotated[i], 1e-5f);
        }
    }

    @Test
    public void testRoPEBackwardMatchesFiniteDifference() {
        // Gradient of sum(rotate(x)) w.r.t. x must equal the inverse rotation
        // applied to an all-ones gradient (rotation is linear and orthogonal).
        int maxSeq = 32;
        int headDim = 4;
        int dModel = 16;
        int heads = 4;
        int seqLen = 3;
        RoPEPositionalEncoding rope = new RoPEPositionalEncoding(maxSeq, headDim, 10000f);

        Random r = new Random(11);
        float[] x = new float[seqLen * dModel];
        for (int i = 0; i < x.length; i++) x[i] = r.nextFloat() * 2f - 1f;

        float[] y = x.clone();
        rope.rotateInPlace(y, dModel, heads, seqLen, 5);
        float[] grad = new float[y.length];
        java.util.Arrays.fill(grad, 1.0f);
        rope.rotateBackwardInPlace(grad, dModel, heads, seqLen, 5);

        for (int i = 0; i < x.length; i++) {
            float eps = 1e-3f;
            float[] xp = x.clone(); xp[i] += eps;
            rope.rotateInPlace(xp, dModel, heads, seqLen, 5);
            float[] xm = x.clone(); xm[i] -= eps;
            rope.rotateInPlace(xm, dModel, heads, seqLen, 5);
            double numeric = (sum(xp) - sum(xm)) / (2.0 * eps);
            Assert.assertEquals("Gradient mismatch at " + i, grad[i], numeric, 2e-2);
        }
    }

    private double sum(float[] a) {
        double s = 0;
        for (float v : a) s += v;
        return s;
    }

    @Test
    public void testRopeModelForwardProducesDistinctPositionAwareOutputs() {
        // Different tokens at different positions: RoPE rotation makes each row's
        // attention position-dependent. (With identical tokens everywhere the
        // attention-weighted average of identical V vectors is trivially equal,
        // so distinctness requires distinct inputs.)
        int vocab = 64, dModel = 16, heads = 4;
        EvoLlmArchitecture arch = new EvoLlmArchitecture(vocab, dModel, heads, 1, 32, 64,
                EvoLlmArchitecture.POSITIONAL_ROPE);
        EvoLlmModel model = new EvoLlmModel(arch);

        Tensor logits = model.forward(new int[]{7, 12, 3, 21});
        float[] d = logits.getData();
        for (float v : d) {
            Assert.assertFalse("NaN in rope model logits", Float.isNaN(v));
            Assert.assertFalse("Inf in rope model logits", Float.isInfinite(v));
        }
        int firstOffset = 0;
        int lastOffset = 3 * vocab;
        boolean diff = false;
        for (int v = 0; v < vocab; v++) {
            if (Math.abs(d[firstOffset + v] - d[lastOffset + v]) > 1e-6f) {
                diff = true;
                break;
            }
        }
        Assert.assertTrue("RoPE positions must produce distinct logits", diff);
    }

    @Test
    public void testRopeModelBackwardPropagatesGradients() {
        int vocab = 32, dModel = 16, heads = 4;
        EvoLlmArchitecture arch = new EvoLlmArchitecture(vocab, dModel, heads, 1, 32, 32,
                EvoLlmArchitecture.POSITIONAL_ROPE);
        EvoLlmModel model = new EvoLlmModel(arch);
        int[] input = new int[]{2, 5, 12, 8};

        model.forward(input);
        Tensor dLogits = new SimpleTensor(input.length, vocab);
        for (int i = 0; i < dLogits.getData().length; i++) dLogits.getData()[i] = 0.1f;
        model.backward(dLogits);

        Tensor embWeights = model.parameters().get(0);
        float[] embGrad = embWeights.getGrad();
        boolean hasGrad = false;
        for (int id : input) {
            for (int d = 0; d < dModel; d++) {
                if (Math.abs(embGrad[id * dModel + d]) > 1e-7f) {
                    hasGrad = true;
                    break;
                }
            }
        }
        Assert.assertTrue("Embedding weights must receive gradients in rope mode", hasGrad);

        Tensor wq = model.getBlocks().get(0).getAttention().getWQ();
        float[] wqGrad = wq.getGrad();
        boolean wqHasGrad = false;
        for (float g : wqGrad) {
            if (Math.abs(g) > 1e-8f) {
                wqHasGrad = true;
                break;
            }
        }
        Assert.assertTrue("WQ must receive gradients in rope mode", wqHasGrad);
    }

    @Test
    public void testLegacySinusoidalModelFallback() {
        // The default (6-arg) constructor now uses RoPE for faithful GGUF/llama.cpp
        // export; legacy additive sinusoidal encoding remains available explicitly.
        EvoLlmArchitecture defaultArch = new EvoLlmArchitecture(32, 16, 2, 1, 32, 32);
        Assert.assertEquals(EvoLlmArchitecture.POSITIONAL_ROPE, defaultArch.getPositionalEncoding());
        EvoLlmModel defaultModel = new EvoLlmModel(defaultArch);
        Assert.assertNull(defaultModel.getPositionalEncoding());
        Assert.assertNotNull(defaultModel.getRoPEPositionalEncoding());

        EvoLlmArchitecture legacy = new EvoLlmArchitecture(32, 16, 2, 1, 32, 32,
                EvoLlmArchitecture.POSITIONAL_SINUSOIDAL_ABS);
        EvoLlmModel model = new EvoLlmModel(legacy);
        Assert.assertNotNull(model.getPositionalEncoding());
        Assert.assertNull(model.getRoPEPositionalEncoding());

        EvoLlmArchitecture ropeArch = new EvoLlmArchitecture(32, 16, 2, 1, 32, 32,
                EvoLlmArchitecture.POSITIONAL_ROPE);
        EvoLlmModel ropeModel = new EvoLlmModel(ropeArch);
        Assert.assertNull(ropeModel.getPositionalEncoding());
        Assert.assertNotNull(ropeModel.getRoPEPositionalEncoding());
    }

    @Test
    public void testRopeMultiHeadAttentionCachedForwardMatchesUncached() {
        // KV-cached generation (used by the inference engine) must reproduce the
        // non-cached forward for the last token: RoPE keys are rotated with their
        // absolute positions before being cached.
        int vocab = 64, dModel = 16, heads = 4;
        EvoLlmArchitecture arch = new EvoLlmArchitecture(vocab, dModel, heads, 2, 32, 64,
                EvoLlmArchitecture.POSITIONAL_ROPE);
        EvoLlmModel model = new EvoLlmModel(arch);

        int[] prompt = new int[]{4, 9, 3, 14};
        Tensor fullLogits = model.forward(prompt);
        int lastRow = prompt.length - 1;

        eu.kalafatic.evolution.forge.model.inference.KVCache kv = new eu.kalafatic.evolution.forge.model.inference.KVCache(2);
        Tensor prefillLogits = model.forwardWithCache(prompt, kv);
        int next = 25;
        Tensor stepLogits = model.forwardWithCache(new int[]{next}, kv);

        float[] full = fullLogits.getData();
        float[] prefill = prefillLogits.getData();
        for (int v = 0; v < vocab; v++) {
            Assert.assertEquals("Prefill/cached forward mismatch", full[lastRow * vocab + v], prefill[v], 1e-4f);
        }
        // The decode step must not produce NaN/Inf and must depend on the KV cache.
        float[] step = stepLogits.getData();
        for (float v : step) {
            Assert.assertFalse("NaN in cached rope decode step", Float.isNaN(v));
            Assert.assertFalse("Inf in cached rope decode step", Float.isInfinite(v));
        }
    }
}
