package eu.kalafatic.evolution.gpu.trainer;

import eu.kalafatic.evolution.gpu.diag.CudaComputeVerifier;
import eu.kalafatic.evolution.gpu.diag.NvidiaGpuDetector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Isolated GPU capability verification process.
 *
 * Native CUDA training kernels are NOT implemented in this module. Per the EVO
 * development rules (no fake GPU execution, no fake training progress), this
 * process never simulates training and never produces a model artifact from a
 * fabricated loop. It performs real GPU/CUDA diagnostics and reports the
 * truthful execution capability: the only available training backend today is
 * the Darwin Forge CPU trainer (EvoLlmTrainer).
 */
public class GpuTrainingProcess {

    public static class TrainingStatus {
        private boolean completed;
        private String computeDeviceUsed = "CPU_FALLBACK";
        private String technicalNote = "";
        private int totalEpochsCompleted;
        private float finalLoss;
        private long durationMs;
        private String modelArtifactPath;
        private String logFilePath;
        private boolean nativeTrainingExecuted;
        private String gpuDeviceName = "";
        private String cudaVerificationLabel = "";

        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }

        public String getComputeDeviceUsed() { return computeDeviceUsed; }
        public void setComputeDeviceUsed(String computeDeviceUsed) { this.computeDeviceUsed = computeDeviceUsed; }

        public String getTechnicalNote() { return technicalNote; }
        public void setTechnicalNote(String technicalNote) { this.technicalNote = technicalNote; }

        public int getTotalEpochsCompleted() { return totalEpochsCompleted; }
        public void setTotalEpochsCompleted(int totalEpochsCompleted) { this.totalEpochsCompleted = totalEpochsCompleted; }

        public float getFinalLoss() { return finalLoss; }
        public void setFinalLoss(float finalLoss) { this.finalLoss = finalLoss; }

        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

        public String getModelArtifactPath() { return modelArtifactPath; }
        public void setModelArtifactPath(String modelArtifactPath) { this.modelArtifactPath = modelArtifactPath; }

        public String getLogFilePath() { return logFilePath; }
        public void setLogFilePath(String logFilePath) { this.logFilePath = logFilePath; }

        public boolean isNativeTrainingExecuted() { return nativeTrainingExecuted; }
        public void setNativeTrainingExecuted(boolean nativeTrainingExecuted) { this.nativeTrainingExecuted = nativeTrainingExecuted; }

        public String getGpuDeviceName() { return gpuDeviceName; }
        public void setGpuDeviceName(String gpuDeviceName) { this.gpuDeviceName = gpuDeviceName; }

        public String getCudaVerificationLabel() { return cudaVerificationLabel; }
        public void setCudaVerificationLabel(String cudaVerificationLabel) { this.cudaVerificationLabel = cudaVerificationLabel; }
    }

    /**
     * Runs GPU capability diagnostics. Training is never executed: no native
     * CUDA training backend exists yet, so no model artifact is produced and
     * no simulated loss/epoch progression is reported.
     */
    public TrainingStatus executeTraining(GpuTrainerConfig config) throws IOException {
        long startTime = System.currentTimeMillis();
        TrainingStatus status = new TrainingStatus();

        File workDir = new File(config.getWorkDir());
        File outDir = new File(config.getOutputDir());
        if (!workDir.exists()) workDir.mkdirs();
        if (!outDir.exists()) outDir.mkdirs();

        File logFile = new File(outDir, "gpu_diagnostics.log");
        status.setLogFilePath(logFile.getAbsolutePath());

        try (FileWriter logWriter = new FileWriter(logFile, false)) {
            log(logWriter, "=========================================================");
            log(logWriter, "        EVO GPU CAPABILITY DIAGNOSTICS PROCESS           ");
            log(logWriter, "=========================================================");
            log(logWriter, "Model Name : " + config.getModelName());
            log(logWriter, "Work Dir   : " + workDir.getAbsolutePath());
            log(logWriter, "Output Dir : " + outDir.getAbsolutePath());

            NvidiaGpuDetector detector = new NvidiaGpuDetector();
            NvidiaGpuDetector.GpuDeviceInfo gpuDev = detector.detectGpu();

            CudaComputeVerifier verifier = new CudaComputeVerifier();
            CudaComputeVerifier.CudaComputeInfo cudaComp = verifier.verifyCudaCompute();

            status.setGpuDeviceName(gpuDev.isAvailable() ? gpuDev.getDeviceName() : "NONE");
            status.setCudaVerificationLabel(cudaComp.getComputeDeviceType());

            String deviceType = "CPU_HOST_JAVA";
            StringBuilder note = new StringBuilder();

            if (gpuDev.isAvailable() && cudaComp.isComputeCapable()) {
                deviceType = "CPU_HOST_JAVA (CUDA DRIVER PRESENT)";
                note.append("NVIDIA GPU device (").append(gpuDev.getDeviceName())
                        .append(") detected and CUDA driver libraries located. ")
                        .append("Native CUDA training kernels are NOT implemented in this module; ")
                        .append("no training was executed and no model artifact was produced. ")
                        .append("Use the Darwin Forge CPU trainer (EvoLlmTrainer) for actual training.");
            } else {
                deviceType = "CPU_FALLBACK";
                note.append("NVIDIA GPU or CUDA driver libraries unavailable. ");
                if (!gpuDev.isAvailable()) {
                    note.append("Reason: NVIDIA GPU device was not detected via nvidia-smi. ");
                } else {
                    note.append("Reason: CUDA driver libraries not located. ");
                }
                note.append("No training was executed and no model artifact was produced.");
            }

            status.setComputeDeviceUsed(deviceType);
            status.setTechnicalNote(note.toString());
            status.setTotalEpochsCompleted(0);
            status.setFinalLoss(0.0f);
            status.setNativeTrainingExecuted(false);
            status.setModelArtifactPath("");
            status.setCompleted(true);

            log(logWriter, "\n[COMPUTE DEVICE] " + deviceType);
            log(logWriter, "[GPU DEVICE] " + status.getGpuDeviceName());
            log(logWriter, "[CUDA VERIFICATION] " + status.getCudaVerificationLabel());
            log(logWriter, "[TECHNICAL NOTE] " + note);
            log(logWriter, "\n[RESULT] Diagnostics completed. Native training NOT executed (kernels not implemented).");

            status.setDurationMs(System.currentTimeMillis() - startTime);
            log(logWriter, "\n[STATUS] Diagnostics process completed in " + status.getDurationMs() + " ms.");
        }

        return status;
    }

    private void log(FileWriter writer, String message) throws IOException {
        String line = "[" + java.time.Instant.now() + "] " + message;
        System.out.println(line);
        writer.write(line + "\n");
        writer.flush();
    }
}
