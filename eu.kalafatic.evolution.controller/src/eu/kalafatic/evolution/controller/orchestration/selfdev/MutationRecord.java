package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.HashMap;
import java.util.Map;

/**
 * Captures technical details of a specific evolutionary mutation.
 */
public class MutationRecord {
    private String strategy;
    private String semanticAnchor;
    private String philosophy;
    private String reasoningFocus;
    private Map<String, String> engineeringDimensions = new HashMap<>();
    private String executionModel;
    private String tradeoffs;
    private String survivalArgument;

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public String getSemanticAnchor() { return semanticAnchor; }
    public void setSemanticAnchor(String semanticAnchor) { this.semanticAnchor = semanticAnchor; }

    public String getPhilosophy() { return philosophy; }
    public void setPhilosophy(String philosophy) { this.philosophy = philosophy; }

    public String getReasoningFocus() { return reasoningFocus; }
    public void setReasoningFocus(String reasoningFocus) { this.reasoningFocus = reasoningFocus; }

    public Map<String, String> getEngineeringDimensions() { return engineeringDimensions; }
    public void setEngineeringDimensions(Map<String, String> engineeringDimensions) { this.engineeringDimensions = engineeringDimensions; }

    public String getExecutionModel() { return executionModel; }
    public void setExecutionModel(String executionModel) { this.executionModel = executionModel; }

    public String getTradeoffs() { return tradeoffs; }
    public void setTradeoffs(String tradeoffs) { this.tradeoffs = tradeoffs; }

    public String getSurvivalArgument() { return survivalArgument; }
    public void setSurvivalArgument(String survivalArgument) { this.survivalArgument = survivalArgument; }
}
