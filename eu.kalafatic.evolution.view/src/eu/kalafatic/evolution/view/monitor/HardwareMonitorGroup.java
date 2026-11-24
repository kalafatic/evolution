package eu.kalafatic.evolution.view.monitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

/**
 * Live hardware monitoring panel (GPU / CPU / RAM) with historical sparkline charts.
 *
 * <p>The panel is a self-contained SWT {@link Composite} that owns a
 * {@link HardwareMetricsCollector}. Metric samples arrive on a background thread and are
 * marshalled to the SWT UI thread via {@link Display#asyncExec}, so the UI thread is never
 * blocked. The collector and chart history are bounded; the panel stops and cleans up on
 * dispose.</p>
 */
public final class HardwareMonitorGroup extends Composite {

    private static final int SAMPLE_INTERVAL_MS = 1000;
    private static final int HISTORY_POINTS = 300;

    private final HardwareMetricsCollector collector;

    private Label gpuName;
    private Label gpuUtil;
    private Label gpuMem;
    private Label gpuTemp;
    private Label gpuPower;
    private Label gpuState;
    private Label cpuUtil;
    private Label cpuInfo;
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

        Group gpuGroup = new Group(this, SWT.NONE);
        gpuGroup.setText("GPU");
        gpuGroup.setLayout(new GridLayout(2, true));
        gpuGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        gpuName = label(gpuGroup, "Model: —");
        gpuUtil = label(gpuGroup, "Utilization: —");
        gpuMem = label(gpuGroup, "VRAM: —");
        gpuTemp = label(gpuGroup, "Temperature: —");
        gpuPower = label(gpuGroup, "Power: —");
        gpuState = label(gpuGroup, "State: —");

        Group cpuGroup = new Group(this, SWT.NONE);
        cpuGroup.setText("CPU");
        cpuGroup.setLayout(new GridLayout(2, true));
        cpuGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        cpuUtil = label(cpuGroup, "Utilization: —");
        cpuInfo = label(cpuGroup, "Model: —");

        Group ramGroup = new Group(this, SWT.NONE);
        ramGroup.setText("RAM");
        ramGroup.setLayout(new GridLayout(2, true));
        ramGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ramInfo = label(ramGroup, "Total: —");
        ramUtil = label(ramGroup, "Utilization: —");
        processMem = label(ramGroup, "Process memory: —");

        Group charts = new Group(this, SWT.NONE);
        charts.setText("Historical");
        charts.setLayout(new GridLayout(2, true));
        charts.setLayoutData(new GridData(GridData.FILL_BOTH));
        chartGpu = new SparklineChart(charts, HISTORY_POINTS, "GPU", "%", 0x2e, 0xcc, 0x71);
        chartVram = new SparklineChart(charts, HISTORY_POINTS, "VRAM", "%", 0x34, 0x98, 0xdb);
        chartCpu = new SparklineChart(charts, HISTORY_POINTS, "CPU", "%", 0xe6, 0x7e, 0x22);
        chartRam = new SparklineChart(charts, HISTORY_POINTS, "RAM", "%", 0x9b, 0x59, 0xb6);
        for (SparklineChart c : new SparklineChart[]{chartGpu, chartVram, chartCpu, chartRam}) {
            c.setLayoutData(new GridData(GridData.FILL_BOTH));
        }

        collector = new HardwareMetricsCollector(SAMPLE_INTERVAL_MS, this::onSample);
        collector.start();

        addListener(SWT.Dispose, e -> {
            collector.stop();
            chartGpu.clear(); chartVram.clear(); chartCpu.clear(); chartRam.clear();
        });
    }

    private static Label label(Composite parent, String text) {
        Label l = new Label(parent, SWT.NONE);
        l.setText(text);
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return l;
    }

    /** Called on the sampling thread with a fresh snapshot. */
    private void onSample(HardwareSnapshot s) {
        Display display = getDisplay();
        if (display == null || display.isDisposed() || isDisposed()) return;
        display.asyncExec(() -> {
            if (isDisposed()) return;
            update(s);
        });
    }

    private void update(HardwareSnapshot s) {
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
