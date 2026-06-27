package eu.kalafatic.evolution.controller.log;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class Log {
    private static final String CONSOLE_NAME = "Evo Orchestration Console";
    private static MessageConsole console;
    private static MessageConsoleStream consoleStream;
    private static boolean redirected = false;
    private static Logger fileLogger;
    private static final String LOG_DIR = getLogDirectory();
    private static final String LOG_FILE = LOG_DIR + "/evo.log";

    public static String getLogDirectory() {
        try {
            // Try to use Eclipse state location
            return Platform.getStateLocation(Platform.getBundle("eu.kalafatic.utils")).toOSString() + "/logs";
        } catch (Exception e) {
            // Fallback to local logs directory if Platform is not available
            return "logs";
        }
    }
    private static final int LOG_FILE_SIZE_LIMIT = 5 * 1024 * 1024; // 5MB
    private static final int LOG_FILE_COUNT = 5;

    // Cache reflection lookups for performance
    private static Class<?> displayClass;
    private static java.lang.reflect.Method getCurrentMethod;
    private static java.lang.reflect.Method getDefaultMethod;
    private static java.lang.reflect.Method asyncExecMethod;
    private static boolean swtAvailable = false;

    static {
        initFileLogger();
        zipOldLogs();
        initSwtReflection();
    }

    private static void initSwtReflection() {
        try {
            displayClass = Class.forName("org.eclipse.swt.widgets.Display");
            getCurrentMethod = displayClass.getMethod("getCurrent");
            getDefaultMethod = displayClass.getMethod("getDefault");
            asyncExecMethod = displayClass.getMethod("asyncExec", Runnable.class);
            swtAvailable = true;
        } catch (Throwable t) {
            swtAvailable = false;
        }
    }

    private static void initFileLogger() {
        try {
            File dir = new File(LOG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            fileLogger = Logger.getLogger("EvoLogger");
            FileHandler fh = new FileHandler(LOG_FILE, LOG_FILE_SIZE_LIMIT, LOG_FILE_COUNT, true);
            fh.setFormatter(new SimpleFormatter());
            fileLogger.addHandler(fh);
            fileLogger.setLevel(Level.ALL);
        } catch (IOException e) {
            System.err.println("Failed to initialize file logger: " + e.getMessage());
        }
    }

    public static String getLogFile() {
        return LOG_FILE;
    }

    public static void log(String msg) {
        // Log to stdout for basic visibility (only if not redirected to avoid duplicates)
        if (!redirected) {
            System.out.println(msg);
        }

        // Log to file
        if (fileLogger != null) {
            fileLogger.info(msg);
        }

        // Log to Eclipse Console
        logToConsole(msg);
    }

    public static void log(Object module, Exception e) {
        String msg = "ERROR [" + module + "]: " + e.getMessage();
        log(msg);
        if (fileLogger != null) {
            fileLogger.log(Level.SEVERE, msg, e);
        }
    }

    public static void redirectSystemStreams() {
        if (redirected) return;
        try {
            initConsole();
            if (consoleStream != null) {
                PrintStream ps = new PrintStream(new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        consoleStream.write(b);
                    }
                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        consoleStream.write(b, off, len);
                    }
                    @Override
                    public void flush() throws IOException {
                        consoleStream.flush();
                    }
                }, true);
                System.setOut(ps);
                System.setErr(ps);
                redirected = true;
                log("System streams redirected to Eclipse Console.");
            }
        } catch (Exception e) {
            System.err.println("Failed to redirect system streams: " + e.getMessage());
        }
    }

    private static void initConsole() {
        if (consoleStream == null) {
            console = findConsole(CONSOLE_NAME);
            if (console != null) {
                consoleStream = console.newMessageStream();
                consoleStream.setActivateOnWrite(true);
            }
        }
    }

    private static void logToConsole(String message) {
        if (!swtAvailable) return;

        try {
            // Check if we are in a headless or test environment that shouldn't touch SWT
            if (Boolean.getBoolean("evolution.test.headless")) return;
            if (System.getProperty("osgi.console") != null) return;

            Object display = getCurrentMethod.invoke(null);

            if (display != null) {
                doLogToConsole(message);
            } else {
                // Background thread, use asyncExec via reflection
                Object defaultDisplay = getDefaultMethod.invoke(null);
                if (defaultDisplay != null) {
                    asyncExecMethod.invoke(defaultDisplay, (Runnable) () -> doLogToConsole(message));
                }
            }
        } catch (Exception | Error e) {
            // Silently ignore - could be SWT not available, GTK init failure, or reflection issue
        }
    }

    private static void doLogToConsole(String message) {
        try {
            initConsole();
            if (consoleStream != null) {
                consoleStream.println(message);
            }
        } catch (Exception e) {
            // Silently ignore
        }
    }

    private static MessageConsole findConsole(String name) {
        try {
            ConsolePlugin plugin = ConsolePlugin.getDefault();
            if (plugin == null) return null;
            IConsoleManager conMan = plugin.getConsoleManager();
            IConsole[] existing = conMan.getConsoles();
            for (int i = 0; i < existing.length; i++) {
                if (name.equals(existing[i].getName())) {
                    return (MessageConsole) existing[i];
                }
            }
            // no console found, so create a new one
            MessageConsole myConsole = new MessageConsole(name, null);
            conMan.addConsoles(new IConsole[]{myConsole});
            return myConsole;
        } catch (NoClassDefFoundError e) {
            return null;
        }
    }

    public static void zipOldLogs() {
        File dir = new File(LOG_DIR);
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles((d, name) -> name.startsWith("evo.log.") && !name.endsWith(".zip"));
        if (files == null || files.length == 0) return;

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File zipFile = new File(LOG_DIR, "logs_archived_" + timestamp + ".zip");

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (File file : files) {
                addToZip(file, zos);
                file.delete();
            }
            log("Old logs archived to: " + zipFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to zip old logs: " + e.getMessage());
        }
    }

    private static void addToZip(File file, ZipOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        }
    }
}
