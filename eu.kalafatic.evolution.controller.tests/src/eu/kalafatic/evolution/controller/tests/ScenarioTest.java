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
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskExecutor;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngine;
import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.tools.ShellTool;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.SelfDevStatus;
import eu.kalafatic.evolution.model.orchestration.Ollama;

public class ScenarioTest {

    private File tempDir;
    private Orchestrator orchestrator;
    private MockProvider mockLlm;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("scenario-test").toFile();

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

        Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
        ollama.setUrl("http://localhost:11434");
        ollama.setModel("llama3");
        orchestrator.setOllama(ollama);

        mockLlm = new MockProvider();
    }

    @Test
    public void testScenario1_SimplePrompt() throws Exception {
        EvolutionOrchestrator engine = new EvolutionOrchestrator();
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);

        injectMocksIntoOrchestrator(engine, mockLlm);

        String planResponse = "[{\"id\": \"t1\", \"name\": \"Write src/Example.java\", \"taskType\": \"file\"}]";
        String javaCode = "public class Example { public static void main(String[] args) { System.out.println(\"Hello\"); } }";
        String evalResponse = "{\"success\": true, \"comment\": \"Looks good\"}";

        mockLlm.setResponseSequence(new String[] {
            "{\"category\":\"CODING\", \"isAmbiguous\":false}", // Analytic
            planResponse, // Planner
            javaCode,     // JavaDev
            evalResponse  // Reviewer
        });

        String result = engine.execute("Write java example", context);
        assertNotNull(result);

        File javaFile = new File(tempDir, "src/Example.java");
        assertTrue("Java file should exist", javaFile.exists());
        assertEquals(javaCode, Files.readString(javaFile.toPath()));
    }

    @Test
    public void testScenario2_PromptIterative() throws Exception {
        orchestrator.setDarwinMode(false);
        orchestrator.setIterativeMode(true);

        SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
        session.setId("iterative-session");
        session.setMaxIterations(3);
        session.setInitialRequest("Write java example and improve it");
        orchestrator.setSelfDevSession(session);

        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);

        // Responses for 3 iterations
        // Iteration 1: Create basic
        String plan1 = "[{\"id\": \"i1-t1\", \"name\": \"Write src/Example.java\", \"taskType\": \"file\"}]";
        String code1 = "public class Example { public static void main(String[] args) { System.out.println(\"Hello\"); } }";
        // Iteration 2: Add "hi"
        String plan2 = "[{\"id\": \"i2-t1\", \"name\": \"Update src/Example.java with hi\", \"taskType\": \"file\"}]";
        String code2 = "public class Example { public static void main(String[] args) { System.out.println(\"Hello\"); System.out.println(\"hi\"); } }";
        // Iteration 3: Add log
        String plan3 = "[{\"id\": \"i3-t1\", \"name\": \"Update src/Example.java with logging\", \"taskType\": \"file\"}]";
        String code3 = "public class Example { public static void main(String[] args) { System.out.println(\"Hello\"); System.out.println(\"hi\"); System.out.println(\"LOG: task complete\"); } }";

        String evalSuccess = "{\"success\": true, \"comment\": \"Looks good\", \"feedback\": \"Good\"}";

        mockLlm.setResponseSequence(new String[] {
            "{\"category\":\"CODING\", \"isAmbiguous\":false}", // Supervisor Analytic
            plan1, code1, evalSuccess, // Iteration 1: Plan, Code, Review
            plan2, code2, evalSuccess, // Iteration 2: Plan, Code, Review
            plan3, code3, evalSuccess  // Iteration 3: Plan, Code, Review
        });

        SelfDevSupervisor supervisor = createMockedSupervisor(session, context);
        supervisor.startSession();

        if (session.getStatus() != SelfDevStatus.COMPLETED) {
            printLogs(context);
        }
        assertEquals(SelfDevStatus.COMPLETED, session.getStatus());
        assertEquals(3, session.getIterations().size());

        File javaFile = new File(tempDir, "src/Example.java");
        assertTrue("Java file should exist at " + javaFile.getAbsolutePath(), javaFile.exists());
        String finalContent = Files.readString(javaFile.toPath());
        assertTrue("Content should contain 'hi'. Content: " + finalContent, finalContent.contains("\"hi\""));
        assertTrue("Content should contain LOG. Content: " + finalContent, finalContent.contains("\"LOG: task complete\""));
    }

    @Test
    public void testScenario3_PromptEvo() throws Exception {
        orchestrator.setDarwinMode(true);

        SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
        session.setId("evo-session");
        session.setMaxIterations(1);
        session.setInitialRequest("Write java example");
        orchestrator.setSelfDevSession(session);

        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);

        // DarwinEngine.generateVariants
        String variantsJson = "[{\"strategy\": \"Basic print\", \"suffix\": \"basic\"}, {\"strategy\": \"Advanced print\", \"suffix\": \"advanced\"}]";

        // Variant 1 (Basic)
        String planV1 = "[{\"id\": \"v1-t1\", \"name\": \"Write src/Example.java\", \"taskType\": \"file\"}]";
        String codeV1 = "public class Example { public static void main(String[] args) { System.out.println(\"Basic\"); } }";

        // Variant 2 (Advanced)
        String planV2 = "[{\"id\": \"v2-t1\", \"name\": \"Write src/Example.java\", \"taskType\": \"file\"}]";
        String codeV2 = "public class Example { public static void main(String[] args) { System.out.println(\"Advanced\"); } }";

        String evalSuccess = "{\"success\": true, \"comment\": \"Looks good\", \"feedback\": \"Good\"}";

        mockLlm.setResponseSequence(new String[] {
            "{\"category\":\"CODING\", \"isAmbiguous\":false}", // Supervisor Analytic
            variantsJson, // DarwinEngine.generateVariants
            planV1, codeV1, evalSuccess, // Variant 1 evaluation: Plan, Code, Review
            planV2, codeV2, evalSuccess, // Variant 2 evaluation: Plan, Code, Review
        });

        SelfDevSupervisor supervisor = createMockedSupervisor(session, context);
        supervisor.startSession();

        if (session.getStatus() != SelfDevStatus.COMPLETED) {
            printLogs(context);
        }
        assertEquals(SelfDevStatus.COMPLETED, session.getStatus());
        assertEquals(1, session.getIterations().size());

        File javaFile = new File(tempDir, "src/Example.java");
        assertTrue("Java file should exist", javaFile.exists());
    }

    @Test
    public void testScenario4_PromptEvoIterative() throws Exception {
        orchestrator.setDarwinMode(true);
        orchestrator.setIterativeMode(true);

        SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
        session.setId("evo-iter-session");
        session.setMaxIterations(2);
        session.setInitialRequest("Write java example");
        orchestrator.setSelfDevSession(session);

        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);

        String evalSuccess = "{\"success\": true, \"comment\": \"Looks good\", \"feedback\": \"Good\"}";

        mockLlm.setResponseSequence(new String[] {
            "{\"category\":\"CODING\", \"isAmbiguous\":false}", // Supervisor Analytic
            // Iteration 1
            "[{\"strategy\": \"S1\", \"suffix\": \"s1\"}]", // Variants
            "[{\"id\": \"i1t1\", \"name\": \"Write src/Example.java\", \"taskType\": \"file\"}]", // Plan S1
            "public class Example { public static void main(String[] args) { System.out.println(\"Iteration 1\"); } }", // Code S1
            evalSuccess, // Review S1
            // Iteration 2
            "[{\"strategy\": \"S2\", \"suffix\": \"s2\"}]", // Variants
            "[{\"id\": \"i2t1\", \"name\": \"Update src/Example.java\", \"taskType\": \"file\"}]", // Plan S2
            "public class Example { public static void main(String[] args) { System.out.println(\"Iteration 1\"); System.out.println(\"hi\"); } }", // Code S2
            evalSuccess, // Review S2
        });

        SelfDevSupervisor supervisor = createMockedSupervisor(session, context);
        supervisor.startSession();

        if (session.getStatus() != SelfDevStatus.COMPLETED) {
            printLogs(context);
        }
        assertEquals(SelfDevStatus.COMPLETED, session.getStatus());
        assertEquals(2, session.getIterations().size());

        File javaFile = new File(tempDir, "src/Example.java");
        assertTrue("Java file should exist", javaFile.exists());
        assertTrue(Files.readString(javaFile.toPath()).contains("\"hi\""));
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

        return supervisor;
    }

    private void injectMocksIntoOrchestrator(EvolutionOrchestrator engine, ILlmProvider mock) throws Exception {
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

        // Use recursive field injection to be sure
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

    private void printLogs(TaskContext context) {
        System.out.println("--- SESSION LOGS ---");
        for (String log : context.getLogs()) {
            System.out.println(log);
        }
        System.out.println("--------------------");
    }

    private static class MockEvaluator extends eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator {
        public MockEvaluator() {
            super(null, null);
        }
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
            return defaultResponse;
        }
    }
}
