package eu.kalafatic.evolution.controller.orchestration.design;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RepoArchitectureScannerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testScanRepositoryStructureAndRelationships() throws IOException {
        File repoRoot = tempFolder.newFolder("evo-test-repo");

        // Module 1: Controller
        File mod1 = new File(repoRoot, "eu.kalafatic.evolution.controller");
        File mod1Meta = new File(mod1, "META-INF");
        mod1Meta.mkdirs();

        File manifest1 = new File(mod1Meta, "MANIFEST.MF");
        try (FileWriter fw = new FileWriter(manifest1)) {
            fw.write("Manifest-Version: 1.0\n");
            fw.write("Bundle-SymbolicName: eu.kalafatic.evolution.controller;singleton:=true\n");
            fw.write("Require-Bundle: eu.kalafatic.evolution.model\n");
        }

        File mod1Src = new File(mod1, "src/eu/kalafatic/evolution/controller/kernel");
        mod1Src.mkdirs();

        File class1 = new File(mod1Src, "CognitiveLoopEngine.java");
        try (FileWriter fw = new FileWriter(class1)) {
            fw.write("package eu.kalafatic.evolution.controller.kernel;\n\n");
            fw.write("import eu.kalafatic.evolution.model.Orchestrator;\n\n");
            fw.write("public class CognitiveLoopEngine extends AbstractEngine implements Runnable {\n");
            fw.write("    private Orchestrator orchestrator;\n");
            fw.write("    public void executeStep() {\n");
            fw.write("        orchestrator.run();\n");
            fw.write("    }\n");
            fw.write("}\n");
        }

        // Module 2: Model
        File mod2 = new File(repoRoot, "eu.kalafatic.evolution.model");
        File mod2Meta = new File(mod2, "META-INF");
        mod2Meta.mkdirs();

        File manifest2 = new File(mod2Meta, "MANIFEST.MF");
        try (FileWriter fw = new FileWriter(manifest2)) {
            fw.write("Manifest-Version: 1.0\n");
            fw.write("Bundle-SymbolicName: eu.kalafatic.evolution.model;singleton:=true\n");
        }

        File mod2Src = new File(mod2, "src/eu/kalafatic/evolution/model");
        mod2Src.mkdirs();

        File class2 = new File(mod2Src, "Orchestrator.java");
        try (FileWriter fw = new FileWriter(class2)) {
            fw.write("package eu.kalafatic.evolution.model;\n\n");
            fw.write("public interface Orchestrator {\n");
            fw.write("    void run();\n");
            fw.write("}\n");
        }

        // Scan Repository
        RepoArchitectureScanner scanner = new RepoArchitectureScanner();
        DesignModel model = scanner.scanRepository(repoRoot);

        assertNotNull("Model should not be null", model);
        assertTrue("Model should contain components", model.getComponents().size() >= 5);

        // Verify Repository Root Node (Level 1)
        ComponentRecord repoNode = model.getComponents().stream()
                .filter(c -> c.getLevel() == 1)
                .findFirst().orElse(null);
        assertNotNull("Repo root node should exist", repoNode);
        assertEquals("evo-test-repo", repoNode.getName());

        // Verify Module Node (Level 2)
        ComponentRecord mod1Node = model.getComponents().stream()
                .filter(c -> c.getLevel() == 2 && c.getName().contains("controller"))
                .findFirst().orElse(null);
        assertNotNull("Controller module node should exist", mod1Node);
        assertEquals("repo", mod1Node.getParentId());

        // Verify Package Node (Level 3)
        ComponentRecord pkgNode = model.getComponents().stream()
                .filter(c -> c.getLevel() == 3 && c.getName().equals("eu.kalafatic.evolution.controller.kernel"))
                .findFirst().orElse(null);
        assertNotNull("Kernel package node should exist", pkgNode);

        // Verify Class Node (Level 4)
        ComponentRecord classNode = model.getComponents().stream()
                .filter(c -> c.getLevel() == 4 && c.getName().equals("CognitiveLoopEngine"))
                .findFirst().orElse(null);
        assertNotNull("CognitiveLoopEngine class node should exist", classNode);
        assertEquals("AbstractEngine", classNode.getSuperClass());
        assertTrue("Interfaces should contain Runnable", classNode.getInterfaces().contains("Runnable"));

        // Verify Member Nodes (Level 5)
        ComponentRecord fieldMember = model.getComponents().stream()
                .filter(c -> c.getLevel() == 5 && c.getName().equals("orchestrator"))
                .findFirst().orElse(null);
        assertNotNull("orchestrator field member node should exist", fieldMember);

        // Verify OSGi Require-Bundle Relationship
        RelationshipRecord osgiRel = model.getRelationships().stream()
                .filter(r -> "OSGI_REQUIRE_BUNDLE".equals(r.getType()))
                .findFirst().orElse(null);
        assertNotNull("OSGi Require-Bundle relationship should exist", osgiRel);

        // Test Serialization via DesignRenderer
        DesignRenderer renderer = new DesignRenderer();
        String json = renderer.serializeModel(model);
        assertTrue("JSON should contain parentId", json.contains("\"parentId\""));
        assertTrue("JSON should contain CognitiveLoopEngine", json.contains("CognitiveLoopEngine"));
        assertTrue("JSON should contain OSGI_REQUIRE_BUNDLE", json.contains("OSGI_REQUIRE_BUNDLE"));
    }

    @Test
    public void testProgressiveLevelScanning() throws IOException {
        File repoRoot = tempFolder.newFolder("evo-prog-test-repo");

        File mod = new File(repoRoot, "eu.kalafatic.evolution.demo");
        File src = new File(mod, "src/eu/kalafatic/demo");
        src.mkdirs();

        File javaClass = new File(src, "DemoRunner.java");
        try (FileWriter fw = new FileWriter(javaClass)) {
            fw.write("package eu.kalafatic.demo;\n\n");
            fw.write("public class DemoRunner {\n");
            fw.write("    private int counter = 0;\n");
            fw.write("    public void run() {}\n");
            fw.write("}\n");
        }

        RepoArchitectureScanner scanner = new RepoArchitectureScanner();

        // Step 1: High level scan (maxLevel = 2)
        DesignModel modelL2 = scanner.scanRepository(repoRoot, 2);
        assertNotNull("L2 model should not be null", modelL2);

        ComponentRecord repoNode = modelL2.getComponents().stream().filter(c -> c.getLevel() == 1).findFirst().orElse(null);
        assertNotNull("Repo node should exist at level 1", repoNode);

        ComponentRecord modNode = modelL2.getComponents().stream().filter(c -> c.getLevel() == 2).findFirst().orElse(null);
        assertNotNull("Module node should exist at level 2", modNode);
        assertTrue("Module node should report unscanned children", modNode.hasUnscannedChildren());

        // Step 2: Progressive scan module to packages (Level 3)
        boolean scannedL3 = scanner.scanNextLevel(repoRoot, modelL2, modNode.getId());
        assertTrue("scanNextLevel to L3 should succeed", scannedL3);

        ComponentRecord pkgNode = modelL2.getComponents().stream().filter(c -> c.getLevel() == 3).findFirst().orElse(null);
        assertNotNull("Package node should exist after L3 scan", pkgNode);

        // Step 3: Progressive scan package to classes (Level 4)
        boolean scannedL4 = scanner.scanNextLevel(repoRoot, modelL2, pkgNode.getId());
        assertTrue("scanNextLevel to L4 should succeed", scannedL4);

        ComponentRecord classNode = modelL2.getComponents().stream().filter(c -> c.getLevel() == 4).findFirst().orElse(null);
        assertNotNull("Class node should exist after L4 scan", classNode);
        assertEquals("DemoRunner", classNode.getName());

        // Step 4: Progressive scan class to members (Level 5)
        boolean scannedL5 = scanner.scanNextLevel(repoRoot, modelL2, classNode.getId());
        assertTrue("scanNextLevel to L5 should succeed", scannedL5);

        ComponentRecord memberNode = modelL2.getComponents().stream().filter(c -> c.getLevel() == 5).findFirst().orElse(null);
        assertNotNull("Member node should exist after L5 scan", memberNode);
    }
}
