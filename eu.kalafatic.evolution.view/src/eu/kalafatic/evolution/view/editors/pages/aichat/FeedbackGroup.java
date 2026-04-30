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
import eu.kalafatic.evolution.model.orchestration.FeedbackLevel;
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

    // Feedback Level controls
    private Button[] levelButtons;
    private Button autoEscalateCheck;
    private Label autoStatusLabel;
    private boolean isUpdating = false;

    public FeedbackGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, AiChatPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    @Override
    protected void refreshUI() {
        if (orchestrator == null || orchestrator.getTasks().isEmpty()) return;
        isUpdating = true;
        eu.kalafatic.evolution.model.orchestration.Task task = orchestrator.getTasks().get(0);
        FeedbackLevel level = task.getFeedbackLevel();
        if (levelButtons != null) {
            for (int i = 0; i < levelButtons.length; i++) {
                levelButtons[i].setSelection(FeedbackLevel.values()[i] == level);
            }
        }
        if (autoEscalateCheck != null) {
            autoEscalateCheck.setSelection(task.isAutoEscalate());
        }

        // Update (auto) status
        if (task.isAutoEscalate() && level.getValue() > FeedbackLevel.SIMPLE_VALUE) {
            setAutoStatus(level.getName() + " (auto)");
        } else {
            setAutoStatus("");
        }

        isUpdating = false;
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

        // 4. Feedback Level Controls (Always Visible inside the group)
        Composite levelBox = toolkit.createComposite(group);
        org.eclipse.swt.layout.GridLayout levelLayout = new org.eclipse.swt.layout.GridLayout(4, false);
        levelLayout.marginHeight = 2; levelLayout.marginWidth = 0;
        levelBox.setLayout(levelLayout);
        levelBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        toolkit.createLabel(levelBox, "Feedback Depth:");

        Composite segmentedControl = toolkit.createComposite(levelBox);
        org.eclipse.swt.layout.RowLayout rowLayout = new org.eclipse.swt.layout.RowLayout(SWT.HORIZONTAL);
        rowLayout.spacing = 0;
        rowLayout.marginHeight = 0;
        segmentedControl.setLayout(rowLayout);

        FeedbackLevel[] levels = FeedbackLevel.values();
        levelButtons = new Button[levels.length];

        for (int i = 0; i < levels.length; i++) {
            final FeedbackLevel level = levels[i];
            levelButtons[i] = toolkit.createButton(segmentedControl, level.getName(), SWT.TOGGLE);
            final int index = i;
            levelButtons[i].addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (isUpdating) return;
                    updateLevelSelection(index);
                    page.handleFeedbackLevelChange(level);
                }
            });
        }

        autoEscalateCheck = toolkit.createButton(levelBox, "Auto Escalation", SWT.CHECK);
        autoEscalateCheck.setToolTipText("Automatically increase feedback level during failures.");
        autoEscalateCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (isUpdating) return;
                if (orchestrator != null && !orchestrator.getTasks().isEmpty()) {
                    orchestrator.getTasks().get(0).setAutoEscalate(autoEscalateCheck.getSelection());
                    editor.setDirty(true);
                }
            }
        });

        autoStatusLabel = toolkit.createLabel(levelBox, "");
        autoStatusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        autoStatusLabel.setForeground(group.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));

        // Ensure boxes are hidden initially, but the group itself might be visible now due to Level control
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

    private void updateLevelSelection(int selectedIndex) {
        isUpdating = true;
        for (int i = 0; i < levelButtons.length; i++) {
            levelButtons[i].setSelection(i == selectedIndex);
        }
        isUpdating = false;
    }

    public void setAutoStatus(String text) {
        if (autoStatusLabel != null && !autoStatusLabel.isDisposed()) {
            autoStatusLabel.setText(text != null ? text : "");
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

        // Group is always visible now as it contains Feedback Depth controls
        group.setVisible(true);
        setExclude(group, false);

        if (group.getParent() instanceof Section) {
            Section section = (Section) group.getParent();
            section.setVisible(true);
            Object layoutData = section.getLayoutData();
            if (layoutData instanceof GridData) {
                ((GridData) layoutData).exclude = false;
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
