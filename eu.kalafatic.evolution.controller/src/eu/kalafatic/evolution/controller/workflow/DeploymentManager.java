package eu.kalafatic.evolution.controller.workflow;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

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

        SessionContainer session = SessionManager.getInstance().getSession(context.getSessionId());
        if (session != null) {
            session.getEventBus().publish(event);
        } else {
            eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getOrCreateSession(context.getSessionId()).getEventBus().publish(event);
        }
    }
}
