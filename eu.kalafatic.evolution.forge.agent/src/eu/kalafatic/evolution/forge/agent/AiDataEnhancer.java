package eu.kalafatic.evolution.forge.agent;

import eu.kalafatic.evolution.controller.manager.OllamaService;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
                    writer.write(extractJson(aiResponse));
                    writer.newLine();
                } catch (Exception e) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        }
    }

    private String extractJson(String text) {
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) return text.substring(start, end + 1);
        return text;
    }
}
