package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.tools.ShellTool;

/**
 * Agent specialized in Observability and Logs.
 */
public class ObservabilityAgent extends BaseAiAgent {
    public ObservabilityAgent() {
        super("Observability", "Observability");
        addTool(new ShellTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Observability Agent. Your task is to monitor application status and analyze logs.\n" +
               "Use commands like tail, grep, or custom log analysis to provide insights into application behavior.";
    }
}
