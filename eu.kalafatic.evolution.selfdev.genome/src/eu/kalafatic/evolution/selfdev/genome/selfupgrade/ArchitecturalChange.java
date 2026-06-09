package eu.kalafatic.evolution.selfdev.genome.selfupgrade;

public class ArchitecturalChange {

    private String component;

    private String currentState;

    private String proposedState;

    private String justification;

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public String getProposedState() {
        return proposedState;
    }

    public void setProposedState(String proposedState) {
        this.proposedState = proposedState;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }
}
