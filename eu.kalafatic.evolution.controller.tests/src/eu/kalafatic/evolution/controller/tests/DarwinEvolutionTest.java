package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.KernelFactory;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevSupervisor;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SystemStateSignalProvider;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskExecutor;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner;
import eu.kalafatic.evolution.controller.tools.ShellTool;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.SelfDevStatus;
import eu.kalafatic.evolution.model.orchestration.Ollama;

public class DarwinEvolutionTest {

    private File tempDir;
    private Orchestrator orchestrator;
    private MockProvider mockLlm;
    private AiService aiService;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("darwin-test").toFile();

        eu.kalafatic.evolution.controller.agents.AgentFactory.registerDefaultAgents();

        ShellTool shell = new ShellTool();
        TaskContext initContext = new TaskContext(OrchestrationFactory.eINSTANCE.createOrchestrator(), tempDir);
        shell.execute("git init", tempDir, initContext);
        shell.execute("git config user.email \"test@example.com\"", tempDir, initContext);
        shell.execute("git config user.name \"Test User\"", tempDir, initContext);
        Files.writeString(new File(tempDir, "pom.xml").toPath(), "<project><modelVersion>4.0.0</modelVersion><groupId>test</groupId><artifactId>test</artifactId><version>1.0</version></project>");
        shell.execute("git add .", tempDir, initContext);
        shell.execute("git commit -m \"Initial commit\"", tempDir, initContext);

        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId("darwin-orch");
        orchestrator.setAiMode(AiMode.LOCAL);
        orchestrator.setDarwinMode(true);

        if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        if (orchestrator.getAiChat().getPromptInstructions() == null) orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());
        orchestrator.getAiChat().getPromptInstructions().setPreferredMaxIterations(1);

        Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
        ollama.setUrl("http://localhost:11434");
        ollama.setModel("llama3");
        orchestrator.setOllama(ollama);

        mockLlm = new MockProvider();
        aiService = new AiService() {
            @Override
            public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                return mockLlm.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
        };
    }

    @Test
    public void testDarwinStateTransition() throws Exception {
        SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
        session.setId("darwin-session");
        session.setMaxIterations(1);
        session.setInitialRequest("Improve error handling");
        orchestrator.setSelfDevSession(session);

        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);

        // State Transition variant
        String variantJson = "[" +
            "{\"strategy\": \"Add Validation\", \"suffix\": \"val\", \"actions\": [" +
            "{\"domain\":\"file\", \"operation\":\"WRITE\", \"target\":\"src/Validator.java\", \"description\":\"public class Validator { }\"}" +
            "], \"expected_effect\": {\"short_term\":\"Fixed\", \"risk\":0.1}}" +
            "]";

        String evalSuccess = "{\"success\": true, \"comment\": \"Pass\", \"feedback\": \"OK\"}";

        mockLlm.setResponseSequence(new String[] {
            variantJson, // Darwin variants
            "[{\"id\": \"t1\", \"name\": \"Write src/Validator.java\", \"taskType\": \"file\"}]", // Planner tasks for variant
            "public class Validator { }", // Content generation
            evalSuccess  // Evaluator
        });

        SelfDevSupervisor supervisor = new SelfDevSupervisor(session, context) {
            @Override
            protected IterationManager createIterationManager(eu.kalafatic.evolution.model.orchestration.Iteration iteration, eu.kalafatic.evolution.controller.orchestration.AiService aiService) {
                return createMockedManager(context);
            }
        };

        supervisor.startSession();

        assertEquals(SelfDevStatus.COMPLETED, session.getStatus());
        File valFile = new File(tempDir, "src/Validator.java");
        assertTrue("Validator file should exist", valFile.exists());
    }

    @Test
    public void testDarwinTrajectoryAwareness() throws Exception {
        SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
        session.setId("trajectory-session");
        session.setMaxIterations(2);
        session.setInitialRequest("Refactor code");
        orchestrator.setSelfDevSession(session);
        orchestrator.getAiChat().getPromptInstructions().setPreferredMaxIterations(2);

        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);

        String failVariant = "[" +
            "{\"strategy\": \"Risky Refactor\", \"suffix\": \"risky\", \"actions\": [" +
            "{\"domain\":\"file\", \"operation\":\"DELETE\", \"target\":\"pom.xml\", \"description\":\"Delete pom\"}" +
            "], \"expected_effect\": {\"short_term\":\"Broken\", \"risk\":0.9}}" +
            "]";

        String evalFail = "{\"success\": false, \"comment\": \"Broken build\", \"feedback\": \"Fail\"}";
        String evalSuccess = "{\"success\": true, \"comment\": \"Pass\", \"feedback\": \"OK\"}";

        String successVariant = "[" +
            "{\"strategy\": \"Safe Refactor\", \"suffix\": \"safe\", \"actions\": [" +
            "{\"domain\":\"file\", \"operation\":\"WRITE\", \"target\":\"README.md\", \"description\":\"Update readme\"}" +
            "], \"expected_effect\": {\"short_term\":\"Doc\", \"risk\":0.0}}" +
            "]";

        mockLlm.setResponseSequence(new String[] {
            failVariant, // Darwin proposes risky
            "[{\"id\": \"t1\", \"name\": \"Delete pom.xml\", \"taskType\": \"file\"}]", // Planner for variant 1
            "delete pom", // Content
            evalFail,    // Evaluator fails it
            successVariant, // Darwin proposes safe
            "[{\"id\": \"t2\", \"name\": \"Write README.md\", \"taskType\": \"file\"}]", // Planner for variant 2
            "Update readme", // Content
            evalSuccess  // Evaluator passes it
        });

        SelfDevSupervisor supervisor = new SelfDevSupervisor(session, context) {
            @Override
            protected IterationManager createIterationManager(eu.kalafatic.evolution.model.orchestration.Iteration iteration, eu.kalafatic.evolution.controller.orchestration.AiService aiService) {
                 return createMockedManager(context);
            }
        };
        supervisor.startSession();

        List<String> logs = context.getLogs();
        boolean historyFound = logs.stream().anyMatch(l -> l.contains("[DARWIN] History Analysis:"));
        assertTrue("Should contain history analysis in logs", historyFound);
    }

    @Test
    public void testAtomicTaskDetection_NotRoutedToDarwin() throws Exception {
        // This test simulates the logic in AiChatPage.handleSend()
        orchestrator.setDarwinMode(true);
        String request = "create a java class Hello";

        // Check if atomic
        boolean isAtomic = eu.kalafatic.evolution.controller.orchestration.IterationManager.isSimpleFileCreate(request);
        assertTrue("Request should be detected as atomic", isAtomic);

        // Simulation of AiChatPage.handleSend routing
        boolean wouldRouteToDarwin = orchestrator.isDarwinMode() && !isAtomic;
        assertFalse("Atomic task should NOT be routed to Darwin even if Darwin mode is enabled", wouldRouteToDarwin);
    }

    @Test
    public void testIsSimpleFileCreate_Variations() {
        assertTrue(eu.kalafatic.evolution.controller.orchestration.IterationManager.isSimpleFileCreate("create java class Test"));
        assertTrue(eu.kalafatic.evolution.controller.orchestration.IterationManager.isSimpleFileCreate("create a java class Test"));
        assertTrue(eu.kalafatic.evolution.controller.orchestration.IterationManager.isSimpleFileCreate("create new file config.xml"));
        assertTrue(eu.kalafatic.evolution.controller.orchestration.IterationManager.isSimpleFileCreate("write to file log.txt"));
        // Non-atomic because of "and" or broad scope
        assertFalse(eu.kalafatic.evolution.controller.orchestration.IterationManager.isSimpleFileCreate("create java class Test and add many methods"));
        assertFalse(eu.kalafatic.evolution.controller.orchestration.IterationManager.isSimpleFileCreate("just a simple chat"));
    }

    private IterationManager createMockedManager(TaskContext context) {
        GitManager gitManager = new GitManager(tempDir, context);
        TaskPlanner taskPlanner = new TaskPlanner();
        TaskExecutor taskExecutor = new TaskExecutor(context, orchestrator);
        IterationMemoryService memoryService = new IterationMemoryService(tempDir);
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(tempDir, context);
        DarwinEngine darwinEngine = new DarwinEngine(context, memoryService, stateProvider);
        darwinEngine.setAiService(aiService);
        context.setAiService(aiService);

        return new IterationManager(
            context,
            aiService,
            gitManager,
            taskPlanner,
            taskExecutor,
            new MockEvaluator(context),
            darwinEngine,
            memoryService
        );
    }

    private static class MockEvaluator extends eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator {
        public MockEvaluator(TaskContext context) { super(new File("."), context); }
        @Override
        public eu.kalafatic.evolution.model.orchestration.EvaluationResult evaluate() throws Exception {
            eu.kalafatic.evolution.model.orchestration.EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
            res.setSuccess(true);
            res.setDecision(eu.kalafatic.evolution.model.orchestration.SelfDevDecision.CONTINUE);
            res.setTestPassRate(1.0);
            return res;
        }
        @Override
        public Evaluation evaluateWithSnapshot() throws Exception {
            Evaluation eval = new Evaluation();
            eval.result = evaluate();
            eval.snapshot = new eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot();
            eval.snapshot.build.status = eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot.BuildStatus.SUCCESS;
            return eval;
        }
    }

    private static class MockProvider implements ILlmProvider {
        private String[] responseSequence;
        private final AtomicInteger callCount = new AtomicInteger(0);
        private String defaultResponse = "{\"success\": true, \"comment\": \"Mock success\"}";
        public void setResponseSequence(String[] sequence) { this.responseSequence = sequence; this.callCount.set(0); }
        @Override
        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
            int current = callCount.getAndIncrement();
            String resp = (responseSequence != null && current < responseSequence.length) ? responseSequence[current] : defaultResponse;
            if (context != null) {
                context.log("[MOCK] Call " + current + " prompt snippet: " + prompt.substring(0, Math.min(prompt.length(), 100)).replace("\n", " "));
                context.log("[MOCK] Call " + current + " response: " + resp);
            }
            return resp;
        }
        @Override
        public String testConnection(Orchestrator orchestrator, float temperature, String proxyUrl, TaskContext context) throws Exception {
            return "{\"status\": \"ok\"}";
        }
    }
}
