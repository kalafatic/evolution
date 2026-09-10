package eu.kalafatic.evolution.forge.data.api.planner;

import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;

import java.util.List;
import java.util.Map;

/**
 * Explicit acquisition plan specifying target allocations per source to avoid source domination.
 */
public class TrainingDataAcquisitionPlan {

    private final TrainingDataPreferences preferences;
    private final List<DataSourceCandidate> primaryCandidates;
    private final List<DataSourceCandidate> fallbackCandidates;
    private final Map<String, Long> allocatedTargetBytesPerSource;

    public TrainingDataAcquisitionPlan(
            TrainingDataPreferences preferences,
            List<DataSourceCandidate> primaryCandidates,
            List<DataSourceCandidate> fallbackCandidates,
            Map<String, Long> allocatedTargetBytesPerSource) {
        this.preferences = preferences;
        this.primaryCandidates = primaryCandidates != null ? List.copyOf(primaryCandidates) : List.of();
        this.fallbackCandidates = fallbackCandidates != null ? List.copyOf(fallbackCandidates) : List.of();
        this.allocatedTargetBytesPerSource = allocatedTargetBytesPerSource != null ? Map.copyOf(allocatedTargetBytesPerSource) : Map.of();
    }

    public TrainingDataPreferences getPreferences() { return preferences; }
    public List<DataSourceCandidate> getPrimaryCandidates() { return primaryCandidates; }
    public List<DataSourceCandidate> getFallbackCandidates() { return fallbackCandidates; }
    public Map<String, Long> getAllocatedTargetBytesPerSource() { return allocatedTargetBytesPerSource; }
}
