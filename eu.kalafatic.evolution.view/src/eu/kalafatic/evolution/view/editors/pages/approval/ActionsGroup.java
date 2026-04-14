package eu.kalafatic.evolution.view.editors.pages.approval;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.view.editors.pages.ApprovalPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

public class ActionsGroup extends AEvoGroup {

    public ActionsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, ApprovalPage page) {
        super(editor, orchestrator);
        createControl(toolkit, parent, page);
    }

    @Override
    protected void refreshUI() {
        // No dynamic model data to refresh
    }

    private void createControl(FormToolkit toolkit, Composite parent, ApprovalPage page) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Approval Actions", 2, true);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button approveBtn = toolkit.createButton(group, "Approve & Apply Changes", SWT.PUSH);
        approveBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        approveBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { page.handleApprove(); }
        });

        Button approveAllBtn = toolkit.createButton(group, "Approve All (Auto)", SWT.PUSH);
        approveAllBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        approveAllBtn.setToolTipText("Automatically approve all remaining tasks in the current loop");
        approveAllBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { page.handleApproveAll(); }
        });

        Button rejectBtn = toolkit.createButton(group, "Reject & Abort", SWT.PUSH);
        rejectBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        rejectBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { page.handleReject(); }
        });
    }
}
