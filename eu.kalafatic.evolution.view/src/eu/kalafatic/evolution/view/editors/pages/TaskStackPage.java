package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.taskstack.*;

import java.util.ArrayList;
import java.util.List;

public class TaskStackPage extends SharedScrolledComposite {

    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private FormToolkit toolkit;
    private Composite body;
    private boolean isUpdating = false;

    private GlobalActionsGroup globalActionsGroup;
    private TaskStackGroup taskStackGroup;

    private List<TaskRow> taskRows = new ArrayList<>();

    public class TaskRow {
        public Task task;
        public Button selectedCheck;
        public Text nameText;
        public Text timeText;
        public Label statusLabel;

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

        globalActionsGroup = new GlobalActionsGroup(toolkit, body, editor, orchestrator, this);
        taskStackGroup = new TaskStackGroup(toolkit, body, editor, orchestrator, this);

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
        if (isUpdating || orchestrator == null || body == null || body.isDisposed()) return;
        isUpdating = true;

        List<Task> tasks = orchestrator.getTasks();
        boolean needsFullRefresh = tasks.size() != taskRows.size();
        if (!needsFullRefresh) {
            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i) != taskRows.get(i).task) {
                    needsFullRefresh = true;
                    break;
                }
            }
        }

        if (needsFullRefresh) {
            taskStackGroup.clear();
            taskRows.clear();

            for (Task task : tasks) {
                taskStackGroup.createTaskRow(task);
            }

            taskStackGroup.layout();
            body.layout(true, true);
            reflow(true);
        } else {
            for (TaskRow row : taskRows) {
                if (row.selectedCheck != null && !row.selectedCheck.isDisposed()) {
                    if (row.selectedCheck.getSelection() != row.task.isSelected()) {
                        row.selectedCheck.setSelection(row.task.isSelected());
                    }
                }
                if (row.nameText != null && !row.nameText.isDisposed()) {
                    if (!row.nameText.isFocusControl()) {
                        String name = row.task.getName() != null ? row.task.getName() : "";
                        if (!row.nameText.getText().equals(name)) {
                            row.nameText.setText(name);
                        }
                    }
                }
                if (row.timeText != null && !row.timeText.isDisposed()) {
                    if (!row.timeText.isFocusControl()) {
                        String time = row.task.getScheduledTime() != null ? row.task.getScheduledTime() : "";
                        if (!row.timeText.getText().equals(time)) {
                            row.timeText.setText(time);
                        }
                    }
                }
                if (row.statusLabel != null && !row.statusLabel.isDisposed()) {
                    String status = row.task.getStatus() != null ? row.task.getStatus().toString() : "";
                    if (!row.statusLabel.getText().equals(status)) {
                        row.statusLabel.setText(status);
                        taskStackGroup.updateStatusColor(row.statusLabel, row.task.getStatus());
                    }
                }
            }
        }

        isUpdating = false;
    }

    public void registerTaskRow(Task task, Button check, Text nameText, Text timeText, Label statusLabel) {
        taskRows.add(new TaskRow(task, check, nameText, timeText, statusLabel));
    }

    public void registerTaskRowCheck(Task task, Button check) {
    }

    public void selectAll(boolean select) {
        for (TaskRow row : taskRows) {
            row.selectedCheck.setSelection(select);
            row.task.setSelected(select);
        }
        editor.setDirty(true);
    }

    public void addNewTask() {
        Task newTask = OrchestrationFactory.eINSTANCE.createTask();
        newTask.setName("New Prompt Idea");
        newTask.setStatus(TaskStatus.PENDING);
        newTask.setSelected(true);
        orchestrator.getTasks().add(newTask);
        editor.setDirty(true);
    }

    public void executeSelected() {
        boolean parallel = globalActionsGroup.isParallel();
        List<Task> selectedTasks = new ArrayList<>();
        for (TaskRow row : taskRows) {
            if (row.selectedCheck.getSelection()) {
                selectedTasks.add(row.task);
            }
        }
        if (selectedTasks.isEmpty()) return;
        if (parallel) {
            for (Task t : selectedTasks) runTask(t);
        } else {
            runTasksSequentially(selectedTasks, 0);
        }
    }

    private void runTasksSequentially(List<Task> tasks, int index) {
        if (index >= tasks.size()) return;
        Task t = tasks.get(index);
        t.setStatus(TaskStatus.RUNNING);
        updateUIFromModel();
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

    public void setDirty(boolean dirty) {
        editor.setDirty(dirty);
    }

    @Override
    public void dispose() {
        if (orchestrator != null) orchestrator.eAdapters().remove(modelAdapter);
        if (toolkit != null) toolkit.dispose();
        super.dispose();
    }
}
