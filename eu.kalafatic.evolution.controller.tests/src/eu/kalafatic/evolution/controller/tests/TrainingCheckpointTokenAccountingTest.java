package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.trainer.api.TrainingCheckpoint;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the exact processed-token counter checkpointed with the optimizer
 * state: round-trips correctly and older checkpoint files (without the
 * trailer) still load with processedTokens=0 (backward compatible).
 */
public class TrainingCheckpointTokenAccountingTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testProcessedTokensRoundTrip() throws Exception {
        EvoLlmModel model = new EvoLlmModel(50, 32, 2, 1, 64, 16);
        Map<String, Integer> vocab = new HashMap<>();
        for (int i = 0; i < 50; i++) vocab.put("t" + i, i);

        TrainingCheckpoint chk = TrainingCheckpoint.capture(
                "run-1", "m1", model, 2, 17, 3.5f, 3.9f, 0.001f, 42L, "fp-abc",
                Collections.emptyMap(), Collections.emptyMap(), vocab, 987_654L);

        assertEquals(987_654L, chk.getProcessedTokens());

        Path file = tempFolder.newFolder("chk").toPath().resolve("checkpoint.chk");
        chk.saveAtomic(file);

        TrainingCheckpoint loaded = TrainingCheckpoint.load(file);
        assertEquals("run-1", loaded.getRunId());
        assertEquals(2, loaded.getCompletedEpochs());
        assertEquals(17, loaded.getTotalOptimizerSteps());
        assertEquals(987_654L, loaded.getProcessedTokens());
        assertEquals(3.5f, loaded.getLastTrainLoss(), 0.0001f);
        assertEquals("fp-abc", loaded.getDatasetFingerprint());
        assertEquals(50, loaded.getTokenizerVocab().size());
    }

    @Test
    public void testLegacyCheckpointWithoutTrailerLoadsWithZeroTokens() throws Exception {
        // Hand-craft a legacy-format checkpoint: identical field order to
        // TrainingCheckpoint.saveAtomic, but WITHOUT the EVO_CHK_TOK trailer.
        Path file = tempFolder.newFolder("legacy").toPath().resolve("legacy.chk");
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file.toFile())))) {
            dos.writeUTF("EVO_CHK_V2");
            dos.writeUTF("run-legacy");
            dos.writeUTF("legacy-model");
            dos.writeInt(1);                       // completedEpochs
            dos.writeLong(9);                      // totalOptimizerSteps
            dos.writeFloat(5.5f);                  // lastTrainLoss
            dos.writeFloat(6.5f);                  // lastValLoss
            dos.writeFloat(0.01f);                 // currentLr
            dos.writeLong(42L);                    // baseSeed
            dos.writeUTF("legacy-fp");             // datasetFingerprint
            dos.writeLong(System.currentTimeMillis()); // timestamp
            dos.writeInt(50);                      // vocabSize
            dos.writeInt(32);                      // dModel
            dos.writeInt(2);                       // numHeads
            dos.writeInt(1);                       // numBlocks
            dos.writeInt(64);                      // dff
            dos.writeInt(16);                      // maxSeqLen
            dos.writeInt(0);                       // weights count
            dos.writeInt(0);                       // moments count
            dos.writeInt(0);                       // variances count
            dos.writeInt(0);                       // vocab count
            dos.flush();
        }

        TrainingCheckpoint loaded = TrainingCheckpoint.load(file);
        assertEquals(1, loaded.getCompletedEpochs());
        assertEquals(9, loaded.getTotalOptimizerSteps());
        assertEquals("legacy-fp", loaded.getDatasetFingerprint());
        assertEquals("Legacy checkpoint must default processedTokens to 0", 0L, loaded.getProcessedTokens());
    }

    @Test
    public void testFreshCaptureDefaultsToZeroTokens() {
        EvoLlmModel model = new EvoLlmModel(50, 32, 2, 1, 64, 16);
        TrainingCheckpoint chk = TrainingCheckpoint.capture(
                "run-2", "m2", model, 0, 0, 0f, 0f, 0.01f, 42L, "fp",
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertEquals(0L, chk.getProcessedTokens());
        assertTrue(Files.isDirectory(tempFolder.getRoot().toPath()));
    }
}
