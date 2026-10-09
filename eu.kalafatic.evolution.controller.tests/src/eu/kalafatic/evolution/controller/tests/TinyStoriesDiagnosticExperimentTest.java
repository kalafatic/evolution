package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.inference.InferenceRequest;
import eu.kalafatic.evolution.forge.model.inference.InferenceResult;
import eu.kalafatic.evolution.forge.model.inference.ReferenceEvoInferenceEngine;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.trainer.api.TrainingResult;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TinyStoriesDiagnosticExperimentTest {

    @Test
    public void runTinyStoriesDiagnosticFromScratchExperiment() throws Exception {
        System.out.println("=== Starting TinyStories From-Scratch Diagnostic Experiment ===");

        // 1. Initialize new model from scratch with random weights
        // vocabSize = 64, dModel = 32, numHeads = 2, numBlocks = 2, dff = 64, maxSeqLen = 32
        EvoLlmModel model = new EvoLlmModel(64, 32, 2, 2, 64, 32);
        assertNotNull(model);

        long paramCount = model.parameters().stream().mapToLong(Tensor::getSize).sum();
        assertTrue("Parameter count should be > 0", paramCount > 0);
        System.out.printf("[Diagnostic] Model parameter count: %d%n", paramCount);

        // 2. Prepare reproducible TinyStories subset tokenized samples
        // Sample sentences: "once upon a time", "lily loved to play", "a big happy dog", "the sun was bright"
        List<int[]> dataset = new ArrayList<>();
        int[][] storyTokens = new int[][] {
            {2, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 3},
            {2, 14, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 3},
            {2, 12, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 3},
            {2, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 3},
            {2, 10, 11, 12, 13, 24, 25, 26, 47, 48, 49, 50, 3},
            {2, 14, 15, 16, 17, 31, 32, 33, 41, 42, 43, 44, 3}
        };

        for (int[] s : storyTokens) {
            dataset.add(s);
        }

        // 3. Inference Before Training
        ReferenceEvoInferenceEngine engine = new ReferenceEvoInferenceEngine();
        InferenceRequest req = InferenceRequest.builder()
                .inputIds(new int[]{2, 10, 11}) // "once upon a..."
                .maxTokens(6)
                .temperature(0.0f)
                .build();

        InferenceResult initResult = engine.generate(model, req, null);
        assertNotNull(initResult);
        int[] preTokens = initResult.getGeneratedTokenIds();
        System.out.println("[Diagnostic] Pre-training generated tokens: " + Arrays.toString(preTokens));

        // 4. Configure Trainer and Progress Telemetry Listener
        EvoLlmTrainer trainer = new EvoLlmTrainer(model, EvoLlmTrainer.TrainingProfile.EVO_FAST);
        trainer.setMicroBatchSize(2);
        trainer.setAccumulationSteps(1);
        trainer.setBaseSeed(12345L);

        List<Double> observedLosses = new ArrayList<>();
        List<Double> observedTokPerSec = new ArrayList<>();

        trainer.setProgressListener((epoch, totalEpochs, sampleIndex, totalSamplesCount, currentLoss) -> {
            observedLosses.add(currentLoss);
        });

        // 5. Run Training for 25 Epochs
        int plannedEpochs = 25;
        long startTimeMs = System.currentTimeMillis();
        TrainingResult trainResult = trainer.train(dataset, plannedEpochs);
        long durationMs = System.currentTimeMillis() - startTimeMs;

        assertNotNull(trainResult);
        assertTrue("Training must succeed normally", trainResult.isCompletedNormally());
        assertEquals("Completed epochs must match planned", plannedEpochs, trainResult.getCompletedEpochs());

        // Verify metadata tags
        assertEquals("FROM_SCRATCH", trainResult.getMetadata().get("trainingMode"));
        assertEquals(false, trainResult.getMetadata().get("pretrainedWeights"));
        assertNotNull(trainResult.getMetadata().get("tokensPerSecond"));

        double initialLoss = trainer.getLossHistory().get(0);
        double finalLoss = trainResult.getLastLoss();
        double valLoss = trainResult.getValLoss();

        System.out.printf("[Diagnostic] Training completed in %d ms%n", durationMs);
        System.out.printf("[Diagnostic] Initial Loss: %.4f | Final Train Loss: %.4f | Val Loss: %.4f%n",
                initialLoss, finalLoss, valLoss);

        assertTrue("Final loss must be lower than initial loss", finalLoss < initialLoss);

        // 6. Save Checkpoint & Reload Roundtrip
        Path tempDir = Files.createTempDirectory("tinystories_checkpoint");
        try {
            model.save(tempDir);

            EvoLlmModel reloadedModel = EvoLlmModel.load(tempDir);
            assertNotNull("Reloaded model must not be null", reloadedModel);

            // 7. Inference After Training on Reloaded Model
            InferenceResult postResult = engine.generate(reloadedModel, req, null);
            assertNotNull(postResult);
            int[] postTokens = postResult.getGeneratedTokenIds();
            System.out.println("[Diagnostic] Post-training generated tokens: " + Arrays.toString(postTokens));

            assertNotNull("Post tokens must not be null", postTokens);
            assertTrue("Post tokens must not be empty", postTokens.length > 0);

        } finally {
            File[] files = tempDir.toFile().listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            Files.deleteIfExists(tempDir);
        }

        System.out.println("=== TinyStories Diagnostic Experiment Completed Successfully ===");
    }
}
