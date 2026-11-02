package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.KernelFacade;
import eu.kalafatic.evolution.controller.orchestration.KernelFactory;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.selfdev.LLMDarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.util.ModeRecognizer;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;

public class ExplicitModeIntentBypassTest {

    private File tempDir;
    private Orchestrator orchestrator;
    private TaskContext context;
    private SessionContainer session;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("mode-bypass-test").toFile();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("session-" + System.currentTimeMillis());
        session = SessionManager.getInstance().getOrCreateSession(context.getSessionId());
    }

    @Test
    public void testModeRecognizerIncludesForgeInForcedTechnicalMode() {
        orchestrator.setAiMode(AiMode.FORGE);
        assertTrue("isForgeMode must return true when AiMode is FORGE", ModeRecognizer.isForgeMode(context));
        assertTrue("isForcedTechnicalMode must return true for FORGE mode", ModeRecognizer.isForcedTechnicalMode(context));
        assertEquals("determineType must resolve to PlatformType.FORGE", PlatformType.FORGE, ModeRecognizer.determineType(context));
    }

    @Test
    public void testPromptIntentAnalyzerBypassesLlmWhenForgeModeActive() throws Exception {
        orchestrator.setAiMode(AiMode.FORGE);

        AtomicInteger callCount = new AtomicInteger(0);
        AiService failingAiService = new AiService() {
            @Override
            public String sendRequest(Orchestrator orch, String prompt, TaskContext ctx) throws Exception {
                callCount.incrementAndGet();
                throw new HttpTimeoutException("request timed out");
            }
        };

        PromptIntentAnalyzer analyzer = new PromptIntentAnalyzer(session, tempDir);
        analyzer.setAiService(failingAiService);

        PromptIntentAnalyzer.IntentResult result = analyzer.analyze("Train local EVO LLM model (SMALL) on configured dataset training sources.", context);

        assertNotNull(result);
        assertTrue("Intent category must be TASK for forced technical modes", result.isTask());
        assertEquals("LLM must not be called when FORGE mode is active", 0, callCount.get());
    }

    @Test
    public void testPromptIntentAnalyzerBypassesLlmForOtherForcedTechnicalModes() throws Exception {
        AiMode[] forcedModes = new AiMode[] { AiMode.DREAM, AiMode.MEDIATED, AiMode.INTENT };
        for (AiMode m : forcedModes) {
            orchestrator.setAiMode(m);
            AtomicInteger callCount = new AtomicInteger(0);
            AiService failingAiService = new AiService() {
                @Override
                public String sendRequest(Orchestrator orch, String prompt, TaskContext ctx) throws Exception {
                    callCount.incrementAndGet();
                    throw new HttpTimeoutException("request timed out");
                }
            };

            PromptIntentAnalyzer analyzer = new PromptIntentAnalyzer(session, tempDir);
            analyzer.setAiService(failingAiService);

            PromptIntentAnalyzer.IntentResult result = analyzer.analyze("Perform mode specific task", context);
            assertNotNull(result);
            assertEquals("LLM must not be called when " + m + " mode is active", 0, callCount.get());
        }
    }

    @Test
    public void testKernelFactoryBypassesIntentAnalysisWhenForgeModeActive() throws Exception {
        orchestrator.setAiMode(AiMode.FORGE);

        AtomicInteger callCount = new AtomicInteger(0);
        AiService failingAiService = new AiService() {
            @Override
            public String sendRequest(Orchestrator orch, String prompt, TaskContext ctx) throws Exception {
                callCount.incrementAndGet();
                throw new HttpTimeoutException("request timed out");
            }
        };

        IterationManager manager = KernelFactory.create(
            "Train local EVO LLM model (SMALL) on configured dataset training sources.",
            context,
            session,
            failingAiService
        );

        assertNotNull(manager);
        assertEquals("LLM intent analyzer must NOT be invoked when FORGE mode is authoritative", 0, callCount.get());
        assertEquals("Engine must be LLMDarwinEngine for FORGE", PlatformType.FORGE, manager.getDarwinEngine().getPlatformType());
        assertTrue(manager.getDarwinEngine() instanceof LLMDarwinEngine);
    }

    @Test
    public void testKernelFacadeBypassesIntentAnalysisWhenForgeModeActive() throws Exception {
        orchestrator.setAiMode(AiMode.FORGE);

        AtomicInteger callCount = new AtomicInteger(0);
        AiService failingAiService = new AiService() {
            @Override
            public String sendRequest(Orchestrator orch, String prompt, TaskContext ctx) throws Exception {
                callCount.incrementAndGet();
                throw new HttpTimeoutException("request timed out");
            }
        };

        // Create initial manager in session with FORGE mode
        IterationManager initialManager = KernelFactory.create(
            "Initial prompt",
            context,
            session,
            failingAiService
        );
        session.setIterationManager(initialManager);

        KernelFacade facade = new KernelFacade();
        Task task = OrchestrationFactory.eINSTANCE.createTask();
        task.setName("Forge Task");
        task.setPrompt("Train local EVO LLM model (SMALL) on configured dataset training sources.");

        // executeTask delegates to kernel execution without re-analyzing intent with LLM
        try {
            facade.executeTask(task, context);
        } catch (Exception e) {
            // Task execution may throw due to unconfigured tasks/tools in test mock,
            // but the critical assertion is that intent analysis was NOT called
        }

        assertEquals("KernelFacade must NOT invoke LLM intent analysis when FORGE mode is active", 0, callCount.get());
    }

    @Test
    public void testOrdinaryRequestStillUsesIntentClassification() throws Exception {
        // Default mode (no forced technical mode)
        orchestrator.setAiMode(null);

        AtomicInteger callCount = new AtomicInteger(0);
        AiService mockAiService = new AiService() {
            @Override
            public String sendRequest(Orchestrator orch, String prompt, TaskContext ctx) throws Exception {
                callCount.incrementAndGet();
                return "<BEGIN_JSON>\n{\"category\": \"TASK\", \"confidence\": 0.95, \"reasoning\": \"Code refactoring request\"}\n<END_JSON>";
            }
        };

        PromptIntentAnalyzer analyzer = new PromptIntentAnalyzer(session, tempDir);
        analyzer.setAiService(mockAiService);

        PromptIntentAnalyzer.IntentResult result = analyzer.analyze("Refactor class OrderService to extract helper methods", context);

        assertNotNull(result);
        assertEquals("Ordinary technical prompt without pre-established mode must call LLM intent analyzer", 1, callCount.get());
        assertTrue(result.isTask());
    }
}
