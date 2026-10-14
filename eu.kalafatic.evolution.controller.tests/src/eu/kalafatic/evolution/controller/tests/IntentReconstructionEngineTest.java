package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.io.FileWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.mediation.model.*;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.controller.orchestration.selfdev.*;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class IntentReconstructionEngineTest {

    private Orchestrator orchestrator;
    private File tempDir;

    @Before
    public void setUp() throws Exception {
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        tempDir = File.createTempFile("evo-test-intent", "");
        tempDir.delete();
        tempDir.mkdirs();

        // Create sample Java class and subfolders for testing initialSeed and peeking
        File srcDir = new File(tempDir, "src/main/java/com/example");
        srcDir.mkdirs();
        File mainFile = new File(srcDir, "MainApplication.java");
        try (FileWriter fw = new FileWriter(mainFile)) {
            fw.write("package com.example;\npublic class MainApplication { public static void main(String[] args){} }\n");
        }

        SessionManager.getInstance().shutdownAll();
    }

    @After
    public void tearDown() throws Exception {
        SessionManager.getInstance().shutdownAll();
    }

    @Test
    public void testIntentReconstructionEngineRunIteration() throws Exception {
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("test-session-intent-engine");
        context.getOrchestrationState().setRawInput("# Intent Discovery\n\nDiscover target project purpose");

        SessionContainer session = SessionManager.getInstance().getOrCreateSession("test-session-intent-engine");
        session.getCognitiveState().setCurrentCapability(eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType.INTENT_RECONSTRUCTION);

        PlatformMode platformMode = new PlatformMode(PlatformType.INTENT_RECONSTRUCTION, null, 1, false);
        context.setPlatformMode(platformMode);

        // Pre-populate a TargetRealityModel in metadata
        TargetRealityModel rm = new TargetRealityModel();
        rm.setDomain("Core Systems");
        rm.setPurpose("High throughput message router");
        Subsystem sub = new Subsystem("sub1", "RouterCore");
        sub.setPurpose("Routes incoming messages");
        sub.setConfidence(0.9);
        rm.addSubsystem(sub);

        ArchitecturalUseCase uc = new ArchitecturalUseCase("uc1", "Route Message", "Processes and routes payload", 0.95);
        rm.addUseCase(uc);

        rm.getArchitecturalAuthorityFiles().add("src/main/java/com/example/MainApplication.java");
        rm.getExecutionFlows().add("Client -> Router -> Handler");

        KnowledgeGap gap = new KnowledgeGap("gap1", "Unclear persistence mechanism", KnowledgeGap.GapType.MISSING_EVIDENCE);
        rm.addKnowledgeGap(gap);

        context.getOrchestrationState().getMetadata().put("targetRealityModel", rm);

        IterationMemoryService memoryService = new IterationMemoryService(tempDir);
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(tempDir, context);

        IntentReconstructionEngine engine = new IntentReconstructionEngine(context, memoryService, stateProvider);

        IterationManager manager = KernelFactory.create(context.getOrchestrationState().getRawInput(), context, session, null);

        EvaluationResult evalResult = engine.runDarwinIteration(context, manager);

        assertNotNull(evalResult);
        assertTrue(evalResult.isSuccess());

        String targetSummary = (String) context.getOrchestrationState().getMetadata().get("intentTargetSummary");
        assertNotNull("intentTargetSummary should be populated in metadata", targetSummary);
        assertTrue(targetSummary.contains("Reconstructed Target Purpose & Knowledge"));
        assertTrue(targetSummary.contains("High throughput message router"));
        assertTrue(targetSummary.contains("RouterCore"));
        assertTrue(targetSummary.contains("Route Message"));
        assertTrue(targetSummary.contains("MainApplication.java"));
        assertTrue(targetSummary.contains("Client -> Router -> Handler"));
        assertTrue(targetSummary.contains("Unclear persistence mechanism"));
    }

    @Test
    public void testIntentReconstructionEngineWithRootDatasetFiles() throws Exception {
        // Create root directory containing dataset files (no subdirectories)
        File datasetDir = File.createTempFile("evo-test-dataset", "");
        datasetDir.delete();
        datasetDir.mkdirs();

        File parquet1 = new File(datasetDir, "0000.parquet");
        try (FileWriter fw = new FileWriter(parquet1)) {
            fw.write("PAR1_MOCK_DATASET_CONTENT");
        }

        File parquet2 = new File(datasetDir, "0001.parquet");
        try (FileWriter fw = new FileWriter(parquet2)) {
            fw.write("PAR1_MOCK_DATASET_CONTENT_2");
        }

        TaskContext context = new TaskContext(orchestrator, datasetDir);
        context.setSessionId("test-session-dataset-engine");
        context.getOrchestrationState().setRawInput("# Intent Discovery\n\nDiscover target project purpose");

        SessionContainer session = SessionManager.getInstance().getOrCreateSession("test-session-dataset-engine");
        session.getCognitiveState().setCurrentCapability(eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType.INTENT_RECONSTRUCTION);

        PlatformMode platformMode = new PlatformMode(PlatformType.INTENT_RECONSTRUCTION, null, 1, false);
        context.setPlatformMode(platformMode);

        IterationMemoryService memoryService = new IterationMemoryService(datasetDir);
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(datasetDir, context);

        IntentReconstructionEngine engine = new IntentReconstructionEngine(context, memoryService, stateProvider);
        IterationManager manager = KernelFactory.create(context.getOrchestrationState().getRawInput(), context, session, null);

        EvaluationResult evalResult = engine.runDarwinIteration(context, manager);

        assertNotNull(evalResult);
        assertTrue(evalResult.isSuccess());

        eu.kalafatic.evolution.controller.orchestration.design.DesignModel designModel =
                (eu.kalafatic.evolution.controller.orchestration.design.DesignModel) context.getOrchestrationState().getMetadata().get("reconstructedDesignModel");
        assertNotNull("reconstructedDesignModel should be populated", designModel);

        boolean foundParquetComponent = designModel.getComponents().stream()
                .anyMatch(c -> c.getName() != null && c.getName().endsWith(".parquet") && "DATASET".equalsIgnoreCase(c.getType()));
        assertTrue("Parquet root files should be discovered and categorized as DATASET components", foundParquetComponent);

        String targetSummary = (String) context.getOrchestrationState().getMetadata().get("intentTargetSummary");
        assertNotNull("intentTargetSummary should be populated", targetSummary);
        assertTrue(targetSummary.contains("Target Nature: Knowledge/Document Repository") || targetSummary.contains("Heterogeneous Artifact Repository") || targetSummary.contains("DATASET"));
    }

    @Test
    public void testIntentReconstructionEngineWithCustomArbitraryFileExtensions() throws Exception {
        // Create root directory containing arbitrary custom file extensions (no hardcoded extension)
        File customDir = File.createTempFile("evo-test-custom-ext", "");
        customDir.delete();
        customDir.mkdirs();

        File customFile1 = new File(customDir, "0000.stories");
        try (FileWriter fw = new FileWriter(customFile1)) {
            fw.write("Once upon a time in a tiny story dataset...");
        }

        File customFile2 = new File(customDir, "data.unknownext");
        try (FileWriter fw = new FileWriter(customFile2)) {
            fw.write("Arbitrary dataset content line 1\nArbitrary dataset content line 2");
        }

        TaskContext context = new TaskContext(orchestrator, customDir);
        context.setSessionId("test-session-custom-ext");
        context.getOrchestrationState().setRawInput("# Intent Discovery\n\nDiscover target project purpose");

        SessionContainer session = SessionManager.getInstance().getOrCreateSession("test-session-custom-ext");
        session.getCognitiveState().setCurrentCapability(eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType.INTENT_RECONSTRUCTION);

        PlatformMode platformMode = new PlatformMode(PlatformType.INTENT_RECONSTRUCTION, null, 1, false);
        context.setPlatformMode(platformMode);

        IterationMemoryService memoryService = new IterationMemoryService(customDir);
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(customDir, context);

        IntentReconstructionEngine engine = new IntentReconstructionEngine(context, memoryService, stateProvider);
        IterationManager manager = KernelFactory.create(context.getOrchestrationState().getRawInput(), context, session, null);

        EvaluationResult evalResult = engine.runDarwinIteration(context, manager);

        assertNotNull(evalResult);
        assertTrue(evalResult.isSuccess());

        eu.kalafatic.evolution.controller.orchestration.design.DesignModel designModel =
                (eu.kalafatic.evolution.controller.orchestration.design.DesignModel) context.getOrchestrationState().getMetadata().get("reconstructedDesignModel");
        assertNotNull("reconstructedDesignModel should be populated", designModel);

        boolean foundCustomFile = designModel.getComponents().stream()
                .anyMatch(c -> c.getName() != null && (c.getName().equals("0000.stories") || c.getName().equals("data.unknownext")));
        assertTrue("Arbitrary custom extension files must be discovered and inspected", foundCustomFile);
    }
}
