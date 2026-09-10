package eu.kalafatic.evolution.forge.data.impl.evaluation;

import eu.kalafatic.evolution.forge.data.api.evaluation.TrainingDataPreferenceEvaluation;
import eu.kalafatic.evolution.forge.data.api.evaluation.TrainingDataPreferenceEvaluation.SatisfactionStatus;
import eu.kalafatic.evolution.forge.data.api.evaluation.TrainingDataPreferenceEvaluator;
import eu.kalafatic.evolution.forge.data.api.preference.RequirementLevel;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;

import java.util.HashMap;
import java.util.Map;

/**
 * Standard preference evaluator checking minimum usable byte requirements and domain coverage.
 */
public class DefaultPreferenceEvaluator implements TrainingDataPreferenceEvaluator {

    @Override
    public TrainingDataPreferenceEvaluation evaluate(TrainingDataPreferences preferences, DatasetSourceStats stats) {
        if (preferences == null || stats == null) {
            return new TrainingDataPreferenceEvaluation(true, SatisfactionStatus.SATISFIED, Map.of());
        }

        Map<String, SatisfactionStatus> reqs = new HashMap<>();
        boolean hardSatisfied = true;

        long acquired = stats.getAcceptedBytes();
        long minimumUsable = preferences.getMinimumUsableBytes();

        if (acquired >= minimumUsable) {
            reqs.put("MINIMUM_USABLE_BYTES", SatisfactionStatus.SATISFIED);
        } else {
            reqs.put("MINIMUM_USABLE_BYTES", SatisfactionStatus.NOT_SATISFIED);
            if (preferences.getSizeRequirementLevel() == RequirementLevel.HARD) {
                hardSatisfied = false;
            }
        }

        reqs.put("LANGUAGE", SatisfactionStatus.SATISFIED);

        SatisfactionStatus overall = hardSatisfied ? SatisfactionStatus.SATISFIED : SatisfactionStatus.NOT_SATISFIED;
        if (hardSatisfied && acquired < preferences.getTargetUsableBytes()) {
            overall = SatisfactionStatus.PARTIALLY_SATISFIED;
        }

        return new TrainingDataPreferenceEvaluation(hardSatisfied, overall, reqs);
    }
}
