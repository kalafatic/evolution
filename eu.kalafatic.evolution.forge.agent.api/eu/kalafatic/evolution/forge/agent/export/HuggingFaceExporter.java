package eu.kalafatic.evolution.forge.agent.export;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.model.llm.EvoModelExporter;
import eu.kalafatic.evolution.forge.model.llm.EvoTokenizerArtifact;

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

        EvoLlmArchitecture arch = artifact.getArchitectureConfig();
        if (arch == null) {
            throw new IllegalArgumentException("Artifact missing architecture configuration.");
        }

        // 1. Write Hugging Face config.json
        StringBuilder configJson = new StringBuilder();
        configJson.append("{\n")
                .append("  \"architectures\": [\"LlamaForCausalLM\"],\n")
                .append("  \"model_type\": \"llama\",\n")
                .append("  \"vocab_size\": ").append(arch.getVocabSize()).append(",\n")
                .append("  \"hidden_size\": ").append(arch.getDModel()).append(",\n")
                .append("  \"intermediate_size\": ").append(arch.getDff()).append(",\n")
                .append("  \"num_hidden_layers\": ").append(arch.getNumBlocks()).append(",\n")
                .append("  \"num_attention_heads\": ").append(arch.getNumHeads()).append(",\n")
                .append("  \"num_key_value_heads\": ").append(arch.getNumHeads()).append(",\n")
                .append("  \"max_position_embeddings\": ").append(arch.getMaxSeqLen()).append(",\n")
                .append("  \"torch_dtype\": \"float32\"\n")
                .append("}");
        Files.writeString(outputDirectory.resolve("config.json"), configJson.toString());

        // 2. Write tokenizer.json
        EvoTokenizerArtifact tokArtifact = artifact.getTokenizerArtifact();
        Map<String, Integer> tokVocab = tokArtifact != null ? tokArtifact.getVocab() : artifact.getTokenizerVocab();
        StringBuilder tokBuilder = new StringBuilder();
        tokBuilder.append("{\n")
                .append("  \"type\": \"").append(tokArtifact != null ? tokArtifact.getTokenizerType() : "SimpleBPE").append("\",\n")
                .append("  \"vocabSize\": ").append(arch.getVocabSize()).append(",\n")
                .append("  \"vocab\": {\n");
        int count = 0;
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
        tokBuilder.append("  }\n}");
        Files.writeString(outputDirectory.resolve("tokenizer.json"), tokBuilder.toString());

        // 3. Save raw float weights file
        Path weightsPath = outputDirectory.resolve("model.bin");
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(weightsPath.toFile())))) {
            for (Tensor p : artifact.getWeights()) {
                for (float val : p.getData()) {
                    dos.writeFloat(val);
                }
            }
            dos.flush();
        }
    }
}
