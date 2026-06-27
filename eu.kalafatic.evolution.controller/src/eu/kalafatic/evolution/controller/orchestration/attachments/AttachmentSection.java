package eu.kalafatic.evolution.controller.orchestration.attachments;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Represents a section of a Markdown attachment with its intent classification.
 */
public class AttachmentSection {
    private final String header;
    private final String content;
    private final AttachmentCategory category;
    private final int priority;
    private String filename;

    public AttachmentSection(String header, String content, AttachmentCategory category) {
        this.header = header;
        this.content = content;
        this.category = category;
        this.priority = calculatePriority(category);
    }

    private int calculatePriority(AttachmentCategory category) {
        return switch (category) {
            case CONSTRAINTS, RULES -> 10;
            case ARCHITECTURE -> 8;
            case DEBUG_WORKFLOW, ANALYSIS_GUIDE, TESTING_GUIDE -> 7;
            case IMPLEMENTATION_STYLE, PROJECT_CONTEXT -> 5;
            case EXAMPLES, REFERENCE -> 3;
            case GENERAL -> 1;
        };
    }

    public String getHeader() { return header; }
    public String getContent() { return content; }
    public AttachmentCategory getCategory() { return category; }
    public int getPriority() { return priority; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
}
