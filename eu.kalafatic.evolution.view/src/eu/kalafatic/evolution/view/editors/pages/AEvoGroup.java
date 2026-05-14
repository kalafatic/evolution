package eu.kalafatic.evolution.view.editors.pages;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
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

    protected Color lightGreen, lightRed, lightOrange, lightBlue, lightPurple, lightCyan;

    public AEvoGroup(MultiPageEditor editor, Orchestrator orchestrator) {
        this.editor = editor;
        this.orchestrator = orchestrator;
        
        this.lightGreen = editor.getLightGreen();
        this.lightRed = editor.getLightRed();
        this.lightOrange = editor.getLightOrange();
        this.lightBlue = editor.getLightBlue();
        this.lightPurple = editor.getLightPurple();
        this.lightCyan = editor.getLightCyan();
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
     * Returns the main composite of this group.
     */
    public Composite getGroup() {
        return group;
    }

    /**
     * Adds a modify listener to all text fields in this group.
     */
    public void addModifyListener(ModifyListener ml) {
        for (Text t : getTextFields()) {
            if (t != null && !t.isDisposed()) {
                t.addModifyListener(ml);
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

    protected void setSelectionSafe(Button button, boolean selected) {
        if (button == null || button.isDisposed()) return;
        if (button.getSelection() != selected) {
            button.setSelection(selected);
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

    /**
     * Disposes of any resources held by this group.
     */
    public void dispose() {
        // Default implementation does nothing
    }
}
