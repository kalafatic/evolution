/*******************************************************************************
 * Copyright (c) 2010, Petr Kalafatic (gemini@kalafatic.eu).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU GPL Version 3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * Contributors:
 *     Petr Kalafatic - initial API and implementation
 ******************************************************************************/
package eu.kalafatic.utils.eclipse;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * The Class class RCPUtils.
 *
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
@Deprecated
public class RCPUtils {

	/**
	 * Sets the control enabled recursive.
	 *
	 * @param comp
	 *            the comp
	 * @param enabled
	 *            the enabled
	 */
	public static void setControlEnabledRecursive(Composite comp,
			boolean enabled) {
		comp.setEnabled(enabled);
		Control[] children = comp.getChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof Composite) {
				setControlEnabledRecursive((Composite) children[i], enabled);
			}
		}
	}

	/**
	 * Sets the button selected.
	 *
	 * @param parent
	 *            the parent
	 * @param button
	 *            the button
	 */
	public static void setButtonSelected(Composite parent, Button button) {
		button.setSelection(true);
		Control[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			if (!children[i].equals(button)) {
				if ((((children[i].getStyle() & SWT.TOGGLE) != 0) || ((children[i]
						.getStyle() & SWT.RADIO) != 0))
						&& (children[i] instanceof Button)) {
					((Button) children[i]).setSelection(false);
				}
			}
		}
	}

	/**
	 * Take application screen shot.
	 *
	 * @return the image data
	 */
	public static ImageData takeApplicationScreenShot() {
		Display display = Display.getCurrent();
		if (display == null) {
			throw new IllegalStateException(
					"This method must be called on the SWT UI thread!"); //$NON-NLS-1$
		}

		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;

		// find the area rectangle of the screen that contains all the shells
		boolean hasVisibleShell = false;
		// iterate all shells in order to get the smallest rectangle containing
		// all of them
		for (Shell shell : display.getShells()) {
			if (!shell.isVisible()) {
				continue;
			}
			if (shell.isDisposed()) {
				continue;
			}

			hasVisibleShell = true;
			shell.redraw();
			Rectangle bounds = shell.getBounds();
			minX = Math.min(bounds.x, minX);
			maxX = Math.max(bounds.x + bounds.width, maxX);
			minY = Math.min(bounds.y, minY);
			maxY = Math.max(bounds.y + bounds.height, maxY);
		}

		// if there is no visible shell, take the complete screen
		if (!hasVisibleShell) {
			Rectangle bounds = display.getBounds();
			minX = bounds.x;
			maxX = minX + bounds.width;
			minY = bounds.y;
			maxY = minY + bounds.height;
		}

		// ensure everything has been redrawn
		display.readAndDispatch();

		// Taking the screen shot with AWT is a bit complicated since AWT
		// manages a multi-screen-environment
		// really separated - hence we would need to put our screen shot
		// together ourselves. Thus, we use SWT now.
		// for (GraphicsDevice gd :
		// GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
		// {
		// java.awt.Rectangle rect = gd.getDefaultConfiguration().getBounds();
		// rect.getBounds();
		// }
		// return new Robot().createScreenCapture(new java.awt.Rectangle(minX,
		// minY, maxX - minX, maxY - minY));

		Image image = new Image(display, new Rectangle(minX, minY, maxX - minX,
				maxY - minY));
		try {
			GC gc = new GC(display);
			try {
				gc.copyArea(image, minX, minY);
			} finally {
				gc.dispose();
			}

			return image.getImageData();
		} finally {
			image.dispose();
		}
	}

	/**
	 * Checks if is view visible.
	 *
	 * @param viewID
	 *            the view id
	 * @return true, if is view visible
	 */
	public static boolean isViewVisible(String viewID) {
		// IWorkbenchPage[] pages =
		// Workbench.getInstance().getActiveWorkbenchWindow().getPages();
		IWorkbenchPage[] pages = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getPages();
		for (int i = 0; i < pages.length; i++) {
			IWorkbenchPart part = pages[i].findView(viewID);
			if (part != null) {
				return isPartVisible(part);
			}
		}
		return false;
	}

	/**
	 * Sets the view actions visible.
	 *
	 * @param view
	 *            the view
	 * @param visible
	 *            the visible
	 */
	public static void setViewActionsVisible(IViewPart view, boolean visible) {
		IToolBarManager toolBarManager = view.getViewSite().getActionBars()
				.getToolBarManager();
		IMenuManager menuManager = view.getViewSite().getActionBars()
				.getMenuManager();
		IContributionItem[] tItems = toolBarManager.getItems();
		for (int i = 0; i < tItems.length; i++) {
			tItems[i].setVisible(visible);
			tItems[i].update();
		}
		IContributionItem[] mItems = menuManager.getItems();
		for (int i = 0; i < mItems.length; i++) {
			mItems[i].setVisible(visible);
			mItems[i].update();
		}
		toolBarManager.update(true);
		menuManager.update(true);
	}

	/**
	 * Checks if is part visible.
	 *
	 * @param part
	 *            the part
	 * @return true, if is part visible
	 */
	public static boolean isPartVisible(IWorkbenchPart part) {
		boolean visible = false;
		IWorkbenchPage[] pages = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getPages();
		for (int i = 0; i < pages.length; i++) {
			if (part != null) {
				if (pages[i].isPartVisible(part)) {
					visible = true;
				}
			}
		}
		return visible;
	}

	/**
	 * Show view.
	 *
	 * @param viewID
	 *            the view id
	 * @return the i workbench part
	 */
	public static IWorkbenchPart showView(String viewID) {
		IWorkbenchPage[] pages = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getPages();
		for (int i = 0; i < pages.length; i++) {
			IWorkbenchPart view = null;
			try {
				view = pages[0].showView(viewID);
			} catch (PartInitException e) {
				throw new RuntimeException(e);
			}
			if (view != null) {
				return view;
			}
		}
		return null;
	}

	/**
	 * Activate view.
	 *
	 * @param viewID
	 *            the view id
	 * @return the i workbench part
	 */
	public static IWorkbenchPart activateView(String viewID) {
		IWorkbenchPage[] pages = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getPages();
		for (int i = 0; i < pages.length; i++) {
			IWorkbenchPart view = null;
			try {
				view = pages[0].showView(viewID);
			} catch (PartInitException e) {
				throw new RuntimeException(e);
			}
			if (view != null) {
				pages[0].activate(view);
				return view;
			}
		}
		return null;
	}

	/**
	 * Gets the workbench shell.
	 *
	 * @return the workbench shell
	 */
	@Deprecated
	public static Shell getWorkbenchShell() {
		return getActiveWorkbenchShell();
	}

	/**
	 * Gets the active workbench shell.
	 *
	 * @return the active workbench shell
	 */
	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		return window == null ? null : window.getShell();
	}

	/**
	 * Gets the active shell.
	 *
	 * @return the active shell
	 */
	public static Shell getActiveShell() {
		Shell shell = null;

		if (Display.getCurrent() != null) {
			shell = Display.getCurrent().getActiveShell();
		}

		if (shell == null) {
			shell = Display.getDefault().getActiveShell();
		}

		if (shell == null) {
			shell = getActiveWorkbenchShell();
		}

		return shell;
	}

	/**
	 * Gets the active workbench window.
	 *
	 * @return the active workbench window
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}

	/**
	 * Gets the workbench page.
	 *
	 * @return the workbench page
	 */
	@Deprecated
	public static IWorkbenchPage getWorkbenchPage() {
		return getActiveWorkbenchPage();
	}

	/**
	 * Gets the active workbench page.
	 *
	 * @return the active workbench page
	 */
	public static IWorkbenchPage getActiveWorkbenchPage() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		return window == null ? null : window.getActivePage();
	}

	/**
	 * Gets the active perspective id.
	 *
	 * @return the active perspective id
	 */
	public static String getActivePerspectiveID() {
		IWorkbenchPage page = getActiveWorkbenchPage();
		IPerspectiveDescriptor perspectiveDescriptor = page == null ? null
				: page.getPerspective();
		return perspectiveDescriptor == null ? null : perspectiveDescriptor
				.getId();
	}

	/**
	 * Dispose all children.
	 *
	 * @param comp
	 *            the comp
	 */
	public static void disposeAllChildren(Composite comp) {
		if (comp != null) {
			if (!comp.isDisposed()) {
				Control[] children = comp.getChildren();
				for (int i = 0; i < children.length; i++) {
					Control c = children[i];
					c.dispose();
				}
			}
		}
	}

	/**
	 * Open editor.
	 *
	 * @param input
	 *            the input
	 * @param editorID
	 *            the editor id
	 * @return the i editor part
	 * @throws PartInitException
	 *             the part init exception
	 */
	public static IEditorPart openEditor(IEditorInput input, String editorID)
			throws PartInitException {
		return getActiveWorkbenchPage().openEditor(input, editorID);
	}

	/**
	 * Open editor.
	 *
	 * @param input
	 *            the input
	 * @param editorID
	 *            the editor id
	 * @param activate
	 *            the activate
	 * @return the i editor part
	 * @throws PartInitException
	 *             the part init exception
	 */
	public static IEditorPart openEditor(IEditorInput input, String editorID,
			boolean activate) throws PartInitException {
		return getActiveWorkbenchPage().openEditor(input, editorID, activate);
	}

	/**
	 * Find editor.
	 *
	 * @param input
	 *            the input
	 * @return the i editor part
	 */
	public static IEditorPart findEditor(IEditorInput input) {
		return getActiveWorkbenchPage().findEditor(input);
	}

	/**
	 * Close editor.
	 *
	 * @param input
	 *            the input
	 * @param save
	 *            the save
	 * @return true, if successful
	 */
	public static boolean closeEditor(IEditorInput input, boolean save) {
		IWorkbenchPage page = getActiveWorkbenchPage();
		IEditorPart editor = page.findEditor(input);
		if (editor == null) {
			return true;
		}

		return page.closeEditor(editor, save);
	}

	/**
	 * Find view.
	 *
	 * @param viewID
	 *            the view id
	 * @return the i view part
	 */
	public static IViewPart findView(String viewID) {
		return getActiveWorkbenchPage().findView(viewID);
	}

	/**
	 * Gets the center position.
	 *
	 * @param bounds
	 *            the bounds
	 * @return the center position
	 */
	public static Point getCenterPosition(Rectangle bounds) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x = 0;
		int y = 0;
		if (bounds.width < screenSize.getWidth()) {
			x = (((int) screenSize.getWidth()) - bounds.width) / 2;
		}
		if (bounds.height < screenSize.getHeight()) {
			y = (((int) screenSize.getHeight()) - bounds.height) / 2;
		}

		return new Point(x, y);
	}

	/**
	 * Adds the all perspective shortcuts.
	 *
	 * @param layout
	 *            the layout
	 */
	public static void addAllPerspectiveShortcuts(IPageLayout layout) {
		IPerspectiveDescriptor[] perspectives = PlatformUI.getWorkbench()
				.getPerspectiveRegistry().getPerspectives();
		for (int i = 0; i < perspectives.length; i++) {
			layout.addPerspectiveShortcut(perspectives[i].getId());
			// IConfigurationElement[] configPerspectives =
			// Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.ui.perspectives");
			// for(int i = 0; i < configPerspectives.length; i++)
			// layout.addPerspectiveShortcut(configPerspectives[i].getAttribute("id"));
		}
	}

	/**
	 * Sets the form border.
	 *
	 * @param comp
	 *            the new form border
	 */
	public static void setFormBorder(Composite comp) {
		comp.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
	}

	/**
	 * Center dialog.
	 *
	 * @param d
	 *            the d
	 */
	public static void centerDialog(Dialog d) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Point shellSize = d.getShell().getSize();
		int diffWidth = screenSize.width - shellSize.x;
		int diffHeight = screenSize.height - shellSize.y;
		d.getShell().setLocation(diffWidth / 2, diffHeight / 2);
	}

	/**
	 * Gets the menu item.
	 *
	 * @param id
	 *            the id
	 * @param menuMan
	 *            the menu man
	 * @return the menu item
	 */
	public static IContributionItem getMenuItem(String id, IMenuManager menuMan) {
		if (menuMan != null) {
			IContributionItem[] menuItems = menuMan.getItems();
			for (int i = 0; i < menuItems.length; i++) {
				IContributionItem menuItem = menuItems[i];
				if (menuItem != null && menuItem.getId() != null) {
					if (menuItem.getId().equals(id)) {
						return menuItem;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Search workbench page.
	 *
	 * @return the i workbench page
	 */
	public static IWorkbenchPage searchWorkbenchPage() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window == null) {
			return null;
		}
		IWorkbenchPage[] pages = window.getPages();
		if (pages.length > 0) {
			return pages[0];
		} else {
			return null;
		}
	}

	/**
	 * Search part reference.
	 *
	 * @param part
	 *            the part
	 * @return the i workbench part reference
	 */
	public static IWorkbenchPartReference searchPartReference(
			IWorkbenchPart part) {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window == null) {
			return null;
		}
		IWorkbenchPage[] pages = window.getPages();
		for (int i = 0; i < pages.length; i++) {
			IWorkbenchPartReference ref = pages[i].getReference(part);
			if (ref != null) {
				return ref;
			}
		}
		return null;
	}

	/** The null monitor. */
	private static IProgressMonitor nullMonitor = new NullProgressMonitor();

	/**
	 * Gets the save progress monitor.
	 *
	 * @param monitor
	 *            the monitor
	 * @return the save progress monitor
	 */
	public static IProgressMonitor getSaveProgressMonitor(
			IProgressMonitor monitor) {
		if (monitor != null) {
			return monitor;
		}
		return nullMonitor;
	}

	/**
	 * Checks if is display thread.
	 *
	 * @return true, if is display thread
	 */
	public static boolean isDisplayThread() {
		return Display.getDefault().getThread().equals(Thread.currentThread());
	}

	/**
	 * Gets the resources workspace.
	 *
	 * @return the resources workspace
	 */
	public File getResourcesWorkspace() {
		return new File(ResourcesPlugin.getWorkspace().getRoot().getLocation()
				.toOSString());
	}

	/**
	 * Gets the resource as file.
	 *
	 * @param resource
	 *            the resource
	 * @return the resource as file
	 */
	public static File getResourceAsFile(IResource resource) {
		return new File(resource.getWorkspace().getRoot().getLocation()
				.toFile(), resource.getFullPath().toOSString());
	}

	/**
	 * Sets the control font style.
	 *
	 * @param control
	 *            the control
	 * @param addStyle
	 *            the add style
	 * @param removeStyle
	 *            the remove style
	 */
	public static void setControlFontStyle(Control control, int addStyle,
			int removeStyle) {
		Font oldFont = control.getFont();
		int newStyle = oldFont.getFontData()[0].getStyle() | addStyle;
		newStyle = newStyle & (~removeStyle);
		final Font newFont = new Font(oldFont.getDevice(),
				oldFont.getFontData()[0].getName(),
				oldFont.getFontData()[0].getHeight(), newStyle);
		control.setFont(newFont);
		control.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				newFont.dispose();
			}
		});
	}

	/**
	 * Gets the font height.
	 *
	 * @param control
	 *            the control
	 * @return the font height
	 */
	public static int getFontHeight(Control control) {
		int fontHeight = 0;
		// since the standard font can change - we recalculate this every time
		GC gc = new GC(control.getParent());
		try {
			gc.setFont(control.getFont());
			FontMetrics fontMetrics = gc.getFontMetrics();
			fontHeight = fontMetrics.getHeight();
		} finally {
			gc.dispose();
		}
		return fontHeight;
	}

	/**
	 * Adds the contribution item trim.
	 *
	 * @param shell
	 *            the shell
	 * @param contributionItem
	 *            the contribution item
	 * @param prependTo
	 *            the prepend to
	 */
	@SuppressWarnings("restriction")//$NON-NLS-1$
	public static void addContributionItemTrim(Shell shell,
			IContributionItem contributionItem, String prependTo) {
//		if (shell != null
//				&& (shell.getLayout() instanceof org.eclipse.ui.internal.layout.TrimLayout)) {
//			// This is how the WorkbenchWindow add the progress and heapstatus
//			// controls
//			// can't be that wrong :-)
//			org.eclipse.ui.internal.layout.TrimLayout layout = (org.eclipse.ui.internal.layout.TrimLayout) shell
//					.getLayout();
//			Composite comp = new Composite(shell, SWT.NONE);
//			contributionItem.fill(comp);
//			org.eclipse.ui.internal.WindowTrimProxy trimProxy = new org.eclipse.ui.internal.WindowTrimProxy(
//					comp, contributionItem.getId(), contributionItem.getClass()
//							.getSimpleName(), SWT.BOTTOM | SWT.TOP) {
//
//				@Override
//				public void handleClose() {
//					getControl().dispose();
//				}
//
//				@Override
//				public boolean isCloseable() {
//					return true;
//				}
//			};
//			org.eclipse.ui.internal.layout.IWindowTrim prependTrim = layout
//					.getTrim(prependTo);
//			trimProxy
//					.setWidthHint(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
//			trimProxy
//					.setHeightHint(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
//			layout.addTrim(SWT.BOTTOM, trimProxy, prependTrim);
//
//			comp.setVisible(true);
//		}
	}

	/**
	 * The Class class WorkaroundTableLayout.
	 *
	 * @author Petr Kalafatic
	 * @project Gemini
	 * @version 3.0.0
	 */
	@SuppressWarnings("unused")
	private static class WorkaroundTableLayout extends TableLayout {

		/** The original data. */
		private List<ColumnLayoutData> originalData;

		/** The pixel data. */
		private List<ColumnPixelData> pixelData = null;

		/**
		 * Instantiates a new workaround table layout.
		 *
		 * @param originalData
		 *            the original data
		 */
		public WorkaroundTableLayout(List<ColumnLayoutData> originalData) {
			assert originalData != null;
			this.originalData = originalData;
			this.pixelData = new ArrayList<ColumnPixelData>(originalData.size());
			for (int i = 0; i < originalData.size(); i++) {
				// set min size of 10 pixels per column
				final ColumnPixelData pData = new ColumnPixelData(10);
				pixelData.add(pData);
				addColumnData(pData);
			}
		}

		/**
		 * Gets the original data.
		 *
		 * @return the original data
		 */
		public List<ColumnLayoutData> getOriginalData() {
			return originalData;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.eclipse.jface.viewers.TableLayout#layout(org.eclipse.swt.widgets
		 * .Composite, boolean)
		 */
		@Override
		public void layout(Composite c, boolean flush) {
			// check if there is currently a vertical scroll bar visible in the
			// composite tree where
			// the given tree of table is used in. If so reduce the available
			// width.
			// Note: The weird thing is, that the ScrollBar is always set and
			// the width is always > 0,
			// BUT with this fix, the table width always matches perfectly.
			// (marius)
			final int verticalScrollBarWidth = c.getVerticalBar().getSize().y;
			final int width = c.getClientArea().width - verticalScrollBarWidth;

			if (width > 1) {
				setPixelData(originalData, pixelData, width);
			}
			super.layout(c, flush);
		}
	}

	/**
	 * Sets the pixel data.
	 *
	 * @param layoutData
	 *            the layout data
	 * @param pixelDatas
	 *            the pixel datas
	 * @param clientWidth
	 *            the client width
	 */
	private static void setPixelData(List<ColumnLayoutData> layoutData,
			List<ColumnPixelData> pixelDatas, int clientWidth) {
		int clientRest = clientWidth - 25;
		int weightSum = 0;
		for (int i = 0; i < layoutData.size(); i++) {
			final ColumnLayoutData columnData = layoutData.get(i);
			if (columnData instanceof ColumnPixelData) {
				clientRest -= ((ColumnPixelData) columnData).width;
				pixelDatas.get(i).width = ((ColumnPixelData) columnData).width;
			} else {
				weightSum += ((ColumnWeightData) columnData).weight;
			}
		}
		for (int i = 0; i < layoutData.size(); i++) {
			final ColumnLayoutData columnData = layoutData.get(i);
			final ColumnPixelData pixelData = pixelDatas.get(i);
			if (columnData instanceof ColumnWeightData) {
				pixelData.width = clientRest
						* ((ColumnWeightData) columnData).weight / weightSum;
			}
		}
	}
}