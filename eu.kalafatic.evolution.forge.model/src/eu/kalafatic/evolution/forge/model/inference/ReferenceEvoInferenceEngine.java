package eu.kalafatic.evolution.forge.model.inference;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.tokenizer.api.Tokenizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Reference implementation of EvoInferenceEngine.
 * Direct, zero-dependency native runtime for EvoLlmModel instances.
 * Prioritizes correctness, clarity, and mathematical exactness.
 */
public class ReferenceEvoInferenceEngine implements EvoInferenceEngine {

    @Override
    public void validateModel(EvoLlmModel model) {
        EvoModelValidator.validate(model);
    }

    @Override
    public Tensor forward(EvoLlmModel model, int[] inputIds) {
        validateModel(model);
        if (inputIds == null || inputIds.length == 0) {
            throw new IllegalArgumentException("Input token IDs cannot be null or empty");
        }

        // Forward pass mathematically identical to EvoLlmModel.forward()
        return model.forward(inputIds);
    }

    @Override
    public InferenceResult generate(EvoLlmModel model, InferenceRequest request, Tokenizer tokenizer) {
        validateModel(model);
        if (request == null) {
            throw new IllegalArgumentException("InferenceRequest cannot be null");
        }

        long startTime = System.currentTimeMillis();

        // 1. Resolve initial prompt token sequence
        List<Integer> currentTokens = new ArrayList<>();
        int[] initialIds = request.getInputIds();

        if (initialIds != null && initialIds.length > 0) {
            for (int id : initialIds) {
                currentTokens.add(id);
            }
        } else if (request.getPrompt() != null && !request.getPrompt().isEmpty()) {
            if (tokenizer != null) {
                List<Integer> encoded = tokenizer.encode(request.getPrompt());
                if (encoded != null) {
                    currentTokens.addAll(encoded);
                }
            }
        }

        if (currentTokens.isEmpty()) {
            currentTokens.add(1); // Default to BOS / token 1 if no input tokens provided
        }

        int maxSeqLen = model.getMaxSeqLen();
        int maxTokensToGenerate = request.getMaxTokens();
        Set<Integer> stopTokens = request.getStopTokenIds();
        Random rng = new Random();

        List<Integer> generatedTokens = new ArrayList<>();
        InferenceResult.TerminationReason terminationReason = InferenceResult.TerminationReason.MAX_TOKENS_REACHED;
        Tensor lastLogits = null;

        // Decoupled token generation loop (forward pass -> sampling -> append)
        for (int step = 0; step < maxTokensToGenerate; step++) {
            // Trim context window if input exceeds max sequence length of model architecture
            int totalCount = currentTokens.size();
            int windowStart = Math.max(0, totalCount - maxSeqLen);
            int windowLen = totalCount - windowStart;
            int[] inputIds = new int[windowLen];
            for (int i = 0; i < windowLen; i++) {
                inputIds[i] = currentTokens.get(windowStart + i);
            }

            // Forward pass (logits)
            lastLogits = forward(model, inputIds);

            // Sample next token from last sequence step logits
            int nextToken = sampleNextToken(lastLogits, currentTokens, request, rng);

            currentTokens.add(nextToken);
            generatedTokens.add(nextToken);

            // Check for EOS or custom stop token IDs
            if (nextToken == 2 || nextToken == 3 || stopTokens.contains(nextToken)) { // 2=</s>, 3=EOS
                terminationReason = InferenceResult.TerminationReason.EOS_REACHED;
                break;
            }
        }

        int[] genTokenIds = generatedTokens.stream().mapToInt(Integer::intValue).toArray();
        String generatedText = "";
        if (tokenizer != null && !generatedTokens.isEmpty()) {
            try {
                generatedText = tokenizer.decode(generatedTokens);
            } catch (Exception e) {
                generatedText = generatedTokens.toString();
            }
        }

        long executionTimeMs = System.currentTimeMillis() - startTime;

        return new InferenceResult(
                genTokenIds,
                generatedText,
                lastLogits,
                generatedTokens.size(),
                executionTimeMs,
                terminationReason
        );
    }

    /**
     * Sampling logic decoupled from model forward pass.
     * Supports deterministic (temperature=0 or greedy), temperature scaling, top-k, top-p, and repeat penalty.
     */
    private int sampleNextToken(Tensor logits, List<Integer> tokenHistory, InferenceRequest request, Random rng) {
        float[] data = logits.getData();
        int seqLen = (int) logits.getShape()[0];
        int vocabSize = (int) logits.getShape()[1];
        int offset = (seqLen - 1) * vocabSize;

        float[] rawLogits = new float[vocabSize];
        System.arraycopy(data, offset, rawLogits, 0, vocabSize);

        // Apply Repeat Penalty
        float repeatPenalty = request.getRepeatPenalty();
        if (repeatPenalty > 1.0f && tokenHistory != null) {
            for (int token : tokenHistory) {
                if (token >= 0 && token < vocabSize) {
                    if (rawLogits[token] < 0) {
                        rawLogits[token] *= repeatPenalty;
                    } else {
                        rawLogits[token] /= repeatPenalty;
                    }
                }
            }
        }

        float temp = request.getTemperature();

        // Greedy decoding / Deterministic mode
        if (temp <= 0.0f) {
            int argmax = 0;
            float maxVal = rawLogits[0];
            for (int i = 1; i < vocabSize; i++) {
                if (rawLogits[i] > maxVal) {
                    maxVal = rawLogits[i];
                    argmax = i;
                }
            }
            return argmax;
        }

        // Temperature scaling
        float maxLogit = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < vocabSize; i++) {
            rawLogits[i] /= temp;
            if (rawLogits[i] > maxLogit) {
                maxLogit = rawLogits[i];
            }
        }

        // Softmax probabilities
        float[] probs = new float[vocabSize];
        float sumExp = 0.0f;
        for (int i = 0; i < vocabSize; i++) {
            probs[i] = (float) Math.exp(rawLogits[i] - maxLogit);
            sumExp += probs[i];
        }
        for (int i = 0; i < vocabSize; i++) {
            probs[i] /= sumExp;
        }

        // Apply Top-K filtering
        int topK = request.getTopK();
        if (topK > 0 && topK < vocabSize) {
            float[] sortedProbs = probs.clone();
            Arrays.sort(sortedProbs);
            float cutoff = sortedProbs[vocabSize - topK];
            float newSum = 0.0f;
            for (int i = 0; i < vocabSize; i++) {
                if (probs[i] < cutoff) {
                    probs[i] = 0.0f;
                } else {
                    newSum += probs[i];
                }
            }
            if (newSum > 0) {
                for (int i = 0; i < vocabSize; i++) {
                    probs[i] /= newSum;
                }
            }
        }

        // Apply Top-P (Nucleus) filtering
        float topP = request.getTopP();
        if (topP < 1.0f && topP > 0.0f) {
            Integer[] indices = new Integer[vocabSize];
            for (int i = 0; i < vocabSize; i++) indices[i] = i;
            Arrays.sort(indices, (a, b) -> Float.compare(probs[b], probs[a]));

            float cumulative = 0.0f;
            boolean cutoffReached = false;
            float newSum = 0.0f;
            for (int idx : indices) {
                if (cutoffReached) {
                    probs[idx] = 0.0f;
                } else {
                    cumulative += probs[idx];
                    newSum += probs[idx];
                    if (cumulative >= topP) {
                        cutoffReached = true;
                    }
                }
            }
            if (newSum > 0) {
                for (int i = 0; i < vocabSize; i++) {
                    probs[i] /= newSum;
                }
            }
        }

        // Categorical sampling
        float r = rng.nextFloat();
        float accum = 0.0f;
        for (int i = 0; i < vocabSize; i++) {
            accum += probs[i];
            if (r <= accum) {
                return i;
            }
        }

        return vocabSize - 1;
    }
}
