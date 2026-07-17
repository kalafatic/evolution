package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinLlmInstance;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinLlmInstance.LlmConfig;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinLlmInstance.CandidateResult;

import eu.kalafatic.evolution.forge.data.impl.DatasetBuilder;
import eu.kalafatic.evolution.forge.data.impl.MarkdownCleaner;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;

public class DarwinLlmInstanceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testLlmConfigCreation() {
        LlmConfig config = new LlmConfig(2000, 128, 2, 4);
        assertEquals(2000, config.vocabSize);
        assertEquals(128, config.embeddingSize);
        assertEquals(2, config.layers);
        assertEquals(4, config.heads);
        assertNotNull(config.toString());
    }

    @Test
    public void testCandidateResultSorting() {
        List<CandidateResult> results = new ArrayList<>();
        results.add(new CandidateResult("Candidate A", new LlmConfig(2000, 64, 2, 2), 2.5, 1000, 500, 2.55));
        results.add(new CandidateResult("Candidate B", new LlmConfig(4000, 128, 2, 4), 1.8, 2000, 800, 1.90));
        results.add(new CandidateResult("Candidate C", new LlmConfig(4000, 128, 4, 4), 2.1, 4000, 1200, 2.30));

        // Sort ascending (lowest fitness score is best)
        results.sort((c1, c2) -> Double.compare(c1.fitness, c2.fitness));

        assertEquals("Candidate B", results.get(0).name);
        assertEquals("Candidate C", results.get(1).name);
        assertEquals("Candidate A", results.get(2).name);
    }

    @Test
    public void testDatasetAndTokenizerBuilding() throws Exception {
        String sampleText = "Evolution genome data management is personal, economical, and political.\n" +
                            "personal: the joy of frontier creation and personal relevance.\n" +
                            "economical: building priceless user and developer know-how.\n" +
                            "political: independence and local control from centralized AI authorities.\n";

        MarkdownCleaner cleaner = new MarkdownCleaner();
        String cleanText = cleaner.clean(sampleText);
        assertNotNull(cleanText);

        SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
        tokenizer.train(cleanText, 1000);
        assertTrue(tokenizer.getVocabSize() > 0);

        List<Integer> tokens = tokenizer.encode(cleanText);
        assertTrue(tokens.size() > 0);

        DatasetBuilder builder = new DatasetBuilder();
        List<DatasetBuilder.Sample> samples = builder.buildSlidingWindow(tokens, 4, 2);
        assertNotNull(samples);
        for (DatasetBuilder.Sample sample : samples) {
            assertEquals(4, sample.input.size());
            assertNotNull(sample.target);
        }
    }

    @Test
    public void testEvoLlmTrainingCycle() {
        int vocabSize = 500;
        int embeddingSize = 64;
        int heads = 2;
        int layers = 1;
        int dff = 256;
        int maxSeqLen = 16;

        EvoLlmModel model = new EvoLlmModel(vocabSize, embeddingSize, heads, layers, dff, maxSeqLen);
        assertNotNull(model);
        assertEquals(vocabSize, model.getVocabSize());
        assertEquals(embeddingSize, model.getDModel());
        assertEquals(heads, model.getNumHeads());
        assertEquals(layers, model.getNumBlocks());

        List<DatasetBuilder.Sample> samples = new ArrayList<>();
        List<Integer> inputIds = new ArrayList<>();
        for (int i = 0; i < maxSeqLen; i++) {
            inputIds.add(i % vocabSize);
        }
        samples.add(new DatasetBuilder.Sample(inputIds, 42));

        EvoLlmTrainer trainer = new EvoLlmTrainer(model);
        trainer.train(samples, 1);

        assertFalse(trainer.getLossHistory().isEmpty());
        double loss = trainer.getLossHistory().get(0);
        assertTrue(loss > 0.0);
    }
}
