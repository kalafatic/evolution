package eu.kalafatic.evolution.controller.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;

import eu.kalafatic.evolution.model.orchestration.NeuronAI;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * @evo.lastModified: 14:A
 * @evo.origin: user
 *
 * Refactored Neuron AI service for context-aware and weighted assist.
 */
public class NeuronService {

    private static final NeuronService INSTANCE = new NeuronService();

    private NeuronService() {}

    public static NeuronService getInstance() {
        return INSTANCE;
    }

    public void train(Orchestrator orchestrator, String text) {
        train(orchestrator, text, "chat", 3);
    }

    public void train(Orchestrator orchestrator, String text, int rating) {
        train(orchestrator, text, "chat", rating);
    }

    /**
     * @evo:14:A reason=categorized-weighted-training
     */
    public void train(Orchestrator orchestrator, String text, String category, int rating) {
        if (orchestrator == null || text == null || text.trim().isEmpty() || rating < 3) return;

        NeuronAI neuronAI = orchestrator.getNeuronAI();
        if (neuronAI == null) {
            neuronAI = OrchestrationFactory.eINSTANCE.createNeuronAI();
            orchestrator.setNeuronAI(neuronAI);
        }

        Map<String, Map<String, Integer>> memory = loadMemory(neuronAI);
        String cat = (category == null || category.isEmpty()) ? "chat" : category;
        Map<String, Integer> catMemory = memory.computeIfAbsent(cat, k -> new HashMap<>());

        // Tokenize and increment weights
        String[] tokens = text.toLowerCase().split("[^a-zA-Z0-9\\-]+");
        for (String token : tokens) {
            if (token.length() > 2) {
                catMemory.put(token, catMemory.getOrDefault(token, 0) + 1);
            }
        }

        // Add short full sentences
        if (text.length() < 100) {
            String trimmed = text.trim();
            catMemory.put(trimmed, catMemory.getOrDefault(trimmed, 0) + 5); // higher weight for full phrases
        }

        // Prune if too large
        if (catMemory.size() > 500) {
            List<String> toKeep = catMemory.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(500)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            catMemory.keySet().retainAll(toKeep);
        }

        saveMemory(neuronAI, memory);
    }

    public String[] getProposals(Orchestrator orchestrator, String prefix) {
        return getProposals(orchestrator, prefix, "chat");
    }

    /**
     * @evo:14:A reason=categorized-weighted-proposals
     */
    public String[] getProposals(Orchestrator orchestrator, String prefix, String category) {
        if (orchestrator == null || orchestrator.getNeuronAI() == null || prefix == null) {
            return new String[0];
        }

        Map<String, Map<String, Integer>> memory = loadMemory(orchestrator.getNeuronAI());
        String cat = (category == null || category.isEmpty()) ? "chat" : category;
        Map<String, Integer> catMemory = memory.get(cat);

        if (catMemory == null) return new String[0];

        String lowerPrefix = prefix.toLowerCase();
        return catMemory.entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().startsWith(lowerPrefix))
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(10)
                .map(Map.Entry::getKey)
                .toArray(String[]::new);
    }

    private Map<String, Map<String, Integer>> loadMemory(NeuronAI neuronAI) {
        Map<String, Map<String, Integer>> memory = new HashMap<>();
        String data = neuronAI.getTrainingData();
        if (data != null && !data.isEmpty()) {
            try {
                JSONObject root = new JSONObject(data);
                String[] categories = JSONObject.getNames(root);
                if (categories != null) {
                    for (String cat : categories) {
                        JSONObject catObj = root.getJSONObject(cat);
                        Map<String, Integer> catMap = new HashMap<>();
                        String[] tokens = JSONObject.getNames(catObj);
                        if (tokens != null) {
                            for (String token : tokens) {
                                catMap.put(token, catObj.getInt(token));
                            }
                        }
                        memory.put(cat, catMap);
                    }
                }
            } catch (Exception e) {
                // Fallback for old format (JSONArray)
                try {
                    org.json.JSONArray oldArray = new org.json.JSONArray(data);
                    Map<String, Integer> chatMap = new HashMap<>();
                    for (int i = 0; i < oldArray.length(); i++) {
                        chatMap.put(oldArray.getString(i), 1);
                    }
                    memory.put("chat", chatMap);
                } catch (Exception ex) {
                    System.err.println("Error loading neuron memory: " + e.getMessage());
                }
            }
        }
        return memory;
    }

    private void saveMemory(NeuronAI neuronAI, Map<String, Map<String, Integer>> memory) {
        JSONObject root = new JSONObject();
        for (Map.Entry<String, Map<String, Integer>> entry : memory.entrySet()) {
            JSONObject catObj = new JSONObject();
            for (Map.Entry<String, Integer> tokenEntry : entry.getValue().entrySet()) {
                catObj.put(tokenEntry.getKey(), tokenEntry.getValue().intValue());
            }
            root.put(entry.getKey(), catObj);
        }
        neuronAI.setTrainingData(root.toString());
    }
}
