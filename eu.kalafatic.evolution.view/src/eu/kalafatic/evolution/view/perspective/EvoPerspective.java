package eu.kalafatic.evolution.view.perspective;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import eu.kalafatic.evolution.view.views.AIOutputView;
import eu.kalafatic.evolution.view.views.InternalBrowserView;
import eu.kalafatic.evolution.view.views.OrchestrationZestView;

/**
 * Default perspective for the AI Evolution Software platform.
 * Organized with Navigators on the left, AI Output below them,
 * and the main editor area flanked by the Orchestration Graph and Internal Browser.
 */
public class EvoPerspective implements IPerspectiveFactory {

    public static final String ID = "eu.kalafatic.evolution.view.evoPerspective";

    @Override
    public void createInitialLayout(IPageLayout layout) {
        String editorArea = layout.getEditorArea();
        layout.setEditorAreaVisible(true);

        // Left column - Top: Navigators (Project Explorer and Evo Navigator)
        IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, 0.22f, editorArea);
        topLeft.addView(IPageLayout.ID_PROJECT_EXPLORER);
        topLeft.addView("eu.kalafatic.views.EvoNavigator");

        // Left column - Bottom: AI Output View
        IFolderLayout bottomLeft = layout.createFolder("bottomLeft", IPageLayout.BOTTOM, 0.40f, "topLeft");
        bottomLeft.addView(AIOutputView.ID);

        // Main Area - Top: Orchestration Zest View (Graph)
        layout.addView(OrchestrationZestView.ID, IPageLayout.TOP, 0.45f, editorArea);

        // Main Area - Bottom: Internal Browser View
        layout.addView(InternalBrowserView.ID, IPageLayout.BOTTOM, 0.25f, editorArea);

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
