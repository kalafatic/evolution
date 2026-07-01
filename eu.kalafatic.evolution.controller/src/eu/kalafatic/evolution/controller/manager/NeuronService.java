package eu.kalafatic.evolution.controller.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;

import eu.kalafatic.evolution.model.orchestration.NeuronAI;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.log.Log;

/**
 * @evo.lastModified: 14:A
 * @evo.origin: user
 *
 * Refactored Neuron AI service for context-aware and weighted assist.
 */
public class NeuronService {

    private static final NeuronService INSTANCE = new NeuronService();
    private static String GLOBAL_DIR = initializeGlobalDir();
    private static String GLOBAL_PATH = initializeGlobalPath();

    private static String initializeGlobalDir() {
        String home = System.getProperty("user.home");
        if (home == null) home = ".";
        return home + java.io.File.separator + "supervisor";
    }

    private static String initializeGlobalPath() {
        return initializeGlobalDir() + java.io.File.separator + "neuron_memory.json";
    }

    private final Map<String, Map<String, Integer>> globalMemory = new HashMap<>();

    private NeuronService() {
        loadGlobalMemory();
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveGlobalMemory));
    }

    public static NeuronService getInstance() {
        return INSTANCE;
    }

    public static void setGlobalPaths(String dir, String path) {
        GLOBAL_DIR = dir;
        GLOBAL_PATH = path;
        INSTANCE.loadGlobalMemory();
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
    public synchronized void train(Orchestrator orchestrator, String text, String category, int rating) {
        if (text == null || text.trim().isEmpty() || rating < 3) return;

        String cat = (category == null || category.isEmpty()) ? "chat" : category;
        Map<String, Integer> globalCatMemory = globalMemory.computeIfAbsent(cat, k -> new HashMap<>());

        // Update Global Memory
        updateMemoryMap(globalCatMemory, text);
        saveGlobalMemory();

        // Update Orchestrator Local Memory (Backward Compatibility)
        if (orchestrator != null) {
            NeuronAI neuronAI = orchestrator.getNeuronAI();
            if (neuronAI == null) {
                neuronAI = OrchestrationFactory.eINSTANCE.createNeuronAI();
                orchestrator.setNeuronAI(neuronAI);
            }
            Map<String, Map<String, Integer>> localMemory = loadMemory(neuronAI);
            Map<String, Integer> localCatMemory = localMemory.computeIfAbsent(cat, k -> new HashMap<>());
            updateMemoryMap(localCatMemory, text);
            saveMemory(neuronAI, localMemory);
        }
    }

    private void updateMemoryMap(Map<String, Integer> catMemory, String text) {
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
    }

    public String[] getProposals(Orchestrator orchestrator, String prefix) {
        return getProposals(orchestrator, prefix, "chat");
    }

    /**
     * @evo:14:A reason=categorized-weighted-proposals
     */
    public synchronized String[] getProposals(Orchestrator orchestrator, String prefix, String category) {
        if (prefix == null) return new String[0];

        String cat = (category == null || category.isEmpty()) ? "chat" : category;
        Map<String, Integer> mergedMemory = new HashMap<>();

        // Add from global memory
        Map<String, Integer> globalCatMemory = globalMemory.get(cat);
        if (globalCatMemory != null) {
            mergedMemory.putAll(globalCatMemory);
        }

        // Merge from local orchestrator memory
        if (orchestrator != null && orchestrator.getNeuronAI() != null) {
            Map<String, Map<String, Integer>> localMemory = loadMemory(orchestrator.getNeuronAI());
            Map<String, Integer> localCatMemory = localMemory.get(cat);
            if (localCatMemory != null) {
                for (Map.Entry<String, Integer> entry : localCatMemory.entrySet()) {
                    mergedMemory.put(entry.getKey(), mergedMemory.getOrDefault(entry.getKey(), 0) + entry.getValue());
                }
            }
        }

        if (mergedMemory.isEmpty()) return new String[0];

        String lowerPrefix = prefix.toLowerCase();
        return mergedMemory.entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().startsWith(lowerPrefix))
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(10)
                .map(Map.Entry::getKey)
                .toArray(String[]::new);
    }

    private Map<String, Map<String, Integer>> loadMemory(NeuronAI neuronAI) {
        return deserializeMemory(neuronAI.getTrainingData());
    }

    private void saveMemory(NeuronAI neuronAI, Map<String, Map<String, Integer>> memory) {
        neuronAI.setTrainingData(serializeMemory(memory));
    }

    private synchronized void loadGlobalMemory() {
        if (GLOBAL_PATH == null) return;
        java.io.File file = new java.io.File(GLOBAL_PATH);
        if (file.exists()) {
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                String data = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                Map<String, Map<String, Integer>> loaded = deserializeMemory(data);
                globalMemory.clear();
                globalMemory.putAll(loaded);
                Log.log("[NEURON] Global memory loaded from: " + GLOBAL_PATH);
            } catch (Exception e) {
                Log.log("[NEURON] Error loading global memory: " + e.getMessage());
            }
        }
    }

    private synchronized void saveGlobalMemory() {
        try {
            java.io.File dir = new java.io.File(GLOBAL_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String data = serializeMemory(globalMemory);
            java.nio.file.Files.write(new java.io.File(GLOBAL_PATH).toPath(), data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            Log.log("[NEURON] Global memory persisted to: " + GLOBAL_PATH);
        } catch (Exception e) {
            Log.log("[NEURON] Error saving global memory: " + e.getMessage());
        }
    }

    private Map<String, Map<String, Integer>> deserializeMemory(String data) {
        Map<String, Map<String, Integer>> memory = new HashMap<>();
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
                    System.err.println("Error deserializing neuron memory: " + ex.getMessage());
                }
            }
        }
        return memory;
    }

    private String serializeMemory(Map<String, Map<String, Integer>> memory) {
        JSONObject root = new JSONObject();
        for (Map.Entry<String, Map<String, Integer>> entry : memory.entrySet()) {
            JSONObject catObj = new JSONObject();
            for (Map.Entry<String, Integer> tokenEntry : entry.getValue().entrySet()) {
                catObj.put(tokenEntry.getKey(), tokenEntry.getValue().intValue());
            }
            root.put(entry.getKey(), catObj);
        }
        return root.toString();
    }
}
