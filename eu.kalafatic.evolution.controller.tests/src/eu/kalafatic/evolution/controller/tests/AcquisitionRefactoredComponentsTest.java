package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.downloader.DataDownloader;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadRequest;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadResult;
import eu.kalafatic.evolution.forge.data.api.processor.DataDeduplicator;
import eu.kalafatic.evolution.forge.data.api.processor.DataFilter;
import eu.kalafatic.evolution.forge.data.api.processor.DataNormalizer;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionResult;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionService;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.api.writer.TrainingDatasetWriter;
import eu.kalafatic.evolution.forge.data.impl.downloader.HuggingFaceDownloader;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DataCleaner;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DatasetDeduplicator;
import eu.kalafatic.evolution.forge.data.impl.pipeline.TrainingSampleQualityScorer;
import eu.kalafatic.evolution.forge.data.impl.service.TrainingDataAcquisitionServiceImpl;

public class AcquisitionRefactoredComponentsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testDownloadRequestAndResultValueObjects() {
        DownloadRequest request = new DownloadRequest("https://example.com/api")
                .setMethod("GET")
                .setConnectTimeoutMs(3000)
                .setReadTimeoutMs(6000)
                .setUserAgent("TestAgent/1.0")
                .addHeader("Authorization", "Bearer token123");

        assertEquals("https://example.com/api", request.getUrl());
        assertEquals("GET", request.getMethod());
        assertEquals(3000, request.getConnectTimeoutMs());
        assertEquals(6000, request.getReadTimeoutMs());
        assertEquals("TestAgent/1.0", request.getUserAgent());
        assertEquals("Bearer token123", request.getHeaders().get("Authorization"));

        DownloadResult result = new DownloadResult(200, "OK", "{\"status\":\"success\"}", null, 22, "application/json");
        assertTrue(result.isSuccess());
        assertEquals(200, result.getStatusCode());
        assertEquals("OK", result.getStatusMessage());
        assertEquals("{\"status\":\"success\"}", result.getContentText());
        assertEquals(22, result.getDownloadedBytes());
        assertEquals("application/json", result.getContentType());
        result.close();
    }

    @Test
    public void testHuggingFaceDatasetSourceWithFakeDownloader() throws Exception {
        DataDownloader fakeDownloader = new DataDownloader() {
            @Override
            public DownloadResult download(DownloadRequest request) throws IOException {
                if (request.getUrl().contains("/splits")) {
                    JSONObject json = new JSONObject();
                    JSONArray splits = new JSONArray();
                    JSONObject split = new JSONObject().put("config", "default").put("split", "train");
                    splits.put(split);
                    json.put("splits", splits);
                    return new DownloadResult(200, "OK", json.toString(), null, json.toString().length(), "application/json");
                } else if (request.getUrl().contains("/rows")) {
                    JSONObject json = new JSONObject();
                    JSONArray rows = new JSONArray();
                    JSONObject row = new JSONObject();
                    JSONObject inner = new JSONObject().put("text", "User: Hello from fake HuggingFace row.\nAssistant: Welcome to EVO Forge.");
                    row.put("row", inner);
                    rows.put(row);
                    json.put("rows", rows);
                    return new DownloadResult(200, "OK", json.toString(), null, json.toString().length(), "application/json");
                }
                throw new IOException("Unexpected URL: " + request.getUrl());
            }
        };

        DatasetSourceConfig config = new DatasetSourceConfig("HUGGING_FACE", "fake/repo");
        config.setMaxSamples(1);

        try (eu.kalafatic.evolution.forge.data.impl.source.HuggingFaceDatasetSource source =
                     new eu.kalafatic.evolution.forge.data.impl.source.HuggingFaceDatasetSource(config, fakeDownloader)) {
            source.initialize();
            assertTrue(source.hasNext());
            NormalizedSample sample = source.next();
            assertNotNull(sample);
            assertTrue(sample.toFullText().contains("Hello from fake HuggingFace row"));
        }
    }

    @Test
    public void testTrainingDataAcquisitionServiceWithMinimumUsableEnforcement() throws Exception {
        DatasetSource fakeSource = new DatasetSource() {
            private int count = 0;
            private final DatasetSourceConfig cfg = new DatasetSourceConfig("MOCK", "test");
            private final DatasetSourceStats stats = new DatasetSourceStats();

            @Override public String getSourceName() { return "FakeSource"; }
            @Override public DatasetSourceConfig getConfig() { return cfg; }
            @Override public DatasetSourceStats getStats() { return stats; }
            @Override public void initialize() {}

            @Override
            public boolean hasNext() {
                return count < 10;
            }

            @Override
            public NormalizedSample next() {
                count++;
                return NormalizedSample.createTextSample("Sample turn #" + count + " for service unit testing with sufficient length.", "src");
            }

            @Override public void close() {}
        };

        TrainingDataAcquisitionService service = new TrainingDataAcquisitionServiceImpl();
        TrainingDataAcquisitionRequest request = new TrainingDataAcquisitionRequest()
                .addSource(fakeSource)
                .setMinimumUsableBytes(500)
                .setValidationSplitRatio(0.1);

        TrainingDataAcquisitionResult result = service.acquireDataset(request);

        assertTrue("Usable bytes should meet minimum requested", result.getUsableContentBytes() >= 500);
        assertTrue("Target reached flag should be true", result.isTargetReached());
        assertEquals(TrainingDataAcquisitionResult.Status.READY, result.getStatus());
        assertFalse("Accepted samples should not be empty", result.getAcceptedSamples().isEmpty());
    }

    @Test
    public void testEvoDataWriterSerialization() throws Exception {
        TrainingDatasetWriter writer = new eu.kalafatic.evolution.forge.data.impl.writer.EvoDataWriter();
        File artifactFile = new File(tempFolder.getRoot(), "writer_test.evodata");

        List<NormalizedSample> samples = List.of(
                NormalizedSample.createTextSample("User: Writer contract sample 1.\nAssistant: Clear detailed answer.", "src"),
                NormalizedSample.createTextSample("User: Writer contract sample 2.\nAssistant: Second clean response.", "src")
        );

        DatasetSourceConfig config = new DatasetSourceConfig("LOCAL", "writer_test");
        DatasetSourceStats stats = new DatasetSourceStats();
        stats.setAcceptedBytes(1000);

        EvoDatasetArtifact artifact = writer.write(artifactFile, samples, config, stats, 0.05);

        assertNotNull(artifact);
        assertTrue(artifactFile.exists());
        EvoDatasetArtifact loaded = EvoDatasetArtifact.load(artifactFile);
        assertEquals(EvoDatasetArtifact.Status.READY, loaded.getStatus());
        assertEquals(2, loaded.getTrainSamples().size() + loaded.getValSamples().size());
    }
}
