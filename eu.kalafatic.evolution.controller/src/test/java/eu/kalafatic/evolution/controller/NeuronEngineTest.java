package eu.kalafatic.evolution.controller;

import org.junit.Test;
import org.junit.Assert;
import eu.kalafatic.evolution.model.orchestration.NeuronType;

public class NeuronEngineTest {

    @Test
    public void testMLP() {
        NeuronEngine engine = new NeuronEngine();
        String result = engine.runModel(NeuronType.MLP, "testModel", "hello world");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("MLP"));
        Assert.assertTrue(result.contains("probabilities"));
    }

    @Test
    public void testCNN() {
        NeuronEngine engine = new NeuronEngine();
        String result = engine.runModel(NeuronType.CNN, "testModel", "hello world convolutional network");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("CNN"));
        Assert.assertTrue(result.contains("Feature map max"));
    }

    @Test
    public void testRNN() {
        NeuronEngine engine = new NeuronEngine();
        String result = engine.runModel(NeuronType.RNN, "testModel", "recurrent neural network processing");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("RNN"));
        Assert.assertTrue(result.contains("norm"));
    }

    @Test
    public void testLSTM() {
        NeuronEngine engine = new NeuronEngine();
        String result = engine.runModel(NeuronType.LSTM, "testModel", "long short term memory sequence");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("LSTM"));
        Assert.assertTrue(result.contains("Gated memory updated"));
    }

    @Test
    public void testTransformer() {
        NeuronEngine engine = new NeuronEngine();
        String result = engine.runModel(NeuronType.TRANSFORMER, "testModel", "transformer self attention attention");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("Transformer"));
        Assert.assertTrue(result.contains("Self-attention applied"));
    }
}
