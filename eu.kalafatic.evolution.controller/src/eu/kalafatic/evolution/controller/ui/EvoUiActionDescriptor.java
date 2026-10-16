package eu.kalafatic.evolution.controller.ui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Unified descriptor model capturing metadata and runtime context of UI control interactions.
 */
public class EvoUiActionDescriptor {

    public enum TriggerType {
        USER_CLICK,
        VALUE_CHANGE,
        SELECTION,
        DOUBLE_CLICK,
        CONFIRMATION,
        CANCEL,
        SHORTCUT,
        HTML_INTERACTION,
        SYSTEM_EVENT
    }

    public enum ActionStatus {
        NOT_STARTED,
        ACTION_START,
        IN_PROGRESS,
        ACTION_SUCCESS,
        ACTION_FAILED,
        ACTION_CANCELLED
    }

    private String controlId;
    private String label;
    private String controlType;
    private String componentName;
    private String operationName;
    private String methodName;
    private String targetPath;
    private Map<String, String> parameters = new LinkedHashMap<>();
    private String oldValue;
    private String newValue;
    private TriggerType trigger = TriggerType.USER_CLICK;
    private ActionStatus status = ActionStatus.NOT_STARTED;
    private String sessionId;
    private String processId = UUID.randomUUID().toString();
    private long startTimeMs = System.currentTimeMillis();
    private long durationMs = -1;
    private String resultSummary;
    private Throwable exception;

    public EvoUiActionDescriptor() {
    }

    public EvoUiActionDescriptor(String controlId, String label, String componentName, String operationName) {
        this.controlId = controlId;
        this.label = label;
        this.componentName = componentName;
        this.operationName = operationName;
    }

    public String getControlId() {
        return controlId != null ? controlId : (label != null ? label.toLowerCase().replaceAll("\\s+", "_") : "control");
    }

    public EvoUiActionDescriptor setControlId(String controlId) {
        this.controlId = controlId;
        return this;
    }

    public String getLabel() {
        return label != null ? label : "";
    }

    public EvoUiActionDescriptor setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getControlType() {
        return controlType != null ? controlType : "Control";
    }

    public EvoUiActionDescriptor setControlType(String controlType) {
        this.controlType = controlType;
        return this;
    }

    public String getComponentName() {
        return componentName != null ? componentName : "UIComponent";
    }

    public EvoUiActionDescriptor setComponentName(String componentName) {
        this.componentName = componentName;
        return this;
    }

    public String getOperationName() {
        return operationName != null ? operationName : "execute";
    }

    public EvoUiActionDescriptor setOperationName(String operationName) {
        this.operationName = operationName;
        return this;
    }

    public String getMethodName() {
        return methodName;
    }

    public EvoUiActionDescriptor setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public EvoUiActionDescriptor setTargetPath(String targetPath) {
        this.targetPath = targetPath;
        return this;
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public EvoUiActionDescriptor addParameter(String key, String value) {
        if (key != null) {
            String sanitizedValue = sanitizeValue(key, value);
            this.parameters.put(key, sanitizedValue);
        }
        return this;
    }

    public EvoUiActionDescriptor setParameters(Map<String, String> params) {
        this.parameters.clear();
        if (params != null) {
            params.forEach(this::addParameter);
        }
        return this;
    }

    public String getOldValue() {
        return oldValue;
    }

    public EvoUiActionDescriptor setOldValue(String oldValue) {
        this.oldValue = oldValue;
        return this;
    }

    public String getNewValue() {
        return newValue;
    }

    public EvoUiActionDescriptor setNewValue(String newValue) {
        this.newValue = newValue;
        return this;
    }

    public TriggerType getTrigger() {
        return trigger;
    }

    public EvoUiActionDescriptor setTrigger(TriggerType trigger) {
        if (trigger != null) {
            this.trigger = trigger;
        }
        return this;
    }

    public ActionStatus getStatus() {
        return status;
    }

    public EvoUiActionDescriptor setStatus(ActionStatus status) {
        if (status != null) {
            this.status = status;
        }
        return this;
    }

    public String getSessionId() {
        return sessionId;
    }

    public EvoUiActionDescriptor setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public String getProcessId() {
        return processId;
    }

    public EvoUiActionDescriptor setProcessId(String processId) {
        this.processId = processId;
        return this;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public EvoUiActionDescriptor setStartTimeMs(long startTimeMs) {
        this.startTimeMs = startTimeMs;
        return this;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public EvoUiActionDescriptor setDurationMs(long durationMs) {
        this.durationMs = durationMs;
        return this;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public EvoUiActionDescriptor setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
        return this;
    }

    public Throwable getException() {
        return exception;
    }

    public EvoUiActionDescriptor setException(Throwable exception) {
        this.exception = exception;
        return this;
    }

    public EvoUiActionDescriptor copy() {
        EvoUiActionDescriptor copy = new EvoUiActionDescriptor();
        copy.controlId = this.controlId;
        copy.label = this.label;
        copy.controlType = this.controlType;
        copy.componentName = this.componentName;
        copy.operationName = this.operationName;
        copy.methodName = this.methodName;
        copy.targetPath = this.targetPath;
        copy.parameters = new LinkedHashMap<>(this.parameters);
        copy.oldValue = this.oldValue;
        copy.newValue = this.newValue;
        copy.trigger = this.trigger;
        copy.status = this.status;
        copy.sessionId = this.sessionId;
        copy.processId = this.processId;
        copy.startTimeMs = this.startTimeMs;
        copy.durationMs = this.durationMs;
        copy.resultSummary = this.resultSummary;
        copy.exception = this.exception;
        return copy;
    }

    public static String sanitizeValue(String key, String value) {
        if (value == null) return null;
        if (key != null) {
            String lowerKey = key.toLowerCase();
            if (lowerKey.contains("pass") || lowerKey.contains("token") || lowerKey.contains("secret") || lowerKey.contains("key")) {
                return "******";
            }
        }
        if (value.length() > 200) {
            return value.substring(0, 197) + "...";
        }
        return value;
    }
}
