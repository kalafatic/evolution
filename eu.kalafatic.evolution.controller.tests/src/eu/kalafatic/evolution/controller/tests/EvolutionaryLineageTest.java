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
    private AiService aiService;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("lineage-test").toFile();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());
        orchestrator.setAiMode(AiMode.LOCAL);
        orchestrator.setDarwinMode(true);

        mockLlm = new MockProvider();
        aiService = new AiService() {
            @Override
            public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                return mockLlm.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
        };

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
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setAutoApprove(true);
        context.setAiService(aiService);
        context.getMetadata().put("testMode", true);

        // Common Mappings
        mockLlm.addMapping("Provide a concise summary of the project structure", "Structure: Java Project");
        mockLlm.addMapping("Determine whether this request is a SINGLE deterministic artifact", "{\"atomic\": false, \"confidence\": 0.1}");
        mockLlm.addMapping("Deconstruct and expand user intent", "{\"state\": \"CLEAR\", \"dominantIntent\": \"Refactor\", \"dimensions\": [], \"hypotheses\": [], \"confidence\": {\"overallConfidence\": 1.0}}");
        mockLlm.addMapping("Generate the full source code for", "Code Content");

        String v0 = "{" +
            "\"id\": \"v-initial\", " +
            "\"strategy_type\": \"PROBABLE_SURVIVOR\", " +
            "\"strategy\": \"Initial Evolutionary Strategy\", " +
            "\"score\": 0.99, " +
            "\"actions\": [{\"domain\":\"file\", \"operation\":\"WRITE\", \"target\":\"t_initial.txt\", \"description\":\"initial\"}], " +
            "\"survival_argument\": \"Technical superiority of the initial trajectory is verified by this very long argument.\", " +
            "\"tradeoffs\": \"Tradeoffs are minimal and well documented in this specific engineering path.\", " +
            "\"failure_risks\": \"Risks are mitigated by strict adherence to evolutionary principles.\", " +
            "\"pros_cons\": \"Pros: clear; Cons: none observed in this simulation.\", " +
            "\"semantic_justification\": \"Deep architectural philosophy grounded in initial surviving lineage logic.\"" +
            "}";

        String vAlt = "{" +
            "\"id\": \"v-alt\", " +
            "\"strategy_type\": \"PHILOSOPHY_MUTATION\", " +
            "\"strategy\": \"Alternative Mutation Strategy\", " +
            "\"score\": 0.8, " +
            "\"actions\": [{\"domain\":\"file\", \"operation\":\"WRITE\", \"target\":\"t_alt.txt\", \"description\":\"alt\"}], " +
            "\"survival_argument\": \"Alternative survival argument that differs significantly from the initial one.\", " +
            "\"tradeoffs\": \"Different set of technical compromises focusing on extensibility over speed.\", " +
            "\"failure_risks\": \"Higher complexity might lead to integration challenges if not managed.\", " +
            "\"pros_cons\": \"Pros: flexible; Cons: slower execution path.\", " +
            "\"semantic_justification\": \"Philosophy focused on long-term interface stability and abstraction.\"" +
            "}";

        String vDiv = "{" +
            "\"id\": \"v-div\", " +
            "\"strategy_type\": \"MAXIMAL_DIVERGENCE\", " +
            "\"strategy\": \"Maximal Divergence Strategy\", " +
            "\"score\": 0.7, " +
            "\"actions\": [{\"domain\":\"file\", \"operation\":\"WRITE\", \"target\":\"t_div.txt\", \"description\":\"div\"}], " +
            "\"survival_argument\": \"Exploring a radically different engineering future with zero shared state.\", " +
            "\"tradeoffs\": \"Sacrifices familiarity for potential orders of magnitude improvement.\", " +
            "\"failure_risks\": \"High risk of concept rejection by existing framework constraints.\", " +
            "\"pros_cons\": \"Pros: extreme speed; Cons: difficult to maintain by traditional means.\", " +
            "\"semantic_justification\": \"Philosophy of radical decentralization and event-driven evolution.\"" +
            "}";

        String vStab = "{" +
            "\"id\": \"v-stab\", " +
            "\"strategy_type\": \"STABILIZATION_RECOVERY\", " +
            "\"strategy\": \"Stabilization Recovery Strategy\", " +
            "\"score\": 0.6, " +
            "\"actions\": [{\"domain\":\"file\", \"operation\":\"WRITE\", \"target\":\"t_stab.txt\", \"description\":\"stab\"}], " +
            "\"survival_argument\": \"Ensures system integrity before applying further implements.\", " +
            "\"tradeoffs\": \"Slowest path but highest guarantee of stability and safety.\", " +
            "\"failure_risks\": \"Might over-analyze and miss implementation windows.\", " +
            "\"pros_cons\": \"Pros: safe; Cons: conservative speed.\", " +
            "\"semantic_justification\": \"Philosophy of defensive engineering and architectural mapping.\"" +
            "}";

        mockLlm.addMapping("strategy_type is FIXED to: PROBABLE_SURVIVOR", v0);
        mockLlm.addMapping("strategy_type is FIXED to: PHILOSOPHY_MUTATION", vAlt);
        mockLlm.addMapping("strategy_type is FIXED to: MAXIMAL_DIVERGENCE", vDiv);
        mockLlm.addMapping("strategy_type is FIXED to: STABILIZATION_RECOVERY", vStab);

        IterationManager manager = createManager(context);
        manager.handle(new TaskRequest("refactor complex goal", tempDir));

        IterationMemoryService memory = context.getKernelContext().getMemoryService();
        List<IterationRecord> records = memory.getRecords();
        assertFalse("Records should not be empty", records.isEmpty());

        // Find the record with ID v-initial (or containing v-initial in strategy)
        IterationRecord survivor = records.stream()
                .filter(r -> "ACTIVE".equals(r.getActivationState()))
                .findFirst().orElse(null);

        assertNotNull("Should have an active survivor", survivor);
        assertEquals("Initial Evolutionary Strategy", survivor.getStrategy());

        // --- GENERATION 1 ---
        final String[] capturedPrompt = new String[1];
        AiService capturingService = new AiService() {
            @Override
            public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                if (prompt.contains("SURVIVING TRAJECTORY:")) {
                    capturedPrompt[0] = prompt;
                }
                return mockLlm.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
        };

        // Re-injecting capturing service and creating a new manager to ensure fresh discovery
        context.setAiService(capturingService);
        manager = createManager(context);

        String v1 = v0.replace("v-initial", "v1").replace("Initial Evolutionary Strategy", "Mutated Strategy").replace("Initial Philosophy", "Mutated Philosophy");
        mockLlm.addMapping("strategy_type is FIXED to: PROBABLE_SURVIVOR", v1);

        // Use "refactor" again to skip simple chat routing
        context.getOrchestrationState().setCurrentPhase("ARCHITECTURE_VARIANTS");
        manager.handle(new TaskRequest("refactor complex goal", tempDir));

        assertNotNull("Prompt for Gen 1 should have been captured", capturedPrompt[0]);
        assertTrue("Prompt should contain survivor info", capturedPrompt[0].contains("Initial Evolutionary Strategy"));
        assertTrue("Prompt should contain semantic justification", capturedPrompt[0].contains("Deep architectural philosophy"));
    }

    private IterationManager createManager(TaskContext context) {
        IterationMemoryService memoryService = context.getKernelContext().getMemoryService();
        return new IterationManager(
            context, context.getAiService(), new GitManager(tempDir),
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
    }
}
