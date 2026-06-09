package eu.kalafatic.evolution.supervisor.model;

public class DarwinProposalRequest {
    private String protocolVersion = "1.0.0";
    private String goal;
    private ContextSnapshot context;
    private int iteration;

    public DarwinProposalRequest() {}

    public String getProtocolVersion() { return protocolVersion; }
    public void setProtocolVersion(String protocolVersion) { this.protocolVersion = protocolVersion; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public ContextSnapshot getContext() { return context; }
    public void setContext(ContextSnapshot context) { this.context = context; }

    public int getIteration() { return iteration; }
    public void setIteration(int iteration) { this.iteration = iteration; }
}
