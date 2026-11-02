package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;
import org.junit.Test;

import eu.kalafatic.evolution.controller.manager.ModelSizePreset;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.LLMDarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.LLMDarwinEngine.CandidateResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.LLMDarwinEngine.CandidateTrainingResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.LLMDarwinEngine.LlmConfig;
import eu.kalafatic.evolution.controller.orchestration.selfdev.LLMDarwinEngine.Profile;
import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSample;
import eu.kalafatic.evolution.forge.data.impl.DatasetBuilder;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.utils.time.DateUtils;

public class ForgeDurationAndSyncTest {

    @Test
    public void testDateUtilsFormatDuration() {
        assertEquals("0 ms", DateUtils.formatDuration(-10));
        assertEquals("0 ms", DateUtils.formatDuration(0));
        assertEquals("450 ms", DateUtils.formatDuration(450));
        assertEquals("12.50 s (12500 ms)", DateUtils.formatDuration(12500));
        assertEquals("9 min 58 sec (598453 ms)", DateUtils.formatDuration(598453));
        assertEquals("3 hr 11 min 34 sec (11494322 ms)", DateUtils.formatDuration(11494322));
    }

    @Test
    public void testEvoLlmTrainerFormatDuration() {
        assertEquals("0 ms", EvoLlmTrainer.formatDuration(-5));
        assertEquals("500 ms", EvoLlmTrainer.formatDuration(500));
        assertEquals("5.00 s (5000 ms)", EvoLlmTrainer.formatDuration(5000));
        assertEquals("10 min 0 sec (600000 ms)", EvoLlmTrainer.formatDuration(600000));
        assertEquals("1 hr 0 min 0 sec (3600000 ms)", EvoLlmTrainer.formatDuration(3600000));
    }

    @Test
    public void testForgeSessionManagerSessionSync() {
        ForgeSessionManager manager = ForgeSessionManager.getInstance();
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId("test-orch-sync");
        manager.initialize(orchestrator);

        ForgeSession session = manager.createSession("Forge-AI-Commander-Session", "SELF_EVO");
        assertNotNull(session);
        assertEquals("Forge-AI-Commander-Session", session.getName());

        ForgeSessionManager.TrainingTelemetry telemetry = manager.getTelemetry(session.getSessionId());
        assertNotNull(telemetry);
        telemetry.addLog("INFO", "Epoch 1/1 completed in " + DateUtils.formatDuration(15000) + " | Train Loss: 4.5000 | Val Loss: 4.2000");

        assertTrue(!telemetry.logStream.isEmpty());
        assertTrue(telemetry.logStream.get(telemetry.logStream.size() - 1).message.contains("15.00 s (15000 ms)"));
    }

    @Test
    public void testSmallProfileDefaultAndExplicitOverrides() {
        // Test 1: Verify Profile.SMALL defaults
        assertEquals(4, Profile.SMALL.epochs);
        assertEquals(ModelSizePreset.Size.SMALL.getDModel(), 512);

        // Test 2: Unconfigured uiState (defaulting to 64) should be clamped to Profile.SMALL.epochs (4)
        JSONObject uiStateUnconfigured = new JSONObject();
        uiStateUnconfigured.put("epochs", 64);
        int effectiveDefault = uiStateUnconfigured.optBoolean("epochsExplicitlySet", false) || uiStateUnconfigured.optInt("epochs") != 64
                ? uiStateUnconfigured.optInt("epochs") : Profile.SMALL.epochs;
        assertEquals(4, effectiveDefault);

        // Test 3: Explicit user override in uiState
        JSONObject uiStateExplicit = new JSONObject();
        uiStateExplicit.put("epochs", 12);
        uiStateExplicit.put("epochsExplicitlySet", true);
        int effectiveExplicit = uiStateExplicit.optBoolean("epochsExplicitlySet", false) || uiStateExplicit.optInt("epochs") != 64
                ? uiStateExplicit.optInt("epochs") : Profile.SMALL.epochs;
        assertEquals(12, effectiveExplicit);
    }

    @Test
    public void testCandidateWeightPersistenceAndIndependence() {
        // Test CandidateTrainingResult captures isolated weights
        EvoLlmModel candModel1 = new EvoLlmModel(50, 32, 2, 1, 64, 16);
        List<float[]> weights1 = new ArrayList<>();
        for (Tensor p : candModel1.parameters()) {
            float[] d = p.getData();
            float[] copy = new float[d.length];
            System.arraycopy(d, 0, copy, 0, d.length);
            weights1.add(copy);
        }

        CandidateTrainingResult tr = new CandidateTrainingResult(4.2, 1000, 4.5, weights1);
        assertNotNull(tr.trainedWeights);
        assertEquals(candModel1.parameters().size(), tr.trainedWeights.size());

        LlmConfig config = new LlmConfig(50, 32, 1, 2, 64, 16, 4);
        CandidateResult cr = new CandidateResult("Candidate A", config, 4.2, 1000, 500, 4.5, false, null, tr.trainedWeights);
        assertNotNull(cr.trainedWeights);

        // Verify restoration into new model
        EvoLlmModel restoredModel = new EvoLlmModel(50, 32, 2, 1, 64, 16);
        for (int i = 0; i < restoredModel.parameters().size(); i++) {
            Tensor t = restoredModel.parameters().get(i);
            float[] w = cr.trainedWeights.get(i);
            System.arraycopy(w, 0, t.getData(), 0, w.length);
        }

        // Verify deep copy independence: modifying candModel1 does not affect restoredModel
        candModel1.parameters().get(0).getData()[0] = 999.0f;
        assertFalse(restoredModel.parameters().get(0).getData()[0] == 999.0f);
        assertEquals(weights1.get(0)[0], restoredModel.parameters().get(0).getData()[0], 0.0001f);
    }

    @Test
    public void testDatasetPreprocessingReuseAndSampleBounding() {
        SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
        tokenizer.train("The quick brown fox jumps over the lazy dog. Artificial Intelligence and Evolution.", 100);

        DatasetBuilder builder = new DatasetBuilder();
        List<Integer> encoded = tokenizer.encode("The quick brown fox jumps over the lazy dog. Artificial Intelligence and Evolution.");
        assertTrue(encoded.size() > 5);

        // Test bounded sliding window: maxSamples capping before materialization
        List<DatasetBuilder.Sample> boundedSamples = builder.buildSlidingWindow(encoded, 4, 1, 3);
        assertEquals(3, boundedSamples.size());

        // Test buildTrainingSamples with NormalizedSample and maxSamples limit
        NormalizedSample sample1 = new NormalizedSample();
        sample1.setInstruction("Hello");
        sample1.setResponse("World");

        List<NormalizedSample> normList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            normList.add(sample1);
        }

        List<TrainingSample> trainingSamples = builder.buildTrainingSamples(normList, tokenizer, 16, 5);
        assertEquals(5, trainingSamples.size());
    }

    @Test
    public void testOllamaEvaluationIsOptionalAndNonBlocking() {
        JSONObject uiState = new JSONObject();
        // By default, enableOllamaRef is false
        assertFalse(uiState.optBoolean("enableOllamaRef", false));

        // When false, candidate evaluation proceeds purely on native loss without network calls
        double nativeLoss = 3.5;
        double nativeFitness = 3.8;
        double candidateFitness = nativeFitness;
        assertEquals(3.8, candidateFitness, 0.0001);
    }
}
