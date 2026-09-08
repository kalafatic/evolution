package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer.IntentCategory;
import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer.IntentResult;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.util.ModeRecognizer;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class IntentClassificationControlFlowTest {

    private TaskContext context;

    @Before
    public void setUp() {
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        context = new TaskContext(orchestrator, new File("."));
    }

    @Test
    public void testConversationalGreetingsResolveToChatWithoutEscalation() throws Exception {
        String[] greetings = new String[] { "hi", "hello", "hey", "good morning", "thanks", "thank you" };

        for (String g : greetings) {
            assertTrue("isSimpleGreeting must be true for: " + g, PromptIntentAnalyzer.isSimpleGreeting(g));

            IntentResult result = new IntentResult(IntentCategory.CHAT, 1.0, "Conversational greeting", null, null);
            PlatformType platformType = ModeRecognizer.determineType(result, context);

            assertEquals("PlatformType for greeting '" + g + "' must be SIMPLE_CHAT", PlatformType.SIMPLE_CHAT, platformType);
            assertNotEquals("Greeting must NOT escalate to ASSISTED_CODING", PlatformType.ASSISTED_CODING, platformType);
            assertNotEquals("Greeting must NOT escalate to DARWIN_MODE", PlatformType.DARWIN_MODE, platformType);
        }
    }

    @Test
    public void testMalformedOrLowConfidenceFallbackToChat() {
        IntentResult lowConfidenceTask = new IntentResult(IntentCategory.CHAT, 0.3, "Low confidence, safe fallback to CHAT", null, null);

        PlatformType platformType = ModeRecognizer.determineType(lowConfidenceTask, context);
        assertEquals("Low confidence classification must resolve to SIMPLE_CHAT", PlatformType.SIMPLE_CHAT, platformType);
        assertNotEquals("Low confidence classification must NOT escalate to ASSISTED_CODING", PlatformType.ASSISTED_CODING, platformType);
    }

    @Test
    public void testInternalRepairFlagPreventsTaskMode() {
        context.getOrchestrationState().getRawInput();
        context.getOrchestrationState().getMetadata().put("isInternalRepair", true);

        ModeRecognizer modeRecognizer = new ModeRecognizer(null);
        boolean isChat = modeRecognizer.isChatMode(context);

        assertTrue("isChatMode must return true during internal repair requests to isolate them", isChat);
    }

    @Test
    public void testHighConfidenceTaskResolvesToAssistedCoding() {
        IntentResult validTask = new IntentResult(IntentCategory.TASK, 0.95, "Valid code creation request", "CODE_CREATION", "UserService.java");

        PlatformType platformType = ModeRecognizer.determineType(validTask, context);
        assertEquals("High confidence TASK must resolve to ASSISTED_CODING", PlatformType.ASSISTED_CODING, platformType);
    }
}
