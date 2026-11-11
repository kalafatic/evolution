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
	 */
	protected File resolveDevelopmentTarget() {
		File targetRoot = context.getProjectRoot();
		if (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null) {
			String sessionId = context.getSessionId();
			ChatSession session = context.getOrchestrator().getAiChat().getSessions().stream()
					.filter(s -> s != null && s.getId() != null && s.getId().equals(sessionId))
					.findFirst().orElse(null);
			if (session != null && session.getTargetPath() != null && !session.getTargetPath().trim().isEmpty()) {
				File configuredTarget = new File(session.getTargetPath().trim());
				if (configuredTarget.exists()) {
					targetRoot = configuredTarget;
					context.setProjectRoot(targetRoot);
					context.log("[DEVELOPER] Resolved target from session configuration: " + targetRoot.getAbsolutePath()
							+ " (Type: " + session.getTargetType() + ")");
				}
			}
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
				context.log("[DEVELOPER][PHASE-4] Verification found build or test issues: "
						+ String.join("; ", buildTestResult.getErrors()));
				if (evalResult != null) {
					evalResult.getErrors().addAll(buildTestResult.getErrors());
					evalResult.setSuccess(false);
				}
			} else {
				context.log("[DEVELOPER][PHASE-4] Verification passed successfully.");
			}
		} catch (Throwable t) {
			context.log("[DEVELOPER][PHASE-4] Warning during verification: " + t.getMessage());
		}

		// -----------------------------------------------------------------
		// PHASE 5: COMPLETE - Report actual changes
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

			String relativeZip = targetRoot.toPath().relativize(exportPackage.toPath()).toString().replace('\\', '/');
			context.getFileChangeTracker().recordChange(relativeZip, FileChangeTracker.ChangeType.NEW);

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
		} catch (Exception e) {
			context.log("[DEVELOPER] Warning during Mediated output publishing: " + e.getMessage());
		}
	}

	@Override
	public String getAgentInstructions() {
		return "Role: Darwin Autonomous Developer.\n"
				+ "MANDATE & ENGINEERING BEHAVIOR:\n"
				+ "1. BEFORE MODIFYING CODE:\n"
				+ "   - Understand the requested objective and scope before proposing any code changes.\n"
				+ "   - Inspect relevant existing implementation and directory structure before proposing changes.\n"
				+ "   - Use available Java/Eclipse tools to verify classes, methods, signatures, references, callers, and dependencies.\n"
				+ "   - Prefer existing project architecture, services, utilities, and coding conventions.\n"
				+ "   - Verify assumptions using available tools; avoid hallucinating APIs or structures.\n"
				+ "2. DURING IMPLEMENTATION:\n"
				+ "   - Make minimal, targeted changes directly addressing the goal.\n"
				+ "   - Preserve existing behavior and public contracts unless explicitly instructed otherwise.\n"
				+ "   - Avoid inventing classes, methods, signatures, APIs, or introducing unnecessary dependencies.\n"
				+ "   - Consider the impact of changes on dependent components and call hierarchies.\n"
				+ "   - Preserve unrelated pre-existing user changes and maintain strict Git safety.\n"
				+ "3. AFTER IMPLEMENTATION:\n"
				+ "   - Retrieve fresh compiler diagnostics after modifying Java code.\n"
				+ "   - Correct errors or compilation warnings introduced by the implementation.\n"
				+ "   - Use existing project build mechanisms and relevant tests for verification.\n"
				+ "   - Clearly distinguish verified facts from assumptions in reports.\n"
				+ "   - Report exactly what changed and publish changes to the shared change tracking and review pipeline.";
	}
}
