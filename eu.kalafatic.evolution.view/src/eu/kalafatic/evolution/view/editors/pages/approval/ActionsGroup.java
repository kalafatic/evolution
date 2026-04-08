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

public class ActionsGroup {
    private Composite group;

    public ActionsGroup(FormToolkit toolkit, Composite parent, ApprovalPage page) {
        createControl(toolkit, parent, page);
    }

    private void createControl(FormToolkit toolkit, Composite parent, ApprovalPage page) {
        group = SWTFactory.createExpandableGroup(parent, "Approval Actions", 2, true);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button approveBtn = toolkit.createButton(group, "Approve & Apply Changes", SWT.PUSH);
        approveBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        approveBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { page.handleApprove(); }
        });

        Button rejectBtn = toolkit.createButton(group, "Reject & Abort", SWT.PUSH);
        rejectBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        rejectBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { page.handleReject(); }
        });
    }
}
