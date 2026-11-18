package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.data.api.TrainingSample;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.trainer.api.TerminationReason;
import eu.kalafatic.evolution.forge.trainer.api.TrainingResult;
import eu.kalafatic.evolution.forge.trainer.api.TrainingTerminationStatus;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the repaired training-loop behaviors:
 * - configured warmup steps are actually applied to the LR schedule,
 * - the hard time budget interrupts even a long epoch (window boundaries),
 * - the learning rate is published through the progress callbacks,
 * - loss history is bounded,
 * - legacy listeners (onProgress/onProgressMetrics) still work.
 */
public class EvoLlmTrainerWarmupBudgetLrTest {

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
    public void testConfiguredWarmupStepsAreApplied() {
        EvoLlmTrainer trainer = newTinyTrainer();
        trainer.setWarmupSteps(50);
        List<TrainingSample> samples = buildPatternSamples(6, 8);

        // Capture the LR of the FIRST optimizer step.
        AtomicReference<Double> firstLr = new AtomicReference<>(-1.0);
        trainer.addProgressListener(new EvoLlmTrainer.ProgressListener() {
            @Override
            public void onProgress(int epoch, int totalEpochs, int sampleIndex, int totalSamplesCount, double currentLoss) {}

            @Override
            public void onProgressMetrics(int epoch, int totalEpochs, int currentStep, int totalSteps,
                                          long processedTokens, long totalTokens, double tokensPerSec, double stepsPerSec,
                                          double estimatedRemainingSeconds, double currentLoss, double learningRate) {
                if (firstLr.get() < 0) {
                    firstLr.set(learningRate);
                }
            }
        });

        TrainingResult result = trainer.train(samples, 2);
        assertEquals(TrainingTerminationStatus.TRAINING_COMPLETED, result.getTerminationStatus());
        double expectedFirstLr = 0.01f * (1.0 / 50.0);
        assertEquals("First step LR must follow the configured warmup schedule (initialLr / warmupSteps)",
                expectedFirstLr, firstLr.get(), 0.000001);
        assertTrue("LR must be positive", firstLr.get() > 0.0);
    }

    @Test
    public void testZeroHourBudgetStopsBeforeFirstEpochCompletes() {
        EvoLlmTrainer trainer = newTinyTrainer();
        trainer.setTimeBudgetHours(0.0f); // elapsed >= 0 -> immediate stop at first window
        List<TrainingSample> samples = buildPatternSamples(6, 8);

        TrainingResult result = trainer.train(samples, 2);
        assertEquals(TrainingTerminationStatus.TRAINING_STOPPED, result.getTerminationStatus());
        assertEquals(TerminationReason.TIME_BUDGET_REACHED, result.getTerminationReason());
        assertEquals("A partial epoch must not be counted as completed", 0, result.getCompletedEpochs());
    }

    @Test
    public void testLearningRateIsPublishedThroughCallbacks() {
        EvoLlmTrainer trainer = newTinyTrainer();
        List<TrainingSample> samples = buildPatternSamples(6, 8);
        AtomicInteger lrCallbacks = new AtomicInteger(0);
        AtomicReference<Double> lastLr = new AtomicReference<>(-1.0);
        trainer.addProgressListener(new EvoLlmTrainer.ProgressListener() {
            @Override
            public void onProgress(int epoch, int totalEpochs, int sampleIndex, int totalSamplesCount, double currentLoss) {}

            @Override
            public void onProgressMetrics(int epoch, int totalEpochs, int currentStep, int totalSteps,
                                          long processedTokens, long totalTokens, double tokensPerSec, double stepsPerSec,
                                          double estimatedRemainingSeconds, double currentLoss, double learningRate) {
                lrCallbacks.incrementAndGet();
                lastLr.set(learningRate);
            }
        });
        trainer.train(samples, 2);
        assertTrue("Progress callbacks must fire", lrCallbacks.get() > 0);
        assertTrue("Learning rate must be published", lastLr.get() > 0.0);
    }

    @Test
    public void testLegacyTenArgProgressListenerStillReceivesCallbacks() {
        EvoLlmTrainer trainer = newTinyTrainer();
        List<TrainingSample> samples = buildPatternSamples(6, 8);
        AtomicInteger callbacks = new AtomicInteger(0);
        trainer.addProgressListener(new EvoLlmTrainer.ProgressListener() {
            @Override
            public void onProgress(int epoch, int totalEpochs, int sampleIndex, int totalSamplesCount, double currentLoss) {}

            @Override
            public void onProgressMetrics(int epoch, int totalEpochs, int currentStep, int totalSteps,
                                          long processedTokens, long totalTokens, double tokensPerSec, double stepsPerSec,
                                          double estimatedRemainingSeconds, double currentLoss) {
                callbacks.incrementAndGet();
            }
        });
        trainer.train(samples, 1);
        assertTrue("Legacy 10-arg listeners must receive progress via the new default bridge", callbacks.get() > 0);
    }

    @Test
    public void testLossHistoryIsBounded() {
        EvoLlmTrainer trainer = newTinyTrainer();
        List<TrainingSample> samples = buildPatternSamples(6, 8);
        // Train many epochs on a tiny dataset so the loss history grows.
        trainer.train(samples, 30);
        trainer.train(samples, 60);
        assertTrue(trainer.getLossHistory().size() <= 4096);
        assertTrue("Loss history must contain measured losses", trainer.getLossHistory().size() >= 30);
    }

    @Test
    public void testTokenCounterAvailableAfterTraining() {
        EvoLlmTrainer trainer = newTinyTrainer();
        List<TrainingSample> samples = buildPatternSamples(6, 8);
        trainer.train(samples, 2);
        assertTrue("Processed tokens must be tracked after training", trainer.getLastProcessedTokens() > 0);
    }
}
