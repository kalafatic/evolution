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

import eu.kalafatic.evolution.controller.orchestration.selfdev.LLMDarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.LLMDarwinEngine.LlmConfig;
import eu.kalafatic.evolution.controller.orchestration.selfdev.LLMDarwinEngine.CandidateResult;

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
    public void testSafePreviewAndNormalization() {
        // LLMDarwinEngine engine needs to be instantiated or we can use reflection if the method is private
        // Actually safePreview is private static, and normalizeCandidateConfig is private
        // Let's call them using reflection to verify exact correctness!
        try {
            java.lang.reflect.Method safePreviewMethod = LLMDarwinEngine.class.getDeclaredMethod("safePreview", String.class, int.class);
            safePreviewMethod.setAccessible(true);

            // Verify requirement 7: safePreview(null, 1000) does not throw and returns empty string
            assertEquals("", safePreviewMethod.invoke(null, null, 1000));
            // Verify requirement 8: safePreview("", 1000) does not throw and returns empty string
            assertEquals("", safePreviewMethod.invoke(null, "", 1000));
            // Verify requirement 9: safePreview(value, 0) returns an empty string
            assertEquals("", safePreviewMethod.invoke(null, "some text", 0));
            assertEquals("", safePreviewMethod.invoke(null, "some text", -5));

            // Verify standard preview truncation
            assertEquals("abc", safePreviewMethod.invoke(null, "abc", 1000));
            assertEquals("abc", safePreviewMethod.invoke(null, "abcdef", 3));

            // Verify normalization method ensures embeddingSize % heads == 0
            java.lang.reflect.Method normalizeMethod = LLMDarwinEngine.class.getDeclaredMethod("normalizeCandidateConfig", LlmConfig.class);
            normalizeMethod.setAccessible(true);

            eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getOrCreateSession("Default");
            eu.kalafatic.evolution.controller.orchestration.TaskContext dummyContext =
                new eu.kalafatic.evolution.controller.orchestration.TaskContext(null, new File("."));
            LLMDarwinEngine engine = new LLMDarwinEngine(dummyContext, null, null);

            // Candidate with invalid divisibility
            LlmConfig badConfig = new LlmConfig(1000, 128, 1, 5, 32, 2);
            LlmConfig normalized = (LlmConfig) normalizeMethod.invoke(engine, badConfig);

            // Should be divisible and head dimension > 0
            assertTrue(normalized.embeddingSize % normalized.heads == 0);
            assertTrue(normalized.embeddingSize / normalized.heads > 0);
            // clamped values check
            assertTrue(normalized.vocabSize >= 4000);
            assertTrue(normalized.layers >= 2);
            assertTrue(normalized.maxSeqLen >= 64);
            assertTrue(normalized.epochs >= 1);

        } catch (Exception e) {
            fail("Exception in reflection tests: " + e.getMessage());
        }
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

    @Test
    public void testAtomicWeightsWriting() throws Exception {
        int vocabSize = 100;
        int embeddingSize = 64;
        int heads = 2;
        int layers = 1;
        int dff = 256;
        int maxSeqLen = 16;

        EvoLlmModel model = new EvoLlmModel(vocabSize, embeddingSize, heads, layers, dff, maxSeqLen);
        File tempFile = tempFolder.newFile("weights.bin");
        Path targetPath = tempFile.toPath();

        // Safe cleanup
        Files.deleteIfExists(targetPath);

        java.lang.reflect.Method writeMethod = LLMDarwinEngine.class.getDeclaredMethod("writeWeightsAtomically", Path.class, EvoLlmModel.class);
        writeMethod.setAccessible(true);

        eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getOrCreateSession("Default");
        eu.kalafatic.evolution.controller.orchestration.TaskContext dummyContext =
            new eu.kalafatic.evolution.controller.orchestration.TaskContext(null, new File("."));
        LLMDarwinEngine engine = new LLMDarwinEngine(dummyContext, null, null);

        // Invoke atomic writing
        writeMethod.invoke(engine, targetPath, model);

        // Target should exist and have real written size (>0 bytes)
        assertTrue(Files.exists(targetPath));
        assertTrue(Files.size(targetPath) > 0);

        // Temporary files should be cleaned up
        Path tempPath = targetPath.getParent().resolve(targetPath.getFileName().toString() + ".tmp");
        assertFalse(Files.exists(tempPath));
    }

    @Test
    public void testProgressListenerAndReporting() {
        int vocabSize = 50;
        int embeddingSize = 32;
        int heads = 2;
        int layers = 1;
        int dff = 128;
        int maxSeqLen = 8;

        EvoLlmModel model = new EvoLlmModel(vocabSize, embeddingSize, heads, layers, dff, maxSeqLen);
        List<DatasetBuilder.Sample> samples = new ArrayList<>();
        List<Integer> inputIds = new ArrayList<>();
        for (int i = 0; i < maxSeqLen; i++) {
            inputIds.add(i % vocabSize);
        }
        samples.add(new DatasetBuilder.Sample(inputIds, 5));

        EvoLlmTrainer trainer = new EvoLlmTrainer(model);
        final int[] progressCount = {0};
        trainer.setProgressListener((epoch, totalEpochs, sampleIndex, totalSamples, currentLoss) -> {
            progressCount[0]++;
            assertEquals(0, epoch);
            assertEquals(1, totalEpochs);
            assertEquals(1, sampleIndex);
            assertEquals(1, totalSamples);
            assertTrue(currentLoss > 0.0);
        });

        trainer.train(samples, 1);
        assertEquals(1, progressCount[0]);
    }
}
