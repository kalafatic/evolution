package eu.kalafatic.evolution.controller.workflow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

public class WorkflowStepRegistry {
    private static final WorkflowStepRegistry instance = new WorkflowStepRegistry();
    private final Map<String, WorkflowStep> steps = new ConcurrentHashMap<>();
    private final Map<String, List<String>> sessionSteps = new ConcurrentHashMap<>();

    private WorkflowStepRegistry() {}

    public static WorkflowStepRegistry getInstance() { return instance; }

    public void registerStep(String sessionId, WorkflowStep step) {
        steps.put(step.getId(), step);
        sessionSteps.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(step.getId());
    }

    public WorkflowStep getStep(String stepId) {
        return steps.get(stepId);
    }

    public List<WorkflowStep> getStepsForSession(String sessionId) {
        List<WorkflowStep> result = new ArrayList<>();
        List<String> ids = sessionSteps.get(sessionId);
        if (ids != null) {
            for (String id : ids) {
                WorkflowStep step = steps.get(id);
                if (step != null) result.add(step);
            }
        }
        return result;
    }

    public WorkflowStep getActiveStepForSession(String sessionId) {
        List<WorkflowStep> sessionStepsList = getStepsForSession(sessionId);
        for (WorkflowStep step : sessionStepsList) {
            if (step.getStatus() == WorkflowStatus.ACTIVE || step.getStatus() == WorkflowStatus.WAITING_USER) {
                return step;
            }
        }
        return null;
    }

    public void clearSession(String sessionId) {
        List<String> ids = sessionSteps.remove(sessionId);
        if (ids != null) {
            for (String id : ids) steps.remove(id);
        }
    }

    public Map<String, List<String>> getSessionStepsMap() {
        return sessionSteps;
    }
}
