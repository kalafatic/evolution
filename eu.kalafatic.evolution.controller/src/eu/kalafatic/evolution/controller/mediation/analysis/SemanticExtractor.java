package eu.kalafatic.evolution.controller.mediation.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.controller.mediation.model.FileDescriptor;
import eu.kalafatic.evolution.controller.mediation.model.SemanticEdge;
import eu.kalafatic.evolution.controller.mediation.model.SemanticNode;
import eu.kalafatic.evolution.controller.mediation.model.TargetDescriptor;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.utils.semantic.AIContextTool;
import eu.kalafatic.utils.semantic.EvoMetadata;

/**
 * Extracts lightweight semantic information from files using heuristics and AI metadata.
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
            analyzeContent(file, lines);

            // AI Metadata Ingestion
            EvoMetadata meta = contextTool.loadMetadata(actualFile);
            if (meta != null) {
                if (meta.getDomain() != null) file.getTags().add("domain:" + meta.getDomain());
                if (meta.getPurpose() != null) file.getTags().add("purpose:" + meta.getPurpose());
            }
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

            // AI Metadata Ingestion
            EvoMetadata meta = contextTool.loadMetadata(actualFile);
            if (meta != null) {
                if (meta.getDomain() != null) node.getAttributes().put("domain", meta.getDomain());
                if (meta.getPurpose() != null) node.getAttributes().put("purpose", meta.getPurpose());
                if (meta.getRole() != null) node.getAttributes().put("role", meta.getRole());
            }

            // Auto Domain Inference based on package
            inferDomainFromPackage(node);

        } catch (IOException e) {
            // Log and skip
        }
    }

    private void inferDomainFromPackage(SemanticNode node) {
        String path = node.getPath();
        if (path.contains("/mediation/")) node.getAttributes().put("inferredDomain", "mediation");
        else if (path.contains("/trajectory/")) node.getAttributes().put("inferredDomain", "trajectory");
        else if (path.contains("/supervision/")) node.getAttributes().put("inferredDomain", "supervision");
        else if (path.contains("/execution/")) node.getAttributes().put("inferredDomain", "execution");
        else if (path.contains("/orchestration/")) node.getAttributes().put("inferredDomain", "orchestration");
    }

    private void analyzeContent(FileDescriptor file, List<String> lines) {
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains("@EvolutionComponent")) {
                file.getTags().add("Evolution Component");
                extractAnnotationMetadata(file.getTags(), trimmed);
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
                extractAnnotationMetadataForNode(node, trimmed);
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

    private void extractAnnotationMetadata(List<String> tags, String line) {
        if (line.contains("domain")) {
            tags.add("domain:" + extractAnnotationValue(line, "domain"));
        }
        if (line.contains("role")) {
            tags.add("role:" + extractAnnotationValue(line, "role"));
        }
    }

    private void extractAnnotationMetadataForNode(SemanticNode node, String line) {
        if (line.contains("domain")) {
            node.getAttributes().put("annotatedDomain", extractAnnotationValue(line, "domain"));
        }
        if (line.contains("role")) {
            node.getAttributes().put("annotatedRole", extractAnnotationValue(line, "role"));
        }
    }

    private String extractAnnotationValue(String line, String key) {
        try {
            int start = line.indexOf(key + " = \"") + key.length() + 4;
            int end = line.indexOf("\"", start);
            if (start > 0 && end > start) {
                return line.substring(start, end);
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
