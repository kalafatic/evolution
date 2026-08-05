package eu.kalafatic.evolution.forge.trainer.impl.llm;

import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.data.impl.DatasetBuilder;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import eu.kalafatic.evolution.forge.trainer.impl.EvoAdamW;
import java.util.List;
import java.util.ArrayList;

public class EvoLlmTrainer {
    public interface ProgressListener {
        void onProgress(int epoch, int totalEpochs, int sampleIndex, int totalSamples, double currentLoss);
    }

    private final EvoLlmModel model;
    private final List<Double> lossHistory = new ArrayList<>();
    private ProgressListener progressListener;
    private String jobId = "forge-job";

    private static final double ETA_ALPHA = 0.05;

    public EvoLlmTrainer(EvoLlmModel model) {
        this.model = model;
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public void train(List<DatasetBuilder.Sample> samples, int epochs) {
        train(samples, epochs, null);
    }

    public void train(List<DatasetBuilder.Sample> samples, int epochs, TrainingProgressListener listener) {
        System.out.println("[Training] Starting genuine EVO training with " + samples.size() + " samples.");
        EvoAdamW optimizer = new EvoAdamW(0.01f, 0.9f, 0.999f, 1e-8f, 0.01f);

        int totalSamples = samples.size();
        long totalSteps = 0;
        try {
            totalSteps = Math.multiplyExact((long) epochs, (long) totalSamples);
        } catch (ArithmeticException e) {
            totalSteps = Long.MAX_VALUE;
        }

        long startTime = System.currentTimeMillis();

        if (listener != null) {
            try {
                TrainingProgress initProgress = TrainingProgress.builder()
                    .jobId(jobId)
                    .status(TrainingStatus.INITIALIZING)
                    .phase("INITIALIZING")
                    .currentEpoch(1)
                    .totalEpochs(epochs)
                    .currentStep(0)
                    .totalSteps(totalSteps)
                    .currentBatch(0)
                    .batchesPerEpoch(totalSamples)
                    .progressPercent(0.0)
                    .trainingLoss(0.0)
                    .validationLoss(0.0)
                    .learningRate(optimizer.getLr())
                    .stepsPerSecond(0.0)
                    .elapsedMillis(0)
                    .estimatedRemainingMillis(-1)
                    .message("Initializing genuine EVO training with " + totalSamples + " samples.")
                    .build();
                listener.onProgress(initProgress);
            } catch (Exception e) {
                System.err.println("[EvoLlmTrainer] Progress listener initialization failure: " + e.getMessage());
            }
        }

        double stepsPerSecond = 0.0;
        long validStepsCount = 0;

        try {
            for (int epoch = 0; epoch < epochs; epoch++) {
                double epochLoss = 0;
                long epochStartTime = System.currentTimeMillis();
                int totalTokensTrained = 0;
                int sampleIndex = 0;

                for (DatasetBuilder.Sample sample : samples) {
                    long stepStartTime = System.nanoTime();

                    // Zero gradients
                    model.parameters().forEach(Tensor::zeroGrad);

                    int[] inputIds = sample.input.stream().mapToInt(i -> i).toArray();
                    Tensor logits = model.forward(inputIds);
                    totalTokensTrained += inputIds.length;

                    // Real loss & cross-entropy gradient
                    float[] logitsData = logits.getData();
                    int seqLen = (int) logits.getShape()[0];
                    int vocabSize = (int) logits.getShape()[1];
                    int lastOffset = (seqLen - 1) * vocabSize;
                    int target = sample.target;

                    // Softmax
                    float max = Float.NEGATIVE_INFINITY;
                    for (int i = 0; i < vocabSize; i++) {
                        if (logitsData[lastOffset + i] > max) max = logitsData[lastOffset + i];
                    }
                    float sum = 0;
                    float[] probs = new float[vocabSize];
                    for (int i = 0; i < vocabSize; i++) {
                        probs[i] = (float) Math.exp(logitsData[lastOffset + i] - max);
                        sum += probs[i];
                    }
                    for (int i = 0; i < vocabSize; i++) probs[i] /= sum;

                    double loss = -Math.log(Math.max(probs[target], 1e-10));
                    epochLoss += loss;

                    // Compute dLogits gradient
                    Tensor dLogits = new SimpleTensor(seqLen, vocabSize);
                    float[] dLogitsData = dLogits.getData();
                    probs[target] -= 1.0f; // cross entropy grad
                    for (int i = 0; i < vocabSize; i++) {
                        dLogitsData[lastOffset + i] = probs[i];
                    }

                    // Real Backpropagation
                    model.backward(dLogits);

                    // Optimizer Step
                    optimizer.step(model.parameters());

                    sampleIndex++;

                    long stepEndTime = System.nanoTime();
                    long stepDurationNs = stepEndTime - stepStartTime;
                    double stepDurationSec = stepDurationNs / 1_000_000_000.0;

                    if (stepDurationSec > 0.0 && stepDurationSec < 60.0) {
                        double currentStepsPerSecond = 1.0 / stepDurationSec;
                        if (stepsPerSecond == 0.0) {
                            stepsPerSecond = currentStepsPerSecond;
                        } else {
                            stepsPerSecond = (ETA_ALPHA * currentStepsPerSecond) + ((1.0 - ETA_ALPHA) * stepsPerSecond);
                        }
                        validStepsCount++;
                    }

                    long completedSteps = 0;
                    try {
                        completedSteps = Math.multiplyExact((long) epoch, (long) totalSamples) + sampleIndex;
                    } catch (ArithmeticException e) {
                        completedSteps = Long.MAX_VALUE;
                    }

                    double progressPercent = -1.0;
                    if (totalSteps > 0) {
                        progressPercent = Math.min(100.0, Math.max(0.0, (completedSteps * 100.0) / totalSteps));
                    }

                    long elapsed = System.currentTimeMillis() - startTime;
                    long estimatedRemainingMillis = -1;
                    if (validStepsCount >= 5 && stepsPerSecond > 0.0) {
                        long remainingSteps = totalSteps - completedSteps;
                        estimatedRemainingMillis = Math.round((remainingSteps / stepsPerSecond) * 1000.0);
                    }

                    if (listener != null) {
                        try {
                            TrainingProgress progress = TrainingProgress.builder()
                                .jobId(jobId)
                                .status(TrainingStatus.TRAINING)
                                .phase("TRAINING")
                                .currentEpoch(epoch + 1)
                                .totalEpochs(epochs)
                                .currentStep(completedSteps)
                                .totalSteps(totalSteps)
                                .currentBatch(sampleIndex)
                                .batchesPerEpoch(totalSamples)
                                .progressPercent(progressPercent)
                                .trainingLoss(loss)
                                .validationLoss(0.0)
                                .learningRate(optimizer.getLr())
                                .stepsPerSecond(stepsPerSecond)
                                .elapsedMillis(elapsed)
                                .estimatedRemainingMillis(estimatedRemainingMillis)
                                .message(String.format("Epoch %d/%d | Sample %d/%d (%.1f%%) | Loss: %.4f",
                                    epoch + 1, epochs, sampleIndex, totalSamples, progressPercent, loss))
                                .build();
                            listener.onProgress(progress);
                        } catch (Exception e) {
                            System.err.println("[EvoLlmTrainer] Progress listener report failure: " + e.getMessage());
                        }
                    }

                    if (progressListener != null) {
                        try {
                            progressListener.onProgress(epoch, epochs, sampleIndex, totalSamples, loss);
                        } catch (Exception e) {
                            System.err.println("[EvoLlmTrainer] Old progress listener invocation failure: " + e.getMessage());
                        }
                    }

                    int logInterval = Math.max(1, totalSamples / 10);
                    if (sampleIndex % logInterval == 0 || sampleIndex == totalSamples) {
                        double pct = (double) sampleIndex / totalSamples * 100.0;
                        System.out.println(String.format("[EVO Training Progress] Epoch %d/%d | %d/%d samples (%.1f%%) | Loss: %.4f",
                            epoch + 1, epochs, sampleIndex, totalSamples, pct, loss));
                    }
                }

                double avgLoss = epochLoss / totalSamples;
                lossHistory.add(avgLoss);
                long duration = System.currentTimeMillis() - epochStartTime;
                double tokensPerSec = duration > 0 ? (totalTokensTrained * 1000.0 / duration) : 0;

                System.out.println(String.format("[EVO Training]\nEpoch %d/%d\nLoss: %.4f\nTokens: %d\nTime: %ds\nTokens/sec: %.2f\nLearning Rate: %.6f\n",
                    epoch + 1, epochs, avgLoss, totalTokensTrained, duration / 1000, tokensPerSec, optimizer.getLr()));
            }

            if (listener != null) {
                try {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double finalLoss = lossHistory.isEmpty() ? 0.0 : lossHistory.get(lossHistory.size() - 1);
                    TrainingProgress progress = TrainingProgress.builder()
                        .jobId(jobId)
                        .status(TrainingStatus.COMPLETED)
                        .phase("COMPLETED")
                        .currentEpoch(epochs)
                        .totalEpochs(epochs)
                        .currentStep(totalSteps)
                        .totalSteps(totalSteps)
                        .currentBatch(totalSamples)
                        .batchesPerEpoch(totalSamples)
                        .progressPercent(100.0)
                        .trainingLoss(finalLoss)
                        .validationLoss(0.0)
                        .learningRate(0.0)
                        .stepsPerSecond(stepsPerSecond)
                        .elapsedMillis(elapsed)
                        .estimatedRemainingMillis(0)
                        .message("Training completed successfully in " + (elapsed / 1000) + " seconds.")
                        .build();
                    listener.onProgress(progress);
                } catch (Exception e) {
                    System.err.println("[EvoLlmTrainer] Progress listener completed notification failure: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            if (listener != null) {
                try {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double lastLoss = lossHistory.isEmpty() ? 10.0 : lossHistory.get(lossHistory.size() - 1);
                    TrainingProgress progress = TrainingProgress.builder()
                        .jobId(jobId)
                        .status(TrainingStatus.FAILED)
                        .phase("FAILED")
                        .currentEpoch(0)
                        .totalEpochs(epochs)
                        .currentStep(0)
                        .totalSteps(totalSteps)
                        .currentBatch(0)
                        .batchesPerEpoch(totalSamples)
                        .progressPercent(0.0)
                        .trainingLoss(lastLoss)
                        .validationLoss(0.0)
                        .learningRate(0.0)
                        .stepsPerSecond(0.0)
                        .elapsedMillis(elapsed)
                        .estimatedRemainingMillis(-1)
                        .message("Training failed: " + e.getMessage())
                        .build();
                    listener.onProgress(progress);
                } catch (Exception ex) {
                    System.err.println("[EvoLlmTrainer] Progress listener failed notification failure: " + ex.getMessage());
                }
            }
            throw e;
        }
    }

    public List<Double> getLossHistory() { return lossHistory; }
}
