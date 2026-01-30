package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.junit.Test;
import eu.kalafatic.evolution.controller.engine.NeuronEngine;
import eu.kalafatic.evolution.model.orchestration.NeuronType;

public class NeuronEngineTest {

    @Test
    public void testRunMLP() {
        NeuronEngine engine = new NeuronEngine();
        String result = engine.runModel(NeuronType.MLP, "test-model", "hello world");
        assertNotNull(result);
        assertTrue(result.contains("NeuronAI MLP"));
        assertTrue(result.contains("test-model"));
    }

    @Test
    public void testRunTransformer() {
        NeuronEngine engine = new NeuronEngine();
        String result = engine.runModel(NeuronType.TRANSFORMER, "test-transformer", "this is a test prompt for attention");
        assertNotNull(result);
        assertTrue(result.contains("NeuronAI Transformer"));
        assertTrue(result.contains("Multi-head self-attention"));
    }

    @Test
    public void testRunLSTM() {
        NeuronEngine engine = new NeuronEngine();
        String result = engine.runModel(NeuronType.LSTM, "test-lstm", "long sequence of data to be remembered");
        assertNotNull(result);
        assertTrue(result.contains("NeuronAI LSTM"));
    }
}
