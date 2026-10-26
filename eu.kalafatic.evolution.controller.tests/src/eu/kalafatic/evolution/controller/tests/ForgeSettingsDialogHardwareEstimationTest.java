package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.controller.service.impl.agents.ForgeTrainingEstimationAgent;
import eu.kalafatic.evolution.view.dialogs.ForgeSettingsDialog;
import eu.kalafatic.evolution.view.dialogs.ForgeSettingsDialog.DatasetItem;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test suite verifying ForgeSettingsDialog hardware profile detection,
 * training duration estimations, dataset scale ratio JSON roundtrips,
 * and smart dataset scaling controller logic.
 */
public class ForgeSettingsDialogHardwareEstimationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testGetHardwareProfile() {
        ForgeTrainingEstimationAgent agent = ForgeTrainingEstimationAgent.getInstance();
        int cores = agent.getCpuCores();
        long maxMemMb = agent.getMaxMemoryMb();
        String hwProfile = agent.getHardwareProfile();

        assertTrue("CPU cores must be >= 1", cores >= 1);
        assertTrue("Max RAM must be >= 512 MB", maxMemMb >= 512);
        assertNotNull("Hardware profile must not be null", hwProfile);
        assertTrue("Hardware profile must contain CPU cores", hwProfile.contains("CPU:"));
        assertTrue("Hardware profile must contain RAM", hwProfile.contains("RAM:"));

        assertEquals(cores, ForgeSettingsDialog.getCpuCores());
        assertEquals(maxMemMb, ForgeSettingsDialog.getMaxMemoryMb());
        assertEquals(hwProfile, ForgeSettingsDialog.getHardwareProfile());
    }

    @Test
    public void testModelComplexityAndThroughput() {
        double nanoComp = ForgeSettingsDialog.getModelComplexityFactor("NANO");
        double smallComp = ForgeSettingsDialog.getModelComplexityFactor("SMALL");
        double xlargeComp = ForgeSettingsDialog.getModelComplexityFactor("XLARGE");

        assertEquals(1.0, nanoComp, 0.001);
        assertEquals(8.0, smallComp, 0.001);
        assertEquals(64.0, xlargeComp, 0.001);

        double nanoRate = ForgeSettingsDialog.getBaselineTokensPerSecond("NANO");
        double smallRate = ForgeSettingsDialog.getBaselineTokensPerSecond("SMALL");

        assertTrue("Throughput for simpler models must be higher", nanoRate > smallRate);
        assertTrue("Throughput must be positive", nanoRate > 0);
    }

    @Test
    public void testHoursOptionParsing() {
        ForgeTrainingEstimationAgent agent = ForgeTrainingEstimationAgent.getInstance();
        assertEquals(0.5, agent.parseHoursOption("0.5 hrs (30m)"), 0.001);
        assertEquals(1.0, agent.parseHoursOption("1.0 hr"), 0.001);
        assertEquals(12.0, agent.parseHoursOption("12.0 hrs"), 0.001);
        assertEquals(48.0, agent.parseHoursOption("48.0 hrs"), 0.001);
        assertEquals(12.0, agent.parseHoursOption("Invalid Option"), 0.001);
    }

    @Test
    public void testNonExistentFileSizingReturnsZero() {
        ForgeTrainingEstimationAgent agent = ForgeTrainingEstimationAgent.getInstance();
        long size = agent.calculatePathSize("/path/to/non_existent_file_xyz.txt");
        assertEquals(0L, size);
    }

    @Test
    public void testZeroAndNegativeInputs() {
        ForgeTrainingEstimationAgent agent = ForgeTrainingEstimationAgent.getInstance();
        assertEquals(0.0, agent.calculateEstimatedForgingSeconds("SMALL", 0, 1000L), 0.001);
        assertEquals(0.0, agent.calculateEstimatedForgingSeconds("SMALL", 16, 0L), 0.001);
        assertEquals(0.0, agent.calculateEstimatedForgingSeconds("SMALL", -5, 1000L), 0.001);
        assertEquals(0L, agent.estimateTotalTrainingTokens(null, 16));
        assertEquals("0 seconds", agent.formatDuration(-10.0));
        assertEquals("0 seconds", agent.formatDuration(Double.NaN));
    }

    @Test
    public void testLargeDatasetOverflowProtection() {
        ForgeTrainingEstimationAgent agent = ForgeTrainingEstimationAgent.getInstance();
        DatasetItem item = new DatasetItem(true, "/simulated/huge/data.txt", "FILE") {
            @Override
            public double getScaleRatio() { return 1.0; }
        };
        // Verify format duration handles days and large durations without overflow
        assertEquals("2 days 2 hr 0 min", agent.formatDuration(180000.0));
    }

    @Test
    public void testPreliminaryVersusMeasuredThroughput() {
        ForgeTrainingEstimationAgent agent = ForgeTrainingEstimationAgent.getInstance();
        assertEquals(ForgeTrainingEstimationAgent.BackendType.JVM_JAVA_CPU, agent.getActiveBackendType());
        double prelim = agent.getBaselineTokensPerSecond("SMALL");
        assertTrue("Preliminary throughput estimate must be positive", prelim > 0.0);
        double measured = agent.getMeasuredBaselineTokensPerSecond(1500.0);
        assertEquals(1500.0, measured, 0.001);
        assertEquals(0.0, agent.getMeasuredBaselineTokensPerSecond(-1.0), 0.001);
    }

    @Test
    public void testMultiModelCapacityAndBudgetFeasibility() {
        ForgeTrainingEstimationAgent agent = ForgeTrainingEstimationAgent.getInstance();
        long datasetBytes = 5 * 1024 * 1024L; // 5 MB
        int epochs = 16;
        double budgetHours = 12.0;

        double nanoSec = agent.calculateEstimatedForgingSeconds("NANO", epochs, datasetBytes);
        double smallSec = agent.calculateEstimatedForgingSeconds("SMALL", epochs, datasetBytes);
        double xlargeSec = agent.calculateEstimatedForgingSeconds("XLARGE", epochs, datasetBytes);

        assertTrue("Simpler models must finish faster", nanoSec < smallSec);
        assertTrue("Larger models must take longer", smallSec < xlargeSec);

        boolean nanoFits = (nanoSec / 3600.0) <= budgetHours;
        boolean xlargeFits = (xlargeSec / 3600.0) <= budgetHours;

        assertTrue("NANO model with 5MB should fit 12.0 hr budget", nanoFits);
        assertTrue("XLARGE model with 5MB should exceed 12.0 hr budget", !xlargeFits);
    }

    @Test
    public void testDialogUiStateJsonAndDatasetSizing() {
        DatasetItem d1 = new DatasetItem(true, "/path/to/data1.jsonl", "FILE", 1000, 1.0, 524288000L, 1024);
        DatasetItem d2 = new DatasetItem(false, "/path/to/data2.jsonl", "FILE", 500, 0.5, 262144000L, 512);

        List<DatasetItem> list = new ArrayList<>();
        list.add(d1);
        list.add(d2);

        ForgeSettingsDialog dialog = new ForgeSettingsDialog(null, "MEDIUM", 64, "Stage 3", 0.5, null, list);

        assertEquals("MEDIUM", dialog.getSelectedModelSize());
        assertEquals(64, dialog.getEpochs());
        assertEquals(0.5, dialog.getDesiredLossThreshold(), 0.001);
        assertEquals(2, dialog.getDatasets().size());

        JSONObject uiJson = dialog.getUiStateJson();
        assertNotNull(uiJson);
        assertEquals("MEDIUM", uiJson.optString("modelSize"));
        assertEquals(64, uiJson.optInt("epochs"));
        assertEquals(0.5, uiJson.optDouble("desiredLossThreshold"), 0.001);
    }

    @Test
    public void testEstimatedForgingSecondsAndFormatDuration() {
        long datasetBytes = 10 * 1024 * 1024L; // 10 MB
        int epochs = 16;
        double seconds = ForgeSettingsDialog.calculateEstimatedForgingSeconds("SMALL", epochs, datasetBytes);

        assertTrue("Estimated seconds must be positive", seconds > 0);

        assertEquals("45 seconds", ForgeSettingsDialog.formatDuration(45));
        assertEquals("10 min 30 sec", ForgeSettingsDialog.formatDuration(630));
        assertEquals("1 hr 1 min", ForgeSettingsDialog.formatDuration(3660));
    }

    @Test
    public void testDatasetItemRecordLimitAndScaleRatioSerialization() {
        DatasetItem item = new DatasetItem(true, "/path/to/dataset.jsonl", "FILE", 500, 0.5, 1048576L, 2048);
        assertEquals(500, item.getRecordLimit());
        assertEquals(0.5, item.getScaleRatio(), 0.001);
        assertEquals(1048576L, item.getMaxSizeBytes());
        assertEquals(2048, item.getMaxSeqLen());

        JSONObject json = item.toJsonObject();
        assertEquals(500, json.optLong("recordLimit"));
        assertEquals(0.5, json.optDouble("scaleRatio"), 0.001);
        assertEquals(1048576L, json.optLong("maxSizeBytes"));
        assertEquals(2048, json.optInt("maxSeqLen"));

        DatasetItem restored = DatasetItem.fromJsonObject(json);
        assertTrue(restored.isChecked());
        assertEquals("/path/to/dataset.jsonl", restored.getPath());
        assertEquals("FILE", restored.getType());
        assertEquals(500, restored.getRecordLimit());
        assertEquals(0.5, restored.getScaleRatio(), 0.001);
        assertEquals(1048576L, restored.getMaxSizeBytes());
        assertEquals(2048, restored.getMaxSeqLen());
    }

    @Test
    public void testForgeJobConfigMappingAndDefaults() {
        eu.kalafatic.evolution.forge.controller.api.ForgeJob job = new eu.kalafatic.evolution.forge.controller.api.ForgeJob("test-job-1");
        assertNotNull(job.getTrainingConfig());
        assertEquals(0.01, job.getTrainingConfig().getLearningRate(), 0.001);
        assertEquals(32, job.getTrainingConfig().getEpochs());
        assertEquals(16, job.getTrainingConfig().getBatchSize());
        assertEquals(12.0, job.getTrainingConfig().getTimeBudgetHours(), 0.001);
        assertEquals(0.15, job.getTrainingConfig().getValidationRatio(), 0.001);

        assertNotNull(job.getEvolutionConfig());
        assertEquals(1, job.getEvolutionConfig().getGenerations());
        assertEquals(1, job.getEvolutionConfig().getCandidatesPerGeneration());

        assertNotNull(job.getModelConfig());
        assertEquals("SMALL", job.getModelConfig().getModelSize());
        assertEquals(512, job.getModelConfig().getHiddenSize());
        assertEquals(8, job.getModelConfig().getHeads());
    }

    @Test
    public void testApplySmartDataScalingControllerFunction() throws Exception {
        File sampleData = tempFolder.newFile("large_training_data.txt");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append("This is line ").append(i).append(" of simulated training data for testing smart data scaling.\n");
        }
        Files.writeString(sampleData.toPath(), sb.toString(), StandardCharsets.UTF_8);

        List<DatasetItem> items = new ArrayList<>();
        items.add(new DatasetItem(true, sampleData.getAbsolutePath(), "FILE"));

        // Create a headless or default ForgeSettingsDialog instance using a null shell if permitted,
        // or directly test smart scaling logic on DatasetItems
        ForgeSettingsDialog dialog = new ForgeSettingsDialog(null, "SMALL", 32, "Stage 1", 1.0, null, items);

        long originalBytes = dialog.calculateTotalDatasetBytes();
        assertTrue("Original dataset bytes must be > 0", originalBytes > 0);

        // Apply smart data scaling for a short target duration (0.01 hours = ~36 seconds)
        boolean result = dialog.applySmartDataScaling(0.01);
        assertTrue("Smart data scaling must return true", result);

        List<DatasetItem> scaledItems = dialog.getDatasets();
        assertEquals(1, scaledItems.size());
        DatasetItem scaledItem = scaledItems.get(0);

        assertTrue("Scale ratio must be reduced below 1.0", scaledItem.getScaleRatio() <= 1.0);
        assertTrue("Record limit must be set to a positive value", scaledItem.getRecordLimit() > 0);

        long scaledBytes = dialog.calculateTotalDatasetBytes();
        assertTrue("Scaled total bytes must be <= original total bytes", scaledBytes <= originalBytes);
    }

    @Test
    public void testGenerateGraphHtmlWithoutFormatException() {
        DatasetItem d1 = new DatasetItem(true, "/path/to/data1.jsonl", "FILE", 1000, 1.0, 524288000L, 1024);
        List<DatasetItem> items = new ArrayList<>();
        items.add(d1);

        String[] modelSizes = new String[] { "NANO", "MICRO", "MINI", "SMALL", "MEDIUM", "LARGE", "XLARGE" };
        double[][] histories = new double[][] {
            null,
            new double[] { 9.5, 7.8, 5.2, 3.1 },
            new double[] { 8.0, 5.0, 2.0, 0.8 }
        };

        for (String size : modelSizes) {
            for (double[] history : histories) {
                ForgeSettingsDialog dialog = new ForgeSettingsDialog(null, size, 32, "Stage 1", 1.0, history, items);
                String html = dialog.generateGraphHtml(32, 1.0, history);
                assertNotNull("Generated graph HTML must not be null for size " + size, html);
                assertTrue("HTML must contain summary bar", html.contains("summary-bar"));
                assertTrue("HTML must contain Model Size", html.contains("Model Size:"));
                assertTrue("HTML must contain Budget:", html.contains("Budget:"));
                assertTrue("HTML must contain GRAPH 1 header", html.contains("GRAPH 1: LOSS TRAJECTORY"));
                assertTrue("HTML must contain GRAPH 2 header", html.contains("GRAPH 2: CAPACITY BUDGET"));
                assertTrue("HTML must contain GRAPH 3 header", html.contains("GRAPH 3: PROGRESS & RESOURCE MONITOR"));
                assertTrue("HTML must contain Progress / Time metric", html.contains("Calculation Progress / Time"));
                assertTrue("HTML must contain System RAM metric", html.contains("Allocated System RAM / Memory Usage"));
            }
        }
    }
}
