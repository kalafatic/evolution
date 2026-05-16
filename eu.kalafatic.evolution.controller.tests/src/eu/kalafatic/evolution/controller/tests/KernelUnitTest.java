package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.agents.AgentFactory;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.ResultType;
import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SystemStateSignalProvider;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskExecutor;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class KernelUnitTest {

    private File tempDir;
    private Orchestrator orchestrator;
    private MockLlmProvider mockLlm;
    private AiService aiService;
    private TaskContext context;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("kernel-unit-test").toFile();

        AgentFactory.registerDefaultAgents();

        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setAiMode(AiMode.LOCAL);

        mockLlm = new MockLlmProvider();
        aiService = new AiService() {
            @Override
            public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                return mockLlm.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
        };

        context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);
        context.getMetadata().put("testMode", true);

        // Initialize as git repo for tests that require it or to avoid failure in repository-first reasoning
        eu.kalafatic.evolution.controller.tools.ShellTool shell = new eu.kalafatic.evolution.controller.tools.ShellTool();
        shell.execute("git init", tempDir, context);
    }

    @Test
    public void testSimpleChatFlow() throws Exception {
        mockLlm.setResponseSequence(new String[] {
            "{\"atomic\": false, \"confidence\": 0.1}", // AtomicIntentClassifier LLM validation
            "Hello, I am a mock response." // GeneralAgent (IterationManager handles SIMPLE_CHAT mode by calling chatAgent directly after atomic check)
        });

        IterationManager manager = createManager();
        context.setPlatformMode(new PlatformMode(PlatformType.SIMPLE_CHAT, null, 1, false));

        // Use a prompt that doesn't trigger fast-track greeting
        TaskRequest request = new TaskRequest("What is the capital of France?", tempDir);
        var response = manager.handle(request);

        assertEquals(ResultType.CHAT, response.getResultType());
        // Since we refactored to use GeneralAgent, and buildPrompt adds prefixes, we check for containment
        assertTrue("Response summary should contain mock response, but was: " + response.getSummary(),
                response.getSummary().contains("mock response"));
        assertEquals(SystemState.DONE, context.getStateHolder().getState());
    }

    @Test
    public void testTransitionToFailedOnException() throws Exception {
        AiService failingService = new AiService() {
            @Override
            public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                throw new RuntimeException("Simulated AI Failure");
            }
        };

        IterationManager manager = new IterationManager(
            context, failingService, new GitManager(tempDir),
            new TaskPlanner(), new TaskExecutor(context), new Evaluator(tempDir, context),
            new DarwinEngine(context, new IterationMemoryService(tempDir), new SystemStateSignalProvider(tempDir, context)),
            new IterationMemoryService(tempDir)
        );

        context.setPlatformMode(new PlatformMode(PlatformType.SIMPLE_CHAT, null, 1, false));

        TaskRequest request = new TaskRequest("Force failure", tempDir);
        try {
            manager.handle(request);
            fail("Expected exception");
        } catch (Exception e) {
            assertEquals(SystemState.FAILED, context.getStateHolder().getState());
        }
    }

    private IterationManager createManager() {
        GitManager gitManager = new GitManager(tempDir);
        TaskPlanner taskPlanner = new TaskPlanner();
        TaskExecutor taskExecutor = new TaskExecutor(context);
        Evaluator evaluator = new Evaluator(tempDir, context);
        IterationMemoryService memoryService = new IterationMemoryService(tempDir);
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(tempDir, context);
        DarwinEngine darwinEngine = new DarwinEngine(context, memoryService, stateProvider);

        return new IterationManager(
            context,
            aiService,
            gitManager,
            taskPlanner,
            taskExecutor,
            evaluator,
            darwinEngine,
            memoryService
        );
    }

    private static class MockLlmProvider implements ILlmProvider {
        private String[] responseSequence;
        private final AtomicInteger callCount = new AtomicInteger(0);

        public void setResponseSequence(String[] sequence) {
            this.responseSequence = sequence;
            this.callCount.set(0);
        }

        @Override
        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
            int current = callCount.getAndIncrement();
            return (responseSequence != null && current < responseSequence.length) ? responseSequence[current] : "{}";
        }

        @Override
        public String testConnection(Orchestrator orchestrator, float temperature, String proxyUrl, TaskContext context) throws Exception {
            return "{\"status\": \"ok\"}";
        }
    }
}
