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

    public void recalculateCountsAndHash() {
        String fullContent = toFullText();
        this.charCount = fullContent != null ? fullContent.length() : 0;
        if (this.hash == null || this.hash.isEmpty()) {
            this.hash = computeHash(fullContent);
        }
    }

    public String toFullText() {
        if (text != null && !text.isEmpty()) {
            return text;
        }
        if (instruction != null || response != null) {
            StringBuilder sb = new StringBuilder();
            if (instruction != null) sb.append(instruction).append("\n");
            if (response != null) sb.append(response);
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
    public void setText(String text) { this.text = text; }

    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }

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
