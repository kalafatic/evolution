package eu.kalafatic.evolution.controller.tests;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

import static org.junit.Assert.*;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.parsers.structured.StructuredResponsePipeline;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import java.util.HashMap;
import java.util.Map;

public class StructuredResponsePipelineTest {

    private StructuredResponsePipeline pipeline;
    private TaskContext context;
    private Map<String, Class<?>> schema;

    @Before
    public void setUp() {
        pipeline = new StructuredResponsePipeline();
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setAiMode(AiMode.LOCAL);
        context = new TaskContext(orchestrator, null);
        schema = new HashMap<>();
        schema.put("state", String.class);
        schema.put("confidence", JSONObject.class);
    }

    @Test
    public void testSuccessfulParsing() {
        String raw = "```json\n{\"state\": \"CLEAR\", \"confidence\": {\"overallConfidence\": 0.9}}\n```";
        JSONObject result = pipeline.process(raw, schema, context);
        assertNotNull(result);
        assertEquals("CLEAR", result.getString("state"));
        assertEquals(0.9, result.getJSONObject("confidence").getDouble("overallConfidence"), 0.001);
    }

    @Test
    public void testRepairMalformedBrackets() {
        String raw = "{\"state\": \"CLEAR\", \"confidence\": {\"overallConfidence\": 0.9";
        JSONObject result = pipeline.process(raw, schema, context);
        assertNotNull(result);
        assertEquals("CLEAR", result.getString("state"));
    }

    @Test
    public void testSchemaValidationFailure() {
        String raw = "{\"state\": \"CLEAR\"}"; // Missing confidence
        JSONObject result = pipeline.process(raw, schema, context);
        assertNotNull(result);
        // Should return fallback
        assertEquals("Fallback after parsing failure", result.getString("dominantIntent"));
    }

    @Test
    public void testSemanticValidationFailure_Placeholder() {
        String raw = "{\"state\": \"CLEAR|NEEDS_CLARIFICATION\", \"confidence\": {\"overallConfidence\": 0.9}}";
        JSONObject result = pipeline.process(raw, schema, context);
        assertNotNull(result);
        assertEquals("Fallback after parsing failure", result.getString("dominantIntent"));
    }

    @Test
    public void testRepairTrailingComma() {
        String raw = "{\"state\": \"CLEAR\", \"confidence\": {\"overallConfidence\": 0.9},}";
        JSONObject result = pipeline.process(raw, schema, context);
        assertNotNull(result);
        assertEquals("CLEAR", result.getString("state"));
    }

    @Test
    public void testNoisyResponse() {
        String raw = "I have analyzed the request.\n<think>Some thoughts</think>\n{\"state\": \"CLEAR\", \"confidence\": {\"overallConfidence\": 0.8}}";
        JSONObject result = pipeline.process(raw, schema, context);
        assertNotNull(result);
        assertEquals("CLEAR", result.getString("state"));
    }
}
