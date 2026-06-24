package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.json.JSONObject;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinStrategyType;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinVariantValidator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ImplementationPlanner;

public class ImplementationPlannerTest {

    @Test
    public void testPlanMissingActionsFails() {
        ImplementationPlanner planner = new ImplementationPlanner();
        JSONObject variant = new JSONObject();
        variant.put("id", "test_variant");
        variant.put("strategy", "A very complex architectural strategy for testing.");
        variant.put("semantic_anchor", "Testing philosophy");
        variant.put("strategy_type", "PHILOSOPHY_MUTATION");

        JSONObject planned = planner.plan(variant, null);

        assertNull("Planning should fail when actions are missing (PROHIBIT SYNTHESIS)", planned);
    }

    @Test
    public void testPlanWithActionsPasses() {
        ImplementationPlanner planner = new ImplementationPlanner();
        JSONObject variant = new JSONObject();
        variant.put("id", "test_variant");
        variant.put("strategy", "A very complex architectural strategy for testing.");
        variant.put("semantic_anchor", "Testing philosophy");
        variant.put("strategy_type", "PHILOSOPHY_MUTATION");

        // Stabilize metadata for test since healing is removed
        variant.put("tradeoffs", "None");
        variant.put("failure_risks", "None");

        org.json.JSONArray actions = new org.json.JSONArray();
        JSONObject action = new JSONObject();
        action.put("domain", "file");
        action.put("operation", "WRITE");
        action.put("target", "src/Test.java");
        action.put("implementation", "public class Test {}");
        actions.put(action);
        variant.put("actions", actions);

        JSONObject planned = planner.plan(variant, null);

        assertNotNull(planned);
        assertTrue(planned.has("actions"));
        assertEquals(1, planned.getJSONArray("actions").length());
        assertTrue(planned.has("tradeoffs"));
        assertTrue(planned.has("failure_risks"));
    }

    @Test
    public void testValidatorRejectsMissingActions() {
        DarwinVariantValidator validator = new DarwinVariantValidator();
        String rawResponse = "{\n" +
                "  \"strategy\": \"Valid architectural strategy that is long enough.\",\n" +
                "  \"semantic_anchor\": \"Valid philosophy that is also long enough.\"\n" +
                "}";

        JSONObject result = validator.validate(rawResponse, DarwinStrategyType.PROBABLE_SURVIVOR, null);
        assertNull("Validation should fail when 'actions' are missing", result);
    }
}
