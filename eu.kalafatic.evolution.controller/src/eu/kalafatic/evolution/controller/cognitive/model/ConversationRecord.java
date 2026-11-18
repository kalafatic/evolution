package eu.kalafatic.evolution.controller.cognitive.model;

import org.json.JSONObject;

/**
 * One structured record of a conversation: message, tool call, tool result,
 * decision or system event. Append-only durable history.
 */
public class ConversationRecord {
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ASSISTANT = "ASSISTANT";
    public static final String ROLE_SYSTEM = "SYSTEM";
    public static final String ROLE_TOOL_CALL = "TOOL_CALL";
    public static final String ROLE_TOOL_RESULT = "TOOL_RESULT";
    public static final String ROLE_DECISION = "DECISION";
    public static final String ROLE_EVENT = "EVENT";
    public static final String ROLE_ERROR = "ERROR";

    private long id;
    private String conversationId;
    private long sequence;
    private String role;
    private String agentType;
    private String toolName;
    private String content;
    private int tokens;
    private long timestamp;
    private String metadata;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public long getSequence() { return sequence; }
    public void setSequence(long sequence) { this.sequence = sequence; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAgentType() { return agentType; }
    public void setAgentType(String agentType) { this.agentType = agentType; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getTokens() { return tokens; }
    public void setTokens(int tokens) { this.tokens = tokens; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("conversationId", conversationId);
        json.put("sequence", sequence);
        json.put("role", role);
        if (agentType != null) json.put("agentType", agentType);
        if (toolName != null) json.put("toolName", toolName);
        json.put("content", content != null ? content : "");
        json.put("tokens", tokens);
        json.put("timestamp", timestamp);
        if (metadata != null) json.put("metadata", metadata);
        return json;
    }

    public static ConversationRecord fromJsonObject(JSONObject json) {
        if (json == null) return null;
        ConversationRecord r = new ConversationRecord();
        if (json.has("id")) r.setId(json.getLong("id"));
        if (json.has("conversationId")) r.setConversationId(json.getString("conversationId"));
        if (json.has("sequence")) r.setSequence(json.getLong("sequence"));
        if (json.has("role")) r.setRole(json.getString("role"));
        if (json.has("agentType")) r.setAgentType(json.getString("agentType"));
        if (json.has("toolName")) r.setToolName(json.optString("toolName", null));
        if (json.has("content")) r.setContent(json.getString("content"));
        if (json.has("tokens")) r.setTokens(json.getInt("tokens"));
        if (json.has("timestamp")) r.setTimestamp(json.getLong("timestamp"));
        if (json.has("metadata")) r.setMetadata(json.optString("metadata", null));
        return r;
    }
}
