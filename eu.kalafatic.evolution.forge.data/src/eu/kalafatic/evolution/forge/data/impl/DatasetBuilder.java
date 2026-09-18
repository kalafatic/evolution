package eu.kalafatic.evolution.forge.data.impl;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;

import java.util.ArrayList;
import java.util.List;

public class DatasetBuilder {
    public static class Sample {
        public List<Integer> input;
        public Integer target;

        public Sample(List<Integer> input, Integer target) {
            this.input = input;
            this.target = target;
        }
    }

    public List<Sample> buildSlidingWindow(List<Integer> tokens, int windowSize, int stride) {
        return buildSlidingWindow(tokens, windowSize, stride, Integer.MAX_VALUE);
    }

    public List<Sample> buildSlidingWindow(List<Integer> tokens, int windowSize, int stride, int maxSamples) {
        List<Sample> samples = new ArrayList<>();
        if (tokens == null || tokens.size() <= windowSize) {
            return samples;
        }
        for (int i = 0; i < tokens.size() - windowSize; i += stride) {
            if (samples.size() >= maxSamples) {
                break;
            }
            List<Integer> input = new ArrayList<>(tokens.subList(i, i + windowSize));
            Integer target = tokens.get(i + windowSize);
            samples.add(new Sample(input, target));
        }
        return samples;
    }

    public List<eu.kalafatic.evolution.forge.data.api.TrainingSample> buildTrainingSamples(List<NormalizedSample> normSamples, SimpleBPETokenizer tokenizer, int maxSeqLen) {
        List<eu.kalafatic.evolution.forge.data.api.TrainingSample> trainingSamples = new ArrayList<>();
        if (normSamples == null || normSamples.isEmpty() || tokenizer == null) {
            return trainingSamples;
        }

        int eosTokenId = tokenizer.getEosTokenId();

        for (NormalizedSample norm : normSamples) {
            if (norm == null) continue;

            if (norm.getType() == eu.kalafatic.evolution.forge.data.api.TrainingSampleType.INSTRUCTION && norm.getInstruction() != null) {
                String promptStr = "### Instruction:\n" + norm.getInstruction().trim() + "\n\n### Response:\n";
                String respStr = (norm.getResponse() != null ? norm.getResponse().trim() : "") + "\n";

                List<Integer> promptTokens = tokenizer.encode(promptStr);
                List<Integer> respTokens = tokenizer.encode(respStr);

                if (promptTokens.isEmpty() || respTokens.isEmpty()) continue;

                List<Integer> fullTokens = new ArrayList<>(promptTokens);
                fullTokens.addAll(respTokens);
                fullTokens.add(eosTokenId);

                int seqLen = Math.min(fullTokens.size(), maxSeqLen);
                int[] inputIds = new int[seqLen];
                int[] labels = new int[seqLen];
                boolean[] lossMask = new boolean[seqLen];
                float[] attMask = new float[seqLen];

                int promptLen = promptTokens.size();

                for (int i = 0; i < seqLen; i++) {
                    inputIds[i] = fullTokens.get(i);
                    labels[i] = (i + 1 < fullTokens.size()) ? fullTokens.get(i + 1) : eosTokenId;
                    // Mask prompt tokens so loss is computed ONLY on response tokens
                    lossMask[i] = (i >= promptLen);
                    attMask[i] = 1.0f;
                }
                trainingSamples.add(new eu.kalafatic.evolution.forge.data.api.TrainingSample(inputIds, labels, lossMask, attMask));

            } else {
                // Continuous text or other sample types
                String fullText = norm.toFullText();
                if (fullText == null || fullText.trim().isEmpty()) continue;

                List<Integer> tokens = tokenizer.encode(fullText);
                if (tokens.isEmpty()) continue;
                tokens.add(eosTokenId);

                if (tokens.size() <= maxSeqLen) {
                    int seqLen = tokens.size();
                    int[] inputIds = new int[seqLen];
                    int[] labels = new int[seqLen];
                    boolean[] lossMask = new boolean[seqLen];
                    float[] attMask = new float[seqLen];

                    for (int i = 0; i < seqLen; i++) {
                        inputIds[i] = tokens.get(i);
                        labels[i] = (i + 1 < seqLen) ? tokens.get(i + 1) : eosTokenId;
                        lossMask[i] = true;
                        attMask[i] = 1.0f;
                    }
                    trainingSamples.add(new eu.kalafatic.evolution.forge.data.api.TrainingSample(inputIds, labels, lossMask, attMask));
                } else {
                    int stride = Math.max(1, maxSeqLen / 2);
                    for (int i = 0; i < tokens.size() - 1; i += stride) {
                        int end = Math.min(i + maxSeqLen, tokens.size());
                        int seqLen = end - i;
                        if (seqLen < 2) break;

                        int[] inputIds = new int[seqLen];
                        int[] labels = new int[seqLen];
                        boolean[] lossMask = new boolean[seqLen];
                        float[] attMask = new float[seqLen];

                        for (int j = 0; j < seqLen; j++) {
                            inputIds[j] = tokens.get(i + j);
                            labels[j] = (i + j + 1 < tokens.size()) ? tokens.get(i + j + 1) : eosTokenId;
                            lossMask[j] = true;
                            attMask[j] = 1.0f;
                        }
                        trainingSamples.add(new eu.kalafatic.evolution.forge.data.api.TrainingSample(inputIds, labels, lossMask, attMask));
                        if (end == tokens.size()) break;
                    }
                }
            }
        }
        return trainingSamples;
    }
}
