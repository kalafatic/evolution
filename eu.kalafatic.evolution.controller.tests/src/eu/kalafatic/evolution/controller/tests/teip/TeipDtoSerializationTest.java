package eu.kalafatic.evolution.controller.tests.teip;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Test;

import eu.kalafatic.evolution.controller.teip.dto.*;

/**
 * Unit test verifying TEIP 1.1 DTO bidirectional serialization and schema validation.
 */
public class TeipDtoSerializationTest {

    @Test
    public void testHealthResponseSerialization() {
        TeipHealthResponse health = new TeipHealthResponse();
        health.setStatus("UP");
        health.getDetails().put("serverPort", 28080);

        String json = TeipJsonConverter.toJson(health);
        assertNotNull(json);

        JSONObject obj = new JSONObject(json);
        assertEquals("TEIP", obj.getString("protocol"));
        assertEquals("1.1", obj.getString("version"));
        assertEquals("UP", obj.getString("status"));
        assertTrue(obj.getBoolean("ready"));
        assertEquals(28080, obj.getJSONObject("details").getInt("serverPort"));
    }

    @Test
    public void testCapabilityDescriptorSerialization() {
        TeipCapabilityDescriptor desc = new TeipCapabilityDescriptor();
        desc.getSupportedModes().add("DARWIN_MODE");
        desc.getSupportedModes().add("SELF_DEV_MODE");
        desc.getAgents().add(Map.of("name", "JavaDevAgent"));
        desc.getModels().add(Map.of("id", "llama3.2:3b"));
        desc.getAvailableModels().add(Map.of("id", "llama3.2:3b"));
        desc.getCapabilities().add(Map.of("id", "CODE_IMPLEMENTATION"));

        String json = TeipJsonConverter.toJson(desc);
        JSONObject obj = new JSONObject(json);

        assertEquals("TEIP", obj.getString("protocol"));
        assertEquals("1.1", obj.getString("version"));
        assertEquals(2, obj.getJSONArray("supportedModes").length());
        assertEquals("DARWIN_MODE", obj.getJSONArray("supportedModes").getString(0));
        assertTrue(obj.getJSONObject("features").getBoolean("asyncExecution"));
        // TEIP 1.1 §4.2 canonical arrays
        assertEquals(1, obj.getJSONArray("availableModels").length());
        assertEquals(1, obj.getJSONArray("capabilities").length());
    }

    @Test
    public void testExecutionRequestParsingAndValidation() {
        String json = """
        {
          "protocol": "TEIP",
          "version": "1.1",
          "requestId": "req-123",
          "correlationId": "corr-456",
          "idempotencyKey": "idem-789",
          "caller": {"system": "TRYST", "taskId": "task-01"},
          "capability": "CODE_IMPLEMENTATION",
          "mode": "DARWIN_MODE",
          "model": "llama3.2:3b",
          "instruction": "Fix null pointer in Filter.java",
          "grantedToolScopes": ["read:workspace", "execute:build"]
        }
        """;

        TeipExecutionRequest req = TeipJsonConverter.parseExecutionRequest(json);
        assertEquals("TEIP", req.getProtocol());
        assertEquals("1.1", req.getVersion());
        assertEquals("req-123", req.getRequestId());
        assertEquals("corr-456", req.getCorrelationId());
        assertEquals("idem-789", req.getIdempotencyKey());
        assertEquals("Fix null pointer in Filter.java", req.getInstruction());
        assertEquals(2, req.getGrantedToolScopes().size());
        assertTrue(req.getGrantedToolScopes().contains("read:workspace"));
        assertEquals("TRYST", req.getCaller().get("system"));
        assertEquals("llama3.2:3b", req.getModel());
    }

    @Test
    public void testDeprecatedAliasesAccepted() {
        String json = """
        {
          "protocol": "TEIP",
          "version": "1.0",
          "aiMode": "DARWIN_MODE",
          "mcpToolScopes": ["read:workspace"],
          "caller": {"app": "TRYST", "taskId": "task-01"},
          "instruction": "Alias test",
          "contextReferences": ["src/Auth.java", {"type": "FILE", "path": "src/User.java", "description": "Target"}]
        }
        """;

        TeipExecutionRequest req = TeipJsonConverter.parseExecutionRequest(json);
        assertEquals("DARWIN_MODE", req.getMode());
        assertEquals(1, req.getGrantedToolScopes().size());
        assertEquals("read:workspace", req.getGrantedToolScopes().get(0));
        // caller.app normalized to caller.system
        assertEquals("TRYST", req.getCaller().get("system"));
        // bare string contextReferences promoted to {type: FILE, path: ...} shorthand
        assertEquals(2, req.getContextReferences().size());
        assertEquals("FILE", req.getContextReferences().get(0).get("type"));
        assertEquals("src/Auth.java", req.getContextReferences().get(0).get("path"));
        assertEquals("Target", req.getContextReferences().get(1).get("description"));
    }

    @Test
    public void testVersionMismatchRejected() {
        try {
            TeipJsonConverter.parseExecutionRequest("{\"protocol\":\"TEIP\",\"version\":\"2.0\",\"instruction\":\"x\"}");
            fail("Expected TeipVersionMismatchException for unsupported major version");
        } catch (TeipVersionMismatchException expected) {
            assertTrue(expected.getMessage().contains("2.0"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExecutionRequestEmptyValidation() {
        TeipJsonConverter.parseExecutionRequest("");
    }

    @Test
    public void testAcceptedResponseSerialization() {
        TeipAcceptedResponse resp = new TeipAcceptedResponse("exec-test-1", "corr-test-1");
        String json = TeipJsonConverter.toJson(resp);

        JSONObject obj = new JSONObject(json);
        assertEquals("exec-test-1", obj.getString("executionId"));
        assertEquals("QUEUED", obj.getString("status"));
        assertEquals("/teip/v1/executions/exec-test-1", obj.getJSONObject("links").getString("status"));
        assertEquals("/teip/v1/executions/exec-test-1/events", obj.getJSONObject("links").getString("events"));
        assertEquals("1.1", obj.getString("version"));
    }

    @Test
    public void testErrorSerialization() {
        TeipError err = new TeipError("NOT_FOUND", "Execution missing", "req-99", "corr-99", false);
        String json = TeipJsonConverter.toJson(err);

        JSONObject obj = new JSONObject(json);
        assertEquals("NOT_FOUND", obj.getString("code"));
        assertEquals("Execution missing", obj.getString("message"));
        assertFalse(obj.getBoolean("retryable"));
    }

    @Test
    public void testResultSerializationContainsPatchAndModifiedFiles() {
        TeipExecutionResult result = new TeipExecutionResult("exec-1", "corr-1", TeipExecutionStatus.COMPLETED, "Done");
        result.setPatch("diff --git a/A.java b/A.java");
        result.setModifiedFiles(List.of("src/A.java"));

        String json = TeipJsonConverter.toJson(result);
        JSONObject obj = new JSONObject(json);
        assertEquals("diff --git a/A.java b/A.java", obj.getString("patch"));
        assertEquals(1, obj.getJSONArray("modifiedFiles").length());
        assertEquals("src/A.java", obj.getJSONArray("modifiedFiles").getString(0));
    }

    @Test
    public void testEventListSerialization() {
        TeipEvent evt1 = new TeipEvent("evt-1", "exec-1", 1, "EXECUTION_STARTED", "corr-1", Map.of("mode", "DARWIN_MODE"));
        TeipEvent evt2 = new TeipEvent("evt-2", "exec-1", 2, "PROGRESS_UPDATED", "corr-1", Map.of("progress", 0.5));
        // TEIP 1.1 §4.5: nextSequence = last returned event's sequence + 1
        TeipEventsResponse resp = new TeipEventsResponse("exec-1", List.of(evt1, evt2), false, 3);

        String json = TeipJsonConverter.toJson(resp);
        JSONObject obj = new JSONObject(json);
        assertEquals("exec-1", obj.getString("executionId"));
        assertEquals(2, obj.getJSONArray("events").length());
        assertFalse(obj.getBoolean("hasMore"));
        assertEquals(3, obj.getLong("nextSequence"));
    }
}
