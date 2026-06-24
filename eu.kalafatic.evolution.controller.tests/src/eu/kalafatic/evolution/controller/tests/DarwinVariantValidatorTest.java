package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.json.JSONArray;
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
                "  \"survival_argument\": \"Most practical path and long enough.\",\n" +
                "  \"semantic_anchor\": \"Minimalist philosophy and long enough.\",\n" +
                "  \"tradeoffs\": \"Lacks extensibility\",\n" +
                "  \"failure_risks\": \"Monolithic\",\n" +
                "  \"projected_steps\": [\"Step 1\"],\n" +
                "  \"expected_outputs\": [\"Output 1\"],\n" +
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
    public void testValidateMissingMandatoryFieldFails() {
        DarwinVariantValidator validator = new DarwinVariantValidator();
        // Missing projected_steps
        String rawResponse = "{\n" +
                "  \"strategy_type\": \"PROBABLE_SURVIVOR\",\n" +
                "  \"strategy\": \"Implement a simple direct execution path.\",\n" +
                "  \"survival_argument\": \"Most practical path and long enough.\",\n" +
                "  \"semantic_anchor\": \"Minimalist philosophy and long enough.\",\n" +
                "  \"tradeoffs\": \"Lacks extensibility\",\n" +
                "  \"failure_risks\": \"Monolithic\",\n" +
                "  \"expected_outputs\": [\"Output 1\"],\n" +
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
        assertNull("Validation should fail when mandatory field 'projected_steps' is missing", result);
    }

    @Test
    public void testValidateGenericTargetFails() {
        DarwinVariantValidator validator = new DarwinVariantValidator();
        String rawResponse = "{\n" +
                "  \"strategy_type\": \"PROBABLE_SURVIVOR\",\n" +
                "  \"strategy\": \"Implement a simple direct execution path.\",\n" +
                "  \"survival_argument\": \"Most practical path and long enough.\",\n" +
                "  \"semantic_anchor\": \"Minimalist philosophy and long enough.\",\n" +
                "  \"tradeoffs\": \"Lacks extensibility\",\n" +
                "  \"failure_risks\": \"Monolithic\",\n" +
                "  \"projected_steps\": [\"Step 1\"],\n" +
                "  \"expected_outputs\": [\"Output 1\"],\n" +
                "  \"actions\": [\n" +
                "    {\n" +
                "      \"domain\": \"file\",\n" +
                "      \"operation\": \"WRITE\",\n" +
                "      \"target\": \".\",\n" +
                "      \"description\": \"Write main class\",\n" +
                "      \"implementation\": \"public class Main {}\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        JSONObject result = validator.validate(rawResponse, DarwinStrategyType.PROBABLE_SURVIVOR, null);
        assertNull("Validation should fail when 'target' is '.' for WRITE action", result);
    }

    @Test
    public void testValidateMissingImplementationFails() {
        DarwinVariantValidator validator = new DarwinVariantValidator();
        String rawResponse = "{\n" +
                "  \"strategy_type\": \"PROBABLE_SURVIVOR\",\n" +
                "  \"strategy\": \"Implement a simple direct execution path.\",\n" +
                "  \"survival_argument\": \"Most practical path and long enough.\",\n" +
                "  \"semantic_anchor\": \"Minimalist philosophy and long enough.\",\n" +
                "  \"tradeoffs\": \"Lacks extensibility\",\n" +
                "  \"failure_risks\": \"Monolithic\",\n" +
                "  \"projected_steps\": [\"Step 1\"],\n" +
                "  \"expected_outputs\": [\"Output 1\"],\n" +
                "  \"actions\": [\n" +
                "    {\n" +
                "      \"domain\": \"file\",\n" +
                "      \"operation\": \"WRITE\",\n" +
                "      \"target\": \"src/Main.java\",\n" +
                "      \"description\": \"Write main class\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        JSONObject result = validator.validate(rawResponse, DarwinStrategyType.PROBABLE_SURVIVOR, null);
        assertNull("Validation should fail when 'implementation' is missing for WRITE action", result);
    }
}
