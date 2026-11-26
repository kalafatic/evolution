package eu.kalafatic.evolution.view.editors.pages.settings;

import java.net.URL;
import java.util.function.Consumer;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import eu.kalafatic.evolution.controller.hardware.CudaDiagnosticReport;
import eu.kalafatic.evolution.controller.hardware.CudaDiagnosticService;
import eu.kalafatic.evolution.controller.hardware.DiagnosticStage;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.monitor.HardwareDashboardBridge;
import eu.kalafatic.evolution.view.monitor.HardwareMonitoringService;
import eu.kalafatic.evolution.view.monitor.HardwareScanResult;
import eu.kalafatic.evolution.view.monitor.HardwareSnapshot;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * Settings → Hardware panel: hardware scan, live monitoring controls, a
 * professional HTML dashboard (SWT Browser, incremental Java→JS updates) and
 * the centralized CUDA diagnostic test. All data comes from the shared
 * {@link HardwareMonitoringService} and {@link CudaDiagnosticService} — the
 * same sources used by the Forge page and the AI Commander Forge panel.
 */
public class HardwareSettingsGroup extends AEvoGroup {

    private Button scanBtn;
    private Button startBtn;
    private Button stopBtn;
    private Button refreshBtn;
    private Button testCudaBtn;
    private Button detailsBtn;
    private Combo intervalCombo;
    private Label monitorStatus;
    private Label cpuInfo;
    private Label ramInfo;
    private Label jvmInfo;
    private Label gpuInfo;
    private Label cudaScanInfo;
    private Label cudaTestInfo;

    private Browser dashboard;
    private boolean jsReady = false;

    private final Consumer<HardwareSnapshot> snapshotListener = s ->
            Display.getDefault().asyncExec(this::pushSampleToDashboard);
    private final Consumer<CudaDiagnosticReport> cudaListener = r ->
            Display.getDefault().asyncExec(() -> {
                pushCudaStatusToDashboard();
                updateUI();
            });

    public HardwareSettingsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor,
                                 Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);

        HardwareMonitoringService.getInstance().addListener(snapshotListener);
        CudaDiagnosticService.getInstance().addListener(cudaListener);
        group.addListener(SWT.Dispose, e -> {
            HardwareMonitoringService.getInstance().removeListener(snapshotListener);
            CudaDiagnosticService.getInstance().removeListener(cudaListener);
        });
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent,
                "Hardware (CPU · RAM · GPU · CUDA)", 1, true);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // --- Actions ---
        Composite actions = toolkit.createComposite(group);
        actions.setLayout(new GridLayout(8, false));
        actions.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        scanBtn = GUIFactory.INSTANCE.createButton(actions, "Scan Hardware");
        scanBtn.addListener(SWT.Selection, e -> startScan());

        startBtn = GUIFactory.INSTANCE.createButton(actions, "Start Monitoring");
        startBtn.addListener(SWT.Selection, e -> startMonitoring());

        stopBtn = GUIFactory.INSTANCE.createButton(actions, "Stop Monitoring");
        stopBtn.addListener(SWT.Selection, e -> stopMonitoring());

        refreshBtn = GUIFactory.INSTANCE.createButton(actions, "Refresh Now");
        refreshBtn.addListener(SWT.Selection, e -> refreshNow());

        toolkit.createLabel(actions, "Interval:");
        intervalCombo = new Combo(actions, SWT.READ_ONLY);
        intervalCombo.setItems("0.5 s", "1 s", "2 s", "5 s");
        intervalCombo.select(1);
        intervalCombo.addListener(SWT.Selection, e -> applyInterval());

        testCudaBtn = GUIFactory.INSTANCE.createButton(actions, "Test CUDA");
        testCudaBtn.addListener(SWT.Selection, e -> testCuda());

        detailsBtn = GUIFactory.INSTANCE.createButton(actions, "View Details");
        detailsBtn.addListener(SWT.Selection, e -> showDetails());

        // --- Status + inventory ---
        monitorStatus = toolkit.createLabel(group, "Monitoring: stopped — no scan executed yet");
        monitorStatus.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        cpuInfo = toolkit.createLabel(group, "CPU: —");
        cpuInfo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ramInfo = toolkit.createLabel(group, "RAM: —");
        ramInfo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        jvmInfo = toolkit.createLabel(group, "JVM: —");
        jvmInfo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        gpuInfo = toolkit.createLabel(group, "GPU: —");
        gpuInfo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        cudaScanInfo = toolkit.createLabel(group, "CUDA: —");
        cudaScanInfo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        cudaTestInfo = toolkit.createLabel(group, "CUDA backend test: not executed");
        cudaTestInfo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // --- HTML dashboard ---
        dashboard = new Browser(group, SWT.NONE);
        GridData browserData = new GridData(GridData.FILL_BOTH);
        browserData.heightHint = 560;
        dashboard.setLayoutData(browserData);
        dashboard.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(ProgressEvent event) {
                installBridges();
                jsReady = true;
                Display.getDefault().asyncExec(() -> {
                    pushScanToDashboard();
                    pushSampleToDashboard();
                    pushCudaStatusToDashboard();
                });
            }
        });
        loadDashboard();

        updateUI();
    }

    private void loadDashboard() {
        try {
            Bundle bundle = FrameworkUtil.getBundle(getClass());
            if (bundle == null) {
                bundle = Platform.getBundle("eu.kalafatic.evolution.view");
            }
            if (bundle != null) {
                URL root = FileLocator.toFileURL(bundle.getEntry("/"));
                dashboard.setUrl(new URL(root, "hardware-dashboard.html").toString());
                return;
            }
        } catch (Exception e) {
            System.err.println("[HARDWARE] Dashboard load failed: " + e.getMessage());
        }
        dashboard.setText("<html><body><h3>Hardware dashboard unavailable</h3></body></html>");
    }

    private void installBridges() {
        if (dashboard == null || dashboard.isDisposed()) return;
        try {
            new BrowserFunction(dashboard, "evoHwReady") {
                @Override
                public Object function(Object[] arguments) {
                    jsReady = true;
                    Display.getDefault().asyncExec(() -> {
                        pushScanToDashboard();
                        pushSampleToDashboard();
                        pushCudaStatusToDashboard();
                    });
                    return null;
                }
            };
            new BrowserFunction(dashboard, "evoHwAction") {
                @Override
                public Object function(Object[] arguments) {
                    if (arguments.length < 1) return null;
                    String action = String.valueOf(arguments[0]);
                    Display.getDefault().asyncExec(() -> dispatchAction(action));
                    return null;
                }
            };
        } catch (Throwable t) {
            System.err.println("[HARDWARE] Bridge installation failed: " + t.getMessage());
        }
    }

    private void dispatchAction(String action) {
        switch (action) {
            case "scan": startScan(); break;
            case "test-cuda": testCuda(); break;
            case "refresh": refreshNow(); break;
            default: break;
        }
    }

    // ------------------------------------------------------------------
    // Actions (all asynchronous; the SWT thread never blocks)
    // ------------------------------------------------------------------
    private void startScan() {
        HardwareMonitoringService service = HardwareMonitoringService.getInstance();
        setEnabledSafe(scanBtn, false);
        setTextSafe(monitorStatus, "Scanning hardware…");
        final Display display = Display.getDefault();
        service.scan(msg -> display.asyncExec(() -> {
            if (!monitorStatus.isDisposed()) {
                monitorStatus.setText("Scanning… " + msg);
            }
        }), result -> display.asyncExec(() -> {
            setEnabledSafe(scanBtn, true);
            pushScanToDashboard();
            updateUI();
        }));
    }

    private void startMonitoring() {
        HardwareMonitoringService.getInstance().start();
        pushCudaStatusToDashboard();
        updateUI();
    }

    private void stopMonitoring() {
        HardwareMonitoringService.getInstance().stop();
        pushCudaStatusToDashboard();
        updateUI();
    }

    private void refreshNow() {
        // Collect off the UI thread (the service delivers the fresh snapshot
        // to listeners, which marshal back to SWT via Display.asyncExec).
        Thread t = new Thread(() -> HardwareMonitoringService.getInstance().refreshNow(),
                "evo-hw-refresh-now");
        t.setDaemon(true);
        t.start();
    }

    private void applyInterval() {
        long ms = switch (intervalCombo.getSelectionIndex()) {
            case 0 -> 500;
            case 2 -> 2000;
            case 3 -> 5000;
            default -> 1000;
        };
        HardwareMonitoringService.getInstance().setIntervalMs(ms);
        updateUI();
    }

    private void testCuda() {
        CudaDiagnosticService service = CudaDiagnosticService.getInstance();
        if (service.isRunning()) {
            return; // already running — the current report is displayed
        }
        service.startAsync();
        setTextSafe(cudaTestInfo, "CUDA backend test: RUNNING…");
        pushCudaStatusToDashboard();
        updateUI();
    }

    private void showDetails() {
        CudaDiagnosticReport report = CudaDiagnosticService.getInstance().getCurrent();
        if (report == null) {
            return;
        }
        openReportDialog(group.getShell(), "CUDA Backend Diagnostic — Details", report.toText());
    }

    /** Opens a read-only report dialog. */
    public static void openReportDialog(Shell shell, String title, String content) {
        Dialog dialog = new Dialog(shell) {
            @Override
            protected Control createDialogArea(Composite parent) {
                Composite area = (Composite) super.createDialogArea(parent);
                area.setLayout(new GridLayout(1, false));
                Text text = new Text(area, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
                text.setEditable(false);
                GridData gd = new GridData(GridData.FILL_BOTH);
                gd.widthHint = 780;
                gd.heightHint = 480;
                text.setLayoutData(gd);
                text.setText(content);
                return area;
            }

            @Override
            protected void configureShell(Shell newShell) {
                super.configureShell(newShell);
                newShell.setText(title);
                newShell.setSize(820, 560);
            }
        };
        dialog.open();
    }

    // ------------------------------------------------------------------
    // Dashboard pushes (Java → JavaScript, incremental)
    // ------------------------------------------------------------------
    private void pushSampleToDashboard() {
        if (dashboard == null || dashboard.isDisposed() || !jsReady) return;
        HardwareSnapshot s = HardwareMonitoringService.getInstance().getLatest();
        if (s == null) return;
        try {
            dashboard.execute("if(window.evoHw){window.evoHw.push("
                    + HardwareDashboardBridge.toSampleJson(s).toString() + ");}");
        } catch (Exception e) {
            System.err.println("[HARDWARE] Dashboard push failed: " + e.getMessage());
        }
    }

    private void pushScanToDashboard() {
        if (dashboard == null || dashboard.isDisposed() || !jsReady) return;
        HardwareScanResult r = HardwareMonitoringService.getInstance().getLastScan();
        if (r == null) return;
        try {
            dashboard.execute("if(window.evoHw){window.evoHw.setScan("
                    + HardwareDashboardBridge.toScanJson(r).toString() + ");}");
        } catch (Exception e) {
            System.err.println("[HARDWARE] Dashboard scan push failed: " + e.getMessage());
        }
    }

    private void pushCudaStatusToDashboard() {
        if (dashboard == null || dashboard.isDisposed() || !jsReady) return;
        HardwareMonitoringService mon = HardwareMonitoringService.getInstance();
        CudaDiagnosticService cuda = CudaDiagnosticService.getInstance();
        StringBuilder js = new StringBuilder("if(window.evoHw){");
        js.append("window.evoHw.setStatus({monitoring:").append(mon.isRunning())
                .append(",intervalMs:").append(mon.getIntervalMs()).append("});");
        CudaDiagnosticReport r = cuda.getCurrent();
        if (r != null) {
            js.append("window.evoHw.setStatus({cuda:'").append(r.overallStatus.name()).append("'});");
        }
        js.append("}");
        try {
            dashboard.execute(js.toString());
        } catch (Exception e) {
            System.err.println("[HARDWARE] Dashboard status push failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // SWT label refresh
    // ------------------------------------------------------------------
    @Override
    protected void refreshUI() {
        HardwareMonitoringService mon = HardwareMonitoringService.getInstance();
        HardwareScanResult scan = mon.getLastScan();
        HardwareSnapshot latest = mon.getLatest();

        setEnabledSafe(scanBtn, !mon.isScanning());
        setEnabledSafe(startBtn, !mon.isRunning());
        setEnabledSafe(stopBtn, mon.isRunning());

        StringBuilder status = new StringBuilder("Monitoring: ")
                .append(mon.isRunning() ? "RUNNING (" + mon.getIntervalMs() + " ms interval)" : "stopped");
        if (latest != null && latest.timestampMs > 0) {
            status.append(" · last update ").append(new java.util.Date(latest.timestampMs));
        }
        if (scan != null) {
            status.append(" · scan ").append(scan.status)
                    .append(" (started ").append(scan.startedAt)
                    .append(", finished ").append(scan.finishedAt).append(")");
        }
        setTextSafe(monitorStatus, status.toString());

        setTextSafe(cpuInfo, formatCpu(scan, latest));
        setTextSafe(ramInfo, formatRam(scan, latest));
        setTextSafe(jvmInfo, formatJvm(scan, latest));
        setTextSafe(gpuInfo, formatGpu(scan, latest));

        if (scan != null) {
            String cuda = scan.cudaStatus + (scan.cudaDetail == null || scan.cudaDetail.isEmpty()
                    ? "" : " — " + scan.cudaDetail);
            setTextSafe(cudaScanInfo, "CUDA: " + cuda);
        } else {
            setTextSafe(cudaScanInfo, "CUDA: not scanned");
        }

        CudaDiagnosticReport report = CudaDiagnosticService.getInstance().getCurrent();
        if (report != null) {
            String stageSummary = summarizeStages(report);
            setTextSafe(cudaTestInfo, "CUDA backend test: " + report.overallStatus
                    + " (" + report.totalDurationMs() + " ms) — " + stageSummary);
            setEnabledSafe(detailsBtn, true);
        } else {
            setTextSafe(cudaTestInfo, "CUDA backend test: not executed");
            setEnabledSafe(detailsBtn, false);
        }
    }

    private static String summarizeStages(CudaDiagnosticReport r) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (DiagnosticStage s : r.getStages()) {
            if (s.status == eu.kalafatic.evolution.controller.hardware.StageStatus.SKIPPED) {
                continue;
            }
            if (!first) sb.append(" · ");
            first = false;
            sb.append(s.name).append(": ").append(s.status);
        }
        return sb.toString();
    }

    private static String formatCpu(HardwareScanResult scan, HardwareSnapshot s) {
        if (scan != null && scan.cpu != null) {
            HardwareScanResult.CpuInfo c = scan.cpu;
            return String.format("CPU: %s %s · %d phys / %d logical · freq %s · temp %s · load %s",
                    c.vendor, c.model, c.physicalCores, c.logicalProcessors,
                    c.frequencyMhz > 0 ? String.format("%.0f MHz (max %.0f MHz)", c.frequencyMhz, c.maxFrequencyMhz) : "N/A",
                    c.temperatureCelsius > 0 ? c.temperatureCelsius + " °C" : "N/A",
                    c.loadAverage1m > 0 ? c.loadAverage1m : "N/A");
        }
        return "CPU: " + (s != null ? s.cpuModel : "—")
                + " (" + (s != null ? s.logicalProcessors : 0) + " logical processors)";
    }

    private static String formatRam(HardwareScanResult scan, HardwareSnapshot s) {
        if (scan != null && scan.ram != null) {
            HardwareScanResult.RamInfo r = scan.ram;
            double util = r.totalBytes > 0 ? (r.usedBytes * 100.0 / r.totalBytes) : -1;
            return String.format("RAM: %s used / %s total (%.1f%%) · %s available · %s free",
                    gib(r.usedBytes), gib(r.totalBytes), util, gib(r.availableBytes), gib(r.freeBytes));
        }
        return "RAM: " + (s != null ? gib(s.ramUsedBytes) + " / " + gib(s.ramTotalBytes) : "—");
    }

    private static String formatJvm(HardwareScanResult scan, HardwareSnapshot s) {
        if (scan != null && scan.jvm != null) {
            HardwareScanResult.JvmInfo j = scan.jvm;
            return String.format("JVM: heap %s / %s max · non-heap %s · Java %s",
                    gib(j.heapUsedBytes), gib(j.heapMaxBytes), gib(j.nonHeapBytes), j.jvmVersion);
        }
        return "JVM: " + (s != null ? gib(s.processHeapBytes) + " heap / " + gib(s.jvmMaxHeapBytes) + " max" : "—");
    }

    private static String formatGpu(HardwareScanResult scan, HardwareSnapshot s) {
        if (scan != null && !scan.gpus.isEmpty()) {
            StringBuilder sb = new StringBuilder("GPU:");
            for (HardwareScanResult.GpuInfo g : scan.gpus) {
                sb.append("  #").append(g.index).append(" ").append(g.name)
                        .append(" · VRAM ").append(g.usedVramMb).append(" / ").append(g.totalVramMb)
                        .append(" MB · driver ").append(g.driverVersion);
                if (!g.computeCapability.isEmpty()) {
                    sb.append(" · CC ").append(g.computeCapability);
                }
                if (g.utilizationPercent >= 0) {
                    sb.append(" · util ").append(g.utilizationPercent).append("%");
                }
                if (g.temperatureCelsius > 0) {
                    sb.append(" · ").append(g.temperatureCelsius).append(" °C");
                }
                sb.append(";");
            }
            return sb.toString();
        }
        if (s != null && s.gpuAvailable) {
            return "GPU: " + s.gpuName + " · " + gib(s.vramUsedBytes) + " / " + gib(s.vramTotalBytes);
        }
        return "GPU: no NVIDIA GPU detected";
    }

    private static String gib(long bytes) {
        if (bytes <= 0) return "N/A";
        return String.format("%.1f GiB", bytes / (1024.0 * 1024 * 1024));
    }
}
