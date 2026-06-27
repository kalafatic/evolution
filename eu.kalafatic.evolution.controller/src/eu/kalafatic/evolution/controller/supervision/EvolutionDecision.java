package eu.kalafatic.evolution.controller.supervision;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.List;
import java.util.Map;

/**
 * Immutable object containing a single authority decision per Darwin cycle.
 */
public class EvolutionDecision {
    private final String selectedVariantId;
    private final List<String> rejectedVariantIds;
    private final String rationale;
    private final Map<String, Double> aggregatedScores;
    private final Map<String, Object> policyMetadata;
    private final AuthorityController.DecisionType type;
    private eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector pressure;

    public EvolutionDecision(
            AuthorityController.DecisionType type,
            String selectedVariantId,
            List<String> rejectedVariantIds,
            String rationale,
            Map<String, Double> aggregatedScores,
            Map<String, Object> policyMetadata) {
        this.type = type;
        this.selectedVariantId = selectedVariantId;
        this.rejectedVariantIds = rejectedVariantIds;
        this.rationale = rationale;
        this.aggregatedScores = aggregatedScores;
        this.policyMetadata = policyMetadata;
    }

    public AuthorityController.DecisionType getType() {
        return type;
    }

    public String getSelectedVariantId() {
        return selectedVariantId;
    }

    public List<String> getRejectedVariantIds() {
        return rejectedVariantIds;
    }

    public String getRationale() {
        return rationale;
    }

    public Map<String, Double> getAggregatedScores() {
        return aggregatedScores;
    }

    public Map<String, Object> getPolicyMetadata() {
        return policyMetadata;
    }

    public eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector getPressure() {
        return pressure;
    }

    public void setPressure(eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector pressure) {
        this.pressure = pressure;
    }
}
