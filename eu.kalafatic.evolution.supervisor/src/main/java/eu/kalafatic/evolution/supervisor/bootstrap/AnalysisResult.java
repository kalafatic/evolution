package eu.kalafatic.evolution.supervisor.bootstrap;

import java.util.ArrayList;
import java.util.List;

public class AnalysisResult {
    private final List<ImprovementCandidate> candidates = new ArrayList<>();
    private String summary;

    public void addCandidate(ImprovementCandidate candidate) {
        candidates.add(candidate);
    }

    public List<ImprovementCandidate> getCandidates() {
        return candidates;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
