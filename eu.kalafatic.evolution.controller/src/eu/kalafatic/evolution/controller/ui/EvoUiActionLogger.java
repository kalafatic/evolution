package eu.kalafatic.evolution.controller.ui;

import java.util.Map;
import java.util.logging.Level;

import eu.kalafatic.evolution.controller.log.Log;

/**
 * Shared service for detailed, non-blocking UI action logging.
 */
public class EvoUiActionLogger {

    private static final EvoUiActionLogger INSTANCE = new EvoUiActionLogger();

    public static EvoUiActionLogger getInstance() {
        return INSTANCE;
    }

    private EvoUiActionLogger() {
    }

    public void logStart(EvoUiActionDescriptor action) {
        if (action == null) return;
        action.setStatus(EvoUiActionDescriptor.ActionStatus.ACTION_START);
        if (action.getStartTimeMs() <= 0) {
            action.setStartTimeMs(System.currentTimeMillis());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n[UI][INFO][ACTION_START]\n");
        sb.append("control=").append(action.getControlId()).append("\n");
        if (!action.getLabel().isBlank()) {
            sb.append("label=").append(action.getLabel()).append("\n");
        }
        sb.append("component=").append(action.getComponentName()).append("\n");
        if (action.getSessionId() != null && !action.getSessionId().isBlank()) {
            sb.append("session=").append(action.getSessionId()).append("\n");
        }
        sb.append("processId=").append(action.getProcessId()).append("\n");
        sb.append("action=").append(action.getOperationName()).append("\n");
        if (action.getTargetPath() != null && !action.getTargetPath().isBlank()) {
            sb.append("target=").append(action.getTargetPath()).append("\n");
        }
        if (action.getMethodName() != null && !action.getMethodName().isBlank()) {
            sb.append("operation=").append(action.getMethodName()).append("\n");
        }
        if (action.getOldValue() != null || action.getNewValue() != null) {
            sb.append("oldValue=").append(action.getOldValue() != null ? action.getOldValue() : "").append("\n");
            sb.append("newValue=").append(action.getNewValue() != null ? action.getNewValue() : "").append("\n");
        }
        if (!action.getParameters().isEmpty()) {
            sb.append("parameters=").append(formatParameters(action.getParameters())).append("\n");
        }
        sb.append("trigger=").append(action.getTrigger());

        Log.log(action.getSessionId(), Level.INFO, sb.toString(), null);
    }

    public void logSuccess(EvoUiActionDescriptor action, String resultSummary) {
        if (action == null) return;
        action.setStatus(EvoUiActionDescriptor.ActionStatus.ACTION_SUCCESS);
        long now = System.currentTimeMillis();
        long start = action.getStartTimeMs();
        long duration = (start > 0 && now >= start) ? (now - start) : 0;
        action.setDurationMs(duration);
        action.setResultSummary(resultSummary);

        StringBuilder sb = new StringBuilder();
        sb.append("\n[UI][INFO][ACTION_SUCCESS]\n");
        sb.append("control=").append(action.getControlId()).append("\n");
        sb.append("action=").append(action.getOperationName()).append("\n");
        sb.append("processId=").append(action.getProcessId()).append("\n");
        if (action.getTargetPath() != null && !action.getTargetPath().isBlank()) {
            sb.append("target=").append(action.getTargetPath()).append("\n");
        }
        sb.append("durationMs=").append(duration).append("\n");
        if (resultSummary != null && !resultSummary.isBlank()) {
            sb.append("result=").append(EvoUiActionDescriptor.sanitizeValue("result", resultSummary));
        }

        Log.log(action.getSessionId(), Level.INFO, sb.toString(), null);
    }

    public void logFailure(EvoUiActionDescriptor action, Throwable exception, String customMessage) {
        if (action == null) return;
        action.setStatus(EvoUiActionDescriptor.ActionStatus.ACTION_FAILED);
        long now = System.currentTimeMillis();
        long start = action.getStartTimeMs();
        long duration = (start > 0 && now >= start) ? (now - start) : 0;
        action.setDurationMs(duration);
        action.setException(exception);

        StringBuilder sb = new StringBuilder();
        sb.append("\n[UI][ERROR][ACTION_FAILED]\n");
        sb.append("control=").append(action.getControlId()).append("\n");
        sb.append("action=").append(action.getOperationName()).append("\n");
        sb.append("processId=").append(action.getProcessId()).append("\n");
        if (action.getTargetPath() != null && !action.getTargetPath().isBlank()) {
            sb.append("target=").append(action.getTargetPath()).append("\n");
        }
        sb.append("durationMs=").append(duration).append("\n");
        if (customMessage != null && !customMessage.isBlank()) {
            sb.append("message=").append(EvoUiActionDescriptor.sanitizeValue("message", customMessage)).append("\n");
        }
        if (exception != null) {
            String excMsg = exception.getMessage() != null ? exception.getMessage() : "";
            sb.append("exception=").append(exception.getClass().getName()).append(": ")
              .append(EvoUiActionDescriptor.sanitizeValue("exception", excMsg));
        }

        Log.log(action.getSessionId(), Level.SEVERE, sb.toString(), exception);
    }

    public void logSingleInteraction(EvoUiActionDescriptor action, String outcome) {
        if (action == null) return;
        action.setStatus(EvoUiActionDescriptor.ActionStatus.ACTION_SUCCESS);
        action.setResultSummary(outcome);

        StringBuilder sb = new StringBuilder();
        sb.append("\n[UI][INFO][ACTION_INTERACTION]\n");
        sb.append("control=").append(action.getControlId()).append("\n");
        if (!action.getLabel().isBlank()) {
            sb.append("label=").append(action.getLabel()).append("\n");
        }
        sb.append("component=").append(action.getComponentName()).append("\n");
        sb.append("action=").append(action.getOperationName()).append("\n");
        if (action.getOldValue() != null || action.getNewValue() != null) {
            sb.append("oldValue=").append(action.getOldValue() != null ? action.getOldValue() : "").append("\n");
            sb.append("newValue=").append(action.getNewValue() != null ? action.getNewValue() : "").append("\n");
        }
        if (outcome != null && !outcome.isBlank()) {
            sb.append("result=").append(EvoUiActionDescriptor.sanitizeValue("result", outcome)).append("\n");
        }
        sb.append("trigger=").append(action.getTrigger());

        Log.log(action.getSessionId(), Level.INFO, sb.toString(), null);
    }

    private String formatParameters(Map<String, String> parameters) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append("=").append(EvoUiActionDescriptor.sanitizeValue(entry.getKey(), entry.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
