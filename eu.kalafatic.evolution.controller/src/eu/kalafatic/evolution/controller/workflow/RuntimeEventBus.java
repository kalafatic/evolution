package eu.kalafatic.evolution.controller.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import eu.kalafatic.evolution.controller.execution.BackpressureController;
import eu.kalafatic.evolution.controller.execution.ExecutionBudget;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

public class RuntimeEventBus {
    private final List<RuntimeEventListener> listeners = new CopyOnWriteArrayList<>();
    private ExecutionBudget budget = ExecutionBudget.defaultProfile();
    private final String sessionId;

    public RuntimeEventBus(String sessionId) {
        this.sessionId = sessionId;
        if (sessionId == null || sessionId.isEmpty()) {
            throw new IllegalArgumentException("RuntimeEventBus: sessionId cannot be null or empty.");
        }
    }

    public void setBudget(ExecutionBudget budget) {
        this.budget = budget;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void subscribe(RuntimeEventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(RuntimeEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(RuntimeEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("RuntimeEventBus [" + sessionId + "]: Cannot publish null event.");
        }
        if (!sessionId.equals(event.getSessionId())) {
            throw new IllegalArgumentException("RuntimeEventBus [" + sessionId + "]: Session mismatch. Event sessionId is " + event.getSessionId());
        }

        if (event.getType() == RuntimeEventType.EVALUATION_SIGNAL_CREATED) {
            SessionContainer session = SessionManager.getInstance().getSession(sessionId);
            if (session != null) {
                BackpressureController bpc = session.getBackpressureController();
                bpc.recordSignal();
                if (bpc.shouldThrottleSignals(budget)) {
                    System.err.println("[BUS] [" + sessionId + "] Throttling evaluation signal: " + event.getSource());
                    return;
                }
            } else {
                BackpressureController.getInstance().recordSignal();
                if (BackpressureController.getInstance().shouldThrottleSignals(budget)) {
                    System.err.println("[BUS] [" + sessionId + "] Throttling evaluation signal: " + event.getSource());
                    return;
                }
            }
        }

        for (RuntimeEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        propagateToRegistry(event);
    }

    private void propagateToRegistry(RuntimeEvent event) {
        try {
            SessionContainer session = SessionManager.getInstance().getSession(sessionId);
            if (session != null) {
                eu.kalafatic.evolution.controller.trajectory.EvolutionRegistry registry = session.getEvolutionRegistry();
                if (registry != null) {
                    registry.processEvent(event, "default-trajectory");
                }
            } else {
                System.err.println("[BUS] [" + sessionId + "] Warning: Session not found in SessionManager. Cannot propagate event to registry.");
            }
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            System.err.println("[BUS] [" + sessionId + "] Skipping signal propagation - Registry not yet available: " + e.getMessage());
        }
    }
}
