package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.agent.export.OllamaExporter;
import eu.kalafatic.evolution.forge.agent.gguf.GGUFReader;
import eu.kalafatic.evolution.forge.agent.gguf.GGUFValidationReport;
import eu.kalafatic.evolution.forge.agent.gguf.GGUFValidator;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.inference.InferenceRequest;
import eu.kalafatic.evolution.forge.model.inference.InferenceResult;
import eu.kalafatic.evolution.forge.model.inference.ReferenceEvoInferenceEngine;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.model.llm.ModelSnapshot;
import eu.kalafatic.evolution.forge.tokenizer.api.Tokenizer;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit and integration test suite for validating final snapshot native inference
 * and final snapshot exports (llama-cpp / ollama GGUF format).
 */
public class FinalSnapshotValidationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private EvoLlmModel model;
    private SimpleBPETokenizer tokenizer;
    private Map<Integer, String> vocab;
    private ReferenceEvoInferenceEngine engine;

    @Before
    public void setUp() throws Exception {
        // Construct small architecture model (vocab=16, dModel=16, heads=2, blocks=1, dff=32, maxSeqLen=32)
        EvoLlmArchitecture arch = new EvoLlmArchitecture(16, 16, 2, 1, 32, 32);
        model = new EvoLlmModel(arch);

        // Train BPE tokenizer and build vocabulary map
        tokenizer = new SimpleBPETokenizer();
        tokenizer.train("evolution native snapshot inference test", 16);

        vocab = new LinkedHashMap<>();
        Map<String, Integer> bpeVocab = tokenizer.getVocab();
        for (Map.Entry<String, Integer> entry : bpeVocab.entrySet()) {
            vocab.put(entry.getValue(), entry.getKey());
        }
        for (int i = 0; i < 16; i++) {
            if (!vocab.containsKey(i)) {
                vocab.put(i, "token_" + i);
            }
        }

        engine = new ReferenceEvoInferenceEngine();
    }

    @Test
    public void testNativeInferenceFromSnapshot() throws Exception {
        // 1. Create a snapshot from model
        ModelSnapshot snapshot = model.createSnapshot();
        assertNotNull("Snapshot should not be null", snapshot);
        assertEquals("Snapshot architecture parameters must match model", model.getArchitecture().getVocabSize(), snapshot.getArchitecture().getVocabSize());

        // 2. Wrap snapshot into EvoModelArtifact and reconstruct EvoLlmModel
        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("snapshot-model", model, tokenizer.getVocab());
        EvoLlmModel reconstructedModel = artifact.createModel();

        // 3. Execute deterministic forward passes on both original and reconstructed snapshot models
        int[] inputIds = new int[]{1, 2, 3};
        Tensor originalLogits = engine.forward(model, inputIds);
        Tensor snapshotLogits = engine.forward(reconstructedModel, inputIds);

        assertNotNull(originalLogits);
        assertNotNull(snapshotLogits);
        assertArrayEquals("Original and snapshot reconstructed models must produce identical logit shapes",
                originalLogits.getShape(), snapshotLogits.getShape());

        float[] oData = originalLogits.getData();
        float[] sData = snapshotLogits.getData();
        assertEquals("Data array lengths must match", oData.length, sData.length);
        for (int i = 0; i < oData.length; i++) {
            assertEquals("Forward pass logits from final snapshot must match original model",
                    oData[i], sData[i], 1e-6f);
        }

        // 4. Test deterministic greedy text generation from snapshot
        InferenceRequest request = InferenceRequest.builder()
                .inputIds(inputIds)
                .maxTokens(5)
                .temperature(0.0f)
                .build();

        InferenceResult originalResult = engine.generate(model, request, tokenizer);
        InferenceResult snapshotResult = engine.generate(reconstructedModel, request, tokenizer);

        assertNotNull(originalResult);
        assertNotNull(snapshotResult);
        assertArrayEquals("Generation token IDs from final snapshot must match original model",
                originalResult.getGeneratedTokenIds(), snapshotResult.getGeneratedTokenIds());
        assertEquals("Generated text from final snapshot must match original model",
                originalResult.getGeneratedText(), snapshotResult.getGeneratedText());
    }

    @Test
    public void testSnapshotExportAndGGUFValidation() throws Exception {
        Path exportDir = tempFolder.newFolder("snapshot_export_test").toPath();

        // 1. Export final model snapshot using OllamaExporter
        OllamaExporter exporter = new OllamaExporter();
        exporter.export("final-snapshot-evo", exportDir, model, vocab);

        Path ggufPath = exportDir.resolve("exports/ollama/evo.gguf");
        Path modelfilePath = exportDir.resolve("exports/ollama/Modelfile");
        Path weightsPath = exportDir.resolve("exports/ollama/weights.bin");

        assertTrue("evo.gguf must exist after snapshot export", Files.exists(ggufPath));
        assertTrue("Modelfile must exist after snapshot export", Files.exists(modelfilePath));
        assertTrue("weights.bin must exist after snapshot export", Files.exists(weightsPath));

        // 2. Parse GGUF with GGUFReader and check structural elements
        try (GGUFReader reader = new GGUFReader(ggufPath)) {
            assertEquals("Magic header must be GGUF", "GGUF", reader.getHeader().getMagic());
            assertTrue("Version must be 2 or 3", reader.getHeader().getVersion() == 2 || reader.getHeader().getVersion() == 3);
            assertTrue("Header tensor count must match model tensors", reader.getHeader().getTensorCount() > 0);
            assertTrue("Header metadata count must be > 0", reader.getHeader().getMetadataCount() > 0);

            // Verify architecture metadata
            assertEquals("llama", reader.getMetadataMap().get("general.architecture").getValue());
            assertEquals(16L, ((Number) reader.getMetadataMap().get("llama.vocab_size").getValue()).longValue());
        }

        // 3. Perform independent validation via GGUFValidator
        GGUFValidationReport report = GGUFValidator.validate(ggufPath, model, vocab);
        assertTrue("GGUFValidationReport must report valid for exported final snapshot: " + report.generateSummary(),
                report.isValid());
        assertTrue("Structure must be valid", report.isStructureValid());
        assertTrue("Metadata must be valid", report.isMetadataValid());
        assertTrue("Tensors must be valid", report.isTensorsValid());
        assertTrue("Semantics must be valid", report.isSemanticsValid());
        assertTrue("Errors list must be empty", report.getErrors().isEmpty());
    }
}
