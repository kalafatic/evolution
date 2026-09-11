package eu.kalafatic.evolution.forge.agent.export;

import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.model.llm.EvoModelExporter;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Exporter implementation serializing native EvoModelArtifact into Hugging Face format.
 */
public class HuggingFaceExporter implements EvoModelExporter {

    @Override
    public void export(EvoModelArtifact artifact, Path outputDirectory) throws Exception {
        Files.createDirectories(outputDirectory);

        int vocabSize = artifact.getVocabSize();
        int dModel = artifact.getDModel();
        int dff = artifact.getDff();
        int numBlocks = artifact.getNumBlocks();
        int numHeads = artifact.getNumHeads();
        int maxSeqLen = artifact.getMaxSeqLen();

        // 1. Write Hugging Face config.json
        StringBuilder configJson = new StringBuilder();
        configJson.append("{\n")
                .append("  \"architectures\": [\"LlamaForCausalLM\"],\n")
                .append("  \"model_type\": \"llama\",\n")
                .append("  \"vocab_size\": ").append(vocabSize).append(",\n")
                .append("  \"hidden_size\": ").append(dModel).append(",\n")
                .append("  \"intermediate_size\": ").append(dff).append(",\n")
                .append("  \"num_hidden_layers\": ").append(numBlocks).append(",\n")
                .append("  \"num_attention_heads\": ").append(numHeads).append(",\n")
                .append("  \"num_key_value_heads\": ").append(numHeads).append(",\n")
                .append("  \"max_position_embeddings\": ").append(maxSeqLen).append(",\n")
                .append("  \"torch_dtype\": \"float32\"\n")
                .append("}");
        Files.writeString(outputDirectory.resolve("config.json"), configJson.toString());

        // 2. Write tokenizer.json
        Map<String, Integer> tokVocab = artifact.getTokenizerVocab();
        StringBuilder tokBuilder = new StringBuilder();
        tokBuilder.append("{\n")
                .append("  \"type\": \"SimpleBPE\",\n")
                .append("  \"vocabSize\": ").append(vocabSize).append(",\n")
                .append("  \"vocab\": {\n");
        int count = 0;
        if (tokVocab != null) {
            for (Map.Entry<String, Integer> entry : tokVocab.entrySet()) {
                String cleanKey = entry.getKey()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");
                tokBuilder.append("    \"").append(cleanKey).append("\": ").append(entry.getValue());
                if (++count < tokVocab.size()) {
                    tokBuilder.append(",");
                }
                tokBuilder.append("\n");
            }
        }
        tokBuilder.append("  }\n}");
        Files.writeString(outputDirectory.resolve("tokenizer.json"), tokBuilder.toString());

        // 3. Save raw float weights file
        Path weightsPath = outputDirectory.resolve("model.bin");
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(weightsPath.toFile())))) {
            if (artifact.getWeightData() != null) {
                for (float[] p : artifact.getWeightData()) {
                    for (float val : p) {
                        dos.writeFloat(val);
                    }
                }
            }
            dos.flush();
        }
    }
}
