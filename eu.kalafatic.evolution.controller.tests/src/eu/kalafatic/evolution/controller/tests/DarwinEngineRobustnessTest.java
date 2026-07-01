package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.json.JSONObject;
import org.junit.Test;

import eu.kalafatic.evolution.controller.agents.RealityDiscoveryAgent;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ADarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;

public class DarwinEngineRobustnessTest {

    @Test
    public void testMapToBranchVariantWithDirtyJson() throws Exception {
        // Dirty JSON: architectural_facts and actions contain a string instead of an object
        String dirtyJson = "{\n" +
                "  \"id\": \"v1\",\n" +
                "  \"strategy\": \"S1\",\n" +
                "  \"actions\": [\n" +
                "    { \"domain\": \"file\", \"operation\": \"WRITE\", \"target\": \"t1\", \"description\": \"d1\" },\n" +
                "    \"Dirty action\"\n" +
                "  ],\n" +
                "  \"mediation_candidate\": {\n" +
                "    \"architectural_facts\": [\n" +
                "      \"Dirty fact\",\n" +
                "      { \"id\": \"f1\", \"subject\": \"S1\", \"predicate\": \"P1\", \"description\": \"D1\" }\n" +
                "    ],\n" +
                "    \"subsystems\": [\n" +
                "      { \"id\": \"s1\", \"name\": \"N1\" },\n" +
                "      \"Dirty subsystem\"\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        Orchestrator orch = OrchestrationFactory.eINSTANCE.createOrchestrator();
        String sid = "test-session-" + System.currentTimeMillis();
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sid);

        TaskContext context = new TaskContext(orch, null);
        context.setSessionId(sid);

        DarwinEngine engine = new DarwinEngine(context, null, null);

        // mapToBranchVariant is protected in ADarwinEngine, using reflection to test it
        Method method = ADarwinEngine.class.getDeclaredMethod("mapToBranchVariant",
                JSONObject.class, String.class, String.class,
                eu.kalafatic.evolution.controller.trajectory.Trajectory.class,
                TaskContext.class);
        method.setAccessible(true);

        BranchVariant v = (BranchVariant) method.invoke(engine, new JSONObject(dirtyJson), "goal", "phase", null, context);

        assertNotNull(v);
        assertEquals("S1", v.getStrategy());
        // Should have 1 action, skipping the dirty string
        assertEquals(1, v.getActions().size());

        assertNotNull(v.getMediationCandidate());
        // Should have 1 fact, skipping the dirty string
        assertEquals(1, v.getMediationCandidate().getArchitecturalFacts().size());
        // Should have 1 subsystem, skipping the dirty string
        assertEquals(1, v.getMediationCandidate().getSubsystems().size());
    }
}
