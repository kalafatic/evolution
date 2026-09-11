package eu.kalafatic.evolution.forge.data.impl.discovery;

import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.discovery.TrainingDataSourceDiscovery;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.impl.source.EvoCodebaseDatasetSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Discovery provider for extracting structured training data from the EVO codebase.
 */
public class EvoCodebaseSourceDiscoveryProvider implements TrainingDataSourceDiscovery {

    private final File rootDir;

    public EvoCodebaseSourceDiscoveryProvider() {
        this(new File("."));
    }

    public EvoCodebaseSourceDiscoveryProvider(File rootDir) {
        this.rootDir = rootDir != null ? rootDir : new File(".");
    }

    @Override
    public List<DataSourceCandidate> discover(TrainingDataPreferences preferences) {
        List<DataSourceCandidate> candidates = new ArrayList<>();
        if (preferences == null) return candidates;

        if (rootDir.exists() && rootDir.isDirectory()) {
            EvoCodebaseDatasetSource src = new EvoCodebaseDatasetSource(rootDir.getAbsolutePath());
            long estimatedUsable = 50_000_000L; // 50 MB estimated codebase text
            candidates.add(new DataSourceCandidate(
                    "evo:codebase",
                    "EVO_CODEBASE",
                    "EVO System Codebase",
                    "Source code, tests, documentation, and OSGi configuration of the EVO platform.",
                    rootDir.toURI().toString(),
                    "repository",
                    src,
                    "en",
                    List.of("en", "java"),
                    List.of("programming", "ai", "evolution", "osgi"),
                    List.of("java", "tycho", "architecture"),
                    "code",
                    "java",
                    estimatedUsable,
                    estimatedUsable,
                    0.95,
                    0.9,
                    "proprietary",
                    "evo-system",
                    1.0,
                    0.0,
                    estimatedUsable
            ));
        }

        return candidates;
    }
}
