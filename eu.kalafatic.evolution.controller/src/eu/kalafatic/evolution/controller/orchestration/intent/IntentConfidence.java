package eu.kalafatic.evolution.controller.orchestration.intent;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Model for explicit intent confidence tracking.
 */
public class IntentConfidence {
    private double overallConfidence;
    private double structuralConfidence;
    private double semanticConfidence;
    private String rationale;

    public double getOverallConfidence() { return overallConfidence; }
    public void setOverallConfidence(double overallConfidence) { this.overallConfidence = overallConfidence; }

    public double getStructuralConfidence() { return structuralConfidence; }
    public void setStructuralConfidence(double structuralConfidence) { this.structuralConfidence = structuralConfidence; }

    public double getSemanticConfidence() { return semanticConfidence; }
    public void setSemanticConfidence(double semanticConfidence) { this.semanticConfidence = semanticConfidence; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
}
