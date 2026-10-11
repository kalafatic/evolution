package eu.kalafatic.evolution.view.editors.pages.taskstack;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionContext;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FunctionalityInventoryManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FunctionalityInventoryManager.FunctionalityEntry;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.TaskStackPage;
import eu.kalafatic.utils.factories.GUIFactory;

public class FunctionalityInventoryGroup {

    private Composite mainContainer;
    private Group tableGroup;
    private Group actionsGroup;

    private TableViewer tableViewer;
    private List<FunctionalityEntry> entries = new ArrayList<>();
    private TaskStackPage page;
    private MultiPageEditor editor;
    private Orchestrator orchestrator;

    private Button btnMoveUp;
    private Button btnMoveDown;
    private Button btnEdit;
    private Button btnAnalyze;
    private Button btnRefactor;

    public FunctionalityInventoryGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, TaskStackPage page) {
        this.editor = editor;
        this.orchestrator = orchestrator;
        this.page = page;

        mainContainer = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Functionality Intelligence Subsystem", 1, true, false);
        mainContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

        createTableGroup(toolkit, mainContainer);
        createActionsGroup(toolkit, mainContainer);

        refreshInventoryData();
    }

    private void createTableGroup(FormToolkit toolkit, Composite parent) {
        tableGroup = new Group(parent, SWT.NONE);
        tableGroup.setText("Discovered Capabilities & Evaluation Inventory");
        tableGroup.setLayout(new GridLayout(1, false));
        tableGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        if (toolkit != null) toolkit.adapt(tableGroup);

        tableViewer = new TableViewer(tableGroup, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER | SWT.CHECK);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        createColumn("#", 40, e -> String.valueOf(entries.indexOf(e) + 1));
        createColumn("Action", 50, e -> "▶");
        createColumn("Edit", 40, e -> "✎");
        createColumn("ID", 70, e -> e.id);
        createColumn("Functionality", 200, e -> e.name);
        createColumn("Primary Class", 170, e -> e.primaryClass);
        createColumn("Module", 150, e -> e.module);
        createColumn("Importance", 80, e -> formatScore(e.importance));
        createColumn("Usage", 65, e -> formatScore(e.usage));
        createColumn("Size/Cmp", 70, e -> formatScore(e.complexity));
        createColumn("References", 80, e -> formatScore(e.references));
        createColumn("Maturity", 70, e -> formatScore(e.maturity));
        createColumn("Risk", 60, e -> formatScore(e.risk));
        createColumn("Overall", 60, e -> String.valueOf(e.calculateOverallScore()));
        createColumn("Status", 90, e -> e.status);
        createColumn("Method", 75, e -> e.method);
        createColumn("Confidence", 80, e -> String.valueOf(e.confidence));
        createColumn("Freshness", 75, e -> e.metadataFresh ? "FRESH" : "STALE");
        createColumn("Rationale", 250, e -> e.rationale);

        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        // Checkbox listener
        table.addListener(SWT.Selection, event -> {
            if (event.detail == SWT.CHECK && event.item.getData() instanceof FunctionalityEntry fe) {
                fe.selected = ((org.eclipse.swt.widgets.TableItem) event.item).getChecked();
                updateButtonEnablement();
            }
        });

        // Mouse click listener for Action and Edit columns
        table.addListener(SWT.MouseDown, event -> {
            org.eclipse.swt.graphics.Point pt = new org.eclipse.swt.graphics.Point(event.x, event.y);
            org.eclipse.swt.widgets.TableItem item = table.getItem(pt);
            if (item != null && item.getData() instanceof FunctionalityEntry entry) {
                for (int col = 0; col < table.getColumnCount(); col++) {
                    if (item.getBounds(col).contains(pt)) {
                        if (col == 1) { // Action Column (▶)
                            handleSingleAction(entry);
                        } else if (col == 2) { // Edit Column (✎)
                            handleEditRow(entry);
                        }
                    }
                }
            }
        });

        // Selection Listener
        tableViewer.addSelectionChangedListener(event -> updateButtonEnablement());

        // Double Click Listener to edit selected row
        tableViewer.addDoubleClickListener(event -> handleEditSelectedRow());
    }

    private void createActionsGroup(FormToolkit toolkit, Composite parent) {
        actionsGroup = new Group(parent, SWT.NONE);
        actionsGroup.setText("Inventory Management & Isolated AI Session Actions");
        actionsGroup.setLayout(new GridLayout(8, false));
        actionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (toolkit != null) toolkit.adapt(actionsGroup);

        btnMoveUp = new Button(actionsGroup, SWT.PUSH);
        btnMoveUp.setText("▲ Move Up");
        btnMoveUp.setEnabled(false);
        btnMoveUp.addListener(SWT.Selection, e -> handleMoveRow(-1));

        btnMoveDown = new Button(actionsGroup, SWT.PUSH);
        btnMoveDown.setText("▼ Move Down");
        btnMoveDown.setEnabled(false);
        btnMoveDown.addListener(SWT.Selection, e -> handleMoveRow(1));

        Button btnSelectAll = new Button(actionsGroup, SWT.PUSH);
        btnSelectAll.setText("☑ Select All");
        btnSelectAll.addListener(SWT.Selection, e -> setAllSelected(true));

        Button btnUnselectAll = new Button(actionsGroup, SWT.PUSH);
        btnUnselectAll.setText("☐ Unselect All");
        btnUnselectAll.addListener(SWT.Selection, e -> setAllSelected(false));

        btnEdit = new Button(actionsGroup, SWT.PUSH);
        btnEdit.setText("Edit Selected Functionality...");
        btnEdit.setEnabled(false);
        btnEdit.addListener(SWT.Selection, e -> handleEditSelectedRow());

        Button btnRefresh = new Button(actionsGroup, SWT.PUSH);
        btnRefresh.setText("Generate / Refresh Inventory");
        btnRefresh.addListener(SWT.Selection, e -> handleRefreshInventory());

        btnAnalyze = new Button(actionsGroup, SWT.PUSH);
        btnAnalyze.setText("Analyze Selected in New AI Session");
        btnAnalyze.setEnabled(false);
        btnAnalyze.addListener(SWT.Selection, e -> handleAnalyzeInNewSession("ANALYZE"));

        btnRefactor = new Button(actionsGroup, SWT.PUSH);
        btnRefactor.setText("Refactor Selected in New AI Session");
        btnRefactor.setEnabled(false);
        btnRefactor.addListener(SWT.Selection, e -> handleAnalyzeInNewSession("REFACTOR"));
    }

    private void setAllSelected(boolean select) {
        for (FunctionalityEntry entry : entries) {
            entry.selected = select;
        }
        for (org.eclipse.swt.widgets.TableItem item : tableViewer.getTable().getItems()) {
            item.setChecked(select);
        }
        updateButtonEnablement();
    }

    private void handleSingleAction(FunctionalityEntry entry) {
        if (entry != null) {
            List<FunctionalityEntry> singleList = Collections.singletonList(entry);
            createAndExecuteSession("ANALYZE", singleList);
        }
    }

    private void handleEditRow(FunctionalityEntry entry) {
        if (entry != null) {
            File repoRoot = resolveRepoRoot();
            FunctionalityEditDialog dialog = new FunctionalityEditDialog(tableViewer.getTable().getShell(), entry, repoRoot);
            if (dialog.open() == org.eclipse.jface.dialogs.Dialog.OK) {
                tableViewer.refresh(entry);
                if (editor != null) editor.setDirty(true);
            }
        }
    }

    private void createColumn(String title, int width, java.util.function.Function<FunctionalityEntry, String> labelFunc) {
        TableViewerColumn col = new TableViewerColumn(tableViewer, SWT.NONE);
        col.getColumn().setText(title);
        col.getColumn().setWidth(width);
        col.getColumn().setResizable(true);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof FunctionalityEntry) {
                    return labelFunc.apply((FunctionalityEntry) element);
                }
                return "";
            }
        });
    }

    private String formatScore(int val) {
        return val >= 0 ? String.valueOf(val) : "UNKNOWN";
    }

    private void updateButtonEnablement() {
        IStructuredSelection sel = (IStructuredSelection) tableViewer.getSelection();
        boolean hasSel = !sel.isEmpty();
        boolean singleSel = sel.size() == 1;

        if (btnEdit != null) btnEdit.setEnabled(singleSel);
        if (btnAnalyze != null) btnAnalyze.setEnabled(hasSel);
        if (btnRefactor != null) btnRefactor.setEnabled(hasSel);

        if (singleSel) {
            FunctionalityEntry selected = (FunctionalityEntry) sel.getFirstElement();
            int idx = entries.indexOf(selected);
            if (btnMoveUp != null) btnMoveUp.setEnabled(idx > 0);
            if (btnMoveDown != null) btnMoveDown.setEnabled(idx >= 0 && idx < entries.size() - 1);
        } else {
            if (btnMoveUp != null) btnMoveUp.setEnabled(false);
            if (btnMoveDown != null) btnMoveDown.setEnabled(false);
        }
    }

    private void handleMoveRow(int delta) {
        IStructuredSelection sel = (IStructuredSelection) tableViewer.getSelection();
        if (sel.size() != 1) return;

        FunctionalityEntry selected = (FunctionalityEntry) sel.getFirstElement();
        int idx = entries.indexOf(selected);
        int newIdx = idx + delta;

        if (newIdx >= 0 && newIdx < entries.size()) {
            Collections.swap(entries, idx, newIdx);
            tableViewer.refresh();
            tableViewer.setSelection(sel, true);
            updateButtonEnablement();
        }
    }

    private void handleEditSelectedRow() {
        IStructuredSelection sel = (IStructuredSelection) tableViewer.getSelection();
        if (sel.size() != 1) return;

        FunctionalityEntry selected = (FunctionalityEntry) sel.getFirstElement();
        File repoRoot = resolveRepoRoot();

        FunctionalityEditDialog dialog = new FunctionalityEditDialog(tableViewer.getTable().getShell(), selected, repoRoot);
        if (dialog.open() == org.eclipse.jface.dialogs.Dialog.OK) {
            tableViewer.refresh(selected);
            if (editor != null) editor.setDirty(true);
        }
    }

    public void refreshInventoryData() {
        Job job = new Job("Loading Functionality Inventory") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                File root = resolveRepoRoot();
                entries = FunctionalityInventoryManager.getInstance().discoverCoreFunctionalities(root);

                Display.getDefault().asyncExec(() -> {
                    if (tableViewer != null && !tableViewer.getTable().isDisposed()) {
                        tableViewer.setInput(entries);
                        tableViewer.refresh();
                        updateButtonEnablement();
                    }
                });
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    private void handleRefreshInventory() {
        Job job = new Job("Generating Functionality Inventory") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                File root = resolveRepoRoot();
                FunctionalityInventoryManager.getInstance().generateInventory(root,
                        orchestrator != null ? orchestrator.getName() : "EVO", "HEAD");
                entries = FunctionalityInventoryManager.getInstance().discoverCoreFunctionalities(root);

                Display.getDefault().asyncExec(() -> {
                    if (tableViewer != null && !tableViewer.getTable().isDisposed()) {
                        tableViewer.setInput(entries);
                        tableViewer.refresh();
                        updateButtonEnablement();
                    }
                });
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    private void handleAnalyzeInNewSession(String mode) {
        IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
        if (selection.isEmpty()) return;

        List<FunctionalityEntry> selectedEntries = new ArrayList<>();
        for (Object item : selection.toList()) {
            if (item instanceof FunctionalityEntry) {
                selectedEntries.add((FunctionalityEntry) item);
            }
        }

        if (selectedEntries.isEmpty()) return;

        File repoRoot = resolveRepoRoot();
        String newSessionId = "func-session-" + System.currentTimeMillis();
        SessionContainer newSession = SessionManager.getInstance().getOrCreateSession(newSessionId);

        TaskContext taskCtx = new TaskContext(orchestrator, repoRoot);
        taskCtx.setSessionId(newSessionId);
        if (newSession instanceof SessionContext) {
            ((SessionContext) newSession).setTaskContext(taskCtx);
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("[").append(mode).append(" MODE] Isolated AI Session Context for Selected Capabilities:\n\n");

        for (FunctionalityEntry e : selectedEntries) {
            prompt.append("=== FUNCTIONALITY: ").append(e.name).append(" (ID: ").append(e.id).append(") ===\n");
            prompt.append("Primary Java Class: ").append(e.fqcn).append("\n");
            prompt.append("Module: ").append(e.module).append("\n");
            prompt.append("Source Path: ").append(e.sourcePath).append("\n");
            prompt.append("Status: ").append(e.status).append("\n");
            prompt.append("Evaluation Dimensions (0-100):\n");
            prompt.append("  • Importance: ").append(e.importance).append("\n");
            prompt.append("  • Usage Estimate: ").append(e.usage).append("\n");
            prompt.append("  • Complexity: ").append(e.complexity).append("\n");
            prompt.append("  • Centrality / References: ").append(e.references).append("\n");
            prompt.append("  • Maturity: ").append(e.maturity).append("\n");
            prompt.append("  • Maintainability Risk: ").append(e.risk).append("\n");
            prompt.append("  • Overall Score: ").append(e.calculateOverallScore()).append("\n");
            prompt.append("Rationale: ").append(e.rationale).append("\n");
            prompt.append("Description: ").append(e.description).append("\n\n");

            // Attach source code snippet if available
            File srcFile = new File(repoRoot, e.sourcePath);
            if (srcFile.exists()) {
                try {
                    String srcContent = Files.readString(srcFile.toPath());
                    if (srcContent.length() > 4000) {
                        srcContent = srcContent.substring(0, 4000) + "\n... [truncated for context limit]";
                    }
                    prompt.append("--- SOURCE CODE SNIPPET (").append(e.primaryClass).append(".java) ---\n");
                    prompt.append(srcContent).append("\n-----------------------------------------------\n\n");
                } catch (Exception ex) {
                    prompt.append("[Source file read exception: ").append(ex.getMessage()).append("]\n\n");
                }
            }
        }

        prompt.append("Task Instructions:\n");
        if ("REFACTOR".equals(mode)) {
            prompt.append("Propose concrete refactoring steps, modularization enhancements, or risk mitigations based on the source code and evaluation scores.");
        } else {
            prompt.append("Provide a comprehensive architectural analysis, dependency review, and correctness assessment for the primary classes above.");
        }

        Task newTask = OrchestrationFactory.eINSTANCE.createTask();
        newTask.setId(newSessionId);
        newTask.setName("[" + mode + "] " + selectedEntries.get(0).name);
        newTask.setPrompt(prompt.toString());
        newTask.setDescription("Isolated " + mode + " session for " + selectedEntries.size() + " functionalities");
        newTask.setStatus(TaskStatus.READY);

        if (orchestrator != null) {
            orchestrator.getTasks().add(newTask);
            if (editor != null) editor.setDirty(true);
            page.runSingleTask(newTask);
        }
    }

    private void createAndExecuteSession(String mode, List<FunctionalityEntry> selectedEntries) {
        if (selectedEntries == null || selectedEntries.isEmpty()) return;

        File repoRoot = resolveRepoRoot();
        String newSessionId = "func-session-" + System.currentTimeMillis();
        SessionContainer newSession = SessionManager.getInstance().getOrCreateSession(newSessionId);

        TaskContext taskCtx = new TaskContext(orchestrator, repoRoot);
        taskCtx.setSessionId(newSessionId);
        if (newSession instanceof SessionContext) {
            ((SessionContext) newSession).setTaskContext(taskCtx);
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("[").append(mode).append(" MODE] Isolated AI Session Context for Selected Capabilities:\n\n");

        for (FunctionalityEntry e : selectedEntries) {
            prompt.append("=== FUNCTIONALITY: ").append(e.name).append(" (ID: ").append(e.id).append(") ===\n");
            prompt.append("Primary Java Class: ").append(e.fqcn).append("\n");
            prompt.append("Module: ").append(e.module).append("\n");
            prompt.append("Source Path: ").append(e.sourcePath).append("\n");
            prompt.append("Status: ").append(e.status).append("\n");
            prompt.append("Rationale: ").append(e.rationale).append("\n");
            prompt.append("Description: ").append(e.description).append("\n\n");

            File srcFile = new File(repoRoot, e.sourcePath);
            if (srcFile.exists()) {
                try {
                    String srcContent = Files.readString(srcFile.toPath());
                    if (srcContent.length() > 4000) {
                        srcContent = srcContent.substring(0, 4000) + "\n... [truncated for context limit]";
                    }
                    prompt.append("--- SOURCE CODE SNIPPET (").append(e.primaryClass).append(".java) ---\n");
                    prompt.append(srcContent).append("\n-----------------------------------------------\n\n");
                } catch (Exception ex) {
                    prompt.append("[Source file read exception: ").append(ex.getMessage()).append("]\n\n");
                }
            }
        }

        Task newTask = OrchestrationFactory.eINSTANCE.createTask();
        newTask.setId(newSessionId);
        newTask.setName("[" + mode + "] " + selectedEntries.get(0).name);
        newTask.setPrompt(prompt.toString());
        newTask.setDescription("Isolated " + mode + " session for " + selectedEntries.size() + " functionalities");
        newTask.setStatus(TaskStatus.READY);

        if (orchestrator != null) {
            orchestrator.getTasks().add(newTask);
            if (editor != null) editor.setDirty(true);
            page.runSingleTask(newTask);
        }
    }

    private File resolveRepoRoot() {
        String codebase = ProjectModelManager.getCodebasePath();
        if (codebase != null && !codebase.isEmpty() && new File(codebase).exists()) {
            return new File(codebase);
        }
        return new File(System.getProperty("user.dir"));
    }
}
