package eu.kalafatic.evolution.controller.workflow;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevBootstrapController;
import org.json.JSONObject;

public class SupervisorManager {
    private final TaskContext context;
    private final SelfDevBootstrapController controller;

    public SupervisorManager(TaskContext context) {
        this.context = context;
        if (context != null) {
            this.controller = new SelfDevBootstrapController(context.getProjectRoot(), context.getOrchestrator());
        } else {
            this.controller = null;
        }
    }

    public void start() throws java.io.IOException {
        if (controller == null) throw new java.io.IOException("Cannot start supervisor: No active task context.");
        controller.startBootstrap();
        publishStatus("STARTING");
        getEventBus().publish(new RuntimeEvent(
            RuntimeEventType.MODE_CHANGED, context.getSessionId(), "SupervisorManager", "SELF_DEV_MODE"));
    }

    public void stop() {
        if (controller != null) controller.stopBootstrap();
        publishStatus("STOPPED");
    }

    public void updateStatus() {
        if (controller == null) return;
        JSONObject status = controller.getStatus();
        if (status != null) {
            String phase = status.optString("phase", "UNKNOWN");
            publishStatus(phase);
        }
    }

    private RuntimeEventBus getEventBus() {
        if (context != null) {
            return SessionManager.getInstance().getOrCreateSession(context.getSessionId()).getEventBus();
        }
        return RuntimeEventBus.getInstance();
    }

    private void publishStatus(String status) {
        String sid = context != null ? context.getSessionId() : "Default";
        getEventBus().publish(new RuntimeEvent(
            RuntimeEventType.SUPERVISOR_STATUS_CHANGED,
            sid,
            "SupervisorManager",
            status
        ));
    }
}
