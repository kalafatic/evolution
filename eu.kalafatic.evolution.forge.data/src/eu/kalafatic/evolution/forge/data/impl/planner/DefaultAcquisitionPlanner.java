package eu.kalafatic.evolution.forge.data.impl.planner;

import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.planner.TrainingDataAcquisitionPlan;
import eu.kalafatic.evolution.forge.data.api.planner.TrainingDataAcquisitionPlanner;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard acquisition planner distributing target usable bytes proportionally across domain candidates to avoid source domination.
 */
public class DefaultAcquisitionPlanner implements TrainingDataAcquisitionPlanner {

    @Override
    public TrainingDataAcquisitionPlan plan(TrainingDataPreferences preferences, List<DataSourceCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new TrainingDataAcquisitionPlan(preferences, List.of(), List.of(), Map.of());
        }

        long totalTargetBytes = preferences != null ? preferences.getMinimumUsableBytes() : 50_000_000L;
        int count = candidates.size();
        long sharePerSource = Math.max(1, totalTargetBytes / count);

        List<DataSourceCandidate> primary = new ArrayList<>();
        List<DataSourceCandidate> fallbacks = new ArrayList<>();
        Map<String, Long> allocations = new HashMap<>();

        for (int i = 0; i < count; i++) {
            DataSourceCandidate cand = candidates.get(i);
            if (i < 3) {
                primary.add(cand);
                allocations.put(cand.getDatasetId(), sharePerSource);
            } else {
                fallbacks.add(cand);
                allocations.put(cand.getDatasetId(), sharePerSource);
            }
        }

        return new TrainingDataAcquisitionPlan(preferences, primary, fallbacks, allocations);
    }
}
