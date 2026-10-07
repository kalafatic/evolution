package eu.kalafatic.evolution.forge.model.target;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EvoModelValidator {

    public static class ModelValidationResult {
        private boolean passed = true;
        private final List<String> errors = new ArrayList<>();
        private long parameterCount = 0;

        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }

        public List<String> getErrors() { return errors; }

        public long getParameterCount() { return parameterCount; }
        public void setParameterCount(long parameterCount) { this.parameterCount = parameterCount; }

        public void addError(String error) {
            this.passed = false;
            this.errors.add(error);
        }

        @Override
        public String toString() {
            return String.format("ModelValidationResult{passed=%b, params=%d, errors=%s}", passed, parameterCount, errors);
        }
    }

    public ModelValidationResult validateModel(EvoLlmModel model, Map<String, Integer> vocab) {
        ModelValidationResult result = new ModelValidationResult();

        if (model == null) {
            result.addError("EvoLlmModel instance is null");
            return result;
        }

        if (model.getArchitecture() == null) {
            result.addError("Model architecture definition is null");
            return result;
        }

        if (model.getArchitecture().getVocabSize() <= 0) {
            result.addError("Vocabulary size in architecture is non-positive: " + model.getArchitecture().getVocabSize());
        }

        if (model.getArchitecture().getDModel() <= 0) {
            result.addError("Hidden dimension dModel in architecture is non-positive: " + model.getArchitecture().getDModel());
        }

        if (model.getArchitecture().getNumBlocks() <= 0) {
            result.addError("Layer count numBlocks in architecture is non-positive: " + model.getArchitecture().getNumBlocks());
        }

        Iterable<Tensor> parameters = model.parameters();
        if (parameters == null) {
            result.addError("Model parameters iterable is null");
            return result;
        }

        long paramCount = 0;
        int tensorCount = 0;

        for (Tensor p : parameters) {
            if (p == null) {
                result.addError("Encountered null tensor parameter in model");
                continue;
            }
            tensorCount++;
            float[] data = p.getData();
            if (data == null || data.length == 0) {
                result.addError("Parameter tensor has null or empty data array");
                continue;
            }
            paramCount += data.length;

            int nanInfCheckLimit = Math.min(data.length, 10000);
            for (int i = 0; i < nanInfCheckLimit; i++) {
                float val = data[i];
                if (Float.isNaN(val)) {
                    result.addError("Tensor parameter contains NaN value at index " + i);
                    break;
                }
                if (Float.isInfinite(val)) {
                    result.addError("Tensor parameter contains Infinity value at index " + i);
                    break;
                }
            }
        }

        result.setParameterCount(paramCount);

        if (tensorCount == 0) {
            result.addError("Model contains zero parameter tensors");
        }

        if (paramCount == 0) {
            result.addError("Model total parameter count is zero");
        }

        if (vocab == null || vocab.isEmpty()) {
            result.addError("Tokenizer vocabulary is null or empty");
        } else if (vocab.size() != model.getArchitecture().getVocabSize()) {
            result.addError(String.format("Vocabulary size mismatch: vocab has %d tokens, model expected %d",
                    vocab.size(), model.getArchitecture().getVocabSize()));
        }

        return result;
    }
}
