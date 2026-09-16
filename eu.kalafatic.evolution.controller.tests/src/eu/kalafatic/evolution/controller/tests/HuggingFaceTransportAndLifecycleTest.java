package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveFailureType;
import eu.kalafatic.evolution.controller.tools.DatasetAcquisitionTool;
import eu.kalafatic.evolution.forge.data.api.downloader.DataDownloader;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadRequest;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadResult;
import eu.kalafatic.evolution.forge.data.api.downloader.TransportClassification;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.ResolvedSource;
import eu.kalafatic.evolution.forge.data.impl.downloader.HuggingFaceDownloader;
import eu.kalafatic.evolution.forge.data.impl.service.TrainingDataAcquisitionServiceImpl;
import eu.kalafatic.evolution.forge.data.impl.source.HuggingFaceDatasetSource;

public class HuggingFaceTransportAndLifecycleTest {

    @Test
    public void testTransientRetriesAndEventualSuccess() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);

        DataDownloader fakeDownloader = new DataDownloader() {
            @Override
            public DownloadResult download(DownloadRequest request) throws IOException {
                int att = attempts.getAndIncrement();
                if (att < 2) {
                    // Return 503 Service Unavailable for first 2 attempts
                    return new DownloadResult(503, "Service Unavailable", "Overloaded", null, 10, "text/plain", 50, att, TransportClassification.HTTP_5XX);
                }
                // Return 200 OK on 3rd attempt
                return new DownloadResult(200, "OK", "{\"splits\":[]}", null, 15, "application/json", 30, att, TransportClassification.HTTP_SUCCESS);
            }
        };

        HuggingFaceDownloader downloader = new HuggingFaceDownloader() {
            @Override
            public DownloadResult download(DownloadRequest request) throws IOException {
                return fakeDownloader.download(request);
            }
        };

        DownloadRequest request = new DownloadRequest("https://datasets-server.huggingface.co/splits?dataset=test/repo")
                .setMaxRetries(3)
                .setBackoffBaseMs(10);

        DownloadResult result = downloader.download(request);
        assertTrue("Request should eventually succeed after retries", result.isSuccess());
        assertEquals(200, result.getStatusCode());
        assertEquals(3, attempts.get());
    }

    @Test
    public void testPermanentFailureDoesNotRetry() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);

        DataDownloader fakeDownloader = new DataDownloader() {
            @Override
            public DownloadResult download(DownloadRequest request) throws IOException {
                attempts.getAndIncrement();
                return new DownloadResult(401, "Unauthorized", "Gated Dataset", null, 12, "application/json", 40, 0, TransportClassification.HTTP_401);
            }
        };

        HuggingFaceDownloader downloader = new HuggingFaceDownloader() {
            @Override
            public DownloadResult download(DownloadRequest request) throws IOException {
                return fakeDownloader.download(request);
            }
        };

        DownloadRequest request = new DownloadRequest("https://datasets-server.huggingface.co/splits?dataset=gated/repo")
                .setMaxRetries(3)
                .setBackoffBaseMs(10);

        DownloadResult result = downloader.download(request);
        assertFalse("401 Unauthorized should fail", result.isSuccess());
        assertEquals(401, result.getStatusCode());
        assertEquals(TransportClassification.HTTP_401, result.getTransportClassification());
        assertEquals("Permanent HTTP 401 must NOT trigger retries", 1, attempts.get());
    }

    @Test
    public void testPreflightAndInitializationFailureClassification() throws Exception {
        DataDownloader fakeDownloader = new DataDownloader() {
            @Override
            public DownloadResult download(DownloadRequest request) throws IOException {
                return new DownloadResult(403, "Forbidden", "Access Denied to Gated Model", null, 25, "application/json", 50, 0, TransportClassification.HTTP_403);
            }
        };

        DatasetSourceConfig config = new DatasetSourceConfig("HUGGING_FACE", "gated/repo");
        HuggingFaceDatasetSource source = new HuggingFaceDatasetSource(config, fakeDownloader);

        ResolvedSource resolved = source.preflight();
        assertFalse("Preflight must fail for HTTP 403 Forbidden", resolved.isAccessible());
        assertNotNull("Failure reason must mention status code or reason", resolved.getFailureReason());
        assertTrue("Failure reason must reflect 403 / Forbidden / HTTP", resolved.getFailureReason().contains("403") || resolved.getFailureReason().contains("HTTP"));

        try {
            source.initialize();
            fail("initialize() must throw exception when source cannot be fetched due to HTTP error");
        } catch (IOException ioe) {
            assertTrue("Exception message should contain failure details", ioe.getMessage().contains("403") || ioe.getMessage().contains("HTTP"));
        }
    }

    @Test
    public void testDatasetAcquisitionToolFailureTypeMapping() throws Exception {
        DataDownloader fakeDownloader = new DataDownloader() {
            @Override
            public DownloadResult download(DownloadRequest request) throws IOException {
                return new DownloadResult(404, "Not Found", "Dataset does not exist", null, 20, "application/json", 30, 0, TransportClassification.HTTP_404);
            }
        };

        TrainingDataAcquisitionServiceImpl acquisitionService = new TrainingDataAcquisitionServiceImpl(
                new eu.kalafatic.evolution.forge.data.impl.pipeline.DataCleaner(),
                new eu.kalafatic.evolution.forge.data.impl.pipeline.TrainingSampleQualityScorer(0.5),
                new eu.kalafatic.evolution.forge.data.impl.pipeline.DatasetDeduplicator(false),
                new eu.kalafatic.evolution.forge.data.impl.evaluation.DefaultPreferenceEvaluator(),
                prefs -> java.util.List.of(new eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate(
                        "nonexistent/repo",
                        "HUGGING_FACE",
                        new HuggingFaceDatasetSource(new DatasetSourceConfig("HUGGING_FACE", "nonexistent/repo"), fakeDownloader),
                        "en",
                        java.util.List.of("text"),
                        0L,
                        0.9
                ))
        );

        DatasetAcquisitionTool tool = new DatasetAcquisitionTool(acquisitionService);
        String command = "{\"repository\": \"nonexistent/repo\", \"targetUsableBytes\": 10000}";
        String jsonResult = tool.execute(command, null, null);

        JSONObject resultObj = new JSONObject(jsonResult);
        assertEquals(CognitiveFailureType.INVALID_CONFIGURATION.name(), resultObj.getString("failureType"));
        assertFalse("Source must NOT be reported as exhausted when failure is HTTP 404 / INVALID_CONFIGURATION", resultObj.getBoolean("isSourceExhausted"));
    }
}
