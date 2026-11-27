package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.util.ModeRecognizer;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Regression test for the "LOCAL selection unexpectedly starts FORGE" defect.
 *
 * <p>The defect: a newly created AI Commander session inherited the previous (FORGE
 * training) session's {@code aiMode} and Forge target, so the LOCAL selection was
 * silently overridden and the Darwin factory dispatched FORGE. This test pins the mode
 * resolution contract: {@code AiMode.LOCAL} must resolve to {@link PlatformType#SIMPLE_CHAT}
 * (local inference) and never to {@link PlatformType#FORGE}, while an explicit FORGE
 * selection still resolves to FORGE.</p>
 */
public class LocalModeForgeIsolationTest {

    private File tempDir;
    private Orchestrator orchestrator;
    private TaskContext context;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("local-forge-isolation-test").toFile();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("local-forge-isolation-" + System.currentTimeMillis());
    }

    @Test
    public void testLocalModeResolvesToSimpleChatNotForge() {
        orchestrator.setAiMode(AiMode.LOCAL);

        assertFalse("LOCAL must not be a forced technical mode",
                ModeRecognizer.isForcedTechnicalMode(context));
        assertFalse("LOCAL must not be recognized as FORGE",
                ModeRecognizer.isForgeMode(context));
        assertEquals("LOCAL must resolve to SIMPLE_CHAT (local inference), never FORGE",
                PlatformType.SIMPLE_CHAT, ModeRecognizer.determineType(context));
    }

    @Test
    public void testForgeModeStillResolvesToForge() {
        orchestrator.setAiMode(AiMode.FORGE);

        assertTrue("FORGE must be a forced technical mode",
                ModeRecognizer.isForcedTechnicalMode(context));
        assertTrue("FORGE must be recognized as FORGE",
                ModeRecognizer.isForgeMode(context));
        assertEquals("Explicit FORGE must still resolve to FORGE",
                PlatformType.FORGE, ModeRecognizer.determineType(context));
    }

    @Test
    public void testLocalModeWithProviderAndModelDoesNotActivateForge() {
        // LOCAL + a trained model + a local provider must stay LOCAL inference.
        orchestrator.setAiMode(AiMode.LOCAL);
        orchestrator.setLocalModel("evo-trained-model");
        context.getMetadata().put("inferenceEngine", "Ollama");

        assertFalse("Provider/model selection must not activate FORGE",
                ModeRecognizer.isForgeMode(context));
        assertEquals("LOCAL + provider + model must resolve to SIMPLE_CHAT",
                PlatformType.SIMPLE_CHAT, ModeRecognizer.determineType(context));
    }

    @Test
    public void testSessionAiModeIsolation() {
        // Two sessions must hold independent modes; a FORGE session must not leak its
        // mode into a separate LOCAL session.
        ChatSession forgeSession = OrchestrationFactory.eINSTANCE.createChatSession();
        forgeSession.setId("forge-session");
        forgeSession.setAiMode(AiMode.FORGE);

        ChatSession localSession = OrchestrationFactory.eINSTANCE.createChatSession();
        localSession.setId("local-session");
        localSession.setAiMode(AiMode.LOCAL);

        assertEquals(AiMode.FORGE, forgeSession.getAiMode());
        assertEquals(AiMode.LOCAL, localSession.getAiMode());
        assertFalse("The two sessions must not share the same mode",
                forgeSession.getAiMode().equals(localSession.getAiMode()));
    }

    @Test
    public void testNewSessionDefaultsToLocalMode() {
        // A freshly created ChatSession must default to LOCAL (the EMF default), so it
        // can never silently inherit FORGE unless explicitly assigned.
        ChatSession fresh = OrchestrationFactory.eINSTANCE.createChatSession();
        assertEquals("A new ChatSession must default to LOCAL",
                AiMode.LOCAL, fresh.getAiMode());
    }
}
