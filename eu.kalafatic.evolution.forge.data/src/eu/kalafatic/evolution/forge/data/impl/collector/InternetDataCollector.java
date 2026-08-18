package eu.kalafatic.evolution.forge.data.impl.collector;

import eu.kalafatic.evolution.forge.data.api.collector.CollectionContext;
import eu.kalafatic.evolution.forge.data.api.collector.CollectionResult;
import eu.kalafatic.evolution.forge.data.api.collector.CollectorType;
import eu.kalafatic.evolution.forge.data.api.collector.SourceType;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataCollector;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataItem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects relevant external technical knowledge from the Internet.
 * Fails gracefully and isolates network errors without breaking preparation.
 */
public class InternetDataCollector extends TrainingDataCollector {

    public interface WebSearchFetcher {
        List<TrainingDataItem> searchAndFetch(String query);
    }

    private final WebSearchFetcher fetcher;

    public InternetDataCollector() {
        this(null);
    }

    public InternetDataCollector(WebSearchFetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public CollectorType getType() {
        return CollectorType.INTERNET;
    }

    @Override
    public String getName() {
        return "Internet Data Collector";
    }

    @Override
    public CollectionResult collect(CollectionContext context) {
        long startTime = System.currentTimeMillis();
        List<String> topics = context.getSearchTopics();

        if (topics == null || topics.isEmpty()) {
            return CollectionResult.success(getType(), new ArrayList<>(), System.currentTimeMillis() - startTime);
        }

        if (fetcher == null) {
            return CollectionResult.failure(getType(), "Internet search fetcher is unavailable or offline", System.currentTimeMillis() - startTime);
        }

        List<TrainingDataItem> items = new ArrayList<>();

        for (String topic : topics) {
            try {
                List<TrainingDataItem> fetched = fetcher.searchAndFetch(topic);
                if (fetched != null) {
                    for (TrainingDataItem item : fetched) {
                        item.setSourceType(SourceType.INTERNET);
                        item.addMetadata("query", topic);
                        if (!item.getMetadata().containsKey("retrievedAt")) {
                            item.addMetadata("retrievedAt", Instant.now().toString());
                        }
                        items.add(item);
                    }
                }
            } catch (Exception e) {
                // Topic search failed; continue processing remaining topics
            }
        }

        return CollectionResult.success(getType(), items, System.currentTimeMillis() - startTime);
    }
}
