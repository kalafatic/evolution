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
        config.setModelName("test_gpu_diag");
        config.setWorkDir(workDir.getAbsolutePath());
        config.setOutputDir(outDir.getAbsolutePath());
        config.setEpochs(1);
        config.setBatchSize(2);
        config.setVocabSize(50);
        config.setEmbedDim(16);

        GpuTrainingProcess process = new GpuTrainingProcess();
        GpuTrainingProcess.TrainingStatus status = process.executeTraining(config);

        assertTrue("Diagnostics process must complete", status.isCompleted());
        assertNotNull(status.getComputeDeviceUsed());
        assertNotNull(status.getTechnicalNote());
        assertFalse(status.getTechnicalNote().isEmpty());
        assertNotNull(status.getCudaVerificationLabel());

        // Honesty invariants: no fake training, no fake model artifact.
        assertFalse("Native training must never be reported as executed without real kernels",
                status.isNativeTrainingExecuted());
        assertEquals("No epochs may be reported from a non-executed training", 0, status.getTotalEpochsCompleted());
        assertNotNull(status.getModelArtifactPath());
        assertTrue("No model artifact may be produced by a diagnostics-only run",
                status.getModelArtifactPath().isEmpty());

        File logFile = new File(status.getLogFilePath());
        assertTrue(logFile.exists());
        assertTrue(logFile.length() > 0);
    }
}
