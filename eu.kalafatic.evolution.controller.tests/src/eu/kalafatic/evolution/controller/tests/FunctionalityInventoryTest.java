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
            assertNotNull("Timestamp should not be null", e.timestamp);
            assertTrue("Timestamp should not be empty", !e.timestamp.isEmpty());
            assertTrue("Overall score should be between 0 and 100", e.calculateOverallScore() >= 0 && e.calculateOverallScore() <= 100);

            // Verify mapping to actual source file
            File srcFile = new File(repoRoot, e.sourcePath);
            assertTrue("Functionality source path must exist: " + e.sourcePath, srcFile.exists());
        }
    }

    @Test
    public void testGenerateInventoryAndSnapshotHistory() throws Exception {
        File tempDir = Files.createTempDir();
        File dummyRepo = new File(tempDir, "dummy-repo");
        dummyRepo.mkdirs();

        FunctionalityInventoryManager mgr = FunctionalityInventoryManager.getInstance();
        File invFile = mgr.generateInventory(dummyRepo, "TEST_PROJECT", "test-commit-123");

        assertNotNull("Generated inventory file should not be null", invFile);
        assertTrue("Generated inventory file must exist in genome/current", invFile.exists());

        File historyDir = new File(new File(dummyRepo, "genome"), "history");
        assertTrue("Genome history directory must be created", historyDir.exists());
        File[] historySnapshots = historyDir.listFiles();
        assertNotNull("History snapshots list should not be null", historySnapshots);
        assertTrue("At least one historical snapshot directory must be created", historySnapshots.length > 0);

        File histInv = new File(historySnapshots[0], "functionality_inventory.json");
        assertTrue("Historical functionality_inventory.json must exist", histInv.exists());

        deleteDirectory(tempDir);
    }

    @Test
    public void testMetadataFreshnessAndStalenessDetection() throws Exception {
        File tempDir = Files.createTempDir();
        File dummySource = new File(tempDir, "TestClass.java");
        Files.touch(dummySource);

        AIContextTool tool = new AIContextTool();

        // 1. Initially no sidecar -> isMetadataStale is true
        assertTrue("Missing sidecar should be considered stale", tool.isMetadataStale(dummySource));

        // 2. Save sidecar -> should be fresh
        FunctionalityMetadata meta = new FunctionalityMetadata();
        meta.setFunctionalityId("FUNC-TEST");
        meta.setFunctionalityName("Test Freshness");
        meta.setPrimaryClass("TestClass");
        meta.setImportanceScore0To100(80);
        meta.setEvaluationTimestamp("2026-11-05 12:00:00");
        tool.saveFunctionalityMetadata(dummySource, meta);

        File sidecar = new File(tempDir, "TestClass.java" + AIContextTool.METADATA_SUFFIX);
        assertTrue("Sidecar must exist", sidecar.exists());

        // Ensure sidecar timestamp is equal or newer than source
        sidecar.setLastModified(dummySource.lastModified() + 2000);
        assertTrue("Sidecar newer than source should be fresh", !tool.isMetadataStale(dummySource));

        // 3. Touch source to simulate source modification -> should become stale
        dummySource.setLastModified(sidecar.lastModified() + 5000);
        assertTrue("Source modified after sidecar should be detected as stale", tool.isMetadataStale(dummySource));

        FunctionalityMetadata loaded = tool.loadFunctionalityMetadata(dummySource);
        assertNotNull("Loaded metadata should not be null", loaded);
        assertTrue("Loaded metadata should have stale flag set to true", loaded.isStale());

        deleteDirectory(tempDir);
    }

    @Test
    public void testTimestampFormatting() {
        String formattedNull = FunctionalityInventoryManager.formatTimestamp(null);
        assertNotNull(formattedNull);
        assertTrue("Null timestamp should default to formatted current date", formattedNull.length() >= 10);

        String standardDate = "2025-02-23 14:30:00";
        assertEquals("Standard date format should be preserved", standardDate, FunctionalityInventoryManager.formatTimestamp(standardDate));

        String millisStr = "1740319200000"; // 2025-02-23T14:00:00Z approx
        String formattedFromMillis = FunctionalityInventoryManager.formatTimestamp(millisStr);
        assertNotNull(formattedFromMillis);
        assertTrue("Millis should be converted to readable date string", formattedFromMillis.contains("-"));
    }

    @Test
    public void testFunctionalityMetadataTimestampEnrichment() throws Exception {
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
        meta.setSourcePath("SelfDevOrchestrator.java");
        meta.setImplementationStatus("IMPLEMENTED");
        meta.setEvaluationTimestamp("2025-02-23 15:45:00");

        tool.saveFunctionalityMetadata(dummySource, meta);

        FunctionalityMetadata loaded = tool.loadFunctionalityMetadata(dummySource);
        assertNotNull("Loaded functionality metadata should not be null", loaded);
        assertEquals("2025-02-23 15:45:00", loaded.getEvaluationTimestamp());

        deleteDirectory(tempDir);
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
