package eu.kalafatic.evolution.controller.mediation.scanner;

import java.io.File;
import java.util.Set;
import java.util.HashSet;
import eu.kalafatic.evolution.controller.mediation.model.FileDescriptor;
import eu.kalafatic.evolution.controller.mediation.model.SemanticNode;
import eu.kalafatic.evolution.controller.mediation.model.TargetDescriptor;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import java.util.ArrayList;

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
        scanRecursiveToSnapshot(root, root, snapshot);
        detectTechnologiesInSnapshot(snapshot);
        return snapshot;
    }

    private void scanRecursive(File current, File root, TargetDescriptor target) {
        if (current.isDirectory()) {
            if (IGNORE_DIRS.contains(current.getName())) return;
            File[] children = current.listFiles();
            if (children != null) {
                for (File child : children) scanRecursive(child, root, target);
            }
        } else {
            String relativePath = root.toPath().relativize(current.toPath()).toString().replace('\\', '/');
            String extension = getExtension(current.getName());
            target.getFiles().add(new FileDescriptor(relativePath, extension, current.length()));
        }
    }

    private void scanRecursiveToSnapshot(File current, File root, TargetSnapshot snapshot) {
        if (current.isDirectory()) {
            if (IGNORE_DIRS.contains(current.getName())) return;
            File[] children = current.listFiles();
            if (children != null) {
                for (File child : children) scanRecursiveToSnapshot(child, root, snapshot);
            }
        } else {
            String relativePath = root.toPath().relativize(current.toPath()).toString().replace('\\', '/');
            String extension = getExtension(current.getName());
            String stableId = relativePath; // Stable identifier is the path
            SemanticNode node = new SemanticNode(stableId, relativePath, extension);
            node.getAttributes().put("size", String.valueOf(current.length()));
            snapshot.addNode(node);
        }
    }

    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot > 0) ? fileName.substring(lastDot + 1).toLowerCase() : "unknown";
    }

    private void detectTechnologies(TargetDescriptor target) {
        Set<String> techs = new HashSet<>();
        for (FileDescriptor file : target.getFiles()) {
            String path = file.getPath();
            if (path.endsWith("pom.xml")) techs.add("Maven");
            if (path.endsWith("build.gradle")) techs.add("Gradle");
            if (path.contains("package.json")) techs.add("Node.js/React");
            if (path.endsWith(".java")) techs.add("Java");
            if (path.endsWith(".ts") || path.endsWith(".tsx")) techs.add("TypeScript/React");
            if (path.endsWith(".py")) techs.add("Python");
            if (path.endsWith(".pdf")) techs.add("PDF Documents");
            if (path.endsWith(".html")) techs.add("HTML/Web");
        }
        target.getDetectedTechnologies().addAll(techs);
    }

    private void detectTechnologiesInSnapshot(TargetSnapshot snapshot) {
        Set<String> techs = new HashSet<>();
        for (SemanticNode node : snapshot.getNodes().values()) {
            String path = node.getPath();
            if (path.endsWith("pom.xml")) techs.add("Maven");
            if (path.endsWith("build.gradle")) techs.add("Gradle");
            if (path.contains("package.json")) techs.add("Node.js/React");
            if (path.endsWith(".java")) techs.add("Java");
            if (path.endsWith(".ts") || path.endsWith(".tsx")) techs.add("TypeScript/React");
            if (path.endsWith(".py")) techs.add("Python");
            if (path.endsWith(".pdf")) techs.add("PDF Documents");
            if (path.endsWith(".html")) techs.add("HTML/Web");
        }
        snapshot.getMetadata().put("detectedTechnologies", new ArrayList<>(techs));
    }
}
