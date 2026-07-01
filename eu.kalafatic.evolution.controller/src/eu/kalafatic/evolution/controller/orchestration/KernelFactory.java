package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer.IntentResult;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityRegistry;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngineFactory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IDarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SystemStateSignalProvider;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskExecutor;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner;
import eu.kalafatic.evolution.controller.orchestration.util.ModeRecognizer;
import eu.kalafatic.evolution.controller.supervision.ActivationResolver;

/**
 * Factory for creating the Kernel's control plane with production dependencies.
 */
public class KernelFactory {

	public static IterationManager create(String prompt, TaskContext context, SessionContainer sessionContext)
			throws Exception {
		return create(prompt, context, sessionContext, new AiService());
	}

	public static IterationManager create(String prompt, TaskContext context, SessionContainer sessionContext,
			AiService aiService) throws Exception {
		GitManager gitManager = new GitManager(context.getProjectRoot());
		TaskPlanner taskPlanner = new TaskPlanner(sessionContext);
		TaskExecutor taskExecutor = new TaskExecutor(context, context.getOrchestrator());
		if (taskExecutor.getOrchestrator() != null) {
			taskExecutor.getOrchestrator().setAiService(aiService);
		}
		Evaluator evaluator = new Evaluator(context.getProjectRoot(), context);

		IterationMemoryService memoryService = (sessionContext != null)
				? sessionContext.getMemoryService(context.getProjectRoot())
				: context.getKernelContext().getMemoryService();

		SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(context.getProjectRoot(), context);

		PromptIntentAnalyzer intentAnalyzer = new PromptIntentAnalyzer(sessionContext, context.getProjectRoot());
		intentAnalyzer.setAiService(aiService);

		IntentResult intentResult = intentAnalyzer.analyze(prompt, context);
		PlatformType platformType = ModeRecognizer.determineType(intentResult, context);

		IDarwinEngine darwinEngine = DarwinEngineFactory.createEngine(platformType, context, memoryService,
				stateProvider);
		darwinEngine.setAiService(aiService);
		darwinEngine.setIntentAnalyzer(intentAnalyzer);

		context.log("[DARWIN] Delegating to " + darwinEngine.getClass().getSimpleName());

		// Register static capabilities
		try {
			if (sessionContext == null) {
				throw new IllegalStateException("KernelFactory: sessionContext is null for session "
						+ context.getSessionId() + ". Cannot register capabilities.");
			}
			CapabilityRegistry reg = sessionContext.getCapabilityRegistry();
			reg.register(new eu.kalafatic.evolution.controller.execution.KernelScheduler(
					eu.kalafatic.evolution.controller.execution.ExecutionBudget.defaultProfile(),
					sessionContext.getBackpressureController()));
			reg.register(new ActivationResolver(memoryService.getTrajectoryMemory()));
		} catch (CapabilityException e) {
			context.log("[KERNEL] Factory capability registration error: " + e.getMessage());
		}

		return new IterationManager(context, sessionContext, aiService, gitManager, taskPlanner, taskExecutor,
				evaluator, darwinEngine, memoryService);
	}

}
