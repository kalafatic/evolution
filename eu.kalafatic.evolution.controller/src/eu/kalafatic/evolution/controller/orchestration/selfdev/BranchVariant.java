package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;

public class BranchVariant {
    private String id;
    private String branchName;
    private List<String> changedFiles = new ArrayList<>();
    private String strategy; // description of approach
    private List<Action> actions = new ArrayList<>();
    private ExpectedEffect expectedEffect;
    private Hypothesis hypothesis;
    private double score;
    private boolean success;
    private String errorMessage;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

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

    public ExpectedEffect getExpectedEffect() { return expectedEffect; }
    public void setExpectedEffect(ExpectedEffect expectedEffect) { this.expectedEffect = expectedEffect; }

    public Hypothesis getHypothesis() { return hypothesis; }
    public void setHypothesis(Hypothesis hypothesis) { this.hypothesis = hypothesis; }

    public static class Action {
        private String domain;
        private String operation;
        private String target;
        private String description;

        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
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

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getExpectedEffects() { return expectedEffects; }
        public void setExpectedEffects(List<String> expectedEffects) { this.expectedEffects = expectedEffects; }
    }
}
