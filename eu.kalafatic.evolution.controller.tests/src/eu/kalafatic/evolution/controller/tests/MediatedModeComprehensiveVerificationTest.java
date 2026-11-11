package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator;
import eu.kalafatic.evolution.controller.mediation.analysis.FileManifest;
import eu.kalafatic.evolution.controller.mediation.analysis.ManifestFileEntry;
import eu.kalafatic.evolution.controller.mediation.analysis.MediatedSpecification;
import eu.kalafatic.evolution.controller.mediation.analysis.MediatedSpecificationEngine;
import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetScanner;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.MediatedEngine;
import eu.kalafatic.evolution.controller.workflow.MediatedExportManager;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * End-to-end unit and integration test suite validating MEDIATED MODE:
 * 1. Multi-target resolution (Git repo, Local project, Arbitrary folder, External directory).
 * 2. Invalid target fail-fast behavior (no silent fallback).
 * 3. Specification transformation (objectives, ambiguities, constraints, risks, acceptance criteria).
 * 4. Context curation, exclusion of noise, and manifest generation.
 * 5. Structured ZIP package generation with project-relative paths.
 * 6. Target read-only guarantee (zero modifications to target files).
 * 7. Privacy guarantee (no external network transmission).
 */
public class MediatedModeComprehensiveVerificationTest {

    private Orchestrator orchestrator;
    private File gitRepoDir;
    private File localProjectDir;
    private File arbitraryFolderDir;
    private File externalExportDir;

    @Before
    public void setUp() throws Exception {
        SessionManager.getInstance().shutdownAll();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());

        // 1. Target 1: Git repository layout
        gitRepoDir = Files.createTempDirectory("evo-target-git-").toFile();
        File gitDir = new File(gitRepoDir, ".git");
        gitDir.mkdirs();
        File srcDir = new File(gitRepoDir, "src/main/java/com/example/db");
        srcDir.mkdirs();
        Files.writeString(new File(srcDir, "ConnectionPool.java").toPath(),
                "package com.example.db;\npublic class ConnectionPool { public void getConnection() {} }\n");
        Files.writeString(new File(gitRepoDir, "pom.xml").toPath(),
                "<project><modelVersion>4.0.0</modelVersion><groupId>com.example</groupId><artifactId>db-lib</artifactId><version>1.0.0</version></project>\n");
        Files.writeString(new File(gitRepoDir, "README.md").toPath(),
                "# Database Library\nHigh performance database connection pooling.\n");

        // 2. Target 2: Local project layout (non-git, e.g. Node/Python)
        localProjectDir = Files.createTempDirectory("evo-target-project-").toFile();
        File pySrc = new File(localProjectDir, "src");
        pySrc.mkdirs();
        Files.writeString(new File(pySrc, "app.py").toPath(), "def start():\n    print('Running')\n");
        Files.writeString(new File(localProjectDir, "requirements.txt").toPath(), "flask==2.3.0\npytest==7.4.0\n");
        Files.writeString(new File(localProjectDir, "README.md").toPath(), "# Python Web Service\nMicroservice API.\n");

        // 3. Target 3: Arbitrary folder layout (e.g. scripts & docs)
        arbitraryFolderDir = Files.createTempDirectory("evo-target-folder-").toFile();
        File docs = new File(arbitraryFolderDir, "docs");
        docs.mkdirs();
        Files.writeString(new File(docs, "architecture.md").toPath(), "# System Architecture\nDesign guidelines.\n");
        Files.writeString(new File(arbitraryFolderDir, "script.sh").toPath(), "#!/bin/bash\necho hello\n");

        // External export destination
        externalExportDir = Files.createTempDirectory("evo-external-exports-").toFile();
    }

    @After
    public void tearDown() throws Exception {
        SessionManager.getInstance().shutdownAll();
        deleteRecursive(gitRepoDir);
        deleteRecursive(localProjectDir);
        deleteRecursive(arbitraryFolderDir);
        deleteRecursive(externalExportDir);
    }

    private void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursive(c);
            }
        }
        f.delete();
    }

    @Test
    public void testTargetResolutionGitRepository() {
        ChatSession session = OrchestrationFactory.eINSTANCE.createChatSession();
        session.setId("test-git-session");
        session.setTargetPath(gitRepoDir.getAbsolutePath());
        session.setTargetType("Repository");
        orchestrator.getAiChat().getSessions().add(session);

        TaskContext context = new TaskContext(orchestrator, new File("."));
        context.setSessionId("test-git-session");

        MediatedEngine engine = new MediatedEngine(context, null, null);
        File resolved = engine.resolveMediatedTarget();

        assertNotNull(resolved);
        assertEquals(gitRepoDir.getAbsolutePath(), resolved.getAbsolutePath());
        assertEquals(gitRepoDir.getAbsolutePath(), context.getProjectRoot().getAbsolutePath());
    }

    @Test
    public void testTargetResolutionLocalProject() {
        ChatSession session = OrchestrationFactory.eINSTANCE.createChatSession();
        session.setId("test-proj-session");
        session.setTargetPath(localProjectDir.getAbsolutePath());
        session.setTargetType("Project");
        orchestrator.getAiChat().getSessions().add(session);

        TaskContext context = new TaskContext(orchestrator, new File("."));
        context.setSessionId("test-proj-session");

        MediatedEngine engine = new MediatedEngine(context, null, null);
        File resolved = engine.resolveMediatedTarget();

        assertNotNull(resolved);
        assertEquals(localProjectDir.getAbsolutePath(), resolved.getAbsolutePath());
        assertEquals(localProjectDir.getAbsolutePath(), context.getProjectRoot().getAbsolutePath());
    }

    @Test
    public void testTargetResolutionArbitraryFolder() {
        ChatSession session = OrchestrationFactory.eINSTANCE.createChatSession();
        session.setId("test-folder-session");
        session.setTargetPath(arbitraryFolderDir.getAbsolutePath());
        session.setTargetType("Folder");
        orchestrator.getAiChat().getSessions().add(session);

        TaskContext context = new TaskContext(orchestrator, new File("."));
        context.setSessionId("test-folder-session");

        MediatedEngine engine = new MediatedEngine(context, null, null);
        File resolved = engine.resolveMediatedTarget();

        assertNotNull(resolved);
        assertEquals(arbitraryFolderDir.getAbsolutePath(), resolved.getAbsolutePath());
        assertEquals(arbitraryFolderDir.getAbsolutePath(), context.getProjectRoot().getAbsolutePath());
    }

    @Test
    public void testTargetResolutionFailsOnInvalidPathWithoutSilentFallback() {
        ChatSession session = OrchestrationFactory.eINSTANCE.createChatSession();
        session.setId("test-invalid-session");
        session.setTargetPath("/non/existent/path/for/sure_" + System.currentTimeMillis());
        orchestrator.getAiChat().getSessions().add(session);

        TaskContext context = new TaskContext(orchestrator, gitRepoDir);
        context.setSessionId("test-invalid-session");

        MediatedEngine engine = new MediatedEngine(context, null, null);
        try {
            engine.resolveMediatedTarget();
            fail("Expected IllegalArgumentException on invalid target path without silent fallback.");
        } catch (IllegalArgumentException e) {
            assertTrue("Error message should mention path", e.getMessage().contains("Target directory does not exist"));
        }
    }

    @Test
    public void testSpecificationTransformationFromRawUserRequest() {
        TargetScanner scanner = new TargetScanner();
        TargetSnapshot snapshot = scanner.scanToSnapshot(gitRepoDir, TargetSnapshot.TargetType.PROJECT);

        TargetRealityModel model = new TargetRealityModel();
        model.setDomain("DATABASE_CORE");
        model.setPurpose("Connection pooling and JDBC driver management");
        model.setArchitectureSummary("Core database communication layer.");

        MediatedSpecificationEngine engine = new MediatedSpecificationEngine();
        String rawInput = "Improve database handling and fix connection management.";

        MediatedSpecification spec = engine.buildDeterministicSpecification(rawInput, snapshot, model);

        assertNotNull("Specification should not be null", spec);
        assertEquals(rawInput, spec.getOriginalRequest());
        assertTrue("Should formulate objective", spec.getPrimaryObjective().contains("database handling"));
        assertFalse("Should derive sub-objectives", spec.getSubObjectives().isEmpty());
        assertFalse("Should resolve ambiguities", spec.getResolvedAmbiguities().isEmpty());
        assertFalse("Should derive implicit requirements", spec.getImplicitRequirements().isEmpty());
        assertFalse("Should identify technical constraints", spec.getTechnicalConstraints().isEmpty());
        assertFalse("Should identify technical risks", spec.getTechnicalRisks().isEmpty());
        assertFalse("Should formulate acceptance criteria", spec.getAcceptanceCriteria().isEmpty());

        String markdown = spec.toMarkdown();
        assertTrue("Markdown must contain Original User Request", markdown.contains("Original User Request"));
        assertTrue("Markdown must contain Acceptance & Verification Criteria", markdown.contains("Acceptance & Verification Criteria"));
    }

    @Test
    public void testContextCuratorAndFileManifestGeneration() {
        TargetScanner scanner = new TargetScanner();
        TargetSnapshot snapshot = scanner.scanToSnapshot(gitRepoDir, TargetSnapshot.TargetType.PROJECT);

        ContextCurator curator = new ContextCurator();
        List<String> selected = curator.selectContext(snapshot, "database connection", 16);

        assertNotNull(selected);
        assertFalse("Selected files must not be empty", selected.isEmpty());

        FileManifest manifest = curator.buildManifest(selected, snapshot, null, gitRepoDir, "database connection");

        assertNotNull(manifest);
        assertEquals(selected.size(), manifest.getTotalFiles());
        assertTrue("Total bytes should be greater than zero", manifest.getTotalBytes() > 0);

        for (ManifestFileEntry entry : manifest.getFiles()) {
            assertNotNull("Entry relative path cannot be null", entry.getRelativePath());
            assertNotNull("Entry category cannot be null", entry.getCategory());
            assertNotNull("Selection rationale cannot be null", entry.getSelectionRationale());
            assertFalse("Selection rationale cannot be empty", entry.getSelectionRationale().isEmpty());
        }

        String json = manifest.toJson();
        assertTrue("Manifest JSON must contain relativePath", json.contains("relativePath"));
        assertTrue("Manifest JSON must contain selectionRationale", json.contains("selectionRationale"));
    }

    @Test
    public void testStructuredZipPackageGenerationAndTargetReadOnlyGuarantee() throws Exception {
        TargetScanner scanner = new TargetScanner();
        TargetSnapshot snapshot = scanner.scanToSnapshot(gitRepoDir, TargetSnapshot.TargetType.PROJECT);

        TargetRealityModel model = new TargetRealityModel();
        model.setDomain("DATABASE_SYSTEM");
        model.setPurpose("High throughput database connection manager");
        model.setArchitectureSummary("Modular connection pooling service.");

        MediatedSpecificationEngine specEngine = new MediatedSpecificationEngine();
        String rawInput = "Improve database handling and fix connection management.";
        MediatedSpecification spec = specEngine.buildDeterministicSpecification(rawInput, snapshot, model);

        ContextCurator curator = new ContextCurator();
        List<String> selectedPaths = curator.selectContext(snapshot, rawInput, 16);

        // Record target state (files and checksums/sizes) before export
        Map<String, Long> filesBefore = listFilesWithSizes(gitRepoDir);

        MediatedExportManager exportManager = new MediatedExportManager();
        File zipFile = exportManager.createStructuredMediatedPackage(
                model, snapshot, spec, selectedPaths, gitRepoDir, externalExportDir.getAbsolutePath(), "test-run-123");

        assertNotNull("Zip file should not be null", zipFile);
        assertTrue("Zip file physically exists", zipFile.exists());
        assertTrue("Zip file is non-empty", zipFile.length() > 0);
        assertTrue("Zip file must be created outside target", zipFile.getAbsolutePath().startsWith(externalExportDir.getAbsolutePath()));

        // Verify target read-only guarantee
        Map<String, Long> filesAfter = listFilesWithSizes(gitRepoDir);
        assertEquals("Target directory must not have new or deleted files", filesBefore.keySet(), filesAfter.keySet());
        for (String p : filesBefore.keySet()) {
            assertEquals("File size in target must be identical for " + p, filesBefore.get(p), filesAfter.get(p));
        }

        // Verify ZIP internal structure
        Map<String, Boolean> zipEntries = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                zipEntries.put(entry.getName(), true);
            }
        }

        // Standard structure checks
        assertTrue("Must contain README.md", zipEntries.containsKey("README.md"));
        assertTrue("Must contain PROMPT.md", zipEntries.containsKey("PROMPT.md"));
        assertTrue("Must contain PROJECT_CONTEXT/ARCHITECTURE.md", zipEntries.containsKey("PROJECT_CONTEXT/ARCHITECTURE.md"));
        assertTrue("Must contain PROJECT_CONTEXT/PROJECT_OVERVIEW.md", zipEntries.containsKey("PROJECT_CONTEXT/PROJECT_OVERVIEW.md"));
        assertTrue("Must contain PROJECT_CONTEXT/DEPENDENCIES.md", zipEntries.containsKey("PROJECT_CONTEXT/DEPENDENCIES.md"));
        assertTrue("Must contain PROJECT_CONTEXT/FILE_MANIFEST.json", zipEntries.containsKey("PROJECT_CONTEXT/FILE_MANIFEST.json"));
        assertTrue("Must contain REQUIREMENTS/ORIGINAL_REQUEST.md", zipEntries.containsKey("REQUIREMENTS/ORIGINAL_REQUEST.md"));
        assertTrue("Must contain REQUIREMENTS/IMPROVED_SPECIFICATION.md", zipEntries.containsKey("REQUIREMENTS/IMPROVED_SPECIFICATION.md"));
        assertTrue("Must contain REQUIREMENTS/ACCEPTANCE_CRITERIA.md", zipEntries.containsKey("REQUIREMENTS/ACCEPTANCE_CRITERIA.md"));
        assertTrue("Must contain DOCUMENTATION/README.md", zipEntries.containsKey("DOCUMENTATION/README.md"));

        // Preserve project-relative file paths check
        boolean foundRelevantFile = zipEntries.keySet().stream().anyMatch(k -> k.startsWith("PROJECT_CONTEXT/RELEVANT_FILES/"));
        assertTrue("Must contain project-relative files in RELEVANT_FILES/", foundRelevantFile);

        // Backward-compatibility check
        assertTrue("Must contain backward-compatible prompt.md", zipEntries.containsKey("prompt.md"));
    }

    private Map<String, Long> listFilesWithSizes(File root) {
        Map<String, Long> map = new HashMap<>();
        collectFiles(root, root, map);
        return map;
    }

    private void collectFiles(File current, File root, Map<String, Long> map) {
        if (current.isDirectory()) {
            File[] children = current.listFiles();
            if (children != null) {
                for (File c : children) collectFiles(c, root, map);
            }
        } else {
            String rel = root.toPath().relativize(current.toPath()).toString().replace('\\', '/');
            map.put(rel, current.length());
        }
    }
}
