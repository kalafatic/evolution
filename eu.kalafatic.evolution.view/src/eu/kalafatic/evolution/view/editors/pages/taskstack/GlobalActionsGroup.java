package eu.kalafatic.evolution.view.editors.pages.taskstack;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.view.editors.pages.TaskStackPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

public class GlobalActionsGroup extends AEvoGroup {
    private Combo executionModeCombo;
    private TaskStackPage page;

    public GlobalActionsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, TaskStackPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    @Override
    protected void refreshUI() {
        // No dynamic model data to refresh currently
    }

    private Button iterativeCheck, selfIterativeCheck, darwinCheck, autoApproveCheck, gitAutomationCheck, stepModeCheck;

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Global Actions", 1, true);

        Composite topComp = toolkit.createComposite(group);
        topComp.setLayout(new GridLayout(6, false));
        topComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button selectAllBtn = SWTFactory.createButton(topComp, "Select All");
        selectAllBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.selectAll(true);
            }
        });

        Button unselectAllBtn = SWTFactory.createButton(topComp, "Unselect All");
        unselectAllBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.selectAll(false);
            }
        });

        SWTFactory.createLabel(topComp, "Execution Mode:");
        executionModeCombo = SWTFactory.createCombo(topComp);
        executionModeCombo.add("Sequential");
        executionModeCombo.add("Parallel (Max 3)");
        executionModeCombo.select(0);

        // Batch Update Checkboxes in 3 columns
        SWTFactory.createLabel(group, "Batch Update Selected Tasks:");
        Composite batchComp = toolkit.createComposite(group);
        batchComp.setLayout(new GridLayout(3, true));
        batchComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        iterativeCheck = toolkit.createButton(batchComp, "Iterative Mode", SWT.CHECK);
        selfIterativeCheck = toolkit.createButton(batchComp, "Self-Dev Mode", SWT.CHECK);
        darwinCheck = toolkit.createButton(batchComp, "Darwin Mode", SWT.CHECK);
        autoApproveCheck = toolkit.createButton(batchComp, "Auto-Approve", SWT.CHECK);
        gitAutomationCheck = toolkit.createButton(batchComp, "Auto-Git", SWT.CHECK);
        stepModeCheck = toolkit.createButton(batchComp, "Step Mode", SWT.CHECK);

        Button applyBatchBtn = SWTFactory.createButton(batchComp, "Apply to Selected", 150);
        applyBatchBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                applyBatchSettings();
            }
        });

        Composite compositeRemote = new Composite(parent, SWT.BORDER);
        compositeRemote.setLayout(new GridLayout(5, false));
        compositeRemote.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button executeBtn = SWTFactory.createButton(compositeRemote, "Execute Selected", 150);
        executeBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.executeSelected();
            }
        });

        Button addPlanBtn = SWTFactory.createButton(compositeRemote, "Add Plan (Session)", 150);
        addPlanBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.addNewPlan();
            }
        });

        Button addTaskBtn = SWTFactory.createButton(compositeRemote, "Add Task to Plan", 150);
        addTaskBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.addNewTaskToSelectedPlan();
            }
        });

        Button addDefaultsBtn = SWTFactory.createButton(compositeRemote, "Add Default Tests", 150);
        addDefaultsBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.addDefaultModeTests();
            }
        });

        Button clearBtn = SWTFactory.createButton(compositeRemote, "Remove Selected", 150);
        clearBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.removeSelected();
            }
        });
    }

    public boolean isParallel() {
        return executionModeCombo.getText().startsWith("Parallel");
    }

    private void applyBatchSettings() {
        if (orchestrator == null) return;
        for (eu.kalafatic.evolution.model.orchestration.Task task : orchestrator.getTasks()) {
            if (task.isSelected()) {
                applyToTask(task);
            }
            for (eu.kalafatic.evolution.model.orchestration.Task sub : task.getSubTasks()) {
                if (sub.isSelected()) {
                    applyToTask(sub);
                }
            }
        }
        page.updateUIFromModel();
    }

    private void applyToTask(eu.kalafatic.evolution.model.orchestration.Task task) {
        task.setIterativeMode(iterativeCheck.getSelection());
        task.setSelfIterativeMode(selfIterativeCheck.getSelection());
        task.setDarwinMode(darwinCheck.getSelection());
        task.setApprovalRequired(!autoApproveCheck.getSelection());
        task.setGitAutomation(gitAutomationCheck.getSelection());
        task.setStepMode(stepModeCheck.getSelection());
    }
}
