package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer.IntentCategory;
import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer.IntentResult;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.ModeRouter;
import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.util.ModeRecognizer;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Regression test suite verifying deterministic precedence and safety across
 * Darwin modes and intent classification:
 * 1. hi -> CHAT -> SIMPLE_CHAT
 * 2. explicit coding request -> appropriate coding workflow (ASSISTED_CODING)
 * 3. explicit FORGE request -> FORGE
 * 4. explicit DREAM activation -> DREAM_MODE
 * 5. explicit SELF-DEV request -> SELF_DEV_MODE
 * 6. malformed intent response -> safe failure or UNKNOWN (never escalating to TASK)
 * 7. forced mode -> no unnecessary LLM classification (zero AI service requests)
 */
public class DarwinModesAndRoutingRegressionTest {

    private File tempDir;
    private Orchestrator orchestrator;
    private TaskContext context;
    private SessionContainer session;
    private ModeRouter router;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("darwin-modes-regression-test").toFile();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("regression-session-" + System.currentTimeMillis());
        session = SessionManager.getInstance().getOrCreateSession(context.getSessionId());
        router = new ModeRouter();
    }

    /**
     * Test 1: hi -> CHAT -> SIMPLE_CHAT
     * Simple greetings must resolve directly to SIMPLE_CHAT and must NEVER escalate
     * to ASSISTED_CODING or DARWIN_MODE.
     */
    @Test
    public void testGreetingResolvesToChatAndSimpleChat() throws Exception {
        String[] greetings = { "hi", "hello", "hey", "good morning", "thanks", "ok" };

        for (String greeting : greetings) {
            assertTrue("Greeting '" + greeting + "' must be recognized as simple greeting",
                    PromptIntentAnalyzer.isSimpleGreeting(greeting));

            // Test analyzer directly
            AtomicInteger callCount = new AtomicInteger(0);
            AiService countingAiService = new AiService() {
                @Override
                public String sendRequest(Orchestrator orch, String prompt, TaskContext ctx) throws Exception {
                    callCount.incrementAndGet();
                    return "{}";
                }
            };

            PromptIntentAnalyzer analyzer = new PromptIntentAnalyzer(session, tempDir);
            analyzer.setAiService(countingAiService);

            IntentResult intent = analyzer.analyze(greeting, context);
            assertNotNull(intent);
            assertTrue("Intent category for greeting '" + greeting + "' must be CHAT", intent.isChat());
            assertEquals("Greeting must NOT call external LLM", 0, callCount.get());

            // Test ModeRecognizer determination
            PlatformType resolvedType = ModeRecognizer.determineType(intent, context);
            assertEquals("Resolved PlatformType must be SIMPLE_CHAT for greeting '" + greeting + "'",
                    PlatformType.SIMPLE_CHAT, resolvedType);

            // Test ModeRouter
            PlatformMode mode = router.route(greeting, orchestrator);
            assertEquals("Router must map greeting to SIMPLE_CHAT", PlatformType.SIMPLE_CHAT, mode.getType());
        }
    }

    /**
     * Test 2: explicit coding request -> appropriate coding workflow (ASSISTED_CODING)
     */
    @Test
    public void testExplicitCodingRequestResolvesToAssistedCoding() throws Exception {
        // Direct prompt directive
        PlatformMode modeDirective = router.route("mode: assisted - Implement user repository", orchestrator);
        assertEquals(PlatformType.ASSISTED_CODING, modeDirective.getType());

        // Cognitive prompt classification
        PlatformMode modeFast = router.routeFast("Create a java class UserService with tests", orchestrator);
        assertEquals(PlatformType.ASSISTED_CODING, modeFast.getType());

        // IntentResult mapping
        IntentResult taskIntent = new IntentResult(IntentCategory.TASK, 0.95, "Create OrderService implementation", "CODE_CREATION", "OrderService.java");
        PlatformType mappedType = ModeRecognizer.determineType(taskIntent, context);
        assertEquals(PlatformType.ASSISTED_CODING, mappedType);
    }

    /**
     * Test 3: explicit FORGE request -> FORGE
     */
    @Test
    public void testExplicitForgeRequestResolvesToForge() {
        // Via UI AiMode override
        orchestrator.setAiMode(AiMode.FORGE);
        PlatformMode modeUi = router.route("Analyze training parameters", orchestrator);
        assertEquals(PlatformType.FORGE, modeUi.getType());

        // Reset and test via prompt directive
        orchestrator.setAiMode(null);
        PlatformMode modePrompt = router.route("mode: forge - Train local model", orchestrator);
        assertEquals(PlatformType.FORGE, modePrompt.getType());
    }

    /**
     * Test 4: explicit DREAM activation -> DREAM_MODE
     */
    @Test
    public void testExplicitDreamActivationResolvesToDream() {
        // Via UI AiMode override
        orchestrator.setAiMode(AiMode.DREAM);
        PlatformMode modeUi = router.route("Consolidate experiences", orchestrator);
        assertEquals(PlatformType.DREAM_MODE, modeUi.getType());

        // Reset and test via prompt directive
        orchestrator.setAiMode(null);
        PlatformMode modePrompt = router.route("mode: dream", orchestrator);
        assertEquals(PlatformType.DREAM_MODE, modePrompt.getType());
    }

    /**
     * Test 5: explicit SELF-DEV request -> SELF_DEV_MODE
     */
    @Test
    public void testExplicitSelfDevRequestResolvesToSelfDevMode() {
        // Via prompt directive
        PlatformMode modePrompt = router.route("mode: self-dev - Refactor supervisor communication", orchestrator);
        assertEquals(PlatformType.SELF_DEV_MODE, modePrompt.getType());
        assertTrue("SelfDev mode must allow self-modification", modePrompt.isAllowSelfModify());
        assertTrue("SelfDev mode must contain allowed controller path",
                modePrompt.getAllowedPaths().contains("eu.kalafatic.evolution.controller/src"));

        // Via prompt instructions self-iterative flag
        if (orchestrator.getAiChat() == null) {
            orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        }
        if (orchestrator.getAiChat().getPromptInstructions() == null) {
            orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());
        }
        orchestrator.getAiChat().getPromptInstructions().setSelfIterativeMode(true);

        PlatformMode modeFlag = router.route("Perform platform maintenance", orchestrator);
        assertEquals(PlatformType.SELF_DEV_MODE, modeFlag.getType());
    }

    /**
     * Test 6: malformed intent response -> safe failure or UNKNOWN (never escalating to TASK)
     */
    @Test
    public void testMalformedIntentResponseResolvesToUnknownOrSafeChat() throws Exception {
        String[] malformedResponses = {
            "Not JSON at all! Random LLM text output.",
            "<BEGIN_JSON>\n{ \"invalid\": \"structure\" }\n<END_JSON>",
            "<BEGIN_JSON>\n{ \"category\": \"NONSENSE_CATEGORY\", \"confidence\": 0.99 }\n<END_JSON>",
            "<BEGIN_JSON>\n{ \"category\": \"TASK\", \"confidence\": 0.15, \"reasoning\": \"Highly unsure\" }\n<END_JSON>",
            ""
        };

        for (String malformed : malformedResponses) {
            AiService mockAiService = new AiService() {
                @Override
                public String sendRequest(Orchestrator orch, String prompt, TaskContext ctx) throws Exception {
                    return malformed;
                }
            };

            PromptIntentAnalyzer analyzer = new PromptIntentAnalyzer(session, tempDir);
            analyzer.setAiService(mockAiService);

            IntentResult result = analyzer.analyze("Some ambiguous user query " + System.nanoTime(), context);
            assertNotNull(result);

            // Assert it never escalated to valid TASK
            assertFalse("Malformed response must NOT result in a valid high-confidence TASK: " + malformed,
                    result.isTask() && result.getConfidence() >= 0.6);

            // Determine platform type
            PlatformType resolved = ModeRecognizer.determineType(result, context);
            assertEquals("Malformed response must safely default to SIMPLE_CHAT", PlatformType.SIMPLE_CHAT, resolved);
            assertNotEquals("Malformed response must NEVER escalate to ASSISTED_CODING", PlatformType.ASSISTED_CODING, resolved);
            assertNotEquals("Malformed response must NEVER escalate to DARWIN_MODE", PlatformType.DARWIN_MODE, resolved);
        }
    }

    /**
     * Test 7: forced mode -> no unnecessary LLM classification (zero AI service requests)
     */
    @Test
    public void testForcedModeBypassesLlmClassificationCompletely() throws Exception {
        AiMode[] forcedModes = { AiMode.FORGE, AiMode.DREAM, AiMode.MEDIATED, AiMode.INTENT };

        for (AiMode mode : forcedModes) {
            orchestrator.setAiMode(mode);

            AtomicInteger callCount = new AtomicInteger(0);
            AiService failingAiService = new AiService() {
                @Override
                public String sendRequest(Orchestrator orch, String prompt, TaskContext ctx) throws Exception {
                    callCount.incrementAndGet();
                    throw new HttpTimeoutException("Should not be invoked!");
                }
            };

            PromptIntentAnalyzer analyzer = new PromptIntentAnalyzer(session, tempDir);
            analyzer.setAiService(failingAiService);

            IntentResult result = analyzer.analyze("Perform work in forced mode", context);
            assertNotNull(result);
            assertEquals("Zero LLM calls must be made when forced mode " + mode + " is active", 0, callCount.get());

            PlatformType resolved = ModeRecognizer.determineType(result, context);
            assertNotEquals(PlatformType.SIMPLE_CHAT, resolved);
        }
    }
}
