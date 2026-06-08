package eu.kalafatic.evolution.controller.mediation.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import eu.kalafatic.evolution.controller.mediation.model.FileDescriptor;
import eu.kalafatic.evolution.controller.mediation.model.SemanticEdge;
import eu.kalafatic.evolution.controller.mediation.model.SemanticNode;
import eu.kalafatic.evolution.controller.mediation.model.TargetDescriptor;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.utils.semantic.AIContextTool;
import eu.kalafatic.utils.semantic.EvoMetadata;

/**
 * Extracts lightweight semantic information from files using heuristics and AI metadata.
 * Implements Semantic Authority Hierarchy: Annotation > Sidecar > Package Context > Heuristic.
 */
public class SemanticExtractor {

    private final AIContextTool contextTool = new AIContextTool();

    public void extract(TargetDescriptor target, File root) {
        for (FileDescriptor file : target.getFiles()) {
            analyzeFile(file, root);
        }
    }

    public void extractToSnapshot(TargetSnapshot snapshot) {
        extractToSnapshot(snapshot, null);
    }

    public void extractToSnapshot(TargetSnapshot snapshot, List<String> pathsToAnalyze) {
        File root = new File(snapshot.getRootPath());
        if (pathsToAnalyze == null) {
            for (SemanticNode node : snapshot.getNodes().values()) {
                analyzeNode(node, root, snapshot);
            }
        } else {
            for (String path : pathsToAnalyze) {
                SemanticNode node = snapshot.getNodes().get(path);
                if (node != null) {
                    analyzeNode(node, root, snapshot);
                }
            }
        }
    }

    private void analyzeFile(FileDescriptor file, File root) {
        File actualFile = new File(root, file.getPath());
        if (actualFile.length() > 500000) return; // Skip large files

        try {
            List<String> lines = Files.readAllLines(actualFile.toPath());

            // Collect metadata from all levels
            Map<String, String> annotationMeta = extractAnnotationMetadataFromLines(lines);
            EvoMetadata sidecarMeta = contextTool.loadMetadata(actualFile);
            String packageDomain = inferDomainFromPath(file.getPath());
            String markdownContext = loadPackageMarkdownContext(actualFile.getParentFile());

            // Resolve with Authority Hierarchy
            resolveMetadata(file.getTags(), annotationMeta, sidecarMeta, markdownContext, packageDomain);

            analyzeContent(file, lines);
        } catch (IOException e) {
            // Log and skip
        }
    }

    private void analyzeNode(SemanticNode node, File root, TargetSnapshot snapshot) {
        File actualFile = new File(root, node.getPath());
        if (actualFile.length() > 500000) return;

        try {
            List<String> lines = Files.readAllLines(actualFile.toPath());
            analyzeContentForNode(node, lines, snapshot);

            // Generate compact summary (first non-empty 3 lines)
            StringBuilder summary = new StringBuilder();
            int count = 0;
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    summary.append(line.trim()).append(" ");
                    if (++count >= 3) break;
                }
            }
            node.setSummary(summary.toString().trim());

            // Resolution with Authority Hierarchy
            Map<String, String> annotationMeta = extractAnnotationMetadataFromLines(lines);
            EvoMetadata sidecarMeta = contextTool.loadMetadata(actualFile);
            String packageDomain = inferDomainFromPath(node.getPath());
            String markdownContext = loadPackageMarkdownContext(actualFile.getParentFile());

            resolveMetadataForNode(node, annotationMeta, sidecarMeta, markdownContext, packageDomain);

        } catch (IOException e) {
            // Log and skip
        }
    }

    private void resolveMetadata(List<String> tags, Map<String, String> ann, EvoMetadata side, String md, String pkg) {
        String domain = ann.containsKey("domain") ? ann.get("domain") :
                        (side != null && side.getDomain() != null ? side.getDomain() :
                        (md != null ? md : pkg));

        if (domain != null) tags.add("domain:" + domain);

        // Conflict detection
        if (ann.containsKey("domain") && side != null && side.getDomain() != null && !ann.get("domain").equals(side.getDomain())) {
            tags.add("warning:metadata_conflict_domain");
        }

        if (side != null && side.isStale()) {
            tags.add("warning:stale_metadata");
        }
    }

    private void resolveMetadataForNode(SemanticNode node, Map<String, String> ann, EvoMetadata side, String md, String pkg) {
        String domain = ann.getOrDefault("domain",
                        (side != null && side.getDomain() != null) ? side.getDomain() :
                        (md != null ? md : pkg));

        String role = ann.getOrDefault("role", (side != null && side.getRole() != null) ? side.getRole() : "unknown");

        if (domain != null) node.getAttributes().put("domain", domain);
        if (role != null) node.getAttributes().put("role", role);

        if (side != null && side.isStale()) {
            node.getTags().add("warning:stale_metadata");
        }
    }

    private String inferDomainFromPath(String path) {
        if (path.contains("/mediation/")) return "mediation";
        if (path.contains("/trajectory/")) return "trajectory";
        if (path.contains("/supervision/")) return "supervision";
        if (path.contains("/execution/")) return "execution";
        if (path.contains("/orchestration/")) return "orchestration";
        return null;
    }

    private String loadPackageMarkdownContext(File directory) {
        File contextFile = new File(directory, "PACKAGE_CONTEXT.md");
        if (contextFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(contextFile.toPath());
                for (String line : lines) {
                    if (line.startsWith("## Domain: ") || line.startsWith("# Domain: ") || line.contains("Domain: ")) {
                         return line.substring(line.indexOf("Domain:") + 7).trim().toLowerCase();
                    }
                }
            } catch (IOException e) {}
        }
        return null;
    }

    private Map<String, String> extractAnnotationMetadataFromLines(List<String> lines) {
        Map<String, String> meta = new HashMap<>();
        boolean inAnnotation = false;
        StringBuilder annotationBlock = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains("@EvolutionComponent")) {
                inAnnotation = true;
            }

            if (inAnnotation) {
                annotationBlock.append(line);
                if (trimmed.endsWith(")")) {
                    inAnnotation = false;
                    parseAnnotationBlock(annotationBlock.toString(), meta);
                    break;
                }
            }
        }
        return meta;
    }

    private void parseAnnotationBlock(String block, Map<String, String> meta) {
        if (block.contains("domain")) meta.put("domain", extractAnnotationValue(block, "domain"));
        if (block.contains("role")) meta.put("role", extractAnnotationValue(block, "role"));
    }

    private void analyzeContent(FileDescriptor file, List<String> lines) {
        for (String line : lines) {
            String trimmed = line.trim();
            // REFACTOR: Generic structural evidence collection.
            // No hardcoded technology tags.
            if (trimmed.contains("@")) {
                addTagIfNotExists(file.getTags(), "Annotated");
            }
            if (trimmed.contains("public static void main") || trimmed.contains("setup()") || trimmed.contains("loop()")) {
                addTagIfNotExists(file.getTags(), "Executory");
            }
        }
    }

    private void analyzeContentForNode(SemanticNode node, List<String> lines, TargetSnapshot snapshot) {
        for (String line : lines) {
            String trimmed = line.trim();

            // Abstract Significance Evidence
            if (trimmed.contains("@")) {
                addTagIfNotExists(node.getTags(), "Annotated");
            }

            // Extract generic structures (classes/functions/definitions)
            // This evidence is used for Semantic Density calculation.
            if (trimmed.startsWith("public class ") || trimmed.startsWith("class ") ||
                (trimmed.contains("void ") && trimmed.contains("()") && trimmed.endsWith("{")) ||
                (trimmed.startsWith("namespace "))) {
                node.getStructures().add("struct:" + trimmed);
            }
            if (trimmed.contains("public ") && trimmed.contains("(") && trimmed.contains(")") && trimmed.endsWith("{")) {
                node.getStructures().add("behavior:" + trimmed);
            }

            // Extract dependencies (Generic: imports, includes, requirements)
            // This evidence is used for Graph Centrality calculation.
            if (trimmed.startsWith("import ") || trimmed.startsWith("#include ") || trimmed.startsWith("require(")) {
                String dep = extractDependency(trimmed);
                if (dep != null) {
                    node.getDependencies().add(dep);
                    linkDependency(node, dep, snapshot);
                }
            }
        }
    }

    private String extractDependency(String line) {
        if (line.startsWith("import ")) return line.substring(7).replace(";", "").trim();
        if (line.startsWith("#include ")) return line.substring(9).replace("\"", "").replace("<", "").replace(">", "").trim();
        if (line.startsWith("require(")) {
            int start = line.indexOf("(") + 1;
            int end = line.indexOf(")");
            if (end > start) return line.substring(start, end).replace("'", "").replace("\"", "").trim();
        }
        return null;
    }

    private void addTagIfNotExists(List<String> tags, String tag) {
        if (!tags.contains(tag)) tags.add(tag);
    }

    private String extractAnnotationValue(String block, String key) {
        try {
            int start = block.indexOf(key + " = \"") + key.length() + 4;
            if (start < key.length() + 4) { // Try without spaces
                 start = block.indexOf(key + "=\"") + key.length() + 2;
            }
            int end = block.indexOf("\"", start);
            if (start > 0 && end > start) {
                return block.substring(start, end);
            }
        } catch (Exception e) {}
        return "unknown";
    }

    private void linkDependency(SemanticNode source, String dependency, TargetSnapshot snapshot) {
        // Generic dependency linking based on path matching.
        // Works for both Java packages and C-style includes.
        String depMatch = dependency.replace(".", "/");
        for (SemanticNode target : snapshot.getNodes().values()) {
            String targetPath = target.getPath();
            if (targetPath.contains(depMatch) || targetPath.endsWith(dependency)) {
                snapshot.addEdge(new SemanticEdge(source.getId(), target.getId(), SemanticEdge.EdgeType.DEPENDS_ON));
            }
        }
    }
}
