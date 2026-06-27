package eu.kalafatic.evolution.controller.orchestration.behavior;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;

/**
 * Registry for behavior policies.
 */
public class BehaviorPolicyRegistry {
    private static final List<IBehaviorPolicy> policies = new ArrayList<>();

    static {
        // MEDIATED POLICY
        policies.add(new IBehaviorPolicy() {
            @Override public boolean applies(TaskContext context) {
                return context.getOrchestrator().getAiMode() == AiMode.MEDIATED;
            }
            @Override public void apply(BehaviorProfile profile, TaskContext context) {
                profile.addTrait(BehaviorTrait.SUPERVISION_MEDIATED);
                profile.addTrait(BehaviorTrait.EXECUTION_MANUAL);
                profile.addTrait(BehaviorTrait.REASONING_DARWIN_ITERATIVE);
                profile.addTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY);
            }
            @Override public long getBitOptions(TaskContext context) { return 0; }
        });

        // SELF-DEV POLICY
        policies.add(new IBehaviorPolicy() {
            @Override public boolean applies(TaskContext context) {
                return (context.getPlatformMode() != null && context.getPlatformMode().getType() == PlatformType.SELF_DEV_MODE);
            }
            @Override public void apply(BehaviorProfile profile, TaskContext context) {
                profile.addTrait(BehaviorTrait.WORKFLOW_SELF_DEV);
                profile.addTrait(BehaviorTrait.REASONING_EXPLORATORY);
            }
            @Override public long getBitOptions(TaskContext context) { return 0; }
        });

        // ASSISTED CODING POLICY
        policies.add(new IBehaviorPolicy() {
            @Override public boolean applies(TaskContext context) {
                return (context.getPlatformMode() != null && context.getPlatformMode().getType() == PlatformType.ASSISTED_CODING);
            }
            @Override public void apply(BehaviorProfile profile, TaskContext context) {
                profile.addTrait(BehaviorTrait.WORKFLOW_TASK_ORIENTED);
                profile.addTrait(BehaviorTrait.REASONING_CONSERVATIVE);
            }
            @Override public long getBitOptions(TaskContext context) { return 0; }
        });

        // DARWIN POLICY
        policies.add(new IBehaviorPolicy() {
            @Override public boolean applies(TaskContext context) {
                return context.getOrchestrator().isDarwinMode() ||
                       (context.getPlatformMode() != null && context.getPlatformMode().getType() == PlatformType.DARWIN_MODE);
            }
            @Override public void apply(BehaviorProfile profile, TaskContext context) {
                profile.addTrait(BehaviorTrait.REASONING_DARWIN_ITERATIVE);
                profile.addTrait(BehaviorTrait.REASONING_EXPLORATORY);
            }
            @Override public long getBitOptions(TaskContext context) { return 0; }
        });

        // SIMPLE CHAT POLICY
        policies.add(new IBehaviorPolicy() {
            @Override public boolean applies(TaskContext context) {
                return (context.getPlatformMode() != null && context.getPlatformMode().getType() == PlatformType.SIMPLE_CHAT);
            }
            @Override public void apply(BehaviorProfile profile, TaskContext context) {
                profile.addTrait(BehaviorTrait.COGNITIVE_SIMPLE_CHAT);
                profile.addTrait(BehaviorTrait.REASONING_ATOMIC);
                profile.addTrait(BehaviorTrait.WORKFLOW_TASK_ORIENTED);
            }
            @Override public long getBitOptions(TaskContext context) { return 0; }
        });

        // Add more policies dynamically here...
    }

    public static List<IBehaviorPolicy> getPolicies() {
        return policies;
    }
}
