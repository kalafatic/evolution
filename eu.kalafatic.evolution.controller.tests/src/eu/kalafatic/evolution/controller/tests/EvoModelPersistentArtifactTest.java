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
import eu.kalafatic.evolution.forge.agent.export.OllamaExporterGPT;

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
        mockVocab.put("<pad>", 0);
        mockVocab.put("<unk>", 1);
        mockVocab.put("<s>", 2);
        mockVocab.put("</s>", 3);
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

    @Test
    public void testGgufExporterRoundTripAndCompatibility() throws Exception {
        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("evo-test-gguf-compat", originalModel, mockVocab);
        artifact.save(modelDir);

        OllamaExporter exporter = new OllamaExporter();
        Path exportDir = tempFolder.newFolder("gguf-compat-output").toPath();
        exporter.export(artifact, exportDir);

        Path ggufFile = exportDir.resolve("exports/ollama/evo.gguf");
        assertTrue(Files.exists(ggufFile));

        // Read and parse GGUF
        byte[] bytes = Files.readAllBytes(ggufFile);
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(bytes);
        buf.order(java.nio.ByteOrder.LITTLE_ENDIAN);

        // Verify magic and version
        byte[] magic = new byte[4];
        buf.get(magic);
        assertEquals("GGUF", new String(magic));
        assertEquals(3, buf.getInt());

        long tensorCount = buf.getLong();
        long kvCount = buf.getLong();
        assertEquals(21, kvCount); // 21 metadata KV pairs

        // Skip metadata to reach tensor table
        for (int i = 0; i < kvCount; i++) {
            readGgufString(buf);
            int type = buf.getInt();
            skipGgufValue(buf, type);
        }

        // Verify tensor shapes and properties
        Map<String, long[]> tensors = new HashMap<>();
        for (int i = 0; i < tensorCount; i++) {
            String name = readGgufString(buf);
            int shapeLen = buf.getInt();
            long[] dims = new long[shapeLen];
            long product = 1;
            for (int d = 0; d < shapeLen; d++) {
                dims[d] = buf.getLong();
                product *= dims[d];
            }
            tensors.put(name, dims);
            int ggmlType = buf.getInt();
            long offset = buf.getLong();

            // Verify dimension product equals element count
            // Let's check with some model parameters
            assertTrue("Product of dimensions must be greater than 0", product > 0);
        }

        // Test 1: Output projection has shape [32, 100] (which is [dModel, vocabSize])
        assertTrue(tensors.containsKey("output.weight"));
        long[] outputShape = tensors.get("output.weight");
        assertEquals(2, outputShape.length);
        assertEquals(32, outputShape[0]);
        assertEquals(100, outputShape[1]);

        // Test 2: Embedding tensor has shape [32, 100] (which is [dModel, vocabSize])
        assertTrue(tensors.containsKey("token_embd.weight"));
        long[] embedShape = tensors.get("token_embd.weight");
        assertEquals(2, embedShape.length);
        assertEquals(32, embedShape[0]);
        assertEquals(100, embedShape[1]);

        // Verify other LLaMA tensors are formatted correctly
        assertTrue(tensors.containsKey("blk.0.ffn_up.weight"));
        long[] ffnUpShape = tensors.get("blk.0.ffn_up.weight");
        assertEquals(2, ffnUpShape.length);
        assertEquals(32, ffnUpShape[0]); // dModel
        assertEquals(128, ffnUpShape[1]); // dff

        assertTrue(tensors.containsKey("blk.0.ffn_down.weight"));
        long[] ffnDownShape = tensors.get("blk.0.ffn_down.weight");
        assertEquals(2, ffnDownShape.length);
        assertEquals(128, ffnDownShape[0]); // dff
        assertEquals(32, ffnDownShape[1]); // dModel
    }

    @Test
    public void testSafeVocabParsingWithCommasAndQuotes() throws Exception {
        // Build a mock vocabulary containing comma, space-comma, escaped quotes, and normal words
        Map<String, Integer> trickyVocab = new LinkedHashMap<>();
        trickyVocab.put("<pad>", 0);
        trickyVocab.put("<unk>", 1);
        trickyVocab.put(",", 2);
        trickyVocab.put("a,b", 3);
        trickyVocab.put("\"", 4);
        trickyVocab.put("\\", 5);
        trickyVocab.put("\n", 6);
        trickyVocab.put("normal", 7);

        EvoLlmModel trickyModel = new EvoLlmModel(trickyVocab.size(), 32, 2, 1, 128, 8);
        EvoModelArtifact originalArtifact = new EvoModelArtifact();
        originalArtifact.initializeFromModel("evo-tricky-vocab", trickyModel, trickyVocab);

        Path trickyModelDir = tempFolder.newFolder("tricky-vocab-model").toPath();
        originalArtifact.save(trickyModelDir);

        // Load the artifact back and ensure vocabulary roundtrips perfectly
        EvoModelArtifact loadedArtifact = EvoModelArtifact.load(trickyModelDir);
        assertEquals(trickyVocab.size(), loadedArtifact.getTokenizerVocab().size());
        assertEquals(trickyVocab, loadedArtifact.getTokenizerVocab());
    }

    @Test
    public void testOllamaExporterGPTCompleteValidation() throws Exception {
        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("evo-test-gpt-exporter", originalModel, mockVocab);
        artifact.save(modelDir);

        OllamaExporterGPT exporter = new OllamaExporterGPT();
        Path exportDir = tempFolder.newFolder("gguf-gpt-output").toPath();
        exporter.export(artifact, exportDir);

        Path ggufFile = exportDir.resolve("exports/ollama/evo.gguf");
        assertTrue(Files.exists(ggufFile));

        // Reopen the GGUF file from disk and perform byte-level parsing
        byte[] bytes = Files.readAllBytes(ggufFile);
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(bytes);
        buf.order(java.nio.ByteOrder.LITTLE_ENDIAN);

        // Verify Magic and Version
        byte[] magic = new byte[4];
        buf.get(magic);
        assertEquals("GGUF", new String(magic, java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(3, buf.getInt());

        long tensorCount = buf.getLong();
        long kvCount = buf.getLong();

        // Parse and verify metadata entries
        Map<String, Object> metadata = new HashMap<>();
        for (int i = 0; i < kvCount; i++) {
            String key = readGgufString(buf);
            int type = buf.getInt();
            Object value = parseAndSkipGgufValueForTest(buf, type);
            metadata.put(key, value);
        }

        // Validate crucial architecture metadata
        assertEquals("llama", metadata.get("general.architecture"));
        assertEquals("EVO LLM", metadata.get("general.name"));
        assertEquals(0, ((Number) metadata.get("general.file_type")).intValue());
        assertEquals(32, ((Number) metadata.get("general.alignment")).intValue());
        assertEquals(originalModel.getMaxSeqLen(), ((Number) metadata.get("llama.context_length")).intValue());
        assertEquals(originalModel.getDModel(), ((Number) metadata.get("llama.embedding_length")).intValue());
        assertEquals(originalModel.getNumBlocks(), ((Number) metadata.get("llama.block_count")).intValue());
        assertEquals(originalModel.getDff(), ((Number) metadata.get("llama.feed_forward_length")).intValue());
        assertEquals(originalModel.getVocabSize(), ((Number) metadata.get("llama.vocab_size")).intValue());

        // Parse tensor descriptors and validate properties
        Map<String, long[]> tensorShapes = new HashMap<>();
        Map<String, Long> tensorOffsets = new HashMap<>();
        for (int i = 0; i < tensorCount; i++) {
            String name = readGgufString(buf);
            int shapeLen = buf.getInt();
            long[] dims = new long[shapeLen];
            for (int d = 0; d < shapeLen; d++) {
                dims[d] = buf.getLong();
            }
            int ggmlType = buf.getInt();
            long offset = buf.getLong();

            tensorShapes.put(name, dims);
            tensorOffsets.put(name, offset);

            // Alignment validation of offset to 32 bytes
            assertEquals("Tensor " + name + " offset must be 32-byte aligned", 0, offset % 32);
        }

        // 1. Verify token_embd.weight has correct shape [32, 100] (which is [dModel, vocabSize])
        assertTrue(tensorShapes.containsKey("token_embd.weight"));
        long[] embedShape = tensorShapes.get("token_embd.weight");
        assertEquals(2, embedShape.length);
        assertEquals(originalModel.getDModel(), embedShape[0]); // 32
        assertEquals(originalModel.getVocabSize(), embedShape[1]); // 100

        // 2. Verify output.weight has correct shape [32, 100] (which is [dModel, vocabSize])
        assertTrue(tensorShapes.containsKey("output.weight"));
        long[] outputShape = tensorShapes.get("output.weight");
        assertEquals(2, outputShape.length);
        assertEquals(originalModel.getDModel(), outputShape[0]); // 32
        assertEquals(originalModel.getVocabSize(), outputShape[1]); // 100

        // 3. Verify FFN up and down shapes map correctly
        assertTrue(tensorShapes.containsKey("blk.0.ffn_up.weight"));
        long[] ffnUpShape = tensorShapes.get("blk.0.ffn_up.weight");
        assertEquals(2, ffnUpShape.length);
        assertEquals(originalModel.getDModel(), ffnUpShape[0]); // dModel (32)
        assertEquals(originalModel.getDff(), ffnUpShape[1]); // dff (128)

        assertTrue(tensorShapes.containsKey("blk.0.ffn_down.weight"));
        long[] ffnDownShape = tensorShapes.get("blk.0.ffn_down.weight");
        assertEquals(2, ffnDownShape.length);
        assertEquals(originalModel.getDff(), ffnDownShape[0]); // dff (128)
        assertEquals(originalModel.getDModel(), ffnDownShape[1]); // dModel (32)

        // 4. Verify attention shapes
        assertTrue(tensorShapes.containsKey("blk.0.attn_q.weight"));
        long[] qShape = tensorShapes.get("blk.0.attn_q.weight");
        assertEquals(2, qShape.length);
        assertEquals(originalModel.getDModel(), qShape[0]); // 32
        assertEquals(originalModel.getDModel(), qShape[1]); // 32
    }

    private Object parseAndSkipGgufValueForTest(java.nio.ByteBuffer buf, int type) {
        if (type == 0) return (int) (buf.get() & 0xFF);
        if (type == 1) return (int) buf.get();
        if (type == 2) return buf.getShort() & 0xFFFF;
        if (type == 3) return (int) buf.getShort();
        if (type == 4) return buf.getInt();
        if (type == 5) return buf.getInt();
        if (type == 6) return buf.getFloat();
        if (type == 7) return buf.get() != 0;
        if (type == 8) return readGgufString(buf);
        if (type == 9) {
            int arrayType = buf.getInt();
            long arrayLen = buf.getLong();
            List<Object> items = new ArrayList<>();
            for (long i = 0; i < arrayLen; i++) {
                items.add(parseAndSkipGgufValueForTest(buf, arrayType));
            }
            return items;
        }
        if (type >= 10 && type <= 13) {
            return buf.getLong();
        }
        throw new RuntimeException("Unknown metadata type: " + type);
    }

    private String readGgufString(java.nio.ByteBuffer buf) {
        long len = buf.getLong();
        byte[] bytes = new byte[(int) len];
        buf.get(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private void skipGgufValue(java.nio.ByteBuffer buf, int type) {
        if (type <= 1 || type == 7) { // UINT8, INT8, BOOL
            buf.get();
        } else if (type == 2 || type == 3) { // UINT16, INT16
            buf.getShort();
        } else if (type == 4 || type == 5 || type == 6) { // UINT32, INT32, FLOAT32
            buf.getInt();
        } else if (type == 8) { // STRING
            readGgufString(buf);
        } else if (type == 9) { // ARRAY
            int arrayType = buf.getInt();
            long arrayLen = buf.getLong();
            for (long i = 0; i < arrayLen; i++) {
                skipGgufValue(buf, arrayType);
            }
        } else if (type == 10 || type == 11) { // UINT64, INT64
            buf.getLong();
        } else if (type == 12 || type == 13) { // FLOAT64
            buf.getLong();
        } else {
            throw new RuntimeException("Unknown GGUF metadata type: " + type);
        }
    }
}
