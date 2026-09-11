package eu.kalafatic.evolution.forge.data.impl.discovery;

import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.discovery.TrainingDataSourceDiscovery;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.impl.source.LocalDatasetSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Local filesystem dataset source discovery provider.
 * Discovers local files and folders satisfying training preferences.
 */
public class LocalSourceDiscoveryProvider implements TrainingDataSourceDiscovery {

    private final List<File> baseDirectories;

    public LocalSourceDiscoveryProvider() {
        this(List.of(new File("."), new File("datasets"), new File("data")));
    }

    public LocalSourceDiscoveryProvider(List<File> baseDirectories) {
        this.baseDirectories = baseDirectories != null ? List.copyOf(baseDirectories) : List.of();
    }

    @Override
    public List<DataSourceCandidate> discover(TrainingDataPreferences preferences) {
        List<DataSourceCandidate> candidates = new ArrayList<>();
        if (preferences == null) return candidates;

        for (File dir : baseDirectories) {
            if (dir != null && dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && (f.getName().endsWith(".txt") || f.getName().endsWith(".jsonl") || f.getName().endsWith(".md") || f.getName().endsWith(".evodata"))) {
                            long estUsable = Math.max(1024, f.length());
                            LocalDatasetSource src = new LocalDatasetSource(f.getAbsolutePath());
                            candidates.add(new DataSourceCandidate(
                                    "local:" + f.getName(),
                                    "LOCAL",
                                    f.getName(),
                                    "Local file data source: " + f.getAbsolutePath(),
                                    f.toURI().toString(),
                                    "file",
                                    src,
                                    preferences.getRequiredLanguage(),
                                    List.of(preferences.getRequiredLanguage()),
                                    preferences.getPrimaryDomains(),
                                    preferences.getTopics(),
                                    preferences.getContentType(),
                                    f.getName().substring(f.getName().lastIndexOf('.') + 1),
                                    f.length(),
                                    estUsable,
                                    0.9,
                                    0.85,
                                    "local",
                                    "local-fs",
                                    0.95,
                                    0.05,
                                    estUsable
                            ));
                        }
                    }
                }
            }
        }
        return candidates;
    }
}
