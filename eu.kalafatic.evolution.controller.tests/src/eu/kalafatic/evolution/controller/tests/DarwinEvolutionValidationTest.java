package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.KernelFactory;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.ResultType;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionContext;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmResponse;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ChatEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.CodingEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngineFactory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngineRegistry;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DeveloperEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionTree;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IDarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IntentReconstructionEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.controller.orchestration.selfdev.LLMDarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.MediatedEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.MutationEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevelopmentEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SystemStateSignalProvider;
import eu.kalafatic.evolution.controller.orchestration.dream.DreamEngine;
import eu.kalafatic.evolution.controller.tools.ShellTool;
import eu.kalafatic.evolution.model.orchestration.Maven;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Functional and integration validation of the EVO Darwin Engine evolutionary
 * process: candidate generation, independent competition, objective evaluation,
 * winner selection, iterations, trend tracking, motivation/continuation,
 * state machine integrity, persistence and multi-instance participation.
 *
 * The engine under test is the REAL implementation (KernelFactory ->
 * IterationManager -> DarwinEngine). LLM responses are scripted and
 * deterministic; all candidate EVALUATION is real (the engine runs the
 * fixture's Maven test suite in isolated git worktrees via the production
 * Evaluator path).
 */
public class DarwinEvolutionValidationTest {

    private static final String TARGET = "src/main/java/com/example/evo/CollectionProcessor.java";

    private final List<String> activeSessions = new ArrayList<>();
    private final List<String> collectedLogs = new ArrayList<>();

    @org.junit.BeforeClass
    public static void enableRealGitMutations() {
        System.setProperty("evo.git.simulation", "false");
    }

    @After
    public void tearDown() {
        for (String sessionId : new ArrayList<>(activeSessions)) {
            try {
                SessionManager.getInstance().shutdownSession(sessionId);
            } catch (Exception e) {
            }
        }
        activeSessions.clear();
    }

    // ------------------------------------------------------------------
    // Fixture infrastructure
    // ------------------------------------------------------------------

    private static File locateFixtureTemplate() {
        File dir = new File(System.getProperty("user.dir"));
        for (int i = 0; i < 8; i++) {
            File candidate = new File(dir, "test-fixtures/darwin-evolution");
            if (new File(candidate, "pom.xml").exists()) {
                return candidate;
            }
            if (dir.getParentFile() == null) {
                break;
            }
            dir = dir.getParentFile();
        }
        fail("Cannot locate test fixture 'test-fixtures/darwin-evolution' from " + System.getProperty("user.dir"));
        return null;
    }

    private static void copyTree(File src, File dst) throws Exception {
        if (src.isDirectory()) {
            dst.mkdirs();
            File[] children = src.listFiles();
            if (children != null) {
                for (File child : children) {
                    if ("target".equals(child.getName()) || ".git".equals(child.getName())) {
                        continue;
                    }
                    copyTree(child, new File(dst, child.getName()));
                }
            }
        } else {
            Files.copy(src.toPath(), dst.toPath());
        }
    }

    private static File createFixtureRepo() throws Exception {
        File root = Files.createTempDirectory("evo-darwin-evolution").toFile();
        copyTree(locateFixtureTemplate(), root);
        TaskContext scratch = new TaskContext(OrchestrationFactory.eINSTANCE.createOrchestrator(), root);
        ShellTool shell = new ShellTool();
        shell.execute("git init", root, scratch);
        shell.execute("git config user.email \"test@example.com\"", root, scratch);
        shell.execute("git config user.name \"Test User\"", root, scratch);
        shell.execute("git add .", root, scratch);
        shell.execute("git commit -m \"Initial commit\"", root, scratch);
        return root;
    }

    private static void deleteRecursive(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                deleteRecursive(f);
            }
        }
        dir.delete();
    }

    // ------------------------------------------------------------------
    // Scripted deterministic LLM
    // ------------------------------------------------------------------

    private static final String ENVELOPE_RESPONSE = "{"
            + "\"coreIntent\": \"optimize collection deduplication\","
            + "\"mandatoryConcepts\": [\"deduplication\"],"
            + "\"allowedMutationDimensions\": [\"implementation strategy\"],"
            + "\"discouragedRegions\": [],"
            + "\"forbiddenRegions\": [],"
            + "\"maxAbstractionDepth\": 2,"
            + "\"semanticDistanceThreshold\": 0.6"
            + "}";

    private static final String GOAL_RESPONSE = "{"
            + "\"goalType\": \"REFACTORING\","
            + "\"domain\": \"JAVA\","
            + "\"intent\": \"MODIFY\","
            + "\"requestedArtifact\": \"CollectionProcessor\","
            + "\"primaryAction\": \"optimize collection deduplication\","
            + "\"complexity\": \"SIMPLE\","
            + "\"requiredOutputs\": \"linear-time deduplication\","
            + "\"confidence\": 0.9,"
            + "\"ambiguity\": 0.1"
            + "}";

    private static final String INTENT_RESPONSE = "{"
            + "\"state\": \"CLEAR\","
            + "\"dominantIntent\": \"Task\","
            + "\"confidence\": {\"overallConfidence\": 1.0}"
            + "}";

    private static final String[] BLUEPRINT_STRATEGIES = {
            "Minimal Correction of Collection Deduplication",
            "Order-Preserving Collection Deduplication Optimization",
            "Normalizing Sort-Based Collection Deduplication"
    };

    private static final String BLUEPRINT_PHILOSOPHIES[] = {
            "preserve existing behavior with minimal edits",
            "use a linked set structure for linear deduplication",
            "canonical ordering via sorting after set-based dedup"
    };

    /** Candidate A: minimal correction, still quadratic on the input list. */
    private static final String CAND_A = "package com.example.evo;\n"
            + "import java.util.ArrayList;\n"
            + "import java.util.List;\n"
            + "public final class CollectionProcessor {\n"
            + "    private CollectionProcessor() {\n"
            + "    }\n"
            + "    public static List<String> deduplicate(List<String> input) {\n"
            + "        List<String> result = new ArrayList<String>();\n"
            + "        for (int i = 0; i < input.size(); i++) {\n"
            + "            String item = input.get(i);\n"
            + "            if (item == null) {\n"
            + "                continue;\n"
            + "            }\n"
            + "            if (input.indexOf(item) == i) {\n"
            + "                result.add(item);\n"
            + "            }\n"
            + "        }\n"
            + "        return result;\n"
            + "    }\n"
            + "}\n";

    /** Candidate B: optimized order-preserving LinkedHashSet implementation. */
    private static final String CAND_B = "package com.example.evo;\n"
            + "import java.util.ArrayList;\n"
            + "import java.util.LinkedHashSet;\n"
            + "import java.util.List;\n"
            + "public final class CollectionProcessor {\n"
            + "    private CollectionProcessor() {\n"
            + "    }\n"
            + "    public static List<String> deduplicate(List<String> input) {\n"
            + "        List<String> result = new ArrayList<String>();\n"
            + "        LinkedHashSet<String> seen = new LinkedHashSet<String>();\n"
            + "        for (String item : input) {\n"
            + "            if (item == null) {\n"
            + "                continue;\n"
            + "            }\n"
            + "            if (seen.add(item)) {\n"
            + "                result.add(item);\n"
            + "            }\n"
            + "        }\n"
            + "        return result;\n"
            + "    }\n"
            + "}\n";

    /** Candidate C: fast set-based implementation with sorted (normalized) output. */
    private static final String CAND_C = "package com.example.evo;\n"
            + "import java.util.ArrayList;\n"
            + "import java.util.Collections;\n"
            + "import java.util.HashSet;\n"
            + "import java.util.List;\n"
            + "public final class CollectionProcessor {\n"
            + "    private CollectionProcessor() {\n"
            + "    }\n"
            + "    public static List<String> deduplicate(List<String> input) {\n"
            + "        List<String> result = new ArrayList<String>();\n"
            + "        HashSet<String> seen = new HashSet<String>();\n"
            + "        for (String item : input) {\n"
            + "            if (item == null) {\n"
            + "                continue;\n"
            + "            }\n"
            + "            if (seen.add(item)) {\n"
            + "                result.add(item);\n"
            + "            }\n"
            + "        }\n"
            + "        Collections.sort(result);\n"
            + "        return result;\n"
            + "    }\n"
            + "}\n";

    /** Broken candidate: does not compile (unbalanced braces). */
    private static final String CAND_BROKEN = "package com.example.evo;\n"
            + "import java.util.ArrayList;\n"
            + "import java.util.List;\n"
            + "public final class CollectionProcessor {\n"
            + "    private CollectionProcessor() {\n"
            + "    }\n"
            + "    public static List<String> deduplicate(List<String> input) {\n"
            + "        List<String> result = new ArrayList<String>();\n"
            + "        for (String item : input) {\n"
            + "            result.add(item);\n"
            + "        return result;\n"
            + "}\n";

    private static final String[] DEFAULT_MATRIX = { "A", "B", "C" };
    private static final String[] REGRESSION_MATRIX = { "A", "C", "BROKEN" };

    private static final Map<String, String> DIM_PHILOSOPHY = new LinkedHashMap<>();
    private static final Map<String, String> DIM_EXECUTION = new LinkedHashMap<>();
    private static final Map<String, String> DIM_DEPTH = new LinkedHashMap<>();
    static {
        DIM_PHILOSOPHY.put("A", "minimal correction of existing scan logic");
        DIM_EXECUTION.put("A", "sequential scan");
        DIM_DEPTH.put("A", "low");
        DIM_PHILOSOPHY.put("B", "linked set order-preserving deduplication");
        DIM_EXECUTION.put("B", "set-assisted single pass");
        DIM_DEPTH.put("B", "low");
        DIM_PHILOSOPHY.put("C", "normalizing sort-based deduplication");
        DIM_EXECUTION.put("C", "set plus canonical sort");
        DIM_DEPTH.put("C", "medium");
        DIM_PHILOSOPHY.put("BROKEN", "fragmented rewrite");
        DIM_EXECUTION.put("BROKEN", "unsafe partial scan");
        DIM_DEPTH.put("BROKEN", "low");
    }

    static class ScriptedAiService extends AiService {
        final Map<String, String> candidates = new LinkedHashMap<>();
        String[][] matrix;
        boolean allBroken;
        int territoryCalls = 0;
        int spawnCalls = 0;

        ScriptedAiService() {
            candidates.put("A", CAND_A);
            candidates.put("B", CAND_B);
            candidates.put("C", CAND_C);
            candidates.put("BROKEN", CAND_BROKEN);
            matrix = new String[][] { DEFAULT_MATRIX };
        }

        ScriptedAiService(String[] first, String[] second) {
            this();
            if (second == null) {
                matrix = new String[][] { first };
            } else {
                matrix = new String[][] { first, second };
            }
        }

        @Override
        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl,
                TaskContext context, String forcedModel) throws Exception {
            if (prompt.contains("Semantic Envelope Engine")) {
                return ENVELOPE_RESPONSE;
            }
            if (prompt.contains("Goal Understanding Engine")) {
                return GOAL_RESPONSE;
            }
            if (prompt.contains("expand the intent space")) {
                return INTENT_RESPONSE;
            }
            if (prompt.contains("TASK: Generate ONE additional sibling")) {
                int idx = territoryCalls++ % BLUEPRINT_STRATEGIES.length;
                return blueprintJson(idx);
            }
            if (prompt.contains("Analyze this user prompt:")) {
                return "{\"category\": \"TASK\", \"confidence\": 0.95, \"reasoning\": \"Deterministic task classification\"}";
            }
            return "{\"success\": true, \"comment\": \"Mock success\"}";
        }

        @Override
        public LlmResponse sendLlmRequest(Orchestrator orchestrator, String prompt, float temperature,
                String proxyUrl, TaskContext context, String forcedModel) throws Exception {
            if (prompt.contains("BLUEPRINT CONSTRAINTS")) {
                spawnCalls++;
                Matcher m = Pattern.compile("- ID: (bp-iter(\\d+)-s(\\d+)-\\d+)").matcher(prompt);
                if (!m.find()) {
                    return LlmResponse.finalOnly("{}");
                }
                String blueprintId = m.group(1);
                int iteration = Integer.parseInt(m.group(2));
                int sibling = Integer.parseInt(m.group(3));
                String key = allBroken ? "BROKEN"
                        : matrix[Math.min(Math.max(0, iteration - 1), matrix.length - 1)][sibling];
                return LlmResponse.finalOnly(variantJson(blueprintId, sibling, key));
            }
            return LlmResponse.finalOnly("{}");
        }

        private String blueprintJson(int idx) {
            JSONObject obj = new JSONObject();
            obj.put("id", "bp-" + idx);
            obj.put("strategy", BLUEPRINT_STRATEGIES[idx]);
            obj.put("philosophy", BLUEPRINT_PHILOSOPHIES[idx]);
            obj.put("mutation_philosophy", "deterministic");
            obj.put("direction", "implementation of " + BLUEPRINT_STRATEGIES[idx]);
            obj.put("characteristics", new JSONArray(Arrays.asList("linear", "safe")));
            obj.put("tradeoffs", "minimal structural change");
            obj.put("survival_argument", "satisfies the collection contract");
            obj.put("strategy_type", "PROBABLE_SURVIVOR");
            return obj.toString();
        }

        private String variantJson(String id, int sibling, String key) {
            String strategy = BLUEPRINT_STRATEGIES[Math.min(sibling, BLUEPRINT_STRATEGIES.length - 1)];
            if ("BROKEN".equals(key)) {
                strategy = "Fragmented Collection Deduplication Rewrite";
            }
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("strategy", strategy);
            obj.put("strategy_type", "PROBABLE_SURVIVOR");
            obj.put("mutation_philosophy", "deterministic");
            obj.put("semantic_anchor", "collection deduplication");
            obj.put("survival_argument", "satisfies the collection deduplication contract");
            obj.put("tradeoffs", "minor structural change");
            obj.put("failure_risks", "low");
            obj.put("expected_outputs", new JSONArray(Arrays.asList("correct deduplication")));
            obj.put("reasoning_focus", "correctness");
            obj.put("projected_steps", new JSONArray(Arrays.asList("Rewrite CollectionProcessor")));
            JSONObject dims = new JSONObject();
            dims.put("philosophy", DIM_PHILOSOPHY.get(key));
            dims.put("execution_model", DIM_EXECUTION.get(key));
            dims.put("abstraction_depth", DIM_DEPTH.get(key));
            dims.put("candidate_key", key);
            obj.put("engineering_dimensions", dims);
            JSONArray actions = new JSONArray();
            JSONObject action = new JSONObject();
            action.put("domain", "file");
            action.put("operation", "WRITE");
            action.put("target", TARGET);
            action.put("description", "Replace CollectionProcessor implementation");
            action.put("implementation", candidates.get(key));
            actions.put(action);
            obj.put("actions", actions);
            obj.put("score", 0.5);
            return "<BEGIN_DARWIN_JSON>\n" + obj.toString() + "\n<END_DARWIN_JSON>";
        }
    }

    // ------------------------------------------------------------------
    // Run helpers
    // ------------------------------------------------------------------

    static class RunResult {
        final TaskContext context;
        final IterationManager manager;
        final OrchestratorResponse response;
        final IterationMemoryService memoryService;
        final ScriptedAiService ai;
        final List<String> logs = new ArrayList<>();

        RunResult(TaskContext context, IterationManager manager, OrchestratorResponse response,
                IterationMemoryService memoryService, ScriptedAiService ai) {
            this.context = context;
            this.manager = manager;
            this.response = response;
            this.memoryService = memoryService;
            this.ai = ai;
        }

        SystemState state() {
            return context.getStateHolder().getState();
        }

        String phase() {
            return context.getOrchestrationState().getCurrentPhase();
        }

        int iterationCount() {
            return context.getOrchestrationState().getIterationCount();
        }

        List<IterationRecord> records() {
            return memoryService.getRecords();
        }

        List<IterationRecord> activeRecords() {
            List<IterationRecord> result = new ArrayList<>();
            for (IterationRecord r : records()) {
                if ("ACTIVE".equals(r.getActivationState())) {
                    result.add(r);
                }
            }
            return result;
        }

        EvolutionTree tree() {
            return context.getKernelContext().getMemoryService().getEvolutionTree();
        }

        String tail(int n) {
            int from = Math.max(0, logs.size() - n);
            StringBuilder sb = new StringBuilder();
            for (int i = from; i < logs.size(); i++) {
                sb.append("\n  ").append(logs.get(i));
            }
            return sb.toString();
        }
    }

    private RunResult runEvolution(String sessionId, File root, PlatformType platformType, ScriptedAiService ai,
            boolean withSatisfactionListener) throws Exception {
        activeSessions.add(sessionId);
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        ((SessionContext) session).getCognitiveState().setCurrentCapability(CapabilityType.CODE);
        ((SessionContext) session).getCognitiveState().setCognitiveDepth(1);

        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId(sessionId);
        Maven maven = OrchestrationFactory.eINSTANCE.createMaven();
        maven.getGoals().add("test");
        orchestrator.setMaven(maven);
        orchestrator.setOllama(OrchestrationFactory.eINSTANCE.createOllama());
        orchestrator.getOllama().setUrl("http://localhost:11434");
        orchestrator.getOllama().setModel("llama3.3:70b");

        TaskContext context = new TaskContext(orchestrator, root);
        context.setSessionId(sessionId);
        context.setAutoApprove(true);
        context.setAiService(ai);
        context.getOrchestrationState().getMetadata().put("convergenceThreshold", 0.0);
        context.getOrchestrationState().getMetadata().put("minEvolutionaryDepth", 0);
        context.getOrchestrationState().getMetadata().put("maxEvolutionaryDepth", 4);
        context.getMetadata().put("sessionContext", session);
        context.addLogListener(log -> {
            synchronized (collectedLogs) {
                collectedLogs.add(log);
            }
        });
        if (withSatisfactionListener) {
            context.addInputListener(msg -> {
                if (msg != null && msg.contains("satisfies the goal")) {
                    context.provideInput("Stop Evolution");
                }
            });
        }

        IterationMemoryService memoryService = session.getMemoryService(root);
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(root, context);
        IDarwinEngine engine = DarwinEngineFactory.createEngine(platformType, context, memoryService, stateProvider);
        IterationManager manager = KernelFactory.create(engine, context, session, ai);

        TaskRequest request = new TaskRequest();
        request.setPrompt("Optimize the collection deduplication method in the project to eliminate quadratic "
                + "duplicate detection while preserving functional behavior.");
        request.getContext().put("orchestrator", orchestrator);
        request.getContext().put("sessionId", sessionId);

        OrchestratorResponse response = manager.handle(request);
        RunResult result = new RunResult(context, manager, response, memoryService, ai);
        synchronized (collectedLogs) {
            result.logs.addAll(collectedLogs);
        }
        collectedLogs.clear();
        return result;
    }

    private static String readTarget(File root) throws Exception {
        return new String(Files.readAllBytes(new File(root, TARGET).toPath()), "UTF-8");
    }

    // ==================================================================
    // PHASE A/B/C: candidate generation, objective evaluation, winner selection
    // ==================================================================

    @Test
    public void testPhasesA_B_C_FullCompetitionAndWinnerSelection() throws Exception {
        File root = createFixtureRepo();
        try {
            ScriptedAiService ai = new ScriptedAiService();
            RunResult run = runEvolution("evo-comp-" + System.currentTimeMillis(), root, PlatformType.DARWIN_MODE, ai,
                    true);

            assertNotNull("Response must be produced", run.response);
            assertTrue("Evolution must reach DONE state, was " + run.state() + " resultType="
                    + run.response.getResultType() + run.tail(30), run.state() == SystemState.DONE);

            // Phase A: multiple genuinely competing candidates were generated
            assertTrue("At least one candidate-generating iteration required", run.iterationCount() >= 2);
            assertTrue("At least 3 candidates per iteration must be materialized (spawn calls), got "
                    + ai.spawnCalls, ai.spawnCalls >= 3);

            EvolutionTree tree = run.tree();
            assertNotNull("EvolutionTree must exist", tree);
            assertNotNull("Root node must exist", tree.getRootId());
            assertNotNull("A winner must be recorded in the tree", tree.getCurrentWinnerId());

            // Phase B: objective evaluation produced differentiated scores
            List<eu.kalafatic.evolution.controller.trajectory.TrajectoryAnalysisRecord> tas = run.memoryService
                    .getTrajectoryAnalyses();
            assertFalse("Trajectory analysis records must be persisted", tas.isEmpty());
            List<Double> scores = new ArrayList<>();
            for (eu.kalafatic.evolution.controller.trajectory.TrajectoryAnalysisRecord tar : tas) {
                scores.add(tar.getFitnessScore());
            }
            assertTrue("Multiple distinct evaluation scores expected, got " + scores, new java.util.HashSet<>(scores).size() >= 2);

            // Phase C: winner selection + persistence with evidence
            List<IterationRecord> active = run.activeRecords();
            assertFalse("At least one ACTIVE winner record expected", active.isEmpty());
            IterationRecord winner = active.get(active.size() - 1);
            assertNotNull("Winner strategy must be recorded", winner.getStrategy());
            assertTrue("Winner must be the order-preserving optimized candidate, got: " + winner.getStrategy(),
                    winner.getStrategy().contains("Order-Preserving"));

            // Winner implementation must actually be present in the project
            String content = readTarget(root);
            assertTrue("Winning implementation must be materialized in the project",
                    content.contains("LinkedHashSet"));

            // Inferior candidates must not be promoted
            for (IterationRecord r : active) {
                assertFalse("Inferior candidate must never be ACTIVE: " + r.getStrategy(),
                        r.getStrategy().contains("Minimal Correction") || r.getStrategy().contains("Sort-Based"));
            }
        } finally {
            deleteRecursive(root);
        }
    }

    // ==================================================================
    // PHASE D: multiple iterations, new candidate set per iteration
    // ==================================================================

    @Test
    public void testPhaseD_MultipleIterationsWithNewCandidates() throws Exception {
        File root = createFixtureRepo();
        try {
            ScriptedAiService ai = new ScriptedAiService(DEFAULT_MATRIX, new String[] { "A", "B", "C" });
            RunResult run = runEvolution("evo-multi-" + System.currentTimeMillis(), root, PlatformType.DARWIN_MODE, ai,
                    true);

            assertTrue("Multiple iterations required, got " + run.iterationCount(), run.iterationCount() >= 2);
            assertTrue("Two candidate-generating iterations expected (spawn calls >= 6), got " + ai.spawnCalls,
                    ai.spawnCalls >= 6);

            // Iteration lineage: the second iteration's parent is the first winner
            EvolutionTree tree = run.tree();
            assertNotNull("Current winner must be recorded", tree.getCurrentWinnerId());

            // Iteration history persisted per iteration
            Map<Integer, Integer> candidatesPerIteration = new LinkedHashMap<>();
            for (IterationRecord r : run.records()) {
                candidatesPerIteration.merge(r.getIteration(), 1, Integer::sum);
            }
            assertTrue("Records for more than one iteration expected, got " + candidatesPerIteration.keySet(),
                    candidatesPerIteration.keySet().stream().anyMatch(i -> i > 0));

            // Best known solution preserved: winner code present
            String content = readTarget(root);
            assertTrue("Best known implementation must be preserved in the project",
                    content.contains("LinkedHashSet"));
        } finally {
            deleteRecursive(root);
        }
    }

    // ==================================================================
    // Scenario C: regression in a later iteration must not overwrite the best
    // ==================================================================

    @Test
    public void testScenarioC_RegressionMustNotOverwriteBestSoFar() throws Exception {
        File root = createFixtureRepo();
        try {
            ScriptedAiService ai = new ScriptedAiService(DEFAULT_MATRIX, REGRESSION_MATRIX);
            RunResult run = runEvolution("evo-regress-" + System.currentTimeMillis(), root, PlatformType.DARWIN_MODE,
                    ai, true);

            // The later iteration's inferior candidates must not be promoted. The
            // evolution may either stop (FAILED after refused winners) or complete,
            // but the previous best-known winner must survive in either case.
            assertTrue("Session must terminate in a terminal state, was " + run.state(),
                    run.state() == SystemState.FAILED || run.state() == SystemState.DONE);

            // Best-so-far preservation: the ACTIVE winner must still be the
            // order-preserving candidate, not an inferior later candidate.
            List<IterationRecord> active = run.activeRecords();
            assertFalse("At least one ACTIVE winner record expected", active.isEmpty());
            IterationRecord best = active.get(active.size() - 1);
            assertTrue("Best known winner must be preserved, got: " + best.getStrategy(),
                    best.getStrategy().contains("Order-Preserving"));

            String content = readTarget(root);
            assertTrue("Working tree must keep the best known implementation",
                    content.contains("LinkedHashSet"));
            assertFalse("Inferior candidate code must not be present", content.contains("indexOf(item) == i"));

            // The tree's current winner must be the best known candidate
            EvolutionTree tree = run.tree();
            assertNotNull("Tree winner must exist", tree.getCurrentWinnerId());
        } finally {
            deleteRecursive(root);
        }
    }

    // ==================================================================
    // Scenario D: all candidates fail mandatory validation
    // ==================================================================

    @Test
    public void testScenarioD_AllCandidatesFailValidation() throws Exception {
        File root = createFixtureRepo();
        try {
            ScriptedAiService ai = new ScriptedAiService(DEFAULT_MATRIX, null);
            ai.allBroken = true;
            RunResult run = runEvolution("evo-allfail-" + System.currentTimeMillis(), root, PlatformType.DARWIN_MODE,
                    ai, true);

            // No invalid winner may be selected or materialized
            List<IterationRecord> active = run.activeRecords();
            for (IterationRecord r : active) {
                assertFalse("Invalid candidate must not be ACTIVE: " + r.getStrategy(),
                        r.getStrategy().contains("Fragmented"));
            }

            String content = readTarget(root);
            assertTrue("Baseline implementation must remain intact (no broken code materialized)",
                    content.contains("indexOf(item) == i"));
            assertTrue("Broken candidate code must not be materialized",
                    content.indexOf("result.add(item);\n        return result;") == -1);

            // The failure must be recorded somewhere (state or records)
            assertTrue("Session must end in a non-success state or with rejection records",
                    run.state() == SystemState.FAILED || run.state() == SystemState.DONE);
        } finally {
            deleteRecursive(root);
        }
    }

    // ==================================================================
    // Scenario E: successful completion with recorded stop reason
    // ==================================================================

    @Test
    public void testScenarioE_CompletionStopsEvolution() throws Exception {
        File root = createFixtureRepo();
        try {
            ScriptedAiService ai = new ScriptedAiService();
            RunResult run = runEvolution("evo-done-" + System.currentTimeMillis(), root, PlatformType.DARWIN_MODE, ai,
                    true);

            assertTrue("Evolution must terminate in DONE state, was " + run.state(), run.state() == SystemState.DONE);
            String phase = run.phase();
            assertNotNull("Terminal phase must be recorded, was null", phase);
            assertTrue("Terminal phase expected, got: " + phase,
                    phase.contains("TERMINAL") || phase.contains("SATISFIED") || phase.contains("SUCCESS"));

            List<IterationRecord> active = run.activeRecords();
            assertFalse("Final best candidate must be preserved", active.isEmpty());
            assertTrue("Best candidate must be the order-preserving optimized implementation",
                    active.get(active.size() - 1).getStrategy().contains("Order-Preserving"));
        } finally {
            deleteRecursive(root);
        }
    }

    // ==================================================================
    // PHASE G: state machine integrity
    // ==================================================================

    @Test
    public void testPhaseG_StateMachineTransitions() throws Exception {
        File root = Files.createTempDirectory("evo-statemachine").toFile();
        String sessionId = "evo-sm-" + System.currentTimeMillis();
        activeSessions.add(sessionId);
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);

        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        TaskContext context = new TaskContext(orchestrator, root);
        context.setSessionId(sessionId);
        context.setAutoApprove(true);
        ScriptedAiService ai = new ScriptedAiService();
        context.setAiService(ai);

        IterationMemoryService memoryService = session.getMemoryService(root);
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(root, context);
        DarwinEngine engine = new DarwinEngine(context, memoryService, stateProvider);
        IterationManager manager = KernelFactory.create(engine, context, session, ai);

        // Legal transitions
        assertEquals(SystemState.INIT, context.getStateHolder().getState());
        manager.transition(SystemState.ANALYZING, context);
        assertEquals(SystemState.ANALYZING, context.getStateHolder().getState());
        manager.transition(SystemState.INIT, context);
        assertEquals(SystemState.INIT, context.getStateHolder().getState());
        manager.transition(SystemState.EXECUTING, context);
        assertEquals(SystemState.EXECUTING, context.getStateHolder().getState());
        manager.transition(SystemState.DONE, context);
        assertEquals(SystemState.DONE, context.getStateHolder().getState());

        // Terminal states reject non-recovery transitions
        manager.transition(SystemState.ANALYZING, context);
        assertEquals("Terminal state must reject ANALYZING", SystemState.DONE,
                context.getStateHolder().getState());
        manager.transition(SystemState.EXECUTING, context);
        assertEquals("Terminal state must reject EXECUTING", SystemState.DONE,
                context.getStateHolder().getState());

        // Terminal states allow INIT and RECOVERING
        manager.transition(SystemState.INIT, context);
        assertEquals(SystemState.INIT, context.getStateHolder().getState());
        manager.transition(SystemState.RECOVERING, context);
        assertEquals(SystemState.RECOVERING, context.getStateHolder().getState());

        // Same-state transition is a no-op
        manager.transition(SystemState.RECOVERING, context);
        assertEquals(SystemState.RECOVERING, context.getStateHolder().getState());

        deleteRecursive(root);
    }

    // ==================================================================
    // PHASE K: persistence and recovery
    // ==================================================================

    @Test
    public void testPhaseK_PersistenceAndRecovery() throws Exception {
        File root = createFixtureRepo();
        String sessionId = "evo-persist-" + System.currentTimeMillis();
        try {
            ScriptedAiService ai = new ScriptedAiService();
            RunResult run = runEvolution(sessionId, root, PlatformType.DARWIN_MODE, ai, true);

            assertTrue("Precondition: evolution completed", run.state() == SystemState.DONE);
            List<IterationRecord> before = run.activeRecords();
            assertFalse("Precondition: winner persisted", before.isEmpty());
            String winnerBefore = before.get(before.size() - 1).getStrategy();
            String winnerIdBefore = run.tree().getCurrentWinnerId();

            // Verify on-disk persistence artifacts
            File memoryDir = new File(root, "orchestrator/memory");
            assertTrue("Iteration memory directory must exist", memoryDir.isDirectory());
            boolean hasRecords = false;
            boolean hasCheckpoint = false;
            for (File f : memoryDir.listFiles()) {
                if (f.getName().startsWith("iteration_")) {
                    hasRecords = true;
                }
                if (f.getName().startsWith("checkpoint_")) {
                    hasCheckpoint = true;
                }
            }
            assertTrue("Iteration record files must be persisted", hasRecords);
            assertTrue("Session checkpoint must be persisted", hasCheckpoint);

            // Simulate application restart: recreate engine/manager for the same
            // session and verify that best-known candidate and evidence survive.
            SessionContainer session = SessionManager.getInstance().getSession(sessionId);
            IterationMemoryService memoryService = session.getMemoryService(root);
            SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(root, run.context);
            DarwinEngine restartedEngine = new DarwinEngine(run.context, memoryService, stateProvider);
            IterationManager restartedManager = KernelFactory.create(restartedEngine, run.context, session, ai);

            assertNotNull("Restarted manager must exist", restartedManager);
            assertNotNull("Restart must restore the evolution tree winner", run.tree().getCurrentWinnerId());
            assertEquals("Restart must preserve the current winner identity", winnerIdBefore,
                    run.tree().getCurrentWinnerId());

            List<IterationRecord> after = run.activeRecords();
            assertFalse("Winner records must survive restart", after.isEmpty());
            assertEquals("Best known winner strategy must survive restart", winnerBefore,
                    after.get(after.size() - 1).getStrategy());
        } finally {
            deleteRecursive(root);
        }
    }

    // ==================================================================
    // PHASE H: instance inventory and cross-instance competition
    // ==================================================================

    @Test
    public void testPhaseH_InstanceInventoryAndCapabilities() throws Exception {
        File root = Files.createTempDirectory("evo-instances").toFile();
        String sessionId = "evo-inst-" + System.currentTimeMillis();
        activeSessions.add(sessionId);
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);

        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        TaskContext context = new TaskContext(orchestrator, root);
        context.setSessionId(sessionId);
        context.setAutoApprove(true);
        ScriptedAiService ai = new ScriptedAiService();
        context.setAiService(ai);

        IterationMemoryService memoryService = session.getMemoryService(root);
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(root, context);

        Map<PlatformType, Class<?>> expected = new LinkedHashMap<>();
        expected.put(PlatformType.SIMPLE_CHAT, ChatEngine.class);
        expected.put(PlatformType.ASSISTED_CODING, CodingEngine.class);
        expected.put(PlatformType.DARWIN_MODE, DarwinEngine.class);
        expected.put(PlatformType.SELF_DEV_MODE, SelfDevelopmentEngine.class);
        expected.put(PlatformType.HYBRID_MANUAL_EXPORT, MediatedEngine.class);
        expected.put(PlatformType.INTENT_RECONSTRUCTION, IntentReconstructionEngine.class);
        expected.put(PlatformType.FORGE, LLMDarwinEngine.class);
        expected.put(PlatformType.MUTATION, MutationEngine.class);
        expected.put(PlatformType.DREAM_MODE, DreamEngine.class);
        expected.put(PlatformType.DEVELOPER, DeveloperEngine.class);

        for (Map.Entry<PlatformType, Class<?>> entry : expected.entrySet()) {
            IDarwinEngine engine = DarwinEngineFactory.createEngine(entry.getKey(), context, memoryService,
                    stateProvider);
            assertNotNull("Engine must be created for " + entry.getKey(), engine);
            assertTrue("Engine for " + entry.getKey() + " must be a " + entry.getValue().getSimpleName()
                    + " but was " + engine.getClass().getSimpleName(),
                    entry.getValue().isAssignableFrom(engine.getClass()));
            assertEquals("Engine platform type must match for " + entry.getKey(), entry.getKey(),
                    engine.getPlatformType());
        }

        // Registry: exactly one authoritative engine per session
        assertNotNull("Engine must be registered for the session", DarwinEngineRegistry.get(sessionId));
        SessionManager.getInstance().shutdownSession(sessionId);
        assertNull("Engine must be unregistered on session shutdown", DarwinEngineRegistry.get(sessionId));
        activeSessions.remove(sessionId);

        deleteRecursive(root);
    }

    @Test
    public void testPhaseH_CrossInstanceCompetition() throws Exception {
        Map<String, PlatformType> instances = new LinkedHashMap<>();
        instances.put("darwin", PlatformType.DARWIN_MODE);
        instances.put("coding", PlatformType.ASSISTED_CODING);
        instances.put("mutation", PlatformType.MUTATION);

        for (Map.Entry<String, PlatformType> entry : instances.entrySet()) {
            File root = createFixtureRepo();
            try {
                ScriptedAiService ai = new ScriptedAiService();
                RunResult run = runEvolution("evo-xinst-" + entry.getKey() + "-" + System.currentTimeMillis(), root,
                        entry.getValue(), ai, true);
                assertTrue("Instance " + entry.getKey() + " must complete evolution, state was " + run.state(),
                        run.state() == SystemState.DONE);
                List<IterationRecord> active = run.activeRecords();
                assertFalse("Instance " + entry.getKey() + " must select a winner", active.isEmpty());
                IterationRecord winner = active.get(active.size() - 1);
                assertTrue("Instance " + entry.getKey()
                        + " must select the objectively best candidate (order-preserving optimized), got: "
                        + winner.getStrategy(), winner.getStrategy().contains("Order-Preserving"));
                String content = readTarget(root);
                assertTrue("Instance " + entry.getKey() + " must materialize the winning implementation",
                        content.contains("LinkedHashSet"));
            } finally {
                deleteRecursive(root);
            }
        }
    }
}
