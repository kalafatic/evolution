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
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.tools.ToolFactory;
import eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider;

/**
 * Core Orchestrator implementation. Blind executor of tasks.
 * Strictly gated by SystemState.
 *
 * @evo:21:A reason=kernel-refactor-alignment
 */
public class EvolutionOrchestrator implements IOrchestrator {

    private static final int MAX_RETRIES = EvolutionConstants.MAX_TASK_RETRIES;
    private AnalyticAgent analyticAgent;
    private ValidatorAgent validator;
    private RepairAgent repairAgent;
    private ProposalConsolidatorAgent consolidator;
    private final List<IAgent> availableAgents = new ArrayList<>();
    private AiService aiService = new AiService();

    public EvolutionOrchestrator() {
        availableAgents.addAll(AgentFactory.getAllAgents());
        analyticAgent = (AnalyticAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_ANALYTIC);
        validator = (ValidatorAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_VALIDATOR);
        repairAgent = (RepairAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_REPAIR);
        consolidator = (ProposalConsolidatorAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_PROPOSAL_CONSOLIDATOR);
    }

    public void setAiService(AiService aiService) {
        this.aiService = aiService;
        if (analyticAgent != null) analyticAgent.setAiService(aiService);
        if (validator != null) validator.setAiService(aiService);
        if (repairAgent != null) repairAgent.setAiService(aiService);
        if (consolidator != null) consolidator.setAiService(aiService);
    }

    @Override
    public OrchestratorResponse handle(TaskRequest taskRequest, TaskContext context) throws Exception {
        String request = taskRequest.getPrompt();
        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(ResultType.CHAT);

        try {
            SystemState kernelState = context.getStateHolder().getState();
            if (kernelState != SystemState.EXECUTING && kernelState != SystemState.INIT) {
                 throw new IllegalStateException("Kernel violation: Orchestrator handle() must be gated by the Control Plane. State: " + kernelState);
            }

            context.setCurrentTaskName("Execution");
            context.log("Evo-Orchestrator-Kernel: Starting " + kernelState + " sequence.");

            List<Task> tasks = new ArrayList<>(context.getOrchestrator().getTasks());
            String lastResult = "";

            if (tasks.isEmpty() && (kernelState == SystemState.EXECUTING || kernelState == SystemState.INIT)) {
                 // Fallback for direct prompt execution if no tasks planned
                 GeneralAgent chatAgent = (GeneralAgent) availableAgents.stream()
                        .filter(a -> a instanceof GeneralAgent)
                        .findFirst()
                        .orElse(new GeneralAgent());
                 lastResult = chatAgent.process(request, context, null);
            }

            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.FAILED) continue;

                context.checkPause();
                context.setCurrentTaskName(task.getName());
                task.setStatus(TaskStatus.RUNNING);

                lastResult = executeTaskInternal(task, context);
                task.setStatus(TaskStatus.DONE);
            }

            response.setSummary(lastResult);
            return response;

        } catch (Exception e) {
            response.setResultType(ResultType.ERROR);
            response.setContent(e.getMessage());
            return response;
        }
    }

    @Override
    public String execute(String request, TaskContext context) throws Exception {
        OrchestratorResponse response = handle(new TaskRequest(request, context.getProjectRoot()), context);
        if (response.getResultType() == ResultType.ERROR) throw new Exception(response.getContent());
        return response.getSummary();
    }

    @Override
    public String executeTask(Task task, TaskContext context) throws Exception {
        SystemState kernelState = context.getStateHolder().getState();
        if (kernelState != SystemState.EXECUTING && kernelState != SystemState.INIT) {
             throw new IllegalStateException("Kernel violation: Orchestrator executeTask() must be gated by the Control Plane. State: " + kernelState);
        }
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
            return ToolFactory.getTool(EvolutionConstants.TOOL_FILE).execute("WRITE " + path + "\n" + patch, context.getProjectRoot(), context);
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
