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
package eu.kalafatic.utils.interfaces;

import java.util.Collection;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.ISaveablesLifecycleListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.SaveablesLifecycleEvent;
import org.eclipse.ui.part.ViewPart;

import eu.kalafatic.utils.dnd.DragProvider;

/**
 * The Class class AViewer.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public abstract class AViewer extends ViewPart implements IViewer {

	/** The viewer. */
	public Viewer viewer;

	/** The Constant PERF_CREATE_PART_CONTROL. */
	protected static final String PERF_CREATE_PART_CONTROL = "org.eclipse.ui.views/createPartControl";

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IViewer#hookContextMenu()
	 */
	@Override
	public void hookContextMenu() {
		if (getSite() != null) {
			MenuManager menuMgr = new MenuManager("#PopupMenu");
			menuMgr.setRemoveAllWhenShown(true);
			menuMgr.addMenuListener(new IMenuListener() {
				@Override
				public void menuAboutToShow(IMenuManager manager) {
					fillContextMenu(manager);
				}
			});
			Menu menu = menuMgr.createContextMenu(viewer.getControl());
			viewer.getControl().setMenu(menu);
			getSite().registerContextMenu(menuMgr, viewer);
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Contribute to action bars.
	 */
	public void contributeToActionBars() {
		if (getViewSite() != null) {
			IActionBars bars = getViewSite().getActionBars();
			fillLocalPullDown(bars.getMenuManager());
			fillLocalToolBar(bars.getToolBarManager());
		}
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IViewer#initColumns()
	 */
	@Override
	public void initColumns() {
		// TODO Auto-generated method stub
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IViewer#initDragAndDrop()
	 */
	@Override
	public void initDragAndDrop() {
		Transfer[] transferTypes = new Transfer[] { FileTransfer.getInstance() };
		((StructuredViewer) viewer).addDragSupport(DND.DROP_COPY, transferTypes, new DragProvider(this));
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IViewer#initAdapters(java.util.Collection )
	 */
	@Override
	public void initAdapters(Collection<?> collection) {
		// TODO Auto-generated method stub
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IViewer#initListeners()
	 */
	@Override
	public void initListeners() {
		if (getSite() != null) {
			ISaveablesLifecycleListener saveablesLifecycleListener = new ISaveablesLifecycleListener() {
				ISaveablesLifecycleListener siteSaveablesLifecycleListener = (ISaveablesLifecycleListener) getSite().getService(ISaveablesLifecycleListener.class);

				@Override
				public void handleLifecycleEvent(SaveablesLifecycleEvent event) {
					if (event.getEventType() == SaveablesLifecycleEvent.DIRTY_CHANGED) {
						firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
					}
					siteSaveablesLifecycleListener.handleLifecycleEvent(event);
				}
			};
			// getSite().
			// viewer.getControl().h.getSaveablesService().init(this, viewer,
			// saveablesLifecycleListener);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui. IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		// TODO Auto-generated method stub

	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveListener#perspectiveActivated(org.eclipse. ui.IWorkbenchPage, org.eclipse.ui.IPerspectiveDescriptor)
	 */
	@Override
	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		// TODO Auto-generated method stub

	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveListener#perspectiveChanged(org.eclipse.ui .IWorkbenchPage, org.eclipse.ui.IPerspectiveDescriptor,
	 * java.lang.String)
	 */
	@Override
	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
		// TODO Auto-generated method stub

	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		if (viewer != null) {
			viewer.getControl().setFocus();
		}
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IViewer#getViewer()
	 */
	@Override
	public Object getViewer() {
		return viewer;
	}

	// ---------------------------------------------------------------

	/**
	 * Fill local tool bar.
	 * @param toolBarManager the tool bar manager
	 */
	protected abstract void fillLocalToolBar(IToolBarManager toolBarManager);

	// ---------------------------------------------------------------
	/**
	 * Fill local pull down.
	 * @param menuManager the menu manager
	 */
	protected abstract void fillLocalPullDown(IMenuManager menuManager);

	// ---------------------------------------------------------------

	/**
	 * Fill context menu.
	 * @param manager the manager
	 */
	protected abstract void fillContextMenu(IMenuManager manager);
}
