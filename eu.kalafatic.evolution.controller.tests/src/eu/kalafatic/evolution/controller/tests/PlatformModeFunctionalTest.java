package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.AutonomyLevel;
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
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.SelfDevStatus;

public class PlatformModeFunctionalTest {

    private File tempDir;
    private Orchestrator orchestrator;
    private MockLlmProvider mockLlm;
    private AiService aiService;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("platform-mode-test").toFile();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setAiMode(AiMode.LOCAL);

        orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());

        mockLlm = new MockLlmProvider();
        aiService = new AiService() {
            @Override
            public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                return mockLlm.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
        };

        eu.kalafatic.evolution.controller.agents.AgentFactory.registerDefaultAgents();
    }

    @Test
    public void testSimpleChatMode() throws Exception {
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setPlatformMode(new PlatformMode(PlatformType.SIMPLE_CHAT, AutonomyLevel.LOW, 1, false));

        mockLlm.setResponseSequence(new String[] {
            "The project is a Tycho-based RCP application. I am in chat mode." // GeneralAgent
        });

        IterationManager manager = createManager(context);
        String result = manager.handle(new TaskRequest("Tell me about the project", tempDir)).getSummary();

        assertNotNull(result);
        assertTrue(result.contains("chat mode"));
        assertTrue(context.getLogs().stream().anyMatch(l -> l.contains("Platform Mode: " + PlatformType.SIMPLE_CHAT)));
    }

    @Test
    public void testAssistedCodingMode() throws Exception {
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);
        context.setPlatformMode(new PlatformMode(PlatformType.ASSISTED_CODING, AutonomyLevel.LOW, 2, false));

        mockLlm.setResponseSequence(new String[] {
            "{\"intent\":\"new\", \"confidence\":1.0, \"category\":\"CODING\", \"isAmbiguous\":false, \"objective\":\"Create a readme\", \"refinedPrompt\":\"Create a readme\"}", // Analytic
            "[{\"id\": \"t1\", \"name\": \"Write README.md\", \"taskType\": \"file\"}]", // Plan
            "This is a readme content", // File content (JavaDev/FileAgent)
            "{\"success\": true, \"comment\": \"Verified\"}", // Review
            "Final summary: Created README.md" // Final Response
        });

        IterationManager manager = createManager(context);
        String result = manager.handle(new TaskRequest("Create a readme", tempDir)).getSummary();

        assertNotNull(result);
        assertTrue(new File(tempDir, "README.md").exists());
    }

    @Test
    public void testDarwinModeRouting() throws Exception {
        orchestrator.setDarwinMode(true);
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);

        assertTrue(orchestrator.isDarwinMode());

        PlatformMode mode = new PlatformMode(PlatformType.DARWIN_MODE, AutonomyLevel.MEDIUM, 3, false);
        context.setPlatformMode(mode);
        assertEquals(PlatformType.DARWIN_MODE, context.getPlatformMode().getType());
    }

    @Test
    public void testSelfDevMode() throws Exception {
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setPlatformMode(new PlatformMode(PlatformType.SELF_DEV_MODE, AutonomyLevel.HIGH, 5, true));

        SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
        session.setId("test-self-dev");
        session.setStatus(SelfDevStatus.RUNNING);
        orchestrator.setSelfDevSession(session);

        assertEquals(SelfDevStatus.RUNNING, orchestrator.getSelfDevSession().getStatus());
        assertTrue(context.getPlatformMode().isAllowSelfModify());
    }

    private IterationManager createManager(TaskContext context) {
        GitManager gitManager = new GitManager(tempDir, context);
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
            if (responseSequence != null && current < responseSequence.length) {
                return responseSequence[current];
            }
            return "{\"success\": true, \"comment\": \"Default mock response\"}";
        }

        @Override
        public String testConnection(Orchestrator orchestrator, float temperature, String proxyUrl, TaskContext context) throws Exception {
            return "{\"status\": \"ok\"}";
        }
    }
}
