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
package eu.kalafatic.utils.hack;

/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids <sdavids@gmx.de> - Fix for bug 19346 - Dialog font should be
 *     activated and used by other components.
 *******************************************************************************/

/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids <sdavids@gmx.de> - Fix for bug 19346 - Dialog font should be
 *     activated and used by other components.
 *******************************************************************************/

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.ITriggerPoint;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.eclipse.ui.model.PerspectiveLabelProvider;

import eu.kalafatic.utils.providers.PerspectiveContentProvider;

/**
 * The Class class SelectPerspectiveDialog.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class SelectPerspectiveDialog extends Dialog implements ISelectionChangedListener {

	/** The Constant LIST_HEIGHT. */
	final private static int LIST_HEIGHT = 300;

	/** The Constant LIST_WIDTH. */
	final private static int LIST_WIDTH = 300;

	/** The viewer. */
	private TableViewer viewer;

	/** The ok button. */
	private Button okButton;

	/** The persp desc. */
	private IPerspectiveDescriptor perspDesc;

	/** The gemini perspectives. */
	private List<IPerspectiveDescriptor> geminiPerspectives;

	/** The show all button. */
	private Button showAllButton;

	/**
	 * Instantiates a new select perspective dialog.
	 * @param parentShell the parent shell
	 * @param geminiPerspectives the gemini perspectives
	 */
	public SelectPerspectiveDialog(Shell parentShell, List<IPerspectiveDescriptor> geminiPerspectives) {
		super(parentShell);
		this.geminiPerspectives = geminiPerspectives;
		setShellStyle(getShellStyle() | SWT.SHEET);
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#cancelPressed()
	 */
	@Override
	protected void cancelPressed() {
		perspDesc = null;
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
		shell.setText("Open Perspective");
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
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
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		// Run super.
		Composite composite = (Composite) super.createDialogArea(parent);
		composite.setFont(parent.getFont());

		createViewer(composite);
		layoutTopControl(viewer.getControl());
		if (needsShowAllButton()) {
			createShowAllButton(composite);
		}
		// Return results.
		return composite;
	}

	// ---------------------------------------------------------------

	/**
	 * Needs show all button.
	 * @return true, if successful
	 */
	private boolean needsShowAllButton() {
		return true;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the show all button.
	 * @param parent the parent
	 */
	private void createShowAllButton(Composite parent) {
		showAllButton = new Button(parent, SWT.CHECK);
		showAllButton.setText("Show all");
		showAllButton.addSelectionListener(new SelectionAdapter() {

			/*
			 * (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (showAllButton.getSelection()) {
					viewer.resetFilters();
				} else {
					// list.addFilter(activityViewerFilter);
				}
			}
		});
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the viewer.
	 * @param parent the parent
	 */
	private void createViewer(Composite parent) {
		// Add perspective list.
		viewer = new TableViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.getTable().setFont(parent.getFont());
		viewer.setLabelProvider(new PerspectiveLabelProvider());
		viewer.setContentProvider(new PerspectiveContentProvider());
		viewer.setComparator(new ViewerComparator());
		viewer.setInput(geminiPerspectives);
		viewer.addSelectionChangedListener(this);
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClickEvent();
			}
		});
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the selection.
	 * @return the selection
	 */
	public IPerspectiveDescriptor getSelection() {
		return perspDesc;
	}

	// ---------------------------------------------------------------

	/**
	 * Handle double click event.
	 */
	protected void handleDoubleClickEvent() {
		okPressed();
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

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
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
	protected void updateButtons() {
		okButton.setEnabled(getSelection() != null);
	}

	// ---------------------------------------------------------------

	/**
	 * Update selection.
	 * @param event the event
	 */
	protected void updateSelection(SelectionChangedEvent event) {
		perspDesc = null;
		IStructuredSelection sel = (IStructuredSelection) event.getSelection();
		if (!sel.isEmpty()) {
			Object obj = sel.getFirstElement();
			if (obj instanceof IPerspectiveDescriptor) {
				perspDesc = (IPerspectiveDescriptor) obj;
			}
		}
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		ITriggerPoint triggerPoint = PlatformUI.getWorkbench().getActivitySupport().getTriggerPointManager().getTriggerPoint("org.eclipse.ui.openPerspectiveDialog");
		if (WorkbenchActivityHelper.allowUseOf(triggerPoint, getSelection())) {
			super.okPressed();
		}
	}

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
