package eu.kalafatic.evolution.forge.data.impl.service;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.service.DatasetPreparationResult;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.api.source.SourceAdapterRegistry;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Authoritative dataset preparation service converting user-selected dataset items into a unified EVO native .evodata dataset artifact.
 */
public class DatasetPreparationService {

    private final SourceAdapterRegistry registry;

    public DatasetPreparationService() {
        this(SourceAdapterRegistry.createDefaultRegistry());
    }

    public DatasetPreparationService(SourceAdapterRegistry registry) {
        this.registry = registry != null ? registry : SourceAdapterRegistry.createDefaultRegistry();
    }

    public DatasetPreparationResult prepareDatasets(List<DatasetItem> datasetItems, DatasetPreparationContext context, File outputDir) throws Exception {
        DatasetPreparationResult result = new DatasetPreparationResult();
        if (context == null) {
            context = new DatasetPreparationContext();
        }

        if (outputDir == null) {
            outputDir = new File(System.getProperty("user.home"), "workspace/datasets");
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // 1. Filter to checked items while strictly preserving user-defined order
        List<DatasetItem> checkedItems = new ArrayList<>();
        if (datasetItems != null) {
            for (DatasetItem item : datasetItems) {
                if (item != null && item.isChecked() && item.getPath() != null && !item.getPath().trim().isEmpty()) {
                    checkedItems.add(item);
                }
            }
        }

        context.log("[forge.dataset] Selected dataset items: " + checkedItems.size());
        result.setTargetBytes(context.getTargetUsableBytes());

        if (checkedItems.isEmpty()) {
            context.log("[FORGE-COMPILER] Warning: No checked dataset sources selected.");
            result.setStatus(DatasetPreparationResult.Status.INSUFFICIENT_DATA);
            result.getWarnings().add("No checked dataset sources selected.");
            return result;
        }

        // Generate canonical timestamped evodata output filename (standard date time: yyyyMMdd_HHmmss.evodata)
        String timestampStr = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String cacheKey = computeCacheKey(checkedItems, context.getTargetUsableBytes());
        File evodataFile = new File(outputDir, timestampStr + ".evodata");
        File cacheFile = new File(outputDir, "prepared_" + cacheKey + ".evodata");

        // Re-use cached evodata artifact if exact sources & configuration match
        if (cacheFile.exists() && cacheFile.length() > 0) {
            try {
                EvoDatasetArtifact cachedArtifact = EvoDatasetArtifact.load(cacheFile);
                if (cachedArtifact != null && cachedArtifact.getStatus() == EvoDatasetArtifact.Status.READY && !cachedArtifact.getTrainSamples().isEmpty()) {
                    context.log("[FORGE-COMPILER] Reusing cached prepared dataset artifact: " + cacheFile.getName());
                    result.setArtifact(cachedArtifact);
                    result.setSamples(cachedArtifact.getSamples());
                    result.setStatus(DatasetPreparationResult.Status.SUCCESS);
                    result.setOutputPath(cacheFile.getAbsolutePath());

                    for (DatasetItem item : checkedItems) {
                        result.getSuccessfulSources().add(item.getPath());
                    }
                    populateResultMetrics(result, cachedArtifact.getSamples(), checkedItems.size());
                    logFinalSummary(context, result, cacheFile.getAbsolutePath());
                    return result;
                }
            } catch (Exception ex) {
                context.log("[FORGE-COMPILER] Cache artifact loading failed, rebuilding: " + ex.getMessage());
            }
        }

        // 2. DISCOVER, IDENTIFY, ANALYZE: First analysis phase before conversion
        context.log("[FORGE-ANALYSIS] Analyzing " + checkedItems.size() + " selected sources...");
        List<NormalizedSample> allRawSamples = new ArrayList<>();
        long totalRecordsRead = 0;
        long totalRecordsRejected = 0;

        for (DatasetItem item : checkedItems) {
            if (context.isCancelled()) {
                context.log("[FORGE-PROCESS] Dataset preparation cancelled.");
                result.setStatus(DatasetPreparationResult.Status.CANCELLED);
                return result;
            }

            DatasetSourceAdapter adapter = registry.findAdapter(item);
            String adapterName = adapter != null ? adapter.getClass().getSimpleName() : "NONE";

            context.log(String.format("[FORGE-SOURCE] source=%s type=%s format=%s", item.getPath(), item.getType(), adapterName));

            if (adapter == null) {
                context.log(String.format("[FORGE-ANALYSIS] source=%s adapter=NONE estimatedBytes=0 status=SKIPPED", item.getPath()));
                result.getSkippedSources().add(item.getPath());
                continue;
            }

            DatasetInspection inspection = adapter.inspect(item);
            context.log(String.format("[FORGE-ANALYSIS] source=%s adapter=%s estimatedBytes=%d status=%s",
                    item.getPath(), adapterName, inspection.getEstimatedBytes(),
                    inspection.isExists() ? "VALID" : "INVALID"));

            if (!inspection.isSupported() || !inspection.isExists()) {
                context.log(String.format("[FORGE-ADAPTER] source=%s adapter=%s status=FAILED error=%s", item.getPath(), adapterName, inspection.getDetails()));
                result.getFailedSources().add(item.getPath());
                result.getErrors().add("Source inaccessible: " + item.getPath() + " (" + inspection.getDetails() + ")");
                continue;
            }

            // 3. CONVERT via Adapter with Failure Isolation
            try {
                context.log(String.format("[FORGE-ADAPTER] source=%s adapter=%s status=PROCESSING", item.getPath(), adapterName));
                List<NormalizedSample> converted = adapter.convert(item, context);
                int count = converted != null ? converted.size() : 0;
                totalRecordsRead += count;

                context.log(String.format("[FORGE-PROCESS] source=%s recordsRead=%d recordsAccepted=%d recordsRejected=0 duplicates=0",
                        item.getPath(), count, count));

                if (converted != null) {
                    allRawSamples.addAll(converted);
                }
                result.getSuccessfulSources().add(item.getPath());
            } catch (Exception ex) {
                context.log(String.format("[FORGE-ADAPTER] source=%s adapter=%s status=FAILED error=%s", item.getPath(), adapterName, ex.getMessage()));
                result.getFailedSources().add(item.getPath());
                result.getErrors().add("Adapter error on " + item.getPath() + ": " + ex.getMessage());
            }
        }

        // 4. MERGE, CLEAN, FILTER, DEDUPLICATE Quality Pipeline
        Set<String> seenHashes = new HashSet<>();
        List<NormalizedSample> deduplicatedSamples = new ArrayList<>();
        long currentUsableBytes = 0;
        long targetBytes = context.getTargetUsableBytes();
        long duplicateBytes = 0;
        long duplicateCount = 0;

        for (NormalizedSample sample : allRawSamples) {
            if (context.isCancelled()) break;

            if (sample == null || sample.toFullText().trim().isEmpty()) {
                totalRecordsRejected++;
                continue;
            }

            String h = sample.getHash();
            if (h != null && !h.isEmpty() && seenHashes.contains(h)) {
                duplicateBytes += sample.toFullText().getBytes(StandardCharsets.UTF_8).length;
                duplicateCount++;
                continue;
            }
            if (h != null && !h.isEmpty()) {
                seenHashes.add(h);
            }

            byte[] sampleBytes = sample.toFullText().getBytes(StandardCharsets.UTF_8);
            if (targetBytes > 0 && currentUsableBytes >= targetBytes) {
                break; // Configured target size reached
            }

            deduplicatedSamples.add(sample);
            currentUsableBytes += sampleBytes.length;
        }

        result.setRecordsRead(totalRecordsRead);
        result.setRecordsAccepted(deduplicatedSamples.size());
        result.setRecordsRejected(totalRecordsRejected);
        result.setDuplicateCount(duplicateCount);
        result.setDuplicateBytes(duplicateBytes);

        context.log(String.format("[FORGE-MERGE] sources=%d records=%d usableBytes=%d",
                result.getSuccessfulSources().size(), deduplicatedSamples.size(), currentUsableBytes));

        if (deduplicatedSamples.isEmpty()) {
            context.log("[FORGE-COMPILER] Error: No usable normalized samples generated from sources.");
            result.setStatus(DatasetPreparationResult.Status.FAILED);
            result.getErrors().add("No usable normalized samples generated from checked sources.");
            return result;
        }

        // 5. EVO NATIVE DATASET COMPILATION -> timestamp.evodata
        DatasetSourceConfig sourceConfig = new DatasetSourceConfig("MULTI_SOURCE", "checked_sources_" + checkedItems.size());
        DatasetSourceStats stats = context.getStats();
        if (stats == null) stats = new DatasetSourceStats();
        stats.setRequestedUsableBytes(targetBytes);
        stats.setAcceptedBytes(currentUsableBytes);
        stats.setDuplicateBytes(duplicateBytes);

        EvoDatasetArtifact artifact = new EvoDatasetArtifact(evodataFile);
        artifact.save(deduplicatedSamples, sourceConfig, stats, 0.02);

        // Also update cache file for fast re-use
        EvoDatasetArtifact cacheArtifact = new EvoDatasetArtifact(cacheFile);
        cacheArtifact.save(deduplicatedSamples, sourceConfig, stats, 0.02);

        result.setArtifact(artifact);
        result.setSamples(artifact.getSamples());
        result.setOutputPath(evodataFile.getAbsolutePath());
        result.setStatus(artifact.getStatus() == EvoDatasetArtifact.Status.INSUFFICIENT_SOURCE_DATA ?
                          DatasetPreparationResult.Status.INSUFFICIENT_DATA : DatasetPreparationResult.Status.SUCCESS);

        populateResultMetrics(result, deduplicatedSamples, checkedItems.size());

        context.log(String.format("[FORGE-COMPILER] output=%s usableBytes=%d status=SUCCESS",
                evodataFile.getName(), result.getAcceptedBytes()));

        logFinalSummary(context, result, evodataFile.getAbsolutePath());
        return result;
    }

    private void populateResultMetrics(DatasetPreparationResult result, List<NormalizedSample> samples, int sourcesCount) {
        result.setSourcesProcessed(sourcesCount);
        result.setSamples(samples);

        int convCount = 0;
        int msgCount = 0;
        long totalBytes = 0;
        long tokens = 0;

        for (NormalizedSample s : samples) {
            byte[] b = s.toFullText().getBytes(StandardCharsets.UTF_8);
            totalBytes += b.length;
            tokens += s.getTokenCount() > 0 ? s.getTokenCount() : (b.length / 4);

            if (s.getType() == TrainingSampleType.CONVERSATION) {
                convCount++;
                if (s.getConversationMessages() != null) {
                    msgCount += s.getConversationMessages().size();
                }
            } else if (s.getMessages() != null && !s.getMessages().isEmpty()) {
                convCount++;
                msgCount += s.getMessages().size();
            }
        }

        result.setAcceptedBytes(totalBytes);
        result.setEstimatedTokens(tokens);
        result.setConversationsCount(convCount);
        result.setMessagesCount(msgCount);
        if (result.getArtifact() != null) {
            result.setTrainSamplesCount(result.getArtifact().getTrainSamples().size());
            result.setValSamplesCount(result.getArtifact().getValSamples().size());
        } else {
            result.setTrainSamplesCount(samples.size());
            result.setValSamplesCount(0);
        }
    }

    private void logFinalSummary(DatasetPreparationContext context, DatasetPreparationResult result, String outputPath) {
        context.log("[forge.dataset]");
        context.log("[forge.dataset] Native preparation complete");
        context.log("Successful Sources: " + result.getSuccessfulSources().size());
        context.log("Failed Sources: " + result.getFailedSources().size());
        context.log("Skipped Sources: " + result.getSkippedSources().size());
        context.log("Records Read: " + result.getRecordsRead());
        context.log("Records Accepted: " + result.getRecordsAccepted());
        context.log("Records Rejected: " + result.getRecordsRejected());
        context.log("Duplicate Count: " + result.getDuplicateCount());
        context.log("Usable Bytes: " + result.getAcceptedBytes());
        context.log("Target Bytes: " + result.getTargetBytes());
        context.log("Output: " + outputPath);
        context.log("Status: " + result.getStatus().name());
    }

    private String computeCacheKey(List<DatasetItem> items, long targetBytes) {
        StringBuilder sb = new StringBuilder();
        sb.append(targetBytes).append(";");
        for (DatasetItem item : items) {
            sb.append(item.getPath()).append(":");
            File f = new File(item.getPath());
            if (f.exists()) {
                sb.append(f.lastModified()).append(":").append(f.length());
            }
            sb.append(";");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.substring(0, 16);
        } catch (Exception ex) {
            return String.valueOf(sb.toString().hashCode());
        }
    }
}
