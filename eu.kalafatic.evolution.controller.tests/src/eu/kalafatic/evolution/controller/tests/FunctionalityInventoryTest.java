package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.selfdev.FunctionalityInventoryManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FunctionalityInventoryManager.FunctionalityEntry;
import eu.kalafatic.utils.semantic.AIContextTool;
import eu.kalafatic.utils.semantic.FunctionalityMetadata;

public class FunctionalityInventoryTest {

    @Test
    public void testFunctionalityInventoryDiscovery() {
        File repoRoot = new File(System.getProperty("user.dir"));
        List<FunctionalityEntry> entries = FunctionalityInventoryManager.getInstance().discoverCoreFunctionalities(repoRoot);

        assertNotNull("Functionality list should not be null", entries);
        assertEquals("Catalogue should contain exactly 28 core EVO functionalities", 28, entries.size());

        for (FunctionalityEntry e : entries) {
            assertNotNull("Functionality ID should not be null", e.id);
            assertNotNull("Functionality Name should not be null", e.name);
            assertNotNull("Primary Class should not be null", e.primaryClass);
            assertNotNull("FQCN should not be null", e.fqcn);
            assertNotNull("Module should not be null", e.module);
            assertNotNull("Source Path should not be null", e.sourcePath);
            assertTrue("Overall score should be between 0 and 100", e.calculateOverallScore() >= 0 && e.calculateOverallScore() <= 100);
        }
    }

    @Test
    public void testFunctionalityMetadataSerialization() throws Exception {
        File tempDir = Files.createTempDir();
        File dummySource = new File(tempDir, "SelfDevOrchestrator.java");
        Files.touch(dummySource);

        AIContextTool tool = new AIContextTool();

        FunctionalityMetadata meta = new FunctionalityMetadata();
        meta.setFunctionalityId("FUNC-01");
        meta.setFunctionalityName("Autonomous Self-Development Pipeline");
        meta.setPrimaryClass("SelfDevOrchestrator");
        meta.setFqcn("eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevOrchestrator");
        meta.setModuleName("eu.kalafatic.evolution.controller");
        meta.setSourcePath("eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/SelfDevOrchestrator.java");
        meta.setImplementationStatus("IMPLEMENTED");

        meta.setImportanceScore0To100(95);
        meta.setUsageEstimate0To100(90);
        meta.setComplexityScore0To100(85);
        meta.setCentralityScore0To100(90);
        meta.setMaturityScore0To100(95);
        meta.setRiskScore0To100(20);

        meta.setEvaluationRationale("Core pipeline orchestration.");
        meta.setEvaluationMethod("STATIC");

        tool.saveFunctionalityMetadata(dummySource, meta);

        FunctionalityMetadata loaded = tool.loadFunctionalityMetadata(dummySource);
        assertNotNull("Loaded functionality metadata should not be null", loaded);
        assertEquals("FUNC-01", loaded.getFunctionalityId());
        assertEquals("Autonomous Self-Development Pipeline", loaded.getFunctionalityName());
        assertEquals("SelfDevOrchestrator", loaded.getPrimaryClass());
        assertEquals(95, loaded.getImportanceScore0To100());
        assertEquals(90, loaded.getUsageEstimate0To100());
        assertEquals(85, loaded.getComplexityScore0To100());
        assertEquals(90, loaded.getCentralityScore0To100());
        assertEquals(95, loaded.getMaturityScore0To100());
        assertEquals(20, loaded.getRiskScore0To100());

        deleteDirectory(tempDir);
    }

    private static class Files {
        public static File createTempDir() throws Exception {
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            String baseName = System.currentTimeMillis() + "-";
            for (int counter = 0; counter < 1000; counter++) {
                File tempDir = new File(baseDir, baseName + counter);
                if (tempDir.mkdir()) {
                    return tempDir;
                }
            }
            throw new IllegalStateException("Failed to create directory");
        }

        public static void touch(File file) throws Exception {
            if (!file.exists()) {
                file.createNewFile();
            }
            file.setLastModified(System.currentTimeMillis());
        }
    }

    private void deleteDirectory(File dir) {
        if (!dir.exists()) return;
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) deleteDirectory(child);
        }
        dir.delete();
    }
}
