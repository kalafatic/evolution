package eu.kalafatic.evolution.view.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.kalafatic.evolution.controller.manager.ModelSizePreset;
import eu.kalafatic.utils.factories.GUIFactory;

public class ForgeSettingsDialog extends Dialog {

    private Combo modelSizeCombo;
    private Combo epochCombo;
    private Combo lossThresholdCombo;
    private Canvas graphCanvas;

    private String selectedModelSize = "SMALL";
    private int selectedEpochs = 32;
    private String selectedLossThreshold = "Epoch 16-30: Loss 2-5 → Learning phrases";
    private double[] lossHistory = null;

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

    public ForgeSettingsDialog(Shell parentShell, String modelSize, int epochs, String lossThreshold) {
        this(parentShell, modelSize, epochs, lossThreshold, null);
    }

    public ForgeSettingsDialog(Shell parentShell, String modelSize, int epochs, String lossThreshold, double[] lossHistory) {
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
        this.lossHistory = lossHistory;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("FORGE Settings & Loss Progress Graph");
    }

    @Override
    protected Point getInitialSize() {
        return new Point(850, 520);
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

        // 3. Loss Threshold Combo
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

        // Help / Info label
        Label helpLabel = new Label(settingsGroup, SWT.WRAP);
        helpLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        helpLabel.setText(
            "Loss / Stage Guidelines:\n" +
            "• Epoch 1-5: Loss 8-10 → Learning letters (g, u, a)\n" +
            "• Epoch 6-15: Loss 5-8 → Learning words (evolution, genome)\n" +
            "• Epoch 16-30: Loss 2-5 → Learning phrases (\"evolution is\")\n" +
            "• Epoch 31-50: Loss 0.5-2 → Learning sentences\n" +
            "• Epoch 51-64: Loss < 1 → Understanding concepts"
        );

        // Panel 2: Progress Graph Panel
        Composite graphPanel = new Composite(sashForm, SWT.NONE);
        graphPanel.setLayout(new GridLayout(1, true));

        Group graphGroup = new Group(graphPanel, SWT.NONE);
        graphGroup.setText("Loss / Epoch Relation Graph");
        graphGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        graphGroup.setLayout(new GridLayout(1, true));

        graphCanvas = new Canvas(graphGroup, SWT.DOUBLE_BUFFERED | SWT.BORDER);
        graphCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        graphCanvas.setBackground(graphCanvas.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        graphCanvas.addPaintListener(e -> paintLossGraph(e.gc));

        epochCombo.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                if (!graphCanvas.isDisposed()) {
                    graphCanvas.redraw();
                }
            }
        });

        sashForm.setWeights(new int[] { 45, 55 });

        return container;
    }

    private void paintLossGraph(GC gc) {
        Rectangle clientArea = graphCanvas.getClientArea();
        int width = clientArea.width;
        int height = clientArea.height;

        gc.setAntialias(SWT.ON);

        // Background
        gc.setBackground(graphCanvas.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        gc.fillRectangle(0, 0, width, height);

        int paddingLeft = 50;
        int paddingRight = 30;
        int paddingTop = 30;
        int paddingBottom = 50;

        int plotWidth = width - paddingLeft - paddingRight;
        int plotHeight = height - paddingTop - paddingBottom;

        if (plotWidth <= 0 || plotHeight <= 0) return;

        // Colors
        Color grayColor = graphCanvas.getDisplay().getSystemColor(SWT.COLOR_GRAY);
        Color lightGrayColor = graphCanvas.getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
        Color blackColor = graphCanvas.getDisplay().getSystemColor(SWT.COLOR_BLACK);
        Color blueColor = graphCanvas.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE);
        Color redColor = graphCanvas.getDisplay().getSystemColor(SWT.COLOR_DARK_RED);

        // Draw Axes
        gc.setForeground(blackColor);
        gc.setLineWidth(2);

        // Y-axis
        gc.drawLine(paddingLeft, paddingTop, paddingLeft, paddingTop + plotHeight);
        // X-axis
        gc.drawLine(paddingLeft, paddingTop + plotHeight, paddingLeft + plotWidth, paddingTop + plotHeight);

        // Axis Titles
        Font originalFont = gc.getFont();
        Font titleFont = new Font(graphCanvas.getDisplay(), new FontData("Arial", 9, SWT.BOLD));
        gc.setFont(titleFont);
        gc.drawString("Loss", 10, paddingTop - 20, true);
        gc.drawString("Epochs", paddingLeft + plotWidth / 2 - 20, paddingTop + plotHeight + 30, true);

        // Y-axis ticks and grid lines (Loss from 0 to 10)
        gc.setFont(originalFont);
        gc.setLineWidth(1);

        for (int lossVal = 0; lossVal <= 10; lossVal++) {
            int y = paddingTop + plotHeight - (int) ((lossVal / 10.0) * plotHeight);

            gc.setForeground(lightGrayColor);
            gc.setLineStyle(SWT.LINE_DOT);
            gc.drawLine(paddingLeft, y, paddingLeft + plotWidth, y);

            gc.setForeground(blackColor);
            gc.setLineStyle(SWT.LINE_SOLID);
            gc.drawLine(paddingLeft - 4, y, paddingLeft, y);
            gc.drawString(String.valueOf(lossVal), paddingLeft - 25, y - 6, true);
        }

        // X-axis ticks (Epochs: 1, 10, 20, 30, 40, 50, 60, 64 or maxEpochs)
        int maxEpochs = 64;
        try {
            int parsed = Integer.parseInt(epochCombo.getText().trim());
            if (parsed > 0) maxEpochs = parsed;
        } catch (Exception ex) {}

        int denom = Math.max(1, maxEpochs - 1);

        int[] xTicks = new int[] { 1, 10, 20, 30, 40, 50, 60, maxEpochs };
        for (int ep : xTicks) {
            if (ep > maxEpochs) continue;
            int x = paddingLeft + (int) (((double) (ep - 1) / denom) * plotWidth);

            gc.setForeground(lightGrayColor);
            gc.setLineStyle(SWT.LINE_DOT);
            gc.drawLine(x, paddingTop, x, paddingTop + plotHeight);

            gc.setForeground(blackColor);
            gc.setLineStyle(SWT.LINE_SOLID);
            gc.drawLine(x, paddingTop + plotHeight, x, paddingTop + plotHeight + 4);
            gc.drawString(String.valueOf(ep), x - 8, paddingTop + plotHeight + 8, true);
        }

        if (lossHistory != null && lossHistory.length > 0) {
            // Plot actual loss points recorded during training
            gc.setForeground(redColor);
            gc.setLineWidth(2);
            gc.setLineStyle(SWT.LINE_SOLID);

            int lastX = paddingLeft;
            int lastY = paddingTop + plotHeight - (int) ((lossHistory[0] / 10.0) * plotHeight);

            for (int i = 0; i < lossHistory.length; i++) {
                int epNum = i + 1;
                double lVal = lossHistory[i];
                int px = paddingLeft + (int) (((double) (epNum - 1) / denom) * plotWidth);
                int py = paddingTop + plotHeight - (int) ((Math.min(10.0, Math.max(0.0, lVal)) / 10.0) * plotHeight);

                gc.drawLine(lastX, lastY, px, py);
                gc.setBackground(redColor);
                gc.fillOval(px - 3, py - 3, 6, 6);

                lastX = px;
                lastY = py;
            }

            gc.setForeground(blackColor);
            gc.drawString(String.format("● Actual Loss (Epoch %d: %.2f)", lossHistory.length, lossHistory[lossHistory.length - 1]), lastX + 6, lastY - 10, true);

            // Draw projected trajectory for remaining epochs
            if (lossHistory.length < maxEpochs) {
                gc.setForeground(blueColor);
                gc.setLineWidth(2);
                gc.setLineStyle(SWT.LINE_DASH);

                double startEp = lossHistory.length;
                double startLoss = lossHistory[lossHistory.length - 1];
                double targetLoss = 1.0;

                int steps = 30;
                int prevX = lastX;
                int prevY = lastY;

                for (int i = 1; i <= steps; i++) {
                    double t = (double) i / steps;
                    double currentEp = startEp + t * (maxEpochs - startEp);
                    double currentLoss = targetLoss + (startLoss - targetLoss) * Math.exp(-3.0 * t);

                    int cx = paddingLeft + (int) (((currentEp - 1.0) / denom) * plotWidth);
                    int cy = paddingTop + plotHeight - (int) ((Math.min(10.0, Math.max(0.0, currentLoss)) / 10.0) * plotHeight);

                    gc.drawLine(prevX, prevY, cx, cy);
                    prevX = cx;
                    prevY = cy;
                }

                gc.setBackground(blueColor);
                gc.fillOval(prevX - 4, prevY - 4, 8, 8);
                gc.drawString("● (Expected Epoch " + maxEpochs + ", Loss ~1.0)", prevX - 180, prevY - 20, true);
            }
        } else {
            // Curve Data Points & Expected Trajectory
            // Point 1: (Epoch 2, Loss 8.87)
            double ep1 = 2.0;
            double loss1 = 8.87;
            int x1 = paddingLeft + (int) (((ep1 - 1.0) / denom) * plotWidth);
            int y1 = paddingTop + plotHeight - (int) ((loss1 / 10.0) * plotHeight);

            // Target Point: (Epoch maxEpochs, Loss ~1.0)
            double ep2 = maxEpochs;
            double loss2 = 1.0;
            int x2 = paddingLeft + (int) (((ep2 - 1.0) / denom) * plotWidth);
            int y2 = paddingTop + plotHeight - (int) ((loss2 / 10.0) * plotHeight);

            // Draw trajectory curve
            gc.setForeground(blueColor);
            gc.setLineWidth(2);
            gc.setLineStyle(SWT.LINE_SOLID);

            int prevX = x1;
            int prevY = y1;
            int steps = 50;

            for (int i = 1; i <= steps; i++) {
                double t = (double) i / steps;
                double currentEp = ep1 + t * (ep2 - ep1);

                // Exponential decay formula matching loss trajectory: loss = 8.87 * e^(-k * t)
                double currentLoss = loss2 + (loss1 - loss2) * Math.exp(-3.5 * t);

                int cx = paddingLeft + (int) (((currentEp - 1.0) / denom) * plotWidth);
                int cy = paddingTop + plotHeight - (int) ((currentLoss / 10.0) * plotHeight);

                gc.drawLine(prevX, prevY, cx, cy);
                prevX = cx;
                prevY = cy;
            }

            // Draw Data Points
            // Point 1 Dot
            gc.setBackground(redColor);
            gc.fillOval(x1 - 4, y1 - 4, 8, 8);
            gc.setForeground(blackColor);
            gc.drawString("● (Epoch 2, Loss 8.87)", x1 + 8, y1 - 10, true);

            // Point 2 Dot
            gc.setBackground(blueColor);
            gc.fillOval(x2 - 4, y2 - 4, 8, 8);
            gc.drawString("● (Expected Epoch " + maxEpochs + ", Loss ~1.0)", x2 - 190, y2 - 20, true);

            // Draw "(Expected)" labels along trajectory
            gc.setFont(originalFont);
            gc.setForeground(grayColor);
            int expX1 = paddingLeft + (int) ((0.35) * plotWidth);
            int expY1 = paddingTop + plotHeight - (int) ((4.5 / 10.0) * plotHeight);
            gc.drawString("(Expected)", expX1, expY1, true);

            int expX2 = paddingLeft + (int) ((0.6) * plotWidth);
            int expY2 = paddingTop + plotHeight - (int) ((2.5 / 10.0) * plotHeight);
            gc.drawString("(Expected)", expX2, expY2, true);
        }

        titleFont.dispose();
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
}
