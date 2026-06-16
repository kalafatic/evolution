package eu.kalafatic.evolution.creatic.engine.test;

import eu.kalafatic.evolution.creatic.engine.GuidanceEngine;
import eu.kalafatic.evolution.creatic.model.ContextGraph;
import eu.kalafatic.evolution.creatic.model.GuidanceResponse;
import org.junit.Test;
import static org.junit.Assert.*;

public class GuidanceEngineTest {

    @Test
    public void testForgeGuidance() {
        GuidanceEngine engine = new GuidanceEngine();
        ContextGraph context = new ContextGraph("forge");
        context.put("model.exists", true);
        context.put("model.trained", false);

        GuidanceResponse response = engine.evaluate(context);
        assertEquals("Forge Model Environment", response.getSummary());
        assertTrue(response.getActions().stream().anyMatch(a -> "TRAIN_MODEL".equals(a.getActionId())));
    }

    @Test
    public void testChatGuidance() {
        GuidanceEngine engine = new GuidanceEngine();
        ContextGraph context = new ContextGraph("chat");
        context.put("darwin.active", true);

        GuidanceResponse response = engine.evaluate(context);
        assertEquals("AI Chat Intelligence", response.getSummary());
        assertFalse(response.getTips().isEmpty());
    }
}
