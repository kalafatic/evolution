package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;

public class JsonRobustnessTest {

    @Test
    public void testMultipleConcatenatedObjects() {
        // Simulation of gemma3:1b noisy output from the issue
        String noisyResponse = "```json\n" +
                "{\n" +
                "  \"category\": \"CODING\",\n" +
                "  \"objective\": \"create java class\",\n" +
                "  \"isAmbiguous\": false,\n" +
                "  \"missingInformation\": [],\n" +
                "  \"clarificationQuestion\": \"\",\n" +
                "  \"refinedPrompt\": \"Create a Java class.\"\n" +
                "}\n" +
                "```\n" +
                "DIAGNOSIS:\n" +
                "{\n" +
                "  \"rootCause\": \"missing return statement\",\n" +
                "  \"repeatFailure\": true,\n" +
                "  \"progress\": \"SAME\",\n" +
                "  \"suggestedStrategy\": \"RETRY\",\n" +
                "  \"explanation\": \"...\"\n" +
                "}";

        // Standard extractJsonObject should pick the first one
        JSONObject first = JsonUtils.extractJsonObject(noisyResponse);
        assertNotNull(first);
        assertEquals("CODING", first.getString("category"));

        // Flexible extraction should find both
        JSONArray all = JsonUtils.extractJsonArrayFlexible(noisyResponse);
        assertNotNull(all);
        assertEquals(2, all.length());
        assertEquals("CODING", all.getJSONObject(0).getString("category"));
        assertEquals("missing return statement", all.getJSONObject(1).getString("rootCause"));
    }

    @Test
    public void testNestedBracesExtraction() {
        String noisyResponse = "Result: {\"status\":\"OK\", \"data\": {\"key\":\"value\"}} Extra stuff";
        JSONObject obj = JsonUtils.extractJsonObject(noisyResponse);
        assertNotNull(obj);
        assertEquals("OK", obj.getString("status"));
        assertEquals("value", obj.getJSONObject("data").getString("key"));
    }

    @Test
    public void testBrokenConcatenatedObjects() {
        // Case where simple extraction might fail but flexible succeeds
        String text = "Object 1: {\"a\":1} Object 2: {\"b\":2}";
        JSONObject first = JsonUtils.extractJsonObject(text);
        assertNotNull(first);
        assertTrue(first.has("a"));

        JSONArray all = JsonUtils.extractJsonArrayFlexible(text);
        assertEquals(2, all.length());
    }
}
