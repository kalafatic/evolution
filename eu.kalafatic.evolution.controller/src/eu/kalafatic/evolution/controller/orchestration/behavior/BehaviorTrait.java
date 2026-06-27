package eu.kalafatic.evolution.controller.orchestration.behavior;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

public enum BehaviorTrait {
    // SUPERVISION
    SUPERVISION_MEDIATED,
    SUPERVISION_AUTONOMOUS,

    // WORKFLOW
    WORKFLOW_SELF_DEV,
    WORKFLOW_TASK_ORIENTED,

    // REASONING
    REASONING_DARWIN_ITERATIVE,
    REASONING_ATOMIC,
    REASONING_CONSERVATIVE,
    REASONING_EXPLORATORY,
    REASONING_GROUNDED_RESEARCH,

    // EXECUTION
    EXECUTION_MANUAL,
    EXECUTION_AUTOMATIC,

    // INTERACTION
    INTERACTION_STEP_MODE,
    INTERACTION_STREAMING,

    // NEW TRAITS
    WORKFLOW_EXPORT_ONLY,
    SUPERVISION_STEP_LOCKED,
    WORKFLOW_HYBRID,

    // COGNITIVE
    COGNITIVE_SIMPLE_CHAT
}
