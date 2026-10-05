package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.forge.data.api.TrainingSample;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer.TrainingProfile;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;

public class ForgeTrainingFinishAndOptimizationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private EvoLlmModel model;

    @Before
    public void setUp() {
        EvoLlmArchitecture arch = new EvoLlmArchitecture(100, 32, 2, 2, 64, 32);
        model = new EvoLlmModel(arch);
    }

    @Test
    public void testEvoFastProfileLearningRateOptimization() {
        EvoLlmTrainer trainer = new EvoLlmTrainer(model, TrainingProfile.EVO_FAST);
        List<TrainingSample> samples = createDummySamples(20, 16, 100);

        trainer.train(samples, 5);

        List<Double> lossHistory = trainer.getLossHistory();
        assertNotNull("Loss history should not be null", lossHistory);
        assertEquals("Loss history should record all 5 epochs", 5, lossHistory.size());

        double initialLoss = lossHistory.get(0);
        double finalLoss = lossHistory.get(lossHistory.size() - 1);

        assertTrue("Initial loss should be recorded", initialLoss > 0.0);
        assertTrue("Final loss should decrease from initial loss with 1e-3 learning rate", finalLoss < initialLoss);
    }

    @Test
    public void testGracefulStopRequestInTrainer() {
        EvoLlmTrainer trainer = new EvoLlmTrainer(model, TrainingProfile.EVO_FAST);
        List<TrainingSample> samples = createDummySamples(50, 16, 100);

        trainer.setProgressListener((epoch, totalEpochs, sampleIndex, totalSamples, currentLoss) -> {
            if (epoch == 1) {
                trainer.requestStop();
            }
        });

        trainer.train(samples, 10);

        assertTrue("Stop should be requested", trainer.isStopRequested());
        List<Double> lossHistory = trainer.getLossHistory();
        assertTrue("Training should have stopped before completing 10 epochs", lossHistory.size() < 10);
        assertTrue("Weights and loss history from executed epochs should be retained", lossHistory.size() >= 1);
    }

    @Test
    public void testForgeSessionManagerRequestFinish() {
        ForgeSessionManager fsm = ForgeSessionManager.getInstance();
        ForgeSession session = fsm.createSession("Test Finish Session", "SELF_EVO");
        assertNotNull(session);

        EvoLlmTrainer trainer = new EvoLlmTrainer(model);
        fsm.registerActiveTrainer(session.getSessionId(), trainer);

        boolean finishRequested = fsm.requestFinish(session.getSessionId());

        assertTrue("requestFinish should return true when active trainer is registered", finishRequested);
        assertTrue("Active trainer should have stop requested flag set", trainer.isStopRequested());

        fsm.unregisterActiveTrainer(session.getSessionId());
    }

    private List<TrainingSample> createDummySamples(int count, int seqLen, int vocabSize) {
        List<TrainingSample> list = new ArrayList<>();
        java.util.Random rand = new java.util.Random(42L);

        for (int i = 0; i < count; i++) {
            int[] inputs = new int[seqLen];
            int[] labels = new int[seqLen];
            boolean[] mask = new boolean[seqLen];
            float[] att = new float[seqLen];

            for (int t = 0; t < seqLen; t++) {
                inputs[t] = rand.nextInt(vocabSize);
                labels[t] = (inputs[t] + 1) % vocabSize;
                mask[t] = true;
                att[t] = 1.0f;
            }
            list.add(new TrainingSample(inputs, labels, mask, att));
        }
        return list;
    }
}
