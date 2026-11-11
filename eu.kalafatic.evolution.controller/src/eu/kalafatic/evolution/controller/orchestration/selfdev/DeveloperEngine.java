package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetClassificationDetector;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetClassificationDetector.ClassificationResult;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetScanner;
import eu.kalafatic.evolution.controller.orchestration.EvolutionProgressPublisher;
import eu.kalafatic.evolution.controller.orchestration.EvolutionStage;
import eu.kalafatic.evolution.controller.orchestration.FileChangeTracker;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.ResultType;
import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.intent.EvolutionAssessment;
import eu.kalafatic.evolution.controller.review.service.PeerReviewService;
import eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool;
import eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider;
import eu.kalafatic.evolution.controller.workflow.MediatedExportManager;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;

/**
 * Developer Engine implementation.
 * Autonomous coding Darwin instance specialized in working directly on a resolved target
 * (Git Repository, Project, or Folder) with a 5-phase workflow (Discover, Understand,
 * Implement, Verify, Complete) and publishing results to the shared Mediated output pipeline.
 */
public class DeveloperEngine extends CodingEngine {

	private final GitVersionControlProvider gitVcsProvider = new GitVersionControlProvider();
	private final DeveloperContextProvider contextProvider = new DeveloperContextProvider();

	public DeveloperEngine(TaskContext context, IterationMemoryService memoryService,
			SystemStateSignalProvider stateProvider) {
		super(context, memoryService, stateProvider);
		this.platformType = PlatformType.DEVELOPER;
	}

	public DeveloperContextProvider getContextProvider() {
		return contextProvider;
	}

	@Override
	public PlatformType getPlatformType() {
		return PlatformType.DEVELOPER;
	}

	@Override
	public OrchestratorResponse evolve(String request, IterationManager iterationManager,
			EvolutionAssessment initialAssessment) throws Exception {
		context.log("[DEVELOPER] Starting autonomous developer evolution for request: " + request);
		File targetRoot = resolveDevelopmentTarget();

		// Inject progressive task-oriented context
		String devContext = contextProvider.buildInitialContext(targetRoot, context, request);
		context.getOrchestrationState().getMetadata().put("projectStructure", devContext);
		context.log("[DEVELOPER] Injected progressive target context:\n" + devContext);

		// Execute core Darwin evolutionary loop
		OrchestratorResponse response = super.evolve(request, iterationManager, initialAssessment);

		// Publish changes to shared Mediated output pipeline
		publishToMediatedOutput(request, iterationManager);

		return response;
	}

	/**
	 * Resolves development target from session configuration or project root.
	 * Enforces explicit validation: never silently substitutes another directory or project root.
	 */
	public File resolveDevelopmentTarget() {
		File targetRoot = null;
		if (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null) {
			String sessionId = context.getSessionId();
			ChatSession session = context.getOrchestrator().getAiChat().getSessions().stream()
					.filter(s -> s != null && s.getId() != null && s.getId().equals(sessionId))
					.findFirst().orElse(null);
			if (session != null && session.getTargetPath() != null && !session.getTargetPath().trim().isEmpty()) {
				File configuredTarget = new File(session.getTargetPath().trim());
				if (!configuredTarget.exists()) {
					throw new IllegalStateException("Developer Target does not exist: " + configuredTarget.getAbsolutePath());
				}
				if (!configuredTarget.canRead()) {
					throw new IllegalStateException("Developer Target is not readable: " + configuredTarget.getAbsolutePath());
				}
				targetRoot = configuredTarget;
				context.setProjectRoot(targetRoot);
				context.log("[DEVELOPER] Resolved target from session configuration: " + targetRoot.getAbsolutePath()
						+ " (Type: " + session.getTargetType() + ")");
			}
		}

		if (targetRoot == null) {
			targetRoot = context.getProjectRoot();
		}

		if (targetRoot == null || !targetRoot.exists()) {
			throw new IllegalStateException("No valid development target configured or target does not exist: "
					+ (targetRoot != null ? targetRoot.getAbsolutePath() : "null"));
		}

		return targetRoot;
	}

	@Override
	public EvaluationResult runDarwinIteration(TaskContext context, IterationManager manager) throws Exception {
		File targetRoot = resolveDevelopmentTarget();
		context.log("[DEVELOPER] Executing autonomous iteration on target: " + targetRoot.getAbsolutePath());

		// -----------------------------------------------------------------
		// PHASE 1: DISCOVER - inspect target, tech stack, and architecture
		// -----------------------------------------------------------------
		EvolutionProgressPublisher.updateStage(context, EvolutionStage.ANALYZE_PARENT);
		context.log("[DEVELOPER][PHASE-1] Discovering target structure and architecture...");
		ClassificationResult classification = TargetClassificationDetector.detect(targetRoot);
		context.log("[DEVELOPER][PHASE-1] Target Category: " + classification.getCategory()
				+ ", Confidence: " + classification.getConfidence());

		TargetScanner scanner = new TargetScanner();
		TargetSnapshot.TargetType snapshotType = classification.getCategory();
		TargetSnapshot snapshot = scanner.scanToSnapshot(targetRoot, snapshotType);
		context.getOrchestrationState().getMetadata().put("developerSnapshot", snapshot);

		// Git safety inspection: Record pre-existing uncommitted changes so we never overwrite them
		List<String> preExistingChanges = Collections.emptyList();
		boolean isGit = EclipseGitEvoTool.isGitRepository(targetRoot);
		if (isGit) {
			try {
				preExistingChanges = gitVcsProvider.getChangedFiles(targetRoot, "HEAD");
				context.log("[DEVELOPER][GIT-SAFETY] Detected " + preExistingChanges.size()
						+ " pre-existing uncommitted changes. These will be preserved.");
			} catch (Exception e) {
				context.log("[DEVELOPER][GIT-SAFETY] Note: could not inspect git status: " + e.getMessage());
			}
		}
		context.getOrchestrationState().getMetadata().put("preExistingGitChanges", preExistingChanges);

		// -----------------------------------------------------------------
		// PHASE 2 & 3: UNDERSTAND & IMPLEMENT (Delegate to Darwin Loop)
		// -----------------------------------------------------------------
		context.log("[DEVELOPER][PHASE-2 & 3] Planning and implementing changes autonomously...");
		EvaluationResult evalResult = super.runDarwinIteration(context, manager);

		// -----------------------------------------------------------------
		// PHASE 4: VERIFY - Retrieve fresh diagnostics and run relevant checks
		// -----------------------------------------------------------------
		context.log("[DEVELOPER][PHASE-4] Verifying implementation via compiler diagnostics & tests...");
		try {
			// Retrieve fresh Java compiler diagnostics post-modification
			eu.kalafatic.evolution.controller.tools.ITool diagTool = eu.kalafatic.evolution.controller.tools.ToolFactory.getTool("java.get_diagnostics");
			if (diagTool != null) {
				try {
					String diagOut = diagTool.execute("", targetRoot, context);
					context.log("[DEVELOPER][PHASE-4] Fresh compiler diagnostics retrieved: " + diagOut);
					context.getOrchestrationState().getMetadata().put("latestCompilerDiagnostics", diagOut);
				} catch (Throwable dt) {
					context.log("[DEVELOPER][PHASE-4] Note: compiler diagnostics check: " + dt.getMessage());
				}
			}

			EvaluationResult buildTestResult = manager.getFitnessEngine().evaluate(targetRoot, context,
					eu.kalafatic.evolution.controller.orchestration.selfdev.RealityLevel.HEAVY);
			if (buildTestResult != null && !buildTestResult.isSuccess()) {
				String failureMsg = String.join("; ", buildTestResult.getErrors());
				context.log("[DEVELOPER][PHASE-4] Verification found build or test issues: " + failureMsg);
				if (evalResult != null) {
					evalResult.getErrors().addAll(buildTestResult.getErrors());
					evalResult.setSuccess(false);
					evalResult.setDecision(eu.kalafatic.evolution.model.orchestration.SelfDevDecision.CONTINUE);
				}
				// Feed verification failure directly back into the Darwin context for repair in next iteration
				context.getOrchestrationState().getMetadata().put("lastVerificationFailure", failureMsg);
				if (context.getOrchestrator() != null && context.getOrchestrator().getTasks() != null) {
					for (eu.kalafatic.evolution.model.orchestration.Task t : context.getOrchestrator().getTasks()) {
						if (t != null) {
							t.setFeedback("Verification failure requiring repair: " + failureMsg);
						}
					}
				}
			} else {
				context.log("[DEVELOPER][PHASE-4] Verification passed successfully.");
				context.getOrchestrationState().getMetadata().remove("lastVerificationFailure");
			}
		} catch (Throwable t) {
			context.log("[DEVELOPER][PHASE-4] Warning during verification: " + t.getMessage());
		}

		// -----------------------------------------------------------------
		// PHASE 5: COMPLETE - Report actual changes & preserve Git state
		// -----------------------------------------------------------------
		context.log("[DEVELOPER][PHASE-5] Completed development iteration.");
		return evalResult;
	}

	/**
	 * Publishes all changes into the shared Mediated output pipeline:
	 * generates unified ZIP export, syncs FileChangeTracker, registers with PeerReviewService,
	 * and allows immediate inspection in Mediated Diff View.
	 */
	public void publishToMediatedOutput(String request, IterationManager iterationManager) {
		try {
			File targetRoot = context.getProjectRoot();
			String sessionId = context.getSessionId();
			String outputPath = null;

			if (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null) {
				ChatSession session = context.getOrchestrator().getAiChat().getSessions().stream()
						.filter(s -> s != null && s.getId() != null && s.getId().equals(sessionId)).findFirst()
						.orElse(null);
				if (session != null) {
					outputPath = session.getOutputPath();
				}
			}

			context.log("[DEVELOPER] Publishing results to shared Mediated output pipeline...");
			MediatedExportManager exportManager = new MediatedExportManager();

			Object realityModelObj = context.getOrchestrationState().getMetadata().get("targetRealityModel");
			TargetRealityModel model = (realityModelObj instanceof TargetRealityModel)
					? (TargetRealityModel) realityModelObj : new TargetRealityModel();

			Map<String, FileChangeTracker.ChangeType> changes = context.getFileChangeTracker().getChangedFiles();
			model.getSelectedFiles().clear();
			model.getSelectedFiles().addAll(changes.keySet());

			File exportPackage = exportManager.createUnifiedExport(model, MediatedExportManager.ExportProfile.FULL,
					request, targetRoot, outputPath, sessionId);

			if (exportPackage != null && exportPackage.exists()) {
				try {
					String relativeZip = targetRoot.toPath().relativize(exportPackage.toPath()).toString().replace('\\', '/');
					context.getFileChangeTracker().recordChange(relativeZip, FileChangeTracker.ChangeType.NEW);
				} catch (Exception ex) {
					// External output directory on different root/drive: use canonical file name
					context.getFileChangeTracker().recordChange(exportPackage.getName(), FileChangeTracker.ChangeType.NEW);
				}

				// Register active review session in PeerReviewService so Diff View & ComparePage are populated immediately
				try {
					PeerReviewService.getInstance().createSession(targetRoot, "HEAD");
				} catch (Throwable t) {
					// Non-git or standalone fallback
				}

				// Refresh workspace resources if available
				try {
					org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot()
							.refreshLocal(org.eclipse.core.resources.IResource.DEPTH_INFINITE, null);
				} catch (Throwable t) {
					// Non-OSGi / headless environment
				}

				context.log("[DEVELOPER] Output published: " + exportPackage.getAbsolutePath()
						+ " (" + changes.size() + " modified files tracked)");
			} else {
				throw new IllegalStateException("Mediated export package creation returned null or unmaterialized file.");
			}
		} catch (Exception e) {
			context.log("[DEVELOPER] Warning during Mediated output publishing: " + e.getMessage());
		}
	}

	@Override
	public String getAgentInstructions() {
		return "Role: Professional Autonomous Software Development Engineer (Developer Mode).\n"
				+ "MANDATE & CORE WORKING METHODOLOGY:\n"
				+ "1. Understand the user's actual requirement before editing.\n"
				+ "2. Inspect the selected target rather than assume its architecture.\n"
				+ "3. Discover relevant classes, methods, dependencies and configuration.\n"
				+ "4. Understand existing implementation patterns and conventions.\n"
				+ "5. Identify the smallest correct implementation strategy.\n"
				+ "6. Reuse existing APIs and infrastructure.\n"
				+ "7. Preserve public contracts and unrelated functionality.\n"
				+ "8. Avoid unnecessary redesign and duplicate abstractions.\n"
				+ "9. Implement changes in the selected target.\n"
				+ "10. Use available tools to inspect and verify your work.\n"
				+ "11. Run relevant builds and tests.\n"
				+ "12. Analyze compiler errors and test failures.\n"
				+ "13. Correct your own implementation when verification fails.\n"
				+ "14. Report actual results rather than claim success without evidence.";
	}
}
