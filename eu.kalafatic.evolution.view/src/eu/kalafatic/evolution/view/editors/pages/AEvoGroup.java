package eu.kalafatic.evolution.view.editors.pages;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

/**
 * Abstract superclass for UI groups in Evolution Editor pages.
 */
public abstract class AEvoGroup {

    protected Composite group;
    protected MultiPageEditor editor;
    protected Orchestrator orchestrator;
    protected AtomicBoolean refreshPending = new AtomicBoolean(false);

    protected Color lightGreen, lightRed, lightOrange, lightBlue, lightPurple, lightCyan, colorWhite;

    public AEvoGroup(MultiPageEditor editor, Orchestrator orchestrator) {
        this.editor = editor;
        this.orchestrator = orchestrator;
        
        this.lightGreen = editor.getLightGreen();
        this.lightRed = editor.getLightRed();
        this.lightOrange = editor.getLightOrange();
        this.lightBlue = editor.getLightBlue();
        this.lightPurple = editor.getLightPurple();
        this.lightCyan = editor.getLightCyan();
        this.colorWhite = Display.getDefault().getSystemColor(org.eclipse.swt.SWT.COLOR_WHITE);
    }

    /**
     * Standardized thread-safe UI update.
     */
    public final void updateUI() {
        scheduleRefresh();
    }

    /**
     * Coalesces multiple refresh requests.
     */
    public void scheduleRefresh() {
        if (refreshPending.compareAndSet(false, true)) {
            Display.getDefault().asyncExec(() -> {
                refreshPending.set(false);
                if (group != null && !group.isDisposed()) {
                    refreshUI();
                }
            });
        }
    }

    /**
     * Updates the UI components with data from the model.
     * To be implemented by subclasses.
     */
    protected abstract void refreshUI();

    /**
     * Updates the model with data from the UI components.
     */
    public void updateModel() {
        // Default implementation does nothing
    }

    /**
     * Returns the text fields in this group for listener attachment.
     */
    public Text[] getTextFields() {
        return new Text[0];
    }

    /**
     * Returns all input controls (Text, Combo, etc.) in this group for listener attachment.
     */
    public Control[] getControls() {
        Text[] texts = getTextFields();
        Control[] controls = new Control[texts.length];
        System.arraycopy(texts, 0, controls, 0, texts.length);
        return controls;
    }

    /**
     * Returns the main composite of this group.
     */
    public Composite getGroup() {
        return group;
    }

    /**
     * Adds a modify listener to all relevant input controls in this group.
     */
    public void addModifyListener(ModifyListener ml) {
        for (Control c : getControls()) {
            if (c == null || c.isDisposed()) continue;
            if (c instanceof Text) {
                ((Text) c).addModifyListener(ml);
            } else if (c instanceof Combo) {
                ((Combo) c).addModifyListener(ml);
            }
        }
    }

    /**
     * Sets the orchestrator model.
     */
    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    protected void setTextSafe(Text text, String value) {
        if (text == null || text.isDisposed()) return;
        String safeValue = value != null ? value : "";
        if (!text.getText().equals(safeValue)) {
            text.setText(safeValue);
        }
    }

    protected void setTextSafe(Label label, String value) {
        if (label == null || label.isDisposed()) return;
        String safeValue = value != null ? value : "";
        if (!label.getText().equals(safeValue)) {
            label.setText(safeValue);
        }
    }

    protected void setTextSafe(StyledText text, String value) {
        if (text == null || text.isDisposed()) return;
        String safeValue = value != null ? value : "";
        if (!text.getText().equals(safeValue)) {
            text.setText(safeValue);
        }
    }

    protected void setTextSafe(org.eclipse.swt.widgets.Group group, String value) {
        if (group == null || group.isDisposed()) return;
        String safeValue = value != null ? value : "";
        if (!group.getText().equals(safeValue)) {
            group.setText(safeValue);
        }
    }

    protected Color getSafeColor(Color color) {
        if (color == null || color.isDisposed()) return null;
        return color;
    }

    protected void setBackgroundSafe(Control control, Color color) {
        if (control == null || control.isDisposed()) return;
        Color safeColor = getSafeColor(color);
        if (safeColor != null && !safeColor.equals(control.getBackground())) {
            control.setBackground(safeColor);
        }
    }

    protected void setForegroundSafe(Control control, Color color) {
        if (control == null || control.isDisposed()) return;
        Color safeColor = getSafeColor(color);
        if (safeColor != null && !safeColor.equals(control.getForeground())) {
            control.setForeground(safeColor);
        }
    }

    protected void setSelectionSafe(Button button, boolean selected) {
        if (button == null || button.isDisposed()) return;
        if (button.getSelection() != selected) {
            button.setSelection(selected);
        }
    }

    protected void setEnabledSafe(Control control, boolean enabled) {
        if (control == null || control.isDisposed()) return;
        if (control.getEnabled() != enabled) {
            control.setEnabled(enabled);
        }
    }

    protected void setVisibleSafe(Control control, boolean visible) {
        if (control == null || control.isDisposed()) return;
        if (control.getVisible() != visible) {
            control.setVisible(visible);
        }
    }

    protected void setToolTipSafe(Control control, String toolTip) {
        if (control == null || control.isDisposed()) return;
        String safeToolTip = toolTip != null ? toolTip : "";
        if (!safeToolTip.equals(control.getToolTipText())) {
            control.setToolTipText(safeToolTip);
        }
    }

    protected void setLayoutDataSafe(Control control, Object layoutData) {
        if (control == null || control.isDisposed()) return;
        if (layoutData != null && layoutData != control.getLayoutData()) {
            control.setLayoutData(layoutData);
        }
    }

    protected void selectSafe(Combo combo, String value) {
        if (combo == null || combo.isDisposed()) return;
        String safeValue = value != null ? value : "";
        int index = combo.indexOf(safeValue);
        if (index >= 0 && combo.getSelectionIndex() != index) {
            combo.select(index);
        } else if (index < 0 && !combo.getText().equals(safeValue)) {
            combo.setText(safeValue);
        }
    }

    protected void setTextSafe(Button button, String value) {
        if (button == null || button.isDisposed()) return;
        String safeValue = value != null ? value : "";
        if (!button.getText().equals(safeValue)) {
            button.setText(safeValue);
        }
    }

    /**
     * Disposes of any resources held by this group.
     */
    public void dispose() {
        // Default implementation does nothing
    }
}
