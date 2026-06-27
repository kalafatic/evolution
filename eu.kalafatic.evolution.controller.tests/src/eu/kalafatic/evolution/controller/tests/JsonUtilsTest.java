package eu.kalafatic.evolution.controller.tests;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

import static org.junit.Assert.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;

public class JsonUtilsTest {

    @Test
    public void testStripThinkBlocks() {
        String text = "<think>some internal thought</think>[\n" +
                "  {\"id\": \"t1\", \"strategy\": \"S1\", \"suffix\": \"s1\"}\n" +
                "]";
        JSONArray result = JsonUtils.extractJsonArrayFlexible(text);
        assertNotNull(result);
        assertEquals(1, result.length());
        assertEquals("S1", result.getJSONObject(0).getString("strategy"));
    }

    @Test
    public void testConvertObjectToArray() {
        String text = "{\n" +
                "  \"1\": {\"id\": \"t1\", \"strategy\": \"S1\", \"suffix\": \"s1\"},\n" +
                "  \"2\": {\"id\": \"t2\", \"strategy\": \"S2\", \"suffix\": \"s2\"}\n" +
                "}";
        JSONArray result = JsonUtils.extractJsonArrayFlexible(text);
        assertNotNull(result);
        assertEquals(2, result.length());
        assertEquals("S1", result.getJSONObject(0).getString("strategy"));
        assertEquals("S2", result.getJSONObject(1).getString("strategy"));
    }

    @Test
    public void testExtractNestedArray() {
        String text = "Here is the plan:\n" +
                "{\n" +
                "  \"tasks\": [\n" +
                "    {\"id\": \"t1\", \"name\": \"N1\"}\n" +
                "  ]\n" +
                "}";
        JSONArray result = JsonUtils.extractJsonArrayFlexible(text);
        assertNotNull(result);
        assertEquals(1, result.length());
        assertEquals("N1", result.getJSONObject(0).getString("name"));
    }

    @Test
    public void testMultipleObjectsGreedy() {
        String text = "Thought 1 { \"a\": 1 } Thought 2 { \"b\": 2 }";
        JSONObject result = JsonUtils.extractJsonObject(text);
        assertNotNull(result);
        assertTrue(result.has("a"));
    }

    @Test
    public void testRepairTruncatedJson() {
        String truncated = "{\n" +
                "  \"state\": \"CLEAR\",\n" +
                "  \"dimensions\": [\n" +
                "    {\n" +
                "      \"id\": \"1\",\n" +
                "      \"value\": \"v1\"";
        JSONObject result = JsonUtils.extractJsonObject(truncated);
        assertNotNull(result);
        assertEquals("CLEAR", result.getString("state"));
        JSONArray dims = result.getJSONArray("dimensions");
        assertEquals(1, dims.length());
        assertEquals("v1", dims.getJSONObject(0).getString("value"));
    }

    @Test
    public void testRepairTruncatedString() {
        String truncated = "{\"key\": \"truncated value";
        JSONObject result = JsonUtils.extractJsonObject(truncated);
        assertNotNull(result);
        assertEquals("truncated value", result.getString("key"));
    }

    @Test
    public void testDarwinTagExtraction() {
        String text = "Some log\n<BEGIN_DARWIN_JSON>\n{\"id\": \"v1\", \"strategy\": \"S1\"}\n<END_DARWIN_JSON>\nMore logs";
        JSONObject result = JsonUtils.extractJsonObject(text);
        assertNotNull(result);
        assertEquals("v1", result.getString("id"));
        assertEquals("S1", result.getString("strategy"));
    }

    @Test
    public void testDarwinTagExtractionWithContamination() {
        String text = "[Default] Some log\n<BEGIN_DARWIN_JSON>\n{\"id\": \"v2\", \"strategy\": \"S2\"}\n<END_DARWIN_JSON>\n[Default] More logs";
        JSONObject result = JsonUtils.extractJsonObject(text);
        assertNotNull(result);
        assertEquals("v2", result.getString("id"));
        assertEquals("S2", result.getString("strategy"));
    }
}
