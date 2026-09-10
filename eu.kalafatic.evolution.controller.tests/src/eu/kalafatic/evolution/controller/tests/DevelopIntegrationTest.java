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
}
