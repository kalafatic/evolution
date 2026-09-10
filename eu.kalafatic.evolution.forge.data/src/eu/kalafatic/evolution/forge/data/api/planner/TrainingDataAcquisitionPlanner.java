package eu.kalafatic.evolution.forge.data.api.planner;

import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;

import java.util.List;

/**
 * Interface constructing structured acquisition plans from user preferences and discovered candidates.
 */
public interface TrainingDataAcquisitionPlanner {

    /**
     * Constructs a multi-source acquisition plan allocating target bytes across sources to avoid single-source domination.
     *
     * @param preferences user preferences
     * @param candidates ranked candidates
     * @return inspectable acquisition plan
     */
    TrainingDataAcquisitionPlan plan(TrainingDataPreferences preferences, List<DataSourceCandidate> candidates);
}
