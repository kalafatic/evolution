package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SystemStateSignalProvider;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskExecutor;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner;

/**
 * Factory for creating the Kernel's control plane with production dependencies.
 */
public class KernelFactory {

    public static IterationManager create(TaskContext context) {
        AiService aiService = new AiService();
        GitManager gitManager = new GitManager(context.getProjectRoot(), context);
        TaskPlanner taskPlanner = new TaskPlanner();
        TaskExecutor taskExecutor = new TaskExecutor(context);
        Evaluator evaluator = new Evaluator(context.getProjectRoot(), context);
        IterationMemoryService memoryService = new IterationMemoryService(context.getProjectRoot());
        SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(context.getProjectRoot(), context);
        DarwinEngine darwinEngine = new DarwinEngine(context, memoryService, stateProvider);
        darwinEngine.setAiService(aiService);

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
