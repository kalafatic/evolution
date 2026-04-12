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
        availableAgents.add(new TerminalAgent());
        availableAgents.add(new FileAgent());
        availableAgents.add(new MavenAgent());
        availableAgents.add(new GitAgent());
        availableAgents.add(new StructureAgent());
        availableAgents.add(new WebSearchAgent());
        availableAgents.add(new QualityAgent());
        availableAgents.add(new ObservabilityAgent());
    }

    @Override
    public String executeTask(Task task, TaskContext context) throws Exception {
        context.log("Evo: Executing single task: " + task.getName());
        boolean success = executeTaskWithRetries(task, context);
        if (!success) {
            task.setStatus(TaskStatus.FAILED);
            throw new Exception("Task failed after maximum retries: " + task.getName());
        }
        task.setStatus(TaskStatus.DONE);
        return task.getResponse();
    }

    @Override
    public String execute(String request, TaskContext context) throws Exception {
        try {
            context.log("Evo: Starting request - " + request);
            context.appendSharedMemory("Initial user request: " + request);

            // 1. Planning
            OrchestrationStatusManager.getInstance().updateAgentStatus("Planner", "Planning...");
            List<Task> originalPlannedTasks = planner.plan(request, context);
            OrchestrationStatusManager.getInstance().updateAgentStatus("Planner", "Finished");
            context.getOrchestrator().getTasks().clear();
            context.getOrchestrator().getTasks().addAll(originalPlannedTasks);

            // Determine if Plan Approval is needed based on task severity
            boolean requiresPlanApproval = false;
            for (Task t : originalPlannedTasks) {
                if (t.isApprovalRequired()) {
                    requiresPlanApproval = true;
                    break;
                }
            }

            if (requiresPlanApproval) {
                // Pause for Plan Approval
                context.log("Evo: Plan generated. Waiting for user review and approval...");
                Boolean planApproved = context.requestApproval(TaskContext.PLAN_APPROVAL_MESSAGE).get();
                if (planApproved == null || !planApproved) {
                    context.log("Evo: Plan rejected by user.");
                    throw new Exception("Orchestration plan rejected by user.");
                }
                context.log("Evo: Plan approved. Starting execution...");
            } else {
                context.log("Evo: Low severity plan generated. Skipping manual approval and starting execution...");
            }

            // Reload tasks from model in case the user modified them during approval
            List<Task> tasks = new ArrayList<>(context.getOrchestrator().getTasks());

            // 2. Execution Loop
            int taskCount = tasks.size();
            String lastResult = "";
            for (int i = 0; i < taskCount; i++) {
                context.checkPause();
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                Task task = tasks.get(i);

                // Check for User Approval
                if (task.isApprovalRequired() || "approval".equalsIgnoreCase(task.getType())) {
                    task.setStatus(TaskStatus.WAITING_FOR_APPROVAL);
                    context.log("Evo: Waiting for user approval for task: " + task.getName());
                    Boolean approved = context.requestApproval("Approve task: " + task.getName() + "?").get();
                    if (approved == null || !approved) {
                        task.setStatus(TaskStatus.FAILED);
                        task.setFeedback("Rejected by user.");
                        throw new Exception("Task rejected by user: " + task.getName());
                    }
                }

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

                // Handle Looping logic
                String loopToId = task.getLoopToTaskId();
                if (loopToId != null && !loopToId.isEmpty() && !"none".equalsIgnoreCase(loopToId)) {
                    // Decide if we should loop. For now, if the agent or a specific 'loop' task type suggests it.
                    // If it's a 'loop' task, we check its response or feedback to see if it should continue.
                    // Simplified: if task type is 'loop' and response contains 'CONTINUE', or if it's just any task with a loopToId
                    // and we haven't exceeded some internal loop limit (safety).

                    int loopTargetIndex = -1;
                    for (int j = 0; j < tasks.size(); j++) {
                        if (loopToId.equals(tasks.get(j).getId())) {
                            loopTargetIndex = j;
                            break;
                        }
                    }

                    if (loopTargetIndex != -1) {
                        context.log("Evo: Looping back to task ID: " + loopToId);
                        i = loopTargetIndex - 1; // -1 because the for loop will increment i
                    }
                }
            }

            updateStatus(context, 1.0, "Completed");
            return lastResult != null && !lastResult.isEmpty() ? lastResult : "Orchestration successful.";
        } catch (Exception e) {
            context.log("Evo Error: " + e.getMessage());
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
            context.checkPause();
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            context.log("Evo: Executing " + task.getName() + " (Attempt " + retry + ")");

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

            if (retry == MAX_RETRIES) {
                context.log("Evo: Task " + task.getName() + " failed after " + MAX_RETRIES + " retries.");
                try {
                    String guidance = context.requestInput("Task [" + task.getName() + "] failed consistently. Guidance? (retry/skip/hint)").get();
                    if (guidance != null) {
                        if ("retry".equalsIgnoreCase(guidance.trim())) {
                            retry = 0; // Reset loop to try again
                            lastFeedback = null;
                            context.log("Evo: User requested retry for task: " + task.getName());
                        } else if ("skip".equalsIgnoreCase(guidance.trim())) {
                            context.log("Evo: User requested to skip task: " + task.getName());
                            task.setFeedback("Skipped by user.");
                            return true;
                        } else {
                            // Treat as hint for one more attempt
                            lastFeedback = "User Hint: " + guidance;
                            context.log("Evo: Applying user hint for one last attempt: " + guidance);
                            retry = MAX_RETRIES - 1;
                        }
                    }
                } catch (Exception ex) {
                    context.log("Evo: Error getting user guidance: " + ex.getMessage());
                }
            }
        }
        return false;
    }

    private String performAction(Task task, IAgent agent, TaskContext context, String lastFeedback) throws Exception {
        String taskType = task.getType();
        String taskName = task.getName();
        String taskDescription = task.getDescription();
        String processInput = (taskDescription != null && !taskDescription.isEmpty()) ? taskDescription : taskName;

        // Check if task maps directly to a tool
        if ("file".equalsIgnoreCase(taskType)) {
            FileTool fileTool = new FileTool();

            // Robust path extraction from task name
            String path = taskName.replaceFirst("(?i)^(Write|Create|Generate|Update|Modify|Delete)(\\s+file)?\\s+", "").trim();

            // Strip quotes if present
            if ((path.startsWith("'") && path.endsWith("'")) || (path.startsWith("\"") && path.endsWith("\""))) {
                path = path.substring(1, path.length() - 1);
            }

            task.setResultSummary(path);
            if (taskName.toLowerCase().startsWith("delete")) {
                context.log("Evo: Detected file deletion request for " + path);
                Boolean approved = context.requestApproval("[DELETE] Approve deletion of file: " + path + "?").get();
                if (approved == null || !approved) {
                    throw new Exception("File deletion rejected by user: " + path);
                }
                try {
                    String existingContent = fileTool.execute("READ " + path, context.getProjectRoot(), context);
                    task.setRationale(existingContent);
                    context.log("Evo: Read existing content for " + path + " to capture rationale.");
                } catch (Exception e) {
                    context.log("Evo: Could not read " + path + " (might be a new file).");
                }
                return fileTool.execute("DELETE " + path, context.getProjectRoot(), context);
            }

            // JavaDev/Architect will generate content first
            context.log("Evo: Agent " + agent.getType() + " is processing content for " + path);
            String content = agent.process(processInput, context, lastFeedback);
            context.log("Evo: Agent generated " + content.length() + " characters for " + path);

            // Check for significant deletions
            try {
                String existingContent = fileTool.execute("READ " + path, context.getProjectRoot(), context);
                task.setRationale(existingContent);
                if (existingContent != null && !existingContent.isEmpty()) {
                    int existingLen = existingContent.length();
                    int newLen = content.length();
                    if (newLen < existingLen * 0.8) {
                        double deletionPercent = (1.0 - (double)newLen / existingLen) * 100;
                        context.log("Evo: Significant text deletion detected (" + String.format("%.1f", deletionPercent) + "%) for " + path);
                        Boolean approved = context.requestApproval("[Significant deletion] Content of " + path + " will be reduced by " + String.format("%.1f", deletionPercent) + "%. Approve?").get();
                        if (approved == null || !approved) {
                            throw new Exception("Significant content reduction rejected by user for: " + path);
                        }
                    }
                }
            } catch (Exception e) {
                // File might not exist, which is fine for new files
            }

            // Sanitize path: remove leading slashes and drive letters (e.g., C:/)
            path = path.replaceFirst("^([a-zA-Z]:)?(/|\\\\)+", "");
            // Normalize path: replace backslashes with forward slashes
            path = path.replace("\\", "/");
            String writeResult = fileTool.execute("WRITE " + path + "\n" + content, context.getProjectRoot(), context);
            context.log("Evo: FileTool result for " + path + ": " + writeResult);
            return writeResult + "\nCONTENT:\n" + content;
        } else if ("maven".equalsIgnoreCase(taskType)) {
            MavenTool mavenTool = new MavenTool();
            return mavenTool.execute(taskName, context.getProjectRoot(), context);
        } else if ("git".equalsIgnoreCase(taskType)) {
            if (taskName.toLowerCase().matches(".*\\b(pr|pull request)\\b.*")) {
                context.log("Evo: Requesting approval for Pull Request action: " + taskName);
                Boolean approved = context.requestApproval("[PR] Approve Pull Request creation? Task: " + taskName).get();
                if (approved == null || !approved) {
                    throw new Exception("PR rejected by user: " + taskName);
                }
            }
            GitTool gitTool = new GitTool();
            return gitTool.execute(taskName, context.getProjectRoot(), context);
        } else if ("shell".equalsIgnoreCase(taskType)) {
            context.log("Evo: Requesting approval for terminal command: " + taskName);
            Boolean approved = context.requestApproval("Approve terminal command: " + taskName + "?").get();
            if (approved == null || !approved) {
                throw new Exception("Terminal command rejected by user: " + taskName);
            }
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

        // 2. Map task types to specialized agents
        if (type.contains("terminal") || type.contains("shell")) return availableAgents.stream().filter(a -> a instanceof TerminalAgent).findFirst().orElse(availableAgents.get(0));
        if (type.contains("file")) return availableAgents.stream().filter(a -> a instanceof FileAgent).findFirst().orElse(availableAgents.get(0));
        if (type.contains("maven")) return availableAgents.stream().filter(a -> a instanceof MavenAgent).findFirst().orElse(availableAgents.get(0));
        if (type.contains("git")) return availableAgents.stream().filter(a -> a instanceof GitAgent).findFirst().orElse(availableAgents.get(0));
        if (type.contains("structure") || type.contains("tree")) return availableAgents.stream().filter(a -> a instanceof StructureAgent).findFirst().orElse(availableAgents.get(0));
        if (type.contains("search") || type.contains("web")) return availableAgents.stream().filter(a -> a instanceof WebSearchAgent).findFirst().orElse(availableAgents.get(0));
        if (type.contains("quality") || type.contains("linter") || type.contains("checkstyle")) return availableAgents.stream().filter(a -> a instanceof QualityAgent).findFirst().orElse(availableAgents.get(0));
        if (type.contains("observability") || type.contains("log") || type.contains("tail")) return availableAgents.stream().filter(a -> a instanceof ObservabilityAgent).findFirst().orElse(availableAgents.get(0));

        if (type.contains("test")) return availableAgents.stream().filter(a -> a instanceof TesterAgent).findFirst().orElse(availableAgents.get(2));
        if (type.contains("java")) return availableAgents.stream().filter(a -> a instanceof JavaDevAgent).findFirst().orElse(availableAgents.get(1));
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
