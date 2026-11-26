package eu.kalafatic.evolution.view.monitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

/**
 * Live hardware monitoring panel (GPU / CPU / RAM) with historical sparkline charts,
 * organized as ONE expandable/collapsible group.
 *
 * <p>Collapsed state: a single compact header row with a live summary
 * (GPU / CPU / RAM / EVO process memory). Expanded state: three compact device
 * sections side by side plus a full-width historical chart row. The Forge
 * application below receives all released vertical space.</p>
 *
 * <p>The panel is a self-contained SWT {@link Composite} that consumes the shared
 * {@link HardwareMonitoringService} — it owns no sampling thread of its own. Metric
 * samples arrive on the central background thread and are marshalled to the SWT UI
 * thread via {@link Display#asyncExec}. Chart history is bounded; the panel
 * unsubscribes on dispose (the shared service keeps running for the other hardware
 * consumers). Collapsing the group does NOT stop monitoring: the listener stays
 * attached and history keeps accumulating, so re-expanding shows the full history.</p>
 */
public final class HardwareMonitorGroup extends Composite {

    private static final int HISTORY_POINTS = 300;
    private static final int CHART_HEIGHT = 88;

    private final java.util.function.Consumer<HardwareSnapshot> sampleListener;
    private final org.eclipse.swt.graphics.Font boldFont;

    private boolean expanded = false;

    private Label toggleBtn;
    private Label summaryGpu;
    private Label summaryCpu;
    private Label summaryRam;
    private Label summaryProc;
    private Composite body;

    // GPU section
    private Label gpuName;
    private Label gpuUtil;
    private Label gpuMem;
    private Label gpuTemp;
    private Label gpuPower;
    private Label gpuState;

    // CPU section
    private Label cpuUtil;
    private Label cpuInfo;

    // RAM section
    private Label ramInfo;
    private Label ramUtil;
    private Label processMem;

    private SparklineChart chartGpu;
    private SparklineChart chartVram;
    private SparklineChart chartCpu;
    private SparklineChart chartRam;

    public HardwareMonitorGroup(Composite parent) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(1, false));

        org.eclipse.swt.graphics.FontData[] fontData = Display.getDefault().getSystemFont().getFontData();
        for (org.eclipse.swt.graphics.FontData fd : fontData) {
            fd.setStyle(SWT.BOLD);
        }
        boldFont = new org.eclipse.swt.graphics.Font(Display.getDefault(), fontData);

        createHeaderRow();
        createBody();

        sampleListener = this::onSample;
        HardwareMonitoringService service = HardwareMonitoringService.getInstance();
        service.addListener(sampleListener);
        // Preserve the historical live behaviour: the panel shows live metrics
        // as long as it exists. The service itself is shared app-wide.
        if (!service.isRunning()) {
            service.start();
        }
        // Render the latest snapshot immediately so the summary is populated
        // before the first periodic sample arrives.
        onSample(service.getLatest());

        // Initial state: collapsed, so the Forge workspace receives the space.
        applyExpandedState(false);

        addListener(SWT.Dispose, e -> {
            HardwareMonitoringService.getInstance().removeListener(sampleListener);
            chartGpu.clear(); chartVram.clear(); chartCpu.clear(); chartRam.clear();
            if (boldFont != null && !boldFont.isDisposed()) {
                boldFont.dispose();
            }
        });
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    private void createHeaderRow() {
        Composite header = new Composite(this, SWT.NONE);
        GridLayout headerLayout = new GridLayout(10, false);
        headerLayout.marginWidth = 6;
        headerLayout.marginHeight = 1;
        headerLayout.verticalSpacing = 0;
        headerLayout.horizontalSpacing = 8;
        header.setLayout(headerLayout);
        header.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Compact toggle indicator: a Label arrow keeps a predictable size on
        // all GTK themes (a Button would force the row to the theme minimum).
        toggleBtn = new Label(header, SWT.CENTER);
        toggleBtn.setText("\u25B8");
        toggleBtn.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        toggleBtn.setToolTipText("Expand / collapse hardware monitoring");
        GridData toggleData = new GridData(GridData.CENTER, GridData.CENTER, false, false);
        toggleData.widthHint = 16;
        toggleBtn.setLayoutData(toggleData);
        Listener toggle = e -> setExpanded(!expanded);
        toggleBtn.addListener(SWT.MouseDown, toggle);

        Label title = new Label(header, SWT.NONE);
        title.setText("Hardware Monitoring");
        title.setFont(boldFont);
        title.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        title.setToolTipText("Expand / collapse hardware monitoring");
        title.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, false, false));
        title.addListener(SWT.MouseDown, toggle);

        summaryGpu = summaryLabel(header);
        summaryCpu = summaryLabel(header);
        summaryRam = summaryLabel(header);
        summaryProc = summaryLabel(header);
    }

    private static Label summaryLabel(Composite parent) {
        Label sep = new Label(parent, SWT.SEPARATOR | SWT.VERTICAL);
        GridData sepData = new GridData(GridData.CENTER, GridData.CENTER, false, false);
        // GTK vertical separators have an oversized natural height; constrain
        // them so they cannot inflate the header row.
        sepData.heightHint = 14;
        sep.setLayoutData(sepData);
        Label l = new Label(parent, SWT.NONE);
        GridData gd = new GridData(GridData.FILL, GridData.CENTER, true, false);
        gd.horizontalIndent = 4;
        l.setLayoutData(gd);
        return l;
    }

    private void createBody() {
        body = new Composite(this, SWT.NONE);
        GridLayout bodyLayout = new GridLayout(3, true);
        bodyLayout.marginWidth = 4;
        bodyLayout.marginHeight = 4;
        bodyLayout.horizontalSpacing = 6;
        bodyLayout.verticalSpacing = 6;
        body.setLayout(bodyLayout);
        GridData bodyData = new GridData(GridData.FILL_HORIZONTAL);
        body.setLayoutData(bodyData);

        Composite gpuSection = section(body, "GPU");
        gpuName = label(gpuSection, "Model: —");
        gpuUtil = label(gpuSection, "Utilization: —");
        gpuMem = label(gpuSection, "VRAM: —");
        gpuTemp = label(gpuSection, "Temperature: —");
        gpuPower = label(gpuSection, "Power: —");
        gpuState = label(gpuSection, "State: —");

        Composite cpuSection = section(body, "CPU");
        cpuUtil = label(cpuSection, "Utilization: —");
        cpuInfo = label(cpuSection, "Model: —");

        Composite ramSection = section(body, "RAM");
        ramInfo = label(ramSection, "Total: —");
        ramUtil = label(ramSection, "Utilization: —");
        processMem = label(ramSection, "Process memory: —");

        Composite chartsSection = section(body, "HISTORICAL");
        chartsSection.setLayout(new GridLayout(4, true));
        GridData chartsData = new GridData(GridData.FILL_BOTH);
        chartsData.horizontalSpan = 3;
        chartsData.heightHint = CHART_HEIGHT + 28;
        chartsSection.setLayoutData(chartsData);
        chartGpu = new SparklineChart(chartsSection, HISTORY_POINTS, "GPU", "%", 0x2e, 0xcc, 0x71);
        chartVram = new SparklineChart(chartsSection, HISTORY_POINTS, "VRAM", "%", 0x34, 0x98, 0xdb);
        chartCpu = new SparklineChart(chartsSection, HISTORY_POINTS, "CPU", "%", 0xe6, 0x7e, 0x22);
        chartRam = new SparklineChart(chartsSection, HISTORY_POINTS, "RAM", "%", 0x9b, 0x59, 0xb6);
        for (SparklineChart c : new SparklineChart[]{chartGpu, chartVram, chartCpu, chartRam}) {
            c.setLayoutData(new GridData(GridData.FILL_BOTH));
        }
    }

    private Composite section(Composite parent, String title) {
        Composite section = new Composite(parent, SWT.BORDER);
        section.setLayout(new GridLayout(1, false));
        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Label caption = new Label(section, SWT.NONE);
        caption.setText(title);
        caption.setFont(boldFont);
        caption.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return section;
    }

    private static Label label(Composite parent, String text) {
        Label l = new Label(parent, SWT.NONE);
        l.setText(text);
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return l;
    }

    // ------------------------------------------------------------------
    // Expand / collapse
    // ------------------------------------------------------------------

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        applyExpandedState(true);
    }

    private void applyExpandedState(boolean relayoutParent) {
        if (body == null || toggleBtn == null) {
            return;
        }
        toggleBtn.setText(expanded ? "\u25BE" : "\u25B8");
        toggleBtn.setToolTipText(expanded ? "Collapse hardware monitoring" : "Expand hardware monitoring");
        GridData bodyData = (GridData) body.getLayoutData();
        bodyData.exclude = !expanded;
        body.setVisible(expanded);
        if (relayoutParent) {
            Composite parent = getParent();
            if (parent != null && !parent.isDisposed()) {
                parent.layout(true, true);
            }
        }
    }

    // ------------------------------------------------------------------
    // Live updates
    // ------------------------------------------------------------------

    /** Called on the sampling thread with a fresh snapshot. */
    private void onSample(HardwareSnapshot s) {
        Display display = getDisplay();
        if (display == null || display.isDisposed() || isDisposed()) {
            return;
        }
        display.asyncExec(() -> {
            if (!isDisposed()) {
                update(s);
            }
        });
    }

    private void update(HardwareSnapshot s) {
        // Detailed sections
        if (s.gpuAvailable) {
            gpuName.setText("Model: " + s.gpuName);
            gpuUtil.setText("Utilization: " + pct(s.gpuUtilizationPercent));
            gpuMem.setText("VRAM: " + mb(s.vramUsedBytes) + " / " + mb(s.vramTotalBytes)
                    + " (" + pct((int) s.vramUtilizationPercent()) + ")");
            gpuTemp.setText("Temperature: " + deg(s.gpuTemperatureCelsius));
            gpuPower.setText("Power: " + fmt(s.gpuPowerWatts) + " W / " + fmt(s.gpuPowerLimitWatts) + " W");
            gpuState.setText("State: " + s.gpuState + "  |  " + s.cudaStatus);
        } else {
            gpuName.setText("Model: No NVIDIA GPU detected");
            gpuUtil.setText("Utilization: —");
            gpuMem.setText("VRAM: —");
            gpuTemp.setText("Temperature: —");
            gpuPower.setText("Power: —");
            gpuState.setText("State: N/A");
        }

        cpuUtil.setText("Utilization: " + pct((int) Math.round(s.cpuUtilizationPercent)));
        cpuInfo.setText("Model: " + s.cpuModel + " (" + s.logicalProcessors + " logical CPUs"
                + (s.cpuFrequencyMhz > 0 ? ", " + String.format("%.0f", s.cpuFrequencyMhz) + " MHz" : "") + ")");

        ramInfo.setText("Total: " + mb(s.ramTotalBytes) + "  |  Used: " + mb(s.ramUsedBytes)
                + "  |  Available: " + mb(s.ramAvailableBytes));
        ramUtil.setText("Utilization: " + pct((int) Math.round(s.ramUtilizationPercent())));
        processMem.setText("Process memory: " + mb(s.processHeapBytes));

        chartGpu.push(s.gpuAvailable ? s.gpuUtilizationPercent : -1);
        chartVram.push(s.gpuAvailable ? s.vramUtilizationPercent() : -1);
        chartCpu.push(s.cpuUtilizationPercent);
        chartRam.push(s.ramUtilizationPercent());

        // Compact summary row (visible in both collapsed and expanded state)
        String gpu = s.gpuAvailable
                ? "GPU: " + shortName(s.gpuName) + " · " + pct(s.gpuUtilizationPercent)
                    + " · VRAM " + pct((int) s.vramUtilizationPercent())
                : "GPU: N/A";
        summaryGpu.setText(gpu);
        summaryCpu.setText("CPU: " + pct((int) Math.round(s.cpuUtilizationPercent)));
        summaryRam.setText("RAM: " + mb(s.ramUsedBytes) + " / " + mb(s.ramTotalBytes)
                + " (" + pct((int) Math.round(s.ramUtilizationPercent())) + ")");
        summaryProc.setText("EVO: " + mb(s.processHeapBytes));
    }

    /** Trims long GPU marketing names for the compact summary. */
    private static String shortName(String name) {
        if (name == null) {
            return "—";
        }
        String n = name.replace("NVIDIA ", "").trim();
        return n.length() > 26 ? n.substring(0, 25) + "…" : n;
    }

    private static String pct(int v) { return v < 0 ? "—" : v + "%"; }
    private static String deg(int v) { return v < 0 ? "—" : v + "°C"; }
    private static String fmt(double v) { return v < 0 ? "—" : String.format("%.1f", v); }
    private static String mb(long bytes) {
        if (bytes <= 0) return "—";
        if (bytes >= 1024L * 1024 * 1024) return String.format("%.1f GiB", bytes / (1024.0 * 1024 * 1024));
        return String.format("%.0f MiB", bytes / (1024.0 * 1024));
    }
}
