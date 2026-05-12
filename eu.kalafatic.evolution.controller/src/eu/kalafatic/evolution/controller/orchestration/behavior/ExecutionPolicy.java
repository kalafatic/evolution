package eu.kalafatic.evolution.controller.orchestration.behavior;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured semantic orchestration policy.
 */
public class ExecutionPolicy {
    public enum ExecutionMode { LOCAL, HYBRID, REMOTE, PROXY, MEDIATED }
    public enum SupervisionLevel { AUTO, MANUAL, HYBRID }
    public enum InteractionMode { CONTINUOUS, STEP, GUIDED }
    public enum ReasoningStrategy { ATOMIC, DARWIN, ITERATIVE, CONSERVATIVE, EXPLORATORY, ANALYTICAL }
    public enum WorkflowModel { TASK_ORIENTED, SELF_DEV, HYBRID, EXPORT_ONLY }
    public enum RepositoryMode { ISOLATED, SHARED, VIRTUAL }

    private ExecutionMode executionMode;
    private SupervisionLevel supervisionLevel;
    private InteractionMode interactionMode;
    private ReasoningStrategy reasoningStrategy;
    private WorkflowModel workflowModel;
    private RepositoryMode repositoryMode = RepositoryMode.ISOLATED;
    private double explorationLevel = 0.5;
    private final List<String> constraints = new ArrayList<>();

    public ExecutionMode getExecutionMode() { return executionMode; }
    public void setExecutionMode(ExecutionMode executionMode) { this.executionMode = executionMode; }

    public SupervisionLevel getSupervisionLevel() { return supervisionLevel; }
    public void setSupervisionLevel(SupervisionLevel supervisionLevel) { this.supervisionLevel = supervisionLevel; }

    public InteractionMode getInteractionMode() { return interactionMode; }
    public void setInteractionMode(InteractionMode interactionMode) { this.interactionMode = interactionMode; }

    public ReasoningStrategy getReasoningStrategy() { return reasoningStrategy; }
    public void setReasoningStrategy(ReasoningStrategy reasoningStrategy) { this.reasoningStrategy = reasoningStrategy; }

    public WorkflowModel getWorkflowModel() { return workflowModel; }
    public void setWorkflowModel(WorkflowModel workflowModel) { this.workflowModel = workflowModel; }

    public RepositoryMode getRepositoryMode() { return repositoryMode; }
    public void setRepositoryMode(RepositoryMode repositoryMode) { this.repositoryMode = repositoryMode; }

    public double getExplorationLevel() { return explorationLevel; }
    public void setExplorationLevel(double explorationLevel) { this.explorationLevel = explorationLevel; }

    public List<String> getConstraints() { return constraints; }
    public void addConstraint(String constraint) { this.constraints.add(constraint); }
}
