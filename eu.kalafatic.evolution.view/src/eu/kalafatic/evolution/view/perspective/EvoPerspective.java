package eu.kalafatic.evolution.view.perspective;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import eu.kalafatic.evolution.view.views.AIOutputView;
import eu.kalafatic.evolution.view.views.InternalBrowserView;
import eu.kalafatic.evolution.view.views.OrchestrationZestView;

/**
 * Default perspective for the AI Evolution Software platform.
 * Organized with Navigator on the left (20%),
 * and the main editor area + graph on the right.
 */
public class EvoPerspective implements IPerspectiveFactory {

    public static final String ID = "eu.kalafatic.evolution.view.evoPerspective";

    @Override
    public void createInitialLayout(IPageLayout layout) {
        String editorArea = layout.getEditorArea();
        layout.setEditorAreaVisible(true);

        // Left column - Navigators (Evo Navigator and Project Explorer) - 20%
        IFolderLayout left = layout.createFolder(EFolder.TOP_LEFT.ID, IPageLayout.LEFT, 0.20f, editorArea);
        left.addView("eu.kalafatic.views.EvoNavigator");
        //left.addView(IPageLayout.ID_PROJECT_EXPLORER);

        // Bottom Area (relative to Editor Area) - Orchestration Graph, AI Output, and Properties - 30% of total height
        IFolderLayout bottom = layout.createFolder(EFolder.BOTTOM_RIGHT.ID, IPageLayout.BOTTOM, 0.30f, editorArea);
        bottom.addView(OrchestrationZestView.ID);
       // bottom.addView(AIOutputView.ID);
       // bottom.addView(IPageLayout.ID_PROP_SHEET);
        bottom.addView(InternalBrowserView.ID);

        // Configure generic workbench features
        addActionSets(layout);
        addViewShortcuts(layout);
        addNewWizardShortcuts(layout);
    }

    /**
     * Adds the action sets to the layout.
     */
    private void addActionSets(IPageLayout layout) {
        layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);
    }

    /**
     * Adds view shortcuts to the "Show View" menu.
     */
    private void addViewShortcuts(IPageLayout layout) {
        layout.addShowViewShortcut(IPageLayout.ID_PROJECT_EXPLORER);
        layout.addShowViewShortcut("eu.kalafatic.views.EvoNavigator");
        layout.addShowViewShortcut(AIOutputView.ID);
        layout.addShowViewShortcut(OrchestrationZestView.ID);
        layout.addShowViewShortcut(InternalBrowserView.ID);
        layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
        layout.addShowViewShortcut(IPageLayout.ID_PROP_SHEET);
        layout.addShowViewShortcut(IPageLayout.ID_PROGRESS_VIEW);
    }

    /**
     * Adds new wizard shortcuts to the "New" menu.
     */
    private void addNewWizardShortcuts(IPageLayout layout) {
        layout.addNewWizardShortcut("eu.kalafatic.evolution.view.wizards.NewEvoProjectWizard");
        layout.addNewWizardShortcut("eu.kalafatic.evolution.view.wizards.AddOrchestrationWizard");
        layout.addNewWizardShortcut("eu.kalafatic.evolution.view.wizards.NewEvoTaskWizard");
        layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder");
        layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.file");
    }
}
