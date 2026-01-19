package eu.kalafatic.evolution.view;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class EvoPerspective implements IPerspectiveFactory {

    public static final String ID = "eu.kalafatic.evolution.view.evoPerspective";

    @Override
    public void createInitialLayout(IPageLayout layout) {
        String editorArea = layout.getEditorArea();
        layout.setEditorAreaVisible(true);

        // Top left: Task Tree View
        layout.addStandaloneView(TaskTreeView.ID, true, IPageLayout.LEFT, 0.25f, editorArea);

        // Bottom left: Command Stack View
        layout.addStandaloneView(CommandStackView.ID, true, IPageLayout.BOTTOM, 0.65f, TaskTreeView.ID);

        // Right: AI Output View
        layout.addStandaloneView(AIOutputView.ID, true, IPageLayout.RIGHT, 0.75f, editorArea);
    }
}
