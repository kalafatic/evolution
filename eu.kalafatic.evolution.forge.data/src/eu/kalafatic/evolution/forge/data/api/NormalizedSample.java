package eu.kalafatic.evolution.forge.data.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clean internal representation of a training sample independent of source format.
 */
public class NormalizedSample {

    public static class Message {
        private String role;
        private String content;

        public Message() {}

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    private String id;
    private TrainingSampleType type;
    private String text;
    private String instruction;
    private String response;
    private List<Message> messages;
    private String conversationId;
    private List<NormalizedMessage> conversationMessages;
    private String category;
    private String source;
    private double qualityScore = 1.0;
    private String language = "en";
    private int tokenCount = 0;
    private int charCount = 0;
    private String hash;
    private Map<String, Object> metadata = new HashMap<>();

    public NormalizedSample() {
        this.type = TrainingSampleType.TEXT;
        this.messages = new ArrayList<>();
        this.conversationMessages = new ArrayList<>();
    }

    public NormalizedSample(TrainingSampleType type, String text) {
        this();
        this.type = type != null ? type : TrainingSampleType.TEXT;
        this.text = text;
        recalculateCountsAndHash();
    }

    public static NormalizedSample createTextSample(String text, String source) {
        NormalizedSample sample = new NormalizedSample(TrainingSampleType.TEXT, text);
        sample.setSource(source);
        return sample;
    }

    public static NormalizedSample createInstructionSample(String instruction, String response, String source) {
        NormalizedSample sample = new NormalizedSample();
        sample.setType(TrainingSampleType.INSTRUCTION);
        sample.setInstruction(instruction);
        sample.setResponse(response);
        sample.setSource(source);
        sample.recalculateCountsAndHash();
        return sample;
    }

    public static NormalizedSample createChatSample(List<Message> messages, String source) {
        NormalizedSample sample = new NormalizedSample();
        sample.setType(TrainingSampleType.CHAT);
        if (messages != null) {
            sample.setMessages(new ArrayList<>(messages));
        }
        sample.setSource(source);
        sample.recalculateCountsAndHash();
        return sample;
    }

    public static NormalizedSample createConversationSample(String conversationId, List<NormalizedMessage> messages, String source) {
        NormalizedSample sample = new NormalizedSample();
        sample.setType(TrainingSampleType.CONVERSATION);
        sample.setConversationId(conversationId);
        if (messages != null) {
            sample.setConversationMessages(new ArrayList<>(messages));
        }
        sample.setSource(source);
        sample.recalculateCountsAndHash();
        return sample;
    }

    public void recalculateCountsAndHash() {
        String fullContent = toFullText();
        this.charCount = fullContent != null ? fullContent.length() : 0;
        if (fullContent != null && !fullContent.isEmpty()) {
            String[] words = fullContent.split("\\s+");
            this.tokenCount = Math.max(1, (int) Math.ceil(words.length * 1.3));
        } else {
            this.tokenCount = 0;
        }
        this.hash = computeHash(fullContent);
    }

    public void setText(String text) {
        this.text = text;
        recalculateCountsAndHash();
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
        recalculateCountsAndHash();
    }

    public void setResponse(String response) {
        this.response = response;
        recalculateCountsAndHash();
    }

    public String toFullText() {
        if (type == TrainingSampleType.INSTRUCTION && (instruction != null || response != null)) {
            StringBuilder sb = new StringBuilder();
            sb.append("### Instruction:\n");
            if (instruction != null) sb.append(instruction.trim()).append("\n");
            sb.append("\n### Response:\n");
            if (response != null) sb.append(response.trim());
            return sb.toString().trim();
        }
        if ((type == TrainingSampleType.CONVERSATION || type == TrainingSampleType.CHAT)
                && conversationMessages != null && !conversationMessages.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (NormalizedMessage m : conversationMessages) {
                if (m != null) {
                    String role = m.getRole() != null ? m.getRole() : "user";
                    sb.append("### ").append(role).append(":\n")
                      .append(m.getText() != null ? m.getText().trim() : "")
                      .append("\n\n");
                }
            }
            return sb.toString().trim();
        }
        if (type == TrainingSampleType.CHAT && messages != null && !messages.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Message m : messages) {
                if (m != null) {
                    String role = m.getRole() != null ? m.getRole() : "user";
                    sb.append("### ").append(role).append(":\n")
                      .append(m.getContent() != null ? m.getContent().trim() : "")
                      .append("\n\n");
                }
            }
            return sb.toString().trim();
        }
        if (text != null && !text.isEmpty()) {
            return text;
        }
        if (instruction != null || response != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("### Instruction:\n");
            if (instruction != null) sb.append(instruction.trim()).append("\n");
            sb.append("\n### Response:\n");
            if (response != null) sb.append(response.trim());
            return sb.toString().trim();
        }
        if (conversationMessages != null && !conversationMessages.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (NormalizedMessage m : conversationMessages) {
                if (m != null) {
                    sb.append(m.getRole() != null ? m.getRole() : "user")
                      .append(": ")
                      .append(m.getText() != null ? m.getText() : "")
                      .append("\n");
                }
            }
            return sb.toString().trim();
        }
        if (messages != null && !messages.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Message m : messages) {
                if (m != null) {
                    sb.append(m.getRole() != null ? m.getRole() : "user")
                      .append(": ")
                      .append(m.getContent() != null ? m.getContent() : "")
                      .append("\n");
                }
            }
            return sb.toString().trim();
        }
        return "";
    }

    public String toJsonLine() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"type\":\"").append(type.name()).append("\",");
        sb.append("\"text\":\"").append(escapeJson(toFullText())).append("\",");

        if (conversationId != null) {
            sb.append("\"conversationId\":\"").append(escapeJson(conversationId)).append("\",");
        }
        if (conversationMessages != null && !conversationMessages.isEmpty()) {
            sb.append("\"messages\":[");
            for (int i = 0; i < conversationMessages.size(); i++) {
                NormalizedMessage m = conversationMessages.get(i);
                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"role\":\"").append(escapeJson(m.getRole() != null ? m.getRole() : "")).append("\",");
                sb.append("\"text\":\"").append(escapeJson(m.getText() != null ? m.getText() : "")).append("\",");
                if (m.getMessageId() != null) {
                    sb.append("\"messageId\":\"").append(escapeJson(m.getMessageId())).append("\",");
                } else {
                    sb.append("\"messageId\":null,");
                }
                if (m.getParentMessageId() != null) {
                    sb.append("\"parentMessageId\":\"").append(escapeJson(m.getParentMessageId())).append("\"");
                } else {
                    sb.append("\"parentMessageId\":null");
                }
                sb.append("}");
            }
            sb.append("],");
        }
        if (instruction != null) {
            sb.append("\"instruction\":\"").append(escapeJson(instruction)).append("\",");
        }
        if (response != null) {
            sb.append("\"response\":\"").append(escapeJson(response)).append("\",");
        }
        if (source != null) {
            sb.append("\"source\":\"").append(escapeJson(source)).append("\",");
        }
        sb.append("\"qualityScore\":").append(qualityScore).append(",");
        sb.append("\"tokenCount\":").append(tokenCount).append(",");
        sb.append("\"hash\":\"").append(hash != null ? hash : "").append("\"");
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private static String computeHash(String input) {
        if (input == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public TrainingSampleType getType() { return type; }
    public void setType(TrainingSampleType type) { this.type = type; }

    public String getText() { return text; }

    public String getInstruction() { return instruction; }

    public String getResponse() { return response; }

    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public List<NormalizedMessage> getConversationMessages() { return conversationMessages; }
    public void setConversationMessages(List<NormalizedMessage> conversationMessages) { this.conversationMessages = conversationMessages; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public double getQualityScore() { return qualityScore; }
    public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public int getTokenCount() { return tokenCount; }
    public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }

    public int getCharCount() { return charCount; }
    public void setCharCount(int charCount) { this.charCount = charCount; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
