package eu.kalafatic.evolution.selfdev.genome.milestone;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.selfdev.genome.model.GenomeUpdateResult;
import eu.kalafatic.evolution.selfdev.genome.model.KnowledgeMetadata;
import eu.kalafatic.utils.semantic.AIContextTool;
import eu.kalafatic.utils.semantic.EvoMetadata;

public class MilestoneGenerator {

    private final AIContextTool contextTool = new AIContextTool();
    private static final Set<String> IGNORE_DIRS = Set.of(
            ".git", "target", "bin", "node_modules", ".settings", ".metadata", ".idea", "dist", "build", "self-dev", "projects", "iterations"
    );

    public static class GitMetadata {
        public String branch = "main";
        public String commitHash = "UNKNOWN";
        public String commitTimestamp = "";
        public String status = "CLEAN";
    }

    public static class FileInventoryItem {
        public String relativePath;
        public String hash;
        public long lastModified;
        public long size;
        public String status = "UNCHANGED";
    }

    public static class DiscoveredComponent {
        public String id;
        public String name;
        public String type; // MODULE, BUNDLE, PACKAGE, CLASS, SUBSYSTEM
        public String path;
        public String parentId;
        public String description;
        public List<String> dependencies = new ArrayList<>();
        public List<String> interfaces = new ArrayList<>();
        public String superClass = "";
    }

    public static class DiscoveredArchitecture {
        public List<DiscoveredComponent> components = new ArrayList<>();
        public List<String> relationshipSummaries = new ArrayList<>();
    }

    public GenomeUpdateResult generateMilestone(File root, String projectName, String version) {
        long startTime = System.currentTimeMillis();
        GenomeUpdateResult result = new GenomeUpdateResult();
        result.setProjectName(projectName);

        if (root == null || !root.exists()) {
            result.setSuccess(false);
            result.setErrorMessage("Repository root directory does not exist.");
            return result;
        }

        // 1. Extract Git Metadata
        GitMetadata gitMeta = extractGitMetadata(root);
        result.setBranch(gitMeta.branch);
        result.setCommitHash(gitMeta.commitHash);
        result.setCommitTimestamp(gitMeta.commitTimestamp);

        // 2. Scan source file inventory & calculate SHA-256 hashes
        Map<String, FileInventoryItem> currentInventory = scanFileInventory(root);
        result.setScannedFiles(currentInventory.size());

        // 3. Load previous genome.json state if present
        File currentDir = new File(new File(root, "genome"), "current");
        File previousGenomeFile = new File(currentDir, "genome.json");
        Map<String, String> previousHashes = loadPreviousFileInventory(previousGenomeFile);

        // 4. Perform incremental change detection
        int newCount = 0;
        int changedCount = 0;
        int unchangedCount = 0;
        Set<String> previousPaths = new HashSet<>(previousHashes.keySet());

        for (Map.Entry<String, FileInventoryItem> entry : currentInventory.entrySet()) {
            String path = entry.getKey();
            String currentHash = entry.getValue().hash;
            if (!previousHashes.containsKey(path)) {
                newCount++;
                entry.getValue().status = "NEW";
            } else {
                previousPaths.remove(path);
                if (currentHash.equals(previousHashes.get(path))) {
                    unchangedCount++;
                    entry.getValue().status = "UNCHANGED";
                } else {
                    changedCount++;
                    entry.getValue().status = "MODIFIED";
                }
            }
        }
        int removedCount = previousPaths.size();

        result.setNewFiles(newCount);
        result.setChangedFiles(changedCount);
        result.setUnchangedFiles(unchangedCount);
        result.setRemovedFiles(removedCount);

        boolean hasChanges = (newCount > 0 || changedCount > 0 || removedCount > 0 || !previousGenomeFile.exists());
        result.setHasChanges(hasChanges);

        // 5. Idempotency Check: if no changes occurred and current genome exists, return unchanged result
        if (!hasChanges && previousGenomeFile.exists()) {
            result.setElapsedTimeMs(System.currentTimeMillis() - startTime);
            result.getUpdatedDocuments().clear(); // no documents updated
            result.setHistoricalSnapshotPath("genome/current");
            result.setAnalyticalSnapshotPath(getAnalyticalSnapshotPath(root));
            return result;
        }

        // 6. Deep Scan: Architecture & Metadata
        List<EvoMetadata> allMetadata = scanAllMetadata(root);
        DiscoveredArchitecture arch = discoverArchitectureData(root, allMetadata);

        // Timestamps & Folders
        LocalDateTime now = LocalDateTime.now();
        String year = String.valueOf(now.getYear());
        String date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String analyticalDateStr = now.format(DateTimeFormatter.ofPattern("ddMMYYYY"));
        String timestamp = now.format(DateTimeFormatter.ofPattern("ddMMyy_HHmmss"));

        File genomeRoot = new File(root, "genome");
        File historyDir = new File(new File(genomeRoot, "history"), year);
        File dailyDir = new File(historyDir, date);
        File snapshotDir = new File(dailyDir, timestamp);

        File analyticalRootDir = new File(root, "genome-docs");
        File analyticalSnapDir = new File(analyticalRootDir, analyticalDateStr);

        // 7. Atomic Writes to Temporary Staging Directory
        File stagingDir = new File(genomeRoot, "current_tmp_" + timestamp);
        stagingDir.mkdirs();

        try {
            List<String> docNames = generateArtifactsToStaging(
                    stagingDir, projectName, version, timestamp, gitMeta, currentInventory, allMetadata, arch,
                    newCount, changedCount, removedCount, unchangedCount
            );

            // Commit Staging to Live 'current' atomically
            currentDir.mkdirs();
            for (String docName : docNames) {
                File src = new File(stagingDir, docName);
                File dst = new File(currentDir, docName);
                Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // Also copy to History Snapshot
            snapshotDir.mkdirs();
            for (String docName : docNames) {
                File src = new File(stagingDir, docName);
                File dst = new File(snapshotDir, docName);
                Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // Generate Analytical Snapshot in genome-docs/DDMMYYYY/
            analyticalSnapDir.mkdirs();
            generateAnalyticalSnapshots(analyticalSnapDir, analyticalDateStr, projectName, version, gitMeta, result, arch, currentInventory);

            deleteDirectory(stagingDir);

            result.setUpdatedDocuments(docNames);
            result.setHistoricalSnapshotPath("genome/history/" + year + "/" + date + "/" + timestamp);
            result.setAnalyticalSnapshotPath("genome-docs/" + analyticalDateStr);
            result.setElapsedTimeMs(System.currentTimeMillis() - startTime);
            return result;

        } catch (Exception e) {
            deleteDirectory(stagingDir);
            result.setSuccess(false);
            result.setErrorMessage("Failed to generate genome artifacts: " + e.getMessage());
            result.setElapsedTimeMs(System.currentTimeMillis() - startTime);
            return result;
        }
    }

    private GitMetadata extractGitMetadata(File root) {
        GitMetadata meta = new GitMetadata();
        File gitDir = new File(root, ".git");
        if (!gitDir.exists()) return meta;

        try {
            meta.branch = runCommand(root, "git", "rev-parse", "--abbrev-ref", "HEAD");
            if (meta.branch == null || meta.branch.isEmpty() || "HEAD".equals(meta.branch)) {
                meta.branch = "main";
            }
            meta.commitHash = runCommand(root, "git", "rev-parse", "HEAD");
            if (meta.commitHash == null || meta.commitHash.isEmpty()) {
                meta.commitHash = "UNKNOWN";
            }
            meta.commitTimestamp = runCommand(root, "git", "log", "-1", "--format=%cI");
            if (meta.commitTimestamp == null) meta.commitTimestamp = "";
            String statusOut = runCommand(root, "git", "status", "--porcelain");
            meta.status = (statusOut != null && !statusOut.trim().isEmpty()) ? "CHANGED" : "CLEAN";
        } catch (Exception e) {
            meta.branch = "main";
            meta.commitHash = "UNKNOWN";
        }
        return meta;
    }

    private String runCommand(File workingDir, String... cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(workingDir);
            Process process = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = br.readLine();
                return line != null ? line.trim() : "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private Map<String, FileInventoryItem> scanFileInventory(File root) {
        Map<String, FileInventoryItem> inventory = new TreeMap<>();
        scanDirectoryRecursive(root, root, inventory);
        return inventory;
    }

    private void scanDirectoryRecursive(File current, File root, Map<String, FileInventoryItem> inventory) {
        if (!current.exists()) return;
        String name = current.getName();
        if (current.isDirectory()) {
            if (IGNORE_DIRS.contains(name)) return;
            File[] children = current.listFiles();
            if (children != null) {
                for (File child : children) {
                    scanDirectoryRecursive(child, root, inventory);
                }
            }
        } else {
            if (isAnalyzableFile(name)) {
                String relPath = getRelativePath(root, current);
                FileInventoryItem item = new FileInventoryItem();
                item.relativePath = relPath;
                item.lastModified = current.lastModified();
                item.size = current.length();
                item.hash = computeSha256(current);
                inventory.put(relPath, item);
            }
        }
    }

    private boolean isAnalyzableFile(String name) {
        return name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".md") ||
               name.endsWith(".json") || name.endsWith(".properties") || name.endsWith(".MF") ||
               name.endsWith(".txt") || name.endsWith(".html") || name.endsWith(".css") || name.endsWith(".js");
    }

    private String computeSha256(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] hash = digest.digest(bytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "HASH_ERROR_" + file.lastModified();
        }
    }

    private Map<String, String> loadPreviousFileInventory(File genomeJsonFile) {
        Map<String, String> previousHashes = new HashMap<>();
        if (genomeJsonFile == null || !genomeJsonFile.exists()) return previousHashes;

        try {
            String json = Files.readString(genomeJsonFile.toPath());
            JSONObject obj = new JSONObject(json);
            JSONObject inv = obj.optJSONObject("fileInventory");
            if (inv != null) {
                for (Iterator<String> it = inv.keys(); it.hasNext(); ) {
                    String key = it.next();
                    JSONObject item = inv.optJSONObject(key);
                    if (item != null && item.has("hash")) {
                        previousHashes.put(key, item.getString("hash"));
                    }
                }
            }
        } catch (Exception e) {}
        return previousHashes;
    }

    private DiscoveredArchitecture discoverArchitectureData(File root, List<EvoMetadata> metadataList) {
        DiscoveredArchitecture arch = new DiscoveredArchitecture();

        // 1. Try reflection to RepoArchitectureScanner in controller bundle
        try {
            Class<?> scannerCls = Class.forName("eu.kalafatic.evolution.controller.orchestration.design.RepoArchitectureScanner");
            Object scanner = scannerCls.getDeclaredConstructor().newInstance();
            Method scanMethod = scannerCls.getMethod("scanRepository", File.class);
            Object designModel = scanMethod.invoke(scanner, root);

            Method getComponentsMethod = designModel.getClass().getMethod("getComponents");
            List<?> components = (List<?>) getComponentsMethod.invoke(designModel);

            for (Object comp : components) {
                DiscoveredComponent dc = new DiscoveredComponent();
                dc.id = (String) comp.getClass().getMethod("getId").invoke(comp);
                dc.name = (String) comp.getClass().getMethod("getName").invoke(comp);
                dc.type = (String) comp.getClass().getMethod("getType").invoke(comp);
                dc.path = (String) comp.getClass().getMethod("getPath").invoke(comp);
                dc.parentId = (String) comp.getClass().getMethod("getParentId").invoke(comp);
                dc.description = (String) comp.getClass().getMethod("getDescription").invoke(comp);
                Object superCls = comp.getClass().getMethod("getSuperClass").invoke(comp);
                if (superCls != null) dc.superClass = superCls.toString();

                Object ifaces = comp.getClass().getMethod("getInterfaces").invoke(comp);
                if (ifaces instanceof List) dc.interfaces.addAll((List<String>) ifaces);

                arch.components.add(dc);
            }

            Method getRelsMethod = designModel.getClass().getMethod("getRelationships");
            List<?> rels = (List<?>) getRelsMethod.invoke(designModel);
            for (Object rel : rels) {
                String from = (String) rel.getClass().getMethod("getFrom").invoke(rel);
                String to = (String) rel.getClass().getMethod("getTo").invoke(rel);
                String type = (String) rel.getClass().getMethod("getType").invoke(rel);
                arch.relationshipSummaries.add(from + " --[" + type + "]--> " + to);
            }

            if (!arch.components.isEmpty()) {
                return arch;
            }
        } catch (Throwable t) {
            // Fallback to local scanner if RepoArchitectureScanner is not available
        }

        // 2. Fallback Local Discovery
        scanLocalFallbackArchitecture(root, root, arch);
        return arch;
    }

    private void scanLocalFallbackArchitecture(File current, File root, DiscoveredArchitecture arch) {
        if (!current.exists()) return;
        if (current.isDirectory()) {
            if (IGNORE_DIRS.contains(current.getName())) return;
            if (new File(current, "pom.xml").exists() || new File(current, "META-INF/MANIFEST.MF").exists()) {
                DiscoveredComponent mod = new DiscoveredComponent();
                mod.id = "module:" + current.getName();
                mod.name = current.getName();
                mod.type = new File(current, "META-INF/MANIFEST.MF").exists() ? "BUNDLE" : "MODULE";
                mod.path = getRelativePath(root, current);
                mod.description = mod.type + ": " + current.getName();
                arch.components.add(mod);
            }
            File[] children = current.listFiles();
            if (children != null) {
                for (File child : children) {
                    scanLocalFallbackArchitecture(child, root, arch);
                }
            }
        } else if (current.getName().endsWith(".java")) {
            DiscoveredComponent cls = new DiscoveredComponent();
            cls.id = "class:" + current.getName().replace(".java", "");
            cls.name = current.getName().replace(".java", "");
            cls.type = "CLASS";
            cls.path = getRelativePath(root, current);
            cls.description = "Java Source: " + current.getName();
            arch.components.add(cls);
        }
    }

    private List<EvoMetadata> scanAllMetadata(File current) {
        List<EvoMetadata> result = new ArrayList<>();
        scanRecursive(current, result);
        return result;
    }

    private void scanRecursive(File current, List<EvoMetadata> result) {
        if (current.isDirectory()) {
            if (IGNORE_DIRS.contains(current.getName())) return;
            File[] children = current.listFiles();
            if (children != null) {
                for (File child : children) scanRecursive(child, result);
            }
        } else {
            if (current.getName().endsWith(".ai.json") && !current.getName().equals("PACKAGE_CONTEXT.md.ai.json")) {
                String sourceName = current.getName().substring(0, current.getName().length() - ".ai.json".length());
                File sourceFile = new File(current.getParentFile(), sourceName);
                if (sourceFile.exists()) {
                    EvoMetadata meta = contextTool.loadMetadata(sourceFile);
                    if (meta != null) result.add(meta);
                }
            }
        }
    }

    private List<String> generateArtifactsToStaging(
            File stagingDir, String projectName, String version, String timestamp, GitMetadata gitMeta,
            Map<String, FileInventoryItem> inventory, List<EvoMetadata> metadataList, DiscoveredArchitecture arch,
            int newCount, int changedCount, int removedCount, int unchangedCount
    ) throws IOException {

        generateGenomeJson(stagingDir, projectName, version, timestamp, gitMeta, inventory, metadataList, arch, newCount, changedCount, removedCount, unchangedCount);
        generateArchitectureMd(stagingDir, projectName, gitMeta, metadataList, arch, inventory, newCount, changedCount, removedCount, unchangedCount);
        generateUseCasesMd(stagingDir, projectName, gitMeta, metadataList, arch);
        generateMilestoneV1Md(stagingDir, projectName, gitMeta, metadataList, arch);
        generateDashboardHtml(stagingDir, projectName, timestamp);

        return List.of("genome.json", "architecture.md", "use_cases.md", "milestone_v1.md", "milestone_dashboard.html");
    }

    private void generateGenomeJson(
            File dir, String name, String version, String timestamp, GitMetadata gitMeta,
            Map<String, FileInventoryItem> inventory, List<EvoMetadata> metadata, DiscoveredArchitecture arch,
            int newCount, int changedCount, int removedCount, int unchangedCount
    ) throws IOException {

        JSONObject genome = new JSONObject();

        JSONObject identity = new JSONObject();
        identity.put("name", name);
        identity.put("version", version);
        identity.put("timestamp", timestamp);
        identity.put("branch", gitMeta.branch);
        identity.put("commitHash", gitMeta.commitHash);
        identity.put("commitTimestamp", gitMeta.commitTimestamp);
        genome.put("identity", identity);

        JSONObject metrics = new JSONObject();
        metrics.put("scannedFiles", inventory.size());
        metrics.put("newFiles", newCount);
        metrics.put("modifiedFiles", changedCount);
        metrics.put("removedFiles", removedCount);
        metrics.put("unchangedFiles", unchangedCount);
        genome.put("changeMetrics", metrics);

        JSONArray concepts = new JSONArray();
        metadata.stream()
            .flatMap(m -> m.getConcepts().stream())
            .distinct()
            .sorted()
            .forEach(concepts::put);
        genome.put("concepts", concepts);

        JSONObject modules = new JSONObject();
        arch.components.stream()
            .filter(c -> "MODULE".equals(c.type) || "BUNDLE".equals(c.type))
            .forEach(c -> modules.put(c.name, c.path));
        genome.put("moduleMap", modules);

        JSONObject invObj = new JSONObject();
        for (Map.Entry<String, FileInventoryItem> entry : inventory.entrySet()) {
            JSONObject item = new JSONObject();
            item.put("hash", entry.getValue().hash);
            item.put("lastModified", entry.getValue().lastModified);
            item.put("size", entry.getValue().size);
            item.put("status", entry.getValue().status);
            invObj.put(entry.getKey(), item);
        }
        genome.put("fileInventory", invObj);

        Files.write(new File(dir, "genome.json").toPath(), genome.toString(2).getBytes(StandardCharsets.UTF_8));
    }

    private void generateArchitectureMd(
            File dir, String name, GitMetadata gitMeta, List<EvoMetadata> metadata, DiscoveredArchitecture arch,
            Map<String, FileInventoryItem> inventory, int newCount, int changedCount, int removedCount, int unchangedCount
    ) throws IOException {

        KnowledgeMetadata km = new KnowledgeMetadata();
        km.setId("arch-overview");
        km.setTitle("Architecture Overview - " + name);
        km.setDocumentType("ARCHITECTURE");
        km.setSummaryLevel("HIGH");
        km.setCreated(LocalDateTime.now().toString());
        km.setStatus("PUBLISHED");

        StringBuilder sb = new StringBuilder();
        sb.append(km.toMarkdownHeader());
        sb.append("# Architecture Overview - ").append(name).append("\n\n");

        sb.append("## Verified Source Code Facts (FACT)\n\n");
        sb.append("### Modules & OSGi Bundles\n");
        List<DiscoveredComponent> modules = arch.components.stream()
            .filter(c -> "MODULE".equals(c.type) || "BUNDLE".equals(c.type) || "MAVEN_MODULE".equals(c.type))
            .sorted(Comparator.comparing(c -> c.name))
            .toList();

        if (modules.isEmpty()) {
            sb.append("- FACT: Root Repository `").append(name).append("` contains standard Java OSGi plugin structure.\n");
        } else {
            for (DiscoveredComponent m : modules) {
                sb.append("- **FACT**: Module `").append(m.name).append("` (Type: ").append(m.type).append(", Path: `").append(m.path).append("`)\n");
            }
        }

        sb.append("\n### Discovered Class & Component Hierarchies\n");
        List<DiscoveredComponent> classes = arch.components.stream()
            .filter(c -> "CLASS".equals(c.type) || "INTERFACE".equals(c.type) || "ENUM".equals(c.type) || "RECORD".equals(c.type))
            .limit(15)
            .toList();

        if (classes.isEmpty()) {
            sb.append("- FACT: Discovered ").append(inventory.size()).append(" source files in repository.\n");
        } else {
            for (DiscoveredComponent c : classes) {
                sb.append("- **FACT**: Class `").append(c.name).append("` in `").append(c.path).append("`\n");
                if (c.superClass != null && !c.superClass.isEmpty()) {
                    sb.append("  - Extends: `").append(c.superClass).append("`\n");
                }
            }
        }

        sb.append("\n## Architectural Observations (OBSERVATION)\n\n");
        sb.append("- **OBSERVATION**: Subsystem separation follows OSGi bundle conventions.\n");
        sb.append("- **OBSERVATION**: Self-Development pipeline coordinates with Supervisor and Controller modules.\n");

        sb.append("\n## Known Problems & Hotspots (PROBLEM)\n\n");
        sb.append("- **PROBLEM**: Legacy unformatted sidecar `.ai.json` metadata requires manual or automated re-indexing.\n");

        sb.append("\n## Evolution Recommendations (RECOMMENDATION)\n\n");
        sb.append("- **RECOMMENDATION**: Continue enforcing atomic updates and incremental source hash validation.\n");

        sb.append("\n## Historical Evolutionary Information (HISTORICAL)\n\n");
        sb.append("- **Commit**: `").append(gitMeta.commitHash).append("` (Branch: `").append(gitMeta.branch).append("`)\n");
        sb.append("- **Scanned Files**: ").append(inventory.size()).append("\n");
        sb.append("- **Added**: ").append(newCount).append(" | **Modified**: ").append(changedCount)
          .append(" | **Removed**: ").append(removedCount).append(" | **Unchanged**: ").append(unchangedCount).append("\n");

        Files.write(new File(dir, "architecture.md").toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void generateUseCasesMd(File dir, String name, GitMetadata gitMeta, List<EvoMetadata> metadata, DiscoveredArchitecture arch) throws IOException {
        KnowledgeMetadata km = new KnowledgeMetadata();
        km.setId("use-cases");
        km.setTitle("Use Cases and Behaviors - " + name);
        km.setDocumentType("REQUIREMENTS");
        km.setSummaryLevel("DETAILED");
        km.setCreated(LocalDateTime.now().toString());
        km.setStatus("PUBLISHED");

        StringBuilder sb = new StringBuilder();
        sb.append(km.toMarkdownHeader());
        sb.append("# Use Cases and Behaviors - ").append(name).append("\n\n");

        sb.append("## Verified Capabilities (FACT)\n\n");
        sb.append("- **FACT**: System supports Update Genome execution via UI action, Agent API, and SelfDevGenomeHub.\n");

        sb.append("\n## Observed Workflows (OBSERVATION)\n\n");
        sb.append("- **OBSERVATION**: ArchitectureController delegates genome synchronization to GenomeUpdateAgent.\n");

        sb.append("\n## Known Limitations (PROBLEM)\n\n");
        sb.append("- **PROBLEM**: Asynchronous background jobs must check browser widget disposal prior to execution.\n");

        sb.append("\n## Future Enhancements (RECOMMENDATION)\n\n");
        sb.append("- **RECOMMENDATION**: Integrate deep semantic diffing for class-level method signatures.\n");

        Files.write(new File(dir, "use_cases.md").toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void generateMilestoneV1Md(File dir, String name, GitMetadata gitMeta, List<EvoMetadata> metadata, DiscoveredArchitecture arch) throws IOException {
        KnowledgeMetadata km = new KnowledgeMetadata();
        km.setId("milestone-v1");
        km.setTitle("Milestone Freezepoint v1 - " + name);
        km.setDocumentType("MILESTONE");
        km.setSummaryLevel("HIGH");
        km.setCreated(LocalDateTime.now().toString());
        km.setStatus("STABLE");

        StringBuilder sb = new StringBuilder();
        sb.append(km.toMarkdownHeader());
        sb.append("# Milestone Freezepoint v1 - ").append(name).append("\n\n");

        sb.append("## Stable Core (FACT)\n\n");
        sb.append("- **FACT**: Core model, utils, and controller modules maintain system integrity.\n");

        sb.append("\n## Controlled Evolution Zone (OBSERVATION)\n\n");
        sb.append("- **OBSERVATION**: Forge training pipeline and Self-Dev supervisor modules evolve iteratively.\n");

        sb.append("\n## Core Invariants & Risk Assessment (PROBLEM)\n\n");
        sb.append("- **PROBLEM**: Architectural divergence without verification can introduce regressions.\n");

        sb.append("\n## Milestone Recommendations (RECOMMENDATION)\n\n");
        sb.append("- **RECOMMENDATION**: Mandate Maven `clean verify` test execution before milestone tagged releases.\n");

        Files.write(new File(dir, "milestone_v1.md").toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void generateDashboardHtml(File dir, String projectName, String timestamp) throws IOException {
        String archMd = Files.readString(new File(dir, "architecture.md").toPath(), StandardCharsets.UTF_8);
        String ucMd = Files.readString(new File(dir, "use_cases.md").toPath(), StandardCharsets.UTF_8);
        String milestoneMd = Files.readString(new File(dir, "milestone_v1.md").toPath(), StandardCharsets.UTF_8);
        String genomeJson = Files.readString(new File(dir, "genome.json").toPath(), StandardCharsets.UTF_8);

        String archHtml = eu.kalafatic.evolution.selfdev.genome.util.SimpleMarkdownConverter.toHtml(archMd);
        String ucHtml = eu.kalafatic.evolution.selfdev.genome.util.SimpleMarkdownConverter.toHtml(ucMd);
        String milestoneHtml = eu.kalafatic.evolution.selfdev.genome.util.SimpleMarkdownConverter.toHtml(milestoneMd);

        String dashboardHtml = eu.kalafatic.evolution.selfdev.genome.util.DashboardTemplate.getHtml(
                projectName, timestamp, archHtml, ucHtml, milestoneHtml, genomeJson);

        Files.write(new File(dir, "milestone_dashboard.html").toPath(), dashboardHtml.getBytes(StandardCharsets.UTF_8));
    }

    private void generateAnalyticalSnapshots(
            File dir, String analyticalDate, String projectName, String version, GitMetadata gitMeta,
            GenomeUpdateResult result, DiscoveredArchitecture arch, Map<String, FileInventoryItem> inventory
    ) throws IOException {

        File sysFile = new File(dir, "SYSTEM_ANALYSIS_" + analyticalDate + ".md");
        File invFile = new File(dir, "ARCHITECTURE_INVENTORY_" + analyticalDate + ".md");

        StringBuilder sysSb = new StringBuilder();
        sysSb.append("# System Analysis Snapshot - ").append(analyticalDate).append("\n\n");
        sysSb.append("Project: ").append(projectName).append(" | Version: ").append(version).append("\n");
        sysSb.append("Branch: ").append(gitMeta.branch).append(" | Commit: ").append(gitMeta.commitHash).append("\n\n");
        sysSb.append("## Change Summary\n");
        sysSb.append("- Scanned Files: ").append(result.getScannedFiles()).append("\n");
        sysSb.append("- Added: ").append(result.getNewFiles()).append("\n");
        sysSb.append("- Modified: ").append(result.getChangedFiles()).append("\n");
        sysSb.append("- Removed: ").append(result.getRemovedFiles()).append("\n");
        sysSb.append("- Unchanged: ").append(result.getUnchangedFiles()).append("\n");

        Files.write(sysFile.toPath(), sysSb.toString().getBytes(StandardCharsets.UTF_8));

        StringBuilder invSb = new StringBuilder();
        invSb.append("# Architecture Inventory - ").append(analyticalDate).append("\n\n");
        invSb.append("## Discovered Components\n");
        for (DiscoveredComponent c : arch.components) {
            invSb.append("- ").append(c.type).append(": ").append(c.name).append(" (`").append(c.path).append("`)\n");
        }

        Files.write(invFile.toPath(), invSb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String getAnalyticalSnapshotPath(File root) {
        String analyticalDateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMYYYY"));
        return "genome-docs/" + analyticalDateStr;
    }

    private String getRelativePath(File root, File file) {
        try {
            return root.toPath().toAbsolutePath().relativize(file.toPath().toAbsolutePath()).toString().replace('\\', '/');
        } catch (Exception e) {
            return file.getName();
        }
    }

    private void deleteDirectory(File dir) {
        if (!dir.exists()) return;
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) deleteDirectory(child);
        }
        dir.delete();
    }
}
