package eu.kalafatic.evolution.controller.orchestration.selfdev;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Chat Engine implementation. Inherits all core evolutionary loop
 * and state-machine logic from ADarwinEngine.
 */
public class ChatEngine extends ADarwinEngine {

	public ChatEngine(TaskContext context, IterationMemoryService memoryService,
			SystemStateSignalProvider stateProvider) {
		super(context, memoryService, stateProvider, PlatformType.SIMPLE_CHAT);
	}

	@Override
	protected String getAgentInstructions() {
		return "Role: Darwin Engine. Strategy: Lineage-driven evolutionary mutation.\n" + "EVOLUTIONARY MANDATE:\n"
				+ "- You are a materializer of architectural lineages.\n"
				+ "- You do NOT invent new dimensions or discover recursion depth.\n"
				+ "- You MUST materialize the EXACT blueprint provided by the orchestrator.\n"
				+ "- Preserve lineage continuity: every mutation MUST inherit from the surviving ancestor.\n"
				+ "- Address identified evolutionary pressures (reliability, extensibility, etc.) in your implementation.";
	}

	@Override
	protected String getFooterInstructions() {
		return "CRITICAL: Return a valid JSON object for the requested Darwin evolutionary trajectory.";
	}

	@Override
	public void setAiService(AiService aiService) {
		super.setAiService(aiService);
		rejectionAnalyzer.setAiService(aiService);
	}

	@Override
	public String getCapabilityId() {
		return "capability.mutation";
	}
}
