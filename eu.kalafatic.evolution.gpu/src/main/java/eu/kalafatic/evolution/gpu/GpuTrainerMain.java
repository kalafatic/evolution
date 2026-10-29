package eu.kalafatic.evolution.gpu;

import eu.kalafatic.evolution.gpu.trainer.GpuTrainerConfig;
import eu.kalafatic.evolution.gpu.trainer.GpuTrainingProcess;

import java.io.File;

/**
 * Standalone CLI entry point for isolated GPU model training process.
 */
public class GpuTrainerMain {

    public static void main(String[] args) {
        System.out.println("[EVO-GPU-TRAINER] Initializing isolated GPU Model Training Process...");

        String configPath = null;
        String workDirPath = "gpu-work";
        String outputDirPath = "gpu-output";

        for (int i = 0; i < args.length; i++) {
            if ("--config".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                configPath = args[i + 1];
            } else if ("--work-dir".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                workDirPath = args[i + 1];
            } else if ("--output-dir".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                outputDirPath = args[i + 1];
            }
        }

        try {
            GpuTrainerConfig config;
            if (configPath != null && new File(configPath).exists()) {
                System.out.println("[EVO-GPU-TRAINER] Loading configuration from: " + configPath);
                config = GpuTrainerConfig.load(new File(configPath));
            } else {
                System.out.println("[EVO-GPU-TRAINER] Using default GPU training configuration.");
                config = new GpuTrainerConfig();
            }

            config.setWorkDir(workDirPath);
            config.setOutputDir(outputDirPath);

            GpuTrainingProcess process = new GpuTrainingProcess();
            GpuTrainingProcess.TrainingStatus status = process.executeTraining(config);

            System.out.println("\n=========================================================");
            System.out.println("          EVO GPU TRAINING PROCESS SUMMARY               ");
            System.out.println("=========================================================");
            System.out.println("Status          : " + (status.isCompleted() ? "SUCCESS" : "FAILED"));
            System.out.println("Compute Device  : " + status.getComputeDeviceUsed());
            System.out.println("Epochs Completed: " + status.getTotalEpochsCompleted());
            System.out.println("Final Loss      : " + status.getFinalLoss());
            System.out.println("Duration        : " + status.getDurationMs() + " ms");
            System.out.println("Model Artifact  : " + status.getModelArtifactPath());
            System.out.println("Execution Log   : " + status.getLogFilePath());
            System.out.println("=========================================================");

        } catch (Exception e) {
            System.err.println("[EVO-GPU-TRAINER] Error executing GPU training process: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
