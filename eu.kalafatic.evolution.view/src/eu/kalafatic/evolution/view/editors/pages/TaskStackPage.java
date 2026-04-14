package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.taskstack.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TaskStackPage extends SharedScrolledComposite {

    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private FormToolkit toolkit;
    private Composite body;
    private boolean isUpdating = false;

    private GlobalActionsGroup globalActionsGroup;
    private TaskStackGroup taskStackGroup;

    private static final int MAX_PARALLEL_PLANS = 3;

    private Adapter modelAdapter = new EContentAdapter() {
        @Override
        public void notifyChanged(Notification notification) {
            super.notifyChanged(notification);
            if (!isUpdating) {
                Display.getDefault().asyncExec(() -> {
                    if (!isDisposed()) {
                        updateUIFromModel();
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
        Display.getDefault().timerExec(10000, new Runnable() {
            @Override
            public void run() {
                if (isDisposed()) return;
                checkScheduledTasks();
                checkParallelQueue();
                Display.getDefault().timerExec(10000, this);
            }
        });
    }

    private void checkScheduledTasks() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm");
        String now = sdf.format(new Date());
        boolean changed = false;
        for (Task plan : orchestrator.getTasks()) {
            if (plan.getStatus() == TaskStatus.PENDING && plan.getScheduledTime() != null) {
                if (now.compareTo(plan.getScheduledTime()) >= 0) {
                    if (!plan.isSelected()) {
                        plan.setSelected(true);
                        changed = true;
                    }
                }
            }
        }
        if (changed) updateUIFromModel();
    }

    private void checkParallelQueue() {
        if (!globalActionsGroup.isParallel()) return;

        long runningCount = orchestrator.getTasks().stream()
                .filter(t -> t.getStatus() == TaskStatus.RUNNING)
                .count();

        if (runningCount < MAX_PARALLEL_PLANS) {
            orchestrator.getTasks().stream()
                .filter(Task::isSelected)
                .filter(t -> t.getStatus() == TaskStatus.PENDING)
                .findFirst()
                .ifPresent(this::runPlan);
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
        taskStackGroup.refreshUI();
        isUpdating = false;
    }

    public void selectAll(boolean select) {
        for (Task task : orchestrator.getTasks()) {
            task.setSelected(select);
        }
        updateUIFromModel();
        setDirty(true);
    }

    public void addNewPlan() {
        Task newPlan = OrchestrationFactory.eINSTANCE.createTask();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm");
        String timestamp = sdf.format(new Date());
        newPlan.setId("P-" + timestamp);
        newPlan.setName("New Plan Thread");
        newPlan.setStatus(TaskStatus.PENDING);
        newPlan.setSelected(true);
        orchestrator.getTasks().add(newPlan);
        setDirty(true);
    }

    public void addNewTaskToSelectedPlan() {
        Task selectedPlan = null;
        for (Task plan : orchestrator.getTasks()) {
            if (plan.isSelected()) {
                selectedPlan = plan;
                break;
            }
        }
        if (selectedPlan == null && !orchestrator.getTasks().isEmpty()) {
            selectedPlan = orchestrator.getTasks().get(0);
        }

        if (selectedPlan != null) {
            Task newTask = OrchestrationFactory.eINSTANCE.createTask();
            newTask.setName("New Sub-Task");
            newTask.setStatus(TaskStatus.PENDING);
            selectedPlan.getSubTasks().add(newTask);
            setDirty(true);
        }
    }

    public void clearDoneTasks() {
        List<Task> toRemove = orchestrator.getTasks().stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .collect(Collectors.toList());
        orchestrator.getTasks().removeAll(toRemove);
        setDirty(true);
    }

    public void executeSelected() {
        List<Task> selectedPlans = orchestrator.getTasks().stream()
                .filter(Task::isSelected)
                .filter(t -> t.getStatus() == TaskStatus.PENDING)
                .collect(Collectors.toList());

        if (selectedPlans.isEmpty()) return;

        if (globalActionsGroup.isParallel()) {
            for (Task plan : selectedPlans) {
                long running = orchestrator.getTasks().stream().filter(t -> t.getStatus() == TaskStatus.RUNNING).count();
                if (running < MAX_PARALLEL_PLANS) {
                    runPlan(plan);
                }
            }
        } else {
            runPlansSequentially(selectedPlans, 0);
        }
    }

    private void runPlansSequentially(List<Task> plans, int index) {
        if (index >= plans.size()) return;
        Task plan = plans.get(index);
        plan.setStatus(TaskStatus.RUNNING);
        updateUIFromModel();

        executePlanTasks(plan, 0, () -> {
            plan.setStatus(TaskStatus.DONE);
            updateUIFromModel();
            Display.getDefault().asyncExec(() -> runPlansSequentially(plans, index + 1));
        });
    }

    private void runPlan(Task plan) {
        plan.setStatus(TaskStatus.RUNNING);
        updateUIFromModel();
        executePlanTasks(plan, 0, () -> {
            plan.setStatus(TaskStatus.DONE);
            updateUIFromModel();
            Display.getDefault().asyncExec(this::checkParallelQueue);
        });
    }

    private void executePlanTasks(Task plan, int taskIndex, Runnable onComplete) {
        if (taskIndex >= plan.getSubTasks().size()) {
            onComplete.run();
            return;
        }

        Task task = plan.getSubTasks().get(taskIndex);
        task.setStatus(TaskStatus.RUNNING);
        updateUIFromModel();

        // Simulate execution
        Display.getDefault().timerExec(2000, () -> {
            if (isDisposed()) return;
            task.setStatus(TaskStatus.DONE);
            updateUIFromModel();
            executePlanTasks(plan, taskIndex + 1, onComplete);
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

    // Compatibility methods
    public void registerTaskRow(Task task, org.eclipse.swt.widgets.Button check, org.eclipse.swt.widgets.Text nameText, org.eclipse.swt.widgets.Text timeText, org.eclipse.swt.widgets.Label statusLabel) {}
    public void registerTaskRowCheck(Task task, org.eclipse.swt.widgets.Button check) {}
}
