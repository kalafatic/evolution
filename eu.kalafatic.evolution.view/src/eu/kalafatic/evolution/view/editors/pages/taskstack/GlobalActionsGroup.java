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

public class GlobalActionsGroup {
    private Composite group;
    private Combo executionModeCombo;
    private TaskStackPage page;

    public GlobalActionsGroup(FormToolkit toolkit, Composite parent, TaskStackPage page) {
        this.page = page;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Global Actions", 4, true);

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
        executionModeCombo.add("Parallel");
        executionModeCombo.select(0);

        Composite compositeRemote = new Composite(parent, SWT.BORDER);
        compositeRemote.setLayout(new GridLayout(3, false));
        compositeRemote.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button executeBtn = SWTFactory.createButton(compositeRemote, "Execute Selected", 150);
        executeBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.executeSelected();
            }
        });

        Button addTaskBtn = SWTFactory.createButton(compositeRemote, "Add New Task Idea", 150);
        addTaskBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.addNewTask();
            }
        });
    }

    public boolean isParallel() {
        return "Parallel".equals(executionModeCombo.getText());
    }
}
