package eu.kalafatic.evolution.view.editors.pages;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
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
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class IterationPage extends Composite {

    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private FormToolkit toolkit;
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
    private Composite flowComposite;
    private StyledText logText;

    private Label[] flowSteps = new Label[6];
    private String[] stepNames = {"PLAN", "CODE", "TEST", "SCORE", "SELECT", "MERGE"};

    public IterationPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.NONE);
        this.editor = editor;
        this.orchestrator = orchestrator;
        this.toolkit = new FormToolkit(Display.getCurrent());
        this.setLayout(new GridLayout(1, false));

        initMemoryService();
        createControl();
        refreshData();
    }

    private void initMemoryService() {
        File projectRoot = null;
        IEditorInput input = editor.getEditorInput();
        if (input instanceof IFileEditorInput) {
            projectRoot = ((IFileEditorInput) input).getFile().getProject().getLocation().toFile();
        }
        if (projectRoot != null) {
            this.memoryService = new IterationMemoryService(projectRoot);
        }
    }

    private void createControl() {
        // Top Bar
        Composite topBar = toolkit.createComposite(this);
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

        Composite infoComp = toolkit.createComposite(this);
        infoComp.setLayout(new GridLayout(1, false));
        infoComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        goalLabel = toolkit.createLabel(infoComp, "Goal: ");
        goalLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        resultLabel = toolkit.createLabel(infoComp, "Result: ");
        resultLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Branch Table
        Section branchSection = toolkit.createSection(this, Section.TITLE_BAR);
        branchSection.setText("Branches");
        branchSection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite branchComp = toolkit.createComposite(branchSection);
        branchComp.setLayout(new GridLayout(1, false));
        branchSection.setClient(branchComp);

        branchTable = new TableViewer(branchComp, SWT.BORDER | SWT.FULL_SELECTION);
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
        Section flowSection = toolkit.createSection(this, Section.TITLE_BAR);
        flowSection.setText("Flow");
        flowSection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        flowComposite = toolkit.createComposite(flowSection);
        flowComposite.setLayout(new GridLayout(11, false)); // 6 steps + 5 arrows
        flowSection.setClient(flowComposite);

        for (int i = 0; i < 6; i++) {
            flowSteps[i] = toolkit.createLabel(flowComposite, stepNames[i], SWT.CENTER);
            flowSteps[i].setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
            if (i < 5) {
                Label arrow = toolkit.createLabel(flowComposite, "\u2192", SWT.CENTER);
                arrow.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
            }
        }

        // Log Area
        Section logSection = toolkit.createSection(this, Section.TITLE_BAR | Section.EXPANDED);
        logSection.setText("Log (selected branch)");
        logSection.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite logComp = toolkit.createComposite(logSection);
        logComp.setLayout(new GridLayout(1, false));
        logSection.setClient(logComp);

        logText = new StyledText(logComp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        logText.setLayoutData(new GridData(GridData.FILL_BOTH));
        logText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
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

    private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
        final TableViewerColumn viewerColumn = new TableViewerColumn(branchTable, SWT.NONE);
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

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        refreshData();
    }

    public void updateUIFromModel() {
        Display.getDefault().asyncExec(() -> {
            if (!isDisposed()) {
                refreshData();
            }
        });
    }

    @Override
    public void dispose() {
        if (toolkit != null) toolkit.dispose();
        super.dispose();
    }
}
