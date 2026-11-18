package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.EvaluationResult;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.JobState;
import eu.kalafatic.evolution.forge.controller.impl.DatasetControllerImpl;
import eu.kalafatic.evolution.forge.controller.impl.ExporterControllerImpl;
import eu.kalafatic.evolution.forge.controller.impl.ModelControllerImpl;
import eu.kalafatic.evolution.forge.controller.impl.SnapshotControllerImpl;
import eu.kalafatic.evolution.forge.controller.impl.TrainingControllerImpl;
import eu.kalafatic.evolution.forge.controller.service.SelfEvoForgingService.ForgingStats;
import eu.kalafatic.evolution.forge.controller.service.impl.ForgeOrchestratorImpl;
import eu.kalafatic.evolution.forge.controller.service.impl.SelfEvoForgingServiceImpl;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Regression tests for telemetry honesty: no fabricated losses, metrics, events,
 * progress values, model names or export paths may be reported as real measurements.
 */
public class TelemetryHonestyRegressionTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // ============ F-1: legacy controller fallbacks must be honest ============

    @Test
    public void trainingMetricsAreNotFabricatedWithoutService() {
        TrainingControllerImpl ctrl = new TrainingControllerImpl(null);
        Map<String, Object> metrics = ctrl.getTrainingMetrics("any-session");
        assertEquals("unavailable flag must be reported", Boolean.FALSE, metrics.get("available"));
        assertFalse("no fabricated loss key", metrics.containsKey("loss"));
        assertFalse("no fabricated acc key", metrics.containsKey("acc"));
        assertTrue("recent events must be empty", ctrl.getRecentEvents("any-session").isEmpty());
    }

    @Test
    public void modelInfoIsNotFabricatedWithoutService() {
        ModelControllerImpl ctrl = new ModelControllerImpl(null);
        assertNull("active model must be null without a service", ctrl.getActiveModel("any-session"));
        assertTrue("models list must be empty", ctrl.getModels("any-session").isEmpty());
        JSONObject structure = new JSONObject(ctrl.getModelStructure("any-session"));
        assertEquals("structure must report unavailability", false, structure.getBoolean("available"));
        try {
            ctrl.createModel("any-session", "TRANSFORMER");
            fail("createModel without a service must throw");
        } catch (UnsupportedOperationException expected) {
            assertTrue(expected.getMessage().contains("not supported"));
        }
    }

    @Test
    public void datasetStatsAndSamplesAreNotFabricatedWithoutService() {
        DatasetControllerImpl ctrl = new DatasetControllerImpl(null);
        Map<String, Object> stats = ctrl.getDatasetStatistics("any-session");
        assertEquals("unavailable flag must be reported", Boolean.FALSE, stats.get("available"));
        assertFalse("no fabricated size key", stats.containsKey("size"));
        assertFalse("no fabricated vocab key", stats.containsKey("vocab"));
        Map<String, Object> sample = ctrl.getDatasetSample("any-session", 0);
        assertEquals("sample must report unavailability", Boolean.FALSE, sample.get("available"));
    }

    @Test
    public void snapshotsAreNotFabricatedWithoutService() {
        SnapshotControllerImpl ctrl = new SnapshotControllerImpl(null);
        assertTrue("snapshot list must be empty", ctrl.getSnapshots("any-session").isEmpty());
        Map<String, Object> diff = ctrl.compareSnapshots("a", "b");
        assertEquals("comparison must report unavailability", Boolean.FALSE, diff.get("available"));
        assertFalse("no fabricated layer deltas", diff.containsKey("layersAdded"));
    }

    @Test
    public void exporterRefusesToFabricateExportPath() throws Exception {
        ExporterControllerImpl exporter = new ExporterControllerImpl(null);
        try {
            exporter.exportModel("any-session", "model", "snap");
            fail("legacy exporter must not fabricate an export path");
        } catch (UnsupportedOperationException expected) {
            assertTrue(expected.getMessage().contains("not implemented"));
        }
    }

    // ============ F-6: telemetry JSON carries real progress and aliases ============

    @Test
    public void telemetryJsonIncludesComputedProgressAndHonestAliases() {
        ForgeSessionManager.TrainingTelemetry telemetry = new ForgeSessionManager.TrainingTelemetry("t-session");
        telemetry.currentStep = 25;
        telemetry.totalSteps = 100;
        telemetry.currentLoss = 1.23456;
        telemetry.valLoss = 1.45678;
        telemetry.stepsPerSec = 12.345;
        telemetry.estimatedRemainingSeconds = 42.0;

        JSONObject json = telemetry.toJsonObject();

        assertEquals("progressPercent must be derived from real step counters", 25, json.getInt("progressPercent"));
        assertEquals("trainingLoss alias must match measured currentLoss", 1.2346, json.getDouble("trainingLoss"), 1e-4);
        assertEquals("validationLoss alias must match measured valLoss", 1.4568, json.getDouble("validationLoss"), 1e-4);
        assertEquals("stepsPerSecond alias", 12.35, json.getDouble("stepsPerSecond"), 1e-2);
        assertEquals("estimatedRemainingMillis alias", 42000L, json.getLong("estimatedRemainingMillis"));
        assertEquals("learningRate must be honestly unavailable", -1.0, json.getDouble("learningRate"), 1e-9);

        ForgeSessionManager.TrainingTelemetry idle = new ForgeSessionManager.TrainingTelemetry("idle");
        assertEquals("idle telemetry must report -1 (unavailable) progress", -1, idle.toJsonObject().getInt("progressPercent"));
    }

    // ============ F-3: orchestrator evaluation uses measured values only ============

    @Test
    public void orchestratorEvaluationUsesRealLossesNotFabrications() throws Exception {
        Path projectDir = tempFolder.newFolder("eval-project").toPath();
        Path dataFile = tempFolder.newFolder("eval-data").toPath().resolve("notes.txt");
        Files.writeString(dataFile,
                "The evolution platform trains small language models over project corpora.\n"
                + "Training data quality, deduplication and loss masking matter for convergence.\n"
                + "The Darwin forge engine orchestrates dataset preparation and training stages.\n"
                + "Each candidate model is validated on a held out validation split before export.\n"
                + "Native model artifacts are exported to the portable EVO format and GGUF.\n");

        ForgeJob job = new ForgeJob("eval-honesty-session");
        job.setSourcePaths(List.of(dataFile.toAbsolutePath().toString()));
        job.getModelConfig().setModelSize("NANO");
        job.getModelConfig().setHiddenSize(64);
        job.getModelConfig().setLayers(1);
        job.getModelConfig().setHeads(4);
        job.getModelConfig().setDff(128);
        job.getModelConfig().setMaxSeqLen(64);
        job.getTrainingConfig().setEpochs(1);
        job.getTrainingConfig().setValidationRatio(0.2);

        ForgeJob completed = new ForgeOrchestratorImpl().executeJob(job, projectDir);
        assertNotNull(completed);
        assertTrue("job must complete", completed.getState() == JobState.COMPLETED
                || completed.getState() == JobState.STOPPED);

        EvaluationResult eval = completed.getEvaluationResult();
        assertNotNull("evaluation must be present", eval);

        double realValLoss = completed.getTrainingResult() != null ? completed.getTrainingResult().getValLoss() : -1;
        assertEquals("evaluation valLoss must equal the measured training-result valLoss",
                realValLoss, eval.getValLoss(), 1e-6);
        assertEquals("quality score must be -1 (not computed) until a documented metric exists",
                -1.0, eval.getQualityScore(), 1e-9);
        assertFalse("summary must not contain a fabricated 0.05 loss", eval.getSummary().contains("0.05"));
        assertFalse("summary must not present fabricated validation loss", eval.getSummary().contains("* 1.05"));
    }

    // ============ F-4: stopForging actually removes stats ============

    @Test
    public void stopForgingClearsStatsHonestly() {
        SelfEvoForgingServiceImpl service = new SelfEvoForgingServiceImpl();
        ForgingStats idle = service.getStats("no-such-session");
        assertEquals("IDLE", idle.status());
        assertEquals(0.0, idle.currentLoss(), 1e-9);

        service.stopForging("no-such-session");
        ForgingStats after = service.getStats("no-such-session");
        assertEquals("stats must be cleared after stop", "IDLE", after.status());
    }

    // ============ F-2: E2E demo no longer writes dummy model files ============

    @Test
    public void e2eDemoDoesNotProduceDummyModelFiles() {
        File dummy = new File("forge-lab/forge-model/src/main/resources/model/demo/demo_transformer.gguf");
        ForgeSessionManager.getInstance().runE2EDemo("telemetry-honesty-no-session");
        // The honest replacement performs no file I/O at all.
        assertFalse("demo must not create dummy model artifacts", dummy.exists());
    }
}
