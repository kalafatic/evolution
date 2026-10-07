package eu.kalafatic.evolution.supervisor.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.supervisor.EVOSupervisorControlServer;
import eu.kalafatic.evolution.supervisor.SupervisorMain.EVOSupervisorServer;
import eu.kalafatic.evolution.supervisor.SelfDevSupervisor;
import fi.iki.elonen.NanoHTTPD;

public class SupervisorUiWindow {

    private final File baseDir;
    private final int supervisorPort;
    private final int controlPort;
    private final boolean debug;

    private Display display;
    private Shell shell;
    private Label statusLabel;
    private ProgressBar progressBar;
    private Text consoleText;
    private Button startButton;
    private Button stopButton;

    private Thread supervisorThread;
    private SelfDevSupervisor supervisor;
    private NanoHTTPD httpServer;
    private NanoHTTPD httpControlServer;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public SupervisorUiWindow() {
        this(new File("."), 8089, 28080, false);
    }

    public SupervisorUiWindow(File baseDir, int supervisorPort, int controlPort, boolean debug) {
        this.baseDir = baseDir != null ? baseDir : new File(".");
        this.supervisorPort = supervisorPort;
        this.controlPort = controlPort;
        this.debug = debug;
    }

    public void open(String[] args) {
        display = Display.getDefault();
        shell = new Shell(display, SWT.SHELL_TRIM);
        shell.setText("EVO External Supervisor (RCP Mode)");
        shell.setSize(800, 600);
        shell.setLayout(new GridLayout(1, false));

        createHeaderSection(shell);
        createControlSection(shell);
        createConsoleSection(shell);

        redirectStreams();

        shell.open();

        // Auto-start supervisor
        startSupervisorLogic();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        stopSupervisorLogic();
    }

    private void createHeaderSection(Composite parent) {
        Composite header = new Composite(parent, SWT.NONE);
        header.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        header.setLayout(new GridLayout(2, false));

        Label title = new Label(header, SWT.NONE);
        title.setText("EVO External Supervisor - RCP Console & Progress");
        GridData titleData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
        title.setLayoutData(titleData);

        statusLabel = new Label(header, SWT.NONE);
        statusLabel.setText("Status: INITIALIZING | Port: " + supervisorPort + " | Control: " + controlPort);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        Label progressName = new Label(header, SWT.NONE);
        progressName.setText("Progress:");
        progressName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        progressBar = new ProgressBar(header, SWT.HORIZONTAL | SWT.SMOOTH);
        progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setSelection(10);
    }

    private void createControlSection(Composite parent) {
        Composite controls = new Composite(parent, SWT.NONE);
        controls.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        controls.setLayout(new GridLayout(4, false));

        startButton = new Button(controls, SWT.PUSH);
        startButton.setText("Start Supervisor");
        startButton.addListener(SWT.Selection, e -> startSupervisorLogic());

        stopButton = new Button(controls, SWT.PUSH);
        stopButton.setText("Stop Supervisor");
        stopButton.addListener(SWT.Selection, e -> stopSupervisorLogic());

        Button checkButton = new Button(controls, SWT.PUSH);
        checkButton.setText("Run Check");
        checkButton.addListener(SWT.Selection, e -> runCheckLogic());

        Button clearButton = new Button(controls, SWT.PUSH);
        clearButton.setText("Clear Console");
        clearButton.addListener(SWT.Selection, e -> {
            if (consoleText != null && !consoleText.isDisposed()) {
                consoleText.setText("");
            }
        });
    }

    private void createConsoleSection(Composite parent) {
        Label consoleLabel = new Label(parent, SWT.NONE);
        consoleLabel.setText("Supervisor Output Console:");
        consoleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

        consoleText = new Text(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.BORDER);
        consoleText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        consoleText.setText("=== EVO RCP SUPERVISOR CONSOLE READY ===\n");
    }

    public synchronized void startSupervisorLogic() {
        if (running.get()) {
            appendLog("[UI] Supervisor is already running.\n");
            return;
        }

        running.set(true);
        updateStatus("Status: RUNNING | Port: " + supervisorPort + " | Control: " + controlPort, 25);

        try {
            appendLog("[SUPERVISOR-RCP] Initializing HTTP API servers on port " + supervisorPort + " & control port " + controlPort + "...\n");
            httpServer = new EVOSupervisorServer(supervisorPort, baseDir);
            httpServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

            httpControlServer = new EVOSupervisorControlServer(controlPort, supervisorPort, baseDir);
            httpControlServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

            appendLog("[SUPERVISOR-RCP] HTTP API server started on http://127.0.0.1:" + supervisorPort + "\n");
            appendLog("[SUPERVISOR-RCP] HTTP Control server started on http://127.0.0.1:" + controlPort + "\n");
        } catch (Throwable t) {
            appendLog("[SUPERVISOR-RCP WARNING] Failed to start HTTP server: " + t.getMessage() + "\n");
        }

        supervisorThread = new Thread(() -> {
            try {
                supervisor = new SelfDevSupervisor(baseDir);

                updateStatus("Status: ACTIVE - MONITORING LOOP RUNNING", 50);
                appendLog("[SUPERVISOR-RCP] Supervisor monitoring loop initialized.\n");

                supervisor.run();

                updateStatus("Status: FINISHED", 100);
            } catch (Exception e) {
                appendLog("[SUPERVISOR-RCP ERROR] " + e.getMessage() + "\n");
                updateStatus("Status: ERROR - " + e.getMessage(), 0);
            } finally {
                running.set(false);
            }
        }, "SupervisorUiThread");

        supervisorThread.setDaemon(true);
        supervisorThread.start();
    }

    public synchronized void stopSupervisorLogic() {
        if (!running.get()) {
            appendLog("[UI] Supervisor is not running.\n");
            return;
        }

        running.set(false);
        appendLog("[SUPERVISOR-RCP] Stopping supervisor...\n");
        updateStatus("Status: STOPPING...", 75);

        if (httpServer != null) {
            try { httpServer.stop(); } catch (Throwable ignored) {}
            httpServer = null;
        }
        if (httpControlServer != null) {
            try { httpControlServer.stop(); } catch (Throwable ignored) {}
            httpControlServer = null;
        }

        if (supervisorThread != null && supervisorThread.isAlive()) {
            supervisorThread.interrupt();
        }

        updateStatus("Status: STOPPED", 0);
    }

    private void runCheckLogic() {
        appendLog("[UI] Running platform check...\n");
        updateStatus("Status: RUNNING CHECK...", 60);
        appendLog("[UI] Base Directory: " + baseDir.getAbsolutePath() + "\n");
        appendLog("[UI] Java Version: " + System.getProperty("java.version") + "\n");
        appendLog("[UI] OS Name: " + System.getProperty("os.name") + "\n");
        updateStatus("Status: RUNNING | Port: " + supervisorPort, 50);
    }

    public void updateStatus(String text, int progress) {
        if (display == null || display.isDisposed()) return;
        display.asyncExec(() -> {
            if (statusLabel != null && !statusLabel.isDisposed()) {
                statusLabel.setText(text);
            }
            if (progressBar != null && !progressBar.isDisposed()) {
                progressBar.setSelection(progress);
            }
        });
    }

    public void appendLog(String text) {
        if (display == null || display.isDisposed()) {
            System.out.print(text);
            return;
        }
        display.asyncExec(() -> {
            if (consoleText != null && !consoleText.isDisposed()) {
                consoleText.append(text);
                consoleText.setSelection(consoleText.getCharCount());
            }
        });
    }

    private void redirectStreams() {
        PrintStream originalOut = System.out;

        PrintStream newOut = new PrintStream(new ByteArrayOutputStream() {
            @Override
            public void flush() {
                String text = toString();
                if (!text.isEmpty()) {
                    appendLog(text);
                    reset();
                }
            }
        }, true) {
            @Override
            public void write(byte[] buf, int off, int len) {
                super.write(buf, off, len);
                originalOut.write(buf, off, len);
                String text = new String(buf, off, len);
                appendLog(text);
            }

            @Override
            public void write(int b) {
                super.write(b);
                originalOut.write(b);
                appendLog(String.valueOf((char) b));
            }
        };

        System.setOut(newOut);
        System.setErr(newOut);
    }

    public Shell getShell() {
        return shell;
    }

    public boolean isRunning() {
        return running.get();
    }
}
