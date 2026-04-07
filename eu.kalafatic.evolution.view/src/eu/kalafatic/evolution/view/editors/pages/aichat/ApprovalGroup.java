package eu.kalafatic.evolution.view.editors.pages.aichat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class ApprovalGroup {
    private Composite group;
    private Label approvalLabel;
    private AiChatPage page;

    public ApprovalGroup(Composite parent, AiChatPage page) {
        this.page = page;
        createControl(parent);
    }

    private void createControl(Composite parent) {
        group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(3, false));
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setVisible(false);
        ((GridData) group.getLayoutData()).exclude = true;

        approvalLabel = new Label(group, SWT.NONE);
        approvalLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button approveButton = SWTFactory.createButton(group, "Approve");
        approveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.provideApproval(true);
            }
        });

        Button rejectButton = SWTFactory.createButton(group, "Reject");
        rejectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.provideApproval(false);
            }
        });

        Button reviewButton = SWTFactory.createButton(group, "Review");
        reviewButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.handleReview();
            }
        });
    }

    public void show(String message) {
        if (group.isDisposed()) return;
        approvalLabel.setText(message);
        group.setVisible(true);
        ((GridData) group.getLayoutData()).exclude = false;
    }

    public void hide() {
        if (group.isDisposed()) return;
        group.setVisible(false);
        ((GridData) group.getLayoutData()).exclude = true;
    }
}
