package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Tracks the decision hierarchy and entropy progression of the evolution.
 */
public class EvolutionMemoryGraph {
    private List<EvolutionDimension> dimensions = new ArrayList<>();
    private Map<String, List<String>> rejectedBranches = new HashMap<>();
    private Map<String, String> rationales = new HashMap<>();
    private List<Double> entropyHistory = new ArrayList<>();

    public void recordDimension(EvolutionDimension dimension) {
        dimensions.add(dimension);
    }

    public void recordRejection(String dimensionId, String branchId, String rationale) {
        rejectedBranches.computeIfAbsent(dimensionId, k -> new ArrayList<>()).add(branchId);
        rationales.put(branchId, rationale);
    }

    public void recordEntropy(double entropy) {
        entropyHistory.add(entropy);
    }

    public List<EvolutionDimension> getDimensions() { return dimensions; }
    public Map<String, List<String>> getRejectedBranches() { return rejectedBranches; }
    public Map<String, String> getRationales() { return rationales; }
    public List<Double> getEntropyHistory() { return entropyHistory; }
}
