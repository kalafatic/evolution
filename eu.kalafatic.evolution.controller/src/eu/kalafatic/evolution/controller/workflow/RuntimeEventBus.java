package eu.kalafatic.evolution.controller.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import eu.kalafatic.evolution.controller.orchestration.scheduling.BackpressureController;
import eu.kalafatic.evolution.controller.orchestration.scheduling.ExecutionBudget;

public class RuntimeEventBus {
    private static final RuntimeEventBus INSTANCE = new RuntimeEventBus();
    private final List<RuntimeEventListener> listeners = new CopyOnWriteArrayList<>();
    private ExecutionBudget budget = ExecutionBudget.defaultProfile();

    private RuntimeEventBus() {}

    public static RuntimeEventBus getInstance() { return INSTANCE; }

    public void setBudget(ExecutionBudget budget) {
        this.budget = budget;
    }

    public void subscribe(RuntimeEventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(RuntimeEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(RuntimeEvent event) {
        // Backpressure check for evaluation signals
        if (event.getType() == RuntimeEventType.EVALUATION_SIGNAL_CREATED) {
            BackpressureController.getInstance().recordSignal();
            if (BackpressureController.getInstance().shouldThrottleSignals(budget)) {
                // Drop low-value signals or log throttling
                System.err.println("[BUS] Throttling evaluation signal: " + event.getSource());
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
        // Evolutionary Signal Propagation: Automatically update continuous system signals
        eu.kalafatic.evolution.controller.orchestration.evolution.EvolutionRegistry registry =
            eu.kalafatic.evolution.controller.manager.ProjectModelManager.getInstance().getEvolutionRegistry();
        if (registry != null) {
            registry.processEvent(event, "default-trajectory");
        }
    }
}
