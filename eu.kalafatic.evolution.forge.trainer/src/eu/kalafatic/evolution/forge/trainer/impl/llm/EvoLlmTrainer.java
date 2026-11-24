package eu.kalafatic.evolution.forge.trainer.impl.llm;

import eu.kalafatic.evolution.forge.data.api.TrainingBatch;
import eu.kalafatic.evolution.forge.data.api.TrainingSample;
import eu.kalafatic.evolution.forge.data.impl.DatasetBuilder;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.ModelParameters;
import eu.kalafatic.evolution.forge.trainer.api.LossReduction;
import eu.kalafatic.evolution.forge.trainer.api.TerminationReason;
import eu.kalafatic.evolution.forge.trainer.api.TrainingCheckpoint;
import eu.kalafatic.evolution.forge.trainer.api.TrainingResult;
import eu.kalafatic.evolution.forge.trainer.api.TrainingTerminationStatus;

import java.util.*;

/**
 * Production Canonical EVO LLM Trainer.
 * Provides deterministic training loop, multi-head backpropagation, AdamW optimization,
 * loss calculation, per-token reduction, full lifecycle management, and training resumption.
 */
public class EvoLlmTrainer {

    public interface ProgressListener {
        void onProgress(int epoch, int totalEpochs, int sampleIndex, int totalSamplesCount, double currentLoss);

        default void onProgressMetrics(int epoch, int totalEpochs, int currentStep, int totalSteps,
                                       long processedTokens, long totalTokens,
                                       double tokensPerSec, double stepsPerSec,
                                       double estimatedRemainingSeconds, double currentLoss) {
            onProgress(epoch, totalEpochs, currentStep, totalSteps, currentLoss);
        }

        default void onProgressMetrics(int epoch, int totalEpochs, int currentStep, int totalSteps,
                                       long processedTokens, long totalTokens,
                                       double tokensPerSec, double stepsPerSec,
                                       double estimatedRemainingSeconds, double currentLoss,
                                       double learningRate) {
            onProgressMetrics(epoch, totalEpochs, currentStep, totalSteps, processedTokens, totalTokens,
                    tokensPerSec, stepsPerSec, estimatedRemainingSeconds, currentLoss);
        }

        /**
         * Extended progress callback carrying the actual sample position (per-epoch
         * sample index / total samples) so consumers can render the true position within
         * the current epoch, not only the optimization-step / token counters.
         */
        default void onProgressMetrics(int epoch, int totalEpochs, int currentStep, int totalSteps,
                                       long processedTokens, long totalTokens,
                                       double tokensPerSec, double stepsPerSec,
                                       double estimatedRemainingSeconds, double currentLoss,
                                       double learningRate, int sampleIndex, int totalSamplesCount) {
            onProgressMetrics(epoch, totalEpochs, currentStep, totalSteps, processedTokens, totalTokens,
                    tokensPerSec, stepsPerSec, estimatedRemainingSeconds, currentLoss, learningRate);
        }

        default void onEpochCompleted(int epoch, int totalEpochs, double trainLoss, double valLoss) {}

        default void onEpochCompleted(int epoch, int totalEpochs, double trainLoss, double valLoss, long durationMs) {
            onEpochCompleted(epoch, totalEpochs, trainLoss, valLoss);
        }
    }

    public enum TrainingProfile {
        EVO_FAST(1e-3f, 5e-5f, 0.01f),
        EVO_CODER(5e-4f, 1e-5f, 0.1f),
        EVO_FINETUNE(1e-4f, 1e-6f, 0.01f);

        public final float initialLr;
        public final float minLr;
        public final float weightDecay;

        TrainingProfile(float initialLr, float minLr, float weightDecay) {
            this.initialLr = initialLr;
            this.minLr = minLr;
            this.weightDecay = weightDecay;
        }
    }

    private final EvoLlmModel model;
    private float initialLr;
    private float minLr;
    private float weightDecay;
    private float beta1 = 0.9f;
    private float beta2 = 0.95f;
    private float eps = 1e-8f;
    private float maxGradNorm = 1.0f;
    private float validationSplitRatio = 0.1f;
    private int microBatchSize = 4;
    private int accumulationSteps = 2;
    private long baseSeed = 42L;
    private int configuredWarmupSteps = -1;

    private LossReduction lossReduction = LossReduction.MEAN_PER_TOKEN;
    private final List<ProgressListener> progressListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final List<Double> lossHistory = new ArrayList<>();
    private static final int MAX_LOSS_HISTORY = 4096;
    private volatile long lastProcessedTokens = 0;
    private volatile boolean stopRequested = false;
    private volatile boolean pauseRequested = false;
    private volatile boolean abortRequested = false;
    private TrainingResult lastResult;

    // ------------------------------------------------------------------
    // Device / backend diagnostics (truthful execution instrumentation).
    // This trainer is a pure-JVM implementation: parameters, activations,
    // gradients and optimizer state all live in Java float[] on the host CPU.
    // These fields make the actual execution device and per-phase cost
    // explicit so it is never mistaken for GPU acceleration.
    // ------------------------------------------------------------------
    private final String backendName = "JVM Java CPU (EvoLlmTrainer)";
    private final String actualDevice = "CPU";
    private final int hostCores = Runtime.getRuntime().availableProcessors();
    private long forwardNanosTotal = 0L;
    private long backwardNanosTotal = 0L;
    private long optimizerNanosTotal = 0L;
    private long optimizerStepsExecuted = 0L;

    // Resumption state
    private TrainingCheckpoint resumeCheckpoint = null;
    private EmbeddedAdamW activeOptimizer = null;
    private int lastCompletedEpochs = 0;

    // Best-validation checkpoint tracking: retains a snapshot of the model weights
    // at the epoch with the lowest validation loss so the caller can select the
    // best-generalizing model instead of the final (potentially overfit) one.
    private double bestValLoss = Double.POSITIVE_INFINITY;
    private int bestEpoch = -1;
    private List<float[]> bestCheckpointWeights = null;

    public EvoLlmTrainer(EvoLlmModel model, TrainingProfile profile) {
        if (model == null) throw new IllegalArgumentException("EvoLlmModel cannot be null");
        this.model = model;
        applyProfile(profile != null ? profile : TrainingProfile.EVO_FAST);
    }

    public EvoLlmTrainer(EvoLlmModel model) {
        this(model, TrainingProfile.EVO_FAST);
    }

    public void applyProfile(TrainingProfile profile) {
        this.initialLr = profile.initialLr;
        this.minLr = profile.minLr;
        this.weightDecay = profile.weightDecay;
    }

    public void setLossReduction(LossReduction lossReduction) { this.lossReduction = lossReduction; }
    public void setMicroBatchSize(int microBatchSize) { this.microBatchSize = microBatchSize; }
    public void setAccumulationSteps(int steps) { this.accumulationSteps = Math.max(1, steps); }
    public void setBaseSeed(long seed) { this.baseSeed = seed; }
    public void setValidationSplitRatio(float ratio) { this.validationSplitRatio = Math.max(0.0f, Math.min(0.5f, ratio)); }
    public void setLearningRate(float lr) { this.initialLr = lr; }
    public void setMaxGradNorm(float maxGradNorm) { this.maxGradNorm = maxGradNorm; }

    public void setWarmupSteps(int warmupSteps) { this.configuredWarmupSteps = warmupSteps > 0 ? warmupSteps : -1; }
    public int getWarmupSteps() { return configuredWarmupSteps; }

    public void setResumeCheckpoint(TrainingCheckpoint checkpoint) {
        this.resumeCheckpoint = checkpoint;
    }

    // Optional training termination criteria. Disabled by default so that plain
    // training runs behave exactly as before; enabled when the orchestrator
    // supplies an explicit configuration.
    private boolean earlyStoppingEnabled = false;
    private float targetLossThreshold = Float.POSITIVE_INFINITY;
    private float timeBudgetHours = Float.POSITIVE_INFINITY;

    public void setEarlyStoppingEnabled(boolean enabled) { this.earlyStoppingEnabled = enabled; }
    public boolean isEarlyStoppingEnabled() { return earlyStoppingEnabled; }

    public void setTargetLossThreshold(float threshold) { this.targetLossThreshold = threshold; }
    public float getTargetLossThreshold() { return targetLossThreshold; }

    public void setTimeBudgetHours(float hours) { this.timeBudgetHours = hours; }
    public float getTimeBudgetHours() { return timeBudgetHours; }

    public TrainingCheckpoint getResumeCheckpoint() {
        return resumeCheckpoint;
    }

    public void setProgressListener(ProgressListener listener) {
        this.progressListeners.clear();
        if (listener != null) this.progressListeners.add(listener);
    }
    public void addProgressListener(ProgressListener listener) {
        if (listener != null && !this.progressListeners.contains(listener)) {
            this.progressListeners.add(listener);
        }
    }
    public void removeProgressListener(ProgressListener listener) {
        if (listener != null) {
            this.progressListeners.remove(listener);
        }
    }
    public int getProgressListenerCount() { return this.progressListeners.size(); }
    public List<Double> getLossHistory() { return lossHistory; }
    public long getLastProcessedTokens() { return lastProcessedTokens; }
    public void requestStop() { this.stopRequested = true; }
    public void finishTraining() { this.stopRequested = true; }
    public boolean isStopRequested() { return stopRequested; }

    public void requestPause() { this.pauseRequested = true; }
    public boolean isPauseRequested() { return pauseRequested; }

    public void requestAbort() { this.abortRequested = true; }
    public boolean isAbortRequested() { return abortRequested; }

    public TrainingResult getLastTrainingResult() { return lastResult; }
    public EvoLlmModel getModel() { return model; }

    // ------------------------------------------------------------------
    // Device / backend diagnostics accessors
    // ------------------------------------------------------------------
    /** Backend implementation name (always the JVM Java CPU trainer). */
    public String getBackendName() { return backendName; }

    /** Actual execution device for parameters/activations/gradients/optimizer. */
    public String getActualDevice() { return actualDevice; }

    /** Host CPU core count observed by the training JVM. */
    public int getHostCores() { return hostCores; }

    /** Cumulative forward-pass wall time in nanoseconds for the last train() run. */
    public long getForwardNanos() { return forwardNanosTotal; }

    /** Cumulative backward-pass wall time in nanoseconds for the last train() run. */
    public long getBackwardNanos() { return backwardNanosTotal; }

    /** Cumulative optimizer-step wall time in nanoseconds for the last train() run. */
    public long getOptimizerNanos() { return optimizerNanosTotal; }

    /** Number of optimizer steps executed in the last train() run. */
    public long getOptimizerStepsExecuted() { return optimizerStepsExecuted; }

    /** Mean forward/backward/optimizer durations (ms) for the last train() run, or zeros. */
    public double[] getPhaseDurationsMs() {
        return new double[]{
                forwardNanosTotal / 1_000_000.0,
                backwardNanosTotal / 1_000_000.0,
                optimizerNanosTotal / 1_000_000.0
        };
    }

    /**
     * Epoch (1-based) with the lowest validation loss observed during the most recent
     * {@link #train} run, or {@code -1} when no validation split was configured.
     */
    public int getBestEpoch() { return bestEpoch; }

    /** Lowest validation loss observed during the most recent {@link #train} run. */
    public double getBestValLoss() { return bestValLoss; }

    /** True when a best-validation weight snapshot is available to restore. */
    public boolean hasBestCheckpoint() { return bestCheckpointWeights != null; }

    /**
     * Restores the model weights to the best-validation checkpoint captured during the
     * most recent {@link #train} run. A no-op returning {@code false} when no validation
     * split was configured (so no best checkpoint was recorded). This lets callers select
     * the best-generalizing model instead of the final, potentially overfit one.
     */
    public boolean restoreBestCheckpoint() {
        if (bestCheckpointWeights == null) {
            return false;
        }
        List<Tensor> params = model.parameters();
        if (params.size() != bestCheckpointWeights.size()) {
            return false;
        }
        for (int i = 0; i < params.size(); i++) {
            System.arraycopy(bestCheckpointWeights.get(i), 0, params.get(i).getData(), 0,
                    Math.min(bestCheckpointWeights.get(i).length, params.get(i).getData().length));
        }
        return true;
    }

    public static String formatDuration(long ms) {
        if (ms < 0) return "0 ms";
        if (ms < 1000) return ms + " ms";
        long totalSec = ms / 1000;
        if (totalSec < 60) return String.format("%.2f s (%d ms)", ms / 1000.0, ms);
        long sec = totalSec % 60;
        long totalMin = totalSec / 60;
        if (totalMin < 60) return String.format("%d min %d sec (%d ms)", totalMin, sec, ms);
        long min = totalMin % 60;
        long totalHours = totalMin / 60;
        if (totalHours < 24) return String.format("%d hr %d min %d sec (%d ms)", totalHours, min, sec, ms);
        long hours = totalHours % 24;
        long days = totalHours / 24;
        return String.format("%d d %d hr %d min %d sec (%d ms)", days, hours, min, sec, ms);
    }

    public TrainingCheckpoint captureCheckpoint(String runId, String modelName, int completedEpochs, float lastTrainLoss, float lastValLoss, String datasetFingerprint, Map<String, Integer> vocab) {
        List<Tensor> paramList = model.parameters();
        List<String> canonicalNames = canonicalParameterNames(paramList);
        Map<String, float[]> m = activeOptimizer != null ? activeOptimizer.getMVectorMap(paramList, canonicalNames) : Collections.emptyMap();
        Map<String, float[]> v = activeOptimizer != null ? activeOptimizer.getVVectorMap(paramList, canonicalNames) : Collections.emptyMap();
        long stepCount = activeOptimizer != null ? activeOptimizer.getStepCounter() : model.getTrainingState().getStep();
        float curLr = activeOptimizer != null ? activeOptimizer.getLr() : initialLr;

        return TrainingCheckpoint.capture(
                runId,
                modelName,
                model,
                completedEpochs,
                stepCount,
                lastTrainLoss,
                lastValLoss,
                curLr,
                baseSeed,
                datasetFingerprint,
                m,
                v,
                vocab,
                lastProcessedTokens
        );
    }

    private List<String> canonicalParameterNames(List<Tensor> paramList) {
        ModelParameters registry = model.getModelParameters();
        List<String> names = new ArrayList<>(paramList.size());
        // Map by identity: registry names are in a different order than parameters();
        // match by data-array identity to obtain the canonical name per position.
        for (Tensor p : paramList) {
            String name = null;
            for (String candidate : registry.names()) {
                if (registry.get(candidate).getData() == p.getData()) {
                    name = candidate;
                    break;
                }
            }
            names.add(name);
        }
        return names;
    }

    public TrainingResult train(List<?> rawSamples, int epochs) {
        long startTime = System.currentTimeMillis();
        // NOTE: stop/pause/abort flags are intentionally NOT reset here. A
        // control request may legitimately arrive between trainer registration
        // and the start of the training loop (e.g. a user clicking Stop right
        // at training start); resetting the flags at entry would lose it. The
        // flags are consumed by this run and cleared at every exit point.
        TrainingResult result = new TrainingResult();
        result.setPlannedEpochs(epochs);

        if (rawSamples == null || rawSamples.isEmpty()) {
            result.setTerminationStatus(TrainingTerminationStatus.TRAINING_FAILED);
            result.setTerminationReason(TerminationReason.NO_TRAINING_SAMPLES);
            result.setModelValid(false);
            result.setFailureDetails("No training samples provided to trainer.");
            this.lastResult = result;
            clearRequestFlags();
            return result;
        }

        List<TrainingSample> samples = new ArrayList<>();
        for (Object item : rawSamples) {
            if (item instanceof TrainingSample) {
                samples.add((TrainingSample) item);
            } else if (item instanceof DatasetBuilder.Sample) {
                DatasetBuilder.Sample s = (DatasetBuilder.Sample) item;
                int len = s.input.size();
                int[] inputIds = new int[len];
                int[] labels = new int[len];
                boolean[] lossMask = new boolean[len];
                float[] attMask = new float[len];
                for (int i = 0; i < len; i++) {
                    inputIds[i] = s.input.get(i);
                    if (i + 1 < len) {
                        labels[i] = s.input.get(i + 1);
                        lossMask[i] = true;
                    } else if (s.target != null) {
                        labels[i] = s.target;
                        lossMask[i] = true;
                    } else {
                        labels[i] = s.input.get(i);
                        lossMask[i] = false;
                    }
                    attMask[i] = 1.0f;
                }
                samples.add(new TrainingSample(inputIds, labels, lossMask, attMask));
            } else if (item instanceof int[]) {
                int[] inputIds = (int[]) item;
                int len = inputIds.length;
                int[] labels = new int[len];
                boolean[] lossMask = new boolean[len];
                float[] attMask = new float[len];
                for (int i = 0; i < len; i++) {
                    if (i + 1 < len) {
                        labels[i] = inputIds[i + 1];
                        lossMask[i] = true;
                    } else {
                        labels[i] = inputIds[i];
                        lossMask[i] = false;
                    }
                    attMask[i] = 1.0f;
                }
                samples.add(new TrainingSample(inputIds, labels, lossMask, attMask));
            }
        }

        if (samples.isEmpty()) {
            result.setTerminationStatus(TrainingTerminationStatus.TRAINING_FAILED);
            result.setTerminationReason(TerminationReason.NO_TRAINING_SAMPLES);
            result.setModelValid(false);
            result.setFailureDetails("Training samples list became empty after filtering.");
            this.lastResult = result;
            clearRequestFlags();
            return result;
        }

        List<TrainingSample> dataset = new ArrayList<>(samples);
        Collections.shuffle(dataset, new Random(baseSeed));

        int valCount = (int) (dataset.size() * validationSplitRatio);
        List<TrainingSample> trainSamples = new ArrayList<>(dataset.subList(0, Math.max(1, dataset.size() - valCount)));
        List<TrainingSample> valSamples = valCount > 0 ? new ArrayList<>(dataset.subList(dataset.size() - valCount, dataset.size())) : Collections.emptyList();

        // Canonical epoch ordering: every epoch permutation is applied to this
        // fixed order (not to the previous epoch's shuffled order) so that a
        // resumed run produces bit-identical batches for any epoch regardless of
        // which earlier epochs were executed elsewhere.
        final List<TrainingSample> canonicalEpochOrder = new ArrayList<>(trainSamples);

        ModelParameterGroups paramGroups = ModelParameterGroups.fromModel(model);
        EmbeddedAdamW optimizer = new EmbeddedAdamW(initialLr, beta1, beta2, eps, weightDecay);
        this.activeOptimizer = optimizer;

        int startEpoch = 0;
        int completedStepsPrior = 0;

        // Check if resuming from checkpoint
        if (resumeCheckpoint != null) {
            resumeCheckpoint.validateCompatibility(model);
            Map<String, float[]> restoredM = new HashMap<>();
            Map<String, float[]> restoredV = new HashMap<>();
            resumeCheckpoint.restoreInto(model, restoredM, restoredV);
            optimizer.loadState(model.parameters(), canonicalParameterNames(model.parameters()), restoredM, restoredV, (int) resumeCheckpoint.getTotalOptimizerSteps());
            startEpoch = resumeCheckpoint.getCompletedEpochs();
            completedStepsPrior = (int) resumeCheckpoint.getTotalOptimizerSteps();
            lossHistory.add((double) resumeCheckpoint.getLastTrainLoss());
            System.out.printf("[EVO Trainer] Resuming from checkpoint: starting epoch %d/%d (completed prior: %d, steps: %d)%n",
                    startEpoch + 1, epochs, startEpoch, completedStepsPrior);
        } else {
            lossHistory.clear();
        }

        long paramCount = model.parameters().stream().mapToLong(Tensor::getSize).sum();
        result.getMetadata().put("trainingMode", (resumeCheckpoint != null) ? "RESUMED" : "FROM_SCRATCH");
        result.getMetadata().put("pretrainedWeights", (resumeCheckpoint != null));
        result.getMetadata().put("parameterCount", paramCount);
        result.getMetadata().put("baseSeed", baseSeed);

        System.out.printf("[EVO Trainer] Mode: %s | Pretrained Weights: %b | Parameters: %d | Base Seed: %d%n",
                result.getMetadata().get("trainingMode"), (resumeCheckpoint != null), paramCount, baseSeed);

        // Reset per-run phase timers and record the truthful execution device/backend.
        forwardNanosTotal = 0L;
        backwardNanosTotal = 0L;
        optimizerNanosTotal = 0L;
        optimizerStepsExecuted = 0L;
        result.getMetadata().put("executionDevice", actualDevice);
        result.getMetadata().put("backend", backendName);
        result.getMetadata().put("hostCores", hostCores);
        System.out.printf("[EVO Trainer] Device: selected=%s actual=%s backend=%s | Host CPU cores=%d%n",
                actualDevice, actualDevice, backendName, hostCores);

        // Check if requested epochs already satisfied. Considers both the resume
        // checkpoint AND epochs already completed by a previous train() invocation
        // on this trainer instance (a re-submission of <= completed epochs is a
        // clean no-op even when the stored checkpoint is stale).
        int effectiveStartEpoch = Math.max(startEpoch, lastCompletedEpochs);
        if (effectiveStartEpoch >= epochs) {
            System.out.printf("[EVO Trainer] Training already completed: requested %d epochs, %d completed epochs. No-op termination.%n",
                    epochs, effectiveStartEpoch);
            result.setTerminationStatus(TrainingTerminationStatus.TRAINING_COMPLETED);
            result.setTerminationReason(TerminationReason.PLANNED_COMPLETION);
            result.setCompletedEpochs(effectiveStartEpoch);
            result.setCompletedSteps(completedStepsPrior);
            result.setPlannedSteps(completedStepsPrior);
            result.setLastLoss(resumeCheckpoint != null ? resumeCheckpoint.getLastTrainLoss()
                    : (lastResult != null ? lastResult.getLastLoss() : 0.0));
            result.setValLoss(resumeCheckpoint != null ? resumeCheckpoint.getLastValLoss()
                    : (lastResult != null ? lastResult.getValLoss() : 0.0));
            result.setTrainingDurationMs(0);
            this.lastResult = result;
            clearRequestFlags();
            return result;
        }
        startEpoch = effectiveStartEpoch;

        List<TrainingBatch> initialBatches = buildBatches(trainSamples, microBatchSize);
        int stepsPerEpoch = Math.max(1, (initialBatches.size() + accumulationSteps - 1) / accumulationSteps);
        int totalOptimizationSteps = epochs * stepsPerEpoch;
        result.setPlannedSteps(totalOptimizationSteps);

        long epochTokenCount = 0;
        for (TrainingSample sample : trainSamples) {
            epochTokenCount += sample.inputIds.length;
        }
        long totalTokensToTrain = epochTokenCount * (long) epochs;
        // On resume, prefer the exact processed-token counter persisted in the
        // checkpoint; older checkpoints without it fall back to the previous
        // approximation (completed epochs x tokens per epoch).
        long resumedTokens = (resumeCheckpoint != null && resumeCheckpoint.getProcessedTokens() > 0)
                ? resumeCheckpoint.getProcessedTokens()
                : (long) startEpoch * epochTokenCount;
        long totalProcessedTokens = resumedTokens;
        lastProcessedTokens = totalProcessedTokens;
        long lastMeasurementTime = System.currentTimeMillis();
        long windowStartTokens = totalProcessedTokens;
        int windowStartStep = completedStepsPrior;
        double movingAvgTokPerSec = 0.0;
        double movingAvgStepsPerSec = 0.0;

        int optStep = completedStepsPrior;

        // Warmup: explicit configuration wins; otherwise the documented
        // default schedule (3% of total optimization steps).
        int warmupSteps = configuredWarmupSteps > 0
                ? configuredWarmupSteps
                : Math.max(1, (int) (totalOptimizationSteps * 0.03f));

        // Each train() invocation tracks its own best-validation checkpoint.
        bestValLoss = Double.POSITIVE_INFINITY;
        bestEpoch = -1;
        bestCheckpointWeights = null;

        try {
            for (int epoch = startEpoch; epoch < epochs; epoch++) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.printf("[EVO Trainer] Thread interrupted before epoch %d/%d.%n", epoch + 1, epochs);
                    result.setTerminationStatus(TrainingTerminationStatus.TRAINING_INTERRUPTED);
                    result.setTerminationReason(TerminationReason.THREAD_INTERRUPTED);
                    break;
                }
                if (abortRequested) {
                    System.out.printf("[EVO Trainer] Abort requested before epoch %d/%d. Terminating immediately.%n", epoch + 1, epochs);
                    result.setTerminationStatus(TrainingTerminationStatus.TRAINING_ABORTED);
                    result.setTerminationReason(TerminationReason.USER_ABORT_REQUEST);
                    result.setModelValid(false);
                    break;
                }
                if (pauseRequested) {
                    System.out.printf("[EVO Trainer] Pause requested before epoch %d/%d. Checkpointing session.%n", epoch + 1, epochs);
                    result.setTerminationStatus(TrainingTerminationStatus.TRAINING_PAUSED);
                    result.setTerminationReason(TerminationReason.USER_PAUSE_REQUEST);
                    break;
                }
                if (stopRequested) {
                    System.out.printf("[EVO Trainer] Graceful stop requested before epoch %d/%d. Completing training state.%n", epoch + 1, epochs);
                    result.setTerminationStatus(TrainingTerminationStatus.TRAINING_STOPPED);
                    result.setTerminationReason(TerminationReason.USER_STOP_REQUEST);
                    break;
                }
                // Shuffle a FRESH copy of the canonical order: an epoch's batches are
                // a pure function of (epoch, seed), independent of prior epochs, so
                // interrupted+resumed training matches uninterrupted training exactly.
                trainSamples = new ArrayList<>(canonicalEpochOrder);
                Collections.shuffle(trainSamples, new Random(baseSeed + epoch + 1));
                List<TrainingBatch> batches = buildBatches(trainSamples, microBatchSize);

                double epochTotalLoss = 0.0;
                long epochValidTokens = 0;
                long epochStartTime = System.currentTimeMillis();

                boolean epochStoppedEarly = false;

                for (int bIdx = 0; bIdx < batches.size(); bIdx += accumulationSteps) {
                    if (Thread.currentThread().isInterrupted()) {
                        System.out.printf("[EVO Trainer] Thread interrupted during epoch %d.%n", epoch + 1);
                        result.setTerminationStatus(TrainingTerminationStatus.TRAINING_INTERRUPTED);
                        result.setTerminationReason(TerminationReason.THREAD_INTERRUPTED);
                        epochStoppedEarly = true;
                        break;
                    }
                    if (abortRequested) {
                        System.out.printf("[EVO Trainer] Abort requested during epoch %d. Halting training.%n", epoch + 1);
                        result.setTerminationStatus(TrainingTerminationStatus.TRAINING_ABORTED);
                        result.setTerminationReason(TerminationReason.USER_ABORT_REQUEST);
                        result.setModelValid(false);
                        epochStoppedEarly = true;
                        break;
                    }
                    if (pauseRequested) {
                        System.out.printf("[EVO Trainer] Pause requested during epoch %d. Finalizing current batch and pausing.%n", epoch + 1);
                        result.setTerminationStatus(TrainingTerminationStatus.TRAINING_PAUSED);
                        result.setTerminationReason(TerminationReason.USER_PAUSE_REQUEST);
                        epochStoppedEarly = true;
                        break;
                    }
                    if (stopRequested) {
                        System.out.printf("[EVO Trainer] Stop requested during epoch %d. Finalizing batch and stopping cleanly.%n", epoch + 1);
                        result.setTerminationStatus(TrainingTerminationStatus.TRAINING_STOPPED);
                        result.setTerminationReason(TerminationReason.USER_STOP_REQUEST);
                        epochStoppedEarly = true;
                        break;
                    }
                    // Hard wall-clock budget is also evaluated at accumulation-window
                    // boundaries so a single very long epoch cannot overrun the budget.
                    if (timeBudgetHours < Float.POSITIVE_INFINITY) {
                        long elapsedMs = System.currentTimeMillis() - startTime;
                        if (elapsedMs >= (long) (timeBudgetHours * 3600_000L)) {
                            System.out.printf("[EVO Trainer] Time budget %.2f h exceeded during epoch %d (elapsed %s).%n",
                                    timeBudgetHours, epoch + 1, formatDuration(elapsedMs));
                            result.setTerminationStatus(TrainingTerminationStatus.TRAINING_STOPPED);
                            result.setTerminationReason(TerminationReason.TIME_BUDGET_REACHED);
                            epochStoppedEarly = true;
                            break;
                        }
                    }
                    long windowValidTokens = 0;
                    for (TrainingBatch batch : batches.subList(bIdx, Math.min(bIdx + accumulationSteps, batches.size()))) {
                        for (float[] maskRow : batch.lossMask) {
                            for (float m : maskRow) {
                                if (m > 0.0f) windowValidTokens++;
                            }
                        }
                    }

                    if (windowValidTokens == 0) continue;

                    optStep++;
                    float currentLr = computeScheduledLr(optStep, totalOptimizationSteps, warmupSteps);
                    optimizer.setLr(currentLr);

                    paramGroups.zeroGradAll();

                    int windowEnd = Math.min(bIdx + accumulationSteps, batches.size());
                    List<TrainingBatch> accumulationWindow = batches.subList(bIdx, windowEnd);

                    double windowLoss = 0.0;
                    for (TrainingBatch batch : accumulationWindow) {
                        windowLoss += processMicroBatch(batch, windowValidTokens);
                    }

                    if (Double.isNaN(windowLoss) || Double.isInfinite(windowLoss)) {
                        System.err.printf("[EVO Trainer] NaN/Inf loss encountered during epoch %d opt step %d.%n", epoch + 1, optStep);
                        result.setTerminationStatus(TrainingTerminationStatus.TRAINING_FAILED);
                        result.setTerminationReason(TerminationReason.NAN_OR_INF_LOSS);
                        result.setModelValid(false);
                        result.setFailureDetails("NaN or Infinity loss value encountered during optimization step " + optStep);
                        epochStoppedEarly = true;
                        break;
                    }

                    epochTotalLoss += windowLoss;
                    epochValidTokens += windowValidTokens;

                    long optStart = System.nanoTime();
                    optimizer.step(paramGroups, maxGradNorm);
                    optimizerNanosTotal += (System.nanoTime() - optStart);
                    optimizerStepsExecuted++;

                    totalProcessedTokens += windowValidTokens;
                    lastProcessedTokens = totalProcessedTokens;

                    long now = System.currentTimeMillis();
                    long durationMs = now - lastMeasurementTime;
                    if (durationMs >= 500) {
                        long windowTokens = totalProcessedTokens - windowStartTokens;
                        int windowSteps = optStep - windowStartStep;
                        double curTokPerSec = (windowTokens * 1000.0) / Math.max(1, durationMs);
                        double curStepsPerSec = (windowSteps * 1000.0) / Math.max(1, durationMs);

                        if (movingAvgTokPerSec <= 0.0) {
                            movingAvgTokPerSec = curTokPerSec;
                            movingAvgStepsPerSec = curStepsPerSec;
                        } else {
                            movingAvgTokPerSec = 0.7 * movingAvgTokPerSec + 0.3 * curTokPerSec;
                            movingAvgStepsPerSec = 0.7 * movingAvgStepsPerSec + 0.3 * curStepsPerSec;
                        }

                        lastMeasurementTime = now;
                        windowStartTokens = totalProcessedTokens;
                        windowStartStep = optStep;
                    }

                    long remainingTokens = Math.max(0, totalTokensToTrain - totalProcessedTokens);
                    double remainingSec = movingAvgTokPerSec > 0.0 ? (remainingTokens / movingAvgTokPerSec) : 0.0;

                    if (!progressListeners.isEmpty()) {
                        double currentLoss = windowValidTokens > 0 ? windowLoss / windowValidTokens : 0.0;
                        int sampleIndex = Math.min(bIdx * microBatchSize, trainSamples.size());
                        for (ProgressListener listener : progressListeners) {
                            listener.onProgressMetrics(epoch, epochs, optStep, totalOptimizationSteps,
                                    totalProcessedTokens, totalTokensToTrain,
                                    movingAvgTokPerSec, movingAvgStepsPerSec,
                                    remainingSec, currentLoss, currentLr, sampleIndex, trainSamples.size());
                        }
                    } else if ((bIdx / accumulationSteps) % 5 == 0 || bIdx + accumulationSteps >= batches.size()) {
                        double currentLoss = windowValidTokens > 0 ? windowLoss / windowValidTokens : 0.0;
                        int sampleIndex = Math.min(bIdx * microBatchSize, trainSamples.size());
                        System.out.printf("[EVO Trainer] Epoch %d/%d | Step %d/%d | Samples %d/%d | Loss: %.4f | Speed: %.1f tok/s | ETA: %.1f s%n",
                                epoch + 1, epochs, optStep, totalOptimizationSteps, sampleIndex, trainSamples.size(), currentLoss, movingAvgTokPerSec, remainingSec);
                    }
                }

                if (epochStoppedEarly) {
                    // The epoch did not run to completion (user stop/pause/abort,
                    // interrupt, or NaN/Inf loss). Do NOT record it as completed:
                    // a partial epoch must not advance completedEpochs, otherwise
                    // pause/resume would skip the remaining portion of the epoch
                    // while the optimizer step counter sits mid-epoch.
                    break;
                }

                double avgTrainLoss = epochValidTokens > 0 ? epochTotalLoss / epochValidTokens : 0.0;
                double avgValLoss = evaluateValidation(valSamples);
                long epochDuration = System.currentTimeMillis() - epochStartTime;

                if (Double.isNaN(avgTrainLoss) || Double.isInfinite(avgTrainLoss)
                        || Double.isNaN(avgValLoss) || Double.isInfinite(avgValLoss)) {
                    System.err.printf("[EVO Trainer] NaN/Inf loss detected after epoch %d (train=%.4f, val=%.4f).%n",
                            epoch + 1, avgTrainLoss, avgValLoss);
                    result.setTerminationStatus(TrainingTerminationStatus.TRAINING_FAILED);
                    result.setTerminationReason(TerminationReason.NAN_OR_INF_LOSS);
                    result.setModelValid(false);
                    result.setFailureDetails("NaN or Infinity loss after epoch " + (epoch + 1));
                    break;
                }

                lossHistory.add(avgTrainLoss);
                if (lossHistory.size() > MAX_LOSS_HISTORY) {
                    lossHistory.remove(0);
                }
                model.getTrainingState().setEpoch(epoch + 1);
                model.getTrainingState().setStep(optStep);
                model.getTrainingState().setLastLoss((float) avgTrainLoss);

                result.setCompletedEpochs(epoch + 1);
                result.setCompletedSteps(optStep);
                result.setLastLoss(avgTrainLoss);
                result.setValLoss(avgValLoss);

                // Track the best-validation checkpoint so the caller can restore the
                // best-generalizing model rather than the final (possibly overfit) one.
                if (!valSamples.isEmpty() && !Double.isNaN(avgValLoss) && avgValLoss < bestValLoss) {
                    bestValLoss = avgValLoss;
                    bestEpoch = epoch + 1;
                    List<Tensor> params = model.parameters();
                    if (bestCheckpointWeights == null) {
                        bestCheckpointWeights = new ArrayList<>(params.size());
                        for (Tensor p : params) {
                            bestCheckpointWeights.add(p.getData().clone());
                        }
                    } else {
                        for (int i = 0; i < params.size(); i++) {
                            System.arraycopy(params.get(i).getData(), 0,
                                    bestCheckpointWeights.get(i), 0, params.get(i).getData().length);
                        }
                    }
                }

                System.out.printf("[EVO Epoch %d/%d] Train Loss (NLL/token): %.4f | Val Loss (NLL/token): %.4f | Time: %s%n",
                        epoch + 1, epochs, avgTrainLoss, avgValLoss, formatDuration(epochDuration));

                for (ProgressListener listener : progressListeners) {
                    listener.onEpochCompleted(epoch + 1, epochs, avgTrainLoss, avgValLoss, epochDuration);
                }

                // Configured termination criteria evaluated ONLY on fully
                // completed epochs, using the real measured losses.
                if (earlyStoppingEnabled && targetLossThreshold < Float.POSITIVE_INFINITY) {
                    double criterion = (valSamples != null && !valSamples.isEmpty()) ? avgValLoss : avgTrainLoss;
                    if (criterion <= targetLossThreshold) {
                        System.out.printf("[EVO Trainer] Early stopping criterion met after epoch %d: loss %.4f <= threshold %.4f.%n",
                                epoch + 1, criterion, targetLossThreshold);
                        result.setTerminationStatus(TrainingTerminationStatus.TRAINING_STOPPED);
                        result.setTerminationReason(TerminationReason.EARLY_STOPPING_CRITERIA);
                        break;
                    }
                }

                if (timeBudgetHours < Float.POSITIVE_INFINITY) {
                    long elapsedMs = System.currentTimeMillis() - startTime;
                    if (elapsedMs >= (long) (timeBudgetHours * 3600_000L)) {
                        System.out.printf("[EVO Trainer] Time budget %.2f h exceeded after epoch %d (elapsed %s).%n",
                                timeBudgetHours, epoch + 1, formatDuration(elapsedMs));
                        result.setTerminationStatus(TrainingTerminationStatus.TRAINING_STOPPED);
                        result.setTerminationReason(TerminationReason.TIME_BUDGET_REACHED);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.printf("[EVO Trainer] Exception caught during training: %s%n", e.getMessage());
            result.setTerminationStatus(TrainingTerminationStatus.TRAINING_FAILED);
            result.setTerminationReason(TerminationReason.EXCEPTION_THROWN);
            result.setModelValid(false);
            result.setFailureDetails("Exception caught during training loop: " + e.getMessage());
        }

        result.setTrainingDurationMs(System.currentTimeMillis() - startTime);
        result.getMetadata().put("totalTokensTrained", totalProcessedTokens);
        result.getMetadata().put("tokensPerSecond", movingAvgTokPerSec);
        result.getMetadata().put("stepsPerSecond", movingAvgStepsPerSec);
        result.getMetadata().put("forwardMs", forwardNanosTotal / 1_000_000.0);
        result.getMetadata().put("backwardMs", backwardNanosTotal / 1_000_000.0);
        result.getMetadata().put("optimizerMs", optimizerNanosTotal / 1_000_000.0);
        result.getMetadata().put("optimizerSteps", optimizerStepsExecuted);
        System.out.printf("[EVO Trainer] Device=%s backend=%s | forward=%.1f ms backward=%.1f ms optimizer=%.1f ms (steps=%d) | %.1f tok/s%n",
                actualDevice, backendName,
                forwardNanosTotal / 1_000_000.0, backwardNanosTotal / 1_000_000.0,
                optimizerNanosTotal / 1_000_000.0, optimizerStepsExecuted, movingAvgTokPerSec);
        if (result.getTerminationStatus() == TrainingTerminationStatus.TRAINING_COMPLETED) {
            this.lastCompletedEpochs = Math.max(this.lastCompletedEpochs, result.getCompletedEpochs());
        }
        this.lastResult = result;
        // Consume the control flags of this run so a later train() invocation
        // on the same trainer instance is not immediately stopped/paused/aborted.
        clearRequestFlags();
        return result;
    }

    private void clearRequestFlags() {
        this.stopRequested = false;
        this.pauseRequested = false;
        this.abortRequested = false;
    }

    public double processMicroBatch(TrainingBatch batch, long accumulationWindowTokens) {
        double microBatchRawLoss = 0.0;

        for (int i = 0; i < batch.batchSize; i++) {
            int[] inputIds = batch.inputIds[i];
            int[] labels = batch.labels[i];
            float[] mask = batch.lossMask[i];
            float[] attMask = batch.attentionMask[i];
            int seqLen = inputIds.length;

            long fwdStart = System.nanoTime();
            Tensor logits = model.forward(inputIds, attMask);
            long fwdEnd = System.nanoTime();
            float[] logitsData = logits.getData();
            int vocabSize = (int) logits.getShape()[1];

            Tensor dLogits = new SimpleTensor(seqLen, vocabSize);
            float[] dLogitsData = dLogits.getData();

            for (int t = 0; t < seqLen; t++) {
                if (mask[t] == 0.0f) continue;

                int offset = t * vocabSize;
                int target = labels[t];

                float maxLogit = Float.NEGATIVE_INFINITY;
                for (int v = 0; v < vocabSize; v++) maxLogit = Math.max(maxLogit, logitsData[offset + v]);

                float sumExp = 0.0f;
                for (int v = 0; v < vocabSize; v++) {
                    sumExp += (float) Math.exp(logitsData[offset + v] - maxLogit);
                }

                float logSumExp = maxLogit + (float) Math.log(sumExp);
                microBatchRawLoss += (logSumExp - logitsData[offset + target]);

                // MEAN_PER_TOKEN: each token gradient is divided by the total valid tokens
                // in the accumulation window so one optimizer step = mean-per-token gradient.
                // SUM: raw per-token gradients (loss is the plain sum over valid tokens),
                // no batch-size or window-size dependent normalization.
                float normFactor = (lossReduction == LossReduction.MEAN_PER_TOKEN)
                        ? accumulationWindowTokens
                        : 1.0f;

                for (int v = 0; v < vocabSize; v++) {
                    float prob = (float) Math.exp(logitsData[offset + v] - logSumExp);
                    float targetDist = (v == target) ? 1.0f : 0.0f;
                    dLogitsData[offset + v] = ((prob - targetDist) * mask[t]) / normFactor;
                }
            }

            long bwdStart = System.nanoTime();
            model.backward(dLogits);
            long bwdEnd = System.nanoTime();
            forwardNanosTotal += (fwdEnd - fwdStart);
            backwardNanosTotal += (bwdEnd - bwdStart);
        }
        return microBatchRawLoss;
    }

    public double evaluateValidation(List<TrainingSample> valSamples) {
        if (valSamples.isEmpty()) return 0.0;
        double totalLoss = 0.0;
        long totalValidTokens = 0;

        for (TrainingSample sample : valSamples) {
            Tensor logits = model.forward(sample.inputIds, sample.getAttentionMaskAsFloat());
            float[] logitsData = logits.getData();
            int seqLen = sample.inputIds.length;
            int vocabSize = (int) logits.getShape()[1];

            for (int t = 0; t < seqLen; t++) {
                if (!sample.lossMask[t]) continue;
                totalValidTokens++;

                int offset = t * vocabSize;
                int target = sample.labels[t];

                float maxLogit = Float.NEGATIVE_INFINITY;
                for (int v = 0; v < vocabSize; v++) maxLogit = Math.max(maxLogit, logitsData[offset + v]);

                float sumExp = 0.0f;
                for (int v = 0; v < vocabSize; v++) sumExp += (float) Math.exp(logitsData[offset + v] - maxLogit);

                float logSumExp = maxLogit + (float) Math.log(sumExp);
                totalLoss += (logSumExp - logitsData[offset + target]);
            }
        }
        return totalValidTokens > 0 ? (totalLoss / totalValidTokens) : 0.0;
    }

    private List<TrainingBatch> buildBatches(List<TrainingSample> samples, int batchSize) {
        List<TrainingBatch> batches = new ArrayList<>();
        for (int i = 0; i < samples.size(); i += batchSize) {
            int end = Math.min(i + batchSize, samples.size());
            List<TrainingSample> sub = samples.subList(i, end);
            int curBatchSize = sub.size();
            int maxLen = 0;
            for (TrainingSample s : sub) maxLen = Math.max(maxLen, s.inputIds.length);

            int[][] bInputs = new int[curBatchSize][maxLen];
            int[][] bLabels = new int[curBatchSize][maxLen];
            float[][] bMasks = new float[curBatchSize][maxLen];
            float[][] bAttMasks = new float[curBatchSize][maxLen];

            for (int r = 0; r < curBatchSize; r++) {
                TrainingSample s = sub.get(r);
                for (int c = 0; c < maxLen; c++) {
                    if (c < s.inputIds.length) {
                        bInputs[r][c] = s.inputIds[c];
                        bLabels[r][c] = s.labels[c];
                        bMasks[r][c] = s.lossMask[c] ? 1.0f : 0.0f;
                        bAttMasks[r][c] = 1.0f;
                    } else {
                        bInputs[r][c] = 0;
                        bLabels[r][c] = 0;
                        bMasks[r][c] = 0.0f;
                        bAttMasks[r][c] = 0.0f;
                    }
                }
            }
            batches.add(new TrainingBatch(bInputs, bLabels, bMasks, bAttMasks));
        }
        return batches;
    }

    private float computeScheduledLr(int step, int totalSteps, int warmupSteps) {
        if (step <= warmupSteps) return initialLr * ((float) step / warmupSteps);
        float progress = (float) (step - warmupSteps) / Math.max(1, totalSteps - warmupSteps);
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        float cosineDecay = 0.5f * (1.0f + (float) Math.cos(Math.PI * progress));
        return minLr + (initialLr - minLr) * cosineDecay;
    }

    public static class ModelParameterGroups {
        public final List<Tensor> decayParameters = new ArrayList<>();
        public final List<Tensor> noDecayParameters = new ArrayList<>();

        public static ModelParameterGroups fromModel(EvoLlmModel model) {
            ModelParameterGroups groups = new ModelParameterGroups();
            for (Tensor p : model.parameters()) {
                if (p.getRank() == 1 || p.getShape().length == 1) {
                    groups.noDecayParameters.add(p);
                } else {
                    groups.decayParameters.add(p);
                }
            }
            return groups;
        }

        public void zeroGradAll() {
            decayParameters.forEach(Tensor::zeroGrad);
            noDecayParameters.forEach(Tensor::zeroGrad);
        }
    }

    public static class EmbeddedAdamW {
        private float lr;
        private final float beta1, beta2, eps, weightDecay;
        private int stepCounter = 0;
        private final Map<Tensor, float[]> m = new IdentityHashMap<>();
        private final Map<Tensor, float[]> v = new IdentityHashMap<>();

        public EmbeddedAdamW(float lr, float beta1, float beta2, float eps, float weightDecay) {
            this.lr = lr; this.beta1 = beta1; this.beta2 = beta2; this.eps = eps; this.weightDecay = weightDecay;
        }

        public void setLr(float lr) { this.lr = lr; }
        public float getLr() { return lr; }
        public int getStepCounter() { return stepCounter; }

        public Map<String, float[]> getMVectorMap(List<Tensor> paramList, List<String> canonicalNames) {
            Map<String, float[]> res = new HashMap<>();
            for (int i = 0; i < paramList.size(); i++) {
                Tensor p = paramList.get(i);
                float[] mArr = m.get(p);
                if (mArr == null) continue;
                String canonical = canonicalNames != null && i < canonicalNames.size() ? canonicalNames.get(i) : null;
                if (canonical != null && !canonical.isEmpty()) {
                    res.put("n_" + canonical, mArr.clone());
                } else {
                    res.put("p_" + i, mArr.clone());
                }
            }
            return res;
        }

        public Map<String, float[]> getVVectorMap(List<Tensor> paramList, List<String> canonicalNames) {
            Map<String, float[]> res = new HashMap<>();
            for (int i = 0; i < paramList.size(); i++) {
                Tensor p = paramList.get(i);
                float[] vArr = v.get(p);
                if (vArr == null) continue;
                String canonical = canonicalNames != null && i < canonicalNames.size() ? canonicalNames.get(i) : null;
                if (canonical != null && !canonical.isEmpty()) {
                    res.put("n_" + canonical, vArr.clone());
                } else {
                    res.put("p_" + i, vArr.clone());
                }
            }
            return res;
        }

        public void loadState(List<Tensor> paramList, Map<String, float[]> mVectors, Map<String, float[]> vVectors, int stepCount) {
            loadState(paramList, Collections.emptyList(), mVectors, vVectors, stepCount);
        }

        public void loadState(List<Tensor> paramList, List<String> canonicalNames, Map<String, float[]> mVectors, Map<String, float[]> vVectors, int stepCount) {
            this.stepCounter = stepCount;
            this.m.clear();
            this.v.clear();
            for (int i = 0; i < paramList.size(); i++) {
                Tensor p = paramList.get(i);
                String canonical = canonicalNames != null && i < canonicalNames.size() ? canonicalNames.get(i) : null;
                String k = (canonical != null && !canonical.isEmpty()) ? "n_" + canonical : null;
                String legacyK = "p_" + i;
                String mKey = (k != null && mVectors != null && mVectors.containsKey(k)) ? k : legacyK;
                String vKey = (k != null && vVectors != null && vVectors.containsKey(k)) ? k : legacyK;
                if (mVectors != null && mVectors.containsKey(mKey)) {
                    this.m.put(p, mVectors.get(mKey).clone());
                }
                if (vVectors != null && vVectors.containsKey(vKey)) {
                    this.v.put(p, vVectors.get(vKey).clone());
                }
            }
        }

        public void step(ModelParameterGroups groups, float maxGradNorm) {
            stepCounter++;

            double sumSquareGrads = 0.0;
            sumSquareGrads += calculateGroupGradNorm(groups.decayParameters);
            sumSquareGrads += calculateGroupGradNorm(groups.noDecayParameters);

            float globalNorm = (float) Math.sqrt(sumSquareGrads);
            float clipScale = (maxGradNorm > 0.0f && globalNorm > maxGradNorm) ? (maxGradNorm / globalNorm) : 1.0f;

            float biasCorrection1 = 1.0f - (float) Math.pow(beta1, stepCounter);
            float biasCorrection2 = 1.0f - (float) Math.pow(beta2, stepCounter);

            updateGroup(groups.decayParameters, clipScale, biasCorrection1, biasCorrection2, true);
            updateGroup(groups.noDecayParameters, clipScale, biasCorrection1, biasCorrection2, false);
        }

        private double calculateGroupGradNorm(List<Tensor> params) {
            double sum = 0.0;
            for (Tensor p : params) {
                float[] grad = p.getGrad();
                if (grad != null) {
                    for (float g : grad) sum += g * g;
                }
            }
            return sum;
        }

        private void updateGroup(List<Tensor> params, float clipScale, float bc1, float bc2, boolean applyDecay) {
            for (Tensor p : params) {
                float[] data = p.getData();
                float[] grad = p.getGrad();
                if (data == null || grad == null) continue;

                int length = data.length;
                float[] mArr = m.computeIfAbsent(p, k -> new float[length]);
                float[] vArr = v.computeIfAbsent(p, k -> new float[length]);

                for (int i = 0; i < length; i++) {
                    float g = grad[i] * clipScale;

                    if (applyDecay && weightDecay > 0.0f) {
                        data[i] -= lr * weightDecay * data[i];
                    }

                    mArr[i] = beta1 * mArr[i] + (1.0f - beta1) * g;
                    vArr[i] = beta2 * vArr[i] + (1.0f - beta2) * g * g;

                    float mHat = mArr[i] / bc1;
                    float vHat = vArr[i] / bc2;

                    data[i] -= lr * mHat / ((float) Math.sqrt(vHat) + eps);
                }
            }
        }
    }
}
