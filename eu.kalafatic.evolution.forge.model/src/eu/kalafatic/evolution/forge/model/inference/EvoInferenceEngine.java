package eu.kalafatic.evolution.forge.model.inference;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.tokenizer.api.Tokenizer;

public interface EvoInferenceEngine {

    /**
     * Executes the forward pass of the model for input token IDs and returns raw logits.
     * Shape of returned tensor: [seqLen, vocabSize].
     *
     * @param model the native EvoLlmModel instance (source of truth for weights and architecture)
     * @param inputIds the sequence of input token IDs
     * @return raw unnormalized logits tensor of shape [seqLen, vocabSize]
     */
    Tensor forward(EvoLlmModel model, int[] inputIds);

    /**
     * Generates tokens using the given model, request configuration, and tokenizer.
     *
     * @param model the native EvoLlmModel instance
     * @param request the inference request options (prompt, inputIds, maxTokens, temperature, etc.)
     * @param tokenizer the tokenizer used for encoding/decoding text
     * @return structured InferenceResult containing generated tokens, text, logits, and metadata
     */
    InferenceResult generate(EvoLlmModel model, InferenceRequest request, Tokenizer tokenizer);

    /**
     * Validates that the provided EvoModel matches architecture constraints and contains required valid weights.
     *
     * @param model the EvoLlmModel to validate
     */
    void validateModel(EvoLlmModel model);
}
