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

		String promptLower = taskRequest.getPrompt() != null ? taskRequest.getPrompt().toLowerCase() : "";
		if (promptLower.contains("acquire") && (promptLower.contains("data") || promptLower.contains("dataset"))) {
			long targetBytes = parseTargetBytesFromPrompt(taskRequest.getPrompt(), context);

			java.util.Map<String, Object> goalParams = new java.util.HashMap<>();
			goalParams.put("targetUsableBytes", targetBytes);
			goalParams.put("targetMetric", (double) targetBytes);
			goalParams.put("currentMetricKey", "quantity");

			context.getMetadata().put("targetUsableBytes", targetBytes);

			eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveGoal goal =
				new eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveGoal(
					taskRequest.getPrompt(),
					"DATASET_ACQUISITION",
					goalParams
				);

			eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveLoopEngine cognitiveEngine =
				new eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveLoopEngine(10, null);

			eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveResult result =
				cognitiveEngine.solve(session, context, goal);

			OrchestratorResponse response = new OrchestratorResponse();
			response.setSummary(result.getSummary());
			response.setContent("Cognitive Execution Result: " + result.getSummary());
			response.setResultType(result.getFinalState() == eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveState.SUCCESS ? ResultType.CHAT : ResultType.ERROR);
			return response;
		}

		return kernel.handle(taskRequest);
	}

	private long parseTargetBytesFromPrompt(String prompt, TaskContext context) {
		if (context != null && context.getMetadata().containsKey("targetUsableBytes")) {
			return ((Number) context.getMetadata().get("targetUsableBytes")).longValue();
		}
		if (prompt != null) {
			java.util.regex.Matcher gbMatcher = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*gb", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(prompt);
			if (gbMatcher.find()) {
				double gb = Double.parseDouble(gbMatcher.group(1));
				return (long) (gb * 1024L * 1024L * 1024L);
			}
			java.util.regex.Matcher mbMatcher = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*mb", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(prompt);
			if (mbMatcher.find()) {
				double mb = Double.parseDouble(mbMatcher.group(1));
				return (long) (mb * 1024L * 1024L);
			}
		}
		return 52_428_800L; // 50 MB default if unspecified
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
