package eu.kalafatic.evolution.forge.agent.export;

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

public class OllamaExporterDS implements EvoModelExporter {

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
    public void export(EvoModelArtifact artifact, Path outputPath) throws Exception {
        EvoLlmModel model = artifact.createModel();
        java.util.Map<Integer, String> customVocab = new java.util.HashMap<>();
        if (artifact.getTokenizerVocab() != null) {
            artifact.getTokenizerVocab().forEach((k, v) -> customVocab.put(v, k));
        }
        export(artifact.getModelName(), outputPath, model, customVocab);
    }

    public void export(String modelName, Path outputPath, EvoLlmModel model) throws IOException {
        export(modelName, outputPath, model, null);
    }

    public void export(String modelName, Path outputPath, EvoLlmModel model, java.util.Map<Integer, String> customVocab) throws IOException {
        System.out.println("[Export-DS] Starting genuine EVO model export to: " + outputPath.toAbsolutePath());

        int modelVocabSize = model.getVocabSize();
        List<Tensor> modelParams = model.parameters();
        if (modelParams.isEmpty()) {
            throw new IllegalArgumentException("GGUF export rejected: Model has 0 parameters.");
        }

        long[] embedShape = modelParams.get(0).getShape();
        int embedVocabDim = (int) embedShape[0];
        long[] lmHeadShape = model.getLmHead().getShape();
        int lmHeadVocabDim = (int) lmHeadShape[1];

        if (modelVocabSize != embedVocabDim || modelVocabSize != lmHeadVocabDim) {
            String errorMsg = "GGUF export rejected: Tokenizer and model vocabulary dimensions are incompatible.";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        List<NamedTensor> serializedTensors = new ArrayList<>();
        Tensor embed = modelParams.get(0);
        serializedTensors.add(new NamedTensor("token_embd.weight", embed));

        int blockParamsCount = 9;
        for (int i = 0; i < model.getNumBlocks(); i++) {
            int baseIdx = 1 + i * blockParamsCount;
            Tensor attnNorm = modelParams.get(baseIdx + 0);
            Tensor wq = modelParams.get(baseIdx + 1);
            Tensor wk = modelParams.get(baseIdx + 2);
            Tensor wv = modelParams.get(baseIdx + 3);
            Tensor wo = modelParams.get(baseIdx + 4);
            Tensor ffnNorm = modelParams.get(baseIdx + 5);
            Tensor w1 = modelParams.get(baseIdx + 6);
            Tensor w3 = modelParams.get(baseIdx + 7);
            Tensor w2 = modelParams.get(baseIdx + 8);

            serializedTensors.add(new NamedTensor("blk." + i + ".attn_norm.weight", attnNorm));
            serializedTensors.add(new NamedTensor("blk." + i + ".attn_q.weight", wq.transpose()));
            serializedTensors.add(new NamedTensor("blk." + i + ".attn_k.weight", wk.transpose()));
            serializedTensors.add(new NamedTensor("blk." + i + ".attn_v.weight", wv.transpose()));
            serializedTensors.add(new NamedTensor("blk." + i + ".attn_output.weight", wo.transpose()));

            serializedTensors.add(new NamedTensor("blk." + i + ".ffn_norm.weight", ffnNorm));
            serializedTensors.add(new NamedTensor("blk." + i + ".ffn_gate.weight", w1.transpose()));
            serializedTensors.add(new NamedTensor("blk." + i + ".ffn_up.weight", w3.transpose()));
            serializedTensors.add(new NamedTensor("blk." + i + ".ffn_down.weight", w2.transpose()));
        }

        Tensor outputNorm = modelParams.get(1 + model.getNumBlocks() * blockParamsCount);
        serializedTensors.add(new NamedTensor("output_norm.weight", outputNorm));

        Tensor lmHead = modelParams.get(modelParams.size() - 1);
        serializedTensors.add(new NamedTensor("output.weight", lmHead.transpose()));

        Files.createDirectories(outputPath);
        Path exportsOllamaDir = outputPath.resolve("exports/ollama");
        Files.createDirectories(exportsOllamaDir);

        Path ggufPath = exportsOllamaDir.resolve("evo.gguf");
        writeGGUF(ggufPath, model, serializedTensors, customVocab);

        System.out.println("[Export-DS] GGUF file written successfully: " + ggufPath.toAbsolutePath() + " (" + Files.size(ggufPath) + " bytes)");

        List<String> modelfile = new ArrayList<>();
        modelfile.add("FROM " + ggufPath.toAbsolutePath().toString().replace("\\", "/"));
        modelfile.add("PARAMETER temperature 0.2");
        modelfile.add("PARAMETER num_ctx " + model.getMaxSeqLen());
        modelfile.add("PARAMETER stop \"</s>\"");
        modelfile.add("SYSTEM \"\"\"You are EVO, a specialized language model trained on Evolution project knowledge.\"\"\"");

        Files.write(exportsOllamaDir.resolve("Modelfile"), modelfile);
        Files.copy(ggufPath, outputPath.resolve("evo.gguf"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(exportsOllamaDir.resolve("Modelfile"), outputPath.resolve("Modelfile"), StandardCopyOption.REPLACE_EXISTING);
    }

    private void writeGGUF(Path path, EvoLlmModel model, List<NamedTensor> tensors, java.util.Map<Integer, String> customVocab) throws IOException {
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
        Set<String> seenTokens = new HashSet<>();
        float[] scores = new float[model.getVocabSize()];
        int[] tokenTypes = new int[model.getVocabSize()];
        int vocabSize = model.getVocabSize();

        for (int i = 0; i < vocabSize; i++) {
            if (i == 0) {
                tokens.add("<unk>");
                tokenTypes[i] = 3;
            } else if (i == 1) {
                tokens.add("<s>");
                tokenTypes[i] = 3;
            } else if (i == 2) {
                tokens.add("</s>");
                tokenTypes[i] = 3;
            } else if (i == 3) {
                tokens.add(" ");
                tokenTypes[i] = 1;
            } else if (i >= 4 && i <= 259 && vocabSize >= 260) {
                int byteVal = i - 4;
                tokens.add(String.format(Locale.US, "<0x%02X>", byteVal));
                tokenTypes[i] = 6;
            } else {
                if (customVocab != null && customVocab.containsKey(i) && customVocab.get(i) != null) {
                    tokens.add(customVocab.get(i));
                } else {
                    token = "token_" + i;
                }
                tokenTypes[i] = 1;
            }
            if (seenTokens.contains(token)) {
                token = token + "_" + i;
            }
            seenTokens.add(token);
            tokens.add(token);
            scores[i] = 0.0f;
        }

        metadataList.add(new MetadataEntry("tokenizer.ggml.tokens", 9, tokens));
        metadataList.add(new MetadataEntry("tokenizer.ggml.scores", 9, scores));
        metadataList.add(new MetadataEntry("tokenizer.ggml.token_type", 9, tokenTypes));

        long totalTensorSize = 0;
        for (NamedTensor nt : tensors) {
            totalTensorSize += nt.tensor.getSize() * 4L + 128;
        }
        int bufferSize = (int) Math.max(16 * 1024 * 1024, totalTensorSize + 10 * 1024 * 1024);

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
        while (buf.position() < aligned) {
            buf.put((byte) 0);
        }

        long tensorDataStart = buf.position();

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
                for (float f : arr) {
                    buf.putFloat(f);
                }
            } else if (me.value instanceof int[]) {
                int[] arr = (int[]) me.value;
                buf.putInt(5);
                buf.putLong(arr.length);
                for (int i : arr) {
                    buf.putInt(i);
                }
            }
        }
    }

    private void writeString(ByteBuffer buf, String str) {
        if (str == null) {
            str = "";
        }
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        buf.putLong(bytes.length);
        buf.put(bytes);
    }
}
