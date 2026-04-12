package eu.kalafatic.evolution.view.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class AgentSettingsPage extends AWizardPage {
    private CheckboxTableViewer tableViewer;
    private List<AgentEntry> agents = new ArrayList<>();
    private Button skipCheck;

    public static class AgentEntry {
        public boolean enabled;
        public String id;
        public String type;

        public AgentEntry(boolean enabled, String id, String type) {
            this.enabled = enabled;
            this.id = id;
            this.type = type;
        }
    }

    public AgentSettingsPage() {
        super("AgentSettingsPage");
        setTitle("Agent Settings");
        setDescription("Select and configure agents for this orchestration.");

        // Default agents
        agents.add(new AgentEntry(true, "planner", "Planner"));
        agents.add(new AgentEntry(true, "developer", "JavaDev"));
        agents.add(new AgentEntry(true, "reviewer", "Reviewer"));
        agents.add(new AgentEntry(true, "terminal", "Terminal"));
        agents.add(new AgentEntry(true, "file", "File"));
        agents.add(new AgentEntry(false, "maven", "Maven"));
        agents.add(new AgentEntry(false, "git", "Git"));
        agents.add(new AgentEntry(false, "structure", "Structure"));
        agents.add(new AgentEntry(false, "search", "Web-Search"));
        agents.add(new AgentEntry(false, "quality", "Quality"));
        agents.add(new AgentEntry(false, "observability", "Observability"));
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        tableViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.FULL_SELECTION);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 250;
        table.setLayoutData(gd);

        TableViewerColumn colEnable = new TableViewerColumn(tableViewer, SWT.NONE);
        colEnable.getColumn().setText("Enable");
        colEnable.getColumn().setWidth(60);
        colEnable.setLabelProvider(new ColumnLabelProvider() {
            @Override public String getText(Object element) { return ""; }
        });

        TableViewerColumn colId = new TableViewerColumn(tableViewer, SWT.NONE);
        colId.getColumn().setText("ID");
        colId.getColumn().setWidth(150);
        colId.setLabelProvider(new ColumnLabelProvider() {
            @Override public String getText(Object element) { return ((AgentEntry) element).id; }
        });
        colId.setEditingSupport(new EditingSupport(tableViewer) {
            @Override protected boolean canEdit(Object element) { return true; }
            @Override protected CellEditor getCellEditor(Object element) { return new TextCellEditor(tableViewer.getTable()); }
            @Override protected Object getValue(Object element) { return ((AgentEntry) element).id; }
            @Override protected void setValue(Object element, Object value) {
                ((AgentEntry) element).id = String.valueOf(value);
                tableViewer.update(element, null);
            }
        });

        TableViewerColumn colType = new TableViewerColumn(tableViewer, SWT.NONE);
        colType.getColumn().setText("Type");
        colType.getColumn().setWidth(150);
        colType.setLabelProvider(new ColumnLabelProvider() {
            @Override public String getText(Object element) { return ((AgentEntry) element).type; }
        });
        colType.setEditingSupport(new EditingSupport(tableViewer) {
            @Override protected boolean canEdit(Object element) { return true; }
            @Override protected CellEditor getCellEditor(Object element) { return new TextCellEditor(tableViewer.getTable()); }
            @Override protected Object getValue(Object element) { return ((AgentEntry) element).type; }
            @Override protected void setValue(Object element, Object value) {
                ((AgentEntry) element).type = String.valueOf(value);
                tableViewer.update(element, null);
            }
        });

        Composite btnComp = new Composite(container, SWT.NONE);
        btnComp.setLayout(new GridLayout(1, false));
        btnComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

        Button addBtn = new Button(btnComp, SWT.PUSH);
        addBtn.setText("Add Agent");
        addBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        addBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                AgentEntry entry = new AgentEntry(true, "new-agent", "General");
                agents.add(entry);
                tableViewer.add(entry);
                tableViewer.setChecked(entry, true);
            }
        });

        Button removeBtn = new Button(btnComp, SWT.PUSH);
        removeBtn.setText("Remove Agent");
        removeBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        removeBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection sel = (IStructuredSelection) tableViewer.getSelection();
                if (!sel.isEmpty()) {
                    AgentEntry entry = (AgentEntry) sel.getFirstElement();
                    agents.remove(entry);
                    tableViewer.remove(entry);
                }
            }
        });

        tableViewer.setContentProvider(ArrayContentProvider.getInstance());
        tableViewer.setInput(agents);
        for (AgentEntry ae : agents) {
            tableViewer.setChecked(ae, ae.enabled);
        }

        skipCheck = new Button(container, SWT.CHECK);
        skipCheck.setText("Skip this step and setup later");
        gd = new GridData(SWT.BEGINNING, SWT.CENTER, true, false);
        gd.horizontalSpan = 2;
        skipCheck.setLayoutData(gd);

        setControl(container);
    }

    public boolean isSkipped() {
        return skipCheck != null && skipCheck.getSelection();
    }

    public List<AgentEntry> getSelectedAgents() {
        List<AgentEntry> result = new ArrayList<>();
        for (Object obj : tableViewer.getCheckedElements()) {
            result.add((AgentEntry) obj);
        }
        return result;
    }
}
