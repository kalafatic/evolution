package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.io.File;
import java.util.List;
import org.json.JSONObject;
import eu.kalafatic.evolution.model.orchestration.LogLevel;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import eu.kalafatic.evolution.controller.log.LoggingService;
import eu.kalafatic.evolution.controller.agents.AgentFactory;
import eu.kalafatic.evolution.controller.agents.AnalyticAgent;
import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.agents.FinalResponseAgent;
import eu.kalafatic.evolution.controller.agents.GeneralAgent;
import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.agents.ProposalConsolidatorAgent;
import eu.kalafatic.evolution.controller.agents.RepairAgent;
import eu.kalafatic.evolution.controller.agents.ValidatorAgent;
import eu.kalafatic.evolution.controller.tools.FileTool;
import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
import eu.kalafatic.evolution.controller.orchestration.util.CodeExtractor;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.tools.ToolFactory;
import eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider;

/**
 * Core Task Executor implementation. Blindly executes individual tasks.
 * State gating and lifecycle are managed by the {@link IterationManager}.
 *
 * @evo:21:A reason=kernel-refactor-alignment
 */
public class EvolutionOrchestrator implements IOrchestrator {

    @Override
    public OrchestratorResponse handle(TaskRequest request, TaskContext context) throws Exception {
        // Direct handling is now deprecated for the executor.
        // It must be routed through IterationManager.
        IterationManager kernel = KernelFactory.create(context, aiService);
        return kernel.handle(request);
    }

    @Override
    public String execute(String request, TaskContext context) throws Exception {
        OrchestratorResponse response = handle(new TaskRequest(request, context.getProjectRoot()), context);
        if (response.getResultType() == ResultType.ERROR) throw new Exception(response.getContent());
        return response.getSummary();
    }

    private static final int MAX_RETRIES = EvolutionConstants.MAX_TASK_RETRIES;
    private final List<IAgent> availableAgents = new ArrayList<>();
    private AiService aiService = new AiService();

    public EvolutionOrchestrator() {
        availableAgents.addAll(AgentFactory.getAllAgents());
    }

    public void setAiService(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * Executes a single task using the orchestrator's available agents and tools.
     * This is now a "blind" execution of a single task.
     *
     * @param task The task to execute.
     * @param context The shared execution context.
     * @return The result of task execution.
     * @throws Exception if execution fails.
     */
    public String executeTask(Task task, TaskContext context) throws Exception {
        return executeTaskInternal(task, context);
    }

    private String executeTaskInternal(Task task, TaskContext context) throws Exception {
        IAgent agent = findAgentForTask(task, context);
        if (agent instanceof BaseAiAgent) {
            ((BaseAiAgent)agent).setAiService(aiService);
        }

        context.checkPause();

        // 1. Context
        ContextPackage contextPkg = ContextBuilder.build(task, context, 1, task.getFeedback());
        String contextPrompt = ContextBuilder.buildPrompt(contextPkg);

        // 2. Execute (Generate + Apply)
        String patch = generatePatch(task, agent, context, task.getFeedback(), task.getPlan(), contextPrompt);
        String result = applyPatch(task, agent, context, task.getFeedback(), patch);
        task.setResponse(result);
        task.setResultSummary("Task completed.");

        return result;
    }

    private String generatePatch(Task task, IAgent agent, TaskContext context, String lastFeedback, String localPlan, String contextPrompt) throws Exception {
        if ("file".equalsIgnoreCase(task.getType())) return agent.process(task.getDescription(), context, lastFeedback);
        return contextPrompt;
    }

    private String applyPatch(Task task, IAgent agent, TaskContext context, String lastFeedback, String patch) throws Exception {
        String taskType = task.getType();
        String taskName = task.getName();

        if ("file".equalsIgnoreCase(taskType)) {
            String path = taskName.replaceFirst("(?i)^(Write|Create|Update|MKDIR|DELETE)\\s+", "").trim().split(" ")[0];
            // Sanitization: Remove leading slashes and drive letters, and normalize separators
            path = path.replaceFirst("^([a-zA-Z]:)?[/\\\\]+", "").replace('\\', '/');

            if (path == null || path.isEmpty() || "null".equals(path)) {
                throw new Exception("Kernel Violation: Attempted to write to a null or empty path: " + taskName);
            }

            String extractedCode = CodeExtractor.extractCode(patch);
            return ToolFactory.getTool(EvolutionConstants.TOOL_FILE).execute("WRITE " + path + "\n" + extractedCode, context.getProjectRoot(), context);
        } else if (EvolutionConstants.TASK_MAVEN.equalsIgnoreCase(taskType)) {
            return ToolFactory.getTool(EvolutionConstants.TOOL_MAVEN).execute(taskName, context.getProjectRoot(), context);
        } else if (EvolutionConstants.TASK_GIT.equalsIgnoreCase(taskType)) {
            return ToolFactory.getTool(EvolutionConstants.TOOL_GIT).execute(taskName, context.getProjectRoot(), context);
        } else if (EvolutionConstants.TASK_SHELL.equalsIgnoreCase(taskType)) {
            return ToolFactory.getTool(EvolutionConstants.TOOL_SHELL).execute(taskName, context.getProjectRoot(), context);
        }
        return agent.process(taskName, context, lastFeedback);
    }

    private IAgent findAgentForTask(Task task, TaskContext context) {
        String type = task.getType().toLowerCase();
        if (type.equals(EvolutionConstants.TASK_FILE)) return AgentFactory.getAgent(EvolutionConstants.AGENT_FILE);
        if (type.equals(EvolutionConstants.TASK_MAVEN)) return AgentFactory.getAgent(EvolutionConstants.AGENT_MAVEN);
        return AgentFactory.getAgent(EvolutionConstants.AGENT_GENERAL);
    }
}
