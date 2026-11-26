package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager.MetricSample;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager.TrainingTelemetry;
import eu.kalafatic.evolution.forge.controller.service.impl.agents.ForgeTrainingEstimationAgent;
import eu.kalafatic.evolution.view.dialogs.ForgeTelemetrySnapshot;
import eu.kalafatic.evolution.view.dialogs.ForgeTelemetrySnapshot.BudgetState;
import eu.kalafatic.evolution.view.dialogs.ForgeTelemetrySnapshot.ThroughputBasis;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the honest derived monitoring values: progress, budget states,
 * ETA resolution and throughput basis. No fabricated metrics: missing data
 * must be N/A (-1), never invented.
 */
public class ForgeTelemetrySnapshotTest {

    @Before
    public void isolateMeasurements() {
        // Isolate from measurement history recorded by other tests/runs.
        ForgeTrainingEstimationAgent.getInstance().resetMeasurementsForTesting();
    }

    private static ForgeTelemetrySnapshot buildSnapshot(boolean active, boolean withHistory) {
        TrainingTelemetry t = new TrainingTelemetry("snap-session");
        t.status = active ? "RUNNING" : "COMPLETED";
        t.startTimeMs = System.currentTimeMillis() - 120_000L;
        t.currentEpoch = active ? 2 : 4;
        t.totalEpochs = 4;
        t.currentStep = active ? 250 : 400;
        t.totalSteps = 400;
        t.processedTokens = active ? 25_000 : 40_000;
        t.totalTokens = 40_000;
        t.currentLoss = 3.2;
        t.valLoss = 3.9;
        t.learningRate = 0.002;
        t.tokensPerSec = active ? 300.0 : 0.0;
        t.stepsPerSec = active ? 3.0 : 0.0;
        t.estimatedRemainingSeconds = active ? 50.0 : 0.0;
        t.ramUsedMb = 1500;
        t.ramTotalMb = 4096;
        if (withHistory) {
            long base = System.currentTimeMillis() - 60_000L;
            for (int i = 0; i < 6; i++) {
                MetricSample s = new MetricSample();
                s.timestamp = base + i * 10_000L;
                s.epoch = 1 + i / 2;
                s.step = (i + 1) * 40;
                s.loss = 8.0 - i * 0.8;
                s.valLoss = 8.5 - i * 0.7;
                s.tokensPerSec = 300.0;
                s.stepsPerSec = 3.0;
                s.ramMb = 1400 + i * 20;
                s.cpuPct = 45.0 + i;
                s.learningRate = 0.002;
                t.addSample(s);
            }
        }
        ForgeTelemetrySnapshot.Builder b = new ForgeTelemetrySnapshot.Builder();
        b.sessionId = "snap-session";
        b.sessionName = "Snapshot Test";
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
        b.cpuPercent = 50.0;
        b.gpuStatus = "N/A (CPU Mode)";
        b.configuredModelSize = "SMALL";
        b.configuredEpochs = 4;
        b.configuredDesiredLoss = 1.0;
        b.configuredTimeBudgetHours = 12.0;
        synchronized (t.history) {
            for (MetricSample s : t.history) {
                b.history.add(new ForgeTelemetrySnapshot.HistoryPoint(
                        s.timestamp, s.epoch, s.step, s.loss, s.valLoss, s.tokensPerSec,
                        s.stepsPerSec, s.ramMb, s.cpuPct, s.learningRate));
            }
        }
        b.totalDatasetBytes = 5 * 1024 * 1024L;
        return b.build();
    }

    @Test
    public void testProgressIsComputedFromSteps() {
        ForgeTelemetrySnapshot snap = buildSnapshot(true, true);
        assertTrue(snap.overallProgress > 0.6 && snap.overallProgress < 0.7); // 250/400
        assertTrue(snap.epochProgress >= 0.0 && snap.epochProgress <= 1.0);
        assertTrue(snap.isTrainingActive);
    }

    @Test
    public void testTerminalStateShowsCompletedProgress() {
        ForgeTelemetrySnapshot snap = buildSnapshot(false, true);
        assertEquals(1.0, snap.overallProgress, 0.0001);
        assertEquals(BudgetState.COMPLETED, snap.budgetState);
        assertFalse(snap.isTrainingActive);
    }

    @Test
    public void testSamplePositionIsCaptured() {
        ForgeTelemetrySnapshot.Builder b = new ForgeTelemetrySnapshot.Builder();
        b.sessionId = "sample-pos";
        b.status = "RUNNING";
        b.currentStep = 250;
        b.totalSteps = 400;
        b.currentSample = 120;
        b.totalSamples = 200;
        b.configuredModelSize = "SMALL";
        b.configuredEpochs = 8;
        ForgeTelemetrySnapshot snap = b.build();
        assertEquals("Sample position must flow through the snapshot", 120, snap.currentSample);
        assertEquals("Total samples must flow through the snapshot", 200, snap.totalSamples);
    }

    @Test
    public void testMeasuredEtaPreferredOverPreliminary() {
        ForgeTelemetrySnapshot snap = buildSnapshot(true, true);
        // Live telemetry throughput > 0 => MEASURED_LIVE basis.
        assertEquals(ThroughputBasis.MEASURED_LIVE, snap.throughputBasis);
        assertEquals(300.0, snap.effectiveTokensPerSec, 0.01);
        // Measured ETA from the timestamped history must be available.
        assertTrue("Measured ETA must be available: " + snap.etaBasis, snap.estimatedRemainingSeconds >= 0.0);
        assertTrue("Confidence must reflect real samples", snap.etaConfidence > 0.0);
    }

    @Test
    public void testIdleSnapshotWithoutHistoryUsesPreliminaryHonestly() {
        TrainingTelemetry t = new TrainingTelemetry("idle-session");
        t.status = "IDLE";
        ForgeTelemetrySnapshot.Builder b = new ForgeTelemetrySnapshot.Builder();
        b.status = t.status;
        b.configuredModelSize = "SMALL";
        b.configuredEpochs = 4;
        b.configuredTimeBudgetHours = 12.0;
        b.totalDatasetBytes = 5 * 1024 * 1024L;
        ForgeTelemetrySnapshot snap = b.build();

        assertEquals(ThroughputBasis.ANALYTICAL_PRELIMINARY, snap.throughputBasis);
        assertEquals(-1.0, snap.estimatedRemainingSeconds, 0.0001); // no measurements -> N/A, not fake
        assertEquals(0.0, snap.etaConfidence, 0.0001);
        assertTrue(snap.initialEstimateSeconds > 0); // preliminary total estimate is available
        // The preliminary estimate (CPU SMALL, 5 MB x 4 epochs) exceeds the
        // 12 h budget: the capacity state must say so honestly.
        assertEquals(BudgetState.EXCEEDS_BUDGET, snap.budgetState);
        assertTrue(snap.budgetConsumption > 1.0);

        // With a generous budget the same configuration is ON TRACK.
        ForgeTelemetrySnapshot.Builder b2 = new ForgeTelemetrySnapshot.Builder();
        b2.status = "IDLE";
        b2.configuredModelSize = "SMALL";
        b2.configuredEpochs = 4;
        b2.configuredTimeBudgetHours = 500.0;
        b2.totalDatasetBytes = 5 * 1024 * 1024L;
        ForgeTelemetrySnapshot generous = b2.build();
        assertEquals(BudgetState.ON_TRACK, generous.budgetState);
        assertTrue(generous.budgetConsumption > 0 && generous.budgetConsumption < 1.0);
    }

    @Test
    public void testBudgetStates() {
        // Elapsed 120s, measured ETA ~small => ON TRACK for a 12h budget.
        ForgeTelemetrySnapshot snap = buildSnapshot(true, true);
        assertEquals(BudgetState.ON_TRACK, snap.budgetState);
        assertTrue(snap.budgetConsumption > 0 && snap.budgetConsumption < 1.0);

        // Tiny budget: consumption must exceed the budget.
        ForgeTelemetrySnapshot.Builder b = new ForgeTelemetrySnapshot.Builder();
        b.status = "RUNNING";
        b.startTimeMs = System.currentTimeMillis() - 120_000L;
        b.currentStep = 250;
        b.totalSteps = 400;
        b.configuredModelSize = "SMALL";
        b.configuredEpochs = 4;
        b.configuredTimeBudgetHours = 0.01; // 36 s budget vs 120 s elapsed
        b.totalDatasetBytes = 5 * 1024 * 1024L;
        ForgeTelemetrySnapshot overrun = b.build();
        assertEquals(BudgetState.EXCEEDS_BUDGET, overrun.budgetState);
        assertTrue(overrun.budgetConsumption > 1.0);
    }

    @Test
    public void testFingerprintChangesWithState() {
        ForgeTelemetrySnapshot a = buildSnapshot(true, true);
        ForgeTelemetrySnapshot b = buildSnapshot(true, false);
        assertFalse("Fingerprint must differ when history differs", a.fingerprint().equals(b.fingerprint()));
    }

    @Test
    public void testCaptureOverlaysLiveConfiguration() {
        // When the dialog edits its working copy (before committing to the
        // session on OK), the snapshot must reflect the live values — not the
        // session's previously committed configuration.
        ForgeTelemetrySnapshot snap = ForgeTelemetrySnapshot.capture(
                null, "s", null, 5 * 1024 * 1024L,
                "LARGE", 32, 0.5, 24.0, true);

        assertEquals("LARGE", snap.configuredModelSize);
        assertEquals(32, snap.configuredEpochs);
        assertEquals(0.5, snap.configuredDesiredLoss, 0.0001);
        assertEquals(24.0, snap.configuredTimeBudgetHours, 0.0001);
        assertTrue("Early stopping overlay must be applied", snap.configuredEarlyStopping);
        // The preliminary estimate must be recomputed from the overlaid
        // configuration (dataset bytes + epochs + model size), not dropped.
        assertTrue("Overlay must keep the dataset bytes and recompute the estimate",
                snap.initialEstimateSeconds > 0.0);
        assertEquals(5 * 1024 * 1024L, snap.totalDatasetBytes);
    }

    @Test
    public void testCaptureWithoutOverlayKeepsSessionDefaults() {
        // No overlay + no session => honest UNKNOWN defaults, unchanged legacy behavior.
        ForgeTelemetrySnapshot snap = ForgeTelemetrySnapshot.capture(null, "s", null, 0L);
        assertEquals("SMALL", snap.configuredModelSize);
        assertEquals(4, snap.configuredEpochs);
        assertEquals(1.0, snap.configuredDesiredLoss, 0.0001);
    }

    @Test
    public void testBuilderCopyPreservesTelemetryAndRecomputesDerived() {
        ForgeTelemetrySnapshot base = buildSnapshot(true, true);
        ForgeTelemetrySnapshot.Builder copy = new ForgeTelemetrySnapshot.Builder(base);
        copy.configuredEpochs = 64;
        copy.configuredModelSize = "LARGE";
        ForgeTelemetrySnapshot overlaid = copy.build();

        // Telemetry must survive the copy unchanged.
        assertEquals(base.status, overlaid.status);
        assertEquals(base.currentEpoch, overlaid.currentEpoch);
        assertEquals(base.totalSteps, overlaid.totalSteps);
        assertEquals(base.history.size(), overlaid.history.size());
        assertEquals(base.totalDatasetBytes, overlaid.totalDatasetBytes);

        // Configuration override applied.
        assertEquals(64, overlaid.configuredEpochs);
        assertEquals("LARGE", overlaid.configuredModelSize);

        // Derived values recomputed from the overlaid config, not the base.
        assertFalse("Overlay must change the fingerprint",
                base.fingerprint().equals(overlaid.fingerprint()));
    }
}
