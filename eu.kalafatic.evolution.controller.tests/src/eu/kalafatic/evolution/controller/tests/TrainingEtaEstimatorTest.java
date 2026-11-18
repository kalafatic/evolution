package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.controller.service.impl.agents.TrainingEtaEstimator;
import eu.kalafatic.evolution.forge.controller.service.impl.agents.TrainingEtaEstimator.EtaEstimate;
import eu.kalafatic.evolution.forge.controller.service.impl.agents.TrainingEtaEstimator.Sample;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the ETA calculator: it must be derived from actual timestamped
 * measurements, adapt to changing throughput, and return an explicit
 * unavailable marker when evidence is missing. No fixed speeds, no fabrication.
 */
public class TrainingEtaEstimatorTest {

    private static final long T0 = 1_000_000L;

    private static List<Sample> steadySamples(long tokensPerSample, int stepPerSample, int count, long totalTokens, int totalSteps) {
        List<Sample> samples = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            samples.add(new Sample(T0 + i * 1000L, (i + 1) * tokensPerSample, (i + 1) * stepPerSample, totalTokens, totalSteps));
        }
        return samples;
    }

    @Test
    public void testUnavailableWhenNoSamples() {
        EtaEstimate e = TrainingEtaEstimator.compute(null, 0, 0, 1000, 10, T0);
        assertFalse(e.isAvailable());
        assertTrue(e.remainingSeconds < 0);

        e = TrainingEtaEstimator.compute(new ArrayList<>(), 0, 0, 1000, 10, T0);
        assertFalse(e.isAvailable());
    }

    @Test
    public void testZeroProgressYieldsUnavailableNotFake() {
        List<Sample> samples = new ArrayList<>();
        samples.add(new Sample(T0, 0, 0, 1000, 10));
        samples.add(new Sample(T0 + 2000L, 0, 0, 1000, 10));
        EtaEstimate e = TrainingEtaEstimator.compute(samples, 0, 0, 1000, 10, T0 + 3000L);
        assertFalse("No progress must produce no ETA", e.isAvailable());
    }

    @Test
    public void testEtaFromMeasuredTokenThroughput() {
        // 1000 tokens per sample every second => 1000 tok/s.
        List<Sample> samples = steadySamples(1000, 1, 10, 10_000, 10);
        EtaEstimate e = TrainingEtaEstimator.compute(samples, 10_000, 10, 10_000, 10, T0 + 10_000L);
        assertTrue(e.isAvailable());
        assertEquals("Nothing remaining", 0.0, e.remainingSeconds, 0.001);
        assertTrue(e.effectiveTokensPerSec > 0);

        // Half done (history up to 5000 tokens): 5000 tokens remain at 1000 tok/s => 5 s.
        e = TrainingEtaEstimator.compute(steadySamples(1000, 1, 5, 10_000, 10), 5_000, 5, 10_000, 10, T0 + 10_000L);
        assertTrue(e.isAvailable());
        assertEquals(5.0, e.remainingSeconds, 1.5);
        assertEquals("measured-token-throughput", e.basis);
        assertTrue(e.confidence > 0.0);
    }

    @Test
    public void testStepBasedFallbackWhenTokensUnknown() {
        List<Sample> samples = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            samples.add(new Sample(T0 + i * 1000L, 0, i, 0, 20));
        }
        EtaEstimate e = TrainingEtaEstimator.compute(samples, 0, 5, 0, 20, T0 + 6000L);
        assertTrue(e.isAvailable());
        assertEquals("measured-step-throughput", e.basis);
        assertEquals(15.0, e.remainingSeconds, 3.0);
    }

    @Test
    public void testEtaAdaptsToSlowerThroughputWithRollingWindow() {
        // 20 samples: first 10 fast (2000 tok/s), last 10 slow (250 tok/s).
        List<Sample> samples = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            long tokens = (i < 10) ? (i + 1) * 2000L : (10 * 2000L) + (i - 9) * 250L;
            samples.add(new Sample(T0 + i * 1000L, tokens, i + 1, 10_000_000L, 200));
        }
        // Window covers only the last ~120 s (all samples here are within 20 s,
        // so the effective rate is dominated by the recent slower samples).
        EtaEstimate e = TrainingEtaEstimator.compute(samples, 20_000 + 2_250, 20, 10_000_000L, 200, T0 + 20_000L);
        assertTrue(e.isAvailable());
        // Effective throughput must be clearly below the fast 2000 tok/s rate.
        assertTrue("ETA must adapt to recent slower throughput: " + e.effectiveTokensPerSec,
                e.effectiveTokensPerSec < 1500.0);
        assertTrue(e.effectiveTokensPerSec > 200.0);
    }

    @Test
    public void testHistoryFallbackBasisWhenWindowHasNoProgress() {
        // Progress happened long ago; the rolling window is empty, so the
        // estimator falls back to full-history measured throughput.
        List<Sample> samples = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            samples.add(new Sample(T0 + i * 1000L, (i + 1) * 100L, i + 1, 1_000_000, 100));
        }
        // now = 200 s after the last sample -> nothing inside the 120 s window.
        EtaEstimate e = TrainingEtaEstimator.compute(samples, 1_000, 10, 1_000_000, 100, T0 + 210_000L);
        assertTrue(e.isAvailable());
        assertTrue("Fallback basis must be measured history", e.basis.contains("history"));
    }

    @Test
    public void testNegativeCurrentCountersAreClampedNotRejected() {
        List<Sample> samples = steadySamples(1000, 1, 5, 10_000, 10);
        EtaEstimate e = TrainingEtaEstimator.compute(samples, -5, -1, 10_000, 10, T0 + 10_000L);
        assertTrue("Negative current counters must be clamped to the last measured value, not produce a fake ETA",
                e.isAvailable());
        assertTrue(e.remainingSeconds >= 0.0);
        assertFalse(Double.isNaN(e.remainingSeconds));
        assertFalse(Double.isInfinite(e.remainingSeconds));
    }
}
