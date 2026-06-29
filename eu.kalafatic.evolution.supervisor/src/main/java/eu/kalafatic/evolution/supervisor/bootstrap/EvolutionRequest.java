package eu.kalafatic.evolution.supervisor.bootstrap;

public class EvolutionRequest {
    private final String target;
    private int iterationBudget = 1;
    private String workspacePath;

    public EvolutionRequest(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    public int getIterationBudget() {
        return iterationBudget;
    }

    public void setIterationBudget(int iterationBudget) {
        this.iterationBudget = iterationBudget;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }
}
