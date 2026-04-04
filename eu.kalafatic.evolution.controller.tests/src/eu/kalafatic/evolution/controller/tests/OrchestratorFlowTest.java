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
import eu.kalafatic.evolution.controller.orchestration.IAgent;
import eu.kalafatic.evolution.controller.orchestration.PlannerAgent;
import eu.kalafatic.evolution.controller.orchestration.ReviewerAgent;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.BaseAiAgent;

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

        mockOllama = new MockProvider();
    }

    @Test
    public void testFullOrchestratorFlow() throws Exception {
        EvolutionOrchestrator engine = new EvolutionOrchestrator();
        TaskContext context = new TaskContext(orchestrator, tempDir);

        // Mock Approval
        context.addApprovalListener(message -> {
            context.provideApproval(true);
        });

        // Inject mocks into the engine's agents
        injectMocksIntoOrchestrator(engine, mockOllama);

        // Define AI responses
        // 1. Planner Response (JSON array of tasks)
        String planResponse = "[" +
                "{\"id\": \"t1\", \"name\": \"Write src/Main.java\", \"taskType\": \"file\"}," +
                "{\"id\": \"t2\", \"name\": \"Say Hello\", \"taskType\": \"llm\"}" +
                "]";

        // 2. JavaDevAgent Response (The Java code)
        String javaCode = "public class Main { public static void main(String[] args) { System.out.println(\"Hello\"); } }";

        // 3. Evaluation Responses (Success JSONs)
        String successEval = "{\"success\": true, \"comment\": \"Looks good\"}";

        mockOllama.setResponseSequence(new String[] {
            planResponse, // Planner
            javaCode,     // JavaDevAgent (for Write src/Main.java)
            successEval,  // Reviewer (for t1)
            "Hello from AI", // GeneralAgent (for Say Hello)
            successEval   // Reviewer (for t2)
        });

        // Execute
        String result = engine.execute("Create a java app", context);

        // Verification
        assertNotNull(result);

        // Check if file was created
        File javaFile = new File(tempDir, "src/Main.java");
        assertTrue("Java file should exist", javaFile.exists());
        String content = Files.readString(javaFile.toPath());
        assertEquals(javaCode, content);

        // Check task status in context (orchestrator model)
        assertEquals(2, orchestrator.getTasks().size());
        assertEquals(eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE, orchestrator.getTasks().get(0).getStatus());
        assertEquals(eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE, orchestrator.getTasks().get(1).getStatus());
    }

    private void injectMocksIntoOrchestrator(EvolutionOrchestrator engine, ILlmProvider mock) throws Exception {
        // Orchestrator has planner (PlannerAgent), reviewer (ReviewerAgent), and availableAgents (List<IAgent>)
        Field plannerField = EvolutionOrchestrator.class.getDeclaredField("planner");
        plannerField.setAccessible(true);
        injectMockIntoAgent((IAgent) plannerField.get(engine), mock);

        Field reviewerField = EvolutionOrchestrator.class.getDeclaredField("reviewer");
        reviewerField.setAccessible(true);
        injectMockIntoAgent((IAgent) reviewerField.get(engine), mock);

        Field agentsField = EvolutionOrchestrator.class.getDeclaredField("availableAgents");
        agentsField.setAccessible(true);
        List<IAgent> agents = (List<IAgent>) agentsField.get(engine);
        for (IAgent agent : agents) {
            injectMockIntoAgent(agent, mock);
        }
    }

    private void injectMockIntoAgent(IAgent agent, ILlmProvider mock) throws Exception {
        if (!(agent instanceof BaseAiAgent)) return;

        // BaseAiAgent has llmRouter and aiService
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
        }

        @Override
        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
            int current = callCount.getAndIncrement();
            return (responseSequence != null && current < responseSequence.length) ? responseSequence[current] : "Default Response";
        }
    }
}
