package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityAnalysis;
import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;
import eu.kalafatic.evolution.controller.orchestration.cognitive.CognitiveAnalysisPipeline;
import eu.kalafatic.evolution.controller.orchestration.cognitive.CognitiveStateEngine;
import eu.kalafatic.evolution.controller.orchestration.cognitive.SessionCognitiveState;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Routes execution based on detected or assigned PlatformMode. Maps
 * PlatformMode to concrete IOrchestrationFlow implementations.
 */
public class ModeRouter {

	private final CognitiveStateEngine cognitiveStateEngine = new CognitiveStateEngine();

	/**
	 * Resolves the appropriate orchestration flow based on the platform mode.
	 */
	public IOrchestrationFlow resolveFlow(PlatformMode mode, AiService aiService, IterationManager manager) {
		String sessionId = manager.getContext().getSessionId();
		SessionContainer session = SessionManager.getInstance().getSession(sessionId);
		if (mode == null) {
			if (session != null) {
				return (IOrchestrationFlow) session.getAgentRegistry()
						.get(eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants.AGENT_GENERAL);
			}
			return null; // Should not happen with session isolation
		}

		switch (mode.getType()) {
		case DARWIN_MODE:
			return new DarwinFlow(aiService, manager);
		case SELF_DEV_MODE:
			return new DarwinFlow(aiService, manager);
		case INTENT_RECONSTRUCTION:
			return new DarwinFlow(aiService, manager);
		case HYBRID_MANUAL_EXPORT:
			// Mediated Mode is now handled by DarwinFlow for iterative cognitive evolution
			return new DarwinFlow(aiService, manager);
		case ASSISTED_CODING:
			return new DarwinFlow(aiService, manager);
		case SIMPLE_CHAT:
		default:
			if (session != null) {
				return (IOrchestrationFlow) session.getAgentRegistry()
						.get(eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants.AGENT_GENERAL);
			}
			return null;
		}
	}

	/**
	 * Detects or assigns PlatformMode based on user input and orchestrator state.
	 */
	public PlatformMode route(String prompt, Orchestrator orchestrator) {
		return route(prompt, orchestrator, null);
	}

	/**
	 * Attempts to determine the mode using the cognitive pipeline without session
	 * state.
	 * 
	 * @return PlatformMode determined by the pipeline or explicit overrides.
	 */
	public PlatformMode routeFast(String prompt, Orchestrator orchestrator) {
		if (prompt == null)
			prompt = "";
		String lower = prompt.toLowerCase().trim();

		// 1. Try explicit overrides (Manual Routing)
		if (lower.contains("mode: chat"))
			return createSimpleChatMode();
		if (lower.contains("mode: assisted"))
			return createAssistedCodingMode();
		if (lower.contains("mode: darwin"))
			return createDarwinMode();
		if (lower.contains("mode: self-dev"))
			return createSelfDevMode();
		if (lower.contains("mode: intent-reconstruction") || lower.contains("reconstruct intent"))
			return createIntentReconstructionMode();
		if (lower.contains("mode: mediated") || lower.contains("analyze target"))
			return createHybridManualExportMode();

		// 2. Use the cognitive pipeline for evidence-based detection
		CognitiveAnalysisPipeline pipeline = new CognitiveAnalysisPipeline();
		CapabilityAnalysis analysis = pipeline.analyze(prompt);

		if (analysis == null || analysis.getWinner() == null) {
			return createSimpleChatMode();
		}

		return mapToPlatformMode(analysis.getWinner().getCapability());
	}

	/**
	 * Detects or assigns PlatformMode based on user input, orchestrator state, and
	 * optional context assist result. Priority: 1. Explicit Prompt Overrides (e.g.
	 * "mode: chat") 2. Explicit Orchestrator Model overrides (User UI selection) 3.
	 * Session Cognitive State (grounded in history) 4. Pipeline Analysis Fallback
	 */
	public PlatformMode route(String prompt, Orchestrator orchestrator, ContextAssistResult assistResult) {
		if (prompt == null)
			prompt = "";
		String lower = prompt.toLowerCase().trim();

		// 1. Check explicit prompt overrides first (e.g. "mode: chat")
		if (lower.contains("mode: chat"))
			return createSimpleChatMode();
		if (lower.contains("mode: assisted"))
			return createAssistedCodingMode();
		if (lower.contains("mode: darwin"))
			return createDarwinMode();
		if (lower.contains("mode: self-dev"))
			return createSelfDevMode();
		if (lower.contains("mode: mediated") || lower.contains("analyze target"))
			return createHybridManualExportMode();

		// 2. Check explicit Orchestrator model overrides (User UI selection)
		if (orchestrator != null) {
			if (orchestrator.getAiMode() == eu.kalafatic.evolution.model.orchestration.AiMode.MEDIATED) {
				// FORCE COGNITIVE SYNC: Ensure explicit UI selection updates the cognitive state
				String sessionId = orchestrator.getId();
				SessionContainer session = (sessionId != null) ? SessionManager.getInstance().getSession(sessionId) : null;
				if (session != null) {
					session.getCognitiveState().setCurrentCapability(CapabilityType.MEDIATED);
					new eu.kalafatic.evolution.controller.orchestration.cognitive.CognitiveStatePublisher().publish(null, session.getCognitiveState());
				}
				return createHybridManualExportMode();
			}
			if (orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null
					&& orchestrator.getAiChat().getPromptInstructions().isSelfIterativeMode()) {
				return createSelfDevMode();
			}
			if (orchestrator.isDarwinMode()) {
				return createDarwinMode();
			}
		}

		// 3. Process through Cognitive State Engine if session exists (Conversation
		// Trajectory)
		String sessionId = (orchestrator != null) ? orchestrator.getId() : null;
		SessionContainer session = (sessionId != null) ? SessionManager.getInstance().getSession(sessionId) : null;
		if (session != null) {
			SessionCognitiveState cogState = session.getCognitiveState();

			// Resolve TaskContext for publisher support if available
			TaskContext context = (session instanceof SessionContext) ? ((SessionContext) session).getTaskContext()
					: null;
			if (context == null && orchestrator != null) {
				// Heuristic context for routing-only interactions
				context = new TaskContext(orchestrator, new java.io.File("."));
				context.setSessionId(sessionId);
			}

			cognitiveStateEngine.processInteraction(prompt, cogState, context, assistResult);

			// Routing is a consequence of cognitive state
			return mapToPlatformMode(cogState.getCurrentCapability());
		}

		// 4. Fallback to Pipeline analysis (Non-Session Path)
		return routeFast(prompt, orchestrator);
	}

	private PlatformMode mapToPlatformMode(CapabilityType capability) {
		switch (capability) {
		case SELF_DEV:
			return createSelfDevMode();
		case INTENT_RECONSTRUCTION:
			return createIntentReconstructionMode();
		case EVOLUTION:
			return createDarwinMode();
		case ARCHITECTURE:
		case MEDIATED:
			return createHybridManualExportMode();
		case CODE:
			return createAssistedCodingMode();
		case CHAT:
		default:
			return createSimpleChatMode();
		}
	}

	private PlatformMode createSimpleChatMode() {
		return new PlatformMode(PlatformType.SIMPLE_CHAT, AutonomyLevel.LOW, 1, false);
	}

	private PlatformMode createAssistedCodingMode() {
		return new PlatformMode(PlatformType.ASSISTED_CODING, AutonomyLevel.LOW, 2, false);
	}

	private PlatformMode createDarwinMode() {
		return new PlatformMode(PlatformType.DARWIN_MODE, AutonomyLevel.MEDIUM, 3, false);
	}

	private PlatformMode createSelfDevMode() {
		PlatformMode mode = new PlatformMode(PlatformType.SELF_DEV_MODE, AutonomyLevel.HIGH, 5, true);
		mode.getAllowedPaths().add("eu.kalafatic.evolution.controller/src");
		mode.getAllowedPaths().add("eu.kalafatic.evolution.view/src");
		mode.getAllowedPaths().add("eu.kalafatic.evolution.model/src");
		return mode;
	}

	private PlatformMode createHybridManualExportMode() {
		return new PlatformMode(PlatformType.HYBRID_MANUAL_EXPORT, AutonomyLevel.LOW, 1, false);
	}

	private PlatformMode createIntentReconstructionMode() {
		return new PlatformMode(PlatformType.INTENT_RECONSTRUCTION, AutonomyLevel.MEDIUM, 3, false);
	}

}
