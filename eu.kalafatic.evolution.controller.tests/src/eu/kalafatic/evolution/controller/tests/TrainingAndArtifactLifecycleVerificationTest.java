package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSample;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.model.llm.ModelParameters;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.trainer.api.TrainingCheckpoint;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

/**
 * WP3/WP4/WP5 verification tests: dataset integrity and legacy compatibility,
 * real checkpoint/resume continuity, and .evo artifact fidelity of a trained
 * model including sampling configuration.
 */
public class TrainingAndArtifactLifecycleVerificationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static List<TrainingSample> syntheticSamples(int count, int seqLen) {
        List<TrainingSample> samples = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int[] inputIds = new int[seqLen];
            int[] labels = new int[seqLen];
            boolean[] lossMask = new boolean[seqLen];
            float[] attMask = new float[seqLen];
            for (int t = 0; t < seqLen; t++) {
                inputIds[t] = 1 + ((i + t) % 7);
                labels[t] = 1 + ((i + t + 1) % 7);
                lossMask[t] = true;
                attMask[t] = 1.0f;
            }
            samples.add(new TrainingSample(inputIds, labels, lossMask, attMask));
        }
        return samples;
    }

    // ============ WP3: dataset integrity & legacy compatibility ============

    @Test
    public void legacyEvodataV10WithoutManifestStillLoadsWithDiagnostic() throws Exception {
        File file = new File(tempFolder.newFolder("legacy"), "legacy.evodata");
        // Build a v1.0-style archive: data.jsonl + minimal metadata.json without
        // files checksums and without tokenizer identity.
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file.toPath()))) {
            ZipEntry dataEntry = new ZipEntry("data.jsonl");
            zos.putNextEntry(dataEntry);
            NormalizedSample s = new NormalizedSample();
            s.setId("legacy-1");
            s.setText("Legacy dataset record content.");
            s.recalculateCountsAndHash();
            zos.write((s.toJsonLine() + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zos.closeEntry();

            ZipEntry metaEntry = new ZipEntry("metadata.json");
            zos.putNextEntry(metaEntry);
            zos.write(("{\"formatVersion\":\"1.0\",\"status\":\"READY\",\"trainSamples\":1,\"valSamples\":0}")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        EvoDatasetArtifact loaded = EvoDatasetArtifact.load(file);
        assertEquals("legacy v1.0 artifacts must load", EvoDatasetArtifact.Status.READY, loaded.getStatus());
        assertEquals(1, loaded.getTrainSamples().size());
        // v1.0 artifacts carry no checksum manifest; loading them is valid and the
        // status remains READY (only the missing manifest triggers a diagnostic).
        assertTrue("legacy diagnostics must not claim corruption",
                !loaded.getLoadDiagnostics().contains("checksum"));
    }

    @Test
    public void tokenizerIdentityMismatchProducesClearError() throws Exception {
        File file = new File(tempFolder.newFolder("tokid"), "tok.evodata");
        EvoDatasetArtifact artifact = new EvoDatasetArtifact(file);
        artifact.setTokenizerIdentity("SimpleBPETokenizer", "2.0", 300, List.of("<unk>"));
        List<NormalizedSample> samples = new ArrayList<>();
        NormalizedSample s = new NormalizedSample();
        s.setId("t1");
        s.setText("Tokenizer identity test content.");
        s.recalculateCountsAndHash();
        samples.add(s);
        artifact.save(samples, new DatasetSourceConfig("LOCAL", "test"), new DatasetSourceStats(), 0.0);

        try {
            artifact.validateTokenizerCompatibility("SimpleBPETokenizer", 500);
            fail("vocab-size mismatch must produce a clear error");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("vocab"));
        }

        try {
            artifact.validateTokenizerCompatibility("DifferentTokenizer", 300);
            fail("tokenizer-type mismatch must produce a clear error");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("mismatch"));
        }

        // Matching identity passes.
        artifact.validateTokenizerCompatibility("SimpleBPETokenizer", 300);

        // A dataset without a declared identity is tokenizer-agnostic.
        EvoDatasetArtifact raw = new EvoDatasetArtifact(new File(tempFolder.newFolder("raw"), "raw.evodata"));
        raw.validateTokenizerCompatibility("Anything", 1234);
    }

    @Test
    public void failedSaveLeavesNoTempArtifact() throws Exception {
        File dir = tempFolder.newFolder("atomicsave");
        File file = new File(dir, "atomic.evodata");
        EvoDatasetArtifact artifact = new EvoDatasetArtifact(file);
        // Saving zero samples fails validation (0 training samples) — the atomic
        // write must not leave .tmp files behind and the original must stay intact.
        try {
            artifact.save(Collections.emptyList(), new DatasetSourceConfig("LOCAL", "test"),
                    new DatasetSourceStats(), 0.0);
            fail("saving an empty dataset must fail validation");
        } catch (IllegalStateException expected) {
            // expected
        }
        File[] leftovers = dir.listFiles((d, n) -> n.endsWith(".tmp"));
        assertTrue("no temp files may remain after a failed save",
                leftovers == null || leftovers.length == 0);
    }

    // ============ WP4: real resume continuity ============

    @Test
    public void interruptedAndResumedTrainingMatchesUninterruptedRun() throws Exception {
        EvoLlmArchitecture arch = new EvoLlmArchitecture(32, 16, 2, 2, 32, 32);
        List<TrainingSample> samples = syntheticSamples(12, 16);

        // Uninterrupted: 2 epochs.
        EvoLlmModel uninterruptedModel = new EvoLlmModel(arch);
        EvoLlmTrainer uninterrupted = new EvoLlmTrainer(uninterruptedModel);
        uninterrupted.setMicroBatchSize(4);
        uninterrupted.setAccumulationSteps(1);
        uninterrupted.setValidationSplitRatio(0.0f);
        uninterrupted.train(samples, 2);

        // Interrupted: SAME planned total (2 epochs), stopped after epoch 1 so the
        // LR schedule totals match, then resumed with a fresh model instance.
        EvoLlmModel firstModel = new EvoLlmModel(arch);
        EvoLlmTrainer first = new EvoLlmTrainer(firstModel);
        first.setMicroBatchSize(4);
        first.setAccumulationSteps(1);
        first.setValidationSplitRatio(0.0f);
        first.addProgressListener(new EvoLlmTrainer.ProgressListener() {
            @Override
            public void onProgress(int epoch, int totalEpochs, int sampleIndex, int totalSamplesCount, double currentLoss) {}

            @Override
            public void onEpochCompleted(int epoch, int totalEpochs, double trainLoss, double valLoss) {
                if (epoch >= 1) {
                    first.requestStop();
                }
            }
        });
        first.train(samples, 2);

        TrainingCheckpoint checkpoint = first.captureCheckpoint(
                "continuity-run", "continuity-model", 1,
                (float) first.getLastTrainingResult().getLastLoss(),
                (float) first.getLastTrainingResult().getValLoss(),
                "sha256:continuity", new java.util.HashMap<>());

        EvoLlmModel resumedModel = new EvoLlmModel(arch);
        EvoLlmTrainer resumed = new EvoLlmTrainer(resumedModel);
        resumed.setMicroBatchSize(4);
        resumed.setAccumulationSteps(1);
        resumed.setValidationSplitRatio(0.0f);
        resumed.setResumeCheckpoint(checkpoint);
        resumed.train(samples, 2);

        assertEquals("resume must complete all epochs", 2, resumed.getLastTrainingResult().getCompletedEpochs());

        ModelParameters expected = uninterruptedModel.getModelParameters();
        ModelParameters actual = resumedModel.getModelParameters();
        for (String name : expected.names()) {
            float[] e = expected.get(name).getData();
            float[] a = actual.get(name).getData();
            assertEquals("parameter tensor sizes must match", e.length, a.length);
            for (int i = 0; i < e.length; i++) {
                assertEquals("resumed weights must match uninterrupted run for " + name + "[" + i + "]",
                        e[i], a[i], 1e-5f);
            }
        }
        assertEquals("final training loss must match the uninterrupted run",
                uninterrupted.getLastTrainingResult().getLastLoss(),
                resumed.getLastTrainingResult().getLastLoss(), 1e-4);
    }

    // ============ WP5: trained-model .evo fidelity incl. sampling config ============

    @Test
    public void trainedModelEvoExportPreservesWeightsLogitsAndSamplingConfig() throws Exception {
        EvoLlmArchitecture arch = new EvoLlmArchitecture(32, 16, 2, 2, 32, 32);
        EvoLlmModel trained = new EvoLlmModel(arch);
        EvoLlmTrainer trainer = new EvoLlmTrainer(trained);
        trainer.setMicroBatchSize(4);
        trainer.setAccumulationSteps(1);
        trainer.setValidationSplitRatio(0.0f);
        trainer.train(syntheticSamples(8, 16), 2);

        int[] probe = {1, 3, 5, 2};
        float[] before = trained.forward(probe).getData();

        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("trained-fidelity", trained, new java.util.HashMap<>());
        artifact.setTemperature(0.42f);
        artifact.setTopP(0.85f);
        artifact.setTopK(25);
        artifact.setRepeatPenalty(1.25f);
        artifact.setFrequencyPenalty(0.1f);
        artifact.setPresencePenalty(0.2f);

        Path evo = tempFolder.newFolder("fidelity").toPath().resolve("trained-fidelity.evo");
        artifact.saveToFile(evo);

        EvoModelArtifact loaded = EvoModelArtifact.load(evo);
        assertEquals("sampling temperature must survive .evo round trip", 0.42f, loaded.getTemperature(), 1e-6);
        assertEquals("top-p must survive .evo round trip", 0.85f, loaded.getTopP(), 1e-6);
        assertEquals("top-k must survive .evo round trip", 25, loaded.getTopK());
        assertEquals("repeat penalty must survive .evo round trip", 1.25f, loaded.getRepeatPenalty(), 1e-6);
        assertEquals("frequency penalty must survive .evo round trip", 0.1f, loaded.getFrequencyPenalty(), 1e-6);
        assertEquals("presence penalty must survive .evo round trip", 0.2f, loaded.getPresencePenalty(), 1e-6);

        EvoLlmModel restored = loaded.createModel();
        float[] after = restored.forward(probe).getData();
        assertArrayEquals("trained logits must survive .evo round trip exactly", before, after, 1e-6f);

        ModelParameters trainedParams = trained.getModelParameters();
        ModelParameters restoredParams = restored.getModelParameters();
        for (String name : trainedParams.names()) {
            assertArrayEquals("trained weights must survive export for " + name,
                    trainedParams.get(name).getData(), restoredParams.get(name).getData(), 1e-6f);
        }
    }
}
