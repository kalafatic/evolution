package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager.MetricSample;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager.TrainingTelemetry;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.ForgeStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

import org.json.JSONObject;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies telemetry improvements: learning-rate publication, MetricSample
 * round-trip, and persistence/restore of the timestamped metric history so
 * graphs survive application restarts.
 */
public class ForgeTelemetryLrHistoryTest {

    @Test
    public void testLearningRateIsStoredAndPublished() {
        TrainingTelemetry telemetry = new TrainingTelemetry("lr-session");
        assertEquals("Learning rate must start as honest -1 (unavailable)", -1.0, telemetry.learningRate, 0.0001);

        telemetry.updateMetrics(1, 4, 10, 40, 1000, 4000, 12.5, 0.3, 240.0, 3.2, 0.0, 0.0025);
        assertEquals(0.0025, telemetry.learningRate, 0.0001);

        JSONObject json = telemetry.toJsonObject();
        assertEquals(0.0025, json.getDouble("learningRate"), 0.0001);
    }

    @Test
    public void testMetricSampleJsonRoundTripIncludesLearningRate() {
        MetricSample s = new MetricSample();
        s.timestamp = 123456789L;
        s.epoch = 2;
        s.step = 17;
        s.loss = 3.4567;
        s.valLoss = 4.1;
        s.tokensPerSec = 320.5;
        s.stepsPerSec = 0.42;
        s.ramMb = 2048;
        s.cpuPct = 55.5;
        s.learningRate = 0.00123;

        JSONObject json = s.toJsonObject();
        assertEquals(123456789L, json.getLong("timestamp"));
        assertEquals(0.00123, json.getDouble("learningRate"), 0.00001);

        MetricSample restored = MetricSample.fromJsonObject(json);
        assertEquals(123456789L, restored.timestamp);
        assertEquals(2, restored.epoch);
        assertEquals(17, restored.step);
        assertEquals(3.4567, restored.loss, 0.0001);
        assertEquals(0.00123, restored.learningRate, 0.00001);
    }

    @Test
    public void testMetricHistoryPersistsAndRestores() throws Exception {
        ForgeSessionManager manager = ForgeSessionManager.getInstance();
        Path stateDir = Files.createTempDirectory("forge-lr-hist-test");
        ForgeSessionManager.setForgeStateDirectory(stateDir);
        try {
            Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
            orchestrator.setId("test-orch-lr-hist");
            manager.initialize(orchestrator);

            ForgeSession session = manager.createSession("LrHist", "SELF_EVO");
            TrainingTelemetry telemetry = manager.getTelemetry(session.getSessionId());
            telemetry.setStatus("RUNNING", "run");
            telemetry.startTimeMs = System.currentTimeMillis();
            telemetry.learningRate = 0.00123;

            for (int i = 0; i < 5; i++) {
                MetricSample s = new MetricSample();
                s.timestamp = System.currentTimeMillis() + i * 1000L;
                s.epoch = 1;
                s.step = i + 1;
                s.loss = 8.0 - i;
                s.valLoss = 8.5 - i;
                s.tokensPerSec = 300.0;
                s.stepsPerSec = 0.4;
                s.ramMb = 1800 + i;
                s.cpuPct = 40.0 + i;
                s.learningRate = 0.00123;
                telemetry.addSample(s);
            }

            // Simulate a failed run: terminal status persists scalars + history.
            manager.completeActiveTrainer(session.getSessionId(), "FAILED");
            Path stateFile = stateDir.resolve(session.getSessionId() + ".json");
            assertTrue(Files.exists(stateFile));

            JSONObject persisted = new JSONObject(Files.readString(stateFile));
            assertEquals("FAILED", persisted.getString("status"));
            assertEquals(0.00123, persisted.getDouble("learningRate"), 0.00001);
            assertTrue("History must be persisted", persisted.has("history"));
            assertEquals(5, persisted.getJSONArray("history").length());

            // Simulate restart: wipe the in-memory telemetry history, then recover.
            synchronized (telemetry.history) {
                telemetry.history.clear();
            }
            telemetry.learningRate = -1.0;
            telemetry.setStatus("IDLE", "cleared");
            assertEquals(ForgeStatus.ERROR, session.getStatus());

            java.lang.reflect.Field recoveredFlag = ForgeSessionManager.class.getDeclaredField("recoveredFromRestart");
            recoveredFlag.setAccessible(true);
            recoveredFlag.set(manager, false);
            manager.recoverFromRestart();

            // Terminal states survive: history + LR restored.
            assertEquals("FAILED", telemetry.status);
            assertEquals(0.00123, telemetry.learningRate, 0.00001);
            assertEquals("History must be restored after restart", 5, telemetry.history.size());
            assertEquals(8.0, telemetry.history.get(0).loss, 0.0001);
            assertEquals(300.0, telemetry.history.get(0).tokensPerSec, 0.01);
        } finally {
            ForgeSessionManager.setForgeStateDirectory(null);
        }
    }

    @Test
    public void testFreshStartClearsPreviousRunHistory() throws Exception {
        ForgeSessionManager manager = ForgeSessionManager.getInstance();
        Path stateDir = Files.createTempDirectory("forge-fresh-hist");
        ForgeSessionManager.setForgeStateDirectory(stateDir);
        try {
            Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
            orchestrator.setId("test-orch-fresh-hist");
            manager.initialize(orchestrator);

            ForgeSession session = manager.createSession("FreshHist", "SELF_EVO");
            TrainingTelemetry telemetry = manager.getTelemetry(session.getSessionId());
            MetricSample old = new MetricSample();
            old.timestamp = System.currentTimeMillis();
            old.epoch = 1;
            old.step = 1;
            old.loss = 9.0;
            telemetry.addSample(old);
            telemetry.learningRate = 0.005;

            // 3-arg registration with freshStart=true clears the previous run.
            eu.kalafatic.evolution.forge.model.llm.EvoLlmModel model = new eu.kalafatic.evolution.forge.model.llm.EvoLlmModel(50, 32, 2, 1, 64, 16);
            eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer trainer =
                    new eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer(model);
            manager.registerActiveTrainer(session.getSessionId(), trainer, true);
            assertEquals(0, telemetry.history.size());
            assertEquals(-1.0, telemetry.learningRate, 0.0001);
            assertEquals("RUNNING", telemetry.status);
            manager.completeActiveTrainer(session.getSessionId(), "COMPLETED");
        } catch (Exception e) {
            throw new AssertionError("Unexpected exception: " + e.getMessage(), e);
        } finally {
            ForgeSessionManager.setForgeStateDirectory(null);
        }
    }
}
