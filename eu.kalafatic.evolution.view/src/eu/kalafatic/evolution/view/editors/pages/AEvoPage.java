package eu.kalafatic.evolution.view.editors.pages;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

/**
 * Abstract superclass for Evolution Editor pages.
 */
public abstract class AEvoPage extends SharedScrolledComposite {

    protected MultiPageEditor editor;
    protected Orchestrator orchestrator;
    protected FormToolkit toolkit;
    protected AtomicBoolean refreshPending = new AtomicBoolean(false);
    protected boolean needsRefresh = false;

    public AEvoPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        this.editor = editor;
        this.orchestrator = orchestrator;
        this.toolkit = new FormToolkit(parent.getDisplay());

        setExpandHorizontal(true);
        setExpandVertical(true);
    }

    /**
     * Coalesces multiple refresh requests.
     */
    public void scheduleRefresh() {
        needsRefresh = true;
        if (refreshPending.compareAndSet(false, true)) {
            Display.getDefault().asyncExec(() -> {
                try {
                    refreshPending.set(false);
                    if (!isDisposed() && isVisible()) {
                        needsRefresh = false;
                        refreshUI();
                    }
                } catch (Exception e) {
                    // Log error but don't crash the background process
                    System.err.println("Error during UI refresh in " + getClass().getSimpleName() + ": " + e.getMessage());
                }
            });
        }
    }

    @Override
    public void setVisible(boolean visible) {
        boolean wasVisible = isVisible();
        super.setVisible(visible);
        if (visible && !wasVisible && needsRefresh) {
            scheduleRefresh();
        }
    }

    /**
     * Updates the UI components with data from the model.
     * To be implemented by subclasses.
     */
    protected abstract void refreshUI();

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

    protected void setTextSafe(org.eclipse.swt.widgets.Group group, String value) {
        if (group == null || group.isDisposed()) return;
        String safeValue = value != null ? value : "";
        if (!group.getText().equals(safeValue)) {
            group.setText(safeValue);
        }
    }

    protected void setTextSafe(Button button, String value) {
        if (button == null || button.isDisposed()) return;
        String safeValue = value != null ? value : "";
        if (!button.getText().equals(safeValue)) {
            button.setText(safeValue);
        }
    }

    protected void setBackgroundSafe(Control control, Color color) {
        if (control == null || control.isDisposed()) return;
        if (color != null && !color.equals(control.getBackground())) {
            control.setBackground(color);
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

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        scheduleRefresh();
    }

    @Override
    public void dispose() {
        if (toolkit != null) {
            toolkit.dispose();
        }
        super.dispose();
    }
}
