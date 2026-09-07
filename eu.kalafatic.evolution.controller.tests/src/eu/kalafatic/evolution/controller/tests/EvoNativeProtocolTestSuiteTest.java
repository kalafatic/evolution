package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.agent.gguf.GGUFValidator;
import eu.kalafatic.evolution.forge.agent.gguf.GGUFValidationReport;
import eu.kalafatic.evolution.forge.agent.export.OllamaExporter;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.inference.EvoModelValidator;
import eu.kalafatic.evolution.forge.model.inference.InferenceRequest;
import eu.kalafatic.evolution.forge.model.inference.InferenceResult;
import eu.kalafatic.evolution.forge.model.inference.ReferenceEvoInferenceEngine;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.model.protocol.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Protocol test suite covering:
 * - Golden Model Test
 * - Round-Trip Test
 * - Corruption Tests
 * - Cross-Component Contract Test
 */
public class EvoNativeProtocolTestSuiteTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private EvoLlmModel createSmallDeterministicModel() {
        // Small architecture: vocab=16, dModel=8, heads=2, blocks=1, dff=16, maxSeq=16
        EvoLlmModel model = new EvoLlmModel(16, 8, 2, 1, 16, 16);
        Random rng = new Random(42);
        for (Tensor p : model.parameters()) {
            float[] d = p.getData();
            for (int i = 0; i < d.length; i++) {
                d[i] = (rng.nextFloat() - 0.5f) * 0.1f;
            }
        }
        Map<Integer, String> vocab = new LinkedHashMap<>();
        for (int i = 0; i < 16; i++) {
            vocab.put(i, "tok_" + i);
        }
        vocab.put(1, "<s>");
        vocab.put(2, "</s>");
        model.getIdToToken().putAll(vocab);
        return model;
    }

    @Test
    public void testGoldenModelWriteReadAndLogits() throws Exception {
        EvoLlmModel originalModel = createSmallDeterministicModel();
        Map<String, Integer> tokenToId = new LinkedHashMap<>();
        originalModel.getIdToToken().forEach((id, tok) -> tokenToId.put(tok, id));

        EvoModelArtifact originalArtifact = new EvoModelArtifact();
        originalArtifact.initializeFromModel("golden_model", originalModel, tokenToId);

        File goldenFile = tempFolder.newFile("golden.evo");
        originalArtifact.saveToFile(goldenFile.toPath());

        assertTrue("Golden file must exist and be non-empty", goldenFile.exists() && goldenFile.length() > 0);

        EvoModelArtifact loadedArtifact = EvoModelArtifact.load(goldenFile.toPath());
        assertNotNull("Loaded golden artifact must not be null", loadedArtifact);
        assertEquals("Golden model content hash must remain identical",
                originalArtifact.getModelContentHash(), loadedArtifact.getModelContentHash());

        ReferenceEvoInferenceEngine engine = new ReferenceEvoInferenceEngine();
        InferenceRequest req = InferenceRequest.builder()
                .inputIds(new int[]{1, 4, 5})
                .maxTokens(3)
                .temperature(0.0f) // Deterministic greedy
                .build();

        InferenceResult originalResult = engine.generateFromArtifact(originalArtifact, req, null);
        InferenceResult loadedResult = engine.generateFromArtifact(loadedArtifact, req, null);

        assertArrayEquals("Deterministic golden model output token sequence must match exactly",
                originalResult.getGeneratedTokenIds(), loadedResult.getGeneratedTokenIds());
    }

    @Test
    public void testRoundTripIdentity() throws Exception {
        EvoLlmModel model = createSmallDeterministicModel();
        Map<String, Integer> tokenToId = new LinkedHashMap<>();
        model.getIdToToken().forEach((id, tok) -> tokenToId.put(tok, id));

        EvoModelArtifact originalArtifact = new EvoModelArtifact();
        originalArtifact.initializeFromModel("roundtrip_model", model, tokenToId);

        File evoFile = tempFolder.newFile("roundtrip.evo");
        originalArtifact.saveToFile(evoFile.toPath());

        EvoModelArtifact loadedArtifact = EvoModelArtifact.load(evoFile.toPath());

        assertEquals(originalArtifact.getVocabSize(), loadedArtifact.getVocabSize());
        assertEquals(originalArtifact.getDModel(), loadedArtifact.getDModel());
        assertEquals(originalArtifact.getNumHeads(), loadedArtifact.getNumHeads());
        assertEquals(originalArtifact.getNumBlocks(), loadedArtifact.getNumBlocks());
        assertEquals(originalArtifact.getManifest().size(), loadedArtifact.getManifest().size());

        List<float[]> origWeights = originalArtifact.getWeightData();
        List<float[]> loadedWeights = loadedArtifact.getWeightData();

        for (int i = 0; i < origWeights.size(); i++) {
            assertArrayEquals("Float array at tensor index " + i + " must match exactly",
                    origWeights.get(i), loadedWeights.get(i), 1e-7f);
        }

        assertEquals(originalArtifact.getModelContentHash(), loadedArtifact.getModelContentHash());
    }

    @Test
    public void testCorruptionDiagnostics() throws Exception {
        EvoLlmModel model = createSmallDeterministicModel();
        Map<String, Integer> tokenToId = new LinkedHashMap<>();
        model.getIdToToken().forEach((id, tok) -> tokenToId.put(tok, id));

        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("corruption_test", model, tokenToId);

        File validFile = tempFolder.newFile("valid.evo");
        artifact.saveToFile(validFile.toPath());

        EvoModelIntegrity validIntegrity = EvoModelValidator.validateEvoFile(validFile.toPath());
        assertTrue("Valid model must pass integrity check", validIntegrity.isValid());

        File corruptFile = tempFolder.newFile("corrupt.evo");
        java.nio.file.Files.copy(validFile.toPath(), corruptFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        try (RandomAccessFile raf = new RandomAccessFile(corruptFile, "rw")) {
            raf.seek(raf.length() - 50);
            raf.write(new byte[]{0x7F, 0x7F, 0x7F, 0x7F});
        }

        EvoModelIntegrity corruptIntegrity = EvoModelValidator.validateEvoFile(corruptFile.toPath());
        assertFalse("Corrupted model file must fail integrity check", corruptIntegrity.isValid());
        assertFalse("Integrity report must contain diagnostic errors", corruptIntegrity.getErrors().isEmpty());
    }

    @Test
    public void testCrossComponentContractPipeline() throws Exception {
        EvoLlmModel model = createSmallDeterministicModel();
        Map<String, Integer> tokenToId = new LinkedHashMap<>();
        model.getIdToToken().forEach((id, tok) -> tokenToId.put(tok, id));

        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("contract_model", model, tokenToId);

        File evoFile = tempFolder.newFile("contract_model.evo");
        artifact.saveToFile(evoFile.toPath());

        // 1. Validator PASS
        EvoModelIntegrity integrity = EvoModelValidator.validateEvoFile(evoFile.toPath());
        assertTrue("Validator check must PASS for pipeline artifact", integrity.isValid());

        // 2. Native Inference PASS
        ReferenceEvoInferenceEngine engine = new ReferenceEvoInferenceEngine();
        InferenceRequest req = InferenceRequest.builder()
                .inputIds(new int[]{1, 2})
                .maxTokens(2)
                .build();
        InferenceResult result = engine.generateFromEvoFile(evoFile.toPath(), req);
        assertNotNull("Native inference result must be produced", result);
        assertTrue("Tokens generated must be > 0", result.getGeneratedTokenCount() > 0);

        // 3. GGUF Export PASS
        File exportDir = tempFolder.newFolder("gguf_export");
        OllamaExporter exporter = new OllamaExporter();
        exporter.export(artifact, exportDir.toPath());

        File ggufFile = new File(exportDir, "evo.gguf");
        assertTrue("Exported evo.gguf must exist", ggufFile.exists());

        // 4. GGUF Validation PASS
        GGUFValidator validator = new GGUFValidator();
        GGUFValidationReport ggufResult = validator.validate(ggufFile.toPath(), artifact);
        assertTrue("GGUF Validation must PASS", ggufResult.isValid());
    }
}
