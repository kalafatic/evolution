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
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewerColumn;
import eu.kalafatic.evolution.view.provider.OrchestrationNavigatorContentProvider.ModelProperty;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;

public class EvoNavigator extends CommonNavigator {

	/** The lock. */
	private final Lock lock = new ReentrantLock(true);

	/** The actions. */
	private Action expandAllAction, collapseAllAction, refreshAction;

	public EvoNavigator() {
		super();
	}

    @Override
    protected Object getInitialInput() {
        return ResourcesPlugin.getWorkspace().getRoot();
    }

	private Text filterText;
	private String pattern = "";

	@Override
	public void createPartControl(final Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		container.setLayout(layout);

		filterText = new Text(container, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		filterText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		filterText.setMessage("Filter elements...");
		filterText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				pattern = filterText.getText().toLowerCase();
				getCommonViewer().refresh();
			}
		});

		super.createPartControl(container);
		getCommonViewer().getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		getCommonViewer().addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (pattern.isEmpty()) return true;
				String text = ((org.eclipse.jface.viewers.ILabelProvider) getCommonViewer().getLabelProvider()).getText(element);
				if (text != null && text.toLowerCase().contains(pattern)) return true;

				// Recursive check for children
				ITreeContentProvider cp = (ITreeContentProvider) getCommonViewer().getContentProvider();
				for (Object child : cp.getChildren(element)) {
					if (select(viewer, element, child)) return true;
				}

				return false;
			}
		});

		Tree tree = getCommonViewer().getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		org.eclipse.swt.widgets.TreeColumn column1 = new org.eclipse.swt.widgets.TreeColumn(tree, SWT.LEFT);
		column1.setText("Element");
		column1.setWidth(300);

		TreeViewerColumn column2 = new TreeViewerColumn(getCommonViewer(), SWT.LEFT);
		column2.getColumn().setText("Status");
		column2.getColumn().setWidth(200);
		column2.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((org.eclipse.jface.viewers.ITableLabelProvider) getCommonViewer().getLabelProvider()).getColumnText(element, 1);
			}
		});

		column2.setEditingSupport(new EditingSupport(getCommonViewer()) {
			@Override
			protected boolean canEdit(Object element) {
				return element instanceof eu.kalafatic.evolution.model.orchestration.Task ||
					   element instanceof eu.kalafatic.evolution.model.orchestration.Orchestrator ||
					   element instanceof ModelProperty;
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(getCommonViewer().getTree());
			}

			@Override
			protected Object getValue(Object element) {
				if (element instanceof eu.kalafatic.evolution.model.orchestration.Task) {
					return ((eu.kalafatic.evolution.model.orchestration.Task) element).getName();
				}
				if (element instanceof eu.kalafatic.evolution.model.orchestration.Orchestrator) {
					return ((eu.kalafatic.evolution.model.orchestration.Orchestrator) element).getName();
				}
				if (element instanceof ModelProperty) {
					ModelProperty mp = (ModelProperty) element;
					Object val = mp.owner.eGet(mp.attribute);
					return val != null ? String.valueOf(val) : "";
				}
				return "";
			}

			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof eu.kalafatic.evolution.model.orchestration.Task) {
					((eu.kalafatic.evolution.model.orchestration.Task) element).setName(String.valueOf(value));
				} else if (element instanceof eu.kalafatic.evolution.model.orchestration.Orchestrator) {
					((eu.kalafatic.evolution.model.orchestration.Orchestrator) element).setName(String.valueOf(value));
				} else if (element instanceof ModelProperty) {
					ModelProperty mp = (ModelProperty) element;
					String strVal = String.valueOf(value);
					if (mp.attribute.getEType().getInstanceClass() == boolean.class || mp.attribute.getEType().getInstanceClass() == Boolean.class) {
						mp.owner.eSet(mp.attribute, Boolean.parseBoolean(strVal));
					} else if (mp.attribute.getEType().getInstanceClass() == int.class || mp.attribute.getEType().getInstanceClass() == Integer.class) {
						try { mp.owner.eSet(mp.attribute, Integer.parseInt(strVal)); } catch (Exception e) {}
					} else if (mp.attribute.getEType().getInstanceClass() == float.class || mp.attribute.getEType().getInstanceClass() == Float.class) {
						try { mp.owner.eSet(mp.attribute, Float.parseFloat(strVal)); } catch (Exception e) {}
					} else {
						mp.owner.eSet(mp.attribute, strVal);
					}
				}
				getCommonViewer().update(element, null);
			}
		});

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
		manager.add(new Separator());
		manager.add(refreshAction);
	}

	/**
	 * Fill local tool bar.
	 * 
	 * @param manager the manager
	 */
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(expandAllAction);
		manager.add(collapseAllAction);
		manager.add(new Separator());
		manager.add(refreshAction);
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

		refreshAction = new Action("Refresh") {
			@Override
			public void run() {
				refresh();
			}
		};
		refreshAction.setToolTipText("Refresh");
		refreshAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));
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
							getCommonViewer().expandAll();
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
