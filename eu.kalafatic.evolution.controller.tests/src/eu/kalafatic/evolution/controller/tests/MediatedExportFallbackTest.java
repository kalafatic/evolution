package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.KernelFactory;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.tools.ShellTool;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.SelfDevStatus;
import eu.kalafatic.evolution.model.orchestration.AiMode;

public class MediatedExportFallbackTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private AiService aiService;
    private Orchestrator orchestrator;
    private TaskContext context;
    private MockLlmProvider mockLlm;

    @Before
    public void setup() throws Exception {
        mockLlm = new MockLlmProvider();
        aiService = new AiService() {
            @Override
            public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                return mockLlm.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
        };
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setAiMode(AiMode.MEDIATED);
        orchestrator.setLocalModel("test-model");

        SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
        session.setId("fallback-test-session");
        session.setStatus(SelfDevStatus.RUNNING);
        orchestrator.setSelfDevSession(session);

        File root = tempFolder.newFolder();
        ShellTool shell = new ShellTool();
        TaskContext initContext = new TaskContext(orchestrator, root);
        shell.execute("git init", root, initContext);
        shell.execute("git config user.email \"test@example.com\"", root, initContext);
        shell.execute("git config user.name \"Test User\"", root, initContext);
        Files.writeString(new File(root, "pom.xml").toPath(), "<project><modelVersion>4.0.0</modelVersion></project>");
        shell.execute("git add .", root, initContext);
        shell.execute("git commit -m \"Initial commit\"", root, initContext);

        context = new TaskContext(orchestrator, root);
        context.setSessionId("fallback-test-session");
        eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getOrCreateSession("fallback-test-session");

        // REASONING_ATOMIC bypasses Discovery phase where targetRealityModel is created
        context.getBehaviorProfile().addTrait(BehaviorTrait.REASONING_ATOMIC);
        context.getBehaviorProfile().addTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY);
        context.setAutoApprove(true);
        context.getMetadata().put("testMode", true);
        context.getOrchestrationState().getMetadata().put("forceSolution", true);

        // Mock LLM responses
        mockLlm.addResponseMapping("IntentExpansionEngine", "{\"state\": \"CLEAR\", \"dominantIntent\": \"Analysis\", \"confidence\": {\"overallConfidence\": 1.0}}");
        mockLlm.addResponseMapping("BLUEPRINT TO MATERIALIZE", "<BEGIN_DARWIN_JSON>{\"strategy\":\"Fallback Test\", \"actions\":[], \"score\":0.9}<END_DARWIN_JSON>");
    }

    @Test
    public void testExportFallbackWhenRealityModelIsNull() throws Exception {
        eu.kalafatic.evolution.controller.orchestration.SessionContainer session = eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getOrCreateSession(context.getSessionId());
        IterationManager manager = KernelFactory.create(context, session, aiService);

        // Pre-condition: Verify targetRealityModel is NOT in metadata
        assertNull("Target Reality Model should be null initially", context.getOrchestrationState().getMetadata().get("targetRealityModel"));

        TaskRequest request = new TaskRequest();
        request.setPrompt("Analyze my project");

        // This should not throw NullPointerException
        OrchestratorResponse response = manager.handle(request);

        assertNotNull("Response should not be null", response);
        assertTrue("Summary should contain success message", response.getSummary().contains("Mediated Darwin Evolution Complete"));

        // Verify ZIP file was created
        File[] files = context.getProjectRoot().listFiles((dir, name) -> name.startsWith("mediated_export_") && name.endsWith(".zip"));
        assertTrue("Export ZIP file should exist despite initial null reality model", files != null && files.length > 0);
    }

    private static class MockLlmProvider {
        private final java.util.Map<String, String> responseMappings = new java.util.concurrent.ConcurrentHashMap<>();

        public void addResponseMapping(String keyword, String response) {
            responseMappings.put(keyword, response);
        }

        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
            for (java.util.Map.Entry<String, String> entry : responseMappings.entrySet()) {
                if (prompt.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return "{}";
        }
    }
}
