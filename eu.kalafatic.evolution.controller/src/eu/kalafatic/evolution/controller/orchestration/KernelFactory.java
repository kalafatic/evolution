package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SystemStateSignalProvider;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskExecutor;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityRegistry;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException;
import eu.kalafatic.evolution.controller.orchestration.scheduling.KernelScheduler;
import eu.kalafatic.evolution.controller.orchestration.decision.ActivationResolver;

/**
 * Factory for creating the Kernel's control plane with production dependencies.
 */
public class KernelFactory {

    public static IterationManager create(TaskContext context) {
        return create(context, new AiService());
    }

    public static IterationManager create(TaskContext context, AiService aiService) {
        GitManager gitManager = new GitManager(context.getProjectRoot(), context);
        TaskPlanner taskPlanner = new TaskPlanner();
        TaskExecutor taskExecutor = new TaskExecutor(context, context.getOrchestrator());
        if (taskExecutor.getOrchestrator() != null) {
            taskExecutor.getOrchestrator().setAiService(aiService);
        }
        Evaluator evaluator = new Evaluator(context.getProjectRoot(), context);
        IterationMemoryService memoryService = new IterationMemoryService(context.getProjectRoot());
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(context.getProjectRoot(), context);
        DarwinEngine darwinEngine = new DarwinEngine(context, memoryService, stateProvider);
        darwinEngine.setAiService(aiService);

        // Register static capabilities
        try {
            CapabilityRegistry.getInstance().register(new KernelScheduler());
            CapabilityRegistry.getInstance().register(new ActivationResolver());
        } catch (CapabilityException e) {
            context.log("[KERNEL] Factory capability registration error: " + e.getMessage());
        }

        return new IterationManager(
            context,
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
