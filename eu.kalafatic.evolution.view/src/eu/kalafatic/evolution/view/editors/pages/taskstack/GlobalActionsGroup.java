package eu.kalafatic.evolution.view.editors.pages.taskstack;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import java.util.List;

import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.TaskStackPage;
import eu.kalafatic.utils.factories.GUIFactory;

public class GlobalActionsGroup extends AEvoGroup {
   
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
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Global Actions", 1, true);
       

        // Batch Update Checkboxes in 3 columns
        GUIFactory.INSTANCE.createLabel(group, "Batch Update Selected Tasks:");
        Composite batchComp = toolkit.createComposite(group);
        batchComp.setLayout(new GridLayout(7, true));
        batchComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        iterativeCheck = toolkit.createButton(batchComp, "Iterative Mode", SWT.CHECK);
        selfIterativeCheck = toolkit.createButton(batchComp, "Self-Dev Mode", SWT.CHECK);
        darwinCheck = toolkit.createButton(batchComp, "Darwin Mode", SWT.CHECK);
        autoApproveCheck = toolkit.createButton(batchComp, "Auto-Approve", SWT.CHECK);
        gitAutomationCheck = toolkit.createButton(batchComp, "Auto-Git", SWT.CHECK);
        stepModeCheck = toolkit.createButton(batchComp, "Step Mode", SWT.CHECK);

        Button applyBatchBtn = GUIFactory.INSTANCE.createButton(batchComp, "Apply to Selected", SWT.PUSH, 2 * GUIFactory.BUTTON_WIDTH);
        applyBatchBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                applyBatchSettings();
            }
        });

        Composite compositeRemote = new Composite(parent, SWT.BORDER);
        compositeRemote.setLayout(new GridLayout(6, false));
        compositeRemote.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button executeBtn = GUIFactory.INSTANCE.createButton(compositeRemote, "Execute Selected", SWT.PUSH, 2 * GUIFactory.BUTTON_WIDTH);
        executeBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {  
				page.executeSelected();
            }
        });
        
        Button addDefaultsBtn = GUIFactory.INSTANCE.createButton(compositeRemote, "Add Default Tests", SWT.PUSH, 2 * GUIFactory.BUTTON_WIDTH);
        addDefaultsBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.addDefaultModeTests();
            }
        });

        Button addPlanBtn = GUIFactory.INSTANCE.createButton(compositeRemote, "Add Plan (Session)", SWT.PUSH, 2 * GUIFactory.BUTTON_WIDTH);
        addPlanBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.addNewPlan();
            }
        });

        Button addTaskBtn = GUIFactory.INSTANCE.createButton(compositeRemote, "Add Task to Plan", SWT.PUSH, 2 * GUIFactory.BUTTON_WIDTH);
        addTaskBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.addNewTaskToSelectedPlan();
            }
        });

        

        Button clearBtn = GUIFactory.INSTANCE.createButton(compositeRemote, "Remove Selected", SWT.PUSH, 2 * GUIFactory.BUTTON_WIDTH);
        clearBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.removeSelected();
            }
        });

        Button saveBtn = GUIFactory.INSTANCE.createButton(compositeRemote, "Save Plan", SWT.PUSH, 2 * GUIFactory.BUTTON_WIDTH);
        saveBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                editor.doSave(new NullProgressMonitor());
            }
        });        
        
    }  

    private void applyBatchSettings() {
        if (orchestrator == null) return;
        applyBatchSettingsRecursive(orchestrator.getTasks());
        page.updateUIFromModel();
    }

    private void applyBatchSettingsRecursive(List<Task> tasks) {
        for (Task task : tasks) {
            if (task.isSelected()) {
                applyToTask(task);
            }
            applyBatchSettingsRecursive(task.getSubTasks());
        }
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
