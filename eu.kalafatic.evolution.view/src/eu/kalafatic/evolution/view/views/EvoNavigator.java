package eu.kalafatic.evolution.view.views;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
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
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.Saveable;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.part.DrillDownAdapter;

import eu.kalafatic.utils.application.ValidationUtils;
import eu.kalafatic.utils.constants.FCoreImageConstants;
import eu.kalafatic.utils.constants.FTextConstants;

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

public class EvoNavigator extends CommonNavigator {

	/** The lock. */
	private final Lock lock = new ReentrantLock(true);

	/** The remove action. */
	private Action expandAllAction, collapseAllAction, addFolderAction, addPageAction, removeAction;

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

	public void refresh(Object... objects) {
		List l = new ArrayList();
		if (objects[0] instanceof EObject) {
			l.add(objects[0]);
			// Route device = (Route) objects[0];
			//
			// if (device.getHost().equals("localhost")) {
			// decoration.addOverlay(imageDescriptor);
			// }
			// ILightweightLabelDecorator labelDecorator2 =
			IBaseLabelProvider labelProvider2 = PlatformUI.getWorkbench().getDecoratorManager().getBaseLabelProvider("eu.kalafatic.explorer.view.providers.ExplorerLabelDecorator");

			// labelProvider2.decorateImage(provider.getImage(objects[0]), objects[0]);

			PlatformUI.getWorkbench().getDecoratorManager().update("eu.kalafatic.evolution.view.providers.ExplorerLabelDecorator");
		}
		// provider.update(l.toArray());

	}

	/**
	 * Show message.
	 * @param message the message
	 */
	private void showMessage(String message) {
		MessageDialog.openInformation(getCommonViewer().getControl().getShell(), "Sample View", message);
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
//		manager.add(addFolderAction);
//		manager.add(addPageAction);
//		manager.add(new Separator());
//		manager.add(removeAction);
//		manager.add(new Separator());
	}

	/**
	 * Fill context menu.
	 * 
	 * @param manager the manager
	 */
	private void fillContextMenu(IMenuManager manager) {
		manager.add(addFolderAction);
		manager.add(addPageAction);
		manager.add(new Separator());
		manager.add(removeAction);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	/**
	 * Fill local tool bar.
	 * 
	 * @param manager the manager
	 */
	private void fillLocalToolBar(IToolBarManager manager) {
//		manager.add(addFolderAction);
//		manager.add(addPageAction);
//		manager.add(removeAction);
//		manager.add(new Separator());
		manager.add(expandAllAction);
		manager.add(collapseAllAction);
//		drillDownAdapter.addNavigationActions(manager);
	}

	/**
	 * Make actions.
	 */
	private void makeActions() {

			expandAllAction = new Action() {
				@Override
				public void run() {
					getCommonViewer().expandAll();
				}
			};
			expandAllAction.setToolTipText(FTextConstants.EXPAND_ALL);
			expandAllAction.setImageDescriptor(FCoreImageConstants.EXPAND_ALL_DESC);

			collapseAllAction = new Action() {
				@Override
				public void run() {
					getCommonViewer().collapseAll();
				}
			};

			collapseAllAction.setToolTipText(FTextConstants.COLLAPSE_ALL);
			collapseAllAction.setImageDescriptor(FCoreImageConstants.COLLAPSE_ALL_DESC);

			addFolderAction = new Action() {
				@Override
				public void run() {
					addFolder();
				}

			};

			addFolderAction.setText(ADD_FOLDER);
			addFolderAction.setToolTipText(ADD_FOLDER);
			addFolderAction.setImageDescriptor(ADD_DESC);

			addPageAction = new Action() {
				@Override
				public void run() {
					addPage();

				}

			};

			addPageAction.setText(ADD_PAGE);
			addPageAction.setToolTipText(ADD_PAGE);
			addPageAction.setImageDescriptor(ADD_X_DESC);

			removeAction = new Action() {
				@Override
				public void run() {
					remove();
				}

			};

			removeAction.setText(REMOVE);
			removeAction.setToolTipText(REMOVE);
			removeAction.setImageDescriptor(DELETE_DESC);
	}

	/**
	 * Adds the folder.
	 */
	private void addFolder() {
		ISelection selection = getCommonViewer().getSelection();
		if (selection instanceof TreeSelection) {
			TreeSelection treeSelection = (TreeSelection) selection;

				InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), "New Folder", "Enter 1+ characters", "", ValidationUtils.INSTANCE.new LengthValidator(1, 50));

				if (dialog.open() == Window.OK) {
					String folderName = dialog.getValue();

//					Folder folder = RcFactory.eINSTANCE.createFolder();
//					folder.setAddress(folderName);
//
//					if (treeSelection.isEmpty()) {
//						RCModelManager.getInstance().getRc().getTree().put(folderName, folder);
//						return;
//					}
//
//					if (treeSelection.getFirstElement() instanceof Folder) {
//						Folder parentFolder = (Folder) treeSelection.getFirstElement();
//						folder.setParent(parentFolder);
//						parentFolder.getFolders().put(folderName, folder);
//					}
					refresh();
				}
		}
	}

	/**
	 * Removes the.
	 */
	private void remove() {
		ISelection selection = getCommonViewer().getSelection();
		if (selection instanceof TreeSelection) {
			TreeSelection treeSelection = (TreeSelection) selection;

//				if (treeSelection.getFirstElement() instanceof Folder) {
//					Folder folder = (Folder) treeSelection.getFirstElement();
//
//					if (folder.getParent() == null) {
//						RCModelManager.getInstance().getRc().getTree().removeKey(folder.getAddress());
//					} else {
//						folder.getParent().getFolders().removeKey(folder.getAddress());
//					}
//
//				} else if (treeSelection.getFirstElement() instanceof Page) {
//					Page page = (Page) treeSelection.getFirstElement();
//					page.getParent().getPages().removeKey(page.getAddress());
//				}
			refresh();
		}
	}

	/**
	 * Adds the page.
	 */
	private void addPage() {
//			RCView findView = (RCView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(EView.RC.ID);
//			String address = findView.getLocation().getText();
//			Page page = RcFactory.eINSTANCE.createPage();
//			page.setAddress(address);
//
//			ISelection selection = getCommonViewer().getSelection();
//			if (selection instanceof TreeSelection) {
//				TreeSelection treeSelection = (TreeSelection) selection;
//				if (treeSelection.getFirstElement() instanceof FolderImpl) {
//					FolderImpl folder = (FolderImpl) treeSelection.getFirstElement();
//					folder.getPages().put(address, page);
//					RCModelManager.getInstance().doSave();
//					refresh();
//				}
//			}

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		getCommonViewer().getControl().setFocus();
	}
}
