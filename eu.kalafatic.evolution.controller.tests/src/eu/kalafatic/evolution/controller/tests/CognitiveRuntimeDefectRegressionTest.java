package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.kalafatic.evolution.controller.cognitive.ProjectKnowledgeService;
import eu.kalafatic.evolution.controller.orchestration.mcp.CognitiveMcpToolProvider;

/**
 * Regression tests for defects discovered during live runtime verification:
 * 1. searchDecisions used an ambiguous 'id' column (join with project_knowledge).
 * 2. Tool results containing org.json types failed Jackson serialization.
 * 3. Resource URIs with query parameters failed provider lookup.
 */
public class CognitiveRuntimeDefectRegressionTest {

    @Test
    public void searchDecisionsFindsRecordsWithAmbiguityFix() {
        String project = "/tmp/regression-project-" + System.nanoTime();
        JSONObject recorded = ProjectKnowledgeService.getInstance().recordDecision(project, null,
                "Regression decision", null, "Use the MCP server on 28280", null, "ACCEPTED");
        assertNotNull(recorded.optString("decisionId"));

        List<JSONObject> found = ProjectKnowledgeService.getInstance()
                .searchDecisions(project, "28280", null, 10);
        assertTrue("searchDecisions must find the recorded decision",
                found.stream().anyMatch(d -> "Regression decision".equals(d.optString("title"))));
    }

    @Test
    public void searchDecisionsWorksWhenContextColumnIsNull() {
        String project = "/tmp/regression-project-null-" + System.nanoTime();
        // recordDecision with context=null writes a NULL context column
        ProjectKnowledgeService.getInstance().recordDecision(project, null,
                "Null context decision", null, "The MCP port is 28280", null, "ACCEPTED");
        List<JSONObject> found = ProjectKnowledgeService.getInstance()
                .searchDecisions(project, "28280", null, 10);
        assertTrue("NULL context column must not break the search (COALESCE fix)",
                found.stream().anyMatch(d -> "Null context decision".equals(d.optString("title"))));
    }

    @Test
    public void toolResultsWithOrgJsonAreJacksonSerializable() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JSONObject nested = new JSONObject();
        nested.put("name", "test");
        JSONArray array = new JSONArray();
        array.put(nested);
        array.put("plain");
        JSONObject root = new JSONObject();
        root.put("items", array);
        root.put("flag", true);
        root.put("count", 3);
        root.put("nothing", JSONObject.NULL);

        Object converted = CognitiveMcpToolProvider.toSerializable(root);
        String json = mapper.writeValueAsString(converted);
        assertTrue("Converted structure must be serializable JSON", json.contains("\"name\":\"test\""));
        assertTrue(json.contains("\"count\":3"));

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("results", java.util.List.of(new JSONObject().put("id", "a1")));
        Object serializable = CognitiveMcpToolProvider.toSerializable(result);
        mapper.writeValueAsString(serializable); // must not throw
    }

    @Test
    public void resourceLookupWithQueryParamsFallsBackToBaseUri() throws Exception {
        eu.kalafatic.evolution.servers.mcp.resources.ResourceRegistry registry =
                new eu.kalafatic.evolution.servers.mcp.resources.ResourceRegistry();
        registry.register(new eu.kalafatic.evolution.servers.mcp.model.Resource(
                "evo://regression/test", "Test", "Test resource", "application/json"),
                uri -> "{\"uri\":\"" + uri + "\"}");
        ObjectMapper mapper = new ObjectMapper();
        Object result = registry.readResource(mapper.readTree(
                "{\"uri\":\"evo://regression/test?param=value\"}"));
        assertNotNull(result);
        assertTrue("Query-param URI must resolve to the base resource",
                result.toString().contains("param=value"));
    }
}
