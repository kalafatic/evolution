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

import eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.mutation.MutationSession;
import eu.kalafatic.evolution.controller.orchestration.mutation.MutationSessionManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ADarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngineFactory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.MutationEngine;
import eu.kalafatic.evolution.controller.orchestration.util.ModeRecognizer;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class MutationIntegrationTest {

    private File tempRepo;
    private MutationSessionManager sessionManager;

    @Before
    public void setUp() throws Exception {
        sessionManager = MutationSessionManager.getInstance();
        sessionManager.clearAllSessions();

        // Create temporary source git repo
        tempRepo = Files.createTempDirectory("evo-mutation-source-repo").toFile();
        GitManager gitManager = new GitManager(tempRepo);
        gitManager.ensureInitialCommit();

        // Add a sample source file
        File sampleFile = new File(tempRepo, "Calculator.java");
        Files.writeString(sampleFile.toPath(), "public class Calculator { public int add(int a, int b) { return a + b; } }");
        gitManager.commit("Add Calculator.java");
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
    public void testMutationSessionCreationAndIsolation() throws Exception {
        String sessionId = "mutation-test-session-1";
        MutationSession session = sessionManager.createSession(tempRepo.getAbsolutePath(), "main", sessionId);

        assertNotNull(session);
        assertEquals(sessionId, session.getSessionId());
        assertTrue(session.getWorkspaceDir().exists());

        // Verify workspace is isolated and contains Calculator.java
        File workspaceFile = new File(session.getWorkspaceDir(), "Calculator.java");
        assertTrue(workspaceFile.exists());
    }

    @Test
    public void testMutationFileModificationAndDiff() throws Exception {
        String sessionId = "mutation-test-session-2";
        MutationSession session = sessionManager.createSession(tempRepo.getAbsolutePath(), "main", sessionId);

        // Modify file in workspace
        File targetFile = new File(session.getWorkspaceDir(), "CalculatorTest.java");
        Files.writeString(targetFile.toPath(), "public class CalculatorTest { public void testAdd() {} }");

        List<String> changedFiles = session.getChangedFiles();
        assertTrue(changedFiles.stream().anyMatch(f -> f.contains("CalculatorTest.java")));

        String diff = session.getDiff();
        assertNotNull(diff);
        assertTrue(diff.contains("CalculatorTest.java"));

        // Commit changes
        session.commit("Add CalculatorTest.java");

        // Verify diff is now clean for HEAD
        List<String> cleanChangedFiles = session.getChangedFiles();
        assertTrue(cleanChangedFiles.isEmpty());
    }

    @Test
    public void testMutationEngineAndModeRecognition() throws Exception {
        String sessionId = "mutation-session-99";
        SessionContainer container = SessionManager.getInstance().getOrCreateSession(sessionId);

        TaskContext context = new TaskContext(null, tempRepo);
        context.setSessionId(sessionId);
        context.getMetadata().put("sessionContext", container);

        assertTrue(ModeRecognizer.isMutationMode(context));
        assertEquals(PlatformType.MUTATION, ModeRecognizer.determineType(context));

        ADarwinEngine engine = DarwinEngineFactory.createEngine(PlatformType.MUTATION, context, null, null);
        assertNotNull(engine);
        assertTrue(engine instanceof MutationEngine);
        assertEquals(PlatformType.MUTATION, engine.getPlatformType());
    }

    @Test
    public void testMutationBuildAndTestExecution() throws Exception {
        String sessionId = "mutation-test-session-3";
        MutationSession session = sessionManager.createSession(tempRepo.getAbsolutePath(), "main", sessionId);

        String buildOutput = session.executeBuildAndTest();
        assertNotNull(buildOutput);
    }

    @Test
    public void testMutationSessionWithModelAndRemoteClientRegistration() throws Exception {
        String sessionId = "mutation-remote-test-1";
        String testModel = "llama3.2:3b";
        MutationSession session = sessionManager.createSession(tempRepo.getAbsolutePath(), "main", sessionId, testModel);

        assertNotNull(session);
        assertEquals(sessionId, session.getSessionId());

        Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();
        assertNotNull(orch);
        assertNotNull(orch.getAiChat());

        ChatSession chatSession = orch.getAiChat().getSessions().stream()
                .filter(s -> sessionId.equals(s.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(chatSession);
        assertEquals("REMOTE_CLIENT", chatSession.getTargetType());
        assertEquals(testModel, chatSession.getLocalModel());
        assertEquals(session.getWorkspaceDir().getAbsolutePath(), chatSession.getTargetPath());
    }
}
