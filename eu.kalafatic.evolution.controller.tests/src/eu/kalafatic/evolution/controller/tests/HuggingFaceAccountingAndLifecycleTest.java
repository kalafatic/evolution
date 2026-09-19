package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.discovery.TrainingDataSourceDiscovery;
import eu.kalafatic.evolution.forge.data.api.downloader.DataDownloader;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadRequest;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadResult;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionResult;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DataCleaner;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DatasetDeduplicator;
import eu.kalafatic.evolution.forge.data.impl.pipeline.TrainingSampleQualityScorer;
import eu.kalafatic.evolution.forge.data.impl.service.TrainingDataAcquisitionServiceImpl;
import eu.kalafatic.evolution.forge.data.impl.source.HuggingFaceDatasetSource;
import eu.kalafatic.evolution.forge.data.impl.writer.EvoDataWriter;
import eu.kalafatic.evolution.controller.tools.DatasetAcquisitionTool;

public class HuggingFaceAccountingAndLifecycleTest {

    @Test
    public void testAlpacaSchemaExtractionAndChunkStateBeforeConsumption() throws Exception {
        // 1. 100 valid Alpaca-style rows: instruction/input/output -> 100 extracted, 0 rejected
        DataDownloader mockDownloader = new DataDownloader() {
            @Override
            public DownloadResult download(DownloadRequest request) throws IOException {
                JSONObject root = new JSONObject();
                JSONArray rows = new JSONArray();
                for (int i = 0; i < 100; i++) {
                    JSONObject row = new JSONObject();
                    row.put("instruction", "Alpaca instruction #" + i);
                    row.put("input", "Alpaca input context #" + i);
                    row.put("output", "Alpaca output response #" + i);
                    rows.put(new JSONObject().put("row", row));
                }
                root.put("rows", rows);
                String json = root.toString();
                return new DownloadResult(200, "OK", json, null, json.getBytes(StandardCharsets.UTF_8).length, "application/json");
            }
        };

        DatasetSourceConfig config = new DatasetSourceConfig("HUGGING_FACE", "tatsu-lab/alpaca");
        config.setSplit("train");

        try (HuggingFaceDatasetSource source = new HuggingFaceDatasetSource(config, mockDownloader)) {
            source.initialize();

            // After initialize(), fetchNextChunk() has run for page 1.
            assertEquals(100, source.getTotalRowsFetched());
            assertEquals(100, source.getTotalSamplesExtracted());
            assertEquals(0, source.getTotalExtractionRejections());

            // 3. Verify [HF-TRACE] / stats reports cumulative accepted as 0 BEFORE next() consumption
            DatasetSourceStats stats = source.getStats();
            assertEquals(0, stats.getAcceptedRecords());
            assertEquals(0, stats.getAcceptedBytes());

            // 4. Verify source is NOT SOURCE_EMPTY while currentChunk contains samples
            assertTrue("Source MUST have next samples available", source.hasNext());

            // 2. Verify acceptance occurs correctly when next() consumes samples
            NormalizedSample firstSample = source.next();
            assertNotNull(firstSample);
            assertEquals(TrainingSampleType.INSTRUCTION, firstSample.getType());
            assertEquals(1, stats.getAcceptedRecords());
            assertTrue(stats.getAcceptedBytes() > 0);

            // Consume rest of chunk
            int totalConsumed = 1;
            while (source.hasNext()) {
                source.next();
                totalConsumed++;
            }
            assertEquals(100, totalConsumed);
            assertEquals(100, stats.getAcceptedRecords());
        }
    }

    @Test
    public void testRawHttpBytesAndAcceptedBytesSeparation() throws Exception {
        // 7. Verify raw HTTP bytes and accepted bytes remain separate (downloadedBytes not double-counted by next())
        final int httpResponseBodyLength = 5000;
        DataDownloader mockDownloader = new DataDownloader() {
            @Override
            public DownloadResult download(DownloadRequest request) throws IOException {
                JSONObject root = new JSONObject();
                JSONArray rows = new JSONArray();
                for (int i = 0; i < 5; i++) {
                    rows.put(new JSONObject().put("row", new JSONObject().put("text", "Sample text line #" + i)));
                }
                root.put("rows", rows);
                String json = root.toString();
                // Return explicitly padded byte count to represent HTTP response size
                return new DownloadResult(200, "OK", json, null, httpResponseBodyLength, "application/json");
            }
        };

        DatasetSourceConfig config = new DatasetSourceConfig("HUGGING_FACE", "mock/repo");
        try (HuggingFaceDatasetSource source = new HuggingFaceDatasetSource(config, mockDownloader)) {
            source.initialize();

            DatasetSourceStats stats = source.getStats();
            // Downloaded bytes immediately after page fetch = raw HTTP body bytes
            assertEquals(httpResponseBodyLength, stats.getDownloadedBytes());
            assertEquals(0, stats.getAcceptedBytes());

            // Consume all 5 samples
            long totalSampleTextBytes = 0;
            while (source.hasNext()) {
                NormalizedSample sample = source.next();
                totalSampleTextBytes += sample.toFullText().getBytes(StandardCharsets.UTF_8).length;
            }

            // Downloaded bytes MUST REMAIN EXACTLY httpResponseBodyLength, NOT httpResponseBodyLength + totalSampleTextBytes
            assertEquals("Downloaded bytes MUST NOT double-count sample text bytes", httpResponseBodyLength, stats.getDownloadedBytes());
            assertEquals("Accepted bytes MUST equal total sample text bytes", totalSampleTextBytes, stats.getAcceptedBytes());
            assertNotEquals(stats.getDownloadedBytes(), stats.getAcceptedBytes());
        }
    }

    @Test
    public void testMultiPageTraversalForTargetUsableBytes() throws Exception {
        // 9. Verify a 20 MB target requires multiple HTTP pages and is evaluated against accepted bytes, not HTTP bytes
        DataDownloader mockDownloader = new DataDownloader() {
            @Override
            public DownloadResult download(DownloadRequest request) throws IOException {
                String url = request.getUrl();
                int offset = 0;
                if (url.contains("offset=")) {
                    String sub = url.substring(url.indexOf("offset=") + 7);
                    if (sub.contains("&")) sub = sub.substring(0, sub.indexOf("&"));
                    offset = Integer.parseInt(sub);
                }

                JSONObject root = new JSONObject();
                JSONArray rows = new JSONArray();
                // Return 10 rows per page up to offset 50 (5 pages total)
                if (offset < 50) {
                    for (int i = 0; i < 10; i++) {
                        rows.put(new JSONObject().put("row", new JSONObject().put("text", "Row text padding 01234567890123456789012345678901234567890123456789012345678901234567890123456789 #" + (offset + i))));
                    }
                }
                root.put("rows", rows);
                String json = root.toString();
                return new DownloadResult(200, "OK", json, null, json.getBytes(StandardCharsets.UTF_8).length, "application/json");
            }
        };

        // Target: 2,500 accepted bytes (requires ~3 pages)
        long targetUsableBytes = 2500;
        DatasetSourceConfig config = new DatasetSourceConfig("HUGGING_FACE", "multipage/repo");
        config.setMaxBytes(targetUsableBytes);

        try (HuggingFaceDatasetSource source = new HuggingFaceDatasetSource(config, mockDownloader)) {
            source.initialize();

            int samplesConsumed = 0;
            while (source.hasNext()) {
                NormalizedSample s = source.next();
                assertNotNull(s);
                samplesConsumed++;
            }

            assertTrue("Must consume samples across multiple pages", samplesConsumed >= 20);
            assertTrue("Accepted bytes must satisfy target limit", source.getStats().getAcceptedBytes() >= targetUsableBytes);
        }
    }

    @Test
    public void testInsufficientSourceDataWhenSourceExhaustsBeforeTarget() throws Exception {
        // 10. Verify INSUFFICIENT_SOURCE_DATA is returned when source genuinely ends before targetUsableBytes
        // 11. Verify HF HTTP 200 response with valid rows cannot result in SOURCE_EMPTY
        DatasetSource source = new DatasetSource() {
            private int index = 0;
            private final DatasetSourceConfig config = new DatasetSourceConfig("HUGGING_FACE", "short/repo");
            private final DatasetSourceStats stats = new DatasetSourceStats();

            @Override public String getSourceName() { return "short/repo"; }
            @Override public DatasetSourceConfig getConfig() { return config; }
            @Override public DatasetSourceStats getStats() { return stats; }
            @Override public void initialize() {}

            @Override
            public boolean hasNext() {
                return index < 5; // Only 5 records available (~500 bytes)
            }

            @Override
            public NormalizedSample next() {
                index++;
                return NormalizedSample.createTextSample("Short dataset text sample record #" + index, "short/repo");
            }

            @Override public void close() {}
        };

        List<DataSourceCandidate> candidateList = List.of(new DataSourceCandidate("short/repo", "HUGGING_FACE", source, "en", List.of("text"), 500, 0.9));
        TrainingDataSourceDiscovery discovery = prefs -> candidateList;

        TrainingDataAcquisitionServiceImpl service = new TrainingDataAcquisitionServiceImpl(
                new DataCleaner(),
                new TrainingSampleQualityScorer(0.5),
                new DatasetDeduplicator(false),
                null,
                discovery
        );

        long requestedTargetBytes = 20_000; // 20 KB requested, only 500 B available
        TrainingDataPreferences prefs = TrainingDataPreferences.builder().minimumUsableBytes(requestedTargetBytes).build();
        TrainingDataAcquisitionRequest req = new TrainingDataAcquisitionRequest().setPreferences(prefs);

        TrainingDataAcquisitionResult result = service.acquireDataset(req);

        assertFalse("Target reached MUST be false when source exhausts before target", result.isTargetReached());
        assertEquals("Status MUST be INSUFFICIENT_SOURCE_DATA", TrainingDataAcquisitionResult.Status.INSUFFICIENT_SOURCE_DATA, result.getStatus());
        assertTrue("Shortfall bytes must be reported", result.getShortfallBytes() > 0);
        assertTrue("Usable content bytes must be > 0", result.getUsableContentBytes() > 0);
    }

    @Test
    public void testMaxSamplesZeroMeansUnlimited() throws Exception {
        // 8. Verify maxSamples = 0 means unlimited
        DatasetSourceConfig config = new DatasetSourceConfig("HUGGING_FACE", "test/repo");
        config.setMaxSamples(0); // 0 = unlimited

        DataDownloader mockDownloader = new DataDownloader() {
            @Override
            public DownloadResult download(DownloadRequest request) throws IOException {
                JSONObject root = new JSONObject();
                JSONArray rows = new JSONArray();
                rows.put(new JSONObject().put("row", new JSONObject().put("text", "Sample record")));
                root.put("rows", rows);
                String json = root.toString();
                return new DownloadResult(200, "OK", json, null, json.getBytes(StandardCharsets.UTF_8).length, "application/json");
            }
        };

        try (HuggingFaceDatasetSource source = new HuggingFaceDatasetSource(config, mockDownloader)) {
            source.initialize();
            assertTrue(source.hasNext());
            assertNotNull(source.next());
        }
    }

    @Test
    public void testSchemaExtractionFormats() throws Exception {
        // 12. Verify schema extraction for multiple HF schemas
        DataDownloader mockSchemaDownloader = new DataDownloader() {
            @Override
            public DownloadResult download(DownloadRequest request) throws IOException {
                JSONObject root = new JSONObject();
                JSONArray rows = new JSONArray();

                // 1. Instruction / Input / Output
                rows.put(new JSONObject().put("row", new JSONObject().put("instruction", "Inst 1").put("input", "In 1").put("output", "Out 1")));
                // 2. Text
                rows.put(new JSONObject().put("row", new JSONObject().put("text", "Plain text sample row")));
                // 3. Messages
                JSONArray msgs = new JSONArray()
                        .put(new JSONObject().put("role", "user").put("content", "User message"))
                        .put(new JSONObject().put("role", "assistant").put("content", "Assistant message"));
                rows.put(new JSONObject().put("row", new JSONObject().put("messages", msgs)));
                // 4. Conversations
                JSONArray convs = new JSONArray()
                        .put(new JSONObject().put("from", "human").put("value", "Human prompt"))
                        .put(new JSONObject().put("from", "gpt").put("value", "GPT response"));
                rows.put(new JSONObject().put("row", new JSONObject().put("conversations", convs)));
                // 5. Question / Answer
                rows.put(new JSONObject().put("row", new JSONObject().put("question", "What is 2+2?").put("answer", "4")));
                // 6. Prompt / Response
                rows.put(new JSONObject().put("row", new JSONObject().put("prompt", "Prompt test").put("response", "Response test")));
                // 7. Chosen
                rows.put(new JSONObject().put("row", new JSONObject().put("chosen", "Chosen response string text")));

                root.put("rows", rows);
                String json = root.toString();
                return new DownloadResult(200, "OK", json, null, json.getBytes(StandardCharsets.UTF_8).length, "application/json");
            }
        };

        DatasetSourceConfig config = new DatasetSourceConfig("HUGGING_FACE", "schema/test");
        try (HuggingFaceDatasetSource source = new HuggingFaceDatasetSource(config, mockSchemaDownloader)) {
            source.initialize();

            List<NormalizedSample> extracted = new ArrayList<>();
            while (source.hasNext()) {
                extracted.add(source.next());
            }

            assertEquals("All 7 schemas must produce valid extracted samples", 7, extracted.size());
            assertEquals(TrainingSampleType.INSTRUCTION, extracted.get(0).getType());
            assertEquals(TrainingSampleType.TEXT, extracted.get(1).getType());
            assertEquals(TrainingSampleType.CHAT, extracted.get(2).getType());
            assertEquals(TrainingSampleType.CHAT, extracted.get(3).getType());
            assertEquals(TrainingSampleType.INSTRUCTION, extracted.get(4).getType());
            assertEquals(TrainingSampleType.INSTRUCTION, extracted.get(5).getType());
            assertEquals(TrainingSampleType.TEXT, extracted.get(6).getType());
        }
    }

    @Test
    public void testEvoDataPreparationReceivesAcceptedSamples() throws Exception {
        // 13. Regression test that existing .evodata preparation receives accepted samples
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "evodata_test_" + System.currentTimeMillis());
        tempDir.mkdirs();

        try {
            DatasetSource mockSrc = new DatasetSource() {
                private int idx = 0;
                private final DatasetSourceConfig cfg = new DatasetSourceConfig("HUGGING_FACE", "evodata/repo");
                private final DatasetSourceStats st = new DatasetSourceStats();
                @Override public String getSourceName() { return "evodata/repo"; }
                @Override public DatasetSourceConfig getConfig() { return cfg; }
                @Override public DatasetSourceStats getStats() { return st; }
                @Override public void initialize() {}
                @Override public boolean hasNext() { return idx < 10; }
                @Override public NormalizedSample next() {
                    idx++;
                    NormalizedSample sample = NormalizedSample.createTextSample("Valid training dataset text content block record #" + idx, "evodata/repo");
                    st.incrementAccepted();
                    st.addAcceptedBytes(sample.toFullText().getBytes(StandardCharsets.UTF_8).length);
                    return sample;
                }
                @Override public void close() {}
            };

            List<NormalizedSample> samples = new ArrayList<>();
            while (mockSrc.hasNext()) {
                samples.add(mockSrc.next());
            }

            File targetFile = new File(tempDir, "test.evodata");
            EvoDataWriter writer = new EvoDataWriter();
            EvoDatasetArtifact artifact = writer.write(targetFile, samples, mockSrc.getConfig(), mockSrc.getStats(), 0.1);

            assertNotNull(artifact);
            assertTrue("EvoDatasetArtifact must contain train samples", artifact.getTrainSamples().size() > 0);
            assertTrue("Artifact total train tokens must be > 0", artifact.getTotalTrainTokens() > 0);

            EvoDatasetArtifact loaded = EvoDatasetArtifact.load(targetFile);
            assertEquals(artifact.getTrainSamples().size(), loaded.getTrainSamples().size());
        } finally {
            deleteRecursive(tempDir);
        }
    }

    @Test
    public void testHuggingFaceParquetFallbackOnHttpError() throws Exception {
        // Mock downloader that fails on offset 100 with HTTP 500, but recovers via /parquet API
        DataDownloader mockDownloader = new DataDownloader() {
            @Override
            public DownloadResult download(DownloadRequest request) throws IOException {
                String url = request.getUrl();
                if (url.contains("/rows") && url.contains("offset=0")) {
                    JSONObject root = new JSONObject();
                    JSONArray rows = new JSONArray();
                    for (int i = 0; i < 5; i++) {
                        rows.put(new JSONObject().put("row", new JSONObject().put("text", "Initial row #" + i)));
                    }
                    root.put("rows", rows);
                    String json = root.toString();
                    return new DownloadResult(200, "OK", json, null, json.getBytes(StandardCharsets.UTF_8).length, "application/json");
                } else if (url.contains("/rows")) {
                    throw new IOException("Hugging Face HTTP Error (500): Server Error");
                } else if (url.contains("/parquet?dataset=")) {
                    JSONObject root = new JSONObject();
                    JSONArray files = new JSONArray();
                    JSONObject file1 = new JSONObject();
                    file1.put("dataset", "roneneldan/TinyStories");
                    file1.put("config", "default");
                    file1.put("split", "train");
                    file1.put("url", "https://mock.hf.co/0000.parquet");
                    files.put(file1);
                    root.put("parquet_files", files);
                    String json = root.toString();
                    return new DownloadResult(200, "OK", json, null, json.getBytes(StandardCharsets.UTF_8).length, "application/json");
                } else if (url.contains("0000.parquet")) {
                    // Parquet fallback payload containing text lines
                    String textPayload = "{\"prompt\": \"Once upon a time in TinyStories.\", \"completion\": \"The end.\"} \n{\"prompt\": \"A little bird sang.\", \"completion\": \"It was happy.\"}";
                    byte[] raw = textPayload.getBytes(StandardCharsets.UTF_8);
                    return new DownloadResult(200, "OK", textPayload, raw, null, raw.length, "application/octet-stream");
                }
                throw new IOException("404 Not Found");
            }
        };

        DatasetSourceConfig config = new DatasetSourceConfig("HUGGING_FACE", "roneneldan/TinyStories");
        config.setSplit("train");

        try (HuggingFaceDatasetSource source = new HuggingFaceDatasetSource(config, mockDownloader)) {
            source.initialize();

            int totalConsumed = 0;
            while (source.hasNext()) {
                NormalizedSample sample = source.next();
                assertNotNull(sample);
                totalConsumed++;
            }

            // 5 samples from page 0 rows + 2 samples from parquet fallback
            assertEquals("Source MUST recover via Parquet fallback and yield 7 total samples", 7, totalConsumed);
        }
    }

    @Test
    public void testResolveDatasetOutputDirFolderStructure() {
        File baseDir = new File("/home/petr/workspace/forge-input");
        File resolved = DatasetAcquisitionTool.resolveDatasetOutputDir(baseDir.getAbsolutePath(), "tatsu-lab/alpaca");
        assertEquals("alpaca", resolved.getName());
        assertEquals(new File(baseDir, "alpaca").getAbsolutePath(), resolved.getAbsolutePath());

        File resolved2 = DatasetAcquisitionTool.resolveDatasetOutputDir(resolved.getAbsolutePath(), "tatsu-lab/alpaca");
        assertEquals("alpaca", resolved2.getName());
        assertEquals(resolved.getAbsolutePath(), resolved2.getAbsolutePath());

        File resolved3 = DatasetAcquisitionTool.resolveDatasetOutputDir(baseDir.getAbsolutePath(), "Salesforce/wikitext");
        assertEquals("wikitext", resolved3.getName());
        assertEquals(new File(baseDir, "wikitext").getAbsolutePath(), resolved3.getAbsolutePath());
    }

    private void deleteRecursive(File f) {
        if (f != null && f.exists()) {
            if (f.isDirectory()) {
                File[] children = f.listFiles();
                if (children != null) {
                    for (File c : children) deleteRecursive(c);
                }
            }
            f.delete();
        }
    }
}
