package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.controller.service.impl.agents.ForgeTrainingEstimationAgent;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the measured-throughput store: real training measurements are
 * recorded, persisted, reloaded after "restart" (new store configuration) and
 * drive the estimation confidence; the analytical preliminary estimate is
 * never presented as a measurement.
 */
public class ForgeTrainingEstimationMeasurementsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testRecordAndRetrieveMeasuredThroughput() throws Exception {
        ForgeTrainingEstimationAgent agent = ForgeTrainingEstimationAgent.getInstance();
        agent.resetMeasurementsForTesting();
        try {
            Path store = tempFolder.newFolder("measure-store").toPath();
            agent.configureMeasurementsStore(store);

            assertEquals(-1.0, agent.getMeasuredTokensPerSecond("SMALL"), 0.0001);
            assertEquals(0, agent.getMeasurementCount("SMALL"));
            assertEquals(0.0, agent.getThroughputConfidence("SMALL"), 0.0001);

            agent.recordTrainingMeasurement("SMALL", 320.5, 0.4, 120_000L);
            agent.recordTrainingMeasurement("SMALL", 350.0, 0.42, 118_000L);
            agent.recordTrainingMeasurement("SMALL", 310.0, 0.38, 125_000L);

            // Median of {310, 320.5, 350} = 320.5
            assertEquals(320.5, agent.getMeasuredTokensPerSecond("SMALL"), 0.01);
            assertEquals(3, agent.getMeasurementCount("SMALL"));
            assertEquals(1.0, agent.getThroughputConfidence("SMALL"), 0.0001);
            assertTrue("Measurement file must be persisted", Files.exists(store.resolve("forge-throughput-measurements.json")));
        } finally {
            agent.resetMeasurementsForTesting();
        }
    }

    @Test
    public void testMeasurementsSurviveStoreReload() throws Exception {
        ForgeTrainingEstimationAgent agent = ForgeTrainingEstimationAgent.getInstance();
        agent.resetMeasurementsForTesting();
        try {
            Path store = tempFolder.newFolder("reload-store").toPath();
            agent.configureMeasurementsStore(store);
            agent.recordTrainingMeasurement("NANO", 900.0, 3.0, 60_000L);
            agent.recordTrainingMeasurement("NANO", 910.0, 3.1, 61_000L);

            // Simulate application restart: reset memory, re-configure the same
            // store, and expect the persisted measurements to be reloaded.
            agent.resetMeasurementsForTesting();
            agent.configureMeasurementsStore(store);

            assertTrue("Persisted measurement must be reloaded", agent.getMeasuredTokensPerSecond("NANO") > 0);
            assertEquals(2, agent.getMeasurementCount("NANO"));
            assertEquals(0.7, agent.getThroughputConfidence("NANO"), 0.0001);
        } finally {
            agent.resetMeasurementsForTesting();
        }
    }

    @Test
    public void testInvalidMeasurementsAreNeverRecorded() throws Exception {
        ForgeTrainingEstimationAgent agent = ForgeTrainingEstimationAgent.getInstance();
        agent.resetMeasurementsForTesting();
        try {
            agent.recordTrainingMeasurement("SMALL", Double.NaN, 1.0, 100L);
            agent.recordTrainingMeasurement("SMALL", -5.0, 1.0, 100L);
            agent.recordTrainingMeasurement("SMALL", 0.0, 1.0, 100L);
            agent.recordTrainingMeasurement(null, 100.0, 1.0, 100L);
            assertEquals(0, agent.getMeasurementCount("SMALL"));
            assertEquals(1, agent.getMeasurementCount("UNKNOWN"));
            assertEquals(-1.0, agent.getMeasuredTokensPerSecond("SMALL"), 0.0001);
        } finally {
            agent.resetMeasurementsForTesting();
        }
    }

    @Test
    public void testAnalyticalPreliminaryRemainsAvailableWithoutMeasurements() throws Exception {
        ForgeTrainingEstimationAgent agent = ForgeTrainingEstimationAgent.getInstance();
        agent.resetMeasurementsForTesting();
        try {
            double prelim = agent.getBaselineTokensPerSecond("SMALL");
            assertTrue("Preliminary analytical estimate must be positive", prelim > 0.0);
            assertEquals(0.0, agent.getThroughputConfidence("SMALL"), 0.0001);
        } finally {
            agent.resetMeasurementsForTesting();
        }
    }

    @Test
    public void testParameterCountsAreConsistentWithModelSizePreset() {
        ForgeTrainingEstimationAgent agent = ForgeTrainingEstimationAgent.getInstance();
        // The documented SMALL architecture is ~48.9M parameters ("Small (50M)").
        assertEquals(48_923_136.0, agent.getModelParameterCount("SMALL"), 1.0);
        assertEquals(48_923_136.0, agent.getModelParameterCount("small"), 1.0);
        assertEquals(162_417_408.0, agent.getModelParameterCount("MEDIUM"), 1.0);
        assertTrue(agent.getModelParameterCount("NANO") < agent.getModelParameterCount("SMALL"));
        assertTrue(agent.getModelParameterCount("SMALL") < agent.getModelParameterCount("MEDIUM"));
    }
}
