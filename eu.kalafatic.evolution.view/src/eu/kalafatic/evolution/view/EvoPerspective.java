package eu.kalafatic.evolution.view;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class EvoPerspective implements IPerspectiveFactory {

    public static final String ID = "eu.kalafatic.evolution.view.evoPerspective";

    @Override
    public void createInitialLayout(IPageLayout layout) {
        String editorArea = layout.getEditorArea();
        layout.setEditorAreaVisible(true);

        // Left: Project Explorer
        layout.addView(IPageLayout.ID_PROJECT_EXPLORER, IPageLayout.LEFT, 0.25f, editorArea);

        // Bottom left: Properties View
        layout.addView(IPageLayout.ID_PROP_SHEET, IPageLayout.BOTTOM, 0.75f, IPageLayout.ID_PROJECT_EXPLORER);

        // Right: AI Output View
        layout.addStandaloneView(AIOutputView.ID, true, IPageLayout.RIGHT, 0.75f, editorArea);
    }
}
