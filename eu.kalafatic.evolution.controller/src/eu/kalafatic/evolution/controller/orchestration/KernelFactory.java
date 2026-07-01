package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer.IntentResult;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityRegistry;
import eu.kalafatic.evolution.controller.orchestration.selfdev.CodingEngine;
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

		// 1. Determine intent and platform type first to resolve target repository
		PromptIntentAnalyzer intentAnalyzer = new PromptIntentAnalyzer(sessionContext, context.getProjectRoot());
		intentAnalyzer.setAiService(aiService);
		IntentResult intentResult = intentAnalyzer.analyze(prompt, context);
		PlatformType platformType = ModeRecognizer.determineType(intentResult, context);

		// 2. Resolve effective root for coding processes
		java.io.File effectiveRoot = context.getProjectRoot();
		if (platformType == PlatformType.ASSISTED_CODING || platformType == PlatformType.SELF_DEV_MODE) {
			if (context.getOrchestrator().getSupervisorSettings() != null &&
				context.getOrchestrator().getSupervisorSettings().getGit() != null) {
				String supPath = context.getOrchestrator().getSupervisorSettings().getGit().getLocalPath();
				if (supPath != null && !supPath.isEmpty()) {
					java.io.File supDir = new java.io.File(supPath);
					if (supDir.exists() && supDir.isDirectory()) {
						effectiveRoot = supDir;
						context.log("[KERNEL] Coding process detected. Redirecting repository root to: " + supPath);
					}
				}
			}
		}

		// 3. Create or use effective context
		TaskContext effectiveContext = context;
		if (!effectiveRoot.equals(context.getProjectRoot())) {
			effectiveContext = new TaskContext(context.getOrchestrator(), effectiveRoot);
			effectiveContext.setSessionId(context.getSessionId());
			effectiveContext.setPlatformMode(context.getPlatformMode());
			effectiveContext.setAiService(aiService);

			// MANDATE: Full state propagation for redirected tasks
			if (context.getOrchestrationState() != null) {
				effectiveContext.getOrchestrationState().setIterationCount(context.getOrchestrationState().getIterationCount());
				effectiveContext.getOrchestrationState().setCurrentPhase(context.getOrchestrationState().getCurrentPhase());
				effectiveContext.getOrchestrationState().setBitState(context.getOrchestrationState().getBitState());
				effectiveContext.getOrchestrationState().setExecutionProfile(context.getOrchestrationState().getExecutionProfile());
				effectiveContext.getOrchestrationState().setLockedAbstractionLevel(context.getOrchestrationState().getLockedAbstractionLevel());
				effectiveContext.getOrchestrationState().getMetadata().putAll(context.getOrchestrationState().getMetadata());
			}
			effectiveContext.getMetadata().putAll(context.getMetadata());

			// Propagate listeners
			context.getLogListeners().forEach(effectiveContext::addLogListener);
			context.getApprovalListeners().forEach(effectiveContext::addApprovalListener);
			context.getInputListeners().forEach(effectiveContext::addInputListener);
		}

		GitManager gitManager = new GitManager(effectiveRoot);
		TaskPlanner taskPlanner = new TaskPlanner(sessionContext);
		TaskExecutor taskExecutor = new TaskExecutor(effectiveContext, effectiveContext.getOrchestrator());
		if (taskExecutor.getOrchestrator() != null) {
			taskExecutor.getOrchestrator().setAiService(aiService);
		}
		Evaluator evaluator = new Evaluator(effectiveRoot, effectiveContext);

		IterationMemoryService memoryService = (sessionContext != null)
				? sessionContext.getMemoryService(effectiveRoot)
				: effectiveContext.getKernelContext().getMemoryService();

		SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(effectiveRoot, effectiveContext);

		IDarwinEngine darwinEngine = DarwinEngineFactory.createEngine(platformType, effectiveContext, memoryService,
				stateProvider);
		darwinEngine.setAiService(aiService);
		darwinEngine.setIntentAnalyzer(intentAnalyzer);

		effectiveContext.log("[DARWIN] Delegating to " + darwinEngine.getClass().getSimpleName());

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
			effectiveContext.log("[KERNEL] Factory capability registration error: " + e.getMessage());
		}

		return new IterationManager(effectiveContext, sessionContext, aiService, gitManager, taskPlanner, taskExecutor,
				evaluator, darwinEngine, memoryService);
	}
	

	public static IterationManager create(PlatformType platformType, SessionContainer sessionContext, AiService aiService) {
		
//		GitManager gitManager = new GitManager(sessionContext.getProjectRoot());
//		TaskPlanner taskPlanner = new TaskPlanner(sessionContext);
//		TaskExecutor taskExecutor = new TaskExecutor(sessionContext, sessionContext.getOrchestrator());
//		if (taskExecutor.getOrchestrator() != null) {
//			taskExecutor.getOrchestrator().setAiService(aiService);
//		}
//		Evaluator evaluator = new Evaluator(context.getProjectRoot(), context);
//		SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(context.getProjectRoot(), context);
//
//		IterationMemoryService memoryService = (sessionContext != null)
//				? sessionContext.getMemoryService(context.getProjectRoot())
//				: context.getKernelContext().getMemoryService();
//
//		
//		PromptIntentAnalyzer intentAnalyzer = new PromptIntentAnalyzer(sessionContext, context.getProjectRoot());
//		intentAnalyzer.setAiService(aiService);		
//
//		IDarwinEngine darwinEngine = DarwinEngineFactory.createEngine(platformType, context, memoryService,
//				stateProvider);
//		darwinEngine.setAiService(aiService);
//		
//		darwinEngine.setIntentAnalyzer(intentAnalyzer);
//		return new IterationManager(context, sessionContext, aiService, gitManager, taskPlanner, taskExecutor,
//				evaluator, darwinEngine, memoryService);
		
		return null; // Placeholder return statement
	}
	
	public static IterationManager create(IDarwinEngine darwinEngine, TaskContext context, SessionContainer sessionContext,
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

	public static IterationManager create(IterationManager iterationManager, String prompt, TaskContext variantContext,
			SessionContainer sessionContainer, AiService aiService) {
		// TODO Auto-generated method stub IterationManager
		return iterationManager;
	}



}
