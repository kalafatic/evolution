package eu.kalafatic.utils.factories;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class CorePerspectiveFactory implements IPerspectiveFactory {

	public static final String ID = "eu.kalafatic.utils.eu.kalafatic.utils.CorePerspectiveFactory";

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveFactory#createInitialLayout(org.eclipse.ui .IPageLayout)
	 */
	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(false);

		IFolderLayout bottomLeft = layout.createFolder("BOTTOM_LEFT", IPageLayout.BOTTOM, 0.65f, "TOP_LEFT");

		bottomLeft.addView(IPageLayout.ID_PROP_SHEET);
		bottomLeft.addView(IPageLayout.ID_OUTLINE);

		addActionSets(layout);
		addViewShortcuts(layout);
		addNewWizardShortcuts(layout);

	}

	// ---------------------------------------------------------------

	/**
	 * Adds the action sets.
	 * @param layout the layout
	 */
	private void addActionSets(IPageLayout layout) {
		layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);
	}

	// ---------------------------------------------------------------

	/**
	 * Adds the view shortcuts.
	 * @param layout the layout
	 */
	private void addViewShortcuts(IPageLayout layout) {
		// layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
//		layout.addShowViewShortcut(IPageLayout.ID_RES_NAV);
		layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
		layout.addShowViewShortcut(IPageLayout.ID_PROP_SHEET);
		layout.addShowViewShortcut(IPageLayout.ID_PROGRESS_VIEW);
		layout.addShowViewShortcut(IPageLayout.ID_PROJECT_EXPLORER);
	}

	// ---------------------------------------------------------------

	/**
	 * Adds the new wizard shortcuts.
	 * @param layout the layout
	 */
	private void addNewWizardShortcuts(IPageLayout layout) {
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder");
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.file");
	}
}
