package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelSerializer;
import eu.kalafatic.evolution.forge.trainer.impl.EvoAdamW;
import eu.kalafatic.evolution.forge.agent.export.OllamaExporter;

public class EvoCoreLlmLifecycleTest {

    @Test
    public void testEvoModelCreation() {
        // vocabSize=50, dModel=16, numHeads=2, numBlocks=1, dff=32, maxSeqLen=8
        EvoLlmModel model = new EvoLlmModel(50, 16, 2, 1, 32, 8);
        assertNotNull(model);

        List<Tensor> params = model.parameters();
        assertNotNull(params);
        assertTrue(params.size() > 0);

        long totalParams = 0;
        for (Tensor p : params) {
            totalParams += p.getSize();
            assertNotNull(p.getGrad());
        }
        assertTrue(totalParams > 0);
        System.out.println("[Test] Created EvoLlmModel with parameter count: " + totalParams);
    }

    @Test
    public void testEvoTraining() {
        // Create model
        EvoLlmModel model = new EvoLlmModel(10, 8, 2, 1, 16, 4);
        EvoAdamW optimizer = new EvoAdamW(0.01f, 0.9f, 0.999f, 1e-8f, 0.01f);

        int[] inputTokens = {1, 2, 3};
        int targetToken = 4;

        // Perform some training epochs and verify loss decreases
        float initialLoss = 0.0f;
        float finalLoss = 0.0f;

        for (int epoch = 0; epoch < 10; epoch++) {
            model.parameters().forEach(Tensor::zeroGrad);

            // Forward
            Tensor logits = model.forward(inputTokens);

            // Calculate simulated cross entropy loss & gradient
            // For target token at index last step
            float[] logitsData = logits.getData();
            int seqLen = (int) logits.getShape()[0];
            int vocabSize = (int) logits.getShape()[1];
            int lastOffset = (seqLen - 1) * vocabSize;

            // Softmax
            float max = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < vocabSize; i++) {
                if (logitsData[lastOffset + i] > max) max = logitsData[lastOffset + i];
            }
            float sum = 0.0f;
            float[] probs = new float[vocabSize];
            for (int i = 0; i < vocabSize; i++) {
                probs[i] = (float) Math.exp(logitsData[lastOffset + i] - max);
                sum += probs[i];
            }
            for (int i = 0; i < vocabSize; i++) {
                probs[i] /= sum;
            }

            float loss = (float) -Math.log(Math.max(probs[targetToken], 1e-10f));
            if (epoch == 0) {
                initialLoss = loss;
            }
            finalLoss = loss;

            // dLogits gradient computation
            Tensor dLogits = new eu.kalafatic.evolution.forge.math.core.SimpleTensor(seqLen, vocabSize);
            float[] dLogitsData = dLogits.getData();
            probs[targetToken] -= 1.0f; // Cross-entropy gradient
            for (int i = 0; i < vocabSize; i++) {
                dLogitsData[lastOffset + i] = probs[i];
            }

            // Backward
            model.backward(dLogits);

            // Step optimizer
            optimizer.step(model.parameters());
        }

        System.out.println("[Test] Initial loss: " + initialLoss + ", Final loss: " + finalLoss);
        assertTrue(finalLoss < initialLoss);
    }

    @Test
    public void testEvoSerialization() throws Exception {
        EvoLlmModel model = new EvoLlmModel(20, 16, 2, 1, 32, 4);
        Path tempDir = Files.createTempDirectory("evo_serialization_test");

        try {
            // Save model
            EvoModelSerializer.save(model, tempDir, 0.5f, 5);

            // Load model
            EvoLlmModel loadedModel = EvoModelSerializer.load(tempDir);

            // Check architectural equivalence
            assertEquals(model.getVocabSize(), loadedModel.getVocabSize());
            assertEquals(model.getDModel(), loadedModel.getDModel());
            assertEquals(model.getNumBlocks(), loadedModel.getNumBlocks());

            // Compare weight values
            List<Tensor> originalParams = model.parameters();
            List<Tensor> loadedParams = loadedModel.parameters();
            assertEquals(originalParams.size(), loadedParams.size());

            for (int i = 0; i < originalParams.size(); i++) {
                float[] orig = originalParams.get(i).getData();
                float[] load = loadedParams.get(i).getData();
                assertArrayEquals(orig, load, 1e-6f);
            }
        } finally {
            // cleanup
            java.io.File[] files = tempDir.toFile().listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    f.delete();
                }
            }
            Files.delete(tempDir);
        }
    }

    @Test
    public void testEvoExport() throws Exception {
        EvoLlmModel model = new EvoLlmModel(20, 16, 2, 1, 32, 4);
        Path tempDir = Files.createTempDirectory("evo_export_test");
        try {
            OllamaExporter exporter = new OllamaExporter();
            exporter.export("evo-test-unit", tempDir, model);

            Path gguf = tempDir.resolve("evo.gguf");
            Path mf = tempDir.resolve("Modelfile");

            assertTrue(Files.exists(gguf));
            assertTrue(Files.exists(mf));

            // Verify GGUF magic header
            byte[] bytes = Files.readAllBytes(gguf);
            assertEquals((byte) 'G', bytes[0]);
            assertEquals((byte) 'G', bytes[1]);
            assertEquals((byte) 'U', bytes[2]);
            assertEquals((byte) 'F', bytes[3]);
        } finally {
            // cleanup
            java.io.File[] files = tempDir.toFile().listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    f.delete();
                }
            }
            Files.delete(tempDir);
        }
    }
}
