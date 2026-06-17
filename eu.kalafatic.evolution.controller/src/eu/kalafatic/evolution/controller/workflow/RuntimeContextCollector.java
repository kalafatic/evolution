package eu.kalafatic.evolution.controller.workflow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects and maintains session-specific workflow state by listening to the RuntimeEventBus.
 * Serves as the bridge between raw system events and semantic guidance context.
 */
public class RuntimeContextCollector implements RuntimeEventListener {
    private final String sessionId;
    private final Map<String, Object> workflowState = new ConcurrentHashMap<>();

    public RuntimeContextCollector(String sessionId) {
        this.sessionId = sessionId;
        initializeDefaults();
    }

    private void initializeDefaults() {
        workflowState.put("model.exists", false);
        workflowState.put("model.trained", false);
        workflowState.put("model.exported", false);
        workflowState.put("training.active", false);
        workflowState.put("darwin.active", false);
        workflowState.put("chat.empty", true);
        workflowState.put("system.busy", false);
        workflowState.put("graph.empty", true);
    }

    @Override
    public void onEvent(RuntimeEvent event) {
        if (event == null) return;

        RuntimeEventType type = event.getType();
        workflowState.put("last.event", type.name());
        workflowState.put("last.payload", event.getPayload() != null ? event.getPayload().toString() : "");
        workflowState.put("last.timestamp", System.currentTimeMillis());

        switch (type) {
            case FORGE_SESSION_CREATED:
                workflowState.put("model.exists", true);
                workflowState.put("model.trained", false);
                break;
            case FORGE_MODEL_CHANGED:
                workflowState.put("model.exists", true);
                workflowState.put("model.trained", false);
                break;
            case FORGE_TRAINING_STARTED:
                workflowState.put("training.active", true);
                break;
            case FORGE_TRAINING_STOPPED:
                workflowState.put("training.active", false);
                workflowState.put("model.trained", true);
                break;
            case EXPORT_READY:
                workflowState.put("model.exported", true);
                break;
            case FLOW_STARTED:
                workflowState.put("system.busy", true);
                break;
            case FLOW_COMPLETED:
                workflowState.put("system.busy", false);
                break;
            case TASK_STARTED:
                workflowState.put("chat.empty", false);
                break;
            case MODE_CHANGED:
                if ("DARWIN".equals(event.getMetadata().get("mode"))) {
                    workflowState.put("darwin.active", true);
                }
                break;
            case VIEW_UPDATED:
                if ("ARCH_DISCOVERED".equals(event.getPayload())) {
                    workflowState.put("graph.empty", false);
                }
                break;
            default:
                break;
        }
    }

    public Map<String, Object> getWorkflowState() {
        return new ConcurrentHashMap<>(workflowState);
    }

    public String getSessionId() {
        return sessionId;
    }
}
