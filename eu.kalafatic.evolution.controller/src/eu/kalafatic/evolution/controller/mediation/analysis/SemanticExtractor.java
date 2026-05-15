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
        inferArchitecture(target);
    }

    public void extractToSnapshot(TargetSnapshot snapshot) {
        File root = new File(snapshot.getRootPath());
        for (SemanticNode node : snapshot.getNodes().values()) {
            analyzeNode(node, root, snapshot);
        }
        inferArchitectureFromSnapshot(snapshot);
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
            if (trimmed.contains("@EvolutionComponent")) {
                file.getTags().add("Evolution Component");
            }
            if (trimmed.contains("@Component") || trimmed.contains("@Service") || trimmed.contains("@Controller")) {
                file.getTags().add("Spring Component");
            }
            if (trimmed.contains("public static void main")) {
                file.getTags().add("Entry Point");
            }
            if (trimmed.contains("extends HttpServlet") || trimmed.contains("@WebServlet")) {
                file.getTags().add("Servlet");
            }
            if (trimmed.contains("import React")) {
                file.getTags().add("React Component");
            }
            if (trimmed.contains("interface ") && !trimmed.contains("(")) {
                file.getTags().add("Interface");
            }
        }
    }

    private void analyzeContentForNode(SemanticNode node, List<String> lines, TargetSnapshot snapshot) {
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains("@EvolutionComponent")) {
                node.getTags().add("Evolution Component");
            }
            if (trimmed.contains("@Component") || trimmed.contains("@Service") || trimmed.contains("@Controller")) {
                node.getTags().add("Spring Component");
            }
            if (trimmed.contains("public static void main")) {
                node.getTags().add("Entry Point");
            }
            if (trimmed.contains("extends HttpServlet") || trimmed.contains("@WebServlet")) {
                node.getTags().add("Servlet");
            }
            if (trimmed.contains("import React")) {
                node.getTags().add("React Component");
            }
            if (trimmed.contains("interface ") && !trimmed.contains("(")) {
                node.getTags().add("Interface");
            }

            // Extract structures (classes/functions)
            if (trimmed.startsWith("public class ") || trimmed.startsWith("class ")) {
                node.getStructures().add("class:" + trimmed);
            }
            if (trimmed.contains("public ") && trimmed.contains("(") && trimmed.contains(")") && trimmed.endsWith("{")) {
                node.getStructures().add("method:" + trimmed);
            }

            // Extract dependencies (imports)
            if (trimmed.startsWith("import ")) {
                String dep = trimmed.substring(7).replace(";", "").trim();
                node.getDependencies().add(dep);

                // Attempt to link to other nodes in snapshot
                linkDependency(node, dep, snapshot);
            }
        }
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
        // Simple heuristic to find target nodes by path similarity or package
        for (SemanticNode target : snapshot.getNodes().values()) {
            String targetPathAsPackage = target.getPath().replace("/", ".").replace(".java", "");
            if (targetPathAsPackage.endsWith(dependency)) {
                snapshot.addEdge(new SemanticEdge(source.getId(), target.getId(), SemanticEdge.EdgeType.DEPENDS_ON));
            }
        }
    }

    private void inferArchitecture(TargetDescriptor target) {
        StringBuilder sb = new StringBuilder();
        boolean hasJava = target.getDetectedTechnologies().contains("Java");
        boolean hasReact = target.getDetectedTechnologies().contains("Node.js/React") || target.getDetectedTechnologies().contains("TypeScript/React");

        if (hasJava && hasReact) {
            sb.append("Full-stack application with Java backend and React frontend.");
        } else if (hasJava) {
            sb.append("Java-based application/service.");
        } else if (hasReact) {
            sb.append("React-based web application.");
        }

        long entryPoints = target.getFiles().stream().filter(f -> f.getTags().contains("Entry Point")).count();
        if (entryPoints > 0) {
            sb.append(" Found ").append(entryPoints).append(" main entry points.");
        }

        target.setArchitectureInference(sb.toString());
    }

    private void inferArchitectureFromSnapshot(TargetSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        List<String> techs = (List<String>) snapshot.getMetadata().get("detectedTechnologies");
        boolean hasJava = techs != null && techs.contains("Java");
        boolean hasReact = techs != null && (techs.contains("Node.js/React") || techs.contains("TypeScript/React"));

        if (hasJava && hasReact) {
            sb.append("Full-stack application with Java backend and React frontend.");
        } else if (hasJava) {
            sb.append("Java-based application/service.");
        } else if (hasReact) {
            sb.append("React-based web application.");
        }

        long entryPoints = snapshot.getNodes().values().stream().filter(f -> f.getTags().contains("Entry Point")).count();
        if (entryPoints > 0) {
            sb.append(" Found ").append(entryPoints).append(" main entry points.");
        }

        snapshot.getMetadata().put("architectureInference", sb.toString());
    }
}
