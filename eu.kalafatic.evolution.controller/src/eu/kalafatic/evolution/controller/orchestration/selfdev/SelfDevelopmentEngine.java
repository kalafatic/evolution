package eu.kalafatic.evolution.controller.orchestration.selfdev;

import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Self Development Engine implementation. Inherits all core evolutionary loop
 * and state-machine logic from ADarwinEngine.
 */
public class SelfDevelopmentEngine extends ADarwinEngine {

	public SelfDevelopmentEngine(TaskContext context, IterationMemoryService memoryService,
			SystemStateSignalProvider stateProvider) {
		super(context, memoryService, stateProvider, PlatformType.SELF_DEV_MODE);
	}
}
