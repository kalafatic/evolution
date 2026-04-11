package eu.kalafatic.evolution.controller.services;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.utils.log.Log;

/**
 * Learns from previous IterationRecords and provides context for future tasks.
 * Synchronizes with a local JSON file to maintain the 'neuron' state.
 */
public class NeuronContextService {

    private final Orchestrator orchestrator;
    private final File projectRoot;
    private final File neuronFile;
    private final ObjectMapper mapper = new ObjectMapper();

    public NeuronContextService(Orchestrator orchestrator, File projectRoot) {
        this.orchestrator = orchestrator;
        this.projectRoot = projectRoot;
        this.neuronFile = new File(projectRoot, "orchestrator/memory/neuron_context.json");
        ensureMemoryDir();
    }

    private void ensureMemoryDir() {
        File dir = neuronFile.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void learnFromHistory() {
        Log.log("NeuronContext: Starting learning process from iteration history...");
        File historyDir = new File(projectRoot, "orchestrator/memory/");
        File[] records = historyDir.listFiles((dir, name) -> name.startsWith("iteration_") && name.endsWith(".json"));

        List<String> insights = new ArrayList<>();
        if (records != null) {
            for (File record : records) {
                try {
                    String content = new String(Files.readAllBytes(record.toPath()), StandardCharsets.UTF_8);
                    if (content.contains("\"score\": 1.0") || content.contains("\"score\":1.0")) {
                        insights.add("Successful Strategy detected in " + record.getName());
                    }
                } catch (IOException e) {
                    Log.log("NeuronContext: Error learning from " + record.getName() + ": " + e.getMessage());
                }
            }
        }

        saveContext(insights);
    }

    private void saveContext(List<String> insights) {
        try {
            mapper.writeValue(neuronFile, insights);
            Log.log("NeuronContext: Saved " + insights.size() + " insights to " + neuronFile.getName());
        } catch (IOException e) {
            Log.log("NeuronContext: Failed to save neuron context: " + e.getMessage());
        }
    }

    public String getLearnedContext() {
        if (!neuronFile.exists()) {
            return "";
        }
        try {
            List<?> insights = mapper.readValue(neuronFile, List.class);
            if (insights == null || insights.isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            sb.append("### LEARNED BEHAVIOR CONTEXT (NEURON)\n");
            sb.append("Based on past successful iterations, prefer these patterns:\n");
            for (Object insight : insights) {
                sb.append("- ").append(insight.toString()).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            Log.log("NeuronContext: Failed to load neuron context: " + e.getMessage());
            return "";
        }
    }
}
