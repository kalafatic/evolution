package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import eu.kalafatic.evolution.forge.model.inference.*;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.tokenizer.api.Tokenizer;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class EvoNativeInferenceEngineTest {

    private ReferenceEvoInferenceEngine engine;
    private Tokenizer tokenizer;

    @Before
    public void setUp() {
        engine = new ReferenceEvoInferenceEngine();
        SimpleBPETokenizer bpe = new SimpleBPETokenizer();
        bpe.train("hello world evolution native inference engine test", 50);
        tokenizer = bpe;
    }

    @Test
    public void testModelValidationSuccess() {
        EvoLlmModel model = new EvoLlmModel(50, 16, 2, 1, 32, 8);
        engine.validateModel(model); // Should pass without exception
    }

    @Test(expected = EvoModelValidator.ValidationException.class)
    public void testModelValidationInvalidHeads() {
        EvoLlmModel model = new EvoLlmModel(50, 16, 2, 1, 32, 8);
        model.getBlocks().clear(); // Corrupt blocks list
        engine.validateModel(model);
    }

    @Test(expected = EvoModelValidator.ValidationException.class)
    public void testModelValidationNullModel() {
        engine.validateModel(null);
    }

    @Test
    public void testDeterministicForwardPass() {
        EvoLlmModel model = new EvoLlmModel(20, 16, 2, 1, 32, 8);
        int[] inputIds = new int[]{1, 5, 8};

        Tensor logits1 = engine.forward(model, inputIds);
        Tensor logits2 = engine.forward(model, inputIds);

        assertNotNull(logits1);
        assertNotNull(logits2);
        assertArrayEquals(logits1.getShape(), logits2.getShape());

        float[] d1 = logits1.getData();
        float[] d2 = logits2.getData();

        assertEquals(d1.length, d2.length);
        for (int i = 0; i < d1.length; i++) {
            assertEquals("Forward pass logits must be exactly deterministic", d1[i], d2[i], 1e-6f);
        }
    }

    @Test
    public void testTrainingVsInferenceForwardPassConsistency() {
        EvoLlmModel model = new EvoLlmModel(30, 16, 2, 2, 32, 8);
        int[] inputIds = new int[]{2, 4, 6};

        Tensor trainingLogits = model.forward(inputIds);
        Tensor inferenceLogits = engine.forward(model, inputIds);

        assertNotNull(trainingLogits);
        assertNotNull(inferenceLogits);
        assertArrayEquals(trainingLogits.getShape(), inferenceLogits.getShape());

        float[] tData = trainingLogits.getData();
        float[] iData = inferenceLogits.getData();

        for (int i = 0; i < tData.length; i++) {
            assertEquals("Training and Inference forward passes must produce identical logits",
                    tData[i], iData[i], 1e-6f);
        }
    }

    @Test
    public void testDeterministicGeneration() {
        EvoLlmModel model = new EvoLlmModel(50, 16, 2, 1, 32, 8);
        InferenceRequest req = InferenceRequest.builder()
                .inputIds(new int[]{1, 2, 3})
                .maxTokens(5)
                .temperature(0.0f) // Deterministic greedy decoding
                .build();

        InferenceResult res1 = engine.generate(model, req, tokenizer);
        InferenceResult res2 = engine.generate(model, req, tokenizer);

        assertNotNull(res1);
        assertNotNull(res2);
        assertArrayEquals("Deterministic generation runs must produce identical token sequences",
                res1.getGeneratedTokenIds(), res2.getGeneratedTokenIds());
        assertEquals(res1.getGeneratedText(), res2.getGeneratedText());
    }

    @Test
    public void testEOSTerminationAndMaxTokens() {
        EvoLlmModel model = new EvoLlmModel(20, 8, 2, 1, 16, 4);

        // Request with max 3 tokens and explicit stop token 5
        InferenceRequest req = InferenceRequest.builder()
                .inputIds(new int[]{1})
                .maxTokens(3)
                .addStopTokenId(5)
                .temperature(0.0f)
                .build();

        InferenceResult res = engine.generate(model, req, tokenizer);
        assertNotNull(res);
        assertTrue("Generated tokens must not exceed maxTokens limit", res.getGeneratedTokenCount() <= 3);
        assertNotNull(res.getTerminationReason());
    }

    @Test
    public void testDarwinCandidateIsolation() {
        // Create candidate A and candidate B with distinct architecture & weight configurations
        EvoLlmModel candidateA = new EvoLlmModel(20, 16, 2, 1, 32, 8);
        EvoLlmModel candidateB = new EvoLlmModel(20, 16, 2, 1, 32, 8);

        // Modify weights of candidate A
        float[] aEmbed = candidateA.parameters().get(0).getData();
        for (int i = 0; i < aEmbed.length; i++) {
            aEmbed[i] = 1.0f;
        }

        // Modify weights of candidate B
        float[] bEmbed = candidateB.parameters().get(0).getData();
        for (int i = 0; i < bEmbed.length; i++) {
            bEmbed[i] = -1.0f;
        }

        int[] inputIds = new int[]{1, 2, 3};

        Tensor logitsA1 = engine.forward(candidateA, inputIds);
        Tensor logitsB1 = engine.forward(candidateB, inputIds);

        // Verify logits are different between distinct candidates
        assertFalse(Arrays.equals(logitsA1.getData(), logitsB1.getData()));

        // Mutate candidate A again
        for (int i = 0; i < aEmbed.length; i++) {
            aEmbed[i] = 2.0f;
        }

        Tensor logitsA2 = engine.forward(candidateA, inputIds);
        Tensor logitsB2 = engine.forward(candidateB, inputIds);

        // Candidate B must remain completely isolated and unchanged by mutations to candidate A
        float[] b1Data = logitsB1.getData();
        float[] b2Data = logitsB2.getData();

        for (int i = 0; i < b1Data.length; i++) {
            assertEquals("Candidate B state must not be altered when evaluating candidate A",
                    b1Data[i], b2Data[i], 1e-6f);
        }
    }
}
