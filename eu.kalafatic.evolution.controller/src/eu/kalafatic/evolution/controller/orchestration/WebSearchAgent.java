package eu.kalafatic.evolution.controller.orchestration;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.agents.BaseAiAgent;

/**
 * Agent specialized in Web Searching and Documentation.
 */
public class WebSearchAgent extends BaseAiAgent {

    public WebSearchAgent(eu.kalafatic.evolution.controller.orchestration.SessionContainer container) {
        super("Web-Search", "Web-Search", container);
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Web-Search Agent. Your role is to find information on the web and analyze documentation.\n" +
               "Provide clear and concise summaries of your findings to assist other agents.";
    }
}
