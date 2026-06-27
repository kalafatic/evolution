package eu.kalafatic.evolution.controller.orchestration.selfdev;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Captures multi-dimensional fitness of an evolutionary node.
 */
public class FitnessRecord {
    private double totalScore;
    private double verificationSuccess;
    private double compilationFitness;
    private double testFitness;
    private double repositoryImpact;
    private double architecturalQuality;
    private double goalSatisfaction;
    private double implementationCompleteness;
    private double userApproval;
    private double llmConfidence;

    public double getTotalScore() { return totalScore; }
    public void setTotalScore(double totalScore) { this.totalScore = totalScore; }

    public double getVerificationSuccess() { return verificationSuccess; }
    public void setVerificationSuccess(double verificationSuccess) { this.verificationSuccess = verificationSuccess; }

    public double getCompilationFitness() { return compilationFitness; }
    public void setCompilationFitness(double compilationFitness) { this.compilationFitness = compilationFitness; }

    public double getTestFitness() { return testFitness; }
    public void setTestFitness(double testFitness) { this.testFitness = testFitness; }

    public double getRepositoryImpact() { return repositoryImpact; }
    public void setRepositoryImpact(double repositoryImpact) { this.repositoryImpact = repositoryImpact; }

    public double getArchitecturalQuality() { return architecturalQuality; }
    public void setArchitecturalQuality(double architecturalQuality) { this.architecturalQuality = architecturalQuality; }

    public double getGoalSatisfaction() { return goalSatisfaction; }
    public void setGoalSatisfaction(double goalSatisfaction) { this.goalSatisfaction = goalSatisfaction; }

    public double getImplementationCompleteness() { return implementationCompleteness; }
    public void setImplementationCompleteness(double implementationCompleteness) { this.implementationCompleteness = implementationCompleteness; }

    public double getUserApproval() { return userApproval; }
    public void setUserApproval(double userApproval) { this.userApproval = userApproval; }

    public double getLlmConfidence() { return llmConfidence; }
    public void setLlmConfidence(double llmConfidence) { this.llmConfidence = llmConfidence; }
}
