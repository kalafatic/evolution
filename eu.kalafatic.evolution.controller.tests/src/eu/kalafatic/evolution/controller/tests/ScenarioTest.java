package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.SessionContext;
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
            @Override
            public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context, String forcedModel) throws Exception {
                return mockLlm.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
            @Override
            public String sendRequest(Orchestrator orchestrator, String prompt, TaskContext context) throws Exception {
                return mockLlm.sendRequest(orchestrator, prompt, 0.7f, null, context);
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
        context.getMetadata().put("testMode", true);

        String javaCode = "public class Example { public static void main(String[] args) { System.out.println(\"Hello\"); } }";
        String evalResponse = "{\"success\": true, \"comment\": \"Looks good\", \"feedback\": \"Looks good\"}";
        String intentExpansion = "{\"state\": \"CLEAR\", \"dominantIntent\": \"create java class Example\", \"hypotheses\": [{\"id\": \"h1\", \"description\": \"Create Example.java\", \"dimensionValues\": []}], \"confidence\": {\"overallConfidence\": 1.0, \"rationale\": \"clear\"}}";
        String variantResponse1 = "{\"id\": \"v-probable\", \"strategy_type\": \"PROBABLE_SURVIVOR\", \"strategy\": \"Direct minimal implementation of: create java class Example\", \"survival_argument\": \"Atomic creation\", \"semantic_justification\": \"Probable path\", \"tradeoffs\": \"Minimal abstraction\", \"failure_risks\": \"None\", \"actions\": [{\"domain\": \"file\", \"operation\": \"WRITE\", \"target\": \"Example.java\", \"description\": \"Write Example.java\"}]}";

        eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis atomic = new eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis();
        atomic.setAtomic(true);
        atomic.setConfidence(1.0);
        context.getOrchestrationState().getMetadata().put("atomicAnalysis", atomic);

        mockLlm.addResponseMapping("Provide a concise summary of the project structure", "Tycho project.");
        mockLlm.addResponseMapping("Analyze the following user request and expand the intent space", intentExpansion);
        mockLlm.addResponseMapping("FIXED to: PROBABLE_SURVIVOR", variantResponse1);
        mockLlm.addResponseMapping("FIXED to: PHILOSOPHY_MUTATION", "{\"id\": \"v-philo\", \"strategy_type\": \"PHILOSOPHY_MUTATION\", \"strategy\": \"Alternative Strategy\", \"survival_argument\": \"Diversity\", \"semantic_justification\": \"Different phil\", \"tradeoffs\": \"None\", \"failure_risks\": \"None\", \"actions\": []}");
        mockLlm.addResponseMapping("FIXED to: MAXIMAL_DIVERGENCE", "{\"id\": \"v-maxdiv\", \"strategy_type\": \"MAXIMAL_DIVERGENCE\", \"strategy\": \"Divergent Strategy\", \"survival_argument\": \"Diversity\", \"semantic_justification\": \"Max divergence\", \"tradeoffs\": \"None\", \"failure_risks\": \"None\", \"actions\": []}");
        mockLlm.addResponseMapping("FIXED to: STABILIZATION_RECOVERY", "{\"id\": \"v-stab\", \"strategy_type\": \"STABILIZATION_RECOVERY\", \"strategy\": \"Stabilization Strategy\", \"survival_argument\": \"Stability\", \"semantic_justification\": \"Stabilization\", \"tradeoffs\": \"None\", \"failure_risks\": \"None\", \"actions\": []}");
        mockLlm.addResponseMapping("You are a Final Response Agent", "Final summary.");

        mockLlm.setResponseSequence(new String[] {
            javaCode,     // JavaDev (Execution)
            evalResponse, // Validator -> Reviewer (Execution)
            evalResponse, // Validator -> ConstraintAgent (Execution)
            evalResponse, // Evaluator.evaluate (Verification) - Reviewer
            evalResponse, // Evaluator.evaluate (Verification) - Constraint
            "Done"        // FinalResponseAgent
        });

        // Add mapping for Analytic diagnosis to return code instead of JSON to avoid the failure
        mockLlm.addResponseMapping("Role: Analytic", javaCode);

        // Inject mock into all agents globally for this test
        eu.kalafatic.evolution.controller.agents.AgentFactory.getAllAgents().forEach(a -> {
            if (a instanceof eu.kalafatic.evolution.controller.agents.BaseAiAgent) {
                ((eu.kalafatic.evolution.controller.agents.BaseAiAgent)a).setAiService(aiService);
            }
        });
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(context.getSessionId());
        if (session instanceof SessionContext) {
            ((SessionContext)session).getAgentRegistry().values().forEach(a -> {
                if (a instanceof eu.kalafatic.evolution.controller.agents.BaseAiAgent) {
                    ((eu.kalafatic.evolution.controller.agents.BaseAiAgent)a).setAiService(aiService);
                }
            });
        }

        IterationManager manager = createManager(context);
        context.setAiService(aiService);

        // Final response mapping
        mockLlm.addResponseMapping("Role: FinalResponse", "Final result summary.");
        mockLlm.addResponseMapping("Final Response Agent", "Final result summary.");

        TaskRequest request = new TaskRequest("create java class Example", tempDir);
        String result = manager.handle(request).getSummary();

        assertNotNull(result);

        File javaFile = new File(tempDir, "Example.java");
        assertTrue("Java file should exist", javaFile.exists());
        assertEquals(javaCode, Files.readString(javaFile.toPath()));
    }

    private IterationManager createManager(TaskContext context) {
        GitManager gitManager = new GitManager(tempDir);
        TaskPlanner taskPlanner = new TaskPlanner();
        TaskExecutor taskExecutor = new TaskExecutor(context);
        Evaluator evaluator = new Evaluator(tempDir, context);
        IterationMemoryService memoryService = new IterationMemoryService(tempDir);
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(tempDir, context);
        DarwinEngine darwinEngine = new DarwinEngine(context, memoryService, stateProvider);

        return new IterationManager(context,
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
        private final java.util.Map<String, String> responseMappings = new java.util.concurrent.ConcurrentHashMap<>();
        private String defaultResponse = "{\"success\": true, \"comment\": \"Mock success\", \"intent\": \"continue\", \"category\": \"CODING\", \"isAmbiguous\": false}";

        public void setResponseSequence(String[] sequence) {
            this.responseSequence = sequence;
            this.callCount.set(0);
        }

        public void addResponseMapping(String keyword, String response) {
            responseMappings.put(keyword, response);
        }

        @Override
        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
            for (java.util.Map.Entry<String, String> entry : responseMappings.entrySet()) {
                if (prompt.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }

            int current = callCount.getAndIncrement();
            return (responseSequence != null && current < responseSequence.length) ? responseSequence[current] : defaultResponse;
        }

        @Override
        public String testConnection(Orchestrator orchestrator, float temperature, String proxyUrl, TaskContext context) throws Exception {
            return "{\"status\": \"ok\"}";
        }
    }
}
