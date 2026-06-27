package eu.kalafatic.evolution.controller.trajectory;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Continuous state field. Evolving semantic indicator of system state.
 */
public class Signal {
    public enum SignalType { SCALAR, CATEGORICAL, TREND, COMPOSITE }
    public enum Trend { INCREASING, STABILIZING, COLLAPSING, STABLE }

    private final String name;
    private final SignalType type;
    private Object value;
    private Trend trend = Trend.STABLE;
    private long lastUpdate;

    public Signal(String name, SignalType type, Object initialValue) {
        this.name = name;
        this.type = type;
        this.value = initialValue;
        this.lastUpdate = System.currentTimeMillis();
    }

    public String getName() { return name; }
    public SignalType getType() { return type; }
    public Object getValue() { return value; }
    public Trend getTrend() { return trend; }
    public long getLastUpdate() { return lastUpdate; }

    public synchronized void update(Object newValue) {
        this.value = newValue;
        this.lastUpdate = System.currentTimeMillis();
    }

    public void setTrend(Trend trend) {
        this.trend = trend;
    }
}
