package eu.kalafatic.evolution.forge.data.impl.collector;

import eu.kalafatic.evolution.forge.data.api.collector.CollectionContext;
import eu.kalafatic.evolution.forge.data.api.collector.CollectionResult;
import eu.kalafatic.evolution.forge.data.api.collector.CollectorType;
import eu.kalafatic.evolution.forge.data.api.collector.SourceType;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataCollector;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
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
 * Semantic Git-aware ingestion pipeline for the complete EVO Codebase.
 */
public class EvoCodebaseDataCollector extends TrainingDataCollector {

    public enum CodebaseMode {
        RAW_CODE,
        DOCUMENTATION,
        ARCHITECTURE,
        MIXED_AUTO
    }

    private static final Set<String> EXCLUDED_DIRS = new HashSet<>(Arrays.asList(
            "target", "bin", "build", "dist", "generated", ".git", "node_modules",
            "forge-input", "forge-output", "self-dev-run", ".settings"
    ));

    private static final Set<String> EXCLUDED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".class", ".jar", ".zip", ".evo", ".evodata", ".lock", ".png", ".jpg", ".jpeg",
            ".gif", ".ico", ".bmp", ".exec", ".pack", ".idx"
    ));

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*import\\s+([a-zA-Z0-9_.]+)\\s*;");
    private static final Pattern CLASS_PATTERN = Pattern.compile("\\b(public\\s+|protected\\s+|private\\s+|abstract\\s+|final\\s+)*(class|interface|enum|record)\\s+([A-Z][a-zA-Z0-9_]*)(?:\\s+extends\\s+([A-Za-z0-9_.]+))?(?:\\s+implements\\s+([A-Za-z0-9_,\\s.]+))?");
    private static final Pattern METHOD_PATTERN = Pattern.compile("\\b(public|protected|private)\\s+(?:static\\s+)?(?:final\\s+)?(?:[A-Za-z0-9_<>?\\[\\]]+)\\s+([a-zA-Z0-9_]+)\\s*\\(([^)]*)\\)");

    private static final Pattern BUNDLE_SYMBOLIC_NAME_PATTERN = Pattern.compile("Bundle-SymbolicName:\\s*([^;\\r\\n]+)");
    private static final Pattern BUNDLE_VERSION_PATTERN = Pattern.compile("Bundle-Version:\\s*([^;\\r\\n]+)");
    private static final Pattern REQUIRE_BUNDLE_PATTERN = Pattern.compile("Require-Bundle:\\s*([^\r\n]+)");
    private static final Pattern EXPORT_PACKAGE_PATTERN = Pattern.compile("Export-Package:\\s*([^\r\n]+)");

    private CodebaseMode mode = CodebaseMode.MIXED_AUTO;

    public EvoCodebaseDataCollector() {}

    public EvoCodebaseDataCollector(CodebaseMode mode) {
        this.mode = mode != null ? mode : CodebaseMode.MIXED_AUTO;
    }

    @Override
    public CollectorType getType() {
        return CollectorType.EVO_CODEBASE;
    }

    @Override
    public String getName() {
        return "EVO Codebase Git-Aware Ingestion Collector";
    }

    public CodebaseMode getMode() {
        return mode;
    }

    public void setMode(CodebaseMode mode) {
        this.mode = mode != null ? mode : CodebaseMode.MIXED_AUTO;
    }

    @Override
    public CollectionResult collect(CollectionContext context) {
        long startTime = System.currentTimeMillis();
        File dir = context.getProjectDirectory();
        if (dir == null || !dir.exists()) {
            dir = new File(System.getProperty("user.dir"));
        }
        final File projectDir = dir;

        List<TrainingDataItem> items = new ArrayList<>();

        // 1. Git metadata resolution
        GitInfo gitInfo = resolveGitInfo(projectDir);

        if ("dirty".equalsIgnoreCase(gitInfo.workingTreeState)) {
            System.err.println("WARNING: EVO codebase contains uncommitted changes.");
        }

        final Path rootPath = projectDir.toPath();

        try (Stream<Path> walk = Files.walk(rootPath)) {
            List<Path> files = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> isNotExcluded(p, rootPath, context.getExclusions()))
                    .filter(p -> isUnderSizeLimit(p, context.getMaxFileSize()))
                    .sorted()
                    .collect(Collectors.toList());

            List<InstructionPair> instructionPairs = new ArrayList<>();

            for (Path path : files) {
                try {
                    String relativePath = projectDir.toPath().relativize(path).toString().replace('\\', '/');
                    String fileName = path.getFileName().toString();
                    String extension = getExtension(fileName);

                    byte[] fileBytes = Files.readAllBytes(path);

                    if (isBinaryFile(fileBytes, extension)) {
                        continue;
                    }

                    String rawContent = extension.equals(".pdf") ? extractPdfText(fileBytes) : new String(fileBytes, StandardCharsets.UTF_8);

                    if (rawContent == null || rawContent.trim().isEmpty()) {
                        continue;
                    }

                    String fileCategory = classifyFileCategory(relativePath, fileName, rawContent);
                    String language = categoryToLanguage(fileCategory, extension);

                    // Filter based on selected mode
                    if (!matchesMode(fileCategory, mode)) {
                        continue;
                    }

                    TrainingDataItem item = new TrainingDataItem();
                    item.setSourceType(SourceType.EVO_CODEBASE);
                    item.setSource(relativePath);
                    item.setTitle(fileName);

                    // Add complete provenance metadata
                    item.addMetadata("repository", gitInfo.repositoryName);
                    item.addMetadata("repoPath", projectDir.getAbsolutePath());
                    item.addMetadata("remoteUrl", gitInfo.remoteUrl);
                    item.addMetadata("branch", gitInfo.branch);
                    item.addMetadata("commit", gitInfo.commitHash);
                    item.addMetadata("workingTreeState", gitInfo.workingTreeState);
                    if ("dirty".equalsIgnoreCase(gitInfo.workingTreeState)) {
                        item.addMetadata("warning", "WARNING: EVO codebase contains uncommitted changes.");
                    }
                    item.addMetadata("fileType", fileCategory);
                    item.addMetadata("language", language);
                    item.addMetadata("fileSize", fileBytes.length);

                    double qualityScore = calculateFileQualityScore(fileCategory, relativePath, rawContent);
                    item.addMetadata("qualityScore", qualityScore);
                    item.addMetadata("hash", computeSha256(rawContent));

                    // Specialized semantic processing
                    String processedContent = processSemanticContent(fileCategory, relativePath, rawContent, item, instructionPairs);
                    item.setContent(processedContent);

                    items.add(item);

                } catch (Exception e) {
                    // Skip unreadable files gracefully
                }
            }

            // Append generated verified instruction QA pairs as training items
            for (InstructionPair ip : instructionPairs) {
                TrainingDataItem instItem = new TrainingDataItem();
                instItem.setSourceType(SourceType.EVO_CODEBASE);
                instItem.setSource(ip.sourcePath);
                instItem.setTitle("INSTRUCTION: " + ip.question);
                instItem.setContent("Q: " + ip.question + "\n\nA: " + ip.answer);
                instItem.addMetadata("fileType", "INSTRUCTION");
                instItem.addMetadata("language", "en");
                instItem.addMetadata("repository", gitInfo.repositoryName);
                instItem.addMetadata("commit", gitInfo.commitHash);
                instItem.addMetadata("branch", gitInfo.branch);
                instItem.addMetadata("workingTreeState", gitInfo.workingTreeState);
                instItem.addMetadata("qualityScore", 0.98);
                items.add(instItem);
            }

            return CollectionResult.success(getType(), items, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            return CollectionResult.failure(getType(), "Failed to traverse EVO codebase: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }

    private static class GitInfo {
        String repositoryName = "evo";
        String remoteUrl = "";
        String branch = "main";
        String commitHash = "unknown";
        String workingTreeState = "clean";
    }

    private GitInfo resolveGitInfo(File workingDir) {
        GitInfo info = new GitInfo();
        info.repositoryName = workingDir.getName();

        try {
            info.commitHash = executeGitCommand(workingDir, "git rev-parse HEAD");
            info.branch = executeGitCommand(workingDir, "git rev-parse --abbrev-ref HEAD");
            info.remoteUrl = executeGitCommand(workingDir, "git remote get-url origin");

            String statusOutput = executeGitCommand(workingDir, "git status --porcelain");
            if (statusOutput != null && !statusOutput.trim().isEmpty()) {
                info.workingTreeState = "dirty";
            } else {
                info.workingTreeState = "clean";
            }
        } catch (Exception e) {
            // Fallback for non-git environments
            info.commitHash = "head-local";
            info.workingTreeState = "clean";
        }
        return info;
    }

    private String executeGitCommand(File dir, String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd, null, dir);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                process.waitFor();
                return sb.toString().trim();
            }
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isNotExcluded(Path path, Path rootPath, List<String> exclusions) {
        String relPath = rootPath.relativize(path).toString().replace('\\', '/');

        for (String excludedDir : EXCLUDED_DIRS) {
            if (relPath.startsWith(excludedDir + "/") || relPath.contains("/" + excludedDir + "/")) {
                return false;
            }
        }

        String fileName = path.getFileName().toString().toLowerCase();
        for (String ext : EXCLUDED_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                return false;
            }
        }

        if (exclusions != null) {
            for (String ex : exclusions) {
                if (relPath.contains(ex)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isUnderSizeLimit(Path path, long maxFileSize) {
        try {
            return maxFileSize <= 0 || Files.size(path) <= maxFileSize;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isBinaryFile(byte[] bytes, String extension) {
        if (".pdf".equalsIgnoreCase(extension)) return false;
        int checkLen = Math.min(bytes.length, 1024);
        int nonPrintable = 0;
        for (int i = 0; i < checkLen; i++) {
            byte b = bytes[i];
            if (b == 0) return true;
            if ((b < 32 && b != 9 && b != 10 && b != 13) || b > 126) {
                nonPrintable++;
            }
        }
        return checkLen > 0 && ((double) nonPrintable / checkLen) > 0.3;
    }

    private String getExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx != -1 ? fileName.substring(idx).toLowerCase() : "";
    }

    public static String classifyFileCategory(String relPath, String fileName, String content) {
        String lowerPath = relPath.toLowerCase();
        String lowerName = fileName.toLowerCase();

        if (lowerPath.contains("/test/") || lowerPath.contains("/tests/") || lowerName.endsWith("test.java") || lowerName.endsWith("tests.java")) {
            return "JAVA_TEST";
        }
        if (lowerName.endsWith(".java")) {
            return "JAVA_SOURCE";
        }
        if ("manifest.mf".equals(lowerName) || lowerName.endsWith("plugin.xml") || lowerName.endsWith("fragment.xml")) {
            return "OSGI_METADATA";
        }
        if (lowerName.endsWith("feature.xml")) {
            return "FEATURE_XML";
        }
        if (lowerName.endsWith(".product")) {
            return "ECLIPSE_RCP";
        }
        if ("pom.xml".equals(lowerName)) {
            if (content.contains("tycho-maven-plugin") || lowerPath.contains("repository") || lowerPath.contains("target-platform")) {
                return "TYCHO";
            }
            return "MAVEN";
        }
        if (lowerName.endsWith(".xml")) {
            return "XML";
        }
        if (lowerName.endsWith(".md")) {
            if (lowerPath.startsWith("docs/") || lowerPath.startsWith("doc/") || lowerName.startsWith("readme")) {
                return "DOCUMENTATION";
            }
            return "MARKDOWN";
        }
        if (lowerName.endsWith(".pdf")) {
            return "PDF";
        }
        if (lowerName.endsWith(".json")) {
            return "JSON";
        }
        if (lowerName.endsWith(".yaml") || lowerName.endsWith(".yml")) {
            return "YAML";
        }
        if (lowerName.endsWith(".properties")) {
            return "PROPERTIES";
        }
        if (lowerName.endsWith(".sh") || lowerName.endsWith(".bat") || lowerName.endsWith(".cmd") || lowerName.endsWith(".ps1")) {
            return "SHELL";
        }
        if (lowerName.endsWith(".ini") || lowerName.endsWith(".conf") || lowerName.endsWith(".options")) {
            return "CONFIGURATION";
        }
        return "RESOURCE";
    }

    private String categoryToLanguage(String category, String extension) {
        return switch (category) {
            case "JAVA_SOURCE", "JAVA_TEST" -> "java";
            case "XML", "OSGI_METADATA", "FEATURE_XML", "MAVEN", "TYCHO", "ECLIPSE_RCP" -> "xml";
            case "MARKDOWN", "DOCUMENTATION" -> "markdown";
            case "PDF" -> "text";
            case "JSON" -> "json";
            case "YAML" -> "yaml";
            case "PROPERTIES" -> "properties";
            case "SHELL" -> "shell";
            default -> "text";
        };
    }

    private boolean matchesMode(String fileCategory, CodebaseMode mode) {
        if (mode == CodebaseMode.MIXED_AUTO) return true;
        return switch (mode) {
            case RAW_CODE -> fileCategory.equals("JAVA_SOURCE") || fileCategory.equals("JAVA_TEST") || fileCategory.equals("SHELL");
            case DOCUMENTATION -> fileCategory.equals("DOCUMENTATION") || fileCategory.equals("MARKDOWN") || fileCategory.equals("PDF");
            case ARCHITECTURE -> fileCategory.equals("OSGI_METADATA") || fileCategory.equals("TYCHO") || fileCategory.equals("ECLIPSE_RCP") || fileCategory.equals("MAVEN") || fileCategory.equals("FEATURE_XML");
            default -> true;
        };
    }

    private double calculateFileQualityScore(String fileCategory, String relPath, String content) {
        if (content.length() < 10) return 0.2;
        double score = 0.90;

        if ("JAVA_SOURCE".equals(fileCategory)) {
            if (content.contains("package ") && content.contains("class ")) score += 0.08;
            if (content.contains("/**")) score += 0.02; // Has Javadoc
        } else if ("DOCUMENTATION".equals(fileCategory) || "MARKDOWN".equals(fileCategory)) {
            if (content.contains("# ")) score += 0.08;
        } else if ("OSGI_METADATA".equals(fileCategory) || "TYCHO".equals(fileCategory)) {
            score = 0.96;
        }

        if (relPath.contains("/generated/") || relPath.contains("AutoGenerated")) {
            score -= 0.50;
        }

        return Math.min(1.0, Math.max(0.1, score));
    }

    private String processSemanticContent(String fileCategory, String relPath, String content, TrainingDataItem item, List<InstructionPair> instructionPairs) {
        StringBuilder sb = new StringBuilder();

        switch (fileCategory) {
            case "JAVA_SOURCE", "JAVA_TEST" -> {
                JavaMetadata jm = analyzeJavaSource(content);
                item.addMetadata("package", jm.packageName);
                item.addMetadata("class", jm.className);
                item.addMetadata("extends", jm.superClass);
                item.addMetadata("implements", jm.interfaces);
                item.addMetadata("methods", jm.methods);

                sb.append("// File: ").append(relPath).append("\n");
                sb.append("// Category: ").append(fileCategory).append("\n");
                if (!jm.packageName.isEmpty()) sb.append("// Package: ").append(jm.packageName).append("\n");
                if (!jm.className.isEmpty()) sb.append("// Class: ").append(jm.className);
                if (!jm.superClass.isEmpty()) sb.append(" extends ").append(jm.superClass);
                if (!jm.interfaces.isEmpty()) sb.append(" implements ").append(String.join(", ", jm.interfaces));
                sb.append("\n");
                if (!jm.methods.isEmpty()) {
                    sb.append("// Key Methods: ").append(String.join(", ", jm.methods.stream().limit(10).collect(Collectors.toList()))).append("\n");
                }
                sb.append("\n").append(content);

                // Generate verified instruction QA pair
                if (!jm.className.isEmpty()) {
                    InstructionPair ip = new InstructionPair();
                    ip.sourcePath = relPath;
                    ip.question = "What is the purpose and structure of class " + jm.className + " in EVO?";
                    StringBuilder ans = new StringBuilder();
                    ans.append("Class ").append(jm.className).append(" is located at '").append(relPath).append("'.");
                    if (!jm.packageName.isEmpty()) ans.append("\nPackage: ").append(jm.packageName);
                    if (!jm.superClass.isEmpty()) ans.append("\nExtends: ").append(jm.superClass);
                    if (!jm.interfaces.isEmpty()) ans.append("\nImplements: ").append(String.join(", ", jm.interfaces));
                    if (!jm.methods.isEmpty()) ans.append("\nMethods declared: ").append(String.join(", ", jm.methods));
                    ip.answer = ans.toString();
                    instructionPairs.add(ip);
                }
            }
            case "OSGI_METADATA" -> {
                if (relPath.endsWith("MANIFEST.MF")) {
                    OsgiManifestOm om = parseManifest(content);
                    item.addMetadata("bundleSymbolicName", om.symbolicName);
                    item.addMetadata("bundleVersion", om.version);
                    item.addMetadata("requireBundle", om.requireBundle);

                    sb.append("# OSGi Bundle Manifest: ").append(om.symbolicName).append("\n");
                    sb.append("# Version: ").append(om.version).append("\n");
                    sb.append("# Required Bundles: ").append(String.join(", ", om.requireBundle)).append("\n\n");
                    sb.append(content);

                    if (!om.symbolicName.isEmpty()) {
                        InstructionPair ip = new InstructionPair();
                        ip.sourcePath = relPath;
                        ip.question = "What dependencies and version are configured for OSGi bundle " + om.symbolicName + "?";
                        ip.answer = "OSGi Bundle: " + om.symbolicName + "\nVersion: " + om.version + "\nRequired Dependencies: " + String.join(", ", om.requireBundle);
                        instructionPairs.add(ip);
                    }
                } else {
                    sb.append("# OSGi Extension Declarations File: ").append(relPath).append("\n\n").append(content);
                }
            }
            case "TYCHO", "MAVEN" -> {
                sb.append("# Tycho/Maven Build Configuration: ").append(relPath).append("\n\n").append(content);
            }
            case "DOCUMENTATION", "MARKDOWN" -> {
                sb.append("# Documentation Document: ").append(relPath).append("\n\n").append(content);
            }
            case "PDF" -> {
                sb.append("# Extracted PDF Document Content: ").append(relPath).append("\n\n").append(content);
            }
            default -> sb.append(content);
        }

        return sb.toString();
    }

    private static class JavaMetadata {
        String packageName = "";
        String className = "";
        String superClass = "";
        List<String> interfaces = new ArrayList<>();
        List<String> methods = new ArrayList<>();
    }

    private JavaMetadata analyzeJavaSource(String content) {
        JavaMetadata jm = new JavaMetadata();
        String[] lines = content.split("\\r?\\n");

        for (String line : lines) {
            Matcher pkgM = PACKAGE_PATTERN.matcher(line);
            if (pkgM.find()) {
                jm.packageName = pkgM.group(1);
            }

            Matcher classM = CLASS_PATTERN.matcher(line);
            if (classM.find()) {
                if (jm.className.isEmpty()) {
                    jm.className = classM.group(3);
                    if (classM.group(4) != null) jm.superClass = classM.group(4).trim();
                    if (classM.group(5) != null) {
                        String[] ifaces = classM.group(5).split(",");
                        for (String iface : ifaces) {
                            if (!iface.trim().isEmpty()) jm.interfaces.add(iface.trim());
                        }
                    }
                }
            }

            Matcher methodM = METHOD_PATTERN.matcher(line);
            if (methodM.find()) {
                String methodName = methodM.group(2);
                if (!methodName.equals("if") && !methodName.equals("for") && !methodName.equals("while") && !methodName.equals("switch")) {
                    jm.methods.add(methodName);
                }
            }
        }
        return jm;
    }

    private static class OsgiManifestOm {
        String symbolicName = "";
        String version = "";
        List<String> requireBundle = new ArrayList<>();
    }

    private OsgiManifestOm parseManifest(String content) {
        OsgiManifestOm om = new OsgiManifestOm();
        Matcher nameM = BUNDLE_SYMBOLIC_NAME_PATTERN.matcher(content);
        if (nameM.find()) om.symbolicName = nameM.group(1).trim();

        Matcher verM = BUNDLE_VERSION_PATTERN.matcher(content);
        if (verM.find()) om.version = verM.group(1).trim();

        Matcher reqM = REQUIRE_BUNDLE_PATTERN.matcher(content);
        if (reqM.find()) {
            String[] reqs = reqM.group(1).split(",");
            for (String r : reqs) {
                String trimmed = r.replaceAll(";.*", "").trim();
                if (!trimmed.isEmpty()) om.requireBundle.add(trimmed);
            }
        }
        return om;
    }

    private String extractPdfText(byte[] bytes) {
        String rawContent = new String(bytes, StandardCharsets.ISO_8859_1);
        StringBuilder sb = new StringBuilder();

        int btIndex = rawContent.indexOf("BT");
        while (btIndex != -1) {
            int etIndex = rawContent.indexOf("ET", btIndex);
            if (etIndex == -1) break;

            String block = rawContent.substring(btIndex + 2, etIndex);
            Matcher tjMatcher = Pattern.compile("\\(([^)]+)\\)\\s*Tj").matcher(block);
            while (tjMatcher.find()) {
                sb.append(tjMatcher.group(1)).append(" ");
            }
            sb.append("\n");
            btIndex = rawContent.indexOf("BT", etIndex + 2);
        }

        String result = sb.toString().trim();
        if (result.length() < 10) {
            // Fallback clean printable text
            StringBuilder fallback = new StringBuilder();
            for (byte b : bytes) {
                if ((b >= 32 && b <= 126) || b == 10 || b == 13 || b == 9) {
                    fallback.append((char) (b & 0xFF));
                } else {
                    fallback.append(' ');
                }
            }
            result = fallback.toString().replaceAll("\\s+", " ").trim();
        }

        return result;
    }

    private static class InstructionPair {
        String sourcePath;
        String question;
        String answer;
    }

    private String computeSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(text.hashCode());
        }
    }
}
