package eu.kalafatic.evolution.controller.workflow;

import eu.kalafatic.evolution.controller.orchestration.evolution.EventCategory;

public enum RuntimeEventType {
    // 1. KERNEL (System Lifecycle)
    KERNEL_STARTED(EventCategory.KERNEL),
    KERNEL_SHUTDOWN(EventCategory.KERNEL),
    MODE_CHANGED(EventCategory.KERNEL),

    // 2. FLOW (Orchestration Control)
    FLOW_STARTED(EventCategory.FLOW),
    FLOW_ROUTED(EventCategory.FLOW),
    FLOW_PAUSED(EventCategory.FLOW),
    FLOW_COMPLETED(EventCategory.FLOW),
    ITERATION_STARTED(EventCategory.FLOW),
    ITERATION_COMPLETED(EventCategory.FLOW),

    // 3. AGENT (Reasoning Layer)
    HYPOTHESIS_GENERATED(EventCategory.AGENT),
    PLAN_SELECTED(EventCategory.AGENT),
    BRANCH_CREATED(EventCategory.AGENT),
    DECISION_UPDATED(EventCategory.AGENT),
    MUTATING(EventCategory.AGENT),
    MUTATION_REVIEW(EventCategory.AGENT),
    REASONING_STEP(EventCategory.AGENT),

    // 4. EXECUTION (Tools & Side Effects)
    TOOL_EXECUTION_STARTED(EventCategory.EXECUTION),
    TOOL_EXECUTION_SUCCEEDED(EventCategory.EXECUTION),
    TASK_STARTED(EventCategory.EXECUTION),
    TASK_COMPLETED(EventCategory.EXECUTION),
    TASK_FAILED(EventCategory.EXECUTION),
    FILE_WRITTEN(EventCategory.EXECUTION),
    COMMAND_FAILED(EventCategory.EXECUTION),

    // 5. UI (Presentation Layer)
    VIEW_UPDATED(EventCategory.UI),
    NODE_RENDERED(EventCategory.UI),
    USER_INTERACTION_RECEIVED(EventCategory.UI),
    EXPORT_READY(EventCategory.UI),

    // 6. SUPERVISOR (Governance Layer)
    ANOMALY_DETECTED(EventCategory.SUPERVISOR),
    POLICY_VIOLATION_DETECTED(EventCategory.SUPERVISOR),
    RESOURCE_LIMIT_APPROACHING(EventCategory.SUPERVISOR),
    RECOVERY_TRIGGERED(EventCategory.SUPERVISOR),
    SUPERVISOR_STATUS_CHANGED(EventCategory.SUPERVISOR),
    DEPLOYMENT_STATUS_CHANGED(EventCategory.SUPERVISOR),
    STEP_CREATED(EventCategory.SUPERVISOR),
    STEP_WAITING(EventCategory.SUPERVISOR),
    STEP_RESUMED(EventCategory.SUPERVISOR),
    STEP_COMPLETED(EventCategory.SUPERVISOR),
    STEP_FAILED(EventCategory.SUPERVISOR);

    private final EventCategory category;

    RuntimeEventType(EventCategory category) {
        this.category = category;
    }

    public EventCategory getCategory() {
        return category;
    }
}
