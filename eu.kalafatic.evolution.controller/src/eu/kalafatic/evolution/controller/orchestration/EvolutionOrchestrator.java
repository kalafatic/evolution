package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.json.JSONObject;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import eu.kalafatic.evolution.controller.agents.AnalyticAgent;
import eu.kalafatic.evolution.controller.agents.ArchitectAgent;
import eu.kalafatic.evolution.controller.agents.FileAgent;
import eu.kalafatic.evolution.controller.agents.GeneralAgent;
import eu.kalafatic.evolution.controller.agents.GitAgent;
import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.agents.JavaDevAgent;
import eu.kalafatic.evolution.controller.agents.MavenAgent;
import eu.kalafatic.evolution.controller.agents.ObservabilityAgent;
import eu.kalafatic.evolution.controller.agents.PlannerAgent;
import eu.kalafatic.evolution.controller.agents.QualityAgent;
import eu.kalafatic.evolution.controller.agents.ReviewerAgent;
import eu.kalafatic.evolution.controller.agents.StructureAgent;
import eu.kalafatic.evolution.controller.agents.TerminalAgent;
import eu.kalafatic.evolution.controller.agents.TesterAgent;
import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
import eu.kalafatic.evolution.controller.tools.FileTool;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.evolution.controller.tools.MavenTool;
import eu.kalafatic.evolution.controller.tools.ShellTool;

/**
 * Core Orchestrator implementation that manages the task lifecycle and execution.
 */
public class EvolutionOrchestrator implements IOrchestrator {

    private static final int MAX_RETRIES = 3;
    private IIntentClassifier intentClassifier = new LlmIntentClassifier();
    private IPolicyEngine policyEngine = new RuleBasedPolicyEngine();
    private AnalyticAgent analyticAgent = new AnalyticAgent();
    private PlannerAgent planner = new PlannerAgent();
    private ReviewerAgent reviewer = new ReviewerAgent();
    private final List<IAgent> availableAgents = new ArrayList<>();

    public EvolutionOrchestrator() {
        // Initialize default agents
        availableAgents.add(analyticAgent);
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
        context.setCurrentTaskName(task.getName());
        context.log("Evo-Orchestrator-" + task.getName() + ": Executing single task");
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
            context.setCurrentTaskName("Initialization");
            context.log("Evo-Orchestrator-Initialization: Starting request - " + request);
            context.appendSharedMemory("Initial user request: " + request);

            // 0. Intent Gate + Policy Engine
            context.log("Evo-Orchestrator-IntentGate: Classifying intent...");
            JSONObject classification = intentClassifier.classify(request, context);
            context.log("Evo-Orchestrator-IntentGate: Intent - " + classification.optString("intent") + " (conf: " + classification.optDouble("confidence") + ")");

            String policyResponse = policyEngine.evaluate(classification, request, context);
            if (policyResponse != null) {
                context.log("Evo-Orchestrator-Policy: Action blocked or handled directly.");
                return policyResponse;
            }

            // 1. Analytic Phase
            String analyzedRequest = analyzeAndClarify(request, context);

            // 2. Planning
            OrchestrationStatusManager.getInstance().updateAgentStatus("Planner", "Planning...");
            List<Task> originalPlannedTasks = planner.plan(analyzedRequest, context);
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
                StringBuilder planSummary = new StringBuilder();
                planSummary.append("Evo-Orchestrator-Planning: Plan generated. Waiting for user review and approval...\n\n### Proposed Plan:\n");
                for (int i = 0; i < originalPlannedTasks.size(); i++) {
                    Task t = originalPlannedTasks.get(i);
                    planSummary.append((i + 1)).append(". [").append(t.getType()).append("] ").append(t.getName()).append("\n");
                }
                context.log(planSummary.toString());

                Boolean planApproved = true;
                if (!context.isAutoApprove()) {
                    planApproved = context.requestApproval(TaskContext.PLAN_APPROVAL_MESSAGE).get();
                } else {
			context.log("Evo-Orchestrator-Planning: Auto-approval enabled. Skipping manual approval.");
                }

                if (planApproved == null || !planApproved) {
                    context.log("Evo-Orchestrator-Planning: Plan rejected by user.");
                    throw new Exception("Orchestration plan rejected by user.");
                }
                context.log("Evo-Orchestrator-Planning: Plan approved. Starting execution...");
            } else {
                context.log("Evo-Orchestrator-Planning: Low severity plan generated. Skipping manual approval and starting execution...");
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
                context.setCurrentTaskName(task.getName());

                // Check for User Approval
                if (task.isApprovalRequired() || "approval".equalsIgnoreCase(task.getType())) {
                    task.setStatus(TaskStatus.WAITING_FOR_APPROVAL);

                    Boolean approved = true;
                    if (!context.isAutoApprove()) {
                        context.log("Evo-Orchestrator-" + task.getName() + ": Waiting for user approval...");
                        approved = context.requestApproval("Approve task: " + task.getName() + "?").get();
                    } else {
                        context.log("Evo-Orchestrator-" + task.getName() + ": Auto-approving task.");
                    }

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
                        context.log("Evo-Orchestrator-" + task.getName() + ": Looping back to task ID: " + loopToId);
                        i = loopTargetIndex - 1; // -1 because the for loop will increment i
                    }
                }
            }

            updateStatus(context, 1.0, "Completed");
            return lastResult != null && !lastResult.isEmpty() ? lastResult : "Orchestration successful.";
        } catch (Exception e) {
            context.log("Evo-Orchestrator-Error: " + e.getMessage());
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
            context.log("Evo-Orchestrator-" + task.getName() + ": Attempt " + retry);

            try {
                // Execute action (either via tool or reasoning)
                String result = performAction(task, agent, context, lastFeedback);
                task.setResponse(result);

                // Handle Clarification/Proposal stall
                if (result != null && (result.contains("CLARIFY") || result.contains("[PROPOSAL:"))) {
                    context.log("Evo-Orchestrator-" + task.getName() + ": Agent requested clarification/proposal. Pausing execution.");
                    String clarification = context.requestInput(result).get();
                    if (clarification != null) {
                        lastFeedback = "User Response: " + clarification;
                        retry--; // Retry with the new information
                        continue;
                    }
                }

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
                context.log("Evo-Orchestrator-" + task.getName() + ": Error during execution: " + e.getMessage());
                lastFeedback = "Exception: " + e.getMessage();
                task.setFeedback("Retry " + retry + " Exception: " + e.getMessage());
            }

            if (retry == MAX_RETRIES) {
                context.log("Evo-Orchestrator-" + task.getName() + ": Failed after " + MAX_RETRIES + " retries");
                try {
                    String guidance = context.requestInput("Evo-Orchestrator-" + task.getName() + ": Task failed consistently. Waiting for user guidance (retry/skip/hint)...").get();
                    if (guidance != null) {
                        if ("retry".equalsIgnoreCase(guidance.trim())) {
                            retry = 0; // Reset loop to try again
                            lastFeedback = null;
                            context.log("Evo-Orchestrator-" + task.getName() + ": User requested retry");
                        } else if ("skip".equalsIgnoreCase(guidance.trim())) {
                            context.log("Evo-Orchestrator-" + task.getName() + ": User requested to skip task");
                            task.setFeedback("Skipped by user.");
                            return true;
                        } else {
                            // Treat as hint for one more attempt
                            lastFeedback = "User Hint: " + guidance;
                            context.log("Evo-Orchestrator-" + task.getName() + ": Applying user hint: " + guidance);
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
            String path = taskName.replaceFirst("(?i)^(.+:\\s*)?(Write|Create|Generate|Update|Modify|Delete)(\\s+file)?\\s+", "").trim();

            // Strip quotes if present
            if ((path.startsWith("'") && path.endsWith("'")) || (path.startsWith("\"") && path.endsWith("\""))) {
                path = path.substring(1, path.length() - 1);
            }

            // Refine path: strip trailing descriptive suffixes (e.g., "with hi", "containing class X")
            path = path.split("(?i)\\s+(with|to|for|using|based|containing|in|at)\\s+")[0];

            if (taskName.toLowerCase().startsWith("delete")) {
                task.setResultSummary("I deleted the file: [FILE:" + path + "]");
                context.log("Evo-Orchestrator-" + taskName + ": File deletion request for " + path + ". Waiting for user approval...");
                Boolean approved = true;
                if (!context.isAutoApprove()) {
			approved = context.requestApproval("[DELETE] Approve deletion of file: " + path + "?").get();
                }
                if (approved == null || !approved) {
                    throw new Exception("File deletion rejected by user: " + path);
                }
                try {
                    String existingContent = fileTool.execute("READ " + path, context.getProjectRoot(), context);
                    task.setRationale(existingContent);
                    context.log("Evo-Orchestrator-" + taskName + ": Capture rationale from " + path);
                } catch (Exception e) {
                    context.log("Evo-Orchestrator-" + taskName + ": Could not read " + path);
                }
                return fileTool.execute("DELETE " + path, context.getProjectRoot(), context);
            }

            // JavaDev/Architect will generate content first
            context.log("Evo-Orchestrator-" + taskName + ": " + agent.getType() + " agent processing " + path);
            String content = agent.process(processInput, context, lastFeedback);
            context.log("Evo-Orchestrator-" + taskName + ": Agent generated " + content.length() + " chars for " + path);

            // Check for significant deletions
            try {
                String existingContent = fileTool.execute("READ " + path, context.getProjectRoot(), context);
                task.setRationale(existingContent);
                if (existingContent != null && !existingContent.isEmpty()) {
                    int existingLen = existingContent.length();
                    int newLen = content.length();
                    if (newLen < existingLen * 0.8) {
                        double deletionPercent = (1.0 - (double)newLen / existingLen) * 100;
                        context.log("Evo-Orchestrator-" + taskName + ": Significant deletion detected (" + String.format("%.1f", deletionPercent) + "%) for " + path + ". Waiting for user approval...");
                        Boolean approved = true;
                        if (!context.isAutoApprove()) {
				approved = context.requestApproval("[Significant deletion] Content of " + path + " will be reduced by " + String.format("%.1f", deletionPercent) + "%. Approve?").get();
                        }
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
            task.setResultSummary("I created/updated the file: [FILE:" + path + "]. It can be opened and verified in the project explorer.");
            context.log("Evo-Orchestrator-" + taskName + ": File write result - " + writeResult);
            return writeResult + "\nCONTENT:\n" + content;
        } else if ("maven".equalsIgnoreCase(taskType)) {
            MavenTool mavenTool = new MavenTool();
            String res = mavenTool.execute(taskName, context.getProjectRoot(), context);
            task.setResultSummary("Maven: " + taskName + " executed. Result: " + (res.length() > 50 ? res.substring(0, 47) + "..." : res));
            return res;
        } else if ("git".equalsIgnoreCase(taskType)) {
            if (taskName.toLowerCase().matches(".*\\b(pr|pull request)\\b.*")) {
                context.log("Evo-Orchestrator-" + taskName + ": Waiting for user approval for PR...");
                Boolean approved = true;
                if (!context.isAutoApprove()) {
                    approved = context.requestApproval("[PR] Approve Pull Request creation? Task: " + taskName).get();
                }
                if (approved == null || !approved) {
                    throw new Exception("PR rejected by user: " + taskName);
                }
            }
            GitTool gitTool = new GitTool();
            String res = gitTool.execute(taskName, context.getProjectRoot(), context);
            task.setResultSummary("Git: " + taskName + " executed.");
            return res;
        } else if ("shell".equalsIgnoreCase(taskType)) {
            context.log("Evo-Orchestrator-" + taskName + ": Waiting for user approval for command...");
            Boolean approved = true;
            if (!context.isAutoApprove()) {
		approved = context.requestApproval("Approve terminal command: " + taskName + "?").get();
            }
            if (approved == null || !approved) {
                throw new Exception("Terminal command rejected by user: " + taskName);
            }
            ShellTool shellTool = new ShellTool();
            String res = shellTool.execute(taskName, context.getProjectRoot(), context);
            task.setResultSummary("Shell: " + taskName + " executed.");
            return res;
        }

        // Default to agent reasoning
        return agent.process(taskName, context, lastFeedback);
    }

    private String analyzeAndClarify(String request, TaskContext context) throws Exception {
        OrchestrationStatusManager.getInstance().updateAgentStatus("Analytic", "Analyzing request...");
        try {
            JSONObject analysis = analyticAgent.analyze(request, context);
            context.log("Evo-Orchestrator-Analysis: Identified category - " + analysis.optString("category"));

            if (analysis.optBoolean("isAmbiguous", false)) {
                String question = analysis.optString("clarificationQuestion", "The request is ambiguous. Can you please provide more details?");
                context.log("Evo-Orchestrator-Analysis: Request is ambiguous. Waiting for user clarification...");

                String clarification = context.requestInput(question).get();
                if (clarification == null || clarification.trim().isEmpty()) {
                    context.log("Evo-Orchestrator-Analysis: No clarification provided.");
                    return request;
                }

                context.log("Evo-Orchestrator-Analysis: Received clarification: " + clarification);
                context.appendSharedMemory("User Clarification: " + clarification);

                // Recursively analyze with the clarification
                return analyzeAndClarify(request + "\nClarification: " + clarification, context);
            }

            String refined = analysis.optString("refinedPrompt", request);
            if (!refined.equals(request)) {
                context.log("Evo-Orchestrator-Analysis: Refined prompt for planning.");
            }
            return refined;
        } catch (Exception e) {
            context.log("Evo-Orchestrator-Analysis-Warning: " + e.getMessage());
            return request;
        } finally {
            OrchestrationStatusManager.getInstance().updateAgentStatus("Analytic", "Idle");
        }
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
