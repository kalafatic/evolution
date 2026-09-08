package eu.kalafatic.evolution.forge.model.inference;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.model.llm.ModelParameters;
import eu.kalafatic.evolution.forge.model.llm.ModelSnapshot;
import eu.kalafatic.evolution.forge.tokenizer.api.Tokenizer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Single-class complete Native Inference Engine for EVO LLMs.
 * Direct, zero-dependency native JVM runtime operating directly on canonical EvoLlmModel, ModelSnapshot, or *.evo file paths.
 */
public class ReferenceEvoInferenceEngine implements EvoInferenceEngine {

    public interface TokenStreamListener {
        void onTokenGenerated(int tokenId, String tokenText);
    }

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
        return model.forward(inputIds);
    }

    public Tensor forwardSnapshotLast(ModelSnapshot snapshot, int[] inputIds) {
        EvoModelValidator.validateSnapshot(snapshot);
        if (inputIds == null || inputIds.length == 0) {
            throw new IllegalArgumentException("Input token IDs cannot be null or empty");
        }

        EvoLlmModel model = new EvoLlmModel(snapshot.getArchitecture());
        ModelParameters snapParams = snapshot.getParameters();
        ModelParameters modelParams = model.getModelParameters();
        for (String name : snapParams.names()) {
            Tensor src = snapParams.get(name);
            Tensor dst = modelParams.get(name);
            if (src != null && dst != null) {
                System.arraycopy(src.getData(), 0, dst.getData(), 0, src.getData().length);
            }
        }
        if (snapshot.getVocabulary() != null) {
            model.getIdToToken().putAll(snapshot.getVocabulary());
        }
        return model.forwardLast(inputIds);
    }

    @Override
    public Tensor forwardSnapshot(ModelSnapshot snapshot, int[] inputIds) {
        EvoModelValidator.validateSnapshot(snapshot);
        if (inputIds == null || inputIds.length == 0) {
            throw new IllegalArgumentException("Input token IDs cannot be null or empty");
        }

        EvoLlmModel model = new EvoLlmModel(snapshot.getArchitecture());
        ModelParameters snapParams = snapshot.getParameters();
        ModelParameters modelParams = model.getModelParameters();
        for (String name : snapParams.names()) {
            Tensor src = snapParams.get(name);
            Tensor dst = modelParams.get(name);
            if (src != null && dst != null) {
                System.arraycopy(src.getData(), 0, dst.getData(), 0, src.getData().length);
            }
        }
        if (snapshot.getVocabulary() != null) {
            model.getIdToToken().putAll(snapshot.getVocabulary());
        }
        return model.forward(inputIds);
    }

    public InferenceResult generateFromEvoFile(Path evoPath, InferenceRequest request) throws IOException {
        EvoModelArtifact artifact = EvoModelArtifact.load(evoPath);
        return generateFromArtifact(artifact, request, null);
    }

    public InferenceResult generateFromArtifact(EvoModelArtifact artifact, InferenceRequest request, TokenStreamListener streamListener) {
        if (artifact == null) {
            throw new IllegalArgumentException("EvoModelArtifact cannot be null");
        }
        EvoLlmModel model = artifact.createModel();

        eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer artifactTokenizer = new eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer();
        if (artifact.getTokenizerVocab() != null && !artifact.getTokenizerVocab().isEmpty()) {
            artifactTokenizer.setVocabulary(artifact.getTokenizerVocab());
        } else if (artifact.getIdToToken() != null && !artifact.getIdToToken().isEmpty()) {
            Map<String, Integer> rev = new java.util.LinkedHashMap<>();
            artifact.getIdToToken().forEach((id, tok) -> rev.put(tok, id));
            artifactTokenizer.setVocabulary(rev);
        }

        return generateWithListener(model, request, artifactTokenizer, streamListener);
    }

    @Override
    public InferenceResult generate(EvoLlmModel model, InferenceRequest request, Tokenizer tokenizer) {
        return generateWithListener(model, request, tokenizer, null);
    }

    private int sampleNextTokenWithVocabulary(Tensor logits, List<Integer> tokenHistory, InferenceRequest request, Random rng, Map<Integer, String> idToToken) {
        float[] data = logits.getData();
        int seqLen = (int) logits.getShape()[0];
        int vocabSize = (int) logits.getShape()[1];
        int offset = (seqLen - 1) * vocabSize;

        float[] rawLogits = new float[vocabSize];
        System.arraycopy(data, offset, rawLogits, 0, vocabSize);

        return sampleNextTokenFromLogits(rawLogits, vocabSize, tokenHistory, request, rng);
    }

    @Override
    public InferenceResult generateFromSnapshot(ModelSnapshot snapshot, InferenceRequest request, Tokenizer tokenizer) {
        EvoModelValidator.validateSnapshot(snapshot);
        if (request == null) {
            throw new IllegalArgumentException("InferenceRequest cannot be null");
        }

        EvoLlmModel model = new EvoLlmModel(snapshot.getArchitecture());
        ModelParameters snapParams = snapshot.getParameters();
        ModelParameters modelParams = model.getModelParameters();
        for (String name : snapParams.names()) {
            Tensor src = snapParams.get(name);
            Tensor dst = modelParams.get(name);
            if (src != null && dst != null) {
                System.arraycopy(src.getData(), 0, dst.getData(), 0, src.getData().length);
            }
        }
        if (snapshot.getVocabulary() != null) {
            model.getIdToToken().putAll(snapshot.getVocabulary());
        }

        return generateWithListener(model, request, tokenizer, null);
    }

    public InferenceResult generateWithListener(EvoLlmModel model, InferenceRequest request, Tokenizer tokenizer, TokenStreamListener streamListener) {
        validateModel(model);
        if (request == null) {
            throw new IllegalArgumentException("InferenceRequest cannot be null");
        }

        long startTime = System.currentTimeMillis();

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
            currentTokens.add(1);
        }

        int maxSeqLen = model.getMaxSeqLen();
        int maxTokensToGenerate = request.getMaxTokens();
        Set<Integer> stopTokens = request.getStopTokenIds();
        Random rng = new Random();

        List<Integer> generatedTokens = new ArrayList<>();
        InferenceResult.TerminationReason terminationReason = InferenceResult.TerminationReason.MAX_TOKENS_REACHED;
        Tensor lastLogits = null;

        System.out.printf("[EvoInferenceEngine] Starting native generation (promptTokens=%d, maxTokens=%d)%n",
                currentTokens.size(), maxTokensToGenerate);

        KVCache kvCache = new KVCache(model.getNumBlocks());

        // Prefill Phase
        int[] promptIds = currentTokens.stream().mapToInt(Integer::intValue).toArray();
        lastLogits = model.forwardWithCache(promptIds, kvCache);

        for (int step = 0; step < maxTokensToGenerate; step++) {
            if (step > 0) {
                int lastToken = currentTokens.get(currentTokens.size() - 1);
                lastLogits = model.forwardWithCache(new int[]{lastToken}, kvCache);
            }

            // Only generated tokens should be penalized to prevent prompt tokens from being suppressed
            int nextToken = sampleNextTokenWithVocabulary(lastLogits, generatedTokens, request, rng, model.getIdToToken());

            currentTokens.add(nextToken);
            generatedTokens.add(nextToken);

            String tokenText = "";
            if (tokenizer != null) {
                try {
                    tokenText = tokenizer.decode(List.of(nextToken));
                } catch (Exception ignored) {}
            }

            if (streamListener != null) {
                streamListener.onTokenGenerated(nextToken, tokenText);
            }

            if ((step + 1) % 10 == 0 || step == 0 || step == maxTokensToGenerate - 1) {
                long elapsed = System.currentTimeMillis() - startTime;
                double tokPerSec = (step + 1) * 1000.0 / Math.max(1, elapsed);
                System.out.printf("[EvoInferenceEngine] Step %d/%d: token=%d ('%s') | Elapsed: %d ms (%.2f tok/s)%n",
                        step + 1, maxTokensToGenerate, nextToken, tokenText.replace("\n", "\\n"), elapsed, tokPerSec);
            }

            String tokStr = model.getIdToToken() != null ? model.getIdToToken().get(nextToken) : null;
            boolean isEos = nextToken == 2 || "</s>".equals(tokStr) || "<|endoftext|>".equals(tokStr) || "<eos>".equals(tokStr);
            if (isEos || (stopTokens != null && stopTokens.contains(nextToken))) {
                terminationReason = InferenceResult.TerminationReason.EOS_REACHED;
                break;
            }

            if (currentTokens.size() >= maxSeqLen) {
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

    private int sampleNextToken(Tensor logits, List<Integer> tokenHistory, InferenceRequest request, Random rng) {
        float[] data = logits.getData();
        int seqLen = (int) logits.getShape()[0];
        int vocabSize = (int) logits.getShape()[1];
        int offset = (seqLen - 1) * vocabSize;

        float[] rawLogits = new float[vocabSize];
        System.arraycopy(data, offset, rawLogits, 0, vocabSize);

        return sampleNextTokenFromLogits(rawLogits, vocabSize, tokenHistory, request, rng);
    }

    private int sampleNextTokenFromLogits(float[] rawLogits, int vocabSize, List<Integer> tokenHistory, InferenceRequest request, Random rng) {
        float repeatPenalty = request.getRepeatPenalty();
        float freqPenalty = request.getFrequencyPenalty();
        float presPenalty = request.getPresencePenalty();

        if (tokenHistory != null && !tokenHistory.isEmpty()) {
            int[] counts = new int[vocabSize];
            for (int token : tokenHistory) {
                if (token >= 0 && token < vocabSize) {
                    counts[token]++;
                }
            }

            for (int i = 0; i < vocabSize; i++) {
                if (counts[i] > 0) {
                    if (repeatPenalty > 1.0f) {
                        if (rawLogits[i] < 0) rawLogits[i] *= repeatPenalty;
                        else rawLogits[i] /= repeatPenalty;
                    }
                    rawLogits[i] -= counts[i] * freqPenalty + presPenalty;
                }
            }
        }

        float temp = request.getTemperature();

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

        float maxLogit = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < vocabSize; i++) {
            rawLogits[i] /= temp;
            if (rawLogits[i] > maxLogit) {
                maxLogit = rawLogits[i];
            }
        }

        float[] probs = new float[vocabSize];
        float sumExp = 0.0f;
        for (int i = 0; i < vocabSize; i++) {
            probs[i] = (float) Math.exp(rawLogits[i] - maxLogit);
            sumExp += probs[i];
        }
        for (int i = 0; i < vocabSize; i++) {
            probs[i] /= sumExp;
        }

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
