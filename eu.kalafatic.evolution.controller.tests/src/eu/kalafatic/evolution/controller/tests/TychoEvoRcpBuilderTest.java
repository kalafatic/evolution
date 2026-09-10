package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BuildArtifact;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskStatus;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TychoEvoRcpBuilder;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TychoEvoRcpBuilder.ProductDefinition;

public class TychoEvoRcpBuilderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private TychoEvoRcpBuilder builder;
    private File mockRepoRoot;

    @Before
    public void setUp() throws Exception {
        builder = new TychoEvoRcpBuilder();
        mockRepoRoot = tempFolder.newFolder("mockRepo");

        // Create root pom.xml
        File rootPom = new File(mockRepoRoot, "pom.xml");
        try (FileWriter fw = new FileWriter(rootPom)) {
            fw.write("<project><modelVersion>4.0.0</modelVersion><groupId>eu.kalafatic.evolution</groupId><artifactId>aggregator</artifactId><version>1.0.0</version><packaging>pom</packaging></project>");
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
    public void testReactorDiscovery() {
        File srcDir = builder.discoverSourceDir(null);
        assertNotNull(srcDir);

        File reactorRoot = builder.discoverReactorRoot(mockRepoRoot);
        assertNotNull(reactorRoot);
        assertEquals(mockRepoRoot.getAbsoluteFile(), reactorRoot.getAbsoluteFile());
    }

    @Test
    public void testProductDiscovery() {
        ProductDefinition prodDef = builder.discoverProductDefinition(mockRepoRoot);
        assertNotNull(prodDef);
        assertEquals("evolution", prodDef.getProductId());
        assertEquals("evo", prodDef.getLauncherName());
        assertEquals("evolution", prodDef.getRootFolder());
        assertEquals("eu.kalafatic.evolution.repository", prodDef.getRepositoryModule());
        assertNotNull(prodDef.getProductFile());
        assertTrue(prodDef.getProductFile().exists());
    }

    @Test
    public void testExactArtifactSelection() throws Exception {
        File productsDir = new File(mockRepoRoot, "eu.kalafatic.evolution.repository/target/products");
        productsDir.mkdirs();

        File winZip = new File(productsDir, "evolution-win32.win32.x86_64.zip");
        try (FileOutputStream fos = new FileOutputStream(winZip)) {
            fos.write("dummy zip content".getBytes());
        }

        File linuxTar = new File(productsDir, "evolution-linux.gtk.x86_64.tar.gz");
        try (FileOutputStream fos = new FileOutputStream(linuxTar)) {
            fos.write("dummy tar content".getBytes());
        }

        File unrelatedZip = new File(productsDir, "unrelated-app-1.0.0.zip");
        try (FileOutputStream fos = new FileOutputStream(unrelatedZip)) {
            fos.write("unrelated content".getBytes());
        }

        ProductDefinition prodDef = builder.discoverProductDefinition(mockRepoRoot);

        File foundWin = builder.locateExportedProduct(mockRepoRoot, prodDef, "windows", "x86_64", null);
        assertNotNull(foundWin);
        assertEquals(winZip.getAbsoluteFile(), foundWin.getAbsoluteFile());

        File foundLinux = builder.locateExportedProduct(mockRepoRoot, prodDef, "linux", "x86_64", null);
        assertNotNull(foundLinux);
        assertEquals(linuxTar.getAbsoluteFile(), foundLinux.getAbsoluteFile());
    }

    @Test
    public void testInvalidDeploymentRejection() throws Exception {
        File incompleteDir = tempFolder.newFolder("incompleteProduct");
        ProductDefinition prodDef = new ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);

        // Missing launcher & plugins & config
        TaskResult resIncomplete = builder.validateProductDeployment(incompleteDir, prodDef, "linux");
        assertNotNull(resIncomplete);
        assertFalse(resIncomplete.isSuccess());
        assertTrue(resIncomplete.getMessage().contains("missing items"));

        // Add launcher and ini, but missing plugins/
        File launcher = new File(incompleteDir, "evo");
        launcher.createNewFile();
        File ini = new File(incompleteDir, "evo.ini");
        ini.createNewFile();

        TaskResult resNoPlugins = builder.validateProductDeployment(incompleteDir, prodDef, "linux");
        assertNotNull(resNoPlugins);
        assertFalse(resNoPlugins.isSuccess());

        // Add plugins/ with no jars
        File pluginsDir = new File(incompleteDir, "plugins");
        pluginsDir.mkdirs();
        TaskResult resEmptyPlugins = builder.validateProductDeployment(incompleteDir, prodDef, "linux");
        assertNotNull(resEmptyPlugins);
        assertFalse(resEmptyPlugins.isSuccess());
    }

    @Test
    public void testWindowsAndLinuxLauncherValidation() throws Exception {
        File validWinProduct = tempFolder.newFolder("validWinProduct");
        File exe = new File(validWinProduct, "evo.exe");
        exe.createNewFile();
        File iniWin = new File(validWinProduct, "evo.ini");
        iniWin.createNewFile();

        File pluginsWin = new File(validWinProduct, "plugins");
        pluginsWin.mkdirs();
        File bundleWin = new File(pluginsWin, "eu.kalafatic.evolution.view_2.6.5.jar");
        bundleWin.createNewFile();

        File configWin = new File(validWinProduct, "configuration");
        configWin.mkdirs();
        File configIniWin = new File(configWin, "config.ini");
        try (FileWriter fw = new FileWriter(configIniWin)) {
            fw.write("eclipse.application=eu.kalafatic.evolution.view.application.Application\n");
        }

        ProductDefinition prodDef = new ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);

        TaskResult winRes = builder.validateProductDeployment(validWinProduct, prodDef, "windows");
        assertNotNull(winRes);
        assertTrue(winRes.isSuccess());

        File validLinuxProduct = tempFolder.newFolder("validLinuxProduct");
        File sh = new File(validLinuxProduct, "evo");
        sh.createNewFile();
        File iniLinux = new File(validLinuxProduct, "evo.ini");
        iniLinux.createNewFile();

        File pluginsLinux = new File(validLinuxProduct, "plugins");
        pluginsLinux.mkdirs();
        File bundleLinux = new File(pluginsLinux, "eu.kalafatic.evolution.view_2.6.5.jar");
        bundleLinux.createNewFile();

        File configLinux = new File(validLinuxProduct, "configuration");
        configLinux.mkdirs();
        File configIniLinux = new File(configLinux, "config.ini");
        try (FileWriter fw = new FileWriter(configIniLinux)) {
            fw.write("eclipse.product=eu.kalafatic.evolution.view.product\n");
        }

        TaskResult linuxRes = builder.validateProductDeployment(validLinuxProduct, prodDef, "linux");
        assertNotNull(linuxRes);
        assertTrue(linuxRes.isSuccess());
    }

    @Test
    public void testBuildAndExportFailurePropagation() throws Exception {
        File emptyDir = tempFolder.newFolder("emptyRoot");
        SelfDevContext context = new SelfDevContext(emptyDir, null);

        TaskResult buildRes = builder.build(context);
        assertNotNull(buildRes);
        assertFalse(buildRes.isSuccess());
        assertTrue(buildRes.getMessage().contains("pom.xml not found"));

        TaskResult exportRes = builder.exportProduct(context);
        assertNotNull(exportRes);
        assertFalse(exportRes.isSuccess());
        assertTrue(exportRes.getMessage().contains("pom.xml not found"));

        BuildArtifact artifact = builder.getArtifact(context);
        assertNull(artifact);
    }
}
