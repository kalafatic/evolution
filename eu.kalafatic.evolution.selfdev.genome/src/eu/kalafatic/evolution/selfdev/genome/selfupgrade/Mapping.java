package eu.kalafatic.evolution.selfdev.genome.selfupgrade;

public class Mapping {
    private final Insight insight;
    private final ProjectContext context;

    public Mapping(Insight insight, ProjectContext context) {
        this.insight = insight;
        this.context = context;
    }

    public Insight getInsight() {
        return insight;
    }

    public ProjectContext getContext() {
        return context;
    }
}
