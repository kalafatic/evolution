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

import eu.kalafatic.evolution.controller.orchestration.selfdev.BuildArtifact;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TychoEvoRcpBuilder;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TychoEvoRcpBuilder.ProductDefinition;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TychoEvoRcpBuilder.TargetPlatform;

public class TychoEvoRcpBuilderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private TychoEvoRcpBuilder builder;
    private File mockRepoRoot;

    @Before
    public void setUp() throws Exception {
        builder = new TychoEvoRcpBuilder();
        mockRepoRoot = tempFolder.newFolder("mockRepo");

        // Create root pom.xml for eu.kalafatic.evolution.aggregator
        File rootPom = new File(mockRepoRoot, "pom.xml");
        try (FileWriter fw = new FileWriter(rootPom)) {
            fw.write("<project><modelVersion>4.0.0</modelVersion><groupId>eu.kalafatic.evolution</groupId><artifactId>eu.kalafatic.evolution.aggregator</artifactId><version>2.6.5-SNAPSHOT</version><packaging>pom</packaging></project>");
        }

        // Create eu.kalafatic.evolution.repository module and evolution.product
        File repoDir = new File(mockRepoRoot, "eu.kalafatic.evolution.repository");
        repoDir.mkdirs();

        File repoPom = new File(repoDir, "pom.xml");
        try (FileWriter fw = new FileWriter(repoPom)) {
            fw.write("<project><modelVersion>4.0.0</modelVersion><artifactId>eu.kalafatic.evolution.repository</artifactId><build><plugins><plugin><configuration><products><product><id>evolution</id><rootFolder>evolution</rootFolder></product></products></configuration></plugin></plugins></build></project>");
        }

        File prodFile = new File(repoDir, "evolution.product");
        try (FileWriter fw = new FileWriter(prodFile)) {
            fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><product name=\"AI Evolution\" uid=\"evolution\" id=\"eu.kalafatic.evolution.view.product\"><launcher name=\"evo\"/></product>");
        }
    }


    @Test
    public void testTargetPlatformResolution() {
        SelfDevContext context = new SelfDevContext(mockRepoRoot, null);

        TargetPlatform platform = builder.resolveTargetPlatform(context);
        assertNotNull(platform);
        assertNotNull(platform.getOs());
        assertNotNull(platform.getWs());
        assertNotNull(platform.getArch());
        assertNotNull(platform.getProfile());
    }

    @Test
    public void testDeterministicArtifactSelection() throws Exception {
        File productsDir = new File(mockRepoRoot, "eu.kalafatic.evolution.repository/target/products");
        productsDir.mkdirs();

        File winZip = new File(productsDir, "evolution-win32.win32.x86_64.zip");
        try (FileOutputStream fos = new FileOutputStream(winZip)) {
            fos.write("dummy win zip content".getBytes());
        }

        ProductDefinition prodDef = new ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);
        TargetPlatform winPlatform = new TargetPlatform("win32", "win32", "x86_64", "zip", "-Pwindows");

        File found = builder.findExactExportedProduct(mockRepoRoot, prodDef, winPlatform, null);
        assertNotNull(found);
        assertEquals(winZip.getAbsoluteFile(), found.getAbsoluteFile());
    }

    @Test
    public void testExplodedDirectoryAndZipNoAmbiguity() throws Exception {
        File productsDir = new File(mockRepoRoot, "eu.kalafatic.evolution.repository/target/products");
        productsDir.mkdirs();

        // Exploded directory
        File explodedDir = new File(productsDir, "evolution");
        explodedDir.mkdirs();

        // Actual ZIP artifact
        File winZip = new File(productsDir, "evolution-win32.win32.x86_64.zip");
        try (FileOutputStream fos = new FileOutputStream(winZip)) {
            fos.write("dummy win zip content".getBytes());
        }

        ProductDefinition prodDef = new ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);
        TargetPlatform winPlatform = new TargetPlatform("win32", "win32", "x86_64", "zip", "-Pwindows");

        File found = builder.findExactExportedProduct(mockRepoRoot, prodDef, winPlatform, null);
        assertNotNull("Should discover the ZIP file without ambiguity error", found);
        assertEquals(winZip.getAbsoluteFile(), found.getAbsoluteFile());
    }

    @Test
    public void testOnlyExplodedDirectoryReturnsNotFoundForZipRequest() throws Exception {
        File productsDir = new File(mockRepoRoot, "eu.kalafatic.evolution.repository/target/products");
        productsDir.mkdirs();

        File explodedDir = new File(productsDir, "evolution");
        explodedDir.mkdirs();

        ProductDefinition prodDef = new ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);
        TargetPlatform winPlatform = new TargetPlatform("win32", "win32", "x86_64", "zip", "-Pwindows");

        File found = builder.findExactExportedProduct(mockRepoRoot, prodDef, winPlatform, null);
        assertNull("Should return null (product not found) when only exploded directory exists for ZIP request", found);
    }

    @Test
    public void testMultipleCandidateArtifactRejection() throws Exception {
        File productsDir = new File(mockRepoRoot, "eu.kalafatic.evolution.repository/target/products");
        productsDir.mkdirs();

        File zip1 = new File(productsDir, "evolution-win32.win32.x86_64.zip");
        try (FileOutputStream fos = new FileOutputStream(zip1)) { fos.write("zip1".getBytes()); }

        File zip2 = new File(productsDir, "evolution-win32.win32.x86_64-v2.zip");
        try (FileOutputStream fos = new FileOutputStream(zip2)) { fos.write("zip2".getBytes()); }

        ProductDefinition prodDef = new ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);
        TargetPlatform winPlatform = new TargetPlatform("win32", "win32", "x86_64", "zip", "-Pwindows");

        try {
            builder.findExactExportedProduct(mockRepoRoot, prodDef, winPlatform, null);
            fail("Expected IOException on ambiguous multiple candidate artifacts");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Multiple candidate exported products found"));
        }
    }

    @Test
    public void testUnrelatedZipFilteredOut() throws Exception {
        File productsDir = new File(mockRepoRoot, "eu.kalafatic.evolution.repository/target/products");
        productsDir.mkdirs();

        File targetZip = new File(productsDir, "evolution-win32.win32.x86_64.zip");
        try (FileOutputStream fos = new FileOutputStream(targetZip)) { fos.write("target zip".getBytes()); }

        File unrelatedZip = new File(productsDir, "other-product-win32.win32.x86_64.zip");
        try (FileOutputStream fos = new FileOutputStream(unrelatedZip)) { fos.write("unrelated zip".getBytes()); }

        ProductDefinition prodDef = new ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);
        TargetPlatform winPlatform = new TargetPlatform("win32", "win32", "x86_64", "zip", "-Pwindows");

        File found = builder.findExactExportedProduct(mockRepoRoot, prodDef, winPlatform, null);
        assertNotNull("Should select the matching product zip", found);
        assertEquals(targetZip.getAbsoluteFile(), found.getAbsoluteFile());
    }

    @Test
    public void testWrongPlatformZipFilteredOut() throws Exception {
        File productsDir = new File(mockRepoRoot, "eu.kalafatic.evolution.repository/target/products");
        productsDir.mkdirs();

        File linuxTar = new File(productsDir, "evolution-linux.gtk.x86_64.tar.gz");
        try (FileOutputStream fos = new FileOutputStream(linuxTar)) { fos.write("linux tar".getBytes()); }

        ProductDefinition prodDef = new ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);
        TargetPlatform winPlatform = new TargetPlatform("win32", "win32", "x86_64", "zip", "-Pwindows");

        File found = builder.findExactExportedProduct(mockRepoRoot, prodDef, winPlatform, null);
        assertNull("Should ignore Linux archive when Windows platform is requested", found);
    }

    @Test
    public void testMissingLauncherRejection() throws Exception {
        File dir = tempFolder.newFolder("noLauncher");
        ProductDefinition prodDef = new ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);
        TargetPlatform platform = new TargetPlatform("linux", "gtk", "x86_64", "tar.gz", "-Plinux");

        TaskResult res = builder.validateProductDeployment(dir, prodDef, platform);
        assertNotNull(res);
        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().contains("Launcher executable"));
    }

    @Test
    public void testMissingPluginsRejection() throws Exception {
        File dir = tempFolder.newFolder("noPlugins");
        new File(dir, "evo").createNewFile();
        new File(dir, "evo.ini").createNewFile();

        ProductDefinition prodDef = new ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);
        TargetPlatform platform = new TargetPlatform("linux", "gtk", "x86_64", "tar.gz", "-Plinux");

        TaskResult res = builder.validateProductDeployment(dir, prodDef, platform);
        assertNotNull(res);
        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().contains("plugins/"));
    }

    @Test
    public void testMissingConfigurationRejection() throws Exception {
        File dir = tempFolder.newFolder("noConfig");
        new File(dir, "evo").createNewFile();
        new File(dir, "evo.ini").createNewFile();

        File plugins = new File(dir, "plugins");
        plugins.mkdirs();
        new File(plugins, "eu.kalafatic.evolution.view_1.0.jar").createNewFile();

        ProductDefinition prodDef = new ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);
        TargetPlatform platform = new TargetPlatform("linux", "gtk", "x86_64", "tar.gz", "-Plinux");

        TaskResult res = builder.validateProductDeployment(dir, prodDef, platform);
        assertNotNull(res);
        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().contains("configuration/"));
    }

    @Test
    public void testValidWindowsDeployment() throws Exception {
        File dir = tempFolder.newFolder("validWinDir");
        new File(dir, "evo.exe").createNewFile();
        new File(dir, "evo.ini").createNewFile();

        File plugins = new File(dir, "plugins");
        plugins.mkdirs();
        createMockPlugins(plugins);

        File config = new File(dir, "configuration");
        config.mkdirs();
        File configIni = new File(config, "config.ini");
        try (FileWriter fw = new FileWriter(configIni)) {
            fw.write("eclipse.application=eu.kalafatic.evolution.view.application.Application\n");
        }

        ProductDefinition prodDef = new ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);
        TargetPlatform platform = new TargetPlatform("win32", "win32", "x86_64", "zip", "-Pwindows");

        TaskResult res = builder.validateProductDeployment(dir, prodDef, platform);
        assertNotNull(res);
        assertTrue(res.getMessage(), res.isSuccess());
    }

    @Test
    public void testValidLinuxDeployment() throws Exception {
        File dir = tempFolder.newFolder("validLinuxDir");
        new File(dir, "evo").createNewFile();
        new File(dir, "evo.ini").createNewFile();

        File plugins = new File(dir, "plugins");
        plugins.mkdirs();
        createMockPlugins(plugins);

        File config = new File(dir, "configuration");
        config.mkdirs();
        File configIni = new File(config, "config.ini");
        try (FileWriter fw = new FileWriter(configIni)) {
            fw.write("eclipse.product=eu.kalafatic.evolution.view.product\n");
        }

        ProductDefinition prodDef = new ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);
        TargetPlatform platform = new TargetPlatform("linux", "gtk", "x86_64", "tar.gz", "-Plinux");

        TaskResult res = builder.validateProductDeployment(dir, prodDef, platform);
        assertNotNull(res);
        assertTrue(res.getMessage(), res.isSuccess());
    }

    private void createMockPlugins(File pluginsDir) throws IOException {
        String[] requiredCoreBundles = {
            "eu.kalafatic.evolution.view_2.6.5.jar",
            "eu.kalafatic.evolution.controller_2.6.5.jar",
            "eu.kalafatic.evolution.model_2.6.5.jar",
            "eu.kalafatic.evolution.model.edit_2.6.5.jar",
            "eu.kalafatic.evolution.model.editor_2.6.5.jar",
            "eu.kalafatic.utils_2.6.5.jar",
            "eu.kalafatic.evolution.servers_2.6.5.jar",
            "eu.kalafatic.evolution.media_2.6.5.jar",
            "eu.kalafatic.evolution.creatic_2.6.5.jar",
            "eu.kalafatic.evolution.selfdev.genome_2.6.5.jar",
            "eu.kalafatic.evolution.forge.math_2.6.5.jar",
            "eu.kalafatic.evolution.forge.model_2.6.5.jar",
            "eu.kalafatic.evolution.forge.tokenizer_2.6.5.jar",
            "eu.kalafatic.evolution.forge.data_2.6.5.jar",
            "eu.kalafatic.evolution.forge.runtime_2.6.5.jar",
            "eu.kalafatic.evolution.forge.trainer_2.6.5.jar",
            "eu.kalafatic.evolution.forge.agent.api_2.6.5.jar",
            "eu.kalafatic.evolution.forge.observability_2.6.5.jar",
            "eu.kalafatic.evolution.forge.controller_2.6.5.jar",
            "eu.kalafatic.evolution.forge.agent_2.6.5.jar"
        };
        for (String b : requiredCoreBundles) {
            new File(pluginsDir, b).createNewFile();
        }
    }

    @Test
    public void testMissingControllerBundleRejection() throws Exception {
        File dir = tempFolder.newFolder("missingControllerDir");
        new File(dir, "evo").createNewFile();
        new File(dir, "evo.ini").createNewFile();

        File plugins = new File(dir, "plugins");
        plugins.mkdirs();
        new File(plugins, "eu.kalafatic.evolution.view_2.6.5.jar").createNewFile();

        File config = new File(dir, "configuration");
        config.mkdirs();
        File configIni = new File(config, "config.ini");
        try (FileWriter fw = new FileWriter(configIni)) {
            fw.write("eclipse.product=eu.kalafatic.evolution.view.product\n");
        }

        ProductDefinition prodDef = new ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);
        TargetPlatform platform = new TargetPlatform("linux", "gtk", "x86_64", "tar.gz", "-Plinux");

        TaskResult res = builder.validateProductDeployment(dir, prodDef, platform);
        assertNotNull(res);
        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().contains("eu.kalafatic.evolution.controller_*.jar"));
    }

    @Test
    public void testZipPathTraversalRejection() throws Exception {
        File zipFile = new File(tempFolder.getRoot(), "malicious.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            ZipEntry entry = new ZipEntry("../outside.txt");
            zos.putNextEntry(entry);
            zos.write("malicious payload".getBytes());
            zos.closeEntry();
        }

        ProductDefinition prodDef = new ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);
        TargetPlatform platform = new TargetPlatform("win32", "win32", "x86_64", "zip", "-Pwindows");

        TaskResult res = builder.validateProductDeployment(zipFile, prodDef, platform);
        assertNotNull(res);
        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().contains("ZIP path traversal"));
    }

    @Test
    public void testMavenTychoFailurePropagation() throws Exception {
        File emptyDir = tempFolder.newFolder("emptyRoot");
        SelfDevContext context = new SelfDevContext(emptyDir, null);

        TaskResult buildRes = builder.build(context);
        assertNotNull(buildRes);
        assertFalse(buildRes.isSuccess());
        assertTrue(buildRes.getMessage().contains("BUILD REACTOR VALIDATION FAILED") || buildRes.getMessage().contains("pom.xml not found"));

        TaskResult exportRes = builder.exportProduct(context);
        assertNotNull(exportRes);
        assertFalse(exportRes.isSuccess());
        assertTrue(exportRes.getMessage().contains("BUILD REACTOR VALIDATION FAILED") || exportRes.getMessage().contains("pom.xml not found"));

        BuildArtifact artifact = builder.getArtifact(context);
        assertNull(artifact);
    }
}
