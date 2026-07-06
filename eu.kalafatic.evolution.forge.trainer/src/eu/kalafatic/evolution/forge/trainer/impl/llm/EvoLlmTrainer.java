package eu.kalafatic.evolution.forge.trainer.impl.llm;

import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.data.impl.DatasetBuilder;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import java.util.List;
import java.util.ArrayList;

public class EvoLlmTrainer {
    private final EvoLlmModel model;
    private final List<Double> lossHistory = new ArrayList<>();

    public EvoLlmTrainer(EvoLlmModel model) {
        this.model = model;
    }

    public void train(List<DatasetBuilder.Sample> samples, int epochs) {
        System.out.println("[Training] Starting training with " + samples.size() + " samples.");
        for (int epoch = 0; epoch < epochs; epoch++) {
            double epochLoss = 0;
            long startTime = System.currentTimeMillis();

            for (DatasetBuilder.Sample sample : samples) {
                int[] inputIds = sample.input.stream().mapToInt(i -> i).toArray();
                Tensor logits = model.forward(inputIds);

                // Simplified Backpropagation:
                // We update the weights of the Linear Head (lmHead) based on the cross-entropy gradient.
                // grad_logits = softmax(logits) - target_one_hot
                updateLmHead(logits, sample.target, 0.01f);

                double loss = calculateSimplifiedLoss(logits, sample.target);
                epochLoss += loss;
            }

            double avgLoss = epochLoss / samples.size();
            lossHistory.add(avgLoss);
            long duration = System.currentTimeMillis() - startTime;
            System.out.println(String.format("[Training] Epoch %d/%d - Loss: %.4f - Duration: %dms",
                epoch + 1, epochs, avgLoss, duration));
        }
    }

    private void updateLmHead(Tensor logits, int target, float lr) {
        float[] data = logits.getData();
        int seqLen = (int) logits.getShape()[0];
        int vocabSize = (int) logits.getShape()[1];
        int lastTokenOffset = (seqLen - 1) * vocabSize;

        // Softmax
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < vocabSize; i++) {
            if (data[lastTokenOffset + i] > max) max = data[lastTokenOffset + i];
        }
        float sum = 0;
        float[] probs = new float[vocabSize];
        for (int i = 0; i < vocabSize; i++) {
            probs[i] = (float) Math.exp(data[lastTokenOffset + i] - max);
            sum += probs[i];
        }
        for (int i = 0; i < vocabSize; i++) probs[i] /= sum;

        // Gradient: probs[target] -= 1
        probs[target] -= 1.0f;

        // Simple SGD update for lmHead weights
        float[] headData = model.getLmHead().getData();
        for (int i = 0; i < vocabSize; i++) {
            // Very simplified: update only the bias-like effect for the specific token
            headData[i] -= probs[i] * lr;
        }
    }

    private double calculateSimplifiedLoss(Tensor logits, int target) {
        // Simple CrossEntropy simulation: -log(softmax(logits)[target])
        float[] data = logits.getData();
        int seqLen = (int) logits.getShape()[0];
        int vocabSize = (int) logits.getShape()[1];

        // Use last token prediction
        int lastTokenOffset = (seqLen - 1) * vocabSize;
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < vocabSize; i++) {
            if (data[lastTokenOffset + i] > max) max = data[lastTokenOffset + i];
        }

        double sum = 0;
        for (int i = 0; i < vocabSize; i++) {
            sum += Math.exp(data[lastTokenOffset + i] - max);
        }

        double prob = Math.exp(data[lastTokenOffset + target] - max) / sum;
        return -Math.log(Math.max(prob, 1e-10));
    }

    public List<Double> getLossHistory() { return lossHistory; }
}
