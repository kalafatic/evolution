package eu.kalafatic.evolution.controller.orchestration.llm;

/**
 * Protocol implementation for API providers that already separate reasoning_content and content fields.
 */
public class SeparatedFieldReasoningProtocol implements ReasoningProtocol {

    private final TagBasedReasoningProtocol tagFallback = new TagBasedReasoningProtocol();

    @Override
    public LlmResponse parse(String rawResponse) {
        return tagFallback.parse(rawResponse);
    }

    @Override
    public LlmResponse parse(String reasoningContent, String mainContent) {
        boolean hasReasoning = reasoningContent != null && !reasoningContent.trim().isEmpty();
        boolean hasContent = mainContent != null && !mainContent.trim().isEmpty();

        if (hasReasoning && hasContent) {
            // Also check if mainContent accidentally contains embedded <think> tags
            LlmResponse parsedContent = tagFallback.parse(mainContent);
            if (parsedContent.getKind() == ResponseKind.REASONING_AND_FINAL) {
                String combinedReasoning = reasoningContent + "\n" + parsedContent.getReasoning();
                return LlmResponse.reasoningAndFinal(combinedReasoning.trim(), parsedContent.getContent());
            }
            return LlmResponse.reasoningAndFinal(reasoningContent, mainContent);
        } else if (hasReasoning) {
            return LlmResponse.reasoningOnly(reasoningContent);
        } else if (hasContent) {
            return tagFallback.parse(mainContent);
        } else {
            return LlmResponse.empty();
        }
    }

    @Override
    public StreamingReasoningParser createStreamingParser() {
        return tagFallback.createStreamingParser();
    }
}
