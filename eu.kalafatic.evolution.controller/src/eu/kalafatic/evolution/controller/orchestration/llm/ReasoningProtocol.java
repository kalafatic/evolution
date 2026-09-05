package eu.kalafatic.evolution.controller.orchestration.llm;

/**
 * Provider and model independent reasoning protocol interface.
 */
public interface ReasoningProtocol {

    /**
     * Parses a complete raw response string into a normalized LlmResponse.
     */
    LlmResponse parse(String rawResponse);

    /**
     * Maps already separated provider fields into a normalized LlmResponse.
     */
    LlmResponse parse(String reasoningContent, String mainContent);

    /**
     * Creates a streaming parser instance for processing stream chunks.
     */
    StreamingReasoningParser createStreamingParser();
}
