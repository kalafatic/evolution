package eu.kalafatic.evolution.controller.orchestration.selfdev;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * @deprecated Legacy duplicate of {@link CodingEngine}. Kept for binary and reflective compatibility.
 * All evolutionary operations are authoritatively handled by {@link CodingEngine} and {@link ADarwinEngine}.
 */
@Deprecated
public class CodingEngine2 extends CodingEngine {

	public CodingEngine2(TaskContext context, IterationMemoryService memoryService,
			SystemStateSignalProvider stateProvider) {
		super(context, memoryService, stateProvider);
	}
}
