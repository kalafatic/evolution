package eu.kalafatic.evolution.controller.orchestration.util;

/**
 * Utility for filtering system files from user-facing views.
 */
public class FileFilterUtil {

    public static boolean isSystemFile(String path) {
        if (path == null) return true;
        String lowerPath = path.toLowerCase().replace('\\', '/');

        // Explicit folder exclusions
        if (lowerPath.startsWith("orchestrator/") ||
            lowerPath.startsWith("iterations/") ||
            lowerPath.startsWith("data/") ||
            lowerPath.startsWith(".git/") ||
            lowerPath.startsWith(".mvn/") ||
            lowerPath.startsWith(".settings/")) {
            return true;
        }

        // Pattern-based exclusions
        if (lowerPath.contains("trajectory") ||
            lowerPath.contains("checkpoint") ||
            lowerPath.contains("iteration_")) {
            return true;
        }

        // Evolution system report exclusions
        if (lowerPath.endsWith("_context.md") ||
            lowerPath.endsWith("_report.md") ||
            lowerPath.endsWith("_analysis.md") ||
            lowerPath.endsWith("_audit.md") ||
            lowerPath.endsWith("trajectory_map.json") ||
            lowerPath.endsWith("semantic_overview.md")) {
            return true;
        }

        return false;
    }
}
