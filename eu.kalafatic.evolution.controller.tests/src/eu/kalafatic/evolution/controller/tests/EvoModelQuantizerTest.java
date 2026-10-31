package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.model.inference.InferenceRequest;
import eu.kalafatic.evolution.forge.model.inference.InferenceResult;
import eu.kalafatic.evolution.forge.model.inference.ReferenceEvoInferenceEngine;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.model.llm.EvoModelQuantizer;
import eu.kalafatic.evolution.forge.model.llm.QuantizationConfig;
import eu.kalafatic.evolution.forge.model.protocol.EvoDtype;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class EvoModelQuantizerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private EvoModelArtifact sourceArtifact;
    private Map<String, Integer> vocab;

    @Before
    public void setUp() {
        vocab = new LinkedHashMap<>();
        vocab.put("<unk>", 0);
        vocab.put("<s>", 1);
        vocab.put("</s>", 2);
        vocab.put("hello", 3);
        vocab.put("world", 4);

        EvoLlmModel model = new EvoLlmModel(5, 32, 2, 2, 64, 32);
        sourceArtifact = new EvoModelArtifact();
        sourceArtifact.initializeFromModel("source-model", model, vocab);
    }

    @Test
    public void testF16Quantization() throws Exception {
        EvoModelQuantizer quantizer = new EvoModelQuantizer();
        QuantizationConfig config = new QuantizationConfig(QuantizationConfig.QuantizationScheme.F16);

        EvoModelQuantizer.QuantizationResult result = quantizer.quantizeArtifact(sourceArtifact, config);

        assertNotNull(result);
        assertNotNull(result.getQuantizedArtifact());
        assertTrue(result.getQuantizedSize() < result.getOriginalSize());
        assertEquals(0.5, result.getCompressionRatio(), 0.05);

        EvoModelArtifact quantArtifact = result.getQuantizedArtifact();
        assertEquals("true", quantArtifact.getMetadata().get("quantized"));
        assertEquals("F16", quantArtifact.getMetadata().get("quantization_scheme"));

        Path savePath = tempFolder.newFile("model-f16.evo").toPath();
        quantArtifact.save(savePath);

        EvoModelArtifact reloaded = EvoModelArtifact.load(savePath);
        assertNotNull(reloaded);
        assertEquals("F16", reloaded.getMetadata().get("quantization_scheme"));

        // Verify native inference
        ReferenceEvoInferenceEngine engine = new ReferenceEvoInferenceEngine();
        InferenceRequest request = InferenceRequest.builder().prompt("hello").maxTokens(5).temperature(0.0f).build();
        InferenceResult inferResult = engine.generateFromArtifact(reloaded, request, null);
        assertNotNull(inferResult);
        assertNotNull(inferResult.getGeneratedText());
    }

    @Test
    public void testInt8Quantization() throws Exception {
        EvoModelQuantizer quantizer = new EvoModelQuantizer();
        QuantizationConfig config = new QuantizationConfig(QuantizationConfig.QuantizationScheme.INT8);

        EvoModelQuantizer.QuantizationResult result = quantizer.quantizeArtifact(sourceArtifact, config);

        assertNotNull(result);
        assertTrue(result.getQuantizedSize() < result.getOriginalSize());
        assertEquals(0.25, result.getCompressionRatio(), 0.05);

        EvoModelArtifact quantArtifact = result.getQuantizedArtifact();
        assertEquals("INT8", quantArtifact.getMetadata().get("quantization_scheme"));

        Path savePath = tempFolder.newFile("model-int8.evo").toPath();
        quantArtifact.save(savePath);

        EvoModelArtifact reloaded = EvoModelArtifact.load(savePath);
        assertNotNull(reloaded);

        ReferenceEvoInferenceEngine engine = new ReferenceEvoInferenceEngine();
        InferenceRequest request = InferenceRequest.builder().prompt("hello").maxTokens(5).temperature(0.0f).build();
        InferenceResult inferResult = engine.generateFromArtifact(reloaded, request, null);
        assertNotNull(inferResult);
    }

    @Test
    public void testInt4Quantization() throws Exception {
        EvoModelQuantizer quantizer = new EvoModelQuantizer();
        QuantizationConfig config = new QuantizationConfig(QuantizationConfig.QuantizationScheme.INT4);

        EvoModelQuantizer.QuantizationResult result = quantizer.quantizeArtifact(sourceArtifact, config);

        assertNotNull(result);
        assertTrue(result.getQuantizedSize() < result.getOriginalSize());
        assertEquals(0.125, result.getCompressionRatio(), 0.05);

        EvoModelArtifact quantArtifact = result.getQuantizedArtifact();
        assertEquals("INT4", quantArtifact.getMetadata().get("quantization_scheme"));

        Path savePath = tempFolder.newFile("model-int4.evo").toPath();
        quantArtifact.save(savePath);

        EvoModelArtifact reloaded = EvoModelArtifact.load(savePath);
        assertNotNull(reloaded);
    }
}
