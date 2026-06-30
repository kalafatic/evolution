package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer.IntentResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngineFactory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IDarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SystemStateSignalProvider;
import eu.kalafatic.evolution.controller.orchestration.util.ModeRecognizer;
import eu.kalafatic.evolution.model.orchestration.Task;

/**
 * Unified entry point for the Evolutionary OS Kernel. Routes all external
 * requests through the {@link IterationManager} state machine.
 */
public class KernelFacade implements IOrchestrator {

	@Override
	public OrchestratorResponse handle(TaskRequest taskRequest, TaskContext context) throws Exception {
		SessionContainer session = (SessionContainer) context.getMetadata().get("sessionContext");
		if (session == null) {
			session = SessionManager.getInstance().getOrCreateSession(context.getSessionId());
		}

		IterationManager kernel = session.getIterationManager();

		if (kernel == null) {
			kernel = KernelFactory.create(taskRequest.getPrompt(), context, session);
			session.setIterationManager(kernel);

		} else {
			String request = taskRequest.getPrompt();
			IntentResult intentResult = kernel.getDarwinEngine().getIntentAnalyzer().analyze(request, context);
			PlatformType platformType = ModeRecognizer.determineType(intentResult, context);

			if (!kernel.getDarwinEngine().getPlatformType().equals(platformType)) {
				createEngine(context, session, kernel, platformType);
			}
		}
		return kernel.handle(taskRequest);
	}	

	@Override
	public String execute(String request, TaskContext context) throws Exception {
		if (context == null)
			throw new Exception("Cannot execute kernel command: No active task context.");
		OrchestratorResponse response = handle(new TaskRequest(request, context.getProjectRoot()), context);
		if (response.getResultType() == ResultType.ERROR) {
			throw new Exception(response.getContent());
		}
		return response.getSummary();
	}

	@Override
	public String executeTask(Task task, TaskContext context) throws Exception {
		// Direct task execution is still routed through IterationManager
		// to ensure the system is in the correct state (EXECUTING).
		SessionContainer session = (SessionContainer) context.getMetadata().get("sessionContext");
		if (session == null) {
			session = SessionManager.getInstance().getOrCreateSession(context.getSessionId());
		}
		
		IterationManager kernel = session.getIterationManager();

		if (kernel == null) {
			kernel = KernelFactory.create(task.getPrompt(), context, session);
			session.setIterationManager(kernel);

		} else {
			String request = task.getPrompt();
			IntentResult intentResult = kernel.getDarwinEngine().getIntentAnalyzer().analyze(request, context);
			PlatformType platformType = ModeRecognizer.determineType(intentResult, context);

			if (!kernel.getDarwinEngine().getPlatformType().equals(platformType)) {
				createEngine(context, session, kernel, platformType);
			}
		}

		java.util.List<Task> tasks = new java.util.ArrayList<>();
		tasks.add(task);
		boolean success = kernel.executeTasksWithRetries(tasks);
		if (!success)
			throw new Exception("Task failed: " + task.getName());
		return task.getResponse();
	}
		
	private void createEngine(TaskContext context, SessionContainer session, IterationManager kernel,
			PlatformType platformType) {
		IterationMemoryService memoryService = (session != null)
				? session.getMemoryService(context.getProjectRoot())
				: context.getKernelContext().getMemoryService();

		SystemStateSignalProvider stateProvider = new SystemStateSignalProvider(context.getProjectRoot(),
				context);
		IDarwinEngine darwinEngine = DarwinEngineFactory.createEngine(platformType, context, memoryService,
				stateProvider);
		darwinEngine.setIntentAnalyzer(kernel.getDarwinEngine().getIntentAnalyzer());
		darwinEngine.setAiService(kernel.getDarwinEngine().getIntentAnalyzer().getAiService());
		
		kernel.setDarwinEngine(darwinEngine);
	}
}
