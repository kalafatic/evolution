package eu.kalafatic.evolution.forge.trainer.impl.llm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;
import org.json.JSONObject;

public class TrainingJobStateStore {

    private final Path jobFolder;
    private long lastErrorLogTime = 0;
    private static final long ERROR_LOG_COOLDOWN_MS = 30000; // 30s rate-limit for persistent errors

    public TrainingJobStateStore(Path jobFolder) {
        this.jobFolder = jobFolder;
    }

    public synchronized void save(TrainingProgress progress) {
        if (progress == null) return;
        Path stateFile = jobFolder.resolve("training-state.json");
        Path tempFile = jobFolder.resolve("training-state." + UUID.randomUUID() + ".tmp");

        try {
            Files.createDirectories(jobFolder);

            JSONObject json = new JSONObject();
            json.put("jobId", progress.getJobId());
            json.put("status", progress.getStatus().name());
            json.put("phase", progress.getPhase());
            json.put("currentEpoch", progress.getCurrentEpoch());
            json.put("totalEpochs", progress.getTotalEpochs());
            json.put("currentStep", progress.getCurrentStep());
            json.put("totalSteps", progress.getTotalSteps());
            json.put("currentBatch", progress.getCurrentBatch());
            json.put("batchesPerEpoch", progress.getBatchesPerEpoch());
            json.put("progressPercent", progress.getProgressPercent());
            json.put("trainingLoss", progress.getTrainingLoss());
            json.put("validationLoss", progress.getValidationLoss());
            json.put("learningRate", progress.getLearningRate());
            json.put("stepsPerSecond", progress.getStepsPerSecond());
            json.put("elapsedMillis", progress.getElapsedMillis());
            json.put("estimatedRemainingMillis", progress.getEstimatedRemainingMillis());
            json.put("lastUpdated", progress.getTimestamp().toString());
            json.put("message", progress.getMessage());

            // Write to temporary file with UTF-8
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                writer.write(json.toString(2));
                writer.flush();
            }

            // Atomic move fallback
            try {
                Files.move(tempFile, stateFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(tempFile, stateFile, StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (Exception e) {
            logError("Failed to save training state for job: " + progress.getJobId(), e);
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}
        }
    }

    public synchronized void appendMetrics(TrainingProgress progress) {
        if (progress == null) return;
        Path metricsFile = jobFolder.resolve("metrics.jsonl");

        try {
            Files.createDirectories(jobFolder);

            JSONObject json = new JSONObject();
            json.put("timestamp", progress.getTimestamp().toString());
            json.put("epoch", progress.getCurrentEpoch());
            json.put("step", progress.getCurrentStep());
            json.put("loss", progress.getTrainingLoss());
            json.put("validationLoss", progress.getValidationLoss());
            json.put("stepsPerSecond", progress.getStepsPerSecond());

            // Append with UTF-8
            try (BufferedWriter writer = Files.newBufferedWriter(metricsFile, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
                writer.write(json.toString());
                writer.newLine();
                writer.flush();
            }
        } catch (Exception e) {
            logError("Failed to append metrics for job: " + progress.getJobId(), e);
        }
    }

    public static TrainingProgress load(Path jobFolder) {
        Path stateFile = jobFolder.resolve("training-state.json");
        if (!Files.exists(stateFile)) {
            return null;
        }

        try (BufferedReader reader = Files.newBufferedReader(stateFile, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            JSONObject json = new JSONObject(sb.toString());
            String jobId = json.optString("jobId", "");
            TrainingStatus status = TrainingStatus.valueOf(json.optString("status", "QUEUED"));
            String phase = json.optString("phase", "");
            int currentEpoch = json.optInt("currentEpoch", 0);
            int totalEpochs = json.optInt("totalEpochs", 0);
            long currentStep = json.optLong("currentStep", 0L);
            long totalSteps = json.optLong("totalSteps", 0L);
            long currentBatch = json.optLong("currentBatch", 0L);
            long batchesPerEpoch = json.optLong("batchesPerEpoch", 0L);
            double progressPercent = json.optDouble("progressPercent", 0.0);
            double trainingLoss = json.optDouble("trainingLoss", 0.0);
            double validationLoss = json.optDouble("validationLoss", 0.0);
            double learningRate = json.optDouble("learningRate", 0.0);
            double stepsPerSecond = json.optDouble("stepsPerSecond", 0.0);
            long elapsedMillis = json.optLong("elapsedMillis", 0L);
            long estimatedRemainingMillis = json.optLong("estimatedRemainingMillis", 0L);
            Instant timestamp = Instant.parse(json.optString("lastUpdated", Instant.now().toString()));
            String message = json.optString("message", "");

            return new TrainingProgress(
                    jobId,
                    status,
                    phase,
                    currentEpoch,
                    totalEpochs,
                    currentStep,
                    totalSteps,
                    currentBatch,
                    batchesPerEpoch,
                    progressPercent,
                    trainingLoss,
                    validationLoss,
                    learningRate,
                    stepsPerSecond,
                    elapsedMillis,
                    estimatedRemainingMillis,
                    timestamp,
                    message
            );
        } catch (Exception e) {
            System.err.println("[TrainingJobStateStore] Failed to load state from " + stateFile + ": " + e.getMessage());
            return null;
        }
    }

    private void logError(String message, Throwable t) {
        long now = System.currentTimeMillis();
        if (now - lastErrorLogTime > ERROR_LOG_COOLDOWN_MS) {
            System.err.println("[TrainingJobStateStore] ERROR: " + message + ". Exception: " + t.getMessage());
            t.printStackTrace();
            lastErrorLogTime = now;
        }
    }
}
