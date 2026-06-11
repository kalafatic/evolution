# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.evolution.view/src/eu/kalafatic/evolution/view/perspective/

## Domain: general

## Components
* `EFolder.java`: package eu.kalafatic.evolution.view.perspective; public enum EFolder { TOP_LEFT(0, "TOP_LEFT"), TOP_RIGHT(1, "TOP_RIGHT"), BOTTOM_LEFT(2, "BOTTOM_LEFT"), BOTTOM_RIGHT(3, "BOTTOM_RIGHT"), BOTTOM_BOTTOM_RIGHT(4, "BOTTOM_BOTTOM_RIGHT"), CENTER(5, "CENTER"), LEFT_RIGHT(6, "LEFT_RIGHT"), MULTI_VIEW(7, ":*"); public int index; public String ID;
* `EvoPerspective.java`: package eu.kalafatic.evolution.view.perspective; import org.eclipse.ui.IFolderLayout; import org.eclipse.ui.IPageLayout; import org.eclipse.ui.IPerspectiveFactory; import org.eclipse.ui.console.IConsoleConstants; import eu.kalafatic.evolution.view.views.AIOutputView; import eu.kalafatic.evolution.view.views.InternalBrowserView; import eu.kalafatic.evolution.view.views.OrchestrationZestView; public class EvoPerspective implements IPerspectiveFactory { public static final String ID = "eu.kalafatic.evolution.view.evoPerspective"; @Override public void createInitialLayout(IPageLayout layout) { String editorArea = layout.getEditorArea(); layout.setEditorAreaVisible(true); IFolderLayout left = layout.createFolder(EFolder.TOP_LEFT.ID, IPageLayout.LEFT, 0.20f, editorArea); left.addView("eu.kalafatic.views.EvoNavigator"); IFolderLayout bottomLeft = layout.createFolder(EFolder.BOTTOM_LEFT.ID, IPageLayout.BOTTOM, 0.50f, EFolder.TOP_LEFT.ID); bottomLeft.addView(IConsoleConstants.ID_CONSOLE_VIEW);
