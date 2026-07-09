package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

public class ChatShortCircuitFixTest {

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
        session.setId("chat-fix-test-session");
        session.setStatus(SelfDevStatus.RUNNING);
        orchestrator.setSelfDevSession(session);

        File root = tempFolder.newFolder();
        ShellTool shell = new ShellTool();
        TaskContext initContext = new TaskContext(orchestrator, root);
        shell.execute("git init", root, initContext);
        shell.execute("git config user.email \"test@example.com\"", root, initContext);
        shell.execute("git config user.name \"Test User\"", root, initContext);
        Files.writeString(new File(root, "pom.xml").toPath(), "<project><modelVersion>4.0.0</modelVersion><groupId>test</groupId><artifactId>test</artifactId><version>1.0</version></project>");
        shell.execute("git add .", root, initContext);
        shell.execute("git commit -m \"Initial commit\"", root, initContext);

        context = new TaskContext(orchestrator, root);
        context.setSessionId("chat-fix-test-session");
        eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getOrCreateSession("chat-fix-test-session");
        context.getBehaviorProfile().addTrait(BehaviorTrait.SUPERVISION_MEDIATED);
        context.getBehaviorProfile().addTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY);
        context.getBehaviorProfile().addTrait(BehaviorTrait.REASONING_DARWIN_ITERATIVE);
        context.setAutoApprove(true);
        context.getMetadata().put("testMode", true);

        // Standard mock responses
        mockLlm.addResponseMapping("IntentExpansionEngine", "{\"state\": \"CLEAR\", \"dominantIntent\": \"Analysis\", \"confidence\": {\"overallConfidence\": 1.0}}");
        mockLlm.addResponseMapping("Analyze this user request and extract the true intent", "{\"primaryGoal\": \"Analyze project\", \"complexity\": \"SIMPLE\", \"goalType\": \"ANALYSIS\", \"domain\": \"JAVA\"}");
        mockLlm.addResponseMapping("design the optimal prompt format", "{\"format\": \"STEP_BY_STEP\", \"siblingCount\": 1}");
        mockLlm.addResponseMapping("produce an analysis package", "PROMPT: Test\nARCHITECTURE: Test\nFILES: pom.xml\nINSTRUCTIONS: Test");
        
        // Use a strategy that matches test-mode success criteria ("Add Validation")
        String darwinVariant = "{\"strategy\": \"Add Validation\", \"score\": 0.99, \"actions\": [], \"mediation_candidate\": {\"prompt\": \"Test\", \"selected_files\": [\"pom.xml\"], \"architecture_summary\": \"Test\"}}";
        mockLlm.addResponseMapping("BLUEPRINT TO MATERIALIZE", "<BEGIN_DARWIN_JSON>" + darwinVariant + "<END_DARWIN_JSON>");
        
        // Mock the final synthesis to reach a terminal phase
        mockLlm.addResponseMapping("Synthesize the results", "Evolution completed successfully with terminal status.");
    }

    @Test
    public void testAnalyzeInMediatedModeDoesNotShortCircuitToChat() throws Exception {
        // GIVEN: A prompt "analyze" which the LLM classifies as CHAT
        mockLlm.addResponseMapping("Analyze this user prompt", "<BEGIN_JSON>{\"category\": \"CHAT\", \"confidence\": 0.95, \"reasoning\": \"Ambiguous word\"}<END_JSON>");
        
        IterationManager manager = KernelFactory.create("analyze", context, 
            eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getSession(context.getSessionId()), aiService);

        TaskRequest request = new TaskRequest();
        request.setPrompt("analyze");

        // WHEN: Orchestration is triggered
        OrchestratorResponse response = manager.handle(request);

        // THEN: It should proceed to evolution, not just return a simple chat response
        assertNotNull(response);
        // If it short-circuited to chat, the summary would be from generateChatResponse ("Hello! How can I help?")
        assertFalse("Should NOT have short-circuited to simple chat. Summary: " + response.getSummary(), 
            response.getSummary().contains("Hello! How can I help?"));
        assertTrue("Summary should indicate evolution happened. Summary: " + response.getSummary(),
            response.getSummary().contains("Evolution completed"));
        assertFalse("Metadata should not contain isChatRequest", context.getOrchestrationState().getMetadata().containsKey("isChatRequest"));
    }

    @Test
    public void testHiInMediatedModeSTILLShortCircuitsToChat() throws Exception {
        // GIVEN: A prompt "hi" which the LLM classifies as CHAT
        mockLlm.addResponseMapping("Analyze this user prompt", "<BEGIN_JSON>{\"category\": \"CHAT\", \"confidence\": 1.0, \"reasoning\": \"Greeting\"}<END_JSON>");
        mockLlm.addResponseMapping("You are a friendly, helpful AI assistant", "Hello! How can I help?");

        IterationManager manager = KernelFactory.create("hi", context, 
            eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getSession(context.getSessionId()), aiService);

        TaskRequest request = new TaskRequest();
        request.setPrompt("hi");

        // WHEN: Orchestration is triggered
        OrchestratorResponse response = manager.handle(request);

        // THEN: It should short-circuit to chat
        assertNotNull(response);
        assertTrue("Should have short-circuited to chat for simple greeting. Summary: " + response.getSummary(), 
            response.getSummary().contains("Hello! How can I help?"));
        assertTrue("Metadata should contain isChatRequest", (Boolean)context.getOrchestrationState().getMetadata().get("isChatRequest"));
    }

    private static class MockLlmProvider {
        private final Map<String, String> responseMappings = new ConcurrentHashMap<>();

        public void addResponseMapping(String keyword, String response) {
            responseMappings.put(keyword, response);
        }

        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
            for (Map.Entry<String, String> entry : responseMappings.entrySet()) {
                if (prompt.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return "{}";
        }
    }
}
