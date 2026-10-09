package eu.kalafatic.evolution.controller.orchestration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventListener;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

/**
 * Listens to existing RuntimeEventBus events to maintain session-scoped live process awareness.
 * Does not create parallel event publishers or global registries.
 */
public class ContextProcessTracker implements RuntimeEventListener {

    private static final ContextProcessTracker INSTANCE = new ContextProcessTracker();

    public static ContextProcessTracker getInstance() {
        return INSTANCE;
    }

    public static class ProcessInfo {
        private final String processId;
        private final String processName;
        private final String sessionId;
        private volatile String state;
        private volatile String phase;
        private final long startTimestamp;
        private volatile long lastTransitionTimestamp;
        private volatile double progressFraction;
        private volatile String lastStatus;
        private volatile String failureDetails;
        private volatile String correlatedResource;

        public ProcessInfo(String processId, String processName, String sessionId, String state, String phase) {
            this.processId = processId != null ? processId : "proc-" + System.currentTimeMillis();
            this.processName = processName != null ? processName : "Task Execution";
            this.sessionId = sessionId != null ? sessionId : "GLOBAL";
            this.state = state != null ? state : "RUNNING";
            this.phase = phase != null ? phase : "INITIALIZING";
            this.startTimestamp = System.currentTimeMillis();
            this.lastTransitionTimestamp = this.startTimestamp;
            this.progressFraction = 0.0;
            this.lastStatus = "Process started";
            this.failureDetails = "";
            this.correlatedResource = "";
        }

        public String getProcessId() { return processId; }
        public String getProcessName() { return processName; }
        public String getSessionId() { return sessionId; }
        public String getState() { return state; }
        public String getPhase() { return phase; }
        public long getStartTimestamp() { return startTimestamp; }
        public long getLastTransitionTimestamp() { return lastTransitionTimestamp; }
        public double getProgressFraction() { return progressFraction; }
        public String getLastStatus() { return lastStatus; }
        public String getFailureDetails() { return failureDetails; }
        public String getCorrelatedResource() { return correlatedResource; }

        public void setState(String state) {
            this.state = state;
            this.lastTransitionTimestamp = System.currentTimeMillis();
        }

        public void setPhase(String phase) {
            this.phase = phase;
            this.lastTransitionTimestamp = System.currentTimeMillis();
        }

        public void setProgressFraction(double progressFraction) {
            this.progressFraction = Math.max(0.0, Math.min(1.0, progressFraction));
        }

        public void setLastStatus(String lastStatus) {
            this.lastStatus = lastStatus;
        }

        public void setFailureDetails(String failureDetails) {
            this.failureDetails = failureDetails;
        }

        public void setCorrelatedResource(String correlatedResource) {
            this.correlatedResource = correlatedResource;
        }

        @Override
        public String toString() {
            return String.format("ProcessInfo[id=%s, name=%s, state=%s, phase=%s, progress=%.1f%%]",
                    processId, processName, state, phase, progressFraction * 100);
        }
    }

    private final Map<String, ProcessInfo> activeProcesses = new ConcurrentHashMap<>();

    public ContextProcessTracker() {
    }

    public void registerSessionBus(RuntimeEventBus bus) {
        if (bus != null) {
            bus.subscribe(this);
        }
    }

    public void unregisterSessionBus(RuntimeEventBus bus) {
        if (bus != null) {
            bus.unsubscribe(this);
        }
    }

    @Override
    public void onEvent(RuntimeEvent event) {
        if (event == null) return;

        String sid = event.getSessionId();
        if (sid == null || sid.isEmpty()) {
            sid = "GLOBAL";
        }

        RuntimeEventType type = event.getType();
        String payloadStr = event.getPayload() != null ? event.getPayload().toString() : "";
        String entityId = event.getEntityId() != null ? event.getEntityId() : event.getSource();

        switch (type) {
            case FLOW_STARTED:
            case TASK_STARTED: {
                ProcessInfo info = new ProcessInfo(entityId, type == RuntimeEventType.FLOW_STARTED ? "Workflow Flow" : "Task Execution", sid, "RUNNING", type.name());
                info.setLastStatus(payloadStr.isEmpty() ? "Execution started" : payloadStr);
                if (event.getMetadata().get("resource") != null) {
                    info.setCorrelatedResource(event.getMetadata().get("resource").toString());
                }
                activeProcesses.put(sid, info);
                break;
            }

            case FORGE_TRAINING_STARTED: {
                ProcessInfo info = new ProcessInfo(entityId, "Forge LLM Training", sid, "RUNNING", "TRAINING");
                info.setLastStatus("LLM Training started");
                activeProcesses.put(sid, info);
                break;
            }

            case STEP_CREATED:
            case STEP_RESUMED: {
                ProcessInfo info = activeProcesses.get(sid);
                if (info == null) {
                    info = new ProcessInfo(entityId, "Step Execution", sid, "RUNNING", "STEP");
                    activeProcesses.put(sid, info);
                }
                info.setState("RUNNING");
                info.setPhase("STEP_EXECUTION");
                info.setLastStatus("Step running: " + payloadStr);
                break;
            }

            case EVOLUTION_PROGRESS: {
                ProcessInfo info = activeProcesses.get(sid);
                if (info != null) {
                    Object frac = event.getMetadata().get("progress");
                    if (frac instanceof Number) {
                        info.setProgressFraction(((Number) frac).doubleValue());
                    }
                    if (!payloadStr.isEmpty()) {
                        info.setLastStatus(payloadStr);
                    }
                }
                break;
            }

            case STEP_WAITING: {
                ProcessInfo info = activeProcesses.get(sid);
                if (info != null) {
                    info.setState("PAUSED");
                    info.setPhase("WAITING_FOR_APPROVAL");
                    info.setLastStatus("Waiting for user approval: " + payloadStr);
                }
                break;
            }

            case FLOW_COMPLETED:
            case TASK_COMPLETED:
            case FORGE_TRAINING_STOPPED:
            case STEP_COMPLETED: {
                ProcessInfo info = activeProcesses.get(sid);
                if (info != null) {
                    info.setState("SUCCESS");
                    info.setPhase("COMPLETED");
                    info.setProgressFraction(1.0);
                    info.setLastStatus("Completed successfully: " + payloadStr);
                }
                break;
            }

            case COMMAND_FAILED:
            case TASK_FAILED:
            case FORGE_TRAINING_FAILED:
            case STEP_FAILED: {
                ProcessInfo info = activeProcesses.get(sid);
                if (info == null) {
                    info = new ProcessInfo(entityId, "Failed Execution", sid, "FAILED", "ERROR");
                    activeProcesses.put(sid, info);
                }
                info.setState("FAILED");
                info.setPhase("ERROR");
                info.setFailureDetails(payloadStr.isEmpty() ? "Execution failed" : payloadStr);
                info.setLastStatus("Failed: " + info.getFailureDetails());
                break;
            }

            default:
                break;
        }
    }

    public ProcessInfo getProcessState(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "GLOBAL";
        }
        return activeProcesses.get(sessionId);
    }

    public void clearSession(String sessionId) {
        if (sessionId != null) {
            activeProcesses.remove(sessionId);
        }
    }

    public void clearAll() {
        activeProcesses.clear();
    }
}
