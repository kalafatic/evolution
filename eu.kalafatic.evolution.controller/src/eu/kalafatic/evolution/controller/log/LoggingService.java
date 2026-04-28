package eu.kalafatic.evolution.controller.log;

import org.json.JSONObject;
import eu.kalafatic.evolution.model.orchestration.LogLevel;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Task-aware, adaptive logging service for structured diagnostics.
 * Follows severity: TRACE(0) < DEBUG(1) < INFO(2) < WARN(3) < ERROR(4).
 */
public class LoggingService {

    private static final LoggingService INSTANCE = new LoggingService();

    public static LoggingService getInstance() {
        return INSTANCE;
    }

    /**
     * Logs a structured message if the current task's log level allows it.
     */
    public void log(TaskContext context, LogLevel level, String message, Object data) {
        LogLevel threshold = getThreshold(context);

        // Log if message level is greater than or equal to threshold
        if (level.getValue() < threshold.getValue()) {
            return;
        }

        JSONObject structured = new JSONObject();
        structured.put("taskId", context.getCurrentTaskId());
        structured.put("iteration", context.getCurrentIteration());
        structured.put("phase", context.getCurrentPhase());
        structured.put("level", level.getName());
        structured.put("message", message);

        if (data != null) {
            structured.put("data", data);
        }

        // Use standard formatting for plain text logs
        String prefix = String.format("[%s][Iter:%d][%s] ",
            context.getCurrentTaskId(), context.getCurrentIteration(), context.getCurrentPhase());

        Log.log(prefix + message);

        // Always log structured JSON at DEBUG level or higher (meaning more verbose)
        if (threshold.getValue() <= LogLevel.DEBUG_VALUE) {
            Log.log("STRUCTURED: " + structured.toString());
        }
    }

    private LogLevel getThreshold(TaskContext context) {
        if (context.getOrchestrator() == null) return LogLevel.INFO;

        // Find the current task in the orchestrator
        return context.getOrchestrator().getTasks().stream()
            .filter(t -> t.getId().equals(context.getCurrentTaskId()))
            .findFirst()
            .map(t -> t.getLogLevel())
            .orElse(LogLevel.INFO);
    }

    public void trace(TaskContext context, String message, Object data) {
        log(context, LogLevel.TRACE, message, data);
    }

    public void info(TaskContext context, String message) {
        log(context, LogLevel.INFO, message, null);
    }

    public void debug(TaskContext context, String message, Object data) {
        log(context, LogLevel.DEBUG, message, data);
    }

    public void warn(TaskContext context, String message) {
        log(context, LogLevel.WARN, message, null);
    }

    public void error(TaskContext context, String message, Exception e) {
        log(context, LogLevel.ERROR, message, e != null ? e.getMessage() : null);
    }
}
