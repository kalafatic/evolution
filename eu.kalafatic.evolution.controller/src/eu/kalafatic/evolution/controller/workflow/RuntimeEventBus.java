package eu.kalafatic.evolution.controller.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import eu.kalafatic.evolution.controller.execution.BackpressureController;
import eu.kalafatic.evolution.controller.execution.ExecutionBudget;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

public class RuntimeEventBus {
    private final List<RuntimeEventListener> listeners = new CopyOnWriteArrayList<>();
    private ExecutionBudget budget = ExecutionBudget.defaultProfile();
    private final String sessionId;

    private final java.util.concurrent.BlockingQueue<RuntimeEvent> eventBuffer = new java.util.concurrent.LinkedBlockingQueue<>();
    private final ScheduledExecutorService scheduler;
    private static final int THROTTLE_MS = 100;

    public RuntimeEventBus(String sessionId) {
        this.sessionId = sessionId;
        if (sessionId == null || sessionId.isEmpty()) {
            throw new IllegalArgumentException("RuntimeEventBus: sessionId cannot be null or empty.");
        }
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "EventBus-Throttler-" + sessionId);
            t.setDaemon(true);
            return t;
        });

        this.scheduler.scheduleAtFixedRate(this::flush, THROTTLE_MS, THROTTLE_MS, TimeUnit.MILLISECONDS);
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

        // Enforce session isolation and invariant checks
        eu.kalafatic.evolution.controller.kernel.RuntimeInvariant.checkSession(event.getSessionId(), "RuntimeEventBus.publish");

        if (!sessionId.equals(event.getSessionId())) {
            throw new IllegalArgumentException("RuntimeEventBus [" + sessionId + "]: Session mismatch. Event sessionId is " + event.getSessionId());
        }

        // Handle immediate system signals (Backpressure)
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
                // Violation: Falling back to global singleton is no longer allowed
                eu.kalafatic.evolution.controller.kernel.RuntimeInvariant.checkNoGlobalAccess("RuntimeEventBus.publish.backpressure");

                BackpressureController.getInstance().recordSignal();
                if (BackpressureController.getInstance().shouldThrottleSignals(budget)) {
                    System.err.println("[BUS] [" + sessionId + "] Throttling evaluation signal: " + event.getSource());
                    return;
                }
            }
        }

        // Buffer event for throttled delivery to UI/Listeners
        eventBuffer.add(event);

        // Critical kernel-side propagation should stay synchronous to ensure trajectory integrity
        propagateToRegistry(event);
    }

    private void flush() {
        if (eventBuffer.isEmpty()) return;

        List<RuntimeEvent> batch = new ArrayList<>();
        eventBuffer.drainTo(batch);

        for (RuntimeEvent event : batch) {
            for (RuntimeEventListener listener : listeners) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void propagateToRegistry(RuntimeEvent event) {
        try {
            // Enforce session integrity
            eu.kalafatic.evolution.controller.kernel.RuntimeInvariant.checkSession(sessionId, "RuntimeEventBus.propagateToRegistry");

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
