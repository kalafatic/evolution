package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionResult;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DataCleaner;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DatasetDeduplicator;
import eu.kalafatic.evolution.forge.data.impl.pipeline.TrainingSampleQualityScorer;
import eu.kalafatic.evolution.forge.data.impl.service.TrainingDataAcquisitionServiceImpl;
import eu.kalafatic.evolution.forge.data.impl.source.LocalDatasetSource;
import eu.kalafatic.evolution.forge.data.impl.writer.EvoDataWriter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Regression test suite verifying .evodata quality verification, record normalization,
 * exact/normalized deduplication, low-quality/invalid sample filtering, train/validation leakage prevention,
 * and comprehensive artifact report generation.
 */
public class DatasetQualityAndLeakageVerificationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testDataCleanerAndQualityScorerFiltersNoisyAndInvalidSamples() {
        DataCleaner cleaner = new DataCleaner();
        TrainingSampleQualityScorer scorer = new TrainingSampleQualityScorer(0.5);

        // 1. Punctuation/noise only sample
        NormalizedSample noiseSample = NormalizedSample.createTextSample("!!! ??? ... --- ***", "test-src");
        NormalizedSample cleanedNoise = cleaner.clean(noiseSample);
        assertTrue("Cleaner or scorer must reject punctuation-only noise", cleanedNoise == null || !scorer.accept(cleanedNoise));

        // 2. Corrupted text with high replacement character density
        NormalizedSample corruptedSample = NormalizedSample.createTextSample("Valid start \uFFFD\uFFFD\uFFFD\uFFFD\uFFFD \uFFFD\uFFFD\uFFFD bad end", "test-src");
        NormalizedSample cleanedCorrupted = cleaner.clean(corruptedSample);
        assertFalse("Quality scorer must reject corrupted text with high \\uFFFD density", scorer.accept(cleanedCorrupted));

        // 3. Invalid QA pair: Prompt equals Response
        NormalizedSample promptEqualsResp = NormalizedSample.createInstructionSample("What is Java?", "What is Java?", "test-src");
        NormalizedSample cleanedPromptResp = cleaner.clean(promptEqualsResp);
        assertFalse("Quality scorer must reject instruction where response equals prompt", scorer.accept(cleanedPromptResp));

        // 4. Invalid QA pair: Trivial non-answer
        NormalizedSample trivialResp = NormalizedSample.createInstructionSample("Explain Quantum Computing", "N/A", "test-src");
        NormalizedSample cleanedTrivial = cleaner.clean(trivialResp);
        assertFalse("Quality scorer must reject trivial non-answer response", scorer.accept(cleanedTrivial));

        // 5. Valid high-quality instruction sample
        NormalizedSample validSample = NormalizedSample.createInstructionSample("What is Java?", "Java is a high-level object-oriented programming language.", "test-src");
        NormalizedSample cleanedValid = cleaner.clean(validSample);
        assertNotNull(cleanedValid);
        assertTrue("Quality scorer must accept valid instruction-response sample", scorer.accept(cleanedValid));
    }

    @Test
    public void testDataCleanerPreservesJavaCodeSyntaxAndAnnotations() {
        DataCleaner cleaner = new DataCleaner();
        TrainingSampleQualityScorer scorer = new TrainingSampleQualityScorer(0.5);

        String javaCode = "package eu.kalafatic.demo;\n\n" +
                          "import java.util.List;\n" +
                          "import java.util.ArrayList;\n\n" +
                          "public class PipelineDemo<T> {\n" +
                          "    @Override\n" +
                          "    public List<T> process(List<T> items) {\n" +
                          "        if (items == null || items.isEmpty()) {\n" +
                          "            return new ArrayList<>();\n" +
                          "        }\n" +
                          "        return items;\n" +
                          "    }\n" +
                          "}\n";

        NormalizedSample codeSample = NormalizedSample.createTextSample(javaCode, "PipelineDemo.java");
        codeSample.setCategory("source_code");
        codeSample.setLanguage("java");

        NormalizedSample cleanedCode = cleaner.clean(codeSample);
        assertNotNull("Cleaner must accept valid Java code sample", cleanedCode);

        String cleanedText = cleanedCode.toFullText();
        assertTrue("Cleaner must preserve generics <T>", cleanedText.contains("List<T>"));
        assertTrue("Cleaner must preserve annotations @Override", cleanedText.contains("@Override"));
        assertTrue("Cleaner must preserve logical operators || and &&", cleanedText.contains("||"));
        assertTrue("Cleaner must preserve braces { }", cleanedText.contains("{") && cleanedText.contains("}"));
        assertTrue("Cleaner must preserve import statements", cleanedText.contains("import java.util.List;"));

        assertTrue("Quality scorer must accept valid Java code sample", scorer.accept(cleanedCode));
    }

    @Test
    public void testDecimalMbParsingAndTargetSizePreservation() throws Exception {
        String inputMbStr = "12.5";
        double parsedMb = Double.parseDouble(inputMbStr);
        long targetBytes = (long) (parsedMb * 1024.0 * 1024.0);

        assertEquals(12.5, parsedMb, 0.001);
        assertEquals(13107200L, targetBytes);

        DatasetSourceStats stats = new DatasetSourceStats();
        stats.setRequestedUsableBytes(targetBytes);
        stats.setAcceptedBytes(5242880L); // Actual generated usable size: 5 MB

        // Verify target size is strictly preserved in stats and report output
        assertEquals(13107200L, stats.getRequestedUsableBytes());

        EvoDatasetArtifact artifact = new EvoDatasetArtifact(new File("dummy.evodata"));
        DatasetSourceConfig config = new DatasetSourceConfig("LOCAL", "test");
        artifact.save(List.of(NormalizedSample.createTextSample("Test sample", "src")), config, stats, 0.1);

        String report = artifact.buildReportText();
        assertTrue("Report must display requested usable data size", report.contains("Requested Usable Data: 12 MB") || report.contains("13107200 bytes"));
        assertTrue("Report must display actual usable bytes separately", report.contains("Actual Usable Bytes: 5242880"));
    }

    @Test
    public void testDatasetDeduplicatorDetectsExactAndNormalizedDuplicates() {
        DatasetDeduplicator deduplicator = new DatasetDeduplicator(true);

        NormalizedSample sample1 = NormalizedSample.createTextSample("This is a unique sentence for deduplication testing.", "src1");
        NormalizedSample sample2Exact = NormalizedSample.createTextSample("This is a unique sentence for deduplication testing.", "src2");
        NormalizedSample sample3Normalized = NormalizedSample.createTextSample("THIS IS A UNIQUE SENTENCE FOR DEDUPLICATION TESTING!!!", "src3");

        assertFalse("First sample should not be duplicate", deduplicator.isDuplicate(sample1));
        deduplicator.register(sample1);

        assertTrue("Exact match must be flagged as duplicate", deduplicator.isDuplicate(sample2Exact));
        deduplicator.register(sample2Exact);

        assertTrue("Normalized match (case & punctuation difference) must be flagged as duplicate", deduplicator.isDuplicate(sample3Normalized));
        deduplicator.register(sample3Normalized);

        assertEquals("Exact duplicate count must be 1", 1, deduplicator.getExactDuplicateCount());
        assertEquals("Normalized duplicate count must be 1", 1, deduplicator.getNormalizedDuplicateCount());
        assertEquals("Total duplicate count must be 2", 2, deduplicator.getTotalDuplicateCount());
    }

    @Test
    public void testTrainValidationLeakagePreventionInEvoDatasetArtifact() throws Exception {
        File targetEvodata = tempFolder.newFile("leakage_test.evodata");

        List<NormalizedSample> samples = new ArrayList<>();
        // Create 10 samples
        for (int i = 0; i < 10; i++) {
            samples.add(NormalizedSample.createTextSample("Unique training statement number " + i + " for leakage test.", "src"));
        }
        // Force sample 9 (which would be in validation split with 10% split) to be an exact duplicate of sample 0
        samples.set(9, NormalizedSample.createTextSample("Unique training statement number 0 for leakage test.", "src"));

        EvoDataWriter writer = new EvoDataWriter();
        DatasetSourceConfig config = new DatasetSourceConfig("LOCAL", targetEvodata.getAbsolutePath());
        DatasetSourceStats stats = new DatasetSourceStats();

        EvoDatasetArtifact artifact = writer.write(targetEvodata, samples, config, stats, 0.1);

        assertNotNull(artifact);
        assertEquals(EvoDatasetArtifact.Status.READY, artifact.getStatus());

        // Validate that train and validation samples do not contain identical items
        assertEquals(9, artifact.getTrainSamples().size());
        assertEquals("Leaking validation sample must be excluded", 0, artifact.getValSamples().size());
        assertEquals("Train/val leakage count must be recorded as 1", 1, stats.getTrainValLeakageCount());

        EvoDatasetArtifact loaded = EvoDatasetArtifact.load(targetEvodata);
        assertEquals(9, loaded.getTrainSamples().size());
        assertEquals(0, loaded.getValSamples().size());

        String report = loaded.buildReportText();
        assertTrue("Report must mention Train/Val Leakage Filtered", report.contains("Train/Val Leakage Filtered: 1"));
    }

    @Test
    public void testFullDatasetAcquisitionPipelineQualityAndReportVerification() throws Exception {
        File dataFile = tempFolder.newFile("input_dataset.jsonl");
        File outputEvodata = new File(tempFolder.getRoot(), "output_result.evodata");

        List<String> lines = List.of(
            "{\"type\":\"TEXT\",\"text\":\"Valid high quality training sample content block 1.\"}",
            "{\"type\":\"TEXT\",\"text\":\"Valid high quality training sample content block 1.\"}",
            "{\"type\":\"TEXT\",\"text\":\"VALID HIGH QUALITY TRAINING SAMPLE CONTENT BLOCK 1!\"}",
            "{\"type\":\"TEXT\",\"text\":\"!!! ??? ... ---\"}",
            "{\"type\":\"INSTRUCTION\",\"instruction\":\"How to bake a cake?\",\"response\":\"How to bake a cake?\"}",
            "{\"type\":\"TEXT\",\"text\":\"Corrupted text sample \\uFFFD\\uFFFD\\uFFFD\\uFFFD\\uFFFD \\uFFFD\\uFFFD\\uFFFD garbage data.\"}",
            "{\"type\":\"TEXT\",\"text\":\"Valid high quality training sample content block 2.\"}",
            "{\"type\":\"TEXT\",\"text\":\"Valid high quality training sample content block 3.\"}"
        );
        Files.write(dataFile.toPath(), lines, StandardCharsets.UTF_8);

        DatasetSourceConfig srcConfig = new DatasetSourceConfig("LOCAL", dataFile.getAbsolutePath());
        LocalDatasetSource source = new LocalDatasetSource(srcConfig);

        TrainingDataAcquisitionServiceImpl acquisitionService = new TrainingDataAcquisitionServiceImpl();
        TrainingDataAcquisitionRequest request = new TrainingDataAcquisitionRequest(List.of(source), 100, 0.1);

        TrainingDataAcquisitionResult result = acquisitionService.acquireDataset(request);
        assertNotNull(result);

        // Verify acquisition stats
        DatasetSourceStats stats = result.getGlobalStats();
        assertTrue("Accepted samples must be > 0", result.getAcceptedSamples().size() > 0);
        assertTrue("Duplicate records must be tracked", stats.getDuplicateRecords() > 0);
        assertTrue("Rejected records must be tracked", stats.getRejectedRecords() > 0);

        // Serialize to .evodata artifact
        EvoDataWriter writer = new EvoDataWriter();
        EvoDatasetArtifact artifact = writer.write(outputEvodata, result.getAcceptedSamples(), srcConfig, stats, 0.1);
        assertNotNull(artifact);
        assertTrue(outputEvodata.exists());

        // Reload artifact and verify quality report output
        EvoDatasetArtifact loadedArtifact = EvoDatasetArtifact.load(outputEvodata);
        String report = loadedArtifact.buildReportText();

        assertTrue("Report must include Accepted Records", report.contains("Accepted Records:"));
        assertTrue("Report must include Rejected Records", report.contains("Rejected Records:"));
        assertTrue("Report must include Duplicate Records", report.contains("Duplicate Records:"));
        assertTrue("Report must include Quality Limitations Summary", report.contains("QUALITY LIMITATIONS & VERIFICATION SUMMARY:"));
        assertTrue("Report must specify Exact & Normalized Deduplication", report.contains("Exact & Normalized Deduplication: APPLIED"));
        assertTrue("Report must specify Train/Val Leakage Prevention", report.contains("Train/Val Leakage Prevention: APPLIED"));
    }
}
