package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;

import java.util.ArrayList;
import java.util.List;

public class TaskStackPage extends SharedScrolledComposite {

    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private FormToolkit toolkit;
    private Composite body;
    private Composite tasksContainer;
    private Combo executionModeCombo;
    private boolean isUpdating = false;

    private List<TaskRow> taskRows = new ArrayList<>();

    private class TaskRow {
        Task task;
        Button selectedCheck;
        Text nameText;
        Text timeText;
        Label statusLabel;

        TaskRow(Task task, Button selectedCheck, Text nameText, Text timeText, Label statusLabel) {
            this.task = task;
            this.selectedCheck = selectedCheck;
            this.nameText = nameText;
            this.timeText = timeText;
            this.statusLabel = statusLabel;
        }
    }

    private Adapter modelAdapter = new EContentAdapter() {
        @Override
        public void notifyChanged(Notification notification) {
            super.notifyChanged(notification);
            if (!isUpdating) {
                Display.getDefault().asyncExec(() -> {
                    if (!isDisposed()) {
                        if (notification.getEventType() == Notification.ADD || notification.getEventType() == Notification.REMOVE) {
                            updateUIFromModel();
                        }
                    }
                });
            }
        }
    };

    public TaskStackPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.V_SCROLL | SWT.H_SCROLL);
        this.editor = editor;
        this.orchestrator = orchestrator;
        this.toolkit = new FormToolkit(parent.getDisplay());

        setExpandHorizontal(true);
        setExpandVertical(true);

        body = toolkit.createComposite(this);
        body.setLayout(new GridLayout(1, false));
        setContent(body);

        createGlobalControls();
        createTasksSection();

        setOrchestrator(orchestrator);
        startTimer();
    }

    private void startTimer() {
        Display.getDefault().timerExec(60000, new Runnable() {
            @Override
            public void run() {
                if (isDisposed()) return;
                checkScheduledTasks();
                Display.getDefault().timerExec(60000, this);
            }
        });
    }

    private void checkScheduledTasks() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
        String now = sdf.format(new java.util.Date());

        for (TaskRow row : taskRows) {
            if (row.task.getStatus() == TaskStatus.PENDING && now.equals(row.task.getScheduledTime())) {
                runTask(row.task);
            }
        }
    }

    private void createGlobalControls() {
        Composite header = SWTFactory.createExpandableGroup(toolkit, body, "Global Actions", 4, true);

        Button selectAllBtn = SWTFactory.createButton(header, "Select All");
        selectAllBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                selectAll(true);
            }
        });

        Button unselectAllBtn = SWTFactory.createButton(header, "Unselect All");
        unselectAllBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                selectAll(false);
            }
        });

        SWTFactory.createLabel(header, "Mode:");
        executionModeCombo = SWTFactory.createCombo(header);
        executionModeCombo.add("Sequential");
        executionModeCombo.add("Parallel");
        executionModeCombo.select(0);

        Button executeBtn = SWTFactory.createButton(body, "Execute Selected", 150);
        executeBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                executeSelected();
            }
        });

        Button addTaskBtn = SWTFactory.createButton(body, "Add New Task Idea", 150);
        addTaskBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                addNewTask();
            }
        });
    }

    private void createTasksSection() {
        tasksContainer = SWTFactory.createExpandableGroup(toolkit, body, "Task/Prompt Stack", 1, true);
        tasksContainer.setLayout(new GridLayout(1, false));
        // Rows will be added here
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        if (this.orchestrator != null) {
            this.orchestrator.eAdapters().remove(modelAdapter);
        }
        this.orchestrator = orchestrator;
        if (this.orchestrator != null) {
            this.orchestrator.eAdapters().add(modelAdapter);
        }
        updateUIFromModel();
    }

    public void updateUIFromModel() {
        if (isUpdating || orchestrator == null || tasksContainer == null || tasksContainer.isDisposed()) return;
        isUpdating = true;

        for (org.eclipse.swt.widgets.Control child : tasksContainer.getChildren()) {
            child.dispose();
        }
        taskRows.clear();

        for (Task task : orchestrator.getTasks()) {
            createTaskRow(task);
        }

        tasksContainer.layout(true, true);
        body.layout(true, true);
        reflow(true);

        isUpdating = false;
    }

    private void createTaskRow(Task task) {
        Composite row = toolkit.createComposite(tasksContainer);
        row.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        row.setLayout(new GridLayout(5, false));

        Button check = new Button(row, SWT.CHECK);
        check.setSelection(task.isSelected());
        check.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                task.setSelected(check.getSelection());
                editor.setDirty(true);
            }
        });

        Text nameText = SWTFactory.createText(row);
        nameText.setText(task.getName() != null ? task.getName() : "");
        nameText.setMessage("Task Idea / Prompt");
        nameText.addModifyListener(e -> {
            task.setName(nameText.getText());
            editor.setDirty(true);
        });

        SWTFactory.createLabel(row, "Time:");
        Text timeText = new Text(row, SWT.BORDER);
        GridData gd = new GridData();
        gd.widthHint = 80;
        timeText.setLayoutData(gd);
        timeText.setText(task.getScheduledTime() != null ? task.getScheduledTime() : "");
        timeText.setMessage("e.g. 13:00");
        timeText.addModifyListener(e -> {
            task.setScheduledTime(timeText.getText());
            editor.setDirty(true);
        });

        Label statusLabel = new Label(row, SWT.NONE);
        statusLabel.setText(task.getStatus().toString());
        updateStatusColor(statusLabel, task.getStatus());

        taskRows.add(new TaskRow(task, check, nameText, timeText, statusLabel));
    }

    private void updateStatusColor(Label label, TaskStatus status) {
        switch (status) {
            case PENDING: label.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK)); break;
            case RUNNING: label.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLUE)); break;
            case DONE: label.setForeground(getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN)); break;
            case FAILED: label.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED)); break;
            default: break;
        }
    }

    private void selectAll(boolean select) {
        for (TaskRow row : taskRows) {
            row.selectedCheck.setSelection(select);
            row.task.setSelected(select);
        }
        editor.setDirty(true);
    }

    private void addNewTask() {
        Task newTask = OrchestrationFactory.eINSTANCE.createTask();
        newTask.setName("New Prompt Idea");
        newTask.setStatus(TaskStatus.PENDING);
        newTask.setSelected(true);
        orchestrator.getTasks().add(newTask);
        editor.setDirty(true);
    }

    private void executeSelected() {
        boolean parallel = "Parallel".equals(executionModeCombo.getText());
        List<Task> selectedTasks = new ArrayList<>();
        for (TaskRow row : taskRows) {
            if (row.selectedCheck.getSelection()) {
                selectedTasks.add(row.task);
            }
        }

        if (selectedTasks.isEmpty()) return;

        if (parallel) {
            for (Task t : selectedTasks) {
                runTask(t);
            }
        } else {
            runTasksSequentially(selectedTasks, 0);
        }
    }

    private void runTasksSequentially(List<Task> tasks, int index) {
        if (index >= tasks.size()) return;
        Task t = tasks.get(index);
        t.setStatus(TaskStatus.RUNNING);
        updateUIFromModel(); // Refresh to show running state

        Display.getDefault().timerExec(1500, () -> {
            if (!isDisposed()) {
                t.setStatus(TaskStatus.DONE);
                updateUIFromModel();
                runTasksSequentially(tasks, index + 1);
            }
        });
    }

    private void runTask(Task t) {
        t.setStatus(TaskStatus.RUNNING);
        updateUIFromModel();
        Display.getDefault().timerExec(2000, () -> {
            if (!isDisposed()) {
                t.setStatus(TaskStatus.DONE);
                updateUIFromModel();
            }
        });
    }

    @Override
    public void dispose() {
        if (orchestrator != null) {
            orchestrator.eAdapters().remove(modelAdapter);
        }
        if (toolkit != null) {
            toolkit.dispose();
        }
        super.dispose();
    }
}
