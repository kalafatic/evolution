package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.intent.EvolutionAssessment;
import eu.kalafatic.evolution.model.orchestration.ChatSession;

/**
 * Mediated Engine implementation.
 * Acts as a Local AI Prompt Engineering and Context Preparation Engine.
 *
 * Analyzes target repositories, local projects, or arbitrary filesystem directories,
 * transforms raw user requests into structured technical specifications, and generates
 * self-contained context packages for manual submission to external frontier AI models.
 */
public class MediatedEngine extends ADarwinEngine {

	public MediatedEngine(TaskContext context, IterationMemoryService memoryService,
			SystemStateSignalProvider stateProvider) {
		super(context, memoryService, stateProvider, PlatformType.HYBRID_MANUAL_EXPORT);
	}

	@Override
	public PlatformType getPlatformType() {
		return PlatformType.HYBRID_MANUAL_EXPORT;
	}

	@Override
	public OrchestratorResponse evolve(String request, IterationManager iterationManager,
			EvolutionAssessment initialAssessment) throws Exception {
		context.log("[MEDIATED] Starting Mediated Prompt & Context Preparation Engine for request: " + request);
		resolveMediatedTarget();
		return super.evolve(request, iterationManager, initialAssessment);
	}

	/**
	 * Resolves mediated target explicitly from session configuration or project root.
	 * Supports Git repositories, Eclipse projects, and arbitrary filesystem folders.
	 * Never silently falls back when an explicit target is invalid or inaccessible.
	 */
	public File resolveMediatedTarget() {
		File targetRoot = null;

		// 1. Check session configuration first (primary source)
		if (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null) {
			String sessionId = context.getSessionId();
			ChatSession session = context.getOrchestrator().getAiChat().getSessions().stream()
					.filter(s -> s != null && s.getId() != null && s.getId().equals(sessionId))
					.findFirst().orElse(null);

			if (session != null && session.getTargetPath() != null && !session.getTargetPath().trim().isEmpty()) {
				String rawPath = session.getTargetPath().trim();
				File candidate = new File(rawPath);
				if (!candidate.exists()) {
					throw new IllegalArgumentException(
							"Target directory does not exist or is inaccessible: " + rawPath);
				}
				if (!candidate.isDirectory()) {
					throw new IllegalArgumentException(
							"Configured target is not a directory: " + rawPath);
				}
				targetRoot = candidate;
				context.setProjectRoot(targetRoot);
				context.log("[MEDIATED] Resolved target from session configuration: " + targetRoot.getAbsolutePath()
						+ " (Configured Type: " + session.getTargetType() + ")");
				return targetRoot;
			}
		}

		// 2. Fall back to context.getProjectRoot() if no session target configured
		targetRoot = context.getProjectRoot();
		if (targetRoot == null || !targetRoot.exists() || !targetRoot.isDirectory()) {
			throw new IllegalStateException(
					"No valid target project or directory resolved for Mediated mode. Target path must be a valid directory.");
		}

		context.log("[MEDIATED] Using context project root as mediated target: " + targetRoot.getAbsolutePath());
		return targetRoot;
	}
}
