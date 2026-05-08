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
            } else if ("CLICK".equals(action)) {
                handleNodeClick(entityId);
            }
        } catch (Exception e) {
            context.log("[GRAPH] Execution failed: " + e.getMessage());
        }
    }

    private void handleNodeClick(String entityId) {
        // Logic to show details or logs for a specific node
        context.log("[GRAPH] Selected node: " + entityId);
    }
}
