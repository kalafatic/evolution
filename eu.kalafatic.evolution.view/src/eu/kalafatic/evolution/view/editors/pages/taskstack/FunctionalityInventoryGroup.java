package eu.kalafatic.evolution.view.editors.pages.taskstack;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
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

public class FunctionalityInventoryGroup {

    private Group group;
    private TableViewer tableViewer;
    private List<FunctionalityEntry> entries = new ArrayList<>();
    private TaskStackPage page;
    private MultiPageEditor editor;
    private Orchestrator orchestrator;

    public FunctionalityInventoryGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, TaskStackPage page) {
        this.editor = editor;
        this.orchestrator = orchestrator;
        this.page = page;

        group = new Group(parent, SWT.NONE);
        group.setText("Functionality Intelligence Inventory (28 Core EVO Capabilities)");
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        if (toolkit != null) toolkit.adapt(group);

        createTableViewer();
        createButtons(group);

        refreshInventoryData();
    }

    private void createTableViewer() {
        tableViewer = new TableViewer(group, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        createColumn("Functionality", 220, e -> e.name);
        createColumn("Primary Class", 180, e -> e.primaryClass);
        createColumn("Module", 160, e -> e.module);
        createColumn("Importance", 80, e -> formatScore(e.importance));
        createColumn("Usage", 70, e -> formatScore(e.usage));
        createColumn("Size", 70, e -> formatScore(e.complexity));
        createColumn("References", 80, e -> formatScore(e.references));
        createColumn("Maturity", 70, e -> formatScore(e.maturity));
        createColumn("Risk", 60, e -> formatScore(e.risk));
        createColumn("Overall", 60, e -> String.valueOf(e.calculateOverallScore()));
        createColumn("Status", 90, e -> e.status);
        createColumn("Freshness", 80, e -> e.metadataFresh ? "FRESH" : "STALE");

        tableViewer.setContentProvider(ArrayContentProvider.getInstance());
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

    private void createButtons(Composite parent) {
        Composite btnComp = new Composite(parent, SWT.NONE);
        btnComp.setLayout(new GridLayout(3, false));
        btnComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button btnRefresh = new Button(btnComp, SWT.PUSH);
        btnRefresh.setText("Generate / Refresh Inventory");
        btnRefresh.addListener(SWT.Selection, e -> handleRefreshInventory());

        Button btnAnalyze = new Button(btnComp, SWT.PUSH);
        btnAnalyze.setText("Analyze Selected in New AI Session");
        btnAnalyze.addListener(SWT.Selection, e -> handleAnalyzeInNewSession("ANALYZE"));

        Button btnRefactor = new Button(btnComp, SWT.PUSH);
        btnRefactor.setText("Refactor Selected in New AI Session");
        btnRefactor.addListener(SWT.Selection, e -> handleAnalyzeInNewSession("REFACTOR"));
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

    private File resolveRepoRoot() {
        String codebase = ProjectModelManager.getCodebasePath();
        if (codebase != null && !codebase.isEmpty() && new File(codebase).exists()) {
            return new File(codebase);
        }
        return new File(System.getProperty("user.dir"));
    }
}
