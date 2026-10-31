package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.develop.DevelopPermissions;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopResult;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopSession;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopSessionManager;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopTask;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopTaskManager;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopTaskStatus;
import eu.kalafatic.evolution.controller.orchestration.develop.agent.DevelopAgent;
import eu.kalafatic.evolution.controller.orchestration.develop.analyzer.RepositoryAnalyzer;
import eu.kalafatic.evolution.controller.orchestration.develop.analyzer.RepositoryAnalyzerImpl;
import eu.kalafatic.evolution.controller.orchestration.develop.vcs.GitService;
import eu.kalafatic.evolution.controller.orchestration.develop.vcs.GitServiceImpl;

public class DevelopIntegrationTest {

    private File tempRepo;
    private DevelopSessionManager sessionManager;
    private DevelopTaskManager taskManager;

    @Before
    public void setUp() throws Exception {
        sessionManager = DevelopSessionManager.getInstance();
        sessionManager.clearAllSessions();
        taskManager = DevelopTaskManager.getInstance();

        tempRepo = Files.createTempDirectory("evo-develop-source-repo").toFile();
        GitService gitService = new GitServiceImpl();
        gitService.ensureInitialCommit(tempRepo);

        File sampleFile = new File(tempRepo, "Calculator.java");
        Files.writeString(sampleFile.toPath(), "public class Calculator { public int add(int a, int b) { return a + b; } }");

        File pomFile = new File(tempRepo, "pom.xml");
        Files.writeString(pomFile.toPath(), "<project><modelVersion>4.0.0</modelVersion><groupId>test</groupId><artifactId>test</artifactId><version>1.0</version></project>");

        gitService.commitChanges(tempRepo, "Initial commit");
    }

    @After
    public void tearDown() throws Exception {
        sessionManager.clearAllSessions();
        if (tempRepo != null && tempRepo.exists()) {
            deleteDirectory(tempRepo);
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) deleteDirectory(f);
        }
        dir.delete();
    }

    @Test
    public void testTaskLifecycleAndPermissions() throws Exception {
        DevelopPermissions perms = new DevelopPermissions(true, true, true, false);
        DevelopTask task = taskManager.createTask("Fix Calculator", "Refactor add method", tempRepo.getAbsolutePath(), "main", perms);

        assertNotNull(task);
        assertEquals(DevelopTaskStatus.CREATED, task.getStatus());
        assertTrue(task.getPermissions().canRead());
        assertTrue(task.getPermissions().canWrite());
        assertTrue(task.getPermissions().canExecute());
        assertTrue(!task.getPermissions().canPush());

        DevelopSession session = sessionManager.createSession(task);
        assertNotNull(session);
        assertTrue(session.getWorkspaceDir().exists());
        assertTrue(session.getRepositoryDir().exists());
    }

    @Test
    public void testRepositoryAnalyzer() throws Exception {
        RepositoryAnalyzer analyzer = new RepositoryAnalyzerImpl();
        RepositoryAnalyzer.ProjectAnalysis analysis = analyzer.analyze(tempRepo);

        assertNotNull(analysis);
        assertEquals("Maven", analysis.getBuildSystem());
        assertTrue(analysis.getLanguages().contains("Java"));

        // Multi-platform test
        File nodeRepo = Files.createTempDirectory("evo-node-repo").toFile();
        try {
            Files.writeString(new File(nodeRepo, "package.json").toPath(), "{\"name\":\"sample\",\"scripts\":{\"test\":\"echo ok\"}}");
            RepositoryAnalyzer.ProjectAnalysis nodeAnalysis = analyzer.analyze(nodeRepo);
            assertEquals("NPM/Yarn/PNPM", nodeAnalysis.getBuildSystem());
            assertTrue(nodeAnalysis.getLanguages().contains("JavaScript/TypeScript"));
        } finally {
            deleteDirectory(nodeRepo);
        }
    }

    @Test
    public void testBuildAndTestToolMetrics() throws Exception {
        eu.kalafatic.evolution.controller.orchestration.develop.tools.RunBuildTool buildTool =
            new eu.kalafatic.evolution.controller.orchestration.develop.tools.RunBuildTool();
        eu.kalafatic.evolution.controller.orchestration.develop.tools.RunTestsTool testsTool =
            new eu.kalafatic.evolution.controller.orchestration.develop.tools.RunTestsTool();

        DevelopPermissions perms = new DevelopPermissions(true, true, true, false);
        DevelopTask task = taskManager.createTask("Metrics Task", "Validate metrics recording", tempRepo.getAbsolutePath(), "main", perms);
        DevelopSession session = sessionManager.createSession(task);

        // Run unconfigured directory test
        File emptyRepo = Files.createTempDirectory("evo-empty-repo").toFile();
        try {
            DevelopTask emptyTask = taskManager.createTask("Empty Task", "Empty project", emptyRepo.getAbsolutePath(), "main", perms);
            DevelopSession emptySession = sessionManager.createSession(emptyTask);

            buildTool.execute(emptySession, new eu.kalafatic.evolution.controller.orchestration.develop.tools.DevelopTool.ToolInput(""));
            assertEquals(eu.kalafatic.evolution.controller.orchestration.develop.ExecutionStatus.NOT_AVAILABLE,
                    emptyTask.getResult().getBuildMetrics().getStatus());

            testsTool.execute(emptySession, new eu.kalafatic.evolution.controller.orchestration.develop.tools.DevelopTool.ToolInput(""));
            assertEquals(eu.kalafatic.evolution.controller.orchestration.develop.ExecutionStatus.NOT_AVAILABLE,
                    emptyTask.getResult().getTestMetrics().getStatus());
        } finally {
            deleteDirectory(emptyRepo);
        }
    }

    @Test
    public void testDevelopAgentExecution() throws Exception {
        DevelopPermissions perms = new DevelopPermissions(true, true, true, false);
        DevelopTask task = taskManager.createTask("Implement Test", "Add unit test for Calculator", tempRepo.getAbsolutePath(), "main", perms);

        DevelopSession session = sessionManager.createSession(task);
        DevelopAgent agent = new DevelopAgent(2);

        DevelopResult result = agent.executeTask(session);

        assertNotNull(result);
        assertEquals(DevelopTaskStatus.COMPLETED, task.getStatus());
        assertNotNull(session.getLogs());
        assertTrue(session.getLogs().size() > 0);
    }

    @Test
    public void testTaskCancellation() throws Exception {
        DevelopPermissions perms = new DevelopPermissions(true, true, true, false);
        DevelopTask task = taskManager.createTask("Cancelled Task", "Test cancellation", tempRepo.getAbsolutePath(), "main", perms);

        DevelopSession session = sessionManager.createSession(task);
        session.cancel();

        assertTrue(session.isCancelled());
        assertEquals(DevelopTaskStatus.CANCELLED, task.getStatus());
    }

    @Test
    public void testGitDeliveryStages() throws Exception {
        DevelopPermissions perms = new DevelopPermissions(true, true, true, false);
        DevelopTask task = taskManager.createTask("Delivery Task", "Testing delivery stages", tempRepo.getAbsolutePath(), "main", perms);
        DevelopSession session = sessionManager.createSession(task);

        // Modify a file via agent parsing
        DevelopAgent agent = new DevelopAgent(2);
        String mockAiResponse = "```file:Calculator.java\npublic class Calculator { public int add(int a, int b) { return a + b + 0; } }\n```";
        agent.parseAndApplyFiles(session.getRepositoryDir(), mockAiResponse, session);

        DevelopResult result = agent.executeTask(session);
        assertNotNull(result);
        assertEquals(DevelopTaskStatus.COMPLETED, task.getStatus());
        assertEquals(eu.kalafatic.evolution.controller.orchestration.develop.DeliveryStage.COMMIT_CREATED, task.getDeliveryStage());
        assertTrue(result.getCommitResult().contains("develop/"));

        // Verify Pull Request generation
        String pr = session.getGitService().createPullRequest(session.getRepositoryDir(), "develop/feature-1", "main", "Feature", "Desc", null);
        assertNotNull(pr);
    }

    @Test
    public void testSafeArchiveExtraction() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            zos.putNextEntry(new java.util.zip.ZipEntry("app/Main.java"));
            zos.write("public class Main {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        File targetDir = Files.createTempDirectory("evo-safe-extract").toFile();
        try {
            int extracted = eu.kalafatic.evolution.controller.orchestration.develop.SafeArchiveExtractor.extractArchive(baos.toByteArray(), targetDir);
            assertEquals(1, extracted);
            assertTrue(new File(targetDir, "app/Main.java").exists());

            // Zip Slip attack test
            java.io.ByteArrayOutputStream maliciousBaos = new java.io.ByteArrayOutputStream();
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(maliciousBaos)) {
                zos.putNextEntry(new java.util.zip.ZipEntry("../evil.txt"));
                zos.write("malicious payload".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            boolean slipBlocked = false;
            try {
                eu.kalafatic.evolution.controller.orchestration.develop.SafeArchiveExtractor.extractArchive(maliciousBaos.toByteArray(), targetDir);
            } catch (SecurityException se) {
                slipBlocked = true;
            }
            assertTrue("Zip Slip attack should be blocked with SecurityException", slipBlocked);
        } finally {
            deleteDirectory(targetDir);
        }
    }

    @Test
    public void testSessionPersistenceAndRecovery() throws Exception {
        DevelopPermissions perms = new DevelopPermissions(true, true, true, false);
        DevelopTask task = taskManager.createTask("Persistent Task", "Test state restoration", tempRepo.getAbsolutePath(), "main", perms);
        DevelopSession session = sessionManager.createSession(task);
        session.addLog("Custom development event");

        File stateFile = new File(session.getWorkspaceDir(), "state.json");
        assertTrue(stateFile.exists());

        // Clear in-memory map to test restart recovery
        sessionManager.recoverSessions();

        DevelopSession recovered = sessionManager.getSession(session.getSessionId());
        assertNotNull(recovered);
        assertEquals("Persistent Task", recovered.getTask().getTitle());
        assertTrue(recovered.getLogs().stream().anyMatch(l -> l.contains("Custom development event")));
    }
}
