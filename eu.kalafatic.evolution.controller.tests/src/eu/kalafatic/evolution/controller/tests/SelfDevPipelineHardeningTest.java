package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Set;

import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.selfdev.AbstractSelfDevTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BuildArtifact;
import eu.kalafatic.evolution.controller.orchestration.selfdev.MavenSupervisorBuilder;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SupervisorRuntime;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskStatus;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TychoEvoRcpBuilder;

public class SelfDevPipelineHardeningTest {

    @Test
    public void testAbsolutePathResolution() {
        File baseRuntime = new File("/runtime/dir").getAbsoluteFile();
        File absoluteConfigured = new File("/foo/bar").getAbsoluteFile();

        File resolved = SelfDevContext.resolvePath(baseRuntime, absoluteConfigured);
        assertEquals(absoluteConfigured, resolved);
        assertFalse(resolved.getAbsolutePath().contains("runtime"));
    }

    @Test
    public void testRelativePathResolution() {
        File projectRoot = new File("/project/root").getAbsoluteFile();
        File resolved = SelfDevContext.resolvePath(projectRoot, "sub/dir");
        assertEquals(new File(projectRoot, "sub/dir").getAbsoluteFile().toPath().normalize().toFile(), resolved);
    }

    @Test
    public void testStalePathRecoveryAndDiscovery() {
        File repoRoot = new File(".").getAbsoluteFile();
        SelfDevContext context = new SelfDevContext(repoRoot, null);

        assertNotNull(context.getSupervisorDirectory());
        assertTrue(context.getSupervisorDirectory().exists());
        assertTrue(context.getSupervisorDirectory().getName().endsWith("eu.kalafatic.evolution.supervisor"));

        assertNotNull(context.getGenomeDirectory());
        assertTrue(context.getGenomeDirectory().exists());
        assertTrue(context.getGenomeDirectory().getName().endsWith("eu.kalafatic.evolution.selfdev.genome"));
    }

    @Test
    public void testBuildExitCode0WithoutArtifactFails() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "test_build_exit0_" + System.currentTimeMillis());
        tempDir.mkdirs();
        try {
            SelfDevContext context = new SelfDevContext(tempDir, null);
            MavenSupervisorBuilder builder = new MavenSupervisorBuilder();

            BuildArtifact artifact = builder.getArtifact(context);
            assertNull("Empty directory must yield no artifact", artifact);
        } finally {
            tempDir.delete();
        }
    }

    @Test
    public void testDependencyFailurePropagationBlocksDependentTasks() {
        File repoRoot = new File(".").getAbsoluteFile();
        SelfDevContext context = new SelfDevContext(repoRoot, null);
        SelfDevOrchestrator orchestrator = new SelfDevOrchestrator(context, null, null);

        // Record a failure for BUILD_SUPERVISOR_LOCAL
        TaskResult localFail = TaskResult.failure("BUILD_SUPERVISOR_LOCAL", "Local compilation error", null);
        context.recordTaskResult(localFail);

        // Execute dependent tasks: BUILD_SUPERVISOR, EXPORT_SUPERVISOR, START_SUPERVISOR
        SelfDevTask exportTask = new AbstractSelfDevTask("EXPORT_SUPERVISOR", "Export Supervisor") {
            @Override
            protected TaskResult run(SelfDevContext ctx) throws Exception {
                return TaskResult.success(id, "Exported");
            }
        };
        exportTask.addDependency("BUILD_SUPERVISOR_LOCAL");

        TaskResult res = exportTask.execute(context);
        assertEquals(TaskStatus.BLOCKED, res.getStatus());
        assertTrue(res.getMessage().contains("BLOCKED: required dependency BUILD_SUPERVISOR_LOCAL failed"));
    }

    @Test
    public void testMissingLauncherPreconditionFailsStartupAndBlocksLoop() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "test_missing_launcher_" + System.currentTimeMillis());
        tempDir.mkdirs();
        try {
            SelfDevContext context = new SelfDevContext(tempDir, null);
            SupervisorRuntime runtime = new SupervisorRuntime();

            TaskResult res = runtime.start(context);
            assertEquals(TaskStatus.FAILED, res.getStatus());
            assertTrue(res.getMessage().contains("Supervisor JAR artifact pre-condition check failed"));
            assertFalse(runtime.isAlive());
        } finally {
            tempDir.delete();
        }
    }

    @Test
    public void testPreflightValidSourcePasses() {
        File repoRoot = new File(".").getAbsoluteFile();
        SelfDevContext context = new SelfDevContext(repoRoot, null);
        SelfDevOrchestrator orchestrator = new SelfDevOrchestrator(context, null, null);

        eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevPreflight preflight = new eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevPreflight();
        eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevPreflightResult result = preflight.executePreflight(context, orchestrator);

        assertTrue("Preflight must succeed for repository root", result.isSuccess());
        assertNotNull(result.getResolvedResources());
        assertTrue(new File(result.getResolvedResources().getSourceDirectory(), "pom.xml").exists());
    }

    @Test
    public void testPreflightDetectsInvalidRuntimeProductDirectory() {
        File productDir = new File(System.getProperty("java.io.tmpdir"), "runtime-eu.kalafatic.evolution.view/.product/evo");
        productDir.mkdirs();
        try {
            SelfDevContext context = new SelfDevContext(productDir, null);
            eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevPreflight preflight = new eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevPreflight();
            eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevPreflightResult result = preflight.executePreflight(context, null);

            // Since productDir is missing pom.xml, preflight either recovers a valid repo root or fails if ambiguous/missing
            assertNotNull(result);
            if (result.isSuccess()) {
                assertFalse("Recovered source directory must not be the product directory",
                        result.getResolvedResources().getSourceDirectory().getAbsolutePath().contains(".product"));
            }
        } finally {
            productDir.delete();
        }
    }

    @Test
    public void testPreflightPathPropagationToCopyTask() throws Exception {
        File repoRoot = new File(".").getAbsoluteFile();
        SelfDevContext context = new SelfDevContext(repoRoot, null);
        SelfDevOrchestrator orchestrator = new SelfDevOrchestrator(context, null, null);

        eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevTask copyTask = orchestrator.getTaskRegistry().get("COPY");
        assertNotNull(copyTask);

        // Preflight already ran on orchestrator creation
        assertNotNull(context.getResolvedResources());
        assertEquals(repoRoot.getAbsoluteFile().toPath().normalize().toFile(), context.getResolvedResources().getSourceDirectory());
    }
}
