package eu.kalafatic.evolution.controller.orchestration;

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
        context.log("[KERNEL] Executing Atomic Flow (Bridged to SingleVariantDarwinFlow).");
        try {
            return new SingleVariantDarwinFlow(aiService, manager).execute(request, context);
        } catch (Exception e) {
            manager.getGitManager().rollback();
            manager.transition(SystemState.FAILED, context);
            throw e;
        }
    }
}
