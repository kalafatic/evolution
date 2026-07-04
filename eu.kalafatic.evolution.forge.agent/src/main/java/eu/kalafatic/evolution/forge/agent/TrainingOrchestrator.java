package eu.kalafatic.evolution.forge.agent;

import java.io.*;
import java.nio.file.*;
import java.util.List;

public class TrainingOrchestrator {
    private final Path projectPath;
    private final SmartScanner scanner;
    private final DataForgeEngine forge;
    private final AiDataEnhancer enhancer;
    private final ModelExporter exporter = new ModelExporter();
    private EvoForgeServer server;

    public TrainingOrchestrator(Path projectPath, AiDataEnhancer enhancer) {
        this.projectPath = projectPath;
        this.scanner = new SmartScanner(projectPath);
        this.forge = new DataForgeEngine();
        this.enhancer = enhancer;
    }

    public void setServer(EvoForgeServer server) { this.server = server; }

    public void runPipeline() throws Exception {
        updateStatus("Forging...", 50);
        List<Path> files = scanner.scan();
        Path dataDir = projectPath.resolve("evo-forge-data");
        Files.createDirectories(dataDir);
        forge.buildDataset(files, dataDir.resolve("raw_data.jsonl"));
        updateStatus("Complete", 100);
    }

    private void updateStatus(String s, int p) { if (server != null) server.updateProgress(s, p); }
}
