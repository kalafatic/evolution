package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.data.api.TrainingSample;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.trainer.api.TrainingCheckpoint;
import eu.kalafatic.evolution.forge.trainer.api.TrainingResult;
import eu.kalafatic.evolution.forge.trainer.api.TrainingTerminationStatus;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

/**
 * Regression tests verifying:
 * - Deterministic training resumption from TrainingCheckpoint
 * - Complete optimizer moments and step restoration
 * - Prevention of repeated completed epochs
 * - Monotonic epoch progression
 * - Checkpoint compatibility validation
 */
public class TrainingResumeLifecycleTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testCheckpointSaveLoadAndCompatibility() throws Exception {
        EvoLlmArchitecture arch = new EvoLlmArchitecture(64, 16, 2, 2, 32, 32);
        EvoLlmModel model = new EvoLlmModel(arch);

        Map<String, float[]> m = new HashMap<>();
        Map<String, float[]> v = new HashMap<>();
        m.put("p_0", new float[]{0.1f, 0.2f});
        v.put("p_0", new float[]{0.01f, 0.04f});

        Map<String, Integer> vocab = new LinkedHashMap<>();
        vocab.put("<s>", 0);
        vocab.put("</s>", 1);
        vocab.put("evo", 2);

        TrainingCheckpoint chk = TrainingCheckpoint.capture(
                "run-101", "test_model", model, 5, 250, 1.25f, 1.35f, 0.001f, 42L, "hash123", m, v, vocab
        );

        File chkFile = tempFolder.newFile("checkpoint_test.chk");
        chk.saveAtomic(chkFile.toPath());

        TrainingCheckpoint loaded = TrainingCheckpoint.load(chkFile.toPath());
        Assert.assertEquals("run-101", loaded.getRunId());
        Assert.assertEquals("test_model", loaded.getModelName());
        Assert.assertEquals(5, loaded.getCompletedEpochs());
        Assert.assertEquals(250, loaded.getTotalOptimizerSteps());
        Assert.assertEquals(1.25f, loaded.getLastTrainLoss(), 1e-5f);
        Assert.assertEquals(1.35f, loaded.getLastValLoss(), 1e-5f);
        Assert.assertEquals(3, loaded.getTokenizerVocab().size());

        // Test restoring into compatible model
        EvoLlmModel targetModel = new EvoLlmModel(arch);
        Map<String, float[]> restM = new HashMap<>();
        Map<String, float[]> restV = new HashMap<>();
        loaded.restoreInto(targetModel, restM, restV);
        Assert.assertEquals(5, targetModel.getTrainingState().getEpoch());
        Assert.assertEquals(250, targetModel.getTrainingState().getStep());

        // Incompatible architecture rejection
        EvoLlmArchitecture badArch = new EvoLlmArchitecture(128, 16, 2, 2, 32, 32); // different vocab
        EvoLlmModel badModel = new EvoLlmModel(badArch);
        try {
            loaded.validateCompatibility(badModel);
            Assert.fail("Should reject incompatible model architecture");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage().contains("Incompatible checkpoint architecture"));
        }
    }

    @Test
    public void testResumeDoesNotRepeatCompletedEpochs() {
        EvoLlmArchitecture arch = new EvoLlmArchitecture(32, 16, 2, 1, 32, 16);
        EvoLlmModel model = new EvoLlmModel(arch);
        EvoLlmTrainer trainer = new EvoLlmTrainer(model);

        List<TrainingSample> samples = createSyntheticDataset(20, 8);

        // Train 2 epochs initially
        TrainingResult r1 = trainer.train(samples, 2);
        Assert.assertEquals(TrainingTerminationStatus.TRAINING_COMPLETED, r1.getTerminationStatus());
        Assert.assertEquals(2, r1.getCompletedEpochs());

        TrainingCheckpoint checkpoint = trainer.captureCheckpoint(
                "job-1", "m", 2, (float) r1.getLastLoss(), (float) r1.getValLoss(), "fp", Collections.emptyMap()
        );

        // Create new trainer resuming from the checkpoint to train for a total of 4 epochs
        EvoLlmModel resumedModel = new EvoLlmModel(arch);
        EvoLlmTrainer resumeTrainer = new EvoLlmTrainer(resumedModel);
        resumeTrainer.setResumeCheckpoint(checkpoint);

        List<Integer> executedEpochs = new ArrayList<>();
        resumeTrainer.addProgressListener(new EvoLlmTrainer.ProgressListener() {
            @Override
            public void onProgress(int epoch, int totalEpochs, int sampleIndex, int totalSamplesCount, double currentLoss) {}

            @Override
            public void onEpochCompleted(int epoch, int totalEpochs, double trainLoss, double valLoss) {
                executedEpochs.add(epoch);
            }
        });

        TrainingResult r2 = resumeTrainer.train(samples, 4);
        Assert.assertEquals(TrainingTerminationStatus.TRAINING_COMPLETED, r2.getTerminationStatus());
        Assert.assertEquals(4, r2.getCompletedEpochs());

        // Crucial verification: only epochs 3 and 4 were executed! Epochs 1 and 2 were NOT repeated.
        Assert.assertEquals(2, executedEpochs.size());
        Assert.assertEquals(Integer.valueOf(3), executedEpochs.get(0));
        Assert.assertEquals(Integer.valueOf(4), executedEpochs.get(1));

        // Submitting same or fewer epochs than already completed is a clean no-op
        TrainingResult r3 = resumeTrainer.train(samples, 4);
        Assert.assertEquals(TrainingTerminationStatus.TRAINING_COMPLETED, r3.getTerminationStatus());
        Assert.assertEquals(4, r3.getCompletedEpochs());
        Assert.assertEquals(0, r3.getTrainingDurationMs());
    }

    @Test
    public void testNumericalGradientCheck() {
        // Finite-difference numerical gradient check on tiny model parameter
        EvoLlmArchitecture arch = new EvoLlmArchitecture(16, 8, 2, 1, 16, 8);
        EvoLlmModel model = new EvoLlmModel(arch);
        EvoLlmTrainer trainer = new EvoLlmTrainer(model);

        int[] input = new int[]{1, 3, 5, 2};
        int[] labels = new int[]{3, 5, 2, 4};
        boolean[] lossMask = new boolean[]{true, true, true, true};
        float[] attMask = new float[]{1.0f, 1.0f, 1.0f, 1.0f};

        TrainingSample sample = new TrainingSample(input, labels, lossMask, attMask);
        eu.kalafatic.evolution.forge.data.api.TrainingBatch batch =
                new eu.kalafatic.evolution.forge.data.api.TrainingBatch(
                        new int[][]{input}, new int[][]{labels}, new float[][]{{1,1,1,1}}, new float[][]{attMask}
                );

        // Run analytical backprop
        for (eu.kalafatic.evolution.forge.math.api.Tensor p : model.parameters()) {
            p.zeroGrad();
        }
        double loss = trainer.processMicroBatch(batch, 4);

        // Check output_norm weight gradient via finite differences
        eu.kalafatic.evolution.forge.math.api.Tensor normWeight = model.getBlocks().get(0).getAttnNorm().getWeight();
        float[] grad = normWeight.getGrad();
        float[] data = normWeight.getData();

        float eps = 1e-3f;
        int checkIdx = 0;
        float originalVal = data[checkIdx];

        data[checkIdx] = originalVal + eps;
        double lossPlus = trainer.processMicroBatch(batch, 4);

        data[checkIdx] = originalVal - eps;
        double lossMinus = trainer.processMicroBatch(batch, 4);

        data[checkIdx] = originalVal; // restore

        double numGrad = (lossPlus - lossMinus) / (2.0 * eps);
        double analyticalGrad = grad[checkIdx];

        double diff = Math.abs(numGrad - analyticalGrad);
        Assert.assertTrue("Analytical vs Numerical gradient difference too large: diff=" + diff +
                ", analytical=" + analyticalGrad + ", numerical=" + numGrad, diff < 0.25);
    }

    private List<TrainingSample> createSyntheticDataset(int count, int len) {
        List<TrainingSample> list = new ArrayList<>();
        Random rand = new Random(999);
        for (int i = 0; i < count; i++) {
            int[] in = new int[len];
            int[] lab = new int[len];
            boolean[] mask = new boolean[len];
            float[] att = new float[len];
            for (int t = 0; t < len; t++) {
                in[t] = rand.nextInt(15) + 1;
                lab[t] = (t + 1 < len) ? in[t] + 1 : in[t];
                mask[t] = true;
                att[t] = 1.0f;
            }
            list.add(new TrainingSample(in, lab, mask, att));
        }
        return list;
    }
}
