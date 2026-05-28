package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.controller.orchestration.selfdev.*;
import eu.kalafatic.evolution.model.orchestration.*;
import eu.kalafatic.evolution.controller.tools.ShellTool;

public class EvolutionaryLineageTest {

    private File tempDir;
    private Orchestrator orchestrator;
    private MockProvider mockLlm;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("lineage-test").toFile();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());
        orchestrator.setAiMode(AiMode.LOCAL);
        orchestrator.setDarwinMode(true);

        mockLlm = new MockProvider();

        // Init git repo
        TaskContext initContext = new TaskContext(orchestrator, tempDir);
        ShellTool shell = new ShellTool();
        shell.execute("git init", tempDir, initContext);
        shell.execute("git config user.email \"test@example.com\"", tempDir, initContext);
        shell.execute("git config user.name \"Test User\"", tempDir, initContext);
        Files.writeString(new File(tempDir, "pom.xml").toPath(), "<project><modelVersion>4.0.0</modelVersion><groupId>test</groupId><artifactId>test</artifactId><version>1.0</version></project>");
        shell.execute("git add .", tempDir, initContext);
        shell.execute("git commit -m \"Initial commit\"", tempDir, initContext);
    }

    @Test
    public void testLineagePersistenceAcrossGenerations() throws Exception {
        orchestrator.getAiChat().getPromptInstructions().setIterativeMode(true);
        orchestrator.getAiChat().getPromptInstructions().setPreferredMaxIterations(2);

        final String[] capturedPrompt = new String[1];
        AiService capturingService = new AiService() {
            @Override
            public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                if (prompt.contains("SURVIVING TRAJECTORY (ANCESTOR):")) {
                    capturedPrompt[0] = prompt;
                }
                return mockLlm.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
        };

        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);
        context.setAiService(capturingService);
        context.getMetadata().put("testMode", true);

        // Common Mappings
        mockLlm.addMapping("Provide a concise summary of the project structure", "Structure: Java Project");
        mockLlm.addMapping("Determine whether this request is a SINGLE deterministic artifact", "{\"atomic\": false, \"confidence\": 0.1}");
        mockLlm.addMapping("Deconstruct and expand user intent", "{\"state\": \"CLEAR\", \"dominantIntent\": \"Refactor\", \"evolutionary_axes\": [], \"hypotheses\": [], \"confidence\": {\"overallConfidence\": 1.0}}");
        mockLlm.addMapping("Generate the full source code for", "Code Content");
        mockLlm.addMapping("adaptive analysis", "{\"avoidGuidelines\": []}");

        String v0 = "{" +
            "\"id\": \"direct_minimal\", " +
            "\"strategy_type\": \"PHILOSOPHY_MUTATION\", " +
            "\"strategy\": \"Initial Evolutionary Strategy\", " +
            "\"score\": 0.99, " +
            "\"actions\": [{\"domain\":\"file\", \"operation\":\"WRITE\", \"target\":\"t_initial.txt\", \"description\":\"initial\"}], " +
            "\"survival_argument\": \"Technical superiority of the initial trajectory is verified by this very long argument.\", " +
            "\"tradeoffs\": \"Tradeoffs are minimal and well documented in this specific engineering path.\", " +
            "\"failure_risks\": \"Risks are mitigated by strict adherence to evolutionary principles.\", " +
            "\"pros_cons\": \"Pros: clear; Cons: none observed in this simulation.\", " +
            "\"semantic_justification\": \"Minimalist\", " +
            "\"engineering_dimensions\": {" +
            "  \"philosophy\": \"Minimalist\", \"execution_model\": \"atomic\", \"abstraction_depth\": \"low\", \"modularity_approach\": \"monolithic\"," +
            "  \"testing_strategy\": \"unit\", \"extensibility\": \"low\", \"dependency_assumptions\": \"none\", \"runtime_behavior\": \"deterministic\", \"risk_acceptance\": \"conservative\"" +
            "}" +
            "}";

        mockLlm.addMapping("ID: direct_minimal", v0);
        // Map other blueprints to avoid stalls
        mockLlm.addMapping("ID: persistent_storage", v0.replace("direct_minimal", "persistent_storage").replace("Minimalist", "Persistent State").replace("0.99", "0.5"));
        mockLlm.addMapping("ID: stabilized_resilient", v0.replace("direct_minimal", "stabilized_resilient").replace("Minimalist", "Resilient").replace("0.99", "0.5"));
        mockLlm.addMapping("ID: reusable_service", v0.replace("direct_minimal", "reusable_service").replace("Minimalist", "Service-Oriented").replace("0.99", "0.5"));

        IterationManager manager = createManager(context, capturingService);
        manager.handle(new TaskRequest("refactor complex goal", tempDir));

        assertNotNull("Prompt for Gen 1 should have been captured", capturedPrompt[0]);
        assertTrue("Prompt should contain survivor info", capturedPrompt[0].contains("Initial Evolutionary Strategy"));
        assertTrue("Prompt should contain semantic justification (Minimalist)", capturedPrompt[0].contains("Minimalist"));
    }

    private IterationManager createManager(TaskContext context, AiService aiService) {
        IterationMemoryService memoryService = context.getKernelContext().getMemoryService();
        return new IterationManager(
            context, aiService, new GitManager(tempDir),
            new TaskPlanner(), new TaskExecutor(context), new Evaluator(tempDir, context),
            new DarwinEngine(context, memoryService, new SystemStateSignalProvider(tempDir, context)),
            memoryService
        );
    }

    private static class MockProvider implements ILlmProvider {
        private final Map<String, String> mappings = new HashMap<>();

        public void addMapping(String keyword, String response) {
            mappings.put(keyword, response);
        }

        @Override
        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) {
            for (Map.Entry<String, String> entry : mappings.entrySet()) {
                if (prompt.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return "{}";
        }

        @Override
        public String testConnection(Orchestrator orchestrator, float temperature, String proxyUrl, TaskContext context) throws Exception {
            return "{\"status\": \"ok\"}";
        }
    }
}
