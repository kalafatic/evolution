package eu.kalafatic.evolution.forge.data.api.discovery;

import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;

import java.util.List;

/**
 * Interface for scoring and ranking candidate dataset sources against user preferences.
 */
public interface TrainingDataSourceRanker {

    /**
     * Ranks candidates in order of relevance and compatibility with hard and soft preferences.
     *
     * @param candidates candidate list
     * @param preferences user preferences
     * @return sorted candidate list
     */
    List<DataSourceCandidate> rank(List<DataSourceCandidate> candidates, TrainingDataPreferences preferences);
}
