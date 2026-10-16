package eu.kalafatic.evolution.view.dialogs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.manager.ModelSizePreset;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.forge.controller.service.impl.agents.ForgeTrainingEstimationAgent;
import eu.kalafatic.evolution.forge.data.api.service.DatasetPreparationResult;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.impl.service.DatasetPreparationService;
import eu.kalafatic.utils.factories.GUIFactory;

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
        return ForgeTrainingEstimationAgent.getInstance().getModelParameterCount(modelSize);
    }

    public static double getBaselineTokensPerSecond(String modelSize) {
        return ForgeTrainingEstimationAgent.getInstance().getBaselineTokensPerSecond(modelSize);
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
        return ForgeTrainingEstimationAgent.getInstance().calculateEstimatedForgingSeconds(modelSize, epochs, datasetBytes);
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
    private org.eclipse.swt.widgets.Text maxDataSizeText;
    private org.eclipse.swt.widgets.Text maxSampleCountText;
    private org.eclipse.swt.widgets.Text maxSeqLenText;
    private org.eclipse.swt.widgets.Text valRatioText;
    private org.eclipse.swt.widgets.Text outputModelNameText;
    private Button qualityFilterCheck;
    private Label estTimeLabel;
    private Label hwProfileLabel;
    private Browser graphBrowser;
    private Table datasetsTable;

    private String selectedModelSize = "SMALL";
    private int selectedEpochs = 32;
    private int selectedBatchSize = 16;
    private double selectedLearningRate = 0.01;
    private int selectedGenerations = 1;
    private int selectedCandidates = 1;
    private int selectedCheckpointFreq = 5;
    private long selectedMaxDataSizeMb = 500;
    private long selectedMaxSampleCount = -1;
    private int selectedMaxSeqLen = 1024;
    private double selectedValRatio = 0.15;
    private boolean selectedQualityFilter = true;
    private String selectedOutputModelName = "";
    private String selectedLossThreshold = "Epoch 16-30: Loss 2-5 → Learning phrases";
    private double selectedDesiredLoss = 1.0;
    private double selectedTargetHours = 12.0;
    private double[] lossHistory = null;
    private List<DatasetItem> datasetItems = new ArrayList<>();

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
        newShell.setText("FORGE Settings & Loss Progress Graph");
    }

    @Override
    protected Point getInitialSize() {
        return new Point(1080, 760);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, true));

        SashForm sashForm = new SashForm(container, SWT.HORIZONTAL | SWT.SMOOTH);
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

        // Reusable auto-refresh listeners for UI controls
        SelectionListener autoRefreshSelectionAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                readEffectiveSettingsFromUi();
                updateHardwareAndTimeInfo();
                updateGraphHtml();
            }
        };

        ModifyListener autoRefreshModifyAdapter = e -> {
            readEffectiveSettingsFromUi();
            updateHardwareAndTimeInfo();
            updateGraphHtml();
        };

        // Left Scrolled Composite for Settings Panel
        ScrolledComposite leftScrolled = new ScrolledComposite(sashForm, SWT.V_SCROLL | SWT.H_SCROLL);
        leftScrolled.setExpandHorizontal(true);
        leftScrolled.setExpandVertical(true);
        leftScrolled.setLayoutData(new GridData(GridData.FILL_BOTH));

        // Panel 1: Settings Panel
        Composite settingsPanel = new Composite(leftScrolled, SWT.NONE);
        settingsPanel.setLayout(new GridLayout(2, false));
        leftScrolled.setContent(settingsPanel);

        settingsPanel.addListener(SWT.Resize, event -> {
            Point size = settingsPanel.computeSize(leftScrolled.getClientArea().width, SWT.DEFAULT, true);
            leftScrolled.setMinSize(size);
        });

        // Presets Group
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
                updateHardwareAndTimeInfo();
                updateGraphHtml();
            }
        });

        Group settingsGroup = new Group(settingsPanel, SWT.NONE);
        settingsGroup.setText("Forge Model & Training Configuration");
        settingsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        settingsGroup.setLayout(new GridLayout(4, false));

        // 1. Preset Model Sizes Combo
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
                updateHardwareAndTimeInfo();
                updateGraphHtml();
            }
        });

        // 2. Epoch Count (With Tooltip)
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

        // 3. Batch Size
        GUIFactory.INSTANCE.createLabel(settingsGroup, "Batch Size:");
        batchSizeCombo = new Combo(settingsGroup, SWT.DROP_DOWN);
        batchSizeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        for (String b : new String[] { "2", "4", "8", "16", "32", "64" }) batchSizeCombo.add(b);
        batchSizeCombo.setText(String.valueOf(selectedBatchSize));
        batchSizeCombo.addSelectionListener(autoRefreshSelectionAdapter);
        batchSizeCombo.addModifyListener(autoRefreshModifyAdapter);

        // 4. Learning Rate
        GUIFactory.INSTANCE.createLabel(settingsGroup, "Learning Rate:");
        learningRateCombo = new Combo(settingsGroup, SWT.DROP_DOWN);
        learningRateCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        for (String lr : new String[] { "0.05", "0.01", "0.005", "0.001", "0.0005", "0.0001" }) learningRateCombo.add(lr);
        learningRateCombo.setText(String.valueOf(selectedLearningRate));
        learningRateCombo.addSelectionListener(autoRefreshSelectionAdapter);
        learningRateCombo.addModifyListener(autoRefreshModifyAdapter);

        // 5. Evolutionary Generations (With Tooltip)
        Label genLabel = GUIFactory.INSTANCE.createLabel(settingsGroup, "Generations:");
        genLabel.setToolTipText("Generations: How many evolutionary rounds are executed.");
        generationsCombo = new Combo(settingsGroup, SWT.DROP_DOWN);
        generationsCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        generationsCombo.setToolTipText("Generations: How many evolutionary rounds are executed.");
        for (String g : new String[] { "1", "2", "3", "5", "10" }) generationsCombo.add(g);
        generationsCombo.setText(String.valueOf(selectedGenerations));
        generationsCombo.addSelectionListener(autoRefreshSelectionAdapter);
        generationsCombo.addModifyListener(autoRefreshModifyAdapter);

        // 6. Candidates Per Generation (With Tooltip)
        Label candLabel = GUIFactory.INSTANCE.createLabel(settingsGroup, "Candidates / Gen:");
        candLabel.setToolTipText("Candidates: How many model candidates are trained or evaluated in each evolutionary generation.");
        candidatesCombo = new Combo(settingsGroup, SWT.DROP_DOWN);
        candidatesCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        candidatesCombo.setToolTipText("Candidates: How many model candidates are trained or evaluated in each evolutionary generation.");
        for (String c : new String[] { "1", "2", "3", "4", "8" }) candidatesCombo.add(c);
        candidatesCombo.setText(String.valueOf(selectedCandidates));
        candidatesCombo.addSelectionListener(autoRefreshSelectionAdapter);
        candidatesCombo.addModifyListener(autoRefreshModifyAdapter);

        // 7. Checkpoint Frequency
        GUIFactory.INSTANCE.createLabel(settingsGroup, "Checkpoint Freq (Epochs):");
        checkpointFreqCombo = new Combo(settingsGroup, SWT.DROP_DOWN);
        checkpointFreqCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        for (String cf : new String[] { "1", "2", "5", "10", "20" }) checkpointFreqCombo.add(cf);
        checkpointFreqCombo.setText(String.valueOf(selectedCheckpointFreq));
        checkpointFreqCombo.addSelectionListener(autoRefreshSelectionAdapter);
        checkpointFreqCombo.addModifyListener(autoRefreshModifyAdapter);

        // 8. Output Model Name
        GUIFactory.INSTANCE.createLabel(settingsGroup, "Output Model Name:");
        outputModelNameText = GUIFactory.INSTANCE.createText(settingsGroup);
        outputModelNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        outputModelNameText.setText(selectedOutputModelName != null ? selectedOutputModelName : "");
        outputModelNameText.addModifyListener(autoRefreshModifyAdapter);

        // 3. Loss Threshold Stage Combo
        GUIFactory.INSTANCE.createLabel(settingsGroup, "Loss Threshold Stage:");
        lossThresholdCombo = GUIFactory.INSTANCE.createCombo(settingsGroup);
        lossThresholdCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        for (String lossOpt : LOSS_THRESHOLD_OPTIONS) {
            lossThresholdCombo.add(lossOpt);
        }

        int defaultLossIdx = 2; // "Epoch 16-30: Loss 2-5 → Learning phrases"
        for (int i = 0; i < LOSS_THRESHOLD_OPTIONS.length; i++) {
            if (LOSS_THRESHOLD_OPTIONS[i].toLowerCase().contains(selectedLossThreshold.toLowerCase()) ||
                selectedLossThreshold.toLowerCase().contains(LOSS_THRESHOLD_OPTIONS[i].toLowerCase())) {
                defaultLossIdx = i;
                break;
            }
        }
        lossThresholdCombo.select(defaultLossIdx);
        lossThresholdCombo.addSelectionListener(autoRefreshSelectionAdapter);

        // 4. Desired / Finish Threshold Combo (When loss < target finish forging)
        GUIFactory.INSTANCE.createLabel(settingsGroup, "Desired Finish Loss:");
        desiredLossCombo = new Combo(settingsGroup, SWT.DROP_DOWN);
        desiredLossCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        for (String opt : DESIRED_LOSS_OPTIONS) {
            desiredLossCombo.add(opt);
        }

        int defaultDesiredIdx = 2; // "1.0"
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

        // Help / Info label
        Label helpLabel = new Label(settingsGroup, SWT.WRAP);
        helpLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        helpLabel.setText(
            "Loss / Stage Guidelines:\n" +
            "• Epoch 1-5: Loss 8-10 → Learning letters (g, u, a)\n" +
            "• Epoch 6-15: Loss 5-8 → Learning words (evolution, genome)\n" +
            "• Epoch 16-30: Loss 2-5 → Learning phrases (\"evolution is\")\n" +
            "• Epoch 31-50: Loss 0.5-2 → Learning sentences\n" +
            "• Epoch 51-64: Loss < 1 → Understanding concepts\n" +
            "• Desired Finish Loss: When actual loss drops below this target, forging stops early."
        );

        // Group 2: Hardware & Forging Time Controller Panel
        Group hwGroup = new Group(settingsPanel, SWT.NONE);
        hwGroup.setText("Hardware & Forging Time Controller");
        hwGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        hwGroup.setLayout(new GridLayout(2, false));

        hwProfileLabel = new Label(hwGroup, SWT.NONE);
        hwProfileLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        estTimeLabel = new Label(hwGroup, SWT.NONE);
        estTimeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        GUIFactory.INSTANCE.createLabel(hwGroup, "Target Forging Duration:");
        targetTimeCombo = new Combo(hwGroup, SWT.DROP_DOWN);
        targetTimeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        for (String opt : TARGET_TIME_OPTIONS) {
            targetTimeCombo.add(opt);
        }
        targetTimeCombo.select(4); // Default "12.0 hrs"
        targetTimeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedTargetHours = ForgeTrainingEstimationAgent.getInstance().parseHoursOption(targetTimeCombo.getText());
                updateHardwareAndTimeInfo();
                updateGraphHtml();
            }
        });

        Button smartScaleBtn = GUIFactory.INSTANCE.createButton(hwGroup, "Smart Scale Data for Target Time");
        smartScaleBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        smartScaleBtn.setToolTipText("Automatically scale dataset record limits and adjust epoch parameters to fit training within the target duration.");
        smartScaleBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleSmartScaling();
            }
        });

        updateHardwareAndTimeInfo();

        // Group 3: Training Datasets (Ordered Target Data)
        Group datasetsGroup = new Group(settingsPanel, SWT.NONE);
        datasetsGroup.setText("Training Datasets & Dataset Input Limits");
        datasetsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        datasetsGroup.setLayout(new GridLayout(2, false));

        // Limits composite inside datasets group
        Composite datasetLimitsComp = new Composite(datasetsGroup, SWT.NONE);
        datasetLimitsComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        datasetLimitsComp.setLayout(new GridLayout(4, false));

        GUIFactory.INSTANCE.createLabel(datasetLimitsComp, "Max Data Size (MB):");
        maxDataSizeText = GUIFactory.INSTANCE.createText(datasetLimitsComp);
        maxDataSizeText.setText(String.valueOf(selectedMaxDataSizeMb));
        maxDataSizeText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        maxDataSizeText.setToolTipText("Maximum input dataset size limit in MB (-1 for unlimited). EVO compiles usable records into .evodata.");
        maxDataSizeText.addModifyListener(autoRefreshModifyAdapter);

        GUIFactory.INSTANCE.createLabel(datasetLimitsComp, "Max Sample Count:");
        maxSampleCountText = GUIFactory.INSTANCE.createText(datasetLimitsComp);
        maxSampleCountText.setText(String.valueOf(selectedMaxSampleCount));
        maxSampleCountText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        maxSampleCountText.setToolTipText("Maximum accepted training records/samples (-1 for unlimited).");
        maxSampleCountText.addModifyListener(autoRefreshModifyAdapter);

        GUIFactory.INSTANCE.createLabel(datasetLimitsComp, "Max Sequence Length:");
        maxSeqLenText = GUIFactory.INSTANCE.createText(datasetLimitsComp);
        maxSeqLenText.setText(String.valueOf(selectedMaxSeqLen));
        maxSeqLenText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        maxSeqLenText.addModifyListener(autoRefreshModifyAdapter);

        GUIFactory.INSTANCE.createLabel(datasetLimitsComp, "Validation Ratio (%):");
        valRatioText = GUIFactory.INSTANCE.createText(datasetLimitsComp);
        valRatioText.setText(String.valueOf((int) (selectedValRatio * 100)));
        valRatioText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        valRatioText.addModifyListener(autoRefreshModifyAdapter);

        qualityFilterCheck = GUIFactory.INSTANCE.createCheckButton(datasetLimitsComp, "Enable Quality Filtering & Rejection");
        qualityFilterCheck.setSelection(selectedQualityFilter);
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
                    updateHardwareAndTimeInfo();
                    updateGraphHtml();
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

        // Right Scrolled Composite for Graphs Panel
        ScrolledComposite rightScrolled = new ScrolledComposite(sashForm, SWT.V_SCROLL | SWT.H_SCROLL);
        rightScrolled.setExpandHorizontal(true);
        rightScrolled.setExpandVertical(true);
        rightScrolled.setLayoutData(new GridData(GridData.FILL_BOTH));

        // Panel 2: Progress Graph Panel (HTML Browser Container)
        Composite graphPanel = new Composite(rightScrolled, SWT.NONE);
        graphPanel.setLayout(new GridLayout(1, true));
        rightScrolled.setContent(graphPanel);

        graphPanel.addListener(SWT.Resize, event -> {
            Point size = graphPanel.computeSize(rightScrolled.getClientArea().width, SWT.DEFAULT, true);
            rightScrolled.setMinSize(size);
        });

        Group graphGroup = new Group(graphPanel, SWT.NONE);
        graphGroup.setText("Training Performance & Capacity Visualizations");
        graphGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        graphGroup.setLayout(new GridLayout(1, true));

        graphBrowser = new Browser(graphGroup, SWT.NONE);
        GridData browserData = new GridData(SWT.FILL, SWT.FILL, true, true);
        browserData.minHeight = 820;
        graphBrowser.setLayoutData(browserData);

        sashForm.setWeights(new int[] { 45, 55 });

        // Initial HTML render
        updateGraphHtml();

        return container;
    }

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

        if (datasetsTable != null && !datasetsTable.isDisposed()) {
            for (int i = 0; i < datasetsTable.getItemCount(); i++) {
                TableItem item = datasetsTable.getItem(i);
                if (i < datasetItems.size()) {
                    datasetItems.get(i).setChecked(item.getChecked());
                }
            }
        }
    }

    private void updateDesiredLossFromCombo() {
        readEffectiveSettingsFromUi();
    }

    private void updateGraphHtml() {
        if (graphBrowser != null && !graphBrowser.isDisposed()) {
            readEffectiveSettingsFromUi();
            String html = generateGraphHtml(selectedEpochs, selectedDesiredLoss, lossHistory);
            graphBrowser.setText(html);
        }
    }

    private String generateGraphHtml(int maxEpochs, double targetFinishLoss, double[] history) {
        long totalDataBytes = calculateTotalDatasetBytes();
        double estSec = getEstimatedForgingSeconds();
        double params = getModelParameterCount(selectedModelSize);
        long totalTokens = estimateTotalTrainingTokens();
        double budgetHours = selectedTargetHours;
        double budgetSec = budgetHours * 3600.0;
        boolean fitsBudget = estSec <= budgetSec;

        // Configuration summary HTML header
        String configSummaryHtml = String.format(
            "<div class=\"summary-bar\">" +
            "  <div class=\"summary-item\"><span class=\"summary-label\">Model Size:</span> <span class=\"summary-value\">%s (%.1fM)</span></div>" +
            "  <div class=\"summary-item\"><span class=\"summary-label\">Epochs:</span> <span class=\"summary-value\">%d</span></div>" +
            "  <div class=\"summary-item\"><span class=\"summary-label\">Batch Size:</span> <span class=\"summary-value\">%d</span></div>" +
            "  <div class=\"summary-item\"><span class=\"summary-label\">Data Size:</span> <span class=\"summary-value\">%.2f MB</span></div>" +
            "  <div class=\"summary-item\"><span class=\"summary-label\">Est. Time:</span> <span class=\"summary-value\">%s</span></div>" +
            "  <div class=\"summary-item\"><span class=\"summary-label\">Budget:</span> <span class=\"summary-value %.0f\">%.1f hrs (%s)</span></div>" +
            "</div>",
            selectedModelSize, params / 1_000_000.0,
            selectedEpochs, selectedBatchSize,
            totalDataBytes / (1024.0 * 1024.0),
            formatDuration(estSec),
            selectedTargetHours,
            fitsBudget ? "FEASIBLE" : "EXCEEDS BUDGET"
        );

        // --- GRAPH 1: LOSS / EPOCH RELATION GRAPH ---
        int width1 = 720;
        int height1 = 320;
        int paddingLeft1 = 60;
        int paddingRight1 = 35;
        int paddingTop1 = 40;
        int paddingBottom1 = 50;

        int plotWidth1 = width1 - paddingLeft1 - paddingRight1;
        int plotHeight1 = height1 - paddingTop1 - paddingBottom1;
        int denom1 = Math.max(1, maxEpochs - 1);

        StringBuilder svg1Builder = new StringBuilder();

        for (int lossVal = 0; lossVal <= 10; lossVal += 2) {
            int y = paddingTop1 + plotHeight1 - (int) ((lossVal / 10.0) * plotHeight1);
            svg1Builder.append(String.format(
                "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#e2e8f0\" stroke-dasharray=\"3,3\" stroke-width=\"1\" />",
                paddingLeft1, y, paddingLeft1 + plotWidth1, y
            ));
            svg1Builder.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"11\" fill=\"#64748b\" text-anchor=\"end\">%d</text>",
                paddingLeft1 - 10, y + 4, lossVal
            ));
        }

        int[] xTicks1 = new int[] { 1, Math.max(2, maxEpochs / 4), Math.max(3, maxEpochs / 2), Math.max(4, (3 * maxEpochs) / 4), maxEpochs };
        for (int ep : xTicks1) {
            if (ep > maxEpochs) continue;
            int x = paddingLeft1 + (int) (((double) (ep - 1) / denom1) * plotWidth1);
            svg1Builder.append(String.format(
                "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#e2e8f0\" stroke-dasharray=\"3,3\" stroke-width=\"1\" />",
                x, paddingTop1, x, paddingTop1 + plotHeight1
            ));
            svg1Builder.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"11\" fill=\"#64748b\" text-anchor=\"middle\">%d</text>",
                x, paddingTop1 + plotHeight1 + 18, ep
            ));
        }

        svg1Builder.append(String.format(
            "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#334155\" stroke-width=\"2\" />",
            paddingLeft1, paddingTop1, paddingLeft1, paddingTop1 + plotHeight1
        ));
        svg1Builder.append(String.format(
            "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#334155\" stroke-width=\"2\" />",
            paddingLeft1, paddingTop1 + plotHeight1, paddingLeft1 + plotWidth1, paddingTop1 + plotHeight1
        ));

        int targetY1 = paddingTop1 + plotHeight1 - (int) (Math.min(10.0, Math.max(0.0, targetFinishLoss)) / 10.0 * plotHeight1);
        svg1Builder.append(String.format(
            "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#10b981\" stroke-width=\"2\" stroke-dasharray=\"6,4\" />",
            paddingLeft1, targetY1, paddingLeft1 + plotWidth1, targetY1
        ));
        svg1Builder.append(String.format(
            "<rect x=\"%d\" y=\"%d\" width=\"210\" height=\"22\" rx=\"4\" fill=\"#ecfdf5\" stroke=\"#10b981\" stroke-width=\"1\" />",
            paddingLeft1 + plotWidth1 - 215, Math.max(paddingTop1 + 2, targetY1 - 26)
        ));
        svg1Builder.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"11\" font-weight=\"600\" fill=\"#047857\">🏁 Target Finish Loss &lt; %.2f</text>",
            paddingLeft1 + plotWidth1 - 208, Math.max(paddingTop1 + 16, targetY1 - 11), targetFinishLoss
        ));

        String statusTitle1;
        String statusSub1;
        String badgeClass1;

        StringBuilder dataPointsSvg1 = new StringBuilder();

        if (history != null && history.length > 0) {
            double currentLoss = history[history.length - 1];
            int currentEpoch = history.length;
            boolean targetMet = currentLoss <= targetFinishLoss;

            if (targetMet) {
                statusTitle1 = "Target Loss Achieved";
                statusSub1 = String.format("Epoch %d: Loss %.2f (Met Target &lt; %.2f)", currentEpoch, currentLoss, targetFinishLoss);
                badgeClass1 = "badge-success";
            } else {
                statusTitle1 = "Forging In Progress";
                statusSub1 = String.format("Epoch %d / %d: Loss %.2f (Measured Training Loss)", currentEpoch, maxEpochs, currentLoss);
                badgeClass1 = "badge-primary";
            }

            StringBuilder polylinePoints = new StringBuilder();
            int lastX = paddingLeft1;
            int lastY = paddingTop1 + plotHeight1;

            for (int i = 0; i < history.length; i++) {
                int epNum = i + 1;
                double lVal = history[i];
                int px = paddingLeft1 + (int) (((double) (epNum - 1) / denom1) * plotWidth1);
                int py = paddingTop1 + plotHeight1 - (int) ((Math.min(10.0, Math.max(0.0, lVal)) / 10.0) * plotHeight1);

                if (i > 0) polylinePoints.append(" ");
                polylinePoints.append(px).append(",").append(py);

                dataPointsSvg1.append(String.format(
                    "<circle cx=\"%d\" cy=\"%d\" r=\"4\" fill=\"#ef4444\" stroke=\"#ffffff\" stroke-width=\"1.5\" class=\"dot\" data-tooltip=\"Epoch %d: Measured Loss %.2f\" />",
                    px, py, epNum, lVal
                ));

                lastX = px;
                lastY = py;
            }

            svg1Builder.append(String.format(
                "<polyline points=\"%s\" fill=\"none\" stroke=\"#ef4444\" stroke-width=\"3\" stroke-linejoin=\"round\" stroke-linecap=\"round\" />",
                polylinePoints.toString()
            ));

            svg1Builder.append(String.format(
                "<circle cx=\"%d\" cy=\"%d\" r=\"10\" fill=\"none\" stroke=\"#ef4444\" stroke-width=\"2\" opacity=\"0.6\"><animate attributeName=\"r\" values=\"8;14;8\" dur=\"2s\" repeatCount=\"indefinite\"/><animate attributeName=\"opacity\" values=\"0.8;0.2;0.8\" dur=\"2s\" repeatCount=\"indefinite\"/></circle>",
                lastX, lastY
            ));
            svg1Builder.append(String.format(
                "<circle cx=\"%d\" cy=\"%d\" r=\"5\" fill=\"#ef4444\" stroke=\"#ffffff\" stroke-width=\"2\" />",
                lastX, lastY
            ));

            if (history.length < maxEpochs) {
                StringBuilder projPoints = new StringBuilder();
                int steps = 25;
                double startEp = history.length;
                double startLoss = currentLoss;

                for (int i = 0; i <= steps; i++) {
                    double t = (double) i / steps;
                    double cEp = startEp + t * (maxEpochs - startEp);
                    double cLoss = targetFinishLoss + (startLoss - targetFinishLoss) * Math.exp(-3.0 * t);

                    int cx = paddingLeft1 + (int) (((cEp - 1.0) / denom1) * plotWidth1);
                    int cy = paddingTop1 + plotHeight1 - (int) ((Math.min(10.0, Math.max(0.0, cLoss)) / 10.0) * plotHeight1);

                    if (i > 0) projPoints.append(" ");
                    projPoints.append(cx).append(",").append(cy);
                }

                svg1Builder.append(String.format(
                    "<polyline points=\"%s\" fill=\"none\" stroke=\"#1d5296\" stroke-width=\"2.5\" stroke-dasharray=\"6,4\" />",
                    projPoints.toString()
                ));
            }
        } else {
            statusTitle1 = "Projected Loss Trajectory";
            statusSub1 = String.format("Estimated trajectory for %d Epochs | Target Finish Loss &lt; %.2f", maxEpochs, targetFinishLoss);
            badgeClass1 = "badge-neutral";

            double ep1 = 1.0;
            double loss1 = 8.87;
            int x1 = paddingLeft1 + (int) (((ep1 - 1.0) / denom1) * plotWidth1);
            int y1 = paddingTop1 + plotHeight1 - (int) ((loss1 / 10.0) * plotHeight1);

            StringBuilder projPoints = new StringBuilder();
            int steps = 40;

            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double cEp = ep1 + t * (maxEpochs - ep1);
                double cLoss = targetFinishLoss + (loss1 - targetFinishLoss) * Math.exp(-3.5 * t);

                int cx = paddingLeft1 + (int) (((cEp - 1.0) / denom1) * plotWidth1);
                int cy = paddingTop1 + plotHeight1 - (int) ((Math.min(10.0, Math.max(0.0, cLoss)) / 10.0) * plotHeight1);

                if (i > 0) projPoints.append(" ");
                projPoints.append(cx).append(",").append(cy);
            }

            svg1Builder.append(String.format(
                "<polyline points=\"%s\" fill=\"none\" stroke=\"#1d5296\" stroke-width=\"2.5\" stroke-dasharray=\"6,4\" />",
                projPoints.toString()
            ));

            svg1Builder.append(String.format(
                "<circle cx=\"%d\" cy=\"%d\" r=\"6\" fill=\"#1d5296\" stroke=\"#ffffff\" stroke-width=\"2\" />",
                x1, y1
            ));
            svg1Builder.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"11\" font-weight=\"600\" fill=\"#1d5296\">★ Ready (Projected Start)</text>",
                x1 + 12, y1 + 4
            ));

            int x2 = paddingLeft1 + plotWidth1;
            int y2 = paddingTop1 + plotHeight1 - (int) ((Math.min(10.0, Math.max(0.0, targetFinishLoss)) / 10.0) * plotHeight1);
            svg1Builder.append(String.format(
                "<circle cx=\"%d\" cy=\"%d\" r=\"5\" fill=\"#10b981\" stroke=\"#ffffff\" stroke-width=\"2\" />",
                x2, y2
            ));
        }

        svg1Builder.append(dataPointsSvg1);

        // --- GRAPH 2: TRAINING BUDGET & MODEL CAPACITY GRAPH ---
        int width2 = 720;
        int height2 = 320;
        int paddingLeft2 = 60;
        int paddingRight2 = 35;
        int paddingTop2 = 40;
        int paddingBottom2 = 50;

        int plotWidth2 = width2 - paddingLeft2 - paddingRight2;
        int plotHeight2 = height2 - paddingTop2 - paddingBottom2;

        StringBuilder svg2Builder = new StringBuilder();

        // Model sizes comparison: NANO, MICRO, MINI, SMALL, MEDIUM, LARGE, XLARGE
        ModelSizePreset.Size[] allPresetSizes = ModelSizePreset.Size.values();
        double[] modelEstHours = new double[allPresetSizes.length];
        double maxEstHours = budgetHours * 1.25;

        for (int i = 0; i < allPresetSizes.length; i++) {
            String szName = allPresetSizes[i].name();
            double sec = calculateEstimatedForgingSeconds(szName, selectedEpochs, totalDataBytes);
            double hrs = sec / 3600.0;
            modelEstHours[i] = hrs;
            if (hrs > maxEstHours) {
                maxEstHours = hrs;
            }
        }
        if (maxEstHours <= 0.0) maxEstHours = 24.0;

        // Y-Axis grid lines (Hours 0 to maxEstHours)
        for (int step = 0; step <= 4; step++) {
            double hrsVal = (maxEstHours / 4.0) * step;
            int y = paddingTop2 + plotHeight2 - (int) ((hrsVal / maxEstHours) * plotHeight2);
            svg2Builder.append(String.format(
                "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#e2e8f0\" stroke-dasharray=\"3,3\" stroke-width=\"1\" />",
                paddingLeft2, y, paddingLeft2 + plotWidth2, y
            ));
            svg2Builder.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"11\" fill=\"#64748b\" text-anchor=\"end\">%.1fh</text>",
                paddingLeft2 - 8, y + 4, hrsVal
            ));
        }

        // Target Duration Budget Reference Line
        int budgetY = paddingTop2 + plotHeight2 - (int) (Math.min(maxEstHours, Math.max(0.0, budgetHours)) / maxEstHours * plotHeight2);
        svg2Builder.append(String.format(
            "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#f59e0b\" stroke-width=\"2\" stroke-dasharray=\"6,4\" />",
            paddingLeft2, budgetY, paddingLeft2 + plotWidth2, budgetY
        ));
        svg2Builder.append(String.format(
            "<rect x=\"%d\" y=\"%d\" width=\"180\" height=\"20\" rx=\"4\" fill=\"#fef3c7\" stroke=\"#f59e0b\" stroke-width=\"1\" />",
            paddingLeft2 + 10, Math.max(paddingTop2 + 2, budgetY - 24)
        ));
        svg2Builder.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"11\" font-weight=\"600\" fill=\"#b45309\">⏱ Target Budget: %.1f hrs</text>",
            paddingLeft2 + 16, Math.max(paddingTop2 + 15, budgetY - 10), budgetHours
        ));

        // X-Axis Columns for Model Sizes
        int barCount = allPresetSizes.length;
        int barGroupWidth = plotWidth2 / barCount;
        int barWidth = Math.max(12, barGroupWidth - 20);

        for (int i = 0; i < barCount; i++) {
            ModelSizePreset.Size sz = allPresetSizes[i];
            String szName = sz.name();
            double hrs = modelEstHours[i];
            boolean isSelected = szName.equalsIgnoreCase(selectedModelSize) || sz.getDisplayName().equalsIgnoreCase(selectedModelSize);

            int centerX = paddingLeft2 + i * barGroupWidth + barGroupWidth / 2;
            int barX = centerX - barWidth / 2;
            int barHeight = (int) ((hrs / maxEstHours) * plotHeight2);
            int barY = paddingTop2 + plotHeight2 - barHeight;

            String fillColor = isSelected ? (hrs <= budgetHours ? "#10b981" : "#ef4444") : "#94a3b8";
            String strokeColor = isSelected ? "#0f172a" : "#64748b";

            // Draw Bar
            svg2Builder.append(String.format(
                "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"4\" fill=\"%s\" stroke=\"%s\" stroke-width=\"%s\" class=\"dot\" data-tooltip=\"Model: %s | Est. Duration: %s | Params: %.1fM\" />",
                barX, barY, barWidth, Math.max(2, barHeight), fillColor, strokeColor, isSelected ? "2" : "1",
                sz.getDisplayName(), formatDuration(hrs * 3600.0), sz.getParamCount() / 1_000_000.0
            ));

            // Highlight Marker for Selected Model Size
            if (isSelected) {
                svg2Builder.append(String.format(
                    "<text x=\"%d\" y=\"%d\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"11\" font-weight=\"700\" fill=\"%s\" text-anchor=\"middle\">★ SELECTED</text>",
                    centerX, Math.max(paddingTop2 + 12, barY - 8), hrs <= budgetHours ? "#047857" : "#b91c1c"
                ));
            }

            // X Label
            svg2Builder.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"10\" font-weight=\"%s\" fill=\"#334155\" text-anchor=\"middle\">%s</text>",
                centerX, paddingTop2 + plotHeight2 + 18, isSelected ? "700" : "400", sz.getDisplayName()
            ));
        }

        // Axes Lines
        svg2Builder.append(String.format(
            "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#334155\" stroke-width=\"2\" />",
            paddingLeft2, paddingTop2, paddingLeft2, paddingTop2 + plotHeight2
        ));
        svg2Builder.append(String.format(
            "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#334155\" stroke-width=\"2\" />",
            paddingLeft2, paddingTop2 + plotHeight2, paddingLeft2 + plotWidth2, paddingTop2 + plotHeight2
        ));

        String statusTitle2 = fitsBudget ? "Selected Configuration Fits Duration Budget" : "Selected Configuration Exceeds Duration Budget";
        String badgeClass2 = fitsBudget ? "badge-success" : "badge-danger";

        return "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "<meta charset=\"UTF-8\">\n" +
            "<style>\n" +
            "  :root {\n" +
            "    --evo-bg: #e8ecef;\n" +
            "    --evo-surface: #ffffff;\n" +
            "    --evo-surface-alt: #f1f3f6;\n" +
            "    --evo-border: #b8c0c8;\n" +
            "    --evo-text: #1a202c;\n" +
            "    --evo-text-muted: #64748b;\n" +
            "    --evo-primary: #1d5296;\n" +
            "    --evo-success: #10b981;\n" +
            "    --evo-danger: #ef4444;\n" +
            "  }\n" +
            "  * { box-sizing: border-box; margin: 0; padding: 0; }\n" +
            "  body {\n" +
            "    font-family: system-ui, -apple-system, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;\n" +
            "    background-color: #f8fafc;\n" +
            "    color: var(--evo-text);\n" +
            "    padding: 12px;\n" +
            "    user-select: none;\n" +
            "    overflow-y: auto;\n" +
            "  }\n" +
            "  .summary-bar {\n" +
            "    display: flex;\n" +
            "    flex-wrap: wrap;\n" +
            "    gap: 12px;\n" +
            "    background: #ffffff;\n" +
            "    border: 1px solid #cbd5e1;\n" +
            "    border-radius: 8px;\n" +
            "    padding: 10px 14px;\n" +
            "    margin-bottom: 12px;\n" +
            "    box-shadow: 0 1px 2px rgba(0,0,0,0.03);\n" +
            "  }\n" +
            "  .summary-item { font-size: 11px; display: flex; align-items: center; gap: 4px; }\n" +
            "  .summary-label { color: #64748b; font-weight: 500; }\n" +
            "  .summary-value { color: #0f172a; font-weight: 700; }\n" +
            "  .card {\n" +
            "    background: #ffffff;\n" +
            "    border: 1px solid #cbd5e1;\n" +
            "    border-radius: 8px;\n" +
            "    box-shadow: 0 1px 3px rgba(0,0,0,0.05);\n" +
            "    padding: 14px;\n" +
            "    margin-bottom: 12px;\n" +
            "    display: flex;\n" +
            "    flex-direction: column;\n" +
            "  }\n" +
            "  .header {\n" +
            "    display: flex;\n" +
            "    justify-content: space-between;\n" +
            "    align-items: center;\n" +
            "    margin-bottom: 8px;\n" +
            "    padding-bottom: 6px;\n" +
            "    border-bottom: 1px solid #e2e8f0;\n" +
            "  }\n" +
            "  .title-group { display: flex; flex-direction: column; }\n" +
            "  .title { font-size: 13px; font-weight: 700; color: #0f172a; }\n" +
            "  .subtitle { font-size: 11px; color: #64748b; margin-top: 1px; }\n" +
            "  .badge {\n" +
            "    padding: 3px 8px;\n" +
            "    border-radius: 12px;\n" +
            "    font-size: 10px;\n" +
            "    font-weight: 600;\n" +
            "    text-transform: uppercase;\n" +
            "    letter-spacing: 0.5px;\n" +
            "  }\n" +
            "  .badge-primary { background: #e0f2fe; color: #0369a1; border: 1px solid #bae6fd; }\n" +
            "  .badge-success { background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0; }\n" +
            "  .badge-danger { background: #fee2e2; color: #b91c1c; border: 1px solid #fecaca; }\n" +
            "  .badge-neutral { background: #f1f5f9; color: #475569; border: 1px solid #e2e8f0; }\n" +
            "  .graph-container {\n" +
            "    position: relative;\n" +
            "    width: 100%;\n" +
            "    display: flex;\n" +
            "    justify-content: center;\n" +
            "    align-items: center;\n" +
            "  }\n" +
            "  svg { width: 100%; height: 280px; overflow: visible; }\n" +
            "  .dot { cursor: pointer; transition: transform 0.15s ease; }\n" +
            "  .dot:hover { opacity: 0.8; transform: scale(1.05); }\n" +
            "  .tooltip {\n" +
            "    position: absolute;\n" +
            "    background: rgba(15, 23, 42, 0.95);\n" +
            "    color: #ffffff;\n" +
            "    padding: 5px 9px;\n" +
            "    border-radius: 4px;\n" +
            "    font-size: 11px;\n" +
            "    font-weight: 500;\n" +
            "    pointer-events: none;\n" +
            "    opacity: 0;\n" +
            "    transition: opacity 0.15s ease;\n" +
            "    white-space: nowrap;\n" +
            "    box-shadow: 0 2px 6px rgba(0,0,0,0.2);\n" +
            "    z-index: 100;\n" +
            "  }\n" +
            "  .legend {\n" +
            "    display: flex;\n" +
            "    gap: 16px;\n" +
            "    justify-content: center;\n" +
            "    margin-top: 6px;\n" +
            "    font-size: 11px;\n" +
            "    color: #475569;\n" +
            "  }\n" +
            "  .legend-item { display: flex; align-items: center; gap: 6px; }\n" +
            "  .legend-line { width: 16px; height: 3px; border-radius: 2px; }\n" +
            "</style>\n" +
            "</head>\n" +
            "<body>\n" +
            configSummaryHtml + "\n" +

            "<!-- CARD 1: LOSS / EPOCH RELATION GRAPH -->\n" +
            "<div class=\"card\">\n" +
            "  <div class=\"header\">\n" +
            "    <div class=\"title-group\">\n" +
            "      <div class=\"title\">" + statusTitle1 + "</div>\n" +
            "      <div class=\"subtitle\">" + statusSub1 + "</div>\n" +
            "    </div>\n" +
            "    <div class=\"badge " + badgeClass1 + "\">GRAPH 1: LOSS TRAJECTORY</div>\n" +
            "  </div>\n" +
            "  <div class=\"graph-container\" id=\"graphContainer1\">\n" +
            "    <div class=\"tooltip\" id=\"tooltip1\"></div>\n" +
            "    <svg viewBox=\"0 0 " + width1 + " " + height1 + "\" preserveAspectRatio=\"xMidYMid meet\">\n" +
            "      <text x=\"20\" y=\"25\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"11\" font-weight=\"700\" fill=\"#334155\">Loss</text>\n" +
            "      <text x=\"" + (width1 / 2) + "\" y=\"" + (height1 - 8) + "\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"11\" font-weight=\"700\" fill=\"#334155\" text-anchor=\"middle\">Epochs</text>\n" +
            "      " + svg1Builder.toString() + "\n" +
            "    </svg>\n" +
            "  </div>\n" +
            "  <div class=\"legend\">\n" +
            "    <div class=\"legend-item\"><div class=\"legend-line\" style=\"background: #ef4444;\"></div>Actual Loss</div>\n" +
            "    <div class=\"legend-item\"><div class=\"legend-line\" style=\"background: #1d5296; border-top: 2px dashed #1d5296;\"></div>Projected Trajectory</div>\n" +
            "    <div class=\"legend-item\"><div class=\"legend-line\" style=\"background: #10b981; border-top: 2px dashed #10b981;\"></div>Target Finish Threshold</div>\n" +
            "  </div>\n" +
            "</div>\n" +

            "<!-- CARD 2: TRAINING BUDGET & MODEL CAPACITY GRAPH -->\n" +
            "<div class=\"card\">\n" +
            "  <div class=\"header\">\n" +
            "    <div class=\"title-group\">\n" +
            "      <div class=\"title\">" + statusTitle2 + "</div>\n" +
            "      <div class=\"subtitle\">Estimated Duration across Model Capacities for " + String.format("%.2f MB Data (%d Epochs)", totalDataBytes / (1024.0 * 1024.0), selectedEpochs) + "</div>\n" +
            "    </div>\n" +
            "    <div class=\"badge " + badgeClass2 + "\">GRAPH 2: CAPACITY BUDGET</div>\n" +
            "  </div>\n" +
            "  <div class=\"graph-container\" id=\"graphContainer2\">\n" +
            "    <div class=\"tooltip\" id=\"tooltip2\"></div>\n" +
            "    <svg viewBox=\"0 0 " + width2 + " " + height2 + "\" preserveAspectRatio=\"xMidYMid meet\">\n" +
            "      <text x=\"18\" y=\"25\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"11\" font-weight=\"700\" fill=\"#334155\">Hours</text>\n" +
            "      <text x=\"" + (width2 / 2) + "\" y=\"" + (height2 - 8) + "\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"11\" font-weight=\"700\" fill=\"#334155\" text-anchor=\"middle\">Model Capacity Preset</text>\n" +
            "      " + svg2Builder.toString() + "\n" +
            "    </svg>\n" +
            "  </div>\n" +
            "  <div class=\"legend\">\n" +
            "    <div class=\"legend-item\"><div class=\"legend-line\" style=\"background: #10b981;\"></div>Fits Budget</div>\n" +
            "    <div class=\"legend-item\"><div class=\"legend-line\" style=\"background: #ef4444;\"></div>Exceeds Budget</div>\n" +
            "    <div class=\"legend-item\"><div class=\"legend-line\" style=\"background: #f59e0b; border-top: 2px dashed #f59e0b;\"></div>Target Duration Budget</div>\n" +
            "  </div>\n" +
            "</div>\n" +

            "<script>\n" +
            "  function setupTooltips(containerId, tooltipId) {\n" +
            "    const container = document.getElementById(containerId);\n" +
            "    const tooltip = document.getElementById(tooltipId);\n" +
            "    if (!container || !tooltip) return;\n" +
            "    container.querySelectorAll('.dot').forEach(dot => {\n" +
            "      dot.addEventListener('mousemove', (e) => {\n" +
            "        const rect = container.getBoundingClientRect();\n" +
            "        tooltip.style.left = (e.clientX - rect.left + 10) + 'px';\n" +
            "        tooltip.style.top = (e.clientY - rect.top - 25) + 'px';\n" +
            "        tooltip.textContent = dot.getAttribute('data-tooltip');\n" +
            "        tooltip.style.opacity = '1';\n" +
            "      });\n" +
            "      dot.addEventListener('mouseleave', () => {\n" +
            "        tooltip.style.opacity = '0';\n" +
            "      });\n" +
            "    });\n" +
            "  }\n" +
            "  setupTooltips('graphContainer1', 'tooltip1');\n" +
            "  setupTooltips('graphContainer2', 'tooltip2');\n" +
            "</script>\n" +
            "</body>\n" +
            "</html>";
    }

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
            updateGraphHtml();
            MessageDialog.openInformation(getShell(), "Smart Data Scaling Applied",
                String.format("Training parameters and dataset sample ratios scaled for target forging time of %.1f hours.\n" +
                    "Updated Epochs: %d\nEstimated Duration: %s",
                    hours, selectedEpochs, formatDuration(getEstimatedForgingSeconds())));
        } else {
            MessageDialog.openWarning(getShell(), "Scaling Warning",
                "Could not apply smart scaling. Ensure checked datasets are valid.");
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
            estTimeLabel.setText(String.format("Data: %.2f MB | Tokens: %.2f M | Model: %s (%.1fM params) | Est. Time: %s",
                bytes / (1024.0 * 1024.0), tokens / 1_000_000.0, selectedModelSize, params / 1_000_000.0, formatDuration(estSec)));
        }
    }

    @Override
    protected void okPressed() {
        if (modelSizeCombo != null && !modelSizeCombo.isDisposed()) {
            int idx = modelSizeCombo.getSelectionIndex();
            if (idx >= 0 && idx < ModelSizePreset.Size.values().length) {
                selectedModelSize = ModelSizePreset.Size.values()[idx].name();
            } else if (!modelSizeCombo.getText().isEmpty()) {
                selectedModelSize = modelSizeCombo.getText();
            }
        }

        if (epochCombo != null && !epochCombo.isDisposed()) {
            try { selectedEpochs = Math.max(1, Integer.parseInt(epochCombo.getText().trim())); } catch (Exception e) { selectedEpochs = 32; }
        }

        if (batchSizeCombo != null && !batchSizeCombo.isDisposed()) {
            try { selectedBatchSize = Math.max(1, Integer.parseInt(batchSizeCombo.getText().trim())); } catch (Exception e) { selectedBatchSize = 16; }
        }

        if (learningRateCombo != null && !learningRateCombo.isDisposed()) {
            try { selectedLearningRate = Math.max(0.00001, Double.parseDouble(learningRateCombo.getText().trim())); } catch (Exception e) { selectedLearningRate = 0.01; }
        }

        if (generationsCombo != null && !generationsCombo.isDisposed()) {
            try { selectedGenerations = Math.max(1, Integer.parseInt(generationsCombo.getText().trim())); } catch (Exception e) { selectedGenerations = 1; }
        }

        if (candidatesCombo != null && !candidatesCombo.isDisposed()) {
            try { selectedCandidates = Math.max(1, Integer.parseInt(candidatesCombo.getText().trim())); } catch (Exception e) { selectedCandidates = 1; }
        }

        if (checkpointFreqCombo != null && !checkpointFreqCombo.isDisposed()) {
            try { selectedCheckpointFreq = Math.max(1, Integer.parseInt(checkpointFreqCombo.getText().trim())); } catch (Exception e) { selectedCheckpointFreq = 5; }
        }

        if (outputModelNameText != null && !outputModelNameText.isDisposed()) {
            selectedOutputModelName = outputModelNameText.getText().trim();
        }

        if (maxDataSizeText != null && !maxDataSizeText.isDisposed()) {
            try { selectedMaxDataSizeMb = Long.parseLong(maxDataSizeText.getText().trim()); } catch (Exception e) { selectedMaxDataSizeMb = 500; }
        }

        if (maxSampleCountText != null && !maxSampleCountText.isDisposed()) {
            try { selectedMaxSampleCount = Long.parseLong(maxSampleCountText.getText().trim()); } catch (Exception e) { selectedMaxSampleCount = -1; }
        }

        if (maxSeqLenText != null && !maxSeqLenText.isDisposed()) {
            try { selectedMaxSeqLen = Math.max(16, Integer.parseInt(maxSeqLenText.getText().trim())); } catch (Exception e) { selectedMaxSeqLen = 1024; }
        }

        if (valRatioText != null && !valRatioText.isDisposed()) {
            try {
                int pct = Integer.parseInt(valRatioText.getText().trim());
                selectedValRatio = Math.max(0.0, Math.min(0.5, pct / 100.0));
            } catch (Exception e) { selectedValRatio = 0.15; }
        }

        if (qualityFilterCheck != null && !qualityFilterCheck.isDisposed()) {
            selectedQualityFilter = qualityFilterCheck.getSelection();
        }

        if (lossThresholdCombo != null && !lossThresholdCombo.isDisposed()) {
            selectedLossThreshold = lossThresholdCombo.getText();
        }

        if (desiredLossCombo != null && !desiredLossCombo.isDisposed()) {
            try {
                selectedDesiredLoss = Double.parseDouble(desiredLossCombo.getText().trim());
            } catch (Exception e) {
                selectedDesiredLoss = 1.0;
            }
        }

        if (datasetsTable != null && !datasetsTable.isDisposed()) {
            for (int i = 0; i < datasetsTable.getItemCount(); i++) {
                TableItem tableItem = datasetsTable.getItem(i);
                if (i < datasetItems.size()) {
                    datasetItems.get(i).setChecked(tableItem.getChecked());
                }
            }
        }

        super.okPressed();
    }

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
        json.put("maxSeqLen", selectedMaxSeqLen);
        json.put("validationRatio", selectedValRatio);
        json.put("filterQuality", selectedQualityFilter);
        json.put("outputModelName", selectedOutputModelName);
        json.put("lossThreshold", selectedLossThreshold);
        json.put("desiredLossThreshold", selectedDesiredLoss);
        json.put("datasets", getDatasetsJson());
        return json;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button finishBtn = createButton(parent, 1001, "🏁 Finish Training", false);
        finishBtn.setToolTipText("Gracefully finish training at current state/epoch and export all model files");
        finishBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleFinishTraining(finishBtn);
            }
        });
        super.createButtonsForButtonBar(parent);
    }

    private void handleFinishTraining(Button btn) {
        try {
            Class<?> managerClass = Class.forName("eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager");
            Object mgr = managerClass.getMethod("getInstance").invoke(null);
            Boolean requested = (Boolean) managerClass.getMethod("requestFinish", String.class).invoke(mgr, (Object) null);

            if (btn != null && !btn.isDisposed()) {
                btn.setText("Finishing Epoch...");
                btn.setEnabled(false);
            }
            if (Boolean.TRUE.equals(requested)) {
                MessageDialog.openInformation(getShell(), "Finishing Training",
                    "Training finish requested!\n\n" +
                    "The trainer will complete its current step/epoch, retain all learned weights, " +
                    "and proceed immediately to compile, save, and export all model files (.evo, GGUF, report).");
            } else {
                MessageDialog.openInformation(getShell(), "No Active Trainer",
                    "No active training run detected in progress.\n" +
                    "When training is running, clicking this button gracefully finishes the current epoch and exports all files.");
            }
        } catch (Exception ex) {
            MessageDialog.openError(getShell(), "Error", "Failed to request finish: " + ex.getMessage());
        }
    }
}
