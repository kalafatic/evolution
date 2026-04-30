package eu.kalafatic.evolution.view.editors.pages.taskstack;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;

import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.view.editors.pages.TaskStackPage;

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
    private ListViewer attachmentsViewer;
    private List<String> attachmentsList;

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
        gridData.widthHint = 600;
        container.setLayoutData(gridData);
        
        createLabel(container, "Name:");
        nameText = createText(container, task.getName());

        createLabel(container, "Type:");
        typeCombo = new Combo(container, SWT.READ_ONLY);
        typeCombo.setItems(new String[]{"chat", "coding", "llm", "file", "shell", "git", "maven", "approval"});
        typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        String type = task.getType();
        if (type != null) {
            typeCombo.setText(type);
        }

        createLabel(container, "Prompt:");
        promptText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        GridData gdPrompt = new GridData(GridData.FILL_BOTH);
        gdPrompt.heightHint = 100;
        promptText.setLayoutData(gdPrompt);
        promptText.setText(task.getPrompt() != null ? task.getPrompt() : "");

        createLabel(container, "Goal:");
        goalText = createText(container, task.getGoal() != null ? task.getGoal() : "");

        createLabel(container, "Description:");
        descriptionText = createText(container, task.getDescription() != null ? task.getDescription() : "");

        createLabel(container, "Approval Required:");
        approvalCheck = new Button(container, SWT.CHECK);
        approvalCheck.setSelection(task.isApprovalRequired());

        createLabel(container, "Attachments:");
        Composite attachComp = new Composite(container, SWT.NONE);
        attachComp.setLayout(new GridLayout(2, false));
        attachComp.setLayoutData(new GridData(GridData.FILL_BOTH));

        attachmentsViewer = new ListViewer(attachComp, SWT.BORDER | SWT.V_SCROLL);
        attachmentsViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        attachmentsViewer.setContentProvider(ArrayContentProvider.getInstance());
        attachmentsViewer.setInput(attachmentsList);

        Composite btnComp = new Composite(attachComp, SWT.NONE);
        btnComp.setLayout(new GridLayout(1, false));

        Button addBtn = new Button(btnComp, SWT.PUSH);
        addBtn.setText("Add");
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

        Button removeBtn = new Button(btnComp, SWT.PUSH);
        removeBtn.setText("Remove");
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

    private void createLabel(Composite parent, String text) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
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
