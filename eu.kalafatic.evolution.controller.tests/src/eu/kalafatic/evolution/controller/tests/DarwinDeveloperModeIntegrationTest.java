package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.zip.ZipFile;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.FileChangeTracker;
import eu.kalafatic.evolution.controller.orchestration.ModeRouter;
import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ADarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngineFactory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DeveloperContextProvider;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DeveloperEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.util.ModeRecognizer;
import eu.kalafatic.evolution.controller.tools.ITool;
import eu.kalafatic.evolution.controller.tools.ToolFactory;
import eu.kalafatic.evolution.controller.tools.java.JavaDevelopmentTools;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.servers.mcp.tools.ToolRegistry;

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
        new File(repoDir, "src/main/java/com/example").mkdirs();
        File pom = new File(repoDir, "pom.xml");
        Files.writeString(pom.toPath(), "<project><dependencies><dependency><groupId>junit</groupId><artifactId>junit</artifactId><version>4.13.2</version></dependency></dependencies></project>");

        File mainJava = new File(repoDir, "src/main/java/com/example/Calculator.java");
        Files.writeString(mainJava.toPath(),
            "package com.example;\n" +
            "public class Calculator implements ICalculator {\n" +
            "    private int result;\n" +
            "    public int add(int a, int b) {\n" +
            "        return a + b;\n" +
            "    }\n" +
            "    public int calculate() {\n" +
            "        return add(1, 2);\n" +
            "    }\n" +
            "}\n"
        );

        File ifaceJava = new File(repoDir, "src/main/java/com/example/ICalculator.java");
        Files.writeString(ifaceJava.toPath(),
            "package com.example;\n" +
            "public interface ICalculator {\n" +
            "    int add(int a, int b);\n" +
            "}\n"
        );

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
    public void testDeveloperEngineInstructionsAndBehaviorPrompt() {
        IterationMemoryService memoryService = new IterationMemoryService(tempDir);
        DeveloperEngine devEngine = new DeveloperEngine(context, memoryService, null);
        String instructions = devEngine.getAgentInstructions();
        assertNotNull("Instructions should not be null", instructions);
        assertTrue("Should contain role", instructions.contains("Darwin Autonomous Developer"));
        assertTrue("Should instruct to understand before modifying", instructions.contains("Understand the requested objective"));
        assertTrue("Should instruct to inspect implementation", instructions.contains("Inspect relevant existing implementation"));
        assertTrue("Should instruct to retrieve fresh compiler diagnostics", instructions.contains("Retrieve fresh compiler diagnostics"));
        assertTrue("Should instruct minimal targeted changes", instructions.contains("Make minimal, targeted changes"));
    }

    @Test
    public void testDeveloperContextProviderProgressiveContext() throws Exception {
        DeveloperContextProvider provider = new DeveloperContextProvider();

        // 1. Repository target resolution
        assertEquals("Should identify REPOSITORY", DeveloperContextProvider.TargetType.REPOSITORY, provider.resolveTargetType(repoDir, context));

        // 2. Folder target resolution
        File plainFolder = new File(tempDir, "plain-folder");
        plainFolder.mkdirs();
        assertEquals("Should identify FOLDER", DeveloperContextProvider.TargetType.FOLDER, provider.resolveTargetType(plainFolder, context));

        // 3. Project target resolution (.project file)
        File projectFolder = new File(tempDir, "eclipse-project");
        projectFolder.mkdirs();
        Files.writeString(new File(projectFolder, ".project").toPath(), "<projectDescription><name>test</name></projectDescription>");
        assertEquals("Should identify PROJECT", DeveloperContextProvider.TargetType.PROJECT, provider.resolveTargetType(projectFolder, context));

        // 4. Initial context building without Genome/INTENT (graceful operation)
        String initialCtx = provider.buildInitialContext(repoDir, context, "Implement multiply");
        assertNotNull("Context should not be null", initialCtx);
        assertTrue("Should contain Target Type", initialCtx.contains("Target Type: REPOSITORY"));
        assertTrue("Should contain Build System", initialCtx.contains("Build System: Maven"));
        assertTrue("Should list key config files", initialCtx.contains("pom.xml"));
        assertTrue("Should list directory overview", initialCtx.contains("src/"));
    }

    @Test
    public void testJavaDevelopmentToolsInspectionAndHierarchy() throws Exception {
        // 1. java.inspect_class
        ITool inspectClassTool = ToolFactory.getTool("java.inspect_class");
        assertNotNull("InspectClassTool should be registered in ToolFactory", inspectClassTool);
        String inspectClassOut = inspectClassTool.execute("Calculator", repoDir, context);
        JSONObject classJson = new JSONObject(inspectClassOut);
        assertTrue("Class inspection should succeed", classJson.optBoolean("success"));
        assertEquals("com.example", classJson.optString("package"));
        assertEquals("Calculator", classJson.optString("name"));
        JSONArray methods = classJson.optJSONArray("methods");
        assertNotNull("Methods should be present", methods);
        assertTrue("Methods should have at least add and calculate", methods.length() >= 2);

        // 2. java.inspect_method
        ITool inspectMethodTool = ToolFactory.getTool("java.inspect_method");
        assertNotNull("InspectMethodTool should be registered", inspectMethodTool);
        String inspectMethodOut = inspectMethodTool.execute("{\"className\":\"Calculator\",\"methodName\":\"add\"}", repoDir, context);
        JSONObject methodJson = new JSONObject(inspectMethodOut);
        assertTrue("Method inspection should succeed", methodJson.optBoolean("success"));
        JSONArray matches = methodJson.optJSONArray("matches");
        assertTrue("Matches should not be empty", matches.length() > 0);
        assertTrue("Method body should contain return a + b", matches.getJSONObject(0).optString("body").contains("return a + b;"));

        // 3. java.find_callers
        ITool findCallersTool = ToolFactory.getTool("java.find_callers");
        assertNotNull("FindCallersTool should be registered", findCallersTool);
        String callersOut = findCallersTool.execute("{\"methodName\":\"add\"}", repoDir, context);
        JSONObject callersJson = new JSONObject(callersOut);
        assertTrue("Callers lookup should succeed", callersJson.optBoolean("success"));
        JSONArray callers = callersJson.optJSONArray("callers");
        assertTrue("Caller should be calculate()", callers.length() > 0);
        assertEquals("calculate", callers.getJSONObject(0).optString("callerMethod"));

        // 4. java.find_implementations
        ITool findImplsTool = ToolFactory.getTool("java.find_implementations");
        assertNotNull("FindImplementationsTool should be registered", findImplsTool);
        String implsOut = findImplsTool.execute("{\"interfaceName\":\"ICalculator\"}", repoDir, context);
        JSONObject implsJson = new JSONObject(implsOut);
        assertTrue("Implementation lookup should succeed", implsJson.optBoolean("success"));
        JSONArray impls = implsJson.optJSONArray("implementations");
        assertTrue("Calculator should implement ICalculator", impls.length() > 0);
        assertEquals("Calculator", impls.getJSONObject(0).optString("className"));

        // 5. java.find_references
        ITool findRefsTool = ToolFactory.getTool("java.find_references");
        assertNotNull("FindReferencesTool should be registered", findRefsTool);
        String refsOut = findRefsTool.execute("{\"target\":\"Calculator\"}", repoDir, context);
        JSONObject refsJson = new JSONObject(refsOut);
        assertTrue("References lookup should succeed", refsJson.optBoolean("success"));
        assertTrue("References count should be >= 1", refsJson.optInt("matchCount") >= 1);

        // 6. java.get_project_structure
        ITool structTool = ToolFactory.getTool("java.get_project_structure");
        assertNotNull("GetProjectStructureTool should be registered", structTool);
        String structOut = structTool.execute("", repoDir, context);
        JSONObject structJson = new JSONObject(structOut);
        assertTrue("Project structure should succeed", structJson.optBoolean("success"));
        boolean hasPkg = false;
        JSONArray pkgs = structJson.optJSONArray("packages");
        for (int i = 0; i < pkgs.length(); i++) {
            if ("com.example".equals(pkgs.getString(i))) {
                hasPkg = true;
                break;
            }
        }
        assertTrue("Packages should contain com.example", hasPkg);

        // 7. java.get_dependencies
        ITool depsTool = ToolFactory.getTool("java.get_dependencies");
        assertNotNull("GetDependenciesTool should be registered", depsTool);
        String depsOut = depsTool.execute("", repoDir, context);
        JSONObject depsJson = new JSONObject(depsOut);
        assertTrue("Dependencies lookup should succeed", depsJson.optBoolean("success"));
        assertTrue("Should detect junit dependency from pom.xml", depsOut.contains("junit"));

        // 8. java.format
        ITool formatTool = ToolFactory.getTool("java.format");
        assertNotNull("FormatTool should be registered", formatTool);
        String formatOut = formatTool.execute("Calculator", repoDir, context);
        JSONObject formatJson = new JSONObject(formatOut);
        assertTrue("Format tool should succeed", formatJson.optBoolean("success"));

        // 9. java.get_diagnostics
        ITool diagTool = ToolFactory.getTool("java.get_diagnostics");
        assertNotNull("GetDiagnosticsTool should be registered", diagTool);
        String diagOut = diagTool.execute("", repoDir, context);
        JSONObject diagJson = new JSONObject(diagOut);
        assertTrue("Diagnostics retrieval should succeed", diagJson.optBoolean("success"));
        assertEquals(0, diagJson.optInt("errorCount"));
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
