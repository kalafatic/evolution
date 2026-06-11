package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.json.JSONObject;
import org.junit.Test;

import eu.kalafatic.evolution.controller.agents.RealityDiscoveryAgent;
import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.SessionContext;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;

public class RealityDiscoveryAgentRobustnessTest {

    @Test
    public void testDiscoverWithDirtyJson() throws Exception {
        // Dirty JSON: architectural_facts contains a string instead of an object
        String dirtyJson = "{\n" +
                "  \"domain\": \"Evolution\",\n" +
                "  \"purpose\": \"Self-improvement\",\n" +
                "  \"architectural_facts\": [\n" +
                "    { \"id\": \"f1\", \"subject\": \"S1\", \"predicate\": \"P1\", \"description\": \"D1\" },\n" +
                "    \"This is a dirty fact that should be skipped\",\n" +
                "    { \"id\": \"f2\", \"subject\": \"S2\", \"predicate\": \"P2\", \"description\": \"D2\" }\n" +
                "  ],\n" +
                "  \"hotspots\": [\n" +
                "    \"Dirty hotspot\",\n" +
                "    { \"id\": \"h1\", \"name\": \"H1\" }\n" +
                "  ]\n" +
                "}";

        Orchestrator orch = OrchestrationFactory.eINSTANCE.createOrchestrator();
        SessionContext session = new SessionContext("test-session");
        TaskContext context = new TaskContext(orch, null);

        AiService aiService = new AiService() {
            @Override
            public String sendRequest(Orchestrator orchestrator, String prompt, TaskContext context) throws Exception {
                return dirtyJson;
            }
        };

        RealityDiscoveryAgent agent = new RealityDiscoveryAgent(session);
        agent.setAiService(aiService);

        TargetRealityModel model = agent.discover("test", context, null);

        assertNotNull(model);
        assertEquals("Evolution", model.getDomain());
        // Should have 2 facts, skipping the dirty string
        assertEquals(2, model.getArchitecturalFacts().size());
        // Should have 1 hotspot, skipping the dirty string
        assertEquals(1, model.getHotspots().size());
    }
}
