package eu.kalafatic.evolution.forge.data.impl.discovery;

import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.discovery.TrainingDataSourceDiscovery;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.impl.source.LocalDatasetSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Discovery provider for web-based text content and documentation.
 */
public class WebSourceDiscoveryProvider implements TrainingDataSourceDiscovery {

    @Override
    public List<DataSourceCandidate> discover(TrainingDataPreferences preferences) {
        List<DataSourceCandidate> candidates = new ArrayList<>();
        if (preferences == null) return candidates;

        if (preferences.getPrimaryDomains().contains("documentation") || preferences.getPrimaryDomains().contains("ai")) {
            File localWebCache = new File("data/web_cache.txt");
            if (localWebCache.exists()) {
                candidates.add(new DataSourceCandidate(
                        "web:doc-cache",
                        "WEB",
                        "Cached Web Documentation",
                        "Web documentation and technical articles",
                        localWebCache.toURI().toString(),
                        "webpage",
                        new LocalDatasetSource(localWebCache.getAbsolutePath()),
                        preferences.getRequiredLanguage(),
                        List.of(preferences.getRequiredLanguage()),
                        preferences.getPrimaryDomains(),
                        preferences.getTopics(),
                        "documentation",
                        "text",
                        localWebCache.length(),
                        localWebCache.length(),
                        0.8,
                        0.75,
                        "public",
                        "web-search",
                        0.8,
                        0.15,
                        localWebCache.length()
                ));
            }
        }
        return candidates;
    }
}
