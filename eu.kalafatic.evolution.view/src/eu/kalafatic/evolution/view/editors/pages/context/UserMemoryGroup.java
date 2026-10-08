package eu.kalafatic.evolution.view.editors.pages.context;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.controller.memory.MemoryContextProvider;
import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryQuery;
import eu.kalafatic.evolution.controller.memory.MemoryScope;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.memory.MemoryType;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

public class UserMemoryGroup extends AEvoGroup {

    private Combo scopeFilterCombo;
    private Combo typeFilterCombo;
    private Text searchInput;
    private Table memoryTable;
    private Text promptPreview;

    private Button addBtn;
    private Button editBtn;
    private Button deleteBtn;
    private Button clearBtn;
    private Button refreshBtn;

    public UserMemoryGroup(Composite parent, MultiPageEditor editor, Orchestrator orchestrator, File projectRoot) {
        super(editor, orchestrator);

        Group g = new Group(parent, SWT.NONE);
        g.setText("EVO Persistent User & Context Memory");
        g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        this.group = g;
        createContent(g);
    }

    private void createContent(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        // Filter Bar
        Composite filterComp = new Composite(parent, SWT.NONE);
        filterComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        filterComp.setLayout(new GridLayout(6, false));

        new Label(filterComp, SWT.NONE).setText("Scope:");
        scopeFilterCombo = new Combo(filterComp, SWT.READ_ONLY);
        scopeFilterCombo.add("ALL");
        for (MemoryScope scope : MemoryScope.values()) {
            scopeFilterCombo.add(scope.name());
        }
        scopeFilterCombo.select(0);

        new Label(filterComp, SWT.NONE).setText("Type:");
        typeFilterCombo = new Combo(filterComp, SWT.READ_ONLY);
        typeFilterCombo.add("ALL");
        for (MemoryType type : MemoryType.values()) {
            typeFilterCombo.add(type.name());
        }
        typeFilterCombo.select(0);

        new Label(filterComp, SWT.NONE).setText("Search:");
        searchInput = new Text(filterComp, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH);
        searchInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        org.eclipse.swt.events.SelectionAdapter filterListener = new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                refreshUI();
            }
        };
        scopeFilterCombo.addSelectionListener(filterListener);
        typeFilterCombo.addSelectionListener(filterListener);
        searchInput.addModifyListener(e -> refreshUI());

        // Memory Table
        memoryTable = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        memoryTable.setHeaderVisible(true);
        memoryTable.setLinesVisible(true);
        GridData tableGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        tableGd.heightHint = 180;
        memoryTable.setLayoutData(tableGd);

        String[] headers = {"Content", "Scope", "Type", "Source", "Importance", "Confidence", "Updated At"};
        int[] widths = {320, 80, 110, 80, 80, 80, 140};
        for (int i = 0; i < headers.length; i++) {
            TableColumn col = new TableColumn(memoryTable, SWT.LEFT);
            col.setText(headers[i]);
            col.setWidth(widths[i]);
        }

        memoryTable.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                updateButtonStates();
            }
        });

        // Action Button Bar
        Composite btnComp = new Composite(parent, SWT.NONE);
        btnComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        btnComp.setLayout(new GridLayout(5, false));

        addBtn = new Button(btnComp, SWT.PUSH);
        addBtn.setText("Add Memory");
        addBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                handleAdd();
            }
        });

        editBtn = new Button(btnComp, SWT.PUSH);
        editBtn.setText("Edit Selected");
        editBtn.setEnabled(false);
        editBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                handleEdit();
            }
        });

        deleteBtn = new Button(btnComp, SWT.PUSH);
        deleteBtn.setText("Delete Selected");
        deleteBtn.setEnabled(false);
        deleteBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                handleDelete();
            }
        });

        clearBtn = new Button(btnComp, SWT.PUSH);
        clearBtn.setText("Clear All");
        clearBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                handleClearAll();
            }
        });

        refreshBtn = new Button(btnComp, SWT.PUSH);
        refreshBtn.setText("Refresh");
        refreshBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                refreshUI();
            }
        });

        // Prompt Context Preview
        new Label(parent, SWT.NONE).setText("Injected Prompt Context Preview:");
        promptPreview = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        GridData previewGd = new GridData(SWT.FILL, SWT.FILL, true, false);
        previewGd.heightHint = 100;
        promptPreview.setLayoutData(previewGd);

        refreshUI();
    }

    private void handleAdd() {
        MemoryEntry entry = new MemoryEntry();
        MemoryEntryDialog dialog = new MemoryEntryDialog(group.getShell(), entry);
        if (dialog.open() == MemoryEntryDialog.OK) {
            MemoryService.getInstance().addEntry(dialog.getEntry());
            refreshUI();
            if (editor != null) editor.setDirty(true);
        }
    }

    private void handleEdit() {
        int index = memoryTable.getSelectionIndex();
        if (index < 0) return;
        TableItem item = memoryTable.getItem(index);
        MemoryEntry entry = (MemoryEntry) item.getData();
        if (entry == null) return;

        MemoryEntryDialog dialog = new MemoryEntryDialog(group.getShell(), entry);
        if (dialog.open() == MemoryEntryDialog.OK) {
            MemoryService.getInstance().updateEntry(dialog.getEntry());
            refreshUI();
            if (editor != null) editor.setDirty(true);
        }
    }

    private void handleDelete() {
        TableItem[] selection = memoryTable.getSelection();
        if (selection == null || selection.length == 0) return;

        if (MessageDialog.openConfirm(group.getShell(), "Delete Memory", "Are you sure you want to delete the selected memory entry/entries?")) {
            for (TableItem item : selection) {
                MemoryEntry entry = (MemoryEntry) item.getData();
                if (entry != null) {
                    MemoryService.getInstance().deleteEntry(entry.getId());
                }
            }
            refreshUI();
            if (editor != null) editor.setDirty(true);
        }
    }

    private void handleClearAll() {
        if (MessageDialog.openConfirm(group.getShell(), "Clear All Memories", "Are you sure you want to permanently clear ALL stored memories?")) {
            MemoryService.getInstance().clearAll();
            refreshUI();
            if (editor != null) editor.setDirty(true);
        }
    }

    private void updateButtonStates() {
        boolean hasSelection = memoryTable.getSelectionCount() > 0;
        editBtn.setEnabled(hasSelection && memoryTable.getSelectionCount() == 1);
        deleteBtn.setEnabled(hasSelection);
    }

    public void setContext(MemoryScope scope, String associatedId) {
        if (scopeFilterCombo != null && !scopeFilterCombo.isDisposed()) {
            if (scope != null) {
                int idx = scopeFilterCombo.indexOf(scope.name());
                if (idx >= 0) {
                    scopeFilterCombo.select(idx);
                }
            } else {
                scopeFilterCombo.select(0);
            }
        }
        if (searchInput != null && !searchInput.isDisposed()) {
            if (associatedId != null && !associatedId.trim().isEmpty()) {
                searchInput.setText(associatedId.trim());
            }
        }
        refreshUI();
    }

    @Override
    protected void refreshUI() {
        if (memoryTable == null || memoryTable.isDisposed()) return;

        memoryTable.removeAll();

        MemoryQuery query = new MemoryQuery();
        if (scopeFilterCombo.getSelectionIndex() > 0) {
            query.setScope(MemoryScope.valueOf(scopeFilterCombo.getText()));
        }
        if (typeFilterCombo.getSelectionIndex() > 0) {
            query.setType(MemoryType.valueOf(typeFilterCombo.getText()));
        }
        if (!searchInput.getText().trim().isEmpty()) {
            query.setSearchText(searchInput.getText().trim());
        }

        List<MemoryEntry> entries = MemoryService.getInstance().retrieveRelevant(query);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (MemoryEntry entry : entries) {
            TableItem item = new TableItem(memoryTable, SWT.NONE);
            item.setData(entry);
            item.setText(0, entry.getContent() != null ? entry.getContent() : "");
            item.setText(1, entry.getScope() != null ? entry.getScope().name() : "");
            item.setText(2, entry.getType() != null ? entry.getType().name() : "");
            item.setText(3, entry.getSource() != null ? entry.getSource().name() : "");
            item.setText(4, entry.getImportance() != null ? entry.getImportance().name() : "");
            item.setText(5, String.format("%.2f", entry.getConfidence()));
            item.setText(6, sdf.format(new Date(entry.getUpdatedAt())));
        }

        updateButtonStates();

        if (promptPreview != null && !promptPreview.isDisposed()) {
            MemoryContextProvider provider = new MemoryContextProvider();
            String contextText = provider.buildMemoryContext(searchInput.getText().trim());
            setTextSafe(promptPreview, contextText);
        }

        System.out.println("MEMORY_UI_REFRESH: Rendered " + entries.size() + " memory entries");
    }
}
