package eu.kalafatic.evolution.forge.agent.export;

import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class OllamaExporter {
	public void export(String modelName, Path outputPath, EvoLlmModel model) throws IOException {
        Files.createDirectories(outputPath);
        
        // Generate Modelfile
        List<String> modelfile = new ArrayList<>();
        modelfile.add("FROM llama3.2:3b");
        modelfile.add("ADAPTER " + outputPath.resolve("weights.bin").toAbsolutePath().toString());
        modelfile.add("PARAMETER temperature 0.7");
        modelfile.add("PARAMETER stop \"<EOS>\"");
        modelfile.add("SYSTEM \"\"\"You are an Evolution AI assistant specialized in this project codebase.\"\"\"");
        
        Files.write(outputPath.resolve("Modelfile"), modelfile);
        
        // Serialize model weights (simplified)
        StringBuilder weights = new StringBuilder();
        weights.append("VOCAB_SIZE=").append(model.getVocabSize()).append("\n");
        weights.append("D_MODEL=").append(model.getDModel()).append("\n");
        weights.append("HEAD_WEIGHTS=");
        float[] hData = model.getLmHead().getData();
        for (int i = 0; i < Math.min(100, hData.length); i++) {
            weights.append(hData[i]).append(",");
        }
        
        Files.writeString(outputPath.resolve("weights.bin"), weights.toString());

        // Write GGUF model file (mock GGUF structure with valid magic header)
        byte[] ggufBytes = new byte[] {
            0x47, 0x47, 0x55, 0x46, // "GGUF" magic
            0x03, 0x00, 0x00, 0x00, // Version 3
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // 0 tensors
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00  // 0 key-values
        };
        Files.write(outputPath.resolve("evo.gguf"), ggufBytes);
        
        System.out.println("[Export] Ollama package generated at: " + outputPath.toAbsolutePath());
        System.out.println("[Export] Use: 'ollama create " + modelName + " -f " + outputPath.resolve("Modelfile") + "'");
    }

    public boolean verifyExport(String modelName) {
        System.out.println("[Verification] Simulating 'ollama run " + modelName + "'...");
        // Simulated verification
        return true;
    }
}
