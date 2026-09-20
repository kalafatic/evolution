package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.selfdev.ArtifactType;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BuildArtifact;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BuildEvoTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvoRcpDeployer;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvoRcpRuntime;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvoRcpVerifier;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ExportEvoTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StartEvoTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskStatus;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TychoEvoRcpBuilder;

public class SelfDevStandalonePipelineTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File mockRepoRoot;
    private File mockReactorDir;
    private SelfDevContext context;

    @Before
    public void setUp() throws Exception {
        mockRepoRoot = tempFolder.newFolder("mockRepo");
        new File(mockRepoRoot, ".git").mkdirs();
        try (FileWriter fw = new FileWriter(new File(mockRepoRoot, "pom.xml"))) {
            fw.write("<project><modelVersion>4.0.0</modelVersion><groupId>eu.kalafatic.evolution</groupId><artifactId>root</artifactId><version>1.0</version></project>");
        }

        File runDir = tempFolder.newFolder("selfDevRun");
        mockReactorDir = new File(runDir, "source");
        mockReactorDir.mkdirs();
        try (FileWriter fw = new FileWriter(new File(mockReactorDir, "pom.xml"))) {
            fw.write("<project><modelVersion>4.0.0</modelVersion><groupId>eu.kalafatic.evolution</groupId><artifactId>prepared</artifactId><version>1.0</version></project>");
        }

        context = new SelfDevContext(mockRepoRoot, runDir, null);
    }

    @Test
    public void testBuildEvoPrevalidationFailsOnNullContext() throws Exception {
        BuildEvoTask task = new BuildEvoTask("BUILD_EVO");
        TaskResult res = task.execute(null);
        assertEquals(TaskStatus.FAILED, res.getStatus());
        assertTrue(res.getMessage().contains("null"));
    }

    @Test
    public void testBuildEvoPrevalidationFailsWhenOnGitRepoRoot() throws Exception {
        SelfDevContext invalidContext = new SelfDevContext(mockRepoRoot, mockRepoRoot, null);
        BuildEvoTask task = new BuildEvoTask("BUILD_EVO");
        TaskResult res = task.execute(invalidContext);
        assertEquals(TaskStatus.FAILED, res.getStatus());
        assertTrue(res.getMessage().contains("Direct build on Git root is forbidden") || res.getMessage().contains("same directory"));
    }

    @Test
    public void testBuildEvoPrevalidationSuccess() throws Exception {
        BuildEvoTask task = new BuildEvoTask("BUILD_EVO") {
            @Override
            protected TaskResult run(SelfDevContext ctx) throws Exception {
                // Return success for standalone task testing
                BuildArtifact artifact = new BuildArtifact(ArtifactType.EVO_RCP, new File(ctx.getPreparedReactorDirectory(), "target"), "HEAD", "linux", null);
                ctx.recordArtifact(artifact);
                return TaskResult.success(id, "Build successful");
            }
        };

        TaskResult res = task.execute(context);
        assertEquals(TaskStatus.SUCCESS, res.getStatus());
        assertNotNull(context.getArtifact(ArtifactType.EVO_RCP));
    }

    @Test
    public void testExportEvoPrevalidationFailsWhenReactorMissing() throws Exception {
        File emptyRunDir = tempFolder.newFolder("emptyRun");
        SelfDevContext emptyCtx = new SelfDevContext(mockRepoRoot, emptyRunDir, null);
        // Remove prepared reactor pom.xml
        new File(emptyCtx.getPreparedReactorDirectory(), "pom.xml").delete();

        ExportEvoTask task = new ExportEvoTask("EXPORT_EVO");
        TaskResult res = task.execute(emptyCtx);
        assertEquals(TaskStatus.FAILED, res.getStatus());
        assertTrue(res.getMessage().contains("missing pom.xml") || res.getMessage().contains("does not exist"));
    }

    @Test
    public void testExportEvoPrevalidationSuccess() throws Exception {
        ExportEvoTask task = new ExportEvoTask("EXPORT_EVO") {
            @Override
            protected TaskResult run(SelfDevContext ctx) throws Exception {
                File exportFile = new File(ctx.getExportDirectory(), "evolution-linux.gtk.x86_64.zip");
                try (FileOutputStream fos = new FileOutputStream(exportFile)) {
                    fos.write("dummy zip content".getBytes());
                }
                BuildArtifact artifact = new BuildArtifact(ArtifactType.EVO_RCP, exportFile, "HEAD", "linux", null);
                ctx.recordArtifact(artifact);
                return TaskResult.success(id, "Export successful");
            }
        };

        TaskResult res = task.execute(context);
        assertEquals(TaskStatus.SUCCESS, res.getStatus());
        assertNotNull(context.getArtifact(ArtifactType.EVO_RCP));
        assertTrue(context.getArtifact(ArtifactType.EVO_RCP).getPath().exists());
    }

    @Test
    public void testDeployerSupportsZipAndDirectory() throws Exception {
        File zipFile = new File(tempFolder.getRoot(), "product.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("evo"));
            zos.write("#!/bin/sh\necho hello".getBytes());
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("plugins/eu.kalafatic.evolution.view_1.0.jar"));
            zos.write("jar content".getBytes());
            zos.closeEntry();
        }

        BuildArtifact zipArtifact = new BuildArtifact(ArtifactType.EVO_RCP, zipFile, "HEAD", "linux", null);

        EvoRcpDeployer deployer = new EvoRcpDeployer();
        TaskResult res = deployer.deploy(context, zipArtifact);
        assertTrue(res.isSuccess());

        File evoRuntimeSubDir = new File(context.getRuntimeDirectory(), "evo");
        assertTrue(new File(evoRuntimeSubDir, "plugins").exists());
        assertTrue(new File(evoRuntimeSubDir, "evo").exists());
    }

    @Test
    public void testStartEvoPrevalidationFailsIfNoArtifact() throws Exception {
        File emptyRunDir = tempFolder.newFolder("emptyRun2");
        SelfDevContext emptyCtx = new SelfDevContext(mockRepoRoot, emptyRunDir, null);

        StartEvoTask task = new StartEvoTask("START_EVO");
        TaskResult res = task.execute(emptyCtx);
        assertEquals(TaskStatus.FAILED, res.getStatus());
        assertTrue(res.getMessage().contains("missing required EVO RCP build artifact"));
    }

    @Test
    public void testPortOffsetAndIsolatedPortInDebugMode() {
        context.setDebugMode(false);
        int normalPort = context.getEffectiveServerPort();
        assertEquals(48081, normalPort);

        context.setDebugMode(true);
        int debugPort = context.getEffectiveServerPort();
        assertEquals(48091, debugPort);
        assertEquals(10, context.getPortOffset());
    }

    @Test
    public void testEvoVerifierDetectsReadyMarkersInLogFiles() throws Exception {
        File logDir = context.getLogDirectory();
        File runtimeLog = new File(logDir, "evo_runtime.log");
        try (FileWriter fw = new FileWriter(runtimeLog)) {
            fw.write("Evolution background server started on port 48091\nApplication READY\n");
        }

        Process dummyProc = new ProcessBuilder("sleep", "10").start();
        try {
            EvoRcpVerifier verifier = new EvoRcpVerifier();
            TaskResult res = verifier.verifyReady(dummyProc, context.getRuntimeDirectory(), logDir, 5);
            assertTrue("Verifier should detect ready marker in runtime log", res.isSuccess());
        } finally {
            dummyProc.destroyForcibly();
        }
    }
}
