package eu.kalafatic.evolution.controller.tests;

import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;

public class DarwinDiagnosticTest {
    @Test
    public void runDiagnostic() throws Exception {
        String sid = "diag-manual";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sid);

        Orchestrator orch = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orch.setLocalModel("gemma3:1b");

        TaskContext context = new TaskContext(orch, null);
        context.setSessionId(sid);
        context.getMetadata().put("testMode", true);

        // GROUNDING: Ensure execution profile is initialized for the test context
        eu.kalafatic.evolution.controller.kernel.EvolutionProfile profile =
            eu.kalafatic.evolution.controller.kernel.EvolutionIntensityCalculator.calculate(context, null, null);
        context.getOrchestrationState().setExecutionProfile(profile);

        AiService ai = new AiService() {
            @Override
            public String sendRequest(Orchestrator o, String p, TaskContext c) throws Exception {
                String response = "This is not JSON but contains <BEGIN_DARWIN_JSON>{\"id\": \"v-test\", \"strategy\": \"Mock strategy\", \"strategy_type\": \"PROBABLE_SURVIVOR\", \"semantic_anchor\": \"Mock anchor\", \"survival_argument\": \"Mock argument\", \"tradeoffs\": \"Mock tradeoffs\", \"failure_risks\": \"Mock risks\", \"actions\": []}<END_DARWIN_JSON>";
                c.log("Stage: LLM\nProvider: Mock\nModel: Mock\nToken count: 0\nRaw response length: " + response.length());
                return response;
            }
        };
        context.setAiService(ai);

        DarwinEngine engine = new DarwinEngine(context, new IterationMemoryService(new java.io.File(".")), null);
        engine.setAiService(ai);

        context.log("TEST_LOG: Starting Darwin diagnostic...");
        GoalModel goalModel = new GoalModel();
        goalModel.setPrimaryAction("Test Goal");
        goalModel.setDomain("JAVA");
        goalModel.setGoalType("CODE_GENERATION");
        goalModel.setRequestedArtifact("Java Class");
        
        try {
            engine.generateVariants(goalModel, null, null, null, null);
        } catch (Exception e) {
             context.log("TEST_LOG: Darwin engine execution completed with error: " + e.getMessage());
             java.io.StringWriter sw = new java.io.StringWriter();
             e.printStackTrace(new java.io.PrintWriter(sw));
             context.log(sw.toString());
        }

        System.out.println("FULL_LOG_START");
        for (String log : context.getLogs()) {
            System.out.println(log);
        }
        System.out.println("FULL_LOG_END");
    }
}
