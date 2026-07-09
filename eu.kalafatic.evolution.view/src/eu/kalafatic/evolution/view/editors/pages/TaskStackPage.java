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

import eu.kalafatic.evolution.controller.orchestration.behavior.BitState;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.taskstack.*;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventListener;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TaskStackPage extends AEvoPage {

    private Composite body;
    private boolean isUpdating = false;

    private GlobalActionsGroup globalActionsGroup;
    private TaskStackGroup taskStackGroup;
    private List<Task> executionQueue = new ArrayList<>();
    private List<Task> activeTasks = new ArrayList<>();

    private Adapter modelAdapter = new EContentAdapter() {
        @Override
        public void notifyChanged(Notification notification) {
            super.notifyChanged(notification);
            if (notification.isTouch()) return;

            if (notification.getNotifier() instanceof Task && notification.getFeatureID(Task.class) == OrchestrationPackage.TASK__STATUS) {
                TaskStatus newStatus = (TaskStatus) notification.getNewValue();
                Task task = (Task) notification.getNotifier();
                if (newStatus == TaskStatus.DONE || newStatus == TaskStatus.FAILED) {
                    synchronized (activeTasks) {
                        activeTasks.remove(task);
                    }
                    if (newStatus == TaskStatus.FAILED) {
                        cancelSubsequentPlanTasks(task);
                    }
                    processNextInQueue();
                }
            }

            if (!isUpdating) {
                scheduleRefresh();
            }
        }
    };

    public TaskStackPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, editor, orchestrator);

        body = toolkit.createComposite(this);
        body.setLayout(new GridLayout(1, false));
        setContent(body);

        taskStackGroup = new TaskStackGroup(toolkit, body, editor, orchestrator, this);
        globalActionsGroup = new GlobalActionsGroup(toolkit, body, editor, orchestrator, this);

        setOrchestrator(orchestrator);
        // startTimer();
    }

    private String getSessionId(Task task) {
        if (task == null) return "Default";
        if (task.eContainer() instanceof Task) {
            return getSessionId((Task) task.eContainer());
        }
        return task.getId() != null ? task.getId() : "Default";
    }

    private void subscribeToTaskEvents(final Task task) {
        String sid = getSessionId(task);
        final SessionContainer session = SessionManager.getInstance().getOrCreateSession(sid);

        RuntimeEventListener listener = new RuntimeEventListener() {
            @Override
            public void onEvent(RuntimeEvent event) {
                if (event.getType() == RuntimeEventType.FLOW_COMPLETED ||
                    event.getType() == RuntimeEventType.TASK_COMPLETED) {

                    session.getEventBus().unsubscribe(this);
                    handleTaskFinished(task, true);
                } else if (event.getType() == RuntimeEventType.TASK_FAILED ||
                           event.getType() == RuntimeEventType.COMMAND_FAILED) {

                    session.getEventBus().unsubscribe(this);
                    handleTaskFinished(task, false);
                }
            }
        };
        session.getEventBus().subscribe(listener);
    }

    private void handleTaskFinished(final Task task, final boolean success) {
        Display.getDefault().asyncExec(() -> {
            if (isDisposed()) return;
            task.setStatus(success ? TaskStatus.DONE : TaskStatus.FAILED);
            setDirty(true);
        });
    }

    private void startTimer() {
        /*
        Display.getDefault().timerExec(1000, new Runnable() {
            @Override
            public void run() {
                if (isDisposed()) return;
                checkAutoExecution();
                updateUIFromModel();
                Display.getDefault().timerExec(1000, this);
            }
        });
        */
    }

    private void checkAutoExecution() {
        /*
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
        */
    }

    
    public String getCountdown(Task task) {
        return "";
        /*
        if (task.getStatus() != TaskStatus.PENDING) return "";
        Long execTime = autoExecuteTimes.get(task);
        if (execTime == null) return "";
        long remaining = execTime - System.currentTimeMillis();
        if (remaining <= 0) return "00:00";
        long seconds = (remaining / 1000) % 60;
        long minutes = (remaining / 1000) / 60;
        return String.format("%02d:%02d", minutes, seconds);
        */
    }


    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            scheduleRefresh();
        }
    }

    @Override
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
        if (taskStackGroup != null && taskStackGroup.getTreeViewer() != null && taskStackGroup.getTreeViewer().getInput() == null) {
            taskStackGroup.getTreeViewer().setInput(orchestrator);
        }
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
        selectAllRecursive(orchestrator.getTasks(), select);
        updateUIFromModel();
        setDirty(true);
    }

    private void selectAllRecursive(List<Task> tasks, boolean select) {
        for (Task task : tasks) {
            task.setSelected(select);
            selectAllRecursive(task.getSubTasks(), select);
        }
    }

    public void addNewPlan() {
        Task newPlan = OrchestrationFactory.eINSTANCE.createTask();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm");
        String timestamp = sdf.format(new Date());
        newPlan.setId("P-" + timestamp);
        newPlan.setName("New Plan Session");
        newPlan.setStatus(TaskStatus.READY);
        newPlan.setSelected(true);
        orchestrator.getTasks().add(newPlan);
        setDirty(true);
    }

    public void addDefaultModeTests() {
        String[] modes = {"SIMPLE_CHAT", "ASSISTED_CODING", "DARWIN_MODE", "SELF_DEV_MODE", "HEADLESS_MODE", "PROMPT_HELLO", "PROMPT_CREATE_LOCAL", "PROMPT_CREATE_MEDIATED", "PROMPT_ANALYZE_MEDIATED"};
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm");
        String timestamp = sdf.format(new Date());
        for (String mode : modes) {
            String name = switch(mode) {
                case "PROMPT_HELLO" -> "Hello Task";
                case "PROMPT_CREATE_LOCAL" -> "Create Java Class (LOCAL)";
                case "PROMPT_CREATE_MEDIATED" -> "Create Java Class (MEDIATED)";
                case "PROMPT_ANALYZE_MEDIATED" -> "Analyze IterationManager (MEDIATED)";
                default -> "Default Test: " + mode;
            };

            boolean exists = orchestrator.getTasks().stream().anyMatch(t -> name.equals(t.getName()));
            if (!exists) {
                Task testPlan = OrchestrationFactory.eINSTANCE.createTask();
                testPlan.setId("DT-" + mode + "-" + timestamp);
                testPlan.setName(name);
                testPlan.setType(mode.startsWith("PROMPT_") ? "coding" : mode);
                testPlan.setStatus(TaskStatus.READY);
                testPlan.setSelected(false);

                String description = switch(mode) {
                    case "SIMPLE_CHAT" -> "Explain the purpose of this project.";
                    case "ASSISTED_CODING" -> "Add a new utility method to stringify JSON in eu.kalafatic.utils.";
                    case "DARWIN_MODE" -> "Optimize the EvolutionOrchestrator performance.";
                    case "SELF_DEV_MODE" -> "Improve the TaskStackPage UI with better execution controls.";
                    case "HEADLESS_MODE" -> "Verify headless execution using the Self-Development Supervisor.";
                    case "PROMPT_HELLO" -> "hello";
                    case "PROMPT_CREATE_LOCAL" -> "create java class which can print text";
                    case "PROMPT_CREATE_MEDIATED" -> "create java class which can print text";
                    case "PROMPT_ANALYZE_MEDIATED" -> "analyze IterationManager.java";
                    default -> "";
                };
                testPlan.setDescription(description);
                testPlan.setPrompt(description);

                // Configure BitState for PROMPT modes
                if (mode.equals("PROMPT_CREATE_LOCAL")) {
                    testPlan.setBitState(BitState.encode(BitState.MODE_LOCAL, BitState.SUPERVISION_AUTO, BitState.INTERACTION_CONTINUOUS, BitState.REASONING_DARWIN, BitState.WORKFLOW_TASK_ORIENTED));
                    testPlan.setDarwinMode(true);
                } else if (mode.equals("PROMPT_CREATE_MEDIATED")) {
                    testPlan.setBitState(BitState.encode(BitState.MODE_MEDIATED, BitState.SUPERVISION_MANUAL, BitState.INTERACTION_CONTINUOUS, BitState.REASONING_DARWIN, BitState.WORKFLOW_TASK_ORIENTED));
                    testPlan.setDarwinMode(true);
                } else if (mode.equals("PROMPT_ANALYZE_MEDIATED")) {
                    testPlan.setBitState(BitState.encode(BitState.MODE_MEDIATED, BitState.SUPERVISION_MANUAL, BitState.INTERACTION_CONTINUOUS, BitState.REASONING_DARWIN, BitState.WORKFLOW_SELF_DEV));
                    testPlan.setDarwinMode(true);
                    testPlan.setSelfIterativeMode(true);
                }

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
                    subTask.setStatus(TaskStatus.READY);
                    testPlan.getSubTasks().add(subTask);
                }

                orchestrator.getTasks().add(testPlan);
            }
        }
        setDirty(true);
        updateUIFromModel();
        taskStackGroup.getTreeViewer().expandAll();
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
            newTask.setStatus(TaskStatus.READY);
            selectedPlan.getSubTasks().add(newTask);
            setDirty(true);
        }
    }

    public void removeSelected() {
        List<Task> toRemove = new ArrayList<>();
        for (Task task : orchestrator.getTasks()) {
            if (task.isSelected()) {
                toRemove.add(task);
            } else {
                removeSelectedSubtasks(task);
            }
        }
        orchestrator.getTasks().removeAll(toRemove);
        setDirty(true);
        updateUIFromModel();
    }

    private void removeSelectedSubtasks(Task parent) {
        List<Task> subTasksToRemove = new ArrayList<>();
        for (Task subTask : parent.getSubTasks()) {
            if (subTask.isSelected()) {
                subTasksToRemove.add(subTask);
            } else {
                removeSelectedSubtasks(subTask);
            }
        }
        parent.getSubTasks().removeAll(subTasksToRemove);
    }

    private void ensurePlanIds() {
        if (orchestrator == null) return;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm");
        long counter = System.currentTimeMillis();
        for (Task task : orchestrator.getTasks()) {
            if (task.getId() == null || task.getId().isEmpty()) {
                task.setId("P-" + sdf.format(new Date()) + "-" + (counter++ % 1000));
            }
            ensureSubTaskIds(task);
        }
    }

    private void ensureSubTaskIds(Task parent) {
        long counter = System.currentTimeMillis();
        for (Task subTask : parent.getSubTasks()) {
            if (subTask.getId() == null || subTask.getId().isEmpty()) {
                subTask.setId("T-" + (counter++ % 100000));
            }
            ensureSubTaskIds(subTask);
        }
    }

    public void executeSelected() {
        ensurePlanIds();
        if (editor != null) {
            editor.doSave(new org.eclipse.core.runtime.NullProgressMonitor());
        }
        List<Task> selectedTasks = new ArrayList<>();
        collectSelectedTasks(orchestrator.getTasks(), selectedTasks);

        if (selectedTasks.isEmpty()) return;

        for (Task t : selectedTasks) {
            t.setStatus(TaskStatus.PENDING);
        }

        executionQueue.addAll(selectedTasks);
        processNextInQueue();
    }

    private void collectSelectedTasks(List<Task> tasks, List<Task> collected) {
        collectSelectedTasks(tasks, collected, false);
    }

    private void collectSelectedTasks(List<Task> tasks, List<Task> collected, boolean parentSelected) {
        for (Task task : tasks) {
            boolean isSelected = task.isSelected() || parentSelected;
            boolean hasChildren = !task.getSubTasks().isEmpty();
            boolean hasPrompt = task.getPrompt() != null && !task.getPrompt().trim().isEmpty();

            if (hasChildren && !hasPrompt) {
                collectSelectedTasks(task.getSubTasks(), collected, isSelected);
            } else {
                if (isSelected && (task.getStatus() == TaskStatus.READY || task.getStatus() == TaskStatus.PENDING)) {
                    collected.add(task);
                }
                collectSelectedTasks(task.getSubTasks(), collected, isSelected);
            }
        }
    }

    public void runSingleTask(Task task) {
        ensurePlanIds();
        if (editor != null) {
            editor.doSave(new org.eclipse.core.runtime.NullProgressMonitor());
        }
        synchronized (activeTasks) {
            int limit = (taskStackGroup != null && taskStackGroup.isParallel()) ? 3 : 1;
            if (activeTasks.size() < limit) {
                activeTasks.add(task);
                task.setStatus(TaskStatus.RUNNING);
                subscribeToTaskEvents(task);
                editor.runTaskInChat(task);
            } else {
                task.setStatus(TaskStatus.PENDING);
                executionQueue.add(task);
            }
        }
    }

    private Task getTopLevelPlan(Task task) {
        if (task == null) return null;
        if (task.eContainer() instanceof Task) {
            return getTopLevelPlan((Task) task.eContainer());
        }
        return task;
    }

    private void cancelSubsequentPlanTasks(Task failedTask) {
        Task plan = getTopLevelPlan(failedTask);
        if (plan == null) return;
        synchronized (activeTasks) {
            List<Task> toCancel = new ArrayList<>();
            for (Task candidate : executionQueue) {
                if (getTopLevelPlan(candidate) == plan) {
                    toCancel.add(candidate);
                }
            }
            executionQueue.removeAll(toCancel);
            for (Task t : toCancel) {
                t.setStatus(TaskStatus.FAILED);
            }
        }
    }

    private void processNextInQueue() {
        synchronized (activeTasks) {
            if (executionQueue.isEmpty()) return;
            int limit = (taskStackGroup != null && taskStackGroup.isParallel()) ? 3 : 1;

            while (activeTasks.size() < limit) {
                Task nextToRun = null;
                for (Task candidate : executionQueue) {
                    Task candidatePlan = getTopLevelPlan(candidate);
                    boolean planHasActiveTasks = false;
                    for (Task active : activeTasks) {
                        if (getTopLevelPlan(active) == candidatePlan) {
                            planHasActiveTasks = true;
                            break;
                        }
                    }
                    if (!planHasActiveTasks) {
                        nextToRun = candidate;
                        break;
                    }
                }

                if (nextToRun != null) {
                    executionQueue.remove(nextToRun);
                    activeTasks.add(nextToRun);
                    nextToRun.setStatus(TaskStatus.RUNNING);
                    subscribeToTaskEvents(nextToRun);
                    editor.runTaskInChat(nextToRun);
                } else {
                    break;
                }
            }
        }
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

	public TaskStackGroup getTaskStackGroup() {
		return taskStackGroup;
	}
}
