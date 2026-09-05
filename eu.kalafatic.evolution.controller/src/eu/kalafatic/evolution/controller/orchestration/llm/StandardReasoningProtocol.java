package eu.kalafatic.evolution.controller.orchestration.llm;

/**
 * Protocol implementation for standard non-reasoning models.
 * Treats all generated text as final user-facing content.
 */
public class StandardReasoningProtocol implements ReasoningProtocol {

    @Override
    public LlmResponse parse(String rawResponse) {
        if (rawResponse == null || rawResponse.isEmpty()) {
            return LlmResponse.empty();
        }
        return LlmResponse.finalOnly(rawResponse);
    }

    @Override
    public LlmResponse parse(String reasoningContent, String mainContent) {
        if (mainContent != null && !mainContent.isEmpty()) {
            return LlmResponse.finalOnly(mainContent);
        }
        if (reasoningContent != null && !reasoningContent.isEmpty()) {
            return LlmResponse.finalOnly(reasoningContent);
        }
        return LlmResponse.empty();
    }

    @Override
    public StreamingReasoningParser createStreamingParser() {
        return new StandardStreamingParser();
    }

    private static class StandardStreamingParser implements StreamingReasoningParser {
        private final StringBuilder contentBuffer = new StringBuilder();

        @Override
        public String appendChunk(String chunk) {
            if (chunk != null && !chunk.isEmpty()) {
                contentBuffer.append(chunk);
                return chunk;
            }
            return "";
        }

        @Override
        public LlmResponse finish() {
            String content = contentBuffer.toString();
            if (content.isEmpty()) {
                return LlmResponse.empty();
            }
            return LlmResponse.finalOnly(content);
        }

        @Override
        public String getReasoning() {
            return "";
        }

        @Override
        public String getContent() {
            return contentBuffer.toString();
        }

        @Override
        public ResponseState getState() {
            return ResponseState.FINAL;
        }
    }
}
