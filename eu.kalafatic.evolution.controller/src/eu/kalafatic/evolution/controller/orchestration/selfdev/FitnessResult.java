package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.Map;

/**
 * Result of a fitness evaluation.
 */
public class FitnessResult {
    public double score;          // 0.0 - 1.0
    public boolean thresholdMet;
    public String justification;
    public double buildWeight;
    public double testWeight;
    public double coverageWeight;
    public double complexityWeight;
    public double securityWeight;
    public Map<String, Double> weights;
    public long timestamp;
    
    public FitnessResult() {
        this.weights = new java.util.HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    public void setWeight(String name, double value) {
        if (weights == null) {
            weights = new java.util.HashMap<>();
        }
        weights.put(name, value);
    }
    
    public double getWeight(String name) {
        return weights != null ? weights.getOrDefault(name, 0.0) : 0.0;
    }
    
    public boolean isSuccess() {
        return thresholdMet;
    }
    
    @Override
    public String toString() {
        return "FitnessResult{" +
                "score=" + String.format("%.3f", score) +
                ", thresholdMet=" + thresholdMet +
                ", buildWeight=" + buildWeight +
                ", testWeight=" + testWeight +
                ", justification='" + justification + '\'' +
                '}';
    }
}
