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
import eu.kalafatic.utils.factories.GUIFactory;
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
    
    private Label expansionValueLabel;
    private Button autoEscalateCheck;
    private Label autoStatusLabel;
    private boolean isUpdating = false;
	private Composite feedbackBox;

    public FeedbackGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, AiChatPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    @Override
    protected void refreshUI() {
        if (orchestrator == null || orchestrator.getTasks().isEmpty()) return;
        isUpdating = true;
        try {
            eu.kalafatic.evolution.model.orchestration.Task task = orchestrator.getTasks().get(0);
            FeedbackLevel level = task.getFeedbackLevel();
            if (levelButtons != null) {
                for (int i = 0; i < levelButtons.length; i++) {
                    setSelectionSafe(levelButtons[i], FeedbackLevel.values()[i] == level);
                }
            }
            setSelectionSafe(autoEscalateCheck, task.isAutoEscalate());

            eu.kalafatic.evolution.view.projection.RuntimeProjection projection = eu.kalafatic.evolution.view.projection.ProjectionService.getInstance().getProjection(page.getCurrentSessionName());
                      

            // Update (auto) status
            if (task.isAutoEscalate() && level.getValue() > FeedbackLevel.SIMPLE_VALUE) {
                setAutoStatus(level.getName() + " (auto)");
            } else {
                setAutoStatus("");
            }       
        } finally {
            isUpdating = false;
        }
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Session Interaction & Feedback", 2, true);
        
        Composite buttonBox = GUIFactory.INSTANCE.createComposite(group, 2);
        
        // 2. Approval Box
        approvalBox = GUIFactory.INSTANCE.createComposite(buttonBox, 5);
        GUIFactory.INSTANCE.createLabel(approvalBox, "Decision Required:");
        
        Button approveButton = GUIFactory.INSTANCE.createButton(approvalBox, "Approve");
        approveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.provideApproval(true);
            }
        });

        Button rejectButton = GUIFactory.INSTANCE.createButton(approvalBox, "Reject");
        rejectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.provideApproval(false);
            }
        });

        Button peerReviewBtn = GUIFactory.INSTANCE.createButton(approvalBox, "Peer Review");
        peerReviewBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.handleReview();
            }
        });
        GUIFactory.INSTANCE.createLabel(approvalBox); // Spacer
        
        
        
     // 3. Feedback Level Controls (Always Visible inside the group)
        Composite levelBox = GUIFactory.INSTANCE.createComposite(group, 3);
       

        GUIFactory.INSTANCE.createLabel(levelBox, "Feedback Depth:");

        Composite segmentedControl = GUIFactory.INSTANCE.createComposite(levelBox, 4, SWT.BORDER);
        
        FeedbackLevel[] levels = FeedbackLevel.values();
        levelButtons = new Button[levels.length];

        for (int i = 0; i < levels.length; i++) {
            final FeedbackLevel level = levels[i];
            levelButtons[i] = GUIFactory.INSTANCE.createButton(segmentedControl, level.getName(), SWT.CHECK, GUIFactory.BUTTON_WIDTH);
            if (level == FeedbackLevel.FULL) levelButtons[i].setSelection(true);
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

        autoEscalateCheck = GUIFactory.INSTANCE.createButton(levelBox, "Auto Escalation", SWT.CHECK, GUIFactory.BUTTON_WIDTH);
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
        

        // 1. Satisfaction Box
        satisfactionBox = GUIFactory.INSTANCE.createComposite(group, 2, SWT.BORDER);
        ((GridData)satisfactionBox.getLayoutData()).widthHint = 200; // Set a fixed width for the satisfaction box
        GUIFactory.INSTANCE.createLabel(satisfactionBox, "Rate Session (1-10):");
        satisfactionScale = new Scale(satisfactionBox, SWT.NO);
       
        satisfactionScale.setMinimum(1);
        satisfactionScale.setMaximum(10);
        satisfactionScale.setIncrement(1);
        satisfactionScale.setSelection(5);       
        
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);        
        gridData.widthHint = 100; // Set a fixed width for the satisfaction box
        satisfactionScale.setLayoutData(gridData);      
        
        satisfactionBox.layout(true, true);
        
        feedbackBox = GUIFactory.INSTANCE.createComposite(group, 3);
        GUIFactory.INSTANCE.createLabel(feedbackBox, "Session Feedback:");
        satisfactionCommentsText = GUIFactory.INSTANCE.createText(feedbackBox, "", SWT.BORDER | SWT.SINGLE);
        Button submitSatButton = GUIFactory.INSTANCE.createButton(feedbackBox, "Submit Feedback");
        submitSatButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.submitFeedback(satisfactionScale.getSelection(), satisfactionCommentsText.getText());
            }
        });
        
    }

    private void updateLevelSelection(int selectedIndex) {
        isUpdating = true;
        for (int i = 0; i < levelButtons.length; i++) {
            setSelectionSafe(levelButtons[i], i == selectedIndex);
        }
        isUpdating = false;
    }

    public void setAutoStatus(String text) {
        if (autoStatusLabel != null && !autoStatusLabel.isDisposed()) {
            setTextSafe(autoStatusLabel, text);
        }
    }
}
