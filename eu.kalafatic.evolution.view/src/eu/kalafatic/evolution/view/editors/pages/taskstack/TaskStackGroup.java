package eu.kalafatic.evolution.view.editors.pages.taskstack;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import eu.kalafatic.evolution.view.editors.pages.TaskStackPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

public class TaskStackGroup extends AEvoGroup {
    private FormToolkit toolkit;
    private TaskStackPage page;

    public TaskStackGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, TaskStackPage page) {
        super(editor, orchestrator);
        this.toolkit = toolkit;
        this.page = page;
        createControl(parent);
    }

    @Override
    protected void refreshUI() {
        if (page != null) {
            page.updateUIFromModel();
        }
    }

    private void createControl(Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Task/Prompt Stack", 1, true);
        group.setLayout(new GridLayout(1, false));
    }

    public void clear() {
        for (org.eclipse.swt.widgets.Control child : group.getChildren()) {
            child.dispose();
        }
    }

    public void createTaskRow(Task task) {
        Composite row = toolkit.createComposite(group);
        row.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        row.setLayout(new GridLayout(5, false));

        Button check = new Button(row, SWT.CHECK);
        check.setSelection(task.isSelected());
        check.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                task.setSelected(check.getSelection());
                page.setDirty(true);
            }
        });
        page.registerTaskRowCheck(task, check);

        Text nameText = SWTFactory.createText(row);
        nameText.setText(task.getName() != null ? task.getName() : "");
        nameText.setMessage("Task Idea / Prompt");
        nameText.addModifyListener(e -> {
            task.setName(nameText.getText());
            page.setDirty(true);
        });

        SWTFactory.createLabel(row, "Time:");
        Text timeText = new Text(row, SWT.BORDER);
        GridData gd = new GridData();
        gd.widthHint = 53;
        timeText.setLayoutData(gd);
        timeText.setText(task.getScheduledTime() != null ? task.getScheduledTime() : "");
        timeText.setMessage("e.g. 13:00");
        timeText.addModifyListener(e -> {
            task.setScheduledTime(timeText.getText());
            page.setDirty(true);
        });

        Label statusLabel = new Label(row, SWT.NONE);
        statusLabel.setText(task.getStatus().toString());
        updateStatusColor(statusLabel, task.getStatus());

        page.registerTaskRow(task, check, nameText, timeText, statusLabel);
    }

    private void updateStatusColor(Label label, TaskStatus status) {
        switch (status) {
            case PENDING: label.setForeground(label.getDisplay().getSystemColor(SWT.COLOR_BLACK)); break;
            case RUNNING: label.setForeground(label.getDisplay().getSystemColor(SWT.COLOR_BLUE)); break;
            case DONE: label.setForeground(label.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN)); break;
            case FAILED: label.setForeground(label.getDisplay().getSystemColor(SWT.COLOR_RED)); break;
            default: break;
        }
    }

    public void layout() {
        group.layout(true, true);
    }
}
