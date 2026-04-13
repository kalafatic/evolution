package eu.kalafatic.utils.factories;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

public class PerspectiveFactory implements IPerspectiveFactory {

	/** The Constant ID. */
	public static final String ID = "eu.kalafatic.utils.factories.PerspectiveFactory";

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveFactory#createInitialLayout(org.eclipse.ui.IPageLayout)
	 */
	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);

		IFolderLayout topLeft = layout.createFolder("TOP_LEFT", IPageLayout.LEFT, 0.25f, editorArea);

		topLeft.addView("eu.kalafatic.maintain.view.views.ProjectNavigator");

		IFolderLayout bottomLeft = layout.createFolder("BOTTOM_LEFT", IPageLayout.BOTTOM, 0.65f, "TOP_LEFT");

		bottomLeft.addView(IPageLayout.ID_PROP_SHEET);
		bottomLeft.addView(IPageLayout.ID_OUTLINE);

		IFolderLayout bottomRight = layout.createFolder("BOTTOM_RIGHT", IPageLayout.BOTTOM, 0.65f, editorArea);

		// bottomRight.addView(EView.CONSOLE.ID);
		bottomRight.addView(IConsoleConstants.ID_CONSOLE_VIEW);

		addActionSets(layout);
		addViewShortcuts(layout);
		addNewWizardShortcuts(layout);

		// try {
		// File file = new File(FMaintainConstants.MAINTAIN_XML_PATH);
		// if (!file.exists()) {
		// new FileOutputStream(file).write(("<!-- Maintain � -->").getBytes());
		// }
		// Map<?, ?> project = Parser.getInstance(file).parse();
		//
		// IProject iProject = AppUtils.INSTANCE.createProject("Maintain");
		// IFile iFile = iProject.getFile(file.getName());
		// if (!iFile.exists()) {
		// iFile.create(new FileInputStream(file), true, null);
		// }
		// AppUtils.INSTANCE.openEditor("eu.kalafatic.maintain.view.editors.MaintainEditor", file, project);
		//
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

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
		layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
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
