package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.model.source.ForgeModelSource;
import eu.kalafatic.evolution.forge.model.source.ForgeModelSourceFactory;
import eu.kalafatic.evolution.forge.model.target.ForgeTarget;
import eu.kalafatic.evolution.forge.model.target.ForgeTargetDetector;
import eu.kalafatic.evolution.forge.model.target.ForgeTargetType;
import eu.kalafatic.evolution.forge.model.target.ForgeTargetValidator;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForgeTargetTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void test1_TrainingDataTarget() throws Exception {
        File txtFile = tempFolder.newFile("dataset.txt");
        Files.writeString(txtFile.toPath(), "Sample evolution dataset content for testing.");

        ForgeTarget target = ForgeTargetDetector.detect(txtFile.getAbsolutePath());
        Assert.assertEquals(ForgeTargetType.TRAINING_DATA, target.getType());
        Assert.assertTrue(target.isValid());

        ForgeModelSource source = ForgeModelSourceFactory.createSource(target);
        Assert.assertEquals("NEW_FORGE", source.getForgeMode());
        Assert.assertFalse(source.isPretrained());
        Assert.assertNull(source.getParentIdentifier());

        EvoLlmModel model = source.createOrRestoreModel(500, 128, 4, 3, 256, 64);
        Assert.assertNotNull(model);
        Assert.assertEquals(500, model.getVocabSize());
        Assert.assertEquals(128, model.getDModel());
    }

    @Test
    public void test2_EvoWorkspaceTarget() throws Exception {
        File wsDir = tempFolder.newFolder("evo-workspace-001");
        EvoLlmModel originalModel = new EvoLlmModel(448, 128, 4, 2, 256, 64);

        Map<String, Integer> vocab = new HashMap<>();
        vocab.put("<unk>", 0);
        vocab.put("<s>", 1);
        vocab.put("</s>", 2);
        vocab.put("evolution", 3);

        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("workspace-model", originalModel, vocab);
        artifact.saveToDirectory(wsDir.toPath());

        ForgeTarget target = ForgeTargetDetector.detect(wsDir.getAbsolutePath());
        Assert.assertEquals(ForgeTargetType.EVO_WORKSPACE, target.getType());
        Assert.assertTrue(target.isValid());
        Assert.assertTrue(target.getStatusMessage().contains("Valid Forge workspace"));

        ForgeModelSource source = ForgeModelSourceFactory.createSource(target);
        Assert.assertEquals("FROM_EVO_WORKSPACE", source.getForgeMode());
        Assert.assertTrue(source.isPretrained());
        Assert.assertEquals("workspace-model", source.getParentIdentifier());

        EvoLlmModel restoredModel = source.createOrRestoreModel(448, 128, 4, 2, 256, 64);
        Assert.assertNotNull(restoredModel);
        Assert.assertEquals(originalModel.parameters().size(), restoredModel.parameters().size());

        // Verify restored weights match original
        List<Tensor> origParams = originalModel.parameters();
        List<Tensor> restParams = restoredModel.parameters();
        for (int i = 0; i < origParams.size(); i++) {
            Assert.assertArrayEquals(origParams.get(i).getData(), restParams.get(i).getData(), 0.0001f);
        }
    }

    @Test
    public void test3_EvoModelTarget() throws Exception {
        File modelFile = tempFolder.newFile("model-base.evo");
        EvoLlmModel baseModel = new EvoLlmModel(448, 256, 8, 4, 512, 128);

        Map<String, Integer> vocab = new HashMap<>();
        vocab.put("<unk>", 0);
        vocab.put("<s>", 1);
        vocab.put("</s>", 2);
        vocab.put("hello", 3);
        vocab.put("world", 4);

        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("base-model", baseModel, vocab);
        artifact.save(modelFile.toPath());

        ForgeTarget target = ForgeTargetDetector.detect(modelFile.getAbsolutePath());
        Assert.assertEquals(ForgeTargetType.EVO_MODEL, target.getType());
        Assert.assertTrue(target.isValid());
        Assert.assertTrue(target.getStatusMessage().contains("Valid EVO model"));

        ForgeModelSource source = ForgeModelSourceFactory.createSource(target);
        Assert.assertEquals("FROM_EVO_MODEL", source.getForgeMode());
        Assert.assertTrue(source.isPretrained());
        Assert.assertEquals("base-model", source.getParentIdentifier());

        SimpleBPETokenizer tokenizer = source.getTokenizer("new training corpus", 448);
        Assert.assertEquals(vocab.size(), tokenizer.getVocabSize());
        Assert.assertEquals(Integer.valueOf(3), tokenizer.getVocab().get("hello"));

        EvoLlmModel restoredModel = source.createOrRestoreModel(448, 256, 8, 4, 512, 128);
        Assert.assertNotNull(restoredModel);
        Assert.assertEquals(256, restoredModel.getDModel());
    }

    @Test
    public void test4_CorruptedEvoFile() throws Exception {
        File corruptedFile = tempFolder.newFile("corrupted.evo");
        Files.writeString(corruptedFile.toPath(), "GARBAGE_BYTES_NOT_AN_EVO_FILE_DATA");

        ForgeTarget target = ForgeTargetDetector.detect(corruptedFile.getAbsolutePath());
        Assert.assertEquals(ForgeTargetType.EVO_MODEL, target.getType());
        Assert.assertFalse(target.isValid());
        Assert.assertTrue(target.getStatusMessage().contains("Invalid EVO model file"));

        try {
            ForgeTargetValidator.validateForForging(target);
            Assert.fail("Expected IllegalArgumentException on corrupted target");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("Cannot forge from target"));
        }
    }

    @Test
    public void test5_IncompatibleArchitectureValidation() {
        ForgeTarget target = new ForgeTarget("/path/to/missing.evo", ForgeTargetType.EVO_MODEL, false, "Path does not exist");
        try {
            ForgeTargetValidator.validateForForging(target);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("Cannot forge from target"));
        }
    }

    @Test
    public void test6_CriticalRoundTripRegressionTest() throws Exception {
        // Concept: model-001.evo -> Import -> Forge model -> Export -> model-001-copy.evo
        File model001 = tempFolder.newFile("model-001.evo");
        EvoLlmModel originalModel = new EvoLlmModel(512, 256, 8, 4, 1024, 128);

        Map<String, Integer> originalVocab = new HashMap<>();
        originalVocab.put("<unk>", 0);
        originalVocab.put("<s>", 1);
        originalVocab.put("</s>", 2);
        originalVocab.put("evolution", 3);
        originalVocab.put("genome", 4);
        originalVocab.put("forge", 5);

        EvoModelArtifact originalArtifact = new EvoModelArtifact();
        originalArtifact.initializeFromModel("model-001", originalModel, originalVocab);
        originalArtifact.save(model001.toPath());

        // 1. Import model-001.evo
        ForgeTarget target = ForgeTargetDetector.detect(model001.getAbsolutePath());
        Assert.assertTrue(target.isValid());
        ForgeModelSource source = ForgeModelSourceFactory.createSource(target);

        // 2. Restore model & tokenizer without training
        EvoLlmModel restoredModel = source.createOrRestoreModel(512, 256, 8, 4, 1024, 128);
        SimpleBPETokenizer restoredTokenizer = source.getTokenizer("", 512);

        // 3. Export to model-001-copy.evo
        File model001Copy = tempFolder.newFile("model-001-copy.evo");
        EvoModelArtifact exportedArtifact = new EvoModelArtifact();
        exportedArtifact.initializeFromModel("model-001-copy", restoredModel, restoredTokenizer.getVocab());
        exportedArtifact.getMetadata().put("forge_mode", source.getForgeMode());
        if (source.getParentIdentifier() != null) {
            exportedArtifact.getMetadata().put("parent_model", source.getParentIdentifier());
        }
        exportedArtifact.save(model001Copy.toPath());

        // 4. Load model-001-copy.evo and verify complete equivalence
        EvoModelArtifact reloadedCopy = EvoModelArtifact.load(model001Copy.toPath());

        Assert.assertEquals(originalArtifact.getVocabSize(), reloadedCopy.getVocabSize());
        Assert.assertEquals(originalArtifact.getDModel(), reloadedCopy.getDModel());
        Assert.assertEquals(originalArtifact.getNumHeads(), reloadedCopy.getNumHeads());
        Assert.assertEquals(originalArtifact.getNumBlocks(), reloadedCopy.getNumBlocks());
        Assert.assertEquals(originalArtifact.getDff(), reloadedCopy.getDff());
        Assert.assertEquals(originalArtifact.getMaxSeqLen(), reloadedCopy.getMaxSeqLen());
        Assert.assertEquals(originalArtifact.getParameterCount(), reloadedCopy.getParameterCount());

        // Verify tensor counts and shapes
        List<Tensor> origTensors = originalArtifact.getWeights();
        List<Tensor> copyTensors = reloadedCopy.getWeights();
        Assert.assertEquals(origTensors.size(), copyTensors.size());

        for (int i = 0; i < origTensors.size(); i++) {
            Tensor t1 = origTensors.get(i);
            Tensor t2 = copyTensors.get(i);
            Assert.assertArrayEquals("Tensor " + i + " shape mismatch", t1.getShape(), t2.getShape());
            Assert.assertArrayEquals("Tensor " + i + " data mismatch", t1.getData(), t2.getData(), 0.0001f);
        }

        // Verify tokenizer & vocabulary equivalence
        Assert.assertEquals(originalVocab.size(), reloadedCopy.getTokenizerVocab().size());
        for (Map.Entry<String, Integer> entry : originalVocab.entrySet()) {
            Assert.assertEquals("Vocabulary mismatch for token: " + entry.getKey(),
                    entry.getValue(), reloadedCopy.getTokenizerVocab().get(entry.getKey()));
        }
    }
}
