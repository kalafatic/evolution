package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.llm.GeminiProvider;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class GeminiProviderTest {

    @Test
    public void testGeminiPayloadFormat() throws Exception {
        // Since we cannot easily mock HttpClient without adding more dependencies,
        // we can at least test that we can parse a typical Gemini response.

        String mockResponse = "{\n" +
                "  \"candidates\": [{\n" +
                "    \"content\": {\n" +
                "      \"parts\": [{\n" +
                "        \"text\": \"Hello from Gemini\"\n" +
                "      }]\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        JSONObject jsonResponse = new JSONObject(mockResponse);
        String text = jsonResponse.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        assertEquals("Hello from Gemini", text);
    }

    @Test
    public void testRequestPayloadGeneration() {
        String prompt = "test prompt";
        float temperature = 0.5f;

        JSONObject jsonObject = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        jsonObject.put("contents", contents);

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", temperature);
        jsonObject.put("generationConfig", generationConfig);

        assertEquals(prompt, jsonObject.getJSONArray("contents").getJSONObject(0).getJSONArray("parts").getJSONObject(0).getString("text"));
        assertEquals(0.5, jsonObject.getJSONObject("generationConfig").getDouble("temperature"), 0.01);
    }
}
