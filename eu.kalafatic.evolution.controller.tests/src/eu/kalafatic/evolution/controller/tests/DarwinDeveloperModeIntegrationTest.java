package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.zip.ZipFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.FileChangeTracker;
import eu.kalafatic.evolution.controller.orchestration.ModeRouter;
import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ADarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngineFactory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DeveloperEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.util.ModeRecognizer;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Integration and unit tests for Darwin DEVELOPER mode.
 */
public class DarwinDeveloperModeIntegrationTest {

    private File tempDir;
    private File repoDir;
    private Orchestrator orchestrator;
    private TaskContext context;
    private ModeRouter modeRouter;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("evo-developer-test-").toFile();
        repoDir = new File(tempDir, "mock-repo");
        repoDir.mkdirs();
        new File(repoDir, ".git").mkdirs();
        new File(repoDir, "src").mkdirs();
        File pom = new File(repoDir, "pom.xml");
        Files.writeString(pom.toPath(), "<project></project>");

        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId("test-dev-session");

        ChatSession session = OrchestrationFactory.eINSTANCE.createChatSession();
        session.setId("test-dev-session");
        session.setTargetPath(repoDir.getAbsolutePath());
        session.setTargetType("Repository");
        session.setOutputPath(new File(tempDir, "output").getAbsolutePath());

        orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        orchestrator.getAiChat().getSessions().add(session);

        context = new TaskContext(orchestrator, repoDir);
        context.setSessionId("test-dev-session");

        modeRouter = new ModeRouter();
    }

    @After
    public void tearDown() throws Exception {
        if (tempDir != null && tempDir.exists()) {
            deleteRecursively(tempDir);
        }
        SessionManager.getInstance().shutdownAll();
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    @Test
    public void testModeRouterRecognizesDeveloperMode() {
        PlatformMode mode1 = modeRouter.routeFast("mode: developer implement calculator feature", orchestrator);
        assertEquals("Should route to DEVELOPER mode", PlatformType.DEVELOPER, mode1.getType());

        PlatformMode mode2 = modeRouter.routeFast("mode: dev fix bug", orchestrator);
        assertEquals("Should route to DEVELOPER mode for 'mode: dev'", PlatformType.DEVELOPER, mode2.getType());

        PlatformMode mode3 = modeRouter.routeFast("developer", orchestrator);
        assertEquals("Should route to DEVELOPER mode for 'developer'", PlatformType.DEVELOPER, mode3.getType());
    }

    @Test
    public void testModeRecognizerDetectsDeveloperMode() {
        context.setPlatformMode(new PlatformMode(PlatformType.DEVELOPER, null, 1, false));
        assertTrue("isDeveloperMode should return true", ModeRecognizer.isDeveloperMode(context));
        assertEquals("determineMode should return DEVELOPER", "DEVELOPER", new ModeRecognizer(null).determineMode(context));
        assertEquals("determineType should return PlatformType.DEVELOPER", PlatformType.DEVELOPER, ModeRecognizer.determineType(context));
    }

    @Test
    public void testDarwinEngineFactoryCreatesDeveloperEngine() {
        IterationMemoryService memoryService = new IterationMemoryService(tempDir);
        ADarwinEngine engine = DarwinEngineFactory.createEngine(PlatformType.DEVELOPER, context, memoryService, null);
        assertNotNull("Engine should not be null", engine);
        assertTrue("Engine should be instance of DeveloperEngine", engine instanceof DeveloperEngine);
        assertEquals("PlatformType should be DEVELOPER", PlatformType.DEVELOPER, engine.getPlatformType());
    }

    @Test
    public void testDeveloperEnginePublishesToMediatedOutput() throws Exception {
        IterationMemoryService memoryService = new IterationMemoryService(tempDir);
        DeveloperEngine devEngine = new DeveloperEngine(context, memoryService, null);

        // Record a mock file change
        File testFile = new File(repoDir, "src/Main.java");
        Files.writeString(testFile.toPath(), "public class Main {}");
        context.getFileChangeTracker().recordChange("src/Main.java", FileChangeTracker.ChangeType.NEW);

        // Execute Mediated output publishing
        devEngine.publishToMediatedOutput("Implement Main class", null);

        File outputDir = new File(tempDir, "output");
        assertTrue("Output directory should exist", outputDir.exists());

        File[] files = outputDir.listFiles((dir, name) -> name.startsWith("mediated_export_") && name.endsWith(".zip"));
        assertNotNull("Zip files array should not be null", files);
        assertTrue("Should have created at least one mediated export ZIP file", files.length > 0);

        try (ZipFile zip = new ZipFile(files[0])) {
            assertNotNull("ZIP should contain prompt.md", zip.getEntry("prompt.md"));
            assertNotNull("ZIP should contain README.md", zip.getEntry("README.md"));
        }
    }
}
