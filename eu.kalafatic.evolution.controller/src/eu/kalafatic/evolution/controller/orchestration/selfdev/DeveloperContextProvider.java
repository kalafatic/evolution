package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetClassificationDetector;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetClassificationDetector.ClassificationResult;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool;
import eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider;

/**
 * Progressive, task-oriented context provider for Darwin Developer mode.
 * Resolves target type (PROJECT, FOLDER, REPOSITORY), technology stack, Java version,
 * build tools, and key files progressively without dumping entire repositories into LLM context.
 * Supports optional Genome and INTENT integration when available.
 */
public class DeveloperContextProvider {

    public enum TargetType {
        PROJECT,
        FOLDER,
        REPOSITORY
    }

    private final GitVersionControlProvider gitVcsProvider = new GitVersionControlProvider();

    /**
     * Resolves the target type for a given root directory and context.
     */
    public TargetType resolveTargetType(File targetRoot, TaskContext context) {
        if (targetRoot == null || !targetRoot.exists()) {
            return TargetType.FOLDER;
        }

        // 1. Check Eclipse Workspace Project
        try {
            org.eclipse.core.resources.IWorkspaceRoot wsRoot = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot();
            if (wsRoot != null) {
                org.eclipse.core.resources.IProject[] projects = wsRoot.getProjects();
                for (org.eclipse.core.resources.IProject p : projects) {
                    if (p != null && p.getLocation() != null && p.getLocation().toFile().equals(targetRoot)) {
                        return TargetType.PROJECT;
                    }
                }
            }
        } catch (Throwable ignored) {
            // Headless / non-OSGi fallback
        }

        if (new File(targetRoot, ".project").exists()) {
            return TargetType.PROJECT;
        }

        // 2. Check Git Repository
        if (EclipseGitEvoTool.isGitRepository(targetRoot)) {
            return TargetType.REPOSITORY;
        }

        // 3. Fallback: Filesystem Folder
        return TargetType.FOLDER;
    }

    /**
     * Builds the progressive baseline development context string for the LLM.
     */
    public String buildInitialContext(File targetRoot, TaskContext context, String request) {
        if (targetRoot == null || !targetRoot.exists()) {
            return "--- DEVELOPER TARGET CONTEXT ---\nTarget: None or does not exist.\n";
        }

        StringBuilder sb = new StringBuilder();
        TargetType targetType = resolveTargetType(targetRoot, context);

        sb.append("--- DEVELOPER TARGET CONTEXT ---\n");
        sb.append("Target Type: ").append(targetType).append("\n");
        sb.append("Target Location: ").append(targetRoot.getAbsolutePath()).append("\n");
        sb.append("Project / Root Name: ").append(targetRoot.getName()).append("\n");

        // Classification / Tech Stack
        ClassificationResult classification = TargetClassificationDetector.detect(targetRoot);
        sb.append("Detected Category: ").append(classification.getCategory()).append(" (Confidence: ").append(classification.getConfidence()).append(")\n");

        // Build System & Java Version
        String buildSystem = detectBuildSystem(targetRoot);
        sb.append("Build System: ").append(buildSystem).append("\n");
        String javaVersion = detectJavaVersion(targetRoot);
        if (javaVersion != null) {
            sb.append("Java Version: ").append(javaVersion).append("\n");
        }

        // Git Status & Branch (if applicable)
        if (targetType == TargetType.REPOSITORY || EclipseGitEvoTool.isGitRepository(targetRoot)) {
            try {
                GitManager gitMgr = new GitManager(targetRoot);
                String branch = gitMgr.getCurrentBranch();
                sb.append("Git Branch: ").append(branch != null ? branch : "unknown").append("\n");
                List<String> uncommitted = gitVcsProvider.getChangedFiles(targetRoot, "HEAD");
                sb.append("Pre-existing Uncommitted Changes: ").append(uncommitted.size()).append(" files\n");
            } catch (Throwable t) {
                sb.append("Git Status: Available (status check: ").append(t.getMessage()).append(")\n");
            }
        }

        // Key Configuration Files
        List<String> configFiles = findKeyConfigFiles(targetRoot);
        if (!configFiles.isEmpty()) {
            sb.append("Key Config Files: ").append(String.join(", ", configFiles)).append("\n");
        }

        // High-level bounded directory overview (top 2 levels, max 25 items)
        sb.append("Directory Overview (Top Level):\n");
        appendDirectoryOverview(targetRoot, sb, 25);

        // Optional: INTENT reality integration (if already established in context)
        appendOptionalIntentContext(context, sb);

        // Progressive Relevant Components & Tools Context
        appendProgressiveToolsAndComponents(targetRoot, context, request, sb);

        // Optional: Genome & Functionality inventory (if available in context / project)
        appendOptionalGenomeContext(targetRoot, context, sb);

        return sb.toString();
    }

    /**
     * Progressively loads relevant classes, methods, dependencies, and tool execution results.
     */
    private void appendProgressiveToolsAndComponents(File targetRoot, TaskContext context, String request, StringBuilder sb) {
        sb.append("\n--- AVAILABLE DEVELOPMENT TOOLS ---\n");
        sb.append("You have access to Darwin Java & project tools:\n");
        sb.append("- java.get_project_structure : Discovers packages, source roots, and class inventory.\n");
        sb.append("- java.get_dependencies      : Inspects Maven dependencies and OSGi Require-Bundle headers.\n");
        sb.append("- java.inspect_class         : Inspects class structure, fields, methods, interfaces, and signatures.\n");
        sb.append("- java.inspect_method        : Inspects method signature and body implementation.\n");
        sb.append("- java.find_references       : Searches references across Java source files.\n");
        sb.append("- java.find_callers          : Identifies methods that invoke a specific method.\n");
        sb.append("- java.find_implementations  : Discovers classes implementing an interface or extending a class.\n");
        sb.append("- java.get_diagnostics       : Retrieves compiler errors and warnings.\n");
        sb.append("- java.format                : Normalizes Java source formatting.\n\n");

        // If compiler diagnostics exist in metadata, append them
        if (context != null && context.getOrchestrationState() != null) {
            Object diag = context.getOrchestrationState().getMetadata().get("latestCompilerDiagnostics");
            if (diag instanceof String && !((String) diag).trim().isEmpty()) {
                sb.append("--- LATEST COMPILER DIAGNOSTICS ---\n").append(diag).append("\n\n");
            }
        }
    }

    /**
     * Detects build system used by target.
     */
    public String detectBuildSystem(File targetRoot) {
        if (new File(targetRoot, "pom.xml").exists()) {
            try {
                String pomContent = Files.readString(new File(targetRoot, "pom.xml").toPath());
                if (pomContent.contains("tycho-maven-plugin") || pomContent.contains("org.eclipse.tycho")) {
                    return "Maven / Eclipse Tycho";
                }
            } catch (Exception ignored) {}
            return "Maven";
        }
        if (new File(targetRoot, "build.gradle").exists() || new File(targetRoot, "build.gradle.kts").exists()) {
            return "Gradle";
        }
        if (new File(targetRoot, "build.xml").exists()) {
            return "Ant";
        }
        if (new File(targetRoot, "package.json").exists()) {
            return "Node.js (npm/yarn/pnpm)";
        }
        if (new File(targetRoot, "CMakeLists.txt").exists()) {
            return "CMake";
        }
        if (new File(targetRoot, "Makefile").exists()) {
            return "Make";
        }
        return "Unknown / Standalone";
    }

    /**
     * Detects Java version compliance.
     */
    public String detectJavaVersion(File targetRoot) {
        File jdtPrefs = new File(targetRoot, ".settings/org.eclipse.jdt.core.prefs");
        if (jdtPrefs.exists()) {
            try {
                List<String> lines = Files.readAllLines(jdtPrefs.toPath());
                for (String l : lines) {
                    if (l.startsWith("org.eclipse.jdt.core.compiler.compliance=")) {
                        return l.split("=", 2)[1].trim();
                    }
                }
            } catch (Exception ignored) {}
        }
        File pom = new File(targetRoot, "pom.xml");
        if (pom.exists()) {
            try {
                String content = Files.readString(pom.toPath());
                if (content.contains("<java.version>")) {
                    return extractXmlTag(content, "java.version");
                }
                if (content.contains("<maven.compiler.release>")) {
                    return extractXmlTag(content, "maven.compiler.release");
                }
                if (content.contains("<maven.compiler.source>")) {
                    return extractXmlTag(content, "maven.compiler.source");
                }
            } catch (Exception ignored) {}
        }
        return System.getProperty("java.specification.version");
    }

    private String extractXmlTag(String xml, String tag) {
        int start = xml.indexOf("<" + tag + ">");
        int end = xml.indexOf("</" + tag + ">");
        if (start != -1 && end != -1 && end > start) {
            return xml.substring(start + tag.length() + 2, end).trim();
        }
        return null;
    }

    private List<String> findKeyConfigFiles(File targetRoot) {
        List<String> found = new ArrayList<>();
        String[] candidates = {
            "pom.xml", "build.gradle", "build.gradle.kts", "build.xml",
            "META-INF/MANIFEST.MF", "plugin.xml", ".project", ".classpath",
            "package.json", "CMakeLists.txt", "README.md"
        };
        for (String c : candidates) {
            if (new File(targetRoot, c).exists()) {
                found.add(c);
            }
        }
        return found;
    }

    private void appendDirectoryOverview(File root, StringBuilder sb, int maxItems) {
        File[] children = root.listFiles();
        if (children == null) return;
        int count = 0;
        for (File child : children) {
            if (child.getName().startsWith(".") && !".project".equals(child.getName()) && !".classpath".equals(child.getName())) {
                continue;
            }
            if (count++ >= maxItems) {
                sb.append("  ... (more items truncated)\n");
                break;
            }
            sb.append("  - ").append(child.getName()).append(child.isDirectory() ? "/" : "").append("\n");
        }
    }

    private void appendOptionalIntentContext(TaskContext context, StringBuilder sb) {
        if (context == null || context.getOrchestrationState() == null) return;
        Object modelObj = context.getOrchestrationState().getMetadata().get("targetRealityModel");
        if (modelObj instanceof TargetRealityModel realityModel) {
            if (realityModel.getPurpose() != null && !realityModel.getPurpose().isEmpty()) {
                sb.append("\n--- DISCOVERED INTENT / PURPOSE (OPTIONAL) ---\n");
                sb.append("Domain: ").append(realityModel.getDomain()).append("\n");
                sb.append("Purpose: ").append(realityModel.getPurpose()).append("\n");
                if (realityModel.getArchitectureSummary() != null) {
                    sb.append("Architecture: ").append(realityModel.getArchitectureSummary()).append("\n");
                }
            }
        }
    }

    private void appendOptionalGenomeContext(File targetRoot, TaskContext context, StringBuilder sb) {
        File genomeInventory = new File(targetRoot, "genome/current/functionality_inventory.json");
        if (!genomeInventory.exists()) {
            genomeInventory = new File("genome/current/functionality_inventory.json");
        }
        if (genomeInventory.exists()) {
            try {
                String json = Files.readString(genomeInventory.toPath());
                JSONObject obj = new JSONObject(json);
                JSONArray funcs = obj.optJSONArray("functionalities");
                if (funcs != null && funcs.length() > 0) {
                    sb.append("\n--- FUNCTIONALITY INVENTORY (GENOME CONTEXT) ---\n");
                    int limit = Math.min(funcs.length(), 10);
                    for (int i = 0; i < limit; i++) {
                        JSONObject f = funcs.getJSONObject(i);
                        sb.append("- ").append(f.optString("name"))
                          .append(" (").append(f.optString("primaryClass", "n/a")).append("): ")
                          .append(f.optString("description", "")).append("\n");
                    }
                    if (funcs.length() > limit) {
                        sb.append("  ... (").append(funcs.length() - limit).append(" more functionalities)\n");
                    }
                }
            } catch (Throwable ignored) {}
        }
    }
}
