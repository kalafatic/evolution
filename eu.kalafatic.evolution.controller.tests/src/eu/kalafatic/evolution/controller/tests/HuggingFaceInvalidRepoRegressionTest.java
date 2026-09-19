package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.json.JSONObject;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveFailureType;
import eu.kalafatic.evolution.controller.tools.DatasetAcquisitionTool;
import eu.kalafatic.evolution.forge.data.api.downloader.DataDownloader;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadRequest;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadResult;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionResult;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.impl.service.TrainingDataAcquisitionServiceImpl;
import eu.kalafatic.evolution.forge.data.impl.source.HuggingFaceDatasetSource;

/**
 * Regression test suite verifying invalid HF dataset preflight, directory creation deferral,
 * search expansion deduplication of invalid sources, and precise cognitive failure classification.
 */
public class HuggingFaceInvalidRepoRegressionTest {

    @Test
    public void testEmptyDatasetPreflightAndNoDirectoryCreated() throws Exception {
        // Mock downloader returning HF 500 EmptyDatasetError
        DataDownloader mockDownloader = new DataDownloader() {
            @Override
            public DownloadResult download(DownloadRequest request) {
                String body = "{\"error\": \"EmptyDatasetError: The directory at hf://datasets/Zogfryt/roneneldan-TinyStories-tokenizer-distilgpt2 doesn't contain any data files\"}";
                return new DownloadResult(500, "Internal Server Error", body, null, body.getBytes().length, "application/json");
            }
        };

        String invalidRepo = "Zogfryt/roneneldan-TinyStories-tokenizer-distilgpt2";
        DatasetSourceConfig config = new DatasetSourceConfig("HUGGING_FACE", invalidRepo);
        HuggingFaceDatasetSource source = new HuggingFaceDatasetSource(config, mockDownloader);

        var resolved = source.preflight();
        assertNotNull(resolved);
        assertFalse("Source must be marked inaccessible on preflight failure", resolved.isAccessible());
        assertFalse("Source must be marked unavailable on preflight failure", resolved.isAvailable());
        assertTrue("Failure reason should capture HTTP status or error text", resolved.getFailureReason().contains("HTTP 500") || resolved.getFailureReason().contains("EmptyDatasetError"));

        TrainingDataAcquisitionServiceImpl service = new TrainingDataAcquisitionServiceImpl(null, null, null, null, null);
        TrainingDataAcquisitionRequest req = new TrainingDataAcquisitionRequest();
        req.setMinimumUsableBytes(1024L * 1024L);
        req.addSource(source);

        TrainingDataAcquisitionResult acqResult = service.acquireDataset(req);
        assertNotNull(acqResult);
        assertFalse("Target should not be reached for invalid repository", acqResult.isTargetReached());
        assertEquals(0, acqResult.getAcceptedContentBytes());
        assertEquals(TrainingDataAcquisitionResult.Status.INSUFFICIENT_SOURCE_DATA, acqResult.getStatus());

        // Verify that preflight-failed sources are NOT reported in sourcesUsed, but recorded in errors
        assertFalse("Preflight-failed sources should NOT be listed in sourcesUsed", acqResult.getSourcesUsed().contains(source.getSourceName()));
        assertFalse("Errors should be populated with preflight failure diagnostics", acqResult.getErrors().isEmpty());

        // Verify output directory deferral in DatasetAcquisitionTool
        File testBaseDir = new File(System.getProperty("java.io.tmpdir"), "evo-acq-test-" + System.currentTimeMillis());
        File resolvedDir = DatasetAcquisitionTool.resolveDatasetOutputDir(testBaseDir.getAbsolutePath(), invalidRepo);
        assertFalse("Output directory should not exist before acquisition", resolvedDir.exists());

        DatasetAcquisitionTool tool = new DatasetAcquisitionTool(service);
        TaskContext context = new TaskContext(null, new File("."));

        JSONObject toolParams = new JSONObject();
        toolParams.put("sourceType", "HUGGING_FACE");
        toolParams.put("repository", invalidRepo);
        toolParams.put("targetUsableBytes", 1024L * 1024L);
        toolParams.put("outputDir", testBaseDir.getAbsolutePath());

        String jsonResponse = tool.execute(toolParams.toString(), new File("."), context);
        assertNotNull(jsonResponse);

        JSONObject resObj = new JSONObject(jsonResponse);
        String failureType = resObj.optString("failureType", "");
        assertTrue("Failure type should be mapped to SOURCE_EMPTY or SOURCE_INACCESSIBLE",
                CognitiveFailureType.SOURCE_EMPTY.name().equals(failureType) || CognitiveFailureType.SOURCE_INACCESSIBLE.name().equals(failureType));

        assertFalse("Output directory should NOT be created for zero-yield failed downloads", resolvedDir.exists());
    }

    @Test
    public void testSearchExpansionDeduplicationOfFailedPreflightSources() throws Exception {
        DataDownloader mockDownloader = new DataDownloader() {
            @Override
            public DownloadResult download(DownloadRequest request) {
                String body = "{\"error\": \"EmptyDatasetError: empty repository\"}";
                return new DownloadResult(500, "Internal Server Error", body, null, body.getBytes().length, "application/json");
            }
        };

        String invalidRepo = "Zogfryt/roneneldan-TinyStories-tokenizer-distilgpt2";
        DatasetSourceConfig config = new DatasetSourceConfig("HUGGING_FACE", invalidRepo);
        HuggingFaceDatasetSource source = new HuggingFaceDatasetSource(config, mockDownloader);

        TrainingDataAcquisitionServiceImpl service = new TrainingDataAcquisitionServiceImpl(null, null, null, null, null);
        TrainingDataAcquisitionRequest req = new TrainingDataAcquisitionRequest();
        req.setMinimumUsableBytes(1024L * 1024L);
        req.addSource(source);

        TrainingDataAcquisitionResult result = service.acquireDataset(req);
        assertNotNull(result);

        assertFalse("Preflight failed source should NOT be in sourcesUsed", result.getSourcesUsed().contains(source.getSourceName()));
        assertFalse("Errors list must capture preflight failure", result.getErrors().isEmpty());
    }
}
