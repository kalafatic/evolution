package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.JobState;
import eu.kalafatic.evolution.forge.controller.service.impl.ForgeOrchestratorImpl;
import eu.kalafatic.evolution.forge.data.api.TrainingSample;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.model.target.EvoModelValidator;
import eu.kalafatic.evolution.forge.model.target.EvoModelValidator.ModelValidationResult;
import eu.kalafatic.evolution.forge.trainer.api.TerminationReason;
import eu.kalafatic.evolution.forge.trainer.api.TrainingResult;
import eu.kalafatic.evolution.forge.trainer.api.TrainingTerminationStatus;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer.TrainingProfile;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class ForgeTrainingFinishAndOptimizationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private EvoLlmModel model;
    private EvoLlmArchitecture arch;

    @Before
    public void setUp() {
        arch = new EvoLlmArchitecture(100, 32, 2, 2, 64, 32);
        model = new EvoLlmModel(arch);

        Orchestrator orch = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orch.setId("test-orch-id");
        ForgeSessionManager.getInstance().initialize(orch);
        Thread.currentThread().setContextClassLoader(ForgeSessionManager.class.getClassLoader());
    }

    @Test
    public void testEvoFastProfileLearningRateOptimization() {
        EvoLlmTrainer trainer = new EvoLlmTrainer(model, TrainingProfile.EVO_FAST);
        List<TrainingSample> samples = createDummySamples(20, 16, 100);

        TrainingResult result = trainer.train(samples, 5);

        assertNotNull("Training result should not be null", result);
        assertEquals(TrainingTerminationStatus.TRAINING_COMPLETED, result.getTerminationStatus());
        assertEquals(TerminationReason.PLANNED_COMPLETION, result.getTerminationReason());
        assertEquals(5, result.getCompletedEpochs());
        assertTrue(result.isModelValid());

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

        TrainingResult result = trainer.train(samples, 10);

        assertTrue("Stop should be requested", trainer.isStopRequested());
        assertNotNull("Result should not be null", result);
        assertEquals(TrainingTerminationStatus.TRAINING_STOPPED, result.getTerminationStatus());
        assertEquals(TerminationReason.USER_REQUEST, result.getTerminationReason());
        assertTrue("Completed epochs should be less than planned 10", result.getCompletedEpochs() < 10);
        assertTrue("Completed epochs should be at least 1", result.getCompletedEpochs() >= 1);
        assertTrue("Model should remain valid after clean early stop boundary", result.isModelValid());
    }

    @Test
    public void testThreadInterruptionInTrainer() throws InterruptedException {
        EvoLlmTrainer trainer = new EvoLlmTrainer(model, TrainingProfile.EVO_FAST);
        List<TrainingSample> samples = createDummySamples(100, 16, 100);

        Thread trainingThread = new Thread(() -> {
            trainer.train(samples, 20);
        });

        trainingThread.start();
        Thread.sleep(50);
        trainingThread.interrupt();
        trainingThread.join(2000);

        TrainingResult result = trainer.getLastTrainingResult();
        assertNotNull("Result should be recorded on trainer", result);
        assertTrue("Termination status should reflect interrupted or stopped",
                result.getTerminationStatus() == TrainingTerminationStatus.TRAINING_INTERRUPTED ||
                result.getTerminationStatus() == TrainingTerminationStatus.TRAINING_STOPPED ||
                result.getTerminationStatus() == TrainingTerminationStatus.TRAINING_COMPLETED);
    }

    @Test
    public void testEmptyDatasetHandling() {
        EvoLlmTrainer trainer = new EvoLlmTrainer(model, TrainingProfile.EVO_FAST);
        TrainingResult result = trainer.train(new ArrayList<>(), 5);

        assertNotNull(result);
        assertEquals(TrainingTerminationStatus.TRAINING_FAILED, result.getTerminationStatus());
        assertEquals(TerminationReason.NO_TRAINING_SAMPLES, result.getTerminationReason());
        assertFalse(result.isModelValid());
    }

    @Test
    public void testModelValidationWithValidModel() {
        EvoModelValidator validator = new EvoModelValidator();
        Map<String, Integer> vocab = createDummyVocab(100);

        ModelValidationResult result = validator.validateModel(model, vocab);

        assertNotNull(result);
        assertTrue("Valid model validation should pass", result.isPassed());
        assertTrue("Parameter count should be positive", result.getParameterCount() > 0);
        assertTrue("Errors list should be empty", result.getErrors().isEmpty());
    }

    @Test
    public void testModelValidationWithNaNWeights() {
        EvoLlmModel badModel = new EvoLlmModel(arch);
        Tensor firstParam = badModel.parameters().iterator().next();
        firstParam.getData()[0] = Float.NaN;

        EvoModelValidator validator = new EvoModelValidator();
        Map<String, Integer> vocab = createDummyVocab(100);

        ModelValidationResult result = validator.validateModel(badModel, vocab);

        assertNotNull(result);
        assertFalse("Model with NaN weights must fail validation", result.isPassed());
        assertTrue("Errors should mention NaN", result.getErrors().stream().anyMatch(e -> e.contains("NaN")));
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

    @Test
    public void testFullForgeJobEarlyStopExportAndMetadata() throws Exception {
        Path projectPath = tempFolder.newFolder("project").toPath();

        ForgeJob job = new ForgeJob("test-job-early-stop");
        job.getSourcePaths().add(projectPath.toString());
        job.getTrainingConfig().setEpochs(100);

        ForgeOrchestratorImpl orchestrator = new ForgeOrchestratorImpl();

        Thread jobThread = new Thread(() -> {
            try {
                orchestrator.executeJob(job, projectPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        jobThread.start();

        long startTime = System.currentTimeMillis();
        boolean stopSent = false;
        while (System.currentTimeMillis() - startTime < 15000) {
            if (ForgeSessionManager.getInstance().requestFinish(job.getJobId())) {
                stopSent = true;
                break;
            }
            Thread.sleep(20);
        }

        jobThread.join(180000);

        assertTrue("Finish request should have been successfully delivered to active trainer", stopSent);
        assertFalse("Job thread should have finished execution", jobThread.isAlive());
        assertNotNull("Training result should be attached to job", job.getTrainingResult());
        assertEquals("Job state should be STOPPED for early stop", JobState.STOPPED, job.getState());
        assertEquals("Job summary should be SUCCESS_WITH_EARLY_STOP", "SUCCESS_WITH_EARLY_STOP", job.getJobResultSummary());

        Path exportedEvo = job.getRunDirectory().resolve(job.getOutputModelName() + ".evo");
        assertTrue("Exported .evo file should exist after early stop with valid model", exportedEvo.toFile().exists());

        EvoModelArtifact loadedArtifact = EvoModelArtifact.load(exportedEvo);
        assertNotNull(loadedArtifact);
        assertEquals("TRAINING_STOPPED", loadedArtifact.getMetadata().get("trainingStatus"));
        assertEquals("USER_REQUEST", loadedArtifact.getMetadata().get("terminationReason"));
        assertEquals("true", loadedArtifact.getMetadata().get("earlyStop"));
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

    private Map<String, Integer> createDummyVocab(int vocabSize) {
        Map<String, Integer> vocab = new HashMap<>();
        for (int i = 0; i < vocabSize; i++) {
            vocab.put("token_" + i, i);
        }
        return vocab;
    }
}
