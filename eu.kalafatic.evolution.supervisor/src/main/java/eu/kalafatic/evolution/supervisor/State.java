package eu.kalafatic.evolution.supervisor;

import java.util.List;

public class State {
    private boolean active;
    private int iteration;
    private String goal;
    private String mode;
    private List<String> plan;
    private String contextPath;

    public State() {}

    public State(boolean active, int iteration) {
        this.active = active;
        this.iteration = iteration;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getIteration() { return iteration; }
    public void setIteration(int iteration) { this.iteration = iteration; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public List<String> getPlan() { return plan; }
    public void setPlan(List<String> plan) { this.plan = plan; }

    public String getContextPath() { return contextPath; }
    public void setContextPath(String contextPath) { this.contextPath = contextPath; }

    @Override
    public String toString() {
        return "State{active=" + active + ", iteration=" + iteration + ", goal=" + goal + ", mode=" + mode + "}";
    }
}
