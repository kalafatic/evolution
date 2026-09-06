package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.inference.InferenceRequest;
import eu.kalafatic.evolution.forge.model.inference.InferenceResult;
import eu.kalafatic.evolution.forge.model.inference.ReferenceEvoInferenceEngine;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class EvoNativeEndToEndSmokeTest {

    @Test
    public void tinyModelCanTrainAndRunNativeInference() {
        // 1. Create tiny EVO model
        // vocabSize = 16, dModel = 16, numHeads = 1, numBlocks = 1, dff = 32, maxSeqLen = 8
        EvoLlmModel model = new EvoLlmModel(16, 16, 1, 1, 32, 8);

        // 2. Verify model initialization
        assertNotNull(model);
        List<Tensor> initialParams = model.parameters();
        assertFalse(initialParams.isEmpty());

        // Snapshot initial parameter values
        List<float[]> initialWeights = new ArrayList<>();
        for (Tensor p : initialParams) {
            float[] data = p.getData().clone();
            initialWeights.add(data);
            for (float v : data) {
                assertTrue("Parameter values must be finite", Float.isFinite(v));
            }
        }

        // 3. Create tiny dataset
        List<int[]> samples = new ArrayList<>();
        samples.add(new int[]{1, 2, 3, 4});
        samples.add(new int[]{2, 3, 4, 5});
        samples.add(new int[]{3, 4, 5, 6});

        // 4. Initial loss evaluation & Training
        EvoLlmTrainer trainer = new EvoLlmTrainer(model, EvoLlmTrainer.TrainingProfile.EVO_FAST);
        trainer.setMicroBatchSize(1);
        trainer.setAccumulationSteps(1);

        // Run training for 20 epochs
        trainer.train(samples, 20);

        List<Double> lossHistory = trainer.getLossHistory();
        assertFalse("Loss history should not be empty", lossHistory.isEmpty());
        double initialLoss = lossHistory.get(0);
        double finalLoss = lossHistory.get(lossHistory.size() - 1);

        System.out.println("[SmokeTest] Initial loss: " + initialLoss + ", Final loss: " + finalLoss);

        assertTrue("Initial loss must be finite", Double.isFinite(initialLoss));
        assertTrue("Final loss must be finite", Double.isFinite(finalLoss));
        assertTrue("Loss must decrease during training (initial: " + initialLoss + ", final: " + finalLoss + ")", finalLoss < initialLoss);

        // 5. Verify parameters actually changed
        List<Tensor> finalParams = model.parameters();
        boolean paramChanged = false;
        for (int i = 0; i < finalParams.size(); i++) {
            float[] initData = initialWeights.get(i);
            float[] finalData = finalParams.get(i).getData();
            for (int j = 0; j < initData.length; j++) {
                if (Math.abs(initData[j] - finalData[j]) > 1e-6f) {
                    paramChanged = true;
                    break;
                }
            }
            if (paramChanged) break;
        }
        assertTrue("Model parameters must actually change after training", paramChanged);

        // 6. Native inference
        ReferenceEvoInferenceEngine engine = new ReferenceEvoInferenceEngine();
        InferenceRequest request = InferenceRequest.builder()
                .inputIds(new int[]{1, 2, 3})
                .maxTokens(4)
                .temperature(0.0f) // Greedy decoding
                .build();

        InferenceResult result = engine.generate(model, request, null);
        assertNotNull(result);

        int[] generatedTokens = result.getGeneratedTokenIds();
        assertNotNull(generatedTokens);
        assertTrue("Generated tokens must not be empty", generatedTokens.length > 0);

        Tensor logits = result.getLastLogits();
        assertNotNull("Inference logits must not be null", logits);
        float[] logitsData = logits.getData();
        for (float val : logitsData) {
            assertTrue("All logits must be finite", Float.isFinite(val));
        }

        System.out.println("[SmokeTest] Input [1,2,3] -> Generated tokens: " + Arrays.toString(generatedTokens));
    }

    @Test
    public void testNumericalGradients() {
        EvoLlmModel model = new EvoLlmModel(8, 8, 1, 1, 16, 4);
        int[] inputIds = new int[]{1, 2, 3};
        int[] labels = new int[]{2, 3, 4};

        model.parameters().forEach(Tensor::zeroGrad);

        // Compute analytical gradients
        Tensor logits = model.forward(inputIds);
        float[] logitsData = logits.getData();
        int seqLen = inputIds.length;
        int vocabSize = model.getVocabSize();

        Tensor dLogits = new eu.kalafatic.evolution.forge.math.core.SimpleTensor(seqLen, vocabSize);
        float[] dLogitsData = dLogits.getData();

        for (int t = 0; t < seqLen; t++) {
            int offset = t * vocabSize;
            int target = labels[t];

            float maxLogit = Float.NEGATIVE_INFINITY;
            for (int v = 0; v < vocabSize; v++) maxLogit = Math.max(maxLogit, logitsData[offset + v]);
            float sumExp = 0.0f;
            for (int v = 0; v < vocabSize; v++) sumExp += (float) Math.exp(logitsData[offset + v] - maxLogit);
            float logSumExp = maxLogit + (float) Math.log(sumExp);

            for (int v = 0; v < vocabSize; v++) {
                float prob = (float) Math.exp(logitsData[offset + v] - logSumExp);
                float targetDist = (v == target) ? 1.0f : 0.0f;
                dLogitsData[offset + v] = prob - targetDist;
            }
        }

        model.backward(dLogits);

        // Perform finite difference check on representative parameter tensors
        float eps = 1e-4f;
        List<Tensor> params = model.parameters();

        for (Tensor p : params) {
            float[] data = p.getData();
            float[] grad = p.getGrad();

            int step = Math.max(1, data.length / 5);
            for (int i = 0; i < data.length; i += step) {
                float orig = data[i];

                // L(w + eps)
                data[i] = orig + eps;
                double lossPlus = computeLoss(model, inputIds, labels);

                // L(w - eps)
                data[i] = orig - eps;
                double lossMinus = computeLoss(model, inputIds, labels);

                // Restore orig
                data[i] = orig;

                float numGrad = (float) ((lossPlus - lossMinus) / (2.0 * eps));
                float anaGrad = grad[i];

                float diff = Math.abs(numGrad - anaGrad);
                float norm = Math.max(Math.abs(numGrad), Math.abs(anaGrad)) + 1e-4f;
                float relErr = diff / norm;

                assertTrue("Numerical and analytical gradients must match for param index " + i +
                        " (analytical: " + anaGrad + ", numerical: " + numGrad + ", diff: " + diff + ", relErr: " + relErr + ")",
                        diff < 0.01f || relErr < 0.5f);
            }
        }
    }

    @Test
    public void testForwardEquivalenceAndSnapshotRoundTrip() throws Exception {
        EvoLlmModel model = new EvoLlmModel(16, 16, 1, 1, 32, 8);
        int[] inputIds = new int[]{1, 3, 5};

        ReferenceEvoInferenceEngine engine = new ReferenceEvoInferenceEngine();

        // 1. Direct model forward
        Tensor modelLogits = model.forward(inputIds);
        assertNotNull(modelLogits);

        // 2. Engine forward on model
        Tensor engineLogits = engine.forward(model, inputIds);
        assertNotNull(engineLogits);

        // 3. Engine forward on snapshot
        Tensor snapshotLogits = engine.forwardSnapshot(model.createSnapshot(), inputIds);
        assertNotNull(snapshotLogits);

        // Assert numerical equivalence
        assertArrayEquals(modelLogits.getShape(), engineLogits.getShape());
        assertArrayEquals(modelLogits.getShape(), snapshotLogits.getShape());

        float[] mData = modelLogits.getData();
        float[] eData = engineLogits.getData();
        float[] sData = snapshotLogits.getData();

        for (int i = 0; i < mData.length; i++) {
            assertEquals("Engine forward on model must match model.forward exactly", mData[i], eData[i], 1e-6f);
            assertEquals("Engine forwardSnapshot must match model.forward exactly", mData[i], sData[i], 1e-6f);
        }

        // 4. Save and load round-trip
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("evo_smoke_saveload");
        try {
            model.save(tempDir);
            EvoLlmModel loadedModel = EvoLlmModel.load(tempDir);
            assertNotNull(loadedModel);

            Tensor loadedLogits = loadedModel.forward(inputIds);
            float[] lData = loadedLogits.getData();

            for (int i = 0; i < mData.length; i++) {
                assertEquals("Loaded model forward must match original model forward exactly", mData[i], lData[i], 1e-6f);
            }
        } finally {
            java.io.File[] files = tempDir.toFile().listFiles();
            if (files != null) {
                for (java.io.File f : files) f.delete();
            }
            java.nio.file.Files.deleteIfExists(tempDir);
        }
    }

    private double computeLoss(EvoLlmModel model, int[] inputIds, int[] labels) {
        Tensor logits = model.forward(inputIds);
        float[] logitsData = logits.getData();
        int seqLen = inputIds.length;
        int vocabSize = model.getVocabSize();
        double totalLoss = 0.0;

        for (int t = 0; t < seqLen; t++) {
            int offset = t * vocabSize;
            int target = labels[t];

            float maxLogit = Float.NEGATIVE_INFINITY;
            for (int v = 0; v < vocabSize; v++) maxLogit = Math.max(maxLogit, logitsData[offset + v]);
            float sumExp = 0.0f;
            for (int v = 0; v < vocabSize; v++) sumExp += (float) Math.exp(logitsData[offset + v] - maxLogit);
            float logSumExp = maxLogit + (float) Math.log(sumExp);

            totalLoss += (logSumExp - logitsData[offset + target]);
        }
        return totalLoss;
    }
}
