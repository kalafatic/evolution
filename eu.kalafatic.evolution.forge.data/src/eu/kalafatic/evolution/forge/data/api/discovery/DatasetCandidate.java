package eu.kalafatic.evolution.forge.data.api.discovery;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model representing a discovered remote dataset candidate evaluated against the current Forge dataset configuration.
 */
public class DatasetCandidate {

    private String id; // Stable identity: provider + ":" + repository + ":" + revision
    private String provider; // e.g. "Hugging Face"
    private String repository; // e.g. "tatsu-lab/alpaca"
    private String revision; // e.g. "main"
    private String description;
    private String url;
    private String author;
    private String license;
    private String task;
    private String domain;
    private String language;
    private String format;
    private long sizeBytes;
    private long estimatedRecords;
    private List<String> splits;
    private List<String> tags;
    private List<String> schema;
    private int compatibilityScore; // 0 - 100
    private List<String> compatibilityReasons;
    private List<String> compatibilityWarnings;
    private long discoveredAt;
    private String status; // "READY", "SMALL", "EXACT_MATCH", "WARNING", "INCOMPATIBLE"

    public DatasetCandidate() {
        this.splits = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.schema = new ArrayList<>();
        this.compatibilityReasons = new ArrayList<>();
        this.compatibilityWarnings = new ArrayList<>();
        this.discoveredAt = System.currentTimeMillis();
        this.status = "READY";
    }

    public DatasetCandidate(String provider, String repository, String revision) {
        this();
        this.provider = provider != null ? provider : "Hugging Face";
        this.repository = repository != null ? repository : "";
        this.revision = revision != null && !revision.trim().isEmpty() ? revision.trim() : "main";
        this.id = createStableId(this.provider, this.repository, this.revision);
        this.url = "https://huggingface.co/datasets/" + this.repository;
    }

    public static String createStableId(String provider, String repository, String revision) {
        String prov = provider != null ? provider.toLowerCase().trim() : "huggingface";
        String repo = repository != null ? repository.trim() : "";
        String rev = revision != null && !revision.trim().isEmpty() ? revision.trim() : "main";
        return prov + ":" + repo + ":" + rev;
    }

    public String getId() { return id != null ? id : createStableId(provider, repository, revision); }
    public void setId(String id) { this.id = id; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getRepository() { return repository; }
    public void setRepository(String repository) {
        this.repository = repository;
        if (this.id == null || this.id.isEmpty()) {
            this.id = createStableId(provider, repository, revision);
        }
    }

    public String getRevision() { return revision; }
    public void setRevision(String revision) { this.revision = revision; }

    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String description) { this.description = description; }

    public String getUrl() { return url != null ? url : ""; }
    public void setUrl(String url) { this.url = url; }

    public String getAuthor() { return author != null ? author : ""; }
    public void setAuthor(String author) { this.author = author; }

    public String getLicense() { return license != null ? license : "unknown"; }
    public void setLicense(String license) { this.license = license; }

    public String getTask() { return task != null ? task : "GENERAL_TEXT"; }
    public void setTask(String task) { this.task = task; }

    public String getDomain() { return domain != null ? domain : "text"; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getLanguage() { return language != null ? language : "en"; }
    public void setLanguage(String language) { this.language = language; }

    public String getFormat() { return format != null ? format : "INSTRUCTION"; }
    public void setFormat(String format) { this.format = format; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public long getEstimatedRecords() { return estimatedRecords; }
    public void setEstimatedRecords(long estimatedRecords) { this.estimatedRecords = estimatedRecords; }

    public List<String> getSplits() { return Collections.unmodifiableList(splits); }
    public void setSplits(List<String> splits) { this.splits = splits != null ? new ArrayList<>(splits) : new ArrayList<>(); }

    public List<String> getTags() { return Collections.unmodifiableList(tags); }
    public void setTags(List<String> tags) { this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>(); }

    public List<String> getSchema() { return Collections.unmodifiableList(schema); }
    public void setSchema(List<String> schema) { this.schema = schema != null ? new ArrayList<>(schema) : new ArrayList<>(); }

    public int getCompatibilityScore() { return compatibilityScore; }
    public void setCompatibilityScore(int compatibilityScore) { this.compatibilityScore = Math.max(0, Math.min(100, compatibilityScore)); }

    public List<String> getCompatibilityReasons() { return Collections.unmodifiableList(compatibilityReasons); }
    public void setCompatibilityReasons(List<String> reasons) { this.compatibilityReasons = reasons != null ? new ArrayList<>(reasons) : new ArrayList<>(); }

    public List<String> getCompatibilityWarnings() { return Collections.unmodifiableList(compatibilityWarnings); }
    public void setCompatibilityWarnings(List<String> warnings) { this.compatibilityWarnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>(); }

    public long getDiscoveredAt() { return discoveredAt; }
    public void setDiscoveredAt(long discoveredAt) { this.discoveredAt = discoveredAt; }

    public String getStatus() { return status != null ? status : "READY"; }
    public void setStatus(String status) { this.status = status; }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        obj.put("id", getId());
        obj.put("provider", getProvider());
        obj.put("repository", getRepository());
        obj.put("revision", getRevision());
        obj.put("description", getDescription());
        obj.put("url", getUrl());
        obj.put("author", getAuthor());
        obj.put("license", getLicense());
        obj.put("task", getTask());
        obj.put("domain", getDomain());
        obj.put("language", getLanguage());
        obj.put("format", getFormat());
        obj.put("sizeBytes", getSizeBytes());
        obj.put("estimatedRecords", getEstimatedRecords());
        obj.put("splits", new JSONArray(splits));
        obj.put("tags", new JSONArray(tags));
        obj.put("schema", new JSONArray(schema));
        obj.put("compatibilityScore", getCompatibilityScore());
        obj.put("compatibilityReasons", new JSONArray(compatibilityReasons));
        obj.put("compatibilityWarnings", new JSONArray(compatibilityWarnings));
        obj.put("discoveredAt", getDiscoveredAt());
        obj.put("status", getStatus());
        return obj;
    }

    public static DatasetCandidate fromJsonObject(JSONObject obj) {
        if (obj == null) return null;
        String provider = obj.optString("provider", "Hugging Face");
        String repository = obj.optString("repository", obj.optString("id", ""));
        String revision = obj.optString("revision", "main");

        DatasetCandidate candidate = new DatasetCandidate(provider, repository, revision);
        candidate.setId(obj.optString("id", createStableId(provider, repository, revision)));
        candidate.setDescription(obj.optString("description", ""));
        candidate.setUrl(obj.optString("url", "https://huggingface.co/datasets/" + repository));
        candidate.setAuthor(obj.optString("author", ""));
        candidate.setLicense(obj.optString("license", "unknown"));
        candidate.setTask(obj.optString("task", "GENERAL_TEXT"));
        candidate.setDomain(obj.optString("domain", "text"));
        candidate.setLanguage(obj.optString("language", "en"));
        candidate.setFormat(obj.optString("format", "INSTRUCTION"));
        candidate.setSizeBytes(obj.optLong("sizeBytes", 0L));
        candidate.setEstimatedRecords(obj.optLong("estimatedRecords", 0L));
        candidate.setCompatibilityScore(obj.optInt("compatibilityScore", 50));
        candidate.setDiscoveredAt(obj.optLong("discoveredAt", System.currentTimeMillis()));
        candidate.setStatus(obj.optString("status", "READY"));

        JSONArray splitsArr = obj.optJSONArray("splits");
        if (splitsArr != null) {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < splitsArr.length(); i++) list.add(splitsArr.getString(i));
            candidate.setSplits(list);
        }

        JSONArray tagsArr = obj.optJSONArray("tags");
        if (tagsArr != null) {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < tagsArr.length(); i++) list.add(tagsArr.getString(i));
            candidate.setTags(list);
        }

        JSONArray schemaArr = obj.optJSONArray("schema");
        if (schemaArr != null) {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < schemaArr.length(); i++) list.add(schemaArr.getString(i));
            candidate.setSchema(list);
        }

        JSONArray reasonsArr = obj.optJSONArray("compatibilityReasons");
        if (reasonsArr != null) {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < reasonsArr.length(); i++) list.add(reasonsArr.getString(i));
            candidate.setCompatibilityReasons(list);
        }

        JSONArray warningsArr = obj.optJSONArray("compatibilityWarnings");
        if (warningsArr != null) {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < warningsArr.length(); i++) list.add(warningsArr.getString(i));
            candidate.setCompatibilityWarnings(list);
        }

        return candidate;
    }
}
