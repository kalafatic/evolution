package eu.kalafatic.evolution.forge.trainer.impl.llm;

import eu.kalafatic.evolution.forge.model.inference.InferenceRequest;
import eu.kalafatic.evolution.forge.model.inference.InferenceResult;
import eu.kalafatic.evolution.forge.model.inference.ReferenceEvoInferenceEngine;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.tokenizer.api.Tokenizer;

/**
 * Legacy wrapper for InferenceEngine.
 * Delegates directly to the authoritative native ReferenceEvoInferenceEngine.
 */
public class InferenceEngine {
    private final EvoLlmModel model;
    private final Tokenizer tokenizer;
    private final ReferenceEvoInferenceEngine nativeEngine;

    public InferenceEngine(EvoLlmModel model, Tokenizer tokenizer) {
        this.model = model;
        this.tokenizer = tokenizer;
        this.nativeEngine = new ReferenceEvoInferenceEngine();
    }

    public String generate(String prompt, int maxTokens, float temperature) {
        InferenceRequest request = InferenceRequest.builder()
                .prompt(prompt)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();

        InferenceResult result = nativeEngine.generate(model, request, tokenizer);
        return result.getGeneratedText();
    }

    public ReferenceEvoInferenceEngine getNativeEngine() {
        return nativeEngine;
    }
}
