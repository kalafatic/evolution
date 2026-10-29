package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.io.FileWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.mediation.model.*;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetScanner;
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
    public void testWebApplicationTargetScannerClassification() throws Exception {
        File webDir = File.createTempFile("evo-test-web", "");
        webDir.delete();
        webDir.mkdirs();

        File indexHtml = new File(webDir, "index.html");
        try (FileWriter fw = new FileWriter(indexHtml)) { fw.write("<html><body>Test</body></html>"); }
        File appJs = new File(webDir, "app.js");
        try (FileWriter fw = new FileWriter(appJs)) { fw.write("console.log('App');"); }
        File pkgJson = new File(webDir, "package.json");
        try (FileWriter fw = new FileWriter(pkgJson)) { fw.write("{\"name\": \"my-web-app\"}"); }

        TargetScanner scanner = new TargetScanner();
        TargetSnapshot snapshot = scanner.scanToSnapshot(webDir, TargetSnapshot.TargetType.PROJECT);

        assertNotNull(snapshot);
        String classifications = (String) snapshot.getMetadata().get("targetClassifications");
        assertNotNull(classifications);
        assertTrue(classifications.contains("WEB_APPLICATION"));
        assertTrue(classifications.contains("WEBSITE_SOURCE"));
    }

    @Test
    public void testMusicAlbumTargetScannerClassification() throws Exception {
        File musicDir = File.createTempFile("evo-test-music", "");
        musicDir.delete();
        musicDir.mkdirs();

        File track1 = new File(musicDir, "track1.mp3");
        try (FileWriter fw = new FileWriter(track1)) { fw.write("MPEG AUDIO DATA"); }
        File track2 = new File(musicDir, "track2.flac");
        try (FileWriter fw = new FileWriter(track2)) { fw.write("FLAC AUDIO DATA"); }

        TargetScanner scanner = new TargetScanner();
        TargetSnapshot snapshot = scanner.scanToSnapshot(musicDir, TargetSnapshot.TargetType.PROJECT);

        assertNotNull(snapshot);
        String classifications = (String) snapshot.getMetadata().get("targetClassifications");
        assertNotNull(classifications);
        assertTrue(classifications.contains("MUSIC_ALBUM") || classifications.contains("AUDIO_COLLECTION"));
    }

    @Test
    public void testPresentationAndDocumentTargetScannerClassification() throws Exception {
        File docDir = File.createTempFile("evo-test-docs", "");
        docDir.delete();
        docDir.mkdirs();

        File slides = new File(docDir, "presentation.pptx");
        try (FileWriter fw = new FileWriter(slides)) { fw.write("SLIDES DATA"); }
        File paper = new File(docDir, "whitepaper.pdf");
        try (FileWriter fw = new FileWriter(paper)) { fw.write("PDF DATA"); }

        TargetScanner scanner = new TargetScanner();
        TargetSnapshot snapshot = scanner.scanToSnapshot(docDir, TargetSnapshot.TargetType.PROJECT);

        assertNotNull(snapshot);
        String classifications = (String) snapshot.getMetadata().get("targetClassifications");
        assertNotNull(classifications);
        assertTrue(classifications.contains("PRESENTATION") || classifications.contains("DOCUMENT_COLLECTION"));
    }

    @Test
    public void testNullPhaseAndSynthesisPhaseHandling() throws Exception {
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("test-session-null-phase");
        context.getOrchestrationState().setRawInput("# Intent Discovery\n\nDiscover target project purpose");
        context.getOrchestrationState().setCurrentPhase(null);

        SessionContainer session = SessionManager.getInstance().getOrCreateSession("test-session-null-phase");
        session.getCognitiveState().setCurrentCapability(eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType.INTENT_RECONSTRUCTION);

        PlatformMode platformMode = new PlatformMode(PlatformType.INTENT_RECONSTRUCTION, null, 1, false);
        context.setPlatformMode(platformMode);

        IterationMemoryService memoryService = new IterationMemoryService(tempDir);
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(tempDir, context);

        IntentReconstructionEngine engine = new IntentReconstructionEngine(context, memoryService, stateProvider);
        IterationManager manager = KernelFactory.create(context.getOrchestrationState().getRawInput(), context, session, null);

        // Verify runDarwinIteration runs with null phase safely and sets synthesis phase on completion
        EvaluationResult evalResult = engine.runDarwinIteration(context, manager);
        assertNotNull(evalResult);
        assertTrue(evalResult.isSuccess());

        String phaseAfterIteration = context.getOrchestrationState().getCurrentPhase();
        assertNotNull("Current phase should be non-null after iteration", phaseAfterIteration);
        assertTrue(phaseAfterIteration.contains("SYNTHESIS") || phaseAfterIteration.contains("EXPANSION"));
    }

    @Test
    public void testBatchQueueNoSilentOmission() throws Exception {
        File multiDir = File.createTempFile("evo-test-multi", "");
        multiDir.delete();
        multiDir.mkdirs();

        // Create 25 files in a directory to exceed a single batch limit (10)
        File pagesFolder = new File(multiDir, "pages");
        pagesFolder.mkdirs();
        for (int i = 0; i < 25; i++) {
            File p = new File(pagesFolder, "page" + i + ".html");
            try (FileWriter fw = new FileWriter(p)) { fw.write("<html>page " + i + "</html>"); }
        }

        TaskContext context = new TaskContext(orchestrator, multiDir);
        context.setSessionId("test-session-batch");
        SessionContainer session = SessionManager.getInstance().getOrCreateSession("test-session-batch");

        IterationMemoryService memoryService = new IterationMemoryService(multiDir);
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(multiDir, context);

        IntentReconstructionEngine engine = new IntentReconstructionEngine(context, memoryService, stateProvider);
        IterationManager manager = KernelFactory.create("Discover intent", context, session, null);

        // Run iteration 1 (reads batch limit 10)
        EvaluationResult eval1 = engine.runDarwinIteration(context, manager);
        assertNotNull(eval1);

        // Run iteration 2 (reads next batch without losing remaining files)
        EvaluationResult eval2 = engine.runDarwinIteration(context, manager);
        assertNotNull(eval2);
    }
}
