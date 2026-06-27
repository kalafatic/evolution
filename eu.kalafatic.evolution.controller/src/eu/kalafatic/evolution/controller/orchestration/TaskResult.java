package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Result of an orchestration task.
 */
public class TaskResult {
    public enum Status {
        SUCCESS, FAILED, RUNNING, WAITING_FOR_APPROVAL, WAITING_FOR_INPUT
    }

    private String id;
    private Status status;
    private String response;
    private List<String> logs = new CopyOnWriteArrayList<>();
    private List<String> fileChanges = new ArrayList<>();
    private String error;
    private String waitingMessage;

    public TaskResult() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }

    public List<String> getFileChanges() {
        return fileChanges;
    }

    public void setFileChanges(List<String> fileChanges) {
        this.fileChanges = fileChanges;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getWaitingMessage() {
        return waitingMessage;
    }

    public void setWaitingMessage(String waitingMessage) {
        this.waitingMessage = waitingMessage;
    }
}
