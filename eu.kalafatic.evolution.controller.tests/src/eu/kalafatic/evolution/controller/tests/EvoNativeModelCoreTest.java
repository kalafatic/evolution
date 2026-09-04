package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.agent.export.OllamaExporter;
import eu.kalafatic.evolution.forge.agent.gguf.GGUFValidationReport;
import eu.kalafatic.evolution.forge.agent.gguf.GGUFValidator;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.inference.ReferenceEvoInferenceEngine;
import eu.kalafatic.evolution.forge.model.llm.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class EvoNativeModelCoreTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ReferenceEvoInferenceEngine engine;

    @Before
    public void setUp() {
        engine = new ReferenceEvoInferenceEngine();
    }

    @Test
    public void testDeterministicSaveLoadRoundTrip() throws IOException {
        EvoLlmArchitecture arch = new EvoLlmArchitecture(20, 16, 2, 1, 32, 8);
        EvoLlmModel originalModel = new EvoLlmModel(arch);

        int[] inputIds = new int[]{1, 4, 7};
        Tensor originalLogits = engine.forward(originalModel, inputIds);
        assertNotNull(originalLogits);

        File saveDir = tempFolder.newFolder("saved_model");
        originalModel.save(saveDir.toPath());

        EvoLlmModel loadedModel = EvoLlmModel.load(saveDir.toPath());
        assertNotNull(loadedModel);

        Tensor loadedLogits = engine.forward(loadedModel, inputIds);
        assertNotNull(loadedLogits);

        assertArrayEquals(originalLogits.getShape(), loadedLogits.getShape());
        float[] origData = originalLogits.getData();
        float[] loadData = loadedLogits.getData();
        assertEquals(origData.length, loadData.length);

        for (int i = 0; i < origData.length; i++) {
            assertEquals("Logits must match exactly after save/load round-trip",
                    origData[i], loadData[i], 1e-6f);
        }
    }

    @Test
    public void testSnapshotImmutability() {
        EvoLlmArchitecture arch = new EvoLlmArchitecture(20, 16, 2, 1, 32, 8);
        EvoLlmModel model = new EvoLlmModel(arch);

        ModelSnapshot snapshot = model.createSnapshot();
        assertNotNull(snapshot);

        Tensor snapshotTokenEmbd = snapshot.getParameters().get("token_embd.weight");
        assertNotNull(snapshotTokenEmbd);
        float initialValue = snapshotTokenEmbd.getData()[0];

        // Mutate live model weights
        Tensor modelTokenEmbd = model.parameters().get(0);
        modelTokenEmbd.getData()[0] += 10.0f;

        // Verify snapshot remains unchanged
        assertEquals("Snapshot parameter data must not change when live model is mutated",
                initialValue, snapshotTokenEmbd.getData()[0], 1e-6f);
    }

    @Test
    public void testSnapshotExportAndGGUFValidation() throws Exception {
        EvoLlmArchitecture arch = new EvoLlmArchitecture(20, 16, 2, 1, 32, 8);
        EvoLlmModel model = new EvoLlmModel(arch);

        Map<Integer, String> vocab = new HashMap<>();
        vocab.put(0, "<unk>");
        vocab.put(1, "<s>");
        vocab.put(2, "</s>");
        for (int i = 3; i < 20; i++) {
            vocab.put(i, "token_" + i);
        }
        model.getIdToToken().putAll(vocab);

        ModelSnapshot snapshot = model.createSnapshot();
        Path exportDir = tempFolder.newFolder("gguf_export").toPath();

        OllamaExporter exporter = new OllamaExporter();
        exporter.exportSnapshot(snapshot, exportDir);

        Path ggufPath = exportDir.resolve("exports/ollama/evo.gguf");
        assertTrue(FilesExists(ggufPath));

        GGUFValidationReport report = GGUFValidator.validate(ggufPath, model, vocab);
        assertTrue("Exported GGUF from ModelSnapshot must pass validation", report.isValid());
    }

    private boolean FilesExists(Path path) {
        return path != null && path.toFile().exists();
    }
}
