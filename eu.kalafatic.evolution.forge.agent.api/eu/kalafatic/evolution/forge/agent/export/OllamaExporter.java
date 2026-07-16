package eu.kalafatic.evolution.forge.agent.export;

import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.math.api.Tensor;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class OllamaExporter {

    public void export(String modelName, Path outputPath, EvoLlmModel model) throws IOException {
        Files.createDirectories(outputPath);
        Path ggufPath = outputPath.resolve("evo.gguf");

        // We will build a GGUF file with:
        // - general.architecture = "evo"
        // - general.name = "evo"
        // - evo.vocab_size, evo.context_length, evo.embedding_length, etc.
        // - Tensors from model.parameters()

        try (FileOutputStream fos = new FileOutputStream(ggufPath.toFile());
             FileChannel channel = fos.getChannel()) {

            // We'll write to a ByteBuffer using LITTLE_ENDIAN
            ByteBuffer buf = ByteBuffer.allocate(256 * 1024 * 1024); // 256MB max for small evo weights
            buf.order(ByteOrder.LITTLE_ENDIAN);

            // 1. Header
            buf.put("GGUF".getBytes()); // magic
            buf.putInt(3); // version

            List<Tensor> params = model.parameters();
            buf.putLong(params.size()); // tensor_count

            // Metadata KV count
            int kvCount = 8;
            buf.putLong(kvCount);

            // Write metadata KVs
            writeStringKV(buf, "general.architecture", "evo");
            writeStringKV(buf, "general.name", "EVO LLM");
            writeIntKV(buf, "evo.context_length", model.getMaxSeqLen());
            writeIntKV(buf, "evo.embedding_length", model.getDModel());
            writeIntKV(buf, "evo.feed_forward_length", model.getDff());
            writeIntKV(buf, "evo.block_count", model.getNumBlocks());
            writeIntKV(buf, "evo.attention.head_count", model.getNumHeads());
            writeIntKV(buf, "evo.vocab_size", model.getVocabSize());

            // 2. Tensor Infos
            // GGUF requires: Name (string), Number of dimensions (uint32), Shape (uint64[]), Type (uint32), Offset (uint64)
            // Let's compute offsets
            long currentOffset = 0;
            List<Long> tensorOffsets = new ArrayList<>();
            for (Tensor p : params) {
                // Align currentOffset to 32 bytes
                currentOffset = (currentOffset + 31) & ~31;
                tensorOffsets.add(currentOffset);
                currentOffset += p.getSize() * 4; // F32
            }

            for (int i = 0; i < params.size(); i++) {
                Tensor p = params.get(i);
                String tName = "tensor_" + i;
                writeString(buf, tName);

                long[] shape = p.getShape();
                buf.putInt(shape.length);
                for (int d = shape.length - 1; d >= 0; d--) {
                    buf.putLong(shape[d]);
                }
                buf.putInt(0); // ggml_type (0 = F32)
                buf.putLong(tensorOffsets.get(i));
            }

            // Align to 32 bytes before tensor data
            int bytesToWrite = buf.position();
            int aligned = (bytesToWrite + 31) & ~31;
            while (buf.position() < aligned) {
                buf.put((byte) 0);
            }

            long tensorDataStart = buf.position();

            // 3. Write Tensor binary data
            for (int i = 0; i < params.size(); i++) {
                // Align to 32 bytes
                while ((buf.position() - tensorDataStart) < tensorOffsets.get(i)) {
                    buf.put((byte) 0);
                }
                Tensor p = params.get(i);
                float[] data = p.getData();
                for (float val : data) {
                    buf.putFloat(val);
                }
            }

            buf.flip();
            channel.write(buf);
        }

        // Generate Modelfile pointing directly to our own GGUF
        List<String> modelfile = new ArrayList<>();
        modelfile.add("FROM " + ggufPath.toAbsolutePath().toString().replace("\\", "/"));
        modelfile.add("PARAMETER temperature 0.7");
        modelfile.add("PARAMETER stop \"<EOS>\"");
        modelfile.add("SYSTEM \"\"\"You are a genuine EVO LLM assistant specialized in this project codebase.\"\"\"");

        Files.write(outputPath.resolve("Modelfile"), modelfile);

        // Copy GGUF model to Ollama default models directory
        Path ollamaHomeModels = Paths.get(System.getProperty("user.home")).resolve(".ollama/models");
        try {
            Files.createDirectories(ollamaHomeModels);
            Files.copy(ggufPath, ollamaHomeModels.resolve("evo.gguf"), StandardCopyOption.REPLACE_EXISTING);
            if (modelName != null && !modelName.isEmpty()) {
                Files.copy(ggufPath, ollamaHomeModels.resolve(modelName + ".gguf"), StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("[Export] Successfully copied GGUF files to Ollama default models folder: " + ollamaHomeModels.toAbsolutePath());
        } catch (Exception ex) {
            System.err.println("[Export] Warning: Failed to copy GGUF files to Ollama default models folder: " + ex.getMessage());
        }

        // Auto-create model in Ollama
        try {
            System.out.println("[Export] Programmatically executing: 'ollama create evo -f Modelfile'...");
            ProcessBuilder pb = new ProcessBuilder("ollama", "create", "evo", "-f", outputPath.resolve("Modelfile").toAbsolutePath().toString());
            pb.inheritIO();
            Process p = pb.start();
            p.waitFor();
            System.out.println("[Export] 'ollama create evo' finished with exit code: " + p.exitValue());
        } catch (Exception e) {
            System.err.println("[Export] Warning: Could not register model in Ollama: " + e.getMessage());
        }
    }

    private void writeStringKV(ByteBuffer buf, String key, String value) {
        writeString(buf, key);
        buf.putInt(8); // GGUF_METADATA_VALUE_TYPE_STRING
        writeString(buf, value);
    }

    private void writeIntKV(ByteBuffer buf, String key, int value) {
        writeString(buf, key);
        buf.putInt(4); // GGUF_METADATA_VALUE_TYPE_UINT32
        buf.putInt(value);
    }

    private void writeString(ByteBuffer buf, String str) {
        byte[] bytes = str.getBytes();
        buf.putLong(bytes.length);
        buf.put(bytes);
    }

    public boolean verifyExport(String modelName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ollama", "show", "evo");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    System.out.println("[Ollama Show] " + line);
                }
            }
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
