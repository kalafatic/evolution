package eu.kalafatic.evolution.forge.data.impl.discovery;

import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.discovery.TrainingDataSourceRanker;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard implementation ranking candidate dataset sources against preference requirements.
 */
public class DefaultSourceRanker implements TrainingDataSourceRanker {

    @Override
    public List<DataSourceCandidate> rank(List<DataSourceCandidate> candidates, TrainingDataPreferences preferences) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<DataSourceCandidate> sorted = new ArrayList<>(candidates);
        sorted.sort((a, b) -> Double.compare(scoreCandidate(b, preferences), scoreCandidate(a, preferences)));
        return sorted;
    }

    private double scoreCandidate(DataSourceCandidate candidate, TrainingDataPreferences preferences) {
        if (candidate == null) return 0.0;
        if (preferences == null) return candidate.getRelevanceScore();

        double score = candidate.getRelevanceScore();

        // Language match check
        if (preferences.getRequiredLanguage().equalsIgnoreCase(candidate.getLanguage())) {
            score += 0.2;
        }

        // Domain coverage match
        for (String domain : preferences.getPrimaryDomains()) {
            for (String tag : candidate.getDomainTags()) {
                if (domain.equalsIgnoreCase(tag)) {
                    score += 0.3;
                    break;
                }
            }
        }

        return score;
    }
}
