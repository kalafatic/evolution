package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentUnderstandingService;
import eu.kalafatic.evolution.controller.orchestration.intent.model.ExecutionBranch;
import eu.kalafatic.evolution.controller.orchestration.intent.model.ReconstructedIntent;
import eu.kalafatic.evolution.controller.orchestration.intent.model.ReconstructedIntent.ActionType;
import eu.kalafatic.evolution.controller.orchestration.intent.model.ReconstructedIntent.TargetRole;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IntentReconstructionEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SystemStateSignalProvider;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Comprehensive End-to-End Integration Tests for INTENT MODE & Intent Understanding.
 * Covers all mandatory scenarios A through I:
 * - Scenario A: Basic LOCAL conversation ("Hi")
 * - Scenario B: LOCAL to programming transition ("Hi" followed by "Write a Java class that manages HTTP connections")
 * - Scenario C: Programming with an existing target ("Analyze this project and implement a REST client")
 * - Scenario D: Ambiguous intent ("Improve this")
 * - Scenario E: Non-programming intent ("Explain how neural networks learn")
 * - Scenario F: Local AI unavailable (graceful degradation, deterministic fallback branches)
 * - Scenario G: Conversation context ("Now add logging")
 * - Scenario H: Target without modification intent ("Explain what this application does")
 * - Scenario I: Branch evaluation (3-4 meaningful candidate branches generated, compared, and justified)
 */
public class IntentModeComprehensiveIntegrationTest {

    private Orchestrator orchestrator;
    private File tempDir;
    private IntentUnderstandingService service;

    @Before
    public void setUp() throws Exception {
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        tempDir = File.createTempFile("evo-intent-test-target", "");
        tempDir.delete();
        tempDir.mkdirs();

        // Build sample Java Maven project target
        File srcDir = new File(tempDir, "src/main/java/com/example");
        srcDir.mkdirs();
        File pom = new File(tempDir, "pom.xml");
        try (FileWriter fw = new FileWriter(pom)) {
            fw.write("<project><modelVersion>4.0.0</modelVersion><groupId>com.example</groupId><artifactId>sample</artifactId><version>1.0.0</version></project>");
        }
        File mainFile = new File(srcDir, "AppServer.java");
        try (FileWriter fw = new FileWriter(mainFile)) {
            fw.write("package com.example;\npublic class AppServer { public void run() {} }\n");
        }

        service = new IntentUnderstandingService();
        SessionManager.getInstance().shutdownAll();
    }

    @After
    public void tearDown() throws Exception {
        SessionManager.getInstance().shutdownAll();
    }

    @Test
    public void testScenarioA_BasicLocalConversation() {
        // Input: "Hi"
        // Expected: LOCAL conversation, CHAT mode, no branches or developer mode invocation
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("test-session-a");

        ReconstructedIntent intent = service.reconstructIntent("Hi", context);

        assertNotNull(intent);
        assertEquals(ActionType.CONVERSATION, intent.getActionType());
        assertEquals(PlatformType.SIMPLE_CHAT, intent.getRecommendedExecutionMode());
        assertFalse(intent.isClarificationRequired());
        assertEquals(TargetRole.NONE, intent.getTargetRole());

        // Mode router verification
        ModeRouter router = new ModeRouter();
        PlatformMode mode = router.routeFast("Hi", orchestrator);
        assertNotNull(mode);
        assertEquals(PlatformType.SIMPLE_CHAT, mode.getType());
    }

    @Test
    public void testScenarioB_LocalToProgrammingTransition() {
        // Input sequence: 1. "Hi", 2. "Write a Java class that manages HTTP connections."
        // Expected: Transition detection, PROGRAMMING objective, DEVELOPER mode recommendation, 3-4 execution branches
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("test-session-b");

        // First message: "Hi"
        ConversationState convState = ConversationState.load(context.getSharedMemory(), context.getSessionId());
        convState.addMessage("User: Hi");
        convState.addMessage("Assistant: Hello! How can I help you today?");

        // Second message: Programming request
        String progPrompt = "Write a Java class that manages HTTP connections.";
        ReconstructedIntent intent = service.reconstructIntent(progPrompt, context);

        assertNotNull(intent);
        assertEquals(ActionType.PROGRAMMING, intent.getActionType());
        assertEquals(PlatformType.DEVELOPER, intent.getRecommendedExecutionMode());
        assertTrue("Candidate branches should be between 3 and 4", intent.getCandidateBranches().size() >= 3 && intent.getCandidateBranches().size() <= 4);

        // Verify branches contain diverse strategies (e.g. project integrated, standalone, assisted)
        List<ExecutionBranch> branches = intent.getCandidateBranches();
        boolean hasDevProject = branches.stream().anyMatch(b -> b.getName().contains("DEVELOPER") && b.getProposedMode() == PlatformType.DEVELOPER);
        boolean hasAssisted = branches.stream().anyMatch(b -> b.getProposedMode() == PlatformType.ASSISTED_CODING);
        assertTrue("Should include DEVELOPER branch", hasDevProject);
        assertTrue("Should include ASSISTED_CODING branch", hasAssisted);

        // Verify ModeRouter routeFast detects dynamic intent change
        ModeRouter router = new ModeRouter();
        PlatformMode routedMode = router.routeFast(progPrompt, orchestrator);
        assertEquals("Dynamic intent change should route to DEVELOPER", PlatformType.DEVELOPER, routedMode.getType());
    }

    @Test
    public void testScenarioC_ProgrammingWithExistingTarget() {
        // Input: "Analyze this project and implement a REST client."
        // Target: Existing Java project
        // Expected: Correct target interpretation, existing architecture analysis, DEVELOPER branch selected
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("test-session-c");

        ReconstructedIntent intent = service.reconstructIntent("Analyze this project and implement a REST client.", context);

        assertNotNull(intent);
        assertEquals(ActionType.PROGRAMMING, intent.getActionType());
        assertEquals(TargetRole.MODIFICATION_TARGET, intent.getTargetRole());
        assertEquals(tempDir.getAbsolutePath(), intent.getTargetPath());
        assertEquals(PlatformType.DEVELOPER, intent.getRecommendedExecutionMode());

        assertNotNull(intent.getSelectedBranch());
        assertEquals(PlatformType.DEVELOPER, intent.getSelectedBranch().getProposedMode());
        assertTrue(intent.getSelectedBranch().getName().contains("DEVELOPER"));

        // Check technical constraints or confirmed facts derived from target
        assertTrue(intent.getConfirmedFacts().stream().anyMatch(f -> f.contains("Maven")));
    }

    @Test
    public void testScenarioD_AmbiguousIntent() {
        // Input: "Improve this."
        // Target: A project or file
        // Expected: Recognition of ambiguity, no uncontrolled modifications, meaningful alternatives, clarification question
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("test-session-d");

        ReconstructedIntent intent = service.reconstructIntent("Improve this.", context);

        assertNotNull(intent);
        assertEquals(ActionType.CLARIFICATION_NEEDED, intent.getActionType());
        assertTrue("Clarification must be required for ambiguous instruction", intent.isClarificationRequired());
        assertNotNull(intent.getClarificationQuestion());
        assertFalse("Ambiguities list must not be empty", intent.getAmbiguities().isEmpty());

        // 3-4 Branches should offer safe alternatives (e.g. clarification, read-only diagnostic review, guided inspection)
        assertTrue(intent.getCandidateBranches().size() >= 3);
        assertEquals(PlatformType.SIMPLE_CHAT, intent.getRecommendedExecutionMode());
    }

    @Test
    public void testScenarioE_NonProgrammingIntent() {
        // Input: "Explain how neural networks learn."
        // Expected: Educational intent, no unnecessary Developer Mode invocation
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("test-session-e");

        ReconstructedIntent intent = service.reconstructIntent("Explain how neural networks learn.", context);

        assertNotNull(intent);
        assertEquals(ActionType.QUESTION, intent.getActionType());
        assertNotEquals(PlatformType.DEVELOPER, intent.getRecommendedExecutionMode());
        assertEquals(PlatformType.SIMPLE_CHAT, intent.getRecommendedExecutionMode());

        assertNotNull(intent.getSelectedBranch());
        assertEquals(PlatformType.SIMPLE_CHAT, intent.getSelectedBranch().getProposedMode());
    }

    @Test
    public void testScenarioF_LocalAiUnavailableGracefulDegradation() {
        // Given no LLM or null AI service
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("test-session-f");
        context.setAiService(null);

        ReconstructedIntent intent = service.reconstructIntent("Write a Java class that manages HTTP connections.", context);

        assertNotNull("Intent reconstruction must not fail with null AI service", intent);
        assertEquals(ActionType.PROGRAMMING, intent.getActionType());
        assertEquals(PlatformType.DEVELOPER, intent.getRecommendedExecutionMode());
        assertTrue("Must provide 3 to 4 fallback candidate branches", intent.getCandidateBranches().size() >= 3 && intent.getCandidateBranches().size() <= 4);
        assertNotNull("Winning branch must be selected", intent.getSelectedBranch());
    }

    @Test
    public void testScenarioG_ConversationContextRetention() {
        // Input sequence: 1. Discuss Java project, 2. "Now add logging."
        // Expected: Relevant previous context retained, correct target and objective reconstructed
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("test-session-g");

        ConversationState convState = ConversationState.load(context.getSharedMemory(), context.getSessionId());
        convState.addMessage("User: We are working on the AppServer HTTP listener in com.example");
        convState.addMessage("Assistant: Noted. AppServer manages network endpoints.");
        String updatedMemory = ConversationState.save(context.getSharedMemory(), context.getSessionId(), convState);
        context.getOrchestrator().setSharedMemory(updatedMemory);

        ReconstructedIntent intent = service.reconstructIntent("Now add logging.", context);

        assertNotNull(intent);
        assertNotNull(intent.getConversationSummary());
        assertTrue("Conversation summary should retain AppServer context", intent.getConversationSummary().contains("AppServer"));
        assertEquals(ActionType.PROGRAMMING, intent.getActionType());
        assertEquals(PlatformType.DEVELOPER, intent.getRecommendedExecutionMode());
    }

    @Test
    public void testScenarioH_TargetWithoutModificationIntent() {
        // Input: "Explain what this application does."
        // Target: Existing Java repository
        // Expected: Repository is treated as an analysis target (ANALYSIS_ONLY), no source modifications
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("test-session-h");

        ReconstructedIntent intent = service.reconstructIntent("Explain what this application does.", context);

        assertNotNull(intent);
        assertEquals(TargetRole.ANALYSIS_ONLY, intent.getTargetRole());
        assertEquals(ActionType.QUESTION, intent.getActionType());
        assertNotEquals("Should not recommend code modification DEVELOPER mode for read-only explanation",
                PlatformType.DEVELOPER, intent.getRecommendedExecutionMode());
    }

    @Test
    public void testScenarioI_BranchEvaluationAndEngineExecution() throws Exception {
        // Complex development request in IntentReconstructionEngine execution
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("test-session-i");
        context.getOrchestrationState().setRawInput("Design and implement an asynchronous connection pool for HTTP connections");

        SessionContainer session = SessionManager.getInstance().getOrCreateSession("test-session-i");
        session.getCognitiveState().setCurrentCapability(eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType.INTENT_RECONSTRUCTION);

        PlatformMode platformMode = new PlatformMode(PlatformType.INTENT_RECONSTRUCTION, null, 1, false);
        context.setPlatformMode(platformMode);

        IterationMemoryService memoryService = new IterationMemoryService(tempDir);
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(tempDir, context);

        IntentReconstructionEngine engine = new IntentReconstructionEngine(context, memoryService, stateProvider);
        IterationManager manager = KernelFactory.create(context.getOrchestrationState().getRawInput(), context, session, null);

        EvaluationResult evalResult = engine.runDarwinIteration(context, manager);

        assertNotNull(evalResult);
        assertTrue(evalResult.isSuccess());

        ReconstructedIntent reconstructed = (ReconstructedIntent) context.getOrchestrationState().getMetadata().get("reconstructedIntent");
        assertNotNull("ReconstructedIntent must be present in metadata", reconstructed);

        // Verify 3-4 distinct branches with explicit advantages and limitations
        List<ExecutionBranch> branches = reconstructed.getCandidateBranches();
        assertTrue("Must generate 3 to 4 execution branches", branches.size() >= 3 && branches.size() <= 4);

        for (ExecutionBranch b : branches) {
            assertNotNull("Branch id must not be null", b.getId());
            assertNotNull("Branch interpretation must not be null", b.getInterpretation());
            assertNotNull("Branch proposed mode must not be null", b.getProposedMode());
            assertTrue("Branch confidence must be between 0 and 1", b.getConfidence() >= 0.0 && b.getConfidence() <= 1.0);
        }

        assertNotNull("Winning branch must be chosen", reconstructed.getSelectedBranch());
        assertNotNull("Decision rationale must be present", reconstructed.getDecisionRationale());

        // Test FinalResponseAssembler formatting of Reconstructed Intent
        FinalResponseAssembler assembler = new FinalResponseAssembler();
        FinalResponse response = assembler.assemble(context, "Completed intent analysis", true, java.time.Instant.now());
        assertNotNull(response);
        String text = response.getSummary();
        assertTrue("Final response should include Reconstructed Intent header", text.contains("Reconstructed Intent & Execution Branches"));
        assertTrue("Final response should list candidate branches", text.contains("Candidate Darwin Execution Branches:"));
        assertTrue("Final response should state selected execution direction", text.contains("Selected Execution Direction:"));
    }
}
