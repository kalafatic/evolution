package eu.kalafatic.evolution.forge.model.inference;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.ModelSnapshot;
import eu.kalafatic.evolution.forge.model.protocol.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Enhanced diagnostic model validator for in-memory and binary `.evo` model artifacts.
 */
public class EvoModelValidator {

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static void validate(EvoLlmModel model) {
        if (model == null) {
            throw new ValidationException("Model cannot be null");
        }
        if (model.getVocabSize() <= 0) {
            throw new ValidationException("Invalid vocabSize: " + model.getVocabSize());
        }
        if (model.getDModel() <= 0) {
            throw new ValidationException("Invalid dModel: " + model.getDModel());
        }
        if (model.getNumHeads() <= 0) {
            throw new ValidationException("Invalid numHeads: " + model.getNumHeads());
        }
        if (model.getNumBlocks() <= 0 || model.getBlocks().isEmpty()) {
            throw new ValidationException("Invalid numBlocks or empty blocks");
        }

        List<Tensor> params = model.parameters();
        if (params == null || params.isEmpty()) {
            throw new ValidationException("Model has no parameters");
        }

        for (int i = 0; i < params.size(); i++) {
            Tensor p = params.get(i);
            if (p == null || p.getData() == null) {
                throw new ValidationException("Parameter tensor at index " + i + " is null");
            }
            float[] data = p.getData();
            for (int j = 0; j < data.length; j++) {
                if (Float.isNaN(data[j]) || Float.isInfinite(data[j])) {
                    throw new ValidationException("Parameter tensor at index " + i + " contains NaN/Infinity at position " + j);
                }
            }
        }
    }

    public static void validateSnapshot(ModelSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("ModelSnapshot cannot be null");
        }
        if (snapshot.getArchitecture() == null) {
            throw new IllegalStateException("ModelSnapshot architecture cannot be null");
        }
        if (snapshot.getParameters() == null) {
            throw new IllegalStateException("ModelSnapshot parameters cannot be null");
        }
    }

    public static EvoModelIntegrity validateEvoFile(Path path) {
        EvoModelIntegrity integrity = new EvoModelIntegrity();
        if (path == null) {
            integrity.addError("FILE", "path", "File path is null");
            return integrity;
        }

        try {
            EvoModelReader reader = new EvoModelReader();
            reader.read(path);
            integrity.addPass("Header magic and section decoding PASS");

            EvoArchitectureDescriptor arch = reader.getArchitecture();
            if (arch == null || arch.getVocabSize() <= 0 || arch.getDModel() <= 0) {
                integrity.addError("ARCHITECTURE", "EvoArchitectureDescriptor", "Invalid architecture parameters");
            } else {
                integrity.addPass("Architecture descriptor parameters PASS");
            }

            EvoTokenizerDescriptor tok = reader.getTokenizer();
            if (tok == null || tok.getVocabSize() <= 0) {
                integrity.addError("TOKENIZER", "EvoTokenizerDescriptor", "Invalid tokenizer descriptor");
            } else {
                integrity.addPass("Tokenizer descriptor parameters PASS");
            }

            List<EvoTensorDescriptor> manifest = reader.getManifest();
            List<float[]> payloads = reader.getTensorDataPayloads();

            if (manifest == null || payloads == null || manifest.size() != payloads.size()) {
                integrity.addError("MANIFEST", "tensor_count", "Manifest size mismatch with payload size");
                return integrity;
            }
            integrity.addPass("Tensor manifest count PASS (" + manifest.size() + " tensors)");

            for (int i = 0; i < manifest.size(); i++) {
                EvoTensorDescriptor desc = manifest.get(i);
                float[] data = payloads.get(i);

                if (desc.getElementCount() != data.length) {
                    integrity.addError("TENSOR_SHAPE", desc.getCanonicalName(),
                            "Shape " + java.util.Arrays.toString(desc.getShape()) +
                                    " element count " + desc.getElementCount() +
                                    " != stored floats " + data.length);
                }

                String computedChecksum = EvoModelContentHash.calculateFloatArrayChecksum(data);
                if (desc.getChecksum() != null && !desc.getChecksum().isEmpty() && !desc.getChecksum().equals(computedChecksum)) {
                    integrity.addError("TENSOR_CHECKSUM", desc.getCanonicalName(),
                            "Checksum failed. Expected: " + desc.getChecksum() + ", Computed: " + computedChecksum);
                }

                for (int j = 0; j < data.length; j++) {
                    if (Float.isNaN(data[j]) || Float.isInfinite(data[j])) {
                        integrity.addError("TENSOR_PAYLOAD", desc.getCanonicalName(),
                                "NaN/Infinity detected at index " + j);
                        break;
                    }
                }
            }

            if (integrity.isValid()) {
                integrity.addPass("All tensor payload shapes, checksums, and float scans PASS");
            }

        } catch (IOException e) {
            integrity.addError("EVO_PROTOCOL_ERROR", path.toString(), "IO / Protocol decoding failed: " + e.getMessage());
        } catch (Exception e) {
            integrity.addError("EVO_PROTOCOL_ERROR", path.toString(), "Validation crash: " + e.getMessage());
        }

        return integrity;
    }
}
