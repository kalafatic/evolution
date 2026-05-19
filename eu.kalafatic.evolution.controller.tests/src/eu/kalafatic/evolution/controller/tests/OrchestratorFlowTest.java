package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.TransitionToken;
import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;

public class OrchestratorFlowTest {

    private File tempDir;
    private Orchestrator orchestrator;
    private MockProvider mockOllama;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("evo-flow-test").toFile();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId("test-flow");
        orchestrator.setAiMode(AiMode.LOCAL);

        Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
        ollama.setUrl("http://localhost:11434");
        ollama.setModel("llama3");
        orchestrator.setOllama(ollama);

        orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());

        mockOllama = new MockProvider();
    }

    @Test
    public void testDirectExecutionFlow() throws Exception {
        EvolutionOrchestrator engine = new EvolutionOrchestrator();
        TaskContext context = new TaskContext(orchestrator, tempDir);
        IterationManager.forceTransition(SystemState.EXECUTING, context);

        // Inject mocks
        injectMocksIntoOrchestrator(engine, mockOllama);

        mockOllama.setResponseSequence(new String[] {
            "Hello from direct execution"
        });

        // Execute direct prompt (no tasks)
        String result = engine.execute("hi", context);

        // Verification
        assertNotNull(result);
        assertTrue("Result was: " + result, result.contains("Hello from direct execution"));
    }

    @Test
    public void testTaskExecutionFlow() throws Exception {
        EvolutionOrchestrator engine = new EvolutionOrchestrator();
        TaskContext context = new TaskContext(orchestrator, tempDir);
        IterationManager.forceTransition(SystemState.EXECUTING, context);

        // Define a task
        var task = OrchestrationFactory.eINSTANCE.createTask();
        task.setId("t1");
        task.setName("Write src/Hello.java");
        task.setType("file");
        task.setDescription("Create a hello class");
        orchestrator.getTasks().add(task);

        // Inject mocks
        injectMocksIntoOrchestrator(engine, mockOllama);

        String javaCode = "public class Hello {}";
        mockOllama.setResponseSequence(new String[] {
            javaCode
        });

        // Execute
        String result = engine.handle(new eu.kalafatic.evolution.controller.orchestration.TaskRequest("run", tempDir), context).getSummary();

        assertNotNull(result);
        File helloFile = new File(tempDir, "src/Hello.java");
        assertTrue("Hello file should exist", helloFile.exists());
        assertEquals(javaCode, Files.readString(helloFile.toPath()));
        assertEquals(eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE, task.getStatus());
    }

    private void injectMocksIntoOrchestrator(EvolutionOrchestrator engine, ILlmProvider mock) throws Exception {
        String[] agentFields = {"analyticAgent", "validator", "repairAgent", "consolidator"};
        for (String fieldName : agentFields) {
            try {
                Field f = EvolutionOrchestrator.class.getDeclaredField(fieldName);
                f.setAccessible(true);
                IAgent agent = (IAgent) f.get(engine);
                if (agent != null) injectMockIntoAgent(agent, mock);
            } catch (NoSuchFieldException e) { }
        }

        Field agentsField = EvolutionOrchestrator.class.getDeclaredField("availableAgents");
        agentsField.setAccessible(true);
        List<IAgent> agents = (List<IAgent>) agentsField.get(engine);
        for (IAgent agent : agents) {
            injectMockIntoAgent(agent, mock);
        }

        Field aiServiceField = EvolutionOrchestrator.class.getDeclaredField("aiService");
        aiServiceField.setAccessible(true);
        AiService aiService = (AiService) aiServiceField.get(engine);
        Field serviceRouterField = AiService.class.getDeclaredField("llmRouter");
        serviceRouterField.setAccessible(true);
        LlmRouter serviceRouter = (LlmRouter) serviceRouterField.get(aiService);
        injectProviderIntoRouter(serviceRouter, mock);
    }

    private void injectMockIntoAgent(IAgent agent, ILlmProvider mock) throws Exception {
        if (!(agent instanceof BaseAiAgent)) return;

        Field routerField = BaseAiAgent.class.getDeclaredField("llmRouter");
        routerField.setAccessible(true);
        LlmRouter router = (LlmRouter) routerField.get(agent);
        injectProviderIntoRouter(router, mock);

        Field serviceField = BaseAiAgent.class.getDeclaredField("aiService");
        serviceField.setAccessible(true);
        AiService service = (AiService) serviceField.get(agent);

        Field serviceRouterField = AiService.class.getDeclaredField("llmRouter");
        serviceRouterField.setAccessible(true);
        LlmRouter serviceRouter = (LlmRouter) serviceRouterField.get(service);
        injectProviderIntoRouter(serviceRouter, mock);
    }

    private void injectProviderIntoRouter(LlmRouter router, ILlmProvider mock) throws Exception {
        Field field = LlmRouter.class.getDeclaredField("ollamaProvider");
        field.setAccessible(true);
        field.set(router, mock);
    }

    private static class MockProvider implements ILlmProvider {
        private String[] responseSequence;
        private final AtomicInteger callCount = new AtomicInteger(0);

        public void setResponseSequence(String[] sequence) {
            this.responseSequence = sequence;
            this.callCount.set(0);
        }

        @Override
        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
            int current = callCount.getAndIncrement();
            return (responseSequence != null && current < responseSequence.length) ? responseSequence[current] : "Default Response";
        }
    }
}
