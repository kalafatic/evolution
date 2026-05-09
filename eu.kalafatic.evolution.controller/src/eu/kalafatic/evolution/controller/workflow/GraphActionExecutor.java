package eu.kalafatic.evolution.controller.workflow;

import eu.kalafatic.evolution.controller.orchestration.KernelFacade;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class GraphActionExecutor {
    private final TaskContext context;

    public GraphActionExecutor(TaskContext context) {
        this.context = context;
    }

    public void execute(String entityId, String action) {
        context.log("[GRAPH] Executing action '" + action + "' on entity '" + entityId + "'");

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
        System.out.println("[GraphActionExecutor] Resuming step for entity: " + entityId + " with status: " + status);
        GraphEntity entity = WorkflowGraphManager.getInstance(context.getSessionId()).getSessionGraph(context.getSessionId()).getEntities().get(entityId);
        if (entity != null && entity.getMetadata().has("currentStepId")) {
            String stepId = entity.getMetadata().getString("currentStepId");
            StepModeController.getInstance().resumeStep(stepId, status);
        }
    }

    private void handleInspect(String entityId) {
        System.out.println("[GraphActionExecutor] Inspecting entity: " + entityId);
        GraphEntity entity = WorkflowGraphManager.getInstance(context.getSessionId()).getSessionGraph(context.getSessionId()).getEntities().get(entityId);
        if (entity != null && entity.getMetadata().has("currentStepId")) {
            String stepId = entity.getMetadata().getString("currentStepId");
            WorkflowStep step = WorkflowStepRegistry.getInstance().getStep(stepId);
            if (step != null) {
                context.log("[INSPECT] Step: " + step.getDescription());
                context.log("[INSPECT] Type: " + step.getStepType());
            }
        }
    }

    private void handleNodeClick(String entityId) {
        // Logic to show details or logs for a specific node
        context.log("[GRAPH] Selected node: " + entityId);
    }
}
