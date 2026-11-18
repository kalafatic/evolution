package eu.kalafatic.evolution.controller.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Represents a single persistent memory entry of the unified cognitive memory
 * model. Extends the legacy user-memory record with scoping (user/application/
 * project/session/task), validity, versioning, provenance and relationships.
 * Serialization remains backward compatible: legacy JSON without the new
 * fields loads with safe defaults.
 */
public class MemoryEntry {
    private String id;
    private String content;
    private MemoryScope scope;
    private MemoryType type;
    private MemorySource source;
    private MemoryImportance importance;
    private double confidence;
    private long createdAt;
    private long updatedAt;
    private String associatedId;
    private Map<String, String> metadata;

    // Unified model extensions
    private String userId;
    private String applicationId;
    private String projectRef;
    private String sessionRef;
    private String taskRef;
    private MemoryValidity validity = MemoryValidity.VALID;
    private MemoryInformationStatus informationStatus = MemoryInformationStatus.CONFIRMED;
    private int version = 1;
    private long lastVerifiedAt;
    private List<MemoryRelationship> relationships = new ArrayList<>();
    private List<String> provenanceChain = new ArrayList<>();

    public MemoryEntry() {
        this.id = UUID.randomUUID().toString();
        this.scope = MemoryScope.USER;
        this.type = MemoryType.FACT;
        this.source = MemorySource.USER;
        this.importance = MemoryImportance.HIGH;
        this.confidence = 1.0;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
        this.metadata = new HashMap<>();
    }

    public MemoryEntry(String content, MemoryScope scope, MemoryType type, MemorySource source, MemoryImportance importance) {
        this();
        this.content = content;
        this.scope = scope != null ? scope : MemoryScope.USER;
        this.type = type != null ? type : MemoryType.FACT;
        this.source = source != null ? source : MemorySource.USER;
        this.importance = importance != null ? importance : MemoryImportance.HIGH;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.updatedAt = System.currentTimeMillis();
    }

    public MemoryScope getScope() {
        return scope;
    }

    public void setScope(MemoryScope scope) {
        this.scope = scope;
        this.updatedAt = System.currentTimeMillis();
    }

    public MemoryType getType() {
        return type;
    }

    public void setType(MemoryType type) {
        this.type = type;
        this.updatedAt = System.currentTimeMillis();
    }

    public MemorySource getSource() {
        return source;
    }

    public void setSource(MemorySource source) {
        this.source = source;
        this.updatedAt = System.currentTimeMillis();
    }

    public MemoryImportance getImportance() {
        return importance;
    }

    public void setImportance(MemoryImportance importance) {
        this.importance = importance;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAssociatedId() {
        return associatedId;
    }

    public void setAssociatedId(String associatedId) {
        this.associatedId = associatedId;
    }

    public Map<String, String> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getProjectRef() {
        return projectRef;
    }

    public void setProjectRef(String projectRef) {
        this.projectRef = projectRef;
    }

    public String getSessionRef() {
        return sessionRef;
    }

    public void setSessionRef(String sessionRef) {
        this.sessionRef = sessionRef;
    }

    public String getTaskRef() {
        return taskRef;
    }

    public void setTaskRef(String taskRef) {
        this.taskRef = taskRef;
    }

    public MemoryValidity getValidity() {
        return validity != null ? validity : MemoryValidity.VALID;
    }

    public void setValidity(MemoryValidity validity) {
        this.validity = validity != null ? validity : MemoryValidity.VALID;
        this.updatedAt = System.currentTimeMillis();
    }

    public MemoryInformationStatus getInformationStatus() {
        if (informationStatus == null) {
            return deriveInformationStatus();
        }
        return informationStatus;
    }

    public void setInformationStatus(MemoryInformationStatus informationStatus) {
        this.informationStatus = informationStatus;
    }

    /**
     * Derives the epistemic status when not explicitly stored: inferred
     * sources become INFERRED, dream/unverified provenance becomes
     * ASSUMPTION, expired temporary metadata becomes TEMPORARY, otherwise
     * CONFIRMED.
     */
    public MemoryInformationStatus deriveInformationStatus() {
        if (source != null && (source == MemorySource.INFERENCE || source == MemorySource.TRAINING)) {
            return MemoryInformationStatus.INFERRED;
        }
        String provenance = getMetadata().get("provenance");
        if (provenance != null && provenance.contains("UNVERIFIED")) {
            return MemoryInformationStatus.ASSUMPTION;
        }
        if (getMetadata().containsKey("temporary") && "true".equalsIgnoreCase(getMetadata().get("temporary"))) {
            return MemoryInformationStatus.TEMPORARY;
        }
        if (validity == MemoryValidity.OUTDATED || validity == MemoryValidity.SUPERSEDED) {
            return MemoryInformationStatus.HISTORICAL;
        }
        return MemoryInformationStatus.CONFIRMED;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public long getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(long lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    /** Freshness in milliseconds: time since last verification (or update). */
    public long getFreshnessMs() {
        long anchor = lastVerifiedAt > 0 ? lastVerifiedAt : updatedAt;
        return Math.max(0, System.currentTimeMillis() - anchor);
    }

    public List<MemoryRelationship> getRelationships() {
        if (relationships == null) {
            relationships = new ArrayList<>();
        }
        return relationships;
    }

    public void setRelationships(List<MemoryRelationship> relationships) {
        this.relationships = relationships != null ? relationships : new ArrayList<>();
    }

    public List<String> getProvenanceChain() {
        if (provenanceChain == null) {
            provenanceChain = new ArrayList<>();
        }
        return provenanceChain;
    }

    public void setProvenanceChain(List<String> provenanceChain) {
        this.provenanceChain = provenanceChain != null ? provenanceChain : new ArrayList<>();
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("content", content != null ? content : "");
        json.put("scope", scope != null ? scope.name() : MemoryScope.USER.name());
        json.put("type", type != null ? type.name() : MemoryType.FACT.name());
        json.put("source", source != null ? source.name() : MemorySource.USER.name());
        json.put("importance", importance != null ? importance.name() : MemoryImportance.HIGH.name());
        json.put("confidence", confidence);
        json.put("createdAt", createdAt);
        json.put("updatedAt", updatedAt);
        if (associatedId != null) {
            json.put("associatedId", associatedId);
        }
        if (metadata != null && !metadata.isEmpty()) {
            json.put("metadata", new JSONObject(metadata));
        }
        // Unified model extensions
        if (userId != null) json.put("userId", userId);
        if (applicationId != null) json.put("applicationId", applicationId);
        if (projectRef != null) json.put("projectRef", projectRef);
        if (sessionRef != null) json.put("sessionRef", sessionRef);
        if (taskRef != null) json.put("taskRef", taskRef);
        json.put("validity", getValidity().name());
        json.put("informationStatus", getInformationStatus().name());
        json.put("version", version);
        json.put("lastVerifiedAt", lastVerifiedAt);
        if (!getRelationships().isEmpty()) {
            JSONArray relArr = new JSONArray();
            for (MemoryRelationship rel : getRelationships()) {
                relArr.put(rel.toJsonObject());
            }
            json.put("relationships", relArr);
        }
        if (!getProvenanceChain().isEmpty()) {
            JSONArray provArr = new JSONArray();
            for (String p : getProvenanceChain()) {
                provArr.put(p);
            }
            json.put("provenanceChain", provArr);
        }
        return json;
    }

    public static MemoryEntry fromJsonObject(JSONObject json) {
        if (json == null) return null;
        MemoryEntry entry = new MemoryEntry();
        if (json.has("id")) entry.setId(json.getString("id"));
        if (json.has("content")) entry.setContent(json.getString("content"));
        if (json.has("scope")) {
            try { entry.setScope(MemoryScope.valueOf(json.getString("scope"))); } catch (Exception ignored) {}
        }
        if (json.has("type")) {
            try { entry.setType(MemoryType.valueOf(json.getString("type"))); } catch (Exception ignored) {}
        }
        if (json.has("source")) {
            try { entry.setSource(MemorySource.valueOf(json.getString("source"))); } catch (Exception ignored) {}
        }
        if (json.has("importance")) {
            try { entry.setImportance(MemoryImportance.valueOf(json.getString("importance"))); } catch (Exception ignored) {}
        }
        if (json.has("confidence")) entry.setConfidence(json.getDouble("confidence"));
        if (json.has("createdAt")) entry.setCreatedAt(json.getLong("createdAt"));
        if (json.has("updatedAt")) entry.setUpdatedAt(json.getLong("updatedAt"));
        if (json.has("associatedId")) entry.setAssociatedId(json.getString("associatedId"));
        if (json.has("metadata")) {
            JSONObject metaObj = json.getJSONObject("metadata");
            Map<String, String> metaMap = new HashMap<>();
            for (Object keyObj : metaObj.keySet()) {
                String key = String.valueOf(keyObj);
                metaMap.put(key, metaObj.getString(key));
            }
            entry.setMetadata(metaMap);
        }
        if (json.has("userId")) entry.setUserId(json.optString("userId", null));
        if (json.has("applicationId")) entry.setApplicationId(json.optString("applicationId", null));
        if (json.has("projectRef")) entry.setProjectRef(json.optString("projectRef", null));
        if (json.has("sessionRef")) entry.setSessionRef(json.optString("sessionRef", null));
        if (json.has("taskRef")) entry.setTaskRef(json.optString("taskRef", null));
        if (json.has("validity")) entry.setValidity(MemoryValidity.fromString(json.getString("validity")));
        if (json.has("informationStatus")) {
            entry.setInformationStatus(MemoryInformationStatus.fromString(json.getString("informationStatus")));
        }
        if (json.has("version")) entry.setVersion(json.getInt("version"));
        if (json.has("lastVerifiedAt")) entry.setLastVerifiedAt(json.getLong("lastVerifiedAt"));
        if (json.has("relationships")) {
            List<MemoryRelationship> rels = new ArrayList<>();
            JSONArray relArr = json.getJSONArray("relationships");
            for (int i = 0; i < relArr.length(); i++) {
                MemoryRelationship rel = MemoryRelationship.fromJsonObject(relArr.optJSONObject(i));
                if (rel != null) rels.add(rel);
            }
            entry.setRelationships(rels);
        }
        if (json.has("provenanceChain")) {
            List<String> chain = new ArrayList<>();
            JSONArray provArr = json.getJSONArray("provenanceChain");
            for (int i = 0; i < provArr.length(); i++) {
                chain.add(provArr.getString(i));
            }
            entry.setProvenanceChain(chain);
        }
        return entry;
    }
}
