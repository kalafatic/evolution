package eu.kalafatic.evolution.forge.agent.export;

import eu.kalafatic.evolution.forge.agent.gguf.GGUFValidationReport;
import eu.kalafatic.evolution.forge.agent.gguf.GGUFValidator;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.model.llm.EvoModelExporter;
import eu.kalafatic.evolution.forge.model.llm.ModelParameters;
import eu.kalafatic.evolution.forge.model.llm.ModelSnapshot;
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

/**
 * OllamaExporter - Handles export of EVO LLM models to GGUF format for Ollama/llama.cpp.
 * Consumes canonical ModelSnapshot instances as its primary source of truth.
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

    @Override
    public void exportSnapshot(ModelSnapshot snapshot, Path outputPath) throws Exception {
        if (snapshot == null) throw new IllegalArgumentException("ModelSnapshot cannot be null");
        if (outputPath == null) throw new IllegalArgumentException("Output path cannot be null");

        String modelName = snapshot.getMetadata() != null ? snapshot.getMetadata().getName() : "evo";
        Map<Integer, String> vocab = snapshot.getVocabulary();
        if (vocab == null || vocab.isEmpty()) {
            vocab = buildDefaultVocabulary(snapshot.getArchitecture().getVocabSize());
        }

        System.out.println("[Export] Starting GGUF export from canonical ModelSnapshot: " + modelName);
        EvoLlmArchitecture arch = snapshot.getArchitecture();
        ModelParameters params = snapshot.getParameters();

        List<NamedTensor> serializedTensors = new ArrayList<>();
        serializedTensors.add(new NamedTensor("token_embd.weight", params.get("token_embd.weight")));

        for (int i = 0; i < arch.getNumBlocks(); i++) {
            serializedTensors.add(new NamedTensor("blk." + i + ".attn_norm.weight", params.get("blk." + i + ".attn_norm.weight")));
            serializedTensors.add(new NamedTensor("blk." + i + ".attn_q.weight", transpose(params.get("blk." + i + ".attn_q.weight"))));
            serializedTensors.add(new NamedTensor("blk." + i + ".attn_k.weight", transpose(params.get("blk." + i + ".attn_k.weight"))));
            serializedTensors.add(new NamedTensor("blk." + i + ".attn_v.weight", transpose(params.get("blk." + i + ".attn_v.weight"))));
            serializedTensors.add(new NamedTensor("blk." + i + ".attn_output.weight", transpose(params.get("blk." + i + ".attn_output.weight"))));
            serializedTensors.add(new NamedTensor("blk." + i + ".ffn_norm.weight", params.get("blk." + i + ".ffn_norm.weight")));
            serializedTensors.add(new NamedTensor("blk." + i + ".ffn_gate.weight", transpose(params.get("blk." + i + ".ffn_gate.weight"))));
            serializedTensors.add(new NamedTensor("blk." + i + ".ffn_up.weight", transpose(params.get("blk." + i + ".ffn_up.weight"))));
            serializedTensors.add(new NamedTensor("blk." + i + ".ffn_down.weight", transpose(params.get("blk." + i + ".ffn_down.weight"))));
        }

        serializedTensors.add(new NamedTensor("output_norm.weight", params.get("output_norm.weight")));
        Tensor outputWeight = params.contains("output.weight") ? params.get("output.weight") : params.get("token_embd.weight");
        serializedTensors.add(new NamedTensor("output.weight", transpose(outputWeight)));

        Files.createDirectories(outputPath);
        Path exportsOllamaDir = outputPath.resolve("exports/ollama");
        Files.createDirectories(exportsOllamaDir);

        Path ggufPath = exportsOllamaDir.resolve("evo.gguf");
        writeGGUF(ggufPath, arch, serializedTensors, vocab);

        EvoLlmModel tempModel = new EvoLlmModel(arch);
        ModelParameters tempParams = tempModel.getModelParameters();
        if (params != null && params.names() != null) {
            for (String paramName : params.names()) {
                if (tempParams.contains(paramName)) {
                    Tensor src = params.get(paramName);
                    Tensor dst = tempParams.get(paramName);
                    if (src != null && dst != null && src.getData() != null && dst.getData() != null) {
                        System.arraycopy(src.getData(), 0, dst.getData(), 0, Math.min(src.getData().length, dst.getData().length));
                    }
                }
            }
        }
        GGUFValidationReport valReport = GGUFValidator.validate(ggufPath, tempModel, vocab);

        if (!valReport.isValid()) {
            System.err.println(valReport.generateSummary());
            throw new IOException("GGUF Export Validation Failed: File is malformed or invalid.");
        }

        List<String> modelfile = new ArrayList<>();
        modelfile.add("FROM " + ggufPath.toAbsolutePath().toString().replace("\\", "/"));
        modelfile.add("PARAMETER temperature 0.2");
        modelfile.add("PARAMETER num_ctx " + arch.getMaxSeqLen());
        modelfile.add("PARAMETER stop \"</s>\"");
        modelfile.add("PARAMETER stop \"<s>\"");
        modelfile.add("SYSTEM \"\"\"You are EVO, a specialized language model trained on Evolution project knowledge.\"\"\"");

        Path modelfilePath = exportsOllamaDir.resolve("Modelfile");
        Files.write(modelfilePath, modelfile);

        Files.copy(ggufPath, outputPath.resolve("evo.gguf"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(modelfilePath, outputPath.resolve("Modelfile"), StandardCopyOption.REPLACE_EXISTING);

        boolean registrationSuccess = false;
        String nameToRegister = (modelName != null && !modelName.isEmpty()) ? modelName : "evo";
        try {
            ProcessBuilder pb = new ProcessBuilder("ollama", "create", nameToRegister, "-f", modelfilePath.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode == 0) {
                registrationSuccess = true;
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void export(EvoModelArtifact artifact, Path outputPath) throws Exception {
        if (artifact == null) throw new IllegalArgumentException("Artifact cannot be null");
        exportSnapshot(artifact.toSnapshot(), outputPath);
    }

    public void export(String modelName, Path exportPath, EvoLlmModel model) throws IOException {
        export(modelName, exportPath, model, null);
    }

    public void export(String modelName, Path outputPath, EvoLlmModel model, Map<Integer, String> customVocab) throws IOException {
        if (model == null) throw new IllegalArgumentException("Model cannot be null");
        if (outputPath == null) throw new IllegalArgumentException("Output path cannot be null");

        if (customVocab != null && !customVocab.isEmpty()) {
            model.getIdToToken().putAll(customVocab);
        }

        try {
            exportSnapshot(model.createSnapshot(), outputPath);
        } catch (IOException e) {
            throw e;
        } catch (Exception ex) {
            throw new IOException("Failed to export model snapshot", ex);
        }
    }

    private int writeGGUF(Path path, EvoLlmArchitecture arch, List<NamedTensor> tensors, Map<Integer, String> customVocab) throws IOException {
        List<MetadataEntry> metadataList = new ArrayList<>();
        metadataList.add(new MetadataEntry("general.architecture", 8, "llama"));
        metadataList.add(new MetadataEntry("general.name", 8, "EVO LLM"));
        metadataList.add(new MetadataEntry("general.file_type", 4, 0));
        metadataList.add(new MetadataEntry("general.alignment", 4, 32));
        metadataList.add(new MetadataEntry("llama.context_length", 4, arch.getMaxSeqLen()));
        metadataList.add(new MetadataEntry("llama.embedding_length", 4, arch.getDModel()));
        metadataList.add(new MetadataEntry("llama.feed_forward_length", 4, arch.getDff()));
        metadataList.add(new MetadataEntry("llama.block_count", 4, arch.getNumBlocks()));
        metadataList.add(new MetadataEntry("llama.attention.head_count", 4, arch.getNumHeads()));
        metadataList.add(new MetadataEntry("llama.attention.head_count_kv", 4, arch.getNumHeads()));
        metadataList.add(new MetadataEntry("llama.vocab_size", 4, arch.getVocabSize()));
        metadataList.add(new MetadataEntry("llama.attention.layer_norm_rms_epsilon", 6, 1e-5f));
        metadataList.add(new MetadataEntry("llama.attention.key_length", 4, arch.getDModel() / arch.getNumHeads()));
        metadataList.add(new MetadataEntry("llama.attention.value_length", 4, arch.getDModel() / arch.getNumHeads()));
        metadataList.add(new MetadataEntry("llama.rope.dimension_count", 4, arch.getDModel() / arch.getNumHeads()));
        metadataList.add(new MetadataEntry("llama.rope.freq_base", 6, 10000.0f));

        metadataList.add(new MetadataEntry("tokenizer.ggml.model", 8, "llama"));
        metadataList.add(new MetadataEntry("tokenizer.ggml.bos_token_id", 4, 1));
        metadataList.add(new MetadataEntry("tokenizer.ggml.eos_token_id", 4, 2));
        metadataList.add(new MetadataEntry("tokenizer.ggml.unknown_token_id", 4, 0));

        List<String> tokens = new ArrayList<>();
        float[] scores = new float[arch.getVocabSize()];
        int[] tokenTypes = new int[arch.getVocabSize()];
        Set<String> seenTokens = new HashSet<>();

        for (int i = 0; i < arch.getVocabSize(); i++) {
            String token = customVocab.getOrDefault(i, "token_" + i);
            if (seenTokens.contains(token)) token = token + "_" + i;
            seenTokens.add(token);
            tokens.add(token);

            if (i <= 2) tokenTypes[i] = 3;
            else if (token.startsWith("<0x") && token.endsWith(">") && token.length() == 6) tokenTypes[i] = 6;
            else tokenTypes[i] = 1;
            scores[i] = 0.0f;
        }

        metadataList.add(new MetadataEntry("tokenizer.ggml.tokens", 9, tokens));
        metadataList.add(new MetadataEntry("tokenizer.ggml.scores", 9, scores));
        metadataList.add(new MetadataEntry("tokenizer.ggml.token_type", 9, tokenTypes));

        int bufferSize = calculateExactGgufSize(arch, tensors, metadataList);
        ByteBuffer buf = ByteBuffer.allocate(bufferSize);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put("GGUF".getBytes(StandardCharsets.UTF_8));
        buf.putInt(3);
        buf.putLong(tensors.size());
        buf.putLong(metadataList.size());

        for (MetadataEntry me : metadataList) {
            writeString(buf, me.key);
            buf.putInt(me.type);
            serializeMetadataValue(buf, me);
        }

        long currentOffset = 0;
        List<Long> tensorOffsets = new ArrayList<>();
        for (NamedTensor nt : tensors) {
            currentOffset = (currentOffset + 31) & ~31;
            tensorOffsets.add(currentOffset);
            currentOffset += nt.tensor.getSize() * 4L;
        }

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

        int bytesToWrite = buf.position();
        int aligned = (bytesToWrite + 31) & ~31;
        while (buf.position() < aligned) buf.put((byte) 0);

        long tensorDataStart = buf.position();
        for (int i = 0; i < tensors.size(); i++) {
            while ((buf.position() - tensorDataStart) < tensorOffsets.get(i)) buf.put((byte) 0);
            NamedTensor nt = tensors.get(i);
            for (float val : nt.tensor.getData()) buf.putFloat(val);
        }

        long totalDataWritten = buf.position() - tensorDataStart;
        long alignedDataEnd = (totalDataWritten + 31) & ~31;
        while ((buf.position() - tensorDataStart) < alignedDataEnd) buf.put((byte) 0);

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
        for (int i = 4; i < vocabSize; i++) vocab.put(i, "token_" + i);
        return vocab;
    }

    private Tensor transpose(Tensor t) {
        if (t == null) return new SimpleTensor(1, 1);
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
        if (me.type == 4) buf.putInt(((Number) me.value).intValue());
        else if (me.type == 6) buf.putFloat(((Number) me.value).floatValue());
        else if (me.type == 8) writeString(buf, (String) me.value);
        else if (me.type == 9) {
            if (me.value instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) me.value;
                buf.putInt(8);
                buf.putLong(list.size());
                for (String s : list) writeString(buf, s);
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

    private int calculateExactGgufSize(EvoLlmArchitecture arch, List<NamedTensor> tensors, List<MetadataEntry> metadataList) {
        long size = 4 + 4 + 8 + 8;
        for (MetadataEntry me : metadataList) {
            size += 8 + me.key.getBytes(StandardCharsets.UTF_8).length + 4 + getMetadataValueSize(me);
        }
        size = (size + 31) & ~31;
        for (NamedTensor nt : tensors) {
            size += 8 + nt.name.getBytes(StandardCharsets.UTF_8).length + 4 + nt.tensor.getShape().length * 8 + 4 + 8;
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
                for (String s : list) size += 8 + s.getBytes(StandardCharsets.UTF_8).length;
            } else if (me.value instanceof float[]) {
                size += ((float[]) me.value).length * 4L;
            } else if (me.value instanceof int[]) {
                size += ((int[]) me.value).length * 4L;
            }
            return size;
        }
        return 0;
    }
}
