package eu.kalafatic.evolution.controller.workflow;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevBootstrapController;
import org.json.JSONObject;

public class SupervisorManager {
    private final TaskContext context;
    private final SelfDevBootstrapController controller;

    public SupervisorManager(TaskContext context) {
        this.context = context;
        this.controller = new SelfDevBootstrapController(context.getProjectRoot(), context.getOrchestrator());
    }

    public void start() throws java.io.IOException {
        controller.startBootstrap();
        publishStatus("STARTING");
        RuntimeEventBus.getInstance().publish(new RuntimeEvent(
            RuntimeEventType.MODE_CHANGED, context.getSessionId(), "SupervisorManager", "SELF_DEV_MODE"));
    }

    public void stop() {
        controller.stopBootstrap();
        publishStatus("STOPPED");
    }

    public void updateStatus() {
        JSONObject status = controller.getStatus();
        if (status != null) {
            String phase = status.optString("phase", "UNKNOWN");
            publishStatus(phase);
        }
    }

    private void publishStatus(String status) {
        RuntimeEventBus.getInstance().publish(new RuntimeEvent(
            RuntimeEventType.SUPERVISOR_STATUS_CHANGED,
            context.getSessionId(),
            "SupervisorManager",
            status
        ));
    }
}
