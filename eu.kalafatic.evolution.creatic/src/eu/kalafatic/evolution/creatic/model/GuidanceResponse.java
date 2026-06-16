package eu.kalafatic.evolution.creatic.model;

import java.util.ArrayList;
import java.util.List;

public class GuidanceResponse {
    private String summary;
    private final List<GuidanceAction> actions = new ArrayList<>();
    private final List<Insight> insights = new ArrayList<>();
    private final List<Warning> warnings = new ArrayList<>();
    private final List<Tip> tips = new ArrayList<>();

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<GuidanceAction> getActions() {
        return actions;
    }

    public List<Insight> getInsights() {
        return insights;
    }

    public List<Warning> getWarnings() {
        return warnings;
    }

    public List<Tip> getTips() {
        return tips;
    }
}
