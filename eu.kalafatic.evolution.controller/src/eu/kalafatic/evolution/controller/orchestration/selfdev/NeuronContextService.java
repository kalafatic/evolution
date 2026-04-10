package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.kalafatic.utils.log.Log;

/**
 * Service to manage "Neural Context" - patterns learned from user behavior and iteration history.
 */
public class NeuronContextService {
    private final File contextFile;
    private final ObjectMapper mapper;
    private NeuronContextModel model;

    public NeuronContextService(File projectRoot) {
        this.contextFile = new File(projectRoot, "orchestrator/memory/neuron_context.json");
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        load();
    }

    public void load() {
        if (contextFile.exists()) {
            try {
                model = mapper.readValue(contextFile, NeuronContextModel.class);
            } catch (IOException e) {
                Log.log("Failed to load neuron context model: " + e.getMessage());
                model = new NeuronContextModel();
            }
        } else {
            model = new NeuronContextModel();
        }
    }

    public void save() {
        try {
            if (!contextFile.getParentFile().exists()) {
                contextFile.getParentFile().mkdirs();
            }
            mapper.writeValue(contextFile, model);
        } catch (IOException e) {
            Log.log("Failed to save neuron context model: " + e.getMessage());
        }
    }

    /**
     * "Learn" from iteration records.
     */
    public void learn(List<IterationRecord> records) {
        if (records == null || records.isEmpty()) return;

        Map<String, Integer> strategySuccessCount = new HashMap<>();
        for (IterationRecord record : records) {
            if ("SUCCESS".equals(record.getResult())) {
                String strategy = record.getStrategy();
                if (strategy != null) {
                    strategySuccessCount.put(strategy, strategySuccessCount.getOrDefault(strategy, 0) + 1);
                }
            }
        }

        // Extract "Successful Patterns" - simple heuristic for now
        List<String> patterns = new ArrayList<>();
        strategySuccessCount.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(5)
            .forEach(e -> patterns.add("Successful Strategy: " + e.getKey() + " (Used " + e.getValue() + " times)"));

        model.setLearnedPatterns(patterns);
        model.setLastUpdated(System.currentTimeMillis());
        save();
    }

    public String getContextPromptSnippet() {
        if (model.getLearnedPatterns().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- LEARNED BEHAVIOR CONTEXT ---\n");
        for (String pattern : model.getLearnedPatterns()) {
            sb.append("- ").append(pattern).append("\n");
        }
        sb.append("--- END LEARNED CONTEXT ---\n");
        return sb.toString();
    }

    public NeuronContextModel getModel() {
        return model;
    }

    public static class NeuronContextModel {
        private List<String> learnedPatterns = new ArrayList<>();
        private long lastUpdated;

        public List<String> getLearnedPatterns() { return learnedPatterns; }
        public void setLearnedPatterns(List<String> learnedPatterns) { this.learnedPatterns = learnedPatterns; }

        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    }
}
