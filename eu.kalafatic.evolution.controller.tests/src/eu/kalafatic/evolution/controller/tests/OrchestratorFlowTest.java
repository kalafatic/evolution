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
import eu.kalafatic.evolution.controller.orchestration.LlmIntentClassifier;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.agents.PlannerAgent;
import eu.kalafatic.evolution.controller.agents.ReviewerAgent;
import eu.kalafatic.evolution.controller.orchestration.AiService;

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
            "{\"mode\":\"ASSISTED_CODING\", \"confidence\":\"HIGH\"}", // ContextAssistant
            "{\"intent\":\"new\", \"confidence\":0.9}", // IntentGate
            "{\"category\":\"CODING\", \"isAmbiguous\":false}", // AnalyticAgent
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

    @Test
    public void testGreetingResponse() throws Exception {
        EvolutionOrchestrator engine = new EvolutionOrchestrator();
        TaskContext context = new TaskContext(orchestrator, tempDir);

        injectMocksIntoOrchestrator(engine, mockOllama);

        // Mock Sequence:
        // 0. ContextAssistant -> SIMPLE_CHAT
        // 1. Intent Classification -> 'chat'
        mockOllama.setResponseSequence(new String[] {
            "{\"mode\":\"SIMPLE_CHAT\", \"confidence\":\"HIGH\"}",
            "{\"intent\":\"chat\", \"confidence\":0.9, \"needs_clarification\":false, \"reason\":\"greeting\"}"
        });

        // Execute
        String result = engine.execute("hi", context);

        // Verification
        assertNotNull(result);
        assertTrue("Response should be a greeting", result.contains("Hello! I'm Evo"));
    }

    @Test
    public void testClarificationWait() throws Exception {
        EvolutionOrchestrator engine = new EvolutionOrchestrator();
        TaskContext context = new TaskContext(orchestrator, tempDir);


        // Inject mocks
        injectMocksIntoOrchestrator(engine, mockOllama);

        // Mock Planner Response
        String planResponse = "[{\"id\": \"t1\", \"name\": \"Say Hello\", \"taskType\": \"llm\"}]";

        // Mock sequence:
        // 0. ContextAssistant -> SIMPLE_CHAT
        // 1. IntentGate -> new
        // 2. Analytic -> CHAT, clear
        // 3. Planner -> planResponse
        // 4. GeneralAgent (performAction attempt 1) -> CLARIFY
        // 5. GeneralAgent (performAction attempt 2 after input) -> "Hello verified"
        // 6. ReviewerAgent -> success
        mockOllama.setResponseSequence(new String[] {
            "{\"mode\":\"SIMPLE_CHAT\", \"confidence\":\"HIGH\"}",
            "{\"intent\":\"new\", \"confidence\":0.9}",
            "{\"category\":\"CHAT\", \"isAmbiguous\":false}",
            planResponse,
            "CLARIFY: What do you mean by hello?",
            "Hello verified",
            "{\"success\": true}"
        });

        // Mock User Input for Clarification
        context.addInputListener(message -> {
            context.provideInput("I mean Hi");
        });

        // Execute - use something that isn't a simple greeting to bypass PolicyEngine directly
        // Use 'Execute' to pass the Planner's safety guard.
        String result = engine.execute("Execute Say Hello", context);

        assertNotNull(result);
        assertNotNull(result);
    }

    private void injectMocksIntoOrchestrator(EvolutionOrchestrator engine, ILlmProvider mock) throws Exception {
        // Orchestrator has planner (PlannerAgent), reviewer (ReviewerAgent), and availableAgents (List<IAgent>)
        Field plannerField = EvolutionOrchestrator.class.getDeclaredField("planner");
        plannerField.setAccessible(true);
        injectMockIntoAgent((IAgent) plannerField.get(engine), mock);

        Field reviewerField = EvolutionOrchestrator.class.getDeclaredField("reviewer");
        reviewerField.setAccessible(true);
        injectMockIntoAgent((IAgent) reviewerField.get(engine), mock);

        Field intentClassifierField = EvolutionOrchestrator.class.getDeclaredField("intentClassifier");
        intentClassifierField.setAccessible(true);
        Object classifier = intentClassifierField.get(engine);
        if (classifier instanceof LlmIntentClassifier) {
            Field routerField = LlmIntentClassifier.class.getDeclaredField("llmRouter");
            routerField.setAccessible(true);
            LlmRouter router = (LlmRouter) routerField.get(classifier);
            injectProviderIntoRouter(router, mock);
        }

        Field contextAssistantField = EvolutionOrchestrator.class.getDeclaredField("contextAssistant");
        contextAssistantField.setAccessible(true);
        Object contextAssistant = contextAssistantField.get(engine);
        Field caRouterField = contextAssistant.getClass().getDeclaredField("llmRouter");
        caRouterField.setAccessible(true);
        injectProviderIntoRouter((LlmRouter) caRouterField.get(contextAssistant), mock);

        Field analyticAgentField = EvolutionOrchestrator.class.getDeclaredField("analyticAgent");
        analyticAgentField.setAccessible(true);
        injectMockIntoAgent((IAgent) analyticAgentField.get(engine), mock);

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
