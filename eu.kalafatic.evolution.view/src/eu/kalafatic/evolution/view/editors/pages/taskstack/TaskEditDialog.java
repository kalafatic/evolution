package eu.kalafatic.evolution.view.editors.pages.taskstack;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;

import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.view.editors.pages.TaskStackPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

import java.util.ArrayList;
import java.util.List;

public class TaskEditDialog extends Dialog {
    private Task task;
    private TaskStackPage page;

    private Text nameText;
    private Combo typeCombo;
    private Text promptText;
    private Text goalText;
    private Text descriptionText;
    private Button approvalCheck;
    private Button iterativeCheck;
    private Button selfIterativeCheck;
    private Button darwinCheck;
    private Button autoGitCheck;
    private Button stepModeCheck;
 
    private ListViewer attachmentsViewer;
    private List<String> attachmentsList;
	private Spinner maxIterationsSpinner;

    public TaskEditDialog(Shell parentShell, Task task, TaskStackPage page) {
        super(parentShell);
        this.task = task;
        this.page = page;
        this.attachmentsList = new ArrayList<>(task.getAttachments());
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Edit Task: " + task.getName());
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(2, false));      
        GridData gridData = new GridData();
        gridData.widthHint = 700;
        container.setLayoutData(gridData);
        
        SWTFactory.createLabel(container, "Name:");
        nameText = createText(container, task.getName());

        SWTFactory.createLabel(container, "Type:");
        typeCombo = new Combo(container, SWT.READ_ONLY);
        typeCombo.setItems(new String[]{"chat", "coding", "llm", "file", "shell", "git", "maven", "approval"});
        typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        String type = task.getType();
        if (type != null) {
            typeCombo.setText(type);
        }

        SWTFactory.createLabel(container, "Prompt:");
        promptText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        GridData gdPrompt = new GridData(GridData.FILL_BOTH);
        gdPrompt.heightHint = 100;
        promptText.setLayoutData(gdPrompt);
        promptText.setText(task.getPrompt() != null ? task.getPrompt() : "");

        SWTFactory.createLabel(container, "Goal:");
        goalText = createText(container, task.getGoal() != null ? task.getGoal() : "");

        SWTFactory.createLabel(container, "Description:");
        descriptionText = createText(container, task.getDescription() != null ? task.getDescription() : "");

        SWTFactory.createLabel(container, "Execution Controls:");
        Composite controlsComp = new Composite(container, SWT.NONE);
        controlsComp.setLayout(new GridLayout(4, true));
        controlsComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        approvalCheck = new Button(controlsComp, SWT.CHECK);
        approvalCheck.setText("Approval Required");
        approvalCheck.setSelection(task.isApprovalRequired());

        iterativeCheck = new Button(controlsComp, SWT.CHECK);
        iterativeCheck.setText("Iterative Mode");
        iterativeCheck.setSelection(task.isIterativeMode());

        selfIterativeCheck = new Button(controlsComp, SWT.CHECK);
        selfIterativeCheck.setText("Self-Dev Mode");
        selfIterativeCheck.setSelection(task.isSelfIterativeMode());

        darwinCheck = new Button(controlsComp, SWT.CHECK);
        darwinCheck.setText("Darwin Mode");
        darwinCheck.setSelection(task.isDarwinMode());

        autoGitCheck = new Button(controlsComp, SWT.CHECK);
        autoGitCheck.setText("Git Automation");
        autoGitCheck.setSelection(task.isGitAutomation());

        stepModeCheck = new Button(controlsComp, SWT.CHECK);
        stepModeCheck.setText("Step Mode");
        stepModeCheck.setSelection(task.isStepMode());
        
        SWTFactory.createLabel(controlsComp, "Max Iterations:");
        
        maxIterationsSpinner = new org.eclipse.swt.widgets.Spinner(controlsComp, SWT.BORDER);
        maxIterationsSpinner.setMinimum(1);
        maxIterationsSpinner.setMaximum(100);
        maxIterationsSpinner.setIncrement(1);
        maxIterationsSpinner.setSelection(task.getMaxIterations());
        maxIterationsSpinner.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	task.setMaxIterations(maxIterationsSpinner.getSelection());
            }
        });
        
        
       

        SWTFactory.createLabel(container, "Attachments:");
        Composite attachComp = SWTFactory.createComposite(container, SWT.NONE, 2);

        attachmentsViewer = new ListViewer(attachComp, SWT.BORDER | SWT.V_SCROLL);
        attachmentsViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        attachmentsViewer.setContentProvider(ArrayContentProvider.getInstance());
        attachmentsViewer.setInput(attachmentsList);

        Composite btnComp = SWTFactory.createComposite(attachComp);

        Button addBtn = SWTFactory.createButton(btnComp, "Add");
        addBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                InputDialog dlg = new InputDialog(getShell(), "Add Attachment", "Enter file path:", "", null);
                if (dlg.open() == Window.OK) {
                    attachmentsList.add(dlg.getValue());
                    attachmentsViewer.refresh();
                }
            }
        });

        Button removeBtn =SWTFactory.createButton(btnComp, "Remove");
        removeBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                int index = attachmentsViewer.getList().getSelectionIndex();
                if (index != -1) {
                    attachmentsList.remove(index);
                    attachmentsViewer.refresh();
                }
            }
        });

        return container;
    }

    private Text createText(Composite parent, String initialValue) {
        Text text = new Text(parent, SWT.BORDER);
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        text.setText(initialValue);
        return text;
    }

    @Override
    protected void okPressed() {
        task.setName(nameText.getText());
        task.setType(typeCombo.getText());
        task.setPrompt(promptText.getText());
        task.setGoal(goalText.getText());
        task.setDescription(descriptionText.getText());
        task.setApprovalRequired(approvalCheck.getSelection());
        task.setIterativeMode(iterativeCheck.getSelection());
        task.setSelfIterativeMode(selfIterativeCheck.getSelection());
        task.setDarwinMode(darwinCheck.getSelection());
        task.setGitAutomation(autoGitCheck.getSelection());
        task.setStepMode(stepModeCheck.getSelection());
        try {
            task.setMaxIterations(maxIterationsSpinner.getSelection());
        } catch (NumberFormatException e) {}
        task.getAttachments().clear();
        task.getAttachments().addAll(attachmentsList);

        page.setDirty(true);
        super.okPressed();
    }

    @Override
    protected boolean isResizable() {
        return true;
    }
}
