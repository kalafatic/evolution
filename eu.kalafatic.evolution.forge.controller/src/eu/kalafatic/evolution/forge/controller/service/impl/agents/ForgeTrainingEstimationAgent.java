package eu.kalafatic.evolution.forge.controller.service.impl.agents;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Specialized agent responsible for hardware inspection, dynamic model capacity sizing,
 * Transformer FLOPs-based training throughput calculation, recursive dataset sizing,
 * training duration estimation, and smart dataset scaling for target forging time budgets.
 *
 * Throughput policy: measured training runs are recorded (recordTrainingMeasurement) and
 * preferred for all time estimates. When no measurement exists for the requested model
 * size, a clearly-labeled ANALYTICAL PRELIMINARY estimate is returned instead — never a
 * fabricated "measured" value.
 */
public class ForgeTrainingEstimationAgent {

    private static final int MAX_MEASUREMENTS_PER_SIZE = 50;
    private static final String MEASUREMENT_FILE_NAME = "forge-throughput-measurements.json";

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

    /**
     * One measured training run. All values originate from real trainer execution
     * (TrainingResult metadata), never from simulation.
     */
    public static class ThroughputMeasurement {
        public final long recordedAtMs;
        public final String modelSize;
        public final double tokensPerSec;
        public final double stepsPerSec;
        public final long durationMs;

        public ThroughputMeasurement(long recordedAtMs, String modelSize, double tokensPerSec, double stepsPerSec, long durationMs) {
            this.recordedAtMs = recordedAtMs;
            this.modelSize = modelSize == null ? "" : modelSize.trim().toUpperCase();
            this.tokensPerSec = tokensPerSec;
            this.stepsPerSec = stepsPerSec;
            this.durationMs = durationMs;
        }

        public JSONObject toJsonObject() {
            JSONObject json = new JSONObject();
            json.put("recordedAtMs", recordedAtMs);
            json.put("modelSize", modelSize);
            json.put("tokensPerSec", Math.round(tokensPerSec * 100.0) / 100.0);
            json.put("stepsPerSec", Math.round(stepsPerSec * 1000.0) / 1000.0);
            json.put("durationMs", durationMs);
            return json;
        }

        public static ThroughputMeasurement fromJsonObject(JSONObject json) {
            return new ThroughputMeasurement(
                    json.optLong("recordedAtMs", 0),
                    json.optString("modelSize", ""),
                    json.optDouble("tokensPerSec", 0.0),
                    json.optDouble("stepsPerSec", 0.0),
                    json.optLong("durationMs", 0));
        }
    }

    public static class DynamicSizingResult {
        public final int dModel;
        public final int numBlocks;
        public final int numHeads;
        public final int dff;
        public final int maxSeqLen;
        public final int vocabSize;
        public final long estimatedParameters;
        public final long estimatedMemoryMb;
        public final String rationale;

        public DynamicSizingResult(int dModel, int numBlocks, int numHeads, int dff, int maxSeqLen, int vocabSize, String rationale) {
            this.dModel = dModel;
            this.numBlocks = numBlocks;
            this.numHeads = numHeads;
            this.dff = dff;
            this.maxSeqLen = maxSeqLen;
            this.vocabSize = vocabSize;
            this.rationale = rationale;

            long params = (long) vocabSize * dModel;
            params += (long) numBlocks * (4L * dModel * dModel + 3L * dModel * dff + 2L * dModel);
            params += dModel;
            params += (long) dModel * vocabSize;
            this.estimatedParameters = params;
            // Weights + Gradients + AdamW m + AdamW v + activations (~16 bytes/param float32)
            this.estimatedMemoryMb = Math.max(32L, (params * 16L) / (1024L * 1024L));
        }
    }

    private static final Pattern LEADING_NUMBER_PATTERN = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)");
    private static final ForgeTrainingEstimationAgent INSTANCE = new ForgeTrainingEstimationAgent();

    // Measured throughput history, keyed by model size. Bounded per size; the
    // most recent measurements are kept. Persisted to JSON so the knowledge
    // survives application restarts.
    private final Map<String, Deque<ThroughputMeasurement>> measurements = new LinkedHashMap<>();
    private Path measurementsStoreDir = null;
    private boolean measurementsLoaded = false;

    public static ForgeTrainingEstimationAgent getInstance() {
        return INSTANCE;
    }

    /**
     * Configures the directory used for the persisted throughput measurement
     * store. Called by the orchestrator (project workspace) and by the Forge
     * Settings dialog (central workspace); both resolve to the same
     * &lt;codebase&gt;/workspace/forge-state directory.
     */
    public synchronized void configureMeasurementsStore(Path storeDir) {
        this.measurementsStoreDir = storeDir;
        this.measurementsLoaded = false;
        loadMeasurements();
    }

    private synchronized void loadMeasurements() {
        if (measurementsLoaded) return;
        measurementsLoaded = true;
        Path dir = resolveMeasurementsDir();
        if (dir == null) return;
        Path file = dir.resolve(MEASUREMENT_FILE_NAME);
        if (!Files.isRegularFile(file)) return;
        try {
            JSONObject root = new JSONObject(Files.readString(file));
            JSONArray arr = root.optJSONArray("measurements");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    ThroughputMeasurement m = ThroughputMeasurement.fromJsonObject(arr.getJSONObject(i));
                    if (m.tokensPerSec > 0.0) {
                        addToMemory(m);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[FORGE][ESTIMATE][WARN] Could not load throughput measurements from " + file + ": " + e.getMessage());
        }
    }

    private Path resolveMeasurementsDir() {
        if (measurementsStoreDir != null) return measurementsStoreDir;
        // Unconfigured fallback: user-home based store.
        return java.nio.file.Paths.get(System.getProperty("user.home"), ".evo-forge");
    }

    private synchronized void addToMemory(ThroughputMeasurement m) {
        if (m == null || m.tokensPerSec <= 0.0) return;
        String key = normalizeSizeKey(m.modelSize);
        Deque<ThroughputMeasurement> deque = measurements.computeIfAbsent(key, k -> new ArrayDeque<>());
        deque.addLast(m);
        while (deque.size() > MAX_MEASUREMENTS_PER_SIZE) {
            deque.removeFirst();
        }
    }

    private void persistMeasurements() {
        Path dir = resolveMeasurementsDir();
        try {
            Files.createDirectories(dir);
            JSONObject root = new JSONObject();
            JSONArray arr = new JSONArray();
            for (Deque<ThroughputMeasurement> deque : measurements.values()) {
                for (ThroughputMeasurement m : deque) {
                    arr.put(m.toJsonObject());
                }
            }
            root.put("measurements", arr);
            Path target = dir.resolve(MEASUREMENT_FILE_NAME);
            Path tmp = dir.resolve(MEASUREMENT_FILE_NAME + ".tmp");
            Files.writeString(tmp, root.toString());
            try {
                Files.move(tmp, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            System.err.println("[FORGE][ESTIMATE][WARN] Could not persist throughput measurements: " + e.getMessage());
        }
    }

    /**
     * Records a REAL completed training run's measured throughput. Values must
     * come from actual trainer execution metadata.
     */
    public void recordTrainingMeasurement(String modelSize, double tokensPerSec, double stepsPerSec, long durationMs) {
        if (Double.isNaN(tokensPerSec) || Double.isInfinite(tokensPerSec) || tokensPerSec <= 0.0) return;
        ThroughputMeasurement m = new ThroughputMeasurement(System.currentTimeMillis(), modelSize, tokensPerSec, stepsPerSec, durationMs);
        addToMemory(m);
        persistMeasurements();
    }

    /**
     * Returns the median measured tokens/sec for the given model size, or -1
     * when no measurement exists. Never fabricates a value.
     */
    public synchronized double getMeasuredTokensPerSecond(String modelSize) {
        loadMeasurements();
        Deque<ThroughputMeasurement> deque = measurements.get(normalizeSizeKey(modelSize));
        if (deque == null || deque.isEmpty()) return -1.0;
        List<Double> values = new ArrayList<>();
        for (ThroughputMeasurement m : deque) {
            if (m.tokensPerSec > 0.0) values.add(m.tokensPerSec);
        }
        if (values.isEmpty()) return -1.0;
        Collections.sort(values);
        return values.get(values.size() / 2);
    }

    public synchronized int getMeasurementCount(String modelSize) {
        loadMeasurements();
        Deque<ThroughputMeasurement> deque = measurements.get(normalizeSizeKey(modelSize));
        return deque == null ? 0 : deque.size();
    }

    /**
     * Estimation quality 0..1: 1.0 = at least 3 real measurements exist for the
     * model size; <1 = fewer measurements; 0 = analytical preliminary estimate only.
     */
    public synchronized double getThroughputConfidence(String modelSize) {
        loadMeasurements();
        Deque<ThroughputMeasurement> deque = measurements.get(normalizeSizeKey(modelSize));
        int n = deque == null ? 0 : deque.size();
        if (n <= 0) return 0.0;
        if (n == 1) return 0.4;
        if (n == 2) return 0.7;
        return 1.0;
    }

    private static String normalizeSizeKey(String modelSize) {
        if (modelSize == null || modelSize.trim().isEmpty()) return "UNKNOWN";
        return modelSize.trim().toUpperCase();
    }

    /**
     * Test seam: clears the in-memory measurement cache. Production code never
     * calls this.
     */
    public synchronized void resetMeasurementsForTesting() {
        measurements.clear();
        measurementsLoaded = true;
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

    /**
     * Dynamically calculates optimal model architecture based on available training tokens,
     * system memory constraints, and GPU accelerator availability.
     */
    public DynamicSizingResult calculateDynamicModelSizing(long usableTokens, int targetVocabSize, long maxMemoryBudgetMb) {
        long safeTokens = Math.max(1_000L, usableTokens);
        int vocab = Math.max(256, targetVocabSize);
        long memoryBudget = maxMemoryBudgetMb > 0 ? maxMemoryBudgetMb : (getMaxMemoryMb() / 2);

        int dModel;
        int numBlocks;
        int numHeads;
        int dff;
        int maxSeqLen;
        String rationale;

        if (safeTokens < 50_000L || memoryBudget < 1024) {
            // Nano tier: <50k tokens or <1GB RAM
            dModel = 128;
            numBlocks = 3;
            numHeads = 4;
            dff = 256;
            maxSeqLen = 128;
            rationale = "Dataset <50k tokens or restricted memory budget: sized as Nano architecture for fast convergence.";
        } else if (safeTokens < 500_000L || memoryBudget < 2048) {
            // Micro tier: 50k - 500k tokens
            dModel = 256;
            numBlocks = 4;
            numHeads = 8;
            dff = 512;
            maxSeqLen = 256;
            rationale = "Dataset between 50k and 500k tokens: sized as Micro architecture (~2-5M parameters).";
        } else if (safeTokens < 2_000_000L || memoryBudget < 4096) {
            // Tiny tier: 500k - 2M tokens
            dModel = 384;
            numBlocks = 6;
            numHeads = 8;
            dff = 1024;
            maxSeqLen = 512;
            rationale = "Dataset between 500k and 2M tokens: sized as Tiny architecture (~15M parameters).";
        } else if (safeTokens < 10_000_000L || memoryBudget < 8192) {
            // Small tier: 2M - 10M tokens
            dModel = 512;
            numBlocks = 8;
            numHeads = 8;
            dff = 2048;
            maxSeqLen = 1024;
            rationale = "Dataset between 2M and 10M tokens: sized as Small architecture (~50M parameters).";
        } else {
            // Medium tier: >10M tokens with sufficient memory budget
            dModel = 768;
            numBlocks = 12;
            numHeads = 12;
            dff = 3072;
            maxSeqLen = 2048;
            rationale = "Large dataset exceeding 10M tokens: sized as Medium architecture (~250M parameters).";
        }

        return new DynamicSizingResult(dModel, numBlocks, numHeads, dff, maxSeqLen, vocab, rationale);
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

    /**
     * Fallback parameter-count table (used when the UI layer cannot resolve the
     * authoritative ModelSizePreset). Values match the architecture-derived
     * counts of ModelSizePreset.Size so the "SMALL (25M)" vs "Small (50M)"
     * discrepancy no longer exists: SMALL is ~48.9M parameters everywhere.
     */
    public double getModelParameterCount(String modelSize) {
        String s = modelSize != null ? modelSize.toUpperCase() : "SMALL";
        if (s.contains("NANO")) return 748_416.0;
        if (s.contains("MICRO")) return 3_647_744.0;
        if (s.contains("TINY")) return 16_765_824.0;
        if (s.contains("MINI")) return 8_000_000.0;
        if (s.contains("SMALL")) return 48_923_136.0;
        if (s.contains("MEDIUM")) return 162_417_408.0;
        if (s.contains("LARGE") && !s.contains("XLARGE")) return 370_869_248.0;
        if (s.contains("GRANITE_PLUS")) return 10_140_717_056.0;
        if (s.contains("GRANITE")) return 8_194_494_464.0;
        if (s.contains("XLARGE")) return 500_000_000.0;
        return 48_923_136.0;
    }

    public double getBaselineTokensPerSecond(String modelSize) {
        return getBaselineTokensPerSecond(getModelParameterCount(modelSize));
    }

    /**
     * Analytical preliminary throughput for an explicit parameter count:
     * effective GFLOPs estimate of the pure-Java JVM trainer divided by
     * ~6 FLOPs per trained token. Clearly a PRELIMINARY analytical estimate;
     * measured values (recordTrainingMeasurement) take precedence in the UI.
     */
    public double getBaselineTokensPerSecond(double paramCount) {
        int cores = getCpuCores();
        double flopsPerCore = 500_000_000.0;
        double totalFlops = cores * flopsPerCore * Math.pow(cores, -0.2);
        double flopsPerToken = 6.0 * paramCount;
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

    /**
     * Analytical preliminary training-duration estimate for an explicit model
     * parameter count (used by the dialog with authoritative ModelSizePreset
     * parameter counts).
     */
    public double calculateEstimatedForgingSecondsWithParams(double paramCount, int epochs, long datasetBytes) {
        if (datasetBytes <= 0 || epochs <= 0 || paramCount <= 0.0) return 0.0;
        long estimatedDatasetTokens = Math.max(1L, datasetBytes / 4L);
        double totalTokensToTrain = (double) estimatedDatasetTokens * (double) Math.max(1, epochs);
        double tokPerSec = getBaselineTokensPerSecond(paramCount);
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
