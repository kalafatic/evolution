package eu.kalafatic.evolution.controller.orchestration.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer.IntentResult;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.model.orchestration.AiMode;

/**
 * Central Mode Recognition Utility. Non-static to allow dependency injection of
 * SessionContainer.
 */
public class ModeRecognizer {

	private final SessionContainer sessionContainer;
	private final Map<String, PromptIntentAnalyzer> analyzerCache = new ConcurrentHashMap<>();

	public ModeRecognizer(SessionContainer sessionContainer) {
		this.sessionContainer = sessionContainer;
	}

	/**
	 * Determines the current mode.
	 */
	public String determineMode(TaskContext context) {
		if (isDreamMode(context)) {
			return "DREAM";
		}
		if (isForgeMode(context)) {
			return "FORGE";
		}
		if (isDeveloperMode(context)) {
			return "DEVELOPER";
		}
		if (isSelfDevMode(context)) {
			return "SELF_DEV";
		}
		if (isMediatedMode(context)) {
			return "MEDIATED";
		}
		if (isChatMode(context)) {
			return "CHAT";
		}
		return "STANDARD";
	}

	public static PlatformType determineType(IntentResult intentResult, TaskContext context) {
		// Explicit UI/context modes are NON-NEGOTIABLE and MUST take precedence over LLM/prompt analysis
		if (isDreamMode(context)) {
			return PlatformType.DREAM_MODE;
		}
		if (isMutationMode(context)) {
			return PlatformType.MUTATION;
		}
		if (isForgeMode(context)) {
			return PlatformType.FORGE;
		}
		if (isDeveloperMode(context)) {
			return PlatformType.DEVELOPER;
		}
		if (isSelfDevMode(context)) {
			return PlatformType.SELF_DEV_MODE;
		}
		if (isMediatedMode(context)) {
			return PlatformType.HYBRID_MANUAL_EXPORT;
		}
		if (isIntentMode(context)) {
			return PlatformType.INTENT_RECONSTRUCTION;
		}
		if (isDarwinMode(context)) {
			return PlatformType.DARWIN_MODE;
		}
		if (intentResult != null && intentResult.getReasoning() != null) {
			String reasoning = intentResult.getReasoning().toLowerCase();
			if (reasoning.contains("reconstruct intent") || reasoning.contains("discover intent")
					|| reasoning.contains("intent discovery") || reasoning.contains("reverse engineering")
					|| reasoning.contains("analyze architecture") || reasoning.contains("discover the project")) {
				return PlatformType.INTENT_RECONSTRUCTION;
			}
		}
		if (intentResult == null || intentResult.isChat() || intentResult.isUnknown()) {
			return PlatformType.SIMPLE_CHAT;
		}
		if (intentResult.isTask() && intentResult.getConfidence() >= 0.6) {
			return PlatformType.ASSISTED_CODING;
		}
		if (intentResult.isControl()) {
			return PlatformType.HYBRID_MANUAL_EXPORT;
		}
		// Safe fallback: Any malformed or unverified category maps to SIMPLE_CHAT
		return PlatformType.SIMPLE_CHAT;
	}

	public static PlatformType determineType(TaskContext context) {
		// Explicit UI/context modes are NON-NEGOTIABLE and MUST take precedence
		if (isDreamMode(context)) {
			return PlatformType.DREAM_MODE;
		}
		if (isMutationMode(context)) {
			return PlatformType.MUTATION;
		}
		if (isForgeMode(context)) {
			return PlatformType.FORGE;
		}
		if (isDeveloperMode(context)) {
			return PlatformType.DEVELOPER;
		}
		if (isSelfDevMode(context)) {
			return PlatformType.SELF_DEV_MODE;
		}
		if (isMediatedMode(context)) {
			return PlatformType.HYBRID_MANUAL_EXPORT;
		}
		if (isIntentMode(context)) {
			return PlatformType.INTENT_RECONSTRUCTION;
		}
		if (isDarwinMode(context)) {
			return PlatformType.DARWIN_MODE;
		}
		if (isCoding(context)) {
			return PlatformType.ASSISTED_CODING;
		}
		return PlatformType.SIMPLE_CHAT;
	}

	public static boolean isDreamMode(TaskContext context) {
		return (context.getOrchestrator() != null && AiMode.DREAM.equals(context.getOrchestrator().getAiMode()))
				|| (context.getPlatformMode() != null && PlatformType.DREAM_MODE.equals(context.getPlatformMode().getType()));
	}

	public static boolean isMutationMode(TaskContext context) {
		if (context == null) return false;
		String sid = context.getSessionId();
		if (sid != null && (sid.startsWith("mutation") || eu.kalafatic.evolution.controller.orchestration.mutation.MutationSessionManager.getInstance().getSession(sid) != null)) {
			return true;
		}
		return false;
	}

	public static boolean isForgeMode(TaskContext context) {
		if (context == null) return false;
		return (context.getOrchestrator() != null && AiMode.FORGE.equals(context.getOrchestrator().getAiMode()))
				|| (context.getPlatformMode() != null && PlatformType.FORGE.equals(context.getPlatformMode().getType()));
	}

	public static boolean isDeveloperMode(TaskContext context) {
		if (context == null) return false;
		return (context.getOrchestrator() != null && AiMode.DEVELOPER.equals(context.getOrchestrator().getAiMode()))
				|| (context.getPlatformMode() != null && PlatformType.DEVELOPER.equals(context.getPlatformMode().getType()));
	}

	public static boolean isSelfDevMode(TaskContext context) {
		if (context == null) return false;
		return (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null
				&& context.getOrchestrator().getAiChat().getPromptInstructions() != null
				&& context.getOrchestrator().getAiChat().getPromptInstructions().isSelfIterativeMode())
				|| (context.getPlatformMode() != null && PlatformType.SELF_DEV_MODE.equals(context.getPlatformMode().getType()))
				|| (context.getBehaviorProfile() != null && context.getBehaviorProfile().hasTrait(BehaviorTrait.WORKFLOW_SELF_DEV));
	}

	public static boolean isMediatedMode(TaskContext context) {
		if (context == null) return false;
		return (context.getOrchestrator() != null && AiMode.MEDIATED.equals(context.getOrchestrator().getAiMode()))
				|| (context.getPlatformMode() != null && PlatformType.HYBRID_MANUAL_EXPORT.equals(context.getPlatformMode().getType()))
				|| (context.getBehaviorProfile() != null && context.getBehaviorProfile().hasTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY));
	}

	public static boolean isIntentMode(TaskContext context) {
		if (context == null) return false;
		return (context.getOrchestrator() != null && AiMode.INTENT.equals(context.getOrchestrator().getAiMode()))
				|| (context.getPlatformMode() != null && PlatformType.INTENT_RECONSTRUCTION.equals(context.getPlatformMode().getType()));
	}

	public static boolean isDarwinMode(TaskContext context) {
		if (context == null) return false;
		return (context.getOrchestrator() != null && context.getOrchestrator().isDarwinMode())
				|| (context.getPlatformMode() != null && PlatformType.DARWIN_MODE.equals(context.getPlatformMode().getType()));
	}

	public static boolean isForcedTechnicalMode(TaskContext context) {
		if (context == null) return false;
		return isDreamMode(context) || isForgeMode(context) || isDeveloperMode(context) || isSelfDevMode(context) || isMediatedMode(context) || isIntentMode(context) || isMutationMode(context) || isDarwinMode(context) || isCoding(context);
	}

	public boolean isChatMode(TaskContext context) {
		if (isDreamMode(context)) return false;
		try {
			Object isChat = context.getOrchestrationState().getMetadata().get("isChatRequest");
			if (isChat instanceof Boolean) {
				return (Boolean) isChat;
			}

			Object isRepair = context.getOrchestrationState().getMetadata().get("isInternalRepair");
			if (isRepair instanceof Boolean && (Boolean) isRepair) {
				return true;
			}

			String rawInput = context.getOrchestrationState().getRawInput();
			if (rawInput == null || rawInput.trim().isEmpty()) {
				return true;
			}

			if (PromptIntentAnalyzer.isSimpleGreeting(rawInput)) {
				return true;
			}

			PromptIntentAnalyzer analyzer = getOrCreateAnalyzer(context);
			if (analyzer == null) {
				return true;
			}

			PromptIntentAnalyzer.IntentResult result = analyzer.analyze(rawInput, context);
			return result.isChat() || result.getConfidence() < 0.6;
		} catch (Exception e) {
			context.log("[ModeRecognizer] [CRITICAL] LLM analysis failed for chat mode detection: " + e.getMessage());
			String rawInput = context.getOrchestrationState().getRawInput();
			if (rawInput == null) return true;
			String lower = rawInput.toLowerCase();
			boolean isExplicitCoding = lower.contains("create class") || lower.contains("refactor") || lower.contains("implement") || lower.contains("fix bug");
			return !isExplicitCoding;
		}
	}

	private PromptIntentAnalyzer getOrCreateAnalyzer(TaskContext context) {
		String sessionId = context.getSessionId();
		PromptIntentAnalyzer analyzer = analyzerCache.get(sessionId);
		if (analyzer != null) {
			return analyzer;
		}

		try {
			java.io.File projectRoot = context.getProjectRoot();
			analyzer = new PromptIntentAnalyzer(sessionContainer, projectRoot);
			analyzer.setAiService(context.getAiService());
			analyzerCache.put(sessionId, analyzer);
			return analyzer;
		} catch (Exception e) {
			context.log("[ModeRecognizer] Failed to create analyzer: " + e.getMessage());
			return null;
		}
	}

	public static boolean isCoding(TaskContext context) {
		if (context == null) return false;
		return (context.getPlatformMode() != null && PlatformType.ASSISTED_CODING.equals(context.getPlatformMode().getType()));
	}

}
