package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.impl.DatasetBuilder;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DataCleaner;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DatasetAcquisitionEngine;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DatasetAcquisitionEngine.AcquisitionResult;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DatasetDeduplicator;
import eu.kalafatic.evolution.forge.data.impl.pipeline.TrainingSampleQualityScorer;
import eu.kalafatic.evolution.forge.data.impl.source.HuggingFaceDatasetSource;
import eu.kalafatic.evolution.forge.data.impl.source.LocalDatasetSource;

public class DatasetAcquisitionAndSizeTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File localDataDir;

    @Before
    public void setUp() throws Exception {
        localDataDir = tempFolder.newFolder("local_dataset");

        // Create 20 sample files with clean conversational text
        for (int i = 0; i < 20; i++) {
            File txtFile = new File(localDataDir, "convo_" + i + ".txt");
            StringBuilder sb = new StringBuilder();
            sb.append("User: Hello, how can I train a native EVO LLM for minimal conversation turn #").append(i).append("?\n");
            sb.append("Assistant: You can select a clean Hugging Face conversational dataset and acquire hundreds of MB of usable training content.\n");
            sb.append("User: What features does EVO Forge support?\n");
            sb.append("Assistant: EVO Forge supports size-aware acquisition, quality filtering, global deduplication, and direct .evodata serialization.\n");
            Files.writeString(txtFile.toPath(), sb.toString(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void testSmallTargetAcquisition() throws Exception {
        DatasetSourceConfig config = new DatasetSourceConfig("LOCAL", localDataDir.getAbsolutePath());
        LocalDatasetSource source = new LocalDatasetSource(config);

        DatasetAcquisitionEngine engine = new DatasetAcquisitionEngine();
        long targetBytes = 2000; // 2 KB target
        AcquisitionResult result = engine.acquireDataset(List.of(source), targetBytes, 0.05);

        assertTrue("Target usable bytes should be satisfied", result.isTargetReached());
        assertTrue("Accepted usable bytes must be >= target usable bytes", result.getGlobalStats().getAcceptedBytes() >= targetBytes);
        assertFalse("Accepted samples list must not be empty", result.getAcceptedSamples().isEmpty());
    }

    @Test
    public void testLargeTargetAcquisition() throws Exception {
        DatasetSourceConfig config = new DatasetSourceConfig("LOCAL", localDataDir.getAbsolutePath());
        LocalDatasetSource source = new LocalDatasetSource(config);

        DatasetAcquisitionEngine engine = new DatasetAcquisitionEngine();
        long targetBytes = 10000; // 10 KB target
        AcquisitionResult result = engine.acquireDataset(List.of(source), targetBytes, 0.02);

        assertTrue("Large target should be reached", result.isTargetReached());
        assertTrue("Accepted bytes >= target bytes", result.getGlobalStats().getAcceptedBytes() >= targetBytes);
    }

    @Test
    public void testFilteringDoesNotStopOnRawBytes() throws Exception {
        File noisyDir = tempFolder.newFolder("noisy_dataset");
        // Create files where 60% of lines are invalid/junk HTML or extremely short
        for (int i = 0; i < 15; i++) {
            File f = new File(noisyDir, "noisy_" + i + ".txt");
            StringBuilder sb = new StringBuilder();
            sb.append("<html><div>HTML garbage spam ").append(i).append("</div></html>\n");
            sb.append("a\n"); // too short
            sb.append("User: Valid conversational turn #").append(i).append(" with substantial English text.\n");
            sb.append("Assistant: Detailed response providing useful conversation for EVO training.\n");
            Files.writeString(f.toPath(), sb.toString(), StandardCharsets.UTF_8);
        }

        DatasetSourceConfig config = new DatasetSourceConfig("LOCAL", noisyDir.getAbsolutePath());
        LocalDatasetSource source = new LocalDatasetSource(config);

        DatasetAcquisitionEngine engine = new DatasetAcquisitionEngine();
        long targetUsableBytes = 2000;
        AcquisitionResult result = engine.acquireDataset(List.of(source), targetUsableBytes, 0.05);

        assertTrue("Target usable bytes should be met despite noise", result.getGlobalStats().getAcceptedBytes() >= targetUsableBytes);
        assertTrue("Rejected bytes should be recorded", result.getGlobalStats().getRejectedBytes() > 0);
    }

    @Test
    public void testDeduplicationMeasuredAfterFilter() throws Exception {
        File dupDir = tempFolder.newFolder("dup_dataset");
        String identicalContent = "User: Identical question for deduplication test.\nAssistant: Identical answer repeated across multiple files.";

        for (int i = 0; i < 10; i++) {
            File f = new File(dupDir, "file_" + i + ".txt");
            Files.writeString(f.toPath(), identicalContent, StandardCharsets.UTF_8);
        }

        DatasetSourceConfig config = new DatasetSourceConfig("LOCAL", dupDir.getAbsolutePath());
        LocalDatasetSource source = new LocalDatasetSource(config);

        DatasetAcquisitionEngine engine = new DatasetAcquisitionEngine();
        AcquisitionResult result = engine.acquireDataset(List.of(source), 50000, 0.05);

        assertEquals("Identical content should be deduplicated to 1 sample", 1, result.getAcceptedSamples().size());
        assertTrue("Duplicate bytes must be recorded", result.getGlobalStats().getDuplicateBytes() > 0);
        assertTrue("Source should report exhausted when target cannot be met with duplicates", result.isSourceExhausted());
    }

    @Test
    public void testTrainValidationSeparateAccounting() throws Exception {
        DatasetSourceConfig config = new DatasetSourceConfig("LOCAL", localDataDir.getAbsolutePath());
        LocalDatasetSource source = new LocalDatasetSource(config);

        DatasetAcquisitionEngine engine = new DatasetAcquisitionEngine();
        long targetUsable = 4000;
        double valRatio = 0.10; // 10% validation
        AcquisitionResult result = engine.acquireDataset(List.of(source), targetUsable, valRatio);

        DatasetSourceStats stats = result.getGlobalStats();
        long totalUsable = stats.getAcceptedBytes();
        long trainBytes = stats.getTrainingBytes();
        long valBytes = stats.getValidationBytes();

        assertEquals("Train bytes + val bytes must equal total accepted bytes", totalUsable, trainBytes + valBytes);
        assertTrue("Validation bytes must be non-zero when valRatio > 0", valBytes > 0);
        assertTrue("Train bytes must be greater than val bytes", trainBytes > valBytes);
    }

    @Test
    public void testDatasetExhaustionReporting() throws Exception {
        DatasetSourceConfig config = new DatasetSourceConfig("LOCAL", localDataDir.getAbsolutePath());
        LocalDatasetSource source = new LocalDatasetSource(config);

        DatasetAcquisitionEngine engine = new DatasetAcquisitionEngine();
        long hugeTarget = 10_000_000L; // 10 MB (more than local directory has)
        AcquisitionResult result = engine.acquireDataset(List.of(source), hugeTarget, 0.02);

        assertTrue("Source should be reported as exhausted", result.isSourceExhausted());
        assertFalse("Target should not be reached when source is exhausted", result.isTargetReached());
        assertTrue("Coverage percent should be < 100%", result.getCoveragePercent() < 100.0);
        assertTrue("Coverage percent should be > 0%", result.getCoveragePercent() > 0.0);
    }

    @Test
    public void testConversationalSchemaNormalization() throws Exception {
        // Test UltraChat / ShareGPT JSON messages format
        JSONObject rowJson = new JSONObject();
        JSONArray msgs = new JSONArray();

        JSONObject msg1 = new JSONObject();
        msg1.put("role", "user");
        msg1.put("content", "What is the capital of France?");
        msgs.put(msg1);

        JSONObject msg2 = new JSONObject();
        msg2.put("role", "assistant");
        msg2.put("content", "The capital of France is Paris.");
        msgs.put(msg2);

        rowJson.put("messages", msgs);

        DatasetSourceConfig hfConfig = new DatasetSourceConfig("HUGGING_FACE", "HuggingFaceH4/ultrachat_200k");
        HuggingFaceDatasetSource hfSource = new HuggingFaceDatasetSource(hfConfig);

        // Reflection call to test extractSampleFromRow
        java.lang.reflect.Method extractMethod = HuggingFaceDatasetSource.class.getDeclaredMethod("extractSampleFromRow", JSONObject.class);
        extractMethod.setAccessible(true);
        NormalizedSample sample = (NormalizedSample) extractMethod.invoke(hfSource, rowJson);

        assertNotNull("Sample should be extracted from messages array", sample);
        assertEquals(TrainingSampleType.CHAT, sample.getType());
        assertTrue("Text must contain user turn", sample.toFullText().contains("user: What is the capital of France?"));
        assertTrue("Text must contain assistant turn", sample.toFullText().contains("assistant: The capital of France is Paris."));
    }

    @Test
    public void testMultiSourceGlobalDeduplication() throws Exception {
        File dirA = tempFolder.newFolder("dirA");
        File dirB = tempFolder.newFolder("dirB");

        String sharedText = "User: Shared dialogue sentence across both sources.\nAssistant: Shared response for cross-source global deduplication testing.";
        Files.writeString(new File(dirA, "a.txt").toPath(), sharedText, StandardCharsets.UTF_8);
        Files.writeString(new File(dirB, "b.txt").toPath(), sharedText, StandardCharsets.UTF_8);

        LocalDatasetSource srcA = new LocalDatasetSource(new DatasetSourceConfig("LOCAL", dirA.getAbsolutePath()));
        LocalDatasetSource srcB = new LocalDatasetSource(new DatasetSourceConfig("LOCAL", dirB.getAbsolutePath()));

        DatasetAcquisitionEngine engine = new DatasetAcquisitionEngine();
        AcquisitionResult result = engine.acquireDataset(List.of(srcA, srcB), 50000, 0.05);

        assertEquals("Global deduplication should reduce identical samples across sources to 1", 1, result.getAcceptedSamples().size());
    }

    @Test
    public void testEvoDatasetArtifactStatisticsPersistence() throws Exception {
        File artifactFile = new File(tempFolder.getRoot(), "persistent_stats.evodata");
        EvoDatasetArtifact artifact = new EvoDatasetArtifact(artifactFile);

        List<NormalizedSample> samples = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            samples.add(NormalizedSample.createTextSample("Real training content sample #" + i + " for persistent statistics test.", "src"));
        }

        DatasetSourceConfig config = new DatasetSourceConfig("LOCAL", localDataDir.getAbsolutePath());
        DatasetSourceStats stats = new DatasetSourceStats();
        stats.setRequestedUsableBytes(100000);
        stats.setAcceptedBytes(50000);
        stats.setDownloadedBytes(70000);
        stats.setRawContentBytes(70000);
        stats.setRejectedBytes(15000);
        stats.setDuplicateBytes(5000);
        stats.setAcceptedSamples(15);

        artifact.save(samples, config, stats, 0.10);

        EvoDatasetArtifact loaded = EvoDatasetArtifact.load(artifactFile);
        assertEquals(EvoDatasetArtifact.Status.READY, loaded.getStatus());
        assertTrue("Report text should contain actual usable bytes", loaded.buildReportText().contains("Actual Usable Bytes"));
        assertTrue("Report text should contain estimated tokens", loaded.buildReportText().contains("Estimated Tokens"));
    }

    @Test
    public void testEndToEndPreparationPipeline() throws Exception {
        DatasetSourceConfig config = new DatasetSourceConfig("LOCAL", localDataDir.getAbsolutePath());
        LocalDatasetSource source = new LocalDatasetSource(config);

        DatasetAcquisitionEngine engine = new DatasetAcquisitionEngine();
        AcquisitionResult acquisition = engine.acquireDataset(List.of(source), 5000, 0.05);

        File evodataFile = new File(tempFolder.getRoot(), "pipeline_test.evodata");
        EvoDatasetArtifact artifact = new EvoDatasetArtifact(evodataFile);
        artifact.save(acquisition.getAcceptedSamples(), config, acquisition.getGlobalStats(), 0.05);

        assertTrue("Evodata file must exist", evodataFile.exists());
        EvoDatasetArtifact loaded = EvoDatasetArtifact.load(evodataFile);

        List<Integer> mockTokens = new ArrayList<>();
        for (NormalizedSample s : loaded.getTrainSamples()) {
            for (char c : s.toFullText().toCharArray()) {
                mockTokens.add((int) c);
            }
        }

        DatasetBuilder builder = new DatasetBuilder();
        List<DatasetBuilder.Sample> trainingSamples = builder.buildSlidingWindow(mockTokens, 16, 8);
        assertTrue("Training samples should be generated for Forge trainer from .evodata", trainingSamples.size() > 0);
    }
}
