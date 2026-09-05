package eu.kalafatic.evolution.forge.model.inference;

import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.ModelParameters;
import eu.kalafatic.evolution.forge.model.llm.ModelSnapshot;
import eu.kalafatic.evolution.forge.math.api.Tensor;

public class EvoModelValidator {

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static void validate(EvoLlmModel model) {
        if (model == null) {
            throw new ValidationException("EvoLlmModel cannot be null");
        }

        EvoLlmArchitecture arch = model.getArchitecture();
        if (arch == null) {
            throw new ValidationException("EvoLlmModel architecture cannot be null");
        }

        if (arch.getVocabSize() <= 0) {
            throw new ValidationException("Invalid vocabulary size: " + arch.getVocabSize());
        }

        if (arch.getDModel() <= 0) {
            throw new ValidationException("Invalid model dimension (dModel): " + arch.getDModel());
        }

        if (arch.getNumHeads() <= 0) {
            throw new ValidationException("Invalid number of heads: " + arch.getNumHeads());
        }

        if (arch.getDModel() % arch.getNumHeads() != 0) {
            throw new ValidationException(String.format("dModel (%d) must be divisible by numHeads (%d)",
                    arch.getDModel(), arch.getNumHeads()));
        }

        if (arch.getNumBlocks() <= 0) {
            throw new ValidationException("Invalid block count: " + arch.getNumBlocks());
        }

        if (arch.getDff() <= 0) {
            throw new ValidationException("Invalid FFN dimension (dff): " + arch.getDff());
        }

        if (arch.getMaxSeqLen() <= 0) {
            throw new ValidationException("Invalid max sequence length: " + arch.getMaxSeqLen());
        }

        if (model.getLmHead() == null || model.getLmHead().getData() == null) {
            throw new ValidationException("LM head tensor or weights cannot be null");
        }

        long[] lmHeadShape = model.getLmHead().getShape();
        if (lmHeadShape == null || lmHeadShape.length != 2
                || lmHeadShape[0] != arch.getDModel() || lmHeadShape[1] != arch.getVocabSize()) {
            throw new ValidationException(String.format("LM head shape mismatch: expected [%d, %d], got %s",
                    arch.getDModel(), arch.getVocabSize(),
                    lmHeadShape != null ? java.util.Arrays.toString(lmHeadShape) : "null"));
        }

        if (model.getBlocks() == null || model.getBlocks().size() != arch.getNumBlocks()) {
            throw new ValidationException(String.format("Transformer blocks count mismatch: expected %d, got %d",
                    arch.getNumBlocks(), model.getBlocks() != null ? model.getBlocks().size() : 0));
        }

        for (int i = 0; i < model.getBlocks().size(); i++) {
            if (model.getBlocks().get(i) == null) {
                throw new ValidationException("Transformer block at index " + i + " is null");
            }
        }
    }

    public static void validateSnapshot(ModelSnapshot snapshot) {
        if (snapshot == null) {
            throw new ValidationException("ModelSnapshot cannot be null");
        }

        EvoLlmArchitecture arch = snapshot.getArchitecture();
        if (arch == null) {
            throw new ValidationException("ModelSnapshot architecture cannot be null");
        }

        ModelParameters params = snapshot.getParameters();
        if (params == null || params.count() == 0) {
            throw new ValidationException("ModelSnapshot parameters registry cannot be empty");
        }

        if (!params.contains("token_embd.weight")) {
            throw new ValidationException("ModelSnapshot missing canonical token_embd.weight tensor");
        }

        for (int i = 0; i < arch.getNumBlocks(); i++) {
            String qName = "blk." + i + ".attn_q.weight";
            if (!params.contains(qName)) {
                throw new ValidationException("ModelSnapshot missing canonical tensor: " + qName);
            }
        }
    }
}
