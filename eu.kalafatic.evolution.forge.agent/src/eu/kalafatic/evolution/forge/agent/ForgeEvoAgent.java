package eu.kalafatic.evolution.forge.agent;

import java.nio.file.Paths;
import eu.kalafatic.evolution.controller.manager.OllamaManager;
import eu.kalafatic.evolution.controller.manager.OllamaService;


public class ForgeEvoAgent {
    public static void main(String[] args) {
        String projectPath = ".";
        String ollamaUrl = "http://localhost:11434";
        try {
            EvoForgeServer server = new EvoForgeServer(58081);
            server.start(30000, false);
            OllamaService ollama = OllamaManager.getInstance().getService(ollamaUrl);
            TrainingOrchestrator orch = new TrainingOrchestrator(Paths.get(projectPath), new AiDataEnhancer(ollama));
            orch.setServer(server);
            orch.runPipeline();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
