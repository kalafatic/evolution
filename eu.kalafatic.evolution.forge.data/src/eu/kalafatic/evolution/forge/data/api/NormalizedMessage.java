package eu.kalafatic.evolution.forge.data.api;

import java.util.Objects;

/**
 * Native EVO conversation message representation.
 */
public class NormalizedMessage {

    private String role;
    private String text;
    private String messageId;
    private String parentMessageId;

    public NormalizedMessage() {}

    public NormalizedMessage(String role, String text) {
        this.role = normalizeRole(role);
        this.text = text;
    }

    public NormalizedMessage(String role, String text, String messageId, String parentMessageId) {
        this.role = normalizeRole(role);
        this.text = text;
        this.messageId = messageId;
        this.parentMessageId = parentMessageId;
    }

    public static String normalizeRole(String rawRole) {
        if (rawRole == null) {
            return null;
        }
        String r = rawRole.trim().toLowerCase();
        switch (r) {
            case "prompter":
            case "human":
            case "user":
                return "user";
            case "assistant":
            case "gpt":
            case "bot":
                return "assistant";
            case "system":
                return "system";
            case "tool":
                return "tool";
            default:
                return r;
        }
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = normalizeRole(role);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getParentMessageId() {
        return parentMessageId;
    }

    public void setParentMessageId(String parentMessageId) {
        this.parentMessageId = parentMessageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NormalizedMessage message = (NormalizedMessage) o;
        return Objects.equals(role, message.role) &&
               Objects.equals(text, message.text) &&
               Objects.equals(messageId, message.messageId) &&
               Objects.equals(parentMessageId, message.parentMessageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, text, messageId, parentMessageId);
    }

    @Override
    public String toString() {
        return "NormalizedMessage{" +
                "role='" + role + '\'' +
                ", text='" + text + '\'' +
                ", messageId='" + messageId + '\'' +
                ", parentMessageId='" + parentMessageId + '\'' +
                '}';
    }
}
