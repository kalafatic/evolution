package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

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

public class MediatedModeIntegrationTest {

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
        session.setId("mediated-test-session");
        session.setStatus(SelfDevStatus.RUNNING);
        orchestrator.setSelfDevSession(session);

        File root = tempFolder.newFolder();
        ShellTool shell = new ShellTool();
        TaskContext initContext = new TaskContext(orchestrator, root);
        shell.execute("git init", root, initContext);
        shell.execute("git config user.email \"test@example.com\"", root, initContext);
        shell.execute("git config user.name \"Test User\"", root, initContext);
        Files.writeString(new File(root, "pom.xml").toPath(), "<project><modelVersion>4.0.0</modelVersion><groupId>test</groupId><artifactId>test</artifactId><version>1.0</version></project>");
        Files.writeString(new File(root, "sloeber.ino").toPath(), "void setup() {} void loop() {}");
        shell.execute("git add .", root, initContext);
        shell.execute("git commit -m \"Initial commit\"", root, initContext);

        context = new TaskContext(orchestrator, root);
        context.setSessionId("mediated-test-session");
        eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getOrCreateSession("mediated-test-session");
        context.getBehaviorProfile().addTrait(BehaviorTrait.SUPERVISION_MEDIATED);
        context.getBehaviorProfile().addTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY);
        context.getBehaviorProfile().addTrait(BehaviorTrait.REASONING_DARWIN_ITERATIVE);
        context.setAutoApprove(true);
        context.getMetadata().put("testMode", true);
        context.getOrchestrationState().getMetadata().put("forceSolution", true);

        // Auto-reply to variant selection and other input requests
        context.addInputListener(message -> {
            new Thread(() -> {
                try {
                    // Robust wait for Darwin to be ready for selection
                    Thread.sleep(500);
                    if (message.contains("Darwin evolved")) {
                        // Extract first ID from list like "- [id] Strategy"
                        int start = message.indexOf("- [");
                        if (start >= 0) {
                            start += 3;
                            int end = message.indexOf("]", start);
                            if (end > start) {
                                String id = message.substring(start, end);
                                context.log("[TEST] Auto-selecting variant: " + id);
                                context.provideInput("Select " + id);
                                return;
                            }
                        }
                    }
                    context.log("[TEST] Auto-approving message: " + (message.length() > 50 ? message.substring(0, 50) + "..." : message));
                    context.provideInput("Approved");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        });

        // Mock LLM responses
        mockLlm.addResponseMapping("IntentExpansionEngine", "{\"state\": \"CLEAR\", \"dominantIntent\": \"Analysis\", \"confidence\": {\"overallConfidence\": 1.0}}");

        String darwinVariant = "{" +
            "  \"strategy_type\": \"ARCHITECTURE_MAPPING\"," +
            "  \"strategy\": \"Test mediated strategy\"," +
            "  \"survival_argument\": \"Test survival\"," +
            "  \"semantic_justification\": \"Test justification\"," +
            "  \"tradeoffs\": \"Test tradeoffs\"," +
            "  \"failure_risks\": \"Test risks\"," +
            "  \"actions\": [{ \"domain\": \"file\", \"operation\": \"WRITE\", \"target\": \"sloeber.ino\", \"description\": \"Test action\", \"implementation\": \"void setup() {} void loop() { /* updated */ }\" }]," +
            "  \"score\": 0.95," +
            "  \"mediation_candidate\": {" +
            "    \"prompt\": \"Optimized prompt for big LLM\"," +
            "    \"selected_files\": [\"pom.xml\", \"sloeber.ino\"]," +
            "    \"architecture_summary\": \"Test arch\"," +
            "    \"subsystems\": [{\"id\":\"s1\",\"name\":\"TestSubsystem\",\"purpose\":\"Test purpose\",\"description\":\"Test desc\",\"boundaries\":[],\"critical_files\":[],\"responsibilities\":[] }]," +
            "    \"architectural_facts\": [{\"id\":\"f1\",\"subject\":\"TestSubsystem\",\"predicate\":\"exists\",\"description\":\"Verified existence\",\"confidence\":1.0,\"evidence\":[] }]," +
            "    \"dependencies\": \"Test deps\"," +
            "    \"execution_instructions\": \"Test instructions\"" +
            "  }" +
            "}";

        // Support sequential sibling generation by providing divergent strategies
        // 1. Discovery for Sibling #2 (More specific, matched when at least one sibling exists)
        mockLlm.addResponseMapping("EXPLORED TERRITORY", "{\"strategy\": \"Strategy 2\", \"philosophy\": \"Philosophy 2\", \"strategy_type\": \"MAXIMAL_DIVERGENCE\"}");
        // 2. Discovery for Sibling #1
        mockLlm.addResponseMapping("ONE additional sibling", "{\"strategy\": \"Strategy 1\", \"philosophy\": \"Philosophy 1\", \"strategy_type\": \"PROBABLE_SURVIVOR\"}");
        // 3. Materialization for Sibling #1 (Matched by specific strategy from discovery)
        mockLlm.addResponseMapping("Strategy 1", "<BEGIN_DARWIN_JSON>" + darwinVariant.replace("Test mediated strategy", "Strategy 1") + "<END_DARWIN_JSON>");
        // 4. Materialization for Sibling #2
        mockLlm.addResponseMapping("Strategy 2", "<BEGIN_DARWIN_JSON>" + darwinVariant.replace("Test mediated strategy", "Strategy 2") + "<END_DARWIN_JSON>");

        // 5. Task Planner Mock (to avoid Ollama not configured if actions are empty or during task generation)
        mockLlm.addResponseMapping("Task Planner", "[{\"id\": \"t1\", \"name\": \"Improvement Task\", \"taskType\": \"llm\", \"priority\": 1, \"rationale\": \"Mock rationale\"}]");
        mockLlm.addResponseMapping("autonomous improvement tasks", "[{\"id\": \"t1\", \"name\": \"Improvement Task\", \"taskType\": \"llm\", \"priority\": 1, \"rationale\": \"Mock rationale\"}]");
    }

    @Test
    public void testMediatedDarwinToZipExport() throws Exception {
        eu.kalafatic.evolution.controller.orchestration.SessionContainer session = eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getOrCreateSession(context.getSessionId());
        IterationManager manager = KernelFactory.create(context, session, aiService);

        TaskRequest request = new TaskRequest();
        request.setPrompt("Analyze my project");

        OrchestratorResponse response = manager.handle(request);

        assertNotNull("Response should not be null", response);
        assertTrue("Summary should contain export package info", response.getSummary().contains("**Unified Export Package (ZIP):**"));
        assertTrue("Summary should mention mediated Darwin", response.getSummary().contains("Mediated Darwin Evolution Complete"));

        // Verify ZIP file creation
        File[] files = context.getProjectRoot().listFiles((dir, name) -> name.startsWith("mediated_export_") && name.endsWith(".zip"));
        assertNotNull("ZIP file list should not be null", files);
        assertTrue("Export ZIP file should exist", files.length > 0);

        // Verify ZIP content
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(files[0])) {
            // Unified Export structure puts files under implementation/files/ or similar if using FULL profile
            // But createExportPackage puts them under affected-files/
            // createUnifiedExport puts them under implementation/files/
            assertNotNull("prompt.md should be in ZIP", zip.getEntry("prompt.md"));

            // Check for implementation/files/ prefix (Unified Export) or affected-files/

            boolean foundPom = zip.getEntry("implementation/files/pom.xml") != null;
            boolean foundIno = zip.getEntry("implementation/files/sloeber.ino") != null;
        }
    }

    private static class MockLlmProvider {
        private final java.util.List<Mapping> mappings = new java.util.ArrayList<>();

        private static class Mapping {
            String keyword;
            String response;
            Mapping(String k, String r) { this.keyword = k; this.response = r; }
        }

        public void addResponseMapping(String keyword, String response) {
            mappings.add(0, new Mapping(keyword, response));
        }

        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
            for (Mapping m : mappings) {
                if (prompt.contains(m.keyword)) {
                    context.log("[MOCK_LLM] Matched keyword: '" + m.keyword + "'");
                    return m.response;
                }
            }
            context.log("[MOCK_LLM] No match found for prompt. Returning empty JSON.");
            return "{}";
        }
    }
}
