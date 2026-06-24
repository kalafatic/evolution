package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Task;

/**
 * Dumb planner that strictly converts validated architectural variants into executable action graphs.
 * No synthetic repair, no reasoning, no "intelligence".
 */
public class ImplementationPlanner {

    /**
     * Converts a BranchVariant's actions into EMF Task objects.
     * @param variant The architectural variant.
     * @return List of Task objects.
     */
    public List<Task> planTasks(BranchVariant variant) {
        List<Task> tasks = new ArrayList<>();
        if (variant == null || variant.getActions() == null) return tasks;

        OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;

        for (BranchVariant.Action action : variant.getActions()) {
            Task task = factory.createTask();
            task.setId("sd-task-" + System.currentTimeMillis() + "-" + tasks.size());

            String op = action.getOperation();
            String target = action.getTarget();
            String domain = action.getDomain();

            task.setName(op + " " + target);
            task.setDescription(action.getDescription());
            task.setRationale("Darwin Strategy: " + variant.getStrategy());
            task.setPriority(1);

            String type = mapDomainToTaskType(domain, op);
            task.setType(type);

            if (action.getImplementation() != null && !action.getImplementation().isEmpty()) {
                task.setResponse(action.getImplementation());
            }

            tasks.add(task);
        }

        return tasks;
    }

    private String mapDomainToTaskType(String domain, String op) {
        String lowerDomain = domain != null ? domain.toLowerCase() : "";
        String upperOp = op != null ? op.toUpperCase() : "";

        if ("file".equalsIgnoreCase(lowerDomain) || "class".equalsIgnoreCase(lowerDomain) || "java".equalsIgnoreCase(lowerDomain)) {
            if ("DELETE".equals(upperOp) || "REMOVE".equals(upperOp) || "MKDIR".equals(upperOp)) {
                return "shell";
            }
            return "file";
        }

        if ("build".equalsIgnoreCase(lowerDomain)) return "maven";
        if ("structure".equalsIgnoreCase(lowerDomain)) return "structure";
        if ("test".equalsIgnoreCase(lowerDomain)) return "maven";
        if ("git".equalsIgnoreCase(lowerDomain)) return "git";

        return "llm";
    }

    /**
     * Final structural validation before execution.
     * @param variant The variant to validate.
     * @return true if valid, false otherwise.
     */
    public boolean validate(BranchVariant variant) {
        if (variant == null) return false;
        if (variant.getStrategy() == null || variant.getStrategy().isEmpty()) return false;
        if (variant.getSemanticAnchor() == null || variant.getSemanticAnchor().isEmpty()) return false;
        if (variant.getActions() == null || variant.getActions().isEmpty()) return false;

        for (BranchVariant.Action action : variant.getActions()) {
            if (action.getDomain() == null || action.getDomain().isEmpty()) return false;
            if (action.getOperation() == null || action.getOperation().isEmpty()) return false;
            if (action.getTarget() == null || action.getTarget().isEmpty()) return false;
            if (action.getDescription() == null || action.getDescription().isEmpty()) return false;

            if ("WRITE".equalsIgnoreCase(action.getOperation())) {
                if (action.getImplementation() == null || action.getImplementation().isEmpty()) return false;
                if (".".equals(action.getTarget()) || "workspace".equalsIgnoreCase(action.getTarget())) return false;
            }
        }

        return true;
    }
}
