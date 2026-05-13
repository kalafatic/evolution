package eu.kalafatic.evolution.controller.orchestration.mediated.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.FileDescriptor;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.TargetDescriptor;

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
}
