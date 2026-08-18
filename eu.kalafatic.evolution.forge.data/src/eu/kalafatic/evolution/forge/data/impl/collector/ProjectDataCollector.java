package eu.kalafatic.evolution.forge.data.impl.collector;

import eu.kalafatic.evolution.forge.data.api.collector.CollectionContext;
import eu.kalafatic.evolution.forge.data.api.collector.CollectionResult;
import eu.kalafatic.evolution.forge.data.api.collector.CollectorType;
import eu.kalafatic.evolution.forge.data.api.collector.SourceType;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataCollector;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataItem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collects and normalizes knowledge directly from the current project source tree.
 */
public class ProjectDataCollector extends TrainingDataCollector {

    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".java", ".xml", ".md", ".json", ".yaml", ".yml", ".properties", ".html", ".js", ".ts"
    ));

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*import\\s+([a-zA-Z0-9_.]+)\\s*;");
    private static final Pattern CLASS_PATTERN = Pattern.compile("\\b(class|interface|enum|record)\\s+([A-Z][a-zA-Z0-9_]*)");

    @Override
    public CollectorType getType() {
        return CollectorType.PROJECT;
    }

    @Override
    public String getName() {
        return "Project Data Collector";
    }

    @Override
    public CollectionResult collect(CollectionContext context) {
        long startTime = System.currentTimeMillis();
        File projectDir = context.getProjectDirectory();

        if (projectDir == null || !projectDir.exists() || !projectDir.isDirectory()) {
            return CollectionResult.failure(getType(), "Invalid or non-existent project directory", System.currentTimeMillis() - startTime);
        }

        List<TrainingDataItem> items = new ArrayList<>();
        List<String> exclusions = context.getExclusions();

        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            List<Path> files = walk
                    .filter(Files::isRegularFile)
                    .filter(path -> isNotExcluded(path, projectDir.toPath(), exclusions))
                    .filter(path -> isSupportedFile(path))
                    .filter(path -> isUnderSizeLimit(path, context.getMaxFileSize()))
                    .sorted()
                    .collect(Collectors.toList());

            for (Path path : files) {
                try {
                    String content = Files.readString(path);
                    String relativePath = projectDir.toPath().relativize(path).toString().replace('\\', '/');
                    String fileName = path.getFileName().toString();

                    TrainingDataItem item = new TrainingDataItem();
                    item.setSourceType(SourceType.PROJECT);
                    item.setSource(relativePath);
                    item.setTitle(fileName);
                    item.setContent(content);

                    String lang = extensionToLanguage(fileName);
                    item.addMetadata("language", lang);
                    item.addMetadata("fileSize", Files.size(path));

                    if ("java".equals(lang)) {
                        analyzeJavaMetadata(content, item);
                    }

                    items.add(item);
                } catch (Exception e) {
                    // Skip unreadable files gracefully
                }
            }

            return CollectionResult.success(getType(), items, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            return CollectionResult.failure(getType(), "Failed to traverse project tree: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }

    private boolean isNotExcluded(Path path, Path rootPath, List<String> exclusions) {
        String relPath = rootPath.relativize(path).toString().replace('\\', '/');
        if (relPath.contains("/.git/") || relPath.startsWith(".git/") ||
            relPath.contains("/target/") || relPath.startsWith("target/") ||
            relPath.contains("/node_modules/") || relPath.startsWith("node_modules/")) {
            return false;
        }
        if (exclusions != null) {
            for (String exclusion : exclusions) {
                if (relPath.contains(exclusion)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isSupportedFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (name.endsWith(ext) || name.equals("pom.xml") || name.equals("manifest.mf")) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnderSizeLimit(Path path, long maxFileSize) {
        try {
            return maxFileSize <= 0 || Files.size(path) <= maxFileSize;
        } catch (Exception e) {
            return false;
        }
    }

    private String extensionToLanguage(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".xml") || lower.equals("pom.xml")) return "xml";
        if (lower.endsWith(".md")) return "markdown";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) return "yaml";
        if (lower.endsWith(".properties") || lower.equals("manifest.mf")) return "properties";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "html";
        if (lower.endsWith(".js")) return "javascript";
        if (lower.endsWith(".ts")) return "typescript";
        return "text";
    }

    private void analyzeJavaMetadata(String content, TrainingDataItem item) {
        String[] lines = content.split("\\r?\\n");
        List<String> imports = new ArrayList<>();
        List<String> symbols = new ArrayList<>();

        for (String line : lines) {
            Matcher pkgMatcher = PACKAGE_PATTERN.matcher(line);
            if (pkgMatcher.find()) {
                item.addMetadata("package", pkgMatcher.group(1));
            }

            Matcher impMatcher = IMPORT_PATTERN.matcher(line);
            if (impMatcher.find()) {
                imports.add(impMatcher.group(1));
            }

            Matcher classMatcher = CLASS_PATTERN.matcher(line);
            if (classMatcher.find()) {
                symbols.add(classMatcher.group(2));
            }
        }

        if (!symbols.isEmpty()) {
            item.addMetadata("symbol", symbols.get(0));
            item.addMetadata("allSymbols", symbols);
        }
        if (!imports.isEmpty()) {
            item.addMetadata("imports", imports);
        }
    }
}
