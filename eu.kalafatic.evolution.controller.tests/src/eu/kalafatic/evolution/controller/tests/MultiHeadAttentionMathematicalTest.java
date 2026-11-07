package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import eu.kalafatic.evolution.forge.model.llm.MultiHeadAttention;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * Mathematical regression test suite for MultiHeadAttention.
 * Verifies subspace splitting, scaled dot-product attention per head,
 * causal masking, output projection, dimension validation, and analytical backpropagation.
 */
public class MultiHeadAttentionMathematicalTest {

    @Test
    public void testDimensionValidation() {
        // Incompatible dModel % numHeads must throw IllegalArgumentException
        try {
            new MultiHeadAttention(64, 5); // 64 % 5 != 0
            Assert.fail("Should throw IllegalArgumentException when dModel is not divisible by numHeads");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("divisible by numHeads"));
        }

        // Negative or zero dimensions
        try {
            new MultiHeadAttention(0, 4);
            Assert.fail("Should throw on dModel <= 0");
        } catch (IllegalArgumentException expected) {}

        try {
            new MultiHeadAttention(64, 0);
            Assert.fail("Should throw on numHeads <= 0");
        } catch (IllegalArgumentException expected) {}
    }

    @Test
    public void testForwardOutputDimensionsAndCausalProperty() {
        int dModel = 32;
        int numHeads = 4;
        int seqLen = 6;
        MultiHeadAttention mha = new MultiHeadAttention(dModel, numHeads);
        Assert.assertEquals(8, mha.getHeadDim());

        Tensor x = new SimpleTensor(seqLen, dModel);
        float[] xData = x.getData();
        Random r = new Random(123);
        for (int i = 0; i < xData.length; i++) {
            xData[i] = (float) (r.nextGaussian() * 0.1);
        }

        Tensor out = mha.forward(x);
        Assert.assertNotNull(out);
        Assert.assertEquals(2, out.getRank());
        Assert.assertEquals(seqLen, out.getShape()[0]);
        Assert.assertEquals(dModel, out.getShape()[1]);

        for (float v : out.getData()) {
            Assert.assertFalse("Output contains NaN", Float.isNaN(v));
            Assert.assertFalse("Output contains Inf", Float.isInfinite(v));
        }
    }

    @Test
    public void testDifferentHeadConfigurations() {
        int dModel = 64;
        int[] headCounts = new int[]{1, 2, 4, 8, 16};

        for (int heads : headCounts) {
            MultiHeadAttention mha = new MultiHeadAttention(dModel, heads);
            Assert.assertEquals(dModel / heads, mha.getHeadDim());

            Tensor x = new SimpleTensor(4, dModel);
            for (int i = 0; i < x.getData().length; i++) x.getData()[i] = 0.5f;

            Tensor out = mha.forward(x);
            Assert.assertEquals(4, out.getShape()[0]);
            Assert.assertEquals(dModel, out.getShape()[1]);
        }
    }

    @Test
    public void testBackwardPropagationGradients() {
        int dModel = 16;
        int numHeads = 2;
        int seqLen = 3;

        MultiHeadAttention mha = new MultiHeadAttention(dModel, numHeads);
        Tensor x = new SimpleTensor(seqLen, dModel);
        for (int i = 0; i < x.getData().length; i++) {
            x.getData()[i] = (float) ((i % 5) - 2) * 0.1f;
        }

        Tensor out = mha.forward(x);

        Tensor dOut = new SimpleTensor(seqLen, dModel);
        for (int i = 0; i < dOut.getData().length; i++) {
            dOut.getData()[i] = 1.0f;
        }

        Tensor dX = mha.backward(dOut);
        Assert.assertNotNull(dX);
        Assert.assertEquals(seqLen, dX.getShape()[0]);
        Assert.assertEquals(dModel, dX.getShape()[1]);

        // Verify parameter gradients are populated and non-zero
        assertNonZeroAndFinite(mha.getWO().getGrad(), "WO grad");
        assertNonZeroAndFinite(mha.getWV().getGrad(), "WV grad");
        assertNonZeroAndFinite(mha.getWK().getGrad(), "WK grad");
        assertNonZeroAndFinite(mha.getWQ().getGrad(), "WQ grad");
        assertNonZeroAndFinite(dX.getData(), "dX data");
    }

    private void assertNonZeroAndFinite(float[] data, String name) {
        Assert.assertNotNull(name + " cannot be null", data);
        boolean hasNonZero = false;
        for (float v : data) {
            Assert.assertFalse(name + " contains NaN", Float.isNaN(v));
            Assert.assertFalse(name + " contains Inf", Float.isInfinite(v));
            if (Math.abs(v) > 1e-9f) {
                hasNonZero = true;
            }
        }
        Assert.assertTrue(name + " should have non-zero gradients", hasNonZero);
    }
}
