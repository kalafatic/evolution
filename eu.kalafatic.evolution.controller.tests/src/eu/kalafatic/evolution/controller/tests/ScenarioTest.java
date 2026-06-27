package eu.kalafatic.evolution.controller.tests;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.controller.orchestration.selfdev.*;
import eu.kalafatic.evolution.model.orchestration.*;
import eu.kalafatic.evolution.controller.tools.ShellTool;

public class ScenarioTest {

    private File tempDir;
    private Orchestrator orchestrator;
    private MockProvider mockLlm;
    private AiService aiService;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("scenario-test").toFile();
        ShellTool shell = new ShellTool();
        TaskContext initContext = new TaskContext(OrchestrationFactory.eINSTANCE.createOrchestrator(), tempDir);
        shell.execute("git init", tempDir, initContext);
        Files.writeString(new File(tempDir, "pom.xml").toPath(), "<project></project>");
        shell.execute("git add .", tempDir, initContext);
        shell.execute("git commit -m \"Initial commit\"", tempDir, initContext);

        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId("scenario-orch");
        orchestrator.setAiMode(AiMode.LOCAL);

        Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
        ollama.setModel("default");
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
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);
        context.getMetadata().put("testMode", true);

        String javaCode = "public class Example {}";
        String evalResponse = "{\"success\": true, \"comment\": \"Looks good\"}";
        String variantResponse1 = "{\"id\": \"v-probable\", \"strategy_type\": \"PROBABLE_SURVIVOR\", \"strategy\": \"Direct minimal implementation\", \"actions\": [{\"domain\": \"file\", \"operation\": \"WRITE\", \"target\": \"Example.java\", \"description\": \"Write Example.java\"}]}";

        mockLlm.addResponseMapping("Provide a concise summary", "Tycho project.");
        mockLlm.addResponseMapping("PROBABLE_SURVIVOR", variantResponse1);
        mockLlm.addResponseMapping("Role: File", javaCode);
        mockLlm.addResponseMapping("Role: JavaDev", javaCode);

        mockLlm.setResponseSequence(new String[] {
            javaCode,
            evalResponse,
            "Final summary."
        });

        SessionContainer session = SessionManager.getInstance().getOrCreateSession(context.getSessionId());
        IterationManager manager = createManager(context, session);
        context.setAiService(aiService);

        TaskRequest request = new TaskRequest("create java class Example", tempDir);
        var response = manager.handle(request);
        assertNotNull(response);
    }

    private IterationManager createManager(TaskContext context, SessionContainer session) {
        GitManager gitManager = new GitManager(tempDir);
        TaskPlanner taskPlanner = new TaskPlanner(session);
        TaskExecutor taskExecutor = new TaskExecutor(context, orchestrator);
        if (taskExecutor.getOrchestrator() != null) taskExecutor.getOrchestrator().setAiService(aiService);
        Evaluator evaluator = new Evaluator(tempDir, context);
        IterationMemoryService memoryService = context.getKernelContext().getMemoryService();
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(tempDir, context);
        DarwinEngine darwinEngine = new DarwinEngine(context, memoryService, stateProvider);

        return new IterationManager(context, session, aiService, gitManager, taskPlanner, taskExecutor, evaluator, darwinEngine, memoryService);
    }

    private static class MockProvider implements ILlmProvider {
        private String[] responseSequence;
        private final AtomicInteger callCount = new AtomicInteger(0);
        private final java.util.Map<String, String> responseMappings = new java.util.concurrent.ConcurrentHashMap<>();

        public void setResponseSequence(String[] sequence) { this.responseSequence = sequence; this.callCount.set(0); }
        public void addResponseMapping(String keyword, String response) { responseMappings.put(keyword, response); }

        @Override
        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
            for (java.util.Map.Entry<String, String> entry : responseMappings.entrySet()) {
                if (prompt.contains(entry.getKey())) return entry.getValue();
            }
            int current = callCount.getAndIncrement();
            return (responseSequence != null && current < responseSequence.length) ? responseSequence[current] : "{\"success\": true}";
        }
        @Override
        public String testConnection(Orchestrator orchestrator, float temperature, String proxyUrl, TaskContext context) throws Exception { return "ok"; }
    }
}
