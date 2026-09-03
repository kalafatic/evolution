package eu.kalafatic.evolution.forge.model.inference;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class InferenceRequest {
    private final String prompt;
    private final int[] inputIds;
    private final int maxTokens;
    private final float temperature;
    private final float topP;
    private final int topK;
    private final float repeatPenalty;
    private final float frequencyPenalty;
    private final float presencePenalty;
    private final Set<Integer> stopTokenIds;

    private InferenceRequest(Builder builder) {
        this.prompt = builder.prompt;
        this.inputIds = builder.inputIds != null ? builder.inputIds.clone() : new int[0];
        this.maxTokens = builder.maxTokens;
        this.temperature = builder.temperature;
        this.topP = builder.topP;
        this.topK = builder.topK;
        this.repeatPenalty = builder.repeatPenalty;
        this.frequencyPenalty = builder.frequencyPenalty;
        this.presencePenalty = builder.presencePenalty;
        this.stopTokenIds = Collections.unmodifiableSet(new HashSet<>(builder.stopTokenIds));
    }

    public String getPrompt() {
        return prompt;
    }

    public int[] getInputIds() {
        return inputIds.clone();
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public float getTemperature() {
        return temperature;
    }

    public float getTopP() {
        return topP;
    }

    public int getTopK() {
        return topK;
    }

    public float getRepeatPenalty() {
        return repeatPenalty;
    }

    public float getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public float getPresencePenalty() {
        return presencePenalty;
    }

    public Set<Integer> getStopTokenIds() {
        return stopTokenIds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String prompt = "";
        private int[] inputIds = new int[0];
        private int maxTokens = 64;
        private float temperature = 0.0f; // Deterministic default
        private float topP = 1.0f;
        private int topK = 0;
        private float repeatPenalty = 1.0f;
        private float frequencyPenalty = 0.0f;
        private float presencePenalty = 0.0f;
        private Set<Integer> stopTokenIds = new HashSet<>();

        public Builder prompt(String prompt) {
            this.prompt = prompt != null ? prompt : "";
            return this;
        }

        public Builder inputIds(int[] inputIds) {
            this.inputIds = inputIds != null ? inputIds : new int[0];
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(float topP) {
            this.topP = topP;
            return this;
        }

        public Builder topK(int topK) {
            this.topK = topK;
            return this;
        }

        public Builder repeatPenalty(float repeatPenalty) {
            this.repeatPenalty = repeatPenalty;
            return this;
        }

        public Builder frequencyPenalty(float frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder presencePenalty(float presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public Builder addStopTokenId(int tokenId) {
            this.stopTokenIds.add(tokenId);
            return this;
        }

        public Builder stopTokenIds(Set<Integer> stopTokenIds) {
            if (stopTokenIds != null) {
                this.stopTokenIds.addAll(stopTokenIds);
            }
            return this;
        }

        public InferenceRequest build() {
            return new InferenceRequest(this);
        }
    }
}
