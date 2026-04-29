package eu.kalafatic.evolution.view.views;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;

public class EvoNavigator extends CommonNavigator {

	/** The lock. */
	private final Lock lock = new ReentrantLock(true);

	/** The actions. */
	private Action expandAllAction, collapseAllAction;

	public EvoNavigator() {
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

	/**
	 * Contribute to action bars.
	 */
	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	/**
	 * Fill local pull down.
	 * 
	 * @param manager the manager
	 */
	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(expandAllAction);
		manager.add(collapseAllAction);
	}

	/**
	 * Fill local tool bar.
	 * 
	 * @param manager the manager
	 */
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(expandAllAction);
		manager.add(collapseAllAction);
	}

	/**
	 * Make actions.
	 */
	private void makeActions() {
		expandAllAction = new Action("Expand All") {
			@Override
			public void run() {
				getCommonViewer().expandAll();
			}
		};
		expandAllAction.setToolTipText("Expand All");
		expandAllAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));

		collapseAllAction = new Action("Collapse All") {
			@Override
			public void run() {
				getCommonViewer().collapseAll();
			}
		};
		collapseAllAction.setToolTipText("Collapse All");
		collapseAllAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL));
	}

	/**
	 * Refresh and expand to the given resource.
	 */
	public void refreshAndExpand(IResource resource) {
		if (lock.tryLock()) {
			try {
				Display.getDefault().asyncExec(() -> {
					if (getCommonViewer() != null && !getCommonViewer().getControl().isDisposed()) {
						getCommonViewer().refresh();
						if (resource != null) {
							getCommonViewer().setSelection(new StructuredSelection(resource), true);
							getCommonViewer().expandToLevel(resource, 1);
						}

						// Trigger decorator refresh
						IDecoratorManager decoratorManager = PlatformUI.getWorkbench().getDecoratorManager();
						decoratorManager.update("eu.kalafatic.evolution.view.evoLabelDecorator");
					}
				});
			} finally {
				lock.unlock();
			}
		}
	}

	/**
	 * Refresh.
	 */
	public void refresh() {
		if (lock.tryLock()) {
			try {
				Display.getDefault().asyncExec(refresh);
			} finally {
				lock.unlock();
			}
		}
	}

	/** The refresh. */
	private final Runnable refresh = new Runnable() {
		@Override
		public void run() {
			lock.lock();
			try {
				if (getCommonViewer() != null && getCommonViewer().getControl() != null && !getCommonViewer().getControl().isDisposed()
						&& getCommonViewer().getControl().isVisible()) {

					getCommonViewer().refresh();
				}
			} catch (Exception e) {
				// e.printStackTrace();
			} finally {
				lock.unlock();
			}
		}
	};

	@Override
	public void setFocus() {
		getCommonViewer().getControl().setFocus();
	}
}
