package eu.kalafatic.evolution.controller.tests;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import static org.junit.Assert.*;
import org.json.JSONObject;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinStrategyType;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinVariantValidator;

public class DarwinVariantValidatorTest {

    @Test
    public void testValidateMissingIdPasses() {
        DarwinVariantValidator validator = new DarwinVariantValidator();
        String rawResponse = "{\n" +
                "  \"strategy_type\": \"PROBABLE_SURVIVOR\",\n" +
                "  \"strategy\": \"Implement a simple direct execution path.\",\n" +
                "  \"reasoning_focus\": \"Direct execution\",\n" +
                "  \"survival_argument\": \"Most practical path\",\n" +
                "  \"semantic_justification\": \"Minimalist philosophy\",\n" +
                "  \"tradeoffs\": \"Lacks extensibility\",\n" +
                "  \"failure_risks\": \"Monolithic\",\n" +
                "  \"actions\": [\n" +
                "    {\n" +
                "      \"domain\": \"file\",\n" +
                "      \"operation\": \"WRITE\",\n" +
                "      \"target\": \"src/Main.java\",\n" +
                "      \"description\": \"Write main class\",\n" +
                "      \"implementation\": \"public class Main {}\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        JSONObject result = validator.validate(rawResponse, DarwinStrategyType.PROBABLE_SURVIVOR, null);
        assertNotNull("Validation should pass even when 'id' is missing", result);
        assertFalse("Result should not have 'id' yet", result.has("id"));
    }

    @Test
    public void testValidateWithIdPasses() {
        DarwinVariantValidator validator = new DarwinVariantValidator();
        String rawResponse = "{\n" +
                "  \"id\": \"direct_minimal\",\n" +
                "  \"strategy_type\": \"PROBABLE_SURVIVOR\",\n" +
                "  \"strategy\": \"Implement a simple direct execution path.\",\n" +
                "  \"reasoning_focus\": \"Direct execution\",\n" +
                "  \"survival_argument\": \"Most practical path\",\n" +
                "  \"semantic_justification\": \"Minimalist philosophy\",\n" +
                "  \"tradeoffs\": \"Lacks extensibility\",\n" +
                "  \"failure_risks\": \"Monolithic\",\n" +
                "  \"actions\": [\n" +
                "    {\n" +
                "      \"domain\": \"file\",\n" +
                "      \"operation\": \"WRITE\",\n" +
                "      \"target\": \"src/Main.java\",\n" +
                "      \"description\": \"Write main class\",\n" +
                "      \"implementation\": \"public class Main {}\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        JSONObject result = validator.validate(rawResponse, DarwinStrategyType.PROBABLE_SURVIVOR, null);
        assertNotNull("Validation should pass when 'id' is present", result);
        assertEquals("direct_minimal", result.getString("id"));
    }

    @Test
    public void testValidateWithSemanticAnchorPasses() {
        DarwinVariantValidator validator = new DarwinVariantValidator();
        String rawResponse = "{\n" +
                "  \"id\": \"anchor_test\",\n" +
                "  \"strategy_type\": \"PROBABLE_SURVIVOR\",\n" +
                "  \"strategy\": \"Implement a simple direct execution path.\",\n" +
                "  \"reasoning_focus\": \"Direct execution\",\n" +
                "  \"survival_argument\": \"Most practical path\",\n" +
                "  \"semantic_anchor\": \"Divergent philosophy anchor\",\n" +
                "  \"tradeoffs\": \"Lacks extensibility\",\n" +
                "  \"failure_risks\": \"Monolithic\",\n" +
                "  \"actions\": [\n" +
                "    {\n" +
                "      \"domain\": \"file\",\n" +
                "      \"operation\": \"WRITE\",\n" +
                "      \"target\": \"src/Main.java\",\n" +
                "      \"description\": \"Write main class\",\n" +
                "      \"implementation\": \"public class Main {}\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        JSONObject result = validator.validate(rawResponse, DarwinStrategyType.PROBABLE_SURVIVOR, null);
        assertNotNull("Validation should pass when 'semantic_anchor' is used instead of 'semantic_justification'", result);
        assertEquals("Divergent philosophy anchor", result.getString("semantic_anchor"));
    }
}
