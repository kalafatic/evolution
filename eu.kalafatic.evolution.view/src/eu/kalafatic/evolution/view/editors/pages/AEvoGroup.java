package eu.kalafatic.evolution.view.editors.pages;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
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
}
