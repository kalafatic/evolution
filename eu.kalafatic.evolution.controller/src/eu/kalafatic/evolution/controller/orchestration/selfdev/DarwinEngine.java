package eu.kalafatic.evolution.controller.orchestration.selfdev;

import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Standard Darwin Engine implementation. Inherits all core evolutionary loop
 * and state-machine logic from ADarwinEngine.
 */
public class DarwinEngine extends ADarwinEngine {	

	public DarwinEngine(TaskContext context, IterationMemoryService memoryService,
			SystemStateSignalProvider stateProvider) {
		super(context, memoryService, stateProvider, PlatformType.DARWIN_MODE);
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
}
