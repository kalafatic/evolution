# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/log/

## Domain: general

## Components
* `LoggingService.java`: package eu.kalafatic.evolution.controller.log; import org.json.JSONObject; import eu.kalafatic.evolution.model.orchestration.LogLevel; import eu.kalafatic.evolution.controller.orchestration.TaskContext; public class LoggingService { private static final LoggingService INSTANCE = new LoggingService(); public static LoggingService getInstance() { return INSTANCE; } public void log(TaskContext context, LogLevel level, String message, Object data) { LogLevel threshold = getThreshold(context); if (level.getValue() < threshold.getValue()) { return; } JSONObject structured = new JSONObject(); structured.put("taskId", context.getCurrentTaskId()); structured.put("iteration", context.getCurrentIteration()); structured.put("phase", context.getCurrentPhase()); structured.put("level", level.getName()); structured.put("message", message);
* `Log.java`: package eu.kalafatic.evolution.controller.log; import java.io.File; import java.io.FileInputStream; import java.io.FileOutputStream; import java.io.IOException; import java.io.OutputStream; import java.io.PrintStream; import java.text.SimpleDateFormat; import java.util.Date; import java.util.logging.FileHandler; import java.util.logging.Level; import java.util.logging.Logger; import java.util.logging.SimpleFormatter; import java.util.zip.ZipEntry; import java.util.zip.ZipOutputStream; import org.eclipse.ui.console.ConsolePlugin; import org.eclipse.ui.console.IConsole; import org.eclipse.core.runtime.Platform; import org.eclipse.ui.console.IConsoleManager; import org.eclipse.ui.console.MessageConsole;
