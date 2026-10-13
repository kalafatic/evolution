package eu.kalafatic.evolution.controller.log;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

/**
 * Session-isolated logger that routes log entries and console messages
 * to session-specific log files and Eclipse MessageConsole streams.
 */
public class SessionLogger {
    private final String sessionId;
    private final Logger fileLogger;
    private final File logFile;
    private MessageConsole console;
    private MessageConsoleStream consoleStream;
    private FileHandler fileHandler;
    private RuntimeEventBus eventBus;

    public SessionLogger(String sessionId) {
        this.sessionId = sessionId;
        String baseDir = Log.getLogDirectory();
        File sessionDir = new File(baseDir, "sessions");
        if (!sessionDir.exists()) {
            sessionDir.mkdirs();
        }
        this.logFile = new File(sessionDir, "session_" + sanitizeSessionId(sessionId) + ".log");

        this.fileLogger = Logger.getLogger("EvoSessionLogger_" + sanitizeSessionId(sessionId));
        this.fileLogger.setUseParentHandlers(false);
        try {
            this.fileHandler = new FileHandler(logFile.getAbsolutePath(), 5 * 1024 * 1024, 3, true);
            this.fileHandler.setFormatter(new SimpleFormatter());
            this.fileLogger.addHandler(this.fileHandler);
            this.fileLogger.setLevel(Level.ALL);
        } catch (IOException e) {
            System.err.println("Failed to initialize session file logger for " + sessionId + ": " + e.getMessage());
        }

        initConsole();
    }

    public void setEventBus(RuntimeEventBus eventBus) {
        this.eventBus = eventBus;
    }

    private String sanitizeSessionId(String id) {
        if (id == null) return "unknown";
        return id.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void initConsole() {
        if (Boolean.getBoolean("evolution.test.headless")) return;
        try {
            String consoleName = "Evo Session Console [" + sessionId + "]";
            ConsolePlugin plugin = ConsolePlugin.getDefault();
            if (plugin != null) {
                IConsoleManager conMan = plugin.getConsoleManager();
                IConsole[] existing = conMan.getConsoles();
                for (IConsole c : existing) {
                    if (consoleName.equals(c.getName()) && c instanceof MessageConsole) {
                        this.console = (MessageConsole) c;
                        break;
                    }
                }
                if (this.console == null) {
                    this.console = new MessageConsole(consoleName, null);
                    conMan.addConsoles(new IConsole[]{this.console});
                }
                this.consoleStream = this.console.newMessageStream();
            }
        } catch (Throwable t) {
            // Eclipse console or SWT plugin not present in headless/unit testing environment
        }
    }

    public String getSessionId() {
        return sessionId;
    }

    public File getLogFile() {
        return logFile;
    }

    public MessageConsole getConsole() {
        return console;
    }

    public MessageConsoleStream getConsoleStream() {
        return consoleStream;
    }

    public void log(String msg) {
        log(Level.INFO, "System", msg, null);
    }

    public void log(String source, String msg) {
        log(Level.INFO, source, msg, null);
    }

    public void log(Level level, String source, String msg, Throwable t) {
        if (fileLogger != null) {
            if (t != null) {
                fileLogger.log(level, "[" + source + "] " + msg, t);
            } else {
                fileLogger.log(level, "[" + source + "] " + msg);
            }
        }

        if (consoleStream != null) {
            try {
                consoleStream.println("[" + level.getName() + "][" + source + "] " + msg);
                if (t != null) {
                    t.printStackTrace(new java.io.PrintStream(consoleStream));
                }
            } catch (Exception e) {
                // Ignore console stream write errors
            }
        }

        if (eventBus != null) {
            try {
                RuntimeEvent event = new RuntimeEvent(RuntimeEventType.LOG_MESSAGE, sessionId, source, msg)
                        .withMetadata("level", level.getName());
                eventBus.publish(event);
            } catch (Exception e) {
                // Ignore event publishing failures
            }
        }
    }

    public void close() {
        if (fileHandler != null) {
            try {
                fileHandler.flush();
                fileHandler.close();
                fileLogger.removeHandler(fileHandler);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        if (consoleStream != null) {
            try {
                consoleStream.flush();
                consoleStream.close();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        if (console != null) {
            try {
                ConsolePlugin plugin = ConsolePlugin.getDefault();
                if (plugin != null) {
                    plugin.getConsoleManager().removeConsoles(new IConsole[]{console});
                }
            } catch (Throwable t) {
                // Ignore cleanup errors
            }
        }
    }
}
