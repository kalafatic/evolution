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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.DrillDownAdapter;

import eu.kalafatic.utils.constants.FTextConstants;
import eu.kalafatic.utils.constants.FUIConstants;

/**
 * The Class class ATreeViewer.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public abstract class ATreeViewer extends AViewer {

	/** The drill down adapter. */
	protected DrillDownAdapter drillDownAdapter;

	/** The collapse all. */
	protected Action expandAll, collapseAll;

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets .Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, FUIConstants.TREE_STYLE_0);
		drillDownAdapter = new DrillDownAdapter((TreeViewer) viewer);

		// viewer.setSorter(new NameSorter());
	}

	// ---------------------------------------------------------------

	/**
	 * Inits the.
	 */
	public void init() {
		initColumns();
		makeActions();
		hookContextMenu();
		contributeToActionBars();
		initDragAndDrop();
		initListeners();
	}

	// ---------------------------------------------------------------

	/**
	 * Make actions.
	 */
	private void makeActions() {
		expandAll = new Action() {
			@Override
			public void run() {
				((AbstractTreeViewer) viewer).expandAll();
			}
		};
		expandAll.setText(FTextConstants.EXPAND_ALL);
		expandAll.setToolTipText(FTextConstants.EXPAND_ALL);
		expandAll.setImageDescriptor(FUIConstants.EXPAND_ALL_DESC);

		collapseAll = new Action() {
			@Override
			public void run() {
				((AbstractTreeViewer) viewer).collapseAll();
			}
		};
		collapseAll.setText(FTextConstants.COLLAPSE_ALL);
		collapseAll.setToolTipText(FTextConstants.COLLAPSE_ALL);
		collapseAll.setImageDescriptor(FUIConstants.COLLAPSE_ALL_DESC);
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.AViewer#fillContextMenu(org.eclipse .jface.action.IMenuManager)
	 */
	@Override
	public void fillContextMenu(IMenuManager manager) {
		manager.add(expandAll);
		manager.add(collapseAll);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.AViewer#fillLocalToolBar(org.eclipse .jface.action.IToolBarManager)
	 */
	@Override
	public void fillLocalToolBar(IToolBarManager manager) {
		manager.add(expandAll);
		manager.add(collapseAll);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.AViewer#fillLocalPullDown(org.eclipse .jface.action.IMenuManager)
	 */
	@Override
	public void fillLocalPullDown(IMenuManager manager) {
		manager.add(new Separator());
		manager.add(expandAll);
		manager.add(collapseAll);
	}

}
