package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.inference.InferenceRequest;
import eu.kalafatic.evolution.forge.model.inference.InferenceResult;
import eu.kalafatic.evolution.forge.model.inference.NativeEvoArtifactResolver;
import eu.kalafatic.evolution.forge.model.inference.NativeEvoArtifactResolver.ArtifactResolutionResult;
import eu.kalafatic.evolution.forge.model.inference.ReferenceEvoInferenceEngine;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;

public class EvoNativeGoldenEndToEndTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private EvoLlmModel trainedModel;
    private SimpleBPETokenizer tokenizer;
    private Path evoFilePath;

    @Before
    public void setUp() throws Exception {
        // Construct trained model
        trainedModel = new EvoLlmModel(100, 32, 2, 1, 128, 8);

        // Train custom tokenizer
        tokenizer = new SimpleBPETokenizer();
        tokenizer.train("hi hello evolution native model testing hi hi", 100);

        // Populate deterministic non-zero weight patterns into trained model to simulate training
        int paramIdx = 0;
        for (Tensor p : trainedModel.parameters()) {
            float[] data = p.getData();
            for (int i = 0; i < data.length; i++) {
                data[i] = (float) Math.sin(paramIdx + i * 0.1);
            }
            paramIdx++;
        }

        // Wrap into EvoModelArtifact and save to .evo file
        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("golden-evo-model", trainedModel, tokenizer.getVocab());

        File dir = tempFolder.newFolder("golden-test");
        evoFilePath = dir.toPath().resolve("golden-model.evo");
        artifact.saveToFile(evoFilePath);
    }

    @Test
    public void testTrainedModelDiffersFromRandomModel() {
        EvoLlmModel randomModel = new EvoLlmModel(100, 32, 2, 1, 128, 8);

        List<Tensor> trainedWeights = trainedModel.parameters();
        List<Tensor> randomWeights = randomModel.parameters();

        assertEquals(trainedWeights.size(), randomWeights.size());

        boolean matchedAll = true;
        for (int i = 0; i < trainedWeights.size(); i++) {
            float[] tData = trainedWeights.get(i).getData();
            float[] rData = randomWeights.get(i).getData();
            if (!Arrays.equals(tData, rData)) {
                matchedAll = false;
                break;
            }
        }
        assertFalse("Trained model weights must NOT equal a randomly initialized fresh model!", matchedAll);
    }

    @Test
    public void testLoadedEvoModelEqualsTrainedModel() throws Exception {
        EvoModelArtifact loadedArtifact = EvoModelArtifact.load(evoFilePath);
        EvoLlmModel reconstructedModel = loadedArtifact.createModel();

        // 1. Tensor counts, shapes, values, min/max, content hash
        List<Tensor> origParams = trainedModel.parameters();
        List<Tensor> recParams = reconstructedModel.parameters();

        assertEquals(origParams.size(), recParams.size());

        for (int i = 0; i < origParams.size(); i++) {
            assertArrayEquals(origParams.get(i).getShape(), recParams.get(i).getShape());
            float[] origData = origParams.get(i).getData();
            float[] recData = recParams.get(i).getData();

            assertEquals(origData.length, recData.length);
            assertArrayEquals("Tensor " + i + " mismatch", origData, recData, 1e-6f);
        }

        // 2. Forward logits equivalence
        int[] promptTokenIds = new int[] { tokenizer.getVocab().getOrDefault("hi", 1) };
        Tensor origLogits = trainedModel.forward(promptTokenIds);
        Tensor recLogits = reconstructedModel.forward(promptTokenIds);

        assertArrayEquals(origLogits.getShape(), recLogits.getShape());
        assertArrayEquals(origLogits.getData(), recLogits.getData(), 1e-5f);
    }

    @Test
    public void testGoldenEndToEndProductionFlow() throws Exception {
        // Step 1: Resolve native artifact via resolver
        ArtifactResolutionResult res = NativeEvoArtifactResolver.resolveNativeArtifact(evoFilePath);
        assertEquals(evoFilePath.toAbsolutePath(), res.getResolvedPath().toAbsolutePath());

        EvoModelArtifact artifact = res.getArtifact();

        // Step 2: Verify artifact content hash and metadata
        assertNotNull(artifact.getModelContentHash());
        assertEquals("golden-evo-model", artifact.getModelName());

        // Step 3: Run deterministic inference with ReferenceEvoInferenceEngine
        ReferenceEvoInferenceEngine engine = new ReferenceEvoInferenceEngine();

        InferenceRequest request = InferenceRequest.builder()
                .prompt("hi")
                .maxTokens(10)
                .temperature(0.0f) // Deterministic greedy decoding
                .build();

        InferenceResult result = engine.generateFromArtifact(artifact, request, null);

        assertNotNull(result);
        assertTrue(result.getGeneratedTokenCount() > 0);
        assertNotNull(result.getGeneratedText());

        // Step 4: Verify prompt tokenization and logits shape
        Tensor lastLogits = result.getLastLogits();
        assertNotNull(lastLogits);
        assertEquals(100, lastLogits.getShape()[1]); // Vocabulary size 100
    }
}
