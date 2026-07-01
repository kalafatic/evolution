package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.workflow.MediatedExportManager;
import eu.kalafatic.evolution.model.orchestration.*;

public class StabilizationValidationSuite {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private AiService mockAiService;
    private Orchestrator orchestrator;
    private File projectRoot;
    private MockLlmProvider mockLlm;

    @Before
    public void setup() throws Exception {
        SessionManager.getInstance().shutdownAll();
        projectRoot = tempFolder.newFolder("default-root-" + UUID.randomUUID().toString().substring(0, 8));
        mockLlm = new MockLlmProvider();
        mockAiService = new AiService() {
            @Override
            public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                return mockLlm.handle(prompt);
            }
        };

        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());

        SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
        session.setId("stabilization-session");
        session.setStatus(SelfDevStatus.RUNNING);
        orchestrator.setSelfDevSession(session);

        // Initialize Git repo
        initGit(projectRoot);
    }

    private void initGit(File root) throws Exception {
        ShellTool shell = new ShellTool();
        TaskContext initContext = new TaskContext(orchestrator, root);
        shell.execute("git init", root, initContext);
        shell.execute("git config user.email \"test@example.com\"", root, initContext);
        shell.execute("git config user.name \"Test User\"", root, initContext);
        Files.writeString(new File(root, "pom.xml").toPath(),
            "<project><modelVersion>4.0.0</modelVersion><groupId>test</groupId><artifactId>test</artifactId><version>1.0</version></project>");
        shell.execute("git add .", root, initContext);
        shell.execute("git commit -m \"Initial commit\"", root, initContext);
    }

    @After
    public void tearDown() {
        SessionManager.getInstance().shutdownAll();
    }

    @Test
    public void testResumeMidRecursion() throws Exception {
        new File(projectRoot, "iterations").mkdirs();
        String sessionId = "resume-test-" + UUID.randomUUID().toString().substring(0, 8);
        TaskContext context = new TaskContext(orchestrator, projectRoot);
        context.setSessionId(sessionId);
        context.getBehaviorProfile().addTrait(BehaviorTrait.REASONING_DARWIN_ITERATIVE);
        context.setAutoApprove(true);
        context.getMetadata().put("testMode", true);

        mockLlm.setResponse("IntentExpansionEngine", "{\"state\": \"CLEAR\", \"dominantIntent\": \"Task\", \"confidence\": {\"overallConfidence\": 1.0}}");
        mockLlm.setResponse("StabilityAnalyzer", "{\"converged\": false, \"rationale\": \"Still evolving\"}");

        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        IterationManager manager = KernelFactory.create("Test", context, session, mockAiService);

        TaskRequest request = new TaskRequest("Initial goal", projectRoot);
        manager.handle(request);

        int firstRunIterations = context.getOrchestrationState().getIterationCount();
        assertTrue("Should have run at least one iteration", firstRunIterations > 0);

        // Restart and Restore
        SessionManager.getInstance().shutdownSession(sessionId);

        TaskContext context2 = new TaskContext(orchestrator, projectRoot);
        context2.setSessionId(sessionId);
        context2.setAutoApprove(true);
        context2.getMetadata().put("testMode", true);

        SessionContainer session2 = SessionManager.getInstance().getOrCreateSession(sessionId);
        IterationManager manager2 = KernelFactory.create("Test", context2, session2, mockAiService);

        assertEquals("Should have restored iteration count", firstRunIterations, context2.getOrchestrationState().getIterationCount());
    }

    @Test
    public void testMediatedModeEndToEnd() throws Exception {
        File mediatedRoot = tempFolder.newFolder("mediated-e2e-root-" + UUID.randomUUID().toString().substring(0, 8));
        initGit(mediatedRoot);
        new File(mediatedRoot, "iterations").mkdirs();

        String sessionId = "mediated-e2e-" + UUID.randomUUID().toString().substring(0, 8);
        TaskContext context = new TaskContext(orchestrator, mediatedRoot);
        context.setSessionId(sessionId);
        context.getBehaviorProfile().addTrait(BehaviorTrait.SUPERVISION_MEDIATED);
        context.setAutoApprove(true);

        // Auto-respond to selections
        context.addInputListener(msg -> {
            if (msg.contains("Darwin evolved")) {
                context.provideInput("Select direct_minimal");
            } else if (msg.contains("Proceed to") || msg.contains("Ready to generate") || msg.contains("Final review")) {
                context.provideInput("Yes");
            }
        });

        mockLlm.setResponse("IntentExpansionEngine", "{\"state\": \"CLEAR\", \"dominantIntent\": \"Task\", \"confidence\": {\"overallConfidence\": 1.0}}");
        // Ensure StabilityAnalyzer converges eventually
        mockLlm.setResponse("StabilityAnalyzer", "{\"converged\": true, \"rationale\": \"Stable\"}");

        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        IterationManager manager = KernelFactory.create("Test", context, session, mockAiService);

        TaskRequest request = new TaskRequest("End to end mediated test", mediatedRoot);
        OrchestratorResponse response = manager.handle(request);

        assertNotNull(response.getSummary());
        assertTrue("Summary should contain mediated completion header or evolution summary, but was: " + response.getSummary(),
            response.getSummary().contains("### Mediated Darwin Evolution Complete") || response.getSummary().contains("### 🧬 Evolution Summary"));

        File[] files = mediatedRoot.listFiles((dir, name) -> name.startsWith("mediated_export_") && name.endsWith(".zip"));
        assertNotNull(files);
    }

    @Test
    public void testMultiSessionParallelExecutionIsolation() throws Exception {
        int sessionCount = 3;
        List<Thread> threads = new ArrayList<>();
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < sessionCount; i++) {
            final int index = i;
            File sessionRoot = tempFolder.newFolder("parallel-root-" + index + "-" + UUID.randomUUID().toString().substring(0, 8));
            initGit(sessionRoot);
            new File(sessionRoot, "iterations").mkdirs();

            Thread t = new Thread(() -> {
                try {
                    String sessionId = "parallel-session-" + index;
                    TaskContext ctx = new TaskContext(orchestrator, sessionRoot);
                    ctx.setSessionId(sessionId);
                    ctx.setAutoApprove(true);
                    ctx.getMetadata().put("testMode", true);

                    mockLlm.setResponse("IntentExpansionEngine", "{\"state\": \"CLEAR\", \"dominantIntent\": \"Task\", \"confidence\": {\"overallConfidence\": 1.0}}");

                    SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
                    IterationManager manager = KernelFactory.create("Test", ctx, session, mockAiService);

                    TaskRequest request = new TaskRequest("Goal for session " + index, sessionRoot);
                    manager.handle(request);

                    assertEquals(sessionId, ctx.getSessionId());
                    String checkpointGoal = (String)ctx.getOrchestrationState().getMetadata().get("checkpoint_goal");
                    assertNotNull("Checkpoint goal should not be null for session " + index, checkpointGoal);
                    assertTrue("Goal mismatch for session " + index + ": " + checkpointGoal, checkpointGoal.contains(String.valueOf(index)));

                } catch (Throwable e) {
                    errors.add(e);
                }
            });
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            t.join(60000);
        }

        if (!errors.isEmpty()) {
            throw new Exception(errors.get(0));
        }
    }

    @Test
    public void testDeterministicBehavior() throws Exception {
        String prompt = "Deterministic goal";

        // Run 1
        File root1 = tempFolder.newFolder("det-root-1-" + UUID.randomUUID().toString().substring(0, 8));
        initGit(root1);
        new File(root1, "iterations").mkdirs();

        String sessionId1 = "det-session-1";
        TaskContext ctx1 = new TaskContext(orchestrator, root1);
        ctx1.setSessionId(sessionId1);
        ctx1.setAutoApprove(true);
        ctx1.getMetadata().put("testMode", true);
        mockLlm.setResponse("IntentExpansionEngine", "{\"state\": \"CLEAR\", \"dominantIntent\": \"Task\", \"confidence\": {\"overallConfidence\": 1.0}}");
        SessionContainer session1 = SessionManager.getInstance().getOrCreateSession(sessionId1);
        IterationManager manager1 = KernelFactory.create("Test", ctx1, session1, mockAiService);
        manager1.handle(new TaskRequest(prompt, root1));

        // Run 2
        File root2 = tempFolder.newFolder("det-root-2-" + UUID.randomUUID().toString().substring(0, 8));
        initGit(root2);
        new File(root2, "iterations").mkdirs();

        String sessionId2 = "det-session-2";
        TaskContext ctx2 = new TaskContext(orchestrator, root2);
        ctx2.setSessionId(sessionId2);
        ctx2.setAutoApprove(true);
        ctx2.getMetadata().put("testMode", true);
        SessionContainer session2 = SessionManager.getInstance().getOrCreateSession(sessionId2);
        IterationManager manager2 = KernelFactory.create("Test", ctx2, session2, mockAiService);
        manager2.handle(new TaskRequest(prompt, root2));

        assertEquals("Iteration count should be identical", ctx1.getOrchestrationState().getIterationCount(), ctx2.getOrchestrationState().getIterationCount());
        assertEquals("Final phase should be identical", ctx1.getOrchestrationState().getCurrentPhase(), ctx2.getOrchestrationState().getCurrentPhase());
    }

    @Test
    public void testLLMMalformedOutputStress() throws Exception {
        String sessionId = "malformed-test-" + UUID.randomUUID().toString().substring(0, 8);
        TaskContext context = new TaskContext(orchestrator, projectRoot);
        context.setSessionId(sessionId);
        context.setAutoApprove(true);

        mockLlm.setResponse("IntentExpansionEngine", "Here is some noise before the json: \n" +
            "{\"state\": \"CLEAR\", \"dominantIntent\": \"Task\", \"confidence\": {\"overallConfidence\": 1.0}} \n" +
            "And some more noise after.");

        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        IterationManager manager = KernelFactory.create("Test", context, session, mockAiService);

        TaskRequest request = new TaskRequest("Malformed test prompt", projectRoot);

        try {
            manager.handle(request);
        } catch (Exception e) {
            fail("Should not have thrown exception on malformed output: " + e.getMessage());
        }

        assertEquals("ARCHITECTURE_VARIANTS", context.getOrchestrationState().getCurrentPhase());
    }

    private static class MockLlmProvider {
        private java.util.Map<String, String> responses = new java.util.HashMap<>();
        private String defaultResponse = "{\"success\": true, \"comment\": \"Default success\", \"converged\": true}";

        public void setResponse(String key, String response) {
            responses.put(key, response);
        }

        public String handle(String prompt) {
            for (String key : responses.keySet()) {
                if (prompt.contains(key)) {
                    return responses.get(key);
                }
            }
            return defaultResponse;
        }
    }

    private static class ShellTool extends eu.kalafatic.evolution.controller.tools.ShellTool {}
}
