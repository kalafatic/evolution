package eu.kalafatic.evolution.view.projection;

import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of the orchestration state for a specific session.
 */
public class RuntimeProjection {
    private final String sessionId;
    private final boolean running;
    private final boolean paused;
    private final String status;
    private final double progress;
    private final List<RuntimeEvent> events;
    private final String lastWaitingMessage;
    private final boolean waitingForUser;

    public RuntimeProjection(String sessionId) {
        this(sessionId, false, false, "INITIALIZING...", 0.0, Collections.emptyList(), null, false);
    }

    private RuntimeProjection(String sessionId, boolean running, boolean paused, String status, double progress,
                             List<RuntimeEvent> events, String lastWaitingMessage, boolean waitingForUser) {
        this.sessionId = sessionId;
        this.running = running;
        this.paused = paused;
        this.status = status;
        this.progress = progress;
        this.events = Collections.unmodifiableList(new ArrayList<>(events));
        this.lastWaitingMessage = lastWaitingMessage;
        this.waitingForUser = waitingForUser;
    }

    public RuntimeProjection withEvent(RuntimeEvent event) {
        List<RuntimeEvent> newEvents = new ArrayList<>(this.events);
        newEvents.add(event);

        boolean newRunning = this.running;
        boolean newPaused = this.paused;
        String newStatus = this.status;
        double newProgress = this.progress;
        String newWaitingMessage = this.lastWaitingMessage;
        boolean newWaitingForUser = this.waitingForUser;

        switch (event.getType()) {
            case KERNEL_STARTED:
            case FLOW_STARTED:
                newRunning = true;
                newPaused = false;
                newStatus = "RUNNING";
                break;
            case FLOW_COMPLETED:
            case TASK_COMPLETED:
            case TASK_FAILED:
            case KERNEL_SHUTDOWN:
                newRunning = false;
                newPaused = false;
                newStatus = event.getType() == RuntimeEventType.KERNEL_SHUTDOWN ? "OFFLINE" : "COMPLETED";
                newWaitingForUser = false;
                break;
            case FLOW_PAUSED:
                newPaused = true;
                newStatus = "PAUSED";
                break;
            case STEP_WAITING:
                newWaitingForUser = true;
                newWaitingMessage = (String) event.getPayload();
                newStatus = "WAITING FOR USER";
                break;
            case STEP_RESUMED:
                newWaitingForUser = false;
                newStatus = "RESUMED";
                break;
            default:
                break;
        }

        return new RuntimeProjection(sessionId, newRunning, newPaused, newStatus, newProgress, newEvents, newWaitingMessage, newWaitingForUser);
    }

    public String getSessionId() { return sessionId; }
    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused; }
    public String getStatus() { return status; }
    public double getProgress() { return progress; }
    public List<RuntimeEvent> getEvents() { return events; }
    public String getLastWaitingMessage() { return lastWaitingMessage; }
    public boolean isWaitingForUser() { return waitingForUser; }
}
