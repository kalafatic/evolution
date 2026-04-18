package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.ModeRouter;
import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class ModeRouterTest {

    private ModeRouter router;
    private Orchestrator orchestrator;

    @Before
    public void setUp() {
        router = new ModeRouter();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
    }

    @Test
    public void testExplicitChatMode() {
        PlatformMode mode = router.route("mode: chat - Tell me a joke", orchestrator);
        assertEquals(PlatformType.SIMPLE_CHAT, mode.getType());
    }

    @Test
    public void testExplicitAssistedMode() {
        PlatformMode mode = router.route("mode: assisted - Fix this bug", orchestrator);
        assertEquals(PlatformType.ASSISTED_CODING, mode.getType());
    }

    @Test
    public void testExplicitDarwinMode() {
        PlatformMode mode = router.route("mode: darwin - Refactor the module", orchestrator);
        assertEquals(PlatformType.DARWIN_MODE, mode.getType());
    }

    @Test
    public void testExplicitSelfDevMode() {
        PlatformMode mode = router.route("mode: self-dev - Improve your logging", orchestrator);
        assertEquals(PlatformType.SELF_DEV_MODE, mode.getType());
        assertTrue(mode.isAllowSelfModify());
        assertTrue(mode.getAllowedPaths().contains("eu.kalafatic.evolution.controller/src"));
    }

    @Test
    public void testModelMappingDarwin() {
        orchestrator.setDarwinMode(true);
        PlatformMode mode = router.route("Solve it", orchestrator);
        assertEquals(PlatformType.DARWIN_MODE, mode.getType());
    }

    @Test
    public void testModelMappingSelfIterative() {
        if (orchestrator.getAiChat() == null) {
            orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        }
        if (orchestrator.getAiChat().getPromptInstructions() == null) {
            orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());
        }
        orchestrator.getAiChat().getPromptInstructions().setSelfIterativeMode(true);
        PlatformMode mode = router.route("Iterate", orchestrator);
        assertEquals(PlatformType.SELF_DEV_MODE, mode.getType());
    }

    @Test
    public void testDefaultToChat() {
        PlatformMode mode = router.route("Hello world", orchestrator);
        assertEquals(PlatformType.SIMPLE_CHAT, mode.getType());
    }
}
