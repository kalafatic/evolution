package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.CompositeModelProvider;
import eu.kalafatic.evolution.controller.orchestration.llm.CompositeModelProvider.DelegatedTask;
import eu.kalafatic.evolution.controller.orchestration.llm.CompositeModelProvider.ModelRole;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class CompositeModelProviderTest {

    @Test
    public void testMultiModelSequentialDelegationAndComposition() throws Exception {
        List<String> invokedRoles = new ArrayList<>();

        ILlmProvider javaSpecialist = (orchestrator, prompt, temperature, proxyUrl, context) -> {
            invokedRoles.add("CODE_SPECIALIST");
            return "Found 1 architectural defect: tightly coupled database access in controller.";
        };

        ILlmProvider reasoningSpecialist = (orchestrator, prompt, temperature, proxyUrl, context) -> {
            invokedRoles.add("REASONING_SPECIALIST");
            assertTrue("Prompt must contain previous stage findings", prompt.contains("tightly coupled"));
            return "Recommendation: Decouple via repository pattern and dependency injection.";
        };

        ILlmProvider englishSpecialist = (orchestrator, prompt, temperature, proxyUrl, context) -> {
            invokedRoles.add("COMMUNICATION_SPECIALIST");
            assertTrue("Prompt must contain reasoning recommendation", prompt.contains("repository pattern"));
            return "In summary, the implementation should introduce a repository abstraction to achieve clean separation of concerns.";
        };

        CompositeModelProvider composite = new CompositeModelProvider();
        composite.addDelegatedTask(new DelegatedTask(
                ModelRole.CODE_SPECIALIST,
                "evo-java-expert",
                javaSpecialist,
                "Analyze Java source code:"
        ));
        composite.addDelegatedTask(new DelegatedTask(
                ModelRole.REASONING_SPECIALIST,
                "evo-reasoning-expert",
                reasoningSpecialist,
                "Evaluate architectural trade-offs:"
        ));
        composite.addDelegatedTask(new DelegatedTask(
                ModelRole.COMMUNICATION_SPECIALIST,
                "evo-english-expert",
                englishSpecialist,
                "Provide final English synthesis:"
        ));

        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setAiMode(AiMode.LOCAL);
        orchestrator.setLocalModel("composite-expert");

        TaskContext context = new TaskContext(orchestrator, null);

        String userPrompt = "Analyze this Java implementation and explain the architectural problems in English.";
        String finalResponse = composite.sendRequest(orchestrator, userPrompt, 0.0f, null, context);

        assertNotNull(finalResponse);
        assertTrue(finalResponse.contains("separation of concerns"));
        assertEquals(3, invokedRoles.size());
        assertEquals("CODE_SPECIALIST", invokedRoles.get(0));
        assertEquals("REASONING_SPECIALIST", invokedRoles.get(1));
        assertEquals("COMMUNICATION_SPECIALIST", invokedRoles.get(2));
    }

    @Test
    public void testLlmRouterRoutesCompositeModelRequest() throws Exception {
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setAiMode(AiMode.LOCAL);
        orchestrator.setLocalModel("composite-multi-model");

        TaskContext context = new TaskContext(orchestrator, null);
        context.getMetadata().put("compositeModel", Boolean.TRUE);

        LlmRouter router = LlmRouter.getInstance();

        // Inject mock native provider that returns simulated answers for composite stages
        router.setNativeEvoProvider((orch, prompt, temp, proxy, ctx) -> {
            if (prompt.contains("implementation and code")) {
                return "Stage 1 code review output";
            } else if (prompt.contains("architectural problems")) {
                return "Stage 2 reasoning output";
            } else {
                return "Stage 3 final synthesized response in English.";
            }
        });

        String userPrompt = "Analyze Java implementation";
        String response = router.sendRequest(orchestrator, userPrompt, 0.0f, null, context);

        assertNotNull(response);
        assertTrue("Response should contain stage 3 synthesis", response.contains("final synthesized response"));
    }
}
