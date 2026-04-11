package eu.kalafatic.evolution.view.editors.pages.peerreview;

import java.io.File;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.review.model.ReviewSession;
import eu.kalafatic.evolution.controller.review.service.PeerReviewService;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class CommentsGroup extends AEvoGroup {
    private Text commentsText;
    private Label statusLabel;

    public CommentsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Review Actions", 1, true);
        GridData gd = new GridData(GridData.FILL_VERTICAL);
        gd.widthHint = 250;
        group.setLayoutData(gd);

        statusLabel = toolkit.createLabel(group, "Status: OPEN");
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        toolkit.createLabel(group, "Comments:");
        commentsText = toolkit.createText(group, "", SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
        commentsText.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite btnComp = toolkit.createComposite(group);
        btnComp.setLayout(new GridLayout(1, true));
        btnComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button approveBtn = toolkit.createButton(btnComp, "Approve & Commit", SWT.PUSH);
        approveBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        approveBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                handleApprove();
            }
        });

        Button changesBtn = toolkit.createButton(btnComp, "Request Changes", SWT.PUSH);
        changesBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        changesBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                handleRequestChanges();
            }
        });

        Button rejectBtn = toolkit.createButton(btnComp, "Reject", SWT.PUSH);
        rejectBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        rejectBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                handleReject();
            }
        });
    }

    private void handleApprove() {
        try {
            ReviewSession session = PeerReviewService.getInstance().getActiveSession();
            if (session == null) {
                session = PeerReviewService.getInstance().createSession(null, "HEAD");
            }
            IProject project = null;
            if (editor.getEditorInput() instanceof IFileEditorInput) {
                project = ((IFileEditorInput) editor.getEditorInput()).getFile().getProject();
            }
            File projectRoot = project != null ? project.getLocation().toFile() : null;
            PeerReviewService.getInstance().approve(session, projectRoot, commentsText.getText());
            MessageDialog.openInformation(group.getShell(), "Approved", "Review approved and changes committed.");
            updateUI();
        } catch (Exception e) {
            MessageDialog.openError(group.getShell(), "Error", "Failed to approve: " + e.getMessage());
        }
    }

    private void handleRequestChanges() {
        ReviewSession session = PeerReviewService.getInstance().getActiveSession();
        if (session != null) {
            PeerReviewService.getInstance().requestChanges(session);
            updateUI();
        }
    }

    private void handleReject() {
        ReviewSession session = PeerReviewService.getInstance().getActiveSession();
        if (session != null) {
            PeerReviewService.getInstance().reject(session);
            updateUI();
        }
    }

    @Override
    public void refreshUI() {
        ReviewSession session = PeerReviewService.getInstance().getActiveSession();
        if (session != null && statusLabel != null && !statusLabel.isDisposed()) {
            statusLabel.setText("Status: " + session.getDecision().toString());
        }
    }
}
