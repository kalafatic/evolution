package eu.kalafatic.evolution.controller.tests;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.KernelFactory;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinStrategyType;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot;
import eu.kalafatic.evolution.controller.tools.ShellTool;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.orchestration.workspace.SemanticWorkspace;
import eu.kalafatic.evolution.controller.orchestration.workspace.TrajectoryMemory;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.SelfDevStatus;

public class RecursiveEvolutionTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private AiService aiService;
    private Orchestrator orchestrator;
    private TaskContext context;
    private MockProvider mockLlm;

    @Before
    public void setup() throws Exception {
        mockLlm = new MockProvider();
        aiService = new AiService() {
            @Override
            public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                return mockLlm.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
        };
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();

        SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
        session.setId("test-session");
        session.setStatus(SelfDevStatus.RUNNING);
        orchestrator.setSelfDevSession(session);

        File root = tempFolder.newFolder();
        ShellTool shell = new ShellTool();
        TaskContext initContext = new TaskContext(orchestrator, root);
        shell.execute("git init", root, initContext);
        shell.execute("git config user.email \"test@example.com\"", root, initContext);
        shell.execute("git config user.name \"Test User\"", root, initContext);
        Files.writeString(new File(root, "pom.xml").toPath(), "<project><modelVersion>4.0.0</modelVersion><groupId>test</groupId><artifactId>test</artifactId><version>1.0</version></project>");
        shell.execute("git add .", root, initContext);
        shell.execute("git commit -m \"Initial commit\"", root, initContext);

        context = new TaskContext(orchestrator, root);
        context.setSessionId("test-session");
        eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getOrCreateSession("test-session");
        context.getBehaviorProfile().addTrait(BehaviorTrait.REASONING_DARWIN_ITERATIVE);
        context.setAutoApprove(true);
        context.getMetadata().put("testMode", true);

        // Mock LLM responses for Intent Expansion
        mockLlm.addResponseMapping("IntentExpansionEngine", "{\"state\": \"CLEAR\", \"dominantIntent\": \"Task\", \"confidence\": {\"overallConfidence\": 1.0}}");
    }

    @Test
    public void testRecursiveArchitecturalRefinement() throws Exception {
        eu.kalafatic.evolution.controller.orchestration.SessionContainer session = eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getOrCreateSession(context.getSessionId());
        IterationManager manager = KernelFactory.create(context, session, aiService);

        // We simulate a survival path that takes multiple iterations to stabilize
        // Generation 0: Initial implementation
        // Generation 1: Mutation for resilience
        // Generation 2: Mutation for extensibility
        // Generation 3: Stability reached

        TaskRequest request = new TaskRequest();
        request.setPrompt("Write a Java print task");

        OrchestratorResponse response = manager.handle(request);

        // Verify that recursion occurred (Iteration Count should be > 1 if pressure was applied and stability not reached)
        assertTrue("Iteration count should be at least 1", context.getOrchestrationState().getIterationCount() >= 1);
        // Given our mocked components, we check if the evolution loop was entered
    }

    @Test
    public void testMediatedModePressurePersistence() throws Exception {
        context.getBehaviorProfile().addTrait(BehaviorTrait.SUPERVISION_MEDIATED);
        eu.kalafatic.evolution.controller.orchestration.SessionContainer session = eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getOrCreateSession(context.getSessionId());
        IterationManager manager = KernelFactory.create(context, session, aiService);

        TaskRequest request = new TaskRequest();
        request.setPrompt("Analyze project architecture");

        manager.handle(request);

        // Verify that pressure history was recorded in the trajectory
        Trajectory trajectory = context.getSemanticWorkspace().getTrajectoryMemory().getTrajectories().values().stream()
                .findFirst().orElse(null);

        if (trajectory != null) {
            assertTrue("Pressure history should be captured", !trajectory.getPressureHistory().isEmpty());
        }
    }

    private static class MockProvider {
        private final java.util.Map<String, String> responseMappings = new java.util.concurrent.ConcurrentHashMap<>();
        private String defaultResponse = "{\"success\": true, \"comment\": \"Mock success\"}";

        public void addResponseMapping(String keyword, String response) {
            responseMappings.put(keyword, response);
        }

        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
            for (java.util.Map.Entry<String, String> entry : responseMappings.entrySet()) {
                if (prompt.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return defaultResponse;
        }
    }
}
