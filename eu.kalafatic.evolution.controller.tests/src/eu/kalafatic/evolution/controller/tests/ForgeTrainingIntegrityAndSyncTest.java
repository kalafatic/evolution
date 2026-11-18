package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager.TrainingTelemetry;
import eu.kalafatic.evolution.controller.orchestration.selfdev.LLMDarwinEngine.Profile;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob;
import eu.kalafatic.evolution.forge.data.api.TrainingSample;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.trainer.api.TerminationReason;
import eu.kalafatic.evolution.forge.trainer.api.TrainingCheckpoint;
import eu.kalafatic.evolution.forge.trainer.api.TrainingResult;
import eu.kalafatic.evolution.forge.trainer.api.TrainingTerminationStatus;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.ForgeStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Training-engine integrity and session/telemetry synchronization tests:
 * profile configuration, mid-epoch termination accounting, early stopping,
 * time budget, checkpoint validation, real parameter optimization, telemetry
 * thread safety and persisted session state recovery.
 */
public class ForgeTrainingIntegrityAndSyncTest {

    // ---------------------------------------------------------------- profiles

    @Test
    public void testProfileDefaults() {
        assertEquals(4, Profile.SMALL.epochs);
        assertEquals(4, Profile.DEFAULT.epochs);
        assertEquals(10, Profile.LARGE.epochs);
    }

    @Test
    public void testForgeJobDefaultConfig() {
        ForgeJob job = new ForgeJob("test-job");
        assertEquals(4, job.getTrainingConfig().getEpochs());
        assertEquals(5000, job.getTrainingConfig().getMaxSamples());
        assertEquals(100_000, job.getTrainingConfig().getMaxTokens());
        assertEquals(5, job.getTrainingConfig().getCheckpointFrequencyEpochs());
    }

    // ------------------------------------------------ training termination logic

    private EvoLlmTrainer newTinyTrainer() {
        EvoLlmModel model = new EvoLlmModel(50, 32, 2, 1, 64, 16);
        EvoLlmTrainer trainer = new EvoLlmTrainer(model, EvoLlmTrainer.TrainingProfile.EVO_FAST);
        trainer.setMicroBatchSize(2);
        trainer.setAccumulationSteps(1);
        trainer.setLearningRate(0.01f);
        return trainer;
    }

    private List<TrainingSample> buildPatternSamples(int count, int seqLen) {
        List<TrainingSample> samples = new ArrayList<>();
        int[] pattern = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 };
        for (int s = 0; s < count; s++) {
            int len = seqLen;
            int[] inputIds = new int[len];
            int[] labels = new int[len];
            boolean[] lossMask = new boolean[len];
            float[] attMask = new float[len];
            for (int i = 0; i < len; i++) {
                inputIds[i] = pattern[(s + i) % pattern.length];
                labels[i] = pattern[(s + i + 1) % pattern.length];
                lossMask[i] = true;
                attMask[i] = 1.0f;
            }
            samples.add(new TrainingSample(inputIds, labels, lossMask, attMask));
        }
        return samples;
    }

    @Test
    public void testMidEpochPauseDoesNotOverstateCompletedEpochs() {
        EvoLlmTrainer trainer = newTinyTrainer();
        List<TrainingSample> samples = buildPatternSamples(6, 8);

        // Pause on the first progress callback: it fires after the first
        // optimizer step of epoch 1, so the pause takes effect mid-epoch.
        trainer.addProgressListener(new EvoLlmTrainer.ProgressListener() {
            @Override
            public void onProgress(int epoch, int totalEpochs, int sampleIndex, int totalSamplesCount, double currentLoss) {}

            @Override
            public void onProgressMetrics(int epoch, int totalEpochs, int currentStep, int totalSteps,
                                          long processedTokens, long totalTokens,
                                          double tokensPerSec, double stepsPerSec,
                                          double estimatedRemainingSeconds, double currentLoss) {
                trainer.requestPause();
            }
        });

        TrainingResult result = trainer.train(samples, 4);

        assertEquals(TrainingTerminationStatus.TRAINING_PAUSED, result.getTerminationStatus());
        assertEquals(TerminationReason.USER_PAUSE_REQUEST, result.getTerminationReason());
        // The interrupted epoch must NOT be counted as completed.
        assertEquals(0, result.getCompletedEpochs());
        assertEquals(4, result.getPlannedEpochs());
    }

    @Test
    public void testMidEpochStopDoesNotOverstateCompletedEpochs() {
        EvoLlmTrainer trainer = newTinyTrainer();
        List<TrainingSample> samples = buildPatternSamples(6, 8);

        trainer.addProgressListener(new EvoLlmTrainer.ProgressListener() {
            @Override
            public void onProgress(int epoch, int totalEpochs, int sampleIndex, int totalSamplesCount, double currentLoss) {}

            @Override
            public void onProgressMetrics(int epoch, int totalEpochs, int currentStep, int totalSteps,
                                          long processedTokens, long totalTokens,
                                          double tokensPerSec, double stepsPerSec,
                                          double estimatedRemainingSeconds, double currentLoss) {
                trainer.requestStop();
            }
        });

        TrainingResult result = trainer.train(samples, 4);

        assertEquals(TrainingTerminationStatus.TRAINING_STOPPED, result.getTerminationStatus());
        assertEquals(TerminationReason.USER_STOP_REQUEST, result.getTerminationReason());
        assertEquals(0, result.getCompletedEpochs());
    }

    @Test
    public void testEarlyStoppingCriteriaStopsAfterCompletedEpoch() {
        EvoLlmTrainer trainer = newTinyTrainer();
        trainer.setEarlyStoppingEnabled(true);
        trainer.setTargetLossThreshold(1_000_000.0f); // trivially reachable after epoch 1

        TrainingResult result = trainer.train(buildPatternSamples(6, 8), 4);

        assertEquals(TrainingTerminationStatus.TRAINING_STOPPED, result.getTerminationStatus());
        assertEquals(TerminationReason.EARLY_STOPPING_CRITERIA, result.getTerminationReason());
        assertTrue(result.isStoppedEarly());
        assertEquals(1, result.getCompletedEpochs());
        assertTrue(result.getLastLoss() > 0.0);
    }

    @Test
    public void testEarlyStoppingDisabledRunsAllEpochs() {
        EvoLlmTrainer trainer = newTinyTrainer();
        trainer.setEarlyStoppingEnabled(false);
        trainer.setTargetLossThreshold(1_000_000.0f);

        TrainingResult result = trainer.train(buildPatternSamples(4, 8), 3);

        assertEquals(TrainingTerminationStatus.TRAINING_COMPLETED, result.getTerminationStatus());
        assertEquals(3, result.getCompletedEpochs());
    }

    @Test
    public void testTimeBudgetStopsTraining() {
        EvoLlmTrainer trainer = newTinyTrainer();
        trainer.setTimeBudgetHours(0.0f);

        TrainingResult result = trainer.train(buildPatternSamples(4, 8), 4);

        assertEquals(TrainingTerminationStatus.TRAINING_STOPPED, result.getTerminationStatus());
        assertEquals(TerminationReason.TIME_BUDGET_REACHED, result.getTerminationReason());
        assertEquals(1, result.getCompletedEpochs());
    }

    // ----------------------------------------------------- real optimization

    @Test
    public void testTrainingActuallyOptimizesParameters() {
        EvoLlmModel model = new EvoLlmModel(50, 32, 2, 1, 64, 16);
        EvoLlmTrainer trainer = new EvoLlmTrainer(model, EvoLlmTrainer.TrainingProfile.EVO_FAST);
        trainer.setMicroBatchSize(2);
        trainer.setAccumulationSteps(1);
        trainer.setLearningRate(0.01f);

        List<float[]> before = new ArrayList<>();
        for (Tensor p : model.parameters()) {
            before.add(p.getData().clone());
        }

        TrainingResult result = trainer.train(buildPatternSamples(10, 8), 3);

        assertTrue(result.isCompletedNormally());
        assertEquals(3, result.getCompletedEpochs());

        List<Double> history = trainer.getLossHistory();
        assertTrue("Loss history must contain one entry per epoch", history.size() >= 3);
        double firstLoss = history.get(0);
        double lastLoss = result.getLastLoss();
        assertTrue("Training loss must decrease (first=" + firstLoss + ", last=" + lastLoss + ")",
                lastLoss < firstLoss);

        boolean changed = false;
        List<Tensor> afterParams = model.parameters();
        for (int i = 0; i < afterParams.size(); i++) {
            float[] b = before.get(i);
            float[] a = afterParams.get(i).getData();
            for (int j = 0; j < a.length; j++) {
                if (a[j] != b[j]) {
                    changed = true;
                    break;
                }
            }
            if (changed) break;
        }
        assertTrue("Trainable parameters must change during training", changed);
    }

    // ------------------------------------------------------- checkpoint checks

    @Test
    public void testCheckpointRoundTripPreservesFingerprintAndValidation() {
        EvoLlmModel model = new EvoLlmModel(50, 32, 2, 1, 64, 16);
        EvoLlmTrainer trainer = new EvoLlmTrainer(model, EvoLlmTrainer.TrainingProfile.EVO_FAST);

        Map<String, Integer> vocab = new LinkedHashMap<>();
        vocab.put("<s>", 0);
        vocab.put("hello", 1);

        TrainingCheckpoint chk = trainer.captureCheckpoint("run-1", "model-1", 2, 4.5f, 4.2f,
                "sha256:00112233445566778899aabbccddeeff", vocab);
        assertEquals("sha256:00112233445566778899aabbccddeeff", chk.getDatasetFingerprint());
        assertEquals(2, chk.getCompletedEpochs());
        assertEquals(16, chk.getMaxSeqLen());

        Path tmp = null;
        try {
            tmp = Files.createTempDirectory("forge-chk-test");
            Path chkFile = tmp.resolve("checkpoint.chk");
            chk.saveAtomic(chkFile);

            TrainingCheckpoint loaded = TrainingCheckpoint.load(chkFile);
            assertEquals(chk.getDatasetFingerprint(), loaded.getDatasetFingerprint());
            assertEquals(2, loaded.getCompletedEpochs());
            assertEquals(16, loaded.getMaxSeqLen());

            // Architecture validation must reject a maxSeqLen mismatch.
            EvoLlmModel other = new EvoLlmModel(50, 32, 2, 1, 64, 32);
            try {
                loaded.validateCompatibility(other);
                fail("Expected IllegalStateException for maxSeqLen mismatch");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("maxSeqLen"));
            }
        } catch (Exception e) {
            fail("Checkpoint round-trip failed: " + e.getMessage());
        }
    }

    // --------------------------------------------------- listener management

    @Test
    public void testProgressListenerRemoval() {
        EvoLlmTrainer trainer = newTinyTrainer();
        EvoLlmTrainer.ProgressListener listener = new EvoLlmTrainer.ProgressListener() {
            @Override
            public void onProgress(int epoch, int totalEpochs, int sampleIndex, int totalSamplesCount, double currentLoss) {}
        };
        trainer.addProgressListener(listener);
        assertEquals(1, trainer.getProgressListenerCount());
        trainer.removeProgressListener(listener);
        assertEquals(0, trainer.getProgressListenerCount());
    }

    // -------------------------------------------------------- telemetry safety

    @Test
    public void testTelemetryConcurrentUpdatesProduceConsistentSnapshots() throws Exception {
        TrainingTelemetry telemetry = new TrainingTelemetry("sync-session");
        int threads = 4;
        int iterations = 5_000;
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            final int tid = t;
            workers[t] = new Thread(() -> {
                for (int i = 0; i < iterations; i++) {
                    telemetry.updateMetrics(i % 4, 4, i, 20_000,
                            i * 8L, 160_000L,
                            i, i, i, i / 10.0,
                            i / 20.0, i);
                    telemetry.addLog("INFO", "t" + tid + "-" + i);
                    if (i % 100 == 0) {
                        telemetry.setStatus("RUNNING", "update-" + tid);
                    }
                }
            });
            workers[t].start();
        }
        for (Thread w : workers) w.join();

        telemetry.setStatus("COMPLETED", "done");
        JSONObject json = telemetry.toJsonObject();
        assertEquals("COMPLETED", json.getString("status"));
        assertTrue(json.getInt("currentStep") >= 0);
        assertTrue(json.getInt("totalSteps") > 0);
        assertTrue(json.has("progressPercent"));
    }

    // ----------------------------------------- session state persistence/sync

    @Test
    public void testSessionStatePersistenceAndRecovery() throws Exception {
        ForgeSessionManager manager = ForgeSessionManager.getInstance();
        Path stateDir = Files.createTempDirectory("forge-state-test");
        ForgeSessionManager.setForgeStateDirectory(stateDir);
        try {
            Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
            orchestrator.setId("test-orch-sync-persist");
            manager.initialize(orchestrator);

            ForgeSession session = manager.createSession("SyncPersist", "SELF_EVO");
            assertNotNull(session);

            // Terminal update persists state with the real status.
            manager.updateSessionTerminalStatus(session.getSessionId(), "FAILED");
            Path stateFile = stateDir.resolve(session.getSessionId() + ".json");
            assertTrue("Session state file must exist", Files.exists(stateFile));
            JSONObject state = new JSONObject(Files.readString(stateFile));
            assertEquals("FAILED", state.getString("status"));

            TrainingTelemetry telemetry = manager.getTelemetry(session.getSessionId());
            assertEquals("FAILED", telemetry.status);
            assertEquals(ForgeStatus.ERROR, session.getStatus());

            // Recovery: a RUNNING persisted state becomes INTERRUPTED after restart.
            ForgeSession s2 = manager.createSession("RecoverySession", "SELF_EVO");
            JSONObject running = new JSONObject();
            running.put("sessionId", s2.getSessionId());
            running.put("status", "RUNNING");
            running.put("currentEpoch", 2);
            running.put("totalEpochs", 4);
            running.put("currentStep", 10);
            running.put("totalSteps", 40);
            running.put("currentLoss", 3.5);
            Files.writeString(stateDir.resolve(s2.getSessionId() + ".json"), running.toString());

            java.lang.reflect.Field recoveredFlag = ForgeSessionManager.class.getDeclaredField("recoveredFromRestart");
            recoveredFlag.setAccessible(true);
            recoveredFlag.set(manager, false);
            manager.recoverFromRestart();

            TrainingTelemetry recovered = manager.getTelemetry(s2.getSessionId());
            assertEquals("INTERRUPTED", recovered.status);
            assertEquals(2, recovered.currentEpoch);
            assertEquals(4, recovered.totalEpochs);
            assertEquals(ForgeStatus.IDLE, s2.getStatus());
        } finally {
            ForgeSessionManager.setForgeStateDirectory(null);
        }
    }

    @Test
    public void testCompleteActiveTrainerRemovesListenerAndSetsTerminalStatus() {
        ForgeSessionManager manager = ForgeSessionManager.getInstance();
        try {
            Path stateDir = Files.createTempDirectory("forge-state-test2");
            ForgeSessionManager.setForgeStateDirectory(stateDir);
            try {
                Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
                orchestrator.setId("test-orch-sync-listener");
                manager.initialize(orchestrator);

                ForgeSession session = manager.createSession("ListenerSync", "SELF_EVO");
                EvoLlmTrainer trainer = newTinyTrainer();

                manager.registerActiveTrainer(session.getSessionId(), trainer);
                assertEquals(1, trainer.getProgressListenerCount());
                assertEquals("RUNNING", manager.getTelemetry(session.getSessionId()).status);
                assertEquals(ForgeStatus.TRAINING, session.getStatus());

                manager.completeActiveTrainer(session.getSessionId(), "FAILED");
                assertEquals(0, trainer.getProgressListenerCount());
                assertEquals("FAILED", manager.getTelemetry(session.getSessionId()).status);
                assertEquals(ForgeStatus.ERROR, session.getStatus());
            } finally {
                ForgeSessionManager.setForgeStateDirectory(null);
            }
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testLegacyUnregisterActiveTrainerMapsToCompleted() {
        ForgeSessionManager manager = ForgeSessionManager.getInstance();
        try {
            Path stateDir = Files.createTempDirectory("forge-state-test3");
            ForgeSessionManager.setForgeStateDirectory(stateDir);
            try {
                Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
                orchestrator.setId("test-orch-sync-legacy");
                manager.initialize(orchestrator);

                ForgeSession session = manager.createSession("LegacySync", "SELF_EVO");
                EvoLlmTrainer trainer = newTinyTrainer();

                manager.registerActiveTrainer(session.getSessionId(), trainer);
                manager.unregisterActiveTrainer(session.getSessionId());

                assertEquals("COMPLETED", manager.getTelemetry(session.getSessionId()).status);
                assertEquals(ForgeStatus.IDLE, session.getStatus());
                assertEquals(0, trainer.getProgressListenerCount());
            } finally {
                ForgeSessionManager.setForgeStateDirectory(null);
            }
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
}
