package eu.kalafatic.evolution.controller.orchestration.attachments;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Structured container for categorized attachment sections.
 */
public class StructuredAttachmentContext {
    private final Map<AttachmentCategory, List<AttachmentSection>> sections = new ConcurrentHashMap<>();

    public void addSection(AttachmentSection section) {
        sections.computeIfAbsent(section.getCategory(), k -> new ArrayList<>()).add(section);
    }

    public List<AttachmentSection> getSections(AttachmentCategory category) {
        return sections.getOrDefault(category, new ArrayList<>());
    }

    public Map<AttachmentCategory, List<AttachmentSection>> getAllSections() {
        return sections;
    }

    public boolean isEmpty() {
        return sections.isEmpty();
    }
}
