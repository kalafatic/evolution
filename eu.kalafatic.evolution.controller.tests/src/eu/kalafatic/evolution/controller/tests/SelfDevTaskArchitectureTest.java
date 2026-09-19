package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.selfdev.AbstractSelfDevTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BuildEvoTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevBootstrapController;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskStatus;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TychoEvoRcpBuilder;

public class SelfDevTaskArchitectureTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File mockRepoRoot;
    private SelfDevOrchestrator orchestrator;

    @Before
    public void setUp() throws Exception {
        mockRepoRoot = tempFolder.newFolder("mockEvoRepo");

        // Create pom.xml in mock repo root
        File rootPom = new File(mockRepoRoot, "pom.xml");
        try (FileWriter fw = new FileWriter(rootPom)) {
            fw.write("<project><modelVersion>4.0.0</modelVersion><groupId>eu.kalafatic.evolution</groupId><artifactId>eu.kalafatic.evolution.aggregator</artifactId><version>2.6.5-SNAPSHOT</version><packaging>pom</packaging></project>");
        }

        // Create required package directories for source snapshot integrity check
        File controllerModelPkg = new File(mockRepoRoot, "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/forge/model/llm");
        controllerModelPkg.mkdirs();

        File supervisorPkg = new File(mockRepoRoot, "eu.kalafatic.evolution.supervisor/src/main/java/eu/kalafatic/evolution/supervisor");
        supervisorPkg.mkdirs();

        orchestrator = new SelfDevOrchestrator(mockRepoRoot, null);
    }

    @Test
    public void testTaskRegistryCompleteness() {
        Map<String, SelfDevTask> registry = orchestrator.getTaskRegistry();
        assertNotNull(registry);

        String[] requiredTaskIds = {
            "LLM", "GIT_EVO", "MAVEN_EVO", "COPY_SUPERVISOR", "BUILD_SUPERVISOR_LOCAL",
            "SUPERVISOR", "GIT_SUPERVISOR", "MAVEN_SUPERVISOR", "GENOME", "PERMISSIONS",
            "COPY", "BUILD_EVO", "BUILD_SUPERVISOR", "EXPORT_EVO", "EXPORT_SUPERVISOR",
            "START_SUPERVISOR", "START_EVO", "START_EVO_SUPERVISOR", "SUPERVISOR_LOOP", "SELF_DEV_LOOP", "STOP_EVO_SUPERVISOR"
        };

        for (String taskId : requiredTaskIds) {
            assertTrue("Task ID [" + taskId + "] must be registered in SelfDevOrchestrator", registry.containsKey(taskId));
            assertNotNull("Task ID [" + taskId + "] implementation must not be null", registry.get(taskId));
            assertEquals("Task ID must match registered key", taskId, registry.get(taskId).getId());
        }
    }

    @Test
    public void testMavenEvoTaskSemanticsAndSkipTests() throws Exception {
        SelfDevTask task = orchestrator.getTaskRegistry().get("MAVEN_EVO");
        assertNotNull(task);
        assertTrue(task instanceof BuildEvoTask);

        Field builderField = BuildEvoTask.class.getDeclaredField("builder");
        builderField.setAccessible(true);
        Object builderObj = builderField.get(task);
        assertTrue(builderObj instanceof TychoEvoRcpBuilder);

        TychoEvoRcpBuilder evoBuilder = (TychoEvoRcpBuilder) builderObj;
        assertTrue("TychoEvoRcpBuilder must default skipTests to true for MAVEN_EVO", evoBuilder.isSkipTests());
    }

    @Test
    public void testTaskDependencyEnforcement() {
        SelfDevTask buildEvo = orchestrator.getTaskRegistry().get("BUILD_EVO");
        assertNotNull(buildEvo);
        assertTrue("BUILD_EVO must depend on COPY", buildEvo.getDependencies().contains("COPY"));

        SelfDevTask exportEvo = orchestrator.getTaskRegistry().get("EXPORT_EVO");
        assertNotNull(exportEvo);
        assertTrue("EXPORT_EVO must depend on BUILD_EVO", exportEvo.getDependencies().contains("BUILD_EVO"));

        // Executing BUILD_EVO without executing COPY first should automatically execute COPY dependency or block if copy fails
        TaskResult res = orchestrator.executeTaskWithDependencies("BUILD_EVO");
        assertNotNull(res);
        // Because COPY ran first, if copy succeeded or failed, result reflects topological dependency execution
        assertTrue("Task result must be recorded in context", orchestrator.getContext().getTaskResult("COPY") != null);
    }

    @Test
    public void testSequentialTaskExecution() {
        SelfDevBootstrapController controller = new SelfDevBootstrapController(orchestrator);

        List<String> executionOrder = new ArrayList<>();
        SelfDevTask fakeTaskA = new AbstractSelfDevTask("FAKE_A", "Fake Task A") {
            @Override
            protected TaskResult run(SelfDevContext context) throws Exception {
                executionOrder.add("FAKE_A");
                return TaskResult.success(id, "A ok");
            }
        };

        SelfDevTask fakeTaskB = new AbstractSelfDevTask("FAKE_B", "Fake Task B") {
            @Override
            protected TaskResult run(SelfDevContext context) throws Exception {
                executionOrder.add("FAKE_B");
                return TaskResult.success(id, "B ok");
            }
        };

        fakeTaskB.addDependency("FAKE_A");

        try {
            Field regField = SelfDevOrchestrator.class.getDeclaredField("taskRegistry");
            regField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, SelfDevTask> reg = (Map<String, SelfDevTask>) regField.get(orchestrator);
            reg.put("FAKE_A", fakeTaskA);
            reg.put("FAKE_B", fakeTaskB);

            TaskResult resB = controller.runTask("FAKE_B");
            assertNotNull(resB);
            assertTrue(resB.isSuccess());
            assertEquals(2, executionOrder.size());
            assertEquals("FAKE_A", executionOrder.get(0));
            assertEquals("FAKE_B", executionOrder.get(1));

        } catch (Exception e) {
            fail("Exception setting up mock tasks: " + e.getMessage());
        }
    }

    @Test
    public void testBuildDirectoryIsolation() {
        SelfDevContext context = orchestrator.getContext();
        assertNotNull(context);
        File repoRoot = context.getRepositoryRoot();
        File sourceDir = context.getPreparedReactorDirectory();

        assertNotEquals("Source reactor directory must be isolated from canonical Git repo",
                repoRoot.getAbsolutePath(), sourceDir.getAbsolutePath());
        assertTrue("Prepared reactor directory must be under workspace run directory",
                sourceDir.getAbsolutePath().contains("self-dev"));
    }
}
