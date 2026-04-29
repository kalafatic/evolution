package eu.kalafatic.evolution.view.editors.pages.aichat;

import org.eclipse.swt.SWT;
import org.json.JSONArray;
import org.json.JSONObject;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

public class FeedbackGroup extends AEvoGroup {
    private AiChatPage page;
    private Section section;

    // Satisfaction controls
    private Composite satisfactionBox;
    private Scale satisfactionScale;
    private Text satisfactionCommentsText;

    // Approval controls
    private Composite approvalBox;

    // Input controls
    private Composite inputBox;
    private Label promptLabel;
    private Text inputText;

    public FeedbackGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, AiChatPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    @Override
    protected void refreshUI() {
        // Managed by AiChatPage specifically
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Session Interaction & Feedback", 1, false);

        // 1. Satisfaction Box
        satisfactionBox = SWTFactory.createComposite(group, 5);
        SWTFactory.createLabel(satisfactionBox, "Rate Session (1-5):");
        satisfactionScale = new Scale(satisfactionBox, SWT.HORIZONTAL);
        satisfactionScale.setMinimum(1);
        satisfactionScale.setMaximum(5);
        satisfactionScale.setSelection(3);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        satisfactionScale.setLayoutData(gd);
        SWTFactory.createLabel(satisfactionBox); // Spacer

        SWTFactory.createLabel(satisfactionBox, "Session Feedback:");
        satisfactionCommentsText = SWTFactory.createText(satisfactionBox, "", SWT.BORDER | SWT.SINGLE);  
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        satisfactionCommentsText.setLayoutData(gd);
        
        Button submitSatButton = toolkit.createButton(satisfactionBox, "Submit Feedback", SWT.PUSH);
        submitSatButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.submitFeedback(satisfactionScale.getSelection(), satisfactionCommentsText.getText());
            }
        });
        SWTFactory.createLabel(satisfactionBox); // Spacer

        // 2. Approval Box
        approvalBox = SWTFactory.createComposite(group, 5);
        SWTFactory.createLabel(approvalBox, "Action Required:");
        
        Button approveButton = toolkit.createButton(approvalBox, "Approve", SWT.PUSH);
        approveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.provideApproval(true);
            }
        });

        Button rejectButton = toolkit.createButton(approvalBox, "Reject", SWT.PUSH);
        rejectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.provideApproval(false);
            }
        });

        Button peerReviewBtn = toolkit.createButton(approvalBox, "Peer Review", SWT.PUSH);
        peerReviewBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.handleReview();
            }
        });
        SWTFactory.createLabel(approvalBox); // Spacer

        // 3. Input Box
        inputBox = SWTFactory.createComposite(group, 5);
        promptLabel = SWTFactory.createLabel(inputBox, "Input Required:", SWT.WRAP);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 1;
        promptLabel.setLayoutData(gd);

        inputText = SWTFactory.createText(inputBox, "", SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        inputText.setLayoutData(gd);

        Button submitButton = toolkit.createButton(inputBox, "Send", SWT.PUSH);
        submitButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.provideInput(inputText.getText());
                inputText.setText("");
            }
        });

        // Ensure all are hidden initially
        satisfactionBox.setVisible(false);
        setExclude(satisfactionBox, true);
        approvalBox.setVisible(false);
        setExclude(approvalBox, true);
        inputBox.setVisible(false);
        setExclude(inputBox, true);
    }

    public void showSatisfaction(boolean visible) {
        if (satisfactionBox == null || satisfactionBox.isDisposed()) return;
        if (visible) {
            updateVisibility(satisfactionBox);
            page.expandFeedbackSection();
        } else {
            satisfactionBox.setVisible(false);
            setExclude(satisfactionBox, true);
            updateVisibility(null);
        }
    }

    public void showApproval(String message) {
        if (approvalBox == null || approvalBox.isDisposed()) return;
        updateVisibility(approvalBox);
        page.expandFeedbackSection();
    }

    public void hideApproval() {
        if (approvalBox == null || approvalBox.isDisposed()) return;
        approvalBox.setVisible(false);
        setExclude(approvalBox, true);
        updateVisibility(null);
    }

    public void showInput(String message) {
        if (inputBox == null || inputBox.isDisposed()) return;
        if (message != null) {
            message = message.replaceAll("(?s)<think>.*?</think>", "").trim();
            if (message.length() > 100) message = message.substring(0, 97) + "...";
            promptLabel.setText(message);
        }
        updateVisibility(inputBox);
        inputText.setFocus();
        page.expandFeedbackSection();
    }

    public void hideInput() {
        if (inputBox == null || inputBox.isDisposed()) return;
        inputBox.setVisible(false);
        setExclude(inputBox, true);
        updateVisibility(null);
    }

    private void setExclude(org.eclipse.swt.widgets.Control control, boolean exclude) {
        if (control == null || control.isDisposed()) return;
        Object layoutData = control.getLayoutData();
        if (layoutData instanceof GridData) {
            ((GridData) layoutData).exclude = exclude;
        }
    }

    private void updateVisibility(Composite visibleBox) {
        if (group == null || group.isDisposed()) return;

        if (visibleBox != null) {
            // Hide others
            if (satisfactionBox != null && satisfactionBox != visibleBox) {
                satisfactionBox.setVisible(false);
                setExclude(satisfactionBox, true);
            }
            if (approvalBox != null && approvalBox != visibleBox) {
                approvalBox.setVisible(false);
                setExclude(approvalBox, true);
            }
            if (inputBox != null && inputBox != visibleBox) {
                inputBox.setVisible(false);
                setExclude(inputBox, true);
            }

            // Show this one
            visibleBox.setVisible(true);
            setExclude(visibleBox, false);
        }

        boolean anyVisible = (satisfactionBox != null && satisfactionBox.getVisible()) ||
                             (approvalBox != null && approvalBox.getVisible()) ||
                             (inputBox != null && inputBox.getVisible());

        group.setVisible(anyVisible);
        setExclude(group, !anyVisible);

        if (group.getParent() instanceof Section) {
            Section section = (Section) group.getParent();
            section.setVisible(anyVisible);
            Object layoutData = section.getLayoutData();
            if (layoutData instanceof GridData) {
                ((GridData) layoutData).exclude = !anyVisible;
            }
            if (!anyVisible) {
                section.setExpanded(false);
            } else if (!section.isExpanded()) {
                section.setExpanded(true);
            }
        }

        page.updateScrolledContent();
    }

    public void updateVisibility() {
        updateVisibility(null);
    }

    public boolean isSatisfactionVisible() { return satisfactionBox != null && !satisfactionBox.isDisposed() && satisfactionBox.getVisible(); }
    public boolean isApprovalVisible() { return approvalBox != null && !approvalBox.isDisposed() && approvalBox.getVisible(); }
    public boolean isInputVisible() { return inputBox != null && !inputBox.isDisposed() && inputBox.getVisible(); }
}
