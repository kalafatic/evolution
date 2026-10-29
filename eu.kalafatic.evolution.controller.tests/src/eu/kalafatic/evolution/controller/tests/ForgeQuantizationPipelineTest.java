package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.controller.api.ForgeJob;
import eu.kalafatic.evolution.forge.controller.service.impl.ForgeOrchestratorImpl;
import eu.kalafatic.evolution.forge.model.llm.QuantizationConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.Assert.*;

public class ForgeQuantizationPipelineTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Path projectPath;
    private Path datasetFile;

    @Before
    public void setUp() throws Exception {
        projectPath = tempFolder.newFolder("test-project").toPath();
        datasetFile = tempFolder.newFile("dataset.txt").toPath();
        Files.writeString(datasetFile, "EVO self-development platform dataset for model quantization testing.");
    }

    @Test
    public void testForgeJobWithQuantizationPipeline() throws Exception {
        ForgeJob job = new ForgeJob("quant-test-job");
        job.setSourcePaths(Collections.singletonList(datasetFile.toString()));

        job.getModelConfig().setModelSize("NANO");
        job.getModelConfig().setHiddenSize(128);
        job.getModelConfig().setLayers(2);
        job.getModelConfig().setHeads(2);
        job.getModelConfig().setDff(256);
        job.getModelConfig().setMaxSeqLen(64);
        job.getModelConfig().setVocabSize(256);

        job.getTrainingConfig().setEpochs(1);
        job.getTrainingConfig().setBatchSize(4);
        job.getTrainingConfig().setSaveEpochCheckpoints(false);

        QuantizationConfig qConfig = new QuantizationConfig(QuantizationConfig.QuantizationScheme.F16);
        qConfig.setEnabled(true);
        job.setQuantizationConfig(qConfig);

        ForgeOrchestratorImpl orchestrator = new ForgeOrchestratorImpl();
        ForgeJob completedJob = orchestrator.executeJob(job, projectPath);

        assertNotNull(completedJob);
        assertTrue(completedJob.getState() == ForgeJob.JobState.COMPLETED || completedJob.getState() == ForgeJob.JobState.STOPPED);

        Path runDir = completedJob.getRunDirectory();
        assertNotNull(runDir);
        assertTrue(Files.exists(runDir));

        // Verify quantized artifact was produced in run directory
        boolean foundQuant = false;
        try (var stream = Files.list(runDir)) {
            foundQuant = stream.anyMatch(p -> p.getFileName().toString().contains("-quant-f16.evo"));
        }
        assertTrue("Quantized .evo artifact should be created in run directory", foundQuant);

        // Verify GGUF export was produced
        Path exportDir = projectPath.resolve("dist/" + completedJob.getOutputModelName());
        Path ggufFile = exportDir.resolve("exports/ollama/evo.gguf");
        assertTrue("Exported GGUF should exist", Files.exists(ggufFile));
    }
}
