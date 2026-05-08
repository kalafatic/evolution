package eu.kalafatic.evolution.controller.workflow;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class DeploymentManager {
    private final TaskContext context;

    public DeploymentManager(TaskContext context) {
        this.context = context;
    }

    public void setStatus(String target, String status) {
        RuntimeEvent event = new RuntimeEvent(
            RuntimeEventType.DEPLOYMENT_STATUS_CHANGED,
            context.getSessionId(),
            "DeploymentManager",
            status
        );
        event.getMetadata().put("target", target);
        RuntimeEventBus.getInstance().publish(event);
    }
}
