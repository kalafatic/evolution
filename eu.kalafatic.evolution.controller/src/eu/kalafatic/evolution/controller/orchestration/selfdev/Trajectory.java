package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class Trajectory {
    public enum Trend { IMPROVING, SAME, WORSE }
    public enum Change { NEW, SAME, RESOLVED }

    public Trend buildTrend;
    public Trend testTrend;
    public Change failureChange;
}
