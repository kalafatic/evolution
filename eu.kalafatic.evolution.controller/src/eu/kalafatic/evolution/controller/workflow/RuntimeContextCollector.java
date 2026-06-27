package eu.kalafatic.evolution.controller.workflow;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

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

        // Structured Workflow State
        workflowState.put("architecture.status", "PENDING");
        workflowState.put("dataset.status", "PENDING");
        workflowState.put("training.status", "IDLE");
        workflowState.put("evaluation.status", "PENDING");
        workflowState.put("snapshot.status", "NONE");
        workflowState.put("export.status", "PENDING");
        workflowState.put("deployment.status", "PENDING");
        workflowState.put("last.action.status", "SUCCESS");
        workflowState.put("last.error", "");
        workflowState.put("current.goal", "NONE");
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
                workflowState.put("architecture.status", "CREATED");
                break;
            case FORGE_MODEL_CHANGED:
                workflowState.put("model.exists", true);
                workflowState.put("model.trained", false);
                workflowState.put("architecture.status", "MODIFIED");
                break;
            case FORGE_TRAINING_CONFIGURED:
                workflowState.put("training.status", "CONFIGURED");
                break;
            case FORGE_TRAINING_STARTED:
                workflowState.put("training.active", true);
                workflowState.put("training.status", "RUNNING");
                break;
            case FORGE_TRAINING_STOPPED:
                workflowState.put("training.active", false);
                workflowState.put("model.trained", true);
                workflowState.put("training.status", "COMPLETED");
                break;
            case FORGE_TRAINING_FAILED:
                workflowState.put("training.active", false);
                workflowState.put("training.status", "FAILED");
                workflowState.put("last.action.status", "FAILED");
                workflowState.put("last.error", event.getPayload() != null ? event.getPayload().toString() : "Unknown training error");
                break;
            case FORGE_DATASET_IMPORTED:
                workflowState.put("dataset.status", "IMPORTED");
                break;
            case FORGE_SNAPSHOT_CREATED:
                workflowState.put("snapshot.status", "CREATED");
                break;
            case EVALUATION_COMPLETED:
                workflowState.put("evaluation.status", "COMPLETED");
                break;
            case EXPORT_READY:
                workflowState.put("model.exported", true);
                workflowState.put("export.status", "READY");
                break;
            case DEPLOYMENT_STARTED:
                workflowState.put("deployment.status", "IN_PROGRESS");
                break;
            case FLOW_STARTED:
                workflowState.put("system.busy", true);
                if (event.getPayload() != null) {
                    workflowState.put("current.goal", event.getPayload().toString());
                }
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
            case ITERATION_COMPLETED:
                if (workflowState.containsKey("darwin.active") && (Boolean)workflowState.get("darwin.active")) {
                    workflowState.put("last.action.status", "SUCCESS");
                }
                break;
            case VIEW_UPDATED:
                if ("ARCH_DISCOVERED".equals(event.getPayload())) {
                    workflowState.put("graph.empty", false);
                    workflowState.put("architecture.status", "DISCOVERED");
                }
                break;
            case COMMAND_FAILED:
            case TASK_FAILED:
                workflowState.put("last.action.status", "FAILED");
                workflowState.put("last.error", event.getPayload() != null ? event.getPayload().toString() : "Operation failed");
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
