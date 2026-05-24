package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides isolated execution state for parallel Darwin variants.
 */
public class VariantExecutionContext {
    private final String variantId;
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();
    private final List<RuntimeEvent> localEvents = new ArrayList<>();
    private final List<eu.kalafatic.evolution.model.orchestration.Task> tasks = new ArrayList<>();

    public VariantExecutionContext(String variantId) {
        this.variantId = variantId;
    }

    public String getVariantId() {
        return variantId;
    }

    public void recordEvent(RuntimeEvent event) {
        localEvents.add(event);
    }

    public List<RuntimeEvent> getLocalEvents() {
        return localEvents;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public List<eu.kalafatic.evolution.model.orchestration.Task> getTasks() {
        return tasks;
    }
}
