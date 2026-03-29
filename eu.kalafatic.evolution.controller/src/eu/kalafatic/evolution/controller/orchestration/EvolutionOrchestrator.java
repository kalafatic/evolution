package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.json.JSONObject;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;

/**
 * Core Orchestrator implementation that manages the task lifecycle and execution.
 */
public class EvolutionOrchestrator implements IOrchestrator {

    private static final int MAX_RETRIES = 3;
    private final PlannerAgent planner = new PlannerAgent();
    private final List<IAgent> availableAgents = new ArrayList<>();
    private final ReviewerAgent reviewer = new ReviewerAgent();

    public EvolutionOrchestrator() {
        // Initialize default agents
        availableAgents.add(new ArchitectAgent());
        availableAgents.add(new JavaDevAgent());
        availableAgents.add(new TesterAgent());
        availableAgents.add(new ReviewerAgent());
        availableAgents.add(new GeneralAgent());
    }

    @Override
    public String execute(String request, TaskContext context) throws Exception {
        try {
            context.log("Orchestrator: Starting request - " + request);

            // 1. Planning
            OrchestrationStatusManager.getInstance().updateAgentStatus("Planner", "Planning...");
            List<Task> tasks = planner.plan(request, context);
            OrchestrationStatusManager.getInstance().updateAgentStatus("Planner", "Finished");
            context.getOrchestrator().getTasks().clear();
            context.getOrchestrator().getTasks().addAll(tasks);

            // 2. Execution Loop
            int taskCount = tasks.size();
            String lastResult = "";
            for (int i = 0; i < taskCount; i++) {
                Task task = tasks.get(i);
                task.setStatus(TaskStatus.RUNNING);
                double progress = (double) i / taskCount;
                updateStatus(context, progress, "Executing: " + task.getName());

                boolean success = executeTaskWithRetries(task, context);

                if (!success) {
                    task.setStatus(TaskStatus.FAILED);
                    throw new Exception("Task failed after maximum retries: " + task.getName());
                }

                task.setStatus(TaskStatus.DONE);
                lastResult = task.getResponse();
                context.appendSharedMemory("Task [" + task.getName() + "] completed. Result: " + lastResult);
            }

            updateStatus(context, 1.0, "Completed");
            return lastResult != null && !lastResult.isEmpty() ? lastResult : "Orchestration successful.";
        } catch (Exception e) {
            context.log("Orchestrator Error: " + e.getMessage());
            throw e;
        } finally {
            OrchestrationStatusManager.getInstance().updateAgentStatus("Planner", "Idle");
            for (IAgent agent : availableAgents) {
                OrchestrationStatusManager.getInstance().updateAgentStatus(agent.getType(), "Idle");
            }
        }
    }

    private boolean executeTaskWithRetries(Task task, TaskContext context) throws Exception {
        IAgent agent = findAgentForTask(task, context);
        String lastFeedback = null;
        OrchestrationStatusManager.getInstance().updateAgentStatus(agent.getType(), "Executing: " + task.getName());

        for (int retry = 1; retry <= MAX_RETRIES; retry++) {
            context.log("Orchestrator: Executing " + task.getName() + " (Attempt " + retry + ")");

            try {
                // Execute action (either via tool or reasoning)
                String result = performAction(task, agent, context, lastFeedback);
                task.setResponse(result);

                // Evaluation
                JSONObject evaluation = reviewer.evaluate(result, task.getName(), context);
                if (evaluation.optBoolean("success", false)) {
                    task.setFeedback("Success: " + evaluation.optString("comment", "Task validated."));
                    return true;
                } else {
                    lastFeedback = evaluation.optString("feedback", "Task failed validation.");
                    task.setFeedback("Retry " + retry + ": " + lastFeedback);
                }
            } catch (Exception e) {
                lastFeedback = "Exception: " + e.getMessage();
                task.setFeedback("Retry " + retry + " Exception: " + e.getMessage());
            }
        }
        return false;
    }

    private String performAction(Task task, IAgent agent, TaskContext context, String lastFeedback) throws Exception {
        String taskType = task.getType();
        String taskName = task.getName();

        // Check if task maps directly to a tool
        if ("file".equalsIgnoreCase(taskType)) {
            FileTool fileTool = new FileTool();
            // JavaDev/Architect will generate content first
            String content = agent.process(taskName, context, lastFeedback);
            return fileTool.execute("WRITE " + taskName.replaceFirst("(?i)Write ", "").trim() + "\n" + content, context.getProjectRoot(), context);
        } else if ("maven".equalsIgnoreCase(taskType)) {
            MavenTool mavenTool = new MavenTool();
            return mavenTool.execute(taskName, context.getProjectRoot(), context);
        } else if ("git".equalsIgnoreCase(taskType)) {
            GitTool gitTool = new GitTool();
            return gitTool.execute(taskName, context.getProjectRoot(), context);
        } else if ("shell".equalsIgnoreCase(taskType)) {
            ShellTool shellTool = new ShellTool();
            return shellTool.execute(taskName, context.getProjectRoot(), context);
        }

        // Default to agent reasoning
        return agent.process(taskName, context, lastFeedback);
    }

    private IAgent findAgentForTask(Task task, TaskContext context) {
        String name = task.getName().toLowerCase();
        String type = task.getType().toLowerCase();

        // 1. Check for explicit agent type in task name
        for (IAgent agent : availableAgents) {
            if (name.contains(agent.getType().toLowerCase())) {
                return agent;
            }
        }

        // 2. Map task types to default agents
        if (type.contains("maven") || type.contains("test")) return availableAgents.stream().filter(a -> a instanceof TesterAgent).findFirst().orElse(availableAgents.get(2));
        if (type.contains("file") || type.contains("java")) return availableAgents.stream().filter(a -> a instanceof JavaDevAgent).findFirst().orElse(availableAgents.get(1));
        if (type.contains("arch") || type.contains("design")) return availableAgents.stream().filter(a -> a instanceof ArchitectAgent).findFirst().orElse(availableAgents.get(0));

        // Default to General Agent for reasoning or unknown tasks
        return availableAgents.stream().filter(a -> a instanceof GeneralAgent).findFirst().orElse(availableAgents.get(availableAgents.size() - 1));
    }

    private void updateStatus(TaskContext context, double progress, String message) {
        String id = context.getOrchestrator().getId();
        if (id != null) {
            OrchestrationStatusManager.getInstance().updateStatus(id, progress, message);
        }
    }
}
