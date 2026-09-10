package eu.kalafatic.evolution.forge.data.api.evaluation;

import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;

/**
 * Interface evaluating acquired dataset metrics against user preferences.
 */
public interface TrainingDataPreferenceEvaluator {

    /**
     * Evaluates whether acquired dataset metrics satisfy user hard and soft preferences.
     *
     * @param preferences user preferences
     * @param stats acquisition stats
     * @return evaluation result
     */
    TrainingDataPreferenceEvaluation evaluate(TrainingDataPreferences preferences, DatasetSourceStats stats);
}
