package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer.IntentCategory;
import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer.IntentResult;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.AutonomyLevel;
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
 * Regression tests for the DYNAMIC platform-mode lifecycle within a single
 * persistent session. These guard the corrections to mode selection:
 *
 * <ul>
 *   <li>A non-null {@link PlatformMode} (a derived routing cache left over from a
 *       prior iteration) must NOT disable LLM intent classification — otherwise every
 *       non-greeting request after the first turn is forced to ASSISTED_CODING.</li>
 *   <li>An auto-selected mode must NOT be treated as a sticky "forced technical mode";
 *       only explicit overrides (AiMode / darwin flag / self-iterative flag / mutation
 *       session / behavior trait) force a mode.</li>
 *   <li>{@link ModeRouter#createMode(PlatformType)} must build a mode whose constraints
 *       (autonomy, allowed paths) match the selected type.</li>
 * </ul>
 */
public class DarwinDynamicModeLifecycleTest {

    private File tempDir;
    private Orchestrator orchestrator;
    private TaskContext context;
    private SessionContainer session;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("darwin-dynamic-mode-test").toFile();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("dynamic-mode-session-" + System.currentTimeMillis());
        session = SessionManager.getInstance().getOrCreateSession(context.getSessionId());
    }

    /**
     * D1 — a prior iteration left a non-forced PlatformMode (e.g. SIMPLE_CHAT) on the
     * context. A subsequent non-greeting request must STILL be classified via the LLM
     * (not silently forced to TASK -> ASSISTED_CODING).
     */
    @Test
    public void testNonForcedPlatformModeStillRunsIntentClassification() throws Exception {
        // Simulate a prior chat turn that set the derived platform mode.
        context.setPlatformMode(new PlatformMode(PlatformType.SIMPLE_CHAT, AutonomyLevel.LOW, 1, false));

        AtomicInteger callCount = new AtomicInteger(0);
        AiService mockAiService = new AiService() {
            @Override
            public String sendRequest(Orchestrator orch, String prompt, TaskContext ctx) throws Exception {
                callCount.incrementAndGet();
                return "<BEGIN_JSON>\n{ \"category\": \"TASK\", \"confidence\": 0.95, \"reasoning\": \"create a class\" }\n<END_JSON>";
            }
        };

        PromptIntentAnalyzer analyzer = new PromptIntentAnalyzer(session, tempDir);
        analyzer.setAiService(mockAiService);

        IntentResult result = analyzer.analyze("Create a Java class UserService", context);

        // The LLM MUST have been invoked (regression: early-exit used to return TASK
        // without any LLM call whenever platformMode != null).
        assertEquals("Non-forced platform mode must not skip LLM intent classification", 1, callCount.get());
        assertTrue("LLM classification must be honoured", result.isTask());

        PlatformType resolved = ModeRecognizer.determineType(result, context);
        assertEquals(PlatformType.ASSISTED_CODING, resolved);
    }

    /**
     * D2 — an auto-selected (cached) mode must not lock the session; only explicit
     * overrides force a mode.
     */
    @Test
    public void testAutoSelectedModesAreNotForced() {
        // Auto-selected SIMPLE_CHAT (derived cache) must NOT be a forced technical mode.
        context.setPlatformMode(new PlatformMode(PlatformType.SIMPLE_CHAT, AutonomyLevel.LOW, 1, false));
        assertFalse("SIMPLE_CHAT cache must not be forced", ModeRecognizer.isForcedTechnicalMode(context));

        // Auto-selected ASSISTED_CODING (derived cache) must NOT be a forced technical mode.
        context.setPlatformMode(new PlatformMode(PlatformType.ASSISTED_CODING, AutonomyLevel.LOW, 2, false));
        assertFalse("ASSISTED_CODING cache must not be forced", ModeRecognizer.isForcedTechnicalMode(context));

        // Explicit AiMode overrides MUST remain forced.
        context.setPlatformMode(null);
        orchestrator.setAiMode(AiMode.FORGE);
        assertTrue("AiMode.FORGE must be forced", ModeRecognizer.isForcedTechnicalMode(context));
        assertEquals(PlatformType.FORGE, ModeRecognizer.determineType(context));
        orchestrator.setAiMode(null);

        // Explicit darwin-mode flag MUST remain forced.
        orchestrator.setDarwinMode(true);
        assertTrue("darwin flag must be forced", ModeRecognizer.isForcedTechnicalMode(context));
        assertEquals(PlatformType.DARWIN_MODE, ModeRecognizer.determineType(context));
        orchestrator.setDarwinMode(false);
    }

    /**
     * D2 — greetings must resolve back to SIMPLE_CHAT even when a coding mode was active,
     * which is the behaviour that makes chat <-> coding transitions work dynamically.
     */
    @Test
    public void testGreetingReturnsToChatAfterCodingMode() throws Exception {
        // A coding turn was active.
        context.setPlatformMode(new PlatformMode(PlatformType.ASSISTED_CODING, AutonomyLevel.LOW, 2, false));

        PromptIntentAnalyzer analyzer = new PromptIntentAnalyzer(session, tempDir);
        analyzer.setAiService(new AiService() {
            @Override
            public String sendRequest(Orchestrator orch, String prompt, TaskContext ctx) {
                throw new UnsupportedOperationException("Greeting must not call the LLM");
            }
        });

        IntentResult result = analyzer.analyze("Thanks!", context);
        assertTrue("Greeting must classify as CHAT", result.isChat());
        assertEquals("Greeting must resolve to SIMPLE_CHAT", PlatformType.SIMPLE_CHAT,
                ModeRecognizer.determineType(result, context));
    }

    /**
     * D3 — ModeRouter.createMode must build the authoritative PlatformMode with the
     * correct constraints for each type (single source of truth used to sync the
     * cached platform mode with the selected engine).
     */
    @Test
    public void testCreateModeBuildsCorrectMode() {
        PlatformMode selfDev = ModeRouter.createMode(PlatformType.SELF_DEV_MODE);
        assertEquals(PlatformType.SELF_DEV_MODE, selfDev.getType());
        assertTrue("SELF_DEV must allow self-modification", selfDev.isAllowSelfModify());
        assertFalse("SELF_DEV must constrain allowed paths", selfDev.getAllowedPaths().isEmpty());

        PlatformMode coding = ModeRouter.createMode(PlatformType.ASSISTED_CODING);
        assertEquals(PlatformType.ASSISTED_CODING, coding.getType());

        PlatformMode darwin = ModeRouter.createMode(PlatformType.DARWIN_MODE);
        assertEquals(PlatformType.DARWIN_MODE, darwin.getType());

        PlatformMode chat = ModeRouter.createMode(PlatformType.SIMPLE_CHAT);
        assertEquals(PlatformType.SIMPLE_CHAT, chat.getType());

        PlatformMode mutation = ModeRouter.createMode(PlatformType.MUTATION);
        assertEquals(PlatformType.MUTATION, mutation.getType());
    }
}
