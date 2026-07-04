package eu.kalafatic.evolution.forge.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class DataForgeEngine {
    private final ObjectMapper mapper = new ObjectMapper();

    public void buildDataset(List<Path> files, Path outputPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            for (Path file : files) {
                String content = Files.readString(file);
                if (content.trim().isEmpty()) continue;

                // Simple pre-training entry
                ObjectNode node = mapper.createObjectNode();
                node.put("text", "File: " + file.getFileName().toString() + "\n\nContent:\n" + content);
                writer.write(mapper.writeValueAsString(node));
                writer.newLine();
            }
        }
    }

    public void createInstructionPairs(List<Path> files, Path outputPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            for (Path file : files) {
                String content = Files.readString(file);
                if (content.trim().isEmpty()) continue;

                ObjectNode node = mapper.createObjectNode();
                node.put("instruction", "Explain the purpose of the code or content in " + file.getFileName().toString());
                node.put("input", "");
                node.put("output", content); // Placeholder, will be improved by AiDataEnhancer
                writer.write(mapper.writeValueAsString(node));
                writer.newLine();
            }
        }
    }
}
