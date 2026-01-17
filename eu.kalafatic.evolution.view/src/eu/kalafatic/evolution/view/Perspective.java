package eu.kalafatic.evolution.view;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class Perspective implements IPerspectiveFactory {

	@Override	
	public void createInitialLayout(IPageLayout layout) {
		layout.addStandaloneView(IPageLayout.ID_PROJECT_EXPLORER, false, IPageLayout.LEFT, 0.25f, layout.getEditorArea());
		layout.addStandaloneView(EcoreZestView.ID, true, IPageLayout.RIGHT, 0.75f, layout.getEditorArea());
	}
}
