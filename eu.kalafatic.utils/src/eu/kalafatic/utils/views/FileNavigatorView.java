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
package eu.kalafatic.utils.views;

import java.io.File;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.OpenFileAction;

import eu.kalafatic.utils.constants.FCoreImageConstants;
import eu.kalafatic.utils.hack.OpenWithMenu;
import eu.kalafatic.utils.interfaces.ATreeViewer;
import eu.kalafatic.utils.providers.FileTreeContentProvider;
import eu.kalafatic.utils.providers.FileTreeLabelProvider;

/**
 * The Class class FileNavigatorView.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
// @SuppressWarnings("unchecked")
public class FileNavigatorView extends ATreeViewer {

	/** The Constant ID. */
	public static final String ID = "eu.kalafatic.gemini.core.views.FileNavigatorView";

	/**
	 * Instantiates a new file navigator view.
	 */
	public FileNavigatorView() {
		setTitleImage(FCoreImageConstants.TREE_IMG);
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	// @Override
	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.ATreeViewer#createPartControl(org .eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		((TreeViewer) viewer).setContentProvider(new FileTreeContentProvider());
		((TreeViewer) viewer).setLabelProvider(new FileTreeLabelProvider());

		viewer.setInput(File.listRoots());
		init();
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.ATreeViewer#fillLocalPullDown(org .eclipse.jface.action.IMenuManager)
	 */
	@Override
	public void fillLocalPullDown(IMenuManager manager) {
		super.fillLocalPullDown(manager);
		manager.add(new Separator());
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.ATreeViewer#fillContextMenu(org.eclipse .jface.action.IMenuManager)
	 */
	@Override
	public void fillContextMenu(IMenuManager menuManager) {
		try {
			super.fillContextMenu(menuManager);
			menuManager.add(new Separator());
			drillDownAdapter.addNavigationActions(menuManager);
			// Other plug-ins can contribute there actions here
			menuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

			addSubmenu(menuManager);
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Adds the submenu.
	 * @param menuManager the menu manager
	 * @throws PartInitException the part init exception
	 */
	void addSubmenu(IMenuManager menuManager) throws PartInitException {
		// Add 'Open' sub-menu
		menuManager.add(new Separator());
		menuManager.add(new OpenFileAction(getSite().getPage()));
		menuManager.add(new Separator());

		IStructuredSelection s = (IStructuredSelection) viewer.getSelection();
		File file = (File) s.getFirstElement();

		// Add 'Open With...' sub-menu
		MenuManager subMenu = new MenuManager("Open &With");
		OpenWithMenu openWithMenu = new OpenWithMenu(getSite().getPage(), file);
		subMenu.add(openWithMenu);

		menuManager.add(subMenu);
		menuManager.add(new Separator());

	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.AViewer#initListeners()
	 */
	@Override
	public void initListeners() {
		// viewer.addOpenListener(new IOpenListener() {
		// @Override
		// public void open(OpenEvent event) {
		// IStructuredSelection selection = (IStructuredSelection) event
		// .getSelection();
		//
		// File file = (File) selection.getFirstElement();
		// if (Desktop.isDesktopSupported()) {
		// Desktop desktop = Desktop.getDesktop();
		// if (desktop.isSupported(Desktop.Action.OPEN)) {
		// try {
		// desktop.open(file);
		// } catch (IOException e) {
		// // DO NOTHING
		// }
		// }
		// }
		// }
		// });
		((TreeViewer) viewer).addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object firstElement = ((TreeSelection) event.getSelection()).getFirstElement();

				if (firstElement instanceof File) {
					File file = (File) firstElement;

					if (file.isFile()) {

						// AppUtils.openEditor(getSite().getPage(), MultiPageEditor.ID, file.getAbsolutePath());
					}
				}
			}
		});
	}
}