package eu.kalafatic.evolution.forge.data.impl.collector;

import eu.kalafatic.evolution.forge.data.api.collector.CollectionContext;
import eu.kalafatic.evolution.forge.data.api.collector.CollectionResult;
import eu.kalafatic.evolution.forge.data.api.collector.CollectorType;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataCollector;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Coordinator responsible for executing selected collectors and merging normalized results with error isolation.
 */
public class TrainingDataCollectorManager {

    private final Map<CollectorType, TrainingDataCollector> registeredCollectors;

    public TrainingDataCollectorManager() {
        this.registeredCollectors = new EnumMap<>(CollectorType.class);
        // Register default collectors
        registerCollector(new ProjectDataCollector());
        registerCollector(new DiskDataCollector());
        registerCollector(new MCPDataCollector());
        registerCollector(new InternetDataCollector());
    }

    public void registerCollector(TrainingDataCollector collector) {
        if (collector != null && collector.getType() != null) {
            registeredCollectors.put(collector.getType(), collector);
        }
    }

    public TrainingDataCollector getCollector(CollectorType type) {
        return registeredCollectors.get(type);
    }

    /**
     * Executes the user-selected collectors in isolation, merges normalized results, and returns combined collection status.
     *
     * @param context Context parameters.
     * @param selectedSources Set of user-selected collector types to participate.
     * @return Aggregated CollectionResult containing items, statuses, and collector execution reports.
     */
    public CollectionResult collect(CollectionContext context, Set<CollectorType> selectedSources) {
        long startTime = System.currentTimeMillis();

        if (selectedSources == null || selectedSources.isEmpty()) {
            CollectionResult emptyResult = CollectionResult.success(CollectorType.PROJECT, new ArrayList<>(), System.currentTimeMillis() - startTime);
            emptyResult.setErrorMessage("No data sources selected");
            return emptyResult;
        }

        List<TrainingDataItem> aggregatedItems = new ArrayList<>();
        Map<CollectorType, CollectionResult> collectorResults = new HashMap<>();
        boolean anySuccess = false;
        boolean anyFailure = false;

        for (CollectorType type : selectedSources) {
            TrainingDataCollector collector = registeredCollectors.get(type);
            if (collector == null) {
                CollectionResult missingRes = CollectionResult.failure(type, "Collector implementation not registered: " + type, 0);
                collectorResults.put(type, missingRes);
                anyFailure = true;
                continue;
            }

            try {
                CollectionResult result = collector.collect(context);
                collectorResults.put(type, result);

                if (result.getStatus() == CollectionResult.Status.SUCCESS || result.getStatus() == CollectionResult.Status.PARTIAL) {
                    if (result.getItems() != null) {
                        aggregatedItems.addAll(result.getItems());
                    }
                    anySuccess = true;
                } else {
                    anyFailure = true;
                }
            } catch (Exception e) {
                CollectionResult errRes = CollectionResult.failure(type, "Unhandled error in collector: " + e.getMessage(), 0);
                collectorResults.put(type, errRes);
                anyFailure = true;
            }
        }

        CollectionResult.Status finalStatus;
        if (anySuccess && !anyFailure) {
            finalStatus = CollectionResult.Status.SUCCESS;
        } else if (anySuccess && anyFailure) {
            finalStatus = CollectionResult.Status.PARTIAL;
        } else {
            finalStatus = CollectionResult.Status.FAILED;
        }

        CollectionResult aggregatedResult = new CollectionResult(null, finalStatus, aggregatedItems);
        aggregatedResult.setDurationMs(System.currentTimeMillis() - startTime);

        // Attach individual collector reports to metadata
        Map<String, Object> reports = new HashMap<>();
        for (Map.Entry<CollectorType, CollectionResult> entry : collectorResults.entrySet()) {
            reports.put(entry.getKey().name(), entry.getValue().toString());
        }
        aggregatedResult.addItem(createSummaryMetadataItem(collectorResults));

        return aggregatedResult;
    }

    private TrainingDataItem createSummaryMetadataItem(Map<CollectorType, CollectionResult> collectorResults) {
        TrainingDataItem summary = new TrainingDataItem();
        summary.setTitle("COLLECTION_SUMMARY");
        for (Map.Entry<CollectorType, CollectionResult> entry : collectorResults.entrySet()) {
            summary.addMetadata("status_" + entry.getKey().name(), entry.getValue().getStatus().name());
            if (entry.getValue().getErrorMessage() != null) {
                summary.addMetadata("error_" + entry.getKey().name(), entry.getValue().getErrorMessage());
            }
        }
        return summary;
    }

    public Map<CollectorType, TrainingDataCollector> getRegisteredCollectors() {
        return registeredCollectors;
    }
}
