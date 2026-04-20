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

public class TaskStackPage extends AEvoPage {

    private Composite body;
    private boolean isUpdating = false;
    private boolean runInUi = false;

    private GlobalActionsGroup globalActionsGroup;
    private TaskStackGroup taskStackGroup;
    private java.util.Map<Task, Long> autoExecuteTimes = new java.util.HashMap<>();

    private static final int MAX_PARALLEL_PLANS = 3;
    private static final int AUTO_EXECUTION_DELAY_MS = 600000; // 10 minutes

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
        super(parent, editor, orchestrator);

        body = toolkit.createComposite(this);
        body.setLayout(new GridLayout(1, false));
        setContent(body);

        globalActionsGroup = new GlobalActionsGroup(toolkit, body, editor, orchestrator, this);
        taskStackGroup = new TaskStackGroup(toolkit, body, editor, orchestrator, this);

        setOrchestrator(orchestrator);
        startTimer();
    }

    private void startTimer() {
        Display.getDefault().timerExec(1000, new Runnable() {
            @Override
            public void run() {
                if (isDisposed()) return;
                checkAutoExecution();
                updateUIFromModel();
                Display.getDefault().timerExec(1000, this);
            }
        });
    }

    private void checkAutoExecution() {
        long now = System.currentTimeMillis();

        // Count currently running tasks
        long runningCount = orchestrator.getTasks().stream()
                .filter(t -> t.getStatus() == TaskStatus.RUNNING)
                .count();

        for (Task task : orchestrator.getTasks()) {
            if (task.getStatus() == TaskStatus.PENDING) {
                Long execTime = autoExecuteTimes.get(task);
                if (execTime == null) {
                    autoExecuteTimes.put(task, now + AUTO_EXECUTION_DELAY_MS);
                } else if (now >= execTime) {
                    // Try to start it
                    if (globalActionsGroup.isParallel()) {
                        if (runningCount < MAX_PARALLEL_PLANS) {
                            autoExecuteTimes.remove(task);
                            runPlan(task);
                            runningCount++; // Increment local count to prevent over-starting
                        }
                    } else {
                        if (runningCount == 0) {
                            autoExecuteTimes.remove(task);
                            runPlan(task);
                            runningCount++;
                        }
                    }
                }
            } else {
                autoExecuteTimes.remove(task);
            }
        }
    }

    @Override
    public String getCountdown(Task task) {
        if (task.getStatus() != TaskStatus.PENDING) return "";
        Long execTime = autoExecuteTimes.get(task);
        if (execTime == null) return "";
        long remaining = execTime - System.currentTimeMillis();
        if (remaining <= 0) return "00:00";
        long seconds = (remaining / 1000) % 60;
        long minutes = (remaining / 1000) / 60;
        return String.format("%02d:%02d", minutes, seconds);
    }


    public void setOrchestrator(Orchestrator orchestrator) {
        if (this.orchestrator != null) {
            this.orchestrator.eAdapters().remove(modelAdapter);
        }
        super.setOrchestrator(orchestrator);
        if (this.orchestrator != null) {
            this.orchestrator.eAdapters().add(modelAdapter);
        }
    }

    @Override
    protected void refreshUI() {
        if (isUpdating || orchestrator == null || body == null || body.isDisposed()) return;
        isUpdating = true;
        taskStackGroup.refreshUI();
        body.layout(true, true);
        this.setMinSize(body.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        this.reflow(true);
        isUpdating = false;
    }

    public void updateUIFromModel() {
        scheduleRefresh();
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

    public void addDefaultModeTests() {
        String[] modes = {"SIMPLE_CHAT", "ASSISTED_CODING", "DARWIN_MODE", "SELF_DEV_MODE", "HEADLESS_MODE"};
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm");
        String timestamp = sdf.format(new Date());
        for (String mode : modes) {
            String name = "Default Test: " + mode;
            boolean exists = orchestrator.getTasks().stream().anyMatch(t -> name.equals(t.getName()));
            if (!exists) {
                Task testPlan = OrchestrationFactory.eINSTANCE.createTask();
                testPlan.setId("DT-" + mode + "-" + timestamp);
                testPlan.setName(name);
                testPlan.setType(mode);
                testPlan.setStatus(TaskStatus.PENDING);
                testPlan.setSelected(true);

                String description = switch(mode) {
                    case "SIMPLE_CHAT" -> "Explain the purpose of this project.";
                    case "ASSISTED_CODING" -> "Add a new utility method to stringify JSON in eu.kalafatic.utils.";
                    case "DARWIN_MODE" -> "Optimize the EvolutionOrchestrator performance.";
                    case "SELF_DEV_MODE" -> "Improve the TaskStackPage UI with better execution controls.";
                    case "HEADLESS_MODE" -> "Verify headless execution using the Self-Development Supervisor.";
                    default -> "";
                };
                testPlan.setDescription(description);

                String[] subtaskNames = switch(mode) {
                    case "SIMPLE_CHAT" -> new String[]{"Intent Analysis (Skip Loop)", "Direct Agent Dispatch", "Response Generation"};
                    case "ASSISTED_CODING" -> new String[]{"Plan Generation", "User Approval Wait", "Atomic Task Execution", "Result Verification"};
                    case "DARWIN_MODE" -> new String[]{"Variant Generation", "Parallel Execution", "Scoring & Selection", "Merge fittest solution"};
                    case "SELF_DEV_MODE" -> new String[]{"Supervisor Session Start", "Iterative Darwin Loop", "Self-Modification Check", "Regression Testing"};
                    case "HEADLESS_MODE" -> new String[]{"Supervisor Initialization", "Headless Maven Build", "External Loop Execution", "Result Aggregation"};
                    default -> new String[0];
                };

                for (String stName : subtaskNames) {
                    Task subTask = OrchestrationFactory.eINSTANCE.createTask();
                    subTask.setName(stName);
                    subTask.setStatus(TaskStatus.PENDING);
                    testPlan.getSubTasks().add(subTask);
                }

                orchestrator.getTasks().add(testPlan);
            }
        }
        setDirty(true);
        updateUIFromModel();
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
        if (runInUi) {
            editor.runTaskInChat(plan);
            return;
        }

        plan.setStatus(TaskStatus.RUNNING);
        updateUIFromModel();
        executePlanTasks(plan, 0, () -> {
            plan.setStatus(TaskStatus.DONE);
            plan.setResultSummary("Plan executed successfully.");
            updateUIFromModel();
        });
    }

    public void runSingleTask(Task task) {
        if (runInUi) {
            editor.runTaskInChat(task);
            return;
        }

        task.setStatus(TaskStatus.RUNNING);
        updateUIFromModel();

        // Simulate execution
        Display.getDefault().timerExec(2000, () -> {
            if (isDisposed()) return;
            task.setStatus(TaskStatus.DONE);
            task.setResultSummary("Task executed successfully.");
            updateUIFromModel();
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
            task.setResultSummary("Sub-Task " + (taskIndex + 1) + " done.");
            updateUIFromModel();
            executePlanTasks(plan, taskIndex + 1, onComplete);
        });
    }

    public boolean isRunInUi() {
        return runInUi;
    }

    public void setRunInUi(boolean runInUi) {
        this.runInUi = runInUi;
    }

    public void setDirty(boolean dirty) {
        editor.setDirty(dirty);
    }

    @Override
    public void dispose() {
        if (orchestrator != null) orchestrator.eAdapters().remove(modelAdapter);
        super.dispose();
    }

    // Compatibility methods
    public void registerTaskRow(Task task, org.eclipse.swt.widgets.Button check, org.eclipse.swt.widgets.Text nameText, org.eclipse.swt.widgets.Text timeText, org.eclipse.swt.widgets.Label statusLabel) {}
    public void registerTaskRowCheck(Task task, org.eclipse.swt.widgets.Button check) {}
}
