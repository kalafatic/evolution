package eu.kalafatic.evolution.controller.workflow;

import java.io.File;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevBootstrapController;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SupervisorClient;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

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

    public SupervisorManager(File projectRoot, Orchestrator orchestrator) {
        this.context = null;
        this.controller = new SelfDevBootstrapController(projectRoot, orchestrator);
    }

    public void start() throws java.io.IOException {
        if (controller == null) throw new java.io.IOException("Cannot start supervisor: No active task context or controller.");
        controller.startBootstrap();
        publishStatus("STARTING");
        if (context != null) {
            getEventBus().publish(new RuntimeEvent(
                RuntimeEventType.MODE_CHANGED, context.getSessionId(), "SupervisorManager", "SELF_DEV_MODE"));
        }
    }

    public void stop() {
        if (controller != null) controller.stopBootstrap();
        publishStatus("STOPPED");
    }

    public boolean ping() {
        SupervisorClient client = new SupervisorClient();
        return client.ping();
    }

    public boolean isAlive() {
        if (controller != null) {
            return controller.isRunning() || ping();
        }
        return ping();
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
        if (context == null) {
            return SessionManager.getInstance().getOrCreateSession("Default").getEventBus();
        }
        return SessionManager.getInstance().getOrCreateSession(context.getSessionId()).getEventBus();
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

    public SelfDevBootstrapController getController() {
        return controller;
    }
}
