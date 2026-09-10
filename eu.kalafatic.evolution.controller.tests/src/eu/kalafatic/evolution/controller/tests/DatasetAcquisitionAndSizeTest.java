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

    @Test
    public void testTinyFirstShardContinuesToNextShards() throws Exception {
        // Shard 1 has ~200 bytes, Shard 2 has ~1000 bytes, Shard 3 has ~1500 bytes. Target = 2000 bytes.
        List<NormalizedSample> shard1 = createSampleBatch(2, "Shard 1 small sample content turn ");
        List<NormalizedSample> shard2 = createSampleBatch(8, "Shard 2 substantial conversational turn ");
        List<NormalizedSample> shard3 = createSampleBatch(12, "Shard 3 additional content turn ");

        ShardedTestSource source = new ShardedTestSource(List.of(shard1, shard2, shard3));
        DatasetAcquisitionEngine engine = new DatasetAcquisitionEngine();

        long requestedMinimum = 2000;
        AcquisitionResult result = engine.acquireDataset(List.of(source), requestedMinimum, 0.02);

        assertTrue("All required shards must be processed until usable >= target", result.isTargetReached());
        assertTrue("Usable bytes must meet requested minimum", result.getUsableContentBytes() >= requestedMinimum);
        assertEquals(AcquisitionResult.Status.READY, result.getStatus());
    }

    @Test
    public void testDownloadSizeMisleadingDoesNotStopAcquisitionEarly() throws Exception {
        // Source where 70% of lines are noisy HTML (rejected), so 1000 raw bytes yield only 300 usable bytes.
        List<NormalizedSample> noisyBatch = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            noisyBatch.add(NormalizedSample.createTextSample("<html><p>HTML noise line " + i + "</p></html>", "noisy"));
            noisyBatch.add(NormalizedSample.createTextSample("a", "noisy"));
            noisyBatch.add(NormalizedSample.createTextSample("User: Valid conversational turn #" + i + " with good text.\nAssistant: Clear detailed answer for EVO training.", "noisy"));
        }

        ShardedTestSource source = new ShardedTestSource(List.of(noisyBatch));
        DatasetAcquisitionEngine engine = new DatasetAcquisitionEngine();

        long targetUsable = 1500;
        AcquisitionResult result = engine.acquireDataset(List.of(source), targetUsable, 0.05);

        assertTrue("Downloaded bytes should be substantially higher than usable bytes",
                result.getGlobalStats().getDownloadedBytes() > result.getUsableContentBytes());
        assertTrue("Target usable bytes must be satisfied despite misleading download size",
                result.getUsableContentBytes() >= targetUsable);
    }

    @Test
    public void testCompressedSourceExtractionYieldsMinimumSatisfied() throws Exception {
        // High density samples
        List<NormalizedSample> denseBatch = createSampleBatch(15, "Dense compressed extracted text record for size accounting ");
        ShardedTestSource source = new ShardedTestSource(List.of(denseBatch));

        DatasetAcquisitionEngine engine = new DatasetAcquisitionEngine();
        long targetUsable = 1000;
        AcquisitionResult result = engine.acquireDataset(List.of(source), targetUsable, 0.05);

        assertTrue("Minimum usable requirement satisfied", result.getUsableContentBytes() >= targetUsable);
        assertEquals(AcquisitionResult.Status.READY, result.getStatus());
    }

    @Test
    public void testSourceExhaustionReturnsInsufficientSourceData() throws Exception {
        List<NormalizedSample> smallBatch = createSampleBatch(3, "Limited sample turn ");
        ShardedTestSource source = new ShardedTestSource(List.of(smallBatch));

        DatasetAcquisitionEngine engine = new DatasetAcquisitionEngine();
        long targetUsable = 50000; // 50 KB target (source only has ~300 bytes)
        AcquisitionResult result = engine.acquireDataset(List.of(source), targetUsable, 0.02);

        assertTrue("Source should report exhausted", result.isSourceExhausted());
        assertFalse("Target should not be reached", result.isTargetReached());
        assertEquals("Status must be INSUFFICIENT_SOURCE_DATA", AcquisitionResult.Status.INSUFFICIENT_SOURCE_DATA, result.getStatus());
        assertTrue("Shortfall bytes must be recorded", result.getShortfallBytes() > 0);
        assertTrue("Coverage percent must be < 100%", result.getCoveragePercent() < 100.0);

        File artifactFile = new File(tempFolder.getRoot(), "insufficient.evodata");
        EvoDatasetArtifact artifact = new EvoDatasetArtifact(artifactFile);
        artifact.save(result.getAcceptedSamples(), source.getConfig(), result.getGlobalStats(), 0.02);

        EvoDatasetArtifact loaded = EvoDatasetArtifact.load(artifactFile);
        assertEquals("Loaded artifact status must preserve INSUFFICIENT_SOURCE_DATA", EvoDatasetArtifact.Status.INSUFFICIENT_SOURCE_DATA, loaded.getStatus());
    }

    @Test
    public void testDeduplicationAccountingContinuesAcquisition() throws Exception {
        List<NormalizedSample> dups = new ArrayList<>();
        String sampleText = "User: Duplicate query sentence.\nAssistant: Duplicate response sentence for testing.";
        for (int i = 0; i < 15; i++) {
            dups.add(NormalizedSample.createTextSample(sampleText, "dup_src"));
        }
        // Add distinct unique samples at the end
        dups.addAll(createSampleBatch(10, "Unique non-duplicate sample record turn "));

        ShardedTestSource source = new ShardedTestSource(List.of(dups));
        DatasetAcquisitionEngine engine = new DatasetAcquisitionEngine();

        AcquisitionResult result = engine.acquireDataset(List.of(source), 1000, 0.05);

        assertTrue("Duplicate bytes must be recorded", result.getGlobalStats().getDuplicateBytes() > 0);
        assertTrue("Usable bytes must meet or exceed target after deduplication", result.getUsableContentBytes() >= 1000);
    }

    @Test
    public void testExactThresholdSatisfiesRequirement() throws Exception {
        String exactText = "User: Standard turn for exact threshold test.\nAssistant: Detailed answer ensuring exact byte calculation.";
        byte[] bytes = exactText.getBytes(StandardCharsets.UTF_8);
        long exactLen = bytes.length;

        List<NormalizedSample> exactBatch = new ArrayList<>();
        exactBatch.add(NormalizedSample.createTextSample(exactText, "exact"));

        ShardedTestSource source = new ShardedTestSource(List.of(exactBatch));
        DatasetAcquisitionEngine engine = new DatasetAcquisitionEngine();

        AcquisitionResult result = engine.acquireDataset(List.of(source), exactLen, 0.0);

        assertEquals("Usable bytes must equal exact threshold", exactLen, result.getUsableContentBytes());
        assertTrue("Target reached", result.isTargetReached());
        assertEquals(AcquisitionResult.Status.READY, result.getStatus());
    }

    @Test
    public void testOvershootAcceptedCleanly() throws Exception {
        List<NormalizedSample> batch = createSampleBatch(5, "Substantial text sample for overshoot testing ");
        ShardedTestSource source = new ShardedTestSource(List.of(batch));

        DatasetAcquisitionEngine engine = new DatasetAcquisitionEngine();
        long targetUsable = 200; // Low target so last sample overshoots
        AcquisitionResult result = engine.acquireDataset(List.of(source), targetUsable, 0.05);

        assertTrue("Accepted usable bytes should exceed target (overshoot)", result.getUsableContentBytes() >= targetUsable);
        assertTrue("Target reached", result.isTargetReached());
        assertEquals(AcquisitionResult.Status.READY, result.getStatus());
    }

    @Test
    public void testCancellationStatusHandling() throws Exception {
        AcquisitionResult result = new AcquisitionResult();
        result.setStatus(AcquisitionResult.Status.CANCELLED);

        assertEquals("Status must be CANCELLED", AcquisitionResult.Status.CANCELLED, result.getStatus());
        assertFalse("Cancelled job is not target reached", result.isTargetReached());

        File cancelFile = new File(tempFolder.getRoot(), "cancelled.evodata");
        EvoDatasetArtifact artifact = new EvoDatasetArtifact(cancelFile);
        artifact.setStatus(EvoDatasetArtifact.Status.CANCELLED);

        DatasetSourceConfig config = new DatasetSourceConfig("LOCAL", "cancel");
        DatasetSourceStats stats = new DatasetSourceStats();
        stats.setRequestedUsableBytes(10000);
        stats.setAcceptedBytes(500);

        List<NormalizedSample> partial = createSampleBatch(2, "Partial cancelled text ");
        artifact.save(partial, config, stats, 0.02);

        assertEquals("Cancelled status must be retained", EvoDatasetArtifact.Status.CANCELLED, artifact.getStatus());
        EvoDatasetArtifact loaded = EvoDatasetArtifact.load(cancelFile);
        assertEquals("Loaded artifact status must preserve CANCELLED", EvoDatasetArtifact.Status.CANCELLED, loaded.getStatus());
    }

    @Test
    public void testProgressBasedOnUsableContentMinimum() throws Exception {
        List<NormalizedSample> batch = createSampleBatch(10, "Progress accounting sample ");
        ShardedTestSource source = new ShardedTestSource(List.of(batch));

        DatasetAcquisitionEngine engine = new DatasetAcquisitionEngine();
        long target = 50000; // Larger target so source is exhausted
        AcquisitionResult result = engine.acquireDataset(List.of(source), target, 0.05);

        double expectedCoverage = (result.getUsableContentBytes() * 100.0) / target;
        assertEquals("Coverage percent must be based on usable content vs target minimum", expectedCoverage, result.getCoveragePercent(), 0.01);
    }

    private List<NormalizedSample> createSampleBatch(int count, String prefix) {
        List<NormalizedSample> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(NormalizedSample.createTextSample(prefix + "#" + i + " with sufficient English text for training.", "batch"));
        }
        return list;
    }

    static class ShardedTestSource implements DatasetSource {
        private final List<List<NormalizedSample>> shards;
        private int currentShard = 0;
        private int currentIndex = 0;
        private final DatasetSourceStats stats = new DatasetSourceStats();
        private final DatasetSourceConfig config = new DatasetSourceConfig("MOCK", "sharded");

        ShardedTestSource(List<List<NormalizedSample>> shards) {
            this.shards = shards;
        }

        @Override public String getSourceName() { return "ShardedTestSource"; }
        @Override public DatasetSourceConfig getConfig() { return config; }
        @Override public DatasetSourceStats getStats() { return stats; }
        @Override public void initialize() {}

        @Override
        public boolean hasNext() {
            while (currentShard < shards.size()) {
                if (currentIndex < shards.get(currentShard).size()) {
                    return true;
                }
                currentShard++;
                currentIndex = 0;
            }
            return false;
        }

        @Override
        public NormalizedSample next() {
            if (!hasNext()) throw new java.util.NoSuchElementException();
            NormalizedSample s = shards.get(currentShard).get(currentIndex++);
            long len = s.toFullText().getBytes(StandardCharsets.UTF_8).length;
            stats.addBytesRead(len);
            stats.addDownloadedBytes(len);
            stats.incrementSamplesRead();
            return s;
        }

        @Override public void close() {}
    }
}
