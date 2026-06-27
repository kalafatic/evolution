package eu.kalafatic.evolution.controller.orchestration.intent;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a coherent interpretation of the request.
 */
public class IntentHypothesis {
    private String id;
    private String description;
    private List<DimensionValue> dimensionValues = new ArrayList<>();
    private double confidence;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<DimensionValue> getDimensionValues() { return dimensionValues; }
    public void setDimensionValues(List<DimensionValue> dimensionValues) { this.dimensionValues = dimensionValues; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public static class DimensionValue {
        private String dimensionId;
        private String value;

        public String getDimensionId() { return dimensionId; }
        public void setDimensionId(String dimensionId) { this.dimensionId = dimensionId; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}
