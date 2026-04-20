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

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Global Actions", 6, true);

        Button selectAllBtn = SWTFactory.createButton(group, "Select All");
        selectAllBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.selectAll(true);
            }
        });

        Button unselectAllBtn = SWTFactory.createButton(group, "Unselect All");
        unselectAllBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.selectAll(false);
            }
        });

        SWTFactory.createLabel(group, "Mode:");
        executionModeCombo = SWTFactory.createCombo(group);
        executionModeCombo.add("Sequential");
        executionModeCombo.add("Parallel (Max 3)");
        executionModeCombo.select(1);

        Composite compositeRemote = new Composite(parent, SWT.BORDER);
        compositeRemote.setLayout(new GridLayout(4, false));
        compositeRemote.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button executeBtn = SWTFactory.createButton(compositeRemote, "Execute Selected", 150);
        executeBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.executeSelected();
            }
        });

        Button addPlanBtn = SWTFactory.createButton(compositeRemote, "Add Plan (Thread)", 150);
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

        Button clearBtn = SWTFactory.createButton(compositeRemote, "Clear Done", 150);
        clearBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.clearDoneTasks();
            }
        });
    }

    public boolean isParallel() {
        return executionModeCombo.getText().startsWith("Parallel");
    }
}
