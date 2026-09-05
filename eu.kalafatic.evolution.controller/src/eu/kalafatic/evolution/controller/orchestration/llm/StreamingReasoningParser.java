package eu.kalafatic.evolution.controller.orchestration.llm;

/**
 * Interface for streaming reasoning state machine parser.
 */
public interface StreamingReasoningParser {

    /**
     * Consumes a stream chunk and returns any incremental final content produced.
     */
    String appendChunk(String chunk);

    /**
     * Signals end-of-stream (EOS) and completes the response parsing.
     */
    LlmResponse finish();

    /**
     * Gets the current accumulated reasoning content.
     */
    String getReasoning();

    /**
     * Gets the current accumulated final content.
     */
    String getContent();

    /**
     * Gets the current response state.
     */
    ResponseState getState();
}
