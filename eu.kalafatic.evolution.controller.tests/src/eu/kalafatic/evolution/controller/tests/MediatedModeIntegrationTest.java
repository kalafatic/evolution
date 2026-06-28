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
                    Thread.sleep(200);
                    if (message.contains("Darwin evolved")) {
                        // Extract first ID from list like "- [id] Strategy"
                        int start = message.indexOf("- [") + 3;
                        int end = message.indexOf("]", start);
                        if (start > 2 && end > start) {
                            String id = message.substring(start, end);
                            context.provideInput("Select " + id);
                        } else {
                            context.provideInput("Approved");
                        }
                    } else {
                        context.provideInput("Approved");
                    }
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
            "  \"actions\": [{ \"domain\": \"kernel\", \"operation\": \"ANALYZE\", \"target\": \"workspace\", \"description\": \"Test action\" }]," +
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

        mockLlm.addResponseMapping("BLUEPRINT TO MATERIALIZE", "<BEGIN_DARWIN_JSON>" + darwinVariant + "<END_DARWIN_JSON>");
    }

    @Test
    public void testMediatedDarwinToZipExport() throws Exception {
        eu.kalafatic.evolution.controller.orchestration.SessionContainer session = eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getOrCreateSession(context.getSessionId());

        // Ensure execution profile is initialized for the test context
        context.getOrchestrationState().setExecutionProfile(
            eu.kalafatic.evolution.controller.kernel.EvolutionIntensityCalculator.calculate(context, null, null));

        IterationManager manager = KernelFactory.create(context, session, aiService);

        TaskRequest request = new TaskRequest();
        request.setPrompt("Analyze my project");

        OrchestratorResponse response = manager.handle(request);

        assertNotNull("Response should not be null", response);
        assertTrue("Summary should contain export package info", response.getSummary().contains("Export Package (ZIP):"));
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
            boolean foundPom = zip.getEntry("affected-files/pom.xml") != null || zip.getEntry("implementation/files/pom.xml") != null;
            boolean foundIno = zip.getEntry("affected-files/sloeber.ino") != null || zip.getEntry("implementation/files/sloeber.ino") != null;

            assertTrue("pom.xml should be in ZIP", foundPom);
            assertTrue("sloeber.ino should be in ZIP", foundIno);
        }
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
