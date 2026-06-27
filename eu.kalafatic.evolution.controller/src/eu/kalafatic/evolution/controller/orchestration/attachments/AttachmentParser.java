package eu.kalafatic.evolution.controller.orchestration.attachments;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses Markdown files into categorized sections.
 */
public class AttachmentParser {

    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#+)\\s+(.*)$", Pattern.MULTILINE);

    public static StructuredAttachmentContext parse(String markdown, String filename) {
        StructuredAttachmentContext context = new StructuredAttachmentContext();
        if (markdown == null || markdown.trim().isEmpty()) {
            return context;
        }

        Matcher matcher = HEADER_PATTERN.matcher(markdown);
        int lastPos = 0;
        String lastHeader = "General";

        while (matcher.find()) {
            int currentStart = matcher.start();
            if (currentStart > lastPos) {
                String sectionContent = markdown.substring(lastPos, currentStart).trim();
                if (!sectionContent.isEmpty()) {
                    AttachmentSection section = new AttachmentSection(lastHeader, sectionContent, classify(lastHeader, sectionContent));
                    section.setFilename(filename);
                    context.addSection(section);
                }
            }
            lastHeader = matcher.group(2);
            lastPos = matcher.end();
        }

        if (lastPos < markdown.length()) {
            String sectionContent = markdown.substring(lastPos).trim();
            if (!sectionContent.isEmpty()) {
                AttachmentSection section = new AttachmentSection(lastHeader, sectionContent, classify(lastHeader, sectionContent));
                section.setFilename(filename);
                context.addSection(section);
            }
        }

        return context;
    }

    private static AttachmentCategory classify(String header, String content) {
        String combined = (header + " " + content).toLowerCase();

        if (combined.contains("must") || combined.contains("never") || combined.contains("rules") || combined.contains("guideline") || combined.contains("protocol")) {
            return AttachmentCategory.RULES;
        }
        if (combined.contains("constraint") || combined.contains("limit") || combined.contains("restriction")) {
            return AttachmentCategory.CONSTRAINTS;
        }
        if (combined.contains("debug") || combined.contains("troubleshoot") || combined.contains("error") || combined.contains("stacktrace")) {
            return AttachmentCategory.DEBUG_WORKFLOW;
        }
        if (combined.contains("analysis") || combined.contains("investigation") || combined.contains("heuristic") || combined.contains("diagnosis")) {
            return AttachmentCategory.ANALYSIS_GUIDE;
        }
        if (combined.contains("architecture") || combined.contains("design") || combined.contains("component") || combined.contains("structure") || combined.contains("layer")) {
            return AttachmentCategory.ARCHITECTURE;
        }
        if (combined.contains("test") || combined.contains("verification") || combined.contains("coverage") || combined.contains("junit")) {
            return AttachmentCategory.TESTING_GUIDE;
        }
        if (combined.contains("example") || combined.contains("sample") || combined.contains("output") || combined.contains("case")) {
            return AttachmentCategory.EXAMPLES;
        }
        if (combined.contains("style") || combined.contains("coding") || combined.contains("standard") || combined.contains("naming") || combined.contains("pattern")) {
            return AttachmentCategory.IMPLEMENTATION_STYLE;
        }
        if (combined.contains("context") || combined.contains("project") || combined.contains("overview") || combined.contains("stack")) {
            return AttachmentCategory.PROJECT_CONTEXT;
        }
        if (combined.contains("reference") || combined.contains("doc") || combined.contains("api") || combined.contains("link")) {
            return AttachmentCategory.REFERENCE;
        }

        return AttachmentCategory.GENERAL;
    }
}
