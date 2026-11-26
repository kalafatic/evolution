package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.view.ecosystem.EcosystemEdge;
import eu.kalafatic.evolution.view.ecosystem.EcosystemNode;
import eu.kalafatic.evolution.view.ecosystem.EcosystemSnapshot;
import eu.kalafatic.evolution.view.ecosystem.EcosystemTopologyBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the ecosystem topology builder (pure mapping from UPM JSON to the
 * node/edge model). Fixture data mirrors the UPM HTTP payloads.
 */
public class EcosystemTopologyBuilderTest {

    private static JSONObject fixtureState() {
        JSONObject s = new JSONObject();
        s.put("app", new JSONObject().put("version", "2.6.11").put("port", 29080));
        JSONArray projects = new JSONArray();
        projects.put(new JSONObject()
                .put("id", "EVO").put("name", "EVO – Evolution Platform").put("type", "eclipse-tycho")
                .put("runtimeState", "RUNNING").put("pid", 1234).put("boundPort", 28080)
                .put("startupAfter", new JSONArray()));
        projects.put(new JSONObject()
                .put("id", "ENR").put("name", "ENR – EVO Native Runtime").put("type", "maven")
                .put("runtimeState", "STOPPED").put("apiBase", "http://127.0.0.1:8111/api/v1")
                .put("enrMonitor", true).put("startupAfter", new JSONArray()));
        projects.put(new JSONObject()
                .put("id", "TRYST").put("name", "TRYST – Async Coding Platform").put("type", "eclipse-tycho")
                .put("runtimeState", "FAILED").put("lastError", "port conflict")
                .put("startupAfter", new JSONArray()));
        s.put("projects", projects);
        JSONArray profiles = new JSONArray();
        profiles.put(new JSONObject().put("id", "development").put("name", "Development")
                .put("projects", new JSONArray().put("EVO").put("DCode").put("ENR"))
                .put("startupOrder", new JSONArray().put("ENR").put("EVO").put("DCode")));
        s.put("profiles", profiles);
        s.put("enr", new JSONObject().put("state", "RUNNING").put("pid", 99)
                .put("health", "healthy"));
        return s;
    }

    private static JSONObject fixturePorts() {
        JSONObject p = new JSONObject();
        JSONArray entries = new JSONArray();
        entries.put(new JSONObject().put("app", "EVO").put("port", 28080).put("service", "HTTP Server")
                .put("status", "RUNNING").put("protocol", "HTTP").put("kind", "implemented")
                .put("pid", 1234).put("process", "evolution").put("detail", "listening"));
        entries.put(new JSONObject().put("app", "EVO").put("port", 28280).put("service", "MCP Server")
                .put("status", "READY").put("protocol", "HTTP").put("kind", "implemented"));
        entries.put(new JSONObject().put("app", "ENR").put("port", 8111).put("service", "HTTP Server")
                .put("status", "READY").put("protocol", "HTTP").put("kind", "implemented"));
        entries.put(new JSONObject().put("app", "TRYST").put("port", 38080).put("service", "HTTP Server")
                .put("status", "OCCUPIED").put("protocol", "HTTP").put("kind", "implemented"));
        p.put("entries", entries);
        p.put("summary", new JSONObject().put("running", 1).put("ready", 2).put("occupied", 1));
        return p;
    }

    private static JSONObject fixtureMetrics() {
        JSONObject m = new JSONObject();
        m.put("cpu", new JSONObject().put("percent", 12.5).put("model", "AMD EPYC")
                .put("physicalCores", 32).put("cores", new JSONArray())
                .put("temperature", "N/A").put("load1", "1.2"));
        m.put("memory", new JSONObject().put("percent", 25.6).put("usedKb", 1L)
                .put("totalKb", 2L).put("availableKb", 1L).put("swapPercent", 0));
        JSONArray gpus = new JSONArray();
        gpus.put(new JSONObject().put("name", "RTX 5070 Ti").put("utilPercent", "5")
                .put("memUsedMb", "297").put("memTotalMb", "16303").put("vramPercent", 1.8)
                .put("temperature", "38").put("driver", "595.99"));
        m.put("gpu", gpus);
        return m;
    }

    private static JSONObject fixtureEnr() {
        JSONObject e = new JSONObject();
        JSONObject loaded = new JSONObject();
        JSONArray models = new JSONArray();
        models.put(new JSONObject().put("id", "gemma").put("name", "gemma-2-9b-it")
                .put("path", "/models/gemma.gguf").put("available", true));
        loaded.put("models", models);
        e.put("loaded", loaded);
        JSONArray files = new JSONArray();
        files.put(new JSONObject().put("name", "qwen2.5.gguf").put("path", "/models/qwen2.5.gguf"));
        e.put("files", files);
        JSONObject sessions = new JSONObject();
        sessions.put("sessions", new JSONArray()
                .put(new JSONObject().put("id", "s1").put("model", "gemma-2-9b-it").put("state", "active"))
                .put(new JSONObject().put("id", "s2").put("model", "gemma-2-9b-it").put("state", "active")));
        e.put("sessions", sessions);
        return e;
    }

    @Test
    public void testUpmRootIsAlwaysPresent() {
        EcosystemSnapshot s = EcosystemTopologyBuilder.build(null, null, null, null, null);
        assertEquals(1, s.size());
        EcosystemNode root = s.node(EcosystemTopologyBuilder.ROOT_ID);
        assertNotNull(root);
        assertEquals(EcosystemNode.NodeType.ROOT, root.getType());
        assertEquals(EcosystemNode.RuntimeStatus.RUNNING, root.getStatus());
    }

    @Test
    public void testProductsBecomeProductNodesUnderRoot() {
        EcosystemSnapshot s = EcosystemTopologyBuilder.build(
                fixtureState(), fixturePorts(), null, null, null);
        EcosystemNode evo = s.node("product:EVO");
        EcosystemNode enr = s.node("product:ENR");
        EcosystemNode tryst = s.node("product:TRYST");
        assertNotNull(evo);
        assertNotNull(enr);
        assertNotNull(tryst);
        assertEquals(EcosystemTopologyBuilder.ROOT_ID, evo.getParentId());
        assertEquals(EcosystemNode.RuntimeStatus.RUNNING, evo.getStatus());
        assertEquals(EcosystemNode.RuntimeStatus.STOPPED, enr.getStatus());
        assertEquals(EcosystemNode.RuntimeStatus.FAILED, tryst.getStatus());
        assertTrue(evo.getStatusDetail().contains("28080"));
        assertTrue(tryst.getStatusDetail().contains("port conflict"));
    }

    @Test
    public void testServicesMapPortStatuses() {
        EcosystemSnapshot s = EcosystemTopologyBuilder.build(
                fixtureState(), fixturePorts(), null, null, null);
        EcosystemNode evoHttp = s.node("service:EVO:28080");
        EcosystemNode mcp = s.node("service:EVO:28280");
        EcosystemNode trystHttp = s.node("service:TRYST:38080");
        assertNotNull(evoHttp);
        assertEquals(EcosystemNode.RuntimeStatus.RUNNING, evoHttp.getStatus());
        assertEquals(EcosystemNode.RuntimeStatus.READY, mcp.getStatus());
        assertEquals(EcosystemNode.RuntimeStatus.OCCUPIED, trystHttp.getStatus());
        assertEquals(28080, evoHttp.getPort());
        assertEquals("http://127.0.0.1:28080", evoHttp.getEndpoint());
        assertEquals("product:EVO", evoHttp.getParentId());
    }

    @Test
    public void testEnrModelsAndSessionsAreBuilt() {
        EcosystemSnapshot s = EcosystemTopologyBuilder.build(
                fixtureState(), fixturePorts(), null, fixtureEnr(), fixtureEnr());
        EcosystemNode provider = s.node("provider:ENR:enr");
        EcosystemNode model = s.node("model:ENR:gemma-2-9b-it");
        EcosystemNode session1 = s.node("session:ENR:s1");
        EcosystemNode session2 = s.node("session:ENR:s2");
        assertNotNull(provider);
        assertNotNull(model);
        assertNotNull(session1);
        assertNotNull(session2);
        assertEquals(EcosystemNode.RuntimeStatus.RUNNING, provider.getStatus());
        assertEquals("provider:ENR:enr", model.getParentId());
        // data-flow edge from session to its model
        boolean dataFlow = s.getEdges().stream().anyMatch(e ->
                e.getType() == EcosystemEdge.EdgeType.DATA_FLOW
                        && e.getSourceId().equals("session:ENR:s1")
                        && e.getTargetId().equals("model:ENR:gemma-2-9b-it"));
        assertTrue(dataFlow);
    }

    @Test
    public void testHardwareNodesFromMetrics() {
        EcosystemSnapshot s = EcosystemTopologyBuilder.build(
                fixtureState(), fixturePorts(), fixtureMetrics(), null, null);
        EcosystemNode cpu = s.node("hw:cpu");
        EcosystemNode ram = s.node("hw:ram");
        EcosystemNode gpu = s.node("hw:gpu:0");
        assertNotNull(cpu);
        assertNotNull(ram);
        assertNotNull(gpu);
        assertEquals(12.5, (Double) cpu.getMetrics().get("utilizationPercent"), 0.001);
        assertEquals(1.8, (Double) gpu.getMetrics().get("vramPercent"), 0.001);
        assertEquals(EcosystemTopologyBuilder.ROOT_ID, gpu.getParentId());
    }

    @Test
    public void testProfileDependencyEdges() {
        EcosystemSnapshot s = EcosystemTopologyBuilder.build(
                fixtureState(), fixturePorts(), null, null, null);
        boolean dep = s.getEdges().stream().anyMatch(e ->
                e.getType() == EcosystemEdge.EdgeType.DEPENDENCY
                        && e.getSourceId().equals("product:ENR")
                        && e.getTargetId().equals("product:EVO"));
        assertTrue(dep);
        // meta exposes launch profiles for the overview panel
        List<?> profiles = (List<?>) s.getMeta().get("profiles");
        assertNotNull(profiles);
        assertEquals(1, profiles.size());
    }

    @Test
    public void testSnapshotJsonRoundTrip() {
        EcosystemSnapshot s = EcosystemTopologyBuilder.build(
                fixtureState(), fixturePorts(), fixtureMetrics(), fixtureEnr(), fixtureEnr());
        String json = s.toJson();
        JSONObject parsed = new JSONObject(json);
        assertEquals(s.size(), parsed.getJSONArray("nodes").length());
        assertEquals(s.getEdges().size(), parsed.getJSONArray("edges").length());
        JSONArray nodes = parsed.getJSONArray("nodes");
        for (int i = 0; i < nodes.length(); i++) {
            JSONObject n = nodes.getJSONObject(i);
            assertTrue(n.has("id") && n.has("type") && n.has("status") && n.has("parentId"));
        }
    }

    @Test
    public void testProductIdNormalization() {
        assertEquals(EcosystemTopologyBuilder.ROOT_ID, EcosystemTopologyBuilder.productId("UPM"));
        assertEquals("product:DCode", EcosystemTopologyBuilder.productId("DCode"));
        assertEquals("product:DCode", EcosystemTopologyBuilder.productId("dcoder"));
        assertEquals("product:EVO", EcosystemTopologyBuilder.productId("Evolution"));
        assertEquals("product:Ollama", EcosystemTopologyBuilder.productId("ollama"));
        assertEquals("product:XYZ", EcosystemTopologyBuilder.productId("XYZ"));
    }

    @Test
    public void testDefensiveParsingNeverThrows() {
        // Garbage payloads must degrade gracefully, never throw.
        JSONObject garbage = new JSONObject();
        garbage.put("projects", "not-an-array");
        garbage.put("ports", new JSONObject().put("entries", 42));
        EcosystemSnapshot s = EcosystemTopologyBuilder.build(garbage, garbage, garbage, garbage, garbage);
        assertNotNull(s.node(EcosystemTopologyBuilder.ROOT_ID));
    }
}
