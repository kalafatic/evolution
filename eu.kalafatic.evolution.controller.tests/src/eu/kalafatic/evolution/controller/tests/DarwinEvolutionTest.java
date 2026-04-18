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
import eu.kalafatic.evolution.controller.orchestration.LlmIntentClassifier;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevSupervisor;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskExecutor;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngine;
import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.orchestration.AiService;
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

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("darwin-test").toFile();

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
        orchestrator.setPreferredMaxIterations(1);

        Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
        ollama.setUrl("http://localhost:11434");
        ollama.setModel("llama3");
        orchestrator.setOllama(ollama);

        mockLlm = new MockProvider();
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
            "{\"intent\":\"new\", \"confidence\":1.0}", // Supervisor Intent
            "{\"category\":\"CODING\", \"isAmbiguous\":false}", // Analytic
            variantJson, // Darwin variants
            "public class Validator { }", // Content generation
            evalSuccess  // Evaluator
        });

        SelfDevSupervisor supervisor = createMockedSupervisor(session, context);
        supervisor.startSession();

        assertEquals(SelfDevStatus.COMPLETED, session.getStatus());
        File valFile = new File(tempDir, "src/Validator.java");
        assertTrue("Validator file should exist", valFile.exists());
    }

    @Test
    public void testDarwinTrajectoryAwareness() throws Exception {
        // First iteration: fail with a specific strategy
        SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
        session.setId("trajectory-session");
        session.setMaxIterations(2);
        session.setInitialRequest("Refactor code");
        orchestrator.setSelfDevSession(session);
        orchestrator.setPreferredMaxIterations(2);

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
            "{\"intent\":\"new\"}", // Iteration 1 Intent
            "{\"category\":\"CODING\"}", // Iteration 1 Analytic
            failVariant, // Darwin proposes risky
            evalFail,    // Evaluator fails it
            "{\"intent\":\"continue\"}", // Iteration 2 Intent
            "{\"category\":\"CODING\"}", // Iteration 2 Analytic
            successVariant, // Darwin proposes safe (ideally it saw the failure in history)
            evalSuccess  // Evaluator passes it
        });

        SelfDevSupervisor supervisor = createMockedSupervisor(session, context);
        supervisor.startSession();

        // Verify history was accessed (Implicitly by the fact it progressed to Iteration 2 and handled the failure)
        List<String> logs = context.getLogs();
        // Check for the [DARWIN] log entry we added
        boolean historyFound = logs.stream().anyMatch(l -> l.contains("[DARWIN] History Analysis:"));
        if (!historyFound) {
            System.out.println("--- LOGS START ---");
            logs.forEach(s -> System.out.println(s));
            System.out.println("--- LOGS END ---");
        }
        assertTrue("Should contain history analysis in logs", historyFound);
    }

    private SelfDevSupervisor createMockedSupervisor(SelfDevSession session, TaskContext context) throws Exception {
        TaskPlanner planner = new TaskPlanner();
        injectMockIntoAgent(planner, mockLlm);
        TaskExecutor executor = new TaskExecutor(context);
        injectMocksIntoOrchestrator(getOrchestratorFromExecutor(executor), mockLlm);

        SelfDevSupervisor supervisor = new SelfDevSupervisor(session, context) {
            @Override
            protected IterationManager createIterationManager(eu.kalafatic.evolution.model.orchestration.Iteration iteration) {
                IterationManager im = new IterationManager(iteration, context, planner, executor);
                try {
                    Field deField = IterationManager.class.getDeclaredField("darwinEngine");
                    deField.setAccessible(true);
                    injectMocksIntoDarwin(deField.get(im), mockLlm, context);

                    Field evalField = IterationManager.class.getDeclaredField("evaluator");
                    evalField.setAccessible(true);
                    evalField.set(im, new MockEvaluator());
                } catch (Exception e) {}
                return im;
            }
        };

        Field analyticField = SelfDevSupervisor.class.getDeclaredField("analyticAgent");
        analyticField.setAccessible(true);
        injectMockIntoAgent(analyticField.get(supervisor), mockLlm);

        Field intentField = SelfDevSupervisor.class.getDeclaredField("intentClassifier");
        intentField.setAccessible(true);
        injectMockIntoAgent(intentField.get(supervisor), mockLlm);

        return supervisor;
    }

    private void injectMocksIntoOrchestrator(EvolutionOrchestrator engine, ILlmProvider mock) throws Exception {
        Field intentClassifierField = EvolutionOrchestrator.class.getDeclaredField("intentClassifier");
        intentClassifierField.setAccessible(true);
        injectMockIntoAgent(intentClassifierField.get(engine), mock);

        Field analyticAgentField = EvolutionOrchestrator.class.getDeclaredField("analyticAgent");
        analyticAgentField.setAccessible(true);
        injectMockIntoAgent(analyticAgentField.get(engine), mock);

        Field plannerField = EvolutionOrchestrator.class.getDeclaredField("planner");
        plannerField.setAccessible(true);
        injectMockIntoAgent(plannerField.get(engine), mock);

        Field reviewerField = EvolutionOrchestrator.class.getDeclaredField("reviewer");
        reviewerField.setAccessible(true);
        injectMockIntoAgent(reviewerField.get(engine), mock);

        Field agentsField = EvolutionOrchestrator.class.getDeclaredField("availableAgents");
        agentsField.setAccessible(true);
        List<IAgent> agents = (List<IAgent>) agentsField.get(engine);
        for (IAgent agent : agents) {
            injectMockIntoAgent(agent, mock);
        }
    }

    private void injectMocksIntoDarwin(Object darwinEngine, ILlmProvider mock, TaskContext context) throws Exception {
        injectMockIntoAgent(darwinEngine, mock);
        DarwinEngine de = (DarwinEngine) darwinEngine;

        TaskExecutor executor = new TaskExecutor(context);
        injectMocksIntoOrchestrator(getOrchestratorFromExecutor(executor), mock);
        de.setExecutor(executor);
        de.setEvaluator(new MockEvaluator());
    }

    private void injectMockIntoAgent(Object agent, ILlmProvider mock) throws Exception {
        if (agent == null) return;

        // System.out.println("DEBUG: Injecting mock into agent: " + agent.getClass().getName());

        if (agent instanceof LlmIntentClassifier) {
            Field field = LlmIntentClassifier.class.getDeclaredField("llmRouter");
            field.setAccessible(true);
            injectRecursive(field.get(agent), mock, 0);
        }

        injectRecursive(agent, mock, 0);
    }

    private void injectRecursive(Object obj, ILlmProvider mock, int depth) throws Exception {
        if (obj == null || depth > 5) return;
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value == null) continue;
                if (value instanceof LlmRouter) {
                    injectProviderIntoRouter((LlmRouter) value, mock);
                } else if (value instanceof AiService) {
                    injectRecursive(value, mock, depth + 1);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private void injectProviderIntoRouter(LlmRouter router, ILlmProvider mock) throws Exception {
        Field field = LlmRouter.class.getDeclaredField("ollamaProvider");
        field.setAccessible(true);
        field.set(router, mock);
    }

    private EvolutionOrchestrator getOrchestratorFromExecutor(TaskExecutor executor) throws Exception {
        Field orchField = TaskExecutor.class.getDeclaredField("orchestrator");
        orchField.setAccessible(true);
        return (EvolutionOrchestrator) orchField.get(executor);
    }

    private static class MockEvaluator extends eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator {
        public MockEvaluator() { super(null, null); }
        @Override
        public eu.kalafatic.evolution.model.orchestration.EvaluationResult evaluate() throws Exception {
            eu.kalafatic.evolution.model.orchestration.EvaluationResult res = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createEvaluationResult();
            res.setSuccess(true);
            res.setDecision(eu.kalafatic.evolution.model.orchestration.SelfDevDecision.CONTINUE);
            res.setTestPassRate(1.0);
            return res;
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
            String resp = defaultResponse;
            if (responseSequence != null && current < responseSequence.length) resp = responseSequence[current];

            if (context != null) {
                context.log("[MOCK] Call " + current + " prompt snippet: " + prompt.substring(0, Math.min(prompt.length(), 100)).replace("\n", " "));
                context.log("[MOCK] Call " + current + " response: " + resp);
            }
            return resp;
        }
    }
}
