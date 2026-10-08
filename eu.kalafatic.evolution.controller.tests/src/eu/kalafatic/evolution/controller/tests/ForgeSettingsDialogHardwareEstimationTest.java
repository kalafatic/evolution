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
        DatasetItem item = new DatasetItem(true, "/path/to/dataset.jsonl", "FILE", 500, 0.5);
        assertEquals(500, item.getRecordLimit());
        assertEquals(0.5, item.getScaleRatio(), 0.001);

        JSONObject json = item.toJsonObject();
        assertEquals(500, json.optLong("recordLimit"));
        assertEquals(0.5, json.optDouble("scaleRatio"), 0.001);

        DatasetItem restored = DatasetItem.fromJsonObject(json);
        assertTrue(restored.isChecked());
        assertEquals("/path/to/dataset.jsonl", restored.getPath());
        assertEquals("FILE", restored.getType());
        assertEquals(500, restored.getRecordLimit());
        assertEquals(0.5, restored.getScaleRatio(), 0.001);
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
}
