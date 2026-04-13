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
package eu.kalafatic.utils.dialogs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.internal.IWorkbenchHelpContextIds;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.views.IViewDescriptor;

import eu.kalafatic.utils.hack.Category;
import eu.kalafatic.utils.providers.CapabilityFilter;
import eu.kalafatic.utils.providers.ViewContentProvider;
import eu.kalafatic.utils.providers.ViewLabelProvider;
import eu.kalafatic.utils.providers.ViewPatternFilter;

/**
 * The Class class ShowViewsDialog.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
@SuppressWarnings({ "restriction", "deprecation" })
public class ShowViewsDialog extends Dialog implements ISelectionChangedListener, IDoubleClickListener {

	/** The Constant DIALOG_SETTING_SECTION_NAME. */
	private static final String DIALOG_SETTING_SECTION_NAME = "ShowViewDialog";

	/** The Constant LIST_HEIGHT. */
	private static final int LIST_HEIGHT = 300;

	/** The Constant LIST_WIDTH. */
	private static final int LIST_WIDTH = 250;

	/** The Constant STORE_EXPANDED_CATEGORIES_ID. */
	private static final String STORE_EXPANDED_CATEGORIES_ID = DIALOG_SETTING_SECTION_NAME + ".STORE_EXPANDED_CATEGORIES_ID";

	/** The Constant STORE_SELECTED_VIEW_ID. */
	private static final String STORE_SELECTED_VIEW_ID = DIALOG_SETTING_SECTION_NAME + ".STORE_SELECTED_VIEW_ID";

	/** The filtered tree. */
	private FilteredTree filteredTree;

	/** The ok button. */
	private Button okButton;

	/** The view descs. */
	private IViewDescriptor[] viewDescs = new IViewDescriptor[0];

	/** The categories. */
	private List<Category> categories;

	/** The window. */
	private IWorkbenchWindow window;

	/** The dimmed foreground. */
	private Color dimmedForeground;

	/**
	 * Instantiates a new show views dialog.
	 * @param window the window
	 * @param categories the categories
	 */
	public ShowViewsDialog(IWorkbenchWindow window, List<Category> categories) {
		super(window.getShell());
		this.window = window;
		this.categories = categories;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			saveWidgetValues();
		}
		super.buttonPressed(buttonId);
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#cancelPressed()
	 */
	@Override
	protected void cancelPressed() {
		viewDescs = new IViewDescriptor[0];
		super.cancelPressed();
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets .Shell)
	 */
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(WorkbenchMessages.ShowView_shellTitle);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IWorkbenchHelpContextIds.SHOW_VIEW_DIALOG);
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse .swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		updateButtons();
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets .Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		// Run super.
		Composite composite = (Composite) super.createDialogArea(parent);
		composite.setFont(parent.getFont());

		createFilteredTreeViewer(composite);

		layoutTopControl(filteredTree);

		// Use F2... label
		Label label = new Label(composite, SWT.WRAP);
		label.setText(WorkbenchMessages.ShowView_selectViewHelp);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Restore the last state
		restoreWidgetValues();

		applyDialogFont(composite);

		// Return results.
		return composite;
	}

	// ---------------------------------------------------------------

	/**
	 * Blend.
	 * @param c1 the c1
	 * @param c2 the c2
	 * @param ratio the ratio
	 * @return the rGB
	 */
	private static RGB blend(RGB c1, RGB c2, int ratio) {
		int r = blend(c1.red, c2.red, ratio);
		int g = blend(c1.green, c2.green, ratio);
		int b = blend(c1.blue, c2.blue, ratio);
		return new RGB(r, g, b);
	}

	// ---------------------------------------------------------------

	/**
	 * Blend.
	 * @param v1 the v1
	 * @param v2 the v2
	 * @param ratio the ratio
	 * @return the int
	 */
	private static int blend(int v1, int v2, int ratio) {
		int b = (ratio * v1 + (100 - ratio) * v2) / 100;
		return Math.min(255, b);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the filtered tree viewer.
	 * @param parent the parent
	 */

	private void createFilteredTreeViewer(Composite parent) {
		PatternFilter filter = new ViewPatternFilter();

		int styleBits = SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER;
		filteredTree = new FilteredTree(parent, styleBits, filter, true);
		filteredTree.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

		TreeViewer treeViewer = filteredTree.getViewer();
		Control treeControl = treeViewer.getControl();
		RGB dimmedRGB = blend(treeControl.getForeground().getRGB(), treeControl.getBackground().getRGB(), 60);
		dimmedForeground = new Color(treeControl.getDisplay(), dimmedRGB);
		treeControl.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				dimmedForeground.dispose();
			}
		});

		treeViewer.setLabelProvider(new ViewLabelProvider(window, dimmedForeground));
		treeViewer.setContentProvider(new ViewContentProvider());
		// treeViewer.setComparator(new ViewComparator((ViewRegistry) viewReg));
		treeViewer.setInput(categories);
		treeViewer.addSelectionChangedListener(this);
		treeViewer.addDoubleClickListener(this);
		treeViewer.addFilter(new CapabilityFilter());
		treeViewer.getControl().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				handleTreeViewerKeyPressed(e);
			}
		});
		// if the tree has only one or zero views, disable the filter text
		// control
		if (hasAtMostOneView(filteredTree.getViewer())) {
			Text filterText = filteredTree.getFilterControl();
			if (filterText != null) {
				filterText.setEnabled(false);
			}
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Checks for at most one view.
	 * @param tree the tree
	 * @return true, if successful
	 */
	private boolean hasAtMostOneView(TreeViewer tree) {
		ITreeContentProvider contentProvider = (ITreeContentProvider) tree.getContentProvider();
		Object[] children = contentProvider.getElements(tree.getInput());

		if (children.length <= 1) {
			if (children.length == 0) {
				return true;
			}
			return !contentProvider.hasChildren(children[0]);
		}
		return false;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IDoubleClickListener#doubleClick(org.eclipse .jface.viewers.DoubleClickEvent)
	 */
	@Override
	public void doubleClick(DoubleClickEvent event) {
		IStructuredSelection s = (IStructuredSelection) event.getSelection();
		Object element = s.getFirstElement();
		if (filteredTree.getViewer().isExpandable(element)) {
			filteredTree.getViewer().setExpandedState(element, !filteredTree.getViewer().getExpandedState(element));
		} else if (viewDescs.length > 0) {
			saveWidgetValues();
			setReturnCode(OK);
			close();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the dialog settings.
	 * @return the dialog settings
	 */
	private IDialogSettings getDialogSettings() {
		IDialogSettings workbenchSettings = WorkbenchPlugin.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection(DIALOG_SETTING_SECTION_NAME);
		if (section == null) {
			section = workbenchSettings.addNewSection(DIALOG_SETTING_SECTION_NAME);
		}
		return section;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the selection.
	 * @return the selection
	 */
	public IViewDescriptor[] getSelection() {
		return viewDescs;
	}

	// ---------------------------------------------------------------

	/**
	 * Layout top control.
	 * @param control the control
	 */
	private void layoutTopControl(Control control) {
		GridData spec = new GridData(GridData.FILL_BOTH);
		spec.widthHint = LIST_WIDTH;
		spec.heightHint = LIST_HEIGHT;
		control.setLayoutData(spec);
	}

	// ---------------------------------------------------------------

	/**
	 * Save widget values.
	 */
	// private IViewDescriptor findView(String selectedViewId) {
	// Iterator<Object> iterator = categories.iterator();
	// while (iterator.hasNext()) {
	// Object object = iterator.next();
	// if (object instanceof IViewDescriptor) {
	// IViewDescriptor iViewDescriptor = (IViewDescriptor) object;
	// if (iViewDescriptor.getId().equals(selectedViewId)) {
	// return iViewDescriptor;
	// }
	// }
	// }
	// return null;
	// }

	// ---------------------------------------------------------------

	/**
	 * Find category.
	 * @param expandedCategoryIds the expanded category ids
	 * @param i the i
	 * @return the i view category
	 */
	// private IViewCategory findCategory(String[] expandedCategoryIds, int i) {
	// findCategory(categories.entrySet(), expandedCategoryIds[i]);
	// Set<Entry<String, Object>> entrySet = categories.entrySet();
	// for (Entry<String, Object> entry : entrySet) {
	// if (entry.getValue() instanceof Map) {
	// Map map = (Map) entry.getValue();
	// if (!map.isEmpty()) {
	//
	// } else {
	//
	// }
	// }
	// if (entry.getValue() instanceof Map) {
	//
	// }
	// if (entry.getKey().equals(expandedCategoryIds[i])) {
	//
	// }
	// }
	// return null;
	// }

	// private IViewCategory findCategory(Set<Entry<String, Object>> entrySet,
	// String expandedCategory) {
	// for (Entry<String, Object> entry : entrySet) {
	// if (entry.getValue() instanceof Map) {
	// Map map = (Map) entry.getValue();
	// if (!map.isEmpty()) {
	// findCategory(map.entrySet(), expandedCategory);
	// } else if (entry.getKey().equals(expandedCategory)) {
	// return null;
	// }
	// }
	// }
	// return null;
	//
	// }

	// ---------------------------------------------------------------

	/**
	 * Save widget values.
	 */
	private void saveWidgetValues() {
		IDialogSettings settings = getDialogSettings();
		// Collect the ids of the all expanded categories
		Object[] expandedElements = filteredTree.getViewer().getExpandedElements();
		String[] expandedCategoryIds = new String[expandedElements.length];
		for (int i = 0; i < expandedElements.length; ++i) {
			expandedCategoryIds[i] = ((Category) expandedElements[i]).getId();
		}
		// Save them for next time.
		settings.put(STORE_EXPANDED_CATEGORIES_ID, expandedCategoryIds);

		String selectedViewId = "";
		if (viewDescs.length > 0) {
			// in the case of a multi-selection, it's probably less confusing
			// to store just the first rather than the whole multi-selection
			selectedViewId = viewDescs[0].getId();
		}
		settings.put(STORE_SELECTED_VIEW_ID, selectedViewId);
	}

	// ---------------------------------------------------------------

	/**
	 * Restore widget values.
	 */
	protected void restoreWidgetValues() {
		IDialogSettings settings = getDialogSettings();

		String[] expandedCategoryIds = settings.getArray(STORE_EXPANDED_CATEGORIES_ID);
		if (expandedCategoryIds == null) {
			return;
		}

		List<Category> categoriesToExpand = new ArrayList<Category>(expandedCategoryIds.length);
		for (int i = 0; i < expandedCategoryIds.length; i++) {
			for (Category category : categories) {
				if (category.getId().equals(expandedCategoryIds[i])) {
					categoriesToExpand.add(category);
				}
			}
		}

		if (!categoriesToExpand.isEmpty()) {
			filteredTree.getViewer().setExpandedElements(categoriesToExpand.toArray());
		}

		String selectedViewId = settings.get(STORE_SELECTED_VIEW_ID);
		if (selectedViewId != null) {
			IViewDescriptor iViewDescriptor = PlatformUI.getWorkbench().getViewRegistry().find(selectedViewId);

			if (iViewDescriptor != null) {
				filteredTree.getViewer().setSelection(new StructuredSelection(iViewDescriptor), true);
			}
		}
	}

	// ---------------------------------------------------------------

	// ---------------------------------------------------------------
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged( org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		updateSelection(event);
		updateButtons();
	}

	// ---------------------------------------------------------------
	/**
	 * Update buttons.
	 */
	private void updateButtons() {
		if (okButton != null) {
			okButton.setEnabled(getSelection().length > 0);
		}
	}

	// ---------------------------------------------------------------
	/**
	 * Update selection.
	 * @param event the event
	 */

	private void updateSelection(SelectionChangedEvent event) {
		ArrayList<Object> descs = new ArrayList<Object>();
		IStructuredSelection sel = (IStructuredSelection) event.getSelection();
		for (Iterator<?> i = sel.iterator(); i.hasNext();) {
			Object o = i.next();
			if (o instanceof IViewDescriptor) {
				descs.add(o);
			}
		}
		viewDescs = new IViewDescriptor[descs.size()];
		descs.toArray(viewDescs);
	}

	// ---------------------------------------------------------------
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.window.Dialog#getDialogBoundsSettings()
	 * @since 3.4
	 */
	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return getDialogSettings();
	}

	// ---------------------------------------------------------------
	/**
	 * Handle tree viewer key pressed.
	 * @param event the event
	 */
	private void handleTreeViewerKeyPressed(KeyEvent event) {
		// popup the description for the selected view
		if (event.keyCode == SWT.F2 && event.stateMask == 0) {
			ITreeSelection selection = (ITreeSelection) filteredTree.getViewer().getSelection();
			// only show description if one view is selected
			if (selection.size() == 1) {
				Object o = selection.getFirstElement();
				if (o instanceof IViewDescriptor) {
					String description = ((IViewDescriptor) o).getDescription();
					if (description.length() == 0) {
						description = WorkbenchMessages.ShowView_noDesc;
					}
//					popUp(description);
				}
			}
		}
	}

	// ---------------------------------------------------------------
	/**
	 * Pop up.
	 * @param description the description
	 */
//	private void popUp(final String description) {
//		new PopupDialog(filteredTree.getShell(), PopupDialog.HOVER_SHELLSTYLE, true, false, false, false, null, null) {
//			private static final int CURSOR_SIZE = 15;
//
//			@Override
//			protected Point getInitialLocation(Point initialSize) {
//				// show popup relative to cursor
//				Display display = getShell().getDisplay();
//				Point location = display.getCursorLocation();
//				location.x += CURSOR_SIZE;
//				location.y += CURSOR_SIZE;
//				return location;
//			}
//
//			@Override
//			protected Control createDialogArea(Composite parent) {
//				Label label = new Label(parent, SWT.WRAP);
//				label.setText(description);
//				label.addFocusListener(new FocusAdapter() {
//					@Override
//					public void focusLost(FocusEvent event) {
//						close();
//					}
//				});
//				// Use the compact margins employed by PopupDialog.
//				GridData gd = new GridData(GridData.BEGINNING | GridData.FILL_BOTH);
//				gd.horizontalIndent = PopupDialog.POPUP_HORIZONTALSPACING;
//				gd.verticalIndent = PopupDialog.POPUP_VERTICALSPACING;
//				label.setLayoutData(gd);
//				return label;
//			}
//		}.open();
//	}

	// ---------------------------------------------------------------
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}
}
