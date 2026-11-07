package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.PositionalEncoding;
import org.junit.Assert;
import org.junit.Test;

/**
 * Mathematical regression test for PositionalEncoding and its integration in EvoLlmModel.
 */
public class PositionalEncodingMathematicalTest {

    @Test
    public void testPositionalEncodingValuesAndBoundedness() {
        int maxSeqLen = 64;
        int dModel = 16;
        PositionalEncoding pe = new PositionalEncoding(maxSeqLen, dModel);

        Tensor input = new SimpleTensor(10, dModel);
        Tensor posOutput = pe.forward(input);

        Assert.assertEquals(10, posOutput.getShape()[0]);
        Assert.assertEquals(dModel, posOutput.getShape()[1]);

        float[] data = posOutput.getData();
        for (int i = 0; i < data.length; i++) {
            float v = data[i];
            Assert.assertFalse("Positional encoding produced NaN", Float.isNaN(v));
            Assert.assertFalse("Positional encoding produced Inf", Float.isInfinite(v));
            // Sinusoidal positional values are in [-1, 1]
            Assert.assertTrue("Positional value out of bounds: " + v, v >= -1.0001f && v <= 1.0001f);
        }
    }

    @Test
    public void testPositionDistinctness() {
        int dModel = 8;
        PositionalEncoding pe = new PositionalEncoding(16, dModel);

        Tensor zeroInput = new SimpleTensor(4, dModel);
        Tensor out = pe.forward(zeroInput);

        float[] pos0 = new float[dModel];
        float[] pos1 = new float[dModel];
        System.arraycopy(out.getData(), 0, pos0, 0, dModel);
        System.arraycopy(out.getData(), dModel, pos1, 0, dModel);

        // Different positions must have distinct encodings
        boolean diff = false;
        for (int i = 0; i < dModel; i++) {
            if (Math.abs(pos0[i] - pos1[i]) > 1e-4) {
                diff = true;
                break;
            }
        }
        Assert.assertTrue("Different token positions must have distinct positional encodings", diff);
    }

    @Test
    public void testModelIntegrationAndBackwardPass() {
        EvoLlmArchitecture arch = new EvoLlmArchitecture(32, 16, 2, 1, 32, 32);
        EvoLlmModel model = new EvoLlmModel(arch);

        Assert.assertNotNull("Positional encoding must be integrated in EvoLlmModel", model.getPositionalEncoding());

        int[] inputIds = new int[]{2, 5, 12, 8};
        Tensor logits = model.forward(inputIds);

        Assert.assertEquals(4, logits.getShape()[0]);
        Assert.assertEquals(32, logits.getShape()[1]);

        Tensor dLogits = new SimpleTensor(4, 32);
        for (int i = 0; i < dLogits.getData().length; i++) {
            dLogits.getData()[i] = 0.1f;
        }

        // Backward through the whole network including positional encoding
        model.backward(dLogits);

        // Embedding weights must receive gradients
        Tensor embWeights = model.parameters().get(0);
        float[] embGrad = embWeights.getGrad();
        boolean hasGrad = false;
        for (int id : inputIds) {
            for (int d = 0; d < 16; d++) {
                if (Math.abs(embGrad[id * 16 + d]) > 1e-7f) {
                    hasGrad = true;
                    break;
                }
            }
        }
        Assert.assertTrue("Embedding weights must receive backpropagated gradients through positional encoding", hasGrad);
    }
}
