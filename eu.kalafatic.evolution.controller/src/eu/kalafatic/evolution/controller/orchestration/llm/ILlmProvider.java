package eu.kalafatic.evolution.controller.orchestration.llm;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Interface for LLM providers.
 */
public interface ILlmProvider {
    /**
     * Sends a request to the LLM and returns normalized LlmResponse containing separated content and internal reasoning.
     *
     * @param orchestrator The orchestrator model
     * @param prompt The prompt string
     * @param temperature The temperature setting
     * @param proxyUrl Optional proxy URL
     * @param context The task context
     * @return The normalized LlmResponse
     * @throws Exception If an error occurs
     */
    default LlmResponse sendLlmRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        String raw = sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
        ReasoningProtocol protocol = ReasoningProtocolRegistry.resolve(orchestrator, context);
        return protocol.parse(raw);
    }

    /**
     * Sends a request to the LLM and returns the normalized final answer content.
     *
     * @param orchestrator The orchestrator model
     * @param prompt The prompt string
     * @param temperature The temperature setting
     * @param proxyUrl Optional proxy URL
     * @param context The task context
     * @return The final user-facing content string
     * @throws Exception If an error occurs
     */
    String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception;

    /**
     * Tests the connection to the LLM.
     *
     * @param orchestrator The orchestrator model
     * @param temperature The temperature setting
     * @param proxyUrl Optional proxy URL
     * @param context The task context
     * @return The LLM response or "Success"
     * @throws Exception If an error occurs
     */
    default String testConnection(Orchestrator orchestrator, float temperature, String proxyUrl, TaskContext context) throws Exception {
        return sendRequest(orchestrator, "Ping", temperature, proxyUrl, context);
    }

    default String sendImageRequest(Orchestrator orchestrator, String prompt, String imagePath, TaskContext context) throws Exception {
        throw new UnsupportedOperationException("Multi-modal image analysis not supported by this provider");
    }
}
