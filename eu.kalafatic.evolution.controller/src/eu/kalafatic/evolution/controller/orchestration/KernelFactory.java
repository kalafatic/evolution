package eu.kalafatic.evolution.controller.orchestration;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityRegistry;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SystemStateSignalProvider;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskExecutor;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner;
import eu.kalafatic.evolution.controller.supervision.ActivationResolver;

/**
 * Factory for creating the Kernel's control plane with production dependencies.
 */
public class KernelFactory {

    public static IterationManager create(TaskContext context, SessionContainer sessionContext) {
        return create(context, sessionContext, new AiService());
    }

    public static IterationManager create(TaskContext context, SessionContainer sessionContext, AiService aiService) {
        GitManager gitManager = new GitManager(context.getProjectRoot());
        TaskPlanner taskPlanner = new TaskPlanner(sessionContext);
        TaskExecutor taskExecutor = new TaskExecutor(context, context.getOrchestrator());
        if (taskExecutor.getOrchestrator() != null) {
            taskExecutor.getOrchestrator().setAiService(aiService);
        }
        Evaluator evaluator = new Evaluator(context.getProjectRoot(), context);

        IterationMemoryService memoryService = (sessionContext != null) ?
                sessionContext.getMemoryService(context.getProjectRoot()) :
                context.getKernelContext().getMemoryService();

        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(context.getProjectRoot(), context);
        eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine darwinEngine = new DarwinEngine(context, memoryService, stateProvider);
        darwinEngine.setAiService(aiService);

        // Register static capabilities
        try {
            if (sessionContext == null) {
                throw new IllegalStateException("KernelFactory: sessionContext is null for session " + context.getSessionId() + ". Cannot register capabilities.");
            }
            CapabilityRegistry reg = sessionContext.getCapabilityRegistry();
            reg.register(new eu.kalafatic.evolution.controller.execution.KernelScheduler(
                eu.kalafatic.evolution.controller.execution.ExecutionBudget.defaultProfile(),
                sessionContext.getBackpressureController()));
            reg.register(new ActivationResolver(memoryService.getTrajectoryMemory()));
        } catch (CapabilityException e) {
            context.log("[KERNEL] Factory capability registration error: " + e.getMessage());
        }

        return new IterationManager(
            context,
            sessionContext,
            aiService,
            gitManager,
            taskPlanner,
            taskExecutor,
            evaluator,
            darwinEngine,
            memoryService
        );
    }
}
