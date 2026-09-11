package eu.kalafatic.evolution.forge.data.impl.discovery;

import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.discovery.TrainingDataSourceDiscovery;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.impl.source.LocalDatasetSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Discovery provider for public-domain literature, books, and classic text collections (e.g. Jules Verne, Gutenberg).
 */
public class PublicDomainSourceDiscoveryProvider implements TrainingDataSourceDiscovery {

    private final File booksDirectory;

    public PublicDomainSourceDiscoveryProvider() {
        this(new File("data/books"));
    }

    public PublicDomainSourceDiscoveryProvider(File booksDirectory) {
        this.booksDirectory = booksDirectory != null ? booksDirectory : new File("data/books");
    }

    @Override
    public List<DataSourceCandidate> discover(TrainingDataPreferences preferences) {
        List<DataSourceCandidate> candidates = new ArrayList<>();
        if (preferences == null) return candidates;

        if (booksDirectory.exists() && booksDirectory.isDirectory()) {
            File[] files = booksDirectory.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && (f.getName().endsWith(".txt") || f.getName().endsWith(".epub"))) {
                        candidates.add(new DataSourceCandidate(
                                "public-domain:" + f.getName(),
                                "PUBLIC_DOMAIN",
                                f.getName(),
                                "Public domain literature: " + f.getName(),
                                f.toURI().toString(),
                                "book",
                                new LocalDatasetSource(f.getAbsolutePath()),
                                preferences.getRequiredLanguage(),
                                List.of(preferences.getRequiredLanguage()),
                                List.of("literature", "books", "public-domain"),
                                preferences.getTopics(),
                                "books",
                                "text",
                                f.length(),
                                f.length(),
                                0.95,
                                0.9,
                                "public-domain",
                                "gutenberg",
                                0.95,
                                0.0,
                                f.length()
                        ));
                    }
                }
            }
        }
        return candidates;
    }
}
