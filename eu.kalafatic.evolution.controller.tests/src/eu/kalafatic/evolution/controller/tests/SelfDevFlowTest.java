package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevSupervisor;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner;
import eu.kalafatic.evolution.controller.tools.ShellTool;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskExecutor;
import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.SelfDevStatus;
import eu.kalafatic.evolution.model.orchestration.Ollama;

public class SelfDevFlowTest {

    private File tempDir;
    private Orchestrator orchestrator;
    private MockProvider mockLlm;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("self-dev-test").toFile();

        // Initialize Git repo
        ShellTool shell = new ShellTool();
        TaskContext initContext = new TaskContext(OrchestrationFactory.eINSTANCE.createOrchestrator(), tempDir);
        shell.execute("git init", tempDir, initContext);
        shell.execute("git config user.email \"test@example.com\"", tempDir, initContext);
        shell.execute("git config user.name \"Test User\"", tempDir, initContext);
        Files.writeString(new File(tempDir, "pom.xml").toPath(), "<project><modelVersion>4.0.0</modelVersion><groupId>test</groupId><artifactId>test</artifactId><version>1.0</version></project>");
        shell.execute("git add .", tempDir, initContext);
        shell.execute("git commit -m \"Initial commit\"", tempDir, initContext);

        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId("test-selfdev");
        orchestrator.setAiMode(AiMode.LOCAL);

        Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
        ollama.setUrl("http://localhost:11434");
        ollama.setModel("llama3");
        orchestrator.setOllama(ollama);

        if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        if (orchestrator.getAiChat().getPromptInstructions() == null) orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());
        orchestrator.getAiChat().getPromptInstructions().setPreferredMaxIterations(-1);

        mockLlm = new MockProvider();
    }

    @Test
    public void testSuccessfulSelfDevIteration() throws Exception {
        SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
        session.setId("session-1");
        session.setMaxIterations(1);

        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.addApprovalListener(message -> context.provideApproval(true));

        String planResponse = "[{\"id\": \"sd1\", \"name\": \"Improve Readme\", \"taskType\": \"llm\", \"priority\": 1}]";
        String taskResponse = "Updated Readme content";
        String evalResponse = "{\"success\": true, \"comment\": \"Build passed\"}";

        mockLlm.setResponseSequence(new String[] {
            planResponse, // TaskPlanner.generateTasks
            taskResponse, // Agent.process (Attempt 1)
            evalResponse  // Reviewer.evaluate (Attempt 1)
        });

        TaskPlanner planner = new TaskPlanner();
        injectMockIntoAgent(planner, mockLlm);

        TaskExecutor executor = new TaskExecutor(context);
        Field orchField = TaskExecutor.class.getDeclaredField("orchestrator");
        orchField.setAccessible(true);
        EvolutionOrchestrator evoOrch = (EvolutionOrchestrator) orchField.get(executor);
        injectMocksIntoOrchestrator(evoOrch, mockLlm);

        SelfDevSupervisor supervisor = createMockedSupervisor(session, context, planner, executor, true);

        supervisor.startSession();

        if (session.getStatus() == SelfDevStatus.FAILED) {
            printLogs(context);
        }

        assertEquals(SelfDevStatus.COMPLETED, session.getStatus());
        assertEquals(1, session.getIterations().size());
        eu.kalafatic.evolution.model.orchestration.Iteration iteration = session.getIterations().get(0);
        assertEquals(eu.kalafatic.evolution.model.orchestration.IterationStatus.DONE, iteration.getStatus());
        assertEquals("LEARN", iteration.getPhase());
    }

    @Test
    public void testFailedSelfDevIteration() throws Exception {
        SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
        session.setId("session-failed");
        session.setMaxIterations(1);

        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.addApprovalListener(message -> context.provideApproval(true));

        String planResponse = "[{\"id\": \"sd2\", \"name\": \"Fail Task\", \"taskType\": \"llm\", \"priority\": 1}]";
        String taskResponse = "Some content";
        String failEval = "{\"success\": false, \"feedback\": \"Try again\", \"decision\": \"ROLLBACK\"}";

        // Fails after 3 retries in Orchestrator
        mockLlm.setResponseSequence(new String[] {
            planResponse,
            taskResponse, failEval, // Attempt 1
            taskResponse, failEval, // Attempt 2
            taskResponse, failEval  // Attempt 3
        });

        TaskPlanner planner = new TaskPlanner();
        injectMockIntoAgent(planner, mockLlm);
        TaskExecutor executor = new TaskExecutor(context);
        Field orchField = TaskExecutor.class.getDeclaredField("orchestrator");
        orchField.setAccessible(true);
        injectMocksIntoOrchestrator((EvolutionOrchestrator) orchField.get(executor), mockLlm);

        SelfDevSupervisor supervisor = createMockedSupervisor(session, context, planner, executor, false);

        supervisor.startSession();

        assertEquals(SelfDevStatus.COMPLETED, session.getStatus());
        assertEquals(1, session.getIterations().size());
        assertEquals(eu.kalafatic.evolution.model.orchestration.IterationStatus.FAILED, session.getIterations().get(0).getStatus());
    }

    @Test
    public void testIterativeDevelopmentFlow() throws Exception {
        SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
        session.setId("session-iterative");
        session.setMaxIterations(1);
        String customRequest = "Add a new C++ class for Arduino";
        session.setInitialRequest(customRequest);
        orchestrator.setSelfDevSession(session);

        if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        if (orchestrator.getAiChat().getPromptInstructions() == null) orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());
        orchestrator.getAiChat().getPromptInstructions().setIterativeMode(true);

        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.addApprovalListener(message -> context.provideApproval(true));

        String planResponse = "[{\"id\": \"it1\", \"name\": \"Create Arduino Class\", \"taskType\": \"file\", \"priority\": 1, \"rationale\": \"To fulfill user request\"}]";
        String taskResponse = "C++ code content";
        String evalResponse = "{\"success\": true, \"comment\": \"Build simulated success\"}";

        mockLlm.setResponseSequence(new String[] {
            planResponse,
            taskResponse,
            evalResponse
        });

        TaskPlanner planner = new TaskPlanner();
        injectMockIntoAgent(planner, mockLlm);

        TaskExecutor executor = new TaskExecutor(context);
        Field orchField = TaskExecutor.class.getDeclaredField("orchestrator");
        orchField.setAccessible(true);
        injectMocksIntoOrchestrator((EvolutionOrchestrator) orchField.get(executor), mockLlm);

        SelfDevSupervisor supervisor = createMockedSupervisor(session, context, planner, executor, true);

        supervisor.startSession();

        assertEquals(SelfDevStatus.COMPLETED, session.getStatus());
        assertEquals(1, session.getIterations().size());

        boolean found = false;
        for (String log : context.getLogs()) {
            if (log != null && log.contains(customRequest)) {
                found = true;
                break;
            }
        }
        if (!found) {
            printLogs(context);
        }
        assertTrue("Log message containing '" + customRequest + "' not found in session logs", found);
    }

    @Test
    public void testMaxFailures() throws Exception {
        SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
        session.setId("session-max-failures");
        session.setMaxIterations(5);

        if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        if (orchestrator.getAiChat().getPromptInstructions() == null) orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());
        orchestrator.getAiChat().getPromptInstructions().setPreferredMaxIterations(5);

        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.addApprovalListener(message -> context.provideApproval(true));

        String planResponse = "[{\"id\": \"fail\", \"name\": \"Failure\", \"taskType\": \"llm\"}]";
        String failEval = "{\"success\": false, \"decision\": \"ROLLBACK\"}";

        // Return a sequence that ensures failure for 3 iterations
        mockLlm.setResponseSequence(new String[] {
            planResponse, "out", failEval, "out", failEval, "out", failEval, // Iteration 1
            planResponse, "out", failEval, "out", failEval, "out", failEval, // Iteration 2
            planResponse, "out", failEval, "out", failEval, "out", failEval  // Iteration 3
        });
        mockLlm.setDefaultResponse(failEval);

        TaskPlanner planner = new TaskPlanner();
        injectMockIntoAgent(planner, mockLlm);
        TaskExecutor executor = new TaskExecutor(context);
        Field orchField = TaskExecutor.class.getDeclaredField("orchestrator");
        orchField.setAccessible(true);
        injectMocksIntoOrchestrator((EvolutionOrchestrator) orchField.get(executor), mockLlm);

        SelfDevSupervisor supervisor = createMockedSupervisor(session, context, planner, executor, false);

        supervisor.startSession();

        assertEquals(SelfDevStatus.FAILED, session.getStatus());
        assertEquals(3, session.getIterations().size());
    }

    private void printLogs(TaskContext context) {
        System.out.println("Logs for failed session:");
        for (String log : context.getLogs()) {
            System.out.println("  " + log);
        }
    }

    private SelfDevSupervisor createMockedSupervisor(SelfDevSession session, TaskContext context, TaskPlanner planner, TaskExecutor executor, boolean iterationSuccess) {
        return new SelfDevSupervisor(session, context) {
            @Override
            protected IterationManager createIterationManager(eu.kalafatic.evolution.model.orchestration.Iteration iteration) {
                IterationManager im = new IterationManager(iteration, context, planner, executor);
                try {
                    Field evalField = IterationManager.class.getDeclaredField("evaluator");
                    evalField.setAccessible(true);
                    MockEvaluator mockEval = new MockEvaluator();
                    mockEval.setSuccess(iterationSuccess);
                    evalField.set(im, mockEval);
                } catch (Exception e) {}
                return im;
            }
        };
    }

    private void injectMocksIntoOrchestrator(EvolutionOrchestrator engine, ILlmProvider mock) throws Exception {
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
        private String defaultResponse = "{\"success\": true, \"comment\": \"Mock success\"}";

        public void setResponseSequence(String[] sequence) {
            this.responseSequence = sequence;
            this.callCount.set(0);
        }

        public void setDefaultResponse(String defaultResponse) {
            this.defaultResponse = defaultResponse;
        }

        @Override
        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
            // Intelligent defaults for infrastructure calls
            if (prompt.contains("Intent Gate") || prompt.contains("IntentClassifier") || prompt.contains("Intent Classifier")) return "{\"intent\": \"new\", \"confidence\": 1.0}";
            if (prompt.contains("Analytic Agent") || prompt.contains("AnalyticAgent") || prompt.contains("Analytic Phase")) return "{\"category\": \"CODING\", \"isAmbiguous\": false}";

            int current = callCount.getAndIncrement();
            if (responseSequence != null && current < responseSequence.length) {
                return responseSequence[current];
            }
            return defaultResponse;
        }

        @Override
        public String testConnection(Orchestrator orchestrator, float temperature, String proxyUrl, TaskContext context) throws Exception {
            return "{\"status\": \"ok\"}";
        }
    }

    private static class MockEvaluator extends eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator {
        private boolean success = true;
        public MockEvaluator() { super(new File("."), null); }
        public void setSuccess(boolean success) { this.success = success; }
        @Override
        public eu.kalafatic.evolution.model.orchestration.EvaluationResult evaluate() throws Exception {
            return evaluateWithSnapshot().result;
        }

        @Override
        public Evaluation evaluateWithSnapshot() throws Exception {
            eu.kalafatic.evolution.model.orchestration.EvaluationResult res = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createEvaluationResult();
            res.setSuccess(success);
            res.setDecision(success ? eu.kalafatic.evolution.model.orchestration.SelfDevDecision.CONTINUE : eu.kalafatic.evolution.model.orchestration.SelfDevDecision.ROLLBACK);
            res.setTestPassRate(success ? 1.0 : 0.0);

            Evaluation eval = new Evaluation();
            eval.result = res;
            eval.snapshot = new eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot();
            eval.snapshot.build.status = success ? eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot.BuildStatus.SUCCESS : eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot.BuildStatus.FAIL;
            return eval;
        }
    }
}
