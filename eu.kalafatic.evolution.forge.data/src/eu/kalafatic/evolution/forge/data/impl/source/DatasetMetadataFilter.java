package eu.kalafatic.evolution.forge.data.impl.source;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Intelligent utility for filtering out repository, environment, and dataset metadata files.
 * Ensures infrastructure files (README, .gitignore, CACHEDIR.TAG, dataset_info.json) are excluded
 * while preserving legitimate data files (train.json, data.json, samples.json, parquet, jsonl, etc.).
 */
public final class DatasetMetadataFilter {

    private DatasetMetadataFilter() {}

    public static boolean isMetadataFile(File file) {
        if (file == null) return true;
        return isMetadataFile(file.toPath());
    }

    public static boolean isMetadataFile(Path path) {
        if (path == null) return true;
        String pathStr = path.toString().replace('\\', '/').toLowerCase(Locale.ROOT);

        // 1. Directory patterns to ignore
        if (pathStr.contains("/.git/") || pathStr.contains("/target/") ||
            pathStr.contains("/node_modules/") || pathStr.contains("/bin/") ||
            pathStr.contains("/.settings/") || pathStr.contains("/.metadata/")) {
            return true;
        }

        String fileName = path.getFileName() != null ? path.getFileName().toString().toLowerCase(Locale.ROOT) : "";

        // 2. Specific metadata/infrastructure files
        if (fileName.equals(".gitignore") || fileName.equals(".gitattributes") ||
            fileName.equals("cachedir.tag") || fileName.equals(".ds_store") ||
            fileName.equals("dataset_info.json") || fileName.equals("state.json") ||
            fileName.equals(".project") || fileName.equals(".classpath")) {
            return true;
        }

        // 3. Extensions for metadata
        if (fileName.endsWith(".metadata") || fileName.endsWith(".git") || fileName.endsWith(".lock")) {
            return true;
        }

        // 4. README, LICENSE, NOTICE, CHANGELOG files (e.g., README, README.md, README.rst, LICENSE.txt)
        if (isDocumentationFile(fileName)) {
            return true;
        }

        return false;
    }

    private static boolean isDocumentationFile(String fileName) {
        if (fileName.equals("readme") || fileName.startsWith("readme.") || fileName.startsWith("readme_")) {
            return true;
        }
        if (fileName.equals("license") || fileName.startsWith("license.") || fileName.startsWith("license_")) {
            return true;
        }
        if (fileName.equals("notice") || fileName.startsWith("notice.") || fileName.startsWith("notice_")) {
            return true;
        }
        if (fileName.equals("changelog") || fileName.startsWith("changelog.") || fileName.startsWith("changelog_")) {
            return true;
        }
        return false;
    }
}
