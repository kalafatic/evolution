package eu.kalafatic.evolution.controller.tests;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import static org.junit.Assert.*;
import org.json.JSONObject;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinStrategyType;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinVariantValidator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ImplementationPlanner;

public class ImplementationPlannerTest {

    @Test
    public void testPlanMissingActions() {
        ImplementationPlanner planner = new ImplementationPlanner();
        JSONObject variant = new JSONObject();
        variant.put("id", "test_variant");
        variant.put("strategy", "A very complex architectural strategy for testing.");
        variant.put("semantic_anchor", "Testing philosophy");
        variant.put("strategy_type", "PHILOSOPHY_MUTATION");

        JSONObject planned = planner.plan(variant, null);

        assertNotNull(planned);
        assertTrue(planned.has("actions"));
        assertTrue(planned.getJSONArray("actions").length() > 0);
        assertTrue(planned.has("tradeoffs"));
        assertTrue(planned.has("failure_risks"));
        assertTrue(planned.has("projected_steps"));

        JSONObject action = planned.getJSONArray("actions").getJSONObject(0);
        assertEquals("kernel", action.getString("domain"));
        assertEquals("ANALYZE", action.getString("operation"));
    }

    @Test
    public void testPlanFromProjectedSteps() {
        ImplementationPlanner planner = new ImplementationPlanner();
        JSONObject variant = new JSONObject();
        variant.put("id", "step_variant");
        variant.put("strategy", "Step-based strategy");
        variant.put("semantic_anchor", "Step philosophy");

        org.json.JSONArray steps = new org.json.JSONArray();
        steps.put("Initialize something");
        steps.put("Write a new class for the logic");
        steps.put("Delete obsolete code");
        variant.put("projected_steps", steps);

        JSONObject planned = planner.plan(variant, null);

        assertNotNull(planned);
        org.json.JSONArray actions = planned.getJSONArray("actions");
        assertEquals(2, actions.length());

        assertEquals("file", actions.getJSONObject(0).getString("domain"));
        assertEquals("WRITE", actions.getJSONObject(0).getString("operation"));

        assertEquals("file", actions.getJSONObject(1).getString("domain"));
        assertEquals("DELETE", actions.getJSONObject(1).getString("operation"));
    }

    @Test
    public void testValidatorAllowsMissingActions() {
        DarwinVariantValidator validator = new DarwinVariantValidator();
        String rawResponse = "{\n" +
                "  \"strategy\": \"Valid architectural strategy that is long enough.\",\n" +
                "  \"semantic_anchor\": \"Valid philosophy that is also long enough.\"\n" +
                "}";

        JSONObject result = validator.validate(rawResponse, DarwinStrategyType.PROBABLE_SURVIVOR, null);
        assertNotNull("Validation should pass even when 'actions' are missing", result);
        assertFalse(result.has("actions"));
    }
}
