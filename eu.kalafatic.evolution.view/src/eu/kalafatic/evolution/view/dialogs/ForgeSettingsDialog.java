package eu.kalafatic.evolution.view.dialogs;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.manager.ModelSizePreset;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl;
import eu.kalafatic.evolution.forge.controller.service.impl.agents.ForgeTrainingEstimationAgent;
import eu.kalafatic.evolution.forge.data.api.service.DatasetPreparationResult;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.impl.service.DatasetPreparationService;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * FORGE Settings &amp; Live Training Control Center.
 *
 * Primary interactive Forge properties and monitoring interface:
 * - left:  configuration (model, training, termination, checkpoint/export, hardware)
 * - center: live training monitor (authoritative telemetry, loss trajectory,
 *           capacity budget, progress & resource monitor) with the tabbed
 *           monitoring graphs (Loss | LR | Throughput | Hardware | Memory |
 *           Timeline) docked at its bottom and a live zoom control
 * - right: datasets & input limits
 *
 * Honesty policy: every displayed metric originates from the authoritative
 * ForgeSessionManager telemetry or from the configured session state. Missing
 * values render as N/A. Estimates are explicitly labeled ANALYTICAL PRELIMINARY
 * until real measurements exist. No simulated progress, no fabricated metrics.
 */
public class ForgeSettingsDialog extends Dialog {

    public static class DatasetItem implements ForgeTrainingEstimationAgent.IDatasetTargetItem {
        private boolean checked;
        private String path;
        private String type; // "FILE" or "FOLDER"
        private long recordLimit = -1;
        private double scaleRatio = 1.0;
        private long maxSizeBytes = -1;
        private int maxSeqLen = 1024;

        public DatasetItem(boolean checked, String path, String type) {
            this(checked, path, type, -1, 1.0, -1, 1024);
        }

        public DatasetItem(boolean checked, String path, String type, long recordLimit, double scaleRatio) {
            this(checked, path, type, recordLimit, scaleRatio, -1, 1024);
        }

        public DatasetItem(boolean checked, String path, String type, long recordLimit, double scaleRatio, long maxSizeBytes, int maxSeqLen) {
            this.checked = checked;
            this.path = path != null ? path : "";
            this.type = type != null ? type : "FOLDER";
            this.recordLimit = recordLimit;
            this.scaleRatio = scaleRatio > 0 ? scaleRatio : 1.0;
            this.maxSizeBytes = maxSizeBytes;
            this.maxSeqLen = maxSeqLen > 0 ? maxSeqLen : 1024;
        }

        public boolean isChecked() {
            return checked;
        }

        public void setChecked(boolean checked) {
            this.checked = checked;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public long getRecordLimit() {
            return recordLimit;
        }

        public void setRecordLimit(long recordLimit) {
            this.recordLimit = recordLimit;
        }

        public double getScaleRatio() {
            return scaleRatio;
        }

        public void setScaleRatio(double scaleRatio) {
            this.scaleRatio = scaleRatio;
        }

        public long getMaxSizeBytes() {
            return maxSizeBytes;
        }

        public void setMaxSizeBytes(long maxSizeBytes) {
            this.maxSizeBytes = maxSizeBytes;
        }

        public int getMaxSeqLen() {
            return maxSeqLen;
        }

        public void setMaxSeqLen(int maxSeqLen) {
            this.maxSeqLen = maxSeqLen;
        }

        public JSONObject toJsonObject() {
            JSONObject obj = new JSONObject();
            obj.put("checked", checked);
            obj.put("path", path);
            obj.put("type", type);
            obj.put("recordLimit", recordLimit);
            obj.put("scaleRatio", scaleRatio);
            obj.put("maxSizeBytes", maxSizeBytes);
            obj.put("maxSeqLen", maxSeqLen);
            return obj;
        }

        public static DatasetItem fromJsonObject(JSONObject obj) {
            boolean checked = obj.optBoolean("checked", true);
            String path = obj.optString("path", "");
            String type = obj.optString("type", "FOLDER");
            long recordLimit = obj.optLong("recordLimit", -1);
            double scaleRatio = obj.optDouble("scaleRatio", 1.0);
            long maxSizeBytes = obj.optLong("maxSizeBytes", -1);
            int maxSeqLen = obj.optInt("maxSeqLen", 1024);
            return new DatasetItem(checked, path, type, recordLimit, scaleRatio, maxSizeBytes, maxSeqLen);
        }
    }

    public static int getCpuCores() {
        return ForgeTrainingEstimationAgent.getInstance().getCpuCores();
    }

    public static long getMaxMemoryMb() {
        return ForgeTrainingEstimationAgent.getInstance().getMaxMemoryMb();
    }

    public static String getHardwareProfile() {
        return ForgeTrainingEstimationAgent.getInstance().getHardwareProfile();
    }

    public static double getModelComplexityFactor(String sizeName) {
        return ForgeTrainingEstimationAgent.getInstance().getModelComplexityFactor(sizeName);
    }

    public static double getModelParameterCount(String modelSize) {
        return ForgeTelemetrySnapshot.resolveParamCount(modelSize);
    }

    public static double getBaselineTokensPerSecond(String modelSize) {
        return ForgeTrainingEstimationAgent.getInstance().getBaselineTokensPerSecond(getModelParameterCount(modelSize));
    }

    public static long calculatePathSize(String path) {
        return ForgeTrainingEstimationAgent.getInstance().calculatePathSize(path);
    }

    public long calculateTotalDatasetBytes() {
        return ForgeTrainingEstimationAgent.getInstance().calculateTotalDatasetBytes(datasetItems);
    }

    public long estimateTotalTrainingTokens() {
        return ForgeTrainingEstimationAgent.getInstance().estimateTotalTrainingTokens(datasetItems, selectedEpochs);
    }

    public double getEstimatedForgingSeconds() {
        return calculateEstimatedForgingSeconds(selectedModelSize, selectedEpochs, calculateTotalDatasetBytes());
    }

    public static double calculateEstimatedForgingSeconds(String modelSize, int epochs, long datasetBytes) {
        return ForgeTrainingEstimationAgent.getInstance()
                .calculateEstimatedForgingSecondsWithParams(getModelParameterCount(modelSize), epochs, datasetBytes);
    }

    public static String formatDuration(double seconds) {
        return ForgeTrainingEstimationAgent.getInstance().formatDuration(seconds);
    }

    public boolean applySmartDataScaling(double targetHours) {
        return ForgeTrainingEstimationAgent.getInstance().applySmartDataScaling(datasetItems, selectedModelSize, selectedEpochs, targetHours).isSuccess();
    }

    private Combo quickPresetCombo;
    private Combo modelSizeCombo;
    private Combo epochCombo;
    private Combo batchSizeCombo;
    private Combo learningRateCombo;
    private Combo generationsCombo;
    private Combo candidatesCombo;
    private Combo checkpointFreqCombo;
    private Combo lossThresholdCombo;
    private Combo desiredLossCombo;
    private Combo targetTimeCombo;
    private Combo refreshIntervalCombo;
    private org.eclipse.swt.widgets.Text maxDataSizeText;
    private org.eclipse.swt.widgets.Text maxSampleCountText;
    private org.eclipse.swt.widgets.Text maxTokensLimitText;
    private org.eclipse.swt.widgets.Text maxSeqLenText;
    private org.eclipse.swt.widgets.Text valRatioText;
    private org.eclipse.swt.widgets.Text outputModelNameText;
    private Button qualityFilterCheck;
    private Button earlyStoppingCheck;
    private Button saveEpochCheckpointsCheck;
    private Button quantizationCheck;
    private Button autoRefreshCheck;
    private Combo quantizationSchemeCombo;
    private Label estTimeLabel;
    private Label hwProfileLabel;
    private Browser graphBrowser;
    private Browser monitorBrowser;
    private Table datasetsTable;

    // Live monitor widgets (SWT, authoritative telemetry driven)
    private Label sessionStatusLabel;
    private Label sessionNameLabel;
    private Label phaseLabel;
    private Label lastUpdatedLabel;
    private Label staleLabel;
    private ProgressBar liveProgressBar;
    private Label liveMetricsLabel;
    private Label liveEtaLabel;

    // Live monitor zoom (applied to both the center graphs and the monitoring tabs)
    private Label zoomLevelLabel;
    private double liveMonitorZoom = 1.0;
    private static final double ZOOM_STEP = 0.1;
    private static final double ZOOM_MIN = 0.6;
    private static final double ZOOM_MAX = 2.5;

    private Button startTrainingBtn;
    private Button pauseBtn;
    private Button resumeBtn;
    private Button cancelBtn;
    private Button finishBtn;
    private Button refreshBtn;
    private Button reportBtn;

    private String selectedModelSize = "SMALL";
    private int selectedEpochs = 4;
    private int selectedBatchSize = 16;
    private double selectedLearningRate = 0.01;
    private int selectedGenerations = 1;
    private int selectedCandidates = 1;
    private int selectedCheckpointFreq = 5;
    private long selectedMaxDataSizeMb = 500;
    private long selectedMaxSampleCount = 5000;
    private int selectedMaxTokensLimit = 100000;
    private int selectedMaxSeqLen = 1024;
    private double selectedValRatio = 0.15;
    private boolean selectedQualityFilter = true;
    private boolean selectedEarlyStoppingEnabled = true;
    private boolean selectedSaveEpochCheckpoints = true;
    private boolean selectedQuantizationEnabled = true;
    private String selectedQuantizationScheme = "F16";
    private String selectedOutputModelName = "";
    private String selectedLossThreshold = "Epoch 16-30: Loss 2-5 → Learning phrases";
    private double selectedDesiredLoss = 1.0;
    private double selectedTargetHours = 12.0;
    private double[] lossHistory = null;
    private List<DatasetItem> datasetItems = new ArrayList<>();

    // Live monitoring state
    private ForgeTelemetrySnapshot lastSnapshot = null;
    private String lastCenterHtml = "";
    private String lastMonitorHtml = "";
    private long lastRefreshMs = 0;
    private int autoRefreshIntervalMs = 3000;
    private boolean disposedTimer = false;

    // Reusable auto-refresh listeners for UI controls
    private SelectionListener autoRefreshSelectionAdapter;
    private ModifyListener autoRefreshModifyAdapter;

    private static final String[] EPOCH_OPTIONS = new String[] {
        "2", "4", "8", "16", "32", "64", "128", "256", "512", "1024"
    };

    private static final String[] LOSS_THRESHOLD_OPTIONS = new String[] {
        "Epoch 1-5: Loss 8-10 → Learning letters (g, u, a)",
        "Epoch 6-15: Loss 5-8 → Learning words (evolution, genome)",
        "Epoch 16-30: Loss 2-5 → Learning phrases (\"evolution is\")",
        "Epoch 31-50: Loss 0.5-2 → Learning sentences",
        "Epoch 51-64: Loss < 1 → Understanding concepts"
    };

    private static final String[] DESIRED_LOSS_OPTIONS = new String[] {
        "2.0", "1.5", "1.0", "0.8", "0.5", "0.2", "0.1", "0.05"
    };

    private static final String[] TARGET_TIME_OPTIONS = new String[] {
        "0.5 hrs (30m)", "1.0 hr", "2.0 hrs", "6.0 hrs", "12.0 hrs", "24.0 hrs", "48.0 hrs"
    };

    public ForgeSettingsDialog(Shell parentShell, String modelSize, int epochs, String lossThreshold) {
        this(parentShell, modelSize, epochs, lossThreshold, 1.0, null, null);
    }

    public ForgeSettingsDialog(Shell parentShell, String modelSize, int epochs, String lossThreshold, double desiredLossThreshold) {
        this(parentShell, modelSize, epochs, lossThreshold, desiredLossThreshold, null, null);
    }

    public ForgeSettingsDialog(Shell parentShell, String modelSize, int epochs, String lossThreshold, double[] lossHistory) {
        this(parentShell, modelSize, epochs, lossThreshold, 1.0, lossHistory, null);
    }

    public ForgeSettingsDialog(Shell parentShell, String modelSize, int epochs, String lossThreshold, double desiredLossThreshold, double[] lossHistory) {
        this(parentShell, modelSize, epochs, lossThreshold, desiredLossThreshold, lossHistory, null);
    }

    public ForgeSettingsDialog(Shell parentShell, String modelSize, int epochs, String lossThreshold, double desiredLossThreshold, double[] lossHistory, List<DatasetItem> datasets) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
        if (modelSize != null && !modelSize.isEmpty()) {
            this.selectedModelSize = modelSize;
        }
        if (epochs > 0) {
            this.selectedEpochs = epochs;
        }
        if (lossThreshold != null && !lossThreshold.isEmpty()) {
            this.selectedLossThreshold = lossThreshold;
        }
        if (desiredLossThreshold > 0.0) {
            this.selectedDesiredLoss = desiredLossThreshold;
        }
        this.lossHistory = lossHistory;
        if (datasets != null) {
            this.datasetItems = new ArrayList<>(datasets);
        }
    }

    public void setDatasetsFromJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) return;
        try {
            JSONArray arr = new JSONArray(jsonStr);
            this.datasetItems.clear();
            for (int i = 0; i < arr.length(); i++) {
                this.datasetItems.add(DatasetItem.fromJsonObject(arr.getJSONObject(i)));
            }
            if (datasetsTable != null && !datasetsTable.isDisposed()) {
                refreshDatasetsTable();
            }
        } catch (Exception ex) {}
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("FORGE Settings & Live Training Control Center");
    }

    @Override
    protected Point getInitialSize() {
        return new Point(1380, 900);
    }

    @Override
    public boolean close() {
        stopAutoRefresh();
        return super.close();
    }

    private void stopAutoRefresh() {
        disposedTimer = true;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, true));

        createTopStatusStrip(container);

        SashForm sashForm = new SashForm(container, SWT.HORIZONTAL | SWT.SMOOTH);
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

        // Reusable auto-refresh listeners for UI controls
        autoRefreshSelectionAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                readEffectiveSettingsFromUi();
                scheduleGraphUpdate();
            }
        };

        autoRefreshModifyAdapter = e -> {
            readEffectiveSettingsFromUi();
            scheduleGraphUpdate();
        };

        // Panel 1 (Left): Model & Hardware Settings Panel
        Composite leftContainer = createPanelContainer(sashForm, "Configuration & Hardware");

        ScrolledComposite leftScrolled = new ScrolledComposite(leftContainer, SWT.V_SCROLL | SWT.H_SCROLL);
        leftScrolled.setExpandHorizontal(true);
        leftScrolled.setExpandVertical(true);
        leftScrolled.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite settingsPanel = new Composite(leftScrolled, SWT.NONE);
        settingsPanel.setLayout(new GridLayout(2, false));
        leftScrolled.setContent(settingsPanel);

        settingsPanel.addListener(SWT.Resize, event -> {
            Point size = settingsPanel.computeSize(leftScrolled.getClientArea().width, SWT.DEFAULT, true);
            leftScrolled.setMinSize(size);
        });

        buildPresetGroup(settingsPanel);
        buildModelTrainingGroup(settingsPanel);
        buildTerminationGroup(settingsPanel);
        buildCheckpointExportGroup(settingsPanel);
        buildHardwareGroup(settingsPanel);

        updateHardwareAndTimeInfo();

        // Panel 2 (Center): Live Training Monitor
        Composite middleContainer = createPanelContainer(sashForm, "Live Training Monitor");

        Composite centerWrap = new Composite(middleContainer, SWT.NONE);
        centerWrap.setLayout(new GridLayout(1, false));
        centerWrap.setLayoutData(new GridData(GridData.FILL_BOTH));

        buildLiveMonitorCards(centerWrap);
        buildZoomBar(centerWrap);

        SashForm monitorSplit = new SashForm(centerWrap, SWT.VERTICAL | SWT.SMOOTH);
        monitorSplit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        graphBrowser = new Browser(monitorSplit, SWT.NONE);

        Group monitorGroup = new Group(monitorSplit, SWT.NONE);
        monitorGroup.setText("Monitoring Graphs (Loss | Learning Rate | Throughput | Hardware | Memory | Timeline)");
        monitorGroup.setLayout(new GridLayout(1, true));

        monitorBrowser = new Browser(monitorGroup, SWT.NONE);
        monitorBrowser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        installZoomGestures(graphBrowser);
        installZoomGestures(monitorBrowser);

        monitorSplit.setWeights(new int[] { 60, 40 });

        // Panel 3 (Right): Datasets & Input Limits
        Composite rightContainer = createPanelContainer(sashForm, "Datasets & Input Limits");

        ScrolledComposite rightScrolled = new ScrolledComposite(rightContainer, SWT.V_SCROLL | SWT.H_SCROLL);
        rightScrolled.setExpandHorizontal(true);
        rightScrolled.setExpandVertical(true);
        rightScrolled.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite rightPanel = new Composite(rightScrolled, SWT.NONE);
        rightPanel.setLayout(new GridLayout(1, true));
        rightScrolled.setContent(rightPanel);

        rightPanel.addListener(SWT.Resize, event -> {
            Point size = rightPanel.computeSize(rightScrolled.getClientArea().width, SWT.DEFAULT, true);
            rightScrolled.setMinSize(size);
        });

        buildDatasetsGroup(rightPanel);

        sashForm.setWeights(new int[] { 28, 48, 24 });

        // Initial HTML render
        renderAll();

        // Start the automatic refresh loop (UI-thread timer; never blocks SWT).
        disposedTimer = false;
        scheduleAutoRefresh();

        return container;
    }

    // =====================================================================
    // TOP STATUS STRIP + LIVE MONITOR CARDS (SWT)
    // =====================================================================

    private void createTopStatusStrip(Composite container) {
        Group strip = new Group(container, SWT.NONE);
        strip.setText("Session & Telemetry");
        strip.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        strip.setLayout(new GridLayout(10, false));

        GUIFactory.INSTANCE.createLabel(strip, "Session:");
        sessionNameLabel = new Label(strip, SWT.NONE);
        sessionNameLabel.setText("No forge session");
        sessionNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        GUIFactory.INSTANCE.createLabel(strip, "Status:");
        sessionStatusLabel = new Label(strip, SWT.NONE);
        sessionStatusLabel.setText("UNKNOWN");
        sessionStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        GUIFactory.INSTANCE.createLabel(strip, "Phase:");
        phaseLabel = new Label(strip, SWT.NONE);
        phaseLabel.setText("—");
        phaseLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        GUIFactory.INSTANCE.createLabel(strip, "Updated:");
        lastUpdatedLabel = new Label(strip, SWT.NONE);
        lastUpdatedLabel.setText("—");
        lastUpdatedLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        staleLabel = new Label(strip, SWT.NONE);
        staleLabel.setText("");
        staleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        autoRefreshCheck = GUIFactory.INSTANCE.createCheckButton(strip, "Auto Refresh");
        autoRefreshCheck.setSelection(true);
        autoRefreshCheck.setToolTipText("Automatically reload authoritative training telemetry while this dialog is open.");
        autoRefreshCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                scheduleAutoRefresh();
            }
        });

        refreshIntervalCombo = new Combo(strip, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (String opt : new String[] { "1 s", "2 s", "3 s", "5 s", "10 s" }) refreshIntervalCombo.add(opt);
        refreshIntervalCombo.select(2);
        refreshIntervalCombo.setToolTipText("Automatic refresh interval (2-5 s recommended).");
        refreshIntervalCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                parseRefreshInterval();
                scheduleAutoRefresh();
            }
        });

        refreshBtn = GUIFactory.INSTANCE.createButton(strip, "Refresh Now");
        refreshBtn.setToolTipText("Reload all session information, training state and graphs from the authoritative FORGE telemetry. Does not restart, reset or modify training.");
        refreshBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleRefreshNow();
            }
        });
    }

    private void parseRefreshInterval() {
        if (refreshIntervalCombo == null || refreshIntervalCombo.isDisposed()) return;
        try {
            String text = refreshIntervalCombo.getText().replaceAll("[^0-9]", "");
            int sec = Integer.parseInt(text.isEmpty() ? "3" : text);
            autoRefreshIntervalMs = Math.max(1000, Math.min(60_000, sec * 1000));
        } catch (Exception ignored) {
            autoRefreshIntervalMs = 3000;
        }
    }

    private void buildLiveMonitorCards(Composite parent) {
        Group cards = new Group(parent, SWT.NONE);
        cards.setText("Training Status (authoritative telemetry)");
        cards.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        cards.setLayout(new GridLayout(4, false));

        liveProgressBar = new ProgressBar(cards, SWT.HORIZONTAL);
        liveProgressBar.setMinimum(0);
        liveProgressBar.setMaximum(100);
        liveProgressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));

        liveMetricsLabel = new Label(cards, SWT.WRAP);
        liveMetricsLabel.setText("No training session data yet. Start training or press Refresh Now.");
        liveMetricsLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));

        liveEtaLabel = new Label(cards, SWT.WRAP);
        liveEtaLabel.setText("ETA: N/A");
        liveEtaLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
    }

    private void buildZoomBar(Composite parent) {
        Composite bar = new Composite(parent, SWT.NONE);
        bar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        GridLayout layout = new GridLayout(7, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 0;
        bar.setLayout(layout);

        Label caption = new Label(bar, SWT.NONE);
        caption.setText("Monitor Zoom:");

        Button zoomOutBtn = new Button(bar, SWT.PUSH);
        zoomOutBtn.setText("\u2212");
        zoomOutBtn.setToolTipText("Zoom out the live training monitor (Ctrl + Mouse Wheel also works).");
        GridData outData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        outData.widthHint = 32;
        outData.heightHint = 22;
        zoomOutBtn.setLayoutData(outData);
        zoomOutBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                adjustZoom(-ZOOM_STEP);
            }
        });

        zoomLevelLabel = new Label(bar, SWT.CENTER);
        zoomLevelLabel.setText("100%");
        zoomLevelLabel.setToolTipText("Current live training monitor zoom.");
        GridData levelData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        levelData.widthHint = 48;
        zoomLevelLabel.setLayoutData(levelData);

        Button zoomInBtn = new Button(bar, SWT.PUSH);
        zoomInBtn.setText("+");
        zoomInBtn.setToolTipText("Zoom in the live training monitor (Ctrl + Mouse Wheel also works).");
        GridData inData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        inData.widthHint = 32;
        inData.heightHint = 22;
        zoomInBtn.setLayoutData(inData);
        zoomInBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                adjustZoom(ZOOM_STEP);
            }
        });

        Button zoomResetBtn = new Button(bar, SWT.PUSH);
        zoomResetBtn.setText("Reset");
        zoomResetBtn.setToolTipText("Reset the live training monitor zoom to 100%.");
        GridData resetData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        resetData.heightHint = 22;
        zoomResetBtn.setLayoutData(resetData);
        zoomResetBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                resetZoom();
            }
        });

        Button openBrowserBtn = new Button(bar, SWT.PUSH);
        openBrowserBtn.setText("Open in Browser");
        openBrowserBtn.setToolTipText("Open the current training monitor diagram in your default web browser (interactive, self-contained HTML).");
        GridData openData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        openData.heightHint = 22;
        openBrowserBtn.setLayoutData(openData);
        openBrowserBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openDiagramInBrowser();
            }
        });

        Label hint = new Label(bar, SWT.NONE);
        hint.setText("Ctrl + Mouse Wheel zooms too");
        hint.setEnabled(false);
        hint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private void installZoomGestures(Browser browser) {
        browser.addMouseWheelListener(e -> {
            if ((e.stateMask & SWT.CTRL) != 0) {
                adjustZoom(e.count > 0 ? ZOOM_STEP : -ZOOM_STEP);
            }
        });
    }

    private void adjustZoom(double delta) {
        double next = Math.round((liveMonitorZoom + delta) * 100.0) / 100.0;
        next = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, next));
        if (Math.abs(next - liveMonitorZoom) < 0.0001) return;
        liveMonitorZoom = next;
        applyZoomToBrowsers();
    }

    private void resetZoom() {
        if (Math.abs(liveMonitorZoom - 1.0) < 0.0001) return;
        liveMonitorZoom = 1.0;
        applyZoomToBrowsers();
    }

    private void applyZoomToBrowsers() {
        applyBrowserZoom(graphBrowser);
        applyBrowserZoom(monitorBrowser);
        if (zoomLevelLabel != null && !zoomLevelLabel.isDisposed()) {
            zoomLevelLabel.setText(String.format("%.0f%%", liveMonitorZoom * 100.0));
        }
    }

    private void applyBrowserZoom(Browser browser) {
        if (browser == null || browser.isDisposed()) return;
        try {
            browser.execute("document.documentElement.style.overflow='auto';"
                    + "document.body.style.overflow='auto';"
                    + "document.body.style.zoom='" + String.format(java.util.Locale.ROOT, "%.2f", liveMonitorZoom) + "';");
        } catch (Exception ignored) {
        }
    }

    private String withInitialZoom(String html) {
        String zoom = String.format(java.util.Locale.ROOT, "%.2f", liveMonitorZoom);
        String script = "<script>document.body.style.zoom='" + zoom + "';</script>\n";
        return html.replace("</body>", script + "</body>");
    }

    // =====================================================================
    // LEFT PANEL BUILDERS
    // =====================================================================

    private void buildPresetGroup(Composite settingsPanel) {
        Group presetGroup = new Group(settingsPanel, SWT.NONE);
        presetGroup.setText("Configuration Preset");
        presetGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        presetGroup.setLayout(new GridLayout(2, false));

        GUIFactory.INSTANCE.createLabel(presetGroup, "Quick Preset:");
        quickPresetCombo = GUIFactory.INSTANCE.createCombo(presetGroup, new String[] {
            "Custom (User Controlled)",
            "Quick Test (1 Epoch, Nano Model, Fast Check)",
            "Small Chat Experiment (Small Model, 16 Epochs)"
        });
        quickPresetCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        quickPresetCombo.select(0);
        quickPresetCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = quickPresetCombo.getSelectionIndex();
                if (idx == 1) { // Quick Test
                    selectedModelSize = "NANO";
                    selectedEpochs = 1;
                    selectedBatchSize = 16;
                    selectedLearningRate = 0.01;
                    selectedGenerations = 1;
                    selectedCandidates = 1;
                } else if (idx == 2) { // Small Chat Experiment
                    selectedModelSize = "SMALL";
                    selectedEpochs = 16;
                    selectedBatchSize = 16;
                    selectedLearningRate = 0.001;
                    selectedGenerations = 1;
                    selectedCandidates = 1;
                }
                updateControlsFromFields();
                scheduleGraphUpdate();
            }
        });
    }

    private void buildModelTrainingGroup(Composite settingsPanel) {
        Group settingsGroup = new Group(settingsPanel, SWT.NONE);
        settingsGroup.setText("Model & Training Configuration");
        settingsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        settingsGroup.setLayout(new GridLayout(4, false));

        GUIFactory.INSTANCE.createLabel(settingsGroup, "Preset Model Size:");
        modelSizeCombo = GUIFactory.INSTANCE.createCombo(settingsGroup);
        modelSizeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        for (ModelSizePreset.Size size : ModelSizePreset.Size.values()) {
            modelSizeCombo.add(size.getDisplayName());
        }
        int defaultSizeIdx = 3;
        for (int i = 0; i < ModelSizePreset.Size.values().length; i++) {
            if (ModelSizePreset.Size.values()[i].name().equalsIgnoreCase(selectedModelSize) ||
                ModelSizePreset.Size.values()[i].getDisplayName().equalsIgnoreCase(selectedModelSize)) {
                defaultSizeIdx = i;
                break;
            }
        }
        modelSizeCombo.select(defaultSizeIdx);
        modelSizeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = modelSizeCombo.getSelectionIndex();
                if (idx >= 0 && idx < ModelSizePreset.Size.values().length) {
                    selectedModelSize = ModelSizePreset.Size.values()[idx].name();
                } else {
                    selectedModelSize = modelSizeCombo.getText();
                }
                scheduleGraphUpdate();
            }
        });

        Label epochLabel = GUIFactory.INSTANCE.createLabel(settingsGroup, "Epoch Count:");
        epochLabel.setToolTipText("Epochs: How many full passes the trainer makes through the training dataset for one model.");
        epochCombo = new Combo(settingsGroup, SWT.DROP_DOWN);
        epochCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        epochCombo.setToolTipText("Epochs: How many full passes the trainer makes through the training dataset for one model.");
        for (String opt : EPOCH_OPTIONS) {
            epochCombo.add(opt);
        }
        epochCombo.setText(String.valueOf(selectedEpochs));
        epochCombo.addSelectionListener(autoRefreshSelectionAdapter);
        epochCombo.addModifyListener(autoRefreshModifyAdapter);

        GUIFactory.INSTANCE.createLabel(settingsGroup, "Batch Size:");
        batchSizeCombo = new Combo(settingsGroup, SWT.DROP_DOWN);
        batchSizeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        for (String b : new String[] { "2", "4", "8", "16", "32", "64" }) batchSizeCombo.add(b);
        batchSizeCombo.setText(String.valueOf(selectedBatchSize));
        batchSizeCombo.addSelectionListener(autoRefreshSelectionAdapter);
        batchSizeCombo.addModifyListener(autoRefreshModifyAdapter);

        GUIFactory.INSTANCE.createLabel(settingsGroup, "Learning Rate:");
        learningRateCombo = new Combo(settingsGroup, SWT.DROP_DOWN);
        learningRateCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        for (String lr : new String[] { "0.05", "0.01", "0.005", "0.001", "0.0005", "0.0001" }) learningRateCombo.add(lr);
        learningRateCombo.setText(String.valueOf(selectedLearningRate));
        learningRateCombo.addSelectionListener(autoRefreshSelectionAdapter);
        learningRateCombo.addModifyListener(autoRefreshModifyAdapter);

        // Optimizer: the trainer uses the embedded AdamW (fixed architecture).
        GUIFactory.INSTANCE.createLabel(settingsGroup, "Optimizer:");
        Label optimizerLabel = new Label(settingsGroup, SWT.NONE);
        optimizerLabel.setText("AdamW (embedded, fixed)");
        optimizerLabel.setToolTipText("The EvoLlmTrainer uses its embedded AdamW optimizer (decoupled weight decay, bias correction, global L2 clipping). Not configurable.");
        optimizerLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        GUIFactory.INSTANCE.createLabel(settingsGroup, "Sequence Length (tokens):");
        maxSeqLenText = GUIFactory.INSTANCE.createText(settingsGroup);
        maxSeqLenText.setText(String.valueOf(selectedMaxSeqLen));
        maxSeqLenText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        maxSeqLenText.setToolTipText("Maximum sequence length used for training samples.");
        maxSeqLenText.addModifyListener(autoRefreshModifyAdapter);

        Label genLabel = GUIFactory.INSTANCE.createLabel(settingsGroup, "Generations:");
        genLabel.setToolTipText("Generations: How many evolutionary rounds are executed.");
        generationsCombo = new Combo(settingsGroup, SWT.DROP_DOWN);
        generationsCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        generationsCombo.setToolTipText("Generations: How many evolutionary rounds are executed.");
        for (String g : new String[] { "1", "2", "3", "5", "10" }) generationsCombo.add(g);
        generationsCombo.setText(String.valueOf(selectedGenerations));
        generationsCombo.addSelectionListener(autoRefreshSelectionAdapter);
        generationsCombo.addModifyListener(autoRefreshModifyAdapter);

        Label candLabel = GUIFactory.INSTANCE.createLabel(settingsGroup, "Candidates / Gen:");
        candLabel.setToolTipText("Candidates: How many model candidates are trained or evaluated in each evolutionary generation.");
        candidatesCombo = new Combo(settingsGroup, SWT.DROP_DOWN);
        candidatesCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        candidatesCombo.setToolTipText("Candidates: How many model candidates are trained or evaluated in each evolutionary generation.");
        for (String c : new String[] { "1", "2", "3", "4", "8" }) candidatesCombo.add(c);
        candidatesCombo.setText(String.valueOf(selectedCandidates));
        candidatesCombo.addSelectionListener(autoRefreshSelectionAdapter);
        candidatesCombo.addModifyListener(autoRefreshModifyAdapter);

        GUIFactory.INSTANCE.createLabel(settingsGroup, "Output Model Name:");
        outputModelNameText = GUIFactory.INSTANCE.createText(settingsGroup);
        outputModelNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
        outputModelNameText.setText(selectedOutputModelName != null ? selectedOutputModelName : "");
        outputModelNameText.addModifyListener(autoRefreshModifyAdapter);
    }

    private void buildTerminationGroup(Composite settingsPanel) {
        Group termGroup = new Group(settingsPanel, SWT.NONE);
        termGroup.setText("Termination Criteria & Loss Targets");
        termGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        termGroup.setLayout(new GridLayout(2, false));

        GUIFactory.INSTANCE.createLabel(termGroup, "Loss Stage Reference (visual only):");
        lossThresholdCombo = GUIFactory.INSTANCE.createCombo(termGroup);
        lossThresholdCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        lossThresholdCombo.setToolTipText("Visual reference for the loss graph only; the backend early-stopping uses the Desired Finish Loss value.");
        for (String lossOpt : LOSS_THRESHOLD_OPTIONS) {
            lossThresholdCombo.add(lossOpt);
        }
        int defaultLossIdx = 2;
        for (int i = 0; i < LOSS_THRESHOLD_OPTIONS.length; i++) {
            if (LOSS_THRESHOLD_OPTIONS[i].toLowerCase().contains(selectedLossThreshold.toLowerCase()) ||
                selectedLossThreshold.toLowerCase().contains(LOSS_THRESHOLD_OPTIONS[i].toLowerCase())) {
                defaultLossIdx = i;
                break;
            }
        }
        lossThresholdCombo.select(defaultLossIdx);
        lossThresholdCombo.addSelectionListener(autoRefreshSelectionAdapter);

        Label desiredLossLabel = GUIFactory.INSTANCE.createLabel(termGroup, "Desired Finish Loss (Early Stop):");
        desiredLossLabel.setToolTipText("Supported: when the measured loss drops below this target, the trainer stops early.");
        desiredLossCombo = new Combo(termGroup, SWT.DROP_DOWN);
        desiredLossCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        desiredLossCombo.setToolTipText("Supported: when the measured loss drops below this target, the trainer stops early.");
        for (String opt : DESIRED_LOSS_OPTIONS) {
            desiredLossCombo.add(opt);
        }
        int defaultDesiredIdx = 2;
        for (int i = 0; i < DESIRED_LOSS_OPTIONS.length; i++) {
            if (DESIRED_LOSS_OPTIONS[i].equals(String.format("%.2f", selectedDesiredLoss)) ||
                DESIRED_LOSS_OPTIONS[i].equals(String.valueOf(selectedDesiredLoss))) {
                defaultDesiredIdx = i;
                break;
            }
        }
        desiredLossCombo.select(defaultDesiredIdx);
        if (desiredLossCombo.getSelectionIndex() < 0) {
            desiredLossCombo.setText(String.valueOf(selectedDesiredLoss));
        }
        desiredLossCombo.addSelectionListener(autoRefreshSelectionAdapter);
        desiredLossCombo.addModifyListener(autoRefreshModifyAdapter);

        GUIFactory.INSTANCE.createLabel(termGroup, "Target Forging Duration (Hard Budget):");
        targetTimeCombo = new Combo(termGroup, SWT.DROP_DOWN);
        targetTimeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        targetTimeCombo.setToolTipText("Supported: hard wall-clock budget. The backend stops training cleanly when the budget is exceeded.");
        for (String opt : TARGET_TIME_OPTIONS) {
            targetTimeCombo.add(opt);
        }
        targetTimeCombo.select(4); // Default "12.0 hrs"
        targetTimeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedTargetHours = ForgeTrainingEstimationAgent.getInstance().parseHoursOption(targetTimeCombo.getText());
                scheduleGraphUpdate();
            }
        });

        GUIFactory.INSTANCE.createLabel(termGroup, "");
        earlyStoppingCheck = GUIFactory.INSTANCE.createCheckButton(termGroup, "Enable Early Stopping");
        earlyStoppingCheck.setSelection(selectedEarlyStoppingEnabled);
        earlyStoppingCheck.setToolTipText("Supported: stops training when the measured loss reaches the Desired Finish Loss.");
        earlyStoppingCheck.addSelectionListener(autoRefreshSelectionAdapter);

        Button smartScaleBtn = GUIFactory.INSTANCE.createButton(termGroup, "Smart Scale Data for Target Time");
        smartScaleBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        smartScaleBtn.setToolTipText("Automatically scale dataset record limits and adjust epoch parameters to fit training within the target duration.");
        smartScaleBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleSmartScaling();
            }
        });
    }

    private void buildCheckpointExportGroup(Composite settingsPanel) {
        Group cpGroup = new Group(settingsPanel, SWT.NONE);
        cpGroup.setText("Checkpoint & Export Configuration");
        cpGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        cpGroup.setLayout(new GridLayout(2, false));

        GUIFactory.INSTANCE.createLabel(cpGroup, "Checkpoint Freq (Epochs):");
        checkpointFreqCombo = new Combo(cpGroup, SWT.DROP_DOWN);
        checkpointFreqCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        for (String cf : new String[] { "1", "2", "5", "10", "20" }) checkpointFreqCombo.add(cf);
        checkpointFreqCombo.setText(String.valueOf(selectedCheckpointFreq));
        checkpointFreqCombo.addSelectionListener(autoRefreshSelectionAdapter);
        checkpointFreqCombo.addModifyListener(autoRefreshModifyAdapter);

        saveEpochCheckpointsCheck = GUIFactory.INSTANCE.createCheckButton(cpGroup, "Save Model Checkpoint After Each Epoch (.evo format)");
        saveEpochCheckpointsCheck.setSelection(selectedSaveEpochCheckpoints);
        saveEpochCheckpointsCheck.setToolTipText("Automatically save a timestamped .evo model checkpoint after each epoch for debugging training & forging progress.");
        saveEpochCheckpointsCheck.addSelectionListener(autoRefreshSelectionAdapter);
        GridData scData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
        saveEpochCheckpointsCheck.setLayoutData(scData);

        quantizationCheck = GUIFactory.INSTANCE.createCheckButton(cpGroup, "Enable Model Quantization (.evo & GGUF export)");
        quantizationCheck.setSelection(selectedQuantizationEnabled);
        quantizationCheck.setToolTipText("Produce quantized model variants (F16, INT8, Q8_0, Q4_K_M) after forging completes. INT4 is honestly rejected by the backend.");
        quantizationCheck.addSelectionListener(autoRefreshSelectionAdapter);
        GridData qcData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
        quantizationCheck.setLayoutData(qcData);

        GUIFactory.INSTANCE.createLabel(cpGroup, "Quantization Scheme:");
        quantizationSchemeCombo = new Combo(cpGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (String qOpt : new String[] { "F16", "INT8", "INT4", "Q8_0", "Q4_0", "Q4_K_M" }) {
            quantizationSchemeCombo.add(qOpt);
        }
        quantizationSchemeCombo.setText(selectedQuantizationScheme);
        quantizationSchemeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        quantizationSchemeCombo.addSelectionListener(autoRefreshSelectionAdapter);
    }

    private void buildHardwareGroup(Composite settingsPanel) {
        Group hwGroup = new Group(settingsPanel, SWT.NONE);
        hwGroup.setText("Hardware, Backend & Time Estimation");
        hwGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        hwGroup.setLayout(new GridLayout(1, false));

        hwProfileLabel = new Label(hwGroup, SWT.WRAP);
        hwProfileLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        estTimeLabel = new Label(hwGroup, SWT.WRAP);
        estTimeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label backendNote = new Label(hwGroup, SWT.WRAP);
        backendNote.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        backendNote.setText("Backend: pure-Java JVM CPU trainer (float32). GPU acceleration is NOT active even when GPU hardware is detected; "
                + "preliminary estimates are analytical and are replaced by measured values as training progresses.");
    }

    // =====================================================================
    // RIGHT PANEL: DATASETS
    // =====================================================================

    private void buildDatasetsGroup(Composite rightPanel) {
        Group datasetsGroup = new Group(rightPanel, SWT.NONE);
        datasetsGroup.setText("Training Datasets & Dataset Input Limits");
        datasetsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        datasetsGroup.setLayout(new GridLayout(2, false));

        Composite datasetLimitsComp = new Composite(datasetsGroup, SWT.NONE);
        datasetLimitsComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        datasetLimitsComp.setLayout(new GridLayout(4, false));

        GUIFactory.INSTANCE.createLabel(datasetLimitsComp, "Max Data Size (MB):");
        maxDataSizeText = GUIFactory.INSTANCE.createText(datasetLimitsComp);
        maxDataSizeText.setText(String.valueOf(selectedMaxDataSizeMb));
        maxDataSizeText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        maxDataSizeText.setToolTipText("Supported: target usable dataset size. EVO compiles usable records into .evodata until this target is reached.");
        maxDataSizeText.addModifyListener(autoRefreshModifyAdapter);

        GUIFactory.INSTANCE.createLabel(datasetLimitsComp, "Max Sample Count:");
        maxSampleCountText = GUIFactory.INSTANCE.createText(datasetLimitsComp);
        maxSampleCountText.setText(String.valueOf(selectedMaxSampleCount));
        maxSampleCountText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        maxSampleCountText.setToolTipText("Supported: maximum training samples generated for the trainer (default cap: 5000).");
        maxSampleCountText.addModifyListener(autoRefreshModifyAdapter);

        GUIFactory.INSTANCE.createLabel(datasetLimitsComp, "Max Token Limit:");
        maxTokensLimitText = GUIFactory.INSTANCE.createText(datasetLimitsComp);
        maxTokensLimitText.setText(String.valueOf(selectedMaxTokensLimit));
        maxTokensLimitText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        maxTokensLimitText.setToolTipText("Supported: maximum encoded tokens used for training (default cap: 100000).");
        maxTokensLimitText.addModifyListener(autoRefreshModifyAdapter);

        GUIFactory.INSTANCE.createLabel(datasetLimitsComp, "Validation Ratio (%):");
        valRatioText = GUIFactory.INSTANCE.createText(datasetLimitsComp);
        valRatioText.setText(String.valueOf((int) (selectedValRatio * 100)));
        valRatioText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        valRatioText.addModifyListener(autoRefreshModifyAdapter);

        qualityFilterCheck = GUIFactory.INSTANCE.createCheckButton(datasetLimitsComp, "Quality Filtering & Rejection (not yet applied by backend)");
        qualityFilterCheck.setSelection(selectedQualityFilter);
        qualityFilterCheck.setToolTipText("Unsupported: this option is recorded in the configuration but the training engine does not consume it yet. The dataset preparation pipeline applies normalization and deduplication only.");
        qualityFilterCheck.addSelectionListener(autoRefreshSelectionAdapter);
        GridData qfData = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
        qualityFilterCheck.setLayoutData(qfData);

        datasetsTable = new Table(datasetsGroup, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        datasetsTable.setHeaderVisible(true);
        datasetsTable.setLinesVisible(true);
        GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
        tableData.heightHint = 150;
        datasetsTable.setLayoutData(tableData);

        TableColumn colUse = new TableColumn(datasetsTable, SWT.LEFT);
        colUse.setText("Use");
        colUse.setWidth(45);

        TableColumn colPath = new TableColumn(datasetsTable, SWT.LEFT);
        colPath.setText("Path / Target Source");
        colPath.setWidth(240);

        TableColumn colType = new TableColumn(datasetsTable, SWT.LEFT);
        colType.setText("Type");
        colType.setWidth(65);

        datasetsTable.addListener(SWT.Selection, event -> {
            if (event.detail == SWT.CHECK && event.item instanceof TableItem) {
                TableItem item = (TableItem) event.item;
                Object data = item.getData();
                if (data instanceof DatasetItem) {
                    ((DatasetItem) data).setChecked(item.getChecked());
                    scheduleGraphUpdate();
                }
            }
        });

        Composite btnComp = new Composite(datasetsGroup, SWT.NONE);
        btnComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        btnComp.setLayout(new GridLayout(1, true));

        Button addFileBtn = GUIFactory.INSTANCE.createButton(btnComp, "Add File...");
        Button addFolderBtn = GUIFactory.INSTANCE.createButton(btnComp, "Add Folder...");
        Button selectTargetBtn = GUIFactory.INSTANCE.createButton(btnComp, "Select Target...");
        Button removeBtn = GUIFactory.INSTANCE.createButton(btnComp, "Remove");
        Button moveUpBtn = GUIFactory.INSTANCE.createButton(btnComp, "Move Up");
        Button moveDownBtn = GUIFactory.INSTANCE.createButton(btnComp, "Move Down");
        Button selectAllBtn = GUIFactory.INSTANCE.createButton(btnComp, "Select All");
        Button createEvodataBtn = GUIFactory.INSTANCE.createButton(btnComp, "Create Native .evodata");

        createEvodataBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleCreateEvodata();
            }
        });

        addFileBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dlg = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
                dlg.setText("Select Training Data Files");
                eu.kalafatic.utils.application.FileUtils.configureDialogFilterPath(dlg, null);
                if (dlg.open() != null) {
                    String filterPath = dlg.getFilterPath();
                    String[] fileNames = dlg.getFileNames();
                    for (String fileName : fileNames) {
                        java.nio.file.Path fullPath = java.nio.file.Paths.get(filterPath, fileName);
                        DatasetItem ds = new DatasetItem(true, fullPath.toString(), "FILE");
                        datasetItems.add(ds);
                    }
                    refreshDatasetsTable();
                }
            }
        });

        addFolderBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dlg = new DirectoryDialog(getShell());
                dlg.setText("Select Training Data Directory");
                eu.kalafatic.utils.application.FileUtils.configureDialogFilterPath(dlg, null);
                String selectedDir = dlg.open();
                if (selectedDir != null && !selectedDir.trim().isEmpty()) {
                    DatasetItem ds = new DatasetItem(true, selectedDir, "FOLDER");
                    datasetItems.add(ds);
                    refreshDatasetsTable();
                }
            }
        });

        selectTargetBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dlg = new FileDialog(getShell(), SWT.OPEN);
                dlg.setText("Select Forging Target Path (.evo model, workspace, or dataset file)");
                dlg.setFilterExtensions(new String[] { "*.evo;*.txt;*.pdf;*.html;*.json;*.md;*.csv", "*.*" });
                eu.kalafatic.utils.application.FileUtils.configureDialogFilterPath(dlg, null);
                String selectedFile = dlg.open();
                if (selectedFile != null && !selectedFile.trim().isEmpty()) {
                    boolean found = false;
                    for (DatasetItem item : datasetItems) {
                        if (item.getPath().equalsIgnoreCase(selectedFile)) {
                            item.setChecked(true);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        datasetItems.add(new DatasetItem(true, selectedFile, selectedFile.endsWith(".evo") ? "EVO_MODEL" : "FILE"));
                    }
                    refreshDatasetsTable();
                }
            }
        });

        removeBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int[] indices = datasetsTable.getSelectionIndices();
                if (indices != null && indices.length > 0) {
                    Arrays.sort(indices);
                    for (int i = indices.length - 1; i >= 0; i--) {
                        if (indices[i] >= 0 && indices[i] < datasetItems.size()) {
                            datasetItems.remove(indices[i]);
                        }
                    }
                    refreshDatasetsTable();
                }
            }
        });

        moveUpBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = datasetsTable.getSelectionIndex();
                if (idx > 0 && idx < datasetItems.size()) {
                    DatasetItem item = datasetItems.remove(idx);
                    datasetItems.add(idx - 1, item);
                    refreshDatasetsTable();
                    datasetsTable.setSelection(idx - 1);
                }
            }
        });

        moveDownBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = datasetsTable.getSelectionIndex();
                if (idx >= 0 && idx < datasetItems.size() - 1) {
                    DatasetItem item = datasetItems.remove(idx);
                    datasetItems.add(idx + 1, item);
                    refreshDatasetsTable();
                    datasetsTable.setSelection(idx + 1);
                }
            }
        });

        selectAllBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean anyUnchecked = false;
                for (DatasetItem item : datasetItems) {
                    if (!item.isChecked()) {
                        anyUnchecked = true;
                        break;
                    }
                }
                for (DatasetItem item : datasetItems) {
                    item.setChecked(anyUnchecked);
                }
                refreshDatasetsTable();
            }
        });

        refreshDatasetsTable();
    }

    private Composite createPanelContainer(SashForm sashForm, String titleText) {
        Composite panelContainer = new Composite(sashForm, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 2;
        layout.marginHeight = 2;
        layout.verticalSpacing = 4;
        panelContainer.setLayout(layout);
        panelContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite headerComp = new Composite(panelContainer, SWT.NONE);
        GridLayout headerLayout = new GridLayout(2, false);
        headerLayout.marginWidth = 4;
        headerLayout.marginHeight = 2;
        headerComp.setLayout(headerLayout);
        headerComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label titleLabel = new Label(headerComp, SWT.NONE);
        titleLabel.setText(titleText);
        titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button maxBtn = new Button(headerComp, SWT.PUSH | SWT.FLAT);
        GridData btnData = new GridData(SWT.END, SWT.CENTER, false, false);
        btnData.widthHint = 24;
        btnData.heightHint = 20;
        maxBtn.setLayoutData(btnData);
        maxBtn.setText("\uD83D\uDDD6"); // 🗖 maximize symbol
        maxBtn.setToolTipText("Maximize Panel");

        maxBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (sashForm.getMaximizedControl() == panelContainer) {
                    sashForm.setMaximizedControl(null);
                    int[] lastWeights = (int[]) sashForm.getData("lastPanelWeights");
                    if (lastWeights != null && lastWeights.length == 3) {
                        sashForm.setWeights(lastWeights);
                    } else {
                        sashForm.setWeights(new int[] { 30, 44, 26 });
                    }
                    maxBtn.setText("\uD83D\uDDD6"); // 🗖
                    maxBtn.setToolTipText("Maximize Panel");
                } else {
                    sashForm.setData("lastPanelWeights", sashForm.getWeights());
                    sashForm.setMaximizedControl(panelContainer);
                    maxBtn.setText("\uD83D\uDDD7"); // 🗗 restore symbol
                    maxBtn.setToolTipText("Restore Panel Layout");
                }
                sashForm.layout(true, true);
            }
        });

        return panelContainer;
    }

    // =====================================================================
    // LIVE REFRESH
    // =====================================================================

    /**
     * Manual Refresh Now: reloads ALL monitoring information from the
     * authoritative ForgeSessionManager state. Does not restart training,
     * does not modify configuration, does not clear history.
     */
    private void handleRefreshNow() {
        captureAndRender(true);
    }

    private void scheduleAutoRefresh() {
        if (getShell() == null || getShell().isDisposed() || disposedTimer) return;
        parseRefreshInterval();
        final boolean auto = autoRefreshCheck != null && !autoRefreshCheck.isDisposed() && autoRefreshCheck.getSelection();
        getShell().getDisplay().timerExec(autoRefreshIntervalMs, () -> {
            if (disposedTimer || getShell() == null || getShell().isDisposed()) return;
            if (auto) {
                captureAndRender(false);
            }
            scheduleAutoRefresh();
        });
    }

    private void scheduleGraphUpdate() {
        if (getShell() == null || getShell().isDisposed()) return;
        getShell().getDisplay().timerExec(200, () -> {
            if (disposedTimer || getShell() == null || getShell().isDisposed()) return;
            // Configuration changed: refresh the derived estimate labels (model
            // size, epochs, dataset size, estimated duration) alongside the
            // diagrams. Debounced and user-driven, never on the telemetry
            // auto-refresh path, so it adds no per-tick directory scans during
            // active training.
            updateHardwareAndTimeInfo();
            captureAndRender(true);
        });
    }

    /**
     * Captures the authoritative snapshot and re-renders everything.
     * All state reads are cheap (volatile scalars + bounded history copy) and
     * run on the SWT UI thread; the refresh is safe during active training.
     */
    private void captureAndRender(boolean force) {
        // Re-read the live working-copy configuration from the UI controls so the
        // snapshot always reflects what the user is currently editing, regardless
        // of which refresh path (graph update, manual refresh, auto refresh)
        // triggered this capture.
        readEffectiveSettingsFromUiSafe();
        ForgeTelemetrySnapshot snap = captureSnapshot();
        if (snap == null) return;
        lastRefreshMs = System.currentTimeMillis();
        boolean changed = force || lastSnapshot == null || !snap.fingerprint().equals(lastSnapshot.fingerprint());
        lastSnapshot = snap;
        updateLiveSwtWidgets(snap, changed);
        if (changed) {
            renderAll();
        }
    }

    private ForgeTelemetrySnapshot captureSnapshot() {
        ForgeSessionManager mgr = ForgeSessionManager.getInstance();
        // The measurement store uses its default location (user.home/.evo-forge),
        // outside the project tree, so recording/persisting measurements never
        // affects the training corpus fingerprint.
        ForgeTrainingEstimationAgent.getInstance().loadDefaultMeasurements();
        // Overlay the dialog's live working-copy configuration onto the
        // authoritative telemetry snapshot: without this, the diagrams would
        // render the previous (committed) session configuration instead of the
        // values currently being edited.
        return ForgeTelemetrySnapshot.capture(mgr, resolveForgeSessionId(), lossHistory,
                calculateTotalDatasetBytes(), selectedModelSize, selectedEpochs,
                selectedDesiredLoss, selectedTargetHours, selectedEarlyStoppingEnabled);
    }

    private String resolveForgeSessionId() {
        try {
            ForgeSessionManager mgr = ForgeSessionManager.getInstance();
            String active = mgr.getActiveTrainingSessionId();
            if (active != null && !active.trim().isEmpty()) return active;
            List<eu.kalafatic.evolution.model.orchestration.ForgeSession> sessions = mgr.getSessions();
            for (eu.kalafatic.evolution.model.orchestration.ForgeSession s : sessions) {
                if (s == null || s.getSessionId() == null) continue;
                ForgeSessionManager.TrainingTelemetry t = mgr.getTelemetry(s.getSessionId());
                if (t != null && t.status != null && !"IDLE".equals(t.status) && !"UNKNOWN".equals(t.status)) {
                    return s.getSessionId();
                }
            }
            if (!sessions.isEmpty() && sessions.get(0) != null) {
                return sessions.get(0).getSessionId();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void updateLiveSwtWidgets(ForgeTelemetrySnapshot snap, boolean changed) {
        if (sessionNameLabel != null && !sessionNameLabel.isDisposed()) {
            String name = snap.sessionName != null && !snap.sessionName.isEmpty()
                    ? snap.sessionName + " (" + shortId(snap.sessionId) + ")"
                    : "No forge session";
            sessionNameLabel.setText(name);
        }
        if (sessionStatusLabel != null && !sessionStatusLabel.isDisposed()) {
            String text = snap.status;
            if (snap.workflowStatus != null && !snap.workflowStatus.isEmpty()) {
                text = snap.status + " · " + snap.workflowStatus;
            }
            sessionStatusLabel.setText(text);
        }
        if (phaseLabel != null && !phaseLabel.isDisposed()) {
            phaseLabel.setText(phaseName(snap));
        }
        if (lastUpdatedLabel != null && !lastUpdatedLabel.isDisposed()) {
            lastUpdatedLabel.setText(new SimpleDateFormat("HH:mm:ss").format(new Date(lastRefreshMs)));
        }
        if (staleLabel != null && !staleLabel.isDisposed()) {
            staleLabel.setText(stalenessText(snap));
        }

        if (liveProgressBar != null && !liveProgressBar.isDisposed()) {
            int pct = snap.overallProgress >= 0 ? (int) Math.round(snap.overallProgress * 100.0) : 0;
            if (liveProgressBar.getSelection() != pct) {
                liveProgressBar.setSelection(pct);
            }
        }
        if (liveMetricsLabel != null && !liveMetricsLabel.isDisposed() && changed) {
            liveMetricsLabel.setText(metricsText(snap));
        }
        if (liveEtaLabel != null && !liveEtaLabel.isDisposed() && changed) {
            liveEtaLabel.setText(etaText(snap));
        }
        if (changed) {
            updateActionButtonStates(snap);
        }
    }

    private static String shortId(String id) {
        if (id == null || id.isEmpty()) return "";
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    private static String phaseName(ForgeTelemetrySnapshot snap) {
        String status = snap.status != null ? snap.status.toUpperCase() : "UNKNOWN";
        String workflow = snap.workflowStatus != null ? snap.workflowStatus : "";
        if (workflow.contains("FINISHING") || workflow.contains("EXPORT")) return "EXPORTING";
        if (workflow.contains("RECOVERED")) return "INTERRUPTED";
        if ("RUNNING".equals(status) || "TRAINING".equals(status)) return "TRAINING";
        if ("PAUSED".equals(status)) return "PAUSED";
        if ("COMPLETED".equals(status)) return "COMPLETED";
        if ("STOPPED".equals(status)) return "STOPPED";
        if ("FAILED".equals(status)) return "FAILED";
        if ("ABORTED".equals(status) || "CANCELLED".equals(status)) return "CANCELLED";
        if ("INTERRUPTED".equals(status)) return "INTERRUPTED";
        if ("IDLE".equals(status)) return "IDLE";
        return "UNKNOWN";
    }

    private static String stalenessText(ForgeTelemetrySnapshot snap) {
        if (!snap.isTrainingActive) return "";
        long lastData = snap.capturedAtMs;
        if (!snap.history.isEmpty()) {
            lastData = snap.history.get(snap.history.size() - 1).timestamp;
        } else if (snap.startTimeMs > 0) {
            lastData = snap.startTimeMs;
        }
        long age = snap.capturedAtMs - lastData;
        if (age > 8000) return "⚠ STALE DATA";
        return "";
    }

    private static String metricsText(ForgeTelemetrySnapshot snap) {
        StringBuilder sb = new StringBuilder();
        if (snap.isTrainingActive || snap.totalEpochs > 0) {
            sb.append("EPOCH ").append(snap.currentEpoch).append(" / ").append(snap.totalEpochs);
            if (snap.epochProgress >= 0) {
                sb.append(" – ").append(String.format("%.0f%%", snap.epochProgress * 100.0));
            }
            if (snap.totalSteps > 0) {
                sb.append("  ·  Step ").append(snap.currentStep).append(" / ").append(snap.totalSteps);
            }
            if (snap.totalSamples > 0) {
                sb.append("  ·  Samples ").append(Math.min(snap.currentSample, snap.totalSamples))
                        .append(" / ").append(snap.totalSamples);
            }
            if (snap.currentLoss > 0) sb.append("  ·  Loss ").append(String.format("%.4f", snap.currentLoss));
            if (snap.valLoss > 0) sb.append("  ·  Val ").append(String.format("%.4f", snap.valLoss));
            if (snap.bestValLoss > 0) sb.append("  ·  Best Val ").append(String.format("%.4f", snap.bestValLoss));
            if (snap.initialLoss > 0) sb.append("  ·  Initial ").append(String.format("%.4f", snap.initialLoss));
            if (snap.learningRate > 0) sb.append("  ·  LR ").append(String.format("%.6f", snap.learningRate));
            if (snap.totalTokens > 0) {
                sb.append("  ·  Tokens ").append(fmtTokens(snap.processedTokens)).append(" / ").append(fmtTokens(snap.totalTokens));
            }
        } else {
            sb.append("No training in progress. ").append(snap.status).append(".");
        }
        return sb.toString();
    }

    private static String etaText(ForgeTelemetrySnapshot snap) {
        StringBuilder sb = new StringBuilder();
        sb.append("Elapsed: ").append(formatDuration(snap.elapsedSeconds));
        if (snap.estimatedRemainingSeconds >= 0.0) {
            sb.append("  ·  ETA remaining: ").append(formatDuration(snap.estimatedRemainingSeconds));
            long completion = (long) (System.currentTimeMillis() + snap.estimatedRemainingSeconds * 1000.0);
            sb.append("  ·  Completion: ").append(new SimpleDateFormat("HH:mm:ss").format(new Date(completion)));
            sb.append("  ·  Basis: ").append(describeEtaBasis(snap));
            sb.append("  ·  Confidence: ").append(String.format("%.0f%%", snap.etaConfidence * 100.0));
        } else if (snap.isTrainingActive) {
            sb.append("  ·  ETA: N/A (insufficient measurements)");
        } else {
            sb.append("  ·  ETA: N/A");
        }
        return sb.toString();
    }

    private static String describeEtaBasis(ForgeTelemetrySnapshot snap) {
        switch (snap.throughputBasis) {
            case MEASURED_LIVE: return "measured live throughput";
            case MEASURED_HISTORY: return "measured history";
            case ANALYTICAL_PRELIMINARY: return "analytical preliminary";
            default: return "unknown";
        }
    }

    private static String fmtTokens(long tokens) {
        if (tokens >= 1_000_000L) return String.format("%.2f M", tokens / 1_000_000.0);
        if (tokens >= 1_000L) return String.format("%.1f k", tokens / 1_000.0);
        return String.valueOf(tokens);
    }

    private void updateActionButtonStates(ForgeTelemetrySnapshot snap) {
        boolean active = snap.isTrainingActive;
        boolean paused = "PAUSED".equalsIgnoreCase(snap.status);
        boolean interrupted = "INTERRUPTED".equalsIgnoreCase(snap.status);
        boolean idle = !active && !paused;

        if (startTrainingBtn != null && !startTrainingBtn.isDisposed()) startTrainingBtn.setEnabled(idle);
        if (pauseBtn != null && !pauseBtn.isDisposed()) pauseBtn.setEnabled(active);
        if (resumeBtn != null && !resumeBtn.isDisposed()) resumeBtn.setEnabled(paused || interrupted);
        if (cancelBtn != null && !cancelBtn.isDisposed()) cancelBtn.setEnabled(active || paused);
        if (finishBtn != null && !finishBtn.isDisposed()) finishBtn.setEnabled(active || paused);
    }

    private void renderAll() {
        ForgeTelemetrySnapshot snap = lastSnapshot != null ? lastSnapshot : captureSnapshot();
        if (snap == null) return;
        String centerHtml = generateGraphHtml(snap);
        if (graphBrowser != null && !graphBrowser.isDisposed() && !centerHtml.equals(lastCenterHtml)) {
            lastCenterHtml = centerHtml;
            graphBrowser.setText(centerHtml);
            applyBrowserZoom(graphBrowser);
        }
        String monitorHtml = generateMonitoringGraphsHtml(snap);
        if (monitorBrowser != null && !monitorBrowser.isDisposed() && !monitorHtml.equals(lastMonitorHtml)) {
            lastMonitorHtml = monitorHtml;
            monitorBrowser.setText(monitorHtml);
            applyBrowserZoom(monitorBrowser);
        }
    }

    // =====================================================================
    // CONFIG READING / WRITING
    // =====================================================================

    private void readEffectiveSettingsFromUi() {
        if (modelSizeCombo != null && !modelSizeCombo.isDisposed()) {
            int idx = modelSizeCombo.getSelectionIndex();
            if (idx >= 0 && idx < ModelSizePreset.Size.values().length) {
                selectedModelSize = ModelSizePreset.Size.values()[idx].name();
            } else if (!modelSizeCombo.getText().trim().isEmpty()) {
                selectedModelSize = modelSizeCombo.getText().trim();
            }
        }

        if (epochCombo != null && !epochCombo.isDisposed()) {
            try {
                int parsed = Integer.parseInt(epochCombo.getText().trim());
                if (parsed > 0) selectedEpochs = parsed;
            } catch (Exception ignored) {}
        }

        if (batchSizeCombo != null && !batchSizeCombo.isDisposed()) {
            try {
                int parsed = Integer.parseInt(batchSizeCombo.getText().trim());
                if (parsed > 0) selectedBatchSize = parsed;
            } catch (Exception ignored) {}
        }

        if (learningRateCombo != null && !learningRateCombo.isDisposed()) {
            try {
                double parsed = Double.parseDouble(learningRateCombo.getText().trim());
                if (parsed > 0.0) selectedLearningRate = parsed;
            } catch (Exception ignored) {}
        }

        if (generationsCombo != null && !generationsCombo.isDisposed()) {
            try {
                int parsed = Integer.parseInt(generationsCombo.getText().trim());
                if (parsed > 0) selectedGenerations = parsed;
            } catch (Exception ignored) {}
        }

        if (candidatesCombo != null && !candidatesCombo.isDisposed()) {
            try {
                int parsed = Integer.parseInt(candidatesCombo.getText().trim());
                if (parsed > 0) selectedCandidates = parsed;
            } catch (Exception ignored) {}
        }

        if (checkpointFreqCombo != null && !checkpointFreqCombo.isDisposed()) {
            try {
                int parsed = Integer.parseInt(checkpointFreqCombo.getText().trim());
                if (parsed > 0) selectedCheckpointFreq = parsed;
            } catch (Exception ignored) {}
        }

        if (outputModelNameText != null && !outputModelNameText.isDisposed()) {
            selectedOutputModelName = outputModelNameText.getText().trim();
        }

        if (lossThresholdCombo != null && !lossThresholdCombo.isDisposed()) {
            if (!lossThresholdCombo.getText().trim().isEmpty()) {
                selectedLossThreshold = lossThresholdCombo.getText().trim();
            }
        }

        if (desiredLossCombo != null && !desiredLossCombo.isDisposed()) {
            try {
                double parsed = Double.parseDouble(desiredLossCombo.getText().trim());
                if (parsed >= 0.0) selectedDesiredLoss = parsed;
            } catch (Exception ignored) {}
        }

        if (targetTimeCombo != null && !targetTimeCombo.isDisposed()) {
            selectedTargetHours = ForgeTrainingEstimationAgent.getInstance().parseHoursOption(targetTimeCombo.getText());
        }

        if (maxDataSizeText != null && !maxDataSizeText.isDisposed()) {
            try {
                long parsed = Long.parseLong(maxDataSizeText.getText().trim());
                selectedMaxDataSizeMb = parsed;
            } catch (Exception ignored) {}
        }

        if (maxSampleCountText != null && !maxSampleCountText.isDisposed()) {
            try {
                long parsed = Long.parseLong(maxSampleCountText.getText().trim());
                selectedMaxSampleCount = parsed;
            } catch (Exception ignored) {}
        }

        if (maxTokensLimitText != null && !maxTokensLimitText.isDisposed()) {
            try {
                int parsed = Integer.parseInt(maxTokensLimitText.getText().trim());
                if (parsed > 0) selectedMaxTokensLimit = parsed;
            } catch (Exception ignored) {}
        }

        if (maxSeqLenText != null && !maxSeqLenText.isDisposed()) {
            try {
                int parsed = Integer.parseInt(maxSeqLenText.getText().trim());
                if (parsed > 0) selectedMaxSeqLen = parsed;
            } catch (Exception ignored) {}
        }

        if (valRatioText != null && !valRatioText.isDisposed()) {
            try {
                int pct = Integer.parseInt(valRatioText.getText().trim());
                selectedValRatio = Math.max(0.0, Math.min(0.5, pct / 100.0));
            } catch (Exception ignored) {}
        }

        if (qualityFilterCheck != null && !qualityFilterCheck.isDisposed()) {
            selectedQualityFilter = qualityFilterCheck.getSelection();
        }

        if (earlyStoppingCheck != null && !earlyStoppingCheck.isDisposed()) {
            selectedEarlyStoppingEnabled = earlyStoppingCheck.getSelection();
        }

        if (saveEpochCheckpointsCheck != null && !saveEpochCheckpointsCheck.isDisposed()) {
            selectedSaveEpochCheckpoints = saveEpochCheckpointsCheck.getSelection();
        }

        if (quantizationCheck != null && !quantizationCheck.isDisposed()) {
            selectedQuantizationEnabled = quantizationCheck.getSelection();
        }

        if (quantizationSchemeCombo != null && !quantizationSchemeCombo.isDisposed()) {
            selectedQuantizationScheme = quantizationSchemeCombo.getText().trim();
        }

        if (datasetsTable != null && !datasetsTable.isDisposed()) {
            for (int i = 0; i < datasetsTable.getItemCount(); i++) {
                TableItem item = datasetsTable.getItem(i);
                if (i < datasetItems.size()) {
                    datasetItems.get(i).setChecked(item.getChecked());
                }
            }
        }
    }

    private void updateControlsFromFields() {
        if (modelSizeCombo != null && !modelSizeCombo.isDisposed()) {
            for (int i = 0; i < ModelSizePreset.Size.values().length; i++) {
                if (ModelSizePreset.Size.values()[i].name().equalsIgnoreCase(selectedModelSize) ||
                    ModelSizePreset.Size.values()[i].getDisplayName().equalsIgnoreCase(selectedModelSize)) {
                    modelSizeCombo.select(i);
                    break;
                }
            }
        }
        if (epochCombo != null && !epochCombo.isDisposed()) {
            epochCombo.setText(String.valueOf(selectedEpochs));
        }
        if (batchSizeCombo != null && !batchSizeCombo.isDisposed()) {
            batchSizeCombo.setText(String.valueOf(selectedBatchSize));
        }
        if (learningRateCombo != null && !learningRateCombo.isDisposed()) {
            learningRateCombo.setText(String.valueOf(selectedLearningRate));
        }
        if (generationsCombo != null && !generationsCombo.isDisposed()) {
            generationsCombo.setText(String.valueOf(selectedGenerations));
        }
        if (candidatesCombo != null && !candidatesCombo.isDisposed()) {
            candidatesCombo.setText(String.valueOf(selectedCandidates));
        }
        if (checkpointFreqCombo != null && !checkpointFreqCombo.isDisposed()) {
            checkpointFreqCombo.setText(String.valueOf(selectedCheckpointFreq));
        }
        if (outputModelNameText != null && !outputModelNameText.isDisposed()) {
            outputModelNameText.setText(selectedOutputModelName != null ? selectedOutputModelName : "");
        }
        if (maxDataSizeText != null && !maxDataSizeText.isDisposed()) {
            maxDataSizeText.setText(String.valueOf(selectedMaxDataSizeMb));
        }
        if (maxSampleCountText != null && !maxSampleCountText.isDisposed()) {
            maxSampleCountText.setText(String.valueOf(selectedMaxSampleCount));
        }
        if (maxSeqLenText != null && !maxSeqLenText.isDisposed()) {
            maxSeqLenText.setText(String.valueOf(selectedMaxSeqLen));
        }
        if (valRatioText != null && !valRatioText.isDisposed()) {
            valRatioText.setText(String.valueOf((int) (selectedValRatio * 100)));
        }
        if (qualityFilterCheck != null && !qualityFilterCheck.isDisposed()) {
            qualityFilterCheck.setSelection(selectedQualityFilter);
        }
        if (saveEpochCheckpointsCheck != null && !saveEpochCheckpointsCheck.isDisposed()) {
            saveEpochCheckpointsCheck.setSelection(selectedSaveEpochCheckpoints);
        }
    }

    private void refreshDatasetsTable() {
        if (datasetsTable == null || datasetsTable.isDisposed()) return;
        datasetsTable.removeAll();
        for (DatasetItem item : datasetItems) {
            TableItem tableItem = new TableItem(datasetsTable, SWT.NONE);
            tableItem.setChecked(item.isChecked());
            tableItem.setText(0, "");
            String pathText = item.getPath();
            if (item.getScaleRatio() < 0.99) {
                pathText += String.format(" (Scaled: %.0f%%)", item.getScaleRatio() * 100.0);
            }
            tableItem.setText(1, pathText);
            tableItem.setText(2, item.getType());
            tableItem.setData(item);
        }
        updateHardwareAndTimeInfo();
        scheduleGraphUpdate();
    }

    private void updateHardwareAndTimeInfo() {
        if (hwProfileLabel != null && !hwProfileLabel.isDisposed()) {
            hwProfileLabel.setText("Hardware Profile: " + getHardwareProfile());
        }
        if (estTimeLabel != null && !estTimeLabel.isDisposed()) {
            double estSec = getEstimatedForgingSeconds();
            long bytes = calculateTotalDatasetBytes();
            long tokens = estimateTotalTrainingTokens();
            double params = getModelParameterCount(selectedModelSize);
            double measured = ForgeTrainingEstimationAgent.getInstance().getMeasuredTokensPerSecond(selectedModelSize);
            String basis = measured > 0
                    ? String.format("Measured history: %.1f tok/s", measured)
                    : "ANALYTICAL PRELIMINARY (no measured runs yet)";
            estTimeLabel.setText(String.format("Data: %.2f MB | Tokens: %.2f M | Model: %s (%.1fM params) | Preliminary Est. Time: %s | %s",
                bytes / (1024.0 * 1024.0), tokens / 1_000_000.0, selectedModelSize, params / 1_000_000.0, formatDuration(estSec), basis));
        }
    }

    // =====================================================================
    // HTML GRAPHS (dark professional theme, real data only)
    // =====================================================================

    /** Legacy-compatible entry: config-only mode (no live telemetry). */
    public String generateGraphHtml(int maxEpochs, double targetFinishLoss, double[] history) {
        readEffectiveSettingsFromUiSafe();
        ForgeTelemetrySnapshot snap = ForgeTelemetrySnapshot.configOnly(
                selectedModelSize, maxEpochs, targetFinishLoss, selectedTargetHours,
                selectedEarlyStoppingEnabled, history, calculateTotalDatasetBytes());
        return generateGraphHtml(snap);
    }

    private void readEffectiveSettingsFromUiSafe() {
        try {
            readEffectiveSettingsFromUi();
        } catch (Exception ignored) {}
    }

    /** Live center-panel HTML: summary + cards + GRAPH 1/2/3 + monitoring loss graph. */
    public String generateGraphHtml(ForgeTelemetrySnapshot snap) {
        if (snap == null) {
            snap = ForgeTelemetrySnapshot.configOnly(selectedModelSize, selectedEpochs, selectedDesiredLoss,
                    selectedTargetHours, selectedEarlyStoppingEnabled, lossHistory, calculateTotalDatasetBytes());
        }

        double params = ForgeTelemetrySnapshot.resolveParamCount(snap.configuredModelSize);
        long totalDataBytes = snap.initialEstimateSeconds > 0 ? calculateTotalDatasetBytesSafe() : 0;
        double estSec = snap.initialEstimateSeconds;
        double budgetHours = snap.configuredTimeBudgetHours > 0 ? snap.configuredTimeBudgetHours : selectedTargetHours;
        double budgetSec = budgetHours * 3600.0;
        boolean fitsBudget = estSec > 0 && estSec <= budgetSec;
        boolean budgetUnknown = estSec <= 0 && !snap.isTrainingActive;

        String throughputText = throughputText(snap);
        String lrText = snap.learningRate > 0 ? String.format("%.6f", snap.learningRate) : "N/A";
        String configSummaryHtml = String.format(
            "<div class=\"summary-bar\">" +
            "  <div class=\"summary-item\"><span class=\"summary-label\">Model Size:</span> <span class=\"summary-value\">%s (%.1fM)</span></div>" +
            "  <div class=\"summary-item\"><span class=\"summary-label\">Epochs:</span> <span class=\"summary-value\">%d</span></div>" +
            "  <div class=\"summary-item\"><span class=\"summary-label\">Batch Size:</span> <span class=\"summary-value\">%d</span></div>" +
            "  <div class=\"summary-item\"><span class=\"summary-label\">Data Size:</span> <span class=\"summary-value\">%.2f MB</span></div>" +
            "  <div class=\"summary-item\"><span class=\"summary-label\">Learning Rate:</span> <span class=\"summary-value\">%s</span></div>" +
            "  <div class=\"summary-item\"><span class=\"summary-label\">Est. Time:</span> <span class=\"summary-value\">%s</span></div>" +
            "  <div class=\"summary-item\"><span class=\"summary-label\">Budget:</span> <span class=\"summary-value\">%.1f hrs (%s)</span></div>" +
            "  <div class=\"summary-item\"><span class=\"summary-label\">Throughput:</span> <span class=\"summary-value\">%s</span></div>" +
            "</div>",
            snap.configuredModelSize, params / 1_000_000.0,
            snap.configuredEpochs, selectedBatchSize,
            totalDataBytes / (1024.0 * 1024.0),
            lrText,
            estSec > 0 ? formatDuration(estSec) : "N/A",
            budgetHours,
            budgetUnknown ? "UNKNOWN" : (fitsBudget ? "FEASIBLE" : "EXCEEDS BUDGET"),
            throughputText
        );

        String svg1 = buildLossTrajectorySvg(snap);
        String svg2 = buildCapacityBudgetSvg(snap, budgetHours, estSec);
        String svg3 = buildResourceMonitorSvg(snap);

        String statusTitle1 = lossTrajectoryTitle(snap);
        String statusSub1 = lossTrajectorySubtitle(snap);
        String badgeClass1 = lossTrajectoryBadge(snap);

        String statusTitle2 = budgetTitle(snap);
        String badgeClass2 = budgetBadge(snap);
        String statusSub2 = budgetSubtitle(snap);

        String statusTitle3 = "Progress & Resource Monitor (measured telemetry; N/A when unavailable)";
        String statusSub3 = resourceSubtitle(snap);
        String badgeClass3 = "badge-neutral";

        return withInitialZoom(buildCenterDocument(configSummaryHtml, statusTitle1, statusSub1, badgeClass1, svg1,
                statusTitle2, statusSub2, badgeClass2, svg2, statusTitle3, statusSub3, badgeClass3, svg3));
    }

    private static String resourceSubtitle(ForgeTelemetrySnapshot snap) {
        StringBuilder sb = new StringBuilder();
        sb.append("Progress: ");
        if (snap.overallProgress >= 0) {
            sb.append(String.format("%.1f%%", snap.overallProgress * 100.0));
        } else {
            sb.append("N/A");
        }
        sb.append(" | Step ").append(snap.currentStep).append(" / ").append(snap.totalSteps);
        sb.append(" | Elapsed: ").append(formatDuration(snap.elapsedSeconds));
        if (snap.estimatedRemainingSeconds >= 0) {
            sb.append(" | ETA: ").append(formatDuration(snap.estimatedRemainingSeconds));
        }
        return sb.toString();
    }

    private long calculateTotalDatasetBytesSafe() {
        try {
            return calculateTotalDatasetBytes();
        } catch (Exception e) {
            return 0;
        }
    }

    private static String throughputText(ForgeTelemetrySnapshot snap) {
        switch (snap.throughputBasis) {
            case MEASURED_LIVE:
                return String.format("%.1f tok/s (measured)", snap.effectiveTokensPerSec);
            case MEASURED_HISTORY:
                return String.format("%.1f tok/s (measured history)", snap.effectiveTokensPerSec);
            case ANALYTICAL_PRELIMINARY:
                return String.format("%.1f tok/s (preliminary)", snap.effectiveTokensPerSec);
            default:
                return "N/A";
        }
    }

    // ---------- GRAPH 1: LOSS TRAJECTORY ----------

    private static String lossTrajectoryTitle(ForgeTelemetrySnapshot snap) {
        if (snap.isTrainingActive) return "Measured Training Loss Trajectory";
        if (!snap.history.isEmpty()) return "Training Loss Trajectory (last run)";
        return "Loss Trajectory (no measured data yet)";
    }

    private static String lossTrajectorySubtitle(ForgeTelemetrySnapshot snap) {
        if (snap.totalEpochs > 0) {
            String pct = snap.overallProgress >= 0 ? String.format(" (overall %.0f%%)", snap.overallProgress * 100.0) : "";
            return String.format("EPOCH %d / %d%s | Step %d / %d | Target Finish Loss < %.3f",
                    snap.currentEpoch, snap.totalEpochs, pct, snap.currentStep, snap.totalSteps, snap.configuredDesiredLoss);
        }
        return "Start training to collect measured loss values.";
    }

    private static String lossTrajectoryBadge(ForgeTelemetrySnapshot snap) {
        if (snap.currentLoss > 0 && snap.configuredDesiredLoss > 0 && snap.currentLoss <= snap.configuredDesiredLoss) {
            return "badge-success";
        }
        if (snap.isTrainingActive) return "badge-primary";
        if (!snap.history.isEmpty()) return "badge-neutral";
        return "badge-neutral";
    }

    private static String buildLossTrajectorySvg(ForgeTelemetrySnapshot snap) {
        int width = 720;
        int height = 340;
        int padL = 60;
        int padR = 35;
        int padT = 40;
        int padB = 50;
        int plotW = width - padL - padR;
        int plotH = height - padT - padB;

        StringBuilder svg = new StringBuilder();

        // X domain: steps when available, else epochs.
        boolean useSteps = snap.totalSteps > 0;
        double xMax = useSteps ? Math.max(1, snap.totalSteps) : Math.max(1, snap.totalEpochs);
        double xMin = 0;

        // Y domain: auto-scaled to data; clamp to sensible loss ranges.
        List<Double> losses = new ArrayList<>();
        for (ForgeTelemetrySnapshot.HistoryPoint p : snap.history) {
            if (p.loss > 0) losses.add(p.loss);
        }
        double yAxisMax = snap.configuredDesiredLoss > 0 ? snap.configuredDesiredLoss : 3.0;
        for (Double l : losses) yAxisMax = Math.max(yAxisMax, l);
        yAxisMax = Math.min(12.0, yAxisMax * 1.1 + 0.25);
        if (losses.isEmpty()) yAxisMax = Math.max(3.0, yAxisMax);
        final double yMax = yAxisMax;
        double yMin = 0;

        java.util.function.DoubleUnaryOperator xOf = v -> padL + (v - xMin) / Math.max(1.0, xMax - xMin) * plotW;
        java.util.function.DoubleUnaryOperator yOf = v -> padT + plotH - (v - yMin) / Math.max(1.0, yMax - yMin) * plotH;

        // Grid + Y labels
        int yTicks = 5;
        for (int i = 0; i <= yTicks; i++) {
            double v = yMin + (yMax - yMin) * i / yTicks;
            int y = (int) yOf.applyAsDouble(v);
            svg.append(String.format(
                "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#2b3a52\" stroke-dasharray=\"3,3\" stroke-width=\"1\" />",
                padL, y, padL + plotW, y));
            svg.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-size=\"10\" fill=\"#8ea3c0\" text-anchor=\"end\">%.2f</text>",
                padL - 8, y + 4, v));
        }

        // X ticks
        int xTicks = 5;
        for (int i = 0; i <= xTicks; i++) {
            double v = xMin + (xMax - xMin) * i / xTicks;
            int x = (int) xOf.applyAsDouble(v);
            svg.append(String.format(
                "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#2b3a52\" stroke-dasharray=\"3,3\" stroke-width=\"1\" />",
                x, padT, x, padT + plotH));
            svg.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-size=\"10\" fill=\"#8ea3c0\" text-anchor=\"middle\">%s</text>",
                x, padT + plotH + 16, useSteps ? String.valueOf((long) v) : String.valueOf((long) v + 1)));
        }

        // Axes
        svg.append(String.format(
            "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#46597a\" stroke-width=\"2\" />",
            padL, padT, padL, padT + plotH));
        svg.append(String.format(
            "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#46597a\" stroke-width=\"2\" />",
            padL, padT + plotH, padL + plotW, padT + plotH));

        // Target threshold line (from the actual configuration)
        if (snap.configuredDesiredLoss > 0) {
            int targetY = (int) yOf.applyAsDouble(Math.min(yMax, snap.configuredDesiredLoss));
            svg.append(String.format(
                "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#10b981\" stroke-width=\"2\" stroke-dasharray=\"6,4\" />",
                padL, targetY, padL + plotW, targetY));
            svg.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-size=\"11\" font-weight=\"600\" fill=\"#10b981\">Target Finish Loss &lt; %.3f (config)</text>",
                padL + plotW - 260, Math.max(padT + 14, targetY - 8), snap.configuredDesiredLoss));
        }

        // Actual measured loss series
        if (!snap.history.isEmpty()) {
            StringBuilder points = new StringBuilder();
            StringBuilder dots = new StringBuilder();
            boolean first = true;
            int lastX = padL, lastY = padT + plotH;
            for (ForgeTelemetrySnapshot.HistoryPoint p : snap.history) {
                if (p.loss <= 0) continue;
                double xv = useSteps ? p.step : p.epoch;
                int x = (int) xOf.applyAsDouble(Math.min(xMax, Math.max(0, xv)));
                int y = (int) yOf.applyAsDouble(Math.min(yMax, Math.max(0, p.loss)));
                if (!first) points.append(" ");
                points.append(x).append(",").append(y);
                first = false;
                lastX = x;
                lastY = y;
                dots.append(String.format(
                    "<circle cx=\"%d\" cy=\"%d\" r=\"3.5\" fill=\"#60a5fa\" stroke=\"#0f172a\" stroke-width=\"1\" class=\"dot\" data-tooltip=\"Epoch %d | Step %d | Loss %.4f\" />",
                    x, y, p.epoch, p.step, p.loss));
            }
            if (!first) {
                svg.append(String.format(
                    "<polyline points=\"%s\" fill=\"none\" stroke=\"#60a5fa\" stroke-width=\"2.5\" stroke-linejoin=\"round\" stroke-linecap=\"round\" />",
                    points.toString()));
                svg.append(dots.toString());
                // Current position marker
                svg.append(String.format(
                    "<circle cx=\"%d\" cy=\"%d\" r=\"10\" fill=\"none\" stroke=\"#f59e0b\" stroke-width=\"2\" opacity=\"0.7\"><animate attributeName=\"r\" values=\"8;14;8\" dur=\"2s\" repeatCount=\"indefinite\"/><animate attributeName=\"opacity\" values=\"0.8;0.2;0.8\" dur=\"2s\" repeatCount=\"indefinite\"/></circle>",
                    lastX, lastY));
                svg.append(String.format(
                    "<circle cx=\"%d\" cy=\"%d\" r=\"5\" fill=\"#f59e0b\" stroke=\"#0f172a\" stroke-width=\"2\" class=\"dot\" data-tooltip=\"Current: Epoch %d/%d | Loss %.4f\" />",
                    lastX, lastY, snap.currentEpoch, snap.totalEpochs, snap.currentLoss > 0 ? snap.currentLoss : 0.0));
            }

            // Validation loss series (when present)
            StringBuilder valPoints = new StringBuilder();
            boolean valFirst = true;
            for (ForgeTelemetrySnapshot.HistoryPoint p : snap.history) {
                if (p.valLoss <= 0) continue;
                double xv = useSteps ? p.step : p.epoch;
                int x = (int) xOf.applyAsDouble(Math.min(xMax, Math.max(0, xv)));
                int y = (int) yOf.applyAsDouble(Math.min(yMax, Math.max(0, p.valLoss)));
                if (!valFirst) valPoints.append(" ");
                valPoints.append(x).append(",").append(y);
                valFirst = false;
            }
            if (!valFirst) {
                svg.append(String.format(
                    "<polyline points=\"%s\" fill=\"none\" stroke=\"#38bdf8\" stroke-width=\"2\" stroke-dasharray=\"5,3\" stroke-linejoin=\"round\" />",
                    valPoints.toString()));
            }

            // PROJECTED LOSS: fitted from the measured history only.
            String proj = buildMeasuredProjection(snap, xOf, yOf, xMax, useSteps);
            if (proj != null) {
                svg.append(proj);
            }
        } else if (snap.legacyLossHistory != null && snap.legacyLossHistory.length > 0) {
            // Historical loss series from the persisted configuration (real
            // values from a previous run), explicitly labeled as such.
            StringBuilder points = new StringBuilder();
            boolean first = true;
            for (int i = 0; i < snap.legacyLossHistory.length; i++) {
                double xv = useSteps ? 0 : (i + 1);
                int x = (int) xOf.applyAsDouble(Math.min(xMax, Math.max(0, xv)));
                int y = (int) yOf.applyAsDouble(Math.min(yMax, Math.max(0, snap.legacyLossHistory[i])));
                if (!first) points.append(" ");
                points.append(x).append(",").append(y);
                first = false;
            }
            if (!first) {
                svg.append(String.format(
                    "<polyline points=\"%s\" fill=\"none\" stroke=\"#60a5fa\" stroke-width=\"2\" stroke-dasharray=\"4,3\" opacity=\"0.8\" />",
                    points.toString()));
            }
            svg.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-size=\"11\" fill=\"#8ea3c0\">Historical loss (persisted config, previous run)</text>",
                padL + 8, padT + 14));
        } else {
            svg.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-size=\"12\" fill=\"#8ea3c0\" text-anchor=\"middle\">No measured training data yet — start training to populate this graph.</text>",
                padL + plotW / 2, padT + plotH / 2));
        }

        return svg.toString();
    }

    /**
     * Projects the loss from the last measured points using a linear fit over
     * the most recent window (max 8 points). Returns null when insufficient
     * history exists: no projection is fabricated from less than 3 points.
     */
    private static String buildMeasuredProjection(ForgeTelemetrySnapshot snap,
                                                  java.util.function.DoubleUnaryOperator xOf,
                                                  java.util.function.DoubleUnaryOperator yOf,
                                                  double xMax, boolean useSteps) {
        List<ForgeTelemetrySnapshot.HistoryPoint> usable = new ArrayList<>();
        for (ForgeTelemetrySnapshot.HistoryPoint p : snap.history) {
            if (p.loss > 0) usable.add(p);
        }
        if (usable.size() < 3) return null;

        int windowSize = Math.min(8, usable.size());
        List<ForgeTelemetrySnapshot.HistoryPoint> window = usable.subList(usable.size() - windowSize, usable.size());
        int n = window.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (ForgeTelemetrySnapshot.HistoryPoint p : window) {
            double x = useSteps ? p.step : p.epoch;
            double y = p.loss;
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }
        double denom = n * sumXX - sumX * sumX;
        if (Math.abs(denom) < 1e-9) return null;
        double slope = (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;

        double lastX = useSteps ? window.get(n - 1).step : window.get(n - 1).epoch;
        if (lastX >= xMax - 1) return null;

        StringBuilder proj = new StringBuilder();
        int steps = 24;
        double firstLoss = Math.max(0.0, intercept + slope * lastX);
        proj.append((int) xOf.applyAsDouble(lastX)).append(",").append((int) yOf.applyAsDouble(Math.min(12.0, firstLoss)));
        for (int i = 1; i <= steps; i++) {
            double x = lastX + (xMax - lastX) * i / steps;
            double y = Math.max(0.0, intercept + slope * x);
            proj.append(" ").append((int) xOf.applyAsDouble(x)).append(",").append((int) yOf.applyAsDouble(Math.min(12.0, y)));
        }
        String trend = slope < 0 ? "decreasing trend" : "non-decreasing trend";
        return String.format(
            "<polyline points=\"%s\" fill=\"none\" stroke=\"#c084fc\" stroke-width=\"2\" stroke-dasharray=\"6,4\" opacity=\"0.9\" />" +
            "<text x=\"%d\" y=\"%d\" font-size=\"10\" fill=\"#c084fc\">Projected (linear fit of last %d measured points, %s)</text>",
            proj.toString(), (int) xOf.applyAsDouble(lastX) + 10, 26, n, trend);
    }

    // ---------- GRAPH 2: CAPACITY BUDGET ----------

    private static String budgetTitle(ForgeTelemetrySnapshot snap) {
        switch (snap.budgetState) {
            case ON_TRACK: return "Capacity Budget: ON TRACK";
            case AT_RISK: return "Capacity Budget: AT RISK";
            case EXCEEDS_BUDGET: return "Capacity Budget: EXCEEDS BUDGET";
            case COMPLETED: return "Capacity Budget: COMPLETED";
            default: return "Capacity Budget: UNKNOWN (no budget or no estimate)";
        }
    }

    private static String budgetBadge(ForgeTelemetrySnapshot snap) {
        switch (snap.budgetState) {
            case ON_TRACK:
            case COMPLETED: return "badge-success";
            case AT_RISK: return "badge-primary";
            case EXCEEDS_BUDGET: return "badge-danger";
            default: return "badge-neutral";
        }
    }

    private static String budgetSubtitle(ForgeTelemetrySnapshot snap) {
        double budgetSec = snap.configuredTimeBudgetHours > 0 ? snap.configuredTimeBudgetHours * 3600.0 : -1;
        double elapsed = snap.elapsedSeconds;
        double remaining = snap.estimatedRemainingSeconds;
        // Terminal-aware total: completed/cancelled/failed runs use their actual elapsed
        // duration; active runs use elapsed + remaining; otherwise fall back to the
        // historical preliminary estimate (clearly not a live projection).
        double estTotal = snap.estimatedTotalSeconds > 0 ? snap.estimatedTotalSeconds
                : (snap.initialEstimateSeconds > 0 ? snap.initialEstimateSeconds : -1);
        boolean terminal = "COMPLETED".equalsIgnoreCase(snap.status) || "STOPPED".equalsIgnoreCase(snap.status)
                || "FAILED".equalsIgnoreCase(snap.status) || "ABORTED".equalsIgnoreCase(snap.status)
                || "CANCELLED".equalsIgnoreCase(snap.status) || "INTERRUPTED".equalsIgnoreCase(snap.status);
        StringBuilder sb = new StringBuilder();
        if (terminal) {
            sb.append("Completed in: ").append(formatDuration(elapsed));
            if (snap.initialEstimateSeconds > 0 && snap.initialEstimateSeconds > elapsed) {
                sb.append(" | Original estimate: ").append(formatDuration(snap.initialEstimateSeconds));
            }
        } else {
            sb.append("Elapsed: ").append(formatDuration(elapsed));
            if (remaining >= 0) sb.append(" | Remaining: ").append(formatDuration(remaining));
            if (estTotal > 0) sb.append(" | Est. total: ").append(formatDuration(estTotal));
            if (snap.isTrainingActive && snap.etaConfidence <= 0.0) {
                sb.append(" | ETA: ANALYTICAL PRELIMINARY");
            }
        }
        if (budgetSec > 0) sb.append(" | Budget: ").append(formatDuration(budgetSec));
        if (snap.budgetConsumption > 0) sb.append(" | Consumption: ").append(String.format("%.0f%%", snap.budgetConsumption * 100.0));
        return sb.toString();
    }

    private static String buildCapacityBudgetSvg(ForgeTelemetrySnapshot snap, double budgetHours, double estSec) {
        int width = 720;
        int height = 300;
        int padL = 220;
        int padR = 45;
        int padT = 30;
        int padB = 40;
        int plotW = width - padL - padR;
        int plotH = height - padT - padB;

        double budgetSec = budgetHours > 0 ? budgetHours * 3600.0 : -1;
        double elapsed = snap.elapsedSeconds;
        double remaining = snap.estimatedRemainingSeconds;
        // Terminal-aware total (same logic as ForgeTelemetrySnapshot.estimateTotalSeconds):
        // a completed/cancelled/failed run has no projected total beyond its elapsed time.
        double estTotal = snap.estimatedTotalSeconds > 0 ? snap.estimatedTotalSeconds : estSec;

        double xMax = Math.max(1.0, Math.max(budgetSec > 0 ? budgetSec : 0, estTotal > 0 ? estTotal : 0) * 1.2);

        StringBuilder svg = new StringBuilder();

        // Budget reference line
        if (budgetSec > 0) {
            int bx = padL + (int) (Math.min(budgetSec, xMax) / xMax * plotW);
            svg.append(String.format(
                "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#f59e0b\" stroke-width=\"2\" stroke-dasharray=\"6,4\" />",
                bx, padT, bx, padT + plotH));
            svg.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-size=\"11\" font-weight=\"600\" fill=\"#f59e0b\">Target Duration Budget: %s</text>",
                Math.min(bx + 6, padL + plotW - 250), padT + 12, formatDuration(budgetSec)));
        }

        double[] bars = { elapsed, remaining >= 0 ? remaining : 0, estTotal > 0 ? estTotal : 0 };
        String[] labels = { "Actual Elapsed (measured)", "Estimated Remaining (measured ETA)", "Estimated Total Duration" };
        String[] colors = { "#60a5fa", "#c084fc", "#f87171" };

        int rowH = 44;
        for (int i = 0; i < bars.length; i++) {
            int barY = padT + 40 + i * (rowH + 14);
            int fill = (int) (Math.min(bars[i], xMax) / xMax * plotW);
            String color = colors[i];
            if (bars[i] <= 0) {
                color = "#33415c";
                fill = 6;
            }
            svg.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-size=\"11\" fill=\"#c3d0e5\">%s</text>",
                padL - 8, barY + 16, labels[i]));
            svg.append(String.format(
                "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"22\" rx=\"4\" fill=\"%s\" stroke=\"#1e293b\" stroke-width=\"1\" class=\"dot\" data-tooltip=\"%s: %s\" />",
                padL, barY, Math.max(fill, 2), color, labels[i], bars[i] > 0 ? formatDuration(bars[i]) : "N/A"));
            if (bars[i] > 0) {
                svg.append(String.format(
                    "<text x=\"%d\" y=\"%d\" font-size=\"11\" font-weight=\"700\" fill=\"#0b1220\">%s</text>",
                    padL + 8, barY + 15, formatDuration(bars[i])));
            }
        }

        // Axes
        svg.append(String.format(
            "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#46597a\" stroke-width=\"2\" />",
            padL, padT + 30, padL + plotW, padT + 30));
        svg.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-size=\"10\" fill=\"#8ea3c0\" text-anchor=\"middle\">Duration</text>",
            padL + plotW / 2, height - 8));

        // Status badge text
        svg.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-size=\"13\" font-weight=\"700\" fill=\"%s\">%s</text>",
            padL, padT + 28, budgetStateColor(snap), budgetStateLabel(snap)));
        return svg.toString();
    }

    private static String budgetStateLabel(ForgeTelemetrySnapshot snap) {
        switch (snap.budgetState) {
            case ON_TRACK: return "● ON TRACK";
            case AT_RISK: return "● AT RISK";
            case EXCEEDS_BUDGET: return "● EXCEEDS BUDGET";
            case COMPLETED: return "● COMPLETED";
            default: return "● UNKNOWN";
        }
    }

    private static String budgetStateColor(ForgeTelemetrySnapshot snap) {
        switch (snap.budgetState) {
            case ON_TRACK:
            case COMPLETED: return "#10b981";
            case AT_RISK: return "#f59e0b";
            case EXCEEDS_BUDGET: return "#ef4444";
            default: return "#8ea3c0";
        }
    }

    // ---------- GRAPH 3: PROGRESS & RESOURCE MONITOR ----------

    private static String buildResourceMonitorSvg(ForgeTelemetrySnapshot snap) {
        int width = 720;
        int height = 340;
        int padL = 240;
        int padR = 40;
        int padT = 30;
        int padB = 40;
        int plotW = width - padL - padR;

        StringBuilder svg = new StringBuilder();

        double progressPct = snap.overallProgress >= 0 ? snap.overallProgress * 100.0 : 0.0;
        boolean progressAvailable = snap.overallProgress >= 0;
        String progressText = progressAvailable ? String.format("%.1f%% (Step %d/%d)", progressPct, snap.currentStep, snap.totalSteps) : "N/A";

        double memPct;
        if (snap.ramTotalMb > 0) {
            memPct = Math.min(100.0, Math.max(0.0, snap.ramUsedMb / snap.ramTotalMb * 100.0));
        } else {
            Runtime rt = Runtime.getRuntime();
            memPct = Math.min(100.0, (rt.totalMemory() - rt.freeMemory()) * 100.0 / Math.max(1, rt.totalMemory()));
        }
        boolean memLive = snap.isTrainingActive || snap.history.size() > 1;

        double tps = snap.effectiveTokensPerSec;
        boolean tpsAvailable = tps > 0;
        boolean tpsPreliminary = snap.throughputBasis == ForgeTelemetrySnapshot.ThroughputBasis.ANALYTICAL_PRELIMINARY;

        double cpu = snap.cpuPercent;
        boolean cpuAvailable = cpu > 0.0;

        int[] barY = { padT + 26, padT + 88, padT + 150, padT + 212 };
        int barH = 24;

        // 1. Calculation Progress / Time
        int timeFillW = (int) (Math.min(100.0, progressPct) / 100.0 * plotW);
        svg.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-size=\"11\" font-weight=\"600\" fill=\"#c3d0e5\">Calculation Progress / Time</text>",
            padL - 8, barY[0] + 12));
        svg.append(String.format(
            "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"4\" fill=\"#1e293b\" stroke=\"#33415c\" stroke-width=\"1\" />",
            padL, barY[0], plotW, barH));
        svg.append(String.format(
            "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"4\" fill=\"#1d5296\" class=\"dot\" data-tooltip=\"Progress: %s | Elapsed: %s | ETA: %s\" />",
            padL, barY[0], Math.max(progressAvailable ? 6 : 0, timeFillW), barH,
            progressText, formatDuration(snap.elapsedSeconds),
            snap.estimatedRemainingSeconds >= 0 ? formatDuration(snap.estimatedRemainingSeconds) : "N/A"));
        svg.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-size=\"11\" font-weight=\"700\" fill=\"%s\">%s%s · Elapsed %s%s</text>",
            padL + 10, barY[0] + 16, progressAvailable ? "#ffffff" : "#8ea3c0",
            progressAvailable ? progressText : "Progress N/A",
            progressAvailable ? "" : "",
            formatDuration(snap.elapsedSeconds),
            snap.estimatedRemainingSeconds >= 0 ? " · ETA " + formatDuration(snap.estimatedRemainingSeconds) : ""));

        // 2. Allocated System RAM / Memory Usage (real JVM measurement)
        int memFillW = (int) (memPct / 100.0 * plotW);
        String memBarColor = memPct > 85.0 ? "#ef4444" : (memPct > 65.0 ? "#f59e0b" : "#10b981");
        svg.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-size=\"11\" font-weight=\"600\" fill=\"#c3d0e5\">Allocated System RAM / Memory Usage</text>",
            padL - 8, barY[1] + 12));
        svg.append(String.format(
            "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"4\" fill=\"#1e293b\" stroke=\"#33415c\" stroke-width=\"1\" />",
            padL, barY[1], plotW, barH));
        svg.append(String.format(
            "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"4\" fill=\"%s\" class=\"dot\" data-tooltip=\"JVM RAM: %d MB / %d MB (%.1f%%, %s)\" />",
            padL, barY[1], Math.max(6, memFillW), barH, memBarColor,
            (long) snap.ramUsedMb, (long) Math.max(snap.ramTotalMb, 1), memPct,
            memLive ? "measured" : "current JVM"));
        svg.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-size=\"11\" font-weight=\"700\" fill=\"%s\">%d MB / %d MB RAM (%.1f%%)%s</text>",
            padL + 10, barY[1] + 16, memPct > 35 ? "#ffffff" : "#c3d0e5",
            (long) snap.ramUsedMb, (long) Math.max(snap.ramTotalMb, 1), memPct,
            memLive ? "" : " (live JVM value)"));

        // 3. Token Throughput
        double tpsPct = tpsAvailable ? Math.min(100.0, tps / 2000.0 * 100.0) : 0.0;
        int tpsFillW = (int) (tpsPct / 100.0 * plotW);
        svg.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-size=\"11\" font-weight=\"600\" fill=\"#c3d0e5\">Token Throughput (Tokens / sec)</text>",
            padL - 8, barY[2] + 12));
        svg.append(String.format(
            "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"4\" fill=\"#1e293b\" stroke=\"#33415c\" stroke-width=\"1\" />",
            padL, barY[2], plotW, barH));
        svg.append(String.format(
            "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"4\" fill=\"#8b5cf6\" class=\"dot\" data-tooltip=\"Throughput: %s | Tokens: %d/%d\" />",
            padL, barY[2], Math.max(tpsAvailable ? 6 : 0, tpsFillW), barH,
            tpsAvailable ? String.format("%.1f tok/s", tps) : "N/A",
            snap.processedTokens, snap.totalTokens));
        svg.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-size=\"11\" font-weight=\"700\" fill=\"%s\">%s · Tokens %s/%s</text>",
            padL + 10, barY[2] + 16, tpsAvailable ? "#ffffff" : "#8ea3c0",
            tpsAvailable ? String.format("%.1f tok/s %s", tps, tpsPreliminary ? "(ANALYTICAL PRELIMINARY)" : "(measured)") : "Throughput N/A",
            fmtTokens(snap.processedTokens), fmtTokens(snap.totalTokens)));

        // 4. Process CPU Utilization (real measurement)
        int cpuFillW = (int) (Math.min(100.0, Math.max(0.0, cpu)) / 100.0 * plotW);
        svg.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-size=\"11\" font-weight=\"600\" fill=\"#c3d0e5\">Process CPU Utilization (measured)</text>",
            padL - 8, barY[3] + 12));
        svg.append(String.format(
            "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"4\" fill=\"#1e293b\" stroke=\"#33415c\" stroke-width=\"1\" />",
            padL, barY[3], plotW, barH));
        svg.append(String.format(
            "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"4\" fill=\"#22c55e\" class=\"dot\" data-tooltip=\"CPU: %.1f%% (process, measured)\" />",
            padL, barY[3], Math.max(cpuAvailable ? 6 : 0, cpuFillW), barH, cpu));
        svg.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-size=\"11\" font-weight=\"700\" fill=\"%s\">%s · GPU: %s</text>",
            padL + 10, barY[3] + 16, cpuAvailable ? "#ffffff" : "#8ea3c0",
            cpuAvailable ? String.format("%.1f%% CPU", cpu) : "CPU N/A (no training run yet)",
            snap.gpuStatus != null ? snap.gpuStatus : "N/A"));

        return svg.toString();
    }

    // ---------- HTML DOCUMENTS ----------

    private static String buildCenterDocument(String summary, String t1, String s1, String b1, String svg1,
                                              String t2, String s2, String b2, String svg2,
                                              String t3, String s3, String b3, String svg3) {
        return "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n" + graphCss() + "\n</head>\n<body>\n"
            + summary + "\n"
            + cardHtml("GRAPH 1: LOSS TRAJECTORY", t1, s1, b1, svg1,
                "<div class=\"legend-item\"><div class=\"legend-line\" style=\"background:#60a5fa;\"></div>Actual Loss</div>" +
                "<div class=\"legend-item\"><div class=\"legend-line\" style=\"background:#38bdf8; border-top:2px dashed #38bdf8;\"></div>Validation Loss</div>" +
                "<div class=\"legend-item\"><div class=\"legend-line\" style=\"background:#c084fc; border-top:2px dashed #c084fc;\"></div>Projected (measured fit)</div>" +
                "<div class=\"legend-item\"><div class=\"legend-line\" style=\"background:#10b981; border-top:2px dashed #10b981;\"></div>Target Threshold</div>")
            + "\n"
            + cardHtml("GRAPH 2: CAPACITY BUDGET", t2, s2, b2, svg2,
                "<div class=\"legend-item\"><div class=\"legend-line\" style=\"background:#f59e0b; border-top:2px dashed #f59e0b;\"></div>Target Budget</div>" +
                "<div class=\"legend-item\"><div class=\"legend-line\" style=\"background:#60a5fa;\"></div>Elapsed</div>" +
                "<div class=\"legend-item\"><div class=\"legend-line\" style=\"background:#c084fc;\"></div>Remaining</div>" +
                "<div class=\"legend-item\"><div class=\"legend-line\" style=\"background:#f87171;\"></div>Estimated Total</div>")
            + "\n"
            + cardHtml("GRAPH 3: PROGRESS & RESOURCE MONITOR", t3, s3, b3, svg3,
                "<div class=\"legend-item\"><div class=\"legend-line\" style=\"background:#1d5296;\"></div>Time Progress</div>" +
                "<div class=\"legend-item\"><div class=\"legend-line\" style=\"background:#10b981;\"></div>RAM Memory</div>" +
                "<div class=\"legend-item\"><div class=\"legend-line\" style=\"background:#8b5cf6;\"></div>Tokens/sec</div>" +
                "<div class=\"legend-item\"><div class=\"legend-line\" style=\"background:#22c55e;\"></div>CPU</div>")
            + "\n" + tooltipScript() + interactionScript() + "</body>\n</html>";
    }

    private static String cardHtml(String badge, String title, String subtitle, String badgeClass, String svg, String legend) {
        return "<!-- CARD: " + badge + " -->\n<div class=\"card\">\n"
            + "  <div class=\"header\">\n"
            + "    <div class=\"title-group\">\n"
            + "      <div class=\"title\">" + title + "</div>\n"
            + "      <div class=\"subtitle\">" + subtitle + "</div>\n"
            + "    </div>\n"
            + "    <div class=\"badge " + badgeClass + "\">" + badge + "</div>\n"
            + "  </div>\n"
            + "  <div class=\"graph-container\" id=\"" + badge.replaceAll("[^a-zA-Z0-9]", "") + "\">\n"
            + "    <div class=\"tooltip\"></div>\n"
            + "    <svg viewBox=\"0 0 720 360\" preserveAspectRatio=\"xMidYMid meet\">\n" + svg + "\n    </svg>\n"
            + "  </div>\n"
            + "  <div class=\"legend\">" + legend + "</div>\n"
            + "</div>\n";
    }

    /** Bottom tabbed monitoring graphs (Loss | LR | Throughput | Hardware | Memory | Timeline). */
    public String generateMonitoringGraphsHtml(ForgeTelemetrySnapshot snap) {
        if (snap == null) {
            snap = ForgeTelemetrySnapshot.configOnly(selectedModelSize, selectedEpochs, selectedDesiredLoss,
                    selectedTargetHours, selectedEarlyStoppingEnabled, lossHistory, calculateTotalDatasetBytes());
        }

        StringBuilder tabs = new StringBuilder();
        tabs.append("<div class=\"tab-bar\">");
        tabs.append(tabButton("tabLoss", "Loss", true));
        tabs.append(tabButton("tabLr", "Learning Rate", false));
        tabs.append(tabButton("tabTp", "Throughput", false));
        tabs.append(tabButton("tabHw", "Hardware", false));
        tabs.append(tabButton("tabMem", "Memory", false));
        tabs.append(tabButton("tabTime", "Timeline", false));
        tabs.append("</div>");

        String lossSvg = buildTimeSeriesSvg(snap, "loss", "Loss", "#60a5fa");
        String lrSvg = buildTimeSeriesSvg(snap, "lr", "Learning Rate", "#f59e0b");
        String tpSvg = buildTimeSeriesSvg(snap, "tps", "Throughput (tok/s)", "#8b5cf6");
        String cpuSvg = buildTimeSeriesSvg(snap, "cpu", "Process CPU %", "#22c55e");
        String memSvg = buildTimeSeriesSvg(snap, "ram", "JVM RAM (MB)", "#10b981");
        String timelineSvg = buildTimelineSvg(snap);

        String html = "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n" + graphCss() + tabsCss() + "\n</head>\n<body>\n"
            + "<div class=\"tab-content\" id=\"tabLoss\">" + "<div class=\"tab-head\">Training Loss over time (measured, timestamped)</div>"
            + svgWrap(lossSvg) + "</div>\n"
            + "<div class=\"tab-content\" id=\"tabLr\" style=\"display:none\">" + "<div class=\"tab-head\">Learning Rate over time (optimizer schedule, measured)</div>"
            + svgWrap(lrSvg) + "</div>\n"
            + "<div class=\"tab-content\" id=\"tabTp\" style=\"display:none\">" + "<div class=\"tab-head\">Training Throughput over time (measured tokens/sec)</div>"
            + svgWrap(tpSvg) + "</div>\n"
            + "<div class=\"tab-content\" id=\"tabHw\" style=\"display:none\">" + "<div class=\"tab-head\">Hardware utilization (measured process CPU %)</div>"
            + svgWrap(cpuSvg) + "</div>\n"
            + "<div class=\"tab-content\" id=\"tabMem\" style=\"display:none\">" + "<div class=\"tab-head\">Memory consumption over time (measured JVM RAM)</div>"
            + svgWrap(memSvg) + "</div>\n"
            + "<div class=\"tab-content\" id=\"tabTime\" style=\"display:none\">" + "<div class=\"tab-head\">Training Phase Timeline (from real session events)</div>"
            + svgWrap(timelineSvg) + "</div>\n"
            + tabScript() + interactionScript() + "\n</body>\n</html>";
        return withInitialZoom(html);
    }

    private static String tabButton(String id, String label, boolean active) {
        return "<button class=\"tab-btn" + (active ? " active" : "") + "\" onclick=\"showTab('" + id + "', this)\">" + label + "</button>";
    }

    private static String svgWrap(String svg) {
        return "<div class=\"tab-svg-wrap\"><svg viewBox=\"0 0 720 300\" preserveAspectRatio=\"xMidYMid meet\">" + svg + "</svg></div>";
    }

    /**
     * Generic timestamped time-series chart. series: "loss" | "valLoss" | "lr"
     * | "tps" | "cpu" | "ram". Values come exclusively from telemetry history
     * samples; empty history renders an honest "no data" placeholder.
     */
    private static String buildTimeSeriesSvg(ForgeTelemetrySnapshot snap, String series, String label, String color) {
        int width = 720;
        int height = 300;
        int padL = 70;
        int padR = 30;
        int padT = 25;
        int padB = 40;
        int plotW = width - padL - padR;
        int plotH = height - padT - padB;

        StringBuilder svg = new StringBuilder();

        List<ForgeTelemetrySnapshot.HistoryPoint> pts = new ArrayList<>();
        for (ForgeTelemetrySnapshot.HistoryPoint p : snap.history) {
            double v = valueOf(p, series);
            if (v >= 0) pts.add(p);
        }

        if (pts.isEmpty()) {
            svg.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-size=\"12\" fill=\"#8ea3c0\" text-anchor=\"middle\">No %s data yet — collect training telemetry first.</text>",
                padL + plotW / 2, padT + plotH / 2, label));
            svg.append(String.format(
                "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#46597a\" stroke-width=\"2\" />",
                padL, padT, padL + plotW, padT + plotH));
            return svg.toString();
        }

        long tMin = pts.get(0).timestamp;
        long tMax = Math.max(tMin + 1, pts.get(pts.size() - 1).timestamp);

        double vMin = Double.MAX_VALUE;
        double vMax = -Double.MAX_VALUE;
        for (ForgeTelemetrySnapshot.HistoryPoint p : pts) {
            double v = valueOf(p, series);
            vMin = Math.min(vMin, v);
            vMax = Math.max(vMax, v);
        }
        if (vMax - vMin < 1e-9) {
            vMax = vMin + 1.0;
        }
        double padV = (vMax - vMin) * 0.1;
        vMin = Math.max(0, vMin - padV);
        vMax = vMax + padV;
        final double tsVMin = vMin;
        final double tsVMax = vMax;

        java.util.function.DoubleUnaryOperator xOf = v -> padL + (v - tMin) / Math.max(1.0, tMax - tMin) * plotW;
        java.util.function.DoubleUnaryOperator yOf = v -> padT + plotH - (v - tsVMin) / (tsVMax - tsVMin) * plotH;

        for (int i = 0; i <= 4; i++) {
            double v = vMin + (vMax - vMin) * i / 4;
            int y = (int) yOf.applyAsDouble(v);
            svg.append(String.format(
                "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#2b3a52\" stroke-dasharray=\"3,3\" stroke-width=\"1\" />",
                padL, y, padL + plotW, y));
            svg.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-size=\"10\" fill=\"#8ea3c0\" text-anchor=\"end\">%s</text>",
                padL - 6, y + 4, formatShort(v)));
        }
        svg.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-size=\"10\" fill=\"#8ea3c0\" text-anchor=\"middle\">Time →</text>",
            padL + plotW / 2, height - 10));
        svg.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-size=\"10\" fill=\"#8ea3c0\">%s</text>",
            padL - 40, padT - 8, label));

        StringBuilder points = new StringBuilder();
        StringBuilder dots = new StringBuilder();
        boolean first = true;
        for (ForgeTelemetrySnapshot.HistoryPoint p : pts) {
            double v = valueOf(p, series);
            int x = (int) xOf.applyAsDouble(p.timestamp);
            int y = (int) yOf.applyAsDouble(v);
            if (!first) points.append(" ");
            points.append(x).append(",").append(y);
            first = false;
            dots.append(String.format(
                "<circle cx=\"%d\" cy=\"%d\" r=\"3\" fill=\"%s\" stroke=\"#0f172a\" stroke-width=\"1\" class=\"dot\" data-tooltip=\"%s | Epoch %d | Step %d | %s\" />",
                x, y, color, timeOf(p.timestamp), p.epoch, p.step, label + ": " + formatShort(v)));
        }
        svg.append(String.format(
            "<polyline points=\"%s\" fill=\"none\" stroke=\"%s\" stroke-width=\"2.5\" stroke-linejoin=\"round\" stroke-linecap=\"round\" />",
            points.toString(), color));
        svg.append(dots.toString());

        return svg.toString();
    }

    private static String timeOf(long ts) {
        return new SimpleDateFormat("HH:mm:ss").format(new Date(ts));
    }

    private static String formatShort(double v) {
        if (Math.abs(v) >= 1000) return String.format("%.0f", v);
        if (Math.abs(v) >= 100) return String.format("%.0f", v);
        if (Math.abs(v) >= 1) return String.format("%.2f", v);
        return String.format("%.4f", v);
    }

    private static double valueOf(ForgeTelemetrySnapshot.HistoryPoint p, String series) {
        switch (series) {
            case "loss": return p.loss > 0 ? p.loss : -1;
            case "valLoss": return p.valLoss > 0 ? p.valLoss : -1;
            case "lr": return p.learningRate > 0 ? p.learningRate : -1;
            case "tps": return p.tokensPerSec > 0 ? p.tokensPerSec : -1;
            case "cpu": return p.cpuPct > 0 ? p.cpuPct : -1;
            case "ram": return p.ramMb > 0 ? p.ramMb : -1;
            default: return -1;
        }
    }

    /** Phase timeline built from real session timing: run start, epochs, end. */
    private static String buildTimelineSvg(ForgeTelemetrySnapshot snap) {
        int width = 720;
        int height = 300;
        int padL = 70;
        int padR = 30;
        int padT = 25;
        int padB = 40;
        int plotW = width - padL - padR;
        int plotH = height - padT - padB;

        StringBuilder svg = new StringBuilder();

        long start = snap.startTimeMs > 0 ? snap.startTimeMs : (snap.history.isEmpty() ? 0 : snap.history.get(0).timestamp);
        long end = snap.durationMs > 0 ? snap.startTimeMs + snap.durationMs : snap.capturedAtMs;
        if (start <= 0 || end <= start) {
            svg.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-size=\"12\" fill=\"#8ea3c0\" text-anchor=\"middle\">No training session timing available.</text>",
                padL + plotW / 2, padT + plotH / 2));
            return svg.toString();
        }
        double span = Math.max(1.0, end - start);
        java.util.function.DoubleUnaryOperator xOf = ms -> padL + (ms - start) / span * plotW;

        svg.append(String.format(
            "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#46597a\" stroke-width=\"2\" />",
            padL, padT + 40, padL + plotW, padT + 40));

        // Training phase bar: measured run window.
        int xStart = (int) xOf.applyAsDouble(start);
        int xEnd = (int) xOf.applyAsDouble(end);
        int phaseY = padT + 40;
        svg.append(String.format(
            "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"26\" rx=\"4\" fill=\"#1d5296\" class=\"dot\" data-tooltip=\"%s: %s → %s (%s)\" />",
            xStart, phaseY, Math.max(8, xEnd - xStart),
            phaseName(snap), timeOf(start), timeOf(end), formatDuration((end - start) / 1000.0)));
        svg.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-size=\"11\" font-weight=\"700\" fill=\"#ffffff\">%s · %s</text>",
            xStart + 8, phaseY + 17, phaseName(snap), formatDuration((end - start) / 1000.0)));

        // Epoch markers from real history samples
        int markerY = phaseY + 60;
        int lastEpoch = -1;
        for (ForgeTelemetrySnapshot.HistoryPoint p : snap.history) {
            if (p.epoch <= 0 || p.epoch == lastEpoch) continue;
            lastEpoch = p.epoch;
            int mx = (int) xOf.applyAsDouble(p.timestamp);
            svg.append(String.format(
                "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#f59e0b\" stroke-width=\"1\" stroke-dasharray=\"4,3\" />",
                mx, phaseY - 8, mx, markerY + 8));
            svg.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-size=\"9\" fill=\"#f59e0b\" transform=\"rotate(-45 %d %d)\">Epoch %d</text>",
                mx + 4, markerY + 12, mx + 4, markerY + 12, p.epoch));
        }

        // Current position marker (real, live)
        long now = snap.isTrainingActive ? snap.capturedAtMs : end;
        int nx = (int) xOf.applyAsDouble(now);
        svg.append(String.format(
            "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#10b981\" stroke-width=\"2\" />",
            nx, phaseY - 12, nx, phaseY + 34));
        svg.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-size=\"10\" fill=\"#10b981\">NOW</text>",
            nx + 4, phaseY - 6));

        svg.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-size=\"10\" fill=\"#8ea3c0\">%s</text>",
            padL, height - 14, timeOf(start)));
        svg.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-size=\"10\" fill=\"#8ea3c0\" text-anchor=\"end\">%s</text>",
            padL + plotW, height - 14, timeOf(end)));
        return svg.toString();
    }

    private static String graphCss() {
        return "<style>\n"
            + "  :root { --bg:#0b1220; --surface:#111c30; --border:#26354f; --text:#e2e8f0; --muted:#8ea3c0; }\n"
            + "  * { box-sizing: border-box; margin: 0; padding: 0; }\n"
            + "  html, body { min-height:100%; }\n"
            + "  body { font-family: system-ui, -apple-system, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;\n"
            + "         background: var(--bg); color: var(--text); padding: 12px; user-select: none; overflow: auto; }\n"
            + "  .summary-bar { display:flex; flex-wrap:wrap; gap:14px 20px; background:var(--surface); border:1px solid var(--border);\n"
            + "         border-radius:8px; padding:12px 16px; margin-bottom:12px; }\n"
            + "  .summary-item { font-size:12px; display:flex; align-items:center; gap:5px; }\n"
            + "  .summary-label { color:var(--muted); font-weight:500; }\n"
            + "  .summary-value { color:#f1f5f9; font-weight:700; }\n"
            + "  .card { background:var(--surface); border:1px solid var(--border); border-radius:8px;\n"
            + "         padding:14px; margin-bottom:12px; display:flex; flex-direction:column; }\n"
            + "  .header { display:flex; justify-content:space-between; align-items:flex-start; gap:10px; margin-bottom:10px;\n"
            + "         padding-bottom:8px; border-bottom:1px solid var(--border); }\n"
            + "  .title-group { display:flex; flex-direction:column; min-width:0; }\n"
            + "  .title { font-size:14px; font-weight:700; color:#f1f5f9; line-height:1.35; }\n"
            + "  .subtitle { font-size:12px; color:var(--muted); margin-top:2px; line-height:1.4; }\n"
            + "  .badge { padding:3px 9px; border-radius:12px; font-size:11px; font-weight:600; white-space:nowrap;\n"
            + "         text-transform:uppercase; letter-spacing:0.5px; }\n"
            + "  .badge-primary { background:#0c2a52; color:#7ab7ff; border:1px solid #1d5296; }\n"
            + "  .badge-success { background:#0c2e24; color:#34d399; border:1px solid #1a6b4a; }\n"
            + "  .badge-danger { background:#3a1215; color:#fca5a5; border:1px solid #7f1d1d; }\n"
            + "  .badge-neutral { background:#1e293b; color:#94a3b8; border:1px solid #33415c; }\n"
            + "  .graph-container { position:relative; width:100%; display:flex; justify-content:center; align-items:center; }\n"
            + "  svg { display:block; width:100%; height:auto; max-height:420px; overflow:visible; }\n"
            + "  .dot { cursor:pointer; }\n"
            + "  .dot:hover { opacity:0.75; }\n"
            + "  .tooltip { position:absolute; background:rgba(2,6,23,0.96); color:#fff; padding:5px 9px;\n"
            + "         border-radius:4px; font-size:11px; pointer-events:none; opacity:0; transition:opacity 0.15s ease;\n"
            + "         white-space:nowrap; z-index:100; border:1px solid #33415c; }\n"
            + "  .legend { display:flex; gap:16px; justify-content:center; margin-top:8px; font-size:11px; color:#c3d0e5; flex-wrap:wrap; }\n"
            + "  .legend-item { display:flex; align-items:center; gap:5px; }\n"
            + "  .legend-line { width:16px; height:3px; border-radius:2px; }\n"
            + interactionCss()
            + "</style>\n";
    }

    private static String tabsCss() {
        return "<style>\n"
            + "  .tab-bar { display:flex; gap:6px; margin-bottom:10px; flex-wrap:wrap; }\n"
            + "  .tab-btn { background:#111c30; color:#8ea3c0; border:1px solid #26354f; border-radius:6px 6px 0 0;\n"
            + "         padding:7px 16px; font-size:12px; cursor:pointer; }\n"
            + "  .tab-btn.active { background:#1d5296; color:#ffffff; border-color:#1d5296; }\n"
            + "  .tab-head { font-size:12px; color:#c3d0e5; margin-bottom:6px; font-weight:600; }\n"
            + "  .tab-svg-wrap { width:100%; }\n"
            + "  .tab-svg-wrap svg { display:block; width:100%; height:auto; max-height:340px; overflow:visible; }\n"
            + interactionCss()
            + "</style>\n";
    }

    /** Shared CSS for the reusable interactive zoom/pan toolbar (embedded + external browser). */
    private static String interactionCss() {
        return "  .viz-pane { position:relative; overflow:hidden; cursor:grab; touch-action:none; user-select:none; }\n"
            + "  .viz-pane svg { transition: none; }\n"
            + "  .viz-toolbar { position:absolute; top:6px; right:6px; display:flex; align-items:center; gap:3px;\n"
            + "         background:rgba(2,6,23,0.82); border:1px solid #33415c; border-radius:6px; padding:2px 4px; z-index:50; }\n"
            + "  .viz-toolbar button { background:#1e293b; color:#cbd5e1; border:1px solid #33415c; border-radius:4px;\n"
            + "         min-width:24px; height:20px; font-size:12px; line-height:1; cursor:pointer; padding:0 5px; }\n"
            + "  .viz-toolbar button:hover { background:#2a3b58; color:#fff; }\n"
            + "  .viz-zoom-level { font-size:11px; color:#8ea3c0; min-width:40px; text-align:center; }\n"
            + "  .external-header { background:var(--surface); border:1px solid var(--border); border-radius:8px;\n"
            + "         padding:10px 16px; margin-bottom:12px; }\n"
            + "  .external-header-title { font-size:15px; font-weight:700; color:#f1f5f9; }\n"
            + "  .external-header-meta { font-size:11px; color:var(--muted); margin-top:3px; }\n";
    }

    private static String tabScript() {
        return "<script>\n"
            + "  function showTab(id, btn) {\n"
            + "    document.querySelectorAll('.tab-content').forEach(function(c){ c.style.display='none'; });\n"
            + "    document.getElementById(id).style.display='block';\n"
            + "    document.querySelectorAll('.tab-btn').forEach(function(b){ b.classList.remove('active'); });\n"
            + "    btn.classList.add('active');\n"
            + "  }\n"
            + "</script>\n";
    }

    private static String tooltipScript() {
        return "<script>\n"
            + "  function setupTooltips() {\n"
            + "    document.querySelectorAll('.graph-container').forEach(function(container) {\n"
            + "      var tooltip = container.querySelector('.tooltip');\n"
            + "      if (!tooltip) return;\n"
            + "      container.querySelectorAll('.dot').forEach(function(dot) {\n"
            + "        dot.addEventListener('mousemove', function(e) {\n"
            + "          var rect = container.getBoundingClientRect();\n"
            + "          tooltip.style.left = (e.clientX - rect.left + 10) + 'px';\n"
            + "          tooltip.style.top = (e.clientY - rect.top - 25) + 'px';\n"
            + "          tooltip.textContent = dot.getAttribute('data-tooltip');\n"
            + "          tooltip.style.opacity = '1';\n"
            + "        });\n"
            + "        dot.addEventListener('mouseleave', function() { tooltip.style.opacity = '0'; });\n"
            + "      });\n"
            + "    });\n"
            + "  }\n"
            + "  setupTooltips();\n"
            + "</script>\n";
    }

    /**
     * Reusable interactive zoom/pan/fit/reset for every SVG diagram pane. Self-contained
     * (no external library), feature-detects pointer vs mouse events and wheel support, and
     * runs identically in the embedded SWT Browser and an external browser.
     */
    private static String interactionScript() {
        return "<script>\n"
            + "  (function() {\n"
            + "    var MIN = 0.2, MAX = 8.0;\n"
            + "    function clamp(v, lo, hi) { return Math.max(lo, Math.min(hi, v)); }\n"
            + "    function initPane(pane) {\n"
            + "      var svg = pane.querySelector('svg');\n"
            + "      if (!svg) return;\n"
            + "      pane.classList.add('viz-pane');\n"
            + "      var st = { s: 1, tx: 0, ty: 0 };\n"
            + "      svg.style.transformOrigin = '0 0';\n"
            + "      svg.style.willChange = 'transform';\n"
            + "      function apply() {\n"
            + "        svg.style.transform = 'translate(' + st.tx + 'px,' + st.ty + 'px) scale(' + st.s + ')';\n"
            + "        var l = pane.querySelector('.viz-zoom-level');\n"
            + "        if (l) l.textContent = Math.round(st.s * 100) + '%';\n"
            + "      }\n"
            + "      var bar = document.createElement('div');\n"
            + "      bar.className = 'viz-toolbar';\n"
            + "      bar.innerHTML = '<button data-a=\"in\" title=\"Zoom in\">+</button>'"
            + " + '<button data-a=\"out\" title=\"Zoom out\">\\u2212</button>'"
            + " + '<button data-a=\"fit\" title=\"Fit to view\">Fit</button>'"
            + " + '<button data-a=\"reset\" title=\"Reset\">Reset</button>'"
            + " + '<span class=\"viz-zoom-level\">100%</span>';\n"
            + "      pane.insertBefore(bar, pane.firstChild);\n"
            + "      bar.addEventListener('click', function(e) {\n"
            + "        var a = e.target.getAttribute && e.target.getAttribute('data-a');\n"
            + "        if (a === 'in') zoom(1.25);\n"
            + "        else if (a === 'out') zoom(0.8);\n"
            + "        else if (a === 'fit' || a === 'reset') { st.s = 1; st.tx = 0; st.ty = 0; apply(); }\n"
            + "      });\n"
            + "      function zoom(factor, cx, cy) {\n"
            + "        var r = pane.getBoundingClientRect();\n"
            + "        if (cx == null) cx = r.width / 2;\n"
            + "        if (cy == null) cy = r.height / 2;\n"
            + "        var ns = clamp(st.s * factor, MIN, MAX);\n"
            + "        var k = ns / st.s;\n"
            + "        st.tx = cx - (cx - st.tx) * k;\n"
            + "        st.ty = cy - (cy - st.ty) * k;\n"
            + "        st.s = ns;\n"
            + "        apply();\n"
            + "      }\n"
            + "      if (typeof window.addEventListener === 'function') {\n"
            + "        pane.addEventListener('wheel', function(e) {\n"
            + "          e.preventDefault();\n"
            + "          var r = pane.getBoundingClientRect();\n"
            + "          zoom(e.deltaY < 0 ? 1.15 : 0.87, e.clientX - r.left, e.clientY - r.top);\n"
            + "        }, { passive: false });\n"
            + "      }\n"
            + "      var dragging = false, sx = 0, sy = 0, otx = 0, oty = 0;\n"
            + "      function down(x, y, id) {\n"
            + "        dragging = true; sx = x; sy = y; otx = st.tx; oty = st.ty;\n"
            + "        pane.style.cursor = 'grabbing';\n"
            + "        if (id != null && pane.setPointerCapture) { try { pane.setPointerCapture(id); } catch (e) {} }\n"
            + "      }\n"
            + "      function move(x, y) { if (!dragging) return; st.tx = otx + (x - sx); st.ty = oty + (y - sy); apply(); }\n"
            + "      function up() { dragging = false; pane.style.cursor = 'grab'; }\n"
            + "      var hasPointer = ('PointerEvent' in window);\n"
            + "      if (hasPointer) {\n"
            + "        pane.addEventListener('pointerdown', function(e) { down(e.clientX, e.clientY, e.pointerId); });\n"
            + "        pane.addEventListener('pointermove', function(e) { move(e.clientX, e.clientY); });\n"
            + "        pane.addEventListener('pointerup', up);\n"
            + "        pane.addEventListener('pointercancel', up);\n"
            + "      } else {\n"
            + "        pane.addEventListener('mousedown', function(e) { down(e.clientX, e.clientY, null); });\n"
            + "        pane.addEventListener('mousemove', function(e) { move(e.clientX, e.clientY); });\n"
            + "        pane.addEventListener('mouseup', up);\n"
            + "        pane.addEventListener('mouseleave', up);\n"
            + "      }\n"
            + "      pane.style.cursor = 'grab';\n"
            + "      pane.style.overflow = 'hidden';\n"
            + "      pane.style.userSelect = 'none';\n"
            + "      apply();\n"
            + "    }\n"
            + "    function init() {\n"
            + "      document.querySelectorAll('.graph-container, .tab-svg-wrap').forEach(initPane);\n"
            + "    }\n"
            + "    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();\n"
            + "  })();\n"
            + "</script>\n";
    }

    // =====================================================================
    // ACTIONS
    // =====================================================================

    private void handleCreateEvodata() {
        List<DatasetItem> checkedItems = new ArrayList<>();
        for (DatasetItem item : datasetItems) {
            if (item.isChecked() && item.getPath() != null && !item.getPath().trim().isEmpty()) {
                checkedItems.add(item);
            }
        }

        if (checkedItems.isEmpty()) {
            MessageDialog.openWarning(getShell(), "No Datasets Selected", "Please select and check at least one dataset item in the table.");
            return;
        }

        List<eu.kalafatic.evolution.forge.data.api.source.DatasetItem> apiItems = new ArrayList<>();
        for (DatasetItem item : checkedItems) {
            apiItems.add(new eu.kalafatic.evolution.forge.data.api.source.DatasetItem(item.isChecked(), item.getPath(), item.getType()));
        }

        String baseWorkspace = ProjectModelManager.getWorkspacePath();
        if (baseWorkspace == null || baseWorkspace.trim().isEmpty()) {
            baseWorkspace = new File(System.getProperty("user.home"), "workspace").getAbsolutePath();
        }
        File outputDir = new File(baseWorkspace, "forge-input");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        new Thread(() -> {
            try {
                DatasetPreparationService service = new DatasetPreparationService();
                DatasetPreparationContext context = new DatasetPreparationContext();
                DatasetPreparationResult result = service.prepareDatasets(apiItems, context, outputDir);

                Display.getDefault().asyncExec(() -> {
                    if (getShell() != null && !getShell().isDisposed()) {
                        if (result.getStatus() == DatasetPreparationResult.Status.SUCCESS && result.getOutputPath() != null) {
                            File evodataFile = new File(result.getOutputPath());
                            boolean existsInList = false;
                            for (DatasetItem item : datasetItems) {
                                if (item.getPath().equalsIgnoreCase(evodataFile.getAbsolutePath())) {
                                    item.setChecked(true);
                                    existsInList = true;
                                    break;
                                }
                            }
                            if (!existsInList) {
                                datasetItems.add(new DatasetItem(true, evodataFile.getAbsolutePath(), "FILE"));
                            }
                            refreshDatasetsTable();

                            MessageDialog.openInformation(getShell(), "Dataset Created",
                                "Native .evodata artifact created successfully!\n\n" +
                                "Output File: " + evodataFile.getAbsolutePath() + "\n" +
                                "Records Accepted: " + result.getRecordsAccepted() + "\n" +
                                "Usable Bytes: " + result.getAcceptedBytes());
                        } else {
                            String errorMsg = !result.getErrors().isEmpty() ? String.join("\n", result.getErrors()) : "Status: " + result.getStatus();
                            MessageDialog.openError(getShell(), "Dataset Creation Failed", "Failed to create .evodata artifact:\n" + errorMsg);
                        }
                    }
                });
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> {
                    if (getShell() != null && !getShell().isDisposed()) {
                        MessageDialog.openError(getShell(), "Error", "Error creating .evodata artifact: " + ex.getMessage());
                    }
                });
            }
        }).start();
    }

    private void handleSmartScaling() {
        double hours = 12.0;
        if (targetTimeCombo != null && !targetTimeCombo.isDisposed()) {
            hours = ForgeTrainingEstimationAgent.getInstance().parseHoursOption(targetTimeCombo.getText());
        }
        selectedTargetHours = hours;

        ForgeTrainingEstimationAgent.SmartScalingResult result =
            ForgeTrainingEstimationAgent.getInstance().applySmartDataScaling(datasetItems, selectedModelSize, selectedEpochs, hours);

        if (result.isSuccess()) {
            selectedEpochs = result.getUpdatedEpochs();
            if (epochCombo != null && !epochCombo.isDisposed()) {
                epochCombo.setText(String.valueOf(selectedEpochs));
            }
            refreshDatasetsTable();
            updateHardwareAndTimeInfo();
            scheduleGraphUpdate();
            MessageDialog.openInformation(getShell(), "Smart Data Scaling Applied",
                String.format("Training parameters and dataset sample ratios scaled for target forging time of %.1f hours.\n" +
                    "Updated Epochs: %d\nEstimated Duration: %s",
                    hours, selectedEpochs, formatDuration(getEstimatedForgingSeconds())));
        } else {
            MessageDialog.openWarning(getShell(), "Scaling Warning",
                "Could not apply smart scaling. Ensure checked datasets are valid.");
        }
    }

    private void handleStartOrResumeTraining() {
        String sid = resolveForgeSessionId();
        if (sid == null) {
            MessageDialog.openWarning(getShell(), "No Forge Session", "No forge session is available to start training.");
            return;
        }
        int port = resolveServerPort();
        final String sessionId = sid;
        final int serverPort = port;
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + serverPort + "/forge/session/" + sessionId + "/forging/start"))
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> {
                    if (getShell() != null && !getShell().isDisposed()) {
                        MessageDialog.openError(getShell(), "Start Failed",
                            "Could not start training via the EVO server: " + ex.getMessage());
                    }
                });
            }
        }, "forge-settings-start").start();
        MessageDialog.openInformation(getShell(), "Training Started",
            "Training start requested. The monitor will refresh automatically.\n\n"
            + "The backend prepares the dataset, validates configuration, initializes the model and starts training.");
    }

    private int resolveServerPort() {
        try {
            var orch = OrchestratorServiceImpl.getInstance().getOrchestrator();
            if (orch != null && orch.getServerSettings() != null && orch.getServerSettings().getPort() > 0) {
                return orch.getServerSettings().getPort();
            }
        } catch (Exception ignored) {}
        return 28080;
    }

    private void handlePauseTraining(Button btn) {
        String sid = resolveForgeSessionId();
        if (sid == null) {
            MessageDialog.openInformation(getShell(), "No Active Trainer", "No active training run detected in progress to pause.");
            return;
        }
        boolean requested = ForgeSessionManager.getInstance().requestPause(sid);
        if (requested) {
            MessageDialog.openInformation(getShell(), "Pausing Training",
                "Training pause requested!\n\nThe trainer will pause at the current batch and save a resumption checkpoint.");
        } else {
            MessageDialog.openInformation(getShell(), "No Active Trainer", "No active training run detected in progress to pause.");
        }
    }

    private void handleAbortTraining(Button btn) {
        boolean confirm = MessageDialog.openConfirm(getShell(), "Confirm Cancel",
                "Are you sure you want to cancel (abort) training? All unfinalized progress will be discarded.");
        if (!confirm) return;
        String sid = resolveForgeSessionId();
        if (sid == null) {
            MessageDialog.openInformation(getShell(), "No Active Trainer", "No active training run detected in progress to cancel.");
            return;
        }
        boolean requested = ForgeSessionManager.getInstance().requestAbort(sid);
        if (requested) {
            MessageDialog.openInformation(getShell(), "Training Cancelled",
                "Training cancel (abort) requested. Running processes are halting.");
        } else {
            MessageDialog.openInformation(getShell(), "No Active Trainer", "No active training run detected in progress to cancel.");
        }
    }

    private void handleFinishTraining(Button btn) {
        String sid = resolveForgeSessionId();
        if (sid == null) {
            MessageDialog.openInformation(getShell(), "No Active Trainer",
                "No active training run detected in progress.\n" +
                "When training is running, clicking this button gracefully finishes the current epoch and exports all files.");
            return;
        }
        boolean requested = ForgeSessionManager.getInstance().requestFinish(sid);
        if (btn != null && !btn.isDisposed()) {
            btn.setText("Finishing Epoch...");
            btn.setEnabled(false);
        }
        if (requested) {
            MessageDialog.openInformation(getShell(), "Finishing Training",
                "Training finish requested!\n\n" +
                "The trainer will complete its current step/epoch, retain all learned weights, " +
                "and proceed immediately to compile, save, and export all model files (.evo, GGUF, report).");
        } else {
            MessageDialog.openInformation(getShell(), "No Active Trainer",
                "No active training run detected in progress.\n" +
                "When training is running, clicking this button gracefully finishes the current epoch and exports all files.");
        }
    }

    /**
     * Writes the current FORGE training-monitor diagrams (center cards + tabbed graphs) to
     * a reusable HTML artifact on disk and opens it in the system default browser. The
     * generated HTML is fully self-contained (inline CSS + JS), so interactivity (zoom,
     * pan, fit, reset, tooltips) is preserved independently of the SWT Browser instance.
     */
    private void openDiagramInBrowser() {
        ForgeTelemetrySnapshot snap = captureSnapshot();
        if (snap == null) {
            snap = ForgeTelemetrySnapshot.configOnly(selectedModelSize, selectedEpochs, selectedDesiredLoss,
                    selectedTargetHours, selectedEarlyStoppingEnabled, lossHistory, calculateTotalDatasetBytes());
        }
        String sessionId = (snap.sessionId != null && !snap.sessionId.trim().isEmpty())
                ? snap.sessionId : "preview";
        String sessionName = (snap.sessionName != null && !snap.sessionName.trim().isEmpty())
                ? snap.sessionName : "FORGE Training Monitor";

        File dir = new File(ForgeSessionManager.getForgeStateDirectory().toFile(), "html-diagrams");
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                dir = new File(System.getProperty("java.io.tmpdir"), "evo-forge-diagrams");
                dir.mkdirs();
            }
            String title = "FORGE " + sessionName + " — Training Monitor";
            String centerHtml = withDocumentHeader(generateGraphHtml(snap), title, sessionId, snap.status);
            String graphsHtml = withDocumentHeader(generateMonitoringGraphsHtml(snap),
                    title + " (Detail Graphs)", sessionId, snap.status);

            File centerFile = new File(dir, sanitizeFileName("forge-" + sessionId + "-monitor.html"));
            File graphsFile = new File(dir, sanitizeFileName("forge-" + sessionId + "-graphs.html"));
            java.nio.file.Files.writeString(centerFile.toPath(), centerHtml, java.nio.charset.StandardCharsets.UTF_8);
            java.nio.file.Files.writeString(graphsFile.toPath(), graphsHtml, java.nio.charset.StandardCharsets.UTF_8);

            // Open the generated diagram through the shared external-browser
            // launcher: validates the target, refuses mail handlers and logs
            // the outcome. User feedback is handled by the launcher.
            eu.kalafatic.evolution.view.util.ExternalBrowserLauncher
                    .launchHtmlFile("ForgeSettingsDialog", centerFile.toPath());
        } catch (Exception ex) {
            MessageDialog.openError(getShell(), "Open in Browser",
                    "Failed to write or open the diagram HTML artifact: " + ex.getMessage());
        }
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String withDocumentHeader(String html, String title, String sessionId, String status) {
        String withTitle = html.contains("<title>") ? html
                : html.replace("</head>", "<title>" + escapeHtml(title) + "</title>\n</head>");
        String header = "<div class=\"external-header\">"
                + "<div class=\"external-header-title\">" + escapeHtml(title) + "</div>"
                + "<div class=\"external-header-meta\">Session: " + escapeHtml(sessionId)
                + " &middot; Status: " + escapeHtml(status != null ? status : "UNKNOWN") + "</div></div>";
        return withTitle.replace("<body>", "<body>\n" + header + "\n");
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private void handleOpenTrainingReport() {
        String sid = resolveForgeSessionId();
        List<File> candidates = new ArrayList<>();
        String codebase = ProjectModelManager.getCodebasePath();
        if (codebase != null && !codebase.isEmpty()) {
            candidates.add(new File(codebase, "dist/forging-" + sid));
            candidates.add(new File(codebase, "dist"));
        }
        String workspace = ProjectModelManager.getWorkspacePath();
        if (workspace != null && !workspace.isEmpty()) {
            candidates.add(new File(workspace, "dist/forging-" + sid));
            candidates.add(new File(workspace, "dist"));
        }
        for (File f : candidates) {
            if (f != null && f.isDirectory()) {
                Program.launch(f.getAbsolutePath());
                return;
            }
        }
        MessageDialog.openInformation(getShell(), "Training Report",
            "No training output folder found yet. The run folder is created in dist/forging-<sessionId> when training starts.");
    }

    // =====================================================================
    // DIALOG LIFECYCLE
    // =====================================================================

    @Override
    protected void okPressed() {
        readEffectiveSettingsFromUiSafe();
        stopAutoRefresh();
        super.okPressed();
    }

    @Override
    protected void cancelPressed() {
        stopAutoRefresh();
        super.cancelPressed();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button estimateBtn = createButton(parent, 2000, "Estimate", false);
        estimateBtn.setToolTipText("Recalculate the training time estimate using current configuration and all available measured history.");
        estimateBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                readEffectiveSettingsFromUiSafe();
                updateHardwareAndTimeInfo();
                scheduleGraphUpdate();
            }
        });

        startTrainingBtn = createButton(parent, 2001, "Start Training", false);
        startTrainingBtn.setToolTipText("Start the real FORGE training flow for the active forge session (dataset preparation → validation → training → export).");
        startTrainingBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleStartOrResumeTraining();
            }
        });

        pauseBtn = createButton(parent, 2002, "Pause", false);
        pauseBtn.setToolTipText("Pause training at the current batch and save a resumption checkpoint.");
        pauseBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handlePauseTraining(pauseBtn);
            }
        });

        resumeBtn = createButton(parent, 2003, "Resume", false);
        resumeBtn.setToolTipText("Resume training from the saved checkpoint (the backend restarts forging and auto-discovers the compatible checkpoint).");
        resumeBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleStartOrResumeTraining();
            }
        });

        cancelBtn = createButton(parent, 2004, "Cancel", false);
        cancelBtn.setToolTipText("Immediately cancel (abort) training and discard the unfinished model.");
        cancelBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleAbortTraining(cancelBtn);
            }
        });

        reportBtn = createButton(parent, 2005, "Open Training Report", false);
        reportBtn.setToolTipText("Open the run output folder (dist/forging-<sessionId>) containing the forging log, checkpoints and exported models.");
        reportBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleOpenTrainingReport();
            }
        });

        finishBtn = createButton(parent, 1001, "Stop & Finalize", false);
        finishBtn.setToolTipText("Gracefully stop training at current state/epoch and export valid .evo model files");
        finishBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleFinishTraining(finishBtn);
            }
        });

        super.createButtonsForButtonBar(parent);
    }

    // =====================================================================
    // GETTERS (public API preserved)
    // =====================================================================

    public String getSelectedModelSize() {
        return selectedModelSize;
    }

    public int getEpochs() {
        return selectedEpochs;
    }

    public int getBatchSize() {
        return selectedBatchSize;
    }

    public double getLearningRate() {
        return selectedLearningRate;
    }

    public int getGenerations() {
        return selectedGenerations;
    }

    public int getCandidatesPerGeneration() {
        return selectedCandidates;
    }

    public int getCheckpointFrequencyEpochs() {
        return selectedCheckpointFreq;
    }

    public String getOutputModelName() {
        return selectedOutputModelName;
    }

    public long getMaxDataSizeMb() {
        return selectedMaxDataSizeMb;
    }

    public long getMaxSampleCount() {
        return selectedMaxSampleCount;
    }

    public int getMaxSeqLen() {
        return selectedMaxSeqLen;
    }

    public double getValidationRatio() {
        return selectedValRatio;
    }

    public boolean isFilterQuality() {
        return selectedQualityFilter;
    }

    public boolean isSaveEpochCheckpoints() {
        return selectedSaveEpochCheckpoints;
    }

    public String getLossThreshold() {
        return selectedLossThreshold;
    }

    public double getDesiredLossThreshold() {
        return selectedDesiredLoss;
    }

    public List<DatasetItem> getDatasets() {
        return datasetItems;
    }

    public String getDatasetsJson() {
        JSONArray arr = new JSONArray();
        for (DatasetItem item : datasetItems) {
            arr.put(item.toJsonObject());
        }
        return arr.toString();
    }

    public JSONObject getUiStateJson() {
        JSONObject json = new JSONObject();
        json.put("modelSize", selectedModelSize);
        json.put("epochs", selectedEpochs);
        json.put("batchSize", selectedBatchSize);
        json.put("learningRate", selectedLearningRate);
        json.put("generations", selectedGenerations);
        json.put("candidatesPerGeneration", selectedCandidates);
        json.put("checkpointFrequencyEpochs", selectedCheckpointFreq);
        json.put("maxDataSizeMb", selectedMaxDataSizeMb);
        json.put("maxSampleCount", selectedMaxSampleCount);
        json.put("maxTokensLimit", selectedMaxTokensLimit);
        json.put("maxSeqLen", selectedMaxSeqLen);
        json.put("validationRatio", selectedValRatio);
        json.put("filterQuality", selectedQualityFilter);
        json.put("earlyStoppingEnabled", selectedEarlyStoppingEnabled);
        json.put("saveEpochCheckpoints", selectedSaveEpochCheckpoints);
        json.put("quantizationEnabled", selectedQuantizationEnabled);
        json.put("quantizationScheme", selectedQuantizationScheme);
        json.put("outputModelName", selectedOutputModelName);
        json.put("lossThreshold", selectedLossThreshold);
        json.put("desiredLossThreshold", selectedDesiredLoss);
        json.put("timeBudgetHours", selectedTargetHours);
        json.put("datasets", getDatasetsJson());
        return json;
    }
}
