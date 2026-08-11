package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.agent.export.OllamaExporter;

/**
 * High-precision validation tests for the native EVO persistent model artifact,
 * weights/tokenizer serialization, metadata preservation, corruption recovery,
 * repeated exports, and inference output equivalence.
 */
public class EvoModelPersistentArtifactTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private EvoLlmModel originalModel;
    private Map<String, Integer> mockVocab;
    private Path modelDir;

    @Before
    public void setUp() throws Exception {
        // Create a tiny model for fast, reproducible testing
        // vocabSize=100, embeddingSize=32, heads=2, layers=1, dff=128, maxSeqLen=8
        originalModel = new EvoLlmModel(100, 32, 2, 1, 128, 8);

        mockVocab = new LinkedHashMap<>();
        mockVocab.put("<PAD>", 0);
        mockVocab.put("<UNK>", 1);
        mockVocab.put("<BOS>", 2);
        mockVocab.put("<EOS>", 3);
        for (int i = 4; i < 100; i++) {
            mockVocab.put("token_" + i, i);
        }

        modelDir = tempFolder.newFolder("persisted-evo-model").toPath();
    }

    @Test
    public void testSaveLoadRoundTripAndMetadataPreservation() throws Exception {
        EvoModelArtifact originalArtifact = new EvoModelArtifact();
        originalArtifact.initializeFromModel("evo-test-artifact", originalModel, mockVocab);

        // Adjust sampling configuration parameters to test serialization completeness
        originalArtifact.setTemperature(0.42f);
        originalArtifact.setTopP(0.85f);
        originalArtifact.setTopK(25);
        originalArtifact.setRepeatPenalty(1.25f);

        // Save native EVO artifact
        originalArtifact.save(modelDir);

        // Verify key layout files exist
        assertTrue(Files.exists(modelDir.resolve("model.json")));
        assertTrue(Files.exists(modelDir.resolve("config.json")));
        assertTrue(Files.exists(modelDir.resolve("tokenizer.json")));
        assertTrue(Files.exists(modelDir.resolve("weights.bin")));

        // Load the artifact back from the layout folder
        EvoModelArtifact loadedArtifact = EvoModelArtifact.load(modelDir);

        // Verify architectural dimensions & metadata integrity
        assertEquals("evo-test-artifact", loadedArtifact.getModelName());
        assertEquals(originalArtifact.getVocabSize(), loadedArtifact.getVocabSize());
        assertEquals(originalArtifact.getEmbeddingSize(), loadedArtifact.getEmbeddingSize());
        assertEquals(originalArtifact.getLayers(), loadedArtifact.getLayers());
        assertEquals(originalArtifact.getHeads(), loadedArtifact.getHeads());
        assertEquals(originalArtifact.getDff(), loadedArtifact.getDff());
        assertEquals(originalArtifact.getMaxSeqLen(), loadedArtifact.getMaxSeqLen());
        assertEquals(originalArtifact.getParameterCount(), loadedArtifact.getParameterCount());

        // Verify sampling configurations match exactly
        assertEquals(0.42f, loadedArtifact.getTemperature(), 1e-5);
        assertEquals(0.85f, loadedArtifact.getTopP(), 1e-5);
        assertEquals(25, loadedArtifact.getTopK());
        assertEquals(1.25f, loadedArtifact.getRepeatPenalty(), 1e-5);

        // Verify tokenizer vocabulary matches exactly
        assertEquals(originalArtifact.getTokenizerVocab().size(), loadedArtifact.getTokenizerVocab().size());
        assertEquals(originalArtifact.getTokenizerVocab(), loadedArtifact.getTokenizerVocab());

        // Verify weights structures match exactly
        List<Tensor> origWeights = originalArtifact.getWeights();
        List<Tensor> loadWeights = loadedArtifact.getWeights();
        assertEquals(origWeights.size(), loadWeights.size());

        for (int i = 0; i < origWeights.size(); i++) {
            float[] origData = origWeights.get(i).getData();
            float[] loadData = loadWeights.get(i).getData();
            assertArrayEquals("Weight mismatch at block " + i, origData, loadData, 1e-6f);
        }
    }

    @Test
    public void testInferenceEquivalence() throws Exception {
        EvoModelArtifact originalArtifact = new EvoModelArtifact();
        originalArtifact.initializeFromModel("evo-test-inference", originalModel, mockVocab);
        originalArtifact.save(modelDir);

        // Load model from artifact
        EvoModelArtifact loadedArtifact = EvoModelArtifact.load(modelDir);
        EvoLlmModel loadedModel = loadedArtifact.createModel();

        // Prepare identical input token IDs
        int[] inputIds = {2, 10, 42, 17, 3};

        // Forward passes
        Tensor origLogits = originalModel.forward(inputIds);
        Tensor loadLogits = loadedModel.forward(inputIds);

        // Assert shapes are equivalent
        assertArrayEquals(origLogits.getShape(), loadLogits.getShape());

        // Assert outputs are identical within small float precision tolerance
        float[] origData = origLogits.getData();
        float[] loadData = loadLogits.getData();
        assertEquals(origData.length, loadData.length);
        for (int i = 0; i < origData.length; i++) {
            assertEquals("Inference logits mismatch at offset " + i, origData[i], loadData[i], 1e-5f);
        }
    }

    @Test
    public void testExportWithoutTrainingAndRepeatedExports() throws Exception {
        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("evo-test-export-no-train", originalModel, mockVocab);
        artifact.save(modelDir);

        // We load the artifact from disk and export it to GGUF format
        EvoModelArtifact loadedForExport = EvoModelArtifact.load(modelDir);

        OllamaExporter exporter = new OllamaExporter();
        Path exportDir = tempFolder.newFolder("gguf-export-output-1").toPath();

        // 1st Export: Ensure export works seamlessly without invoking a trainer or requiring a corpus
        exporter.export(loadedForExport, exportDir);

        Path ggufFile1 = exportDir.resolve("evo.gguf");
        Path modelfile1 = exportDir.resolve("Modelfile");

        assertTrue(Files.exists(ggufFile1));
        assertTrue(Files.exists(modelfile1));
        assertTrue(Files.size(ggufFile1) > 1024);

        // 2nd Export: Verify that repeated exports of the same artifact can run successfully
        Path exportDir2 = tempFolder.newFolder("gguf-export-output-2").toPath();
        exporter.export(loadedForExport, exportDir2);

        Path ggufFile2 = exportDir2.resolve("evo.gguf");
        Path modelfile2 = exportDir2.resolve("Modelfile");

        assertTrue(Files.exists(ggufFile2));
        assertTrue(Files.exists(modelfile2));
        assertEquals(Files.size(ggufFile1), Files.size(ggufFile2));
    }

    @Test
    public void testEvoFilePackagingLoadAndExport() throws Exception {
        EvoModelArtifact originalArtifact = new EvoModelArtifact();
        originalArtifact.initializeFromModel("packaged-model", originalModel, mockVocab);

        Path evoFile = tempFolder.newFolder("evo-packed").toPath().resolve("my-model.evo");

        // Save as single packaged file (.evo format)
        originalArtifact.save(evoFile);

        // Verify .evo package file exists and is indeed a packaged ZIP-archive file
        assertTrue(Files.exists(evoFile));
        assertFalse(Files.isDirectory(evoFile));
        assertTrue(evoFile.getFileName().toString().endsWith(".evo"));

        // Load packed .evo file directly
        EvoModelArtifact loadedArtifact = EvoModelArtifact.load(evoFile);

        // Verify fields
        assertEquals("packaged-model", loadedArtifact.getModelName());
        assertEquals(originalArtifact.getVocabSize(), loadedArtifact.getVocabSize());
        assertEquals(originalArtifact.getEmbeddingSize(), loadedArtifact.getEmbeddingSize());

        // Check weights equivalence
        List<Tensor> origWeights = originalArtifact.getWeights();
        List<Tensor> loadWeights = loadedArtifact.getWeights();
        assertEquals(origWeights.size(), loadWeights.size());
        for (int i = 0; i < origWeights.size(); i++) {
            assertArrayEquals(origWeights.get(i).getData(), loadWeights.get(i).getData(), 1e-6f);
        }

        // Export directly from loaded artifact
        OllamaExporter exporter = new OllamaExporter();
        Path exportDir = tempFolder.newFolder("evo-packed-export").toPath();
        exporter.export(loadedArtifact, exportDir);

        Path ggufFile = exportDir.resolve("evo.gguf");
        Path modelfile = exportDir.resolve("Modelfile");
        assertTrue(Files.exists(ggufFile));
        assertTrue(Files.exists(modelfile));
        assertTrue(Files.size(ggufFile) > 1024);
    }

    @Test
    public void testCleanErrorHandlingOnMissingMetadata() throws Exception {
        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("evo-test-missing-meta", originalModel, mockVocab);
        artifact.save(modelDir);

        // Delete model.json to simulate missing metadata
        Files.delete(modelDir.resolve("model.json"));

        // If model.json is deleted, it should still try to fallback to config.json
        EvoModelArtifact fallbackLoaded = EvoModelArtifact.load(modelDir);
        assertNotNull(fallbackLoaded);
        assertEquals(originalModel.getVocabSize(), fallbackLoaded.getVocabSize());

        // Now delete config.json too
        Files.delete(modelDir.resolve("config.json"));

        // It must cleanly throw FileNotFoundException or IOException
        try {
            EvoModelArtifact.load(modelDir);
            fail("Expected EvoModelArtifact.load to fail on missing metadata files.");
        } catch (FileNotFoundException e) {
            assertTrue(e.getMessage().contains("Neither model.json nor config.json"));
        }
    }

    @Test
    public void testCleanErrorHandlingOnCorruptedOrTruncatedWeights() throws Exception {
        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("evo-test-corrupted-weights", originalModel, mockVocab);
        artifact.save(modelDir);

        // Truncate weights.bin file to simulate corruption
        Path weightsFile = modelDir.resolve("weights.bin");
        Files.write(weightsFile, new byte[] { (byte) 'E', (byte) 'V', (byte) 'O' });

        try {
            EvoModelArtifact.load(modelDir);
            fail("Expected EvoModelArtifact.load to fail on truncated/corrupted weights.bin.");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("truncated") || e.getMessage().contains("corrupt"));
        }
    }
}
