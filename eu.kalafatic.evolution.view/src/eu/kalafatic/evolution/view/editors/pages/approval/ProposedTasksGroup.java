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
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.ApprovalPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

public class ProposedTasksGroup extends AEvoGroup {
    private TableViewer tableViewer;
    private ApprovalPage page;

    public ProposedTasksGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, ApprovalPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Proposed Tasks", 1, true);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridData)group.getLayoutData()).heightHint = 220;

        Composite taskTableComposite = toolkit.createComposite(group);
        taskTableComposite.setLayout(new GridLayout(2, false));
        taskTableComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        tableViewer = new TableViewer(taskTableComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        page.createColumns(tableViewer);
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        Composite taskActions = toolkit.createComposite(taskTableComposite);
        taskActions.setLayout(new GridLayout(1, false));
        taskActions.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false));

        Button upBtn = toolkit.createButton(taskActions, "Move Up", SWT.PUSH);
        upBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        upBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { page.handleMoveTask(-1); }
        });

        Button downBtn = toolkit.createButton(taskActions, "Move Down", SWT.PUSH);
        downBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        downBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { page.handleMoveTask(1); }
        });

        Button deleteBtn = toolkit.createButton(taskActions, "Delete", SWT.PUSH);
        deleteBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        deleteBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { page.handleDeleteTask(); }
        });
    }

    @Override
    public void refreshUI() {
        if (orchestrator != null) {
            tableViewer.setInput(orchestrator.getTasks());
        }
    }

    public TableViewer getTableViewer() { return tableViewer; }
}
