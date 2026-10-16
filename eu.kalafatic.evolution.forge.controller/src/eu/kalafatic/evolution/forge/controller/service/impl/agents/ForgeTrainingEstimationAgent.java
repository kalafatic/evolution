package eu.kalafatic.evolution.forge.controller.service.impl.agents;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specialized agent responsible for hardware inspection, Transformer FLOPs-based
 * training throughput calculation, recursive dataset sizing, training duration
 * estimation, and smart dataset scaling for target forging time budgets.
 */
public class ForgeTrainingEstimationAgent {

    public interface IDatasetTargetItem {
        boolean isChecked();
        String getPath();
        double getScaleRatio();
        void setScaleRatio(double scaleRatio);
        long getRecordLimit();
        void setRecordLimit(long recordLimit);
    }

    public static class SmartScalingResult {
        private final boolean success;
        private final double scaleFactor;
        private final int updatedEpochs;

        public SmartScalingResult(boolean success, double scaleFactor, int updatedEpochs) {
            this.success = success;
            this.scaleFactor = scaleFactor;
            this.updatedEpochs = updatedEpochs;
        }

        public boolean isSuccess() {
            return success;
        }

        public double getScaleFactor() {
            return scaleFactor;
        }

        public int getUpdatedEpochs() {
            return updatedEpochs;
        }
    }

    private static final Pattern LEADING_NUMBER_PATTERN = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)");
    private static final ForgeTrainingEstimationAgent INSTANCE = new ForgeTrainingEstimationAgent();

    public static ForgeTrainingEstimationAgent getInstance() {
        return INSTANCE;
    }

    public int getCpuCores() {
        return Math.max(1, Runtime.getRuntime().availableProcessors());
    }

    public long getMaxMemoryMb() {
        return Math.max(512, Runtime.getRuntime().maxMemory() / (1024 * 1024));
    }

    public boolean isGpuAvailable() {
        String cudaVis = System.getenv("CUDA_VISIBLE_DEVICES");
        if (cudaVis != null && !cudaVis.trim().isEmpty() && !"-1".equals(cudaVis.trim())) {
            return true;
        }
        String nvidiaVis = System.getenv("NVIDIA_VISIBLE_DEVICES");
        if (nvidiaVis != null && !nvidiaVis.trim().isEmpty() && !"none".equals(nvidiaVis.trim())) {
            return true;
        }
        String evoGpu = System.getProperty("evo.gpu");
        if ("true".equalsIgnoreCase(evoGpu) || "1".equals(evoGpu)) {
            return true;
        }
        try {
            if (new File("/usr/bin/nvidia-smi").exists() ||
                new File("/usr/local/cuda/bin/nvcc").exists() ||
                new File("C:\\Windows\\System32\\nvidia-smi.exe").exists()) {
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public enum BackendType {
        JVM_JAVA_CPU("JVM Native Java CPU Trainer (EvoLlmTrainer)"),
        EXTERNAL_GPU_CUDA("External GPU CUDA Accelerated Engine"),
        UNKNOWN("Unknown Backend");

        private final String description;

        BackendType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public BackendType getActiveBackendType() {
        // Native EvoLlmTrainer runs in-process on JVM CPU
        return BackendType.JVM_JAVA_CPU;
    }

    public String getHardwareProfile() {
        String gpuDetected = isGpuAvailable() ? " | GPU Hardware: Detected" : " | GPU Hardware: None";
        String backend = " | Engine: " + getActiveBackendType().getDescription();
        return String.format("CPU: %d Cores | JVM RAM: %d MB%s%s | OS: %s (%s)",
            getCpuCores(), getMaxMemoryMb(), gpuDetected, backend,
            System.getProperty("os.name"), System.getProperty("os.arch"));
    }

    public double getModelComplexityFactor(String sizeName) {
        if (sizeName == null) return 8.0;
        String s = sizeName.toUpperCase();
        if (s.contains("NANO")) return 1.0;
        if (s.contains("MICRO")) return 2.0;
        if (s.contains("MINI")) return 4.0;
        if (s.contains("SMALL")) return 8.0;
        if (s.contains("MEDIUM")) return 16.0;
        if (s.contains("LARGE") && !s.contains("XLARGE")) return 32.0;
        if (s.contains("XLARGE")) return 64.0;
        return 8.0;
    }

    public double getModelParameterCount(String modelSize) {
        String s = modelSize != null ? modelSize.toUpperCase() : "SMALL";
        if (s.contains("NANO")) return 500_000.0;
        if (s.contains("MICRO")) return 2_000_000.0;
        if (s.contains("MINI")) return 8_000_000.0;
        if (s.contains("SMALL")) return 25_000_000.0;
        if (s.contains("MEDIUM")) return 75_000_000.0;
        if (s.contains("LARGE") && !s.contains("XLARGE")) return 200_000_000.0;
        if (s.contains("XLARGE")) return 500_000_000.0;
        return 25_000_000.0;
    }

    public double getBaselineTokensPerSecond(String modelSize) {
        double params = getModelParameterCount(modelSize);
        int cores = getCpuCores();
        // JVM native pure-Java matrix backpropagation throughput FLOPs estimate.
        // Effective ~0.5 GFLOPs/sec per core on JVM float array operations.
        double flopsPerCore = 500_000_000.0;
        double totalFlops = cores * flopsPerCore * Math.pow(cores, -0.2);
        double flopsPerToken = 6.0 * params;
        double estimatedTokPerSec = totalFlops / flopsPerToken;
        return Math.max(5.0, Math.min(25_000.0, estimatedTokPerSec));
    }

    public double getMeasuredBaselineTokensPerSecond(double measuredTokPerSec) {
        if (Double.isNaN(measuredTokPerSec) || Double.isInfinite(measuredTokPerSec) || measuredTokPerSec <= 0.0) {
            return 0.0;
        }
        return measuredTokPerSec;
    }

    public double calculateRemainingSecondsFromMeasuredThroughput(long remainingTokens, double measuredTokensPerSecond) {
        if (remainingTokens <= 0 || measuredTokensPerSecond <= 0.0) {
            return 0.0;
        }
        return remainingTokens / measuredTokensPerSecond;
    }

    public double parseHoursOption(String textOption) {
        if (textOption == null || textOption.trim().isEmpty()) return 12.0;
        Matcher matcher = LEADING_NUMBER_PATTERN.matcher(textOption.trim());
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (Exception ex) {}
        }
        return 12.0;
    }

    public long calculatePathSize(String path) {
        if (path == null || path.trim().isEmpty()) return 0;
        try {
            File f = new File(path);
            if (!f.exists()) return 0;
            if (f.isFile()) return f.length();
            if (f.isDirectory()) {
                return calculateDirectorySizeRecursive(f);
            }
        } catch (Exception ex) {}
        return 0;
    }

    public long calculateDirectorySizeRecursive(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long total = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File child : files) {
                if (child.isFile()) {
                    total += child.length();
                } else if (child.isDirectory()) {
                    total += calculateDirectorySizeRecursive(child);
                }
            }
        }
        return total;
    }

    public long calculateTotalDatasetBytes(List<? extends IDatasetTargetItem> datasetItems) {
        if (datasetItems == null || datasetItems.isEmpty()) return 0;
        long totalBytes = 0;
        for (IDatasetTargetItem item : datasetItems) {
            if (item != null && item.isChecked() && item.getPath() != null && !item.getPath().trim().isEmpty()) {
                long fullSize = calculatePathSize(item.getPath());
                if (fullSize > 0) {
                    double ratio = item.getScaleRatio() > 0 ? item.getScaleRatio() : 1.0;
                    long scaledSize = (long) (fullSize * ratio);
                    totalBytes += Math.max(1L, scaledSize);
                }
            }
        }
        return totalBytes;
    }

    public long estimateTotalTrainingTokens(List<? extends IDatasetTargetItem> datasetItems, int epochs) {
        long datasetBytes = calculateTotalDatasetBytes(datasetItems);
        if (datasetBytes <= 0 || epochs <= 0) return 0L;
        long tokensPerByte = 4L; // ~1 token per 4 bytes of text/json
        long estimatedDatasetTokens = Math.max(1L, datasetBytes / tokensPerByte);
        long safeEpochs = Math.max(1L, (long) epochs);
        try {
            return Math.multiplyExact(estimatedDatasetTokens, safeEpochs);
        } catch (ArithmeticException ex) {
            return Long.MAX_VALUE;
        }
    }

    public double calculateEstimatedForgingSeconds(String modelSize, int epochs, long datasetBytes) {
        if (datasetBytes <= 0 || epochs <= 0) return 0.0;
        long estimatedDatasetTokens = Math.max(1L, datasetBytes / 4L);
        double totalTokensToTrain = (double) estimatedDatasetTokens * (double) Math.max(1, epochs);
        double tokPerSec = getBaselineTokensPerSecond(modelSize);
        if (tokPerSec <= 0.0 || Double.isNaN(tokPerSec) || Double.isInfinite(tokPerSec)) {
            return 0.0;
        }
        double seconds = totalTokensToTrain / tokPerSec;
        return Double.isNaN(seconds) || Double.isInfinite(seconds) ? 0.0 : seconds;
    }

    public String formatDuration(double seconds) {
        if (Double.isNaN(seconds) || Double.isInfinite(seconds) || seconds <= 0) {
            return "0 seconds";
        } else if (seconds < 60) {
            return String.format("%.0f seconds", seconds);
        } else if (seconds < 3600) {
            long mins = (long) (seconds / 60);
            long secs = (long) (seconds % 60);
            return String.format("%d min %d sec", mins, secs);
        } else if (seconds < 86400) {
            long hours = (long) (seconds / 3600);
            long mins = (long) ((seconds % 3600) / 60);
            return String.format("%d hr %d min", hours, mins);
        } else {
            long days = (long) (seconds / 86400);
            long hours = (long) ((seconds % 86400) / 3600);
            long mins = (long) ((seconds % 3600) / 60);
            return String.format("%d days %d hr %d min", days, hours, mins);
        }
    }

    public SmartScalingResult applySmartDataScaling(List<? extends IDatasetTargetItem> datasetItems, String modelSize, int epochs, double targetHours) {
        if (targetHours <= 0 || Double.isNaN(targetHours) || datasetItems == null || datasetItems.isEmpty()) {
            return new SmartScalingResult(false, 1.0, epochs);
        }

        // Calculate total unscaled full size without compounding previous ratios
        long unscaledTotal = 0;
        for (IDatasetTargetItem item : datasetItems) {
            if (item != null && item.isChecked() && item.getPath() != null) {
                long size = calculatePathSize(item.getPath());
                if (size > 0) {
                    unscaledTotal += size;
                }
            }
        }

        if (unscaledTotal <= 0) {
            return new SmartScalingResult(false, 1.0, epochs);
        }

        double tokPerSec = getBaselineTokensPerSecond(modelSize);
        double maxTokensToTrain = targetHours * 3600.0 * tokPerSec;
        int activeEpochs = Math.max(1, epochs);
        double targetDatasetTokensPerEpoch = maxTokensToTrain / activeEpochs;
        double targetDatasetBytesPerEpoch = targetDatasetTokensPerEpoch * 4.0;

        double scaleFactor = Math.min(1.0, targetDatasetBytesPerEpoch / (double) unscaledTotal);
        if (scaleFactor < 0.05 && activeEpochs > 4) {
            activeEpochs = Math.max(4, activeEpochs / 2);
            targetDatasetTokensPerEpoch = maxTokensToTrain / activeEpochs;
            targetDatasetBytesPerEpoch = targetDatasetTokensPerEpoch * 4.0;
            scaleFactor = Math.min(1.0, targetDatasetBytesPerEpoch / (double) unscaledTotal);
        }

        scaleFactor = Math.max(0.01, Math.min(1.0, scaleFactor));

        for (IDatasetTargetItem item : datasetItems) {
            if (item != null && item.isChecked() && item.getPath() != null) {
                long fullSize = calculatePathSize(item.getPath());
                if (fullSize > 0) {
                    item.setScaleRatio(scaleFactor);
                    long targetItemBytes = (long) (fullSize * scaleFactor);
                    long targetItemTokens = targetItemBytes / 4L;
                    item.setRecordLimit(Math.max(10L, targetItemTokens / 50L));
                }
            }
        }
        return new SmartScalingResult(true, scaleFactor, activeEpochs);
    }
}
