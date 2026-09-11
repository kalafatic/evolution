package eu.kalafatic.evolution.forge.agent.gguf;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Independent GGUF Validator.
 *
 * Reopens and reads GGUF file from disk independently to validate binary structure,
 * metadata values and types, tensor shapes and types, payload boundaries,
 * and performs semantic float comparison against original EvoLlmModel / EvoModelArtifact.
 */
public class GGUFValidator {

    public static class ExpectedTensorSpec {
        public final String name;
        public final long[] expectedGgufShape; // In GGUF dimension convention [ne0, ne1, ...]
        public final GGMLType expectedGgmlType;
        public final float[] expectedData; // Original float data (un-transposed native row-major order)
        public final boolean isTransposed; // True if GGUF tensor is transposed from 2D native matrix

        public ExpectedTensorSpec(String name, long[] expectedGgufShape, GGMLType expectedGgmlType, float[] expectedData, boolean isTransposed) {
            this.name = name;
            this.expectedGgufShape = expectedGgufShape;
            this.expectedGgmlType = expectedGgmlType;
            this.expectedData = expectedData;
            this.isTransposed = isTransposed;
        }
    }

    public static GGUFValidationReport validate(Path ggufPath, EvoModelArtifact artifact) {
        try {
            EvoLlmModel model = artifact.createModel();
            return validate(ggufPath, model, artifact.getIdToToken());
        } catch (Exception e) {
            GGUFValidationReport report = new GGUFValidationReport();
            report.addError("artifact", "createModel", "valid EvoLlmModel", e.getMessage(), -1,
                "Failed to construct expected EvoLlmModel from artifact for validation: " + e.getMessage());
            return report;
        }
    }

    public static GGUFValidationReport validate(Path ggufPath, EvoLlmModel model) {
        return validate(ggufPath, model, null);
    }

    public static GGUFValidationReport validate(Path ggufPath, EvoLlmModel model, Map<Integer, String> expectedVocab) {
        GGUFValidationReport report = new GGUFValidationReport();

        if (ggufPath == null || !ggufPath.toFile().exists()) {
            report.addError("file", "path", "existing file", ggufPath != null ? ggufPath.toString() : "null", -1, "GGUF file does not exist");
            return report;
        }

        try (GGUFReader reader = new GGUFReader(ggufPath)) {
            report.addDiagnostic("Successfully opened and parsed GGUF file: " + ggufPath.toAbsolutePath());

            // 1. Structural & Header validation
            validateStructure(reader, report);

            // 2. Metadata validation
            validateMetadata(reader, model, expectedVocab, report);

            // 3. Tensor specification & dimension validation
            List<ExpectedTensorSpec> expectedTensors = buildExpectedTensorList(model);
            validateTensors(reader, expectedTensors, report);

            // 4. Semantic payload round-trip validation
            if (report.isValid()) {
                validateSemantics(reader, expectedTensors, report, 1e-4f);
            }

        } catch (GGUFException ge) {
            report.addError(ge.getSection(), "reader_parse", "valid_gguf", "parse_exception", ge.getOffset(), ge.getMessage());
        } catch (Exception e) {
            report.addError("reader", "exception", "no_exception", e.getClass().getName() + ": " + e.getMessage(), -1, "Unhandled exception during GGUF validation: " + e.getMessage());
        }

        return report;
    }

    private static void validateStructure(GGUFReader reader, GGUFValidationReport report) {
        GGUFHeader header = reader.getHeader();
        if (!"GGUF".equals(header.getMagic())) {
            report.addError("header", "magic", "GGUF", header.getMagic(), header.getHeaderOffset(), "Invalid magic header");
        }
        if (header.getVersion() != 2 && header.getVersion() != 3) {
            report.addError("header", "version", "2 or 3", String.valueOf(header.getVersion()), header.getHeaderOffset() + 4, "Unsupported version");
        }
        if (header.getTensorCount() != reader.getTensorList().size()) {
            report.addError("header", "tensor_count", String.valueOf(header.getTensorCount()),
                String.valueOf(reader.getTensorList().size()), header.getHeaderOffset() + 8, "Tensor count mismatch");
        }
        if (header.getMetadataCount() != reader.getMetadataList().size()) {
            report.addError("header", "metadata_count", String.valueOf(header.getMetadataCount()),
                String.valueOf(reader.getMetadataList().size()), header.getHeaderOffset() + 16, "Metadata count mismatch");
        }

        long alignment = reader.getAlignment();
        if (alignment <= 0 || (alignment & (alignment - 1)) != 0) {
            report.addError("alignment", "general.alignment", "power of 2 > 0", String.valueOf(alignment), -1, "Invalid alignment");
        }
    }

    private static void validateMetadata(GGUFReader reader, EvoLlmModel model, Map<Integer, String> expectedVocab, GGUFValidationReport report) {
        Map<String, GGUFMetadata> metaMap = reader.getMetadataMap();

        checkMetadataField(metaMap, "general.architecture", GGUFValueType.STRING, "llama", report);
        checkMetadataField(metaMap, "llama.context_length", GGUFValueType.UINT32, (long) model.getMaxSeqLen(), report);
        checkMetadataField(metaMap, "llama.embedding_length", GGUFValueType.UINT32, (long) model.getDModel(), report);
        checkMetadataField(metaMap, "llama.feed_forward_length", GGUFValueType.UINT32, (long) model.getDff(), report);
        checkMetadataField(metaMap, "llama.block_count", GGUFValueType.UINT32, (long) model.getNumBlocks(), report);
        checkMetadataField(metaMap, "llama.attention.head_count", GGUFValueType.UINT32, (long) model.getNumHeads(), report);
        checkMetadataField(metaMap, "llama.vocab_size", GGUFValueType.UINT32, (long) model.getVocabSize(), report);

        // Tokenizer validation
        checkMetadataField(metaMap, "tokenizer.ggml.model", GGUFValueType.STRING, "llama", report);

        GGUFMetadata tokensMeta = metaMap.get("tokenizer.ggml.tokens");
        if (tokensMeta == null) {
            report.addError("tokenizer", "tokenizer.ggml.tokens", "present", "missing", -1, "Missing tokenizer.ggml.tokens metadata");
        } else if (tokensMeta.getValueType() != GGUFValueType.ARRAY) {
            report.addError("tokenizer", "tokenizer.ggml.tokens", "ARRAY", tokensMeta.getValueType().name(), tokensMeta.getOffset(), "Wrong tokens value type");
        } else {
            @SuppressWarnings("unchecked")
            List<Object> tokensList = (List<Object>) tokensMeta.getValue();
            if (tokensList.size() != model.getVocabSize()) {
                report.addError("tokenizer", "tokens.length", String.valueOf(model.getVocabSize()),
                    String.valueOf(tokensList.size()), tokensMeta.getOffset(), "Tokenizer tokens array length mismatch with vocabSize");
            }
        }

        GGUFMetadata scoresMeta = metaMap.get("tokenizer.ggml.scores");
        if (scoresMeta == null) {
            report.addError("tokenizer", "tokenizer.ggml.scores", "present", "missing", -1, "Missing tokenizer.ggml.scores metadata");
        } else if (scoresMeta.getValueType() != GGUFValueType.ARRAY) {
            report.addError("tokenizer", "tokenizer.ggml.scores", "ARRAY", scoresMeta.getValueType().name(), scoresMeta.getOffset(), "Wrong scores value type");
        } else {
            @SuppressWarnings("unchecked")
            List<Object> scoresList = (List<Object>) scoresMeta.getValue();
            if (scoresList.size() != model.getVocabSize()) {
                report.addError("tokenizer", "scores.length", String.valueOf(model.getVocabSize()),
                    String.valueOf(scoresList.size()), scoresMeta.getOffset(), "Tokenizer scores array length mismatch with vocabSize");
            }
        }

        GGUFMetadata typesMeta = metaMap.get("tokenizer.ggml.token_type");
        if (typesMeta == null) {
            report.addError("tokenizer", "tokenizer.ggml.token_type", "present", "missing", -1, "Missing tokenizer.ggml.token_type metadata");
        } else if (typesMeta.getValueType() != GGUFValueType.ARRAY) {
            report.addError("tokenizer", "tokenizer.ggml.token_type", "ARRAY", typesMeta.getValueType().name(), typesMeta.getOffset(), "Wrong token_type value type");
        } else {
            @SuppressWarnings("unchecked")
            List<Object> typesList = (List<Object>) typesMeta.getValue();
            if (typesList.size() != model.getVocabSize()) {
                report.addError("tokenizer", "token_type.length", String.valueOf(model.getVocabSize()),
                    String.valueOf(typesList.size()), typesMeta.getOffset(), "Tokenizer token_type array length mismatch with vocabSize");
            }
        }
    }

    private static void checkMetadataField(Map<String, GGUFMetadata> metaMap, String key, GGUFValueType expectedType, Object expectedValue, GGUFValidationReport report) {
        GGUFMetadata meta = metaMap.get(key);
        if (meta == null) {
            report.addError("metadata", key, "present", "missing", -1, "Missing metadata key: " + key);
            return;
        }
        if (meta.getValueType() != expectedType) {
            report.addError("metadata", key + ".type", expectedType.name(), meta.getValueType().name(), meta.getOffset(), "Incorrect metadata value type");
            return;
        }

        if (expectedValue instanceof Number && meta.getValue() instanceof Number) {
            long expVal = ((Number) expectedValue).longValue();
            long actVal = ((Number) meta.getValue()).longValue();
            if (expVal != actVal) {
                report.addError("metadata", key, String.valueOf(expVal), String.valueOf(actVal), meta.getOffset(), "Metadata numerical value mismatch");
            }
        } else if (!Objects.equals(expectedValue, meta.getValue())) {
            report.addError("metadata", key, String.valueOf(expectedValue), String.valueOf(meta.getValue()), meta.getOffset(), "Metadata value mismatch");
        }
    }

    /**
     * Builds the exact list of expected tensors from the EvoLlmModel.
     * Documents GGUF dimension convention vs Native Java Tensor convention:
     * - Native 2D matrix shape: [rows, cols] (e.g. [vocabSize, dModel] or [dModel, dff])
     * - In GGUF/GGML format, 2D weight matrices are transposed to [cols, rows]
     * - GGUF tensor shape metadata stores dimensions in REVERSE ORDER: [ne0, ne1]
     *   Where ne0 = fastest-varying dimension (rows of transposed matrix = cols of original).
     *   Therefore, for transposed 2D matrix [rows, cols], GGUF shape metadata stores [rows, cols].
     *   For un-transposed 1D/2D tensor [dim0, dim1], GGUF shape metadata stores [dim1, dim0].
     */
    public static List<ExpectedTensorSpec> buildExpectedTensorList(EvoLlmModel model) {
        List<ExpectedTensorSpec> specs = new ArrayList<>();
        List<Tensor> modelParams = model.parameters();

        // 1. Embedding weight [vocabSize, dModel] -> GGUF shape [dModel, vocabSize]
        Tensor embWeight = modelParams.get(0);
        long[] embShape = embWeight.getShape(); // [vocabSize, dModel]
        long[] embGgufShape = new long[]{ embShape[1], embShape[0] };
        specs.add(new ExpectedTensorSpec("token_embd.weight", embGgufShape, GGMLType.F32, embWeight.getData(), false));

        // 2. Transformer blocks
        int paramsPerBlock = 9;
        for (int i = 0; i < model.getNumBlocks(); i++) {
            int baseIdx = 1 + i * paramsPerBlock;

            // attn_norm.weight (1D [dModel]) -> GGUF shape [dModel]
            Tensor attnNorm = modelParams.get(baseIdx + 0);
            specs.add(new ExpectedTensorSpec("blk." + i + ".attn_norm.weight", new long[]{ attnNorm.getShape()[0] }, GGMLType.F32, attnNorm.getData(), false));

            // attn_q, attn_k, attn_v, attn_output (2D transposed)
            // Native shape [dModel, dModel], transposed in GGUF
            // Transposed matrix shape: [dModel, dModel]. GGUF stores reversed shape [dModel, dModel]
            specs.add(new ExpectedTensorSpec("blk." + i + ".attn_q.weight", new long[]{ dModel(model), dModel(model) }, GGMLType.F32, modelParams.get(baseIdx + 1).getData(), true));
            specs.add(new ExpectedTensorSpec("blk." + i + ".attn_k.weight", new long[]{ dModel(model), dModel(model) }, GGMLType.F32, modelParams.get(baseIdx + 2).getData(), true));
            specs.add(new ExpectedTensorSpec("blk." + i + ".attn_v.weight", new long[]{ dModel(model), dModel(model) }, GGMLType.F32, modelParams.get(baseIdx + 3).getData(), true));
            specs.add(new ExpectedTensorSpec("blk." + i + ".attn_output.weight", new long[]{ dModel(model), dModel(model) }, GGMLType.F32, modelParams.get(baseIdx + 4).getData(), true));

            // ffn_norm (1D [dModel])
            Tensor ffnNorm = modelParams.get(baseIdx + 5);
            specs.add(new ExpectedTensorSpec("blk." + i + ".ffn_norm.weight", new long[]{ ffnNorm.getShape()[0] }, GGMLType.F32, ffnNorm.getData(), false));

            // ffn_gate (W1), ffn_up (W3), ffn_down (W2)
            // W1 shape [dModel, dff], transposed in GGUF to [dff, dModel].
            // GGUF stores reversed shape: [dModel, dff]
            Tensor w1 = modelParams.get(baseIdx + 6); // [dModel, dff]
            specs.add(new ExpectedTensorSpec("blk." + i + ".ffn_gate.weight", new long[]{ w1.getShape()[0], w1.getShape()[1] }, GGMLType.F32, w1.getData(), true));

            Tensor w3 = modelParams.get(baseIdx + 7); // [dModel, dff]
            specs.add(new ExpectedTensorSpec("blk." + i + ".ffn_up.weight", new long[]{ w3.getShape()[0], w3.getShape()[1] }, GGMLType.F32, w3.getData(), true));

            Tensor w2 = modelParams.get(baseIdx + 8); // [dff, dModel]
            specs.add(new ExpectedTensorSpec("blk." + i + ".ffn_down.weight", new long[]{ w2.getShape()[0], w2.getShape()[1] }, GGMLType.F32, w2.getData(), true));
        }

        // 3. Output norm and LM Head
        int outputNormIdx = 1 + model.getNumBlocks() * paramsPerBlock;
        Tensor outputNorm = modelParams.get(outputNormIdx);
        specs.add(new ExpectedTensorSpec("output_norm.weight", new long[]{ outputNorm.getShape()[0] }, GGMLType.F32, outputNorm.getData(), false));

        // output.weight: native lmHead shape [dModel, vocabSize], transposed in GGUF to [vocabSize, dModel].
        // GGUF stores reversed shape: [dModel, vocabSize]
        Tensor lmHead = modelParams.get(outputNormIdx + 1); // [dModel, vocabSize]
        specs.add(new ExpectedTensorSpec("output.weight", new long[]{ lmHead.getShape()[0], lmHead.getShape()[1] }, GGMLType.F32, lmHead.getData(), true));

        return specs;
    }

    private static long dModel(EvoLlmModel model) {
        return model.getDModel();
    }

    private static void validateTensors(GGUFReader reader, List<ExpectedTensorSpec> expectedSpecs, GGUFValidationReport report) {
        Map<String, GGUFTensorInfo> parsedMap = reader.getTensorMap();

        Set<String> expectedNames = new LinkedHashSet<>();
        for (ExpectedTensorSpec spec : expectedSpecs) {
            expectedNames.add(spec.name);
        }

        Set<String> parsedNames = parsedMap.keySet();

        // Check for missing tensors
        for (String expName : expectedNames) {
            if (!parsedNames.contains(expName)) {
                report.addError("tensor_names", expName, "present", "missing", -1, "Expected tensor missing from GGUF");
            }
        }

        // Check for unexpected tensors
        for (String pName : parsedNames) {
            if (!expectedNames.contains(pName)) {
                report.addError("tensor_names", pName, "absent", "present", -1, "Unexpected tensor present in GGUF");
            }
        }

        // Compare shapes and types
        for (ExpectedTensorSpec spec : expectedSpecs) {
            GGUFTensorInfo actualInfo = parsedMap.get(spec.name);
            if (actualInfo == null) continue;

            if (actualInfo.getGgmlType() != spec.expectedGgmlType) {
                report.addError("tensor_info", spec.name + ".type", spec.expectedGgmlType.name(), actualInfo.getGgmlType().name(),
                    actualInfo.getDescriptorOffset(), "GGML tensor type mismatch");
            }

            if (!Arrays.equals(spec.expectedGgufShape, actualInfo.getDimensions())) {
                report.addError("tensor_dims", spec.name + ".dimensions", Arrays.toString(spec.expectedGgufShape),
                    Arrays.toString(actualInfo.getDimensions()), actualInfo.getDescriptorOffset(),
                    "Tensor shape mismatch (GGUF dimension convention [ne0, ne1, ...])");
            }
        }
    }

    private static void validateSemantics(GGUFReader reader, List<ExpectedTensorSpec> expectedSpecs, GGUFValidationReport report, float tolerance) throws IOException, GGUFException {
        for (ExpectedTensorSpec spec : expectedSpecs) {
            GGUFTensorInfo info = reader.getTensorMap().get(spec.name);
            if (info == null) continue;

            float[] decodedGgufData = reader.readTensorFloatData(spec.name);
            float[] expectedData = spec.expectedData;

            if (decodedGgufData.length != expectedData.length) {
                report.addError("semantics", spec.name + ".length", String.valueOf(expectedData.length),
                    String.valueOf(decodedGgufData.length), info.getAbsoluteFileOffset(), "Decoded float array length mismatch");
                continue;
            }

            if (!spec.isTransposed) {
                // Direct element comparison
                for (int i = 0; i < expectedData.length; i++) {
                    float diff = Math.abs(expectedData[i] - decodedGgufData[i]);
                    if (diff > tolerance) {
                        report.addError("semantics", spec.name + "[" + i + "]", String.valueOf(expectedData[i]),
                            String.valueOf(decodedGgufData[i]), info.getAbsoluteFileOffset(),
                            String.format("Value mismatch exceeding tolerance %f (diff: %f)", tolerance, diff));
                        break; // Log first value error per tensor
                    }
                }
            } else {
                // Transposed matrix comparison
                // Native shape: [rows, cols]. Transposed GGUF shape in memory: [cols, rows].
                // GGUF shape metadata = [ne0, ne1] = [rows, cols]
                int rows = (int) spec.expectedGgufShape[0];
                int cols = (int) spec.expectedGgufShape[1];

                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        float nativeVal = expectedData[r * cols + c];
                        float ggufVal = decodedGgufData[c * rows + r];
                        float diff = Math.abs(nativeVal - ggufVal);
                        if (diff > tolerance) {
                            report.addError("semantics", spec.name + "[row=" + r + ", col=" + c + "]",
                                String.valueOf(nativeVal), String.valueOf(ggufVal), info.getAbsoluteFileOffset(),
                                String.format("Transposed matrix value mismatch exceeding tolerance %f (diff: %f)", tolerance, diff));
                            r = rows; // Break outer loop
                            break;
                        }
                    }
                }
            }
        }
    }
}
