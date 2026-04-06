package eu.kalafatic.evolution.view.editors.pages.approval;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.ApprovalPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class ProposedTasksGroup {
    private Group group;
    private TableViewer tableViewer;
    private ApprovalPage page;

    public ProposedTasksGroup(Composite parent, ApprovalPage page) {
        this.page = page;
        createControl(parent);
    }

    private void createControl(Composite parent) {
        group = SWTFactory.createGroup(parent, "Proposed Tasks", 1);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridData)group.getLayoutData()).heightHint = 180;

        Composite taskTableComposite = new Composite(group, SWT.NONE);
        taskTableComposite.setLayout(new GridLayout(2, false));
        taskTableComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        tableViewer = new TableViewer(taskTableComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        page.createColumns(tableViewer);
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        Composite taskActions = new Composite(taskTableComposite, SWT.NONE);
        taskActions.setLayout(new GridLayout(1, false));
        taskActions.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false));

        Button upBtn = SWTFactory.createButton(taskActions, "Move Up", 80);
        upBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { page.handleMoveTask(-1); }
        });

        Button downBtn = SWTFactory.createButton(taskActions, "Move Down", 80);
        downBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { page.handleMoveTask(1); }
        });

        Button deleteBtn = SWTFactory.createButton(taskActions, "Delete", 80);
        deleteBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { page.handleDeleteTask(); }
        });
    }

    public void updateUI(Orchestrator orchestrator) {
        if (orchestrator != null) {
            tableViewer.setInput(orchestrator.getTasks());
        }
    }

    public TableViewer getTableViewer() { return tableViewer; }
}
