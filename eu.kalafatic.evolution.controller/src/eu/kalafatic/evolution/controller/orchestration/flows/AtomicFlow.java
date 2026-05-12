package eu.kalafatic.evolution.controller.orchestration.flows;

import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Task;

/**
 * Deterministic atomic execution flow for simple, high-confidence tasks.
 */
public class AtomicFlow implements IOrchestrationFlow {
    private final AiService aiService;
    private final IterationManager manager;

    public AtomicFlow(AiService aiService, IterationManager manager) {
        this.aiService = aiService;
        this.manager = manager;
    }

    @Override
    public OrchestratorResponse execute(String request, TaskContext context) throws Exception {
        context.log("[KERNEL] Executing Atomic Flow.");
        OrchestrationState state = context.getOrchestrationState();
        AtomicIntentAnalysis atomicAnalysis = (AtomicIntentAnalysis) state.getMetadata().get("atomicAnalysis");

        List<Task> tasks = manager.createAtomicFilePlan(request, atomicAnalysis, context);
        state.getExecutionPlan().addAll(tasks);
        context.getOrchestrator().getTasks().addAll(tasks);

        manager.checkStep("evolution_loop", "ATOMIC_PLAN", "Review atomic execution plan.");

        manager.transition(SystemState.PLAN_LOCKED, context);
        boolean success = manager.executeTasksWithRetries(tasks);

        manager.transition(SystemState.VERIFYING, context);
        String path = tasks.get(0).getName().replaceFirst("(?i)^Write\\s+", "");
        String summary = "Created file " + path + ".";

        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(ResultType.CHAT);
        response.setSummary(summary);

        manager.transition(success ? SystemState.DONE : SystemState.FAILED, context);
        return response;
    }
}
