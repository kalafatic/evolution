package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Regression test for the forge telemetry-propagation fix: the Darwin engine previously
 * called {@code EvoLlmTrainer.setProgressListener(...)} right after
 * {@code ForgeSessionManager.registerActiveTrainer(...)}, which cleared the telemetry
 * progress listener and left the Training Monitor without any live metrics.
 * {@code addProgressListener} must accumulate listeners instead of replacing them.
 *
 * <p>The GPU path fix ({@code GpuTrainingAdapter} listener list +
 * {@code ForgeSessionManager.registerActiveGpuTrainer}) is covered by compilation; the GPU
 * adapter cannot be constructed in this test runtime because the optional
 * {@code eu.kalafatic.evolution.gpu} bundle (GpuTrainingEngine) is not resolved here.
 */
public class GpuTrainingTelemetryTest {

    @Test
    public void testEvoLlmTrainerAddProgressListenerPreservesExisting() {
        // The CPU-path bug: LLMDarwinEngine called setProgressListener after
        // registerActiveTrainer, which cleared the telemetry listener. addProgressListener
        // must accumulate listeners rather than replace them.
        EvoLlmModel model = new EvoLlmModel(new EvoLlmArchitecture(100, 32, 2, 2, 64, 32));
        EvoLlmTrainer trainer = new EvoLlmTrainer(model);
        assertEquals(0, trainer.getProgressListenerCount());

        EvoLlmTrainer.ProgressListener telemetry = (e, t, s, ts, l) -> {};
        EvoLlmTrainer.ProgressListener log = (e, t, s, ts, l) -> {};
        trainer.addProgressListener(telemetry);
        assertEquals(1, trainer.getProgressListenerCount());
        trainer.addProgressListener(log);
        assertEquals("addProgressListener must preserve the first (telemetry) listener", 2, trainer.getProgressListenerCount());
    }
}
