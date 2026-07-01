package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.controller.mediation.model.MediationCandidate;

public class BranchVariant {
    public enum ActivationState {
        CREATED,
        ANALYZING,
        PLANNED,
        SCORING,
        RECOMMENDED,
        KEPT,
        APPROVED,
        ACTIVE,
        EXECUTING,
        VERIFIED,
        REJECTED,
        ARCHIVED
    }

    public enum ReasoningLevel {
        MINIMAL,
        BALANCED,
        DEEP
    }

    private String id;
    private String branchName;
    private List<String> changedFiles = new ArrayList<>();
    private String strategy; // description of approach
    private String strategyType; // IMPLEMENTATION, ANALYTICAL, CURIOSITY, STABILIZATION, EXPLORATION
    private ReasoningLevel reasoningLevel = ReasoningLevel.BALANCED;
    private boolean architectureEnabled = true;
    private boolean implementationEnabled = true;
    private List<String> selectedFiles = new ArrayList<>();
    private String reasoningFocus;
    private List<Action> actions = new ArrayList<>();
    private ExpectedEffect expectedEffect;
    private Hypothesis hypothesis;
    private double score;
    private boolean success;
    private String errorMessage;
    private MediationCandidate mediationCandidate;

    // Extended Darwin Branch Model fields
    private String branchId;
    private String lineageId;
    private String trajectoryId;
    private String rank; // winner / runner-up / noise
    private ActivationState activationState = ActivationState.CREATED;
    private String semanticAnchor;
    private String mutationPhilosophy;
    private String mutationTrace;
    private String inheritedContext;
    private List<String> rejectedSiblings = new ArrayList<>();

    private List<String> expectedOutputs = new ArrayList<>();

    public List<String> getExpectedOutputs() { return expectedOutputs; }
    public void setExpectedOutputs(List<String> expectedOutputs) { this.expectedOutputs = expectedOutputs; }

    private String survivalArgument;
    private String tradeoffs;
    private String failureRisks;
    private List<String> projectedSteps = new ArrayList<>();

    private double shortTermFitness;
    private double longTermStability;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public String getStrategyType() { return strategyType; }
    public void setStrategyType(String strategyType) { this.strategyType = strategyType; }

    public ReasoningLevel getReasoningLevel() { return reasoningLevel; }
    public void setReasoningLevel(ReasoningLevel reasoningLevel) { this.reasoningLevel = reasoningLevel; }

    public boolean isArchitectureEnabled() { return architectureEnabled; }
    public void setArchitectureEnabled(boolean architectureEnabled) { this.architectureEnabled = architectureEnabled; }

    public boolean isImplementationEnabled() { return implementationEnabled; }
    public void setImplementationEnabled(boolean implementationEnabled) { this.implementationEnabled = implementationEnabled; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public List<String> getChangedFiles() { return changedFiles; }
    public void setChangedFiles(List<String> changedFiles) { this.changedFiles = changedFiles; }

    public List<Action> getActions() { return actions; }
    public void setActions(List<Action> actions) { this.actions = actions; }

    public List<String> getSelectedFiles() { return selectedFiles; }
    public void setSelectedFiles(List<String> selectedFiles) { this.selectedFiles = selectedFiles; }

    public String getReasoningFocus() { return reasoningFocus; }
    public void setReasoningFocus(String reasoningFocus) { this.reasoningFocus = reasoningFocus; }

    public ExpectedEffect getExpectedEffect() { return expectedEffect; }
    public void setExpectedEffect(ExpectedEffect expectedEffect) { this.expectedEffect = expectedEffect; }

    public Hypothesis getHypothesis() { return hypothesis; }
    public void setHypothesis(Hypothesis hypothesis) { this.hypothesis = hypothesis; }

    // Getters and Setters for extended fields
    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }

    public String getLineageId() { return lineageId; }
    public void setLineageId(String lineageId) { this.lineageId = lineageId; }

    public String getRank() { return rank; }
    public void setRank(String rank) { this.rank = rank; }

    public ActivationState getActivationState() { return activationState; }
    public void setActivationState(ActivationState activationState) { this.activationState = activationState; }

    public String getSemanticAnchor() { return semanticAnchor; }
    public void setSemanticAnchor(String semanticAnchor) { this.semanticAnchor = semanticAnchor; }

    public String getMutationPhilosophy() { return mutationPhilosophy; }
    public void setMutationPhilosophy(String mutationPhilosophy) { this.mutationPhilosophy = mutationPhilosophy; }

    public String getMutationTrace() { return mutationTrace; }
    public void setMutationTrace(String mutationTrace) { this.mutationTrace = mutationTrace; }

    public String getInheritedContext() { return inheritedContext; }
    public void setInheritedContext(String inheritedContext) { this.inheritedContext = inheritedContext; }

    public List<String> getRejectedSiblings() { return rejectedSiblings; }
    public void setRejectedSiblings(List<String> rejectedSiblings) { this.rejectedSiblings = rejectedSiblings; }

    public String getSurvivalArgument() { return survivalArgument; }
    public void setSurvivalArgument(String survivalArgument) { this.survivalArgument = survivalArgument; }

    public String getTradeoffs() { return tradeoffs; }
    public void setTradeoffs(String tradeoffs) { this.tradeoffs = tradeoffs; }

    public String getFailureRisks() { return failureRisks; }
    public void setFailureRisks(String failureRisks) { this.failureRisks = failureRisks; }

    public List<String> getProjectedSteps() { return projectedSteps; }
    public void setProjectedSteps(List<String> projectedSteps) { this.projectedSteps = projectedSteps; }

    public double getShortTermFitness() { return shortTermFitness; }
    public void setShortTermFitness(double shortTermFitness) { this.shortTermFitness = shortTermFitness; }

    public double getLongTermStability() { return longTermStability; }
    public void setLongTermStability(double longTermStability) { this.longTermStability = longTermStability; }

    public MediationCandidate getMediationCandidate() { return mediationCandidate; }
    public void setMediationCandidate(MediationCandidate mediationCandidate) { this.mediationCandidate = mediationCandidate; }

    public String getTrajectoryId() { return trajectoryId; }
    public void setTrajectoryId(String trajectoryId) { this.trajectoryId = trajectoryId; }

    public static class Action {
        private String domain;
        private String operation;
        private String target;
        private String description;
        private String implementation;

        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getImplementation() { return implementation; }
        public void setImplementation(String implementation) { this.implementation = implementation; }
    }

    public static class ExpectedEffect {
        private String shortTerm;
        private String longTerm;
        private double risk;
        private double reversibility;

        public String getShortTerm() { return shortTerm; }
        public void setShortTerm(String shortTerm) { this.shortTerm = shortTerm; }
        public String getLongTerm() { return longTerm; }
        public void setLongTerm(String longTerm) { this.longTerm = longTerm; }
        public double getRisk() { return risk; }
        public void setRisk(double risk) { this.risk = risk; }
        public double getReversibility() { return reversibility; }
        public void setReversibility(double reversibility) { this.reversibility = reversibility; }
    }

    public static class Hypothesis {
        private String description;
        private List<String> expectedEffects = new ArrayList<>();
        private double shortTermFitness;
        private double longTermStability;

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getExpectedEffects() { return expectedEffects; }
        public void setExpectedEffects(List<String> expectedEffects) { this.expectedEffects = expectedEffects; }
        public double getShortTermFitness() { return shortTermFitness; }
        public void setShortTermFitness(double shortTermFitness) { this.shortTermFitness = shortTermFitness; }
        public double getLongTermStability() { return longTermStability; }
        public void setLongTermStability(double longTermStability) { this.longTermStability = longTermStability; }
    }
}
