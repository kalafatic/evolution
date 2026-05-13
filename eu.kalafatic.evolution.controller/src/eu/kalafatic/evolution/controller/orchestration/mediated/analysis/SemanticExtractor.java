package eu.kalafatic.evolution.controller.orchestration.mediated.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.FileDescriptor;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.SemanticEdge;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.SemanticNode;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.TargetDescriptor;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.TargetSnapshot;

/**
 * Extracts lightweight semantic information from files using heuristics.
 */
public class SemanticExtractor {

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
        } catch (IOException e) {
            // Log and skip
        }
    }

    private void analyzeContent(FileDescriptor file, List<String> lines) {
        for (String line : lines) {
            String trimmed = line.trim();
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
