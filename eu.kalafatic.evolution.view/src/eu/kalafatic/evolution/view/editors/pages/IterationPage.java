package eu.kalafatic.evolution.view.editors.pages;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.json.JSONArray;
import org.json.JSONObject;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import eu.kalafatic.evolution.view.factories.SWTFactory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.application.Activator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class IterationPage extends AEvoPage {

    private static class SelfDevRow {
        String name;
        String path;
        String status;

        SelfDevRow(String name, String path, String status) {
            this.name = name;
            this.path = path;
            this.status = status;
        }
    }

    private IterationMemoryService memoryService;

    private Map<Integer, List<IterationRecord>> iterationsMap = new TreeMap<>();
    private List<Integer> iterationNumbers = new ArrayList<>();
    private int currentIterationIndex = -1;

    private Label iterationLabel;
    private Label goalLabel;
    private Label resultLabel;
    private Button prevBtn;
    private Button nextBtn;

    private TableViewer branchTable;
    private TableViewer selfDevTable;
    private Composite flowComposite;
    private StyledText logText;

    private ImageRegistry imageRegistry;

    private Label[] flowSteps = new Label[6];
    private String[] stepNames = {"PLAN", "CODE", "TEST", "SCORE", "SELECT", "MERGE"};

    public IterationPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, editor, orchestrator);
        this.setLayout(new GridLayout(1, false));

        initImageRegistry();
        initMemoryService();
        createControl();
        refreshData();
    }

    private void initImageRegistry() {
        this.imageRegistry = new ImageRegistry(Display.getDefault());
        registerImage("play", "eu.kalafatic.utils", "icons/actions/play.png");
        registerImage("pause", "eu.kalafatic.utils", "icons/actions/pause.png");
        registerImage("stop", "eu.kalafatic.utils", "icons/actions/stop.png");
        registerImage("resume", "eu.kalafatic.utils", "icons/actions/restart.png");
        registerImage("edit", "eu.kalafatic.utils", "icons/ovr16/write.gif");
    }

    private void registerImage(String key, String pluginId, String path) {
        ImageDescriptor desc = Activator.getImageDescriptor(pluginId, path);
        if (desc != null) {
            imageRegistry.put(key, desc);
        }
    }

    private void initMemoryService() {
        File projectRoot = null;
        if (orchestrator != null && orchestrator.eResource() != null) {
            org.eclipse.emf.common.util.URI uri = orchestrator.eResource().getURI();
            if (uri.isPlatformResource()) {
                org.eclipse.core.resources.IResource res = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().findMember(uri.toPlatformString(true));
                if (res != null) projectRoot = res.getProject().getLocation().toFile();
            } else if (uri.isFile()) {
                projectRoot = new File(uri.toFileString()).getParentFile();
            }
        }
        if (projectRoot == null) {
            IEditorInput input = editor.getEditorInput();
            if (input instanceof IFileEditorInput) {
                projectRoot = ((IFileEditorInput) input).getFile().getProject().getLocation().toFile();
            }
        }
        if (projectRoot != null) {
            this.memoryService = new IterationMemoryService(projectRoot);
        }
    }

    private void createControl() {
        Composite container = toolkit.createComposite(this);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));
        this.setContent(container);

        // Top Bar
        Composite topBar = toolkit.createComposite(container);
        topBar.setLayout(new GridLayout(4, false));
        topBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        prevBtn = toolkit.createButton(topBar, "< Prev", SWT.PUSH);
        iterationLabel = toolkit.createLabel(topBar, "Iteration: N/A");
        iterationLabel.setFont(org.eclipse.jface.resource.JFaceResources.getBannerFont());
        nextBtn = toolkit.createButton(topBar, "Next >", SWT.PUSH);

        prevBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (currentIterationIndex > 0) {
                    currentIterationIndex--;
                    updateUI();
                }
            }
        });

        nextBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (currentIterationIndex < iterationNumbers.size() - 1) {
                    currentIterationIndex++;
                    updateUI();
                }
            }
        });

        // 1. Branches Section
        Composite branchesComp = SWTFactory.createExpandableGroup(toolkit, container, "Branches", 1, true);

        goalLabel = toolkit.createLabel(branchesComp, "Goal: ");
        goalLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        resultLabel = toolkit.createLabel(branchesComp, "Result: ");
        resultLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        branchTable = new TableViewer(branchesComp, SWT.BORDER | SWT.FULL_SELECTION);
        Table table = branchTable.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData gdTable = new GridData(GridData.FILL_HORIZONTAL);
        gdTable.heightHint = 100;
        table.setLayoutData(gdTable);

        createColumns();
        branchTable.setContentProvider(ArrayContentProvider.getInstance());
        branchTable.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                updateDetails();
            }
        });

        // Flow View
        toolkit.createLabel(branchesComp, "Evolution Flow:");
        flowComposite = toolkit.createComposite(branchesComp);
        flowComposite.setLayout(new GridLayout(11, false)); // 6 steps + 5 arrows
        flowComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        for (int i = 0; i < 6; i++) {
            flowSteps[i] = toolkit.createLabel(flowComposite, stepNames[i], SWT.CENTER);
            flowSteps[i].setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
            if (i < 5) {
                Label arrow = toolkit.createLabel(flowComposite, "\u2192", SWT.CENTER);
                arrow.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
            }
        }

        // 2. Logs Section
        Composite logComp = SWTFactory.createExpandableGroup(toolkit, container, "Logs", 1, true, true);

        logText = new StyledText(logComp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        logText.setLayoutData(new GridData(GridData.FILL_BOTH));
        logText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

        // 3. Self-Development Section
        Composite selfDevComp = SWTFactory.createExpandableGroup(toolkit, container, "Self-Development", 1, true);

        selfDevTable = new TableViewer(selfDevComp, SWT.BORDER | SWT.FULL_SELECTION);
        Table sdTable = selfDevTable.getTable();
        sdTable.setHeaderVisible(true);
        sdTable.setLinesVisible(true);
        GridData gdSdTable = new GridData(GridData.FILL_HORIZONTAL);
        gdSdTable.heightHint = 80;
        sdTable.setLayoutData(gdSdTable);

        createSelfDevColumns();
        selfDevTable.setContentProvider(ArrayContentProvider.getInstance());
        List<SelfDevRow> sdData = new ArrayList<>();
        sdData.add(new SelfDevRow("Supervisor", "path", "ready"));
        sdData.add(new SelfDevRow("RCP EVO", "path", "ready"));
        selfDevTable.setInput(sdData);

        sdTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.item != null && e.item.getData() instanceof SelfDevRow) {
                    SelfDevRow row = (SelfDevRow) e.item.getData();
                    Table table = (Table) e.widget;
                    int columnIndex = table.getSelectionIndex(); // This is not reliable for column
                }
            }
        });

        // Better: use mouseDown to detect column
        sdTable.addListener(SWT.MouseDown, event -> {
            org.eclipse.swt.graphics.Point pt = new org.eclipse.swt.graphics.Point(event.x, event.y);
            org.eclipse.swt.widgets.TableItem item = sdTable.getItem(pt);
            if (item != null) {
                for (int i = 0; i < sdTable.getColumnCount(); i++) {
                    org.eclipse.swt.graphics.Rectangle rect = item.getBounds(i);
                    if (rect.contains(pt)) {
                        handleSelfDevAction((SelfDevRow) item.getData(), i);
                    }
                }
            }
        });
    }

    private void handleSelfDevAction(SelfDevRow row, int columnIndex) {
        if (columnIndex == 0) { // Action
            if ("running".equals(row.status)) {
                row.status = "paused";
            } else if ("paused".equals(row.status)) {
                row.status = "running";
            } else {
                row.status = "running";
            }
            selfDevTable.refresh(row);
        } else if (columnIndex == 1) { // Edit
            // Open edit dialog or similar
        }
    }

    private void createColumns() {
        String[] titles = { "Branch", "Strategy", "Result", "Score" };
        int[] bounds = { 150, 400, 100, 100 };

        TableViewerColumn col = createTableViewerColumn(titles[0], bounds[0], 0);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                IterationRecord r = (IterationRecord) element;
                return r.getBranch();
            }
        });

        col = createTableViewerColumn(titles[1], bounds[1], 1);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                IterationRecord r = (IterationRecord) element;
                return r.getStrategy();
            }
        });

        col = createTableViewerColumn(titles[2], bounds[2], 2);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                IterationRecord r = (IterationRecord) element;
                return r.getResult() + (r.getResult().equals("SUCCESS") ? " \u2705" : " \u274C");
            }
        });

        col = createTableViewerColumn(titles[3], bounds[3], 3);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                IterationRecord r = (IterationRecord) element;
                return String.format("%.2f", r.getScore());
            }
        });
    }

    private void createSelfDevColumns() {
        String[] titles = { "Action", "Edit", "Name", "Path/URL", "Status" };
        int[] bounds = { 100, 50, 100, 250, 100 };

        TableViewerColumn col = createTableViewerColumn(selfDevTable, titles[0], bounds[0], 0);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                SelfDevRow row = (SelfDevRow) element;
                if ("running".equals(row.status)) {
                    return "\u23F8 \u23F9"; // pause stop
                } else if ("paused".equals(row.status)) {
                    return "\u25B6 \u23F9"; // play stop
                } else {
                    return "\u25B6"; // play
                }
            }
            @Override
            public Image getImage(Object element) {
                SelfDevRow row = (SelfDevRow) element;
                if ("running".equals(row.status)) {
                    return imageRegistry.get("pause");
                } else if ("paused".equals(row.status)) {
                    return imageRegistry.get("play");
                } else {
                    return imageRegistry.get("play");
                }
            }
        });

        col = createTableViewerColumn(selfDevTable, titles[1], bounds[1], 1);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ""; // Icon only
            }
            @Override
            public Image getImage(Object element) {
                return imageRegistry.get("edit");
            }
        });

        col = createTableViewerColumn(selfDevTable, titles[2], bounds[2], 2);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((SelfDevRow) element).name;
            }
        });

        col = createTableViewerColumn(selfDevTable, titles[3], bounds[3], 3);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((SelfDevRow) element).path;
            }
        });

        col = createTableViewerColumn(selfDevTable, titles[4], bounds[4], 4);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((SelfDevRow) element).status;
            }
        });
    }

    private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
        return createTableViewerColumn(branchTable, title, bound, colNumber);
    }

    private TableViewerColumn createTableViewerColumn(TableViewer viewer, String title, int bound, final int colNumber) {
        final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
        final org.eclipse.swt.widgets.TableColumn column = viewerColumn.getColumn();
        column.setText(title);
        column.setWidth(bound);
        column.setResizable(true);
        column.setMoveable(true);
        return viewerColumn;
    }

    public void refreshData() {
        if (memoryService == null) return;

        List<IterationRecord> records = memoryService.getRecords();
        iterationsMap = records.stream().collect(Collectors.groupingBy(IterationRecord::getIteration, TreeMap::new, Collectors.toList()));
        iterationNumbers = new ArrayList<>(iterationsMap.keySet());

        if (currentIterationIndex == -1 && !iterationNumbers.isEmpty()) {
            currentIterationIndex = iterationNumbers.size() - 1; // Last one
        }

        updateUI();
    }

    private void updateUI() {
        if (currentIterationIndex < 0 || currentIterationIndex >= iterationNumbers.size()) {
            iterationLabel.setText("Iteration: N/A");
            goalLabel.setText("Goal: ");
            resultLabel.setText("Result: ");
            branchTable.setInput(Collections.emptyList());
            return;
        }

        int itNum = iterationNumbers.get(currentIterationIndex);
        List<IterationRecord> variants = iterationsMap.get(itNum);

        iterationLabel.setText("Iteration: " + itNum);
        prevBtn.setEnabled(currentIterationIndex > 0);
        nextBtn.setEnabled(currentIterationIndex < iterationNumbers.size() - 1);

        if (!variants.isEmpty()) {
            IterationRecord first = variants.get(0);
            goalLabel.setText("Goal: " + first.getGoal());

            boolean anySuccess = variants.stream().anyMatch(v -> "SUCCESS".equals(v.getResult()));
            resultLabel.setText("Result: " + (anySuccess ? "SUCCESS \u2705" : "FAILED \u274C"));
            resultLabel.setForeground(anySuccess ? Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN) : Display.getCurrent().getSystemColor(SWT.COLOR_RED));
        }

        branchTable.setInput(variants);
        if (!variants.isEmpty()) {
            IterationRecord selected = variants.stream().filter(v -> "SUCCESS".equals(v.getResult())).findFirst().orElse(variants.get(0));
            branchTable.setSelection(new org.eclipse.jface.viewers.StructuredSelection(selected));
        }
        updateDetails();
    }

    private void updateDetails() {
        IStructuredSelection selection = (IStructuredSelection) branchTable.getSelection();
        IterationRecord selected = (IterationRecord) selection.getFirstElement();

        if (selected == null) {
            logText.setText("");
            updateFlow(false);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Strategy: ").append(selected.getStrategy()).append("\n");
        sb.append("Branch: ").append(selected.getBranch()).append("\n");
        sb.append("Result: ").append(selected.getResult()).append("\n");
        sb.append("Score: ").append(selected.getScore()).append("\n");
        if (selected.getErrorMessage() != null && !selected.getErrorMessage().isEmpty()) {
            sb.append("\nError:\n").append(selected.getErrorMessage()).append("\n");
        }
        if (selected.getChangedFiles() != null && !selected.getChangedFiles().isEmpty()) {
            sb.append("\nChanged Files:\n");
            for (String f : selected.getChangedFiles()) {
                sb.append("- ").append(f).append("\n");
            }
        }
        logText.setText(sb.toString());

        updateFlow("SUCCESS".equals(selected.getResult()));
    }

    private void updateFlow(boolean success) {
        for (int i = 0; i < 6; i++) {
            if (success) {
                flowSteps[i].setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
                flowSteps[i].setText(stepNames[i] + " \u2714");
            } else {
                // If it failed, maybe only PLAN/CODE/TEST/SCORE were done?
                // For simplicity, just show checkmarks up to SCORE if it failed there
                if (i <= 3) {
                    flowSteps[i].setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
                    flowSteps[i].setText(stepNames[i] + " \u2714");
                } else {
                    flowSteps[i].setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
                    flowSteps[i].setText(stepNames[i] + " \u2718");
                }
            }
        }
        flowComposite.layout();
    }

    @Override
    public void setOrchestrator(Orchestrator orchestrator) {
        super.setOrchestrator(orchestrator);
        refreshData();
    }

    @Override
    protected void refreshUI() {
        refreshData();
    }

    public void updateUIFromModel() {
        scheduleRefresh();
    }

    @Override
    public void dispose() {
        if (imageRegistry != null) {
            imageRegistry.dispose();
        }
        if (toolkit != null) toolkit.dispose();
        super.dispose();
    }
}
