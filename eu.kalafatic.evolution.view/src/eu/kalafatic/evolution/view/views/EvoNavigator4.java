package eu.kalafatic.evolution.view.views;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.Saveable;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.progress.UIJob;

//import eu.kalafatic.utils.application.ValidationUtils;
//import eu.kalafatic.utils.constants.FCoreImageConstants;
//import eu.kalafatic.utils.constants.FTextConstants;

import static eu.kalafatic.utils.constants.FCoreImageConstants.ADD_DESC;
import static eu.kalafatic.utils.constants.FCoreImageConstants.ADD_X_DESC;
import static eu.kalafatic.utils.constants.FCoreImageConstants.COLLAPSE_ALL_DESC;
import static eu.kalafatic.utils.constants.FCoreImageConstants.DELETE_DESC;
import static eu.kalafatic.utils.constants.FCoreImageConstants.EXPAND_ALL_DESC;
import static eu.kalafatic.utils.constants.FCoreImageConstants.TREE_IMG;
import static eu.kalafatic.utils.constants.FTextConstants.ADD_FOLDER;
import static eu.kalafatic.utils.constants.FTextConstants.ADD_PAGE;
import static eu.kalafatic.utils.constants.FTextConstants.COLLAPSE_ALL;
import static eu.kalafatic.utils.constants.FTextConstants.EXPAND_ALL;
import static eu.kalafatic.utils.constants.FTextConstants.REMOVE;

public class EvoNavigator4 extends CommonNavigator {

	private Action expandAllAction;
	private Action collapseAllAction;

	/**
	 * Prevent refresh storms/reentrancy.
	 */
	private final AtomicBoolean refreshScheduled = new AtomicBoolean(false);

	public EvoNavigator4() {
		super();
	}

	@Override
	protected Object getInitialInput() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	@Override
	public void createPartControl(final Composite parent) {
		super.createPartControl(parent);

		Tree tree = getCommonViewer().getTree();

		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		org.eclipse.swt.widgets.TreeColumn column1 = new org.eclipse.swt.widgets.TreeColumn(tree, SWT.LEFT);

		column1.setText("Element");
		column1.setWidth(300);

		org.eclipse.swt.widgets.TreeColumn column2 = new org.eclipse.swt.widgets.TreeColumn(tree, SWT.LEFT);

		column2.setText("Status");
		column2.setWidth(200);

		makeActions();
		contributeToActionBars();
	}

	private void contributeToActionBars() {

		IActionBars bars = getViewSite().getActionBars();

		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(expandAllAction);
		manager.add(collapseAllAction);
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(expandAllAction);
		manager.add(collapseAllAction);
	}

	private void makeActions() {

		expandAllAction = new Action("Expand All") {
			@Override
			public void run() {
				if (getCommonViewer() != null) {
					getCommonViewer().expandAll();
				}
			}
		};

		expandAllAction.setToolTipText("Expand All");

		expandAllAction.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));

		collapseAllAction = new Action("Collapse All") {
			@Override
			public void run() {
				if (getCommonViewer() != null) {
					getCommonViewer().collapseAll();
				}
			}
		};

		collapseAllAction.setToolTipText("Collapse All");

		collapseAllAction.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL));
	}

	/**
	 * Safe delayed refresh.
	 */
	public void refresh() {

		if (!refreshScheduled.compareAndSet(false, true)) {
			return;
		}

		UIJob job = new UIJob("Refresh Navigator") {

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {

				refreshScheduled.set(false);

				try {

					if (getCommonViewer() == null) {
						return Status.OK_STATUS;
					}

					if (getCommonViewer().getControl() == null) {
						return Status.OK_STATUS;
					}

					if (getCommonViewer().getControl().isDisposed()) {
						return Status.OK_STATUS;
					}

					/*
					 * IMPORTANT: Avoid full refresh storms during editor save.
					 */
					getCommonViewer().refresh();

				} catch (Exception e) {
					e.printStackTrace();
				}

				return Status.OK_STATUS;
			}
		};

		/*
		 * Delay avoids refresh during active editor save transaction.
		 */
		job.schedule(250);
	}

	/**
	 * Refresh only affected resource.
	 */
	public void refreshAndExpand(IResource resource) {

		Display.getDefault().asyncExec(() -> {

			try {

				if (getCommonViewer() == null) {
					return;
				}

				if (getCommonViewer().getControl() == null) {
					return;
				}

				if (getCommonViewer().getControl().isDisposed()) {
					return;
				}

				if (resource != null) {

					/*
					 * Refresh ONLY changed resource.
					 */
					getCommonViewer().refresh(resource, true);

					getCommonViewer().setSelection(new StructuredSelection(resource), true);

					getCommonViewer().expandToLevel(resource, 1);

				} else {

					getCommonViewer().refresh();
				}

				/*
				 * Avoid aggressive decorator updates during save storms.
				 */
				IDecoratorManager decoratorManager = PlatformUI.getWorkbench().getDecoratorManager();

				decoratorManager.update("eu.kalafatic.evolution.view.evoLabelDecorator");

			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public void setFocus() {

		if (getCommonViewer() != null && getCommonViewer().getControl() != null
				&& !getCommonViewer().getControl().isDisposed()) {

			getCommonViewer().getControl().setFocus();
		}
	}
}