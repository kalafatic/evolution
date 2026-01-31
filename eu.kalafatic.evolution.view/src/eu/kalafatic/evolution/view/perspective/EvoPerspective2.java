package eu.kalafatic.evolution.view.perspective;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;


import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.views.AIOutputView;
import eu.kalafatic.evolution.view.views.InternalBrowserView;
import eu.kalafatic.evolution.view.views.OrchestrationZestView;

/**
 * Default perspective for the AI Evolution Software platform. Organized with
 * Navigators on the left, AI Output below them, and the main editor area
 * flanked by the Orchestration Graph and Internal Browser.
 */
public class EvoPerspective2 implements IPerspectiveFactory {

	public static final String ID = "eu.kalafatic.evolution.view.evoPerspective";
	
	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);

		IFolderLayout topLeft = layout.createFolder("TOP_LEFT", IPageLayout.LEFT, 0.25f, editorArea);

		topLeft.addView("eu.kalafatic.maintain.view.views.ProjectNavigator");

		IFolderLayout bottomLeft = layout.createFolder("BOTTOM_LEFT", IPageLayout.BOTTOM, 0.65f, "TOP_LEFT");

		bottomLeft.addView(IPageLayout.ID_PROP_SHEET);
		bottomLeft.addView(IPageLayout.ID_OUTLINE);

		IFolderLayout bottomRight = layout.createFolder("BOTTOM_RIGHT", IPageLayout.TOP, 0.99f, editorArea);
		bottomRight.addView(MultiPageEditor.ID);
		bottomRight.addView(OrchestrationZestView.ID);
		bottomRight.addView(InternalBrowserView.ID);
		
		// bottomRight.addView(EView.CONSOLE.ID);
//		bottomRight.addView(IConsoleConstants.ID_CONSOLE_VIEW);

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

//	@Override
//	public void createInitialLayout(IPageLayout layout) {
//		String editorArea = layout.getEditorArea(); // "editorArea" is the fixed ID of the central editor stack
//
//		// Option 1: Stack your view ON TOP of the editor area (tabs shared with
//		// editors)
//		// This makes your view appear as a tab INSIDE the central editor region
////		layout.addView("com.example.myView", IPageLayout.TOP, // or BOTTOM, LEFT, RIGHT – but TOP/BOTTOM often used for
////																// stacking
////				IPageLayout.RATIO_MAX, // 1.0f to fill the area
////				editorArea); // ← key: relative to editor area
//		
//		// Left column - Top: Navigators (Project Explorer and Evo Navigator)
//		IFolderLayout topLeft = layout.createFolder(EFolder.TOP_LEFT.ID, IPageLayout.LEFT, 0.25f, editorArea);
//		topLeft.addView(IPageLayout.ID_PROJECT_EXPLORER);
//		topLeft.addView("eu.kalafatic.views.EvoNavigator");
//
//		// Option 2: Create a new folder/stack that includes both editor area + views
//		// (more flexible for multiple stacked items including editors)
//		IFolderLayout centralStack = layout.createFolder("central", IPageLayout.TOP, 0.7f, editorArea);
////		centralStack.addView("com.example.previewView"); // your custom view as tab
//		centralStack.addView(MultiPageEditor.ID);
//		centralStack.addView(OrchestrationZestView.ID);
//		centralStack.addView(InternalBrowserView.ID);
//		
//		centralStack.addPlaceholder(IPageLayout.ID_EDITOR_AREA); // keeps editors in same stack
//
//		// Typical surrounding views (not in editor area)
////		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.25f, editorArea);
////		left.addView(IPageLayout.ID_PROJECT_EXPLORER);
////		left.addView(IPageLayout.ID_OUTLINE);
//
//		// Optional: Hide editor area initially if you want views-only in center
//		layout.setEditorAreaVisible(true);
//
//		// Optional: Make views fast-view / minimizable / non-closeable
////		layout.getViewLayout(AIOutputView.ID).setCloseable(false);
//	}

//	@Override
//	public void createInitialLayout(IPageLayout layout) {
//		String editorArea = layout.getEditorArea();
//		layout.setEditorAreaVisible(true);
//
//		// Left column - Top: Navigators (Project Explorer and Evo Navigator)
//		IFolderLayout topLeft = layout.createFolder(EFolder.TOP_LEFT.ID, IPageLayout.LEFT, 0.25f, editorArea);
//		topLeft.addView(IPageLayout.ID_PROJECT_EXPLORER);
//		topLeft.addView("eu.kalafatic.views.EvoNavigator");
//
//		IFolderLayout bottomLeft = layout.createFolder(EFolder.BOTTOM_LEFT.ID, IPageLayout.BOTTOM, 0.65f,
//				EFolder.TOP_LEFT.ID);
//		bottomLeft.addView(AIOutputView.ID);
//
//		IFolderLayout bottomRight = layout.createFolder("bottomRight", IPageLayout.BOTTOM, 0.40f, editorArea);
//		bottomRight.addView(MultiPageEditor.ID);
//		bottomRight.addView(OrchestrationZestView.ID);
//		bottomRight.addView(InternalBrowserView.ID);
//
//	}

//    @Override
//    public void createInitialLayout(IPageLayout layout) {
//        String editorArea = layout.getEditorArea();
//        layout.setEditorAreaVisible(true);
//
//        // Left column - Top: Navigators (Project Explorer and Evo Navigator)
//        IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, 0.22f, editorArea);
//        topLeft.addView(IPageLayout.ID_PROJECT_EXPLORER);
//        topLeft.addView("eu.kalafatic.views.EvoNavigator");
//
//        // Left column - Bottom: AI Output View
//        IFolderLayout bottomLeft = layout.createFolder("bottomLeft", IPageLayout.BOTTOM, 0.40f, "topLeft");
//        bottomLeft.addView(AIOutputView.ID);
//
//        // Main Area - Top: Orchestration Zest View (Graph)
//        layout.addView(OrchestrationZestView.ID, IPageLayout.TOP, 0.45f, editorArea);
//
//        // Main Area - Bottom: Internal Browser View
//        layout.addView(InternalBrowserView.ID, IPageLayout.BOTTOM, 0.25f, editorArea);
//
//        // Configure generic workbench features
//        addActionSets(layout);
//        addViewShortcuts(layout);
//        addNewWizardShortcuts(layout);
//    }

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
