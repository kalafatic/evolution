package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.controller.manager.ModelSizePreset;
import eu.kalafatic.evolution.forge.agent.export.HuggingFaceExporter;
import eu.kalafatic.evolution.forge.agent.export.OllamaExporter;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.model.llm.EvoTokenizerArtifact;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class EvoArchitectureAndExporterPipelineTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testArchitecturePreservationAcrossPresets() throws Exception {
        for (ModelSizePreset.Size preset : ModelSizePreset.Size.values()) {
            if (preset == ModelSizePreset.Size.CUSTOM) continue;

            EvoLlmArchitecture arch = preset.toArchitecture();
            assertNotNull(arch);

            assertEquals(preset.getVocabSize(), arch.getVocabSize());
            assertEquals(preset.getDModel(), arch.getDModel());
            assertEquals(preset.getNumHeads(), arch.getNumHeads());
            assertEquals(preset.getNumBlocks(), arch.getNumBlocks());
            assertEquals(preset.getDff(), arch.getDff());
            assertEquals(preset.getMaxSeqLen(), arch.getMaxSeqLen());

            // Instantiate float weight tensors only for models <= 50M params in unit tests to prevent OOM
            if (preset.getParameterCount() <= 50) {
                EvoLlmModel model = new EvoLlmModel(arch);
                assertEquals(arch, model.getArchitecture());

                Map<String, Integer> mockVocab = new HashMap<>();
                mockVocab.put("<unk>", 0);
                mockVocab.put("<s>", 1);
                mockVocab.put("</s>", 2);
                mockVocab.put(" ", 3);

                EvoModelArtifact artifact = new EvoModelArtifact();
                artifact.initializeFromModel("test-" + preset.name(), model, mockVocab);

                File saveFile = tempFolder.newFile("model-" + preset.name() + ".evo");
                artifact.save(saveFile.toPath());

                EvoModelArtifact loaded = EvoModelArtifact.load(saveFile.toPath());

                assertEquals("Loaded architecture vocabSize mismatch for " + preset.name(),
                        arch.getVocabSize(), loaded.getArchitectureConfig().getVocabSize());
                assertEquals("Loaded architecture dModel mismatch for " + preset.name(),
                        arch.getDModel(), loaded.getArchitectureConfig().getDModel());
                assertEquals("Loaded architecture numHeads mismatch for " + preset.name(),
                        arch.getNumHeads(), loaded.getArchitectureConfig().getNumHeads());
                assertEquals("Loaded architecture numBlocks mismatch for " + preset.name(),
                        arch.getNumBlocks(), loaded.getArchitectureConfig().getNumBlocks());
                assertEquals("Loaded architecture dff mismatch for " + preset.name(),
                        arch.getDff(), loaded.getArchitectureConfig().getDff());
                assertEquals("Loaded architecture maxSeqLen mismatch for " + preset.name(),
                        arch.getMaxSeqLen(), loaded.getArchitectureConfig().getMaxSeqLen());

                EvoLlmModel recreated = loaded.createModel();
                assertEquals("Recreated model architecture mismatch for " + preset.name(),
                        arch, recreated.getArchitecture());
            }
        }
    }

    @Test
    public void testTinyPresetRegression() {
        ModelSizePreset.Size tiny = ModelSizePreset.Size.TINY;
        EvoLlmArchitecture tinyArch = tiny.toArchitecture();

        assertEquals(8000, tinyArch.getVocabSize());
        assertEquals(384, tinyArch.getDModel());
        assertEquals(8, tinyArch.getNumHeads());
        assertEquals(6, tinyArch.getNumBlocks());
        assertEquals(1024, tinyArch.getDff());
        assertEquals(512, tinyArch.getMaxSeqLen());

        SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
        tokenizer.train("short corpus", tinyArch.getVocabSize());

        assertEquals("Tokenizer vocabulary must be padded to target architecture size",
                8000, tokenizer.getVocabSize());

        EvoLlmModel tinyModel = new EvoLlmModel(tinyArch);
        assertEquals(1024, tinyModel.getDff());
        assertEquals(8000, tinyModel.getVocabSize());
        assertNotEquals(1536, tinyModel.getDff());
        assertNotEquals(50, tinyModel.getVocabSize());
    }

    @Test
    public void testExportSizeSanityScaling() throws Exception {
        ModelSizePreset.Size[] scalingPresets = new ModelSizePreset.Size[] {
                ModelSizePreset.Size.NANO,
                ModelSizePreset.Size.MICRO,
                ModelSizePreset.Size.TINY
        };

        long prevParams = 0;
        long prevGgufSize = 0;

        for (ModelSizePreset.Size preset : scalingPresets) {
            EvoLlmArchitecture arch = preset.toArchitecture();
            long params = arch.getParameterCount();

            assertTrue("Parameter count must be positive for " + preset.name(), params > 0);
            if (prevParams > 0) {
                assertTrue("Parameter count must scale monotonically: " + preset.name() + " (" + params + ") > previous (" + prevParams + ")",
                        params > prevParams);
            }
            prevParams = params;

            EvoLlmModel model = new EvoLlmModel(arch);
            Map<String, Integer> vocab = new HashMap<>();
            vocab.put("<unk>", 0);
            vocab.put("<s>", 1);
            vocab.put("</s>", 2);
            vocab.put(" ", 3);

            EvoModelArtifact artifact = new EvoModelArtifact();
            artifact.initializeFromModel("export-" + preset.name(), model, vocab);

            File outDir = tempFolder.newFolder("out-" + preset.name());
            OllamaExporter exporter = new OllamaExporter();
            exporter.export(artifact, outDir.toPath());

            Path ggufPath = outDir.toPath().resolve("evo.gguf");
            assertTrue(Files.exists(ggufPath));
            long ggufSize = Files.size(ggufPath);

            if (prevGgufSize > 0) {
                assertTrue("Exported GGUF file size must scale monotonically: " + preset.name() + " (" + ggufSize + ") > previous (" + prevGgufSize + ")",
                        ggufSize > prevGgufSize);
            }
            prevGgufSize = ggufSize;
        }
    }

    @Test
    public void testHuggingFaceExporterSerialization() throws Exception {
        ModelSizePreset.Size tiny = ModelSizePreset.Size.TINY;
        EvoLlmArchitecture arch = tiny.toArchitecture();
        EvoLlmModel model = new EvoLlmModel(arch);

        Map<String, Integer> vocab = new HashMap<>();
        vocab.put("<unk>", 0);
        vocab.put("<s>", 1);
        vocab.put("</s>", 2);
        vocab.put(" ", 3);

        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("hf-tiny", model, vocab);

        File hfDir = tempFolder.newFolder("hf-out");
        HuggingFaceExporter exporter = new HuggingFaceExporter();
        exporter.export(artifact, hfDir.toPath());

        assertTrue(Files.exists(hfDir.toPath().resolve("config.json")));
        assertTrue(Files.exists(hfDir.toPath().resolve("tokenizer.json")));
        assertTrue(Files.exists(hfDir.toPath().resolve("model.bin")));

        String configContent = Files.readString(hfDir.toPath().resolve("config.json"));
        assertTrue(configContent.contains("\"vocab_size\": 8000"));
        assertTrue(configContent.contains("\"hidden_size\": 384"));
        assertTrue(configContent.contains("\"intermediate_size\": 1024"));
        assertTrue(configContent.contains("\"num_hidden_layers\": 6"));
        assertTrue(configContent.contains("\"num_attention_heads\": 8"));
    }
}
