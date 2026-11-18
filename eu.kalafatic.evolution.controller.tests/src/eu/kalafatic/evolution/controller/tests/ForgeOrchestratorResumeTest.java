package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.JobState;
import eu.kalafatic.evolution.forge.controller.service.impl.ForgeOrchestratorImpl;
import eu.kalafatic.evolution.forge.trainer.api.TrainingResult;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Orchestrator-level checkpoint resume integration: proves that a second
 * execution of the same forge session discovers the fingerprint-matching
 * checkpoint in the stable per-session run folder and continues training
 * (and that a dataset change starts fresh instead).
 */
public class ForgeOrchestratorResumeTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ForgeJob smallJob(String jobId, Path projectPath, int epochs, int checkpointFreq) {
        ForgeJob job = new ForgeJob(jobId);
        job.getSourcePaths().add(projectPath.toString());
        job.getTrainingConfig().setEpochs(epochs);
        job.getTrainingConfig().setCheckpointFrequencyEpochs(checkpointFreq);
        job.getTrainingConfig().setSaveEpochCheckpoints(true);
        job.getTrainingConfig().setEarlyStoppingEnabled(false);
        job.getTrainingConfig().setLearningRate(0.01);
        job.getTrainingConfig().setBatchSize(4);
        job.getTrainingConfig().setValidationRatio(0.1);
        job.getModelConfig().setHiddenSize(64);
        job.getModelConfig().setLayers(2);
        job.getModelConfig().setHeads(4);
        job.getModelConfig().setDff(128);
        job.getModelConfig().setMaxSeqLen(64);
        job.getModelConfig().setVocabSize(200);
        job.getQuantizationConfig().setEnabled(false);
        return job;
    }

    @Test
    public void testSecondExecutionResumesFromCheckpointInStableRunFolder() throws Exception {
        Path projectPath = tempFolder.newFolder("resume-project").toPath();
        Path corpusFile = projectPath.resolve("corpus.txt");
        StringBuilder corpus = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            corpus.append("The evolution genome platform trains local models on prepared datasets and exports them to ollama. ");
        }
        Files.writeString(corpusFile, corpus.toString());

        ForgeSessionManager.getInstance().initialize(OrchestrationFactory.eINSTANCE.createOrchestrator());
        Orchestrator orch = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orch.setId("resume-test-orch");
        ForgeSessionManager.getInstance().initialize(orch);

        ForgeOrchestratorImpl orchestrator = new ForgeOrchestratorImpl();

        // Run 1: 2 epochs, checkpoint every epoch.
        ForgeJob job1 = smallJob("resume-job", projectPath, 2, 1);
        orchestrator.executeJob(job1, projectPath);
        assertEquals(JobState.COMPLETED, job1.getState());
        assertEquals(2, job1.getTrainingResult().getCompletedEpochs());

        // The run folder is stable per session; the epoch checkpoint must exist.
        Path checkpointsDir = job1.getRunDirectory().resolve("checkpoints");
        assertTrue("Checkpoints directory must exist", Files.isDirectory(checkpointsDir));
        boolean checkpointFound;
        try (var stream = Files.list(checkpointsDir)) {
            checkpointFound = stream.anyMatch(p -> p.getFileName().toString().startsWith("checkpoint_resume-job_")
                    && p.getFileName().toString().endsWith(".chk"));
        }
        assertTrue("Epoch checkpoint must be written into the stable run folder", checkpointFound);

        // Run 2: same session, 4 epochs → must resume from epoch 2.
        ForgeJob job2 = smallJob("resume-job", projectPath, 4, 1);
        orchestrator.executeJob(job2, projectPath);
        assertEquals(JobState.COMPLETED, job2.getState());
        TrainingResult result2 = job2.getTrainingResult();
        assertNotNull(result2);
        assertEquals("RESUMED", result2.getMetadata().get("trainingMode"));
        assertEquals(4, result2.getCompletedEpochs());
    }

    @Test
    public void testDatasetChangeStartsFreshInsteadOfResumingWrongCheckpoint() throws Exception {
        Path projectPath = tempFolder.newFolder("fingerprint-project").toPath();
        Path corpusFile = projectPath.resolve("corpus.txt");
        Files.writeString(corpusFile, "Original dataset content used for the first training run. ".repeat(40));

        ForgeSessionManager.getInstance().initialize(OrchestrationFactory.eINSTANCE.createOrchestrator());
        Orchestrator orch = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orch.setId("fp-test-orch");
        ForgeSessionManager.getInstance().initialize(orch);

        ForgeOrchestratorImpl orchestrator = new ForgeOrchestratorImpl();

        ForgeJob job1 = smallJob("fp-job", projectPath, 2, 1);
        orchestrator.executeJob(job1, projectPath);
        assertEquals(JobState.COMPLETED, job1.getState());

        // Change the dataset content: the .evodata fingerprint changes.
        Files.writeString(corpusFile, "Completely different corpus material for the second run. ".repeat(60));

        ForgeJob job2 = smallJob("fp-job", projectPath, 2, 1);
        orchestrator.executeJob(job2, projectPath);
        assertEquals(JobState.COMPLETED, job2.getState());
        TrainingResult result2 = job2.getTrainingResult();
        assertNotNull(result2);
        assertEquals("FROM_SCRATCH", result2.getMetadata().get("trainingMode"));
        assertEquals(2, result2.getCompletedEpochs());
    }
}
