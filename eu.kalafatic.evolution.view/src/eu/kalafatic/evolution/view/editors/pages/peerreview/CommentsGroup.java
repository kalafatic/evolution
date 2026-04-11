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

import eu.kalafatic.evolution.controller.review.service.PeerReviewService;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.ReviewSession;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class CommentsGroup extends AEvoGroup {
    private Text commentsText;
    private Label statusLabel;
    private Label lineLabel;
    private int currentLine = -1;
    private String currentFile = "";

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

        lineLabel = toolkit.createLabel(group, "Selected Line: None");
        lineLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        toolkit.createLabel(group, "Add Comment:");
        commentsText = toolkit.createText(group, "", SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
        commentsText.setLayoutData(new GridData(GridData.FILL_BOTH));

        Button addCommentBtn = toolkit.createButton(group, "Add Line Comment", SWT.PUSH);
        addCommentBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        addCommentBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                handleAddLineComment();
            }
        });

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
            IProject project = null;
            if (editor.getEditorInput() instanceof IFileEditorInput) {
                project = ((IFileEditorInput) editor.getEditorInput()).getFile().getProject();
            }
            File projectRoot = project != null ? project.getLocation().toFile() : null;

            ReviewSession session = PeerReviewService.getInstance().getActiveSession();
            if (session == null && projectRoot != null) {
                session = PeerReviewService.getInstance().createSession(projectRoot, "HEAD");
            }

            if (session == null) {
                MessageDialog.openError(group.getShell(), "Error", "No active session and project root not found.");
                return;
            }

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

    private void handleAddLineComment() {
        ReviewSession session = PeerReviewService.getInstance().getActiveSession();
        if (session == null) {
            try {
                IProject project = null;
                if (editor.getEditorInput() instanceof IFileEditorInput) {
                    project = ((IFileEditorInput) editor.getEditorInput()).getFile().getProject();
                }
                File projectRoot = project != null ? project.getLocation().toFile() : null;
                if (projectRoot != null) {
                    session = PeerReviewService.getInstance().createSession(projectRoot, "HEAD");
                }
            } catch (Exception e) {}
        }
        if (session != null && currentLine != -1) {
            PeerReviewService.getInstance().addComment(session, currentFile, currentLine, commentsText.getText());
            commentsText.setText("");
            MessageDialog.openInformation(group.getShell(), "Comment Added", "Line comment added to model.");
            updateUI();
        } else {
            MessageDialog.openWarning(group.getShell(), "Warning", "Please select a line in the diff first.");
        }
    }

    private void handleReject() {
        ReviewSession session = PeerReviewService.getInstance().getActiveSession();
        if (session != null) {
            PeerReviewService.getInstance().reject(session);
            updateUI();
        }
    }

    public void setSelectedLine(String filePath, int lineNum) {
        this.currentFile = filePath;
        this.currentLine = lineNum;
        if (lineLabel != null && !lineLabel.isDisposed()) {
            lineLabel.setText("Selected Line: " + lineNum + " in " + filePath);
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
