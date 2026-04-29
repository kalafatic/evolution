package eu.kalafatic.evolution.controller.tests;

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
}
