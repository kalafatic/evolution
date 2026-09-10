package eu.kalafatic.evolution.forge.controller.service.impl;

import java.io.File;
import java.nio.file.Path;

import eu.kalafatic.evolution.forge.controller.api.ForgeJob;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.ModelConfig;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.PreflightResult;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.TrainingConfig;

public class ForgePreflightValidator {

    public PreflightResult validate(ForgeJob job) {
        PreflightResult result = new PreflightResult();

        if (job == null) {
            result.addError("ForgeJob is null");
            result.setPassed(false);
            return result;
        }

        // 1. Validate Dataset Sources
        if (job.getSourcePaths() == null || job.getSourcePaths().isEmpty()) {
            result.addError("No training sources selected for ForgeJob");
        } else {
            result.addCheck("Training sources present: " + job.getSourcePaths().size() + " source(s)");
        }

        // 2. Validate Model Configuration
        ModelConfig modelConfig = job.getModelConfig();
        if (modelConfig == null) {
            result.addError("Model configuration is missing");
        } else {
            if (modelConfig.getHiddenSize() <= 0 || modelConfig.getHeads() <= 0) {
                result.addError("Invalid model dimensions: hiddenSize=" + modelConfig.getHiddenSize() + ", heads=" + modelConfig.getHeads());
            } else if (modelConfig.getHiddenSize() % modelConfig.getHeads() != 0) {
                result.addError("Hidden size (" + modelConfig.getHiddenSize() + ") must be divisible by attention heads (" + modelConfig.getHeads() + ")");
            } else {
                result.addCheck("Model architecture dimensions valid (hidden=" + modelConfig.getHiddenSize() + ", heads=" + modelConfig.getHeads() + ")");
            }
        }

        // 3. Validate Training Hyperparameters
        TrainingConfig trainingConfig = job.getTrainingConfig();
        if (trainingConfig == null) {
            result.addError("Training configuration is missing");
        } else {
            if (trainingConfig.getLearningRate() <= 0 || trainingConfig.getLearningRate() > 1.0) {
                result.addError("Learning rate out of bounds: " + trainingConfig.getLearningRate());
            } else {
                result.addCheck("Training learning rate valid: " + trainingConfig.getLearningRate());
            }
        }

        // 4. Hardware-Aware Resource Estimation & Optimization
        long freeMemMb = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        long maxMemMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);

        if (modelConfig != null && trainingConfig != null) {
            int h = modelConfig.getHiddenSize();
            int l = modelConfig.getLayers();
            int heads = modelConfig.getHeads();
            int dff = modelConfig.getDff();
            int vocab = modelConfig.getVocabSize();
            int seqLen = modelConfig.getMaxSeqLen();
            int batchSize = Math.max(1, trainingConfig.getBatchSize());

            // Parameter Count Estimation
            long approxParams = (long) vocab * h + (long) l * (4L * h * h + 2L * h * dff) + (long) h * vocab;
            long weightMemBytes = approxParams * 4L;
            long optMemBytes = approxParams * 8L; // AdamW momentum & variance
            long actMemBytes = (long) batchSize * seqLen * h * l * 8L;
            long totalReqBytes = weightMemBytes + optMemBytes + actMemBytes;
            long totalReqMb = totalReqBytes / (1024 * 1024);

            result.addCheck("Hardware Estimation: Params=" + (approxParams / 1_000_000) + "M, Required Memory=" + totalReqMb + "MB (Weights: " + (weightMemBytes / (1024 * 1024)) + "MB, Optimizer: " + (optMemBytes / (1024 * 1024)) + "MB)");

            // If required memory exceeds 80% of max JVM heap, automatically recommend downscaling hyperparameters
            if (totalReqMb > (maxMemMb * 0.85)) {
                result.addCheck("WARNING: Estimated training memory (" + totalReqMb + "MB) exceeds 85% of available JVM heap (" + maxMemMb + "MB). Auto-adjusting batch size and sequence length for safety.");
                trainingConfig.setBatchSize(Math.max(1, batchSize / 2));
                modelConfig.setMaxSeqLen(Math.max(128, seqLen / 2));
            }
        }

        result.addCheck("Runtime memory check: Free=" + freeMemMb + "MB, Max=" + maxMemMb + "MB");

        Path runDir = job.getRunDirectory();
        if (runDir != null) {
            File runFile = runDir.toFile();
            long usableSpaceMb = runFile.getUsableSpace() / (1024 * 1024);
            if (usableSpaceMb < 100) {
                result.addError("Insufficient disk space in run directory: " + usableSpaceMb + "MB available (minimum 100MB required)");
            } else {
                result.addCheck("Disk space check: " + usableSpaceMb + "MB available");
            }
        }

        result.setPassed(result.getErrors().isEmpty());
        return result;
    }
}
