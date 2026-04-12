package eu.kalafatic.evolution.view.editors.pages;

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
   
    protected Color lightGreen, lightRed, lightOrange;

    public AEvoGroup(MultiPageEditor editor, Orchestrator orchestrator) {
        this.editor = editor;
        this.orchestrator = orchestrator;
        
        this.lightGreen = new Color(Display.getDefault(), 220, 255, 220);
        this.lightRed = new Color(Display.getDefault(), 255, 220, 220);
        this.lightOrange = new Color(Display.getDefault(), 255, 240, 200);
    }

    /**
     * Standardized thread-safe UI update.
     */
    public final void updateUI() {
        if (Display.getCurrent() != null) {
            if (group != null && !group.isDisposed()) {
                refreshUI();
            }
        } else {
            Display.getDefault().asyncExec(() -> {
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
