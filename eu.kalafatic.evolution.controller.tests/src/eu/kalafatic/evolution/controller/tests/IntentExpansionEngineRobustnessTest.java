package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionEngine;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionDimension;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.SessionContext;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.intent.InterpretationState;

import java.util.List;

public class IntentExpansionEngineRobustnessTest {

    private IntentExpansionEngine engine;
    private TaskContext context;

    @Before
    public void setUp() {
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        SessionContext session = (SessionContext) SessionManager.getInstance().getOrCreateSession("test-session-" + System.currentTimeMillis());
        context = new TaskContext(orchestrator, null);
        context.setSessionId(session.getSessionId());
        session.setTaskContext(context);
        engine = new IntentExpansionEngine(session);
    }

    @Test
    public void testRobustArrayParsing() throws Exception {
        // Mock AI Service to return mixed types in unresolvedDimensions
        AiService mockAi = new AiService() {
            @Override
            public String sendRequest(Orchestrator orchestrator, String prompt, TaskContext context) throws Exception {
                return "{\n" +
                       "  \"state\": \"CLEAR\",\n" +
                       "  \"dominantIntent\": \"IMPLEMENTATION\",\n" +
                       "  \"confidence\": {\"overallConfidence\": 0.9, \"rationale\": \"test\"},\n" +
                       "  \"unresolvedDimensions\": [\n" +
                       "    \"A simple string dimension\",\n" +
                       "    {\n" +
                       "      \"id\": \"dim-1\",\n" +
                       "      \"description\": \"An object dimension\",\n" +
                       "      \"candidateBlueprints\": [\n" +
                       "        \"A string blueprint\",\n" +
                       "        {\"id\": \"bp-1\", \"strategy\": \"An object blueprint\"}\n" +
                       "      ]\n" +
                       "    }\n" +
                       "  ]\n" +
                       "}";
            }
        };
        engine.setAiService(mockAi);

        IntentExpansionResult result = engine.expand("test", context);

        assertNotNull(result);
        assertEquals(InterpretationState.CLEAR, result.getState());
        List<EvolutionDimension> dims = result.getUnresolvedDimensions();
        assertEquals(2, dims.size());

        // Verify string dimension
        assertEquals("dim-0", dims.get(0).getId());
        assertEquals("A simple string dimension", dims.get(0).getDescription());

        // Verify object dimension and its mixed blueprints
        assertEquals("dim-1", dims.get(1).getId());
        assertEquals(2, dims.get(1).getCandidateBranches().size());
        assertEquals("v-0", dims.get(1).getCandidateBranches().get(0).getId());
        assertEquals("A string blueprint", dims.get(1).getCandidateBranches().get(0).getStrategy());
        assertEquals("bp-1", dims.get(1).getCandidateBranches().get(1).getId());
        assertEquals("An object blueprint", dims.get(1).getCandidateBranches().get(1).getStrategy());
    }
}
