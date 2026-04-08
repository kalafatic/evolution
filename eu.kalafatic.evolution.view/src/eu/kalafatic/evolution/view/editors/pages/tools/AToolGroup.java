package eu.kalafatic.evolution.view.editors.pages.tools;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

/**
 * Abstract superclass for tool-specific UI groups with status feedback.
 */
public abstract class AToolGroup extends AEvoGroup {
    protected Color successColor;

    public AToolGroup(MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        super(editor, orchestrator);
        this.successColor = successColor;
    }

    /**
     * Updates the group background color based on the test status.
     */
    public void updateGroupStatus() {
        if (group == null || group.isDisposed()) return;

        String status = getTestStatus();
        if ("SUCCESS".equals(status)) {
            group.setBackground(successColor);
        } else if ("FAILED".equals(status)) {
            group.setBackground(group.getDisplay().getSystemColor(SWT.COLOR_RED));
        } else {
            group.setBackground(group.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        }
    }

    /**
     * Resets the test status in the model and updates the UI.
     */
    public void resetStatus() {
        clearTestStatus();
        updateGroupStatus();
    }

    /**
     * Gets the test status from the model.
     */
    protected abstract String getTestStatus();

    /**
     * Clears the test status in the model.
     */
    protected abstract void clearTestStatus();
}
