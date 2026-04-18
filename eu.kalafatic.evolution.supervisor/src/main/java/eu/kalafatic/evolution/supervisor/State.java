package eu.kalafatic.evolution.supervisor;

public class State {
    private boolean active;
    private int iteration;

    public State() {}

    public State(boolean active, int iteration) {
        this.active = active;
        this.iteration = iteration;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getIteration() { return iteration; }
    public void setIteration(int iteration) { this.iteration = iteration; }

    @Override
    public String toString() {
        return "State{active=" + active + ", iteration=" + iteration + "}";
    }
}
