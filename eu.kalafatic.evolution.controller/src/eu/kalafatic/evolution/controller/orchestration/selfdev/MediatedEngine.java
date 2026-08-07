package eu.kalafatic.evolution.controller.orchestration.selfdev;

import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Mediated Engine implementation. Inherits all core evolutionary loop
 * and state-machine logic from ADarwinEngine.
 */
public class MediatedEngine extends ADarwinEngine {

	public MediatedEngine(TaskContext context, IterationMemoryService memoryService,
			SystemStateSignalProvider stateProvider) {
		super(context, memoryService, stateProvider, PlatformType.HYBRID_MANUAL_EXPORT);
	}
}
