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

        // Honesty invariants: training is reported as executed ONLY when genuine CUDA
        // kernels ran; otherwise an explicit CPU fallback is reported with no fake
        // epochs/loss/artifact.
        boolean cudaAvailable = eu.kalafatic.evolution.gpu.cuda.CudaDevice.isAvailable();
        if (cudaAvailable) {
            assertTrue("With CUDA available, genuine GPU training must be executed",
                    status.isNativeTrainingExecuted());
            assertEquals("Executed training must report the configured epochs", config.getEpochs(),
                    status.getTotalEpochsCompleted());
            assertTrue("Executed training must report a real loss", status.getFinalLoss() > 0.0f);
            assertTrue("GPU device must be reported", status.getComputeDeviceUsed().contains("GPU"));
        } else {
            assertFalse("Without CUDA, no training may be reported as executed",
                    status.isNativeTrainingExecuted());
            assertEquals("No epochs may be reported from a non-executed training", 0, status.getTotalEpochsCompleted());
        }

        File logFile = new File(status.getLogFilePath());
        assertTrue(logFile.exists());
        assertTrue(logFile.length() > 0);
    }
}
