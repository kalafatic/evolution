package eu.kalafatic.evolution.forge.trainer.impl.llm;

import eu.kalafatic.evolution.forge.data.api.TrainingBatch;
import eu.kalafatic.evolution.forge.data.api.TrainingSample;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.trainer.api.LossReduction;

import java.util.*;

public class EvoLlmTrainer {

    public enum TrainingProfile {
        EVO_FAST(2e-4f, 1e-5f, 0.01f),
        EVO_CODER(1e-4f, 1e-5f, 0.1f),
        EVO_FINETUNE(2e-5f, 1e-6f, 0.01f);

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

    public EvoLlmTrainer(EvoLlmModel model, TrainingProfile profile) {
        if (model == null) throw new IllegalArgumentException("EvoLlmModel cannot be null");
        this.model = model;
        applyProfile(profile);
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

    public void train(List<TrainingSample> samples, int epochs) {
        if (samples == null || samples.isEmpty()) return;

        // 1. Initial Split (Fixed Validation Set)
        List<TrainingSample> dataset = new ArrayList<>(samples);
        Collections.shuffle(dataset, new Random(baseSeed));

        int valCount = (int) (dataset.size() * validationSplitRatio);
        List<TrainingSample> trainSamples = new ArrayList<>(dataset.subList(0, dataset.size() - valCount));
        List<TrainingSample> valSamples = valCount > 0 ? new ArrayList<>(dataset.subList(dataset.size() - valCount, dataset.size())) : Collections.emptyList();

        // Separate Parameter Groups (Decay vs No-Decay)
        ModelParameterGroups paramGroups = ModelParameterGroups.fromModel(model);
        EmbeddedAdamW optimizer = new EmbeddedAdamW(initialLr, beta1, beta2, eps, weightDecay);

        for (int epoch = 0; epoch < epochs; epoch++) {
            // Re-shuffle train samples per epoch
            Collections.shuffle(trainSamples, new Random(baseSeed + epoch + 1));
            List<TrainingBatch> batches = buildBatches(trainSamples, microBatchSize);

            // Ceil-based calculation for optimization steps
            int stepsPerEpoch = (batches.size() + accumulationSteps - 1) / accumulationSteps;
            int totalOptimizationSteps = epochs * stepsPerEpoch;
            int warmupSteps = Math.max(1, (int) (totalOptimizationSteps * 0.03f));

            double epochTotalLoss = 0.0;
            long epochValidTokens = 0;
            long startTime = System.currentTimeMillis();

            int optStep = epoch * stepsPerEpoch;

            for (int bIdx = 0; bIdx < batches.size(); bIdx += accumulationSteps) {
                optStep++;
                float currentLr = computeScheduledLr(optStep, totalOptimizationSteps, warmupSteps);
                optimizer.setLr(currentLr);

                // Zero gradients BEFORE accumulation window starts
                paramGroups.zeroGradAll();

                int windowEnd = Math.min(bIdx + accumulationSteps, batches.size());
                List<TrainingBatch> accumulationWindow = batches.subList(bIdx, windowEnd);

                // 1. First Pass: Compute total valid tokens in the entire accumulation window
                long windowValidTokens = 0;
                for (TrainingBatch batch : accumulationWindow) {
                    for (float[] maskRow : batch.lossMask) {
                        for (float m : maskRow) {
                            if (m > 0.0f) windowValidTokens++;
                        }
                    }
                }

                if (windowValidTokens == 0) continue; // Skip window if no valid tokens

                // 2. Second Pass: Forward & Backward pass with accurate normalization
                double windowLoss = 0.0;
                for (TrainingBatch batch : accumulationWindow) {
                    windowLoss += processMicroBatch(batch, windowValidTokens);
                }

                epochTotalLoss += windowLoss;
                epochValidTokens += windowValidTokens;

                // 3. Optimizer Step
                optimizer.step(paramGroups, maxGradNorm);
            }

            double avgTrainLoss = epochValidTokens > 0 ? epochTotalLoss / epochValidTokens : 0.0;
            double avgValLoss = evaluateValidation(valSamples);
            long duration = System.currentTimeMillis() - startTime;

            System.out.printf("[EVO Epoch %d/%d] Train Loss (NLL/token): %.4f | Val Loss (NLL/token): %.4f | Time: %d ms%n",
                    epoch + 1, epochs, avgTrainLoss, avgValLoss, duration);
        }
    }

    private double processMicroBatch(TrainingBatch batch, long accumulationWindowTokens) {
        double microBatchRawLoss = 0.0;

        for (int i = 0; i < batch.batchSize; i++) {
            int[] inputIds = batch.inputIds[i];
            int[] labels = batch.labels[i];
            float[] mask = batch.lossMask[i];
            float[] attMask = batch.attentionMask[i]; // Attention mask passed down
            int seqLen = inputIds.length;

            // Model forward pass: Expected to handle attentionMask internally
            Tensor logits = model.forward(inputIds, attMask);
            float[] logitsData = logits.getData();
            int vocabSize = (int) logits.getShape()[1];

            Tensor dLogits = new SimpleTensor(seqLen, vocabSize);
            float[] dLogitsData = dLogits.getData();

            for (int t = 0; t < seqLen; t++) {
                if (mask[t] == 0.0f) continue; // Loss masked

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

                // Gradient normalized by strict total window tokens
                float normFactor = (lossReduction == LossReduction.MEAN_PER_TOKEN)
                        ? accumulationWindowTokens
                        : (accumulationWindowTokens / (float) batch.batchSize);

                for (int v = 0; v < vocabSize; v++) {
                    float prob = (float) Math.exp(logitsData[offset + v] - logSumExp);
                    float targetDist = (v == target) ? 1.0f : 0.0f;
                    dLogitsData[offset + v] = ((prob - targetDist) * mask[t]) / normFactor;
                }
            }

            // BACKWARD: Must ACCUMULATE (grad += dLogits) internally in model!
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
                        bAttMasks[r][c] = 1.0f; // Token exists
                    } else {
                        bInputs[r][c] = 0; // Padding token ID
                        bLabels[r][c] = 0;
                        bMasks[r][c] = 0.0f; // Loss masked
                        bAttMasks[r][c] = 0.0f; // Attention masked
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
        progress = Math.max(0.0f, Math.min(1.0f, progress)); // Safe clamping
        float cosineDecay = 0.5f * (1.0f + (float) Math.cos(Math.PI * progress));
        return minLr + (initialLr - minLr) * cosineDecay;
    }

    // =========================================================================
    // Parameter Group Management (Decay vs No-Decay)
    // =========================================================================
    public static class ModelParameterGroups {
        public final List<Tensor> decayParameters = new ArrayList<>();
        public final List<Tensor> noDecayParameters = new ArrayList<>();

        public static ModelParameterGroups fromModel(EvoLlmModel model) {
            ModelParameterGroups groups = new ModelParameterGroups();
            for (Tensor p : model.parameters()) {
                // Heuristic: RMSNorm/LayerNorm weights, biases, and embeddings usually skip weight decay
                if (p.getName() != null && (p.getName().contains("norm") || p.getName().contains("bias") || p.getName().contains("embed"))) {
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

    // =========================================================================
    // Parameter-Group-Aware AdamW
    // =========================================================================
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

            // 1. Calculate Global Gradient Norm across ALL parameters
            double sumSquareGrads = 0.0;
            sumSquareGrads += calculateGroupGradNorm(groups.decayParameters);
            sumSquareGrads += calculateGroupGradNorm(groups.noDecayParameters);

            float globalNorm = (float) Math.sqrt(sumSquareGrads);
            float clipScale = (maxGradNorm > 0.0f && globalNorm > maxGradNorm) ? (maxGradNorm / globalNorm) : 1.0f;

            float biasCorrection1 = 1.0f - (float) Math.pow(beta1, stepCounter);
            float biasCorrection2 = 1.0f - (float) Math.pow(beta2, stepCounter);

            // Step with Decay
            updateGroup(groups.decayParameters, clipScale, biasCorrection1, biasCorrection2, true);
            // Step WITHOUT Decay
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
