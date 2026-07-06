package eu.kalafatic.evolution.forge.agent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DataForgeEngine {
    private final ObjectMapper mapper = new ObjectMapper();

    public void buildDataset(List<Path> files, Path outputPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            for (Path file : files) {
                String content = Files.readString(file);
                if (content.trim().isEmpty()) continue;
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
                node.put("output", content);
                writer.write(mapper.writeValueAsString(node));
                writer.newLine();
            }
        }
    }
}
