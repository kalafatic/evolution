package eu.kalafatic.evolution.view.projection;

import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages runtime projections and notifies UI observers.
 */
public class ProjectionService implements RuntimeEventListener {
    private static final ProjectionService INSTANCE = new ProjectionService();
    private final Map<String, RuntimeProjection> projections = new ConcurrentHashMap<>();
    private final List<Consumer<RuntimeProjection>> observers = new CopyOnWriteArrayList<>();

    private ProjectionService() {
        // In a real implementation, we would subscribe to the session-scoped buses.
        // For now, we subscribe to the global one to maintain legacy compatibility while
        // providing the new projection-based API.
        RuntimeEventBus.getInstance().subscribe(this);
    }

    public static ProjectionService getInstance() {
        return INSTANCE;
    }

    public RuntimeProjection getProjection(String sessionId) {
        return projections.computeIfAbsent(sessionId, RuntimeProjection::new);
    }

    public void subscribe(Consumer<RuntimeProjection> observer) {
        observers.add(observer);
    }

    public void unsubscribe(Consumer<RuntimeProjection> observer) {
        observers.remove(observer);
    }

    @Override
    public void onEvent(RuntimeEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId == null) return;

        RuntimeProjection updated = projections.compute(sessionId, (id, current) -> {
            if (current == null) current = new RuntimeProjection(id);
            return current.withEvent(event);
        });

        notifyObservers(updated);
    }

    private void notifyObservers(RuntimeProjection projection) {
        for (Consumer<RuntimeProjection> observer : observers) {
            try {
                observer.accept(projection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
