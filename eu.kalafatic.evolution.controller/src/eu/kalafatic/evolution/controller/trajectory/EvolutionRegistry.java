package eu.kalafatic.evolution.controller.trajectory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

/**
 * Manages active signals and trajectories.
 * Orchestrates signal updates from events and trajectory evolution.
 */
public class EvolutionRegistry {
    private final Map<String, Signal> signals = new ConcurrentHashMap<>();
    private final Map<String, Trajectory> trajectories = new ConcurrentHashMap<>();

    public EvolutionRegistry() {
        initializeDefaultSignals();
    }

    private void initializeDefaultSignals() {
        signals.put("proposalFitness", new Signal("proposalFitness", Signal.SignalType.SCALAR, 0.5));
        signals.put("explorationPressure", new Signal("explorationPressure", Signal.SignalType.CATEGORICAL, "MEDIUM"));
        signals.put("branchNovelty", new Signal("branchNovelty", Signal.SignalType.CATEGORICAL, "LOW"));
        signals.put("userConfidence", new Signal("userConfidence", Signal.SignalType.CATEGORICAL, "MEDIUM"));
        signals.put("convergenceTrend", new Signal("convergenceTrend", Signal.SignalType.TREND, Signal.Trend.STABLE));
    }

    public Signal getSignal(String name) {
        return signals.get(name);
    }

    public Trajectory getOrCreateTrajectory(String id, String goal) {
        return trajectories.computeIfAbsent(id, k -> new Trajectory(id, goal));
    }

    public void processEvent(RuntimeEvent event, String trajectoryId) {
        // Update signals based on event
        updateSignalsFromEvent(event);

        // Evolve trajectory
        Trajectory trajectory = trajectories.get(trajectoryId);
        if (trajectory != null) {
            evolveTrajectory(trajectory);
        }
    }

    private void updateSignalsFromEvent(RuntimeEvent event) {
        RuntimeEventType type = event.getType();

        if (type == RuntimeEventType.TASK_COMPLETED) {
            adjustSignal("proposalFitness", 0.05);
        } else if (type == RuntimeEventType.TASK_FAILED) {
            adjustSignal("proposalFitness", -0.1);
            signals.get("explorationPressure").update("HIGH");
        } else if (type == RuntimeEventType.MUTATING) {
            signals.get("convergenceTrend").update(Signal.Trend.STABILIZING);
        } else if (type == RuntimeEventType.USER_INTERACTION_RECEIVED) {
            if ("Rejected".equals(event.getPayload())) {
                adjustSignal("userConfidence", -0.2);
                signals.get("explorationPressure").update("HIGH");
            } else {
                adjustSignal("userConfidence", 0.1);
            }
        }
    }

    private void adjustSignal(String name, double delta) {
        Signal s = signals.get(name);
        if (s != null && s.getValue() instanceof Double) {
            double current = (Double) s.getValue();
            s.update(Math.max(0.0, Math.min(1.0, current + delta)));
        }
    }

    private void evolveTrajectory(Trajectory trajectory) {
        Signal fitness = signals.get("proposalFitness");

        if (fitness != null && fitness.getValue() instanceof Double) {
            double f = (Double) fitness.getValue();
            trajectory.setStabilityScore(f);

            if (f < 0.3) {
                trajectory.setPhase(Trajectory.Phase.COLLAPSE);
            } else if (f > 0.8) {
                trajectory.setPhase(Trajectory.Phase.CONVERGENCE);
            }
        }

        // Record signal history in trajectory
        signals.forEach((name, signal) -> trajectory.recordSignal(name, signal.getValue()));
    }

    public Map<String, Signal> getSignals() {
        return signals;
    }
}
