package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.impl.DatasetBuilder;
import eu.kalafatic.evolution.forge.data.impl.source.HuggingFaceDatasetSource;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;

import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * End-to-End Pipeline test verifying HF Alpaca -> NormalizedSample -> .evodata -> DatasetBuilder -> TrainingSample -> EvoLlmTrainer.
 */
public class AlpacaEvodataTrainingPipelineTest {

    @Test
    public void testHuggingFaceAlpacaExtractionAndRejection() throws Exception {
        HuggingFaceDatasetSource source = new HuggingFaceDatasetSource("tatsu-lab/alpaca");
        Method extractMethod = HuggingFaceDatasetSource.class.getDeclaredMethod("extractSampleFromRow", JSONObject.class);
        extractMethod.setAccessible(true);

        // 1. Valid Alpaca row
        JSONObject validRow = new JSONObject();
        validRow.put("instruction", "Explain gravity.");
        validRow.put("input", "In simple terms.");
        validRow.put("output", "Gravity is a force that pulls objects toward each other.");

        NormalizedSample sample = (NormalizedSample) extractMethod.invoke(source, validRow);
        assertNotNull("Valid Alpaca row should be extracted", sample);
        assertEquals(TrainingSampleType.INSTRUCTION, sample.getType());
        assertTrue("Instruction should contain combined input context", sample.getInstruction().contains("Explain gravity."));
        assertTrue("Instruction should contain context", sample.getInstruction().contains("Context:\nIn simple terms."));
        assertEquals("Gravity is a force that pulls objects toward each other.", sample.getResponse());

        // 2. Partial Alpaca row - missing output
        JSONObject partialRow1 = new JSONObject();
        partialRow1.put("instruction", "Explain gravity.");
        partialRow1.put("input", "");

        NormalizedSample rejected1 = (NormalizedSample) extractMethod.invoke(source, partialRow1);
        assertNull("Partial Alpaca row missing output must be rejected", rejected1);

        // 3. Partial Alpaca row - missing instruction and input
        JSONObject partialRow2 = new JSONObject();
        partialRow2.put("output", "Gravity is a force.");

        NormalizedSample rejected2 = (NormalizedSample) extractMethod.invoke(source, partialRow2);
        assertNull("Partial Alpaca row missing instruction must be rejected", rejected2);

        // 4. Unknown schema with metadata key
        JSONObject unknownSchemaRow = new JSONObject();
        unknownSchemaRow.put("dataset_version", "v1.0");
        unknownSchemaRow.put("license", "MIT");

        NormalizedSample rejectedUnknown = (NormalizedSample) extractMethod.invoke(source, unknownSchemaRow);
        assertNull("Unknown schema metadata fields must NOT be turned into TEXT samples", rejectedUnknown);
    }

    @Test
    public void testNormalizedSampleAndEvodataSerialization() throws Exception {
        NormalizedSample sample = NormalizedSample.createInstructionSample(
                "Write a poem about space.",
                "Stars shine bright,\nin the dark night.",
                "test-source"
        );

        // Verify toFullText format
        String fullText = sample.toFullText();
        assertTrue(fullText.contains("### Instruction:\nWrite a poem about space."));
        assertTrue(fullText.contains("### Response:\nStars shine bright,"));

        // Verify count and hash recalculation on setters
        int initialCharCount = sample.getCharCount();
        assertNotNull(sample.getHash());

        sample.setResponse("New response line.");
        assertNotEquals(initialCharCount, sample.getCharCount());
        assertNotNull(sample.getHash());

        // Save to .evodata
        File tempEvodata = File.createTempFile("alpaca_test_", ".evodata");
        tempEvodata.deleteOnExit();

        List<NormalizedSample> samples = new ArrayList<>();
        samples.add(sample);

        DatasetSourceConfig sourceConfig = new DatasetSourceConfig("HUGGING_FACE", "tatsu-lab/alpaca");
        DatasetSourceStats stats = new DatasetSourceStats();

        EvoDatasetArtifact artifact = new EvoDatasetArtifact(tempEvodata);
        artifact.save(samples, sourceConfig, stats, 0.0);

        // Reload .evodata
        EvoDatasetArtifact reloadedArtifact = EvoDatasetArtifact.load(tempEvodata);
        assertEquals(1, reloadedArtifact.getTrainSamples().size());

        NormalizedSample reloadedSample = reloadedArtifact.getTrainSamples().get(0);
        assertEquals(TrainingSampleType.INSTRUCTION, reloadedSample.getType());
        assertEquals("Write a poem about space.", reloadedSample.getInstruction());
        assertEquals("New response line.", reloadedSample.getResponse());
        assertNull("Text field should remain null on reloaded instruction sample to prevent text shadowing", reloadedSample.getText());

        assertTrue(reloadedSample.toFullText().contains("### Instruction:"));
        assertTrue(reloadedSample.toFullText().contains("### Response:"));
    }

    @Test
    public void testDatasetBuilderAndTrainerPromptMasking() throws Exception {
        NormalizedSample sample = NormalizedSample.createInstructionSample(
                "What is 2 + 2?",
                "4",
                "math-source"
        );

        SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
        tokenizer.train(sample.toFullText() + "\nAddition and numbers.", 100);

        DatasetBuilder builder = new DatasetBuilder();
        List<NormalizedSample> normSamples = List.of(sample);

        List<TrainingSample> trainingSamples = builder.buildTrainingSamples(normSamples, tokenizer, 128);
        assertEquals(1, trainingSamples.size());

        TrainingSample tSample = trainingSamples.get(0);
        assertNotNull(tSample.inputIds);
        assertNotNull(tSample.labels);
        assertNotNull(tSample.lossMask);

        // Verify prompt loss masking: initial tokens (prompt) should have lossMask = false
        String promptStr = "### Instruction:\nWhat is 2 + 2?\n\n### Response:\n";
        List<Integer> promptTokens = tokenizer.encode(promptStr);
        int promptLen = promptTokens.size();

        assertTrue("Prompt length should be > 0", promptLen > 0);
        assertTrue("Sequence length should cover prompt and response", tSample.inputIds.length > promptLen);

        for (int i = 0; i < promptLen; i++) {
            assertFalse("Prompt token at index " + i + " must have lossMask = false", tSample.lossMask[i]);
        }

        // Response tokens should have lossMask = true
        for (int i = promptLen; i < tSample.inputIds.length; i++) {
            assertTrue("Response token at index " + i + " must have lossMask = true", tSample.lossMask[i]);
        }

        // Execute short trainer run
        EvoLlmModel model = new EvoLlmModel(tokenizer.getVocabSize(), 32, 2, 2, 64, 128);
        EvoLlmTrainer trainer = new EvoLlmTrainer(model);
        trainer.train(trainingSamples, 2);

        assertFalse("Loss history should record training loss", trainer.getLossHistory().isEmpty());
    }
}
