package eu.kalafatic.evolution.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleConstants;

public class Perspective implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);

		layout.addView(IPageLayout.ID_PROJECT_EXPLORER, IPageLayout.LEFT, 0.25f, layout.getEditorArea());

		layout.addStandaloneView(EcoreZestView.ID, true, IPageLayout.RIGHT, 0.75f, layout.getEditorArea());

		IFolderLayout topLeft = layout.createFolder("TOP_LEFT", IPageLayout.LEFT, 0.25f, editorArea);

		// topLeft.addView("eu.kalafatic.maintain.view.views.ProjectNavigator");

		IFolderLayout bottomLeft = layout.createFolder("BOTTOM_LEFT", IPageLayout.BOTTOM, 0.65f, "TOP_LEFT");

		bottomLeft.addView(IPageLayout.ID_PROP_SHEET);
		bottomLeft.addView(PropertiesView.ID);
		bottomLeft.addView(IPageLayout.ID_OUTLINE);

		IFolderLayout bottomRight = layout.createFolder("BOTTOM_RIGHT", IPageLayout.BOTTOM, 0.65f, editorArea);
		
		IWorkbenchPage page = PlatformUI.getWorkbench()
		        .getActiveWorkbenchWindow()
		        .getActivePage();

		//page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
				
		addActionSets(layout);
		addViewShortcuts(layout);
		addNewWizardShortcuts(layout);

	}
	

	
	/**
	 * Adds the action sets.
	 * @param layout the layout
	 */
	private void addActionSets(IPageLayout layout) {
		layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);
	}

	
	/**
	 * Adds the view shortcuts.
	 * @param layout the layout
	 */
	private void addViewShortcuts(IPageLayout layout) {		
		layout.addShowViewShortcut(IPageLayout.ID_PROJECT_EXPLORER);
		layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
		layout.addShowViewShortcut(IPageLayout.ID_PROP_SHEET);
		layout.addShowViewShortcut(IPageLayout.ID_PROGRESS_VIEW);
		layout.addShowViewShortcut(IPageLayout.ID_PROJECT_EXPLORER);
	}

	
	/**
	 * Adds the new wizard shortcuts.
	 * @param layout the layout
	 */
	private void addNewWizardShortcuts(IPageLayout layout) {
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder");
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.file");
	}
}
