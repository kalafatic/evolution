package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Set;

import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.selfdev.AbstractSelfDevTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ArtifactType;
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
        File repoRoot = findRepoRoot();
        SelfDevContext context = new SelfDevContext(repoRoot, null);

        assertNotNull(context.getSupervisorDirectory());
        assertTrue(context.getSupervisorDirectory().exists());
        assertTrue(context.getSupervisorDirectory().getName().endsWith("eu.kalafatic.evolution.supervisor"));

        assertNotNull(context.getGenomeDirectory());
        assertTrue(context.getGenomeDirectory().exists());
        assertTrue(context.getGenomeDirectory().getName().endsWith("eu.kalafatic.evolution.selfdev.genome"));
    }

    private File findRepoRoot() {
        File app = new File("/app");
        if (app.exists() && new File(app, "pom.xml").exists()) return app;
        File cur = new File(".").getAbsoluteFile();
        while (cur != null) {
            if (new File(cur, "pom.xml").exists() && new File(cur, "eu.kalafatic.evolution.supervisor").exists()) {
                return cur;
            }
            cur = cur.getParentFile();
        }
        return eu.kalafatic.evolution.controller.resource.ResourceManager.getInstance().getEvoGitRepository().toFile();
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
    public void testSupervisorRuntimeDiagnosticOutputAndCleanupOnFailure() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "test_sup_diag_" + System.currentTimeMillis());
        tempDir.mkdirs();
        try {
            File fakeJar = new File(tempDir, "eu.kalafatic.evolution.supervisor.jar");
            java.nio.file.Files.write(fakeJar.toPath(), "invalid jar content".getBytes());

            SelfDevContext context = new SelfDevContext(tempDir, null);
            BuildArtifact artifact = new BuildArtifact(ArtifactType.SUPERVISOR, fakeJar, "HEAD", null, null);
            context.recordArtifact(artifact);

            SupervisorRuntime runtime = new SupervisorRuntime();
            TaskResult res = runtime.start(context);

            assertEquals(TaskStatus.FAILED, res.getStatus());
            assertFalse(runtime.isAlive());
            assertNotNull("Diagnostic log snippet should be captured", res.getDiagnostics().get("logSnippet"));
            assertNotNull("Log file location should be recorded", res.getLogFile());
            assertTrue("Command should be recorded in result", res.getCommand() != null && res.getCommand().contains("java"));
        } catch (Exception e) {
            fail("Exception thrown during supervisor diagnostic test: " + e.getMessage());
        } finally {
            deleteRecursively(tempDir);
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
        File repoRoot = findRepoRoot();
        SelfDevContext context = new SelfDevContext(repoRoot, null);
        SelfDevOrchestrator orchestrator = new SelfDevOrchestrator(context, null, null);

        eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevPreflight preflight = new eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevPreflight();
        eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevPreflightResult result = preflight.executePreflight(context, orchestrator);

        assertTrue("Preflight must succeed for repository root", result.isSuccess());
        assertNotNull(result.getResolvedResources());
        assertTrue(new File(result.getResolvedResources().getRepositoryRoot(), "pom.xml").exists());
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
        SelfDevContext context = new SelfDevContext(null, null);
        File repoRoot = context.getRepositoryRoot();
        SelfDevOrchestrator orchestrator = new SelfDevOrchestrator(context, null, null);

        eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevTask copyTask = orchestrator.getTaskRegistry().get("COPY");
        assertNotNull(copyTask);

        // Preflight already ran on orchestrator creation
        assertNotNull(context.getResolvedResources());
        assertEquals(repoRoot.getAbsoluteFile().toPath().normalize().toFile(), context.getResolvedResources().getRepositoryRoot());
    }

    @Test
    public void testCopyDirectoryPreservesSourcePackageTargetAndExcludesBuildTarget() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "test_copy_target_" + System.currentTimeMillis());
        File srcRepo = new File(tempDir, "srcRepo");
        File dstRepo = new File(tempDir, "dstRepo");

        File buildTargetDir = new File(srcRepo, "target");
        buildTargetDir.mkdirs();
        new File(buildTargetDir, "build.log").createNewFile();

        File srcPackageTargetDir = new File(srcRepo, "src/eu/kalafatic/evolution/forge/model/target");
        srcPackageTargetDir.mkdirs();
        File forgeTargetJava = new File(srcPackageTargetDir, "ForgeTarget.java");
        forgeTargetJava.createNewFile();

        try {
            eu.kalafatic.evolution.controller.orchestration.selfdev.GitSourceProvider provider =
                new eu.kalafatic.evolution.controller.orchestration.selfdev.GitSourceProvider();
            provider.fetchSource(srcRepo, dstRepo);

            File copiedBuildLog = new File(dstRepo, "target/build.log");
            assertFalse("Build output target directory must be excluded", copiedBuildLog.exists());

            File copiedForgeTarget = new File(dstRepo, "src/eu/kalafatic/evolution/forge/model/target/ForgeTarget.java");
            assertTrue("Java source package 'target' under src/ must be copied", copiedForgeTarget.exists());
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void testPortOffsetInNormalAndDebugMode() {
        File repoRoot = new File(".").getAbsoluteFile();
        SelfDevContext context = new SelfDevContext(repoRoot, null);

        // Normal mode
        context.setDebugMode(false);
        assertFalse(context.isDebugMode());
        assertEquals("Port offset for normal run must be 0", 0, context.getPortOffset());

        int baseSupervisorPort = context.getResourceManager().getService("SUPERVISOR") != null ? context.getResourceManager().getService("SUPERVISOR").getPort() : 8089;
        int baseServerPort = context.getResourceManager().getService("SERVER") != null ? context.getResourceManager().getService("SERVER").getPort() : 48081;

        assertEquals(baseSupervisorPort, context.getEffectiveSupervisorPort());
        assertEquals(28080, context.getEffectiveSupervisorControlPort());
        assertEquals(baseServerPort, context.getEffectiveServerPort());

        // Debug mode
        context.setDebugMode(true);
        assertTrue(context.isDebugMode());
        assertEquals("Port offset for debug run must be 10", 10, context.getPortOffset());

        assertEquals(baseSupervisorPort + 10, context.getEffectiveSupervisorPort());
        assertEquals(28080 + 10, context.getEffectiveSupervisorControlPort());
        assertEquals(baseServerPort + 10, context.getEffectiveServerPort());

        // Verify base ResourceManager configuration was NOT modified
        assertEquals(baseSupervisorPort, context.getResourceManager().getService("SUPERVISOR").getPort());
        assertEquals(baseServerPort, context.getResourceManager().getService("SERVER").getPort());
    }

    @Test
    public void testSupervisorClientAndRuntimeEffectivePortResolution() {
        File repoRoot = new File(".").getAbsoluteFile();
        SelfDevContext context = new SelfDevContext(repoRoot, null);

        context.setDebugMode(true);
        eu.kalafatic.evolution.controller.orchestration.selfdev.SupervisorClient client =
                new eu.kalafatic.evolution.controller.orchestration.selfdev.SupervisorClient(context);

        assertTrue("SupervisorClient URL in debug mode must use offset port",
                client.getBaseUrl().contains(":" + context.getEffectiveSupervisorPort()));
    }

    private void deleteRecursively(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursively(c);
            }
        }
        f.delete();
    }
}
