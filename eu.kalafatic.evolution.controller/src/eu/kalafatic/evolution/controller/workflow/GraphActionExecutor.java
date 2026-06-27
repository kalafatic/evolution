package eu.kalafatic.evolution.controller.workflow;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.KernelFacade;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.SessionContext;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class GraphActionExecutor {
    private TaskContext context;
    private String sessionId;

    public GraphActionExecutor(TaskContext context) {
        this.context = context;
        if (context != null) this.sessionId = context.getSessionId();
    }

    public void setContext(TaskContext context) {
        this.context = context;
        if (context != null) this.sessionId = context.getSessionId();
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void execute(String entityId, String action) {
        if (context != null) context.log("[GRAPH] Executing action '" + action + "' on entity '" + entityId + "'");
        else System.out.println("[GRAPH] Executing action '" + action + "' on entity '" + entityId + "' (No Context)");

        try {
            if ("START_SUPERVISOR".equals(action)) {
                new SupervisorManager(context).start();
            } else if ("STOP_SUPERVISOR".equals(action)) {
                new SupervisorManager(context).stop();
            } else if ("RUN_EVO_LOOP".equals(action)) {
                new KernelFacade().execute("run evolution loop", context);
            } else if ("APPLY_PATCH".equals(action)) {
                new KernelFacade().execute("apply patch", context);
            } else if ("CONTINUE".equals(action)) {
                handleStepAction(entityId, WorkflowStatus.COMPLETED);
            } else if ("RETRY".equals(action)) {
                handleStepAction(entityId, WorkflowStatus.PENDING);
            } else if ("SKIP".equals(action)) {
                handleStepAction(entityId, WorkflowStatus.SKIPPED);
            } else if ("INSPECT".equals(action)) {
                handleInspect(entityId);
            } else if ("CLICK".equals(action)) {
                handleNodeClick(entityId);
            }
        } catch (Exception e) {
            context.log("[GRAPH] Execution failed: " + e.getMessage());
        }
    }

    private void handleStepAction(String entityId, WorkflowStatus status) {
	System.err.println("[GraphActionExecutor] Handling step action for entity: " + entityId + " with status: " + status);
        String sid = sessionId != null ? sessionId : (context != null ? context.getSessionId() : "Default");
        SessionContainer session = SessionManager.getInstance().getSession(sid);
        if (session == null) {
            throw new IllegalStateException("GraphActionExecutor: session is null for sessionId: " + sid);
        }
        GraphEntity entity = session.getWorkflowGraphManager().getEntity(sid, entityId);
        if (entity != null && entity.getMetadata().has("currentStepId")) {
            String stepId = entity.getMetadata().getString("currentStepId");
            if (session instanceof SessionContext) {
                ((SessionContext)session).getStepModeController().resumeStep(stepId, status);
            }
        }
    }

    private void handleInspect(String entityId) {
    	System.err.println("[GraphActionExecutor] Handling inspect action for entity: " + entityId);
        String sid = sessionId != null ? sessionId : (context != null ? context.getSessionId() : "Default");
        SessionContainer session = SessionManager.getInstance().getSession(sid);
        if (session == null) {
            throw new IllegalStateException("GraphActionExecutor: session is null for sessionId: " + sid);
        }
        GraphEntity entity = session.getWorkflowGraphManager().getEntity(sid, entityId);
        if (entity != null && entity.getMetadata().has("currentStepId")) {
            String stepId = entity.getMetadata().getString("currentStepId");
            WorkflowStep step = session.getWorkflowRegistry().getStep(stepId);
            if (step != null && context != null) {
                context.log("[INSPECT] Step: " + step.getDescription());
                context.log("[INSPECT] Type: " + step.getStepType());
                // In a real UI, this would open a dialog or a detail panel
            }
        }
    }

    private void handleNodeClick(String entityId) {
        // Logic to show details or logs for a specific node
        if (context != null) context.log("[GRAPH] Selected node: " + entityId);
        else System.out.println("[GRAPH] Selected node: " + entityId + " (No Context)");
    }
}
