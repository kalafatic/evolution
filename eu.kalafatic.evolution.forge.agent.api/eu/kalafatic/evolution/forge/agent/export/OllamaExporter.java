package eu.kalafatic.evolution.forge.agent.export;

import eu.kalafatic.evolution.forge.agent.gguf.GGUFValidationReport;
import eu.kalafatic.evolution.forge.agent.gguf.GGUFValidator;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.model.llm.EvoModelExporter;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OllamaExporter - Handles export of EVO LLM models to GGUF format for Ollama/llama.cpp
 * with independent GGUFReader and GGUFValidator structural, semantic, and round-trip verification.
 */
public class OllamaExporter implements EvoModelExporter {

    public static class NamedTensor {
        public final String name;
        public final Tensor tensor;

        public NamedTensor(String name, Tensor tensor) {
            this.name = name;
            this.tensor = tensor;
        }
    }

    public static class ValidationResult {
        public boolean training = true;
        public boolean checkpoint = true;
        public boolean ggufStructure = true;
        public boolean ggufTensors = true;
        public boolean registration = false;
        public boolean inference = false;
        public boolean knowledgeTest = false;
        public String identityStatus = "OK";
        public String details = "";
        public String fallbackRequiredReason = null;
    }

    private static class MetadataEntry {
        final String key;
        final int type;
        final Object value;

        MetadataEntry(String key, int type, Object value) {
            this.key = key;
            this.type = type;
            this.value = value;
        }
    }

    // OS Detection
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    @Override
    public void export(EvoModelArtifact artifact, Path outputPath) throws Exception {
        System.out.println("[Export] Starting EVO model export with vocabulary preservation");
        System.out.println("[Export] Artifact: " + artifact.getModelName());
        System.out.println("[Export] Vocabulary size: " + artifact.getTokenizerVocab().size());

        Map<Integer, String> idToToken = artifact.getIdToToken();
        if (idToToken.isEmpty()) {
            throw new IllegalArgumentException("Artifact has empty vocabulary! Cannot export GGUF.");
        }

        EvoLlmModel model = artifact.createModel();
        export(artifact.getModelName(), outputPath, model, idToToken);
    }

    public void export(String modelName, Path exportPath, EvoLlmModel model) throws IOException {
        export(modelName, exportPath, model, null);
    }

    public void export(String modelName, Path outputPath, EvoLlmModel model, Map<Integer, String> customVocab) throws IOException {
        if (model == null) throw new IllegalArgumentException("Model cannot be null");
        if (outputPath == null) throw new IllegalArgumentException("Output path cannot be null");

        if (customVocab == null || customVocab.isEmpty()) {
            System.out.println("[Export] No custom vocabulary provided, building default vocabulary for size " + model.getVocabSize());
            customVocab = buildDefaultVocabulary(model.getVocabSize());
        }

        System.out.println("[Export] Starting genuine EVO model export to: " + outputPath.toAbsolutePath());
        System.out.println("[Export] OS detected: " + System.getProperty("os.name"));
        System.out.println("[Export] Vocabulary size: " + customVocab.size());

        List<Tensor> modelParams = model.parameters();
        if (modelParams.isEmpty()) {
            throw new IllegalArgumentException("GGUF export rejected: Model has 0 parameters.");
        }

        System.out.println("========== EVO EXPORT ARCHITECTURE ==========");
        System.out.println("Vocab       : " + model.getVocabSize());
        System.out.println("DModel      : " + model.getDModel());
        System.out.println("DFF         : " + model.getDff());
        System.out.println("Blocks      : " + model.getNumBlocks());
        System.out.println("Heads       : " + model.getNumHeads());
        System.out.println("Context     : " + model.getMaxSeqLen());
        System.out.println("Parameters  : " + model.parameters().size());

        // Extract tensors in exact GGUF order
        List<NamedTensor> serializedTensors = new ArrayList<>();
        serializedTensors.add(new NamedTensor("token_embd.weight", modelParams.get(0)));

        int paramsPerBlock = 9;
        for (int i = 0; i < model.getNumBlocks(); i++) {
            int baseIdx = 1 + i * paramsPerBlock;
            serializedTensors.add(new NamedTensor("blk." + i + ".attn_norm.weight", modelParams.get(baseIdx + 0)));
            serializedTensors.add(new NamedTensor("blk." + i + ".attn_q.weight", transpose(modelParams.get(baseIdx + 1))));
            serializedTensors.add(new NamedTensor("blk." + i + ".attn_k.weight", transpose(modelParams.get(baseIdx + 2))));
            serializedTensors.add(new NamedTensor("blk." + i + ".attn_v.weight", transpose(modelParams.get(baseIdx + 3))));
            serializedTensors.add(new NamedTensor("blk." + i + ".attn_output.weight", transpose(modelParams.get(baseIdx + 4))));
            serializedTensors.add(new NamedTensor("blk." + i + ".ffn_norm.weight", modelParams.get(baseIdx + 5)));
            serializedTensors.add(new NamedTensor("blk." + i + ".ffn_gate.weight", transpose(modelParams.get(baseIdx + 6))));
            serializedTensors.add(new NamedTensor("blk." + i + ".ffn_up.weight", transpose(modelParams.get(baseIdx + 7))));
            serializedTensors.add(new NamedTensor("blk." + i + ".ffn_down.weight", transpose(modelParams.get(baseIdx + 8))));
        }

        int outputNormIdx = 1 + model.getNumBlocks() * paramsPerBlock;
        serializedTensors.add(new NamedTensor("output_norm.weight", modelParams.get(outputNormIdx)));
        serializedTensors.add(new NamedTensor("output.weight", transpose(modelParams.get(outputNormIdx + 1))));

        // Create directories
        Files.createDirectories(outputPath);
        Path exportsOllamaDir = outputPath.resolve("exports/ollama");
        Files.createDirectories(exportsOllamaDir);

        // 1. WRITE GGUF FILE TO DISK
        Path ggufPath = exportsOllamaDir.resolve("evo.gguf");
        writeGGUF(ggufPath, model, serializedTensors, customVocab);
        long ggufSize = Files.size(ggufPath);
        System.out.println("[Export] GGUF file written: " + ggufPath.toAbsolutePath() + " (" + ggufSize + " bytes)");

        // 2. INDEPENDENT GGUF READER & VALIDATOR VERIFICATION
        System.out.println("[Export] Running independent GGUFReader & GGUFValidator on serialized disk file...");
        GGUFValidationReport valReport = GGUFValidator.validate(ggufPath, model, customVocab);
        System.out.println(valReport.generateSummary());

        if (!valReport.isValid()) {
            System.err.println("[Export] ❌ CRITICAL: GGUF Validation failed! Aborting registration with Ollama/llama.cpp.");
            for (String err : valReport.getErrors()) {
                System.err.println("  [VALIDATION ERROR] " + err);
            }
            throw new IOException("GGUF Export Validation Failed: File is malformed or invalid. Registration aborted.");
        }
        System.out.println("[Export] ✅ GGUF Structure, Metadata, Tensors, and Semantic Round-trip Validation PASSED!");

        // 3. OPTIONAL EXTERNAL LLAMA-CLI CROSS-CHECK (WITH 30s TIMEOUT)
        String llamaCppStatus = "SKIPPED";
        try {
            LlamaCppRunner runner = LlamaCppRunner.builder(ggufPath.toAbsolutePath().toString()).build();
            if (runner.isAvailable()) {
                System.out.println("[Export] Running optional secondary llama-cli validation gate...");
                boolean llamaOk = runner.validateModel();
                llamaCppStatus = llamaOk ? "PASS ✅" : "FAIL ❌";
            } else {
                System.out.println("[Export] llama-cli binary not configured, skipping external llama-cli check.");
            }
        } catch (Exception ex) {
            System.err.println("[Export] Secondary llama-cli check exception: " + ex.getMessage());
            llamaCppStatus = "FAIL ❌ (" + ex.getMessage() + ")";
        }

        // 4. GENERATE MODELFILE AND SECONDARY ARTIFACTS
        List<String> modelfile = new ArrayList<>();
        modelfile.add("FROM " + ggufPath.toAbsolutePath().toString().replace("\\", "/"));
        modelfile.add("PARAMETER temperature 0.2");
        modelfile.add("PARAMETER num_ctx " + model.getMaxSeqLen());
        modelfile.add("PARAMETER stop \"</s>\"");
        modelfile.add("PARAMETER stop \"<s>\"");
        modelfile.add("SYSTEM \"\"\"You are EVO, a specialized language model trained on Evolution project knowledge.\"\"\"");

        Path modelfilePath = exportsOllamaDir.resolve("Modelfile");
        Files.write(modelfilePath, modelfile);

        // Copy to root & internal llama-cpp lib folder
        Files.copy(ggufPath, outputPath.resolve("evo.gguf"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(modelfilePath, outputPath.resolve("Modelfile"), StandardCopyOption.REPLACE_EXISTING);
        copyToLlamaCppLibFolder(ggufPath, modelName);

        // Save weights.bin
        Path weightsPath = outputPath.resolve("weights.bin");
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(weightsPath.toFile())))) {
            for (Tensor p : modelParams) {
                for (float val : p.getData()) dos.writeFloat(val);
            }
        }

        // Save config.json
        Path configPath = outputPath.resolve("config.json");
        Map<String, Object> configMap = new LinkedHashMap<>();
        configMap.put("model_name", modelName != null ? modelName : "evo");
        configMap.put("vocab_size", model.getVocabSize());
        configMap.put("d_model", model.getDModel());
        configMap.put("num_heads", model.getNumHeads());
        configMap.put("num_blocks", model.getNumBlocks());
        configMap.put("dff", model.getDff());
        configMap.put("max_seq_len", model.getMaxSeqLen());
        configMap.put("export_date", new Date().toString());
        org.json.JSONObject configJson = new org.json.JSONObject(configMap);
        Files.writeString(configPath, configJson.toString(2));

        // Save tokenizer.json
        Path vocabJsonPath = outputPath.resolve("tokenizer.json");
        Map<String, Object> vocabData = new LinkedHashMap<>();
        vocabData.put("vocab_size", model.getVocabSize());
        vocabData.put("tokens", new ArrayList<>(customVocab.values()));
        org.json.JSONObject vocabJson = new org.json.JSONObject(vocabData);
        Files.writeString(vocabJsonPath, vocabJson.toString(2));

        // 5. REGISTER WITH OLLAMA ONLY AFTER VALIDATION PASSED
        boolean registrationSuccess = false;
        String nameToRegister = (modelName != null && !modelName.isEmpty()) ? modelName : "evo";
        try {
            System.out.println("[Forge] Registering model with Ollama: " + nameToRegister);
            ProcessBuilder pb = new ProcessBuilder("ollama", "create", nameToRegister, "-f",
                modelfilePath.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = readProcessOutput(p);
            int exitCode = p.waitFor();
            System.out.println("[Ollama] " + output);
            if (exitCode == 0) {
                registrationSuccess = true;
            }
        } catch (Exception e) {
            System.err.println("[Ollama] Registration failed: " + e.getMessage());
        }

        // Update 'evo' alias
        boolean aliasUpdated = false;
        if (registrationSuccess) {
            try {
                ProcessBuilder pbAlias = new ProcessBuilder("ollama", "create", "evo", "-f",
                    modelfilePath.toAbsolutePath().toString());
                pbAlias.redirectErrorStream(true);
                Process pAlias = pbAlias.start();
                int exitCodeAlias = pAlias.waitFor();
                if (exitCodeAlias == 0) {
                    aliasUpdated = true;
                    System.out.println("[Ollama] 'evo' alias updated");
                }
            } catch (Exception ex) {
                System.err.println("[Ollama] Alias update failed: " + ex.getMessage());
            }
        }

        ValidationResult valResult = validateModel(nameToRegister, ggufPath, model);
        valResult.registration = registrationSuccess;

        // Structured Final Export Report
        System.out.println("\n=======================================================");
        System.out.println("EVO FORGE EXPORT PIPELINE REPORT");
        System.out.println("=======================================================");
        System.out.println("GGUF EXPORT     : PASS ✅");
        System.out.println("GGUF STRUCTURE  : " + (valReport.isStructureValid() ? "PASS ✅" : "FAIL ❌"));
        System.out.println("GGUF METADATA   : " + (valReport.isMetadataValid() ? "PASS ✅" : "FAIL ❌"));
        System.out.println("GGUF TENSORS    : " + (valReport.isTensorsValid() ? "PASS ✅" : "FAIL ❌"));
        System.out.println("GGUF SEMANTICS  : " + (valReport.isSemanticsValid() ? "PASS ✅" : "FAIL ❌"));
        System.out.println("GGUF ROUNDTRIP  : " + (valReport.isValid() ? "PASS ✅" : "FAIL ❌"));
        System.out.println("LLAMA.CPP       : " + llamaCppStatus);
        System.out.println("OLLAMA          : " + (registrationSuccess ? "PASS ✅" : "FAIL/SKIPPED ❌"));
        System.out.println("EVO ALIAS       : " + (aliasUpdated ? "PASS ✅" : "FAIL/SKIPPED ❌"));
        System.out.println("INFERENCE TEST  : " + (valResult.inference ? "PASS ✅" : "FAIL ❌"));
        System.out.println("=======================================================\n");
    }

    private int writeGGUF(Path path, EvoLlmModel model, List<NamedTensor> tensors, Map<Integer, String> customVocab) throws IOException {
        System.out.println("[Export] Writing GGUF with " + customVocab.size() + " vocabulary entries");

        List<MetadataEntry> metadataList = new ArrayList<>();
        metadataList.add(new MetadataEntry("general.architecture", 8, "llama"));
        metadataList.add(new MetadataEntry("general.name", 8, "EVO LLM"));
        metadataList.add(new MetadataEntry("general.file_type", 4, 0));
        metadataList.add(new MetadataEntry("general.alignment", 4, 32));
        metadataList.add(new MetadataEntry("llama.context_length", 4, model.getMaxSeqLen()));
        metadataList.add(new MetadataEntry("llama.embedding_length", 4, model.getDModel()));
        metadataList.add(new MetadataEntry("llama.feed_forward_length", 4, model.getDff()));
        metadataList.add(new MetadataEntry("llama.block_count", 4, model.getNumBlocks()));
        metadataList.add(new MetadataEntry("llama.attention.head_count", 4, model.getNumHeads()));
        metadataList.add(new MetadataEntry("llama.attention.head_count_kv", 4, model.getNumHeads()));
        metadataList.add(new MetadataEntry("llama.vocab_size", 4, model.getVocabSize()));
        metadataList.add(new MetadataEntry("llama.attention.layer_norm_rms_epsilon", 6, 1e-5f));
        metadataList.add(new MetadataEntry("llama.attention.key_length", 4, model.getDModel() / model.getNumHeads()));
        metadataList.add(new MetadataEntry("llama.attention.value_length", 4, model.getDModel() / model.getNumHeads()));
        metadataList.add(new MetadataEntry("llama.rope.dimension_count", 4, model.getDModel() / model.getNumHeads()));
        metadataList.add(new MetadataEntry("llama.rope.freq_base", 6, 10000.0f));

        metadataList.add(new MetadataEntry("tokenizer.ggml.model", 8, "llama"));
        metadataList.add(new MetadataEntry("tokenizer.ggml.bos_token_id", 4, 1));
        metadataList.add(new MetadataEntry("tokenizer.ggml.eos_token_id", 4, 2));
        metadataList.add(new MetadataEntry("tokenizer.ggml.unknown_token_id", 4, 0));

        List<String> tokens = new ArrayList<>();
        float[] scores = new float[model.getVocabSize()];
        int[] tokenTypes = new int[model.getVocabSize()];
        Set<String> seenTokens = new HashSet<>();

        for (int i = 0; i < model.getVocabSize(); i++) {
            String token;
            if (customVocab.containsKey(i)) {
                token = customVocab.get(i);
            } else {
                token = "token_" + i;
                System.err.println("[Warning] Missing token for ID " + i + ", using fallback");
            }

            if (seenTokens.contains(token)) {
                token = token + "_" + i;
            }
            seenTokens.add(token);
            tokens.add(token);

            if (i == 0 || i == 1 || i == 2) {
                tokenTypes[i] = 3; // CONTROL
            } else if (token.startsWith("<0x") && token.endsWith(">") && token.length() == 6) {
                tokenTypes[i] = 6; // BYTE
            } else {
                tokenTypes[i] = 1; // NORMAL
            }
            scores[i] = 0.0f;
        }

        metadataList.add(new MetadataEntry("tokenizer.ggml.tokens", 9, tokens));
        metadataList.add(new MetadataEntry("tokenizer.ggml.scores", 9, scores));
        metadataList.add(new MetadataEntry("tokenizer.ggml.token_type", 9, tokenTypes));

        int bufferSize = calculateExactGgufSize(model, tensors, metadataList);
        ByteBuffer buf = ByteBuffer.allocate(bufferSize);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        // Header
        buf.put("GGUF".getBytes(StandardCharsets.UTF_8));
        buf.putInt(3);
        buf.putLong(tensors.size());
        buf.putLong(metadataList.size());

        // Metadata
        for (MetadataEntry me : metadataList) {
            writeString(buf, me.key);
            buf.putInt(me.type);
            serializeMetadataValue(buf, me);
        }

        // Tensor offsets
        long currentOffset = 0;
        List<Long> tensorOffsets = new ArrayList<>();
        for (NamedTensor nt : tensors) {
            currentOffset = (currentOffset + 31) & ~31;
            tensorOffsets.add(currentOffset);
            currentOffset += nt.tensor.getSize() * 4L;
        }

        // Tensor descriptors
        for (int i = 0; i < tensors.size(); i++) {
            NamedTensor nt = tensors.get(i);
            writeString(buf, nt.name);
            long[] shape = nt.tensor.getShape();
            buf.putInt(shape.length);
            for (int d = shape.length - 1; d >= 0; d--) {
                buf.putLong(shape[d]);
            }
            buf.putInt(0);
            buf.putLong(tensorOffsets.get(i));
        }

        // Align
        int bytesToWrite = buf.position();
        int aligned = (bytesToWrite + 31) & ~31;
        while (buf.position() < aligned) {
            buf.put((byte) 0);
        }

        long tensorDataStart = buf.position();

        // Write tensor data
        for (int i = 0; i < tensors.size(); i++) {
            while ((buf.position() - tensorDataStart) < tensorOffsets.get(i)) {
                buf.put((byte) 0);
            }
            NamedTensor nt = tensors.get(i);
            float[] data = nt.tensor.getData();
            for (float val : data) {
                buf.putFloat(val);
            }
        }

        long totalDataWritten = buf.position() - tensorDataStart;
        long alignedDataEnd = (totalDataWritten + 31) & ~31;
        while ((buf.position() - tensorDataStart) < alignedDataEnd) {
            buf.put((byte) 0);
        }

        buf.flip();
        try (FileOutputStream fos = new FileOutputStream(path.toFile());
             FileChannel channel = fos.getChannel()) {
            channel.write(buf);
        }

        return metadataList.size();
    }

    private Map<Integer, String> buildDefaultVocabulary(int vocabSize) {
        Map<Integer, String> vocab = new LinkedHashMap<>();
        vocab.put(0, "<unk>");
        vocab.put(1, "<s>");
        vocab.put(2, "</s>");
        vocab.put(3, " ");

        String[] commonTokens = {
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
            "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            ".", ",", "?", "!", "\"", "'", "(", ")", "[", "]", "{", "}",
            ":", ";", "-", "_", "+", "=", "*", "/", "\\", "|", "<", ">",
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "I",
            "it", "for", "not", "on", "with", "he", "as", "you", "do", "at"
        };

        int idx = 4;
        if (vocabSize >= 260) {
            for (int b = 0; b < 256; b++) {
                String byteToken = String.format("<0x%02X>", b);
                vocab.put(idx++, byteToken);
            }
        }

        for (String token : commonTokens) {
            if (idx < vocabSize) {
                vocab.put(idx++, token);
            }
        }

        while (idx < vocabSize) {
            vocab.put(idx, "token_" + idx);
            idx++;
        }

        return vocab;
    }

    private Tensor transpose(Tensor t) {
        long[] shape = t.getShape();
        if (shape.length != 2) return t;

        int rows = (int) shape[0];
        int cols = (int) shape[1];
        float[] data = t.getData();
        float[] transposed = new float[rows * cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                transposed[j * rows + i] = data[i * cols + j];
            }
        }

        Tensor result = new SimpleTensor(cols, rows);
        System.arraycopy(transposed, 0, result.getData(), 0, transposed.length);
        return result;
    }

    private void writeString(ByteBuffer buf, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        buf.putLong(bytes.length);
        buf.put(bytes);
    }

    private void serializeMetadataValue(ByteBuffer buf, MetadataEntry me) {
        if (me.type == 4) {
            buf.putInt(((Number) me.value).intValue());
        } else if (me.type == 6) {
            buf.putFloat(((Number) me.value).floatValue());
        } else if (me.type == 8) {
            writeString(buf, (String) me.value);
        } else if (me.type == 9) {
            if (me.value instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) me.value;
                buf.putInt(8);
                buf.putLong(list.size());
                for (String s : list) {
                    writeString(buf, s);
                }
            } else if (me.value instanceof float[]) {
                float[] arr = (float[]) me.value;
                buf.putInt(6);
                buf.putLong(arr.length);
                for (float f : arr) buf.putFloat(f);
            } else if (me.value instanceof int[]) {
                int[] arr = (int[]) me.value;
                buf.putInt(5);
                buf.putLong(arr.length);
                for (int i : arr) buf.putInt(i);
            }
        }
    }

    private int calculateExactGgufSize(EvoLlmModel model, List<NamedTensor> tensors, List<MetadataEntry> metadataList) {
        long size = 4 + 4 + 8 + 8; // GGUF + version + tensorCount + metadataCount

        for (MetadataEntry me : metadataList) {
            size += 8 + me.key.getBytes(StandardCharsets.UTF_8).length;
            size += 4;
            size += getMetadataValueSize(me);
        }

        size = (size + 31) & ~31;

        for (NamedTensor nt : tensors) {
            size += 8 + nt.name.getBytes(StandardCharsets.UTF_8).length;
            size += 4;
            size += nt.tensor.getShape().length * 8;
            size += 4;
            size += 8;
        }

        size = (size + 31) & ~31;

        for (NamedTensor nt : tensors) {
            size += nt.tensor.getSize() * 4L;
            size = (size + 31) & ~31;
        }

        return (int) size;
    }

    private long getMetadataValueSize(MetadataEntry me) {
        if (me.type == 4 || me.type == 6) return 4;
        if (me.type == 8) return 8 + ((String) me.value).getBytes(StandardCharsets.UTF_8).length;
        if (me.type == 9) {
            long size = 4 + 8;
            if (me.value instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) me.value;
                for (String s : list) {
                    size += 8 + s.getBytes(StandardCharsets.UTF_8).length;
                }
            } else if (me.value instanceof float[]) {
                size += ((float[]) me.value).length * 4L;
            } else if (me.value instanceof int[]) {
                size += ((int[]) me.value).length * 4L;
            }
            return size;
        }
        return 0;
    }

    private String readProcessOutput(Process p) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    public ValidationResult validateModel(String modelName, Path ggufPath, EvoLlmModel model) {
        System.out.println("[Forge] Running inference validation...");
        ValidationResult res = new ValidationResult();

        if (ggufPath == null || !Files.exists(ggufPath)) {
            res.ggufStructure = false;
            res.fallbackRequiredReason = "GGUF file does not exist.";
            return res;
        }

        try {
            if (Files.size(ggufPath) < 1024) {
                res.ggufTensors = false;
                res.fallbackRequiredReason = "GGUF size is too small.";
                return res;
            }
        } catch (IOException e) {
            res.ggufStructure = false;
            return res;
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String jsonPayload = "{\n" +
                "  \"model\": \"evo\",\n" +
                "  \"prompt\": \"hi\",\n" +
                "  \"stream\": false,\n" +
                "  \"options\": {\n" +
                "    \"num_ctx\": 128\n" +
                "  }\n" +
                "}";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/generate"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                res.inference = true;
                String body = resp.body();
                if (body.contains("response") && !body.contains("token_")) {
                    res.knowledgeTest = true;
                } else if (body.contains("token_")) {
                    res.knowledgeTest = false;
                } else {
                    res.knowledgeTest = true;
                }
            }
        } catch (Exception e) {
            res.fallbackRequiredReason = "Inference test skipped/failed: " + e.getMessage();
        }

        return res;
    }

    private void copyToLlamaCppLibFolder(Path ggufPath, String modelName) {
        if (ggufPath == null || !Files.exists(ggufPath)) return;
        try {
            Class<?> clazz = Class.forName("eu.kalafatic.evolution.controller.manager.LlamaService");
            try {
                clazz.getMethod("copyToModelsDir", Path.class, String.class).invoke(null, ggufPath, modelName);
            } catch (Throwable ignored) {}
            clazz.getMethod("copyToLlamaCppLibDir", Path.class, String.class).invoke(null, ggufPath, modelName);
        } catch (Throwable t) {
            try {
                String codebasePath = null;
                try {
                    Class<?> pmClazz = Class.forName("eu.kalafatic.evolution.controller.manager.ProjectModelManager");
                    codebasePath = (String) pmClazz.getMethod("getCodebasePath").invoke(null);
                } catch (Throwable ignored) {}

                String userDir = System.getProperty("user.dir");
                Path modelsDir = null;
                if (codebasePath != null) {
                    Path p = Paths.get(codebasePath, "eu.kalafatic.evolution.controller/lib/models");
                    if (Files.exists(p) && Files.isDirectory(p)) modelsDir = p;
                }
                if (modelsDir == null) {
                    Path p = Paths.get(userDir, "eu.kalafatic.evolution.controller/lib/models");
                    if (Files.exists(p) && Files.isDirectory(p)) modelsDir = p;
                    else modelsDir = Paths.get(userDir, "lib/models");
                }
                Files.createDirectories(modelsDir);
                Files.copy(ggufPath, modelsDir.resolve("evo.gguf"), StandardCopyOption.REPLACE_EXISTING);
                if (modelName != null && !modelName.isEmpty() && !"evo".equalsIgnoreCase(modelName)) {
                    Files.copy(ggufPath, modelsDir.resolve(modelName + ".gguf"), StandardCopyOption.REPLACE_EXISTING);
                }

                Path llamaCppDir = null;
                if (codebasePath != null) {
                    Path p = Paths.get(codebasePath, "eu.kalafatic.evolution.controller/lib/llama-cpp");
                    if (Files.exists(p) && Files.isDirectory(p)) llamaCppDir = p;
                }
                if (llamaCppDir == null) {
                    Path p = Paths.get(userDir, "eu.kalafatic.evolution.controller/lib/llama-cpp");
                    if (Files.exists(p) && Files.isDirectory(p)) llamaCppDir = p;
                    else llamaCppDir = Paths.get(userDir, "lib/llama-cpp");
                }
                Files.createDirectories(llamaCppDir);
                Files.copy(ggufPath, llamaCppDir.resolve("evo.gguf"), StandardCopyOption.REPLACE_EXISTING);
                if (modelName != null && !modelName.isEmpty() && !"evo".equalsIgnoreCase(modelName)) {
                    Files.copy(ggufPath, llamaCppDir.resolve(modelName + ".gguf"), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Throwable ignored) {}
        }
    }
}
