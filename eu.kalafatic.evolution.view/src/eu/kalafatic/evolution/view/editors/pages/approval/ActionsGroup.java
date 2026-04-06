package eu.kalafatic.evolution.view.editors.pages.approval;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import eu.kalafatic.evolution.view.editors.pages.ApprovalPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class ActionsGroup {
    private Group group;
    private ApprovalPage page;

    public ActionsGroup(Composite parent, ApprovalPage page) {
        this.page = page;
        createControl(parent);
    }

    private void createControl(Composite parent) {
        group = SWTFactory.createGroup(parent, "Review Actions", 2);
        Button approveBtn = SWTFactory.createButton(group, "Approve & Apply", 150);
        approveBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { page.handleApprove(); }
        });

        Button rejectBtn = SWTFactory.createButton(group, "Reject & Request Changes", 200);
        rejectBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { page.handleReject(); }
        });
    }
}
