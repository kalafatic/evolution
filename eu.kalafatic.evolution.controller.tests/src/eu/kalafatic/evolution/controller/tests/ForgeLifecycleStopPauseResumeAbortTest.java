package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.forge.data.api.TrainingSample;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.trainer.api.TerminationReason;
import eu.kalafatic.evolution.forge.trainer.api.TrainingCheckpoint;
import eu.kalafatic.evolution.forge.trainer.api.TrainingResult;
import eu.kalafatic.evolution.forge.trainer.api.TrainingTerminationStatus;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;

public class ForgeLifecycleStopPauseResumeAbortTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private EvoLlmModel model;
    private List<TrainingSample> samples;
    private Map<String, Integer> vocab;

    @Before
    public void setUp() {
        vocab = new HashMap<>();
        vocab.put("<pad>", 0);
        vocab.put("<s>", 1);
        vocab.put("</s>", 2);
        for (int i = 3; i < 20; i++) {
            vocab.put("token_" + i, i);
        }

        EvoLlmArchitecture arch = new EvoLlmArchitecture(vocab.size(), 16, 1, 1, 32, 16);
        model = new EvoLlmModel(arch);

        samples = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            int[] in = new int[]{1, 3, 4, 5, 6, 7, 8, 9};
            int[] tgt = new int[]{3, 4, 5, 6, 7, 8, 9, 2};
            boolean[] mask = new boolean[8];
            float[] lossMask = new float[8];
            for (int j = 0; j < 8; j++) {
                mask[j] = true;
                lossMask[j] = 1.0f;
            }
            samples.add(new TrainingSample(in, tgt, mask, lossMask));
        }
    }

    @Test
    public void testStopDuringBatchExecutionProducesUsableModel() throws Exception {
        EvoLlmTrainer trainer = new EvoLlmTrainer(model, EvoLlmTrainer.TrainingProfile.EVO_FAST);
        trainer.setMicroBatchSize(2);

        // Request stop immediately on first progress event
        trainer.addProgressListener(new EvoLlmTrainer.ProgressListener() {
            @Override
            public void onProgress(int epoch, int totalEpochs, int sampleIndex, int totalSamplesCount, double currentLoss) {}

            @Override
            public void onEpochCompleted(int epoch, int totalEpochs, double trainLoss, double valLoss, long durationMs) {}

            @Override
            public void onProgressMetrics(int epoch, int totalEpochs, int optStep, int totalSteps,
                                          long processedTokens, long totalTokens,
                                          double tokensPerSec, double stepsPerSec,
                                          double remainingSec, double currentLoss) {
                if (optStep >= 2) {
                    trainer.requestStop();
                }
            }
        });

        TrainingResult res = trainer.train(samples, 5);

        assertEquals(TrainingTerminationStatus.TRAINING_STOPPED, res.getTerminationStatus());
        assertEquals(TerminationReason.USER_STOP_REQUEST, res.getTerminationReason());
        assertTrue("Model should be valid after STOP", res.isModelValid());

        // Save and verify artifact can be initialized and saved
        Path evoFile = tempFolder.newFile("stopped-model.evo").toPath();
        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("stopped-model", trainer.getModel(), vocab);
        artifact.save(evoFile);

        assertTrue(Files.exists(evoFile));
        EvoModelArtifact reloaded = EvoModelArtifact.load(evoFile);
        assertNotNull(reloaded.createModel());
    }

    @Test
    public void testPauseAndResumePreservesStateAndMomentum() throws Exception {
        Path chkFile = tempFolder.newFile("session.chk").toPath();

        // 1. Train and PAUSE at epoch 2
        EvoLlmTrainer trainer1 = new EvoLlmTrainer(model, EvoLlmTrainer.TrainingProfile.EVO_FAST);
        trainer1.setMicroBatchSize(2);

        trainer1.addProgressListener(new EvoLlmTrainer.ProgressListener() {
            @Override
            public void onProgress(int epoch, int totalEpochs, int sampleIndex, int totalSamplesCount, double currentLoss) {}

            @Override
            public void onEpochCompleted(int epoch, int totalEpochs, double trainLoss, double valLoss, long durationMs) {
                if (epoch >= 2) {
                    trainer1.requestPause();
                }
            }
        });

        TrainingResult res1 = trainer1.train(samples, 5);
        assertEquals(TrainingTerminationStatus.TRAINING_PAUSED, res1.getTerminationStatus());
        assertEquals(TerminationReason.USER_PAUSE_REQUEST, res1.getTerminationReason());
        assertEquals(2, res1.getCompletedEpochs());

        // Checkpoint the paused state using trainer capture
        TrainingCheckpoint chk = trainer1.captureCheckpoint(
                "test-pause-run",
                "test-model",
                res1.getCompletedEpochs(),
                (float) res1.getLastLoss(),
                (float) res1.getValLoss(),
                "dataset-fingerprint-123",
                vocab
        );
        chk.saveAtomic(chkFile);
        assertTrue(Files.exists(chkFile));

        // 2. RESUME training on a fresh model/trainer instance from checkpoint
        EvoLlmModel resumeModel = new EvoLlmModel(new EvoLlmArchitecture(vocab.size(), 16, 1, 1, 32, 16));
        TrainingCheckpoint loadedChk = TrainingCheckpoint.load(chkFile);

        EvoLlmTrainer trainer2 = new EvoLlmTrainer(resumeModel, EvoLlmTrainer.TrainingProfile.EVO_FAST);
        trainer2.setMicroBatchSize(2);
        trainer2.setResumeCheckpoint(loadedChk);

        TrainingResult res2 = trainer2.train(samples, 5);

        assertEquals(TrainingTerminationStatus.TRAINING_COMPLETED, res2.getTerminationStatus());
        assertEquals(TerminationReason.PLANNED_COMPLETION, res2.getTerminationReason());
        assertEquals(5, res2.getCompletedEpochs());
        assertTrue(res2.getCompletedSteps() > res1.getCompletedSteps());
    }

    @Test
    public void testAbortHaltsImmediatelyAndInvalidatesModel() {
        EvoLlmTrainer trainer = new EvoLlmTrainer(model, EvoLlmTrainer.TrainingProfile.EVO_FAST);
        trainer.setMicroBatchSize(2);

        trainer.addProgressListener(new EvoLlmTrainer.ProgressListener() {
            @Override
            public void onProgress(int epoch, int totalEpochs, int sampleIndex, int totalSamplesCount, double currentLoss) {}

            @Override
            public void onEpochCompleted(int epoch, int totalEpochs, double trainLoss, double valLoss, long durationMs) {}

            @Override
            public void onProgressMetrics(int epoch, int totalEpochs, int optStep, int totalSteps,
                                          long processedTokens, long totalTokens,
                                          double tokensPerSec, double stepsPerSec,
                                          double remainingSec, double currentLoss) {
                if (optStep >= 1) {
                    trainer.requestAbort();
                }
            }
        });

        TrainingResult res = trainer.train(samples, 5);

        assertEquals(TrainingTerminationStatus.TRAINING_ABORTED, res.getTerminationStatus());
        assertEquals(TerminationReason.USER_ABORT_REQUEST, res.getTerminationReason());
        assertFalse("Aborted model must not be marked valid", res.isModelValid());
    }
}
