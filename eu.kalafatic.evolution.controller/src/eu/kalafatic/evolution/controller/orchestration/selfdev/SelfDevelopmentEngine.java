package eu.kalafatic.evolution.controller.orchestration.selfdev;

import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class SelfDevelopmentEngine extends ADarwinEngine {

	public SelfDevelopmentEngine(TaskContext context, IterationMemoryService memoryService,
			SystemStateSignalProvider stateProvider) {
		super(context, memoryService, stateProvider, PlatformType.SELF_DEV_MODE);
	}
}
