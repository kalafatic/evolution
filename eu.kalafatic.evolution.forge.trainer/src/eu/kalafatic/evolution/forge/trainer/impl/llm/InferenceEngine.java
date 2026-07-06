package eu.kalafatic.evolution.forge.trainer.impl.llm;

import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.tokenizer.api.Tokenizer;
import java.util.*;

public class InferenceEngine {
    private final EvoLlmModel model;
    private final Tokenizer tokenizer;

    public InferenceEngine(EvoLlmModel model, Tokenizer tokenizer) {
        this.model = model;
        this.tokenizer = tokenizer;
    }

    public String generate(String prompt, int maxTokens, float temperature) {
        List<Integer> tokens = tokenizer.encode(prompt);
        Random rand = new Random();

        for (int i = 0; i < maxTokens; i++) {
            int[] inputIds = tokens.stream().mapToInt(it -> it).toArray();
            Tensor logits = model.forward(inputIds);
            int nextToken = sample(logits, temperature, rand);
            tokens.add(nextToken);
            if (nextToken == 3) break; // EOS
        }

        return tokenizer.decode(tokens);
    }

    private int sample(Tensor logits, float temperature, Random rand) {
        float[] data = logits.getData();
        int seqLen = (int) logits.getShape()[0];
        int vocabSize = (int) logits.getShape()[1];
        int offset = (seqLen - 1) * vocabSize;

        float[] probs = new float[vocabSize];
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < vocabSize; i++) {
            probs[i] = data[offset + i] / temperature;
            if (probs[i] > max) max = probs[i];
        }

        float sum = 0;
        for (int i = 0; i < vocabSize; i++) {
            probs[i] = (float) Math.exp(probs[i] - max);
            sum += probs[i];
        }

        float r = rand.nextFloat() * sum;
        float current = 0;
        for (int i = 0; i < vocabSize; i++) {
            current += probs[i];
            if (current >= r) return i;
        }
        return vocabSize - 1;
    }
}
