package eu.kalafatic.evolution.controller.orchestration.goal;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Formal semantic contract representing the user's goal.
 */
public class GoalModel {
    private String goalType;         // e.g., CODE_GENERATION, REFACTORING, ANALYSIS
    private String domain;           // e.g., JAVA, SPRING, SQL, HTML, AI, ARCHITECTURE
    private String intent;           // e.g., CREATE, MODIFY, DELETE, EXPLORE
    private String requestedArtifact; // e.g., Java Class, Method, Configuration, Documentation
    private String primaryAction;    // e.g., print text, calculate sum, connect to DB
    private String complexity;       // e.g., SIMPLE, MEDIUM, HIGH
    private String requiredOutputs;  // e.g., Java source file, SQL script
    private double confidence;       // 0.0 to 1.0
    private double ambiguity;        // 0.0 to 1.0

    public String getGoalType() { return goalType; }
    public void setGoalType(String goalType) { this.goalType = goalType; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getRequestedArtifact() { return requestedArtifact; }
    public void setRequestedArtifact(String requestedArtifact) { this.requestedArtifact = requestedArtifact; }

    public String getPrimaryAction() { return primaryAction; }
    public void setPrimaryAction(String primaryAction) { this.primaryAction = primaryAction; }

    public String getComplexity() { return complexity; }
    public void setComplexity(String complexity) { this.complexity = complexity; }

    public String getRequiredOutputs() { return requiredOutputs; }
    public void setRequiredOutputs(String requiredOutputs) { this.requiredOutputs = requiredOutputs; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public double getAmbiguity() { return ambiguity; }
    public void setAmbiguity(double ambiguity) { this.ambiguity = ambiguity; }

    @Override
    public String toString() {
        return String.format("GoalModel[Type=%s, Domain=%s, Intent=%s, Artifact=%s, Action=%s, Complexity=%s, Confidence=%.2f]",
            goalType, domain, intent, requestedArtifact, primaryAction, complexity, confidence);
    }
}
