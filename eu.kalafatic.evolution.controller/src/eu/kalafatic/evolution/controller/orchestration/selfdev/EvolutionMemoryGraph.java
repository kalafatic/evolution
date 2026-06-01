package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks the decision hierarchy and entropy progression of the evolution.
 */
public class EvolutionMemoryGraph {
    private List<EvolutionDimension> dimensions = new ArrayList<>();
    private Map<String, List<String>> rejectedBranches = new HashMap<>();
    private Map<String, String> rationales = new HashMap<>();
    private List<Double> entropyHistory = new ArrayList<>();
    private List<String> convergenceReasoning = new ArrayList<>();
    private List<EvolutionaryPressureVector> globalPressureHistory = new ArrayList<>();

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

    public void recordConvergenceReasoning(String reasoning) { convergenceReasoning.add(reasoning); }
    public List<String> getConvergenceReasoning() { return convergenceReasoning; }

    public void recordGlobalPressure(EvolutionaryPressureVector pressure) { globalPressureHistory.add(pressure); }
    public List<EvolutionaryPressureVector> getGlobalPressureHistory() { return globalPressureHistory; }

    public synchronized void restore(
            List<EvolutionDimension> dimensions,
            Map<String, List<String>> rejectedBranches,
            Map<String, String> rationales,
            List<Double> entropyHistory,
            List<String> convergenceReasoning,
            List<EvolutionaryPressureVector> globalPressureHistory) {
        if (dimensions != null) {
            this.dimensions.clear();
            this.dimensions.addAll(dimensions);
        }
        if (rejectedBranches != null) {
            this.rejectedBranches.clear();
            this.rejectedBranches.putAll(rejectedBranches);
        }
        if (rationales != null) {
            this.rationales.clear();
            this.rationales.putAll(rationales);
        }
        if (entropyHistory != null) {
            this.entropyHistory.clear();
            this.entropyHistory.addAll(entropyHistory);
        }
        if (convergenceReasoning != null) {
            this.convergenceReasoning.clear();
            this.convergenceReasoning.addAll(convergenceReasoning);
        }
        if (globalPressureHistory != null) {
            this.globalPressureHistory.clear();
            this.globalPressureHistory.addAll(globalPressureHistory);
        }
    }
}
