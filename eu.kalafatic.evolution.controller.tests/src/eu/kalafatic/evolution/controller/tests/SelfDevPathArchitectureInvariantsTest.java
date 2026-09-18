package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BuildArtifact;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TychoEvoRcpBuilder;
import eu.kalafatic.evolution.controller.resource.EvoPath;
import eu.kalafatic.evolution.controller.resource.ProductDefinition;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

public class SelfDevPathArchitectureInvariantsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ResourceManager resourceManager;

    @Before
    public void setUp() {
        resourceManager = ResourceManager.getInstance();
        resourceManager.refresh();
    }

    @Test
    public void test1CanonicalRepositoryResolution() {
        File repo = resourceManager.getEvoGitRepository().toFile();
        assertNotNull("Canonical Git repository should not be null", repo);
        assertTrue("Canonical repository path must contain 'git' or 'evolution'",
                repo.getAbsolutePath().toLowerCase().contains("git") || repo.getAbsolutePath().toLowerCase().contains("evolution"));
    }

    @Test
    public void test2NoRuntimeFallbackInResourceManager() {
        File evoSource = resourceManager.getEvoSource().toFile();
        File evoReactor = resourceManager.getEvoReactor().toFile();
        File evoBuild = resourceManager.getEvoBuildOutput().toFile();
        File evoExport = resourceManager.getEvoExport().toFile();

        assertFalse("ResourceManager getEvoSource must not point to workspace/runtime/sources",
                evoSource.getAbsolutePath().contains("workspace" + File.separator + "runtime" + File.separator + "sources"));
        assertFalse("ResourceManager getEvoReactor must not point to workspace/runtime/sources",
                evoReactor.getAbsolutePath().contains("workspace" + File.separator + "runtime" + File.separator + "sources"));
        assertFalse("ResourceManager getEvoBuildOutput must not point to workspace/runtime/builds",
                evoBuild.getAbsolutePath().contains("workspace" + File.separator + "runtime" + File.separator + "builds"));
        assertFalse("ResourceManager getEvoExport must not point to workspace/runtime/exports",
                evoExport.getAbsolutePath().contains("workspace" + File.separator + "runtime" + File.separator + "exports"));

        assertEquals("ResourceManager source/reactor/build/export accessors should resolve to canonical repository",
                resourceManager.getEvoGitRepository().toFile().getAbsoluteFile(), evoSource.getAbsoluteFile());
    }

    @Test
    public void test3SelfDevContextPathIsolation() {
        File repoRoot = resourceManager.getEvoGitRepository().toFile();
        SelfDevContext context = new SelfDevContext(repoRoot, resourceManager.getOrchestrator());

        assertNotEquals("Prepared reactor directory must not equal repository root",
                repoRoot.getAbsoluteFile(), context.getPreparedReactorDirectory().getAbsoluteFile());
        assertTrue("Prepared reactor directory must reside under self-dev run/source",
                context.getPreparedReactorDirectory().getAbsolutePath().contains("self-dev"));
        assertTrue("Build directory must reside under self-dev run/build",
                context.getBuildDirectory().getAbsolutePath().contains("self-dev"));
        assertTrue("Export directory must reside under self-dev run/export",
                context.getExportDirectory().getAbsolutePath().contains("self-dev"));
    }

    @Test
    public void test4MavenIsolationRejectsCanonicalRepoAsReactor() {
        File repoRoot = resourceManager.getEvoGitRepository().toFile();
        // Create context where prepared reactor equals repository root
        SelfDevContext context = new SelfDevContext(repoRoot, resourceManager.getOrchestrator()) {
            @Override
            public File getPreparedReactorDirectory() {
                return getRepositoryRoot();
            }
        };

        TychoEvoRcpBuilder builder = new TychoEvoRcpBuilder();
        TaskResult buildRes = builder.build(context);

        assertNotNull(buildRes);
        assertFalse("TychoEvoRcpBuilder must reject building directly against canonical Git repository", buildRes.isSuccess());
        assertTrue(buildRes.getMessage().contains("BUILD REACTOR VALIDATION FAILED"));
    }

    @Test
    public void test5And6ExportPathPlacement() throws Exception {
        File mockRunDir = tempFolder.newFolder("run_test");
        File mockRepo = tempFolder.newFolder("mockRepo");
        File mockSource = new File(mockRunDir, "source");
        File mockExport = new File(mockRunDir, "export");
        mockSource.mkdirs();
        mockExport.mkdirs();

        // Create pom.xml in source
        File pom = new File(mockSource, "pom.xml");
        try (FileWriter fw = new FileWriter(pom)) {
            fw.write("<project><modelVersion>4.0.0</modelVersion></project>");
        }

        // Create target/products with a dummy product archive in source
        File productsDir = new File(mockSource, "eu.kalafatic.evolution.repository/target/products");
        productsDir.mkdirs();
        File dummyZip = new File(productsDir, "evolution-win32.win32.x86_64.zip");
        try (FileWriter fw = new FileWriter(dummyZip)) {
            fw.write("dummy archive content");
        }

        SelfDevContext context = new SelfDevContext(mockRepo, mockRunDir, resourceManager.getOrchestrator());

        TychoEvoRcpBuilder builder = new TychoEvoRcpBuilder();
        TychoEvoRcpBuilder.ProductDefinition prodDef = new TychoEvoRcpBuilder.ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);
        TychoEvoRcpBuilder.TargetPlatform platform = new TychoEvoRcpBuilder.TargetPlatform("win32", "win32", "x86_64", "zip", "-Pwindows");

        File foundProduct = builder.findExactExportedProduct(mockSource, prodDef, platform, context);
        assertNotNull("Product should be discovered under source target/products", foundProduct);

        // Verify exportProduct places/copies product to export directory
        TaskResult exportRes = builder.exportProduct(context);
        assertNotNull(exportRes);
        assertTrue("Export product must succeed when artifact exists in reactor", exportRes.isSuccess());
        assertNotNull("Export artifact must be recorded", exportRes.getArtifact());

        File exportedArtifact = exportRes.getArtifact().getPath();
        assertTrue("Exported artifact must reside inside SelfDevContext exportDirectory",
                exportedArtifact.getAbsolutePath().startsWith(context.getExportDirectory().getAbsolutePath()));
    }

    @Test
    public void test7NoPathGuessingOnInvalidDirectory() {
        File nonExistentDir = new File(tempFolder.getRoot(), "does_not_exist_source");
        SelfDevContext context = new SelfDevContext(nonExistentDir, resourceManager.getOrchestrator()) {
            @Override
            public File getPreparedReactorDirectory() {
                return nonExistentDir;
            }
        };

        TychoEvoRcpBuilder builder = new TychoEvoRcpBuilder();
        TaskResult res = builder.build(context);
        assertNotNull(res);
        assertFalse("Build must fail when prepared reactor directory does not exist", res.isSuccess());
        assertTrue(res.getMessage().contains("BUILD REACTOR VALIDATION FAILED"));
    }

    @Test
    public void test8ProductDefinitionResolutionFromCanonicalRepo() {
        ProductDefinition prodDef = resourceManager.getProductDefinition();
        assertNotNull("ProductDefinition must not be null", prodDef);
        assertNotNull("Product ID must be resolved", prodDef.getProductId());
        assertNotNull("Launcher name must be resolved", prodDef.getLauncherName());
        assertEquals("Repository module name should match", "eu.kalafatic.evolution.repository", prodDef.getRepositoryModule());
    }
}
