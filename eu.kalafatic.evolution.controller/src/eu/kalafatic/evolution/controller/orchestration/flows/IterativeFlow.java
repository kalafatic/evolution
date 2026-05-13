package eu.kalafatic.evolution.controller.orchestration.flows;

import java.util.List;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentAnalysisResult;
import eu.kalafatic.evolution.controller.orchestration.intent.ClarificationManager;
import eu.kalafatic.evolution.controller.orchestration.intent.ConfirmedRequirements;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.model.orchestration.Task;

/**
 * Standard iterative planning and execution flow.
 */
public class IterativeFlow implements IOrchestrationFlow {
    private final AiService aiService;
    private final IterationManager manager;
    private final ClarificationManager clarificationManager = new ClarificationManager();

    public IterativeFlow(AiService aiService, IterationManager manager) {
        this.aiService = aiService;
        this.manager = manager;
    }

    @Override
    public OrchestratorResponse execute(String request, TaskContext context) throws Exception {
        context.log("[KERNEL] Executing Iterative Flow.");
        OrchestrationState state = context.getOrchestrationState();

        // Deep analysis for complex tasks
        JSONObject analysis = manager.getAnalyticAgent().analyze(request, context);
        context.log("[KERNEL] Analysis Result: " + analysis.toString());
        state.getMetadata().put("deepAnalysis", analysis);

        // Policy check
        IPolicyEngine policyEngine = new RuleBasedPolicyEngine();
        String policyResponse = policyEngine.evaluate(analysis, request, context);
        if (policyResponse != null) {
            OrchestratorResponse response = new OrchestratorResponse();
            response.setResultType(ResultType.CHAT);
            response.setSummary(policyResponse);
            manager.transition(SystemState.DONE, context);
            return response;
        }

        IntentAnalysisResult deepAnalysis = state.getIntentAnalysis();
        ConversationState convState = ConversationState.load(context.getSharedMemory(), context.getSessionId());

        // Clarification loop
        if (clarificationManager.shouldClarify(deepAnalysis) && !context.getOrchestrator().isDarwinMode() && !context.isAutoApprove()) {
            manager.transition(SystemState.CLARIFYING, context);
            String question = clarificationManager.generateClarificationQuestion(deepAnalysis, context);
            clarificationManager.updateState(convState, deepAnalysis, question);
            context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getSessionId(), convState));

            // Delegate decision to AuthorityController
            AuthorityController authority = new AuthorityController();
            String clarification = context.requestInput(question).get();
            if (clarification != null && !clarification.isEmpty() && !clarification.equalsIgnoreCase("Rejected")) {
                convState.addClarification(clarification);
                convState.setRequirementMet(true);
                context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getSessionId(), convState));
                return manager.handle(new TaskRequest(request + "\nClarification: " + clarification, context.getProjectRoot()));
            } else {
                OrchestratorResponse response = new OrchestratorResponse();
                response.setResultType(ResultType.CHAT);
                response.setSummary("Generation stopped: Clarification required.");
                manager.transition(SystemState.FAILED, context);
                return response;
            }
        }

        freezeRequirements(convState, deepAnalysis, context);
        context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getSessionId(), convState));

        // Iterative Planning
        List<Task> tasks = manager.iterativePlan(analysis.optString("refinedPrompt", request), context);
        state.getExecutionPlan().addAll(tasks);
        context.getOrchestrator().getTasks().addAll(tasks);

        manager.checkStep("evolution_loop", "PLANNING", "Plan generated. Review tasks before execution.");

        // EXECUTING stage
        manager.transition(SystemState.PLAN_LOCKED, context);
        boolean success = manager.executeTasksWithRetries(tasks);

        // VERIFYING stage
        manager.transition(SystemState.VERIFYING, context);
        String summary = manager.getFinalResponseAgent().generateFinalResponse(request, tasks, context);

        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(ResultType.CHAT);
        response.setSummary(summary);

        manager.transition(success ? SystemState.DONE : SystemState.FAILED, context);
        return response;
    }

    private void freezeRequirements(ConversationState state, IntentAnalysisResult result, TaskContext context) {
        ConfirmedRequirements existing = state.getConfirmedRequirements();
        if (existing != null && existing.getHash().equals(Integer.toHexString(java.util.Objects.hash(result.getGoal(), result.getLanguage(), result.getFramework(), result.getConstraints(), result.getExpectedOutput())))) return;
        int version = existing != null ? existing.getVersion() + 1 : 1;
        ConfirmedRequirements frozen = new ConfirmedRequirements(result.getGoal(), result.getLanguage(), result.getFramework(), result.getConstraints(), result.getExpectedOutput(), version);
        state.setConfirmedRequirements(frozen);
    }
}
