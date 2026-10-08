package eu.kalafatic.evolution.view.editors.pages.properties;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryQuery;
import eu.kalafatic.evolution.controller.memory.MemoryScope;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

/**
 * Embedded Memory / Context section inside Properties Page.
 * Displays persistent memory relevant to the active project / orchestrator context.
 */
public class MemoryContextGroup extends AEvoGroup {

    private Table memoryTable;
    private Button openFullMemoryBtn;

    public MemoryContextGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);

        Group g = new Group(parent, SWT.NONE);
        g.setText("Memory / Context");
        g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        this.group = g;
        if (toolkit != null) toolkit.adapt(g);

        createContent(g);
    }

    private void createContent(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        memoryTable = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        memoryTable.setHeaderVisible(true);
        memoryTable.setLinesVisible(true);
        GridData tableGd = new GridData(SWT.FILL, SWT.FILL, true, false);
        tableGd.heightHint = 120;
        memoryTable.setLayoutData(tableGd);

        String[] headers = {"Content", "Scope", "Type", "Importance", "Updated At"};
        int[] widths = {280, 80, 100, 80, 140};
        for (int i = 0; i < headers.length; i++) {
            TableColumn col = new TableColumn(memoryTable, SWT.LEFT);
            col.setText(headers[i]);
            col.setWidth(widths[i]);
        }

        Composite btnComp = new Composite(parent, SWT.NONE);
        btnComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        btnComp.setLayout(new GridLayout(2, false));

        openFullMemoryBtn = new Button(btnComp, SWT.PUSH);
        openFullMemoryBtn.setText("Open Full Memory / Context");
        openFullMemoryBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                handleOpenFullMemory();
            }
        });

        Button refreshBtn = new Button(btnComp, SWT.PUSH);
        refreshBtn.setText("Refresh");
        refreshBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                refreshUI();
            }
        });

        refreshUI();
    }

    private void handleOpenFullMemory() {
        System.out.println("PROPERTIES_MEMORY_OPEN");
        if (editor != null) {
            String assocId = orchestrator != null ? orchestrator.getId() : null;
            System.out.println("MEMORY_CONTEXT_RESOLVED: scope=PROJECT, associatedId=" + (assocId != null ? assocId : "NONE"));
            System.out.println("MEMORY_SCOPE_RESOLVED: PROJECT");
            editor.showContextPage(MemoryScope.PROJECT, assocId);
            System.out.println("MEMORY_UI_REFRESH: Refreshed Properties Memory / Context UI");
        }
    }

    @Override
    protected void refreshUI() {
        if (memoryTable == null || memoryTable.isDisposed()) return;

        memoryTable.removeAll();

        MemoryQuery query = new MemoryQuery();
        query.setScope(MemoryScope.PROJECT);
        if (orchestrator != null && orchestrator.getId() != null) {
            query.setAssociatedId(orchestrator.getId());
        }

        List<MemoryEntry> entries = MemoryService.getInstance().retrieveRelevant(query);
        if (entries == null || entries.isEmpty()) {
            query = new MemoryQuery().setScope(MemoryScope.PROJECT);
            entries = MemoryService.getInstance().retrieveRelevant(query);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (MemoryEntry entry : entries) {
            TableItem item = new TableItem(memoryTable, SWT.NONE);
            item.setData(entry);
            item.setText(0, entry.getContent() != null ? entry.getContent() : "");
            item.setText(1, entry.getScope() != null ? entry.getScope().name() : "");
            item.setText(2, entry.getType() != null ? entry.getType().name() : "");
            item.setText(3, entry.getImportance() != null ? entry.getImportance().name() : "");
            item.setText(4, sdf.format(new Date(entry.getUpdatedAt())));
        }

        System.out.println("MEMORY_UI_REFRESH: Properties tab rendered " + entries.size() + " project memory entries");
    }
}
