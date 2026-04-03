package eu.kalafatic.evolution.controller.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;

import eu.kalafatic.evolution.model.orchestration.NeuronAI;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Simple "Neuron AI" service for context assist.
 * It simulates a simple memory by storing unique tokens/phrases in the EMF model.
 */
public class NeuronService {

    private static final NeuronService INSTANCE = new NeuronService();

    private NeuronService() {}

    public static NeuronService getInstance() {
        return INSTANCE;
    }

    /**
     * Trains the "neuron" on the given text.
     */
    public void train(Orchestrator orchestrator, String text) {
        if (orchestrator == null || text == null || text.trim().isEmpty()) return;

        NeuronAI neuronAI = orchestrator.getNeuronAI();
        if (neuronAI == null) {
            neuronAI = OrchestrationFactory.eINSTANCE.createNeuronAI();
            orchestrator.setNeuronAI(neuronAI);
        }

        Set<String> memory = loadMemory(neuronAI);

        // Simple tokenization: split by non-alphanumeric characters
        String[] tokens = text.toLowerCase().split("[^a-zA-Z0-9\\-]+");
        for (String token : tokens) {
            if (token.length() > 2) {
                memory.add(token);
            }
        }

        // Also add full sentences as possible completions if they are short
        if (text.length() < 100) {
            memory.add(text.trim());
        }

        saveMemory(neuronAI, memory);
    }

    /**
     * Returns proposals based on the trained data.
     */
    public String[] getProposals(Orchestrator orchestrator, String prefix) {
        if (orchestrator == null || orchestrator.getNeuronAI() == null || prefix == null) {
            return new String[0];
        }

        Set<String> memory = loadMemory(orchestrator.getNeuronAI());
        String lowerPrefix = prefix.toLowerCase();

        return memory.stream()
                .filter(s -> s.toLowerCase().startsWith(lowerPrefix))
                .sorted()
                .limit(10)
                .toArray(String[]::new);
    }

    private Set<String> loadMemory(NeuronAI neuronAI) {
        Set<String> memory = new HashSet<>();
        String data = neuronAI.getTrainingData();
        if (data != null && !data.isEmpty()) {
            try {
                JSONArray array = new JSONArray(data);
                for (int i = 0; i < array.length(); i++) {
                    memory.add(array.getString(i));
                }
            } catch (Exception e) {
                // Fallback or log
                System.err.println("Error loading neuron memory: " + e.getMessage());
            }
        }
        return memory;
    }

    private void saveMemory(NeuronAI neuronAI, Set<String> memory) {
        JSONArray array = new JSONArray();
        // Limit memory size to prevent EMF resource bloat
        memory.stream().limit(500).forEach(array::put);
        neuronAI.setTrainingData(array.toString());
    }
}
