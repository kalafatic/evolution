package eu.kalafatic.evolution.controller.orchestration;

/**
 * Agent specialized in Web Searching and Documentation.
 */
public class WebSearchAgent extends BaseAiAgent {
    public WebSearchAgent() {
        super("Web-Search", "Web-Search");
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Web-Search Agent. Your role is to find information on the web and analyze documentation.\n" +
               "Provide clear and concise summaries of your findings to assist other agents.";
    }
}
