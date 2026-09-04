package eu.kalafatic.evolution.forge.model.inference;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.ModelSnapshot;
import eu.kalafatic.evolution.forge.tokenizer.api.Tokenizer;

/**
 * Unified Inference Engine interface operating directly on canonical EvoLlmModel or ModelSnapshot.
 */
public interface EvoInferenceEngine {

    void validateModel(EvoLlmModel model);

    Tensor forward(EvoLlmModel model, int[] inputIds);

    InferenceResult generate(EvoLlmModel model, InferenceRequest request, Tokenizer tokenizer);

    Tensor forwardSnapshot(ModelSnapshot snapshot, int[] inputIds);

    InferenceResult generateFromSnapshot(ModelSnapshot snapshot, InferenceRequest request, Tokenizer tokenizer);
}
