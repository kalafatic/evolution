package eu.kalafatic.evolution.controller.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import eu.kalafatic.evolution.controller.execution.BackpressureController;
import eu.kalafatic.evolution.controller.execution.ExecutionBudget;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

public class RuntimeEventBus {
    private static final RuntimeEventBus INSTANCE = new RuntimeEventBus("global");
    private final List<RuntimeEventListener> listeners = new CopyOnWriteArrayList<>();
    private ExecutionBudget budget = ExecutionBudget.defaultProfile();
    private final String sessionId;

    public RuntimeEventBus(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * @deprecated Use session-scoped bus instead.
     */
    @Deprecated
    public static RuntimeEventBus getInstance() { return INSTANCE; }

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
        if (event.getType() == RuntimeEventType.EVALUATION_SIGNAL_CREATED) {
            BackpressureController.getInstance().recordSignal();
            if (BackpressureController.getInstance().shouldThrottleSignals(budget)) {
                System.err.println("[BUS] [" + sessionId + "] Throttling evaluation signal: " + event.getSource());
                return;
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
            eu.kalafatic.evolution.controller.trajectory.EvolutionRegistry registry = null;
            SessionContainer session = SessionManager.getInstance().getSession(sessionId);
            if (session != null) {
                registry = session.getEvolutionRegistry();
            } else {
                registry = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getInstance().getEvolutionRegistry();
            }

            if (registry != null) {
                registry.processEvent(event, "default-trajectory");
            }
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            System.err.println("[BUS] [" + sessionId + "] Skipping signal propagation - Registry not yet available: " + e.getMessage());
        }
    }
}
