package eu.kalafatic.evolution.forge.data.api.service;

import eu.kalafatic.evolution.forge.data.api.planner.TrainingDataAcquisitionPlan;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Coherent request configuration for training data acquisition supporting explicit sources, preferences, or acquisition plans.
 */
public class TrainingDataAcquisitionRequest {

    private final List<DatasetSource> sources = new ArrayList<>();
    private long minimumUsableBytes = 0;
    private double validationSplitRatio = 0.02;
    private TrainingDataPreferences preferences;
    private TrainingDataAcquisitionPlan plan;

    public TrainingDataAcquisitionRequest() {}

    public TrainingDataAcquisitionRequest(List<DatasetSource> sources, long minimumUsableBytes, double validationSplitRatio) {
        if (sources != null) this.sources.addAll(sources);
        this.minimumUsableBytes = minimumUsableBytes;
        this.validationSplitRatio = validationSplitRatio;
    }

    public List<DatasetSource> getSources() {
        return sources;
    }

    public TrainingDataAcquisitionRequest addSource(DatasetSource source) {
        if (source != null) {
            this.sources.add(source);
        }
        return this;
    }

    public long getMinimumUsableBytes() {
        return minimumUsableBytes;
    }

    public TrainingDataAcquisitionRequest setMinimumUsableBytes(long minimumUsableBytes) {
        this.minimumUsableBytes = minimumUsableBytes;
        return this;
    }

    public double getValidationSplitRatio() {
        return validationSplitRatio;
    }

    public TrainingDataAcquisitionRequest setValidationSplitRatio(double validationSplitRatio) {
        this.validationSplitRatio = validationSplitRatio;
        return this;
    }

    public TrainingDataPreferences getPreferences() {
        return preferences;
    }

    public TrainingDataAcquisitionRequest setPreferences(TrainingDataPreferences preferences) {
        this.preferences = preferences;
        if (preferences != null) {
            this.minimumUsableBytes = preferences.getMinimumUsableBytes();
            this.validationSplitRatio = preferences.getValidationSplitRatio();
        }
        return this;
    }

    public TrainingDataAcquisitionPlan getPlan() {
        return plan;
    }

    public TrainingDataAcquisitionRequest setPlan(TrainingDataAcquisitionPlan plan) {
        this.plan = plan;
        if (plan != null && plan.getPreferences() != null) {
            setPreferences(plan.getPreferences());
        }
        return this;
    }
}
