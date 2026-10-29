package eu.kalafatic.evolution.gpu.trainer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.*;

public class GpuTrainingProcessTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testExecuteTrainingIsolated() throws Exception {
        File workDir = tempFolder.newFolder("work");
        File outDir = tempFolder.newFolder("output");

        GpuTrainerConfig config = new GpuTrainerConfig();
        config.setModelName("test_gpu_train");
        config.setWorkDir(workDir.getAbsolutePath());
        config.setOutputDir(outDir.getAbsolutePath());
        config.setEpochs(1);
        config.setBatchSize(2);
        config.setVocabSize(50);
        config.setEmbedDim(16);

        GpuTrainingProcess process = new GpuTrainingProcess();
        GpuTrainingProcess.TrainingStatus status = process.executeTraining(config);

        assertTrue(status.isCompleted());
        assertEquals(1, status.getTotalEpochsCompleted());
        assertNotNull(status.getComputeDeviceUsed());
        assertNotNull(status.getModelArtifactPath());

        File modelFile = new File(status.getModelArtifactPath());
        assertTrue(modelFile.exists());
        assertTrue(modelFile.length() > 0);

        File logFile = new File(status.getLogFilePath());
        assertTrue(logFile.exists());
    }
}
