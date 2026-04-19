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
import eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevSupervisor;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskExecutor;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.tools.ShellTool;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.SelfDevStatus;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.model.orchestration.Task;

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

        if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        if (orchestrator.getAiChat().getPromptInstructions() == null) orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());
        orchestrator.getAiChat().getPromptInstructions().setPreferredMaxIterations(0);

        Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
        ollama.setUrl("http://localhost:11434");
        ollama.setModel("llama3");
        orchestrator.setOllama(ollama);

        mockLlm = new MockProvider();
    }

    @Test
    public void testScenario1_SimplePrompt() throws Exception {
        EvolutionOrchestrator engine = new EvolutionOrchestrator();

        if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        if (orchestrator.getAiChat().getPromptInstructions() == null) orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());
        orchestrator.getAiChat().getPromptInstructions().setIterativeMode(false);
        orchestrator.getAiChat().getPromptInstructions().setSelfIterativeMode(false);

        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);
        context.setPlatformMode(new eu.kalafatic.evolution.controller.orchestration.PlatformMode(
            eu.kalafatic.evolution.controller.orchestration.PlatformType.ASSISTED_CODING,
            eu.kalafatic.evolution.controller.orchestration.AutonomyLevel.LOW, 2, false));

        injectMocksIntoOrchestrator(engine, mockLlm);

        String planResponse = "[{\"id\": \"t1\", \"name\": \"Write Example.java\", \"taskType\": \"file\"}]";
        String javaCode = "public class Example { public static void main(String[] args) { System.out.println(\"Hello\"); } }";
        String evalResponse = "{\"success\": true, \"comment\": \"Looks good\"}";

        mockLlm.setResponseSequence(new String[] {
            "{\"intent\":\"new\", \"confidence\":1.0}", // Intent Classifier
            "{\"category\":\"CODING\", \"isAmbiguous\":false}", // Analytic
            planResponse, // Planner
            javaCode,     // JavaDev
            evalResponse  // Reviewer
        });

        String result = null;
        try {
            result = engine.execute("Write java example", context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result == null) {
            printLogs(context);
        }
        assertNotNull(result);

        File javaFile = new File(tempDir, "Example.java");
        if (!javaFile.exists()) {
            printLogs(context);
        }
        assertTrue("Java file should exist", javaFile.exists());
        assertEquals(javaCode, Files.readString(javaFile.toPath()));
    }

    @Test
    public void testScenario2_PromptIterative() throws Exception {
        orchestrator.setDarwinMode(false);
        orchestrator.getAiChat().getPromptInstructions().setIterativeMode(true);

        SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
        session.setId("iterative-session");
        session.setMaxIterations(3);
        session.setInitialRequest("Write java example and improve it");
        orchestrator.setSelfDevSession(session);

        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);

        // Responses for 3 iterations
        String plan1 = "[{\"id\": \"i1-t1\", \"name\": \"Write src/Example.java\", \"taskType\": \"file\"}]";
        String code1 = "public class Example { public static void main(String[] args) { System.out.println(\"Hello\"); } }";
        String plan2 = "[{\"id\": \"i2-t1\", \"name\": \"Update src/Example.java with hi\", \"taskType\": \"file\"}]";
        String code2 = "public class Example { public static void main(String[] args) { System.out.println(\"Hello\"); System.out.println(\"hi\"); } }";
        String plan3 = "[{\"id\": \"i3-t1\", \"name\": \"Update src/Example.java with logging\", \"taskType\": \"file\"}]";
        String code3 = "public class Example { public static void main(String[] args) { System.out.println(\"Hello\"); System.out.println(\"hi\"); System.out.println(\"LOG: task complete\"); } }";

        String evalSuccess = "{\"success\": true, \"comment\": \"Looks good\", \"feedback\": \"Good\"}";

        mockLlm.setResponseSequence(new String[] {
            "{\"category\":\"CODING\", \"isAmbiguous\":false}", // Analytic
            plan1, code1, evalSuccess, // Iteration 1
            plan2, code2, evalSuccess, // Iteration 2
            plan3, code3, evalSuccess  // Iteration 3
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

        String codeV1 = "public class Example { public static void main(String[] args) { System.out.println(\"Basic\"); } }";
        String codeV2 = "public class Example { public static void main(String[] args) { System.out.println(\"Advanced\"); } }";
        String evalSuccess = "{\"success\": true, \"comment\": \"Looks good\", \"feedback\": \"Good\"}";

        String vJson = "[" +
            "{\"strategy\": \"S1\", \"suffix\": \"s1\", \"actions\": [{\"domain\":\"file\", \"operation\":\"Write\", \"target\":\"src/Example.java\", \"description\":\"S1\"}]}, " +
            "{\"strategy\": \"S2\", \"suffix\": \"s2\", \"actions\": [{\"domain\":\"file\", \"operation\":\"Write\", \"target\":\"src/Example.java\", \"description\":\"S2\"}]}" +
            "]";

        mockLlm.setResponseSequence(new String[] {
            "{\"category\":\"CODING\", \"isAmbiguous\":false}", // Analytic
            vJson, // DarwinEngine.generateVariants
            codeV1, evalSuccess, // Variant 1
            codeV2, evalSuccess, // Variant 2
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
        orchestrator.getAiChat().getPromptInstructions().setIterativeMode(true);

        SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
        session.setId("evo-iter-session");
        session.setMaxIterations(2);
        session.setInitialRequest("Write java example");
        orchestrator.setSelfDevSession(session);

        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);

        String evalSuccess = "{\"success\": true, \"comment\": \"Looks good\", \"feedback\": \"Good\"}";
        mockLlm.setResponseSequence(new String[] {
            "{\"category\":\"CODING\", \"isAmbiguous\":false}", // Analytic
            // Iteration 1
            "[{\"strategy\": \"S1\", \"suffix\": \"s1\", \"actions\": [{\"domain\":\"file\", \"operation\":\"Write\", \"target\":\"src/Example.java\", \"description\":\"Iter1\"}]}]", // Variants
            "public class Example { public static void main(String[] args) { System.out.println(\"Iteration 1\"); } }", // Code S1
            evalSuccess, // Review S1
            // Iteration 2
            "[{\"strategy\": \"S2\", \"suffix\": \"s2\", \"actions\": [{\"domain\":\"file\", \"operation\":\"Write\", \"target\":\"src/Example.java\", \"description\":\"Iter2\"}]}]", // Variants
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

            @Override
            protected JSONObject analyzeIntent(String request) throws Exception {
                JSONObject json = new JSONObject();
                json.put("intent", "new");
                json.put("confidence", 1.0);
                return json;
            }
        };

        injectMockIntoAgent(supervisor, mockLlm);

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
        injectRecursive(agent, mock, 0);
    }

    private void injectRecursive(Object obj, ILlmProvider mock, int depth) throws Exception {
        if (obj == null || depth > 15) return;

        String name = obj.getClass().getName();
        if (name.startsWith("java.") || name.startsWith("org.json") || name.startsWith("org.eclipse.emf") || name.startsWith("org.eclipse.core") || name.startsWith("eu.kalafatic.evolution.model")) return;

        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value == null || value == obj) continue;

                    if (value instanceof LlmRouter) {
                        injectProviderIntoRouter((LlmRouter) value, mock);
                    } else if (value instanceof AiService) {
                        Field routerField = AiService.class.getDeclaredField("llmRouter");
                        routerField.setAccessible(true);
                        injectProviderIntoRouter((LlmRouter) routerField.get(value), mock);
                    } else if (!value.getClass().isPrimitive() && !(value instanceof String) && !(value instanceof Number) && !(value instanceof Boolean) && !(value instanceof Enum)) {
                        injectRecursive(value, mock, depth + 1);
                    }
                } catch (Throwable t) {}
            }
            clazz = clazz.getSuperclass();
        }
    }

    private void injectProviderIntoRouter(LlmRouter router, ILlmProvider mock) throws Exception {
        if (router == null) return;
        Class<?> clazz = LlmRouter.class;
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType().isAssignableFrom(mock.getClass()) || field.getType().equals(ILlmProvider.class)) {
                    field.setAccessible(true);
                    field.set(router, mock);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private EvolutionOrchestrator getOrchestratorFromExecutor(TaskExecutor executor) throws Exception {
        Field orchField = TaskExecutor.class.getDeclaredField("orchestrator");
        orchField.setAccessible(true);
        return (EvolutionOrchestrator) orchField.get(executor);
    }

    private void printLogs(TaskContext context) {
        System.err.println("--- SESSION LOGS ---");
        for (String log : context.getLogs()) {
            System.err.println(log);
        }
        System.err.println("--------------------");
    }

    private static class MockEvaluator extends eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator {
        public MockEvaluator() { super(new File("."), null); }
        @Override
        public eu.kalafatic.evolution.model.orchestration.EvaluationResult evaluate() throws Exception {
            return evaluateWithSnapshot().result;
        }

        @Override
        public Evaluation evaluateWithSnapshot() throws Exception {
            eu.kalafatic.evolution.model.orchestration.EvaluationResult res = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createEvaluationResult();
            res.setSuccess(true);
            res.setDecision(eu.kalafatic.evolution.model.orchestration.SelfDevDecision.CONTINUE);
            res.setTestPassRate(1.0);

            Evaluation eval = new Evaluation();
            eval.result = res;
            eval.snapshot = new eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot();
            eval.snapshot.build.status = eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot.BuildStatus.SUCCESS;
            return eval;
        }
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
            // Intelligent override based on prompt content if sequence is exhausted or mismatching
            if (responseSequence != null && callCount.get() >= responseSequence.length) {
                if (prompt.contains("Intent Gate")) return "{\"intent\": \"continue\", \"confidence\": 1.0}";
                if (prompt.contains("Analytic")) return "{\"category\":\"CODING\", \"isAmbiguous\":false}";
            }

            int current = callCount.getAndIncrement();
            String res = (responseSequence != null && current < responseSequence.length) ? responseSequence[current] : defaultResponse;

            // Darwin robustness
            if (prompt.contains("DarwinEngine") && !res.startsWith("[")) res = "[{\"strategy\": \"Fallback\", \"suffix\": \"fb\", \"actions\": []}]";
            // Planner robustness
            if (prompt.contains("Planner") && !res.startsWith("[") && !res.startsWith("{")) res = "[]";

            if (context != null) {
                context.log("[MOCK] Call " + current + " prompt snippet: " + prompt.substring(0, Math.min(prompt.length(), 100)).replace("\n", " "));
                context.log("[MOCK] Call " + current + " response: " + res);
            }
            return res;
        }

        @Override
        public String testConnection(Orchestrator orchestrator, float temperature, String proxyUrl, TaskContext context) throws Exception {
            return "{\"status\": \"ok\"}";
        }
    }
}
