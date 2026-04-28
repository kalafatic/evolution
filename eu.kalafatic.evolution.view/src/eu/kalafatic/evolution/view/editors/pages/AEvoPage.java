package eu.kalafatic.evolution.view.editors.pages;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
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
