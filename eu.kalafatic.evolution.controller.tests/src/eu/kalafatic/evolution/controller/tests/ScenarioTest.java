package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator;
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
import eu.kalafatic.evolution.controller.tools.ShellTool;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.Ollama;

public class ScenarioTest {

    private File tempDir;
    private Orchestrator orchestrator;
    private MockProvider mockLlm;
    private AiService aiService;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("scenario-test").toFile();

        eu.kalafatic.evolution.controller.agents.AgentFactory.registerDefaultAgents();

        // Initialize Git repo for SelfDev scenarios
        ShellTool shell = new ShellTool();
        TaskContext initContext = new TaskContext(OrchestrationFactory.eINSTANCE.createOrchestrator(), tempDir);
        shell.execute("git init", tempDir, initContext);
        shell.execute("git config user.email \"test@example.com\"", tempDir, initContext);
        shell.execute("git config user.name \"Test User\"", tempDir, initContext);
        Files.writeString(new File(tempDir, "pom.xml").toPath(), "<project><modelVersion>4.0.0</modelVersion><groupId>test</groupId><artifactId>test</artifactId><version>1.0</version></project>");
        shell.execute("git add .", tempDir, initContext);
        shell.execute("git commit -m \"Initial commit\"", tempDir, initContext);

        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId("scenario-orch");
        orchestrator.setAiMode(AiMode.LOCAL);

        if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        if (orchestrator.getAiChat().getPromptInstructions() == null) orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());
        orchestrator.getAiChat().getPromptInstructions().setPreferredMaxIterations(0);

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
    public void testScenario1_SimplePrompt() throws Exception {
        if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        if (orchestrator.getAiChat().getPromptInstructions() == null) orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());
        orchestrator.getAiChat().getPromptInstructions().setIterativeMode(false);
        orchestrator.getAiChat().getPromptInstructions().setSelfIterativeMode(false);

        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);

        String planResponse = "[{\"id\": \"t1\", \"name\": \"Write Example.java\", \"taskType\": \"file\"}]";
        String javaCode = "public class Example { public static void main(String[] args) { System.out.println(\"Hello\"); } }";
        String evalResponse = "{\"success\": true, \"comment\": \"Looks good\"}";

        mockLlm.setResponseSequence(new String[] {
            "{\"intent\":\"new\", \"confidence\":1.0, \"category\":\"CODING\", \"isAmbiguous\":false, \"refinedPrompt\":\"Write java example\"}", // Analytic
            planResponse, // Planner
            javaCode,     // JavaDev (Execution)
            evalResponse  // Validator -> Reviewer (Execution)
            // Constraint check is skipped because DesignModel is missing in shared memory
        });

        IterationManager manager = createManager(context);

        TaskRequest request = new TaskRequest("Write java example", tempDir);
        String result = manager.handle(request).getSummary();

        assertNotNull(result);

        File javaFile = new File(tempDir, "Example.java");
        assertTrue("Java file should exist", javaFile.exists());
        assertEquals(javaCode, Files.readString(javaFile.toPath()));
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

    private static class MockProvider implements ILlmProvider {
        private String[] responseSequence;
        private final AtomicInteger callCount = new AtomicInteger(0);
        private String defaultResponse = "{\"success\": true, \"comment\": \"Mock success\", \"intent\": \"continue\", \"category\": \"CODING\", \"isAmbiguous\": false}";

        public void setResponseSequence(String[] sequence) {
            this.responseSequence = sequence;
            this.callCount.set(0);
        }

        @Override
        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
            int current = callCount.getAndIncrement();
            return (responseSequence != null && current < responseSequence.length) ? responseSequence[current] : defaultResponse;
        }

        @Override
        public String testConnection(Orchestrator orchestrator, float temperature, String proxyUrl, TaskContext context) throws Exception {
            return "{\"status\": \"ok\"}";
        }
    }
}
