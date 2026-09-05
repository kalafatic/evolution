package eu.kalafatic.evolution.controller.orchestration.llm;

import java.util.ArrayList;
import java.util.List;

/**
 * State-machine streaming and batch parser for tag-based reasoning (e.g. <think>, <thinking>, <reasoning>).
 * Handles split chunk tag boundaries, tagless opening reasoning, EOS during thinking, and malformed tags.
 */
public class TagBasedReasoningProtocol implements ReasoningProtocol {

    private static final String[] OPEN_TAGS = { "<think>", "<thinking>", "<reasoning>" };
    private static final String[] CLOSE_TAGS = { "</think>", "</thinking>", "</reasoning>" };

    private final boolean defaultToThinkingIfFirstTagIsClosing;

    public TagBasedReasoningProtocol() {
        this(true);
    }

    public TagBasedReasoningProtocol(boolean defaultToThinkingIfFirstTagIsClosing) {
        this.defaultToThinkingIfFirstTagIsClosing = defaultToThinkingIfFirstTagIsClosing;
    }

    @Override
    public LlmResponse parse(String rawResponse) {
        if (rawResponse == null || rawResponse.isEmpty()) {
            return LlmResponse.empty();
        }

        StreamingReasoningParser parser = createStreamingParser();
        parser.appendChunk(rawResponse);
        return parser.finish();
    }

    @Override
    public LlmResponse parse(String reasoningContent, String mainContent) {
        boolean hasReasoning = reasoningContent != null && !reasoningContent.trim().isEmpty();
        boolean hasContent = mainContent != null && !mainContent.trim().isEmpty();

        if (hasReasoning && hasContent) {
            return LlmResponse.reasoningAndFinal(reasoningContent, mainContent);
        } else if (hasReasoning) {
            return LlmResponse.reasoningOnly(reasoningContent);
        } else if (hasContent) {
            return LlmResponse.finalOnly(mainContent);
        } else {
            return LlmResponse.empty();
        }
    }

    @Override
    public StreamingReasoningParser createStreamingParser() {
        return new TagStreamingParser(defaultToThinkingIfFirstTagIsClosing);
    }

    private static class TagStreamingParser implements StreamingReasoningParser {

        private final boolean allowOpeningClosingTagFallback;
        private ResponseState state = ResponseState.UNKNOWN;
        private final StringBuilder reasoningBuffer = new StringBuilder();
        private final StringBuilder contentBuffer = new StringBuilder();
        private final StringBuilder streamBuffer = new StringBuilder();

        private boolean openedImplicitly = false;
        private boolean encounteredAnyTags = false;

        public TagStreamingParser(boolean allowOpeningClosingTagFallback) {
            this.allowOpeningClosingTagFallback = allowOpeningClosingTagFallback;
        }

        @Override
        public String appendChunk(String chunk) {
            if (chunk == null || chunk.isEmpty()) {
                return "";
            }

            int prevContentLen = contentBuffer.length();
            streamBuffer.append(chunk);
            processStreamBuffer(false);

            if (contentBuffer.length() > prevContentLen) {
                return contentBuffer.substring(prevContentLen);
            }
            return "";
        }

        @Override
        public LlmResponse finish() {
            processStreamBuffer(true);

            String reasoning = reasoningBuffer.toString();
            String content = contentBuffer.toString();

            if (state == ResponseState.THINKING) {
                // Ended while still thinking
                if (content.trim().isEmpty()) {
                    return LlmResponse.reasoningOnly(reasoning);
                } else {
                    return LlmResponse.malformed(reasoning, content, "eos_during_thinking");
                }
            }

            if (reasoning.isEmpty() && content.isEmpty()) {
                return LlmResponse.empty();
            }

            if (!reasoning.isEmpty() && !content.isEmpty()) {
                return LlmResponse.reasoningAndFinal(reasoning, content);
            }

            if (!reasoning.isEmpty() && content.isEmpty()) {
                return LlmResponse.reasoningOnly(reasoning);
            }

            return LlmResponse.finalOnly(content);
        }

        @Override
        public String getReasoning() {
            return reasoningBuffer.toString();
        }

        @Override
        public String getContent() {
            return contentBuffer.toString();
        }

        @Override
        public ResponseState getState() {
            return state;
        }

        private void processStreamBuffer(boolean isEof) {
            while (streamBuffer.length() > 0) {
                if (state == ResponseState.UNKNOWN) {
                    // Look for open tag or close tag
                    TagMatch openMatch = findFirstMatch(streamBuffer.toString(), OPEN_TAGS);
                    TagMatch closeMatch = findFirstMatch(streamBuffer.toString(), CLOSE_TAGS);

                    if (openMatch != null && (closeMatch == null || openMatch.index < closeMatch.index)) {
                        // Found opening tag
                        encounteredAnyTags = true;
                        String prefix = streamBuffer.substring(0, openMatch.index);
                        if (!prefix.isEmpty()) {
                            contentBuffer.append(prefix);
                        }
                        state = ResponseState.THINKING;
                        streamBuffer.delete(0, openMatch.index + openMatch.tag.length());
                    } else if (closeMatch != null && allowOpeningClosingTagFallback) {
                        // Found closing tag without preceding open tag!
                        encounteredAnyTags = true;
                        openedImplicitly = true;
                        String reasoningPrefix = streamBuffer.substring(0, closeMatch.index);
                        reasoningBuffer.append(reasoningPrefix);
                        state = ResponseState.FINAL;
                        streamBuffer.delete(0, closeMatch.index + closeMatch.tag.length());
                    } else {
                        // No tag match found yet
                        if (isEof) {
                            contentBuffer.append(streamBuffer);
                            streamBuffer.setLength(0);
                            state = ResponseState.FINAL;
                        } else {
                            // Check if streamBuffer end might be a partial tag
                            int safeLen = getSafeBufferLength(streamBuffer.toString());
                            if (safeLen > 0) {
                                contentBuffer.append(streamBuffer.substring(0, safeLen));
                                streamBuffer.delete(0, safeLen);
                            } else {
                                break; // Wait for more tokens
                            }
                        }
                    }
                } else if (state == ResponseState.THINKING) {
                    TagMatch closeMatch = findFirstMatch(streamBuffer.toString(), CLOSE_TAGS);
                    if (closeMatch != null) {
                        reasoningBuffer.append(streamBuffer.substring(0, closeMatch.index));
                        state = ResponseState.FINAL;
                        streamBuffer.delete(0, closeMatch.index + closeMatch.tag.length());
                    } else {
                        if (isEof) {
                            reasoningBuffer.append(streamBuffer);
                            streamBuffer.setLength(0);
                        } else {
                            int safeLen = getSafeBufferLength(streamBuffer.toString());
                            if (safeLen > 0) {
                                reasoningBuffer.append(streamBuffer.substring(0, safeLen));
                                streamBuffer.delete(0, safeLen);
                            } else {
                                break; // Wait for more tokens
                            }
                        }
                    }
                } else if (state == ResponseState.FINAL) {
                    // In FINAL state, all remaining text goes to contentBuffer
                    contentBuffer.append(streamBuffer);
                    streamBuffer.setLength(0);
                }
            }
        }

        private TagMatch findFirstMatch(String text, String[] tags) {
            TagMatch best = null;
            for (String tag : tags) {
                int idx = text.indexOf(tag);
                if (idx != -1) {
                    if (best == null || idx < best.index) {
                        best = new TagMatch(idx, tag);
                    }
                }
            }
            return best;
        }

        private int getSafeBufferLength(String text) {
            // Find max potential match length with start of any tag
            int maxTagLen = 0;
            for (String tag : OPEN_TAGS) {
                if (tag.length() > maxTagLen) maxTagLen = tag.length();
            }
            for (String tag : CLOSE_TAGS) {
                if (tag.length() > maxTagLen) maxTagLen = tag.length();
            }

            int textLen = text.length();
            for (int i = 1; i <= Math.min(textLen, maxTagLen - 1); i++) {
                String tail = text.substring(textLen - i);
                if (isPrefixOfAnyTag(tail)) {
                    return textLen - i;
                }
            }
            return textLen;
        }

        private boolean isPrefixOfAnyTag(String str) {
            for (String tag : OPEN_TAGS) {
                if (tag.startsWith(str)) return true;
            }
            for (String tag : CLOSE_TAGS) {
                if (tag.startsWith(str)) return true;
            }
            return false;
        }

        private static class TagMatch {
            final int index;
            final String tag;

            TagMatch(int index, String tag) {
                this.index = index;
                this.tag = tag;
            }
        }
    }
}
