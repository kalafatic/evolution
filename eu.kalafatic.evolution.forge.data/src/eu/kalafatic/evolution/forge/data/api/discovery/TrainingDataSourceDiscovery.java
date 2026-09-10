package eu.kalafatic.evolution.forge.data.api.discovery;

import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;

import java.util.List;

/**
 * Interface for discovering dataset candidates matching user preferences.
 */
public interface TrainingDataSourceDiscovery {

    /**
     * Discovers candidate sources matching preferences.
     *
     * @param preferences user requirements and soft domain goals
     * @return list of candidates
     */
    List<DataSourceCandidate> discover(TrainingDataPreferences preferences);
}
