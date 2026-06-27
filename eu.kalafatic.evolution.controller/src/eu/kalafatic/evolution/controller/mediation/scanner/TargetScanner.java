package eu.kalafatic.evolution.controller.mediation.scanner;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.io.File;
import java.util.Set;
import java.util.List;
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
        return (lastDot > 0) ? fileName.substring(lastDot + 1).toLowerCase() : "unknown";
    }

    private void detectTechnologies(TargetDescriptor target) {
        // REFACTOR: Technology detection is no longer assumed by labels.
        // It is an emergent property discovered during semantic extraction and curation.
    }

    private void addIfNotExists(List<String> list, String value) {
        if (!list.contains(value)) list.add(value);
    }

    private void detectTechnologiesInSnapshot(TargetSnapshot snapshot) {
        // REFACTOR: Move technology detection to runtime cognitive inference.
    }
}
