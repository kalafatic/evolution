package eu.kalafatic.evolution.forge.trainer.impl.llm;

import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.data.impl.DatasetBuilder;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import eu.kalafatic.evolution.forge.trainer.impl.EvoAdamW;
import java.util.List;
import java.util.ArrayList;

public class EvoLlmTrainer {
    private final EvoLlmModel model;
    private final List<Double> lossHistory = new ArrayList<>();

    public EvoLlmTrainer(EvoLlmModel model) {
        this.model = model;
    }

    public void train(List<DatasetBuilder.Sample> samples, int epochs) {
        System.out.println("[Training] Starting genuine EVO training with " + samples.size() + " samples.");
        EvoAdamW optimizer = new EvoAdamW(0.01f, 0.9f, 0.999f, 1e-8f, 0.01f);

        for (int epoch = 0; epoch < epochs; epoch++) {
            double epochLoss = 0;
            long startTime = System.currentTimeMillis();
            int totalTokensTrained = 0;
            
            for (DatasetBuilder.Sample sample : samples) {
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
            }
            
            double avgLoss = epochLoss / samples.size();
            lossHistory.add(avgLoss);
            long duration = System.currentTimeMillis() - startTime;
            double tokensPerSec = duration > 0 ? (totalTokensTrained * 1000.0 / duration) : 0;

            System.out.println(String.format("[EVO Training]\nEpoch %d/%d\nLoss: %.4f\nTokens: %d\nTime: %ds\nTokens/sec: %.2f\nLearning Rate: %.6f\n",
                epoch + 1, epochs, avgLoss, totalTokensTrained, duration / 1000, tokensPerSec, optimizer.getLr()));
        }
    }

    public List<Double> getLossHistory() { return lossHistory; }
}
