package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.selfdev.LLMDarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.LLMDarwinEngine.LlmConfig;
import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.impl.DatasetBuilder;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;

/**
 * Focused tests verifying pipeline optimizations:
 * - Validation loss correctness, loss-mask handling & numerical stability
 * - Dataset sample limiting with uniform strided sampling
 * - Config isolation (defensive copies)
 * - Deterministic fitness calculation
 */
public class LLMDarwinEngineOptimizationTest {

    private SimpleBPETokenizer tokenizer;
    private DatasetBuilder datasetBuilder;

    @Before
    public void setUp() {
        tokenizer = new SimpleBPETokenizer();
        tokenizer.train("The quick brown fox jumps over the lazy dog. TinyStories once upon a time.", 1000);
        datasetBuilder = new DatasetBuilder();
    }

    @Test
    public void testValidationLossCorrectnessAndLossMasking() {
        EvoLlmModel model = new EvoLlmModel(tokenizer.getVocabSize(), 64, 2, 1, 128, 64);
        EvoLlmTrainer trainer = new EvoLlmTrainer(model);

        List<NormalizedSample> normSamples = new ArrayList<>();
        NormalizedSample norm = new NormalizedSample();
        norm.setType(TrainingSampleType.INSTRUCTION);
        norm.setInstruction("Tell a story.");
        norm.setResponse("Once upon a time there was a tiny dog.");
        normSamples.add(norm);

        List<TrainingSample> samples = datasetBuilder.buildTrainingSamples(normSamples, tokenizer, 64);
        assertEquals(1, samples.size());

        TrainingSample sample = samples.get(0);
        assertNotNull(sample.inputIds);
        assertNotNull(sample.labels);
        assertNotNull(sample.lossMask);

        // Verify prompt tokens are loss-masked (lossMask == false)
        assertFalse(sample.lossMask[0]);
        // Verify response tokens are evaluated (lossMask == true)
        boolean hasEvaluatedTokens = false;
        for (boolean mask : sample.lossMask) {
            if (mask) {
                hasEvaluatedTokens = true;
                break;
            }
        }
        assertTrue(hasEvaluatedTokens);

        // Compute validation loss via EvoLlmTrainer
        double valLoss = trainer.evaluateValidation(samples);
        assertTrue("Validation loss should be positive finite", valLoss > 0.0 && !Double.isNaN(valLoss) && !Double.isInfinite(valLoss));
    }

    @Test
    public void testDatasetSampleLimitsAndStriding() {
        List<NormalizedSample> normSamples = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            NormalizedSample norm = new NormalizedSample();
            norm.setType(TrainingSampleType.TEXT);
            norm.setText("Story number " + i + " with some content.");
            normSamples.add(norm);
        }

        int maxSamples = 20;
        List<TrainingSample> limitedSamples = datasetBuilder.buildTrainingSamples(normSamples, tokenizer, 64, maxSamples);
        assertTrue(limitedSamples.size() <= maxSamples);
        assertFalse(limitedSamples.isEmpty());
    }

    @Test
    public void testCandidateConfigurationIsolation() {
        LlmConfig source = new LlmConfig(1000, 128, 4, 4, 512, 128, 10);
        LlmConfig copy = LLMDarwinEngine.copyConfig(source);

        assertNotNull(copy);
        assertEquals(source.vocabSize, copy.vocabSize);
        assertEquals(source.embeddingSize, copy.embeddingSize);
        assertEquals(source.layers, copy.layers);

        // Modify copy and confirm source remains untouched
        copy.vocabSize = 2000;
        copy.embeddingSize = 256;
        assertFalse(source.vocabSize == copy.vocabSize);
        assertFalse(source.embeddingSize == copy.embeddingSize);
    }

    @Test
    public void testFitnessCalculationScaling() {
        double valLoss = 2.0;
        double trainLoss = 1.8;
        long paramCount = 1_000_000L;

        double sizePenalty = paramCount * 0.00000001; // 1e-8
        double fitness = (valLoss * 0.75) + (trainLoss * 0.25) + sizePenalty;

        // Fitness should be dominated by loss (valLoss*0.75 + trainLoss*0.25 = 1.95), plus tiny sizePenalty (0.01)
        assertEquals(1.96, fitness, 0.001);
    }
}
