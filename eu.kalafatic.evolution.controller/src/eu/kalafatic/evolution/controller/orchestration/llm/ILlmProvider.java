package eu.kalafatic.evolution.controller.orchestration.llm;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Interface for LLM providers.
 */
public interface ILlmProvider {
    /**
     * Sends a request to the LLM.
     *
     * @param orchestrator The orchestrator model
     * @param prompt The prompt string
     * @param temperature The temperature setting
     * @param proxyUrl Optional proxy URL
     * @return The LLM response
     * @throws Exception If an error occurs
     */
    String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl) throws Exception;
}
