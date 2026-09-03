package eu.kalafatic.evolution.forge.trainer.impl.llm;

import eu.kalafatic.evolution.forge.data.impl.DatasetBuilder;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single-class complete EVO LLM Forging and Training Engine.
 * Implements professional-grade LLM training routines:
 * - Embedded AdamW optimizer with decoupled weight decay
 * - Learning rate scheduler with linear warmup and cosine decay
 * - Global gradient norm clipping
 * - Numerically stable cross-entropy with log-sum-exp trick
 * - Validation tracking & loss metrics
 * - Direct *.evo artifact persistence
 */
public class EvoLlmTrainer {

    public interface ProgressListener {
        void onProgress(int epoch, int totalEpochs, int sampleIndex, int totalSamples, double currentLoss);
    }

    private final EvoLlmModel model;
    private final List<Double> lossHistory = new ArrayList<>();
    private final List<Double> valLossHistory = new ArrayList<>();
    private ProgressListener progressListener;

    // Hyperparameters
    private float initialLr = 1e-3f;
    private float minLr = 1e-5f;
    private float beta1 = 0.9f;
    private float beta2 = 0.999f;
    private float eps = 1e-8f;
    private float weightDecay = 0.01f;
    private float maxGradNorm = 1.0f;
    private float labelSmoothing = 0.0f;
    private float validationSplitRatio = 0.1f;

    public EvoLlmTrainer(EvoLlmModel model) {
        if (model == null) {
            throw new IllegalArgumentException("EvoLlmModel cannot be null");
        }
        this.model = model;
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public void setInitialLr(float lr) { this.initialLr = lr; }
    public void setMinLr(float minLr) { this.minLr = minLr; }
    public void setWeightDecay(float weightDecay) { this.weightDecay = weightDecay; }
    public void setMaxGradNorm(float maxGradNorm) { this.maxGradNorm = maxGradNorm; }
    public void setLabelSmoothing(float labelSmoothing) { this.labelSmoothing = labelSmoothing; }
    public void setValidationSplitRatio(float validationSplitRatio) { this.validationSplitRatio = validationSplitRatio; }

    /**
     * Executes complete training loop over dataset samples for specified epochs.
     */
    public void train(List<DatasetBuilder.Sample> samples, int epochs) {
        if (samples == null || samples.isEmpty()) {
            System.out.println("[EVO Forging Engine] Warning: No training samples provided.");
            return;
        }

        // 1. Split dataset into train / validation sets
        int valCount = (int) (samples.size() * validationSplitRatio);
        List<DatasetBuilder.Sample> trainSamples = samples;
        List<DatasetBuilder.Sample> valSamples = null;

        if (valCount > 0 && valCount < samples.size()) {
            trainSamples = samples.subList(0, samples.size() - valCount);
            valSamples = samples.subList(samples.size() - valCount, samples.size());
        }

        System.out.println(String.format("[EVO Forging Engine] Starting training: %d train samples, %d val samples, %d epochs.",
                trainSamples.size(), valSamples != null ? valSamples.size() : 0, epochs));

        // 2. Initialize embedded AdamW optimizer and scheduler
        EmbeddedAdamW optimizer = new EmbeddedAdamW(initialLr, beta1, beta2, eps, weightDecay);
        int totalSteps = epochs * trainSamples.size();
        int warmupSteps = Math.max(1, (int) (totalSteps * 0.05f));
        int currentStep = 0;

        for (int epoch = 0; epoch < epochs; epoch++) {
            double epochLoss = 0.0;
            long startTime = System.currentTimeMillis();
            int totalTokensTrained = 0;
            int sampleIndex = 0;

            for (DatasetBuilder.Sample sample : trainSamples) {
                currentStep++;

                // Learning rate warmup + cosine decay schedule
                float currentLr = computeScheduledLr(currentStep, totalSteps, warmupSteps);
                optimizer.setLr(currentLr);

                // Zero gradients
                model.parameters().forEach(Tensor::zeroGrad);

                int inputLen = sample.input.size();
                int[] inputIds = new int[inputLen];
                for (int i = 0; i < inputLen; i++) {
                    inputIds[i] = sample.input.get(i);
                }

                // Forward pass
                Tensor logits = model.forward(inputIds);
                totalTokensTrained += inputIds.length;

                // Loss & Cross-Entropy computation with Log-Sum-Exp stability
                float[] logitsData = logits.getData();
                int seqLen = (int) logits.getShape()[0];
                int vocabSize = (int) logits.getShape()[1];
                int lastOffset = (seqLen - 1) * vocabSize;
                int target = sample.target;

                // Log-Sum-Exp & Softmax probabilities
                float maxLogit = Float.NEGATIVE_INFINITY;
                for (int i = 0; i < vocabSize; i++) {
                    float val = logitsData[lastOffset + i];
                    if (val > maxLogit) maxLogit = val;
                }

                float sumExp = 0.0f;
                float[] probs = new float[vocabSize];
                for (int i = 0; i < vocabSize; i++) {
                    probs[i] = (float) Math.exp(logitsData[lastOffset + i] - maxLogit);
                    sumExp += probs[i];
                }
                for (int i = 0; i < vocabSize; i++) probs[i] /= sumExp;

                double sampleLoss = -Math.log(Math.max(probs[target], 1e-10));
                epochLoss += sampleLoss;

                // Gradient computation with optional label smoothing
                Tensor dLogits = new SimpleTensor(seqLen, vocabSize);
                float[] dLogitsData = dLogits.getData();

                if (labelSmoothing > 0.0f) {
                    float smoothTarget = 1.0f - labelSmoothing;
                    float smoothUnused = labelSmoothing / (vocabSize - 1);
                    for (int i = 0; i < vocabSize; i++) {
                        float targetVal = (i == target) ? smoothTarget : smoothUnused;
                        dLogitsData[lastOffset + i] = probs[i] - targetVal;
                    }
                } else {
                    probs[target] -= 1.0f;
                    System.arraycopy(probs, 0, dLogitsData, lastOffset, vocabSize);
                }

                // Backpropagation pass
                model.backward(dLogits);

                // AdamW step with global gradient norm clipping
                optimizer.step(model.parameters(), maxGradNorm);

                sampleIndex++;

                if (progressListener != null) {
                    progressListener.onProgress(epoch, epochs, sampleIndex, trainSamples.size(), sampleLoss);
                }

                int logInterval = Math.max(1, trainSamples.size() / 10);
                if (sampleIndex % logInterval == 0 || sampleIndex == trainSamples.size()) {
                    double pct = (double) sampleIndex / trainSamples.size() * 100.0;
                    System.out.println(String.format("[EVO Training Progress] Epoch %d/%d | %d/%d samples (%.1f%%) | Loss: %.4f | LR: %.6f",
                            epoch + 1, epochs, sampleIndex, trainSamples.size(), pct, sampleLoss, currentLr));
                }
            }

            double avgTrainLoss = epochLoss / trainSamples.size();
            lossHistory.add(avgTrainLoss);

            // Validation evaluation pass
            double avgValLoss = 0.0;
            if (valSamples != null && !valSamples.isEmpty()) {
                avgValLoss = evaluateValidationLoss(valSamples);
                valLossHistory.add(avgValLoss);
            }

            long duration = System.currentTimeMillis() - startTime;
            double tokensPerSec = duration > 0 ? (totalTokensTrained * 1000.0 / duration) : 0;

            System.out.println(String.format("[EVO Forging Epoch %d/%d Complete]\n  Train Loss: %.4f\n  Val Loss: %.4f\n  Tokens/sec: %.2f\n  Duration: %d ms",
                    epoch + 1, epochs, avgTrainLoss, avgValLoss, tokensPerSec, duration));
        }
    }

    /**
     * Evaluates loss on validation set without parameter updates.
     */
    private double evaluateValidationLoss(List<DatasetBuilder.Sample> valSamples) {
        double totalLoss = 0.0;
        for (DatasetBuilder.Sample sample : valSamples) {
            int inputLen = sample.input.size();
            int[] inputIds = new int[inputLen];
            for (int i = 0; i < inputLen; i++) {
                inputIds[i] = sample.input.get(i);
            }

            Tensor logits = model.forward(inputIds);
            float[] logitsData = logits.getData();
            int seqLen = (int) logits.getShape()[0];
            int vocabSize = (int) logits.getShape()[1];
            int lastOffset = (seqLen - 1) * vocabSize;
            int target = sample.target;

            float maxLogit = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < vocabSize; i++) {
                float val = logitsData[lastOffset + i];
                if (val > maxLogit) maxLogit = val;
            }

            float sumExp = 0.0f;
            float[] probs = new float[vocabSize];
            for (int i = 0; i < vocabSize; i++) {
                probs[i] = (float) Math.exp(logitsData[lastOffset + i] - maxLogit);
                sumExp += probs[i];
            }
            float targetProb = probs[target] / sumExp;
            totalLoss += -Math.log(Math.max(targetProb, 1e-10));
        }
        return totalLoss / valSamples.size();
    }

    /**
     * Computes linear warmup and cosine decay learning rate.
     */
    private float computeScheduledLr(int step, int totalSteps, int warmupSteps) {
        if (step <= warmupSteps) {
            return initialLr * ((float) step / warmupSteps);
        }
        float progress = (float) (step - warmupSteps) / Math.max(1, totalSteps - warmupSteps);
        float cosineDecay = 0.5f * (1.0f + (float) Math.cos(Math.PI * progress));
        return minLr + (initialLr - minLr) * cosineDecay;
    }

    /**
     * Trains the model and persists the model directly into a single *.evo file.
     */
    public void trainAndPersist(List<DatasetBuilder.Sample> samples, int epochs, Path evoOutputPath, String modelName, Map<String, Integer> tokenizerVocab) throws IOException {
        train(samples, epochs);
        saveToEvoFile(evoOutputPath, modelName, tokenizerVocab);
    }

    /**
     * Persists current model state into a *.evo artifact file.
     */
    public void saveToEvoFile(Path evoOutputPath, String modelName, Map<String, Integer> tokenizerVocab) throws IOException {
        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel(modelName != null ? modelName : "evo_model", model, tokenizerVocab != null ? tokenizerVocab : new HashMap<>());
        artifact.save(evoOutputPath);
        System.out.println("[EVO Forging Engine] Model persisted successfully to *.evo file: " + evoOutputPath.toAbsolutePath());
    }

    public List<Double> getLossHistory() { return lossHistory; }
    public List<Double> getValLossHistory() { return valLossHistory; }
    public EvoLlmModel getModel() { return model; }

    // =========================================================================
    // Embedded Professional AdamW Optimizer Class (Keeps engine code inside 1 class)
    // =========================================================================
    private static class EmbeddedAdamW {
        private float lr;
        private final float beta1;
        private final float beta2;
        private final float eps;
        private final float weightDecay;
        private int stepCounter = 0;

        private final Map<Tensor, float[]> m = new HashMap<>();
        private final Map<Tensor, float[]> v = new HashMap<>();

        public EmbeddedAdamW(float lr, float beta1, float beta2, float eps, float weightDecay) {
            this.lr = lr;
            this.beta1 = beta1;
            this.beta2 = beta2;
            this.eps = eps;
            this.weightDecay = weightDecay;
        }

        public void setLr(float lr) { this.lr = lr; }
        public float getLr() { return lr; }

        public void step(List<Tensor> parameters, float maxGradNorm) {
            stepCounter++;

            // 1. Calculate global gradient norm
            double sumSquareGrads = 0.0;
            for (Tensor p : parameters) {
                float[] grad = p.getGrad();
                if (grad != null) {
                    for (float g : grad) {
                        sumSquareGrads += g * g;
                    }
                }
            }
            float globalNorm = (float) Math.sqrt(sumSquareGrads);

            // Compute gradient clipping scale
            float clipScale = 1.0f;
            if (maxGradNorm > 0.0f && globalNorm > maxGradNorm) {
                clipScale = maxGradNorm / globalNorm;
            }

            // Bias correction factors
            float biasCorrection1 = 1.0f - (float) Math.pow(beta1, stepCounter);
            float biasCorrection2 = 1.0f - (float) Math.pow(beta2, stepCounter);

            // 2. Update parameters
            for (Tensor p : parameters) {
                float[] data = p.getData();
                float[] grad = p.getGrad();
                if (data == null || grad == null) continue;

                int length = data.length;
                float[] mArr = m.computeIfAbsent(p, k -> new float[length]);
                float[] vArr = v.computeIfAbsent(p, k -> new float[length]);

                for (int i = 0; i < length; i++) {
                    float g = grad[i] * clipScale;

                    // Decoupled Weight Decay
                    if (weightDecay > 0.0f) {
                        data[i] -= lr * weightDecay * data[i];
                    }

                    // Moment estimates
                    mArr[i] = beta1 * mArr[i] + (1.0f - beta1) * g;
                    vArr[i] = beta2 * vArr[i] + (1.0f - beta2) * g * g;

                    float mHat = mArr[i] / biasCorrection1;
                    float vHat = vArr[i] / biasCorrection2;

                    data[i] -= lr * mHat / ((float) Math.sqrt(vHat) + eps);
                }
            }
        }
    }
}
