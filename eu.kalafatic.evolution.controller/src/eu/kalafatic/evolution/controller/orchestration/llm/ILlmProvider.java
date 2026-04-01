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

    /**
     * Tests the connection to the LLM.
     *
     * @param orchestrator The orchestrator model
     * @param temperature The temperature setting
     * @param proxyUrl Optional proxy URL
     * @return The LLM response or "Success"
     * @throws Exception If an error occurs
     */
    default String testConnection(Orchestrator orchestrator, float temperature, String proxyUrl) throws Exception {
        return sendRequest(orchestrator, "Ping", temperature, proxyUrl);
    }
}
