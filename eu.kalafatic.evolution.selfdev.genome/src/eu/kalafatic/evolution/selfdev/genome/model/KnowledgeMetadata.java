package eu.kalafatic.evolution.selfdev.genome.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured metadata for Genome documents.
 */
public class KnowledgeMetadata {
    private String id;
    private String title;
    private String module;
    private String subsystem;
    private String documentType;
    private String summaryLevel;
    private String version;
    private String created;
    private String updated;
    private String milestone;
    private String author;
    private String generatedBy = "EVO Genome Agent";
    private String status = "DRAFT";
    private String importance = "MEDIUM";
    private String stability = "EVOLVING";
    private String audience = "AI / Developer";
    private List<String> relatedModules = new ArrayList<>();
    private List<String> relatedWorkflows = new ArrayList<>();
    private List<String> relatedDocuments = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private List<String> keywords = new ArrayList<>();
    private List<String> technologies = new ArrayList<>();

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getSubsystem() { return subsystem; }
    public void setSubsystem(String subsystem) { this.subsystem = subsystem; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public String getSummaryLevel() { return summaryLevel; }
    public void setSummaryLevel(String summaryLevel) { this.summaryLevel = summaryLevel; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getCreated() { return created; }
    public void setCreated(String created) { this.created = created; }
    public String getUpdated() { return updated; }
    public void setUpdated(String updated) { this.updated = updated; }
    public String getMilestone() { return milestone; }
    public void setMilestone(String milestone) { this.milestone = milestone; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getImportance() { return importance; }
    public void setImportance(String importance) { this.importance = importance; }
    public String getStability() { return stability; }
    public void setStability(String stability) { this.stability = stability; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public List<String> getRelatedModules() { return relatedModules; }
    public void setRelatedModules(List<String> relatedModules) { this.relatedModules = relatedModules; }
    public List<String> getRelatedWorkflows() { return relatedWorkflows; }
    public void setRelatedWorkflows(List<String> relatedWorkflows) { this.relatedWorkflows = relatedWorkflows; }
    public List<String> getRelatedDocuments() { return relatedDocuments; }
    public void setRelatedDocuments(List<String> relatedDocuments) { this.relatedDocuments = relatedDocuments; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    public List<String> getTechnologies() { return technologies; }
    public void setTechnologies(List<String> technologies) { this.technologies = technologies; }

    public String toMarkdownHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        appendIfPresent(sb, "id", id);
        appendIfPresent(sb, "title", title);
        appendIfPresent(sb, "module", module);
        appendIfPresent(sb, "subsystem", subsystem);
        appendIfPresent(sb, "documentType", documentType);
        appendIfPresent(sb, "summaryLevel", summaryLevel);
        appendIfPresent(sb, "version", version);
        appendIfPresent(sb, "created", created);
        appendIfPresent(sb, "updated", updated);
        appendIfPresent(sb, "milestone", milestone);
        appendIfPresent(sb, "author", author);
        appendIfPresent(sb, "generatedBy", generatedBy);
        appendIfPresent(sb, "status", status);
        appendIfPresent(sb, "importance", importance);
        appendIfPresent(sb, "stability", stability);
        appendIfPresent(sb, "audience", audience);
        appendListIfPresent(sb, "relatedModules", relatedModules);
        appendListIfPresent(sb, "relatedWorkflows", relatedWorkflows);
        appendListIfPresent(sb, "relatedDocuments", relatedDocuments);
        appendListIfPresent(sb, "tags", tags);
        appendListIfPresent(sb, "keywords", keywords);
        appendListIfPresent(sb, "technologies", technologies);
        sb.append("---\n\n");
        return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, String key, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append(key).append(": ").append(value).append("\n");
        }
    }

    private void appendListIfPresent(StringBuilder sb, String key, List<String> list) {
        if (list != null && !list.isEmpty()) {
            sb.append(key).append(": [").append(String.join(", ", list)).append("]\n");
        }
    }
}
