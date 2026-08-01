package eu.kalafatic.evolution.forge.controller.service.impl.agents;

import java.util.ArrayList;
import java.util.List;

public class ConsistencyAgent {

    public static class ConsistencyViolation {
        private final String category;
        private final String description;
        private final String location1;
        private final String location2;

        public ConsistencyViolation(String category, String description, String location1, String location2) {
            this.category = category;
            this.description = description;
            this.location1 = location1;
            this.location2 = location2;
        }

        public String getCategory() {
            return category;
        }

        public String getDescription() {
            return description;
        }

        public String getLocation1() {
            return location1;
        }

        public String getLocation2() {
            return location2;
        }

        @Override
        public String toString() {
            return "[" + category + "] " + description + " (Ref 1: " + location1 + ", Ref 2: " + location2 + ")";
        }
    }

    public List<ConsistencyViolation> checkConsistency(List<KnowledgeUnit> units) {
        List<ConsistencyViolation> violations = new ArrayList<>();

        // Scan for potential documentation-implementation drift
        KnowledgeUnit docsUnit = null;
        KnowledgeUnit codeUnit = null;

        for (KnowledgeUnit unit : units) {
            if ("MARKDOWN".equals(unit.getFileType())) {
                docsUnit = unit;
            } else if ("JAVA".equals(unit.getFileType())) {
                codeUnit = unit;
            }
        }

        if (docsUnit != null && codeUnit != null) {
            String docsContent = docsUnit.getContent().toLowerCase();
            String codeContent = codeUnit.getContent().toLowerCase();

            // Simple heuristic check: if code defines a class/interface that documentation describes differently
            // e.g., check for component name consistency
            if (codeContent.contains("class iterationmanager") && !docsContent.contains("iterationmanager")) {
                violations.add(new ConsistencyViolation(
                    "DOCUMENTATION_DRIFT",
                    "The codebase implements IterationManager but it is not documented in markdown files.",
                    codeUnit.getRelativePath(),
                    docsUnit.getRelativePath()
                ));
            }

            // Conflict check: check if code defines obsolete properties or names
            if (docsContent.contains("obsolete") && codeContent.contains("obsolete")) {
                violations.add(new ConsistencyViolation(
                    "OBSOLETE_DOCUMENTATION",
                    "Obsolete component references found in documentation matching legacy code elements.",
                    docsUnit.getRelativePath(),
                    codeUnit.getRelativePath()
                ));
            }
        }

        return violations;
    }
}
