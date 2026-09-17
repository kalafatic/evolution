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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import eu.kalafatic.evolution.forge.data.api.service.DatasetPreparationResult;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.impl.service.DatasetPreparationService;
import eu.kalafatic.utils.factories.GUIFactory;

public class ForgeSettingsDialog extends Dialog {

    public static class DatasetItem {
        private boolean checked;
        private String path;
        private String type; // "FILE" or "FOLDER"

        public DatasetItem(boolean checked, String path, String type) {
            this.checked = checked;
            this.path = path != null ? path : "";
            this.type = type != null ? type : "FOLDER";
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

        public JSONObject toJsonObject() {
            JSONObject obj = new JSONObject();
            obj.put("checked", checked);
            obj.put("path", path);
            obj.put("type", type);
            return obj;
        }

        public static DatasetItem fromJsonObject(JSONObject obj) {
            boolean checked = obj.optBoolean("checked", true);
            String path = obj.optString("path", "");
            String type = obj.optString("type", "FOLDER");
            return new DatasetItem(checked, path, type);
        }
    }

    private Combo modelSizeCombo;
    private Combo epochCombo;
    private Combo lossThresholdCombo;
    private Combo desiredLossCombo;
    private Browser graphBrowser;
    private Table datasetsTable;

    private String selectedModelSize = "SMALL";
    private int selectedEpochs = 32;
    private String selectedLossThreshold = "Epoch 16-30: Loss 2-5 → Learning phrases";
    private double selectedDesiredLoss = 1.0;
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
        return new Point(980, 700);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, true));

        SashForm sashForm = new SashForm(container, SWT.HORIZONTAL | SWT.SMOOTH);
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

        // Panel 1: Settings Panel
        Composite settingsPanel = new Composite(sashForm, SWT.NONE);
        settingsPanel.setLayout(new GridLayout(2, false));

        Group settingsGroup = new Group(settingsPanel, SWT.NONE);
        settingsGroup.setText("Forge Model & Training Configuration");
        settingsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        settingsGroup.setLayout(new GridLayout(2, false));

        // 1. Preset Model Sizes Combo
        GUIFactory.INSTANCE.createLabel(settingsGroup, "Preset Model Size:");
        modelSizeCombo = GUIFactory.INSTANCE.createCombo(settingsGroup);
        modelSizeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        for (ModelSizePreset.Size size : ModelSizePreset.Size.values()) {
            modelSizeCombo.add(size.getDisplayName());
        }

        // Select initial model size
        int defaultSizeIdx = 3; // SMALL by default
        for (int i = 0; i < ModelSizePreset.Size.values().length; i++) {
            if (ModelSizePreset.Size.values()[i].name().equalsIgnoreCase(selectedModelSize) ||
                ModelSizePreset.Size.values()[i].getDisplayName().equalsIgnoreCase(selectedModelSize)) {
                defaultSizeIdx = i;
                break;
            }
        }
        modelSizeCombo.select(defaultSizeIdx);

        // 2. Epoch Integer Editable Combo
        GUIFactory.INSTANCE.createLabel(settingsGroup, "Epoch Count:");
        epochCombo = new Combo(settingsGroup, SWT.DROP_DOWN);
        epochCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        for (String opt : EPOCH_OPTIONS) {
            epochCombo.add(opt);
        }

        int defaultEpochIdx = 4; // "32"
        for (int i = 0; i < EPOCH_OPTIONS.length; i++) {
            if (EPOCH_OPTIONS[i].equals(String.valueOf(selectedEpochs))) {
                defaultEpochIdx = i;
                break;
            }
        }
        epochCombo.select(defaultEpochIdx);
        if (epochCombo.getSelectionIndex() < 0) {
            epochCombo.setText(String.valueOf(selectedEpochs));
        }

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

        // Group 2: Training Datasets (Ordered Target Data)
        Group datasetsGroup = new Group(settingsPanel, SWT.NONE);
        datasetsGroup.setText("Training Datasets (Ordered Target Data)");
        datasetsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        datasetsGroup.setLayout(new GridLayout(2, false));

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

        // Panel 2: Progress Graph Panel (HTML Browser)
        Composite graphPanel = new Composite(sashForm, SWT.NONE);
        graphPanel.setLayout(new GridLayout(1, true));

        Group graphGroup = new Group(graphPanel, SWT.NONE);
        graphGroup.setText("Loss / Epoch Relation Graph");
        graphGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        graphGroup.setLayout(new GridLayout(1, true));

        graphBrowser = new Browser(graphGroup, SWT.NONE);
        graphBrowser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        SelectionAdapter redrawAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateDesiredLossFromCombo();
                updateGraphHtml();
            }
        };

        epochCombo.addSelectionListener(redrawAdapter);
        desiredLossCombo.addSelectionListener(redrawAdapter);
        desiredLossCombo.addModifyListener(e -> {
            updateDesiredLossFromCombo();
            updateGraphHtml();
        });

        sashForm.setWeights(new int[] { 45, 55 });

        // Initial HTML render
        updateGraphHtml();

        return container;
    }

    private void updateDesiredLossFromCombo() {
        if (desiredLossCombo != null && !desiredLossCombo.isDisposed()) {
            try {
                double parsed = Double.parseDouble(desiredLossCombo.getText().trim());
                if (parsed >= 0.0) {
                    selectedDesiredLoss = parsed;
                }
            } catch (Exception ex) {}
        }
    }

    private void updateGraphHtml() {
        if (graphBrowser != null && !graphBrowser.isDisposed()) {
            int maxEpochs = 64;
            try {
                int parsed = Integer.parseInt(epochCombo.getText().trim());
                if (parsed > 0) maxEpochs = parsed;
            } catch (Exception ex) {}

            double targetLoss = selectedDesiredLoss;
            String html = generateGraphHtml(maxEpochs, targetLoss, lossHistory);
            graphBrowser.setText(html);
        }
    }

    private String generateGraphHtml(int maxEpochs, double targetFinishLoss, double[] history) {
        int width = 720;
        int height = 440;
        int paddingLeft = 60;
        int paddingRight = 35;
        int paddingTop = 50;
        int paddingBottom = 60;

        int plotWidth = width - paddingLeft - paddingRight;
        int plotHeight = height - paddingTop - paddingBottom;
        int denom = Math.max(1, maxEpochs - 1);

        StringBuilder svgBuilder = new StringBuilder();

        // 1. Grid Lines and Y-Axis Ticks (Loss 0 to 10)
        for (int lossVal = 0; lossVal <= 10; lossVal += 2) {
            int y = paddingTop + plotHeight - (int) ((lossVal / 10.0) * plotHeight);
            svgBuilder.append(String.format(
                "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#e2e8f0\" stroke-dasharray=\"3,3\" stroke-width=\"1\" />",
                paddingLeft, y, paddingLeft + plotWidth, y
            ));
            svgBuilder.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"11\" fill=\"#64748b\" text-anchor=\"end\">%d</text>",
                paddingLeft - 10, y + 4, lossVal
            ));
        }

        // 2. X-Axis Ticks (Epochs)
        int[] xTicks = new int[] { 1, Math.max(2, maxEpochs / 4), Math.max(3, maxEpochs / 2), Math.max(4, (3 * maxEpochs) / 4), maxEpochs };
        for (int ep : xTicks) {
            if (ep > maxEpochs) continue;
            int x = paddingLeft + (int) (((double) (ep - 1) / denom) * plotWidth);
            svgBuilder.append(String.format(
                "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#e2e8f0\" stroke-dasharray=\"3,3\" stroke-width=\"1\" />",
                x, paddingTop, x, paddingTop + plotHeight
            ));
            svgBuilder.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"11\" fill=\"#64748b\" text-anchor=\"middle\">%d</text>",
                x, paddingTop + plotHeight + 20, ep
            ));
        }

        // 3. Axes Lines
        svgBuilder.append(String.format(
            "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#334155\" stroke-width=\"2\" />",
            paddingLeft, paddingTop, paddingLeft, paddingTop + plotHeight
        ));
        svgBuilder.append(String.format(
            "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#334155\" stroke-width=\"2\" />",
            paddingLeft, paddingTop + plotHeight, paddingLeft + plotWidth, paddingTop + plotHeight
        ));

        // 4. Target Finish Line
        int targetY = paddingTop + plotHeight - (int) (Math.min(10.0, Math.max(0.0, targetFinishLoss)) / 10.0 * plotHeight);
        svgBuilder.append(String.format(
            "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#10b981\" stroke-width=\"2\" stroke-dasharray=\"6,4\" />",
            paddingLeft, targetY, paddingLeft + plotWidth, targetY
        ));
        svgBuilder.append(String.format(
            "<rect x=\"%d\" y=\"%d\" width=\"210\" height=\"22\" rx=\"4\" fill=\"#ecfdf5\" stroke=\"#10b981\" stroke-width=\"1\" />",
            paddingLeft + plotWidth - 215, Math.max(paddingTop + 2, targetY - 26)
        ));
        svgBuilder.append(String.format(
            "<text x=\"%d\" y=\"%d\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"11\" font-weight=\"600\" fill=\"#047857\">🏁 Target Finish Loss &lt; %.2f</text>",
            paddingLeft + plotWidth - 208, Math.max(paddingTop + 16, targetY - 11), targetFinishLoss
        ));

        // Status Card Header Logic
        String statusTitle;
        String statusSub;
        String badgeClass;

        StringBuilder dataPointsSvg = new StringBuilder();

        if (history != null && history.length > 0) {
            double currentLoss = history[history.length - 1];
            int currentEpoch = history.length;
            boolean targetMet = currentLoss <= targetFinishLoss;

            if (targetMet) {
                statusTitle = "Target Loss Achieved";
                statusSub = String.format("Epoch %d: Loss %.2f (Met Target &lt; %.2f)", currentEpoch, currentLoss, targetFinishLoss);
                badgeClass = "badge-success";
            } else {
                statusTitle = "Forging In Progress";
                statusSub = String.format("Epoch %d / %d: Loss %.2f", currentEpoch, maxEpochs, currentLoss);
                badgeClass = "badge-primary";
            }

            // Real Loss Polyline
            StringBuilder polylinePoints = new StringBuilder();
            int lastX = paddingLeft;
            int lastY = paddingTop + plotHeight;

            for (int i = 0; i < history.length; i++) {
                int epNum = i + 1;
                double lVal = history[i];
                int px = paddingLeft + (int) (((double) (epNum - 1) / denom) * plotWidth);
                int py = paddingTop + plotHeight - (int) ((Math.min(10.0, Math.max(0.0, lVal)) / 10.0) * plotHeight);

                if (i > 0) polylinePoints.append(" ");
                polylinePoints.append(px).append(",").append(py);

                // Individual Point Dots
                dataPointsSvg.append(String.format(
                    "<circle cx=\"%d\" cy=\"%d\" r=\"4\" fill=\"#ef4444\" stroke=\"#ffffff\" stroke-width=\"1.5\" class=\"dot\" data-tooltip=\"Epoch %d: Loss %.2f\" />",
                    px, py, epNum, lVal
                ));

                lastX = px;
                lastY = py;
            }

            svgBuilder.append(String.format(
                "<polyline points=\"%s\" fill=\"none\" stroke=\"#ef4444\" stroke-width=\"3\" stroke-linejoin=\"round\" stroke-linecap=\"round\" />",
                polylinePoints.toString()
            ));

            // Current State Halo Marker
            svgBuilder.append(String.format(
                "<circle cx=\"%d\" cy=\"%d\" r=\"10\" fill=\"none\" stroke=\"#ef4444\" stroke-width=\"2\" opacity=\"0.6\"><animate attributeName=\"r\" values=\"8;14;8\" dur=\"2s\" repeatCount=\"indefinite\"/><animate attributeName=\"opacity\" values=\"0.8;0.2;0.8\" dur=\"2s\" repeatCount=\"indefinite\"/></circle>",
                lastX, lastY
            ));
            svgBuilder.append(String.format(
                "<circle cx=\"%d\" cy=\"%d\" r=\"5\" fill=\"#ef4444\" stroke=\"#ffffff\" stroke-width=\"2\" />",
                lastX, lastY
            ));

            // Expected trajectory from current point
            if (history.length < maxEpochs) {
                StringBuilder projPoints = new StringBuilder();
                int steps = 25;
                double startEp = history.length;
                double startLoss = currentLoss;

                for (int i = 0; i <= steps; i++) {
                    double t = (double) i / steps;
                    double cEp = startEp + t * (maxEpochs - startEp);
                    double cLoss = targetFinishLoss + (startLoss - targetFinishLoss) * Math.exp(-3.0 * t);

                    int cx = paddingLeft + (int) (((cEp - 1.0) / denom) * plotWidth);
                    int cy = paddingTop + plotHeight - (int) ((Math.min(10.0, Math.max(0.0, cLoss)) / 10.0) * plotHeight);

                    if (i > 0) projPoints.append(" ");
                    projPoints.append(cx).append(",").append(cy);
                }

                svgBuilder.append(String.format(
                    "<polyline points=\"%s\" fill=\"none\" stroke=\"#1d5296\" stroke-width=\"2.5\" stroke-dasharray=\"6,4\" />",
                    projPoints.toString()
                ));
            }
        } else {
            statusTitle = "Ready to Forge";
            statusSub = String.format("Configured: %d Max Epochs | Target Finish Loss &lt; %.2f", maxEpochs, targetFinishLoss);
            badgeClass = "badge-neutral";

            // Projected Curve from Epoch 1 (Loss ~8.87) to Max Epochs
            double ep1 = 1.0;
            double loss1 = 8.87;
            int x1 = paddingLeft + (int) (((ep1 - 1.0) / denom) * plotWidth);
            int y1 = paddingTop + plotHeight - (int) ((loss1 / 10.0) * plotHeight);

            StringBuilder projPoints = new StringBuilder();
            int steps = 40;

            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double cEp = ep1 + t * (maxEpochs - ep1);
                double cLoss = targetFinishLoss + (loss1 - targetFinishLoss) * Math.exp(-3.5 * t);

                int cx = paddingLeft + (int) (((cEp - 1.0) / denom) * plotWidth);
                int cy = paddingTop + plotHeight - (int) ((Math.min(10.0, Math.max(0.0, cLoss)) / 10.0) * plotHeight);

                if (i > 0) projPoints.append(" ");
                projPoints.append(cx).append(",").append(cy);
            }

            svgBuilder.append(String.format(
                "<polyline points=\"%s\" fill=\"none\" stroke=\"#1d5296\" stroke-width=\"2.5\" stroke-dasharray=\"6,4\" />",
                projPoints.toString()
            ));

            // Initial State Point (Epoch 0 / Ready)
            svgBuilder.append(String.format(
                "<circle cx=\"%d\" cy=\"%d\" r=\"6\" fill=\"#1d5296\" stroke=\"#ffffff\" stroke-width=\"2\" />",
                x1, y1
            ));
            svgBuilder.append(String.format(
                "<text x=\"%d\" y=\"%d\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"11\" font-weight=\"600\" fill=\"#1d5296\">★ Ready (Epoch 0)</text>",
                x1 + 12, y1 + 4
            ));

            // Target Point Dot
            int x2 = paddingLeft + plotWidth;
            int y2 = paddingTop + plotHeight - (int) ((Math.min(10.0, Math.max(0.0, targetFinishLoss)) / 10.0) * plotHeight);
            svgBuilder.append(String.format(
                "<circle cx=\"%d\" cy=\"%d\" r=\"5\" fill=\"#10b981\" stroke=\"#ffffff\" stroke-width=\"2\" />",
                x2, y2
            ));
        }

        // Append Data Point Circles
        svgBuilder.append(dataPointsSvg);

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
            "    overflow: hidden;\n" +
            "  }\n" +
            "  .card {\n" +
            "    background: #ffffff;\n" +
            "    border: 1px solid #cbd5e1;\n" +
            "    border-radius: 8px;\n" +
            "    box-shadow: 0 1px 3px rgba(0,0,0,0.05);\n" +
            "    padding: 14px;\n" +
            "    display: flex;\n" +
            "    flex-direction: column;\n" +
            "    height: 100vh;\n" +
            "  }\n" +
            "  .header {\n" +
            "    display: flex;\n" +
            "    justify-content: space-between;\n" +
            "    align-items: center;\n" +
            "    margin-bottom: 10px;\n" +
            "    padding-bottom: 8px;\n" +
            "    border-bottom: 1px solid #e2e8f0;\n" +
            "  }\n" +
            "  .title-group { display: flex; flex-direction: column; }\n" +
            "  .title { font-size: 14px; font-weight: 700; color: #0f172a; }\n" +
            "  .subtitle { font-size: 12px; color: #64748b; margin-top: 2px; }\n" +
            "  .badge {\n" +
            "    padding: 4px 10px;\n" +
            "    border-radius: 12px;\n" +
            "    font-size: 11px;\n" +
            "    font-weight: 600;\n" +
            "    text-transform: uppercase;\n" +
            "    letter-spacing: 0.5px;\n" +
            "  }\n" +
            "  .badge-primary { background: #e0f2fe; color: #0369a1; border: 1px solid #bae6fd; }\n" +
            "  .badge-success { background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0; }\n" +
            "  .badge-neutral { background: #f1f5f9; color: #475569; border: 1px solid #e2e8f0; }\n" +
            "  .graph-container {\n" +
            "    flex: 1;\n" +
            "    position: relative;\n" +
            "    width: 100%;\n" +
            "    display: flex;\n" +
            "    justify-content: center;\n" +
            "    align-items: center;\n" +
            "  }\n" +
            "  svg { width: 100%; height: 100%; max-height: 380px; overflow: visible; }\n" +
            "  .dot { cursor: pointer; transition: transform 0.15s ease; }\n" +
            "  .dot:hover { transform: scale(1.5); fill: #b91c1c; }\n" +
            "  .tooltip {\n" +
            "    position: absolute;\n" +
            "    background: rgba(15, 23, 42, 0.9);\n" +
            "    color: #ffffff;\n" +
            "    padding: 5px 9px;\n" +
            "    border-radius: 4px;\n" +
            "    font-size: 11px;\n" +
            "    font-weight: 500;\n" +
            "    pointer-events: none;\n" +
            "    opacity: 0;\n" +
            "    transition: opacity 0.15s ease;\n" +
            "    white-space: nowrap;\n" +
            "    box-shadow: 0 2px 6px rgba(0,0,0,0.15);\n" +
            "  }\n" +
            "  .legend {\n" +
            "    display: flex;\n" +
            "    gap: 16px;\n" +
            "    justify-content: center;\n" +
            "    margin-top: 8px;\n" +
            "    font-size: 11px;\n" +
            "    color: #475569;\n" +
            "  }\n" +
            "  .legend-item { display: flex; align-items: center; gap: 6px; }\n" +
            "  .legend-line { width: 18px; height: 3px; border-radius: 2px; }\n" +
            "</style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div class=\"card\">\n" +
            "  <div class=\"header\">\n" +
            "    <div class=\"title-group\">\n" +
            "      <div class=\"title\">" + statusTitle + "</div>\n" +
            "      <div class=\"subtitle\">" + statusSub + "</div>\n" +
            "    </div>\n" +
            "    <div class=\"badge " + badgeClass + "\">EVO FORGE GRAPH</div>\n" +
            "  </div>\n" +
            "  <div class=\"graph-container\" id=\"graphContainer\">\n" +
            "    <div class=\"tooltip\" id=\"tooltip\"></div>\n" +
            "    <svg viewBox=\"0 0 " + width + " " + height + "\" preserveAspectRatio=\"xMidYMid meet\">\n" +
            "      <!-- Y-Axis Title -->\n" +
            "      <text x=\"20\" y=\"30\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"12\" font-weight=\"700\" fill=\"#334155\">Loss</text>\n" +
            "      <!-- X-Axis Title -->\n" +
            "      <text x=\"" + (width / 2) + "\" y=\"" + (height - 12) + "\" font-family=\"system-ui, -apple-system, sans-serif\" font-size=\"12\" font-weight=\"700\" fill=\"#334155\" text-anchor=\"middle\">Epochs</text>\n" +
            "      " + svgBuilder.toString() + "\n" +
            "    </svg>\n" +
            "  </div>\n" +
            "  <div class=\"legend\">\n" +
            "    <div class=\"legend-item\"><div class=\"legend-line\" style=\"background: #ef4444;\"></div>Actual Loss</div>\n" +
            "    <div class=\"legend-item\"><div class=\"legend-line\" style=\"background: #1d5296; border-top: 2px dashed #1d5296;\"></div>Projected Trajectory</div>\n" +
            "    <div class=\"legend-item\"><div class=\"legend-line\" style=\"background: #10b981; border-top: 2px dashed #10b981;\"></div>Target Finish Threshold</div>\n" +
            "  </div>\n" +
            "</div>\n" +
            "<script>\n" +
            "  const tooltip = document.getElementById('tooltip');\n" +
            "  const container = document.getElementById('graphContainer');\n" +
            "  document.querySelectorAll('.dot').forEach(dot => {\n" +
            "    dot.addEventListener('mousemove', (e) => {\n" +
            "      const rect = container.getBoundingClientRect();\n" +
            "      tooltip.style.left = (e.clientX - rect.left + 10) + 'px';\n" +
            "      tooltip.style.top = (e.clientY - rect.top - 25) + 'px';\n" +
            "      tooltip.textContent = dot.getAttribute('data-tooltip');\n" +
            "      tooltip.style.opacity = '1';\n" +
            "    });\n" +
            "    dot.addEventListener('mouseleave', () => {\n" +
            "      tooltip.style.opacity = '0';\n" +
            "    });\n" +
            "  });\n" +
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
            tableItem.setText(1, item.getPath());
            tableItem.setText(2, item.getType());
            tableItem.setData(item);
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
            try {
                selectedEpochs = Integer.parseInt(epochCombo.getText().trim());
            } catch (Exception e) {
                selectedEpochs = 32;
            }
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
}
