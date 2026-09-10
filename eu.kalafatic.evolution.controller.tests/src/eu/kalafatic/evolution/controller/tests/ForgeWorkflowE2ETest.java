package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.forge.controller.api.ForgeJob;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.JobState;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.ModelObjective;
import eu.kalafatic.evolution.forge.controller.service.ForgeOrchestrator;
import eu.kalafatic.evolution.forge.controller.service.impl.ForgeOrchestratorImpl;

public class ForgeWorkflowE2ETest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Path projectDir;
    private Path dataDir;
    private Path sampleFile1;
    private Path sampleFile2;

    @Before
    public void setUp() throws Exception {
        projectDir = tempFolder.newFolder("project").toPath();
        dataDir = tempFolder.newFolder("data").toPath();

        sampleFile1 = dataDir.resolve("knowledge.txt");
        Files.writeString(sampleFile1, "EVO Forge is an intelligent LLM training pipeline built for deep self-evolution.");

        sampleFile2 = dataDir.resolve("code.py");
        Files.writeString(sampleFile2, "def forge_model():\n    print('Forging EVO native model...')\n");
    }

    @Test
    public void testFullForgeWorkflowE2E() throws Exception {
        ForgeJob job = new ForgeJob("e2e-test-session");
        job.setObjective(ModelObjective.CODING);
        job.setSourcePaths(List.of(
            sampleFile1.toAbsolutePath().toString(),
            sampleFile2.toAbsolutePath().toString(),
            dataDir.toAbsolutePath().toString()
        ));

        // Use Nano size for fast test execution
        job.getModelConfig().setModelSize("NANO");
        job.getModelConfig().setHiddenSize(128);
        job.getModelConfig().setLayers(2);
        job.getModelConfig().setHeads(4);
        job.getModelConfig().setDff(256);
        job.getModelConfig().setMaxSeqLen(128);
        job.getTrainingConfig().setEpochs(1);

        ForgeOrchestrator orchestrator = new ForgeOrchestratorImpl();
        ForgeJob completedJob = orchestrator.executeJob(job, projectDir);

        assertNotNull(completedJob);
        assertEquals(JobState.COMPLETED, completedJob.getState());
        assertTrue("Source profiles should be populated", completedJob.getSourceProfiles().size() >= 2);
        assertTrue("Preflight check should pass", completedJob.getPreflightResult().isPassed());
        assertTrue("Smoke test should pass", completedJob.getSmokeTestResult().isPassed());
        assertNotNull("Generated text should not be null", completedJob.getSmokeTestResult().getGeneratedText());
    }
}
