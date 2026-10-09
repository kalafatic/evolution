package eu.kalafatic.evolution.forge.trainer.impl.llm;

import eu.kalafatic.evolution.forge.data.api.TrainingBatch;
import eu.kalafatic.evolution.forge.data.api.TrainingSample;
import eu.kalafatic.evolution.forge.data.impl.DatasetBuilder;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.trainer.api.LossReduction;
import eu.kalafatic.evolution.forge.trainer.api.TerminationReason;
import eu.kalafatic.evolution.forge.trainer.api.TrainingResult;
import eu.kalafatic.evolution.forge.trainer.api.TrainingTerminationStatus;

import java.util.*;

public class EvoLlmTrainer {

    public interface ProgressListener {
        void onProgress(int epoch, int totalEpochs, int sampleIndex, int totalSamplesCount, double currentLoss);

        default void onProgressMetrics(int epoch, int totalEpochs, int currentStep, int totalSteps,
                                       long processedTokens, long totalTokens,
                                       double tokensPerSec, double stepsPerSec,
                                       double estimatedRemainingSeconds, double currentLoss) {
            onProgress(epoch, totalEpochs, currentStep, totalSteps, currentLoss);
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

    private LossReduction lossReduction = LossReduction.MEAN_PER_TOKEN;
    private ProgressListener progressListener;
    private final List<Double> lossHistory = new ArrayList<>();
    private volatile boolean stopRequested = false;
    private TrainingResult lastResult;

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
    public void setProgressListener(ProgressListener listener) { this.progressListener = listener; }
    public List<Double> getLossHistory() { return lossHistory; }
    public void requestStop() { this.stopRequested = true; }
    public void finishTraining() { this.stopRequested = true; }
    public boolean isStopRequested() { return stopRequested; }
    public TrainingResult getLastTrainingResult() { return lastResult; }

    public TrainingResult train(List<?> rawSamples, int epochs) {
        long startTime = System.currentTimeMillis();
        this.stopRequested = false;
        TrainingResult result = new TrainingResult();
        result.setPlannedEpochs(epochs);

        if (rawSamples == null || rawSamples.isEmpty()) {
            result.setTerminationStatus(TrainingTerminationStatus.TRAINING_FAILED);
            result.setTerminationReason(TerminationReason.NO_TRAINING_SAMPLES);
            result.setModelValid(false);
            result.setFailureDetails("No training samples provided to trainer.");
            this.lastResult = result;
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
            return result;
        }

        List<TrainingSample> dataset = new ArrayList<>(samples);
        Collections.shuffle(dataset, new Random(baseSeed));

        int valCount = (int) (dataset.size() * validationSplitRatio);
        List<TrainingSample> trainSamples = new ArrayList<>(dataset.subList(0, Math.max(1, dataset.size() - valCount)));
        List<TrainingSample> valSamples = valCount > 0 ? new ArrayList<>(dataset.subList(dataset.size() - valCount, dataset.size())) : Collections.emptyList();

        ModelParameterGroups paramGroups = ModelParameterGroups.fromModel(model);
        EmbeddedAdamW optimizer = new EmbeddedAdamW(initialLr, beta1, beta2, eps, weightDecay);

        lossHistory.clear();

        long paramCount = model.parameters().stream().mapToLong(Tensor::getSize).sum();
        result.getMetadata().put("trainingMode", "FROM_SCRATCH");
        result.getMetadata().put("pretrainedWeights", false);
        result.getMetadata().put("parameterCount", paramCount);
        result.getMetadata().put("baseSeed", baseSeed);

        System.out.printf("[EVO Trainer] Mode: FROM_SCRATCH | Pretrained Weights: FALSE | Parameters: %d | Base Seed: %d%n",
                paramCount, baseSeed);

        List<TrainingBatch> initialBatches = buildBatches(trainSamples, microBatchSize);
        int stepsPerEpoch = (initialBatches.size() + accumulationSteps - 1) / accumulationSteps;
        int totalOptimizationSteps = epochs * stepsPerEpoch;
        result.setPlannedSteps(totalOptimizationSteps);

        long epochTokenCount = 0;
        for (TrainingSample sample : trainSamples) {
            epochTokenCount += sample.inputIds.length;
        }
        long totalTokensToTrain = epochTokenCount * (long) epochs;
        long totalProcessedTokens = 0;
        long lastMeasurementTime = System.currentTimeMillis();
        long windowStartTokens = 0;
        int windowStartStep = 0;
        double movingAvgTokPerSec = 0.0;
        double movingAvgStepsPerSec = 0.0;

        try {
            for (int epoch = 0; epoch < epochs; epoch++) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.printf("[EVO Trainer] Thread interrupted before epoch %d/%d.%n", epoch + 1, epochs);
                    result.setTerminationStatus(TrainingTerminationStatus.TRAINING_INTERRUPTED);
                    result.setTerminationReason(TerminationReason.THREAD_INTERRUPTED);
                    break;
                }
                if (stopRequested) {
                    System.out.printf("[EVO Trainer] Graceful finish requested before epoch %d/%d. Completing training state.%n", epoch + 1, epochs);
                    result.setTerminationStatus(TrainingTerminationStatus.TRAINING_STOPPED);
                    result.setTerminationReason(TerminationReason.USER_REQUEST);
                    break;
                }
                Collections.shuffle(trainSamples, new Random(baseSeed + epoch + 1));
                List<TrainingBatch> batches = buildBatches(trainSamples, microBatchSize);

                int warmupSteps = Math.max(1, (int) (totalOptimizationSteps * 0.03f));

                double epochTotalLoss = 0.0;
                long epochValidTokens = 0;
                long epochStartTime = System.currentTimeMillis();

                int optStep = epoch * stepsPerEpoch;
                int actualCompletedSteps = 0;
                boolean epochStoppedEarly = false;

                for (int bIdx = 0; bIdx < batches.size(); bIdx += accumulationSteps) {
                    if (Thread.currentThread().isInterrupted()) {
                        System.out.printf("[EVO Trainer] Thread interrupted during epoch %d.%n", epoch + 1);
                        result.setTerminationStatus(TrainingTerminationStatus.TRAINING_INTERRUPTED);
                        result.setTerminationReason(TerminationReason.THREAD_INTERRUPTED);
                        epochStoppedEarly = true;
                        break;
                    }
                    if (stopRequested) {
                        System.out.printf("[EVO Trainer] Stop requested. Finishing current epoch %d boundary cleanly.%n", epoch + 1);
                        result.setTerminationStatus(TrainingTerminationStatus.TRAINING_STOPPED);
                        result.setTerminationReason(TerminationReason.USER_REQUEST);
                        epochStoppedEarly = true;
                        break;
                    }
                    optStep++;
                    float currentLr = computeScheduledLr(optStep, totalOptimizationSteps, warmupSteps);
                    optimizer.setLr(currentLr);

                    paramGroups.zeroGradAll();

                    int windowEnd = Math.min(bIdx + accumulationSteps, batches.size());
                    List<TrainingBatch> accumulationWindow = batches.subList(bIdx, windowEnd);

                    long windowValidTokens = 0;
                    for (TrainingBatch batch : accumulationWindow) {
                        for (float[] maskRow : batch.lossMask) {
                            for (float m : maskRow) {
                                if (m > 0.0f) windowValidTokens++;
                            }
                        }
                    }

                    if (windowValidTokens == 0) continue;

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

                    optimizer.step(paramGroups, maxGradNorm);
                    actualCompletedSteps++;

                    totalProcessedTokens += windowValidTokens;

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

                    if (progressListener != null) {
                        double currentLoss = windowValidTokens > 0 ? windowLoss / windowValidTokens : 0.0;
                        progressListener.onProgressMetrics(epoch, epochs, optStep, totalOptimizationSteps,
                                totalProcessedTokens, totalTokensToTrain,
                                movingAvgTokPerSec, movingAvgStepsPerSec,
                                remainingSec, currentLoss);
                    } else if ((bIdx / accumulationSteps) % 5 == 0 || bIdx + accumulationSteps >= batches.size()) {
                        double currentLoss = windowValidTokens > 0 ? windowLoss / windowValidTokens : 0.0;
                        int sampleIndex = Math.min(bIdx * microBatchSize, trainSamples.size());
                        System.out.printf("[EVO Trainer] Epoch %d/%d | Step %d/%d | Samples %d/%d | Loss: %.4f | Speed: %.1f tok/s | ETA: %.1f s%n",
                                epoch + 1, epochs, optStep, totalOptimizationSteps, sampleIndex, trainSamples.size(), currentLoss, movingAvgTokPerSec, remainingSec);
                    }
                }

                double avgTrainLoss = epochValidTokens > 0 ? epochTotalLoss / epochValidTokens : 0.0;
                double avgValLoss = evaluateValidation(valSamples);
                long epochDuration = System.currentTimeMillis() - epochStartTime;

                lossHistory.add(avgTrainLoss);
                model.getTrainingState().setEpoch(epoch + 1);
                model.getTrainingState().setLastLoss((float) avgTrainLoss);

                result.setCompletedEpochs(epoch + 1);
                result.setCompletedSteps(actualCompletedSteps);
                result.setLastLoss(avgTrainLoss);
                result.setValLoss(avgValLoss);

                System.out.printf("[EVO Epoch %d/%d] Train Loss (NLL/token): %.4f | Val Loss (NLL/token): %.4f | Time: %d ms%n",
                        epoch + 1, epochs, avgTrainLoss, avgValLoss, epochDuration);

                if (epochStoppedEarly) {
                    break;
                }
            }
        } catch (Exception e) {
            System.err.printf("[EVO Trainer] Exception caught during training: %s%n", e.getMessage());
            result.setTerminationStatus(TrainingTerminationStatus.TRAINING_FAILED);
            result.setTerminationReason(TerminationReason.EXCEPTION_THROWN);
            result.setModelValid(false);
            result.setFailureDetails("Exception caught during training loop: " + e.getMessage());
        }

        if (result.getTerminationStatus() == TrainingTerminationStatus.TRAINING_COMPLETED
                && result.getCompletedEpochs() < epochs) {
            // Default completion set if all requested epochs ran
            result.setTerminationStatus(TrainingTerminationStatus.TRAINING_COMPLETED);
            result.setTerminationReason(TerminationReason.PLANNED_COMPLETION);
        }

        result.setTrainingDurationMs(System.currentTimeMillis() - startTime);
        result.getMetadata().put("totalTokensTrained", totalProcessedTokens);
        result.getMetadata().put("tokensPerSecond", movingAvgTokPerSec);
        result.getMetadata().put("stepsPerSecond", movingAvgStepsPerSec);
        this.lastResult = result;
        return result;
    }

    private double processMicroBatch(TrainingBatch batch, long accumulationWindowTokens) {
        double microBatchRawLoss = 0.0;

        for (int i = 0; i < batch.batchSize; i++) {
            int[] inputIds = batch.inputIds[i];
            int[] labels = batch.labels[i];
            float[] mask = batch.lossMask[i];
            float[] attMask = batch.attentionMask[i];
            int seqLen = inputIds.length;

            Tensor logits = model.forward(inputIds, attMask);
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

                float normFactor = (lossReduction == LossReduction.MEAN_PER_TOKEN)
                        ? accumulationWindowTokens
                        : (accumulationWindowTokens / (float) batch.batchSize);

                for (int v = 0; v < vocabSize; v++) {
                    float prob = (float) Math.exp(logitsData[offset + v] - logSumExp);
                    float targetDist = (v == target) ? 1.0f : 0.0f;
                    dLogitsData[offset + v] = ((prob - targetDist) * mask[t]) / normFactor;
                }
            }

            model.backward(dLogits);
        }
        return microBatchRawLoss;
    }

    private double evaluateValidation(List<TrainingSample> valSamples) {
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

    private static class EmbeddedAdamW {
        private float lr;
        private final float beta1, beta2, eps, weightDecay;
        private int stepCounter = 0;
        private final Map<Tensor, float[]> m = new IdentityHashMap<>();
        private final Map<Tensor, float[]> v = new IdentityHashMap<>();

        public EmbeddedAdamW(float lr, float beta1, float beta2, float eps, float weightDecay) {
            this.lr = lr; this.beta1 = beta1; this.beta2 = beta2; this.eps = eps; this.weightDecay = weightDecay;
        }

        public void setLr(float lr) { this.lr = lr; }

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
