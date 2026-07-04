package eu.kalafatic.evolution.forge.agent;

import eu.kalafatic.evolution.forge.controller.service.OllamaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class AiDataEnhancer {
    private final OllamaService ollama;
    private final ObjectMapper mapper = new ObjectMapper();

    public AiDataEnhancer(OllamaService ollama) {
        this.ollama = ollama;
    }

    public void enhance(Path inputDataset, Path outputDataset) throws Exception {
        List<String> lines = Files.readAllLines(inputDataset);
        try (BufferedWriter writer = Files.newBufferedWriter(outputDataset)) {
            for (String line : lines) {
                ObjectNode node = (ObjectNode) mapper.readTree(line);
                String code = node.get("output").asText();

                String prompt = "Review this code and generate a high-quality programming instruction for it. " +
                                "Return only a JSON object with 'instruction' and 'response' keys.\n\nCode:\n" + code;

                try {
                    String aiResponse = ollama.generate(prompt);
                    // Extract JSON from AI response if it's wrapped in markdown
                    String cleanJson = extractJson(aiResponse);
                    writer.write(cleanJson);
                    writer.newLine();
                } catch (Exception e) {
                    System.err.println("AI Enhancement failed for an entry: " + e.getMessage());
                    writer.write(line); // Fallback to original
                    writer.newLine();
                }
            }
        }
    }

    private String extractJson(String text) {
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
