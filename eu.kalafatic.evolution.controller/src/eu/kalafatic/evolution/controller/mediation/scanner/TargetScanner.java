package eu.kalafatic.evolution.controller.mediation.scanner;

import java.io.File;
import java.util.List;
import java.util.Set;

import eu.kalafatic.evolution.controller.mediation.model.FileDescriptor;
import eu.kalafatic.evolution.controller.mediation.model.SemanticNode;
import eu.kalafatic.evolution.controller.mediation.model.TargetDescriptor;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;

/**
 * Recursively scans a target directory and identifies technologies and files.
 */
public class TargetScanner {
    private static final Set<String> IGNORE_DIRS = Set.of(".git", "node_modules", "target", "build", ".settings", ".metadata");

    public TargetDescriptor scan(File root) {
        TargetDescriptor target = new TargetDescriptor(root.getAbsolutePath());
        scanRecursive(root, root, target);
        detectTechnologies(target);
        return target;
    }

    public TargetSnapshot scanToSnapshot(File root, TargetSnapshot.TargetType type) {
        String id = "snapshot-" + System.currentTimeMillis();
        TargetSnapshot snapshot = new TargetSnapshot(id, "1.0", type, root.getAbsolutePath());
        eu.kalafatic.evolution.controller.log.Log.log("[TARGET_SCANNER] Starting scan of: " + root.getAbsolutePath());
        scanRecursiveToSnapshot(root, root, snapshot);
        detectTechnologiesInSnapshot(snapshot);
        eu.kalafatic.evolution.controller.log.Log.log("[TARGET_SCANNER] Scan complete. Total nodes: " + snapshot.getNodes().size());
        return snapshot;
    }

    private void scanRecursive(File current, File root, TargetDescriptor target) {
        if (current.isDirectory()) {
            // Robust check: normalize name and ensure root is never ignored even if named like an ignored dir
            String dirName = current.getName();
            if (!current.equals(root) && IGNORE_DIRS.contains(dirName)) return;

            File[] children = current.listFiles();
            if (children != null) {
                for (File child : children) scanRecursive(child, root, target);
            }
        } else {
            try {
                // Ensure cross-platform relative paths with forward slashes
                String relativePath = root.toPath().toAbsolutePath().relativize(current.toPath().toAbsolutePath()).toString().replace('\\', '/');
                if (relativePath.isEmpty()) relativePath = current.getName();
                String extension = getExtension(current.getName());
                target.getFiles().add(new FileDescriptor(relativePath, extension, current.length()));
            } catch (Exception e) {
                // Fallback for edge cases where relativize fails
                String path = current.getAbsolutePath().replace('\\', '/');
                String rootPath = root.getAbsolutePath().replace('\\', '/');
                if (path.startsWith(rootPath)) {
                    String rel = path.substring(rootPath.length());
                    if (rel.startsWith("/")) rel = rel.substring(1);
                    target.getFiles().add(new FileDescriptor(rel, getExtension(current.getName()), current.length()));
                }
            }
        }
    }

    private void scanRecursiveToSnapshot(File current, File root, TargetSnapshot snapshot) {
        if (current.isDirectory()) {
            String dirName = current.getName();
            if (!current.equals(root) && IGNORE_DIRS.contains(dirName)) return;

            File[] children = current.listFiles();
            if (children != null) {
                for (File child : children) scanRecursiveToSnapshot(child, root, snapshot);
            }
        } else {
            try {
                String relativePath = root.toPath().toAbsolutePath().relativize(current.toPath().toAbsolutePath()).toString().replace('\\', '/');
                if (relativePath.isEmpty()) relativePath = current.getName();
                String extension = getExtension(current.getName());
                String stableId = relativePath;
                SemanticNode node = new SemanticNode(stableId, relativePath, extension);
                node.getAttributes().put("size", String.valueOf(current.length()));
                snapshot.addNode(node);
            } catch (Exception e) {
                String path = current.getAbsolutePath().replace('\\', '/');
                String rootPath = root.getAbsolutePath().replace('\\', '/');
                if (path.startsWith(rootPath)) {
                    String rel = path.substring(rootPath.length());
                    if (rel.startsWith("/")) rel = rel.substring(1);
                    String extension = getExtension(current.getName());
                    SemanticNode node = new SemanticNode(rel, rel, extension);
                    node.getAttributes().put("size", String.valueOf(current.length()));
                    snapshot.addNode(node);
                }
            }
        }
    }

    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot <= 0) return "unknown";
        String ext = fileName.substring(lastDot + 1).toLowerCase();

        // Categorize file types into carrier classes
        return switch (ext) {
            case "java", "c", "cpp", "py", "js", "ts", "jsx", "tsx", "html", "css", "scss", "sql", "sh", "bat" -> "source_code";
            case "md", "txt", "pdf", "docx", "doc", "odt" -> "document";
            case "pptx", "odp", "key" -> "presentation";
            case "json", "xml", "yaml", "yml" -> "data";
            case "evodata", "parquet", "jsonl", "csv", "tsv" -> "dataset";
            case "png", "jpg", "jpeg", "webp", "gif", "svg" -> "image";
            case "mp3", "wav", "flac", "m4a", "aac", "ogg" -> "audio";
            case "mp4", "webm", "mkv", "avi", "mov" -> "video";
            case "zip", "tar", "gz", "rar", "7z" -> "archive";
            default -> ext;
        };
    }

    private void detectTechnologies(TargetDescriptor target) {
        Set<String> classifications = new java.util.LinkedHashSet<>();
        long audioCount = target.getFiles().stream().filter(f -> "audio".equals(f.getType())).count();
        long videoCount = target.getFiles().stream().filter(f -> "video".equals(f.getType())).count();
        long docCount = target.getFiles().stream().filter(f -> "document".equals(f.getType())).count();
        long presCount = target.getFiles().stream().filter(f -> "presentation".equals(f.getType())).count();
        long datasetCount = target.getFiles().stream().filter(f -> "dataset".equals(f.getType())).count();
        long codeCount = target.getFiles().stream().filter(f -> "source_code".equals(f.getType())).count();

        boolean hasJava = target.getFiles().stream().anyMatch(f -> f.getPath().endsWith(".java") || "pom.xml".equalsIgnoreCase(f.getPath()) || "build.gradle".equalsIgnoreCase(f.getPath()));
        boolean hasWeb = target.getFiles().stream().anyMatch(f -> f.getPath().endsWith(".html") || f.getPath().endsWith(".js") || f.getPath().endsWith(".ts") || "package.json".equalsIgnoreCase(f.getPath()));

        if (hasJava) { classifications.add("JAVA_CODEBASE"); classifications.add("SOFTWARE_PROJECT"); }
        if (hasWeb) { classifications.add("WEB_APPLICATION"); classifications.add("WEBSITE_SOURCE"); }
        if (presCount > 0) classifications.add("PRESENTATION");
        if (audioCount > 0) { classifications.add("MUSIC_ALBUM"); classifications.add("AUDIO_COLLECTION"); }
        if (videoCount > 0) { classifications.add("VIDEO_PROJECT"); classifications.add("MULTIMEDIA_PROJECT"); }
        if (docCount > 0 && codeCount == 0) classifications.add("DOCUMENT_COLLECTION");
        if (datasetCount > 0) classifications.add("DATASET");

        if (classifications.size() > 2) classifications.add("MIXED_PROJECT");
        if (classifications.isEmpty()) classifications.add("UNKNOWN");

        target.getDetectedTechnologies().addAll(classifications);
    }

    private void addIfNotExists(List<String> list, String value) {
        if (!list.contains(value)) list.add(value);
    }

    private void detectTechnologiesInSnapshot(TargetSnapshot snapshot) {
        Set<String> classifications = new java.util.LinkedHashSet<>();
        long audioCount = snapshot.getNodes().values().stream().filter(n -> "audio".equals(n.getType())).count();
        long videoCount = snapshot.getNodes().values().stream().filter(n -> "video".equals(n.getType())).count();
        long docCount = snapshot.getNodes().values().stream().filter(n -> "document".equals(n.getType())).count();
        long presCount = snapshot.getNodes().values().stream().filter(n -> "presentation".equals(n.getType())).count();
        long datasetCount = snapshot.getNodes().values().stream().filter(n -> "dataset".equals(n.getType())).count();
        long codeCount = snapshot.getNodes().values().stream().filter(n -> "source_code".equals(n.getType())).count();

        boolean hasJava = snapshot.getNodes().values().stream().anyMatch(n -> n.getPath().endsWith(".java") || n.getPath().endsWith("pom.xml") || n.getPath().endsWith("build.gradle"));
        boolean hasWeb = snapshot.getNodes().values().stream().anyMatch(n -> n.getPath().endsWith(".html") || n.getPath().endsWith(".js") || n.getPath().endsWith(".ts") || n.getPath().endsWith("package.json"));

        if (hasJava) { classifications.add("JAVA_CODEBASE"); classifications.add("SOFTWARE_PROJECT"); }
        if (hasWeb) { classifications.add("WEB_APPLICATION"); classifications.add("WEBSITE_SOURCE"); }
        if (presCount > 0) classifications.add("PRESENTATION");
        if (audioCount > 0) { classifications.add("MUSIC_ALBUM"); classifications.add("AUDIO_COLLECTION"); }
        if (videoCount > 0) { classifications.add("VIDEO_PROJECT"); classifications.add("MULTIMEDIA_PROJECT"); }
        if (docCount > 0 && codeCount == 0) classifications.add("DOCUMENT_COLLECTION");
        if (datasetCount > 0) classifications.add("DATASET");

        if (classifications.size() > 2) classifications.add("MIXED_PROJECT");
        if (classifications.isEmpty()) classifications.add("UNKNOWN");

        snapshot.getMetadata().put("targetClassifications", String.join(", ", classifications));
    }
}
