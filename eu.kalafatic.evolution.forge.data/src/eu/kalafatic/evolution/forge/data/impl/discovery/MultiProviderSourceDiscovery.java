package eu.kalafatic.evolution.forge.data.impl.discovery;

import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.discovery.TrainingDataSourceDiscovery;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite discovery provider aggregating candidates across HuggingFace, Local Filesystem, EVO Codebase, Web, and Public Domain providers.
 */
public class MultiProviderSourceDiscovery implements TrainingDataSourceDiscovery {

    private final List<TrainingDataSourceDiscovery> providers;

    public MultiProviderSourceDiscovery() {
        this(List.of(
                new HuggingFaceSourceDiscovery(),
                new LocalSourceDiscoveryProvider(),
                new EvoCodebaseSourceDiscoveryProvider(),
                new WebSourceDiscoveryProvider(),
                new PublicDomainSourceDiscoveryProvider()
        ));
    }

    public MultiProviderSourceDiscovery(List<TrainingDataSourceDiscovery> providers) {
        this.providers = providers != null ? List.copyOf(providers) : List.of();
    }

    @Override
    public List<DataSourceCandidate> discover(TrainingDataPreferences preferences) {
        List<DataSourceCandidate> allCandidates = new ArrayList<>();
        if (preferences == null) return allCandidates;

        for (TrainingDataSourceDiscovery provider : providers) {
            try {
                List<DataSourceCandidate> discovered = provider.discover(preferences);
                if (discovered != null) {
                    allCandidates.addAll(discovered);
                }
            } catch (Exception ex) {
                System.err.println("[MULTI-PROVIDER DISCOVERY] Error discovering candidates from provider " + provider.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }
        return allCandidates;
    }
}
