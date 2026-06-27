package eu.kalafatic.evolution.controller.orchestration.behavior;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Interface for behavior policies that dynamically determine traits based on context signals.
 */
public interface IBehaviorPolicy {
    boolean applies(TaskContext context);
    void apply(BehaviorProfile profile, TaskContext context);
    long getBitOptions(TaskContext context);
}
