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

        satisfactionBox = SWTFactory.createComposite(group, 5);

        SWTFactory.createLabel(satisfactionBox, "Rate Session (1-5):");
        satisfactionScale = new Scale(satisfactionBox, SWT.HORIZONTAL);
        satisfactionScale.setMinimum(1);
        satisfactionScale.setMaximum(5);
        satisfactionScale.setSelection(3);
        GridData gd=new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        satisfactionScale.setLayoutData(gd);
        SWTFactory.createLabel(satisfactionBox); // Spacer

        SWTFactory.createLabel(satisfactionBox, "Session Feedback:");
        satisfactionCommentsText = SWTFactory.createText(satisfactionBox, "", SWT.BORDER | SWT.SINGLE);  
        gd=new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        satisfactionCommentsText.setLayoutData(gd);
        SWTFactory.createLabel(satisfactionBox); // Spacer
        
        promptLabel = SWTFactory.createLabel(satisfactionBox, "", SWT.WRAP);
        inputText = SWTFactory.createText(satisfactionBox, "", SWT.BORDER);       
        gd=new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        inputText.setLayoutData(gd);

        Button submitButton = SWTFactory.createButton(satisfactionBox, "Submit");
        submitButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.provideInput(inputText.getText());
                inputText.setText("");
            }
        });


        approvalBox = SWTFactory.createComposite(group, 5);
        
        Button submitSatButton = SWTFactory.createButton(approvalBox, "Submit Feedback");
        submitSatButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.submitFeedback(satisfactionScale.getSelection(), satisfactionCommentsText.getText());
            }
        });

        Button peerReviewBtn = SWTFactory.createButton(approvalBox, "Peer Review");
        peerReviewBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.handleReview();
            }
        });
        
        Button approveButton = SWTFactory.createButton(approvalBox, "Approve");
        approveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.provideApproval(true);
            }
        });

        Button rejectButton = SWTFactory.createButton(approvalBox, "Reject");
        rejectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.provideApproval(false);
            }
        });

        Button reviewButton = SWTFactory.createButton(approvalBox, "Review");
        reviewButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.handleReview();
            }
        });
    }

    public void showSatisfaction(boolean visible) {
        if (satisfactionBox.isDisposed()) return;
        satisfactionBox.setVisible(visible);
        ((GridData) satisfactionBox.getLayoutData()).exclude = !visible;
        if (visible) {
            page.expandFeedbackSection();
        }
        updateVisibility();
    }

    public void showApproval(String message) {
        if (approvalBox.isDisposed()) return;
        if (message != null) {
            message = message.replaceAll("(?s)<think>.*?</think>", "").trim();
        }
       
        approvalBox.setVisible(true);
        ((GridData) approvalBox.getLayoutData()).exclude = false;
        page.expandFeedbackSection();
        updateVisibility();
    }

    public void hideApproval() {
        if (approvalBox.isDisposed()) return;
        approvalBox.setVisible(false);
        ((GridData) approvalBox.getLayoutData()).exclude = true;
        updateVisibility();
    }

    public void showInput(String message) {
        if (inputBox.isDisposed()) return;
        if (message != null) {
            message = message.replaceAll("(?s)<think>.*?</think>", "").trim();
        }
        promptLabel.setText(message);
        inputBox.setVisible(true);
        ((GridData) inputBox.getLayoutData()).exclude = false;
        inputText.setFocus();
        page.expandFeedbackSection();
        updateVisibility();
    }

    public void hideInput() {
        if (inputBox.isDisposed()) return;
        inputBox.setVisible(false);
        ((GridData) inputBox.getLayoutData()).exclude = true;
        updateVisibility();
    }

    public void updateVisibility() {
        if (section == null || section.isDisposed()) return;
        boolean anyVisible = satisfactionBox.getVisible() || approvalBox.getVisible() || inputBox.getVisible();
        section.setVisible(anyVisible);
        ((GridData) section.getLayoutData()).exclude = !anyVisible;
        if (!anyVisible) {
            section.setExpanded(false);
        }
        page.updateScrolledContent();
    }

    public boolean isSatisfactionVisible() { return satisfactionBox != null && !satisfactionBox.isDisposed() && satisfactionBox.getVisible(); }
    public boolean isApprovalVisible() { return approvalBox != null && !approvalBox.isDisposed() && approvalBox.getVisible(); }
    public boolean isInputVisible() { return inputBox != null && !inputBox.isDisposed() && inputBox.getVisible(); }
}
