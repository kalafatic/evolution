package eu.kalafatic.evolution.controller.orchestration.adapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.ContextProcessTracker;
import eu.kalafatic.evolution.controller.orchestration.ContextProcessTracker.ProcessInfo;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.workflow.WorkflowStep;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Adapter for general workflow and step-based execution processes.
 * Normalizes workflow steps into ProcessUnitRecord objects for tab visualization.
 */
public class WorkflowProcessAdapter implements IProcessAdapter {

    private final String sessionId;
    private final Orchestrator orchestrator;

    public WorkflowProcessAdapter(String sessionId, Orchestrator orchestrator) {
        this.sessionId = sessionId != null ? sessionId : "Default";
        this.orchestrator = orchestrator;
    }

    @Override
    public String getProcessType() {
        return "WORKFLOW_STEP";
    }

    @Override
    public String getProcessName() {
        return "Self-Dev Workflow Execution";
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String getProcessStatus() {
        ProcessInfo proc = ContextProcessTracker.getInstance().getProcessState(sessionId);
        if (proc != null) {
            return proc.getState();
        }
        return "READY";
    }

    @Override
    public String getCurrentPhase() {
        ProcessInfo proc = ContextProcessTracker.getInstance().getProcessState(sessionId);
        if (proc != null) {
            return proc.getPhase();
        }
        return "IDLE";
    }

    @Override
    public double getProgressFraction() {
        ProcessInfo proc = ContextProcessTracker.getInstance().getProcessState(sessionId);
        if (proc != null) {
            return proc.getProgressFraction();
        }
        return 0.0;
    }

    @Override
    public String getUnitSelectorLabel() {
        return "Select Step:";
    }

    @Override
    public List<ProcessUnitRecord> getUnits() {
        List<ProcessUnitRecord> unitList = new ArrayList<>();
        SessionContainer container = SessionManager.getInstance().getSession(sessionId);

        List<WorkflowStep> steps = (container != null && container.getWorkflowRegistry() != null)
                ? container.getWorkflowRegistry().getStepsForSession(sessionId)
                : new ArrayList<>();

        if (!steps.isEmpty()) {
            for (int i = 0; i < steps.size(); i++) {
                WorkflowStep step = steps.get(i);
                int stepNum = i + 1;

                ProcessUnitRecord unit = new ProcessUnitRecord(stepNum, "Step " + stepNum + ": " + step.getName());
                unit.setUnitType("WORKFLOW_STEP");
                unit.setGoal(step.getCommand() != null ? step.getCommand() : step.getName());
                unit.setStatus(step.getStatus() != null ? step.getStatus().name() : "READY");
                unit.setResult(step.getStatus() != null ? step.getStatus().name() : "READY");
                unit.setFlowSteps(getFlowStepLabels());

                if (step.getFailureDetails() != null) {
                    unit.setErrorMessage(step.getFailureDetails());
                }

                int flowIdx = determineFlowIndex(step.getName());
                unit.setActiveFlowStepIndex(flowIdx);

                ProcessUnitRecord.BranchRecord branch = new ProcessUnitRecord.BranchRecord(
                        "step-" + stepNum,
                        step.getName(),
                        unit.getResult(),
                        "COMPLETED".equals(unit.getResult()) ? 1.0 : 0.0
                );
                unit.setBranches(Arrays.asList(branch));

                unitList.add(unit);
            }
        } else {
            ProcessUnitRecord defaultUnit = new ProcessUnitRecord(1, "Step 1: Workflow Execution");
            defaultUnit.setUnitType("WORKFLOW_STEP");
            defaultUnit.setGoal("Execute session task");
            defaultUnit.setStatus(getProcessStatus());
            defaultUnit.setResult(getProcessStatus());
            defaultUnit.setFlowSteps(getFlowStepLabels());
            defaultUnit.setActiveFlowStepIndex(0);

            ProcessUnitRecord.BranchRecord branch = new ProcessUnitRecord.BranchRecord("step-1", "Task Run", getProcessStatus(), 0.0);
            defaultUnit.setBranches(Arrays.asList(branch));

            unitList.add(defaultUnit);
        }

        return unitList;
    }

    private int determineFlowIndex(String stepName) {
        if (stepName == null) return 0;
        String upper = stepName.toUpperCase();
        if (upper.contains("INIT") || upper.contains("PLAN")) return 0;
        if (upper.contains("BUILD") || upper.contains("CODE")) return 1;
        if (upper.contains("TEST") || upper.contains("VERIFY")) return 2;
        if (upper.contains("EVAL") || upper.contains("SCORE")) return 3;
        if (upper.contains("SELECT") || upper.contains("DECIDE")) return 4;
        if (upper.contains("DEPLOY") || upper.contains("INTEGRATE") || upper.contains("MERGE")) return 5;
        return 1;
    }

    @Override
    public List<String> getFlowStepLabels() {
        return Arrays.asList("INIT", "BUILD", "TEST", "EVALUATE", "DECIDE", "DEPLOY");
    }

    @Override
    public String getSummary() {
        return String.format("Workflow Process - Status: %s, Progress: %.0f%%",
                getProcessStatus(), getProgressFraction() * 100);
    }
}
