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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPart;

import eu.kalafatic.utils.application.AppUtils;
import eu.kalafatic.utils.constants.FTextConstants;
import eu.kalafatic.utils.interfaces.ATreeViewer;
import eu.kalafatic.utils.listeners.OpenFileListener;
import eu.kalafatic.utils.providers.FileTreeContentProvider;
import eu.kalafatic.utils.providers.FileTreeLabelProvider;

/**
 * The Class class WorkspaceView.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class WorkspaceView extends ATreeViewer {

	/** The Constant ID. */
	public static final String ID = "eu.kalafatic.gemini.core.views.WorkspaceView";

	/** The path regex. */
	private final String pathRegex = "[/\\\\]";

	/** The tree. */
	private Tree tree;

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets .Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		((TreeViewer) viewer).setContentProvider(new FileTreeContentProvider());
		((TreeViewer) viewer).setLabelProvider(new FileTreeLabelProvider());

		File workspace = new File(getWorkspace());
		if (!workspace.exists()) {
			workspace.mkdirs();
		}
		// viewer.setInput(File.listRoots());
		viewer.setInput(workspace);
		tree = ((TreeViewer) viewer).getTree();

		((TreeViewer) viewer).expandToLevel(1);

		String[] segments = getWorkspace().split(pathRegex, -1);
		segments[0] += "\\";
		expand(tree.getItems(), segments, 0);

		init();
	}

	// ---------------------------------------------------------------

	/**
	 * Expand.
	 * @param items the items
	 * @param segments the segments
	 * @param index the index
	 */
	public void expand(TreeItem[] items, String[] segments, int index) {
		TreeItem lastItem = null;
		for (int i = 0; i < items.length && index < segments.length; i++) {
			if (items[i].getText().equalsIgnoreCase(segments[index])) {
				lastItem = items[i];
				lastItem.setExpanded(true);
				viewer.refresh();
				expand(lastItem.getItems(), segments, ++index);
			}
		}
		if ((index >= segments.length) && (lastItem != null)) {
			tree.setSelection(lastItem);
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the workspace.
	 * @return the workspace
	 */
	private String getWorkspace() {
		String id = AppUtils.getCurrentPerspective().getId();
		// if (id.equals(CorePerspective.ID)) {
		// return PREFERENCES.get(ECorePreferences.WORKSPACE_LOC.getName(), (String) ECorePreferences.WORKSPACE_LOC.getDef());
		// }
		return FTextConstants.DEF_WORKSPACE;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IViewer#initListeners()
	 */
	@Override
	public void initListeners() {
		// treeViewer.addDoubleClickListener(new
		// SwarmMapTreeDoubleClickListener(
		// this));
		//
		tree.addMouseListener(new OpenFileListener(getSite()));
		getSite().getWorkbenchWindow().addPerspectiveListener(this);

		// PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
		// IJavaHelpContextIds.TOGGLE_BREADCRUMB_ACTION);
		// viewer.addTreeListener(new SwarmMapTreeDropListener(tree));
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IViewer#getViewer()
	 */
	@Override
	public TreeViewer getViewer() {
		return (TreeViewer) viewer;
	}

	// ---------------------------------------------------------------

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
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();

		getSite().getWorkbenchWindow().removePerspectiveListener(this);
	}
}