package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager.MetricSample;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager.TrainingTelemetry;
import eu.kalafatic.evolution.forge.controller.service.impl.agents.ForgeTrainingEstimationAgent;
import eu.kalafatic.evolution.view.dialogs.ForgeSettingsDialog;
import eu.kalafatic.evolution.view.dialogs.ForgeTelemetrySnapshot;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the dialog graph generation displays ONLY real data:
 * - real measured values appear in the HTML when telemetry exists,
 * - no fabricated projection curves are produced without measurements,
 * - all graph sections render with the required headers,
 * - the monitoring tab set renders all required graph tabs.
 */
public class ForgeSettingsDialogLiveMonitorTest {

    @Before
    public void isolateMeasurements() {
        ForgeTrainingEstimationAgent.getInstance().resetMeasurementsForTesting();
    }

    private ForgeTelemetrySnapshot buildSnapshot(boolean withHistory) {
        TrainingTelemetry t = new TrainingTelemetry("dlg-session");
        t.status = withHistory ? "RUNNING" : "IDLE";
        t.startTimeMs = System.currentTimeMillis() - 300_000L;
        t.currentEpoch = 3;
        t.totalEpochs = 8;
        t.currentStep = 150;
        t.totalSteps = 400;
        t.processedTokens = 15_000;
        t.totalTokens = 40_000;
        t.currentLoss = 2.75;
        t.valLoss = 3.1;
        t.learningRate = 0.0015;
        t.tokensPerSec = 128.5;
        t.stepsPerSec = 1.25;
        t.estimatedRemainingSeconds = 2000.0;
        t.ramUsedMb = 1700;
        t.ramTotalMb = 8192;
        t.cpuPercent = 62.5;

        ForgeTelemetrySnapshot.Builder b = new ForgeTelemetrySnapshot.Builder();
        b.sessionId = "dlg-session";
        b.status = t.status;
        b.startTimeMs = t.startTimeMs;
        b.currentEpoch = t.currentEpoch;
        b.totalEpochs = t.totalEpochs;
        b.currentStep = t.currentStep;
        b.totalSteps = t.totalSteps;
        b.processedTokens = t.processedTokens;
        b.totalTokens = t.totalTokens;
        b.currentLoss = t.currentLoss;
        b.valLoss = t.valLoss;
        b.learningRate = t.learningRate;
        b.tokensPerSec = t.tokensPerSec;
        b.stepsPerSec = t.stepsPerSec;
        b.trainerEstimatedRemainingSeconds = t.estimatedRemainingSeconds;
        b.ramUsedMb = t.ramUsedMb;
        b.ramTotalMb = t.ramTotalMb;
        b.cpuPercent = t.cpuPercent;
        b.gpuStatus = "N/A (CPU Mode)";
        b.configuredModelSize = "SMALL";
        b.configuredEpochs = 8;
        b.configuredDesiredLoss = 1.0;
        b.configuredTimeBudgetHours = 12.0;
        if (withHistory) {
            long base = System.currentTimeMillis() - 120_000L;
            for (int i = 0; i < 8; i++) {
                MetricSample s = new MetricSample();
                s.timestamp = base + i * 15_000L;
                s.epoch = i / 2 + 1;
                s.step = (i + 1) * 18;
                s.loss = 6.5 - i * 0.5;
                s.valLoss = 6.9 - i * 0.45;
                s.tokensPerSec = 120.0 + i;
                s.stepsPerSec = 1.2;
                s.ramMb = 1600 + i * 15;
                s.cpuPct = 55.0 + i;
                s.learningRate = 0.0015;
                t.addSample(s);
            }
            synchronized (t.history) {
                for (MetricSample s : t.history) {
                    b.history.add(new ForgeTelemetrySnapshot.HistoryPoint(
                            s.timestamp, s.epoch, s.step, s.loss, s.valLoss, s.tokensPerSec,
                            s.stepsPerSec, s.ramMb, s.cpuPct, s.learningRate));
                }
            }
        }
        b.totalDatasetBytes = 8 * 1024 * 1024L;
        return b.build();
    }

    @Test
    public void testLiveGraphHtmlContainsRealMeasuredValues() {
        ForgeSettingsDialog dialog = new ForgeSettingsDialog(null, "SMALL", 8, "Stage 2", 1.0, null, null);
        ForgeTelemetrySnapshot snap = buildSnapshot(true);
        String html = dialog.generateGraphHtml(snap);

        // Required headers preserved (regression contract with the existing suite).
        assertTrue(html.contains("summary-bar"));
        assertTrue(html.contains("Model Size:"));
        assertTrue(html.contains("Budget:"));
        assertTrue(html.contains("GRAPH 1: LOSS TRAJECTORY"));
        assertTrue(html.contains("GRAPH 2: CAPACITY BUDGET"));
        assertTrue(html.contains("GRAPH 3: PROGRESS & RESOURCE MONITOR"));
        assertTrue(html.contains("Calculation Progress / Time"));
        assertTrue(html.contains("Allocated System RAM / Memory Usage"));

        // Real measured values must appear verbatim in the HTML.
        assertTrue("Measured loss must be displayed", html.contains("2.7500"));
        assertTrue("Measured tokens must be displayed", html.contains("15.0 k"));
        assertTrue("Measured epoch must be displayed", html.contains("EPOCH 3 / 8"));
        assertTrue("Measured step must be displayed", html.contains("150"));
        assertTrue("Measured LR must be displayed", html.contains("0.001500"));
        assertTrue("Measured throughput must be displayed", html.contains("128.5 tok/s (measured)"));
        assertTrue("Measured RAM must be displayed", html.contains("1700 MB / 8192 MB RAM"));
        assertTrue("Measured CPU must be displayed", html.contains("62.5% CPU"));
    }

    @Test
    public void testNoFabricatedProjectionWithoutMeasurements() {
        ForgeSettingsDialog dialog = new ForgeSettingsDialog(null, "SMALL", 8, "Stage 2", 1.0, null, null);
        String html = dialog.generateGraphHtml(8, 1.0, null);

        assertTrue(html.contains("GRAPH 1: LOSS TRAJECTORY"));
        // The hardcoded reference trajectory (8.87 start, exp(-3.5t)) must be GONE.
        assertFalse("No fabricated initial loss constant", html.contains("8.87"));
        assertFalse("No fabricated exponential decay", html.contains("-3.5"));
        // The honest placeholder must be shown instead.
        assertTrue("Honest empty-state placeholder required",
                html.contains("No measured training data yet"));
        assertFalse("Fabricated 'Projected Loss Trajectory' state must not exist",
                html.contains("Projected Loss Trajectory"));
    }

    @Test
    public void testProjectionOnlyFromMeasuredFit() {
        ForgeSettingsDialog dialog = new ForgeSettingsDialog(null, "SMALL", 8, "Stage 2", 1.0, null, null);
        String html = dialog.generateGraphHtml(buildSnapshot(true));
        assertTrue("Projected series must be labeled as a measured fit",
                html.contains("Projected (linear fit of last"));
    }

    @Test
    public void testCapacityBudgetStatesRender() {
        ForgeSettingsDialog dialog = new ForgeSettingsDialog(null, "SMALL", 8, "Stage 2", 1.0, null, null);
        String html = dialog.generateGraphHtml(buildSnapshot(true));
        assertTrue(html.contains("Capacity Budget"));
        assertTrue(html.contains("Actual Elapsed (measured)"));
        assertTrue(html.contains("Estimated Remaining (measured ETA)"));
        assertTrue(html.contains("Estimated Total Duration"));
    }

    @Test
    public void testMonitoringTabsRenderAllGraphs() {
        ForgeSettingsDialog dialog = new ForgeSettingsDialog(null, "SMALL", 8, "Stage 2", 1.0, null, null);
        ForgeTelemetrySnapshot snap = buildSnapshot(true);
        String html = dialog.generateMonitoringGraphsHtml(snap);

        assertNotNull(html);
        for (String tab : new String[] { "Loss", "Learning Rate", "Throughput", "Hardware", "Memory", "Timeline" }) {
            assertTrue("Monitoring tab missing: " + tab, html.contains(tab));
        }
        assertTrue(html.contains("tabLoss"));
        assertTrue(html.contains("tabLr"));
        assertTrue(html.contains("tabTp"));
        assertTrue(html.contains("tabHw"));
        assertTrue(html.contains("tabMem"));
        assertTrue(html.contains("tabTime"));
    }

    @Test
    public void testMonitoringTabsGracefullyHandleMissingData() {
        ForgeSettingsDialog dialog = new ForgeSettingsDialog(null, "SMALL", 8, "Stage 2", 1.0, null, null);
        ForgeTelemetrySnapshot snap = buildSnapshot(false);
        String html = dialog.generateMonitoringGraphsHtml(snap);
        assertNotNull(html);
        assertTrue("Empty state must be honest",
                html.contains("No ") && html.contains("data yet"));
    }

    @Test
    public void testLegacySignatureStillWorksWithHistory() {
        ForgeSettingsDialog dialog = new ForgeSettingsDialog(null, "SMALL", 8, "Stage 2", 1.0, new double[] { 9.0, 6.0, 3.0 }, null);
        String html = dialog.generateGraphHtml(8, 1.0, new double[] { 9.0, 6.0, 3.0 });
        assertTrue(html.contains("GRAPH 1: LOSS TRAJECTORY"));
        assertTrue("Historical persisted loss must be labeled as such",
                html.contains("Historical loss (persisted config, previous run)"));
    }

    @Test
    public void testProjectionRequiresMinimumHistory() {
        // Only 2 measured points: projection must NOT be drawn (insufficient).
        TrainingTelemetry t = new TrainingTelemetry("few-session");
        t.status = "RUNNING";
        t.totalSteps = 400;
        t.currentStep = 100;
        t.totalTokens = 40_000;
        t.processedTokens = 10_000;
        t.tokensPerSec = 200.0;

        ForgeTelemetrySnapshot.Builder b = new ForgeTelemetrySnapshot.Builder();
        b.status = "RUNNING";
        b.startTimeMs = System.currentTimeMillis() - 60_000L;
        b.totalSteps = 400;
        b.currentStep = 100;
        b.totalTokens = 40_000;
        b.processedTokens = 10_000;
        b.tokensPerSec = 200.0;
        b.configuredModelSize = "SMALL";
        b.configuredEpochs = 8;
        b.configuredDesiredLoss = 1.0;
        b.configuredTimeBudgetHours = 12.0;
        List<ForgeTelemetrySnapshot.HistoryPoint> hist = new ArrayList<>();
        long base = System.currentTimeMillis() - 40_000L;
        hist.add(new ForgeTelemetrySnapshot.HistoryPoint(base, 1, 50, 6.0, 0, 200, 1.0, 1500, 50, 0.001));
        hist.add(new ForgeTelemetrySnapshot.HistoryPoint(base + 20_000L, 1, 100, 5.0, 0, 200, 1.0, 1510, 51, 0.001));
        b.history = hist;
        b.totalDatasetBytes = 8 * 1024 * 1024L;
        ForgeTelemetrySnapshot snap = b.build();

        ForgeSettingsDialog dialog = new ForgeSettingsDialog(null, "SMALL", 8, "Stage 2", 1.0, null, null);
        String html = dialog.generateGraphHtml(snap);
        assertFalse("Projection must not be fabricated from <3 measured points",
                html.contains("Projected (linear fit of last"));
    }

    @Test
    public void testCompletedSessionHasNoMisleadingRemainingTime() {
        // A COMPLETED session must never retain a projected remaining duration or a
        // stale "estimated total" (the historical bug that showed "24 days" for a
        // completed run). It reports its actual elapsed duration instead.
        TrainingTelemetry t = new TrainingTelemetry("completed-session");
        t.status = "COMPLETED";
        t.startTimeMs = System.currentTimeMillis() - (96 * 60_000L); // 1h36m ago
        t.currentEpoch = 8;
        t.totalEpochs = 8;
        t.currentStep = 400;
        t.totalSteps = 400;
        t.processedTokens = 20_000;
        t.totalTokens = 40_000;   // > processed: would have produced a stale positive ETA
        t.tokensPerSec = 128.0;
        t.currentLoss = 1.2;

        ForgeTelemetrySnapshot.Builder b = new ForgeTelemetrySnapshot.Builder();
        b.sessionId = "completed-session";
        b.status = t.status;
        b.startTimeMs = t.startTimeMs;
        b.currentEpoch = t.currentEpoch;
        b.totalEpochs = t.totalEpochs;
        b.currentStep = t.currentStep;
        b.totalSteps = t.totalSteps;
        b.processedTokens = t.processedTokens;
        b.totalTokens = t.totalTokens;
        b.tokensPerSec = t.tokensPerSec;
        b.currentLoss = t.currentLoss;
        b.configuredModelSize = "SMALL";
        b.configuredEpochs = 8;
        b.configuredDesiredLoss = 1.0;
        b.configuredTimeBudgetHours = 12.0;
        b.totalDatasetBytes = 8 * 1024 * 1024L;
        ForgeTelemetrySnapshot snap = b.build();

        assertTrue("Completed session must not report a remaining duration (got "
                + snap.estimatedRemainingSeconds + ")", snap.estimatedRemainingSeconds < 0.0);
        assertTrue("Completed total duration must equal actual elapsed time (got "
                + snap.estimatedTotalSeconds + " vs elapsed " + snap.elapsedSeconds + ")",
                Math.abs(snap.estimatedTotalSeconds - snap.elapsedSeconds) < 0.5);
        assertTrue("Budget state must be COMPLETED",
                snap.budgetState == ForgeTelemetrySnapshot.BudgetState.COMPLETED);

        ForgeSettingsDialog dialog = new ForgeSettingsDialog(null, "SMALL", 8, "Stage 2", 1.0, null, null);
        String html = dialog.generateGraphHtml(snap);
        assertFalse("Completed session must not show 'Remaining:'", html.contains("Remaining:"));
        assertTrue("Completed session must show 'Completed in'", html.contains("Completed in"));
    }

    @Test
    public void testAllTerminalStatesHaveNoRemainingTime() {
        for (String status : new String[] { "COMPLETED", "STOPPED", "FAILED", "CANCELLED" }) {
            ForgeTelemetrySnapshot.Builder b = new ForgeTelemetrySnapshot.Builder();
            b.sessionId = "terminal-" + status.toLowerCase();
            b.status = status;
            b.startTimeMs = System.currentTimeMillis() - (96 * 60_000L);
            b.currentEpoch = 8;
            b.totalEpochs = 8;
            b.currentStep = 400;
            b.totalSteps = 400;
            b.processedTokens = 20_000;
            b.totalTokens = 40_000; // > processed: would produce a stale ETA in the buggy path
            b.tokensPerSec = 128.0;
            b.currentLoss = 1.2;
            b.configuredModelSize = "SMALL";
            b.configuredEpochs = 8;
            b.configuredDesiredLoss = 1.0;
            b.configuredTimeBudgetHours = 12.0;
            b.totalDatasetBytes = 8 * 1024 * 1024L;
            ForgeTelemetrySnapshot snap = b.build();

            assertTrue("Terminal state " + status + " must not report remaining time (got "
                    + snap.estimatedRemainingSeconds + ")", snap.estimatedRemainingSeconds < 0.0);
            assertTrue("Terminal state " + status + " total must equal elapsed (got "
                    + snap.estimatedTotalSeconds + " vs elapsed " + snap.elapsedSeconds + ")",
                    Math.abs(snap.estimatedTotalSeconds - snap.elapsedSeconds) < 0.5);
        }
    }

    @Test
    public void testRunningSessionStillProjectsRemainingTime() {
        // A RUNNING session with remaining work must still project a remaining duration
        // (the terminal fix must not suppress legitimate live ETA).
        ForgeTelemetrySnapshot.Builder b = new ForgeTelemetrySnapshot.Builder();
        b.sessionId = "running-session";
        b.status = "RUNNING";
        b.startTimeMs = System.currentTimeMillis() - 60_000L;
        b.currentEpoch = 2;
        b.totalEpochs = 8;
        b.currentStep = 100;
        b.totalSteps = 400;
        b.processedTokens = 10_000;
        b.totalTokens = 40_000;
        b.tokensPerSec = 128.0;
        b.currentLoss = 2.5;
        b.configuredModelSize = "SMALL";
        b.configuredEpochs = 8;
        b.configuredDesiredLoss = 1.0;
        b.configuredTimeBudgetHours = 12.0;
        b.totalDatasetBytes = 8 * 1024 * 1024L;
        ForgeTelemetrySnapshot snap = b.build();

        assertTrue("Running session should still project remaining time (got "
                + snap.estimatedRemainingSeconds + ")", snap.estimatedRemainingSeconds >= 0.0);
    }
}
