package eu.kalafatic.evolution.controller.orchestration.llm;

import java.util.Objects;

/**
 * Represents a normalized LLM response separating internal reasoning/thinking
 * from the user-facing content/final answer.
 */
public final class LlmResponse {

    private final String content;
    private final String reasoning;
    private final ResponseKind kind;
    private final boolean complete;
    private final String finishReason;

    public LlmResponse(String content, String reasoning, ResponseKind kind, boolean complete, String finishReason) {
        this.content = content != null ? content : "";
        this.reasoning = reasoning != null ? reasoning : "";
        this.kind = kind != null ? kind : ResponseKind.EMPTY;
        this.complete = complete;
        this.finishReason = finishReason != null ? finishReason : "";
    }

    public String getContent() {
        return content;
    }

    public String getReasoning() {
        return reasoning;
    }

    public ResponseKind getKind() {
        return kind;
    }

    public boolean isComplete() {
        return complete;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public static LlmResponse finalOnly(String content) {
        if (content == null || content.isEmpty()) {
            return new LlmResponse("", "", ResponseKind.EMPTY, true, "stop");
        }
        return new LlmResponse(content, "", ResponseKind.FINAL_ONLY, true, "stop");
    }

    public static LlmResponse reasoningAndFinal(String reasoning, String content) {
        return new LlmResponse(content, reasoning, ResponseKind.REASONING_AND_FINAL, true, "stop");
    }

    public static LlmResponse reasoningOnly(String reasoning) {
        return new LlmResponse("", reasoning, ResponseKind.REASONING_ONLY, false, "eos_during_thinking");
    }

    public static LlmResponse empty() {
        return new LlmResponse("", "", ResponseKind.EMPTY, true, "stop");
    }

    public static LlmResponse malformed(String reasoning, String content, String reason) {
        return new LlmResponse(content, reasoning, ResponseKind.MALFORMED, false, reason);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LlmResponse that = (LlmResponse) o;
        return complete == that.complete &&
                Objects.equals(content, that.content) &&
                Objects.equals(reasoning, that.reasoning) &&
                kind == that.kind &&
                Objects.equals(finishReason, that.finishReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, reasoning, kind, complete, finishReason);
    }

    @Override
    public String toString() {
        return "LlmResponse{" +
                "kind=" + kind +
                ", reasoningLength=" + reasoning.length() +
                ", contentLength=" + content.length() +
                ", complete=" + complete +
                ", finishReason='" + finishReason + '\'' +
                '}';
    }
}
