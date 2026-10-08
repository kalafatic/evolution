package eu.kalafatic.evolution.controller.memory;

import java.util.HashSet;
import java.util.Set;

/**
 * Filter query specification for retrieving memory entries.
 */
public class MemoryQuery {
    private MemoryScope scope;
    private MemoryType type;
    private MemorySource source;
    private MemoryImportance minImportance;
    private double minConfidence = 0.0;
    private String searchText;
    private String associatedId;
    private int limit = 50;

    public MemoryScope getScope() {
        return scope;
    }

    public MemoryQuery setScope(MemoryScope scope) {
        this.scope = scope;
        return this;
    }

    public MemoryType getType() {
        return type;
    }

    public MemoryQuery setType(MemoryType type) {
        this.type = type;
        return this;
    }

    public MemorySource getSource() {
        return source;
    }

    public MemoryQuery setSource(MemorySource source) {
        this.source = source;
        return this;
    }

    public MemoryImportance getMinImportance() {
        return minImportance;
    }

    public MemoryQuery setMinImportance(MemoryImportance minImportance) {
        this.minImportance = minImportance;
        return this;
    }

    public double getMinConfidence() {
        return minConfidence;
    }

    public MemoryQuery setMinConfidence(double minConfidence) {
        this.minConfidence = minConfidence;
        return this;
    }

    public String getSearchText() {
        return searchText;
    }

    public MemoryQuery setSearchText(String searchText) {
        this.searchText = searchText;
        return this;
    }

    public String getAssociatedId() {
        return associatedId;
    }

    public MemoryQuery setAssociatedId(String associatedId) {
        this.associatedId = associatedId;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public MemoryQuery setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public boolean matches(MemoryEntry entry) {
        if (entry == null) return false;
        if (scope != null && entry.getScope() != scope) return false;
        if (type != null && entry.getType() != type) return false;
        if (source != null && entry.getSource() != source) return false;
        if (associatedId != null && !associatedId.equalsIgnoreCase(entry.getAssociatedId())) return false;
        if (entry.getConfidence() < minConfidence) return false;
        if (minImportance != null && entry.getImportance() != null) {
            if (entry.getImportance().getLevel() < minImportance.getLevel()) return false;
        }
        if (searchText != null && !searchText.trim().isEmpty()) {
            String lowerContent = entry.getContent() != null ? entry.getContent().toLowerCase() : "";
            String[] tokens = searchText.toLowerCase().split("\\s+");
            boolean found = false;
            for (String token : tokens) {
                if (token.length() > 2 && lowerContent.contains(token)) {
                    found = true;
                    break;
                }
            }
            if (!found && !lowerContent.contains(searchText.toLowerCase())) {
                return false;
            }
        }
        return true;
    }
}
